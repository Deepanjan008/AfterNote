package com.afternote.app.data.db

import androidx.room.*
import com.afternote.app.data.model.VaultNote
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultNoteDao {

    @Query("SELECT * FROM vault_notes ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<VaultNote>>

    @Query("SELECT * FROM vault_notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): VaultNote?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: VaultNote): Long

    @Update
    suspend fun update(note: VaultNote)

    @Delete
    suspend fun delete(note: VaultNote)

    @Query("DELETE FROM vault_notes")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<VaultNote>)
}
