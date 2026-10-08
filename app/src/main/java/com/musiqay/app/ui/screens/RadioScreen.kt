package com.musiqay.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musiqay.app.data.RadioStation
import com.musiqay.app.playback.NowPlayingState
import com.musiqay.app.ui.AlbumArtwork
import com.musiqay.app.ui.MusicViewModel
import com.musiqay.app.ui.SleepTimerDialog
import com.musiqay.app.ui.ManualRadioDialog
import com.musiqay.app.ui.RadioFavoritesSheet
import com.musiqay.app.ui.radioLogoBackground
import com.musiqay.app.ui.theme.*
import com.musiqay.app.util.normalizeRadioSearch
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalContext
import android.content.Intent

@Composable
fun RadioScreen(
    vm: MusicViewModel,
    onRequestNotifications: () -> Unit,
    onMusic: () -> Unit,
    audioPermission: Boolean = true
) {
    val state by vm.player.summary.collectAsStateWithLifecycle()
    val catalog by vm.radioCatalog.collectAsStateWithLifecycle()
    val favorites by vm.radioFavorites.collectAsStateWithLifecycle()
    val lastId by vm.lastRadioStation.collectAsStateWithLifecycle()
    val favoriteOrder by vm.radioFavoriteOrder.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val canResumeMusic by vm.player.canResumeMusic.collectAsStateWithLifecycle()
    val sleepEnd by vm.player.sleepTimerEnd.collectAsStateWithLifecycle()

    // Render the screen shell first, then refresh. This also keeps malformed remote data
    // from blocking the first frame on slower or vendor-customized devices.
    LaunchedEffect(Unit) {
        delay(120)
        vm.refreshRadio()
    }

    var category by rememberSaveable { mutableStateOf("الكل") }
    var query by rememberSaveable { mutableStateOf("") }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    var addStation by rememberSaveable { mutableStateOf(false) }
    var orderFavorites by rememberSaveable { mutableStateOf(false) }
    var deleteManual by remember { mutableStateOf<RadioStation?>(null) }
    var timer by rememberSaveable { mutableStateOf(false) }

    val safeCatalogStations = remember(catalog.stations) {
        catalog.stations
            .asSequence()
            .filter { it.id.isNotBlank() && it.name.isNotBlank() && it.streamUrl.isNotBlank() }
            .distinctBy { station ->
                runCatching { normalizeRadioSearch(station.name) + "|" + station.category }
                    .getOrDefault(station.id)
            }
            .toList()
    }
    val searchIndex = remember(safeCatalogStations) {
        safeCatalogStations.associate { station ->
            station.id to runCatching {
                normalizeRadioSearch(
                    station.name + " " + station.badge + " " + station.description + " " + station.tags
                )
            }.getOrDefault("")
        }
    }
    val stations = remember(query, favoritesOnly, favorites, category, safeCatalogStations, searchIndex, favoriteOrder) {
        val term = runCatching { normalizeRadioSearch(query) }.getOrDefault("")
        safeCatalogStations
            .asSequence()
            .filter { station ->
                (!favoritesOnly || station.id in favorites) &&
                    (category == "الكل" || station.category == category) &&
                    searchIndex[station.id].orEmpty().contains(term)
            }
            .sortedWith(compareByDescending<RadioStation> { it.id in favorites }
                .thenBy { favoriteOrder.indexOf(it.id).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE }.thenBy { it.name })
            .toList()
    }

    val play: (RadioStation) -> Unit = { station ->
        onRequestNotifications()
        vm.playRadio(station)
    }
    val current = safeCatalogStations.firstOrNull { it.id == state.radioStationId }
    val last = safeCatalogStations.firstOrNull { it.id == lastId }
    val listedStations = remember(stations, current?.id) {
        if (current == null) stations else stations.filterNot { it.id == current.id }
    }

    if (addStation) ManualRadioDialog(onAdd = { name, url ->
        vm.addManualRadio(name, url)?.also { play(it); addStation = false }
    }, onDismiss = { addStation = false })
    if (orderFavorites) RadioFavoritesSheet(safeCatalogStations, favoriteOrder, vm::moveRadioFavorite) { orderFavorites = false }
    deleteManual?.let { station -> AlertDialog(onDismissRequest = { deleteManual = null }, title = { Text("إزالة المحطة اليدوية") },
        text = { Text(station.name) }, confirmButton = { TextButton(onClick = {
            if (state.radioStationId == station.id) vm.player.stopRadio()
            vm.removeManualRadio(station.id); deleteManual = null
        }) { Text("إزالة") } }, dismissButton = { TextButton(onClick = { deleteManual = null }) { Text("إلغاء") } }) }
    if (timer) {
        SleepTimerDialog(sleepEnd != null, vm.player::setSleepTimer) { timer = false }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(premiumScreenBrush())
            .premiumAmbientSurface()
            .statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            RadioHeader(
                stationCount = safeCatalogStations.size,
                loading = catalog.loading,
                onAdd = { addStation = true },
                onRefresh = { vm.refreshRadio(force = true) }
            )
        }

        catalog.error?.let { error ->
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = .72f)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            error,
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        TextButton(onClick = { vm.refreshRadio(force = true) }) { Text("إعادة") }
                    }
                }
            }
        }

        if (current != null) {
            item {
                CurrentRadioCard(
                    station = current,
                    state = state,
                    isFavorite = current.id in favorites,
                    canResumeMusic = canResumeMusic && audioPermission,
                    sleepEnd = sleepEnd,
                    onToggleFavorite = { vm.toggleRadioFavorite(current.id) },
                    onTogglePlayback = {
                        if (state.wantsPlayback) vm.player.stopRadio() else play(current)
                    },
                    onTimer = { timer = true },
                    onMusic = {
                        vm.player.resumeMusic()
                        onMusic()
                    }
                )
            }
        } else if (last != null) {
            item {
                OutlinedButton(
                    onClick = { play(last) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Rounded.History, null)
                    Spacer(Modifier.width(8.dp))
                    Text("آخر محطة: ${last.name}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                trailingIcon = if (query.isNotBlank()) {
                    { IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "مسح البحث") } }
                } else null,
                placeholder = { Text("ابحث عن محطة") },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = .96f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = .86f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .42f)
                )
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !favoritesOnly,
                    onClick = { favoritesOnly = false },
                    label = { Text("كل المحطات") },
                    leadingIcon = { Icon(Icons.Rounded.Radio, null, Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = favoritesOnly,
                    onClick = { favoritesOnly = true },
                    label = { Text("المفضلة") },
                    leadingIcon = { Icon(Icons.Rounded.Favorite, null, Modifier.size(16.dp)) }
                )
            }
        }

        if ((favoritesOnly && favorites.size > 1) || current != null) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (favoritesOnly && favorites.size > 1) {
                        TextButton(onClick = { orderFavorites = true }) { Text("ترتيب المفضلة") }
                    }
                    current?.let { station -> TextButton(onClick = {
                        runCatching { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"; putExtra(Intent.EXTRA_TEXT, station.name + "\n" + station.streamUrl)
                        }, "مشاركة المحطة")) }
                    }) { Icon(Icons.Rounded.Share, "مشاركة المحطة"); Spacer(Modifier.width(5.dp)); Text("مشاركة") } }
                }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("الكل", "قرآن", "موسيقى ومنوعات", "أخبار ورياضة"), key = { it }) { label ->
                    FilterChip(
                        selected = category == label,
                        onClick = { category = label },
                        label = { Text(label) }
                    )
                }
            }
        }

        if (catalog.loading) {
            item {
                LinearProgressIndicator(
                    Modifier.fillMaxWidth(),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        if (current != null && listedStations.isNotEmpty()) {
            item {
                Text(
                    "محطات أخرى",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        items(listedStations, key = { "radio:${it.id}" }, contentType = { "radioStation" }) { station ->
            RadioStationRow(
                station = station,
                active = station.id == state.radioStationId,
                state = state,
                favorite = station.id in favorites,
                onToggleFavorite = { vm.toggleRadioFavorite(station.id) },
                onRemove = if (station.id.startsWith("manual-")) ({ deleteManual = station }) else null,
                onTogglePlayback = {
                    if (station.id == state.radioStationId && state.wantsPlayback) {
                        vm.player.stopRadio()
                    } else {
                        play(station)
                    }
                }
            )
        }

        if (listedStations.isEmpty() && current == null) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = .82f)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Rounded.Radio,
                            null,
                            modifier = Modifier.size(34.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            when {
                                query.isNotBlank() -> "لا توجد محطة بهذا الاسم"
                                favoritesOnly -> "لا توجد محطات مفضلة في هذا القسم"
                                else -> "لا توجد محطات في هذا القسم حاليًا"
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Text(
                "دليل المحطات: Radio Browser • البث يحتاج إنترنت",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RadioHeader(stationCount: Int, loading: Boolean, onAdd: () -> Unit, onRefresh: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(42.dp).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = .78f), RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Radio, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(23.dp))
        }
        Column(Modifier.weight(1f).padding(horizontal = 11.dp)) {
            Text("الراديو", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(
                "$stationCount محطة • مصر والتلاوات",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onAdd, modifier = Modifier.size(44.dp)) {
            Icon(Icons.Rounded.Add, "إضافة محطة", modifier = Modifier.size(21.dp))
        }
        FilledTonalIconButton(onClick = onRefresh, enabled = !loading, modifier = Modifier.size(44.dp)) {
            Icon(Icons.Rounded.Refresh, "تحديث المحطات", modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun CurrentRadioCard(
    station: RadioStation,
    state: NowPlayingState,
    isFavorite: Boolean,
    canResumeMusic: Boolean,
    sleepEnd: Long?,
    onToggleFavorite: () -> Unit,
    onTogglePlayback: () -> Unit,
    onTimer: () -> Unit,
    onMusic: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = .42f), shape)
            .border(.7.dp, MaterialTheme.colorScheme.primary.copy(alpha = .34f), shape)
            .padding(horizontal = 11.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioArtwork(station, Modifier.size(52.dp))
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        station.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (state.wantsPlayback) {
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
                    radioStatus(state),
                    color = if (state.isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(48.dp)) {
                Icon(
                    if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    if (isFavorite) "إزالة المحطة من المفضلة" else "إضافة المحطة إلى المفضلة",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(21.dp)
                )
            }
            FilledIconButton(onClick = onTogglePlayback, modifier = Modifier.size(48.dp)) {
                Icon(
                    if (state.wantsPlayback) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                    if (state.wantsPlayback) "إيقاف البث" else "تشغيل البث",
                    modifier = Modifier.size(23.dp)
                )
            }
        }
        if (state.broadcastTitle.isNotBlank()) Text(state.broadcastTitle, maxLines = 2, style = MaterialTheme.typography.bodySmall)
        if (state.buffering && state.wantsPlayback) {
            LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AssistChip(
                onClick = onTimer,
                label = { TimerLabel(sleepEnd) },
                leadingIcon = { Icon(Icons.Rounded.Timer, null, Modifier.size(17.dp)) }
            )
            if (canResumeMusic) {
                AssistChip(
                    onClick = onMusic,
                    label = { Text("العودة للملفات") },
                    leadingIcon = { Icon(Icons.Rounded.LibraryMusic, null, Modifier.size(17.dp)) }
                )
            }
        }
    }
}

@Composable
private fun RadioStationRow(
    station: RadioStation,
    active: Boolean,
    state: NowPlayingState,
    favorite: Boolean,
    onToggleFavorite: () -> Unit,
    onRemove: (() -> Unit)? = null,
    onTogglePlayback: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .background(
                if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .58f)
                else MaterialTheme.colorScheme.surface.copy(alpha = .94f),
                shape
            )
            .border(
                if (active) .8.dp else .35.dp,
                if (active) MaterialTheme.colorScheme.primary.copy(alpha = .48f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = .28f),
                shape
            )
            .clickable(onClick = onTogglePlayback)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            RadioArtwork(station, Modifier.size(46.dp))
            if (active) {
                Box(
                    Modifier
                        .size(17.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.GraphicEq,
                        null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(
                station.name,
                fontWeight = if (active) FontWeight.Black else FontWeight.SemiBold,
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                if (active) radioStatus(state) else station.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (onRemove != null) IconButton(onClick = onRemove) { Icon(Icons.Rounded.DeleteOutline, "إزالة المحطة اليدوية") }
        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(48.dp)) {
            Icon(
                if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                if (favorite) "إزالة ${station.name} من المفضلة" else "إضافة ${station.name} إلى المفضلة",
                tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(21.dp)
            )
        }
        FilledTonalIconButton(onClick = onTogglePlayback, modifier = Modifier.size(48.dp)) {
            Icon(
                if (active && state.wantsPlayback) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                if (active && state.wantsPlayback) "إيقاف ${station.name}" else "تشغيل ${station.name}",
                modifier = Modifier.size(21.dp)
            )
        }
    }
}

private fun radioStatus(state: NowPlayingState): String = when {
    state.isPlaying -> "بث مباشر الآن"
    state.playbackError && state.wantsPlayback -> "جارٍ إعادة الاتصال…"
    state.buffering && state.wantsPlayback -> "جارٍ الاتصال بالمحطة…"
    state.playbackError -> "تعذر الاتصال — اضغط تشغيل للمحاولة"
    state.wantsPlayback -> "جارٍ بدء البث…"
    else -> "البث متوقف"
}

@Composable
private fun RadioArtwork(station: RadioStation, modifier: Modifier) {
    if (station.artworkUrl != null) {
        AlbumArtwork(
            station.artworkUrl,
            modifier,
            13,
            logo = true,
            logoBackground = radioLogoBackground(station.id)
        )
    } else {
        Box(
            modifier.background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                station.name.trim().take(1).ifBlank { "FM" },
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun TimerLabel(end: Long?) {
    val minutes by produceState(0L, end) {
        while (end != null) {
            value = ((end - System.currentTimeMillis() + 59_999) / 60_000).coerceAtLeast(0)
            delay(15_000)
        }
    }
    Text(if (end == null) "مؤقت النوم" else "متبقي $minutes د", maxLines = 1)
}
