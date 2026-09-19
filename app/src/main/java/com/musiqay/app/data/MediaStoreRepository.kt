package com.musiqay.app.data

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreRepository(private val context: Context) {

    suspend fun loadSongs(): List<Song> = withContext(Dispatchers.IO) {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.ALBUM_ID)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.DATE_ADDED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(MediaStore.Audio.Media.RELATIVE_PATH)
        }.toTypedArray()

        val songs = mutableListOf<Song>()
        try {
            context.contentResolver.query(
                collection,
                projection,
                "${MediaStore.Audio.Media.DURATION} >= ?",
                arrayOf("1000"),
                "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val pathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH) else -1

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val albumId = cursor.getLong(albumIdCol)
                    val relativePath = if (pathCol >= 0) cursor.getString(pathCol).orEmpty() else ""
                    val folder = relativePath.trimEnd('/')
                        .substringAfterLast('/', missingDelimiterValue = relativePath.trimEnd('/'))
                        .ifBlank { "الموسيقى" }

                    songs += Song(
                        id = id,
                        title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() } ?: "بدون عنوان",
                        artist = cursor.getString(artistCol)?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "فنان غير معروف",
                        album = cursor.getString(albumCol)?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "ألبوم غير معروف",
                        albumId = albumId,
                        durationMs = cursor.getLong(durationCol),
                        dateAddedSeconds = cursor.getLong(dateAddedCol),
                        uri = ContentUris.withAppendedId(collection, id),
                        artworkUri = albumId.takeIf { it > 0 }?.let {
                            ContentUris.withAppendedId(android.net.Uri.parse("content://media/external/audio/albumart"), it)
                        },
                        folder = folder
                    )
                }
            }
        } catch (_: SecurityException) {
            return@withContext emptyList()
        }
        songs
    }
}
