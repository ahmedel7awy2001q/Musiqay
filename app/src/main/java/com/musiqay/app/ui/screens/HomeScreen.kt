package com.musiqay.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    hiddenFolders: Set<String>,
    onSong: (Song) -> Unit,
    onSettings: () -> Unit,
    onFavorites: () -> Unit,
    onAllSongs: () -> Unit,
    onArtists: () -> Unit,
    onAlbums: () -> Unit,
    onFolders: () -> Unit
) {
    val recent = songs.sortedByDescending { it.dateAddedSeconds }.take(10)
    val artists = songs.map { it.artist }.filter { it.isNotBlank() }.distinct().size
    val albums = songs.map { it.album }.filter { it.isNotBlank() }.distinct().size
    val folders = songs.map { it.folder }.filter { it.isNotBlank() && it !in hiddenFolders }.distinct().size
    val shape = RoundedCornerShape(26.dp)

    LazyColumn(
        modifier = Modifier.statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 26.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("موسيقاي", fontSize = 31.sp, fontWeight = FontWeight.Black)
                    Text(
                        "موسيقاك... بطريقتك",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Box(
                    Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .62f), CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .20f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = "الإعدادات")
                    }
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .padding(horizontal = 18.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .height(136.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF5E7CFF),
                                Color(0xFF7B5CFF),
                                Color(0xFFB44FD8)
                            )
                        ),
                        shape
                    )
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = .58f),
                                Color.White.copy(alpha = .08f)
                            )
                        ),
                        shape
                    )
                    .clickable(onClick = onAllSongs)
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Column(Modifier.align(Alignment.CenterStart)) {
                    Text(
                        "موسيقى لكل لحظة",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        if (songs.isEmpty()) "ابدأ بإضافة الموسيقى إلى هاتفك" else "\${songs.size} أغنية جاهزة للتشغيل",
                        color = Color.White.copy(alpha = .90f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "مكتبتك المحلية • تشغيل سريع • خصوصية كاملة",
                        color = Color.White.copy(alpha = .72f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .size(58.dp)
                        .background(Color.White.copy(alpha = .15f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = .25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.LibraryMusic,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Color.White
                    )
                }
            }
        }

        item {
            Text(
                "مكتبتك",
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Column(Modifier.padding(horizontal = 14.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    LibraryCard("المفضلة", "\$favoriteCount أغنية", Icons.Rounded.Favorite, onFavorites, Modifier.weight(1f))
                    Spacer(Modifier.width(10.dp))
                    LibraryCard("الفنانون", "\$artists فنان", Icons.Rounded.Person, onArtists, Modifier.weight(1f))
                }
                Spacer(Modifier.height(9.dp))
                Row(Modifier.fillMaxWidth()) {
                    LibraryCard("الألبومات", "\$albums ألبوم", Icons.Rounded.Album, onAlbums, Modifier.weight(1f))
                    Spacer(Modifier.width(10.dp))
                    LibraryCard("المجلدات", "\$folders مجلد", Icons.Rounded.Folder, onFolders, Modifier.weight(1f))
                }
            }
        }

        if (recent.isNotEmpty()) {
            item {
                SectionTitle(
                    title = "المضافة حديثًا",
                    action = "عرض الكل",
                    onAction = onAllSongs,
                    modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp)
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    items(recent, key = { it.id }) { song ->
                        Column(modifier = Modifier.width(126.dp).clickable { onSong(song) }) {
                            AlbumArtwork(song.artworkUri, Modifier.size(126.dp), 20)
                            Spacer(Modifier.height(7.dp))
                            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                            Text(
                                song.artist.ifBlank { "فنان غير معروف" },
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
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .height(94.dp)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .78f),
                        MaterialTheme.colorScheme.surface.copy(alpha = .52f)
                    )
                ),
                shape
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = .28f),
                        Color.White.copy(alpha = .05f)
                    )
                ),
                shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column(Modifier.align(Alignment.CenterStart)) {
            Box(
                Modifier
                    .size(34.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = .13f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.height(7.dp))
            Text(title, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
