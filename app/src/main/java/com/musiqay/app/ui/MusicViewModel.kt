package com.musiqay.app.ui

import android.app.Application
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.musiqay.app.MusiqayApplication
import com.musiqay.app.data.*
import com.musiqay.app.util.isHiddenFolder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class LibraryStatus(val loading: Boolean = true, val error: String? = null, val scannedAt: Long? = null)
data class PlaylistSummary(val count: Int = 0, val durationMs: Long = 0, val artwork: android.net.Uri? = null)

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MusiqayApplication
    private val dao = app.database.musicDao()
    private val scanMutex = Mutex()
    private var refreshJob: Job? = null
    private var initialSessionRestored = false
    private var observerRegistered = false
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs = _songs.asStateFlow()
    private val _libraryStatus = MutableStateFlow(LibraryStatus())
    val libraryStatus = _libraryStatus.asStateFlow()
    val favoriteIds = dao.observeFavoriteIds().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val playlists = dao.observePlaylists().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val settingsReady = MutableStateFlow(false)
    val settings = app.settingsRepository.settings.onEach { settingsReady.value = true }.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    val visibleSongs = combine(songs, app.settingsRepository.settings.map { it.hiddenFolders }.distinctUntilChanged()) { songs, hidden ->
        songs.filterNot { isHiddenFolder(it.folderKey, it.folder, hidden) }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val visibleSongIds = visibleSongs.map { songs -> songs.mapTo(HashSet()) { it.id } }
        .flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val visibleFavoriteIds = combine(favoriteIds, visibleSongIds) { ids, visible ->
        ids.filterTo(LinkedHashSet()) { it in visible }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())
    val playlistSummaries = combine(dao.observeAllPlaylistTracks(), visibleSongs) { tracks, songs ->
        val byId = songs.associateBy { it.id }
        tracks.groupBy { it.playlistId }.mapValues { (_, entries) ->
            val found = entries.mapNotNull { byId[it.mediaId] }
            PlaylistSummary(found.size, found.sumOf { it.durationMs }, found.firstOrNull { it.artworkUri != null }?.artworkUri)
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
    val listening = ListeningRepository.get(application)
    val listeningPositions = listening.positions
    val audioBookmarks = listening.bookmarks
    fun addBookmark(song: Song, label: String = "") = listening.addBookmark(song, player.currentPosition(), label)
    fun removeBookmark(id: String) = listening.removeBookmark(id)
    val player = app.playerController
    val radioCatalog = combine(app.radioRepository.catalog, app.radioRepository.customStations) { catalog, custom ->
        catalog.copy(stations = (custom + catalog.stations).distinctBy { it.id })
    }.stateIn(viewModelScope, SharingStarted.Eagerly, RadioCatalog())
    val radioFavoriteOrder = app.settingsRepository.radioFavoriteOrder.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun moveRadioFavorite(id: String, delta: Int) { viewModelScope.launch { app.settingsRepository.moveRadioFavorite(id, delta) } }
    fun addManualRadio(name: String, url: String): RadioStation? = app.radioRepository.addManual(name, url)
    fun removeManualRadio(id: String) {
        app.radioRepository.removeManual(id)
        viewModelScope.launch { app.settingsRepository.forgetRadio(id) }
    }
    fun setStartPage(page: StartPage) { viewModelScope.launch { app.settingsRepository.setStartPage(page) } }
    val radioFavorites = app.settingsRepository.radioFavorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())
    val lastRadioStation = app.settingsRepository.lastRadioStation
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    fun refreshRadio(force: Boolean = false) {
        viewModelScope.launch {
            val favorites = try {
                app.settingsRepository.radioFavorites.first()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                emptySet()
            }
            val lastId = try {
                app.settingsRepository.lastRadioStation.first()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
            try {
                app.radioRepository.refresh(force, favorites, lastId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Radio must never take the app process down. The repository keeps
                // curated stations as its safe fallback for the next interaction.
            }
        }
    }
    fun playRadio(station: RadioStation) {
        player.playRadio(station)
        viewModelScope.launch { app.settingsRepository.saveLastRadio(station.id) }
    }
    fun toggleRadioFavorite(id: String) { viewModelScope.launch { app.settingsRepository.toggleRadioFavorite(id) } }
    private val mediaObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) { refreshLibrary() }
    }

    init {
        viewModelScope.launch { visibleSongs.collect { player.updateRestoreCatalog(it) } }
        viewModelScope.launch {
            app.settingsRepository.settings
                .map { it.minimumAudioDurationSeconds to it.includeNonMusicAudio }
                .distinctUntilChanged()
                .collectLatest { scanLibrary() }
        }
    }

    private suspend fun scanLibrary() = scanMutex.withLock {
        _libraryStatus.value = _libraryStatus.value.copy(loading = true, error = null)
        try {
            // Read persisted settings, rather than an initial UI default.
            val prefs = app.settingsRepository.settings.first()
            val loaded = app.mediaStoreRepository.loadSongs(prefs.minimumAudioDurationSeconds, prefs.includeNonMusicAudio)
            if (!observerRegistered) {
                app.contentResolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, mediaObserver)
                observerRegistered = true
            }
            _songs.value = loaded
            if (!initialSessionRestored && loaded.isNotEmpty()) {
                player.restoreSession(loaded.filterNot { isHiddenFolder(it.folderKey, it.folder, prefs.hiddenFolders) })
                initialSessionRestored = true
            }
            _libraryStatus.value = LibraryStatus(false, scannedAt = System.currentTimeMillis())
        } catch (cancelled: CancellationException) {
            _libraryStatus.value = _libraryStatus.value.copy(loading = false)
            throw cancelled
        } catch (error: Exception) {
            _libraryStatus.value = _libraryStatus.value.copy(loading = false, error =
                if (error is SecurityException) "اسمح بالوصول إلى ملفات الصوت من إعدادات الهاتف"
                else "تعذر تحديث المكتبة. حاول مرة أخرى")
        }
    }

    fun refreshLibrary() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch { delay(250); scanLibrary() }
    }

    fun openExternalAudio(uri: Uri) {
        viewModelScope.launch {
            val library = visibleSongs.value
            val existing = library.firstOrNull { it.uri == uri }
            if (existing != null) {
                player.play(existing, library)
            } else {
                val external = app.mediaStoreRepository.loadExternalSong(uri)
                player.play(external, listOf(external))
            }
        }
    }

    fun play(song: Song, source: List<Song> = visibleSongs.value) {
        if (source.any { it.id == song.id }) player.play(song, source)
    }
    fun playAll(shuffle: Boolean = false) {
        visibleSongs.value.firstOrNull()?.let { player.play(it, visibleSongs.value, shuffle) }
    }
    fun toggleFavorite(mediaId: Long) { viewModelScope.launch {
        dao.toggleFavorite(mediaId)
    } }
    fun createPlaylist(name: String, mediaIdToAdd: Long? = null) {
        val clean = name.trim().take(100)
        if (clean.isBlank()) return
        viewModelScope.launch {
            dao.createPlaylistWithSong(clean, mediaIdToAdd)
        }
    }
    fun renamePlaylist(id: Long, name: String) {
        val clean = name.trim().take(100)
        if (clean.isNotBlank()) viewModelScope.launch { dao.renamePlaylist(id, clean) }
    }
    fun deletePlaylist(id: Long) { viewModelScope.launch { dao.deletePlaylist(id) } }
    fun addToPlaylist(playlistId: Long, mediaId: Long) { viewModelScope.launch { dao.addTrackToPlaylist(playlistId, mediaId) } }
    fun removeFromPlaylist(playlistId: Long, mediaId: Long, onRemoved: (PlaylistTrackEntity) -> Unit = {}) {
        viewModelScope.launch { dao.takePlaylistTrack(playlistId, mediaId)?.let(onRemoved) }
    }
    fun restorePlaylistTrack(track: PlaylistTrackEntity) { viewModelScope.launch { dao.restorePlaylistTrack(track) } }
    fun cleanupDeletedMedia(mediaId: Long) {
        player.removeMediaId(mediaId)
        viewModelScope.launch { dao.removeDeletedMediaReferences(mediaId) }
    }
    fun playlistTrackIds(id: Long): Flow<List<Long>> = dao.observePlaylistTrackIds(id)
    fun setTheme(mode: ThemeMode) { viewModelScope.launch { app.settingsRepository.setTheme(mode) } }
    fun setDynamicColors(enabled: Boolean) { viewModelScope.launch { app.settingsRepository.setDynamicColors(enabled) } }
    fun setReduceMotion(enabled: Boolean) { viewModelScope.launch { app.settingsRepository.setReduceMotion(enabled) } }
    fun setMinimumAudioDuration(seconds: Int) { viewModelScope.launch { app.settingsRepository.setMinimumAudioDuration(seconds) } }
    fun setIncludeNonMusicAudio(enabled: Boolean) { viewModelScope.launch { app.settingsRepository.setIncludeNonMusicAudio(enabled) } }
    fun hideFolder(folder: String) { viewModelScope.launch { app.settingsRepository.hideFolder("path:$folder") } }
    fun showFolder(folder: String) { viewModelScope.launch { app.settingsRepository.showFolder(folder) } }
    fun clearHiddenFolders() { viewModelScope.launch { app.settingsRepository.clearHiddenFolders() } }
    override fun onCleared() {
        if (observerRegistered) app.contentResolver.unregisterContentObserver(mediaObserver)
        super.onCleared()
    }
}
