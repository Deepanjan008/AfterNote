package com.afternote.app.data.repository

import com.afternote.app.data.db.NoteDao
import com.afternote.app.data.model.Note
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(private val dao: NoteDao) {
    fun observeActiveNotes(): Flow<List<Note>>       = dao.observeActiveNotes()
    fun observeArchivedNotes(): Flow<List<Note>>     = dao.observeArchivedNotes()
    fun searchNotes(query: String): Flow<List<Note>> = dao.searchNotes(query)
    suspend fun getNoteById(id: Long): Note?         = dao.getNoteById(id)
    suspend fun saveNote(note: Note): Long            = dao.upsert(note)
    suspend fun deleteNote(note: Note)               = dao.delete(note)
    suspend fun deleteAll()                          = dao.deleteAll()
    suspend fun setArchived(id: Long, archived: Boolean) = dao.setArchived(id, archived)
    suspend fun setPinned(id: Long, pinned: Boolean)     = dao.setPinned(id, pinned)
    suspend fun insertAll(notes: List<Note>)             = dao.insertAll(notes)
}
