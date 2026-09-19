package com.musiqay.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val mediaId: Long,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "mediaId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playlistId"), Index("mediaId")]
)
data class PlaylistTrackEntity(
    val playlistId: Long,
    val mediaId: Long,
    val position: Int
)

@Dao
interface MusicDao {
    @Query("SELECT mediaId FROM favorites ORDER BY addedAt DESC")
    fun observeFavoriteIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(item: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE mediaId = :mediaId")
    suspend fun removeFavorite(mediaId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE mediaId = :mediaId)")
    suspend fun isFavorite(mediaId: Long): Boolean

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Insert
    suspend fun createPlaylist(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("SELECT mediaId FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY position ASC")
    fun observePlaylistTrackIds(playlistId: Long): Flow<List<Long>>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun nextPosition(playlistId: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistTrack(item: PlaylistTrackEntity)

    @Transaction
    suspend fun addTrackToPlaylist(playlistId: Long, mediaId: Long) {
        insertPlaylistTrack(PlaylistTrackEntity(playlistId, mediaId, nextPosition(playlistId)))
    }

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND mediaId = :mediaId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, mediaId: Long)
}

@Database(
    entities = [FavoriteEntity::class, PlaylistEntity::class, PlaylistTrackEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MusiqayDatabase : RoomDatabase() {
    abstract fun musicDao(): MusicDao

    companion object {
        @Volatile private var INSTANCE: MusiqayDatabase? = null

        fun get(context: Context): MusiqayDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MusiqayDatabase::class.java,
                    "musiqay.db"
                ).build().also { INSTANCE = it }
            }
    }
}
