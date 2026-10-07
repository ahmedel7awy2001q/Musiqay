package com.musiqay.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.musiqay.app.R
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musiqay.app.data.AppSettings
import com.musiqay.app.data.ThemeMode

val LocalReduceMotion = staticCompositionLocalOf { false }

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8EA2FF),
    onPrimary = Color(0xFF07123A),
    primaryContainer = Color(0xFF223A86),
    onPrimaryContainer = Color(0xFFE1E7FF),
    secondary = Color(0xFFC077FF),
    onSecondary = Color(0xFF2B004D),
    secondaryContainer = Color(0xFF4A286B),
    onSecondaryContainer = Color(0xFFF3E0FF),
    tertiary = Color(0xFF58D8FF),
    onTertiary = Color(0xFF003545),
    tertiaryContainer = Color(0xFF0B4E63),
    onTertiaryContainer = Color(0xFFC8F2FF),
    background = Color(0xFF050914),
    onBackground = Color(0xFFF6F7FF),
    surface = Color(0xFF0A1022),
    onSurface = Color(0xFFF6F7FF),
    surfaceVariant = Color(0xFF141E39),
    onSurfaceVariant = Color(0xFFC7CEE5),
    outline = Color(0xFF7C89AF),
    outlineVariant = Color(0xFF2E3A5A)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF4B5FEA),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE1E6FF),
    onPrimaryContainer = Color(0xFF111A50),
    secondary = Color(0xFF8B43D4),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0DFFF),
    onSecondaryContainer = Color(0xFF2B0B45),
    tertiary = Color(0xFF007F9F),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC8F2FF),
    onTertiaryContainer = Color(0xFF003542),
    background = Color(0xFFF8F9FF),
    onBackground = Color(0xFF11131C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF11131C),
    surfaceVariant = Color(0xFFE9EDFA),
    onSurfaceVariant = Color(0xFF525A70),
    outline = Color(0xFF727A91),
    outlineVariant = Color(0xFFC9CEDD)
)

private val ArabicFont = FontFamily(
    Font(R.font.noto_sans_arabic_regular, FontWeight.Normal),
    Font(R.font.noto_sans_arabic_bold, FontWeight.Bold)
)

private val PremiumTypography = Typography(
    headlineLarge = TextStyle(
        fontSize = 32.sp,
        lineHeight = 38.sp,
        fontWeight = FontWeight.Black
    ),
    headlineMedium = TextStyle(
        fontSize = 27.sp,
        lineHeight = 33.sp,
        fontWeight = FontWeight.ExtraBold
    ),
    titleLarge = TextStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold
    ),
    titleMedium = TextStyle(
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Bold
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 23.sp,
        fontWeight = FontWeight.Medium
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 21.sp
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Bold
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold
    )
).let { t ->
    t.copy(
        displayLarge = t.displayLarge.copy(fontFamily = ArabicFont), displayMedium = t.displayMedium.copy(fontFamily = ArabicFont),
        displaySmall = t.displaySmall.copy(fontFamily = ArabicFont), headlineLarge = t.headlineLarge.copy(fontFamily = ArabicFont),
        headlineMedium = t.headlineMedium.copy(fontFamily = ArabicFont), headlineSmall = t.headlineSmall.copy(fontFamily = ArabicFont),
        titleLarge = t.titleLarge.copy(fontFamily = ArabicFont), titleMedium = t.titleMedium.copy(fontFamily = ArabicFont),
        titleSmall = t.titleSmall.copy(fontFamily = ArabicFont), bodyLarge = t.bodyLarge.copy(fontFamily = ArabicFont),
        bodyMedium = t.bodyMedium.copy(fontFamily = ArabicFont), bodySmall = t.bodySmall.copy(fontFamily = ArabicFont),
        labelLarge = t.labelLarge.copy(fontFamily = ArabicFont), labelMedium = t.labelMedium.copy(fontFamily = ArabicFont),
        labelSmall = t.labelSmall.copy(fontFamily = ArabicFont)
    )
}



private val PremiumShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp)
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

    CompositionLocalProvider(LocalReduceMotion provides settings.reduceMotion) {
        MaterialTheme(
            colorScheme = colors,
            typography = PremiumTypography,
            shapes = PremiumShapes,
            content = content
        )
    }
}
