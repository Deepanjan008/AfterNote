package com.afternote.app.data.repository

import com.afternote.app.data.db.VaultNoteDao
import com.afternote.app.data.model.VaultNote
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultRepository @Inject constructor(private val dao: VaultNoteDao) {
    fun observeAll(): Flow<List<VaultNote>>        = dao.observeAll()
    suspend fun getById(id: Long): VaultNote?      = dao.getById(id)
    suspend fun save(note: VaultNote): Long        = dao.upsert(note)
    suspend fun delete(note: VaultNote)            = dao.delete(note)
    suspend fun deleteAll()                        = dao.deleteAll()
    suspend fun insertAll(notes: List<VaultNote>)  = dao.insertAll(notes)
}
