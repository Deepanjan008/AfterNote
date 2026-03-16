package com.afternote.app.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "afternote_prefs")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val KEY_DARK_MODE         = booleanPreferencesKey("dark_mode")
        val KEY_APP_LOCK_ENABLED  = booleanPreferencesKey("app_lock")
        val KEY_THEME_INDEX       = intPreferencesKey("theme_index")
        val KEY_RECOVERY_KEY_HASH = stringPreferencesKey("recovery_key_hash")
        val KEY_RECOVERY_KEY_SET  = booleanPreferencesKey("recovery_key_set")
        val KEY_FAILED_ATTEMPTS   = intPreferencesKey("failed_attempts")
        val KEY_VAULT_UNLOCKED_ONCE = booleanPreferencesKey("vault_unlocked")
    }

    val preferences: Flow<Preferences> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

    val isDarkMode: Flow<Boolean>        = preferences.map { it[KEY_DARK_MODE] ?: false }
    val isAppLockEnabled: Flow<Boolean>  = preferences.map { it[KEY_APP_LOCK_ENABLED] ?: false }
    val themeIndex: Flow<Int>            = preferences.map { it[KEY_THEME_INDEX] ?: 0 }
    val isRecoveryKeySet: Flow<Boolean>  = preferences.map { it[KEY_RECOVERY_KEY_SET] ?: false }
    val failedAttempts: Flow<Int>        = preferences.map { it[KEY_FAILED_ATTEMPTS] ?: 0 }

    suspend fun setDarkMode(enabled: Boolean)       = set(KEY_DARK_MODE, enabled)
    suspend fun setAppLock(enabled: Boolean)        = set(KEY_APP_LOCK_ENABLED, enabled)
    suspend fun setThemeIndex(index: Int)           = set(KEY_THEME_INDEX, index)
    suspend fun setRecoveryKeyHash(hash: String)    = set(KEY_RECOVERY_KEY_HASH, hash)
    suspend fun setRecoveryKeySet(set: Boolean)     = this.set(KEY_RECOVERY_KEY_SET, set)
    suspend fun setFailedAttempts(count: Int)       = set(KEY_FAILED_ATTEMPTS, count)

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }

    private suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { it[key] = value }
    }
}
