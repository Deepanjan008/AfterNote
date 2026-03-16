package com.afternote.app.data.model

data class NoteBackup(
    val id: Long,
    val title: String,
    val content: String,
    val colorHex: String,
    val isArchived: Boolean,
    val isPinned: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

data class BackupPayload(
    val schemaVersion: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val notes: List<NoteBackup>,
    val vaultNotes: List<VaultNoteBackup>
)

data class VaultNoteBackup(
    val id: Long,
    val encryptedTitleB64: String,
    val titleIvB64: String,
    val encryptedContentB64: String,
    val contentIvB64: String,
    val colorHex: String,
    val createdAt: Long,
    val updatedAt: Long
)
