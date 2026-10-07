package com.musiqay.app.data

import android.content.Context
import android.net.Uri
import com.musiqay.app.MusiqayApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class BackupPreview(val document: JSONObject, val favorites: Int, val playlists: Int, val bookmarks: Int)

class BackupRepository(private val app: MusiqayApplication) {
    private val dao get() = app.database.musicDao()
    suspend fun export(uri: Uri) = withContext(Dispatchers.IO) {
        val result = JSONObject().apply {
            put("format", "musiqay-backup"); put("version", 1); put("createdAt", System.currentTimeMillis())
            put("favorites", JSONArray(dao.favoriteSnapshot().map { JSONObject().apply { put("mediaId", it.mediaId); put("addedAt", it.addedAt) } }))
            put("playlists", JSONArray(dao.playlistSnapshot().map { JSONObject().apply { put("id", it.id); put("name", it.name); put("createdAt", it.createdAt) } }))
            put("tracks", JSONArray(dao.trackSnapshot().map { JSONObject().apply { put("playlistId", it.playlistId); put("mediaId", it.mediaId); put("position", it.position) } }))
            put("settings", app.settingsRepository.exportBackup())
            put("listening", ListeningRepository.get(app).export())
            put("manualStations", app.radioRepository.exportManual())
        }
        val output = app.contentResolver.openOutputStream(uri, "wt") ?: error("Cannot open backup")
        output.bufferedWriter(Charsets.UTF_8).use { it.write(result.toString(2)) }
    }
    suspend fun preview(uri: Uri): BackupPreview = withContext(Dispatchers.IO) {
        val input = app.contentResolver.openInputStream(uri) ?: error("Cannot open backup")
        val bytes = input.use { it.readBytesLimited(5 * 1024 * 1024) }
        val document = JSONObject(bytes.toString(Charsets.UTF_8))
        require(document.optString("format") == "musiqay-backup" && document.optInt("version") == 1)
        listOf("favorites", "playlists", "tracks").forEach { require((document.optJSONArray(it)?.length() ?: 0) <= 20_000) }
        BackupPreview(document, document.optJSONArray("favorites")?.length() ?: 0,
            document.optJSONArray("playlists")?.length() ?: 0, document.optJSONObject("listening")?.optJSONArray("bookmarks")?.length() ?: 0)
    }
    suspend fun restore(preview: BackupPreview) = withContext(Dispatchers.IO) {
        val document = preview.document
        fun rows(key: String): List<JSONObject> {
            val array = document.optJSONArray(key) ?: return emptyList()
            return (0 until array.length()).mapNotNull { array.optJSONObject(it) }
        }
        val favorites = rows("favorites").mapNotNull { row ->
            row.optLong("mediaId", -1).takeIf { it >= 0 }?.let { FavoriteEntity(it, row.optLong("addedAt", 0)) }
        }
        val lists = rows("playlists").mapNotNull { row ->
            val id = row.optLong("id", -1); val name = row.optString("name").trim().take(100)
            if (id < 0 || name.isBlank()) null else PlaylistEntity(id, name, row.optLong("createdAt", 0))
        }.distinctBy { it.id }
        val tracks = rows("tracks").mapNotNull { row ->
            val playlist = row.optLong("playlistId", -1); val media = row.optLong("mediaId", -1)
            if (playlist < 0 || media < 0) null else PlaylistTrackEntity(playlist, media, row.optInt("position", 0).coerceAtLeast(0))
        }
        // Merge, never clear: a failed or repeated import cannot erase existing playlists.
        dao.mergeBackup(favorites, lists, tracks)
        document.optJSONObject("settings")?.let { app.settingsRepository.mergeBackup(it) }
        document.optJSONObject("listening")?.let { ListeningRepository.get(app).merge(it) }
        document.optJSONArray("manualStations")?.let { app.radioRepository.mergeManual(it) }
    }
    private fun java.io.InputStream.readBytesLimited(limit: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val n = read(buffer); if (n < 0) break
            require(out.size() + n <= limit)
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }
}
