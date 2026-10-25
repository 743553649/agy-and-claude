package com.example.gemini.ui.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch

/**
 * 按钮级按压反馈（「A 方案」）：按下时轻微放大 + 一团跟随手指的镜面高光，抬手弹回。
 *
 * 三条硬约束（改之前先读，否则会踩坏别的东西）：
 * 1. **不消费指针事件**：`awaitFirstDown(requireUnconsumed = false)` 且全程不 `consume()`，
 *    外层 `IconButton` / `Surface(onClick)` 照常收到点击；与 [pressSheen] 同一套时序。
 * 2. **不新增采样节点**：只对按钮做 `graphicsLayer` 缩放、在现有玻璃之上画高光，不挂 `drawBackdrop`，
 *    所以同屏玻璃块数量不变（性能红线见 `docs/design/DESIGN.md` §6）。
 * 3. **放大只有 1.08**：顶栏按钮间距约 8dp，上游 tab 那种 1.39× 放大放到图标按钮上会压住邻居。
 *
 * 高光用 `RadialGradient + BlendMode.Plus`（上游 demo 用 2.0.x 的 `RuntimeShader`，backdrop 1.0.6
 * 没有那套 API，`GlassSheen.kt` 走的是同一条退路）。
 */
@Composable
fun Modifier.pressFeedback(
    maxScale: Float = 1.08f,
    highlightAlpha: Float = 0.18f,
): Modifier {
    val progress = remember { Animatable(0f) }
    var pointerField by remember { mutableStateOf(Offset.Zero) }
    val scope = rememberCoroutineScope()

    return this
        .graphicsLayer {
            val p = progress.value
            if (p > 0.001f) {
                val s = 1f + (maxScale - 1f) * p
                scaleX = s
                scaleY = s
            }
        }
        .drawWithContent {
            drawContent()
            val p = progress.value
            if (p > 0.01f) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = highlightAlpha * p),
                            Color.White.copy(alpha = highlightAlpha * 0.35f * p),
                            Color.Transparent
                        ),
                        center = pointerField,
                        radius = size.minDimension * 0.9f
                    ),
                    blendMode = BlendMode.Plus
                )
            }
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    pointerField = down.position
                    scope.launch {
                        progress.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = 0.7f,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    }
                    // 跟手到抬起（只观察，不消费）
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        pointerField = change.position
                    }
                    scope.launch {
                        progress.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = 0.9f,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }
                }
            }
        }
}
