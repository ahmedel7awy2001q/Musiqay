package com.musiqay.app.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.musiqay.app.data.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class NowPlayingState(
    val mediaId: Long? = null,
    val title: String = "لا توجد أغنية قيد التشغيل",
    val artist: String = "",
    val album: String = "",
    val artworkUri: Uri? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF
)

class PlayerController(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var sleepJob: Job? = null

    private val playbackPrefs =
        context.getSharedPreferences("playback_state", Context.MODE_PRIVATE)

    private var lastPersistAt = 0L
    private var pendingRestoreSongs: List<Song>? = null

    private val _state = MutableStateFlow(NowPlayingState())
    val state: StateFlow<NowPlayingState> = _state.asStateFlow()

    private val _queue = MutableStateFlow<List<MediaItem>>(emptyList())
    val queue: StateFlow<List<MediaItem>> = _queue.asStateFlow()

    private val _sleepTimerEnd = MutableStateFlow<Long?>(null)
    val sleepTimerEnd: StateFlow<Long?> = _sleepTimerEnd.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            updateFromPlayer(player)
            persistSession(player)
        }
    }

    fun connect() {
        if (controllerFuture != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        future.addListener({
            runCatching { future.get() }.onSuccess { mediaController ->
                controller = mediaController
                mediaController.addListener(listener)
                updateFromPlayer(mediaController)

                pendingRestoreSongs?.let { songs ->
                    restoreSession(songs)
                }
            }
        }, ContextCompat.getMainExecutor(context))

        scope.launch {
            while (isActive) {
                controller?.let { updatePosition(it) }
                delay(500)
            }
        }
    }

    fun play(song: Song, source: List<Song>) {
        val player = controller ?: return
        val items = source.map { it.toMediaItem() }
        val index = source.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        player.setMediaItems(items, index, 0L)
        player.prepare()
        player.play()
    }

    fun playMediaId(mediaId: Long) {
        val player = controller ?: return
        for (i in 0 until player.mediaItemCount) {
            if (player.getMediaItemAt(i).mediaId == mediaId.toString()) {
                player.seekToDefaultPosition(i)
                player.play()
                return
            }
        }
    }

    fun togglePlayPause() {
        val player = controller ?: return
        if (player.isPlaying) player.pause() else {
            if (player.playbackState == Player.STATE_IDLE) player.prepare()
            player.play()
        }
    }

    fun next() {
        controller?.seekToNextMediaItem()
    }

    fun previous() {
        val player = controller ?: return
        if (player.currentPosition > 5_000) player.seekTo(0) else player.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs.coerceAtLeast(0))
    }

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun cycleRepeatMode() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    fun moveQueueItem(from: Int, to: Int) {
        val player = controller ?: return
        if (from in 0 until player.mediaItemCount && to in 0 until player.mediaItemCount && from != to) {
            player.moveMediaItem(from, to)
        }
    }

    fun removeQueueItem(index: Int) {
        controller?.let { player ->
            if (index in 0 until player.mediaItemCount) player.removeMediaItem(index)
        }
    }

    fun playNext(song: Song) {
        val player = controller ?: return
        val insertAt = (player.currentMediaItemIndex + 1).coerceAtMost(player.mediaItemCount)
        player.addMediaItem(insertAt, song.toMediaItem())
    }

    fun addToQueue(song: Song) {
        controller?.addMediaItem(song.toMediaItem())
    }

    fun setSleepTimer(minutes: Int?) {
        sleepJob?.cancel()
        sleepJob = null
        if (minutes == null || minutes <= 0) {
            _sleepTimerEnd.value = null
            return
        }
        val end = System.currentTimeMillis() + minutes * 60_000L
        _sleepTimerEnd.value = end
        sleepJob = scope.launch {
            delay(minutes * 60_000L)
            controller?.pause()
            _sleepTimerEnd.value = null
        }
    }

    fun restoreSession(songs: List<Song>) {
        val player = controller

        if (player == null) {
            pendingRestoreSongs = songs
            return
        }

        pendingRestoreSongs = null

        // If Android service already has an active queue, keep it as-is.
        if (player.mediaItemCount > 0) {
            updateFromPlayer(player)
            return
        }

        val savedIds = playbackPrefs
            .getString("queue_ids", null)
            ?.split(",")
            ?.mapNotNull { it.toLongOrNull() }
            .orEmpty()

        if (savedIds.isEmpty()) return

        val songsById = songs.associateBy { it.id }
        val restoredSongs = savedIds.mapNotNull { songsById[it] }

        if (restoredSongs.isEmpty()) return

        val currentId = playbackPrefs.getLong("current_media_id", -1L)
        val savedPosition = playbackPrefs
            .getLong("position_ms", 0L)
            .coerceAtLeast(0L)

        val index = restoredSongs
            .indexOfFirst { it.id == currentId }
            .takeIf { it >= 0 }
            ?: 0

        player.setMediaItems(
            restoredSongs.map { it.toMediaItem() },
            index,
            savedPosition
        )

        player.shuffleModeEnabled =
            playbackPrefs.getBoolean("shuffle_enabled", false)

        player.repeatMode =
            playbackPrefs.getInt(
                "repeat_mode",
                Player.REPEAT_MODE_OFF
            )

        player.prepare()
        player.pause()

        updateFromPlayer(player)
    }

    private fun persistSession(player: Player) {
        if (player.mediaItemCount <= 0) return

        val queueIds = buildList {
            for (index in 0 until player.mediaItemCount) {
                player.getMediaItemAt(index)
                    .mediaId
                    .toLongOrNull()
                    ?.let(::add)
            }
        }

        if (queueIds.isEmpty()) return

        playbackPrefs.edit()
            .putString("queue_ids", queueIds.joinToString(","))
            .putLong(
                "current_media_id",
                player.currentMediaItem?.mediaId?.toLongOrNull() ?: -1L
            )
            .putLong(
                "position_ms",
                player.currentPosition.coerceAtLeast(0L)
            )
            .putBoolean(
                "shuffle_enabled",
                player.shuffleModeEnabled
            )
            .putInt(
                "repeat_mode",
                player.repeatMode
            )
            .apply()
    }
    private fun updateFromPlayer(player: Player) {
        val metadata = player.currentMediaItem?.mediaMetadata
        _state.value = NowPlayingState(
            mediaId = player.currentMediaItem?.mediaId?.toLongOrNull(),
            title = metadata?.title?.toString().orEmpty().ifBlank { "لا توجد أغنية قيد التشغيل" },
            artist = metadata?.artist?.toString().orEmpty(),
            album = metadata?.albumTitle?.toString().orEmpty(),
            artworkUri = metadata?.artworkUri,
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition.coerceAtLeast(0),
            durationMs = player.duration.takeIf { it > 0 } ?: 0,
            shuffleEnabled = player.shuffleModeEnabled,
            repeatMode = player.repeatMode
        )
        _queue.value = List(player.mediaItemCount) { player.getMediaItemAt(it) }
    }

    private fun updatePosition(player: Player) {
        val previous = _state.value
        _state.value = previous.copy(
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition.coerceAtLeast(0),
            durationMs = player.duration.takeIf { it > 0 } ?: previous.durationMs
        )

        val now = System.currentTimeMillis()
        if (now - lastPersistAt >= 5_000L) {
            lastPersistAt = now
            persistSession(player)
        }
    }
}
