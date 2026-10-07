package com.musiqay.app.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.musiqay.app.data.Song
import com.musiqay.app.data.RadioStation
import com.musiqay.app.util.MusicSession
import com.musiqay.app.util.isRadioMediaId
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class NowPlayingState(
    val mediaId: Long? = null,
    val title: String = "لا يوجد ملف قيد التشغيل",
    val artist: String = "",
    val album: String = "",
    val artworkUri: Uri? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val queueIndex: Int = -1,
    val radioStationId: String? = null,
    val buffering: Boolean = false,
    val wantsPlayback: Boolean = false,
    val playbackError: Boolean = false,
    val playbackSpeed: Float = 1f,
    val broadcastTitle: String = ""
) {
    val isRadio: Boolean get() = radioStationId != null
    val hasMedia: Boolean get() = mediaId != null || isRadio
}
data class RemovedQueueItem(val item: MediaItem, val index: Int, val positionMs: Long, val wasCurrent: Boolean, val wasPlaying: Boolean)

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlayerController(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var pollJob: Job? = null
    private val history = com.musiqay.app.data.ListeningRepository.get(context)
    private val playbackPrefs = context.getSharedPreferences("playback_state", Context.MODE_PRIVATE)
    private var lastPersistAt = 0L
    private var musicSpeed = playbackPrefs.getFloat("music_speed", 1f).takeIf { it in com.musiqay.app.util.PlaybackSpeeds } ?: 1f
    private var musicSession = readMusicSession()
    private val _canResumeMusic = MutableStateFlow(false)
    val canResumeMusic = _canResumeMusic.asStateFlow()
    private var pendingRestoreSongs: List<Song>? = null
    private var restoreCatalog: List<Song> = emptyList()
    private val pendingActions = ArrayDeque<(MediaController) -> Unit>()
    private var restoring = false
    private var hasSession = false
    private val _state = MutableStateFlow(NowPlayingState())
    val state = _state.asStateFlow()
    // Library surfaces do not subscribe to the changing playback clock.
    val summary = state.map { it.copy(positionMs = 0L) }.distinctUntilChanged()
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), NowPlayingState())
    private val _queue = MutableStateFlow<List<MediaItem>>(emptyList())
    val queue = _queue.asStateFlow()
    val sleepTimerEnd = PlaybackService.sleepTimerEnd
    val sleepAtTrackEnd = PlaybackService.sleepAtTrackEnd
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    fun dismissError() { _error.value = null }

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (restoring) return
            val timelineChanged = events.contains(Player.EVENT_TIMELINE_CHANGED)
            updateFromPlayer(player, timelineChanged)
            if (timelineChanged) persistQueue(player)
            else if (hasSession) persistPosition(player)
        }
        override fun onPlayerError(error: PlaybackException) {
            _error.value = if (isRadioMediaId(controller?.currentMediaItem?.mediaId))
                "انقطع بث المحطة. نحاول إعادة الاتصال؛ يمكنك إيقافه أو المحاولة مجددًا"
                else "تعذر تشغيل الملف. تأكد أنه ما زال موجودًا على الهاتف"
        }
    }

    fun connect() {
        if (controllerFuture != null) return
        val future = MediaController.Builder(context,
            SessionToken(context, ComponentName(context, PlaybackService::class.java)))
            .setListener(object : MediaController.Listener {
                override fun onDisconnected(controller: MediaController) {
                    this@PlayerController.controller = null
                    controllerFuture = null
                    _state.value = _state.value.copy(isPlaying = false, wantsPlayback = false, buffering = false)
                }
            }).buildAsync()
        controllerFuture = future
        future.addListener({
            runCatching { future.get() }.onSuccess { connected ->
                controller = connected
                connected.addListener(listener)
                hasSession = connected.mediaItemCount > 0
                updateFromPlayer(connected, true)
                if (connected.mediaItemCount == 0) {
                    (pendingRestoreSongs ?: restoreCatalog.takeIf { it.isNotEmpty() })?.let(::restoreSession)
                }
                while (pendingActions.isNotEmpty()) pendingActions.removeFirst().invoke(connected)
            }.onFailure {
                controllerFuture = null
                pendingActions.clear()
                _error.value = "تعذر الاتصال بالمشغل. اضغط تشغيل للمحاولة مرة أخرى"
            }
        }, ContextCompat.getMainExecutor(context))
        if (pollJob == null) pollJob = scope.launch {
            while (isActive) {
                controller?.let { player ->
                    if (_state.subscriptionCount.value > 0) updatePosition(player)
                    val now = android.os.SystemClock.elapsedRealtime()
                    if (hasSession && player.isPlaying && now - lastPersistAt >= 10_000) {
                        persistPosition(player)
                        lastPersistAt = now
                    }
                }
                delay(if (!isRadioMediaId(controller?.currentMediaItem?.mediaId) &&
                    (controller?.isPlaying == true || _state.subscriptionCount.value > 0)) 1_000 else 10_000)
            }
        }
    }
    private fun withPlayer(action: (MediaController) -> Unit) {
        val player = controller
        if (player != null) action(player)
        else { pendingActions.addLast(action); connect() }
    }
    fun playRadio(station: RadioStation) = withPlayer { player ->
        if (!isRadioMediaId(player.currentMediaItem?.mediaId)) persistQueue(player)
        if (player.currentMediaItem?.mediaId != "radio:" + station.id) {
            restoring = true
            try {
                player.setMediaItem(station.toMediaItem())
                player.shuffleModeEnabled = false
                player.repeatMode = Player.REPEAT_MODE_OFF
            } finally { restoring = false }
        }
        player.setPlaybackSpeed(1f)
        player.seekToDefaultPosition(); player.prepare(); player.play()
        updateFromPlayer(player, true); _error.value = null
    }
    fun stopRadio() { controller?.takeIf { isRadioMediaId(it.currentMediaItem?.mediaId) }?.let { it.pause(); it.stop() } }
    fun resumeMusic() = withPlayer { switchToMusic(it, autoplay = true) }
    private fun switchToMusic(player: MediaController, autoplay: Boolean): Boolean {
        val byId = restoreCatalog.associateBy { it.id }
        val saved = musicSession.available(byId.keys)
        if (saved.ids.isEmpty()) {
            _error.value = "لا توجد قائمة ملفات محفوظة متاحة. اختر ملفًا من المكتبة"
            return false
        }
        val songs = saved.ids.mapNotNull(byId::get)
        val position = saved.positionMs.coerceIn(0, songs[saved.index].durationMs.coerceAtLeast(0))
        restoring = true
        try {
            hasSession = true
            player.setMediaItems(songs.map { it.toMediaItem() }, saved.index, position)
            player.shuffleModeEnabled = saved.shuffle; player.repeatMode = saved.repeat
            player.setPlaybackSpeed(musicSpeed)
            player.prepare()
            if (autoplay) player.play() else player.pause()
        } finally { restoring = false }
        updateFromPlayer(player, true); persistQueue(player); _error.value = null
        return true
    }
    fun play(song: Song, source: List<Song>, shuffle: Boolean? = null) {
        if (source.isEmpty()) return
        val index = source.indexOfFirst { it.id == song.id }
        if (index < 0) return
        withPlayer { player ->
            val previousMusic = musicSession
            val returningFromRadio = isRadioMediaId(player.currentMediaItem?.mediaId)
            val resumeSameSong = returningFromRadio && previousMusic.ids == source.map { it.id } && previousMusic.currentId == song.id
            hasSession = true
            val sameQueue = player.mediaItemCount == source.size && source.indices.all {
                player.getMediaItemAt(it).mediaId == source[it].id.toString()
            }
            if (sameQueue) {
                if (player.currentMediaItemIndex != index) player.seekTo(index, history.resumeFor(song))
                else if (player.playbackState == Player.STATE_ENDED) player.seekTo(index, 0)
            } else player.setMediaItems(source.map { it.toMediaItem() },
                if (resumeSameSong) previousMusic.index else index,
                if (resumeSameSong) previousMusic.positionMs.coerceIn(0, song.durationMs.coerceAtLeast(0)) else history.resumeFor(song))
            player.setPlaybackSpeed(musicSpeed)
            if (returningFromRadio) {
                player.shuffleModeEnabled = previousMusic.shuffle
                player.repeatMode = previousMusic.repeat
            }
            if (shuffle != null) player.shuffleModeEnabled = shuffle
            player.prepare()
            player.play()
            _error.value = null
        }
    }
    fun playQueueIndex(index: Int) = withPlayer { player ->
        if (isRadioMediaId(player.currentMediaItem?.mediaId) && !switchToMusic(player, autoplay = false)) return@withPlayer
        if (index in 0 until player.mediaItemCount) {
            if (index != player.currentMediaItemIndex) {
                val song = restoreCatalog.firstOrNull { it.id.toString() == player.getMediaItemAt(index).mediaId }
                player.seekTo(index, song?.let(history::resumeFor) ?: 0L)
            }
            player.prepare(); player.play()
        }
    }
    fun togglePlayPause() = withPlayer { player ->
        if (player.mediaItemCount > 0) {
            if (isRadioMediaId(player.currentMediaItem?.mediaId)) {
                if (player.playWhenReady) { player.pause(); player.stop() }
                else { player.seekToDefaultPosition(); player.prepare(); player.play() }
            } else if (player.playWhenReady && player.playbackState != Player.STATE_ENDED) player.pause() else {
                if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
                player.prepare(); player.play()
            }
        }
    }
    fun next() { controller?.takeUnless { isRadioMediaId(it.currentMediaItem?.mediaId) }?.seekToNextMediaItem() }
    fun previous() { controller?.takeUnless { isRadioMediaId(it.currentMediaItem?.mediaId) }?.let { if (it.currentPosition > 5_000) it.seekTo(0) else it.seekToPreviousMediaItem() } }
    fun seekTo(positionMs: Long) { controller?.takeUnless { isRadioMediaId(it.currentMediaItem?.mediaId) }?.seekTo(positionMs.coerceAtLeast(0)) }
    fun toggleShuffle() { controller?.takeUnless { isRadioMediaId(it.currentMediaItem?.mediaId) }?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }
    fun cycleRepeatMode() { controller?.takeUnless { isRadioMediaId(it.currentMediaItem?.mediaId) }?.let { it.repeatMode = when (it.repeatMode) {
        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
        else -> Player.REPEAT_MODE_OFF
    } } }
    fun moveQueueItem(from: Int, to: Int) { controller?.takeUnless { isRadioMediaId(it.currentMediaItem?.mediaId) }?.let {
        if (from in 0 until it.mediaItemCount && to in 0 until it.mediaItemCount && from != to) it.moveMediaItem(from, to)
    } }
    fun removeQueueItem(index: Int): RemovedQueueItem? {
        val player = controller ?: return null
        if (isRadioMediaId(player.currentMediaItem?.mediaId)) return null
        if (index !in 0 until player.mediaItemCount) return null
        val removed = RemovedQueueItem(player.getMediaItemAt(index), index, player.currentPosition,
            index == player.currentMediaItemIndex, player.isPlaying)
        hasSession = true
        player.removeMediaItem(index)
        persistQueue(player)
        return removed
    }
    fun restoreQueueItem(removed: RemovedQueueItem) = withPlayer { player ->
        if (isRadioMediaId(player.currentMediaItem?.mediaId)) {
            removed.item.mediaId.toLongOrNull()?.let { id ->
                persistMusicSession(musicSession.insert(id, removed.index, if (removed.wasCurrent) removed.positionMs else null))
            }
            return@withPlayer
        }
        hasSession = true
        val index = removed.index.coerceIn(0, player.mediaItemCount)
        player.addMediaItem(index, removed.item)
        if (removed.wasCurrent) {
            player.seekTo(index, removed.positionMs.coerceAtLeast(0)); player.prepare()
            if (removed.wasPlaying) player.play()
        }
    }
    fun clearQueue() { controller?.let {
        if (isRadioMediaId(it.currentMediaItem?.mediaId)) persistMusicSession(MusicSession())
        else { hasSession = true; it.clearMediaItems(); persistQueue(it) }
    } }
    fun removeMediaId(id: Long) { controller?.let { player ->
        if (isRadioMediaId(player.currentMediaItem?.mediaId)) {
            persistMusicSession(musicSession.available(musicSession.ids.toSet() - id)); return@let
        }
        for (i in player.mediaItemCount - 1 downTo 0) if (player.getMediaItemAt(i).mediaId == id.toString()) player.removeMediaItem(i)
        if (hasSession) persistQueue(player)
    } }
    fun playNext(song: Song) = withPlayer { player ->
        if (isRadioMediaId(player.currentMediaItem?.mediaId)) {
            persistMusicSession(musicSession.append(song.id, next = true)); return@withPlayer
        }
        hasSession = true
        player.addMediaItem((player.currentMediaItemIndex + 1).coerceIn(0, player.mediaItemCount), song.toMediaItem())
    }
    fun addToQueue(song: Song) = withPlayer {
        if (isRadioMediaId(it.currentMediaItem?.mediaId)) persistMusicSession(musicSession.append(song.id))
        else { hasSession = true; it.addMediaItem(song.toMediaItem()) }
    }
    fun setSleepTimer(minutes: Int?) = withPlayer { PlaybackService.setSleepTimer(minutes) }
    fun setSleepAtTrackEnd() = withPlayer { PlaybackService.setSleepAtTrackEnd() }
    fun currentPosition(): Long = controller?.currentPosition?.coerceAtLeast(0) ?: state.value.positionMs
    fun seekBy(deltaMs: Long) { controller?.takeUnless { isRadioMediaId(it.currentMediaItem?.mediaId) }?.let {
        it.seekTo(com.musiqay.app.util.boundedSeek(it.currentPosition, deltaMs, it.duration))
    } }
    fun setPlaybackSpeed(speed: Float) {
        if (speed !in com.musiqay.app.util.PlaybackSpeeds) return
        controller?.takeUnless { isRadioMediaId(it.currentMediaItem?.mediaId) }?.let {
            musicSpeed = speed; playbackPrefs.edit().putFloat("music_speed", speed).apply()
            it.setPlaybackSpeed(speed)
        }
    }
    fun updateRestoreCatalog(songs: List<Song>) {
        restoreCatalog = songs
        if (pendingRestoreSongs != null) pendingRestoreSongs = songs
        updateMusicAvailability()
    }
    fun restoreSession(songs: List<Song>) {
        restoreCatalog = songs
        val player = controller
        if (player == null) { pendingRestoreSongs = songs; connect(); return }
        pendingRestoreSongs = null
        if (player.mediaItemCount > 0 || hasSession) return
        if (musicSession.available(songs.mapTo(HashSet()) { it.id }).ids.isNotEmpty()) switchToMusic(player, autoplay = false)
    }
    private fun readMusicSession(): MusicSession {
        val ids = playbackPrefs.getString("queue_ids", null)?.split(",")?.mapNotNull(String::toLongOrNull).orEmpty()
        val currentId = playbackPrefs.getLong("current_media_id", -1L)
        val index = playbackPrefs.getInt("current_index", -1).takeIf { it in ids.indices && ids[it] == currentId }
            ?: ids.indexOf(currentId).coerceAtLeast(0)
        return MusicSession(ids, index, playbackPrefs.getLong("position_ms", 0).coerceAtLeast(0),
            playbackPrefs.getBoolean("shuffle_enabled", false),
            playbackPrefs.getInt("repeat_mode", Player.REPEAT_MODE_OFF).coerceIn(Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ALL))
    }
    private fun updateMusicAvailability() {
        val ids = restoreCatalog.mapTo(HashSet()) { it.id }
        _canResumeMusic.value = musicSession.ids.any { it in ids }
    }
    private fun persistMusicSession(saved: MusicSession) {
        musicSession = saved
        if (isRadioMediaId(controller?.currentMediaItem?.mediaId)) {
            val byId = restoreCatalog.associateBy { it.id }
            _queue.value = saved.ids.mapNotNull { byId[it]?.toMediaItem() }
        }
        playbackPrefs.edit().putString("queue_ids", saved.ids.joinToString(","))
            .putLong("current_media_id", saved.currentId ?: -1)
            .putInt("current_index", if (saved.ids.isEmpty()) -1 else saved.index)
            .putLong("position_ms", saved.positionMs)
            .putBoolean("shuffle_enabled", saved.shuffle).putInt("repeat_mode", saved.repeat).apply()
        updateMusicAvailability()
    }
    private fun persistQueue(player: Player) {
        if (!hasSession || restoring || isRadioMediaId(player.currentMediaItem?.mediaId)) return
        val ids = (0 until player.mediaItemCount).mapNotNull { player.getMediaItemAt(it).mediaId.toLongOrNull() }
        persistMusicSession(MusicSession(ids, player.currentMediaItemIndex.coerceAtLeast(0),
            player.currentPosition.coerceAtLeast(0), player.shuffleModeEnabled, player.repeatMode))
    }
    private fun persistPosition(player: Player) {
        if (!hasSession || restoring || isRadioMediaId(player.currentMediaItem?.mediaId)) return
        musicSession = musicSession.copy(index = player.currentMediaItemIndex.coerceAtLeast(0),
            positionMs = player.currentPosition.coerceAtLeast(0), shuffle = player.shuffleModeEnabled, repeat = player.repeatMode)
        playbackPrefs.edit().putLong("current_media_id", player.currentMediaItem?.mediaId?.toLongOrNull() ?: -1)
            .putInt("current_index", if (player.mediaItemCount > 0) player.currentMediaItemIndex else -1)
            .putLong("position_ms", musicSession.positionMs)
            .putBoolean("shuffle_enabled", musicSession.shuffle).putInt("repeat_mode", musicSession.repeat).apply()
    }
    private fun updateFromPlayer(player: Player, updateQueue: Boolean) {
        val metadata = player.currentMediaItem?.mediaMetadata
        val radio = player.currentMediaItem?.mediaId?.takeIf(::isRadioMediaId)?.removePrefix("radio:")
        _state.value = NowPlayingState(
            mediaId = player.currentMediaItem?.mediaId?.toLongOrNull(),
            title = metadata?.title?.toString().orEmpty().ifBlank { "لا يوجد ملف قيد التشغيل" },
            artist = metadata?.artist?.toString().orEmpty(), album = metadata?.albumTitle?.toString().orEmpty(),
            artworkUri = metadata?.artworkUri, isPlaying = player.isPlaying,
            positionMs = player.currentPosition.coerceAtLeast(0), durationMs = player.duration.takeIf { it > 0 } ?: 0,
            shuffleEnabled = player.shuffleModeEnabled, repeatMode = player.repeatMode,
            queueIndex = if (radio == null && player.mediaItemCount > 0) player.currentMediaItemIndex else -1,
            radioStationId = radio, buffering = player.playbackState == Player.STATE_BUFFERING,
            wantsPlayback = player.playWhenReady, playbackError = player.playerError != null,
            playbackSpeed = player.playbackParameters.speed,
            broadcastTitle = if (radio != null) player.mediaMetadata.title?.toString().orEmpty()
                .takeUnless { it == metadata?.title?.toString() }.orEmpty() else "")
        if (updateQueue && radio == null) _queue.value = List(player.mediaItemCount) { player.getMediaItemAt(it) }
    }
    private fun updatePosition(player: Player) {
        if (isRadioMediaId(player.currentMediaItem?.mediaId)) return
        _state.value = _state.value.copy(positionMs = player.currentPosition.coerceAtLeast(0),
            durationMs = player.duration.takeIf { it > 0 } ?: _state.value.durationMs)
    }
}
