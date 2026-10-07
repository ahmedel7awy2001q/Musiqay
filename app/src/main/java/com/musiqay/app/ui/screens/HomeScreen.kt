package com.musiqay.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musiqay.app.data.Song
import com.musiqay.app.ui.*
import com.musiqay.app.util.displayTitle
import com.musiqay.app.util.displayArtist
import com.musiqay.app.ui.theme.*

@Composable
fun HomeScreen(
    vm: MusicViewModel,
    songs: List<Song>,
    favoriteCount: Int,
    onSong: (Song) -> Unit,
    onSettings: () -> Unit,
    onFavorites: () -> Unit,
    onAllSongs: () -> Unit,
    onArtists: () -> Unit,
    onAlbums: () -> Unit,
    onFolders: () -> Unit,
    onRadio: () -> Unit,
    onResume: () -> Unit,
    onPlayAll: () -> Unit,
    audioPermission: Boolean = true,
    onGrantAudio: () -> Unit = {}
) {
    val recent = remember(songs) { songs.sortedByDescending { it.dateAddedSeconds }.take(12) }
    val counts = remember(songs) { Triple(songs.map { it.artist }.distinct().size,
        songs.map { if (it.albumId > 0) "${it.albumId}" else "${it.artist}/${it.album}" }.distinct().size,
        songs.map { it.folderKey }.distinct().size) }
    val status by vm.libraryStatus.collectAsStateWithLifecycle()
    val history by vm.listeningPositions.collectAsStateWithLifecycle()
    val radioCatalog by vm.radioCatalog.collectAsStateWithLifecycle()
    val radioFavorites by vm.radioFavorites.collectAsStateWithLifecycle()
    val unfinished = remember(history, songs) {
        val byId = songs.associateBy { it.id }
        history.values.sortedByDescending { it.updatedAt }.mapNotNull { entry ->
            byId[entry.mediaId]?.takeIf { vm.listening.resumeFor(it) > 0 }?.let { it to entry.positionMs }
        }.take(3)
    }
    val favoriteRadios = remember(radioCatalog.stations, radioFavorites) { radioCatalog.stations.filter { it.id in radioFavorites }.take(4) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(premiumScreenBrush()).premiumAmbientSurface().statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("موسيقاي", style = MaterialTheme.typography.headlineMedium)
                    Text("استماعك... بطريقتك", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalIconButton(onClick = onSettings, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.Settings, "الإعدادات", modifier = Modifier.size(20.dp))
                }
            }
        }
        item { ResumeCard(vm, onResume, onPlayAll, audioPermission && songs.isNotEmpty()) }
        if (!audioPermission) item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("الراديو متاح الآن. اسمح بقراءة ملفات الصوت لعرض ملفات الهاتف الصوتية.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = onGrantAudio, modifier = Modifier.fillMaxWidth()) { Text("إظهار ملفات الهاتف الصوتية") }
            }
        }
        if (status.loading) item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("جارٍ تحديث مكتبتك…", style = MaterialTheme.typography.bodySmall)
            }
        }
        status.error?.takeIf { audioPermission }?.let { error -> item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(error, Modifier.weight(1f), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = vm::refreshLibrary) { Text("إعادة المحاولة") }
            }
        } }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("مكتبتك", "${songs.size} ملف صوتي", onAllSongs)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LibraryShortcut("المفضلة", "$favoriteCount ملف صوتي", Icons.Rounded.Favorite, onFavorites, Modifier.weight(1f))
                    LibraryShortcut("الفنانون", "${counts.first} فنان", Icons.Rounded.Person, onArtists, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LibraryShortcut("الألبومات", "${counts.second} ألبوم", Icons.Rounded.Album, onAlbums, Modifier.weight(1f))
                    LibraryShortcut("المجلدات", "${counts.third} مجلد", Icons.Rounded.Folder, onFolders, Modifier.weight(1f))
                }
            }
        }
        if (unfinished.isNotEmpty()) {
            item { SectionTitle("استكمل تسجيلاتك") }
            items(unfinished, key = { "resume:${it.first.id}" }) { (song, position) ->
                Row(Modifier.fillMaxWidth().clickable { onSong(song) }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    AlbumArtwork(song.artworkUri, Modifier.size(44.dp), placeholderTitle = displayTitle(song.title))
                    Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                        Text(displayTitle(song.title), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("استكمال من ${com.musiqay.app.util.formatDuration(position)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Icon(Icons.Rounded.PlayArrow, "استكمال")
                }
            }
        }
        if (favoriteRadios.isNotEmpty()) {
            item { SectionTitle("محطاتك المفضلة", "الراديو", onRadio) }
            items(favoriteRadios, key = { "favorite-radio:${it.id}" }) { station ->
                TextButton(onClick = { vm.playRadio(station); onRadio() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Radio, null); Spacer(Modifier.width(8.dp)); Text(station.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (recent.isNotEmpty()) {
            item { SectionTitle("مضاف حديثًا", "عرض الكل", onAllSongs) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(recent, key = { it.id }, contentType = { "recentSong" }) { song ->
                        Column(Modifier.width(112.dp).clickable { onSong(song) }) {
                            AlbumArtwork(song.artworkUri, Modifier.size(112.dp), 18)
                            Spacer(Modifier.height(6.dp))
                            Text(displayTitle(song.title), maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                            Text(displayArtist(song.artist), maxLines = 1, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        } else if (!status.loading && status.error == null) item {
            Text("لم نعثر على ملفات صوتية. أضف ملفات صوت إلى الهاتف أو راجع مرشحات المكتبة في الإعدادات.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ResumeCard(vm: MusicViewModel, onOpen: () -> Unit, onPlayAll: () -> Unit, hasSongs: Boolean) {
    val state by vm.player.summary.collectAsStateWithLifecycle()
    val shape = RoundedCornerShape(16.dp)
    Column(Modifier.fillMaxWidth().background(premiumPanelBrush(), shape)
        .border(.45.dp, premiumOutlineBrush(), shape).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.hasMedia) {
            Text(if (state.isPlaying) "تستمع الآن" else "استكمل الاستماع", color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth().clickable(onClick = onOpen), verticalAlignment = Alignment.CenterVertically) {
                AlbumArtwork(state.artworkUri, Modifier.size(52.dp), 13, logo = state.isRadio,
                    logoBackground = radioLogoBackground(state.radioStationId))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(if (state.isRadio) state.title else displayTitle(state.title), maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                    Text(displayArtist(state.artist).ifBlank { "ملف صوتي محلي" }, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledIconButton(onClick = vm.player::togglePlayPause, modifier = Modifier.size(48.dp)) {
                    Icon(if (state.isRadio && state.wantsPlayback) Icons.Rounded.Stop else if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        if (state.isRadio && state.wantsPlayback) "إيقاف البث" else if (state.isPlaying) "إيقاف مؤقت" else "استكمال التشغيل")
                }
            }
        } else {
            Text("استماع لكل لحظة", style = MaterialTheme.typography.titleLarge)
            Text("ملفاتك ومحطاتك في مكان واحد", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!state.hasMedia) FilledTonalButton(
            onClick = onPlayAll,
            enabled = hasSongs,
            modifier = Modifier.fillMaxWidth().height(42.dp),
            shape = RoundedCornerShape(13.dp)
        ) {
            Icon(Icons.Rounded.PlayArrow, null, Modifier.size(20.dp)); Spacer(Modifier.width(7.dp)); Text("تشغيل الكل")
        }
    }
}

@Composable
private fun LibraryShortcut(title: String, count: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier
            .background(premiumPanelBrush(), shape)
            .border(.4.dp, premiumOutlineBrush(), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(36.dp).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = .78f), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp))
        }
        Column(Modifier.weight(1f).padding(start = 9.dp)) {
            Text(title, fontWeight = FontWeight.Bold, maxLines = 1, style = MaterialTheme.typography.bodyLarge)
            Text(count, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
