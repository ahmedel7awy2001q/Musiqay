package com.musiqay.app.data

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val dateAddedSeconds: Long,
    val uri: Uri,
    val artworkUri: Uri?,
    val folder: String,
    val folderKey: String = folder
) {
    fun toMediaItem(): MediaItem {
        val extras = Bundle().apply {
            putLong("duration_ms", durationMs)
            putBoolean("spoken_audio", com.musiqay.app.util.detectedSurah(title) != null)
            putLong("song_id", id)
            putString("folder", folder)
            putString("queue_entry_id", java.util.UUID.randomUUID().toString())
        }
        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setArtworkUri(artworkUri)
                    .setExtras(extras)
                    .build()
            )
            .build()
    }
}
