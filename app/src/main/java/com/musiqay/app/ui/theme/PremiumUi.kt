package com.musiqay.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

@Composable
fun isPremiumDark(): Boolean =
    MaterialTheme.colorScheme.background.luminance() < 0.45f

@Composable
fun premiumScreenBrush(): Brush {
    val colors = MaterialTheme.colorScheme
    return if (isPremiumDark()) {
        Brush.verticalGradient(
            listOf(
                colors.background,
                colors.surfaceVariant.copy(alpha = .78f),
                colors.background
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFFFBFCFF),
                colors.primaryContainer.copy(alpha = .34f),
                colors.secondaryContainer.copy(alpha = .22f),
                colors.background
            )
        )
    }
}

@Composable
fun premiumPanelBrush(): Brush {
    val colors = MaterialTheme.colorScheme
    return if (isPremiumDark()) {
        Brush.linearGradient(
            listOf(
                colors.surfaceVariant.copy(alpha = .96f),
                colors.surface.copy(alpha = .94f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                colors.surface.copy(alpha = .99f),
                colors.surfaceVariant.copy(alpha = .90f)
            )
        )
    }
}

@Composable
fun premiumOutlineBrush(): Brush {
    val colors = MaterialTheme.colorScheme
    val primaryAlpha = if (isPremiumDark()) .48f else .30f
    return Brush.linearGradient(
        listOf(
            colors.primary.copy(alpha = primaryAlpha),
            colors.outlineVariant.copy(alpha = .92f),
            colors.secondary.copy(alpha = primaryAlpha * .72f)
        )
    )
}
