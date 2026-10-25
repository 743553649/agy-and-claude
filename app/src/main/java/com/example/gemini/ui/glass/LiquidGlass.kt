package com.example.gemini.ui.glass

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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
    // 兜底分支很安静：只有一块半透明色块，和"参数没调好"长得一模一样（踩过一次）。
    // 这里在每个玻璃节点上打一次日志，方便一眼看出是谁没拿到世界层。
    remember(role, backdrop) {
        if (backdrop == null) {
            android.util.Log.w(
                "LiquidGlass",
                "LocalGlassBackdrop 为 null，$role 退化为拟玻璃（没有采样/模糊/折射）——provider 没包住这个节点"
            )
        }
        Unit
    }
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
        // 上游配方：vibrancy（提饱和）+ 小半径模糊 + 大比例折射。折射才是"液态"的来源。
        colorControls(brightness = spec.brightness, saturation = spec.saturation)
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
        Highlight(
            width = spec.highlightWidth,
            blurRadius = spec.highlightWidth / 2,
            alpha = spec.highlightAlpha,
        )
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
        if (spec.shade > 0f) {
            drawRect(Color.Black.copy(alpha = spec.shade))
        }
        drawRect(spec.tint)
        // 静态顶部高光：不用手指也像一层「湿」的玻璃（上游靠 InteractiveHighlight 的常驻态）
        if (spec.topSheen > 0f) {
            drawRect(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = spec.topSheen),
                        Color.White.copy(alpha = spec.topSheen * 0.25f),
                        Color.Transparent
                    ),
                    endY = size.height * 0.85f
                )
            )
        }
    },
)

private fun Modifier.frostedGlass(
    shape: Shape,
    spec: GlassSpec,
    opaque: Boolean,
): Modifier {
    val topAlpha = if (opaque) 0.96f else (spec.tint.alpha + 0.5f).coerceAtMost(0.9f)
    val bottomAlpha = if (opaque) 0.92f else (spec.tint.alpha + 0.3f).coerceAtMost(0.86f)
    return this
        .shadow(
            elevation = spec.shadowBlur / 2,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = spec.shadowAlpha),
            spotColor = Color.Black.copy(alpha = spec.shadowAlpha),
        )
        .clip(shape)
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    spec.tint.copy(alpha = topAlpha),
                    spec.tint.copy(alpha = bottomAlpha),
                )
            )
        )
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(colors = listOf(spec.rimTop, spec.rimBottom)),
            shape = shape,
        )
}
