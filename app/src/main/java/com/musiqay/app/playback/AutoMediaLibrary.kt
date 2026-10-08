package com.musiqay.app.playback

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.musiqay.app.data.EgyptianRadio
import com.musiqay.app.data.MediaStoreRepository
import com.musiqay.app.data.MusiqayDatabase
import com.musiqay.app.data.PlaylistEntity
import com.musiqay.app.data.PlaylistTrackEntity
import com.musiqay.app.data.RadioRepository
import com.musiqay.app.data.RadioStation
import com.musiqay.app.data.Song
import com.musiqay.app.util.normalizeSearch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Driver-safe media library exposed to Android Auto / MediaBrowser clients.
 * The car owns the UI; Musiqay only serves browse/search/playable media items.
 */
class AutoMediaLibrary(context: Context) : MediaLibraryService.MediaLibrarySession.Callback {
    companion object {
        private const val ROOT = "auto:root"
        private const val RECENT = "auto:recent"
        private const val ALL = "auto:all"
        private const val FAVORITES = "auto:favorites"
        private const val FOLDERS = "auto:folders"
        private const val PLAYLISTS = "auto:playlists"
        private const val RADIO = "auto:radio"
        private const val FOLDER_PREFIX = "auto:folder:"
        private const val PLAYLIST_PREFIX = "auto:playlist:"
    }

    private val appContext = context.applicationContext
    private val mediaStore = MediaStoreRepository(appContext)
    private val database = MusiqayDatabase.get(appContext)
    private val radioRepository = RadioRepository(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var snapshot = Snapshot()

    data class Snapshot(
        val songs: List<Song> = emptyList(),
        val favoriteIds: Set<Long> = emptySet(),
        val playlists: List<PlaylistEntity> = emptyList(),
        val tracks: List<PlaylistTrackEntity> = emptyList(),
        val stations: List<RadioStation> = EgyptianRadio.stations
    )

    init {
        refresh()
    }

    fun refresh() {
        scope.launch {
            val songs = runCatching {
                mediaStore.loadSongs(minimumDurationSeconds = 0, includeNonMusicAudio = true)
            }.getOrDefault(emptyList())
            val dao = database.musicDao()
            val favorites = runCatching { dao.favoriteSnapshot().mapTo(HashSet()) { it.mediaId } }
                .getOrDefault(emptySet())
            val playlists = runCatching { dao.playlistSnapshot() }.getOrDefault(emptyList())
            val tracks = runCatching { dao.trackSnapshot() }.getOrDefault(emptyList())
            val stations = (radioRepository.catalog.value.stations + radioRepository.customStations.value)
                .filter { it.id.isNotBlank() && it.name.isNotBlank() && it.streamUrl.isNotBlank() }
                .distinctBy { it.id }

            snapshot = Snapshot(
                songs = songs,
                favoriteIds = favorites,
                playlists = playlists,
                tracks = tracks,
                stations = stations.ifEmpty { EgyptianRadio.stations }
            )
        }
    }

    fun release() {
        scope.cancel()
    }

    private fun browseNode(id: String, title: String, subtitle: String? = null): MediaItem =
        MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

    private fun songItem(song: Song): MediaItem =
        song.toMediaItem().buildUpon()
            .setMediaMetadata(
                song.toMediaItem().mediaMetadata.buildUpon()
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .build()
            )
            .build()

    private fun radioItem(station: RadioStation): MediaItem =
        station.toMediaItem().buildUpon()
            .setMediaMetadata(
                station.toMediaItem().mediaMetadata.buildUpon()
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
                    .build()
            )
            .build()

    private fun rootChildren(data: Snapshot): List<MediaItem> = listOf(
        browseNode(RECENT, "مضاف حديثًا", "آخر الملفات على الهاتف"),
        browseNode(FAVORITES, "المفضلة", "${data.favoriteIds.size} ملف"),
        browseNode(FOLDERS, "المجلدات", "${data.songs.map { it.folderKey }.distinct().size} مجلد"),
        browseNode(PLAYLISTS, "قوائم التشغيل", "${data.playlists.size} قائمة"),
        browseNode(RADIO, "الراديو", "${data.stations.size} محطة"),
        browseNode(ALL, "كل الملفات", "${data.songs.size} ملف صوتي")
    )

    private fun folderNode(folderKey: String, songs: List<Song>): MediaItem {
        val title = songs.firstOrNull()?.folder?.ifBlank { null } ?: "مجلد"
        return browseNode(FOLDER_PREFIX + Uri.encode(folderKey), title, "${songs.size} ملف")
    }

    private fun playlistNode(item: PlaylistEntity, count: Int): MediaItem =
        browseNode(PLAYLIST_PREFIX + item.id, item.name, "$count ملف")

    private fun childrenFor(parentId: String, data: Snapshot): List<MediaItem> = when {
        parentId == ROOT -> rootChildren(data)
        parentId == RECENT -> data.songs.sortedByDescending { it.dateAddedSeconds }.take(100).map(::songItem)
        parentId == ALL -> data.songs.map(::songItem)
        parentId == FAVORITES -> data.songs.filter { it.id in data.favoriteIds }.map(::songItem)
        parentId == FOLDERS -> data.songs.groupBy { it.folderKey }
            .toList()
            .sortedBy { (_, songs) -> songs.firstOrNull()?.folder?.lowercase() }
            .map { (key, songs) -> folderNode(key, songs) }
        parentId.startsWith(FOLDER_PREFIX) -> {
            val key = Uri.decode(parentId.removePrefix(FOLDER_PREFIX))
            data.songs.filter { it.folderKey == key }.map(::songItem)
        }
        parentId == PLAYLISTS -> {
            val counts = data.tracks.groupingBy { it.playlistId }.eachCount()
            data.playlists.map { playlistNode(it, counts[it.id] ?: 0) }
        }
        parentId.startsWith(PLAYLIST_PREFIX) -> {
            val playlistId = parentId.removePrefix(PLAYLIST_PREFIX).toLongOrNull()
            if (playlistId == null) emptyList() else {
                val byId = data.songs.associateBy { it.id }
                data.tracks.filter { it.playlistId == playlistId }
                    .sortedBy { it.position }
                    .mapNotNull { byId[it.mediaId] }
                    .map(::songItem)
            }
        }
        parentId == RADIO -> data.stations.map(::radioItem)
        else -> emptyList()
    }

    private fun findItem(mediaId: String, data: Snapshot): MediaItem? {
        return when {
            mediaId == ROOT -> browseNode(ROOT, "موسيقاي")
            mediaId in setOf(RECENT, ALL, FAVORITES, FOLDERS, PLAYLISTS, RADIO) ->
                rootChildren(data).firstOrNull { it.mediaId == mediaId }
            mediaId.startsWith(FOLDER_PREFIX) -> {
                val key = Uri.decode(mediaId.removePrefix(FOLDER_PREFIX))
                val songs = data.songs.filter { it.folderKey == key }
                songs.takeIf { it.isNotEmpty() }?.let { folderNode(key, it) }
            }
            mediaId.startsWith(PLAYLIST_PREFIX) -> {
                val id = mediaId.removePrefix(PLAYLIST_PREFIX).toLongOrNull()
                data.playlists.firstOrNull { it.id == id }?.let { playlist ->
                    playlistNode(playlist, data.tracks.count { it.playlistId == playlist.id })
                }
            }
            mediaId.startsWith("radio:") -> data.stations
                .firstOrNull { "radio:" + it.id == mediaId }
                ?.let(::radioItem)
            else -> mediaId.toLongOrNull()?.let { id ->
                data.songs.firstOrNull { it.id == id }?.let(::songItem)
            }
        }
    }

    private fun searchItems(query: String, data: Snapshot): List<MediaItem> {
        val term = normalizeSearch(query)
        if (term.isBlank()) return emptyList()
        val local = data.songs.asSequence()
            .filter {
                normalizeSearch(it.title).contains(term) ||
                    normalizeSearch(it.artist).contains(term) ||
                    normalizeSearch(it.album).contains(term) ||
                    normalizeSearch(it.folder).contains(term)
            }
            .take(80)
            .map(::songItem)
            .toList()
        val radio = data.stations.asSequence()
            .filter { normalizeSearch(it.name).contains(term) || normalizeSearch(it.category).contains(term) }
            .take(20)
            .map(::radioItem)
            .toList()
        return local + radio
    }

    private fun page(items: List<MediaItem>, page: Int, pageSize: Int): List<MediaItem> {
        if (items.isEmpty() || page < 0 || pageSize <= 0) return emptyList()
        val from = (page.toLong() * pageSize.toLong()).coerceAtMost(items.size.toLong()).toInt()
        val to = (from + pageSize).coerceAtMost(items.size)
        return items.subList(from, to)
    }

    override fun onGetLibraryRoot(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<MediaItem>> =
        Futures.immediateFuture(LibraryResult.ofItem(browseNode(ROOT, "موسيقاي"), params))

    override fun onGetChildren(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val items = page(childrenFor(parentId, snapshot), page, pageSize)
        return Futures.immediateFuture(LibraryResult.ofItemList(items, params))
    }

    override fun onGetItem(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        mediaId: String
    ): ListenableFuture<LibraryResult<MediaItem>> {
        val item = findItem(mediaId, snapshot)
        return if (item != null) {
            Futures.immediateFuture(LibraryResult.ofItem(item, null))
        } else {
            Futures.immediateFuture(LibraryResult.ofError(androidx.media3.session.SessionError.ERROR_BAD_VALUE))
        }
    }

    override fun onSearch(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<Void>> {
        val count = searchItems(query, snapshot).size
        session.notifySearchResultChanged(browser, query, count, params)
        return Futures.immediateFuture(LibraryResult.ofVoid(params))
    }

    override fun onGetSearchResult(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val items = page(searchItems(query, snapshot), page, pageSize)
        return Futures.immediateFuture(LibraryResult.ofItemList(items, params))
    }

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>
    ): ListenableFuture<MutableList<MediaItem>> {
        val resolved = mediaItems.mapNotNull { requested ->
            requested.localConfiguration?.let { requested } ?: findItem(requested.mediaId, snapshot)
        }.toMutableList()
        return Futures.immediateFuture(resolved)
    }
}
