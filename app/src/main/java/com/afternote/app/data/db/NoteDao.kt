package com.afternote.app.data.db

import androidx.room.*
import com.afternote.app.data.model.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes WHERE isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun observeActiveNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isArchived = 1 ORDER BY updatedAt DESC")
    fun observeArchivedNotes(): Flow<List<Note>>

    @Query("""
        SELECT * FROM notes
        WHERE isArchived = 0
          AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%')
        ORDER BY isPinned DESC, updatedAt DESC
    """)
    fun searchNotes(query: String): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: Long): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Query("DELETE FROM notes")
    suspend fun deleteAll()

    @Query("UPDATE notes SET isArchived = :archived, updatedAt = :ts WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean, ts: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isPinned = :pinned, updatedAt = :ts WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean, ts: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<Note>)
}
