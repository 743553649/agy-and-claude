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
 * - 0 [A 通透]：低染色 + 高折射，字能隐约透出，最"薄"
 * - 1 [B 厚玻璃]（默认）：增厚折射边 + 强内阴影，最接近上游胶囊的观感
 * - 2 [C 磨砂]：高染色 + 大模糊，可读性优先，接近 iOS 的雾面
 */
object GlassPresets {

    val names: List<String> = listOf("A 通透", "B 厚玻璃", "C 磨砂")

    /** 默认预设下标（B 厚玻璃）。 */
    const val DEFAULT = 1

    private val darkAA: GlassTokens = GlassTokens(
        chrome = spec(
            blur = 8.dp, saturation = 1.6f, brightness = 0.02f, tint = Color(0x1AFFFFFF),
            rimTop = Color(0xF2FFFFFF), rimBottom = Color(0x24FFFFFF),
            highlightAlpha = 0.85f, highlightWidth = 1.2.dp, topSheen = 0.06f,
            shadowBlur = 18.dp, shadowOffsetY = 8.dp, shadowAlpha = 0.40f, innerShadowAlpha = 0.14f,
            refractionHeight = 16.dp, refractionAmount = 28.dp,
        ),
        dock = spec(
            blur = 6.dp, saturation = 1.65f, brightness = 0.02f, tint = Color(0x1FFFFFFF),
            rimTop = Color(0xFFFFFFFF), rimBottom = Color(0x2EFFFFFF),
            highlightAlpha = 0.9f, highlightWidth = 1.4.dp, topSheen = 0.08f,
            shadowBlur = 18.dp, shadowOffsetY = 8.dp, shadowAlpha = 0.38f, innerShadowAlpha = 0.12f,
            refractionHeight = 20.dp, refractionAmount = 34.dp,
        ),
        panel = spec(
            blur = 12.dp, saturation = 1.45f, brightness = 0f, tint = Color(0x1AFFFFFF),
            rimTop = Color(0xD9FFFFFF), rimBottom = Color(0x1FFFFFFF),
            highlightAlpha = 0.75f, highlightWidth = 1.2.dp, topSheen = 0.06f,
            shadowBlur = 30.dp, shadowOffsetY = 12.dp, shadowAlpha = 0.42f, innerShadowAlpha = 0.16f,
            refractionHeight = 16.dp, refractionAmount = 26.dp,
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
            blur = 8.dp, saturation = 1.35f, brightness = 0.03f, tint = Color(0x38FFFFFF),
            rimTop = Color(0xFFFFFFFF), rimBottom = Color(0x3DFFFFFF),
            highlightAlpha = 0.9f, highlightWidth = 1.2.dp, topSheen = 0.10f,
            shadowBlur = 14.dp, shadowOffsetY = 6.dp, shadowAlpha = 0.20f, innerShadowAlpha = 0.10f,
            refractionHeight = 16.dp, refractionAmount = 28.dp,
        ),
        dock = spec(
            blur = 6.dp, saturation = 1.35f, brightness = 0.03f, tint = Color(0x42FFFFFF),
            rimTop = Color(0xFFFFFFFF), rimBottom = Color(0x47FFFFFF),
            highlightAlpha = 0.95f, highlightWidth = 1.4.dp, topSheen = 0.12f,
            shadowBlur = 16.dp, shadowOffsetY = 6.dp, shadowAlpha = 0.22f, innerShadowAlpha = 0.08f,
            refractionHeight = 20.dp, refractionAmount = 34.dp,
        ),
        panel = spec(
            blur = 12.dp, saturation = 1.3f, brightness = 0f, tint = Color(0x3DFFFFFF),
            rimTop = Color(0xF2FFFFFF), rimBottom = Color(0x33FFFFFF),
            highlightAlpha = 0.8f, highlightWidth = 1.2.dp, topSheen = 0.08f,
            shadowBlur = 24.dp, shadowOffsetY = 10.dp, shadowAlpha = 0.20f, innerShadowAlpha = 0.10f,
            refractionHeight = 16.dp, refractionAmount = 26.dp,
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

    val dark: List<GlassTokens> = listOf(darkAA, darkBB, darkCC)
    val light: List<GlassTokens> = listOf(lightAA, lightBB, lightCC)

    fun of(isDark: Boolean): List<GlassTokens> = if (isDark) dark else light
}

/** 默认预设（B 厚玻璃）：Theme 层先给一份，具体屏幕可用 [LocalGlassTokens] 覆盖。 */
val DarkGlassTokens: GlassTokens = GlassPresets.dark[GlassPresets.DEFAULT]
val LightGlassTokens: GlassTokens = GlassPresets.light[GlassPresets.DEFAULT]

val LocalGlassTokens = staticCompositionLocalOf { DarkGlassTokens }

/** 总开关：设置里的「省电流畅模式」或性能降级时置 false，玻璃退化为无效果。 */
val LocalGlassEnabled = staticCompositionLocalOf { true }
