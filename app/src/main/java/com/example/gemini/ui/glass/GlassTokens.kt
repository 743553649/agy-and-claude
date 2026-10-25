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
 * 参数取向照 AndroidLiquidGlass 上游：**折射（lens）负责"液态"，模糊只负责"磨砂"**，
 * 所以 blur 相对克制，lens 是主力；提饱和用 [saturation]。
 */
@Immutable
data class GlassSpec(
    val blur: Dp,
    val saturation: Float,
    /** 采样后的整体亮度偏移（-1..1），用于把玻璃提亮/压暗。 */
    val brightness: Float,
    /** 叠在采样结果之上的染色，也是无法采样时的兜底底色。 */
    val tint: Color,
    /** 采样内容的压暗补偿（0..1）；scrim 画在世界层内部后一般用不到。 */
    val shade: Float,
    val rimTop: Color,
    val rimBottom: Color,
    val highlightAlpha: Float,
    val highlightWidth: Dp,
    /** 顶部静态高光强度（0..1）：不动手时也像一层"湿"的玻璃。 */
    val topSheen: Float,
    /** 底部内反光强度（0..1）：水面从下往上反的那层光，让玻璃有"水感"。 */
    val innerGlow: Float,
    /** 折射边是否带色散（彩虹分离）；只有 API 33+ 生效。上游配方不开。 */
    val chromaticAberration: Boolean,
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
 * 定稿配方（2026-10-25，用户从 A/B/C/S 四组里选定 S「上游原味」）。
 *
 * 逐项对齐 `AndroidLiquidGlass` 的 `ScrollContainerContent` 配方：
 * effects 只有 `vibrancy()` + `lens(16.dp, 32.dp)`（**无模糊、无染色**），
 * highlight 用库默认 `Highlight(0.5.dp, 0.25.dp, α=1)`，
 * shadow 用库默认 `Shadow(24.dp, offsetY 4.dp, 黑 α0.10)`，**innerShadow = null**。
 *
 * 正因为没有染色和模糊，它只在「有颜色、有细节」的背景上成立（上游压的是壁纸），
 * 所以我们才需要 `AmbientBackdrop` 的颗粒层和用户自选底图——纯渐变会让 lens 白做。
 *
 * 与上游字面不同、刻意收窄的两处：
 * - `shadowBlur` 24dp/α0.10 → 16dp/α0.06：上游默认值压在浅底上是一圈灰晕；
 * - `refractionHeight` 16dp → 8dp：16dp 在小面积（顶栏高度 64dp）上会被看成粗描边。
 */
private val upstreamSpec: GlassSpec = GlassSpec(
    blur = 0.dp,
    saturation = 1.5f,
    brightness = 0f,
    tint = Color.Transparent,
    shade = 0f,
    rimTop = Color(0xFFFFFFFF),
    rimBottom = Color(0x26FFFFFF),
    highlightAlpha = 1f,
    highlightWidth = 0.5.dp,
    topSheen = 0f,
    innerGlow = 0f,
    chromaticAberration = false,
    shadowBlur = 16.dp,
    shadowOffsetY = 3.dp,
    shadowAlpha = 0.06f,
    innerShadowAlpha = 0f,
    useLens = true,
    refractionHeight = 8.dp,
    refractionAmount = 20.dp,
)

/** 三个角色共用同一份配方：上游的常量配方不区分顶栏 / 输入栏 / 面板。 */
private val upstreamTokens: GlassTokens = GlassTokens(
    chrome = upstreamSpec,
    dock = upstreamSpec,
    panel = upstreamSpec,
)

/** 暗色定稿。 */
val DarkGlassTokens: GlassTokens = upstreamTokens

/** 亮色定稿：上游配方不含染色，不随明暗反相，所以与暗色同一份。 */
val LightGlassTokens: GlassTokens = upstreamTokens

val LocalGlassTokens = staticCompositionLocalOf { DarkGlassTokens }

/** 总开关：设置里的「省电流畅模式」或性能降级时置 false，玻璃退化为无效果。 */
val LocalGlassEnabled = staticCompositionLocalOf { true }