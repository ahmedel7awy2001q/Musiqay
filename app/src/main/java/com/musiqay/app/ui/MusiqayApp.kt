package com.musiqay.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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

    val rootItems = listOf(
        NavItem("home", "الرئيسية", Icons.Rounded.Home),
        NavItem("songs", "الأغاني", Icons.Rounded.LibraryMusic),
        NavItem("playlists", "القوائم", Icons.AutoMirrored.Rounded.QueueMusic),
        NavItem("search", "البحث", Icons.Rounded.Search)
    )
    val showBottom = currentRoute in rootItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottom) {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    MiniPlayer(
                        state = playerState,
                        onOpen = { navController.navigate("player") },
                        onToggle = vm.player::togglePlayPause,
                        onNext = vm.player::next
                    )
                    NavigationBar {
                        rootItems.forEach { item ->
                            NavigationBarItem(
                                selected = currentRoute == item.route,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) }
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
                    songs = songs,
                    favoriteCount = favorites.size,
                    onSong = { song ->
                        vm.play(song)
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
                    songs = songs,
                    favoriteIds = favorites.toSet(),
                    playlists = playlists,
                    vm = vm,
                    onOpenPlayer = { navController.navigate("player") }
                )
            }
            composable("playlists") {
                PlaylistsScreen(
                    playlists = playlists,
                    favoriteCount = favorites.size,
                    onFavorites = { navController.navigate("favorites") },
                    onPlaylist = { navController.navigate("playlist/$it") },
                    onCreate = { vm.createPlaylist(it) },
                    onDelete = vm::deletePlaylist
                )
            }
            composable("search") {
                SearchScreen(
                    songs = songs,
                    favoriteIds = favorites.toSet(),
                    playlists = playlists,
                    vm = vm,
                    onOpenPlayer = { navController.navigate("player") }
                )
            }
            composable("player") {
                NowPlayingScreen(
                    vm = vm,
                    songs = songs,
                    favoriteIds = favorites.toSet(),
                    playlists = playlists,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("settings") {
                SettingsScreen(vm = vm, onBack = { navController.popBackStack() })
            }
            composable("favorites") {
                FavoritesScreen(
                    songs = songs.filter { it.id in favorites },
                    favoriteIds = favorites.toSet(),
                    playlists = playlists,
                    vm = vm,
                    onBack = { navController.popBackStack() },
                    onOpenPlayer = { navController.navigate("player") }
                )
            }
            composable("browse/{type}") { entry ->
                BrowseGroupsScreen(
                    type = entry.arguments?.getString("type").orEmpty(),
                    songs = songs,
                    favoriteIds = favorites.toSet(),
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
                    allSongs = songs,
                    favoriteIds = favorites.toSet(),
                    playlists = playlists,
                    vm = vm,
                    onBack = { navController.popBackStack() },
                    onOpenPlayer = { navController.navigate("player") }
                )
            }
        }
    }
}
