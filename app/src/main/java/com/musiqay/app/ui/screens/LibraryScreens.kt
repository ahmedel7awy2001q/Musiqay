package com.musiqay.app.ui.screens

import androidx.compose.ui.platform.LocalContext
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import android.provider.MediaStore
import android.os.Build
import android.app.RecoverableSecurityException
import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.musiqay.app.data.PlaylistEntity
import com.musiqay.app.data.Song
import com.musiqay.app.ui.MusicViewModel
import com.musiqay.app.ui.PlaylistPickerDialog
import com.musiqay.app.ui.SongRow

private enum class SongSort(val label: String) {
    NEWEST("الأحدث"),
    TITLE("الاسم"),
    ARTIST("الفنان"),
    DURATION("المدة")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongsScreen(
    songs: List<Song>,
    favoriteIds: Set<Long>,
    playlists: List<PlaylistEntity>,
    vm: MusicViewModel,
    onOpenPlayer: () -> Unit
) {
    var sort by rememberSaveable { mutableStateOf(SongSort.NEWEST) }
    var sortMenu by remember { mutableStateOf(false) }
    val sorted = remember(songs, sort) {
        when (sort) {
            SongSort.NEWEST -> songs.sortedByDescending { it.dateAddedSeconds }
            SongSort.TITLE -> songs.sortedBy { it.title.lowercase() }
            SongSort.ARTIST -> songs.sortedBy { it.artist.lowercase() }
            SongSort.DURATION -> songs.sortedByDescending { it.durationMs }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Column {
                        Text("الأغاني", fontWeight = FontWeight.Bold)
                        Text("${songs.size} أغنية", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { sortMenu = true }) { Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = "الترتيب") }
                        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            SongSort.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = { sort = option; sortMenu = false }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        SongList(
            songs = sorted,
            allSongsForPlayback = sorted,
            favoriteIds = favoriteIds,
            playlists = playlists,
            vm = vm,
            onOpenPlayer = onOpenPlayer,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
fun SearchScreen(
    songs: List<Song>,
    favoriteIds: Set<Long>,
    playlists: List<PlaylistEntity>,
    vm: MusicViewModel,
    onOpenPlayer: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(songs, query) {
        if (query.isBlank()) emptyList()
        else songs.filter {
            it.title.contains(query, true) ||
                it.artist.contains(query, true) ||
                it.album.contains(query, true) ||
                it.folder.contains(query, true)
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text(
            "البحث",
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            placeholder = { Text("ابحث باسم الأغنية أو الفنان أو الألبوم") },
            shape = RoundedCornerShape(18.dp)
        )
        if (query.isBlank()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("اكتب شيئًا للبحث في مكتبتك", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            SongList(
                songs = results,
                allSongsForPlayback = results,
                favoriteIds = favoriteIds,
                playlists = playlists,
                vm = vm,
                onOpenPlayer = onOpenPlayer,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    songs: List<Song>,
    favoriteIds: Set<Long>,
    playlists: List<PlaylistEntity>,
    vm: MusicViewModel,
    onBack: () -> Unit,
    onOpenPlayer: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = { Text("المفضلة", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "رجوع") } },
                actions = {
                    if (songs.isNotEmpty()) {
                        IconButton(onClick = { vm.play(songs.first(), songs); onOpenPlayer() }) {
                            Icon(Icons.Rounded.PlayArrow, "تشغيل")
                        }
                    }
                }
            )
        }
    ) { padding ->
        SongList(
            songs = songs,
            allSongsForPlayback = songs,
            favoriteIds = favoriteIds,
            playlists = playlists,
            vm = vm,
            onOpenPlayer = onOpenPlayer,
            emptyMessage = "لم تضف أي أغنية إلى المفضلة بعد.",
            modifier = Modifier.padding(padding)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    playlists: List<PlaylistEntity>,
    favoriteCount: Int,
    onFavorites: () -> Unit,
    onPlaylist: (Long) -> Unit,
    onCreate: (String) -> Unit,
    onDelete: (Long) -> Unit
) {
    var createDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }

    if (createDialog) {
        AlertDialog(
            onDismissRequest = { createDialog = false },
            title = { Text("قائمة تشغيل جديدة") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم القائمة") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreate(name)
                        name = ""
                        createDialog = false
                    },
                    enabled = name.isNotBlank()
                ) { Text("إنشاء") }
            },
            dismissButton = { TextButton(onClick = { createDialog = false }) { Text("إلغاء") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = { Text("قوائم التشغيل", fontWeight = FontWeight.Bold) },
                actions = { IconButton(onClick = { createDialog = true }) { Icon(Icons.Rounded.Add, "إنشاء") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                PlaylistCard(
                    title = "المفضلة",
                    subtitle = "$favoriteCount أغنية",
                    icon = Icons.Rounded.Favorite,
                    onClick = onFavorites
                )
            }
            items(playlists, key = { it.id }) { playlist ->
                PlaylistCard(
                    title = playlist.name,
                    subtitle = "قائمة تشغيل محلية",
                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                    onClick = { onPlaylist(playlist.id) },
                    trailing = {
                        IconButton(onClick = { onDelete(playlist.id) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "حذف")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PlaylistCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .48f)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            trailing?.invoke()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseGroupsScreen(
    type: String,
    songs: List<Song>,
    favoriteIds: Set<Long>,
    playlists: List<PlaylistEntity>,
    vm: MusicViewModel,
    onBack: () -> Unit,
    onOpenPlayer: () -> Unit
) {
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val title = when (type) {
        "artist" -> "الفنانون"
        "album" -> "الألبومات"
        else -> "المجلدات"
    }
    val grouped = remember(songs, type) {
        when (type) {
            "artist" -> songs.groupBy { it.artist }
            "album" -> songs.groupBy { it.album }
            else -> songs.groupBy { it.folder }
        }.toSortedMap(String.CASE_INSENSITIVE_ORDER)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = { Text(selected ?: title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { if (selected != null) selected = null else onBack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        val current = selected
        if (current == null) {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(grouped.entries.toList(), key = { it.key }) { entry ->
                    Row(
                        Modifier.fillMaxWidth().clickable { selected = entry.key }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            when (type) {
                                "artist" -> Icons.Rounded.Person
                                "album" -> Icons.Rounded.Album
                                else -> Icons.Rounded.Folder
                            },
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp)
                        )
                        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                            Text(entry.key, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${entry.value.size} أغنية", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider()
                }
            }
        } else {
            val list = grouped[current].orEmpty()
            SongList(
                songs = list,
                allSongsForPlayback = list,
                favoriteIds = favoriteIds,
                playlists = playlists,
                vm = vm,
                onOpenPlayer = onOpenPlayer,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    title: String,
    allSongs: List<Song>,
    favoriteIds: Set<Long>,
    playlists: List<PlaylistEntity>,
    vm: MusicViewModel,
    onBack: () -> Unit,
    onOpenPlayer: () -> Unit
) {
    val ids by vm.playlistTrackIds(playlistId).collectAsState(initial = emptyList())
    val byId = remember(allSongs) { allSongs.associateBy { it.id } }
    val songs = remember(ids, byId) { ids.mapNotNull(byId::get) }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Column {
                        Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                        Text("${songs.size} أغنية", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "رجوع") } },
                actions = {
                    if (songs.isNotEmpty()) {
                        FilledTonalButton(onClick = { vm.play(songs.first(), songs); onOpenPlayer() }) {
                            Icon(Icons.Rounded.PlayArrow, null)
                            Text("تشغيل")
                        }
                    }
                }
            )
        }
    ) { padding ->
        SongList(
            songs = songs,
            allSongsForPlayback = songs,
            favoriteIds = favoriteIds,
            playlists = playlists,
            vm = vm,
            onOpenPlayer = onOpenPlayer,
            emptyMessage = "هذه القائمة فارغة. أضف إليها أغاني من قائمة الأغاني.",
            extraTrailing = { song ->
                IconButton(onClick = { vm.removeFromPlaylist(playlistId, song.id) }) {
                    Icon(Icons.Rounded.Delete, "إزالة من القائمة")
                }
            },
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
private fun SongList(
    songs: List<Song>,
    allSongsForPlayback: List<Song>,
    favoriteIds: Set<Long>,
    playlists: List<PlaylistEntity>,
    vm: MusicViewModel,
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier,
    emptyMessage: String = "لا توجد أغاني هنا.",
    extraTrailing: (@Composable (Song) -> Unit)? = null
) {
    var pickerSong by remember { mutableStateOf<Song?>(null) }
    var createForSong by remember { mutableStateOf<Song?>(null) }
    var newPlaylistName by remember { mutableStateOf("") }
    var songPendingDelete by remember { mutableStateOf<Song?>(null) }
    var songAwaitingSystemDelete by remember { mutableStateOf<Song?>(null) }
    var retryDeleteAfterGrant by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val pendingSong = songAwaitingSystemDelete

        if (result.resultCode == Activity.RESULT_OK) {
            if (retryDeleteAfterGrant && pendingSong != null) {
                val deleted = runCatching {
                    context.contentResolver.delete(
                        pendingSong.uri,
                        null,
                        null
                    )
                }.getOrDefault(0)

                if (deleted > 0) {
                    vm.cleanupDeletedMedia(pendingSong.id)
                }
            } else if (pendingSong != null) {
                vm.cleanupDeletedMedia(pendingSong.id)
            }

            vm.refreshLibrary()
        }

        songAwaitingSystemDelete = null
        retryDeleteAfterGrant = false
    }

    fun deleteFromDevice(song: Song) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val pendingIntent = MediaStore.createDeleteRequest(
                context.contentResolver,
                listOf(song.uri)
            )

            songAwaitingSystemDelete = song
            retryDeleteAfterGrant = false

            deleteLauncher.launch(
                IntentSenderRequest.Builder(
                    pendingIntent.intentSender
                ).build()
            )
        } else {
            try {
                val deleted = context.contentResolver.delete(
                    song.uri,
                    null,
                    null
                )

                if (deleted > 0) {
                    vm.cleanupDeletedMedia(song.id)
                    vm.refreshLibrary()
                }
            } catch (error: SecurityException) {
                if (
                    Build.VERSION.SDK_INT == Build.VERSION_CODES.Q &&
                    error is RecoverableSecurityException
                ) {
                    songAwaitingSystemDelete = song
                    retryDeleteAfterGrant = true

                    deleteLauncher.launch(
                        IntentSenderRequest.Builder(
                            error.userAction.actionIntent.intentSender
                        ).build()
                    )
                }
            }
        }
    }

    pickerSong?.let { song ->
        PlaylistPickerDialog(
            playlists = playlists,
            onDismiss = { pickerSong = null },
            onPick = { id ->
                vm.addToPlaylist(id, song.id)
                pickerSong = null
            },
            onCreateRequested = {
                createForSong = song
                pickerSong = null
            }
        )
    }

    createForSong?.let { song ->
        AlertDialog(
            onDismissRequest = { createForSong = null },
            title = { Text("إنشاء قائمة وإضافة الأغنية") },
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
                    onClick = {
                        vm.createPlaylist(newPlaylistName, song.id)
                        newPlaylistName = ""
                        createForSong = null
                    },
                    enabled = newPlaylistName.isNotBlank()
                ) { Text("إنشاء") }
            },
            dismissButton = { TextButton(onClick = { createForSong = null }) { Text("إلغاء") } }
        )
    }

    songPendingDelete?.let { song ->
        AlertDialog(
            onDismissRequest = {
                songPendingDelete = null
            },
            title = {
                Text("حذف الملف من الجهاز")
            },
            text = {
                Text(
                    "سيتم حذف \"${song.title}\" من الهاتف نفسه وليس من موسيقاي فقط. هل تريد المتابعة"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val songToDelete = song
                        songPendingDelete = null
                        deleteFromDevice(songToDelete)
                    }
                ) {
                    Text("متابعة")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        songPendingDelete = null
                    }
                ) {
                    Text("إلغاء")
                }
            }
        )
    }
    if (songs.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(emptyMessage, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(24.dp))
        }
        return
    }

    LazyColumn(modifier.fillMaxSize()) {
        items(songs, key = { it.id }) { song ->
            SongRow(
                song = song,
                isFavorite = song.id in favoriteIds,
                onClick = {
                    vm.play(song, allSongsForPlayback)
                    onOpenPlayer()
                },
                onToggleFavorite = { vm.toggleFavorite(song.id) },
                onPlayNext = { vm.player.playNext(song) },
                onAddToQueue = { vm.player.addToQueue(song) },
                onAddToPlaylist = { pickerSong = song },
                onDeleteFromDevice = { songPendingDelete = song },
                trailingContent = extraTrailing?.let { content -> { content(song) } }
            )
        }
    }
}
