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
    primary = Color(0xFF9DB2FF),
    secondary = Color(0xFFB994FF),
    tertiary = Color(0xFF65D8FF),
    background = Color(0xFF090E1A),
    surface = Color(0xFF11182A),
    surfaceVariant = Color(0xFF1A2340),
    primaryContainer = Color(0xFF24366F),
    secondaryContainer = Color(0xFF352A60),
    onPrimary = Color(0xFF071335),
    onBackground = Color(0xFFF7F8FF),
    onSurface = Color(0xFFF7F8FF),
    onSurfaceVariant = Color(0xFFC5CCE3),
    outline = Color(0xFF69759D)
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
