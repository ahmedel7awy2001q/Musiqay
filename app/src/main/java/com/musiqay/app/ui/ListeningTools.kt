package com.musiqay.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musiqay.app.data.Song
import com.musiqay.app.util.PlaybackSpeeds
import com.musiqay.app.util.formatDuration
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListeningTools(vm: MusicViewModel, song: Song?) {
    val state by vm.player.summary.collectAsStateWithLifecycle()
    val allMarks by vm.audioBookmarks.collectAsStateWithLifecycle()
    val markCount = remember(allMarks, song?.id, song?.uri) {
        if (song == null) 0 else allMarks.count { it.mediaId == song.id && it.uri == song.uri.toString() }
    }
    var speedMenu by remember { mutableStateOf(false) }
    var marksOpen by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        TextButton(onClick = { vm.player.seekBy(-10_000) }, enabled = song != null) {
            Icon(Icons.Rounded.Replay10, null, Modifier.size(20.dp)); Text("10 ث")
        }
        Box {
            TextButton(onClick = { speedMenu = true }, enabled = song != null) {
                Text(String.format(Locale.ROOT, "%s×", state.playbackSpeed.toString()))
            }
            DropdownMenu(expanded = speedMenu, onDismissRequest = { speedMenu = false }) {
                PlaybackSpeeds.forEach { speed ->
                    DropdownMenuItem(text = { Text("${speed}×") }, onClick = { vm.player.setPlaybackSpeed(speed); speedMenu = false })
                }
            }
        }
        TextButton(onClick = { marksOpen = true }, enabled = song != null) {
            Icon(if (markCount > 0) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder, null, Modifier.size(20.dp))
            Text(if (markCount > 0) "علامات $markCount" else "علامات")
        }
        TextButton(onClick = { vm.player.seekBy(30_000) }, enabled = song != null) {
            Icon(Icons.Rounded.Forward30, null, Modifier.size(20.dp)); Text("30 ث")
        }
    }
    if (marksOpen && song != null) BookmarkSheet(vm, song) { marksOpen = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookmarkSheet(vm: MusicViewModel, song: Song, onDismiss: () -> Unit) {
    val allMarks by vm.audioBookmarks.collectAsStateWithLifecycle()
    val marks = remember(allMarks, song.id, song.uri) { allMarks.filter { it.mediaId == song.id && it.uri == song.uri.toString() } }
    var label by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("علامات التسجيل", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(label, { label = it.take(80) }, label = { Text("اسم العلامة — اختياري") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            FilledTonalButton(onClick = { vm.addBookmark(song, label); label = "" }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.BookmarkAdd, null); Spacer(Modifier.width(8.dp)); Text("حفظ الموضع الحالي")
            }
            if (marks.isEmpty()) Text("احفظ أي موضع للعودة إليه لاحقًا.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyColumn(Modifier.heightIn(max = 320.dp)) {
                items(marks, key = { it.id }) { mark ->
                    Row(Modifier.fillMaxWidth()) {
                        TextButton(onClick = { vm.player.seekTo(mark.positionMs); onDismiss() }, modifier = Modifier.weight(1f)) {
                            Text(listOf(mark.label.takeIf { it.isNotBlank() }, formatDuration(mark.positionMs)).filterNotNull().joinToString(" • "))
                        }
                        IconButton(onClick = { vm.removeBookmark(mark.id) }) { Icon(Icons.Rounded.DeleteOutline, "حذف العلامة") }
                    }
                }
            }
        }
    }
}
