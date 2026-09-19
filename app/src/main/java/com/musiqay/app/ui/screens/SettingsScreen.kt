package com.musiqay.app.ui.screens

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musiqay.app.BuildConfig
import com.musiqay.app.data.ThemeMode
import com.musiqay.app.ui.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MusicViewModel, onBack: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = { Text("الإعدادات", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "رجوع") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Text("المظهر", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))

            ThemeMode.entries.forEach { mode ->
                val label = when (mode) {
                    ThemeMode.SYSTEM -> "حسب إعداد الهاتف"
                    ThemeMode.DARK -> "الوضع الداكن"
                    ThemeMode.LIGHT -> "الوضع الفاتح"
                }
                Row(
                    Modifier.fillMaxWidth().clickable { vm.setTheme(mode) }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = settings.themeMode == mode, onClick = { vm.setTheme(mode) })
                    Text(label, Modifier.weight(1f))
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Tune, null)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text("ألوان ديناميكية", fontWeight = FontWeight.SemiBold)
                        Text("استخدام ألوان خلفية الهاتف عند توفرها", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = settings.dynamicColors, onCheckedChange = vm::setDynamicColors)
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            Text("المكتبة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Row(
                Modifier.fillMaxWidth().clickable { vm.refreshLibrary() }.padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Refresh, null)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text("إعادة فحص الموسيقى", fontWeight = FontWeight.SemiBold)
                    Text("تحديث الأغاني والألبومات والمجلدات من الهاتف", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "الحد الأدنى لمدة الملف",
                fontWeight = FontWeight.SemiBold
            )

            listOf(
                0 to "بدون فلترة",
                10 to "10 ثواني",
                20 to "20 ثانية",
                60 to "60 ثانية"
            ).forEach { (seconds, label) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            vm.setMinimumAudioDuration(seconds)
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = settings.minimumAudioDurationSeconds == seconds,
                        onClick = {
                            vm.setMinimumAudioDuration(seconds)
                        }
                    )
                    Text(
                        label,
                        Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Tune, null)

                Column(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        "عرض كل الملفات الصوتية",
                        fontWeight = FontWeight.SemiBold
                    )
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

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f))) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.Security, null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.padding(horizontal = 12.dp)) {
                        Text("خصوصيتك أولًا", fontWeight = FontWeight.Bold)
                        Text(
                            "موسيقاي مشغل محلي. الأغاني والمفضلة وقوائم التشغيل تبقى على هاتفك، ولا يحتاج التطبيق إلى حساب أو خادم سحابي لتشغيل الموسيقى.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text("موسيقاي ${BuildConfig.VERSION_NAME}", fontWeight = FontWeight.Bold)
            Text("مشغل موسيقى عربي محلي لنظام Android", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
