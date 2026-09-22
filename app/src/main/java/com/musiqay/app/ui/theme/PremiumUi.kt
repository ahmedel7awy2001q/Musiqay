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
                colors.primary.copy(alpha = .10f),
                colors.surfaceVariant.copy(alpha = .72f),
                colors.secondary.copy(alpha = .07f),
                colors.background
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFFFCFDFF),
                colors.primaryContainer.copy(alpha = .42f),
                colors.surface,
                colors.secondaryContainer.copy(alpha = .26f),
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


@Composable
fun premiumHeroBrush(): Brush =
    Brush.linearGradient(
        listOf(
            Color(0xFF4169FF),
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary,
            Color(0xFFC13FEA)
        )
    )
