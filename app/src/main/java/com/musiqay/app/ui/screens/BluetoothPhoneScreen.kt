package com.musiqay.app.ui.screens

import android.annotation.SuppressLint

import android.Manifest
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.Launch
import androidx.compose.material.icons.rounded.PhoneAndroid
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
import androidx.core.content.ContextCompat
import com.musiqay.app.ui.theme.premiumAmbientSurface
import com.musiqay.app.ui.theme.premiumOutlineBrush
import com.musiqay.app.ui.theme.premiumPanelBrush
import com.musiqay.app.ui.theme.premiumScreenBrush

private data class BluetoothAudioApp(
    val label: String,
    val packageName: String,
    val activityName: String
)

@SuppressLint("MissingPermission")
@Composable
fun BluetoothPhoneScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val bluetoothManager = remember { context.getSystemService(BluetoothManager::class.java) }
    val adapter = remember(bluetoothManager) { bluetoothManager?.adapter }

    var permissionGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionGranted = it
    }

    var bluetoothEnabled by remember { mutableStateOf(false) }
    var connectedDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var audioApps by remember { mutableStateOf(findBluetoothAudioApps(context)) }
    var refreshToken by remember { mutableIntStateOf(0) }

    DisposableEffect(adapter, permissionGranted, refreshToken) {
        if (adapter == null || !permissionGranted) {
            bluetoothEnabled = false
            connectedDevices = emptyList()
            onDispose { }
        } else {
            runCatching { bluetoothEnabled = adapter.isEnabled }

            var profile: BluetoothProfile? = null
            val listener = object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profileType: Int, proxy: BluetoothProfile) {
                    if (profileType == BluetoothProfile.A2DP) {
                        profile = proxy
                        connectedDevices = runCatching { proxy.connectedDevices }.getOrDefault(emptyList())
                    }
                }
                override fun onServiceDisconnected(profileType: Int) {
                    if (profileType == BluetoothProfile.A2DP) {
                        profile = null
                        connectedDevices = emptyList()
                    }
                }
            }

            runCatching { adapter.getProfileProxy(context, listener, BluetoothProfile.A2DP) }

            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    when (intent?.action) {
                        BluetoothAdapter.ACTION_STATE_CHANGED -> {
                            bluetoothEnabled = runCatching { adapter.isEnabled }.getOrDefault(false)
                        }
                        BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                            val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED)
                            val device = if (Build.VERSION.SDK_INT >= 33) {
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                            } else {
                                @Suppress("DEPRECATION")
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                            }
                            if (device != null) {
                                connectedDevices = when (state) {
                                    BluetoothProfile.STATE_CONNECTED -> (connectedDevices + device).distinctBy { it.address }
                                    BluetoothProfile.STATE_DISCONNECTED -> connectedDevices.filterNot { it.address == device.address }
                                    else -> connectedDevices
                                }
                            }
                        }
                    }
                }
            }

            val filter = IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            }
            runCatching {
                ContextCompat.registerReceiver(
                    context,
                    receiver,
                    filter,
                    ContextCompat.RECEIVER_EXPORTED
                )
            }

            onDispose {
                runCatching { context.unregisterReceiver(receiver) }
                profile?.let { runCatching { adapter.closeProfileProxy(BluetoothProfile.A2DP, it) } }
            }
        }
    }

    val primaryApp = audioApps.firstOrNull()
    val shape = RoundedCornerShape(18.dp)

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
                    Text("بلوتوث الهاتف", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text("تشغيل صوت الهاتف عبر شاشة السيارة", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalIconButton(onClick = {
                    audioApps = findBluetoothAudioApps(context)
                    refreshToken++
                }) {
                    Icon(Icons.Rounded.Refresh, "تحديث")
                }
            }
        }

        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(premiumPanelBrush(), shape)
                    .border(.5.dp, premiumOutlineBrush(), shape)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (connectedDevices.isNotEmpty()) Icons.Rounded.BluetoothAudio else Icons.Rounded.Bluetooth,
                        null,
                        tint = if (connectedDevices.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(30.dp)
                    )
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(
                            when {
                                adapter == null -> "Bluetooth غير متاح على هذه الشاشة"
                                !permissionGranted -> "يلزم السماح بالوصول إلى Bluetooth"
                                !bluetoothEnabled -> "Bluetooth متوقف"
                                connectedDevices.isNotEmpty() -> "Bluetooth Audio متصل"
                                else -> "Bluetooth يعمل — حالة صوت الهاتف غير متاحة للتطبيق"
                            },
                            fontWeight = FontWeight.Bold
                        )
                        if (connectedDevices.isNotEmpty()) {
                            val names = connectedDevices.map {
                                runCatching { it.name }.getOrNull()?.takeIf(String::isNotBlank) ?: "هاتف متصل"
                            }
                            Text(
                                names.joinToString(" • "),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (!permissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.PhoneAndroid, null)
                        Spacer(Modifier.width(8.dp))
                        Text("السماح بالوصول إلى الهاتف المتصل")
                    }
                }

                if (primaryApp != null) {
                    Button(
                        onClick = { launchBluetoothAudioApp(context, primaryApp) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.BluetoothAudio, null)
                        Spacer(Modifier.width(8.dp))
                        Text("فتح \${primaryApp.label}")
                    }
                }

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
        }

        item {
            Text(
                "مشغلات Bluetooth على الشاشة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (audioApps.isEmpty()) {
            item {
                Surface(
                    shape = shape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = .84f)
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("لم أتعرف تلقائيًا على مشغل Bluetooth مستقل.", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(5.dp))
                        Text(
                            "يمكنك فتح إعدادات Bluetooth الآن. بعض شاشات السيارة تدمج مشغل Bluetooth داخل تطبيق النظام ولا تعرضه كتطبيق مستقل.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(audioApps, key = { it.packageName + "/" + it.activityName }) { app ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = .92f), RoundedCornerShape(15.dp))
                        .border(.35.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .35f), RoundedCornerShape(15.dp))
                        .clickable { launchBluetoothAudioApp(context, app) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.BluetoothAudio, null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                        Text(app.label, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    Icon(Icons.Rounded.Launch, "فتح")
                }
            }
        }

        item {
            Text(
                "ملاحظة: بعض شاشات السيارة لا تكشف اتصال A2DP Sink للتطبيقات العادية. إذا كان الهاتف يعمل بالفعل في مشغل Bluetooth الأصلي، فالصوت سيظل يعمل حتى لو لم تظهر حالة الاتصال هنا. Musiqay يستخدم مشغل الشاشة الأصلي بدل إعادة بث الصوت.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun findBluetoothAudioApps(context: Context): List<BluetoothAudioApp> {
    val pm = context.packageManager
    val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val matches = runCatching {
        if (Build.VERSION.SDK_INT >= 33) {
            pm.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(launcherIntent, 0)
        }
    }.getOrDefault(emptyList())

    fun score(label: String, pkg: String): Int {
        val text = (label + " " + pkg).lowercase()
        var value = 0
        if ("bluetooth music" in text || "bt music" in text) value += 120
        if ("a2dp" in text) value += 100
        if ("bluetooth" in text || "بلوتوث" in text) value += 70
        if ("btmusic" in text || "bt_music" in text) value += 70
        if (label.trim().equals("bt", ignoreCase = true) || label.lowercase().startsWith("bt ")) value += 80
        if ("music" in text || "audio" in text) value += 15
        return value
    }

    return matches.mapNotNull { info ->
        val activity = info.activityInfo ?: return@mapNotNull null
        val pkg = activity.packageName ?: return@mapNotNull null
        if (pkg == context.packageName) return@mapNotNull null
        val label = runCatching { info.loadLabel(pm).toString() }.getOrDefault(pkg)
        val rank = score(label, pkg)
        if (rank < 70) return@mapNotNull null
        Triple(
            BluetoothAudioApp(label = label, packageName = pkg, activityName = activity.name),
            rank,
            pkg
        )
    }
        .distinctBy { it.first.packageName + "/" + it.first.activityName }
        .sortedWith(compareByDescending<Triple<BluetoothAudioApp, Int, String>> { it.second }.thenBy { it.first.label })
        .map { it.first }
        .take(8)
}

private fun launchBluetoothAudioApp(context: Context, app: BluetoothAudioApp) {
    val intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
        setClassName(app.packageName, app.activityName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}
