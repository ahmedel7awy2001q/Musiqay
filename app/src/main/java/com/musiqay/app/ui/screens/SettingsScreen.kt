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
import com.musiqay.app.ui.theme.premiumOutlineBrush
import com.musiqay.app.ui.theme.premiumPanelBrush
import com.musiqay.app.ui.theme.premiumScreenBrush

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MusicViewModel, onBack: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = .94f),
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
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
                .background(premiumScreenBrush())
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
                        Text(
                            "استبعد المقاطع القصيرة من مكتبتك",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                0 to "بدون",
                                10 to "10ث",
                                20 to "20ث",
                                60 to "60ث"
                            ).forEach { (seconds, label) ->
                                val selected = settings.minimumAudioDurationSeconds == seconds
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .background(
                                            if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(14.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outlineVariant,
                                            RoundedCornerShape(14.dp)
                                        )
                                        .clickable { vm.setMinimumAudioDuration(seconds) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        label,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
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
                                    "${settings.hiddenFolders.size} مجلد مخفي",
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
                                        .padding(top = 10.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f),
                                            RoundedCornerShape(14.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
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
                Text("موسيقاي ${BuildConfig.VERSION_NAME}", fontWeight = FontWeight.Bold)
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
    val shape = RoundedCornerShape(22.dp)
    val contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = modifier
            .height(112.dp)
            .shadow(if (selected) 16.dp else 5.dp, shape)
            .background(
                if (selected) {
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                } else {
                    premiumPanelBrush()
                },
                shape
            )
            .border(
                1.dp,
                if (selected) Color.White.copy(alpha = .30f)
                else MaterialTheme.colorScheme.outlineVariant,
                shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(44.dp)
                    .background(
                        if (selected) Color.White.copy(alpha = .16f)
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = .75f),
                        CircleShape
                    )
                    .border(
                        1.dp,
                        if (selected) Color.White.copy(alpha = .20f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = .70f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = contentColor)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1
            )
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
            .shadow(4.dp, shape)
            .background(premiumPanelBrush(), shape)
            .border(1.dp, premiumOutlineBrush(), shape)
            .padding(15.dp)
    ) {
        content()
    }
}
