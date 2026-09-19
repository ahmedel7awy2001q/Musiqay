package com.musiqay.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

enum class ThemeMode { SYSTEM, DARK, LIGHT }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val dynamicColors: Boolean = false,
    val minimumAudioDurationSeconds: Int = 10,
    val includeNonMusicAudio: Boolean = false
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val DYNAMIC = booleanPreferencesKey("dynamic_colors")
        val MIN_AUDIO_DURATION = intPreferencesKey("minimum_audio_duration_seconds")
        val INCLUDE_NON_MUSIC = booleanPreferencesKey("include_non_music_audio")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = runCatching {
                ThemeMode.valueOf(
                    prefs[Keys.THEME] ?: ThemeMode.DARK.name
                )
            }.getOrDefault(ThemeMode.DARK),

            dynamicColors = prefs[Keys.DYNAMIC] ?: false,

            minimumAudioDurationSeconds =
                prefs[Keys.MIN_AUDIO_DURATION] ?: 10,

            includeNonMusicAudio =
                prefs[Keys.INCLUDE_NON_MUSIC] ?: false
        )
    }

    suspend fun setTheme(mode: ThemeMode) {
        context.dataStore.edit {
            it[Keys.THEME] = mode.name
        }
    }

    suspend fun setDynamicColors(enabled: Boolean) {
        context.dataStore.edit {
            it[Keys.DYNAMIC] = enabled
        }
    }

    suspend fun setMinimumAudioDuration(seconds: Int) {
        val safeValue = seconds.coerceIn(0, 3600)

        context.dataStore.edit {
            it[Keys.MIN_AUDIO_DURATION] = safeValue
        }
    }

    suspend fun setIncludeNonMusicAudio(enabled: Boolean) {
        context.dataStore.edit {
            it[Keys.INCLUDE_NON_MUSIC] = enabled
        }
    }
}