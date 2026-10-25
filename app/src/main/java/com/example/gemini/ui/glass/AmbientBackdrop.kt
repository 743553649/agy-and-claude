package com.example.gemini.ui.glass

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
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

// 参考上游壁纸的色相（蓝→青→紫）：纯折射的玻璃必须压在"有色、有细节"的背景上才成立，
// 中性灰会把 lens 的效果吃掉。暗色这边保持低明度，白字仍然清楚。
private val DarkAmbient = AmbientPalette(
    base = Color(0xFF04060B),
    glowTop = Color(0x9E2E6BFF),
    glowRight = Color(0x6600D9C0),
    glowBottom = Color(0x526D3BFF),
    streak = Color(0x1FFFFFFF),
    vignette = Color(0x8C000000),
)

// 亮色用淡彩（冰蓝/薄荷/藕紫）：比纯灰更有色相可供折射，同时保持高明度，深色正文仍可读。
private val LightAmbient = AmbientPalette(
    base = Color(0xFFEAF1F8),
    glowTop = Color(0xF2FFFFFF),
    glowRight = Color(0xBF8FCBF0),
    glowBottom = Color(0x8CA8F0D4),
    streak = Color(0x59FFFFFF),
    vignette = Color(0x24000000),
)

@Composable
fun AmbientBackdrop(
    modifier: Modifier = Modifier,
    isDark: Boolean = isAppInDarkTheme(),
) {
    val palette = if (isDark) DarkAmbient else LightAmbient
    // 一层很淡的颗粒：玻璃的模糊/折射需要有高频细节才看得出"玻璃"，纯渐变会被糊成一片均匀色。
    val grain = remember(isDark) { buildGrainBitmap(isDark) }
    Canvas(modifier = modifier) {
        drawAmbient(palette)
        drawRect(
            brush = ShaderBrush(ImageShader(grain, TileMode.Repeated, TileMode.Repeated))
        )
    }
}

/** 64×64 的可平铺颗粒：暗色用白点、亮色用黑点，透明度都很低。只生成一次。 */
private fun buildGrainBitmap(isDark: Boolean): ImageBitmap {
    val side = 64
    val bitmap = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
    var seed = 0x9E3779B9.toInt()
    for (y in 0 until side) {
        for (x in 0 until side) {
            seed = seed * 1103515245 + 12345
            val r = (seed ushr 16) and 0x7FFF
            if (r % 9 == 0) {
                val alpha = if (isDark) 8 + (r % 14) else 6 + (r % 10)
                val color = if (isDark) {
                    (alpha shl 24) or 0xFFFFFF
                } else {
                    (alpha shl 24) or 0x000000
                }
                bitmap.setPixel(x, y, color)
            }
        }
    }
    return bitmap.asImageBitmap()
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
