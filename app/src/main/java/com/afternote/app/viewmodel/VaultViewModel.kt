package com.afternote.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.app.data.model.NoteColor
import com.afternote.app.data.model.VaultNote
import com.afternote.app.data.repository.VaultRepository
import com.afternote.app.security.CryptoManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DecryptedVaultNote(
    val id: Long,
    val title: String,
    val content: String,
    val colorHex: String,
    val createdAt: Long,
    val updatedAt: Long
)

sealed class VaultUiState {
    object Loading                        : VaultUiState()
    data class Ready(val notes: List<DecryptedVaultNote>) : VaultUiState()
    data class DecryptionError(val msg: String)           : VaultUiState()
}

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val repo: VaultRepository,
    private val crypto: CryptoManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<VaultUiState>(VaultUiState.Loading)
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    // Convenience accessor
    val vaultNotes: StateFlow<List<DecryptedVaultNote>> = uiState.map {
        (it as? VaultUiState.Ready)?.notes ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _currentDecrypted = MutableStateFlow<DecryptedVaultNote?>(null)
    val currentDecrypted: StateFlow<DecryptedVaultNote?> = _currentDecrypted.asStateFlow()

    init {
        viewModelScope.launch {
            repo.observeAll().collect { rawList ->
                // If ANY note fails to decrypt → show error state (no silent skip)
                val decrypted = mutableListOf<DecryptedVaultNote>()
                for (raw in rawList) {
                    val result = runCatching { decrypt(raw) }
                    if (result.isFailure) {
                        _uiState.value = VaultUiState.DecryptionError(
                            "Decryption failed — Keystore key may be unavailable. " +
                            "This can happen after device reset or biometric re-enrollment.")
                        return@collect
                    }
                    decrypted += result.getOrThrow()
                }
                _uiState.value = VaultUiState.Ready(decrypted)
            }
        }
    }

    fun loadNote(id: Long) = viewModelScope.launch {
        _currentDecrypted.value = if (id == -1L) {
            DecryptedVaultNote(-1L, "", "", NoteColor.DEFAULT.hex,
                System.currentTimeMillis(), System.currentTimeMillis())
        } else {
            val raw = repo.getById(id) ?: return@launch
            runCatching { decrypt(raw) }.getOrNull()
        }
    }

    fun saveNote(title: String, content: String, colorHex: String, existingId: Long = -1L) =
        viewModelScope.launch {
            val (encTitle, titleIv)     = crypto.encryptVault(title.toByteArray(Charsets.UTF_8))
            val (encContent, contentIv) = crypto.encryptVault(content.toByteArray(Charsets.UTF_8))
            val note = VaultNote(
                id               = if (existingId == -1L) 0L else existingId,
                encryptedTitle   = encTitle,
                titleIv          = titleIv,
                encryptedContent = encContent,
                contentIv        = contentIv,
                colorHex         = colorHex,
                updatedAt        = System.currentTimeMillis()
            )
            repo.save(note)
        }

    fun deleteNote(id: Long) = viewModelScope.launch {
        repo.getById(id)?.let { repo.delete(it) }
    }

    // Throws — caller must handle (no silent null return)
    private fun decrypt(note: VaultNote): DecryptedVaultNote {
        val title   = crypto.decryptVault(note.encryptedTitle, note.titleIv)
                          .toString(Charsets.UTF_8)
        val content = crypto.decryptVault(note.encryptedContent, note.contentIv)
                          .toString(Charsets.UTF_8)
        return DecryptedVaultNote(note.id, title, content,
            note.colorHex, note.createdAt, note.updatedAt)
    }
}
