package com.musiqay.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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

private data class NavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun MusiqayApp(vm: MusicViewModel) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val playerState by vm.player.state.collectAsStateWithLifecycle()
    val songs by vm.songs.collectAsStateWithLifecycle()
    val favorites by vm.favoriteIds.collectAsStateWithLifecycle()
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()

    // Hidden folders stay in storage and remain restorable from Settings, but their
    // tracks are excluded from every library-facing surface.
    val visibleSongs = songs.filter { song ->
        song.folder !in settings.hiddenFolders
    }
    val visibleSongIds = visibleSongs.asSequence().map { it.id }.toSet()
    val visibleFavoriteIds = favorites.filter { it in visibleSongIds }

    val rootItems = listOf(
        NavItem("home", "الرئيسية", Icons.Rounded.Home),
        NavItem("songs", "الأغاني", Icons.Rounded.LibraryMusic),
        NavItem("playlists", "القوائم", Icons.AutoMirrored.Rounded.QueueMusic),
        NavItem("search", "البحث", Icons.Rounded.Search)
    )
    val showBottom = currentRoute in rootItems.map { it.route }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottom) {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    AnimatedVisibility(
                        visible = playerState.mediaId != null,
                        enter = fadeIn(tween(180)) + slideInVertically(
                            animationSpec = tween(220),
                            initialOffsetY = { it / 3 }
                        ),
                        exit = fadeOut(tween(140)) + slideOutVertically(
                            animationSpec = tween(180),
                            targetOffsetY = { it / 3 }
                        )
                    ) {
                        MiniPlayer(
                            state = playerState,
                            onOpen = { navController.navigate("player") },
                            onToggle = vm.player::togglePlayPause,
                            onNext = vm.player::next
                        )
                    }
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .98f),
                        tonalElevation = 0.dp
                    ) {
                        rootItems.forEach { item ->
                            val selected = currentRoute == item.route
                            val iconScale by animateFloatAsState(
                                targetValue = if (selected) 1.14f else 1f,
                                animationSpec = tween(220),
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
                                        item.icon,
                                        contentDescription = item.label,
                                        modifier = Modifier.graphicsLayer {
                                            scaleX = iconScale
                                            scaleY = iconScale
                                        }
                                    )
                                },
                                label = { Text(item.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary,
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
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") {
                HomeScreen(
                    songs = visibleSongs,
                    favoriteCount = visibleFavoriteIds.size,
                    hiddenFolders = settings.hiddenFolders,
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
                SongsScreen(
                    songs = visibleSongs,
                    favoriteIds = visibleFavoriteIds.toSet(),
                    playlists = playlists,
                    vm = vm,
                    onOpenPlayer = { navController.navigate("player") }
                )
            }
            composable("playlists") {
                PlaylistsScreen(
                    playlists = playlists,
                    favoriteCount = visibleFavoriteIds.size,
                    onFavorites = { navController.navigate("favorites") },
                    onPlaylist = { navController.navigate("playlist/$it") },
                    onCreate = { vm.createPlaylist(it) },
                    onDelete = vm::deletePlaylist
                )
            }
            composable("search") {
                SearchScreen(
                    songs = visibleSongs,
                    favoriteIds = visibleFavoriteIds.toSet(),
                    playlists = playlists,
                    vm = vm,
                    onOpenPlayer = { navController.navigate("player") }
                )
            }
            composable("player") {
                NowPlayingScreen(
                    vm = vm,
                    songs = visibleSongs,
                    favoriteIds = visibleFavoriteIds.toSet(),
                    playlists = playlists,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("settings") {
                SettingsScreen(vm = vm, onBack = { navController.popBackStack() })
            }
            composable("favorites") {
                FavoritesScreen(
                    songs = visibleSongs.filter { it.id in visibleFavoriteIds },
                    favoriteIds = visibleFavoriteIds.toSet(),
                    playlists = playlists,
                    vm = vm,
                    onBack = { navController.popBackStack() },
                    onOpenPlayer = { navController.navigate("player") }
                )
            }
            composable("browse/{type}") { entry ->
                BrowseGroupsScreen(
                    type = entry.arguments?.getString("type").orEmpty(),
                    songs = visibleSongs,
                    favoriteIds = visibleFavoriteIds.toSet(),
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
                    favoriteIds = visibleFavoriteIds.toSet(),
                    playlists = playlists,
                    vm = vm,
                    onBack = { navController.popBackStack() },
                    onOpenPlayer = { navController.navigate("player") }
                )
            }
        }
    }
}
