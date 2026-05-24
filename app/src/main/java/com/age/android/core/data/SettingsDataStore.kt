/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package com.age.android.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
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

enum class ThemeMode {
    SYSTEM,
    DARK,
    LIGHT;

    companion object {
        fun fromStoredName(value: String?): ThemeMode =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

enum class ThemeAccent(
    val label: String,
    val description: String,
    val liquidGlass: Boolean
) {
    LIQUID_DEFAULT("默认", "Liquid Glass 绿青", true),
    LIQUID_AURORA("极光", "Liquid Glass 蓝紫", true),
    LIQUID_SUNRISE("晨光", "Liquid Glass 橙粉", true),
    LIQUID_OCEAN("海洋", "Liquid Glass 蓝绿", true),
    LIQUID_GRAPE("葡萄", "Liquid Glass 紫红", true),
    SOLID_GREEN("绿色", "纯色外观: 白/黑底，绿色强调色", false),
    SOLID_BLUE("蓝色", "纯色外观: 白/黑底，蓝色强调色", false),
    SOLID_RED("红色", "纯色外观: 白/黑底，红色强调色", false),
    SOLID_PURPLE("紫色", "纯色外观: 白/黑底，紫色强调色", false),
    SOLID_ORANGE("橙色", "纯色外观: 白/黑底，橙色强调色", false);

    companion object {
        fun fromStoredName(value: String?): ThemeAccent =
            entries.firstOrNull { it.name == value } ?: LIQUID_DEFAULT
    }
}

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
    private val compressEnabledKey = booleanPreferencesKey("compress_enabled")
    private val concurrencyKey = intPreferencesKey("concurrency")
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val themeAccentKey = stringPreferencesKey("theme_accent")
    private val glassEffectEnabledKey = booleanPreferencesKey("glass_effect_enabled")

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

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        ThemeMode.fromStoredName(preferences[themeModeKey])
    }

    val themeAccent: Flow<ThemeAccent> = context.dataStore.data.map { preferences ->
        ThemeAccent.fromStoredName(preferences[themeAccentKey])
    }

    val glassEffectEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[glassEffectEnabledKey] != false
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

    suspend fun getCompressEnabledOnce(): Boolean {
        return context.dataStore.data.map { preferences ->
            preferences[compressEnabledKey] != false
        }.first()
    }

    val compressEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[compressEnabledKey] != false
    }

    suspend fun setCompressEnabled(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[compressEnabledKey] = value
        }
    }

    val concurrency: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[concurrencyKey] ?: 4
    }

    suspend fun getConcurrencyOnce(): Int {
        return context.dataStore.data.map { preferences ->
            preferences[concurrencyKey] ?: 4
        }.first()
    }

    suspend fun setConcurrency(value: Int) {
        context.dataStore.edit { preferences ->
            preferences[concurrencyKey] = value
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[themeModeKey] = mode.name
        }
    }

    suspend fun setThemeAccent(accent: ThemeAccent) {
        context.dataStore.edit { preferences ->
            preferences[themeAccentKey] = accent.name
        }
    }

    suspend fun setGlassEffectEnabled(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[glassEffectEnabledKey] = value
        }
    }
}
