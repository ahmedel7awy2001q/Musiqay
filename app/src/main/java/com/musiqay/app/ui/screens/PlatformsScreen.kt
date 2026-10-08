package com.musiqay.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.musiqay.app.ui.theme.premiumAmbientSurface
import com.musiqay.app.ui.theme.premiumOutlineBrush
import com.musiqay.app.ui.theme.premiumPanelBrush
import com.musiqay.app.ui.theme.premiumScreenBrush
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private data class MusicPlatform(
    val name: String,
    val packageName: String,
    val description: String,
    val icon: ImageVector,
    val homeUrl: String,
    val searchUrl: (String) -> String,
    val nativeSearchUri: ((String) -> Uri)? = null
)

private val MusicPlatforms = listOf(
    MusicPlatform(
        name = "Spotify",
        packageName = "com.spotify.music",
        description = "افتح Spotify أو ابحث فيه مباشرة",
        icon = Icons.Rounded.GraphicEq,
        homeUrl = "https://open.spotify.com/",
        searchUrl = { q -> "https://open.spotify.com/search/" + urlEncode(q) },
        nativeSearchUri = { q -> Uri.parse("spotify:search:" + Uri.encode(q)) }
    ),
    MusicPlatform(
        name = "YouTube Music",
        packageName = "com.google.android.apps.youtube.music",
        description = "بحث وتشغيل عبر تطبيق YouTube Music الرسمي",
        icon = Icons.Rounded.PlayCircle,
        homeUrl = "https://music.youtube.com/",
        searchUrl = { q -> "https://music.youtube.com/search?q=" + urlEncode(q) }
    ),
    MusicPlatform(
        name = "Anghami",
        packageName = "com.anghami",
        description = "افتح أنغامي أو ابحث من خلال منصته الرسمية",
        icon = Icons.Rounded.Headphones,
        homeUrl = "https://play.anghami.com/",
        searchUrl = { q -> "https://play.anghami.com/search/" + urlEncode(q) }
    ),
    MusicPlatform(
        name = "SoundCloud",
        packageName = "com.soundcloud.android",
        description = "بحث وتشغيل من SoundCloud",
        icon = Icons.Rounded.Cloud,
        homeUrl = "https://soundcloud.com/",
        searchUrl = { q -> "https://soundcloud.com/search?q=" + urlEncode(q) }
    ),
    MusicPlatform(
        name = "Apple Music",
        packageName = "com.apple.android.music",
        description = "فتح Apple Music عند تثبيته على الهاتف",
        icon = Icons.Rounded.MusicNote,
        homeUrl = "https://music.apple.com/",
        searchUrl = { q -> "https://music.apple.com/us/search?term=" + urlEncode(q) }
    ),
    MusicPlatform(
        name = "Deezer",
        packageName = "deezer.android.app",
        description = "فتح Deezer والبحث من خلال منصته",
        icon = Icons.Rounded.Equalizer,
        homeUrl = "https://www.deezer.com/",
        searchUrl = { q -> "https://www.deezer.com/search/" + urlEncode(q) }
    )
)

private fun urlEncode(value: String): String =
    URLEncoder.encode(value, StandardCharsets.UTF_8.toString())

private fun Context.isInstalled(packageName: String): Boolean =
    runCatching { packageManager.getPackageInfo(packageName, 0) }.isSuccess

private fun openPlatform(context: Context, platform: MusicPlatform, query: String?) {
    val cleanQuery = query?.trim().orEmpty()
    if (cleanQuery.isBlank()) {
        context.packageManager.getLaunchIntentForPackage(platform.packageName)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(it)
            return
        }
    } else {
        platform.nativeSearchUri?.let { builder ->
            val nativeIntent = Intent(Intent.ACTION_VIEW, builder(cleanQuery)).apply {
                setPackage(platform.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (nativeIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(nativeIntent)
                return
            }
        }
    }

    val url = if (cleanQuery.isBlank()) platform.homeUrl else platform.searchUrl(cleanQuery)
    val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        setPackage(platform.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (appIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(appIntent)
    } else {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

@Composable
fun PlatformsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .background(premiumScreenBrush())
            .premiumAmbientSurface()
            .statusBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "رجوع")
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("المنصات", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "وصول موحّد لخدمات الموسيقى المثبتة",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val heroShape = RoundedCornerShape(18.dp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(premiumPanelBrush(), heroShape)
                    .border(.5.dp, premiumOutlineBrush(), heroShape)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Hub, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("مركز البحث بين المنصات", fontWeight = FontWeight.Bold)
                }
                Text(
                    "اكتب اسم أغنية أو فنان مرة واحدة، ثم اختر المنصة. التشغيل يبقى داخل التطبيق الرسمي للخدمة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    trailingIcon = {
                        if (query.isNotBlank()) IconButton(onClick = { query = "" }) {
                            Icon(Icons.Rounded.Close, "مسح")
                        }
                    },
                    label = { Text("أغنية، فنان أو ألبوم") }
                )
            }

            MusicPlatforms.forEach { platform ->
                val installed = remember(platform.packageName) {
                    context.isInstalled(platform.packageName)
                }
                val shape = RoundedCornerShape(16.dp)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(premiumPanelBrush(), shape)
                        .border(.4.dp, premiumOutlineBrush(), shape)
                        .clickable { openPlatform(context, platform, query.takeIf { it.isNotBlank() }) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(13.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .62f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(platform.icon, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(platform.name, fontWeight = FontWeight.Bold, maxLines = 1)
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(7.dp),
                                color = if (installed)
                                    MaterialTheme.colorScheme.primary.copy(alpha = .12f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    if (installed) "مثبت" else "ويب",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (installed)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            platform.description,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalIconButton(
                        onClick = { openPlatform(context, platform, query.takeIf { it.isNotBlank() }) },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            if (query.isBlank()) Icons.Rounded.OpenInNew else Icons.Rounded.Search,
                            if (query.isBlank()) "فتح" else "بحث"
                        )
                    }
                }
            }

            Text(
                "موسيقاي لا ينسخ أو يعيد بث محتوى هذه الخدمات؛ هو يفتح التطبيق الرسمي أو صفحة الخدمة ويحافظ على شروط كل منصة.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}
