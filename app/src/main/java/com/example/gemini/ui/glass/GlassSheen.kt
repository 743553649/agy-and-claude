package com.example.gemini.ui.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.awaitPointerEvent
import androidx.compose.ui.input.pointer.awaitPointerEventScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlinx.coroutines.launch

/**
 * 「湿玻璃」的镜面高光：手指按下去时一团高光跟着走，抬起后收回。
 *
 * 上游把这个写在 demo 模块里（`app/.../utils/InteractiveHighlight.kt`），用 2.0.x 才公开的
 * `RuntimeShader` 画径向衰减；backdrop 1.0.6 没有那套 API，这里用 `RadialGradient + BlendMode.Plus`
 * 实现等价观感。
 *
 * 只给体量小、会被按的东西用（输入栏、浮钮、玻璃按钮）；大面板不加，省一次 pointerInput。
 */
@Composable
fun Modifier.pressSheen(
    maxAlpha: Float = 0.16f,
): Modifier {
    val progress = remember { Animatable(0f) }
    val pointer = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val scope = rememberCoroutineScope()

    return this
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    pointer.snapTo(down.position)
                    scope.launch {
                        progress.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = 0.7f,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    }
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        if (change.positionChange() != Offset.Zero) {
                            pointer.snapTo(change.position)
                        }
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
        .drawWithContent {
            val p = progress.value
            if (p > 0.01f) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = maxAlpha * p),
                            Color.White.copy(alpha = maxAlpha * 0.35f * p),
                            Color.Transparent
                        ),
                        center = pointer.value,
                        radius = size.minDimension * 0.9f
                    ),
                    blendMode = BlendMode.Plus
                )
            }
            drawContent()
        }
}
