package com.musiqay.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.musiqay.app.data.PlaylistEntity
import com.musiqay.app.data.Song
import com.musiqay.app.playback.NowPlayingState
import com.musiqay.app.util.formatDuration

@Composable
fun AlbumArtwork(
    model: Any?,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 14,
    prominentPlaceholder: Boolean = false
) {
    val shape = RoundedCornerShape(cornerRadius.dp)
    val imageFailed = remember(model) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (model != null && !imageFailed.value) {
            AsyncImage(
                model = model,
                contentDescription = null,
                onError = { imageFailed.value = true },
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            val containerSize = if (prominentPlaceholder) 112.dp else 38.dp
            val iconSize = if (prominentPlaceholder) 56.dp else 22.dp

            Box(
                modifier = Modifier
                    .size(containerSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}

@Composable
fun SectionTitle(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (action != null && onAction != null) TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable
fun SongRow(
    song: Song,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDeleteFromDevice: () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var menu by remember { mutableStateOf(false) }
    val rowShape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .70f),
                        MaterialTheme.colorScheme.surface.copy(alpha = .82f)
                    )
                ),
                rowShape
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = .72f),
                rowShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumArtwork(song.artworkUri, Modifier.size(54.dp), 10)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
            Text(
                "${song.artist} • ${formatDuration(song.durationMs)}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        trailingContent?.invoke()
        IconButton(onClick = onToggleFavorite) {
            Icon(
                if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = "المفضلة",
                tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = "المزيد") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("تشغيل التالي") },
                    leadingIcon = { Icon(Icons.Rounded.PlayArrow, null) },
                    onClick = { menu = false; onPlayNext() }
                )
                DropdownMenuItem(
                    text = { Text("إضافة إلى قائمة الانتظار") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.QueueMusic, null) },
                    onClick = { menu = false; onAddToQueue() }
                )
                DropdownMenuItem(
                    text = { Text("إضافة إلى قائمة تشغيل") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null) },
                    onClick = { menu = false; onAddToPlaylist() }
                )

                DropdownMenuItem(
                    text = {
                        Text(
                            "حذف من الجهاز",
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = {
                        menu = false
                        onDeleteFromDevice()
                    }
                )
            }
        }
    }
}

@Composable
fun MiniPlayer(
    state: NowPlayingState,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.mediaId == null) return
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .shadow(12.dp, shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF142144).copy(alpha = .96f),
                        Color(0xFF0C1530).copy(alpha = .96f)
                    )
                ),
                shape
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = .58f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = .22f)
                    )
                ),
                shape
            )
            .clickable(onClick = onOpen)
    ) {
        Column {
            Row(
                Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlbumArtwork(
                    state.artworkUri,
                    Modifier.size(52.dp),
                    14
                )

                Spacer(Modifier.width(11.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = state.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Text(
                        text = state.artist.ifBlank { "فنان غير معروف" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (state.isPlaying) "إيقاف مؤقت" else "تشغيل"
                    )
                }

                IconButton(onClick = onNext) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = "التالي"
                    )
                }
            }

            val progress =
                if (state.durationMs > 0L)
                    (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
                else
                    0f

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
            )
        }
    }
}

@Composable
fun PlaylistPickerDialog(
    playlists: List<PlaylistEntity>,
    onDismiss: () -> Unit,
    onPick: (Long) -> Unit,
    onCreateRequested: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة إلى قائمة تشغيل") },
        text = {
            Column {
                if (playlists.isEmpty()) Text("لا توجد قوائم تشغيل بعد.")
                else playlists.forEach { playlist ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onPick(playlist.id) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.QueueMusic, null)
                        Spacer(Modifier.width(10.dp))
                        Text(playlist.name, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth().clickable { onCreateRequested() }.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text("إنشاء قائمة جديدة", color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}
