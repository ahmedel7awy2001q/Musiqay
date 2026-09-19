package com.musiqay.app.ui.screens

import android.content.Intent
import android.media.audiofx.AudioEffect
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.musiqay.app.data.PlaylistEntity
import com.musiqay.app.data.Song
import com.musiqay.app.ui.AlbumArtwork
import com.musiqay.app.ui.MusicViewModel
import com.musiqay.app.ui.PlaylistPickerDialog
import com.musiqay.app.util.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    vm: MusicViewModel,
    songs: List<Song>,
    favoriteIds: Set<Long>,
    playlists: List<PlaylistEntity>,
    onBack: () -> Unit
) {
    val state by vm.player.state.collectAsStateWithLifecycle()
    val queue by vm.player.queue.collectAsStateWithLifecycle()
    val sleepEnd by vm.player.sleepTimerEnd.collectAsStateWithLifecycle()
    val currentSong = remember(state.mediaId, songs) { songs.firstOrNull { it.id == state.mediaId } }
    var dragPosition by remember { mutableStateOf<Float?>(null) }
    var showQueue by remember { mutableStateOf(false) }
    var showTimer by remember { mutableStateOf(false) }
    var customTimerMinutes by remember { mutableStateOf("") }
    var showCustomTimerDialog by remember { mutableStateOf(false) }
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var createPlaylist by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    val context = LocalContext.current

    if (showTimer) {
        AlertDialog(
            onDismissRequest = { showTimer = false },
            title = { Text("مؤقت الإيقاف") },
            text = {
                Column {
                    listOf(15, 30, 45, 60, 90).forEach { minutes ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                vm.player.setSleepTimer(minutes)
                                showTimer = false
                            }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Timer, null, tint = MaterialTheme.colorScheme.primary)
                            Text("إيقاف التشغيل بعد $minutes دقيقة", Modifier.padding(horizontal = 12.dp))
                        }
                    }
                    Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            showTimer = false
                            showCustomTimerDialog = true
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "مدة مخصصة",
                        Modifier.padding(horizontal = 12.dp)
                    )
                }
                if (sleepEnd != null) {
                        TextButton(onClick = {
                            vm.player.setSleepTimer(null)
                            showTimer = false
                        }) { Text("إلغاء المؤقت الحالي") }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showTimer = false }) { Text("إغلاق") } }
        )
    }

    if (showCustomTimerDialog) {
        AlertDialog(
            onDismissRequest = {
                showCustomTimerDialog = false
                customTimerMinutes = ""
            },
            title = { Text("مدة مخصصة") },
            text = {
                OutlinedTextField(
                    value = customTimerMinutes,
                    onValueChange = { value ->
                        customTimerMinutes = value
                            .filter { it.isDigit() }
                            .take(4)
                    },
                    label = { Text("عدد الدقائق") },
                    placeholder = { Text("مثال: 25") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    )
                )
            },
            confirmButton = {
                Button(
                    enabled = customTimerMinutes
                        .toIntOrNull()
                        ?.let { it in 1..1440 } == true,
                    onClick = {
                        val minutes = customTimerMinutes.toIntOrNull()

                        if (minutes != null && minutes in 1..1440) {
                            vm.player.setSleepTimer(minutes)
                            showCustomTimerDialog = false
                            customTimerMinutes = ""
                        }
                    }
                ) {
                    Text("تشغيل المؤقت")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCustomTimerDialog = false
                        customTimerMinutes = ""
                    }
                ) {
                    Text("إلغاء")
                }
            }
        )
    }
    if (showPlaylistPicker && currentSong != null) {
        PlaylistPickerDialog(
            playlists = playlists,
            onDismiss = { showPlaylistPicker = false },
            onPick = {
                vm.addToPlaylist(it, currentSong.id)
                showPlaylistPicker = false
            },
            onCreateRequested = {
                showPlaylistPicker = false
                createPlaylist = true
            }
        )
    }

    if (createPlaylist && currentSong != null) {
        AlertDialog(
            onDismissRequest = { createPlaylist = false },
            title = { Text("قائمة تشغيل جديدة") },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("اسم القائمة") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    enabled = newPlaylistName.isNotBlank(),
                    onClick = {
                        vm.createPlaylist(newPlaylistName, currentSong.id)
                        newPlaylistName = ""
                        createPlaylist = false
                    }
                ) { Text("إنشاء") }
            },
            dismissButton = { TextButton(onClick = { createPlaylist = false }) { Text("إلغاء") } }
        )
    }

    if (showQueue) {
        ModalBottomSheet(
            onDismissRequest = { showQueue = false },
            modifier = Modifier.navigationBarsPadding()
        ) {
            Column(Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("قائمة الانتظار", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${queue.size} أغنية", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { showQueue = false }) { Icon(Icons.Rounded.Close, "إغلاق") }
                }
                LazyColumn {
                    itemsIndexed(queue, key = { index, item -> "${item.mediaId}-$index" }) { index, item ->
                        val active = item.mediaId == state.mediaId?.toString()
                        Row(
                            Modifier.fillMaxWidth()
                                .background(if (active) MaterialTheme.colorScheme.primary.copy(alpha = .10f) else androidx.compose.ui.graphics.Color.Transparent)
                                .clickable {
                                    vm.player.playMediaId(item.mediaId.toLongOrNull() ?: return@clickable)
                                }
                                .padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AlbumArtwork(item.mediaMetadata.artworkUri, Modifier.size(48.dp), 10)
                            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                Text(
                                    item.mediaMetadata.title?.toString().orEmpty().ifBlank { "بدون عنوان" },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                )
                                Text(
                                    item.mediaMetadata.artist?.toString().orEmpty(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(enabled = index > 0, onClick = { vm.player.moveQueueItem(index, index - 1) }) {
                                Icon(Icons.Rounded.KeyboardArrowUp, "لأعلى")
                            }
                            IconButton(enabled = index < queue.lastIndex, onClick = { vm.player.moveQueueItem(index, index + 1) }) {
                                Icon(Icons.Rounded.KeyboardArrowDown, "لأسفل")
                            }
                            IconButton(onClick = { vm.player.removeQueueItem(index) }) { Icon(Icons.Rounded.Close, "إزالة") }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().navigationBarsPadding().statusBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 30.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "رجوع") }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("قيد التشغيل الآن", fontWeight = FontWeight.Bold)
                    if (queue.isNotEmpty()) Text("${queue.size} في قائمة الانتظار", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { showQueue = true }) { Icon(Icons.Rounded.MoreVert, "القائمة") }
            }
        }

        item {
            Box(Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 10.dp)) {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(1f)
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.secondary.copy(alpha = .35f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = .18f),
                                    MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        )
                ) {
                    AlbumArtwork(state.artworkUri, Modifier.fillMaxSize(), 28, prominentPlaceholder = true)
                }
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(state.title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        state.artist.ifBlank { "فنان غير معروف" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp
                    )
                    if (state.album.isNotBlank()) {
                        Text(state.album, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (state.mediaId != null) {
                    IconButton(onClick = { vm.toggleFavorite(state.mediaId!!) }) {
                        Icon(
                            if (state.mediaId in favoriteIds) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            "المفضلة",
                            tint = if (state.mediaId in favoriteIds) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 24.dp)) {
                val max = state.durationMs.coerceAtLeast(1L).toFloat()
                val shown = (dragPosition ?: state.positionMs.toFloat()).coerceIn(0f, max)
                Slider(
                    value = shown,
                    onValueChange = { dragPosition = it },
                    onValueChangeFinished = {
                        dragPosition?.let { vm.player.seekTo(it.toLong()) }
                        dragPosition = null
                    },
                    valueRange = 0f..max,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatDuration(shown.toLong()), style = MaterialTheme.typography.bodySmall)
                    Text(formatDuration(state.durationMs), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = vm.player::toggleShuffle,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Rounded.Shuffle,
                        contentDescription = "عشوائي",
                        tint = if (state.shuffleEnabled)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = vm.player::previous,
                    modifier = Modifier.size(60.dp)
                ) {
                    Icon(
                        Icons.Rounded.SkipPrevious,
                        contentDescription = "السابق",
                        modifier = Modifier.size(40.dp)
                    )
                }

                FilledIconButton(
                    onClick = vm.player::togglePlayPause,
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(
                        if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (state.isPlaying) "إيقاف مؤقت" else "تشغيل",
                        modifier = Modifier.size(44.dp)
                    )
                }

                IconButton(
                    onClick = vm.player::next,
                    modifier = Modifier.size(60.dp)
                ) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = "التالي",
                        modifier = Modifier.size(40.dp)
                    )
                }

                IconButton(
                    onClick = vm.player::cycleRepeatMode,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        if (state.repeatMode == Player.REPEAT_MODE_ONE)
                            Icons.Rounded.RepeatOne
                        else
                            Icons.Rounded.Repeat,
                        contentDescription = "تكرار",
                        tint = if (state.repeatMode != Player.REPEAT_MODE_OFF)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionButton(Icons.AutoMirrored.Rounded.PlaylistAdd, "قائمة تشغيل") { showPlaylistPicker = currentSong != null }
                ActionButton(Icons.Rounded.GraphicEq, "مؤثرات صوتية") {
                    val effectIntent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                        putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                        putExtra(AudioEffect.EXTRA_AUDIO_SESSION, 0)
                    }
                    runCatching { context.startActivity(effectIntent) }.onFailure {
                        runCatching { context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS)) }
                    }
                }
                val currentSleepEnd = sleepEnd

                val timerLabel = if (currentSleepEnd != null) {
                    val remainingMinutes =
                        ((currentSleepEnd - System.currentTimeMillis()) / 60_000L)
                            .coerceAtLeast(0L)

                    if (remainingMinutes > 0L)
                        "متبقي $remainingMinutes د"
                    else
                        "المؤقت مفعل"
                } else {
                    "مؤقت الإيقاف"
                }

                ActionButton(
                    Icons.Rounded.Timer,
                    timerLabel
                ) {
                    showTimer = true
                }
                ActionButton(Icons.AutoMirrored.Rounded.QueueMusic, "الانتظار") { showQueue = true }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .42f)),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.MusicNote, null, tint = MaterialTheme.colorScheme.primary)
                        Text("كلمات الأغنية", Modifier.weight(1f).padding(horizontal = 10.dp), fontWeight = FontWeight.Bold)
                        Icon(Icons.Rounded.ExpandMore, null)
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "موسيقاي لا يرسل اسم الأغنية إلى أي خدمة خارجية. لذلك لا يتم جلب كلمات من الإنترنت تلقائيًا، حفاظًا على الخصوصية.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(6.dp)
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(48.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = label) }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}
