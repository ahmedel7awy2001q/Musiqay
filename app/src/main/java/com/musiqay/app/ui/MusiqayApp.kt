package com.musiqay.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.outlined.Home as HomeOutlined
import androidx.compose.material.icons.outlined.LibraryMusic as LibraryMusicOutlined
import androidx.compose.material.icons.outlined.Search as SearchOutlined
import androidx.compose.material.icons.outlined.Radio as RadioOutlined
import androidx.compose.material.icons.automirrored.outlined.QueueMusic as QueueMusicOutlined
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.musiqay.app.ui.screens.BrowseGroupsScreen
import com.musiqay.app.ui.screens.FavoritesScreen
import com.musiqay.app.ui.screens.HomeScreen
import com.musiqay.app.ui.screens.NowPlayingScreen
import com.musiqay.app.ui.screens.PlaylistDetailScreen
import com.musiqay.app.ui.screens.PlaylistsScreen
import com.musiqay.app.ui.screens.SearchScreen
import com.musiqay.app.ui.screens.SettingsScreen
import com.musiqay.app.ui.screens.SongsScreen
import com.musiqay.app.ui.screens.RadioScreen

private data class NavItem(
    val route: String,
    val label: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
)

private val RootNavItems = listOf(
    NavItem("home", "الرئيسية", Icons.Rounded.Home, Icons.Outlined.HomeOutlined),
    NavItem("songs", "المكتبة", Icons.Rounded.LibraryMusic, Icons.Outlined.LibraryMusicOutlined),
    NavItem("playlists", "القوائم", Icons.AutoMirrored.Rounded.QueueMusic, Icons.AutoMirrored.Outlined.QueueMusicOutlined),
    NavItem("search", "البحث", Icons.Rounded.Search, Icons.Outlined.SearchOutlined),
    NavItem("radio", "راديو", Icons.Rounded.Radio, Icons.Outlined.RadioOutlined)
)
private val RootRoutes = RootNavItems.mapTo(HashSet()) { it.route }

@Composable
fun MusiqayApp(vm: MusicViewModel, audioPermission: Boolean = true,
    onGrantAudio: () -> Unit = {}, onRequestNotifications: () -> Unit = {}) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val playerState by vm.player.summary.collectAsStateWithLifecycle()
    val librarySongs by vm.visibleSongs.collectAsStateWithLifecycle()
    val visibleSongs = if (audioPermission) librarySongs else emptyList()
    val visibleFavoriteIds by vm.visibleFavoriteIds.collectAsStateWithLifecycle()
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val settingsReady by vm.settingsReady.collectAsStateWithLifecycle()
    if (!settingsReady) {
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { androidx.compose.material3.CircularProgressIndicator() }
        return
    }
    val initialRoute = remember { settings.startPage.route }

    val summaries by vm.playlistSummaries.collectAsStateWithLifecycle()
    val playbackError by vm.player.error.collectAsStateWithLifecycle()
    val snackbars = remember { SnackbarHostState() }
    LaunchedEffect(playbackError) {
        playbackError?.let { snackbars.showSnackbar(it); vm.player.dismissError() }
    }

    val showBottom = currentRoute in RootRoutes

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbars) },
        bottomBar = {
            if (currentRoute != "player" && currentRoute != "settings") {
                Column(modifier = if (showBottom) Modifier else Modifier.navigationBarsPadding()) {
                    AnimatedVisibility(
                        visible = playerState.hasMedia,
                        enter = fadeIn(tween(if (settings.reduceMotion) 0 else 180)) + slideInVertically(
                            animationSpec = tween(if (settings.reduceMotion) 0 else 160),
                            initialOffsetY = { it / 3 }
                        ),
                        exit = fadeOut(tween(if (settings.reduceMotion) 0 else 140)) + slideOutVertically(
                            animationSpec = tween(if (settings.reduceMotion) 0 else 180),
                            targetOffsetY = { it / 3 }
                        )
                    ) {
                        PlaybackMiniPlayer(
                            vm = vm,
                            onOpen = { navController.navigate(if (playerState.isRadio) "radio" else "player") { launchSingleTop = true } },
                            onToggle = vm.player::togglePlayPause,
                            onNext = vm.player::next
                        )
                    }
                    if (showBottom) NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .98f),
                        tonalElevation = 0.dp
                    ) {
                        RootNavItems.forEach { item ->
                            val selected = currentRoute == item.route
                            val iconScale by animateFloatAsState(
                                targetValue = if (selected) 1.05f else .97f,
                                animationSpec = tween(if (settings.reduceMotion) 0 else 220),
                                label = "navIconScale"
                            )
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label,
                                        modifier = Modifier.graphicsLayer {
                                            scaleX = iconScale
                                            scaleY = iconScale
                                        }
                                    )
                                },
                                label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .64f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = initialRoute,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding)
        ) {
            composable("home") {
                HomeScreen(
                    vm = vm,
                    onRadio = { navController.navigate("radio") { launchSingleTop = true } },
                    onResume = { navController.navigate(if (playerState.isRadio) "radio" else "player") { launchSingleTop = true } },
                    onPlayAll = { vm.playAll(); navController.navigate("player") },
                    audioPermission = audioPermission,
                    onGrantAudio = onGrantAudio,
                    songs = visibleSongs,
                    favoriteCount = visibleFavoriteIds.size,
                    onSong = { song ->
                        vm.play(song, visibleSongs)
                        navController.navigate("player")
                    },
                    onSettings = { navController.navigate("settings") },
                    onFavorites = { navController.navigate("favorites") },
                    onAllSongs = { navController.navigate("songs") },
                    onArtists = { navController.navigate("browse/artist") },
                    onAlbums = { navController.navigate("browse/album") },
                    onFolders = { navController.navigate("browse/folder") }
                )
            }
            composable("songs") {
                if (!audioPermission) PermissionScreen(onGrantAudio) else SongsScreen(
                    songs = visibleSongs,
                    favoriteIds = visibleFavoriteIds,
                    playlists = playlists,
                    vm = vm,
                    onOpenPlayer = { navController.navigate("player") },
                    onBrowse = { navController.navigate("browse/$it") }
                )
            }
            composable("radio") {
                RadioScreen(vm, onRequestNotifications, onMusic = { navController.navigate("player") }, audioPermission = audioPermission)
            }
            composable("playlists") {
                PlaylistsScreen(
                    playlists = playlists,
                    favoriteCount = visibleFavoriteIds.size,
                    onFavorites = { navController.navigate("favorites") },
                    onPlaylist = { navController.navigate("playlist/$it") },
                    onCreate = { vm.createPlaylist(it) },
                    onDelete = vm::deletePlaylist,
                    onRename = vm::renamePlaylist,
                    summaries = summaries
                )
            }
            composable("search") {
                SearchScreen(
                    songs = visibleSongs,
                    favoriteIds = visibleFavoriteIds,
                    playlists = playlists,
                    vm = vm,
                    onOpenPlayer = { navController.navigate("player") }
                )
            }
            composable("player") {
                if (playerState.isRadio) {
                    LaunchedEffect(Unit) {
                        navController.navigate("radio") {
                            popUpTo("player") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                } else NowPlayingScreen(
                    vm = vm,
                    songs = visibleSongs,
                    favoriteIds = visibleFavoriteIds,
                    playlists = playlists,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("settings") {
                SettingsScreen(vm = vm, onBack = { navController.popBackStack() })
            }
            composable("favorites") {
                if (!audioPermission) PermissionScreen(onGrantAudio) else FavoritesScreen(
                    songs = visibleSongs.filter { it.id in visibleFavoriteIds },
                    favoriteIds = visibleFavoriteIds,
                    playlists = playlists,
                    vm = vm,
                    onBack = { navController.popBackStack() },
                    onOpenPlayer = { navController.navigate("player") }
                )
            }
            composable("browse/{type}") { entry ->
                if (!audioPermission) PermissionScreen(onGrantAudio) else BrowseGroupsScreen(
                    type = entry.arguments?.getString("type").orEmpty(),
                    songs = visibleSongs,
                    favoriteIds = visibleFavoriteIds,
                    playlists = playlists,
                    vm = vm,
                    onBack = { navController.popBackStack() },
                    onOpenPlayer = { navController.navigate("player") }
                )
            }
            composable("playlist/{id}") { entry ->
                val id = entry.arguments?.getString("id")?.toLongOrNull() ?: return@composable
                val name = playlists.firstOrNull { it.id == id }?.name ?: "قائمة تشغيل"
                PlaylistDetailScreen(
                    playlistId = id,
                    title = name,
                    allSongs = visibleSongs,
                    favoriteIds = visibleFavoriteIds,
                    playlists = playlists,
                    vm = vm,
                    onBack = { navController.popBackStack() },
                    onOpenPlayer = { navController.navigate("player") }
                )
            }
        }
    }
}
