package com.musiqay.app.ui.screens

import android.content.Intent
import android.media.audiofx.AudioEffect
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.rounded.Share
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF071022),
                        Color(0xFF0B1430),
                        Color(0xFF080D1B)
                    )
                )
            )
            .navigationBarsPadding()
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp)
                .size(310.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = .22f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = .10f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "رجوع")
                }

                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "قيد التشغيل الآن",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 21.sp
                    )
                    if (queue.isNotEmpty()) {
                        Text(
                            "${queue.size} في قائمة الانتظار",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = { showQueue = true }) {
                    Icon(Icons.Rounded.MoreVert, "المزيد")
                }
            }

            Spacer(Modifier.height(26.dp))

            val infoShape = RoundedCornerShape(30.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(164.dp)
                    .shadow(18.dp, infoShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF17234A).copy(alpha = .94f),
                                Color(0xFF101A39).copy(alpha = .92f),
                                Color(0xFF131B32).copy(alpha = .95f)
                            )
                        ),
                        infoShape
                    )
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = .70f),
                                Color.White.copy(alpha = .20f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = .32f)
                            )
                        ),
                        infoShape
                    )
                    .padding(horizontal = 22.dp, vertical = 16.dp)
            ) {
                Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = .34f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = .08f)
                                    )
                                ),
                                CircleShape
                            )
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = .45f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(Modifier.height(9.dp))

                    Text(
                        state.title,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 24.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(7.dp))

                    Text(
                        state.artist.ifBlank { "فنان غير معروف" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )

                    if (state.album.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            state.album,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .78f),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                if (state.mediaId != null) {
                    IconButton(
                        onClick = { vm.toggleFavorite(state.mediaId!!) },
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Icon(
                            if (state.mediaId in favoriteIds) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            "المفضلة",
                            tint = if (state.mediaId in favoriteIds)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Column(Modifier.fillMaxWidth()) {
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

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatDuration(shown.toLong()), style = MaterialTheme.typography.bodySmall)
                    Text(formatDuration(state.durationMs), style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(16.dp))

            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PremiumControlButton(
                        active = state.shuffleEnabled,
                        size = 52,
                        onClick = vm.player::toggleShuffle
                    ) {
                        Icon(Icons.Rounded.Shuffle, "عشوائي")
                    }

                    PremiumControlButton(size = 62, onClick = vm.player::previous) {
                        Icon(
                            Icons.Rounded.SkipPrevious,
                            "السابق",
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    FilledIconButton(
                        onClick = vm.player::togglePlayPause,
                        modifier = Modifier
                            .size(88.dp)
                            .shadow(22.dp, CircleShape),
                        shape = CircleShape
                    ) {
                        Icon(
                            if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (state.isPlaying) "إيقاف مؤقت" else "تشغيل",
                            modifier = Modifier.size(46.dp)
                        )
                    }

                    PremiumControlButton(size = 62, onClick = vm.player::next) {
                        Icon(
                            Icons.Rounded.SkipNext,
                            "التالي",
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    PremiumControlButton(
                        active = state.repeatMode != Player.REPEAT_MODE_OFF,
                        size = 52,
                        onClick = vm.player::cycleRepeatMode
                    ) {
                        Icon(
                            if (state.repeatMode == Player.REPEAT_MODE_ONE)
                                Icons.Rounded.RepeatOne
                            else
                                Icons.Rounded.Repeat,
                            "تكرار"
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            val currentSleepEnd = sleepEnd
            val timerLabel = if (currentSleepEnd != null) {
                val remainingMinutes =
                    ((currentSleepEnd - System.currentTimeMillis()) / 60_000L).coerceAtLeast(0L)
                if (remainingMinutes > 0L) "متبقي \$remainingMinutes د" else "المؤقت مفعل"
            } else {
                "مؤقت النوم"
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GlassActionButton(Icons.Rounded.Share, "مشاركة") {
                    val shareText = buildString {
                        append(state.title)
                        if (state.artist.isNotBlank()) append(" - ").append(state.artist)
                    }
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    runCatching {
                        context.startActivity(Intent.createChooser(shareIntent, "مشاركة"))
                    }
                }

                GlassActionButton(Icons.Rounded.Timer, timerLabel) {
                    showTimer = true
                }

                GlassActionButton(Icons.Rounded.GraphicEq, "المؤثرات") {
                    val effectIntent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                        putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                        putExtra(AudioEffect.EXTRA_AUDIO_SESSION, 0)
                    }
                    runCatching { context.startActivity(effectIntent) }.onFailure {
                        runCatching { context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS)) }
                    }
                }

                GlassActionButton(Icons.AutoMirrored.Rounded.PlaylistAdd, "القائمة") {
                    showPlaylistPicker = currentSong != null
                }
            }

            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun GlassActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(78.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 72.dp, height = 66.dp)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .88f),
                            MaterialTheme.colorScheme.surface.copy(alpha = .58f)
                        )
                    ),
                    shape
                )
                .border(
                    1.dp,
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = .42f),
                            Color.White.copy(alpha = .08f)
                        )
                    ),
                    shape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(27.dp)
            )
        }
        Spacer(Modifier.height(7.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PremiumControlButton(
    size: Int,
    active: Boolean = false,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(
                if (active)
                    MaterialTheme.colorScheme.primary.copy(alpha = .20f)
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .36f),
                CircleShape
            )
            .border(
                1.dp,
                if (active)
                    MaterialTheme.colorScheme.primary.copy(alpha = .72f)
                else
                    MaterialTheme.colorScheme.primary.copy(alpha = .20f),
                CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
