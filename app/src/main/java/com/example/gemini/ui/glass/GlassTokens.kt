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
 * 定稿配方（2026-10-25 v2，用户指着上游 demo 的胶囊说「就用这个效果，三个面都改过来」）。
 *
 * 依据是上游 `kmp` 分支的 `app/src/commonMain/.../components/LiquidBottomTabs.kt`（截图里那两个胶囊），逐项抄：
 * - `effects = { vibrancy(); blur(8.dp); lens(24.dp, 24.dp) }`
 * - `highlight = Highlight.Default`（0.5dp / blur 0.25dp / α1）
 * - `shadow = Shadow.Default`（24dp / offsetY 4dp / 黑 α0.10）
 * - `innerShadow = null`
 * - `onDrawSurface = { drawRect(containerColor) }`：亮 `#FAFAFA`@0.40、暗 `#121212`@0.40
 *
 * **关键认知**：demo 的玻璃不是「零模糊零染色」。它靠 **40% 染色 + 8dp 模糊** 把身后内容压成色块，
 * 再叠 **24dp 厚折射边**，所以看起来厚、润、有颜色。之前照 `ScrollContainerContent`（同仓库的列表容器）
 * 收出来的「0 模糊 / 0 染色 / 8dp 折射」是另一个组件的配方，不是这张截图里的胶囊。
 *
 * 三个角色共用同一份配方（用户明确要求顶栏/输入栏/抽屉统一），亮暗只差 tint。
 * 附带解决两件事：① 40% 染色 + 8dp 模糊让抽屉不再和身后聊天正文糊在一起；② tint 不再透明，
 * 拟玻璃兜底（`tint.copy(alpha = tint.alpha + 0.5f)`）不会再退化成半透明黑板。
 */
private val demoSpecDark: GlassSpec = GlassSpec(
    blur = 8.dp,
    saturation = 1.5f,
    brightness = 0f,
    // #121212 @ 40%
    tint = Color(0x66121212),
    shade = 0f,
    rimTop = Color(0xFFFFFFFF),
    rimBottom = Color(0x26FFFFFF),
    highlightAlpha = 1f,
    highlightWidth = 0.5.dp,
    topSheen = 0f,
    innerGlow = 0f,
    chromaticAberration = false,
    shadowBlur = 24.dp,
    shadowOffsetY = 4.dp,
    shadowAlpha = 0.10f,
    innerShadowAlpha = 0f,
    useLens = true,
    refractionHeight = 24.dp,
    refractionAmount = 24.dp,
)

/** 亮色：只换染色（`#FAFAFA` @ 40%），其余与暗色相同。 */
private val demoSpecLight: GlassSpec = demoSpecDark.copy(tint = Color(0x66FAFAFA))

/** 暗色定稿：顶栏 / 输入栏 / 抽屉同一份配方。 */
val DarkGlassTokens: GlassTokens = GlassTokens(
    chrome = demoSpecDark,
    dock = demoSpecDark,
    panel = demoSpecDark,
)

/** 亮色定稿：与暗色只差 tint。 */
val LightGlassTokens: GlassTokens = GlassTokens(
    chrome = demoSpecLight,
    dock = demoSpecLight,
    panel = demoSpecLight,
)

val LocalGlassTokens = staticCompositionLocalOf { DarkGlassTokens }

/** 总开关：设置里的「省电流畅模式」或性能降级时置 false，玻璃退化为无效果。 */
val LocalGlassEnabled = staticCompositionLocalOf { true }