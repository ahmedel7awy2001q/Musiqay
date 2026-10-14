package com.musiqay.app.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

data class CarBluetoothApp(
    val label: String,
    val packageName: String,
    val activityName: String
)

private const val PREFS = "musiqay_car_bluetooth"
private const val KEY_LABEL = "label"
private const val KEY_PACKAGE = "package"
private const val KEY_ACTIVITY = "activity"

fun savedCarBluetoothApp(context: Context): CarBluetoothApp? {
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val pkg = prefs.getString(KEY_PACKAGE, null)?.takeIf { it.isNotBlank() } ?: return null
    val activity = prefs.getString(KEY_ACTIVITY, null)?.takeIf { it.isNotBlank() } ?: return null
    val label = prefs.getString(KEY_LABEL, null)?.takeIf { it.isNotBlank() } ?: "Bluetooth Music"
    return CarBluetoothApp(label, pkg, activity)
}

fun saveCarBluetoothApp(context: Context, app: CarBluetoothApp) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_LABEL, app.label)
        .putString(KEY_PACKAGE, app.packageName)
        .putString(KEY_ACTIVITY, app.activityName)
        .apply()
}

fun clearCarBluetoothApp(context: Context) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
}

fun launchCarBluetoothApp(context: Context, app: CarBluetoothApp): Boolean {
    val intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
        setClassName(app.packageName, app.activityName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return runCatching {
        context.startActivity(intent)
        true
    }.getOrDefault(false)
}

fun discoverCarBluetoothApps(context: Context, includeAll: Boolean = false): List<CarBluetoothApp> {
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
        if ("bluetooth music" in text || "bt music" in text) value += 140
        if ("a2dp" in text) value += 120
        if ("bluetooth" in text || "بلوتوث" in text) value += 90
        if ("btmusic" in text || "bt_music" in text) value += 90
        if (label.trim().equals("bt", ignoreCase = true) || label.lowercase().startsWith("bt ")) value += 90
        if ("music" in text || "audio" in text) value += 20
        return value
    }

    return matches.mapNotNull { info ->
        val activity = info.activityInfo ?: return@mapNotNull null
        val pkg = activity.packageName ?: return@mapNotNull null
        if (pkg == context.packageName) return@mapNotNull null
        val label = runCatching { info.loadLabel(pm).toString() }.getOrDefault(pkg)
        val rank = score(label, pkg)
        if (!includeAll && rank < 70) return@mapNotNull null
        Triple(CarBluetoothApp(label, pkg, activity.name), rank, label.lowercase())
    }
        .distinctBy { it.first.packageName + "/" + it.first.activityName }
        .sortedWith(
            compareByDescending<Triple<CarBluetoothApp, Int, String>> { it.second }
                .thenBy { it.third }
        )
        .map { it.first }
}
