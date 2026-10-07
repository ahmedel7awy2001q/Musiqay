package com.musiqay.app.data

import android.content.Context
import com.musiqay.app.util.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

data class RadioCatalog(val stations: List<RadioStation> = EgyptianRadio.stations,
    val loading: Boolean = false, val error: String? = null)

class RadioRepository(private val context: Context) {
    private val preferences = context.getSharedPreferences("radio_catalog", Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private var cacheLoaded = false
    private var lastAttemptAt = 0L
    private val _catalog = MutableStateFlow(RadioCatalog())
    val catalog = _catalog.asStateFlow()
    private val _customStations = MutableStateFlow(readManual())
    val customStations = _customStations.asStateFlow()

    @Synchronized fun addManual(name: String, stream: String): RadioStation? {
        val cleanName = cleanRadioStationName(name)
        val url = safeRadioUrl(stream) ?: return null
        if (cleanName.isBlank() || _customStations.value.size >= 100) return null
        (_customStations.value + _catalog.value.stations).firstOrNull {
            canonicalRadioUrl(it.streamUrl) == canonicalRadioUrl(url)
        }?.let { return it }
        val category = radioCategory(cleanName, "")
        val station = RadioStation("manual-${java.util.UUID.randomUUID()}", cleanName, category,
            cleanName.take(1), url, "", if (url.substringBefore('?').endsWith(".m3u8", true)) "application/x-mpegURL" else null,
            category = category)
        _customStations.value = _customStations.value + station
        saveManual()
        return station
    }
    @Synchronized fun removeManual(id: String) {
        _customStations.value = _customStations.value.filterNot { it.id == id }
        saveManual()
    }
    fun exportManual(): JSONArray = JSONArray(_customStations.value.map { station ->
        JSONObject().apply { put("id", station.id); put("name", station.name); put("url", station.streamUrl) }
    })
    @Synchronized fun mergeManual(entries: JSONArray) {
        val current = _customStations.value.toMutableList()
        val ids = current.mapTo(HashSet()) { it.id }
        val urls = current.mapTo(HashSet()) { canonicalRadioUrl(it.streamUrl) }
        for (i in 0 until entries.length().coerceAtMost(100)) {
            val row = entries.optJSONObject(i) ?: continue
            val id = row.optString("id").takeIf { it.matches(Regex("manual-[a-zA-Z0-9-]{1,70}")) } ?: continue
            val url = safeRadioUrl(row.optString("url")) ?: continue
            val name = cleanRadioStationName(row.optString("name")).takeIf { it.isNotBlank() } ?: continue
            if (id in ids || canonicalRadioUrl(url) in urls || current.size >= 100) continue
            val category = radioCategory(name, "")
            current.add(RadioStation(id, name, category, name.take(1), url, "", mimeType =
                if (url.substringBefore('?').endsWith(".m3u8", true)) "application/x-mpegURL" else null, category = category))
            ids.add(id); urls.add(canonicalRadioUrl(url))
        }
        _customStations.value = current
        saveManual()
    }
    private fun saveManual() { preferences.edit().putString("manual", exportManual().toString()).apply() }
    private fun readManual(): List<RadioStation> = runCatching {
        val entries = JSONArray(preferences.getString("manual", "[]"))
        buildList {
            for (i in 0 until entries.length().coerceAtMost(100)) {
                val row = entries.optJSONObject(i) ?: continue
                val id = row.optString("id").takeIf { it.matches(Regex("manual-[a-zA-Z0-9-]{1,70}")) } ?: continue
                val url = safeRadioUrl(row.optString("url")) ?: continue
                val name = cleanRadioStationName(row.optString("name")).takeIf { it.isNotBlank() } ?: continue
                val category = radioCategory(name, "")
                add(RadioStation(id, name, category, name.take(1), url, "", mimeType =
                    if (url.substringBefore('?').endsWith(".m3u8", true)) "application/x-mpegURL" else null, category = category))
            }
        }
    }.getOrDefault(emptyList())

    /** Fetch on entering Radio; never on local playback or each search keystroke. */
    suspend fun refresh(force: Boolean, favorites: Set<String>, lastId: String?) = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                if (!cacheLoaded) {
                    val cached = preferences.getString("stations", null)?.let {
                        runCatching { parseStations(it) }.getOrNull()?.takeIf { entries -> entries.isNotEmpty() }
                    }
                    val seed = cached ?: runCatching {
                        context.assets.open("egypt-radio.json").bufferedReader().use { parseStations(it.readText()) }
                    }.getOrDefault(emptyList())
                    _catalog.value = RadioCatalog(mergeStations(seed).distinctBy { it.id })
                    cacheLoaded = true
                }

                val now = System.currentTimeMillis()
                if (!force && (now - preferences.getLong("updated_at", 0) in 0..86_400_000 ||
                    now - lastAttemptAt in 0..60_000)) return@withLock

                lastAttemptAt = now
                _catalog.value = _catalog.value.copy(loading = true, error = null)

                val discovered = runCatching {
                    InetAddress.getAllByName("all.api.radio-browser.info").map { it.canonicalHostName }
                        .filter { it.endsWith(".api.radio-browser.info") }
                }.getOrDefault(emptyList())
                val servers = (discovered + "de1.api.radio-browser.info").distinct().shuffled().take(3)
                var payload: String? = null
                var failure: Exception? = null
                for (server in servers) {
                    currentCoroutineContext().ensureActive()
                    try {
                        val response = request("https://$server/json/stations/bycountrycodeexact/EG?hidebroken=true&order=clickcount&reverse=true&limit=1000")
                        require(parseStations(response).isNotEmpty()) { "Empty directory" }
                        payload = response
                        break
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        failure = error
                    }
                }

                val text = payload ?: throw (failure ?: IllegalStateException("No radio servers"))
                val fetched = mergeStations(parseStations(text), includeRetained = false).distinctBy { it.id }
                val retained = _catalog.value.stations.filter {
                    (it.id in favorites || it.id == lastId) && fetched.none { fresh -> fresh.id == it.id }
                }
                _catalog.value = RadioCatalog((fetched + retained).distinctBy { it.id })
                preferences.edit().putString("stations", text).putLong("updated_at", now).apply()
                saveRetainedStations(retained)
            } catch (cancelled: CancellationException) {
                _catalog.value = _catalog.value.copy(loading = false)
                throw cancelled
            } catch (_: Exception) {
                cacheLoaded = true
                _catalog.value = _catalog.value.copy(
                    stations = _catalog.value.stations.ifEmpty { EgyptianRadio.stations },
                    loading = false,
                    error = "تعذر تحميل دليل المحطات. المحطات الأساسية ما زالت متاحة"
                )
            }
        }
    }
    private fun parseStations(text: String): List<RadioStation> {
        val entries = JSONArray(text)
        return buildList {
            for (i in 0 until entries.length().coerceAtMost(1000)) {
                val row = entries.optJSONObject(i) ?: continue
                if (row.optString("countrycode") != "EG" || row.optInt("lastcheckok") != 1) continue
                val id = row.optString("stationuuid")
                val name = cleanRadioStationName(row.optString("name"))
                if (!id.matches(Regex("[a-zA-Z0-9-]{1,80}")) || name.isBlank()) continue
                val url = preferredRadioStream(row.optString("url"), row.optString("url_resolved")) ?: continue
                val tags = row.optString("tags").take(500)
                val category = radioCategory(name, tags)
                add(RadioStation(id, name, category, name.take(4), url,
                    safeRadioUrl(row.optString("homepage")).orEmpty(),
                    mimeType = if (row.optInt("hls") == 1 || url.substringBefore('?').endsWith(".m3u8")) "application/x-mpegURL" else null,
                    artworkUrl = safeRadioUrl(row.optString("favicon")), category = category, tags = tags, directoryId = id))
            }
        }
    }
    private fun mergeStations(incoming: List<RadioStation>, includeRetained: Boolean = true): List<RadioStation> {
        val result = EgyptianRadio.stations.toMutableList()
        val seenUrls = result.mapTo(HashSet()) { canonicalRadioUrl(it.streamUrl) }
        val seenIds = result.mapTo(HashSet()) { it.id }
        for (station in incoming) {
            if (curatedRadioId(station.name, station.streamUrl) != null || station.id in seenIds) continue
            if (seenUrls.add(canonicalRadioUrl(station.streamUrl))) { result.add(station); seenIds.add(station.id) }
        }
        val retained = if (!includeRetained) emptyList() else runCatching {
            val entries = JSONArray(preferences.getString("retained", "[]"))
            buildList {
                for (i in 0 until entries.length()) {
                    val row = entries.getJSONObject(i)
                    val url = safeRadioUrl(row.optString("url")) ?: continue
                    add(RadioStation(row.getString("id"), row.getString("name"), row.optString("category"), "", url,
                        row.optString("website"), row.optString("mime").takeIf { it.isNotBlank() },
                        safeRadioUrl(row.optString("artwork")), row.optString("category")))
                }
            }
        }.getOrDefault(emptyList())
        for (station in retained) if (station.id !in seenIds && seenUrls.add(canonicalRadioUrl(station.streamUrl))) {
            result.add(station); seenIds.add(station.id)
        }
        return result
    }
    private fun saveRetainedStations(stations: List<RadioStation>) {
        val entries = JSONArray()
        stations.forEach { station -> entries.put(JSONObject().apply {
            put("id", station.id); put("name", station.name); put("url", station.streamUrl)
            put("website", station.website); put("mime", station.mimeType.orEmpty())
            put("artwork", station.artworkUrl.orEmpty()); put("category", station.category)
        }) }
        preferences.edit().putString("retained", entries.toString()).apply()
    }
    private fun request(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 8_000; connection.readTimeout = 10_000
        connection.setRequestProperty("User-Agent", "Musiqay/1.4 (Android)")
        connection.setRequestProperty("Accept", "application/json")
        try {
            require(connection.responseCode == 200) { "Radio directory unavailable" }
            val output = java.io.ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= 2 * 1024 * 1024) { "Radio directory too large" }
                    output.write(buffer, 0, count)
                }
            }
            return output.toString("UTF-8")
        } finally { connection.disconnect() }
    }
}
