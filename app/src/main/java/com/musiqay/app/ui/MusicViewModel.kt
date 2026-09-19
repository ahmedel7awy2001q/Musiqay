package com.musiqay.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.musiqay.app.MusiqayApplication
import com.musiqay.app.data.FavoriteEntity
import com.musiqay.app.data.PlaylistEntity
import com.musiqay.app.data.Song
import com.musiqay.app.data.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MusiqayApplication
    private val dao = app.database.musicDao()

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

    val favoriteIds = dao.observeFavoriteIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playlists = dao.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val settings = app.settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.musiqay.app.data.AppSettings())

    val player = app.playerController

    init {
        refreshLibrary()
    }

    fun refreshLibrary() {
        viewModelScope.launch { _songs.value = app.mediaStoreRepository.loadSongs() }
    }

    fun play(song: Song, source: List<Song> = songs.value) {
        app.playerController.play(song, source.ifEmpty { songs.value })
    }

    fun toggleFavorite(mediaId: Long) {
        viewModelScope.launch {
            if (dao.isFavorite(mediaId)) dao.removeFavorite(mediaId)
            else dao.addFavorite(FavoriteEntity(mediaId))
        }
    }

    fun createPlaylist(name: String, mediaIdToAdd: Long? = null) {
        val clean = name.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            val id = dao.createPlaylist(PlaylistEntity(name = clean))
            if (mediaIdToAdd != null) dao.addTrackToPlaylist(id, mediaIdToAdd)
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch { dao.deletePlaylist(id) }
    }

    fun addToPlaylist(playlistId: Long, mediaId: Long) {
        viewModelScope.launch { dao.addTrackToPlaylist(playlistId, mediaId) }
    }

    fun removeFromPlaylist(playlistId: Long, mediaId: Long) {
        viewModelScope.launch { dao.removeTrackFromPlaylist(playlistId, mediaId) }
    }

    fun playlistTrackIds(id: Long): Flow<List<Long>> = dao.observePlaylistTrackIds(id)

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { app.settingsRepository.setTheme(mode) }
    }

    fun setDynamicColors(enabled: Boolean) {
        viewModelScope.launch { app.settingsRepository.setDynamicColors(enabled) }
    }
}
