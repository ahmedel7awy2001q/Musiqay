package com.musiqay.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.json.JSONArray

private val Context.dataStore by preferencesDataStore(name = "settings")

enum class ThemeMode { SYSTEM, DARK, AMOLED, LIGHT }
enum class StartPage(val route: String, val label: String) { HOME("home", "الرئيسية"), LIBRARY("songs", "المكتبة"), RADIO("radio", "الراديو") }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val dynamicColors: Boolean = false,
    val minimumAudioDurationSeconds: Int = 10,
    val includeNonMusicAudio: Boolean = false,
    val hiddenFolders: Set<String> = emptySet(),
    val reduceMotion: Boolean = false,
    val startPage: StartPage = StartPage.HOME
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val START_PAGE = stringPreferencesKey("start_page")
        val RADIO_ORDER = stringPreferencesKey("radio_favorite_order")
        val RADIO_FAVORITES = stringSetPreferencesKey("radio_favorites")
        val LAST_RADIO = stringPreferencesKey("last_radio_station")
        val THEME = stringPreferencesKey("theme")
        val DYNAMIC = booleanPreferencesKey("dynamic_colors")
        val MIN_AUDIO_DURATION = intPreferencesKey("minimum_audio_duration_seconds")
        val INCLUDE_NON_MUSIC = booleanPreferencesKey("include_non_music_audio")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val HIDDEN_FOLDERS = stringSetPreferencesKey("hidden_folders")
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
                prefs[Keys.INCLUDE_NON_MUSIC] ?: false,

            hiddenFolders = prefs[Keys.HIDDEN_FOLDERS] ?: emptySet(),
            reduceMotion = prefs[Keys.REDUCE_MOTION] ?: false,
            startPage = runCatching { StartPage.valueOf(prefs[Keys.START_PAGE] ?: StartPage.HOME.name) }.getOrDefault(StartPage.HOME)
        )
    }

    val radioFavorites: Flow<Set<String>> = context.dataStore.data.map { it[Keys.RADIO_FAVORITES] ?: emptySet() }
    val radioFavoriteOrder: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val favorites = prefs[Keys.RADIO_FAVORITES] ?: emptySet()
        val saved = prefs[Keys.RADIO_ORDER].orEmpty().split(",").filter { it in favorites }.distinct()
        saved + (favorites - saved.toSet()).sorted()
    }
    suspend fun setStartPage(page: StartPage) { context.dataStore.edit { it[Keys.START_PAGE] = page.name } }
    suspend fun moveRadioFavorite(id: String, delta: Int) {
        context.dataStore.edit { prefs ->
            val favorites = prefs[Keys.RADIO_FAVORITES] ?: emptySet()
            val order = (prefs[Keys.RADIO_ORDER].orEmpty().split(",").filter { it in favorites }.distinct() +
                (favorites - prefs[Keys.RADIO_ORDER].orEmpty().split(",").toSet()).sorted()).toMutableList()
            val from = order.indexOf(id); val to = from + delta
            if (from >= 0 && to in order.indices) { order.add(to, order.removeAt(from)); prefs[Keys.RADIO_ORDER] = order.joinToString(",") }
        }
    }
    val lastRadioStation: Flow<String?> = context.dataStore.data.map { it[Keys.LAST_RADIO] }
    suspend fun toggleRadioFavorite(id: String) {
        if (id.isBlank() || id.length > 80) return
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.RADIO_FAVORITES] ?: emptySet()
            prefs[Keys.RADIO_FAVORITES] = if (id in current) current - id else current + id
            val order = prefs[Keys.RADIO_ORDER].orEmpty().split(",").filter { it.isNotBlank() && it != id }
            prefs[Keys.RADIO_ORDER] = (if (id in current) order else order + id).joinToString(",")
        }
    }
    suspend fun saveLastRadio(id: String) {
        if (id.isBlank() || id.length > 80) return
        context.dataStore.edit { it[Keys.LAST_RADIO] = id }
    }
    suspend fun forgetRadio(id: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.RADIO_FAVORITES] = (prefs[Keys.RADIO_FAVORITES] ?: emptySet()) - id
            prefs[Keys.RADIO_ORDER] = prefs[Keys.RADIO_ORDER].orEmpty().split(",").filter { it.isNotBlank() && it != id }.joinToString(",")
            if (prefs[Keys.LAST_RADIO] == id) prefs.remove(Keys.LAST_RADIO)
        }
    }
    suspend fun setReduceMotion(enabled: Boolean) {
        context.dataStore.edit { it[Keys.REDUCE_MOTION] = enabled }
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

    suspend fun hideFolder(folder: String) {
        val clean = folder.trim()
        if (clean.isBlank()) return
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.HIDDEN_FOLDERS] ?: emptySet()
            prefs[Keys.HIDDEN_FOLDERS] = current + clean
        }
    }

    suspend fun showFolder(folder: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.HIDDEN_FOLDERS] ?: emptySet()
            prefs[Keys.HIDDEN_FOLDERS] = current - folder
        }
    }

    suspend fun clearHiddenFolders() {
        context.dataStore.edit { prefs -> prefs.remove(Keys.HIDDEN_FOLDERS) }
    }

    suspend fun exportBackup(): JSONObject {
        val value = settings.first()
        return JSONObject().apply {
            put("theme", value.themeMode.name); put("dynamicColors", value.dynamicColors)
            put("minimumAudioDurationSeconds", value.minimumAudioDurationSeconds)
            put("includeNonMusicAudio", value.includeNonMusicAudio); put("reduceMotion", value.reduceMotion)
            put("startPage", value.startPage.name); put("hiddenFolders", JSONArray(value.hiddenFolders.toList()))
            put("radioFavorites", JSONArray(radioFavorites.first().toList()))
            put("radioFavoriteOrder", JSONArray(radioFavoriteOrder.first()))
            put("lastRadio", lastRadioStation.first().orEmpty())
        }
    }
    suspend fun mergeBackup(value: JSONObject) {
        fun strings(key: String): Set<String> {
            val array = value.optJSONArray(key) ?: return emptySet()
            return (0 until array.length().coerceAtMost(2_000)).mapNotNull {
                array.optString(it).takeIf { text -> text.isNotBlank() && text.length <= 1_000 }
            }.toSet()
        }
        context.dataStore.edit { prefs ->
            runCatching { ThemeMode.valueOf(value.optString("theme")) }.getOrNull()?.let { prefs[Keys.THEME] = it.name }
            runCatching { StartPage.valueOf(value.optString("startPage")) }.getOrNull()?.let { prefs[Keys.START_PAGE] = it.name }
            if (value.has("dynamicColors")) prefs[Keys.DYNAMIC] = value.optBoolean("dynamicColors")
            if (value.has("reduceMotion")) prefs[Keys.REDUCE_MOTION] = value.optBoolean("reduceMotion")
            if (value.has("includeNonMusicAudio")) prefs[Keys.INCLUDE_NON_MUSIC] = value.optBoolean("includeNonMusicAudio")
            if (value.has("minimumAudioDurationSeconds")) prefs[Keys.MIN_AUDIO_DURATION] = value.optInt("minimumAudioDurationSeconds", 10).coerceIn(0, 3600)
            prefs[Keys.HIDDEN_FOLDERS] = (prefs[Keys.HIDDEN_FOLDERS] ?: emptySet()) + strings("hiddenFolders")
            val favorites = (prefs[Keys.RADIO_FAVORITES] ?: emptySet()) + strings("radioFavorites").filter { it.matches(Regex("[a-zA-Z0-9-]{1,80}")) }
            prefs[Keys.RADIO_FAVORITES] = favorites
            val previous = prefs[Keys.RADIO_ORDER].orEmpty().split(",").filter { it in favorites }
            prefs[Keys.RADIO_ORDER] = (previous + strings("radioFavoriteOrder").filter { it in favorites } + favorites).distinct().joinToString(",")
            val last = value.optString("lastRadio")
            if (prefs[Keys.LAST_RADIO].isNullOrBlank() && last.matches(Regex("[a-zA-Z0-9-]{1,80}"))) prefs[Keys.LAST_RADIO] = last
        }
    }
}
