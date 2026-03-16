package com.afternote.app.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AES-256-GCM — hardened version
 *
 * Changes vs previous version:
 *  ① decryptVault() has ZERO fallback — Keystore failure = hard exception
 *  ② Every encrypt() generates a fresh SecureRandom IV (reuse impossible)
 *  ③ encryptWithKey / decryptWithKey accept caller-supplied AAD bytes
 *     so BackupManager can bind the ciphertext to app identity
 */
@Singleton
class CryptoManager @Inject constructor() {

    companion object {
        private const val ANDROID_KEYSTORE  = "AndroidKeyStore"
        private const val VAULT_KEY_ALIAS   = "afternote_vault_key_v2"
        private const val AES_GCM           = "AES/GCM/NoPadding"
        private const val GCM_IV_BYTES      = 12   // 96-bit IV  (NIST recommended)
        private const val GCM_TAG_BITS      = 128  // 128-bit auth tag
        private const val AES_KEY_BITS      = 256
    }

    // ── Android Keystore vault key ────────────────────────────────────────────

    private fun getOrCreateVaultKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).also { it.load(null) }
        (ks.getEntry(VAULT_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let {
            return it.secretKey
        }
        val spec = KeyGenParameterSpec.Builder(
            VAULT_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(AES_KEY_BITS)
            .setUserAuthenticationRequired(false)
            .setInvalidatedByBiometricEnrollment(true)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            .apply { init(spec) }
            .generateKey()
    }

    /** Returns (ciphertext, iv). Throws on any failure — no silent fallback. */
    fun encryptVault(plaintext: ByteArray): Pair<ByteArray, ByteArray> {
        val iv = freshIv()
        val cipher = Cipher.getInstance(AES_GCM).apply {
            init(Cipher.ENCRYPT_MODE, getOrCreateVaultKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        }
        return cipher.doFinal(plaintext) to iv
    }

    /**
     * ① NO fallback — if Keystore fails the call throws.
     * ② GCM tag mismatch (tamper) → AEADBadTagException is propagated to caller.
     */
    fun decryptVault(ciphertext: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_GCM).apply {
            init(Cipher.DECRYPT_MODE, getOrCreateVaultKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        }
        // AEADBadTagException propagates — no catch, no fallback
        return cipher.doFinal(ciphertext)
    }

    // ── Backup key (user-supplied Recovery Key) ───────────────────────────────

    /**
     * 64 hex chars grouped in 8×8, e.g.:
     *   A1B2C3D4-E5F6A7B8-C9D0E1F2-A3B4C5D6-E7F8A9B0-C1D2E3F4-A5B6C7D8-E9F0A1B2
     */
    fun generateRecoveryKey(): String {
        val raw = ByteArray(32).also { SecureRandom().nextBytes(it) }
        return raw.toHex().chunked(8).joinToString("-").uppercase()
    }

    fun recoveryKeyToBytes(key: String): ByteArray {
        val hex = key.replace("-", "").replace(" ", "").uppercase()
        require(hex.length == 64) { "Recovery key must be 64 hex chars (got ${hex.length})" }
        return hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    /**
     * @param aad  Additional Authenticated Data — ties ciphertext to context.
     *             Decryption will FAIL if aad doesn't match (tamper detection).
     */
    fun encryptWithKey(
        plaintext: ByteArray,
        keyBytes: ByteArray,
        aad: ByteArray
    ): Pair<ByteArray, ByteArray> {
        val iv     = freshIv()
        val secret = SecretKeySpec(keyBytes, "AES")
        val cipher = Cipher.getInstance(AES_GCM).apply {
            init(Cipher.ENCRYPT_MODE, secret, GCMParameterSpec(GCM_TAG_BITS, iv))
            updateAAD(aad)           // ← AAD bound to ciphertext via GCM tag
        }
        return cipher.doFinal(plaintext) to iv
    }

    fun decryptWithKey(
        ciphertext: ByteArray,
        iv: ByteArray,
        keyBytes: ByteArray,
        aad: ByteArray
    ): ByteArray {
        val secret = SecretKeySpec(keyBytes, "AES")
        val cipher = Cipher.getInstance(AES_GCM).apply {
            init(Cipher.DECRYPT_MODE, secret, GCMParameterSpec(GCM_TAG_BITS, iv))
            updateAAD(aad)           // ← must match what was used during encrypt
        }
        return cipher.doFinal(ciphertext)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun freshIv(): ByteArray =
        ByteArray(GCM_IV_BYTES).also { SecureRandom.getInstanceStrong().nextBytes(it) }

    private fun ByteArray.toHex(): String =
        joinToString("") { "%02x".format(it) }
}
