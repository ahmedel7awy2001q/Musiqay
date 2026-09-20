package com.musiqay.app.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musiqay.app.BuildConfig
import com.musiqay.app.data.ThemeMode
import com.musiqay.app.ui.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MusicViewModel, onBack: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text("الإعدادات", fontWeight = FontWeight.Black, fontSize = 27.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF050B19),
                            Color(0xFF0A1430),
                            Color(0xFF050B19)
                        )
                    )
                )
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                SectionLabel("المظهر")

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ThemeChoice(
                        title = "حسب الهاتف",
                        icon = Icons.Rounded.PhoneAndroid,
                        selected = settings.themeMode == ThemeMode.SYSTEM,
                        onClick = { vm.setTheme(ThemeMode.SYSTEM) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeChoice(
                        title = "الوضع الداكن",
                        icon = Icons.Rounded.DarkMode,
                        selected = settings.themeMode == ThemeMode.DARK,
                        onClick = { vm.setTheme(ThemeMode.DARK) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeChoice(
                        title = "الوضع الفاتح",
                        icon = Icons.Rounded.LightMode,
                        selected = settings.themeMode == ThemeMode.LIGHT,
                        onClick = { vm.setTheme(ThemeMode.LIGHT) },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Spacer(Modifier.height(12.dp))
                    SettingCard {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Tune, null, tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text("ألوان ديناميكية", fontWeight = FontWeight.Bold)
                                Text(
                                    "استخدام ألوان خلفية الهاتف عند توفرها",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.dynamicColors,
                                onCheckedChange = vm::setDynamicColors
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                SectionLabel("المكتبة")

                SettingCard(
                    modifier = Modifier.clickable { vm.refreshLibrary() }
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Refresh, null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text("إعادة فحص الموسيقى", fontWeight = FontWeight.Bold)
                            Text(
                                "تحديث الأغاني والألبومات والمجلدات من الهاتف",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                SettingCard {
                    Column {
                        Text("الحد الأدنى لمدة الملف", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        listOf(
                            0 to "بدون فلترة",
                            10 to "10 ثواني",
                            20 to "20 ثانية",
                            60 to "60 ثانية"
                        ).forEach { (seconds, label) ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { vm.setMinimumAudioDuration(seconds) }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = settings.minimumAudioDurationSeconds == seconds,
                                    onClick = { vm.setMinimumAudioDuration(seconds) }
                                )
                                Text(label, Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                SettingCard {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Tune, null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text("عرض كل الملفات الصوتية", fontWeight = FontWeight.Bold)
                            Text(
                                "يشمل التسجيلات وأصوات التطبيقات والملفات التي لا يصنفها Android كموسيقى",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.includeNonMusicAudio,
                            onCheckedChange = vm::setIncludeNonMusicAudio
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                SettingCard {
                    Column {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Folder, null, tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text("إدارة المجلدات المخفية", fontWeight = FontWeight.Bold)
                                Text(
                                    "\${settings.hiddenFolders.size} مجلد مخفي",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (settings.hiddenFolders.isNotEmpty()) {
                                TextButton(onClick = vm::clearHiddenFolders) {
                                    Text("إظهار الكل")
                                }
                            }
                        }

                        settings.hiddenFolders
                            .sortedWith(String.CASE_INSENSITIVE_ORDER)
                            .forEach { folder ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(folder, modifier = Modifier.weight(1f), maxLines = 1)
                                    TextButton(onClick = { vm.showFolder(folder) }) {
                                        Text("إظهار")
                                    }
                                }
                            }
                    }
                }

                Spacer(Modifier.height(18.dp))
                SectionLabel("الخصوصية")

                SettingCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Rounded.Security, null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.padding(horizontal = 12.dp)) {
                            Text("خصوصيتك أولًا", fontWeight = FontWeight.Bold)
                            Text(
                                "موسيقاي مشغل محلي. الأغاني والمفضلة وقوائم التشغيل تبقى على هاتفك.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text("موسيقاي \${BuildConfig.VERSION_NAME}", fontWeight = FontWeight.Bold)
                Text(
                    "مشغل موسيقى عربي محلي لنظام Android",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Black,
        fontSize = 18.sp,
        modifier = Modifier.padding(vertical = 10.dp)
    )
}

@Composable
private fun ThemeChoice(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .height(104.dp)
            .shadow(if (selected) 14.dp else 4.dp, shape)
            .background(
                Brush.linearGradient(
                    if (selected)
                        listOf(Color(0xFF203B8F), Color(0xFF40266E))
                    else
                        listOf(Color(0xFF111A35), Color(0xFF0C142B))
                ),
                shape
            )
            .border(
                1.dp,
                if (selected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.outlineVariant,
                shape
            )
            .clickable(onClick = onClick)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(42.dp)
                    .background(
                        if (selected)
                            MaterialTheme.colorScheme.primary.copy(alpha = .18f)
                        else
                            Color.White.copy(alpha = .04f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SettingCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .72f),
                        MaterialTheme.colorScheme.surface.copy(alpha = .88f)
                    )
                ),
                shape
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = .85f),
                shape
            )
            .padding(14.dp)
    ) {
        content()
    }
}
