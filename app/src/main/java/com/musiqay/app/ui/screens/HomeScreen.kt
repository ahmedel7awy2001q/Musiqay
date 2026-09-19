package com.musiqay.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musiqay.app.data.Song
import com.musiqay.app.ui.AlbumArtwork
import com.musiqay.app.ui.SectionTitle

@Composable
fun HomeScreen(
    songs: List<Song>,
    favoriteCount: Int,
    onSong: (Song) -> Unit,
    onSettings: () -> Unit,
    onFavorites: () -> Unit,
    onAllSongs: () -> Unit,
    onArtists: () -> Unit,
    onAlbums: () -> Unit,
    onFolders: () -> Unit
) {
    val recent = songs.sortedByDescending { it.dateAddedSeconds }.take(10)
    val artists = songs.map { it.artist }.distinct().size
    val albums = songs.map { it.album }.distinct().size
    val folders = songs.map { it.folder }.distinct().size

    LazyColumn(
        modifier = Modifier.statusBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("موسيقاي", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "موسيقاك... بطريقتك",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                IconButton(onClick = onSettings) {
                    Icon(Icons.Rounded.Settings, contentDescription = "الإعدادات")
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .padding(horizontal = 18.dp, vertical = 6.dp)
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = .82f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = .74f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = .54f)
                            )
                        ),
                        RoundedCornerShape(24.dp)
                    )
                    .clickable(onClick = onAllSongs)
                    .padding(22.dp)
            ) {
                Column(Modifier.align(Alignment.CenterStart)) {
                    Text(
                        "موسيقى لكل لحظة",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (songs.isEmpty()) "ابدأ بإضافة الموسيقى إلى هاتفك" else "${songs.size} أغنية جاهزة للتشغيل",
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = .9f)
                    )
                }
                Icon(
                    Icons.Rounded.LibraryMusic,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.CenterEnd).size(62.dp),
                    tint = androidx.compose.ui.graphics.Color.White.copy(alpha = .88f)
                )
            }
        }

        item {
            Text(
                "مكتبتك",
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Column(Modifier.padding(horizontal = 14.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    LibraryCard("المفضلة", "$favoriteCount أغنية", Icons.Rounded.Favorite, onFavorites, Modifier.weight(1f))
                    Spacer(Modifier.width(10.dp))
                    LibraryCard("الفنانون", "$artists فنان", Icons.Rounded.Person, onArtists, Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth()) {
                    LibraryCard("الألبومات", "$albums ألبوم", Icons.Rounded.Album, onAlbums, Modifier.weight(1f))
                    Spacer(Modifier.width(10.dp))
                    LibraryCard("المجلدات", "$folders مجلد", Icons.Rounded.Folder, onFolders, Modifier.weight(1f))
                }
            }
        }

        if (recent.isNotEmpty()) {
            item {
                SectionTitle(
                    title = "المضافة حديثًا",
                    action = "عرض الكل",
                    onAction = onAllSongs,
                    modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp)
                )
            }
            item {
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recent, key = { it.id }) { song ->
                        Column(modifier = Modifier.width(142.dp).clickable { onSong(song) }) {
                            AlbumArtwork(song.artworkUri, Modifier.size(142.dp), 18)
                            Spacer(Modifier.height(8.dp))
                            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                            Text(
                                song.artist,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (songs.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Rounded.LibraryMusic,
                        null,
                        modifier = Modifier.size(54.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("لم نجد ملفات موسيقى على الهاتف", fontWeight = FontWeight.Bold)
                    Text("عند إضافة ملفات صوتية ستظهر هنا تلقائيًا.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun LibraryCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
            Spacer(Modifier.height(14.dp))
            Text(title, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
