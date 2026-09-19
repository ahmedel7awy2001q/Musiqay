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
    primary = Color(0xFFFF67AD),
    secondary = Color(0xFFB783FF),
    tertiary = Color(0xFF6DB8FF),
    background = Color(0xFF07111E),
    surface = Color(0xFF0D1929),
    surfaceVariant = Color(0xFF17243A),
    onPrimary = Color.White,
    onBackground = Color(0xFFF4F5FA),
    onSurface = Color(0xFFF4F5FA),
    onSurfaceVariant = Color(0xFFB9C2D3)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFC62F79),
    secondary = Color(0xFF7247C7),
    tertiary = Color(0xFF286AA8),
    background = Color(0xFFF8F8FC),
    surface = Color.White,
    surfaceVariant = Color(0xFFEEF0F7),
    onBackground = Color(0xFF171923),
    onSurface = Color(0xFF171923),
    onSurfaceVariant = Color(0xFF555B6B)
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
