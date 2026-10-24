package com.example.gemini.ui.glass

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 液态玻璃在界面里的三种角色。三者结构相同，只是参数不同：
 * - [Chrome] 顶栏、工具条这类细长、长期驻留的界面壳
 * - [Dock]   输入栏、悬浮按钮这类贴身控件（唯一带折射的一类）
 * - [Panel]  抽屉、弹层这类大面积遮罩
 */
enum class GlassRole { Chrome, Dock, Panel }

/** 一块玻璃的全部参数。渲染路径见 [Modifier.liquidGlass]。 */
@Immutable
data class GlassSpec(
    val blur: Dp,
    val saturation: Float,
    /** 叠在采样结果之上的染色，也是无法采样时的兜底底色。 */
    val tint: Color,
    val rimTop: Color,
    val rimBottom: Color,
    val highlightAlpha: Float,
    val shadowBlur: Dp,
    val shadowOffsetY: Dp,
    val shadowAlpha: Float,
    val innerShadowAlpha: Float,
    /** 是否启用折射（需要 API 33+，且形状必须是圆角矩形族）。 */
    val useLens: Boolean,
    val refractionHeight: Dp,
    val refractionAmount: Dp,
)

@Immutable
data class GlassTokens(
    val chrome: GlassSpec,
    val dock: GlassSpec,
    val panel: GlassSpec,
) {
    fun specFor(role: GlassRole): GlassSpec = when (role) {
        GlassRole.Chrome -> chrome
        GlassRole.Dock -> dock
        GlassRole.Panel -> panel
    }
}

/**
 * 暗色：世界是「中性深灰」（见 [AmbientBackdrop]），玻璃靠白色系高光把层次拉开。
 * 数值来自 ui-preview/liquid-glass.html 的玻璃基座，换算成 dp 后按中性灰底重新配平。
 */
val DarkGlassTokens = GlassTokens(
    chrome = GlassSpec(
        blur = 18.dp,
        saturation = 1.35f,
        tint = Color(0x1FFFFFFF),
        rimTop = Color(0xB3FFFFFF),
        rimBottom = Color(0x1AFFFFFF),
        highlightAlpha = 0.55f,
        shadowBlur = 26.dp,
        shadowOffsetY = 10.dp,
        shadowAlpha = 0.55f,
        innerShadowAlpha = 0.12f,
        useLens = false,
        refractionHeight = 0.dp,
        refractionAmount = 0.dp,
    ),
    dock = GlassSpec(
        blur = 8.dp,
        saturation = 1.45f,
        tint = Color(0x24FFFFFF),
        rimTop = Color(0xCCFFFFFF),
        rimBottom = Color(0x24FFFFFF),
        highlightAlpha = 0.8f,
        shadowBlur = 24.dp,
        shadowOffsetY = 8.dp,
        shadowAlpha = 0.45f,
        innerShadowAlpha = 0.10f,
        useLens = true,
        refractionHeight = 12.dp,
        refractionAmount = 20.dp,
    ),
    panel = GlassSpec(
        blur = 26.dp,
        saturation = 1.25f,
        tint = Color(0x1AFFFFFF),
        rimTop = Color(0x99FFFFFF),
        rimBottom = Color(0x14FFFFFF),
        highlightAlpha = 0.5f,
        shadowBlur = 40.dp,
        shadowOffsetY = 16.dp,
        shadowAlpha = 0.55f,
        innerShadowAlpha = 0.16f,
        useLens = false,
        refractionHeight = 0.dp,
        refractionAmount = 0.dp,
    ),
)

/**
 * 亮色：同一套结构换参数——染色更白更厚，高光更淡，阴影更浅；不是把暗色反相。
 */
val LightGlassTokens = GlassTokens(
    chrome = GlassSpec(
        blur = 18.dp,
        saturation = 1.25f,
        tint = Color(0x99FFFFFF),
        rimTop = Color(0xE6FFFFFF),
        rimBottom = Color(0x33FFFFFF),
        highlightAlpha = 0.7f,
        shadowBlur = 20.dp,
        shadowOffsetY = 8.dp,
        shadowAlpha = 0.18f,
        innerShadowAlpha = 0.06f,
        useLens = false,
        refractionHeight = 0.dp,
        refractionAmount = 0.dp,
    ),
    dock = GlassSpec(
        blur = 8.dp,
        saturation = 1.3f,
        tint = Color(0xB8FFFFFF),
        rimTop = Color(0xFFFFFFFF),
        rimBottom = Color(0x3DFFFFFF),
        highlightAlpha = 0.85f,
        shadowBlur = 22.dp,
        shadowOffsetY = 8.dp,
        shadowAlpha = 0.16f,
        innerShadowAlpha = 0.05f,
        useLens = true,
        refractionHeight = 12.dp,
        refractionAmount = 20.dp,
    ),
    panel = GlassSpec(
        blur = 26.dp,
        saturation = 1.2f,
        tint = Color(0xA6FFFFFF),
        rimTop = Color(0xF2FFFFFF),
        rimBottom = Color(0x29FFFFFF),
        highlightAlpha = 0.6f,
        shadowBlur = 34.dp,
        shadowOffsetY = 14.dp,
        shadowAlpha = 0.2f,
        innerShadowAlpha = 0.08f,
        useLens = false,
        refractionHeight = 0.dp,
        refractionAmount = 0.dp,
    ),
)

val LocalGlassTokens = staticCompositionLocalOf { DarkGlassTokens }

/** 总开关：设置里的「省电流畅模式」或性能降级时置 false，玻璃退化为无效果。 */
val LocalGlassEnabled = staticCompositionLocalOf { true }
