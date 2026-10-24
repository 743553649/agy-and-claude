package com.example.gemini.ui.glass

import android.os.Build
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow

/**
 * 把任意容器变成一块液态玻璃。
 *
 * 三级降级，调用方不需要关心：
 * 1. 世界层可用且 API 31+ → 真玻璃：模糊 + 调色 + 边缘高光 + 阴影（API 33+ 再叠折射）
 * 2. 世界层不可用或 API < 31 → 拟玻璃：染色 + 双面描边 + 阴影，不采样，但仍是一块「玻璃」
 * 3. [enabled] = false（省电模式）→ 近乎不透明的拟玻璃，保证文字可读
 *
 * 注意：挂了本 modifier 的节点**不能**是 [GlassWorld] 的子孙，否则会采样到自己。
 */
@Composable
fun Modifier.liquidGlass(
    role: GlassRole,
    shape: Shape = RoundedCornerShape(20.dp),
    enabled: Boolean = LocalGlassEnabled.current,
): Modifier {
    val backdrop = LocalGlassBackdrop.current
    val spec = LocalGlassTokens.current.specFor(role)
    return when {
        !enabled -> frostedGlass(shape, spec, opaque = true)
        backdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            glassWithBackdrop(backdrop, shape, spec)
        else -> frostedGlass(shape, spec, opaque = false)
    }
}

/**
 * 拟玻璃：不采样世界层，纯绘制。用于 API < 31、独立 Window 的弹层、以及玻璃必须
 * 藏在世界层内部的场合（例如还留在世界里的输入栏）。
 */
@Composable
fun Modifier.frostedGlassSurface(
    role: GlassRole,
    shape: Shape = RoundedCornerShape(20.dp),
): Modifier {
    val spec = LocalGlassTokens.current.specFor(role)
    return frostedGlass(shape, spec, opaque = false)
}

private fun Modifier.glassWithBackdrop(
    backdrop: Backdrop,
    shape: Shape,
    spec: GlassSpec,
): Modifier = drawBackdrop(
    backdrop = backdrop,
    shape = { shape },
    effects = {
        colorControls(saturation = spec.saturation)
        blur(spec.blur.toPx())
        if (spec.useLens && shape is CornerBasedShape) {
            lens(
                refractionHeight = spec.refractionHeight.toPx(),
                refractionAmount = spec.refractionAmount.toPx(),
                depthEffect = true,
            )
        }
    },
    highlight = {
        Highlight(width = 1.dp, blurRadius = 0.5.dp, alpha = spec.highlightAlpha)
    },
    shadow = {
        Shadow(
            radius = spec.shadowBlur,
            offset = DpOffset(0.dp, spec.shadowOffsetY),
            color = Color.Black.copy(alpha = spec.shadowAlpha),
        )
    },
    innerShadow = {
        InnerShadow(
            radius = spec.shadowBlur * 0.6f,
            offset = DpOffset(0.dp, spec.shadowOffsetY * 0.5f),
            color = Color.Black.copy(alpha = spec.innerShadowAlpha),
        )
    },
    onDrawSurface = {
        drawRect(spec.tint)
    },
)

private fun Modifier.frostedGlass(
    shape: Shape,
    spec: GlassSpec,
    opaque: Boolean,
): Modifier = this
    .shadow(
        elevation = spec.shadowBlur / 2,
        shape = shape,
        clip = false,
        ambientColor = Color.Black.copy(alpha = spec.shadowAlpha),
        spotColor = Color.Black.copy(alpha = spec.shadowAlpha),
    )
    .clip(shape)
    .drawWithCache {
        val topAlpha = if (opaque) 0.96f else (spec.tint.alpha + 0.55f).coerceAtMost(0.92f)
        val bottomAlpha = if (opaque) 0.92f else (spec.tint.alpha + 0.35f).coerceAtMost(0.88f)
        val fill = Brush.verticalGradient(
            colors = listOf(
                spec.tint.copy(alpha = topAlpha),
                spec.tint.copy(alpha = bottomAlpha),
            )
        )
        val rim = Brush.verticalGradient(colors = listOf(spec.rimTop, spec.rimBottom))
        val outline = shape.createOutline(size, layoutDirection, this)
        val rimWidth = 1.dp.toPx()
        onDrawBehind {
            drawRect(fill)
            drawOutline(outline, rim, style = Stroke(width = rimWidth))
        }
    }
