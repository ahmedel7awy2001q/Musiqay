package com.musiqay.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.musiqay.app.data.AppSettings
import com.musiqay.app.data.ThemeMode

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FA7FF),
    secondary = Color(0xFFB967FF),
    tertiary = Color(0xFF56D7FF),
    background = Color(0xFF050B19),
    surface = Color(0xFF0B1430),
    surfaceVariant = Color(0xFF121F42),
    primaryContainer = Color(0xFF203B8F),
    secondaryContainer = Color(0xFF40266E),
    onPrimary = Color(0xFF07102B),
    onBackground = Color(0xFFF7F8FF),
    onSurface = Color(0xFFF7F8FF),
    onSurfaceVariant = Color(0xFFC7CEEA),
    outline = Color(0xFF6174AD),
    outlineVariant = Color(0xFF26375F)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF4965D6),
    secondary = Color(0xFF7652C8),
    tertiary = Color(0xFF247DA5),
    background = Color(0xFFF6F7FD),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE9EDFA),
    primaryContainer = Color(0xFFDDE5FF),
    secondaryContainer = Color(0xFFEADFFF),
    onBackground = Color(0xFF101425),
    onSurface = Color(0xFF101425),
    onSurfaceVariant = Color(0xFF555D76),
    outline = Color(0xFF7A829B)
)

@Composable
fun MusiqayTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val context = LocalContext.current
    val colors = if (settings.dynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (dark) DarkColors else LightColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}
