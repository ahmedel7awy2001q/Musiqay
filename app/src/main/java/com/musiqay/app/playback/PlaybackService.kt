package com.musiqay.app.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.media3.common.*
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.*
import com.musiqay.app.MainActivity
import com.musiqay.app.R
import com.musiqay.app.util.isRadioMediaId
import com.musiqay.app.util.radioRetryDelayMs
import com.musiqay.app.data.ListeningRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlaybackService : MediaSessionService() {
    companion object {
        private var activePlayer: ExoPlayer? = null
        private var activeService: PlaybackService? = null
        private val _sleepTimerEnd = MutableStateFlow<Long?>(null)
        val sleepTimerEnd = _sleepTimerEnd.asStateFlow()
        private val _sleepAtTrackEnd = MutableStateFlow(false)
        val sleepAtTrackEnd = _sleepAtTrackEnd.asStateFlow()
        fun setSleepTimer(minutes: Int?) { activeService?.scheduleSleep(minutes) }
        fun setSleepAtTrackEnd() { activeService?.scheduleTrackEnd() }
        val audioSessionId: Int get() = activePlayer?.audioSessionId ?: C.AUDIO_SESSION_ID_UNSET
    }
    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private val recoveryHandler = Handler(Looper.getMainLooper())
    private val sleepHandler = Handler(Looper.getMainLooper())
    private val historyHandler = Handler(Looper.getMainLooper())
    private val history by lazy { ListeningRepository.get(this) }
    private var sleepMediaId: String? = null
    private var previousItem: MediaItem? = null
    private var retryAttempt = 0
    private val historyTick = object : Runnable {
        override fun run() {
            player?.takeIf { it.isPlaying }?.let(::rememberPosition)
            historyHandler.postDelayed(this, 10_000)
        }
    }
    private fun rememberPosition(current: Player) {
        val item = current.currentMediaItem ?: return
        savePosition(item, current.currentPosition, current.duration)
    }
    private fun savePosition(item: MediaItem, position: Long, duration: Long) {
        val id = item.mediaId.toLongOrNull() ?: return
        val uri = item.localConfiguration?.uri?.toString() ?: return
        val knownDuration = duration.takeIf { it > 0 } ?: item.mediaMetadata.extras?.getLong("duration_ms") ?: 0L
        history.savePosition(id, position, knownDuration, item.mediaMetadata.title?.toString().orEmpty(), uri)
    }
    private fun scheduleSleep(minutes: Int?) {
        sleepHandler.removeCallbacksAndMessages(null)
        _sleepAtTrackEnd.value = false; sleepMediaId = null
        if (minutes == null || minutes <= 0) { _sleepTimerEnd.value = null; return }
        val millis = minutes.coerceAtMost(1440) * 60_000L
        _sleepTimerEnd.value = System.currentTimeMillis() + millis
        // Runs in the playback service, independent of screen/ViewModel coroutine lifetimes.
        sleepHandler.postDelayed({ stopForSleep() }, millis)
    }
    private fun scheduleTrackEnd() {
        val current = player ?: return
        val id = current.currentMediaItem?.mediaId ?: return
        if (isRadioMediaId(id) || current.duration <= 0) return
        scheduleSleep(null)
        sleepMediaId = id; _sleepAtTrackEnd.value = true
    }
    private fun stopForSleep() {
        sleepHandler.removeCallbacksAndMessages(null)
        _sleepTimerEnd.value = null; _sleepAtTrackEnd.value = false; sleepMediaId = null
        recoveryHandler.removeCallbacksAndMessages(null)
        player?.let { current ->
            rememberPosition(current)
            current.pause()
            if (isRadioMediaId(current.currentMediaItem?.mediaId)) current.stop()
        }
    }
    private val radioListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            recoveryHandler.removeCallbacksAndMessages(null); retryAttempt = 0
            val oldId = previousItem?.mediaId
            previousItem = mediaItem
            if (_sleepAtTrackEnd.value && oldId == sleepMediaId) {
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO || reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) stopForSleep()
                else if (mediaItem?.mediaId != sleepMediaId) { _sleepAtTrackEnd.value = false; sleepMediaId = null }
            }
            val speech = mediaItem?.mediaMetadata?.extras?.getBoolean("spoken_audio", false) == true
            player?.setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                .setContentType(if (speech) C.AUDIO_CONTENT_TYPE_SPEECH else C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
        }
        override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
            if (oldPosition.mediaItem?.mediaId != newPosition.mediaItem?.mediaId) {
                oldPosition.mediaItem?.let { savePosition(it, oldPosition.positionMs, it.mediaMetadata.extras?.getLong("duration_ms") ?: 0L) }
            }
        }
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED && _sleepAtTrackEnd.value) stopForSleep()
        }
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) { retryAttempt = 0; recoveryHandler.removeCallbacksAndMessages(null) }
        }
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            val current = player ?: return
            if (!playWhenReady) {
                rememberPosition(current)
                recoveryHandler.removeCallbacksAndMessages(null); retryAttempt = 0
                if (isRadioMediaId(current.currentMediaItem?.mediaId)) current.stop()
            } else if (isRadioMediaId(current.currentMediaItem?.mediaId) && current.playbackState == Player.STATE_IDLE) {
                current.seekToDefaultPosition(); current.prepare()
            }
        }
        override fun onPlayerError(error: PlaybackException) {
            val current = player ?: return
            val key = current.currentMediaItem?.mediaId
            if (!isRadioMediaId(key) || !current.playWhenReady) return
            recoveryHandler.removeCallbacksAndMessages(null)
            val delayMs = radioRetryDelayMs(retryAttempt++)
            if (delayMs == null) { current.pause(); return }
            recoveryHandler.postDelayed({
                if (current.currentMediaItem?.mediaId == key && current.playWhenReady) {
                    current.seekToDefaultPosition(); current.prepare()
                }
            }, delayMs)
        }
    }
    override fun onCreate() {
        super.onCreate()
        setMediaNotificationProvider(DefaultMediaNotificationProvider.Builder(this).build().apply {
            setSmallIcon(R.drawable.ic_stat_musiqay)
        })
        val http = DefaultHttpDataSource.Factory().setUserAgent("Musiqay/1.4.2 (Android)")
            .setConnectTimeoutMs(10_000).setReadTimeoutMs(12_000).setAllowCrossProtocolRedirects(true)
        val exoPlayer = ExoPlayer.Builder(this)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setMediaSourceFactory(DefaultMediaSourceFactory(DefaultDataSource.Factory(this, http)))
            .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
            .setHandleAudioBecomingNoisy(true).build()
        val activity = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        // The official Quran HLS includes static video; select its audio track only.
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true).build()
        activeService = this; activePlayer = exoPlayer; player = exoPlayer
        historyHandler.postDelayed(historyTick, 10_000)
        exoPlayer.addListener(radioListener)
        mediaSession = MediaSession.Builder(this, exoPlayer).setSessionActivity(activity).build()
    }
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession
    override fun onDestroy() {
        player?.let(::rememberPosition)
        recoveryHandler.removeCallbacksAndMessages(null)
        historyHandler.removeCallbacksAndMessages(null)
        sleepHandler.removeCallbacksAndMessages(null)
        _sleepTimerEnd.value = null; _sleepAtTrackEnd.value = false
        activeService = null
        mediaSession?.release(); mediaSession = null; activePlayer = null
        player?.release(); player = null
        super.onDestroy()
    }
}
