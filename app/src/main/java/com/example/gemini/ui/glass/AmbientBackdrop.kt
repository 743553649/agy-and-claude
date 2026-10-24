package com.example.gemini.ui.glass

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.gemini.theme.isAppInDarkTheme

/**
 * 玻璃背后的「中性深灰」世界。
 *
 * 它只做一件事：给玻璃一点可折射的明暗变化。三处低饱和径向光 + 底部压暗，
 * 全部是静态绘制，不涉及任何 RenderEffect，老设备也能画。
 */
@Immutable
private data class AmbientPalette(
    val base: Color,
    val glowTop: Color,
    val glowRight: Color,
    val glowBottom: Color,
    val vignette: Color,
)

private val DarkAmbient = AmbientPalette(
    base = Color(0xFF0E1013),
    glowTop = Color(0x8C2A2F3A),
    glowRight = Color(0x66171F28),
    glowBottom = Color(0x4D241F1B),
    vignette = Color(0x59000000),
)

private val LightAmbient = AmbientPalette(
    base = Color(0xFFF1F2F4),
    glowTop = Color(0xE6FFFFFF),
    glowRight = Color(0xB3E3E7EC),
    glowBottom = Color(0x80EDE7E0),
    vignette = Color(0x14000000),
)

@Composable
fun AmbientBackdrop(
    modifier: Modifier = Modifier,
    isDark: Boolean = isAppInDarkTheme(),
) {
    val palette = if (isDark) DarkAmbient else LightAmbient
    Canvas(modifier = modifier) {
        drawAmbient(palette)
    }
}

private fun DrawScope.drawAmbient(p: AmbientPalette) {
    drawRect(p.base)
    drawRect(
        Brush.radialGradient(
            colors = listOf(p.glowTop, Color.Transparent),
            center = Offset(size.width * 0.10f, -size.height * 0.05f),
            radius = size.width * 1.05f,
        )
    )
    drawRect(
        Brush.radialGradient(
            colors = listOf(p.glowRight, Color.Transparent),
            center = Offset(size.width * 0.98f, size.height * 0.12f),
            radius = size.width * 0.9f,
        )
    )
    drawRect(
        Brush.radialGradient(
            colors = listOf(p.glowBottom, Color.Transparent),
            center = Offset(size.width * 0.62f, size.height * 1.02f),
            radius = size.width,
        )
    )
    drawRect(
        Brush.verticalGradient(
            colors = listOf(Color.Transparent, p.vignette),
            startY = size.height * 0.45f,
            endY = size.height,
        )
    )
}
