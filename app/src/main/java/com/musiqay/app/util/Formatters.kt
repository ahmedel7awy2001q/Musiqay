package com.musiqay.app.util

fun formatDuration(ms: Long): String {
    val totalSeconds = ms.coerceAtLeast(0) / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) String.format(java.util.Locale.ROOT, "%d:%02d:%02d", hours, minutes % 60, seconds)
        else String.format(java.util.Locale.ROOT, "%d:%02d", minutes, seconds)
}
