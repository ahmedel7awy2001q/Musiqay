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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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
import com.musiqay.app.ui.theme.premiumHeroBrush
import com.musiqay.app.ui.theme.premiumOutlineBrush
import com.musiqay.app.ui.theme.premiumPanelBrush
import com.musiqay.app.ui.theme.premiumAmbientSurface
import com.musiqay.app.ui.theme.premiumScreenBrush

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(premiumScreenBrush())
            .premiumAmbientSurface()
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 54.dp)
                .size(280.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = .16f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = .09f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(230.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            MaterialTheme.colorScheme.tertiary.copy(alpha = .10f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        LazyColumn(
            modifier = Modifier.statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            item {
                val headerShape = RoundedCornerShape(26.dp)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .shadow(6.dp, headerShape)
                        .background(premiumPanelBrush(), headerShape)
                        .border(1.dp, premiumOutlineBrush(), headerShape)
                        .padding(horizontal = 15.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("موسيقاي", fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Text(
                            "موسيقاك... بطريقتك",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    PremiumRoundIcon(Icons.Rounded.Settings, "الإعدادات", onSettings)
                }
            }

            item {
                val shape = RoundedCornerShape(30.dp)
                Box(
                    modifier = Modifier
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                        .fillMaxWidth()
                        .height(156.dp)
                        .shadow(24.dp, shape)
                        .background(premiumHeroBrush(), shape)
                        .border(
                            1.dp,
                            Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = .72f),
                                    Color.White.copy(alpha = .08f)
                                )
                            ),
                            shape
                        )
                        .clickable(onClick = onAllSongs)
                        .padding(18.dp)
                ) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 10.dp, end = 14.dp)
                            .fillMaxWidth(.58f)
                            .height(2.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = .62f),
                                        Color.Transparent
                                    )
                                ),
                                CircleShape
                            )
                    )

                    Box(
                        Modifier
                            .align(Alignment.TopStart)
                            .size(72.dp)
                            .background(Color.White.copy(alpha = .12f), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = .30f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.LibraryMusic,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Column(
                        Modifier.align(Alignment.CenterEnd),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            "موسيقى لكل لحظة",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            if (songs.isEmpty()) "ابدأ بإضافة الموسيقى إلى هاتفك" else "${songs.size} أغنية جاهزة للتشغيل",
                            color = Color.White.copy(alpha = .93f),
                            fontSize = 15.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.GraphicEq,
                                null,
                                tint = Color.White.copy(alpha = .86f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(7.dp))
                            Text(
                                "دع الموسيقى تتحدث",
                                color = Color.White.copy(alpha = .80f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Box(
                        Modifier
                            .align(Alignment.BottomStart)
                            .size(42.dp)
                            .background(Color.White.copy(alpha = .18f), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = .32f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                }
            }

            item {
                Text(
                    "مكتبتك",
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        LibraryCard(
                            "المفضلة",
                            "$favoriteCount أغنية",
                            Icons.Rounded.Favorite,
                            listOf(Color(0xFFC94878), Color(0xFF6C2B67)),
                            onFavorites,
                            Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(10.dp))
                        LibraryCard(
                            "الفنانون",
                            "$artists فنان",
                            Icons.Rounded.Person,
                            listOf(Color(0xFF4667E6), Color(0xFF243878)),
                            onArtists,
                            Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth()) {
                        LibraryCard(
                            "الألبومات",
                            "$albums ألبوم",
                            Icons.Rounded.Album,
                            listOf(Color(0xFF2698A7), Color(0xFF1F6077)),
                            onAlbums,
                            Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(10.dp))
                        LibraryCard(
                            "المجلدات",
                            "$folders مجلد",
                            Icons.Rounded.Folder,
                            listOf(Color(0xFF2C7ED3), Color(0xFF244C84)),
                            onFolders,
                            Modifier.weight(1f)
                        )
                    }
                }
            }

            if (recent.isNotEmpty()) {
                item {
                    SectionTitle(
                        title = "مضاف حديثًا",
                        action = "عرض الكل",
                        onAction = onAllSongs,
                        modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 12.dp)
                    )
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(recent, key = { it.id }) { song ->
                            val recentShape = RoundedCornerShape(20.dp)
                            Column(
                                modifier = Modifier
                                    .width(100.dp)
                                    .shadow(6.dp, recentShape)
                                    .background(
                                        MaterialTheme.colorScheme.surface.copy(alpha = .82f),
                                        recentShape
                                    )
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = .52f),
                                        recentShape
                                    )
                                    .clickable { onSong(song) }
                                    .padding(5.dp)
                            ) {
                                AlbumArtwork(song.artworkUri, Modifier.size(90.dp), 16)
                                Spacer(Modifier.height(7.dp))
                                Text(
                                    song.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.Bold
                                )
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
        }
    }
}

@Composable
private fun PremiumRoundIcon(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .size(44.dp)
            .shadow(8.dp, CircleShape)
            .background(premiumPanelBrush(), CircleShape)
            .border(1.dp, premiumOutlineBrush(), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, description, tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun LibraryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    colors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .height(92.dp)
            .shadow(12.dp, shape)
            .background(Brush.linearGradient(colors), shape)
            .border(1.dp, Color.White.copy(alpha = .14f), shape)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .fillMaxWidth(.55f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = .42f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )
        Box(
            Modifier
                .align(Alignment.TopStart)
                .size(36.dp)
                .background(Color.White.copy(alpha = .16f), CircleShape)
                .border(1.dp, Color.White.copy(alpha = .16f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.align(Alignment.BottomEnd), horizontalAlignment = Alignment.End) {
            Text(title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Text(
                subtitle,
                color = Color.White.copy(alpha = .80f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
