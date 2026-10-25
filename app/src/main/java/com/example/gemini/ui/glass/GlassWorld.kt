package com.example.gemini.ui.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/**
 * 当前「世界」的录制层：玻璃采样它，所以它只放**会被玻璃看到的内容**（环境底 + 列表/画布）。
 *
 * 两条硬规则，漏一条玻璃就会退化成纯色块（[LiquidGlass] 里 `LocalGlassBackdrop == null` 的兜底分支）：
 *
 * 1. **provider 必须在最外层**：`LocalGlassBackdrop` 要包住所有玻璃面（顶栏、抽屉、输入栏……），
 *    而 CompositionLocal 只向下生效。把 provider 放在录制 Box 里，只有录制 Box 的子孙看得到，
 *    Scaffold 的 `topBar`、`ModalNavigationDrawer` 的 `drawerContent`、玻璃层自己的兄弟节点
 *    全都读不到 → 全部走兜底分支（曾经踩过）。
 * 2. **录制层不能包含玻璃节点本身**：任何挂了 [Modifier.liquidGlass] 的节点都要画在录制 Box
 *    的**外面、之后**，否则会采样到自己（重影）。
 *
 * 典型用法：
 * ```
 * val backdrop = rememberGlassBackdrop()
 * CompositionLocalProvider(LocalGlassBackdrop provides backdrop) {
 *     Scaffold(topBar = { ...liquidGlass(...) }) {      // 玻璃：在录制层之外
 *         GlassWorld(backdrop) { content }              // 录制层：只有内容
 *         DockOverlay()                                 // 玻璃：与录制层同级、之后绘制
 *     }
 * }
 * ```
 */
val LocalGlassBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

/** 每个屏幕建一个；必须与玻璃节点处在同一棵组合里（同一 window）。 */
@Composable
fun rememberGlassBackdrop(): LayerBackdrop = rememberLayerBackdrop()

/**
 * 建立一个可被玻璃采样的世界（只负责**录制**，不提供 CompositionLocal）。
 * 记得在外面用 [LocalGlassBackdrop] 提供同一个 [backdrop]。
 */
@Composable
fun GlassWorld(
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val stableBackdrop = remember(backdrop) { backdrop }
    Box(
        modifier = modifier.layerBackdrop(stableBackdrop),
        content = content,
    )
}
