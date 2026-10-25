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
    /** 折射边是否带色散（彩虹分离）；只有 API 33+ 生效，是「水灵」的主要来源之一。 */
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

private fun spec(
    blur: Dp,
    saturation: Float,
    brightness: Float,
    tint: Color,
    rimTop: Color,
    rimBottom: Color,
    highlightAlpha: Float,
    highlightWidth: Dp,
    topSheen: Float,
    innerGlow: Float = 0f,
    chromaticAberration: Boolean = false,
    shadowBlur: Dp,
    shadowOffsetY: Dp,
    shadowAlpha: Float,
    innerShadowAlpha: Float,
    refractionHeight: Dp,
    refractionAmount: Dp,
    shade: Float = 0f,
) = GlassSpec(
    blur = blur,
    saturation = saturation,
    brightness = brightness,
    tint = tint,
    shade = shade,
    rimTop = rimTop,
    rimBottom = rimBottom,
    highlightAlpha = highlightAlpha,
    highlightWidth = highlightWidth,
    topSheen = topSheen,
    innerGlow = innerGlow,
    chromaticAberration = chromaticAberration,
    shadowBlur = shadowBlur,
    shadowOffsetY = shadowOffsetY,
    shadowAlpha = shadowAlpha,
    innerShadowAlpha = innerShadowAlpha,
    useLens = true,
    refractionHeight = refractionHeight,
    refractionAmount = refractionAmount,
)

/**
 * 三组观感预设。用于现场挑参数（长按顶栏切换），定稿后把选中的那组留下、
 * 其余删掉即可，其它代码只依赖 [GlassPresets.of]。
 *
 * - 0 [A 水灵]：低染色 + 强折射 + 边缘色散 + 顶部湿光，最"薄"最亮
 * - 1 [B 厚玻璃]（默认）：增厚折射边 + 强内阴影，最接近上游胶囊的观感
 * - 2 [C 磨砂]：高染色 + 大模糊，可读性优先，接近 iOS 的雾面
 * - 3 [S 上游原味]：照抄 AndroidLiquidGlass 的卡片配方（无模糊、无染色、0.5dp 边光）
 */
object GlassPresets {

    val names: List<String> = listOf("A 水灵", "B 厚玻璃", "C 磨砂", "S 上游原味")

    /** 默认预设下标（B 厚玻璃）。 */
    const val DEFAULT = 1

    private val darkAA: GlassTokens = GlassTokens(
        chrome = spec(
            blur = 6.dp, saturation = 1.75f, brightness = 0.02f, tint = Color(0x12FFFFFF),
            rimTop = Color(0xFFFFFFFF), rimBottom = Color(0x1FFFFFFF),
            highlightAlpha = 1f, highlightWidth = 1.4.dp, topSheen = 0.16f,
            innerGlow = 0.10f, chromaticAberration = true,
            shadowBlur = 18.dp, shadowOffsetY = 8.dp, shadowAlpha = 0.40f, innerShadowAlpha = 0.12f,
            refractionHeight = 14.dp, refractionAmount = 36.dp,
        ),
        dock = spec(
            blur = 5.dp, saturation = 1.8f, brightness = 0.02f, tint = Color(0x18FFFFFF),
            rimTop = Color(0xFFFFFFFF), rimBottom = Color(0x2EFFFFFF),
            highlightAlpha = 1f, highlightWidth = 1.6.dp, topSheen = 0.20f,
            innerGlow = 0.12f, chromaticAberration = true,
            shadowBlur = 18.dp, shadowOffsetY = 8.dp, shadowAlpha = 0.38f, innerShadowAlpha = 0.12f,
            refractionHeight = 16.dp, refractionAmount = 42.dp,
        ),
        panel = spec(
            blur = 10.dp, saturation = 1.5f, brightness = 0f, tint = Color(0x14FFFFFF),
            rimTop = Color(0xE6FFFFFF), rimBottom = Color(0x1FFFFFFF),
            highlightAlpha = 0.85f, highlightWidth = 1.2.dp, topSheen = 0.14f,
            innerGlow = 0.08f, chromaticAberration = true,
            shadowBlur = 30.dp, shadowOffsetY = 12.dp, shadowAlpha = 0.42f, innerShadowAlpha = 0.16f,
            refractionHeight = 14.dp, refractionAmount = 30.dp,
        ),
    )

    private val darkBB: GlassTokens = GlassTokens(
        chrome = spec(
            blur = 14.dp, saturation = 1.5f, brightness = 0.02f, tint = Color(0x24FFFFFF),
            rimTop = Color(0xF2FFFFFF), rimBottom = Color(0x26FFFFFF),
            highlightAlpha = 1f, highlightWidth = 2.dp, topSheen = 0.12f,
            shadowBlur = 26.dp, shadowOffsetY = 10.dp, shadowAlpha = 0.38f, innerShadowAlpha = 0.28f,
            refractionHeight = 20.dp, refractionAmount = 36.dp,
        ),
        dock = spec(
            blur = 12.dp, saturation = 1.55f, brightness = 0.02f, tint = Color(0x2EFFFFFF),
            rimTop = Color(0xFFFFFFFF), rimBottom = Color(0x33FFFFFF),
            highlightAlpha = 1f, highlightWidth = 2.dp, topSheen = 0.16f,
            shadowBlur = 22.dp, shadowOffsetY = 9.dp, shadowAlpha = 0.40f, innerShadowAlpha = 0.20f,
            refractionHeight = 24.dp, refractionAmount = 42.dp,
        ),
        panel = spec(
            blur = 18.dp, saturation = 1.4f, brightness = 0f, tint = Color(0x29FFFFFF),
            rimTop = Color(0xCCFFFFFF), rimBottom = Color(0x24FFFFFF),
            highlightAlpha = 0.85f, highlightWidth = 1.8.dp, topSheen = 0.12f,
            shadowBlur = 36.dp, shadowOffsetY = 14.dp, shadowAlpha = 0.42f, innerShadowAlpha = 0.22f,
            refractionHeight = 18.dp, refractionAmount = 32.dp,
        ),
    )

    private val darkCC: GlassTokens = GlassTokens(
        chrome = spec(
            blur = 20.dp, saturation = 1.35f, brightness = 0.02f, tint = Color(0x3DFFFFFF),
            rimTop = Color(0xE6FFFFFF), rimBottom = Color(0x29FFFFFF),
            highlightAlpha = 0.8f, highlightWidth = 1.4.dp, topSheen = 0.10f,
            shadowBlur = 16.dp, shadowOffsetY = 6.dp, shadowAlpha = 0.30f, innerShadowAlpha = 0.12f,
            refractionHeight = 10.dp, refractionAmount = 16.dp,
        ),
        dock = spec(
            blur = 18.dp, saturation = 1.4f, brightness = 0.02f, tint = Color(0x47FFFFFF),
            rimTop = Color(0xF2FFFFFF), rimBottom = Color(0x33FFFFFF),
            highlightAlpha = 0.85f, highlightWidth = 1.4.dp, topSheen = 0.14f,
            shadowBlur = 16.dp, shadowOffsetY = 6.dp, shadowAlpha = 0.32f, innerShadowAlpha = 0.10f,
            refractionHeight = 12.dp, refractionAmount = 20.dp,
        ),
        panel = spec(
            blur = 22.dp, saturation = 1.3f, brightness = 0f, tint = Color(0x42FFFFFF),
            rimTop = Color(0xCCFFFFFF), rimBottom = Color(0x24FFFFFF),
            highlightAlpha = 0.7f, highlightWidth = 1.2.dp, topSheen = 0.10f,
            shadowBlur = 28.dp, shadowOffsetY = 10.dp, shadowAlpha = 0.36f, innerShadowAlpha = 0.14f,
            refractionHeight = 10.dp, refractionAmount = 18.dp,
        ),
    )

    // 亮色不是暗色反相：白玻璃压浅底，必须靠冷调染色 + 暗边 + 更强的投影立起来
    private val lightAA: GlassTokens = GlassTokens(
        chrome = spec(
            blur = 6.dp, saturation = 1.45f, brightness = 0.03f, tint = Color(0x2EEDF3FB),
            rimTop = Color(0xFFFFFFFF), rimBottom = Color(0x3DFFFFFF),
            highlightAlpha = 1f, highlightWidth = 1.4.dp, topSheen = 0.22f,
            innerGlow = 0.14f, chromaticAberration = true,
            shadowBlur = 14.dp, shadowOffsetY = 6.dp, shadowAlpha = 0.22f, innerShadowAlpha = 0.08f,
            refractionHeight = 14.dp, refractionAmount = 36.dp,
        ),
        dock = spec(
            blur = 5.dp, saturation = 1.5f, brightness = 0.03f, tint = Color(0x38E9F1FA),
            rimTop = Color(0xFFFFFFFF), rimBottom = Color(0x47FFFFFF),
            highlightAlpha = 1f, highlightWidth = 1.6.dp, topSheen = 0.26f,
            innerGlow = 0.16f, chromaticAberration = true,
            shadowBlur = 16.dp, shadowOffsetY = 6.dp, shadowAlpha = 0.24f, innerShadowAlpha = 0.08f,
            refractionHeight = 16.dp, refractionAmount = 42.dp,
        ),
        panel = spec(
            blur = 10.dp, saturation = 1.35f, brightness = 0f, tint = Color(0x33EDF3FB),
            rimTop = Color(0xF2FFFFFF), rimBottom = Color(0x33FFFFFF),
            highlightAlpha = 0.9f, highlightWidth = 1.2.dp, topSheen = 0.20f,
            innerGlow = 0.12f, chromaticAberration = true,
            shadowBlur = 24.dp, shadowOffsetY = 10.dp, shadowAlpha = 0.22f, innerShadowAlpha = 0.10f,
            refractionHeight = 14.dp, refractionAmount = 30.dp,
        ),
    )

    private val lightBB: GlassTokens = GlassTokens(
        chrome = spec(
            blur = 14.dp, saturation = 1.3f, brightness = 0.03f,
            // 偏冷一档的染色：和暖白的内容拉开色相，白玻璃才不会糊成一块
            tint = Color(0x4DE8EEF9),
            rimTop = Color(0xFFFFFFFF), rimBottom = Color(0x2EFFFFFF),
            highlightAlpha = 0.95f, highlightWidth = 1.8.dp, topSheen = 0.14f,
            shadowBlur = 20.dp, shadowOffsetY = 8.dp, shadowAlpha = 0.24f, innerShadowAlpha = 0.12f,
            refractionHeight = 20.dp, refractionAmount = 36.dp,
        ),
        dock = spec(
            blur = 12.dp, saturation = 1.3f, brightness = 0.03f, tint = Color(0x57E6ECF7),
            rimTop = Color(0xFFFFFFFF), rimBottom = Color(0x47FFFFFF),
            highlightAlpha = 1f, highlightWidth = 1.8.dp, topSheen = 0.18f,
            shadowBlur = 22.dp, shadowOffsetY = 8.dp, shadowAlpha = 0.26f, innerShadowAlpha = 0.10f,
            refractionHeight = 24.dp, refractionAmount = 42.dp,
        ),
        panel = spec(
            blur = 18.dp, saturation = 1.25f, brightness = 0f, tint = Color(0x52E8EEF9),
            rimTop = Color(0xFFFFFFFF), rimBottom = Color(0x33FFFFFF),
            highlightAlpha = 0.85f, highlightWidth = 1.6.dp, topSheen = 0.12f,
            shadowBlur = 30.dp, shadowOffsetY = 12.dp, shadowAlpha = 0.24f, innerShadowAlpha = 0.10f,
            refractionHeight = 18.dp, refractionAmount = 32.dp,
        ),
    )

    private val lightCC: GlassTokens = GlassTokens(
        chrome = spec(
            blur = 20.dp, saturation = 1.2f, brightness = 0.03f, tint = Color(0x66FFFFFF),
            rimTop = Color(0xFFFFFFFF), rimBottom = Color(0x42FFFFFF),
            highlightAlpha = 0.85f, highlightWidth = 1.4.dp, topSheen = 0.14f,
            shadowBlur = 16.dp, shadowOffsetY = 6.dp, shadowAlpha = 0.18f, innerShadowAlpha = 0.08f,
            refractionHeight = 10.dp, refractionAmount = 16.dp,
        ),
        dock = spec(
            blur = 18.dp, saturation = 1.25f, brightness = 0.03f, tint = Color(0x70FFFFFF),
            rimTop = Color(0xFFFFFFFF), rimBottom = Color(0x52FFFFFF),
            highlightAlpha = 0.9f, highlightWidth = 1.4.dp, topSheen = 0.16f,
            shadowBlur = 16.dp, shadowOffsetY = 6.dp, shadowAlpha = 0.20f, innerShadowAlpha = 0.06f,
            refractionHeight = 12.dp, refractionAmount = 20.dp,
        ),
        panel = spec(
            blur = 22.dp, saturation = 1.2f, brightness = 0f, tint = Color(0x6BFFFFFF),
            rimTop = Color(0xF2FFFFFF), rimBottom = Color(0x38FFFFFF),
            highlightAlpha = 0.75f, highlightWidth = 1.2.dp, topSheen = 0.12f,
            shadowBlur = 26.dp, shadowOffsetY = 10.dp, shadowAlpha = 0.20f, innerShadowAlpha = 0.08f,
            refractionHeight = 10.dp, refractionAmount = 18.dp,
        ),
    )

    /**
     * S 上游原味：逐项对齐 `AndroidLiquidGlass` 的 ScrollContainerContent 配方——
     * effects 只有 `vibrancy()` + `lens(16.dp, 32.dp)`（**无模糊、无染色**），
     * highlight 用库默认 `Highlight(0.5.dp, 0.25.dp, alpha=1)`，
     * shadow 用库默认 `Shadow(24.dp, offsetY 4.dp, 黑 α0.10)`，**innerShadow = null**。
     *
     * 正因为没有染色和模糊，它只在「有颜色、有细节」的背景上成立（上游压的是壁纸），
     * 这也是它比其它几组更"透"的原因。
     */
    private fun upstream(): GlassTokens {
        fun spec(): GlassSpec = GlassSpec(
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
            // 上游默认 Shadow(24dp, α0.10) 压在浅底上会变成一圈灰晕，这里降到 16dp/α0.06；
            // 折射带 16dp 在大面积上会被看成"很粗的描边"，收到 8dp。
            shadowBlur = 16.dp,
            shadowOffsetY = 3.dp,
            shadowAlpha = 0.06f,
            innerShadowAlpha = 0f,
            useLens = true,
            refractionHeight = 8.dp,
            refractionAmount = 20.dp,
        )
        return GlassTokens(chrome = spec(), dock = spec(), panel = spec())
    }

    private val darkSS: GlassTokens = upstream()
    private val lightSS: GlassTokens = upstream()

    val dark: List<GlassTokens> = listOf(darkAA, darkBB, darkCC, darkSS)
    val light: List<GlassTokens> = listOf(lightAA, lightBB, lightCC, lightSS)

    fun of(isDark: Boolean): List<GlassTokens> = if (isDark) dark else light
}

/** 默认预设（B 厚玻璃）：Theme 层先给一份，具体屏幕可用 [LocalGlassTokens] 覆盖。 */
val DarkGlassTokens: GlassTokens = GlassPresets.dark[GlassPresets.DEFAULT]
val LightGlassTokens: GlassTokens = GlassPresets.light[GlassPresets.DEFAULT]

val LocalGlassTokens = staticCompositionLocalOf { DarkGlassTokens }

/** 总开关：设置里的「省电流畅模式」或性能降级时置 false，玻璃退化为无效果。 */
val LocalGlassEnabled = staticCompositionLocalOf { true }
