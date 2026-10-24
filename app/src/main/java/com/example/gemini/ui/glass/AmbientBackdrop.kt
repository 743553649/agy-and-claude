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
 * 它只做一件事：给玻璃提供可被折射/模糊的明暗层次。全部是静态绘制，
 * 不涉及任何 RenderEffect，老设备也能画。
 *
 * 这一版刻意加大了对比：玻璃的折射只在有明暗落差的地方看得见，
 * 一条均匀的灰渐变会让 lens 白做（观感就退化成磨砂塑料）。
 */
@Immutable
private data class AmbientPalette(
    val base: Color,
    val glowTop: Color,
    val glowRight: Color,
    val glowBottom: Color,
    val streak: Color,
    val vignette: Color,
)

private val DarkAmbient = AmbientPalette(
    base = Color(0xFF06070A),
    glowTop = Color(0xB3323A4C),
    glowRight = Color(0x8C16222E),
    glowBottom = Color(0x662A2018),
    streak = Color(0x1AFFFFFF),
    vignette = Color(0x8C000000),
)

private val LightAmbient = AmbientPalette(
    base = Color(0xFFE7E9EE),
    glowTop = Color(0xF2FFFFFF),
    glowRight = Color(0xA6CFD7E2),
    glowBottom = Color(0x8CE2D9CC),
    streak = Color(0x33FFFFFF),
    vignette = Color(0x1F000000),
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

    // 左上：主光源，玻璃的亮边靠它撑住
    drawRect(
        Brush.radialGradient(
            colors = listOf(p.glowTop, Color.Transparent),
            center = Offset(size.width * 0.08f, -size.height * 0.02f),
            radius = size.width * 0.95f,
        )
    )
    // 右上：冷色反光，给折射一个冷暖交界
    drawRect(
        Brush.radialGradient(
            colors = listOf(p.glowRight, Color.Transparent),
            center = Offset(size.width * 1.02f, size.height * 0.16f),
            radius = size.width * 0.85f,
        )
    )
    // 下方：暖色余晖，避免整屏死灰
    drawRect(
        Brush.radialGradient(
            colors = listOf(p.glowBottom, Color.Transparent),
            center = Offset(size.width * 0.58f, size.height * 1.04f),
            radius = size.width * 0.9f,
        )
    )
    // 一道斜向光带：斜边缘最能看出玻璃的折射位移
    drawRect(
        Brush.linearGradient(
            colors = listOf(Color.Transparent, p.streak, Color.Transparent),
            start = Offset(0f, size.height * 0.62f),
            end = Offset(size.width, size.height * 0.18f),
        )
    )
    // 底部压暗，让输入栏那块玻璃有落脚点
    drawRect(
        Brush.verticalGradient(
            colors = listOf(Color.Transparent, p.vignette),
            startY = size.height * 0.35f,
            endY = size.height,
        )
    )
}
