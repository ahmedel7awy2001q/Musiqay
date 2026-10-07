package com.musiqay.app.ui.screens

import android.content.ClipData
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.musiqay.app.data.PlaylistEntity
import com.musiqay.app.data.Song
import com.musiqay.app.playback.NowPlayingState
import com.musiqay.app.playback.PlaybackService
import com.musiqay.app.ui.*
import com.musiqay.app.ui.theme.*
import com.musiqay.app.util.formatDuration
import com.musiqay.app.util.displayTitle
import com.musiqay.app.util.displayArtist
import com.musiqay.app.util.displayAlbum
import androidx.compose.foundation.clickable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(vm: MusicViewModel, songs: List<Song>, favoriteIds: Set<Long>,
    playlists: List<PlaylistEntity>, onBack: () -> Unit) {
    val state by vm.player.summary.collectAsStateWithLifecycle()
    val queue by vm.player.queue.collectAsStateWithLifecycle()
    val sleepEnd by vm.player.sleepTimerEnd.collectAsStateWithLifecycle()
    val sleepAtEnd by vm.player.sleepAtTrackEnd.collectAsStateWithLifecycle()
    val currentSong = remember(state.mediaId, songs) { songs.firstOrNull { it.id == state.mediaId } }
    var showQueue by rememberSaveableBoolean()
    var showTimer by rememberSaveableBoolean()
    var showPicker by rememberSaveableBoolean()
    var showCreate by rememberSaveableBoolean()
    var playlistName by remember { mutableStateOf("") }
    val context = LocalContext.current

    val openEffects: () -> Unit = {
        val session = PlaybackService.audioSessionId
        if (session > 0) runCatching {
            context.startActivity(Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                .putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                .putExtra(AudioEffect.EXTRA_AUDIO_SESSION, session)
                .putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC))
        }.onFailure { Toast.makeText(context, "لوحة المؤثرات غير متاحة على هذا الهاتف", Toast.LENGTH_SHORT).show() }
        else Toast.makeText(context, "ابدأ تشغيل أغنية لفتح المؤثرات", Toast.LENGTH_SHORT).show()
    }
    val shareSong: () -> Unit = {
        currentSong?.let { song -> runCatching {
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, song.uri)
                clipData = ClipData.newUri(context.contentResolver, song.title, song.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "مشاركة الأغنية"))
        }.onFailure { Toast.makeText(context, "تعذرت مشاركة الملف", Toast.LENGTH_SHORT).show() } }
        Unit
    }

    if (showTimer) SleepTimerDialog(sleepEnd != null, vm.player::setSleepTimer,
        onAtEnd = if (currentSong != null) vm.player::setSleepAtTrackEnd else null, atEndActive = sleepAtEnd) { showTimer = false }
    if (showPicker && currentSong != null) PlaylistPickerDialog(playlists, onDismiss = { showPicker = false },
        onPick = { vm.addToPlaylist(it, currentSong.id); showPicker = false },
        onCreateRequested = { showPicker = false; showCreate = true; playlistName = "" })
    if (showCreate && currentSong != null) AlertDialog(onDismissRequest = { showCreate = false }, title = { Text("قائمة تشغيل جديدة") },
        text = { OutlinedTextField(playlistName, { playlistName = it.take(100) }, label = { Text("اسم القائمة") }, singleLine = true) },
        confirmButton = { Button(enabled = playlistName.isNotBlank(), onClick = { vm.createPlaylist(playlistName, currentSong.id); showCreate = false }) { Text("إنشاء") } },
        dismissButton = { TextButton(onClick = { showCreate = false }) { Text("إلغاء") } })
    if (showQueue) QueueSheet(vm, queue, state.queueIndex, onDismiss = { showQueue = false })

    BoxWithConstraints(Modifier.fillMaxSize().background(premiumScreenBrush()).premiumAmbientSurface()
        .statusBarsPadding().navigationBarsPadding()) {
        val landscape = maxWidth > maxHeight && maxWidth >= 600.dp
        val artSize = if (landscape) minOf(maxHeight - 90.dp, maxWidth * .34f).coerceAtLeast(80.dp)
            else minOf(maxWidth - 76.dp, maxHeight * .26f).coerceIn(96.dp, 218.dp)
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "رجوع", modifier = Modifier.size(21.dp))
                }
                Text("قيد التشغيل", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                FilledTonalIconButton(onClick = { showQueue = true }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.QueueMusic, "قائمة الانتظار", modifier = Modifier.size(21.dp))
                }
            }
            if (landscape) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(.40f), contentAlignment = Alignment.Center) { AlbumArtwork(state.artworkUri, Modifier.size(artSize), 28, true) }
                    Column(Modifier.weight(.60f).verticalScroll(rememberScrollState()).padding(start = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        PlayerDetails(state, favoriteIds, vm)
                        PlayerProgress(vm)
                        PlayerControls(state, vm)
                        ListeningTools(vm, currentSong)
                        PlayerActions(sleepEnd, sleepAtEnd, currentSong != null, onTimer = { showTimer = true }, onPlaylist = { showPicker = true },
                            onEffects = openEffects, onShare = shareSong)
                    }
                }
            } else {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    AlbumArtwork(state.artworkUri, Modifier.size(artSize).border(.4.dp, premiumOutlineBrush(), RoundedCornerShape(22.dp)), 22, true)
                    PlayerDetails(state, favoriteIds, vm)
                    PlayerProgress(vm)
                    PlayerControls(state, vm)
                        ListeningTools(vm, currentSong)
                    PlayerActions(sleepEnd, sleepAtEnd, currentSong != null, onTimer = { showTimer = true }, onPlaylist = { showPicker = true },
                        onEffects = openEffects, onShare = shareSong)
                }
            }
        }
    }
}

@Composable
private fun rememberSaveableBoolean() = androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }

@Composable
private fun PlayerDetails(state: NowPlayingState, favorites: Set<Long>, vm: MusicViewModel) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(displayTitle(state.title), modifier = Modifier.clickable { android.widget.Toast.makeText(vm.getApplication(), state.title, android.widget.Toast.LENGTH_LONG).show() }, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text(displayArtist(state.artist).ifBlank { "ملف صوتي محلي" }, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (displayAlbum(state.album).isNotBlank()) Text(displayAlbum(state.album), maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(enabled = state.mediaId != null, onClick = { state.mediaId?.let(vm::toggleFavorite) }) {
            Icon(if (state.mediaId in favorites) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                if (state.mediaId in favorites) "إزالة من المفضلة" else "إضافة إلى المفضلة", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerProgress(vm: MusicViewModel) {
    val state by vm.player.state.collectAsStateWithLifecycle()
    var drag by remember(state.mediaId) { mutableStateOf<Float?>(null) }
    val maximum = state.durationMs.coerceAtLeast(1L).toFloat()
    val shown = (drag ?: state.positionMs.toFloat()).coerceIn(0f, maximum)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(Modifier.fillMaxWidth()) {
            Slider(value = shown, valueRange = 0f..maximum, enabled = state.mediaId != null && state.durationMs > 0,
                onValueChange = { drag = it }, onValueChangeFinished = { drag?.let { vm.player.seekTo(it.toLong()) }; drag = null },
                thumb = { Box(Modifier.size(12.dp).background(MaterialTheme.colorScheme.primary, CircleShape)) },
                track = { SliderDefaults.Track(it, modifier = Modifier.height(4.dp)) })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDuration(shown.toLong()), style = MaterialTheme.typography.bodySmall)
                Text(formatDuration(state.durationMs), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun PlayerControls(state: NowPlayingState, vm: MusicViewModel) {
    val reduced = LocalReduceMotion.current
    val scale = if (state.isPlaying && !reduced) rememberInfiniteTransition(label = "playPulse").animateFloat(
        initialValue = 1f, targetValue = 1.012f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "playScale") else rememberUpdatedState(1f)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = vm.player::toggleShuffle, enabled = state.mediaId != null, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.Shuffle, if (state.shuffleEnabled) "إلغاء التشغيل العشوائي" else "تشغيل عشوائي",
                    tint = if (state.shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = vm.player::previous, enabled = state.mediaId != null, modifier = Modifier.size(48.dp)) { Icon(Icons.Rounded.SkipPrevious, "السابق", modifier = Modifier.size(32.dp)) }
            FilledIconButton(onClick = vm.player::togglePlayPause, enabled = state.mediaId != null,
                modifier = Modifier.size(64.dp).graphicsLayer { scaleX = scale.value; scaleY = scale.value }) {
                Icon(if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    if (state.isPlaying) "إيقاف مؤقت" else "تشغيل", modifier = Modifier.size(33.dp))
            }
            IconButton(onClick = vm.player::next, enabled = state.mediaId != null, modifier = Modifier.size(48.dp)) { Icon(Icons.Rounded.SkipNext, "التالي", modifier = Modifier.size(32.dp)) }
            IconButton(onClick = vm.player::cycleRepeatMode, enabled = state.mediaId != null, modifier = Modifier.size(48.dp)) {
                Icon(if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    when (state.repeatMode) { Player.REPEAT_MODE_ONE -> "تكرار أغنية واحدة"; Player.REPEAT_MODE_ALL -> "تكرار القائمة"; else -> "التكرار متوقف" },
                    tint = if (state.repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PlayerActions(sleepEnd: Long?, sleepAtEnd: Boolean, canShare: Boolean, onTimer: () -> Unit,
    onPlaylist: () -> Unit, onEffects: () -> Unit, onShare: () -> Unit) {
    val remaining by produceState(0L, sleepEnd) {
        while (sleepEnd != null) { value = ((sleepEnd - System.currentTimeMillis() + 59_999) / 60_000).coerceAtLeast(0); delay(15_000) }
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = onTimer, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)) {
                Icon(Icons.Rounded.Timer, null, Modifier.size(20.dp)); Spacer(Modifier.width(6.dp))
                Text(if (sleepAtEnd) "حتى نهاية المقطع" else if (sleepEnd == null) "مؤقت النوم" else "متبقي $remaining د", maxLines = 2)
            }
            OutlinedButton(onClick = onEffects, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)) {
                Icon(Icons.Rounded.GraphicEq, null, Modifier.size(20.dp)); Spacer(Modifier.width(6.dp)); Text("المؤثرات")
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onPlaylist, enabled = canShare, modifier = Modifier.weight(1f)) {
                Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null, Modifier.size(20.dp)); Spacer(Modifier.width(6.dp)); Text("إضافة لقائمة")
            }
            TextButton(onClick = onShare, enabled = canShare, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.Share, null, Modifier.size(20.dp)); Spacer(Modifier.width(6.dp)); Text("مشاركة الملف")
            }
        }
    }
}

private fun queueKey(item: MediaItem, index: Int): String = item.mediaMetadata.extras?.getString("queue_entry_id") ?: "${item.mediaId}-$index"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueSheet(vm: MusicViewModel, queue: List<MediaItem>, currentIndex: Int, onDismiss: () -> Unit) {
    val currentQueue by rememberUpdatedState(queue)
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmClear by remember { mutableStateOf(false) }
    val stepPx = with(LocalDensity.current) { 76.dp.toPx() }
    if (confirmClear) AlertDialog(onDismissRequest = { confirmClear = false }, title = { Text("تفريغ قائمة الانتظار") },
        text = { Text("سيتم إيقاف التشغيل وإزالة الأغاني من الانتظار. ملفاتك وقوائم التشغيل ستبقى محفوظة.") },
        confirmButton = { TextButton(onClick = { vm.player.clearQueue(); confirmClear = false }) { Text("تفريغ") } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("إلغاء") } })
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("قائمة الانتظار • ${queue.size}", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                TextButton(enabled = queue.isNotEmpty(), onClick = { confirmClear = true }) { Text("تفريغ") }
            }
            Text("اضغط مطولًا على مقبض الترتيب واسحب الأغنية", Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false)) {
                itemsIndexed(queue, key = { index, item -> queueKey(item, index) }, contentType = { _, _ -> "queueItem" }) { index, item ->
                    val key = queueKey(item, index)
                    val active = currentIndex == index
                    Row(Modifier.fillMaxWidth().heightIn(min = 76.dp)
                        .background(if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .5f) else MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        AlbumArtwork(item.mediaMetadata.artworkUri, Modifier.size(48.dp), 12)
                        TextButton(onClick = { vm.player.playQueueIndex(index) }, modifier = Modifier.weight(1f)) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(item.mediaMetadata.title?.toString().orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                                Text(item.mediaMetadata.artist?.toString().orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Icon(Icons.Rounded.DragHandle, "ترتيب الأغنية", modifier = Modifier.size(48.dp)
                            .semantics { customActions = listOf(
                                CustomAccessibilityAction("تحريك لأعلى") { if (index > 0) { vm.player.moveQueueItem(index, index - 1); true } else false },
                                CustomAccessibilityAction("تحريك لأسفل") { if (index < queue.lastIndex) { vm.player.moveQueueItem(index, index + 1); true } else false }) }
                            .pointerInput(key) {
                                var accumulated = 0f
                                detectDragGesturesAfterLongPress(onDragStart = { accumulated = 0f },
                                    onDragEnd = { accumulated = 0f }, onDragCancel = { accumulated = 0f }) { change, amount ->
                                    change.consume(); accumulated += amount.y
                                    if (abs(accumulated) >= stepPx) {
                                        val from = currentQueue.indexOfFirst { it.mediaMetadata.extras?.getString("queue_entry_id") == key }
                                        val to = from + if (accumulated > 0) 1 else -1
                                        if (from >= 0 && to in currentQueue.indices) vm.player.moveQueueItem(from, to)
                                        accumulated = 0f
                                    }
                                }
                            })
                        IconButton(onClick = {
                            vm.player.removeQueueItem(index)?.let { removed -> scope.launch {
                                snackbar.currentSnackbarData?.dismiss()
                                if (snackbar.showSnackbar("أُزيلت الأغنية من الانتظار", "تراجع", duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed)
                                    vm.player.restoreQueueItem(removed)
                            } }
                        }) { Icon(Icons.Rounded.Close, "إزالة من الانتظار") }
                    }
                }
            }
            if (queue.isEmpty()) Text("قائمة الانتظار فارغة", Modifier.padding(24.dp))
            SnackbarHost(snackbar)
        }
    }
}
