package com.afternote.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.app.data.model.Note
import com.afternote.app.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val repo: NoteRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: StateFlow<List<Note>> = _searchQuery
        .debounce(300)
        .flatMapLatest { q ->
            if (q.isBlank()) repo.observeActiveNotes()
            else repo.searchNotes(q)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val archivedNotes: StateFlow<List<Note>> = repo.observeArchivedNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _currentNote = MutableStateFlow<Note?>(null)
    val currentNote: StateFlow<Note?> = _currentNote.asStateFlow()

    fun setSearchQuery(q: String) { _searchQuery.value = q }

    fun loadNote(id: Long) = viewModelScope.launch {
        _currentNote.value = if (id == -1L) Note() else repo.getNoteById(id)
    }

    fun saveNote(note: Note) = viewModelScope.launch { repo.saveNote(note) }

    fun deleteNote(note: Note) = viewModelScope.launch { repo.deleteNote(note) }

    fun archiveNote(note: Note) = viewModelScope.launch {
        repo.setArchived(note.id, !note.isArchived)
    }

    fun pinNote(note: Note) = viewModelScope.launch {
        repo.setPinned(note.id, !note.isPinned)
    }
}
