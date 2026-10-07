package com.musiqay.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.musiqay.app.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupMergeTest {
    @Test fun repeatedImportsPreserveExistingDataAndAppendOnlyNewTracks() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MusiqayDatabase::class.java).build()
        try {
            val dao = db.musicDao()
            dao.addFavorite(FavoriteEntity(11, 100))
            val existingId = dao.createPlaylist(PlaylistEntity(name = "قائمتي", createdAt = 10))
            dao.addTrackToPlaylist(existingId, 11)
            val favorites = listOf(FavoriteEntity(11, 900), FavoriteEntity(22, 200))
            val lists = listOf(PlaylistEntity(99, "قائمتي", 300), PlaylistEntity(100, "تلاوات", 400))
            val tracks = listOf(PlaylistTrackEntity(99, 11, 0), PlaylistTrackEntity(99, 22, 1), PlaylistTrackEntity(100, 33, 0))
            repeat(2) { dao.mergeBackup(favorites, lists, tracks) }
            assertEquals(2, dao.favoriteSnapshot().size)
            assertEquals(100L, dao.favoriteSnapshot().first { it.mediaId == 11L }.addedAt)
            assertEquals(2, dao.playlistSnapshot().size)
            assertEquals(existingId, dao.playlistSnapshot().first { it.name == "قائمتي" }.id)
            assertEquals(listOf(11L, 22L), dao.trackSnapshot().filter { it.playlistId == existingId }.map { it.mediaId })
            assertEquals(3, dao.trackSnapshot().size)
        } finally { db.close() }
    }
}
