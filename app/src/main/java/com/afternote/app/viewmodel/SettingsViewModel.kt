package com.afternote.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.app.data.repository.NoteRepository
import com.afternote.app.data.repository.VaultRepository
import com.afternote.app.security.CryptoManager
import com.afternote.app.security.SettingsDataStore
import com.afternote.app.security.TamperDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: SettingsDataStore,
    private val noteRepo: NoteRepository,
    private val vaultRepo: VaultRepository,
    private val crypto: CryptoManager,
    private val tamper: TamperDetector
) : ViewModel() {

    val isDarkMode: StateFlow<Boolean>       = dataStore.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val isAppLockEnabled: StateFlow<Boolean> = dataStore.isAppLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val themeIndex: StateFlow<Int>           = dataStore.themeIndex
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val isRecoveryKeySet: StateFlow<Boolean> = dataStore.isRecoveryKeySet
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _event = MutableSharedFlow<SettingsEvent>()
    val event: SharedFlow<SettingsEvent> = _event.asSharedFlow()

    fun toggleDarkMode()     = viewModelScope.launch { dataStore.setDarkMode(!isDarkMode.value) }
    fun toggleAppLock()      = viewModelScope.launch { dataStore.setAppLock(!isAppLockEnabled.value) }
    fun setTheme(index: Int) = viewModelScope.launch { dataStore.setThemeIndex(index) }

    fun generateAndStoreRecoveryKey() = viewModelScope.launch {
        val key = crypto.generateRecoveryKey()
        dataStore.setRecoveryKeySet(true)
        _event.emit(SettingsEvent.ShowRecoveryKey(key))
    }

    /** Wipes all notes, vault, prefs, and resets tamper counter. */
    fun selfDestruct() = viewModelScope.launch {
        noteRepo.deleteAll()
        vaultRepo.deleteAll()
        dataStore.clear()
        tamper.resetFailedAttempts()
        _event.emit(SettingsEvent.SelfDestructComplete)
    }
}

sealed class SettingsEvent {
    data class ShowRecoveryKey(val key: String) : SettingsEvent()
    object SelfDestructComplete                  : SettingsEvent()
}
