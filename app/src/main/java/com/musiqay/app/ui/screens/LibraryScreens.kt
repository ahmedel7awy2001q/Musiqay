package com.musiqay.app.ui.screens

import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import android.provider.MediaStore
import android.os.Build
import android.app.RecoverableSecurityException
import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.musiqay.app.util.detectedSurah
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarDuration
import kotlinx.coroutines.launch
import androidx.compose.runtime.produceState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Edit
import com.musiqay.app.ui.PlaylistSummary
import com.musiqay.app.ui.AlbumArtwork
import com.musiqay.app.util.formatDuration
import com.musiqay.app.util.normalizeSearch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.musiqay.app.data.PlaylistEntity
import com.musiqay.app.data.Song
import com.musiqay.app.ui.MusicViewModel
import com.musiqay.app.ui.PlaylistPickerDialog
import com.musiqay.app.ui.SongRow
import com.musiqay.app.ui.theme.premiumOutlineBrush
import com.musiqay.app.ui.theme.premiumPanelBrush
import com.musiqay.app.ui.theme.premiumAmbientSurface
import com.musiqay.app.ui.theme.premiumScreenBrush

private fun groupTitle(type: String, key: String, songs: List<Song>): String = when (type) {
    "album" -> songs.firstOrNull()?.album ?: "ألبوم غير معروف"
    "folder" -> songs.firstOrNull()?.folder ?: key.substringAfterLast('/')
    else -> key.ifBlank { "غير معروف" }
}

private data class SearchResults(val query: String = "", val loading: Boolean = false, val songs: List<Song> = emptyList())

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
    onOpenPlayer: () -> Unit,
    onBrowse: (String) -> Unit
) {
    var sort by rememberSaveable { mutableStateOf(SongSort.NEWEST) }
    var sortMenu by remember { mutableStateOf(false) }
    val sorted by produceState(songs, songs, sort) {
        value = withContext(Dispatchers.Default) { when (sort) {
            SongSort.NEWEST -> songs.sortedByDescending { it.dateAddedSeconds }
            SongSort.TITLE -> songs.sortedBy { it.title.lowercase() }
            SongSort.ARTIST -> songs.sortedBy { it.artist.lowercase() }
            SongSort.DURATION -> songs.sortedByDescending { it.durationMs }
        } }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                expandedHeight = 84.dp,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = .96f),
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        Text("المكتبة", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        Text("${songs.size} ملف صوتي", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    Box {
                        Box(
                            Modifier.background(
                                MaterialTheme.colorScheme.primaryContainer,
                                CircleShape
                            )
                        ) {
                            IconButton(onClick = { sortMenu = true }) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.Sort,
                                    contentDescription = "الترتيب",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
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
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("surah" to "التلاوات", "reader" to "القراء", "artist" to "الفنانون", "album" to "الألبومات", "folder" to "المجلدات").forEach { (type, label) ->
                    FilterChip(selected = false, onClick = { onBrowse(type) }, label = { Text(label) })
                }
            }
            SongList(songs = sorted, allSongsForPlayback = sorted, favoriteIds = favoriteIds,
                playlists = playlists, vm = vm, onOpenPlayer = onOpenPlayer, modifier = Modifier.weight(1f))
        }
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
    val searchIndex by produceState<Map<Long, String>>(emptyMap(), songs) {
        value = withContext(Dispatchers.Default) { songs.associate { song ->
            song.id to normalizeSearch("${song.title} ${song.artist} ${song.album} ${song.folderKey}")
        } }
    }
    val results by produceState<SearchResults>(SearchResults(), songs, searchIndex, query) {
        value = SearchResults(query, true)
        if (query.isNotBlank() && songs.isNotEmpty() && searchIndex.size != songs.size) return@produceState
        if (query.isNotBlank()) delay(180)
        val matched = withContext(Dispatchers.Default) {
            val term = normalizeSearch(query)
            if (term.isBlank()) emptyList() else songs.filter { searchIndex[it.id]?.contains(term) == true }
        }
        value = SearchResults(query, false, matched)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(premiumScreenBrush())
                .premiumAmbientSurface()
            .statusBarsPadding()
    ) {
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
            placeholder = { Text("ابحث باسم الملف أو القارئ أو الألبوم") },
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = .96f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = .86f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
        if (query.isBlank()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(60.dp).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = .68f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("ابحث في مكتبتك", fontWeight = FontWeight.Bold)
                    Text(
                        "بالملف أو القارئ أو الألبوم أو المجلد",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (results.loading || results.query != query) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            SongList(
                songs = results.songs,
                allSongsForPlayback = results.songs,
                emptyMessage = "لا توجد نتائج. جرّب اسم الملف أو القارئ أو المجلد.",
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = .96f),
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
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
            emptyMessage = "لم تضف أي ملف صوتي إلى المفضلة بعد.",
            modifier = Modifier.padding(padding)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    playlists: List<PlaylistEntity>, favoriteCount: Int,
    onFavorites: () -> Unit, onPlaylist: (Long) -> Unit,
    onCreate: (String) -> Unit, onDelete: (Long) -> Unit,
    onRename: (Long, String) -> Unit, summaries: Map<Long, PlaylistSummary>
) {
    var createDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<PlaylistEntity?>(null) }
    var deleting by remember { mutableStateOf<PlaylistEntity?>(null) }
    var menuId by remember { mutableStateOf<Long?>(null) }
    var name by remember { mutableStateOf("") }
    if (createDialog || editing != null) {
        AlertDialog(onDismissRequest = { createDialog = false; editing = null },
            title = { Text(if (editing == null) "قائمة تشغيل جديدة" else "إعادة تسمية القائمة") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it.take(100) }, label = { Text("اسم القائمة") }, singleLine = true) },
            confirmButton = { Button(enabled = name.isNotBlank(), onClick = {
                editing?.let { onRename(it.id, name) } ?: onCreate(name)
                createDialog = false; editing = null; name = ""
            }) { Text("حفظ") } },
            dismissButton = { TextButton(onClick = { createDialog = false; editing = null }) { Text("إلغاء") } })
    }
    deleting?.let { playlist ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text("حذف قائمة التشغيل") },
            text = { Text("حذف قائمة «${playlist.name}»؟ ملفات الأغاني ستبقى على الهاتف.") },
            confirmButton = { TextButton(onClick = { onDelete(playlist.id); deleting = null }) {
                Text("حذف القائمة", color = MaterialTheme.colorScheme.error)
            } }, dismissButton = { TextButton(onClick = { deleting = null }) { Text("إلغاء") } })
    }
    Scaffold(topBar = { TopAppBar(title = { Text("قوائم التشغيل", fontWeight = FontWeight.Bold) },
        actions = { IconButton(onClick = { name = ""; createDialog = true }) { Icon(Icons.Rounded.Add, "إنشاء قائمة") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().background(premiumScreenBrush()).premiumAmbientSurface().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { PlaylistCard("المفضلة", "$favoriteCount ملف صوتي", Icons.Rounded.Favorite, onFavorites) }
            if (playlists.isEmpty()) item {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text("ابدأ قائمتك الأولى", style = MaterialTheme.typography.titleMedium)
                    Text("اجمع التلاوات أو المقاطع التي تريد سماعها معًا.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { name = ""; createDialog = true }) { Text("إنشاء قائمة") }
                }
            }
            items(playlists, key = { it.id }, contentType = { "playlist" }) { playlist ->
                val summary = summaries[playlist.id] ?: PlaylistSummary()
                PlaylistCard(title = playlist.name, subtitle = "${summary.count} ملف صوتي • ${formatDuration(summary.durationMs)}",
                    icon = Icons.AutoMirrored.Rounded.QueueMusic, onClick = { onPlaylist(playlist.id) },
                    artwork = summary.artwork,
                    trailing = { Box {
                        IconButton(onClick = { menuId = playlist.id }) { Icon(Icons.Rounded.MoreVert, "خيارات ${playlist.name}") }
                        DropdownMenu(expanded = menuId == playlist.id, onDismissRequest = { menuId = null }) {
                            DropdownMenuItem(text = { Text("إعادة تسمية") }, leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                                onClick = { name = playlist.name; editing = playlist; menuId = null })
                            DropdownMenuItem(text = { Text("حذف القائمة", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = { deleting = playlist; menuId = null })
                        }
                    } })
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
    trailing: (@Composable () -> Unit)? = null,
    artwork: android.net.Uri? = null
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(.5.dp, shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 8.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (artwork != null) AlbumArtwork(artwork, Modifier.size(44.dp), 11) else Box(
                Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = .70f), RoundedCornerShape(12.dp))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = .14f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(21.dp)
                )
            }
            Column(Modifier.weight(1f).padding(horizontal = 11.dp)) {
                Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
    var query by rememberSaveable { mutableStateOf("") }
    val appSettings by vm.settings.collectAsStateWithLifecycle()
    val title = when (type) {
        "artist" -> "الفنانون"
        "reader" -> "القراء"
        "surah" -> "التلاوات حسب السورة"
        "album" -> "الألبومات"
        else -> "المجلدات"
    }
    BackHandler(enabled = selected != null) { selected = null }
    val grouped by produceState<Map<String, List<Song>>>(emptyMap(), songs, type, appSettings.hiddenFolders) {
        value = withContext(Dispatchers.Default) { when (type) {
            "artist" -> songs.groupBy { it.artist }
            "reader" -> songs.filter { detectedSurah(it.title) != null }.groupBy { com.musiqay.app.util.displayArtist(it.artist).ifBlank { "قارئ غير محدد" } }
            "surah" -> songs.mapNotNull { song -> detectedSurah(song.title)?.let { it to song } }.groupBy({ it.first }, { it.second })
            "album" -> songs.groupBy { if (it.albumId > 0) "album:${it.albumId}" else "${it.artist}\u001f${it.album}" }
            else -> songs
                .filterNot { com.musiqay.app.util.isHiddenFolder(it.folderKey, it.folder, appSettings.hiddenFolders) }
                .groupBy { it.folderKey }
        } }
    }
    val visibleEntries = remember(grouped, query) {
        grouped.entries.filter { query.isBlank() || normalizeSearch(groupTitle(type, it.key, it.value)).contains(normalizeSearch(query)) }
            .sortedBy { normalizeSearch(groupTitle(type, it.key, it.value)) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = .96f),
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = {
                    Column {
                        Text(
                            selected?.let { groupTitle(type, it, grouped[it].orEmpty()) } ?: title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Black
                        )
                        if (selected == null) {
                            Text(
                                "${visibleEntries.size} عنصر",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(premiumScreenBrush())
                .premiumAmbientSurface()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 12.dp,
                    vertical = 10.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Rounded.Search, null) },
                        placeholder = { Text("البحث في $title") },
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = .96f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = .86f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }

                items(visibleEntries, key = { it.key }, contentType = { "libraryGroup" }) { entry ->
                    val shape = RoundedCornerShape(16.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(.5.dp, shape)
                            .background(premiumPanelBrush(), shape)
                            .border(.4.dp, premiumOutlineBrush(), shape)
                            .clickable { selected = entry.key }
                            .padding(horizontal = 11.dp, vertical = 8.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = .68f),
                                        RoundedCornerShape(13.dp)
                                    )
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = .14f),
                                        RoundedCornerShape(13.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    when (type) {
                                        "artist", "reader" -> Icons.Rounded.Person
                                        "surah" -> Icons.Rounded.MenuBook
                                        "album" -> Icons.Rounded.Album
                                        else -> Icons.Rounded.Folder
                                    },
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(Modifier.weight(1f).padding(horizontal = 11.dp)) {
                                Text(
                                    groupTitle(type, entry.key, entry.value),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${entry.value.size} ملف صوتي" + if (type == "folder") " • ${entry.key}" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (type == "folder") {
                                IconButton(onClick = { vm.hideFolder(entry.key) }) {
                                    Icon(
                                        Icons.Rounded.VisibilityOff,
                                        contentDescription = "إخفاء المجلد",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
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
                modifier = Modifier
                    .background(premiumScreenBrush())
                .premiumAmbientSurface()
                    .padding(padding)
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
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val trackFlow = remember(vm, playlistId) { vm.playlistTrackIds(playlistId) }
    val ids by trackFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val byId = remember(allSongs) { allSongs.associateBy { it.id } }
    val songs = remember(ids, byId) { ids.mapNotNull(byId::get) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = .96f),
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = {
                    Column {
                        Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                        Text("${songs.size} ملف صوتي", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            onRemoveFromPlaylist = { song ->
                vm.removeFromPlaylist(playlistId, song.id) { removed -> scope.launch {
                    snackbar.currentSnackbarData?.dismiss()
                    if (snackbar.showSnackbar("أُزيلت الأغنية من القائمة", "تراجع", duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed)
                        vm.restorePlaylistTrack(removed)
                } }
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
    onRemoveFromPlaylist: ((Song) -> Unit)? = null
) {
    var pickerSong by remember { mutableStateOf<Song?>(null) }
    var createForSong by remember { mutableStateOf<Song?>(null) }
    var newPlaylistName by remember { mutableStateOf("") }
    var songPendingDelete by remember { mutableStateOf<Song?>(null) }
    var songAwaitingSystemDelete by remember { mutableStateOf<Song?>(null) }
    var retryDeleteAfterGrant by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val playerState by vm.player.summary.collectAsStateWithLifecycle()

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
        Box(
            modifier
                .fillMaxSize()
                .background(premiumScreenBrush())
                .premiumAmbientSurface(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(70.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = .24f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    emptyMessage,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 28.dp)
                )
            }
        }
        return
    }

    LazyColumn(
        modifier
            .fillMaxSize()
            .background(premiumScreenBrush())
                .premiumAmbientSurface()
    ) {
        items(songs, key = { it.id }, contentType = { "song" }) { song ->
            SongRow(
                song = song,
                isFavorite = song.id in favoriteIds,
                isCurrent = playerState.mediaId == song.id,
                isPlaying = playerState.mediaId == song.id && playerState.isPlaying,
                onClick = { vm.play(song, allSongsForPlayback) },
                onToggleFavorite = { vm.toggleFavorite(song.id) },
                onPlayNext = { vm.player.playNext(song) },
                onAddToQueue = { vm.player.addToQueue(song) },
                onAddToPlaylist = { pickerSong = song },
                onDeleteFromDevice = { songPendingDelete = song },
                onRemoveFromPlaylist = onRemoveFromPlaylist?.let { callback -> { callback(song) } }
            )
        }
    }
}
