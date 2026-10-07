package com.musiqay.app.data

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
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
                        title = cleanMediaTitle(cursor.getString(titleCol)),
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
}
