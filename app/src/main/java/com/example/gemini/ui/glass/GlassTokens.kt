package com.example.gemini.ui.glass

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 液态玻璃在界面里的三种角色。三者结构相同，只是参数不同：
 * - [Chrome] 顶栏、工具条这类细长、长期驻留的界面壳
 * - [Dock]   输入栏、悬浮按钮这类贴身控件
 * - [Panel]  抽屉、弹层这类大面积遮罩
 */
enum class GlassRole { Chrome, Dock, Panel }

/**
 * 一块玻璃的全部参数。渲染路径见 [Modifier.liquidGlass]。
 *
 * 参数取向照 AndroidLiquidGlass 上游的做法：**折射（lens）负责"液态"，模糊只负责"磨砂"**，
 * 所以 blur 都很小（2–8dp），lens 是主力；vibrancy 用 [saturation] 提饱和。
 */
@Immutable
data class GlassSpec(
    val blur: Dp,
    val saturation: Float,
    /** 采样后的整体亮度偏移（-1..1），用于把玻璃提亮/压暗。 */
    val brightness: Float,
    /** 叠在采样结果之上的染色，也是无法采样时的兜底底色。 */
    val tint: Color,
    /** 采样内容的压暗补偿（0..1）：抽屉/弹层采样到的是没被遮罩压暗的内容，用它拉回来。 */
    val shade: Float,
    val rimTop: Color,
    val rimBottom: Color,
    val highlightAlpha: Float,
    val highlightWidth: Dp,
    /** 顶部静态高光强度（0..1）：不动手时也像一层"湿"的玻璃。 */
    val topSheen: Float,
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
 * 暗色：世界是「中性深灰」（见 [AmbientBackdrop]）。
 * 参照上游 LiquidBottomTabs（栏）与 ScrollContainerContent（面板）的配方配平。
 */
val DarkGlassTokens = GlassTokens(
    // DIAG：临时诊断值（blur 48dp / 无染色 / 折射 40-60dp）。字若仍清晰=renderEffect 未生效。
    chrome = GlassSpec(
        blur = 48.dp,
        saturation = 1.5f,
        brightness = 0.02f,
        tint = Color.Transparent,
        shade = 0f,
        rimTop = Color(0xF2FFFFFF),
        rimBottom = Color(0x26FFFFFF),
        highlightAlpha = 0.9f,
        highlightWidth = 1.5.dp,
        topSheen = 0f,
        shadowBlur = 22.dp,
        shadowOffsetY = 8.dp,
        shadowAlpha = 0.5f,
        innerShadowAlpha = 0.22f,
        useLens = true,
        refractionHeight = 40.dp,
        refractionAmount = 60.dp,
    ),
    // DIAG：同上
    dock = GlassSpec(
        blur = 48.dp,
        saturation = 1.55f,
        brightness = 0.02f,
        tint = Color.Transparent,
        shade = 0f,
        rimTop = Color(0xFFFFFFFF),
        rimBottom = Color(0x2EFFFFFF),
        highlightAlpha = 0.95f,
        highlightWidth = 1.6.dp,
        topSheen = 0f,
        shadowBlur = 20.dp,
        shadowOffsetY = 8.dp,
        shadowAlpha = 0.45f,
        innerShadowAlpha = 0.16f,
        useLens = true,
        refractionHeight = 40.dp,
        refractionAmount = 60.dp,
    ),
    panel = GlassSpec(
        blur = 16.dp,
        saturation = 1.4f,
        brightness = 0f,
        tint = Color(0x1FFFFFFF),
        // scrim 已挪进世界层（ChatScreen.glassDrawerScrim），采样到的内容本身就是压暗的，不再需要 shade
        shade = 0f,
        rimTop = Color(0xCCFFFFFF),
        rimBottom = Color(0x1FFFFFFF),
        highlightAlpha = 0.8f,
        highlightWidth = 1.4.dp,
        topSheen = 0.10f,
        shadowBlur = 36.dp,
        shadowOffsetY = 14.dp,
        shadowAlpha = 0.5f,
        innerShadowAlpha = 0.2f,
        useLens = true,
        refractionHeight = 16.dp,
        refractionAmount = 28.dp,
    ),
)

/**
 * 亮色：同一套结构换参数。染色压到 α≈0.3（再高压在浅底上就是一块实心白条），
 * 靠边缘高光 + 阴影把玻璃立起来；不是把暗色反相。
 */
val LightGlassTokens = GlassTokens(
    // DIAG：亮色同样临时诊断值
    chrome = GlassSpec(
        blur = 48.dp,
        saturation = 1.3f,
        brightness = 0.03f,
        tint = Color.Transparent,
        shade = 0f,
        rimTop = Color(0xFFFFFFFF),
        rimBottom = Color(0x3DFFFFFF),
        highlightAlpha = 0.95f,
        highlightWidth = 1.5.dp,
        topSheen = 0.22f,
        shadowBlur = 16.dp,
        shadowOffsetY = 6.dp,
        shadowAlpha = 0.14f,
        innerShadowAlpha = 0.06f,
        useLens = true,
        refractionHeight = 14.dp,
        refractionAmount = 26.dp,
    ),
    // DIAG
    dock = GlassSpec(
        blur = 48.dp,
        saturation = 1.35f,
        brightness = 0.03f,
        tint = Color.Transparent,
        shade = 0f,
        rimTop = Color(0xFFFFFFFF),
        rimBottom = Color(0x47FFFFFF),
        highlightAlpha = 1f,
        highlightWidth = 1.6.dp,
        topSheen = 0.26f,
        shadowBlur = 18.dp,
        shadowOffsetY = 6.dp,
        shadowAlpha = 0.16f,
        innerShadowAlpha = 0.05f,
        useLens = true,
        refractionHeight = 18.dp,
        refractionAmount = 34.dp,
    ),
    panel = GlassSpec(
        blur = 16.dp,
        saturation = 1.25f,
        brightness = 0f,
        tint = Color(0x5CFFFFFF),
        shade = 0f,
        rimTop = Color(0xFFFFFFFF),
        rimBottom = Color(0x2EFFFFFF),
        highlightAlpha = 0.85f,
        highlightWidth = 1.4.dp,
        topSheen = 0.20f,
        shadowBlur = 30.dp,
        shadowOffsetY = 12.dp,
        shadowAlpha = 0.18f,
        innerShadowAlpha = 0.08f,
        useLens = true,
        refractionHeight = 16.dp,
        refractionAmount = 28.dp,
    ),
)

val LocalGlassTokens = staticCompositionLocalOf { DarkGlassTokens }

/** 总开关：设置里的「省电流畅模式」或性能降级时置 false，玻璃退化为无效果。 */
val LocalGlassEnabled = staticCompositionLocalOf { true }
