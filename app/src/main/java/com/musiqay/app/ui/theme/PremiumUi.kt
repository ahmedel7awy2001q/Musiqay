package com.musiqay.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
                colors.primary.copy(alpha = .032f),
                colors.surfaceVariant.copy(alpha = .34f),
                colors.background
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFFFBFCFF),
                colors.primaryContainer.copy(alpha = .10f),
                colors.surface.copy(alpha = .99f),
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
                colors.surfaceVariant.copy(alpha = .72f),
                colors.surface.copy(alpha = .98f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                colors.surface.copy(alpha = .995f),
                colors.surfaceVariant.copy(alpha = .58f)
            )
        )
    }
}

@Composable
fun premiumOutlineBrush(): Brush {
    val colors = MaterialTheme.colorScheme
    val primaryAlpha = if (isPremiumDark()) .24f else .16f
    return Brush.linearGradient(
        listOf(
            colors.primary.copy(alpha = primaryAlpha),
            colors.outlineVariant.copy(alpha = .58f),
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


fun Modifier.premiumAmbientSurface(): Modifier = composed {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.45f
    val glowAlpha = if (dark) .034f else .020f
    val waveAlpha = if (dark) .014f else .008f

    drawBehind {
        val minSide = size.minDimension

        drawCircle(
            color = colors.primary.copy(alpha = glowAlpha),
            radius = minSide * .46f,
            center = Offset(size.width * 1.02f, size.height * .08f)
        )
        drawCircle(
            color = colors.secondary.copy(alpha = glowAlpha * .78f),
            radius = minSide * .34f,
            center = Offset(size.width * -.02f, size.height * .55f)
        )
        drawCircle(
            color = colors.tertiary.copy(alpha = glowAlpha * .62f),
            radius = minSide * .40f,
            center = Offset(size.width * .90f, size.height * .96f)
        )

        val baseY = size.height * .78f
        repeat(1) { index ->
            val path = Path().apply {
                moveTo(-size.width * .05f, baseY + index * 22f)
                cubicTo(
                    size.width * .22f,
                    baseY - 34f + index * 18f,
                    size.width * .48f,
                    baseY + 28f + index * 14f,
                    size.width * .72f,
                    baseY - 18f + index * 20f
                )
                cubicTo(
                    size.width * .88f,
                    baseY - 34f + index * 16f,
                    size.width * 1.02f,
                    baseY + 20f + index * 22f,
                    size.width * 1.08f,
                    baseY + index * 22f
                )
            }
            drawPath(
                path = path,
                color = when (index) {
                    0 -> colors.primary.copy(alpha = waveAlpha)
                    1 -> colors.secondary.copy(alpha = waveAlpha * .82f)
                    else -> colors.tertiary.copy(alpha = waveAlpha * .68f)
                },
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
            )
        }
    }
}
