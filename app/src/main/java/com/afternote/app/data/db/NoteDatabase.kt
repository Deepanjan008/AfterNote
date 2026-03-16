package com.afternote.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.afternote.app.data.model.Note
import com.afternote.app.data.model.VaultNote

@Database(
    entities    = [Note::class, VaultNote::class],
    version     = 1,
    exportSchema = false
)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun vaultNoteDao(): VaultNoteDao
}
