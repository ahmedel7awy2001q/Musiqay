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
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.animation.core.tween
import com.musiqay.app.ui.theme.LocalReduceMotion
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.material3.TextButton
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
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
import com.musiqay.app.ui.theme.premiumOutlineBrush
import com.musiqay.app.ui.theme.premiumAmbientSurface
import com.musiqay.app.ui.theme.premiumPanelBrush
import com.musiqay.app.util.formatDuration
import com.musiqay.app.util.displayTitle
import com.musiqay.app.util.displayArtist

@Composable
fun AlbumArtwork(
    model: Any?,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 14,
    prominentPlaceholder: Boolean = false,
    logo: Boolean = false,
    logoBackground: Color = Color.White,
    placeholderTitle: String = ""
) {
    val shape = RoundedCornerShape(cornerRadius.dp)
    val imageFailed = remember(model) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(shape)
            .background(if (logo) logoBackground else MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (model != null && !imageFailed.value) {
            AsyncImage(
                model = model,
                contentDescription = null,
                onError = { imageFailed.value = true },
                modifier = Modifier.matchParentSize(),
                contentScale = if (logo) ContentScale.Fit else ContentScale.Crop
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
                if (placeholderTitle.isNotBlank()) Text(placeholderTitle.trim().take(1), color = MaterialTheme.colorScheme.primary,
                    style = if (prominentPlaceholder) MaterialTheme.typography.displaySmall else MaterialTheme.typography.titleMedium)
                else Icon(
                    if (logo) Icons.Rounded.Radio else Icons.Rounded.MusicNote,
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
    isCurrent: Boolean = false,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDeleteFromDevice: () -> Unit,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var menu by remember { mutableStateOf(false) }
    val rowShape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 1.dp)
            .background(
                if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .34f)
                else Color.Transparent,
                rowShape
            )
            .border(
                if (isCurrent) .7.dp else 0.dp,
                if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = .46f) else Color.Transparent,
                rowShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            AlbumArtwork(song.artworkUri, Modifier.size(42.dp), 10, placeholderTitle = displayTitle(song.title))
            if (isCurrent) {
                Box(
                    Modifier
                        .size(17.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.GraphicEq,
                        contentDescription = if (isPlaying) "قيد التشغيل" else "الملف الحالي",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(
                displayTitle(song.title),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                listOf(displayArtist(song.artist), formatDuration(song.durationMs)).filter { it.isNotBlank() }.joinToString(" • "),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(40.dp)) {
            Icon(
                if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = if (isFavorite) "إزالة من المفضلة" else "إضافة إلى المفضلة",
                tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(21.dp)
            )
        }
        Box {
            IconButton(onClick = { menu = true }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "المزيد", modifier = Modifier.size(21.dp))
            }
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
                if (onRemoveFromPlaylist != null) DropdownMenuItem(
                    text = { Text("إزالة من هذه القائمة") },
                    leadingIcon = { Icon(Icons.Rounded.Remove, null) },
                    onClick = { menu = false; onRemoveFromPlaylist() }
                )
                DropdownMenuItem(
                    text = { Text("حذف من الجهاز", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
                    onClick = { menu = false; onDeleteFromDevice() }
                )
            }
        }
    }
}

@Composable
fun PlaybackMiniPlayer(vm: MusicViewModel, onOpen: () -> Unit, onToggle: () -> Unit, onNext: () -> Unit) {
    val state by vm.player.state.collectAsStateWithLifecycle()
    MiniPlayer(state, onOpen, onToggle, onNext)
}

@Composable
fun MiniPlayer(
    state: NowPlayingState,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.hasMedia) return
    val shape = RoundedCornerShape(16.dp)
    val miniElevation by animateDpAsState(
        targetValue = if (state.isPlaying) 1.5.dp else .5.dp,
        animationSpec = tween(if (LocalReduceMotion.current) 0 else 180),
        label = "miniElevation"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .shadow(miniElevation, shape)
            .background(premiumPanelBrush(), shape)
            .border(.4.dp, premiumOutlineBrush(), shape)
            .clickable(onClickLabel = "فتح المشغل", onClick = onOpen)
            .semantics { contentDescription = "فتح المشغل" }
    ) {
        Column {
            val progress = if (state.durationMs > 0L)
                (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f) else 0f
            if (!state.isRadio) LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .42f)
            )
            Row(
                Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlbumArtwork(
                    state.artworkUri,
                    Modifier.size(38.dp),
                    10,
                    logo = state.isRadio,
                    logoBackground = radioLogoBackground(state.radioStationId)
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (state.isRadio) state.title else displayTitle(state.title),
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (state.isRadio && state.wantsPlayback) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = .14f)
                            ) {
                                Text(
                                    "LIVE",
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Text(
                        text = if (state.isRadio) state.artist else displayArtist(state.artist).ifBlank { "ملف صوتي محلي" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledIconButton(
                    onClick = onToggle,
                    modifier = Modifier.size(44.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (state.isRadio && state.wantsPlayback) Icons.Rounded.Stop
                            else if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (state.isRadio && state.wantsPlayback) "إيقاف البث"
                            else if (state.isPlaying) "إيقاف مؤقت" else "تشغيل",
                        modifier = Modifier.size(22.dp)
                    )
                }
                if (!state.isRadio) IconButton(onClick = onNext, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "التالي", modifier = Modifier.size(22.dp))
                }
            }
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
