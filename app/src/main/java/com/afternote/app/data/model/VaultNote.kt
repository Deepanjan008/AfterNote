package com.afternote.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_notes")
data class VaultNote(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val encryptedTitle: ByteArray   = ByteArray(0),
    val titleIv: ByteArray          = ByteArray(0),
    val encryptedContent: ByteArray = ByteArray(0),
    val contentIv: ByteArray        = ByteArray(0),
    val colorHex: String            = NoteColor.DEFAULT.hex,
    val createdAt: Long             = System.currentTimeMillis(),
    val updatedAt: Long             = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VaultNote) return false
        return id == other.id
    }
    override fun hashCode(): Int = id.hashCode()
}
