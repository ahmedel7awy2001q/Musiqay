package com.musiqay.app.data

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.musiqay.app.util.cleanMediaAlbum
import com.musiqay.app.util.cleanMediaArtist
import com.musiqay.app.util.cleanMediaTitle

class MediaStoreRepository(private val context: Context) {

    suspend fun loadSongs(minimumDurationSeconds: Int = 10, includeNonMusicAudio: Boolean = false): List<Song> = withContext(Dispatchers.IO) {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.DISPLAY_NAME)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.ALBUM_ID)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.DATE_ADDED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(MediaStore.Audio.Media.RELATIVE_PATH)
            else add(MediaStore.Audio.Media.DATA)
        }.toTypedArray()

        val songs = mutableListOf<Song>()
        run {
            val safeMinimumSeconds = minimumDurationSeconds.coerceIn(0, 3600)
            val minimumDurationMs = safeMinimumSeconds * 1000L

            val selection = if (includeNonMusicAudio) {
                "${MediaStore.Audio.Media.DURATION} >= ?"
            } else {
                "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= ?"
            }

            val selectionArgs = arrayOf(minimumDurationMs.toString())

            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val displayNameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val pathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH) else cursor.getColumnIndex(MediaStore.Audio.Media.DATA)

                while (cursor.moveToNext()) {
                    currentCoroutineContext().ensureActive()
                    val id = cursor.getLong(idCol)
                    val albumId = cursor.getLong(albumIdCol)
                    val relativePath = if (pathCol >= 0) cursor.getString(pathCol).orEmpty() else ""
                    val folderPath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) relativePath.trimEnd('/')
                        else relativePath.substringBeforeLast('/', "")
                    val folder = folderPath.trimEnd('/')
                        .substringAfterLast('/', missingDelimiterValue = folderPath.trimEnd('/'))
                        .ifBlank { "الموسيقى" }

                    songs += Song(
                        id = id,
                        title = cursor.getString(displayNameCol).orEmpty().trim()
                            .ifBlank { cleanMediaTitle(cursor.getString(titleCol)) },
                        artist = cleanMediaArtist(cursor.getString(artistCol)),
                        album = cleanMediaAlbum(cursor.getString(albumCol)),
                        albumId = albumId,
                        durationMs = cursor.getLong(durationCol),
                        dateAddedSeconds = cursor.getLong(dateAddedCol),
                        uri = ContentUris.withAppendedId(collection, id),
                        artworkUri = albumId.takeIf { it > 0 }?.let {
                            ContentUris.withAppendedId(android.net.Uri.parse("content://media/external/audio/albumart"), it)
                        },
                        folder = folder,
                        folderKey = folderPath.ifBlank { "الموسيقى" }
                    )
                }
            }
        }
        songs
    }

    /**
     * Build a temporary playable Song for an audio URI opened from a file manager.
     * The visible title deliberately uses the provider's stored file name, not embedded tags.
     */
    suspend fun loadExternalSong(uri: Uri): Song = withContext(Dispatchers.IO) {
        val displayName = runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (column >= 0) cursor.getString(column).orEmpty() else ""
                } else ""
            }.orEmpty()
        }.getOrDefault("").trim().ifBlank {
            uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "ملف صوتي"
        }

        var durationMs = 0L
        var artist: String? = null
        var album: String? = null
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
        } catch (_: Exception) {
            // The URI can still be played even when its provider exposes no readable tags.
        } finally {
            runCatching { retriever.release() }
        }

        val externalId = -(kotlin.math.abs(uri.toString().hashCode().toLong()).coerceAtLeast(1L))
        Song(
            id = externalId,
            title = displayName,
            artist = cleanMediaArtist(artist),
            album = cleanMediaAlbum(album),
            albumId = 0L,
            durationMs = durationMs,
            dateAddedSeconds = System.currentTimeMillis() / 1000L,
            uri = uri,
            artworkUri = null,
            folder = "ملف خارجي",
            folderKey = "external:${uri.authority.orEmpty()}"
        )
    }
}
