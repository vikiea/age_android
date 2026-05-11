package com.age.android.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "age_settings")

enum class DuplicateStrategy { RENAME, OVERWRITE }

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val outputDirUriKey = stringPreferencesKey("output_dir_uri")
    private val duplicateStrategyKey = stringPreferencesKey("duplicate_strategy")
    private val encryptModeKey = stringPreferencesKey("encrypt_mode")
    private val encryptUsePassphraseKey = stringPreferencesKey("encrypt_use_passphrase")
    private val decryptUsePassphraseKey = stringPreferencesKey("decrypt_use_passphrase")
    private val selectedPublicKeyKey = stringPreferencesKey("selected_public_key")
    private val selectedPrivateKeyKey = stringPreferencesKey("selected_private_key")

    @Volatile
    private var cachedOutputDirUri: String? = null

    val outputDirUri: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[outputDirUriKey].also { cachedOutputDirUri = it }
    }

    fun peekOutputDirUri(): String? = cachedOutputDirUri

    suspend fun getOutputDirUriOnce(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[outputDirUriKey]
        }.first()
    }

    suspend fun getDuplicateStrategyOnce(): DuplicateStrategy {
        return context.dataStore.data.map { preferences ->
            when (preferences[duplicateStrategyKey]) {
                "OVERWRITE" -> DuplicateStrategy.OVERWRITE
                else -> DuplicateStrategy.RENAME
            }
        }.first()
    }

    val duplicateStrategy: Flow<DuplicateStrategy> = context.dataStore.data.map { preferences ->
        when (preferences[duplicateStrategyKey]) {
            "OVERWRITE" -> DuplicateStrategy.OVERWRITE
            else -> DuplicateStrategy.RENAME
        }
    }

    suspend fun setOutputDirUri(uri: String?) {
        context.dataStore.edit { preferences ->
            if (uri == null) {
                preferences.remove(outputDirUriKey)
            } else {
                preferences[outputDirUriKey] = uri
            }
        }
    }

    suspend fun setDuplicateStrategy(strategy: DuplicateStrategy) {
        context.dataStore.edit { preferences ->
            preferences[duplicateStrategyKey] = strategy.name
        }
    }

    suspend fun getEncryptModeOnce(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[encryptModeKey]
        }.first()
    }

    suspend fun setEncryptMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[encryptModeKey] = mode
        }
    }

    suspend fun getEncryptUsePassphraseOnce(): Boolean {
        return context.dataStore.data.map { preferences ->
            preferences[encryptUsePassphraseKey] != "false"
        }.first()
    }

    suspend fun setEncryptUsePassphrase(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[encryptUsePassphraseKey] = value.toString()
        }
    }

    suspend fun getDecryptUsePassphraseOnce(): Boolean {
        return context.dataStore.data.map { preferences ->
            preferences[decryptUsePassphraseKey] != "false"
        }.first()
    }

    suspend fun setDecryptUsePassphrase(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[decryptUsePassphraseKey] = value.toString()
        }
    }

    suspend fun getSelectedPublicKeyOnce(): String {
        return context.dataStore.data.map { preferences ->
            preferences[selectedPublicKeyKey] ?: ""
        }.first()
    }

    suspend fun setSelectedPublicKey(value: String) {
        context.dataStore.edit { preferences ->
            preferences[selectedPublicKeyKey] = value
        }
    }

    suspend fun getSelectedPrivateKeyOnce(): String {
        return context.dataStore.data.map { preferences ->
            preferences[selectedPrivateKeyKey] ?: ""
        }.first()
    }

    suspend fun setSelectedPrivateKey(value: String) {
        context.dataStore.edit { preferences ->
            preferences[selectedPrivateKeyKey] = value
        }
    }
}
