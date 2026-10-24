package com.example.gemini.ui.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/**
 * 当前「世界」的录制层：玻璃采样它，所以它只放**会被玻璃看到的内容**（环境底 + 列表/画布）。
 *
 * 硬规则：任何挂了 [Modifier.liquidGlass] 的节点都必须是这个 [GlassWorld] 的**兄弟**，
 * 不能是它的子孙——否则玻璃会采样到自己正在录制的那一层，出现重影/花屏。
 */
val LocalGlassBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

/**
 * 建立一个可被玻璃采样的世界。每个屏幕只建一个。
 */
@Composable
fun GlassWorld(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val backdrop = rememberLayerBackdrop()
    CompositionLocalProvider(LocalGlassBackdrop provides backdrop) {
        Box(
            modifier = modifier.layerBackdrop(backdrop),
            content = content,
        )
    }
}
