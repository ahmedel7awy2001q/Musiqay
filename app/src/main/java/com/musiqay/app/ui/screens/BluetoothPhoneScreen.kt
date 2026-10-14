package com.musiqay.app.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Launch
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SettingsBluetooth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.musiqay.app.ui.theme.premiumAmbientSurface
import com.musiqay.app.ui.theme.premiumOutlineBrush
import com.musiqay.app.ui.theme.premiumPanelBrush
import com.musiqay.app.ui.theme.premiumScreenBrush

@Composable
fun BluetoothPhoneScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var saved by remember { mutableStateOf(savedCarBluetoothApp(context)) }
    var candidates by remember { mutableStateOf(discoverCarBluetoothApps(context)) }
    var showAll by remember { mutableStateOf(false) }
    var allApps by remember { mutableStateOf<List<CarBluetoothApp>>(emptyList()) }
    val shape = RoundedCornerShape(18.dp)

    fun choose(app: CarBluetoothApp) {
        saveCarBluetoothApp(context, app)
        saved = app
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(premiumScreenBrush())
            .premiumAmbientSurface()
            .statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "رجوع")
                }
                Column(Modifier.weight(1f)) {
                    Text("بلوتوث السيارة", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text("اختر مشغل Bluetooth الأصلي للشاشة مرة واحدة", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalIconButton(onClick = {
                    candidates = discoverCarBluetoothApps(context)
                    allApps = emptyList()
                }) {
                    Icon(Icons.Rounded.Refresh, "تحديث")
                }
            }
        }

        saved?.let { app ->
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(premiumPanelBrush(), shape)
                        .border(.6.dp, premiumOutlineBrush(), shape)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp)
                        )
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text("المشغل المعتمد", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(app.label, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }

                    Button(
                        onClick = {
                            if (!launchCarBluetoothApp(context, app)) {
                                clearCarBluetoothApp(context)
                                saved = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.BluetoothAudio, null)
                        Spacer(Modifier.width(8.dp))
                        Text("فتح مشغل Bluetooth")
                    }

                    OutlinedButton(
                        onClick = {
                            clearCarBluetoothApp(context)
                            saved = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("تغيير التطبيق")
                    }
                }
            }
        }

        if (saved == null) {
            item {
                Surface(
                    shape = shape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .35f)
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text("اختيار أول مرة فقط", fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "اختر التطبيق الذي يشغّل موسيقى الهاتف في شاشة السيارة. بعد الحفظ، الضغط على «بلوتوث الهاتف» من الرئيسية سيفتحه مباشرة.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (candidates.isNotEmpty()) {
                item {
                    Text("تطبيقات مرشحة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(candidates, key = { it.packageName + "/" + it.activityName }) { app ->
                    AppChoiceRow(app = app, selected = false, onClick = { choose(app) })
                }
            }

            item {
                OutlinedButton(
                    onClick = {
                        allApps = discoverCarBluetoothApps(context, includeAll = true)
                        showAll = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (candidates.isEmpty()) "اختيار من تطبيقات الشاشة" else "لم أجده — عرض كل التطبيقات")
                }
            }
        }

        item {
            OutlinedButton(
                onClick = {
                    runCatching { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.SettingsBluetooth, null)
                Spacer(Modifier.width(8.dp))
                Text("إعدادات Bluetooth")
            }
        }

        item {
            Text(
                "Musiqay لا يحاول قراءة اتصال A2DP المخفي أو إعادة بث الصوت؛ المشغل الأصلي للشاشة يظل المسؤول عن صوت الهاتف.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showAll) {
        ModalBottomSheet(onDismissRequest = { showAll = false }) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
                Text("اختر تطبيق Bluetooth الأصلي", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("اختر التطبيق الذي ظهر في صورتك ويشغّل أغاني الهاتف.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    Modifier.fillMaxWidth().heightIn(max = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(allApps, key = { it.packageName + "/" + it.activityName }) { app ->
                        AppChoiceRow(
                            app = app,
                            selected = saved?.packageName == app.packageName && saved?.activityName == app.activityName,
                            onClick = {
                                choose(app)
                                showAll = false
                            }
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun AppChoiceRow(app: CarBluetoothApp, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(15.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .55f)
                else MaterialTheme.colorScheme.surface.copy(alpha = .92f),
                shape
            )
            .border(
                if (selected) .7.dp else .35.dp,
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .5f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = .35f),
                shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.BluetoothAudio, null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(app.label, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        Icon(if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.Launch, null)
    }
}
