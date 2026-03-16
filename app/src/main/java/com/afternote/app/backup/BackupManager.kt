package com.afternote.app.backup

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.afternote.app.data.model.*
import com.afternote.app.data.repository.NoteRepository
import com.afternote.app.data.repository.VaultRepository
import com.afternote.app.security.CryptoManager
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AfterNote Encrypted Backup — .emb format v2
 *
 * Binary layout:
 *  [4 bytes]  Magic: 0x454D4202  ("EMB" + version 2)
 *  [12 bytes] AES-GCM IV
 *  [4 bytes]  Encrypted payload length (big-endian int)
 *  [N bytes]  AES-256-GCM ciphertext (GCM tag embedded at end)
 *
 * AAD bound to ciphertext (tamper-proof):
 *   "AfterNote|v2|com.afternote.app"
 *
 * This means:
 *  - Copying the .emb to a different app → decryption fails (AAD mismatch)
 *  - Modifying any byte of ciphertext → GCM auth tag fails
 *  - Without the 256-bit Recovery Key → mathematically infeasible to decrypt
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val noteRepo: NoteRepository,
    private val vaultRepo: VaultRepository,
    private val crypto: CryptoManager
) {
    companion object {
        // Version 2 magic — old v1 files will be rejected
        private val MAGIC     = byteArrayOf(0x45, 0x4D, 0x42, 0x02)
        private val gson      = Gson()
        private const val AAD = "AfterNote|v2|com.afternote.app"
    }

    // ── Export ────────────────────────────────────────────────────────────────

    suspend fun exportToUri(uri: Uri, recoveryKey: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val keyBytes = crypto.recoveryKeyToBytes(recoveryKey)
                val payload  = buildPayload()
                val json     = gson.toJson(payload).toByteArray(Charsets.UTF_8)

                // AAD = app identity — tamper / cross-app use fails GCM tag
                val (ciphertext, iv) = crypto.encryptWithKey(json, keyBytes, AAD.toByteArray())

                context.contentResolver.openOutputStream(uri)!!.use { out ->
                    out.write(MAGIC)
                    out.write(iv)                                              // 12 bytes
                    out.write(ByteBuffer.allocate(4).putInt(ciphertext.size).array())
                    out.write(ciphertext)
                }
            }
        }

    // ── Import ────────────────────────────────────────────────────────────────

    suspend fun importFromUri(uri: Uri, recoveryKey: String): Result<ImportStats> =
        withContext(Dispatchers.IO) {
            runCatching {
                val keyBytes = crypto.recoveryKeyToBytes(recoveryKey)

                context.contentResolver.openInputStream(uri)!!.use { stream ->
                    // Magic verification
                    val magic = stream.readExact(4)
                    require(magic.contentEquals(MAGIC)) {
                        "Not a valid AfterNote v2 backup file. " +
                        "If this is a v1 file, export again from the original device."
                    }

                    val iv         = stream.readExact(12)
                    val payloadLen = ByteBuffer.wrap(stream.readExact(4)).int
                    require(payloadLen in 1..(50 * 1024 * 1024)) {
                        "Payload length suspicious: $payloadLen bytes"
                    }
                    val ciphertext = stream.readExact(payloadLen)

                    // Decrypt — AEADBadTagException if key wrong OR file tampered
                    val json    = crypto.decryptWithKey(ciphertext, iv, keyBytes, AAD.toByteArray())
                    val payload = gson.fromJson(String(json, Charsets.UTF_8), BackupPayload::class.java)

                    applyPayload(payload)
                    ImportStats(
                        notesRestored      = payload.notes.size,
                        vaultNotesRestored = payload.vaultNotes.size
                    )
                }
            }
        }

    // ── Private helpers ───────────────────────────────────────────────────────

    private suspend fun buildPayload(): BackupPayload {
        val notes      = noteRepo.observeActiveNotes().first() +
                         noteRepo.observeArchivedNotes().first()
        val vaultNotes = vaultRepo.observeAll().first()

        return BackupPayload(
            notes = notes.map { n ->
                NoteBackup(n.id, n.title, n.content, n.colorHex,
                    n.isArchived, n.isPinned, n.createdAt, n.updatedAt)
            },
            vaultNotes = vaultNotes.map { v ->
                VaultNoteBackup(
                    id                  = v.id,
                    encryptedTitleB64   = v.encryptedTitle.b64(),
                    titleIvB64          = v.titleIv.b64(),
                    encryptedContentB64 = v.encryptedContent.b64(),
                    contentIvB64        = v.contentIv.b64(),
                    colorHex            = v.colorHex,
                    createdAt           = v.createdAt,
                    updatedAt           = v.updatedAt
                )
            }
        )
    }

    private suspend fun applyPayload(payload: BackupPayload) {
        val notes = payload.notes.map { n ->
            com.afternote.app.data.model.Note(
                title      = n.title,
                content    = n.content,
                colorHex   = n.colorHex,
                isArchived = n.isArchived,
                isPinned   = n.isPinned,
                createdAt  = n.createdAt,
                updatedAt  = n.updatedAt
            )
        }
        noteRepo.insertAll(notes)

        val vault = payload.vaultNotes.map { v ->
            com.afternote.app.data.model.VaultNote(
                encryptedTitle   = v.encryptedTitleB64.unB64(),
                titleIv          = v.titleIvB64.unB64(),
                encryptedContent = v.encryptedContentB64.unB64(),
                contentIv        = v.contentIvB64.unB64(),
                colorHex         = v.colorHex,
                createdAt        = v.createdAt,
                updatedAt        = v.updatedAt
            )
        }
        vaultRepo.insertAll(vault)
    }

    private fun InputStream.readExact(n: Int): ByteArray {
        val buf  = ByteArray(n)
        var read = 0
        while (read < n) {
            val r = read(buf, read, n - read)
            if (r < 0) throw IllegalStateException("Unexpected end of stream after $read/$n bytes")
            read += r
        }
        return buf
    }

    private fun ByteArray.b64(): String  = Base64.encodeToString(this, Base64.NO_WRAP)
    private fun String.unB64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)
}

data class ImportStats(val notesRestored: Int, val vaultNotesRestored: Int)
