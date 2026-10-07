package com.musiqay.app.data

import android.content.Context
import com.musiqay.app.util.resumePosition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.util.UUID

data class ListeningPosition(val mediaId: Long, val positionMs: Long, val durationMs: Long,
    val title: String, val updatedAt: Long, val uri: String)
data class AudioBookmark(val id: String, val mediaId: Long, val positionMs: Long, val label: String,
    val createdAt: Long, val uri: String)

/** A separate additive store leaves the existing Room favorites/playlists schema unchanged. */
class ListeningRepository private constructor(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("listening_history", Context.MODE_PRIVATE)
    private val _positions = MutableStateFlow(readPositions())
    val positions = _positions.asStateFlow()
    private val _bookmarks = MutableStateFlow(readBookmarks())
    val bookmarks = _bookmarks.asStateFlow()

    @Synchronized fun savePosition(mediaId: Long, positionMs: Long, durationMs: Long, title: String, uri: String) {
        if (mediaId < 0 || durationMs <= 0 || uri.isBlank()) return
        val entry = ListeningPosition(mediaId, positionMs.coerceIn(0, durationMs), durationMs,
            title.take(180), System.currentTimeMillis(), uri)
        val previous = _positions.value[mediaId]
        // Repeated paused/metadata events must not flood disk or reorder listening history.
        if (previous != null && previous.positionMs == entry.positionMs && previous.uri == uri && previous.durationMs == durationMs) return
        _positions.value = _positions.value + (mediaId to entry)
        prefs.edit().putString("position_$mediaId", positionJson(entry).toString()).apply()
    }

    fun resumeFor(song: Song): Long = _positions.value[song.id]?.takeIf {
        it.uri == song.uri.toString() && kotlin.math.abs(it.durationMs - song.durationMs) < 2_000
    }?.let { resumePosition(it.positionMs, song.durationMs) } ?: 0L

    @Synchronized fun addBookmark(song: Song, positionMs: Long, label: String = ""): AudioBookmark? {
        if (song.durationMs <= 0 || _bookmarks.value.size >= 2_000) return null
        val position = positionMs.coerceIn(0, song.durationMs)
        _bookmarks.value.firstOrNull { it.mediaId == song.id && it.uri == song.uri.toString() && kotlin.math.abs(it.positionMs - position) < 1_000 }?.let { return it }
        val mark = AudioBookmark(UUID.randomUUID().toString(), song.id, position, label.trim().take(80), System.currentTimeMillis(), song.uri.toString())
        _bookmarks.value = (_bookmarks.value + mark).sortedBy { it.positionMs }
        prefs.edit().putString("bookmark_${mark.id}", bookmarkJson(mark).toString()).apply()
        return mark
    }

    @Synchronized fun removeBookmark(id: String) {
        _bookmarks.value = _bookmarks.value.filterNot { it.id == id }
        prefs.edit().remove("bookmark_$id").apply()
    }

    fun export(): JSONObject = JSONObject().apply {
        put("positions", org.json.JSONArray(_positions.value.values.map(::positionJson)))
        put("bookmarks", org.json.JSONArray(_bookmarks.value.map(::bookmarkJson)))
    }

    @Synchronized fun merge(json: JSONObject) {
        val editor = prefs.edit()
        val positions = _positions.value.toMutableMap()
        val incoming = json.optJSONArray("positions")
        for (i in 0 until (incoming?.length() ?: 0).coerceAtMost(10_000)) {
            val p = incoming?.optJSONObject(i)?.let(::parsePosition) ?: continue
            if (positions[p.mediaId]?.updatedAt?.let { it >= p.updatedAt } == true) continue
            positions[p.mediaId] = p
            editor.putString("position_${p.mediaId}", positionJson(p).toString())
        }
        val marks = _bookmarks.value.associateBy { it.id }.toMutableMap()
        val incomingMarks = json.optJSONArray("bookmarks")
        for (i in 0 until (incomingMarks?.length() ?: 0).coerceAtMost(2_000)) {
            val m = incomingMarks?.optJSONObject(i)?.let(::parseBookmark) ?: continue
            if (m.id in marks || marks.size >= 2_000) continue
            marks[m.id] = m
            editor.putString("bookmark_${m.id}", bookmarkJson(m).toString())
        }
        editor.apply()
        _positions.value = positions
        _bookmarks.value = marks.values.sortedBy { it.positionMs }
    }

    private fun readPositions(): Map<Long, ListeningPosition> = prefs.all.entries.asSequence()
        .filter { it.key.startsWith("position_") }.take(10_000)
        .mapNotNull { runCatching { parsePosition(JSONObject(it.value as String)) }.getOrNull() }
        .associateBy { it.mediaId }
    private fun readBookmarks(): List<AudioBookmark> = prefs.all.entries.asSequence()
        .filter { it.key.startsWith("bookmark_") }.take(2_000)
        .mapNotNull { runCatching { parseBookmark(JSONObject(it.value as String)) }.getOrNull() }
        .sortedBy { it.positionMs }.toList()
    private fun positionJson(p: ListeningPosition) = JSONObject().apply {
        put("mediaId", p.mediaId); put("positionMs", p.positionMs); put("durationMs", p.durationMs)
        put("title", p.title); put("updatedAt", p.updatedAt); put("uri", p.uri)
    }
    private fun bookmarkJson(m: AudioBookmark) = JSONObject().apply {
        put("id", m.id); put("mediaId", m.mediaId); put("positionMs", m.positionMs)
        put("label", m.label); put("createdAt", m.createdAt); put("uri", m.uri)
    }
    private fun parsePosition(j: JSONObject): ListeningPosition? {
        val id = j.optLong("mediaId", -1); val duration = j.optLong("durationMs", 0)
        val uri = j.optString("uri").takeIf { it.startsWith("content://") } ?: return null
        if (id < 0 || duration <= 0) return null
        return ListeningPosition(id, j.optLong("positionMs", 0).coerceIn(0, duration), duration,
            j.optString("title").take(180), j.optLong("updatedAt", 0), uri)
    }
    private fun parseBookmark(j: JSONObject): AudioBookmark? {
        val id = j.optString("id").takeIf { it.matches(Regex("[a-zA-Z0-9-]{1,80}")) } ?: return null
        val mediaId = j.optLong("mediaId", -1)
        val uri = j.optString("uri").takeIf { it.startsWith("content://") } ?: return null
        if (mediaId < 0) return null
        return AudioBookmark(id, mediaId, j.optLong("positionMs", 0).coerceAtLeast(0), j.optString("label").take(80), j.optLong("createdAt", 0), uri)
    }
    companion object {
        @Volatile private var instance: ListeningRepository? = null
        fun get(context: Context): ListeningRepository = instance ?: synchronized(this) {
            instance ?: ListeningRepository(context).also { instance = it }
        }
    }
}
