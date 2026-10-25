# 液态玻璃 UI 重构 · 交接文档

> 面向「下一个接手的人/agent」。读完这一篇就能继续干，不需要翻对话记录。
> 最后更新：分支 `feat/liquid-glass`，HEAD `c0bab8a`，CI run `40199763480`（success）。**观感定稿 = 上游 demo 胶囊配方（40% 染色 + 8dp 模糊 + 24dp 折射），顶栏/输入栏/抽屉统一；按钮不做玻璃，只做配色贴合；调试入口已删。**

## 0. 一页速览

| 项 | 值 |
| --- | --- |
| 分支 | `feat/liquid-glass`（`master` 未被改动，未合并） |
| 范围 | P0–P1 已完成：聊天主屏的顶栏 / 输入栏 / 抽屉 + 世界层 + 世界底图；观感 = 上游 demo 胶囊配方，三个面统一（§4.2） |
| 构建 | **只能走云端 CI**：push 该分支 → `.github/workflows/ci.yml` → `:app:assembleTermuxRelease` → 下载 artifact |
| 产物 | `antiGem-termux-v<版本>-release-<短sha>.apk`（release 签名，包名 `com.termux`） |
| 未做 | P2 弹层伪玻璃（含拟玻璃兜底的黑色退化修复）、P3 其它四屏、P4 省电开关 / DESIGN.md / 性能与无障碍收口 |
| 临时物 | 无。长按顶栏的「玻璃预设切换条」已随定稿删除（§4.3） |
| 待决策 | 无（观感已定稿）。P2 起需要重新排期 |

**继续干活的最短路径**：读 §2（作用域规则）→ §3（代码地图）→ §4（参数系统）→ §8（下一步）。

## 1. 现状（实测，不是估计）

- 提交：`master..HEAD` 共 23 个提交（截至 `f5539e4`，文档提交另计），`git diff --stat master..HEAD` = 20 文件 / +1461 / −38。
- 新增 Kotlin 包：`com.example.gemini.ui.glass`（5 个文件，见 §3）。
- 依赖：新增 `io.github.kyant0:backdrop:1.0.6`；显式声明 `io.github.kyant0:shapes:1.2.0`（backdrop 以 runtime 传递，runtime 不进编译类路径，必须显式）。**没有其它新依赖**（Coil 2.7.0 本来就在）。
- i18n：`python3 scripts/i18n/verify.py` 通过，`values: 2063 / values-zh-rCN: 2060 / 源码引用: 2030`。
- 观感定稿 v1（`4d7929d`）：四组 A/B/C/S 里选 S「上游原味」→ 删 `GlassPresets` 与长按切换条。**但 S 照的是 `ScrollContainerContent`（列表容器），不是用户真正想要的胶囊**。
- 中间态（`ffa15ad`）：抽屉单独走磨砂（blur 20dp + 白 12%/72%），救过一次「抽屉透明、与正文叠字」。
- 观感定稿 v2（`f5539e4`，现行）：用户对着上游 demo 的 Tab 胶囊说「三个面都改成这个效果」→ 照 `LiquidBottomTabs.kt` 重写为 `demoSpecDark`/`demoSpecLight`（暗 `#121212`@40% / 亮 `#FAFAFA`@40% + blur 8dp + lens 24dp/24dp + `Shadow.Default`），三角色共用一份，只有 tint 分亮暗；磨砂例外随之删除（§4.2）。
- 按钮不做玻璃（`3985a50` 撤回了 `f963f3e` 的按压反馈）：用户判断「按钮玻璃」性价比低 → 改为**纯配色贴合**（`c0bab8a`）：输入栏内部控件底色从实心 `background` 换成半透明填充 + 细边（暗色白 10% 填充 / 白 14% 边，亮色黑 6% / 黑 10%），在 40% 染色的玻璃上读作「玻璃里嵌一小块」。顶栏按钮本来就是无底色图标（只带语义色），不需要改。
- **不要给按钮挂 `drawBackdrop`**：那会给每个按钮加一个采样节点，直接破掉「同屏 ≤6 块玻璃」的性能红线（§7-3）。
- 用户实测结论（真机，release 包，**四组预设时期**的结论）：
  - ✅ 顶栏/输入栏背后的内容被模糊、四条圆角有折射、边缘有亮线、按下有高光跟随；
  - ✅ 抽屉玻璃里的聊天内容与身后页面的明暗一致；
  - ✅ 滚动、键盘、抽屉、四屏、性能（D 组）通过；
  - ⚠️ 那轮之后暴露了两个问题：S 的零模糊零染色让抽屉和正文叠字（`ffa15ad` 用磨砂救过）；三个面也都不像 demo 胶囊（`f5539e4` 补齐 40% 染色 + 8dp 模糊 + 24dp 折射后解决）。
- 用户明确要求：**自选相册图作为世界底** —— 已实现（§5.2），实机效果待用户确认。
- 真机验收（另一台手机，不是本机 Termux 那台）：`4d7929d` 已装，用户确认**帧率很丝滑、不掉帧**；`ffa15ad`（抽屉磨砂）与 `f5539e4`（demo 胶囊配方）**均待复验**。

## 2. 架构与三条硬规则（踩过坑，务必遵守）

### 2.1 采样模型

- `rememberLayerBackdrop()` 造一个 `LayerBackdrop`；`Modifier.layerBackdrop(backdrop)` 把一棵子树**录制**进 `GraphicsLayer`；
- 玻璃节点用 `Modifier.drawBackdrop(backdrop, shape, effects, highlight, shadow, innerShadow, onDrawSurface)` **采样**那个图层；
- 绘制顺序（库 `DrawBackdropNode.draw()`，1.0.6）：
  `onDrawBehind → drawBackdropLayer()（采样层+挂 renderEffect）→ onDrawSurface（染色/高光）→ drawContent（自己的内容）→ onDrawFront`。

### 2.2 三条硬规则

1. **`LocalGlassBackdrop` 的 provider 必须在最外层**（`ChatScreen.kt:934-948`）。
   `liquidGlass()` 读到 `null` 时会**静默**走 `frostedGlass()`（半透明色块 + 描边），观感就是"换了个颜色的透明块"。
   CompositionLocal 只向下生效；`Scaffold.topBar`、`ModalNavigationDrawer.drawerContent`、输入栏（GlassWorld 的兄弟）都在录制 Box 之外。
   → 曾经把 provider 放在 `GlassWorld` 内部，导致顶栏/抽屉/输入栏**全部**走兜底，白调了 4 轮参数。现在兜底分支会打 `Log.w("LiquidGlass", ...)`（`LiquidGlass.kt:39-62`），排查先看 logcat。
2. **玻璃节点不能是录制层的子孙**，必须是「录制 Box 之外、之后绘制」的兄弟节点，否则会采样到自己（重影）。
3. **弹层拿不到世界层**：`Dialog` / `ModalBottomSheet` / `Popup` 在**独立 Window**，主窗口的图层采样不到 → 那里的玻璃只能是伪玻璃（P2 的范围）。

### 2.3 世界层里必须包含"玻璃要折射的东西"

- 环境底（`AmbientBackdrop`）+ 聊天内容（列表）；
- **抽屉 scrim 也画在世界层内部**（`ChatScreen.kt:1981-1991`），否则抽屉玻璃采样到的是没被压暗的内容，必然比身后亮一档；
- 对应地 `ModalNavigationDrawer(scrimColor = Color.Transparent)`（`ChatScreen.kt:956`）。

## 3. 代码地图（文件 + 锚点 + 职责）

新增（`app/src/main/java/com/example/gemini/ui/glass/`）：

| 文件 | 关键锚点 | 职责 / 改这里做什么 |
| --- | --- | --- |
| `GlassWorld.kt` | `LocalGlassBackdrop` L36、`rememberGlassBackdrop()` L40、`GlassWorld()` L47 | 世界层的「建层 / 录制」两半；文件头注释写清了三条硬规则 |
| `LiquidGlass.kt` | `Modifier.liquidGlass()` L39、`frostedGlassSurface()` L70、`glassWithBackdrop()` L78、`frostedGlass()` L163 | 唯一渲染入口；`effects`（vibrancy+blur+lens）、`highlight`、`shadow`、`innerShadow`（α≤0 时不挂）、`onDrawSurface`（shade/tint/topSheen/innerGlow） |
| `GlassTokens.kt` | `GlassSpec` L24、`demoSpecDark` L84、`demoSpecLight` L108、`DarkGlassTokens`/`LightGlassTokens` L111/118、`LocalGlassTokens` L124、`LocalGlassEnabled` L127 | 参数系统：定稿 = 上游 demo 胶囊配方，三角色共用，亮暗只差 tint |
| `AmbientBackdrop.kt` | `AmbientBackdrop()` L65、图片分支 L71、`buildGrainBitmap()` L107、`drawAmbient()` L129 | 世界底：极光渐变 + 64×64 可平铺颗粒 + 用户自选图片 + 可读性罩 |
| `GlassSheen.kt` | `Modifier.pressSheen()` L35 | 手指跟随的镜面高光（上游 `InteractiveHighlight` 的等价实现，1.0.6 没有 RuntimeShader API） |

改动（既有文件）：

| 文件 | 锚点 | 改了什么 |
| --- | --- | --- |
| `ui/chat/ChatScreen.kt` | L932 `rememberGlassBackdrop`、L938 `LocalGlassBackdrop` provides、L1093 顶栏玻璃、L1277 `GlassWorld`、L1281 `AmbientBackdrop`、L1964 世界内 scrim、L2177 `SettingsDialog` | 组装整屏：录制层 / 玻璃面 / 让位尺寸（预设切换入口已删，§4.3） |
| `ui/components/ChatInputBar.kt` | L609 玻璃 + `RoundedRectangle(30.dp)`、L610 `pressSheen()`（必须在玻璃**内层**） | 输入栏 |
| `ui/drawer/ChatHistoryDrawer.kt` | L571 `ModalDrawerSheet`、L575 玻璃 | 抽屉（`drawerContainerColor = Transparent`） |
| `ui/chat/SlashCommandOverlay.kt` | `listMaxHeight` 参数 | `/` 面板按「顶栏到输入栏剩余高度」限高 |
| `data/preferences/AuthPreferences.kt` | key `glass_world_image_uri`、`glassWorldImageUri` flow、`saveGlassWorldImageUri()` | 世界底图持久化（DataStore） |
| `ui/chat/ChatViewModel.kt` | `glassWorldImageUri: StateFlow`、`setGlassWorldImageUri()` | 透传 |
| `ui/settings/SettingsDialog.kt` | 参数 L122、`AppearanceSubScreen` L954、选图 launcher L960、UI 行 L1156 | 「世界底图」入口 |
| `theme/Theme.kt` | `LocalGlassTokens` / `LocalIsDarkTheme` | 主题层给默认 tokens（屏幕内可覆盖） |
| `gradle/libs.versions.toml`、`app/build.gradle.kts` | `kyantBackdrop` 1.0.6、`kyantShapes` 1.2.0 | 依赖 |

## 4. 参数系统

### 4.1 `GlassSpec` 字段语义（`GlassTokens.kt:24`）

| 字段 | 含义 | 调它会发生什么 |
| --- | --- | --- |
| `blur` | 采样后的模糊半径 | ↑=更"磨砂/雾"，↓=更"透"（0 = 纯折射） |
| `saturation` | 采样后的饱和（上游 `vibrancy()` = 1.5） | ↑=折射出的颜色更艳 |
| `brightness` | 采样亮度偏移 | 微调整体明暗，别超过 ±0.05 |
| `tint` | 叠在采样上的染色 | 白色 α 越高越"瓷"，α 0 = 纯玻璃 |
| `shade` | 采样内容压暗补偿 | scrim 进世界层后应保持 0 |
| `rimTop` / `rimBottom` | **仅拟玻璃兜底**用 | 真玻璃路径不读 |
| `highlightWidth` / `highlightAlpha` | 边缘亮线（库 `Highlight`，AGSL） | 上游默认 0.5dp/1.0；>1.5dp 会像"白描边" |
| `topSheen` | 顶部 35% 的湿光渐变 | >0.25 会发白 |
| `innerGlow` | 底部 30% 的内反光 | 0.12–0.16 有"水感" |
| `shadowBlur` / `shadowOffsetY` / `shadowAlpha` | 外投影 | 浅底上 >0.1 就是一圈灰晕 |
| `innerShadowAlpha` | 内阴影（厚度） | 0 = 不挂（上游卡片就是这样） |
| `useLens` / `refractionHeight` / `refractionAmount` | 折射 | 高度是**边缘带宽度**，大面积 >12dp 会被看成粗描边 |
| `chromaticAberration` | 折射边色散（API 33+） | 定稿**关闭**（上游不开）；开了会偏"水灵"，但偏离上游配方 |

### 4.2 定稿配方（上游 demo 胶囊，`GlassTokens.kt:83`）

2026-10-25 用户指着上游 demo 的 Tab 胶囊说「顶栏 / 输入栏 / 抽屉都用这个效果」→ 照 `app/src/commonMain/.../components/LiquidBottomTabs.kt` 逐项抄，**三个角色共用一份** `demoSpecDark` / `demoSpecLight`（只差 tint）。

| 字段 | 定稿值 | 上游依据 |
| --- | --- | --- |
| `blur` | **8dp** | `blur(8.dp)` |
| `saturation` | 1.5 | `vibrancy()` = `colorControls(saturation = 1.5f)` |
| `brightness` | 0 | `vibrancy()` 不调亮度 |
| `tint` | 暗 **`#121212`@40%**（`0x66121212`）/ 亮 **`#FAFAFA`@40%**（`0x66FAFAFA`） | `onDrawSurface = { drawRect(containerColor) }` |
| `refractionHeight` / `refractionAmount` | **24dp / 24dp** | `lens(24.dp, 24.dp)` |
| `highlightWidth` / `highlightAlpha` | 0.5dp / 1.0（blur 自动 0.25dp） | `Highlight.Default` |
| `shadowBlur` / `shadowOffsetY` / `shadowAlpha` | 24dp / 4dp / 0.10 | `Shadow.Default`（offsetY = radius/6） |
| `innerShadowAlpha` | 0 | `innerShadow = null` |
| `topSheen` / `innerGlow` / `chromaticAberration` | 0 / 0 / false | 主胶囊没有这三层（色散只出现在选中指示器上） |

**为什么 v1「不像」**：S 照的是 `ScrollContainerContent`（同仓库的列表容器）——`vibrancy() + lens(16dp, 32dp)`、无模糊无染色，折射还被我们自己收窄到 8dp。截图那块胶囊是 `LiquidBottomTabs`，靠 **40% 染色 + 8dp 模糊**把身后内容压成色块，再叠 **24dp 厚折射边**，所以「厚、润、有颜色」。只抄前者，chrome 就是一片几乎看不见的透明片。

附带效果：抽屉不再与聊天正文叠字（40% 染色 + 8dp 模糊）；拟玻璃兜底也不再是黑板（tint 不再透明，见 §7-1）。

### 4.3 调试入口（已删除）

原「长按顶栏 → chip 条切 4 组」已随定稿删除（`4d7929d`）。删除项：`glassPresetIndex` / `glassPresetPickerVisible` / 屏幕内 `LocalGlassTokens provides glassPresetTokens` / 长按 `pointerInput` / chip 条 / `GlassPresets` 全组；顺带清掉两个不再使用的 import（`detectTapGestures`、`pointerInput`）。

现在 `Theme.kt:136-138` 是 **唯一** tokens provider，屏幕内不再覆盖。要改观感只改 `demoSpecDark` 一处（`demoSpecLight` 用 `copy()` 只换 tint）；真要按角色差异化，才在 `GlassTokens(...)` 构造里给三个角色传不同 spec。

## 5. 世界层

### 5.1 内置极光底（`AmbientBackdrop.kt`）

- 暗色：底 `#04060B` + 蓝 `#2E6BFF` / 青 `#00D9C0` / 紫 `#6D3BFF` 三团径向光 + 斜向光带 + 底部压暗；
- 亮色：底 `#EAF1F8` + 冰蓝 / 薄荷淡彩；
- 一次性生成的 64×64 可平铺颗粒（`buildGrainBitmap`）：给模糊/折射提供高频细节，纯渐变会让 lens 白做。

### 5.2 用户自选图（已实现）

- 存储：DataStore key `glass_world_image_uri`（`content://`，空 = 内置极光）；
- 选图：设置 → 外观 → 「世界底图」→ 选择（`ActivityResultContracts.OpenDocument` + `takePersistableUriPermission`，重启仍有效）/ 清除；
- 渲染：`AmbientBackdrop(imageUri != null)` → Coil `AsyncImage(model = uri, contentScale = Crop)` 铺满 + 可读性罩（暗色黑 `0x66→0x99`，亮色白 `0x8C→0xB3`）；
- i18n：`settings_world_image_{title,subtitle,selected,pick,clear}`（中英各 5）；
- 待办：罩的 α 是经验值，需要实机确认；大图首次解码约 0.5s 空白。

## 6. 上游配方对照（省得下一个人再扒源码）

上游仓库 `github.com/Kyant0/AndroidLiquidGlass`，分支 `kmp`。**库和 demo 的分工必须分清**：

| 观感要素 | 归属 | 关键事实 |
| --- | --- | --- |
| 层录制/采样 | 库 `backdrop` | `rememberLayerBackdrop` + `layerBackdrop` + `drawBackdrop` |
| 模糊 / 调色 / 折射 | 库 | `BackdropEffectScope.blur/colorControls/lens`，`effect()` 用 `RenderEffect.createChainEffect` **链式包裹**（不会互相覆盖） |
| 边缘高光 | 库 | `Highlight.Default = (0.5dp, blur 0.25dp, α1)`，`HighlightStyle.Default` 是 AGSL 着色器 |
| 投影 / 内阴影 | 库 | `Shadow.Default = (24dp, offsetY 4dp, 黑 α0.10)`；`InnerShadow.Default = (24dp, offsetY=radius, 黑 α0.15)` |
| 手指跟随高光 | **demo 纯代码** | `app/.../utils/InteractiveHighlight.kt`（用 2.0.x 才公开的 `RuntimeShader`，1.0.6 没有 → 我们改用 `RadialGradient + BlendMode.Plus`，`GlassSheen.kt`） |
| 按下放大折射 | **demo 纯代码** | `DampedDragAnimation.pressProgress` 乘进 `lens(...)`（我们**未实现**，见 §8） |
| 背景 | demo 资源 | `wallpaper_light.webp`（未采用，版权来源不确定；改走用户自选图） |
| 胶囊配方（**现行定稿**） | `components/LiquidBottomTabs.kt` | 主胶囊：`vibrancy(); blur(8.dp); lens(24.dp, 24.dp)`；`onDrawSurface = drawRect(containerColor)`（亮 `#FAFAFA`@0.4 / 暗 `#121212`@0.4）；highlight / shadow 用库默认，`innerShadow = null` |
| 选中指示器（未采用） | 同文件第二段 | `lens(10dp×pressProgress, 14dp×pressProgress, chromaticAberration=true)` + 白/黑 10% + `InnerShadow(8dp×progress)`；我们的类比物是 `pressSheen` |
| 参考配方（**不是**截图那块） | `ScrollContainerContent.kt` | `RoundedRectangle(32.dp)` + `vibrancy(); lens(16.dp, 32.dp)`、无模糊无染色——列表容器，当 chrome 用会得到几乎看不见的透明片 |

其它实测结论：

- `updateEffects()` 在 `SDK >= 31` 时把 `renderEffect` 挂到采样图层，glide 生效没问题；`lens()` 需要 `SDK >= 33`；
- `lens()` 接受的形状：`RoundedRectangularShape`（kyant shapes，连续圆角）**或** `CornerBasedShape`（Compose 普通圆角）——两者都要在 `LiquidGlass.kt` 里判断，漏一个折射会被**静默跳过**；
- 1.0.6 **没有** `RuntimeShader` / `asComposeShader` / `isRuntimeShaderSupported`（那是 2.0.x API），不要照抄 demo 的 utils。

## 7. 已知问题 / 技术债（按优先级）

1. **拟玻璃兜底的黑板问题已随 v2 配方解决**（`f5539e4`）：v1 `tint = Color.Transparent` 时 `frostedGlass()` 的 `tint.copy(alpha + 0.5)` 等于半透明黑；现在 tint 是 40% 底色，兜底算出的 α0.9 是正常半透明卡、`opaque`（省电模式）α0.96 也成立。API<31 / P2 弹层 / P4 省电模式三处都不用再额外修补。
2. **三个面都带 24dp 折射、都在采样**：PLAN §6 红线是「lens 只给 ≤96dp 高的小控件」，但用户明确要求抽屉也用同款胶囊效果，所以 `Panel` 是 82% 屏宽 + 全高的折射面板。**这是用户拍板的取舍**，代价是抽屉每次重绘重算一遍折射；性能数字仍未测（见下一条）。
3. **每帧重录世界层**：`LayerBackdropNode.draw()` = `drawContent()` + `recordLayer(...)`，滚动时每帧重录全屏内容。这是当前最大性能开销，也是 P4「省电模式」的主要理由。量化未做（`docs/project-doc.md` §12 里没有帧率数据）。
4. **按钮只有配色、没有按压动画**（用户 2026-10-25 明确决定，理由：性价比低）：上游的 `pressProgress`（按下放大 + 折射变厚 + 手指跟随高光）**整体未实现**；`GlassSheen` 仍只作用于整条输入栏卡片。将来若要，必须走 B 方案（`rememberCombinedBackdrop` + 每按钮独立采样节点），并重新核算性能预算。
5. **亮色主题**：现在有 40% `#FAFAFA` 染色托底，比 v1（无染色）稳；但白底照片 + 白染仍可能偏「平」。若仍嫌平，优先改**内容底色**，不要再加染色（会退回"瓷片"）。
6. **弹层仍是伪玻璃**（P2）。
7. **其它四屏未接入**（P3，`SettingsDialog` / IDE / Browser / Terminal 仍是既有样式）。
8. **`ui-preview/` 仍是未跟踪文件**（早期 HTML 原型，刻意不入库）。
9. **`GlassSheen` 用 `pointerInput` 劫持输入栏的按压事件**：目前不影响功能，但若后续输入栏要加长按菜单需复核事件消费顺序。

## 8. 下一步（建议顺序）

**P0（收尾，剩 2 项）**
1. ✅ 定稿观感 + 删调试入口（`4d7929d` 删入口；观感在 `f5539e4` 定为**上游 demo 胶囊配方**）。
2. 补 `DESIGN.md`（落点 `docs/design/DESIGN.md`：哪些面用玻璃、亮暗规则、对比度下限、性能红线、动效规则）。
3. 帧率：2026-10-25 用户在另一台真机主观验收「很丝滑、不掉帧」→ 已在 `docs/project-doc.md` §12 记为「主观通过 / 数字未测量」。**数字仍缺**（装机那台没接 adb）；能用 adb 时补 `dumpsys gfxinfo <pkg> framestats`（先 `--reset`，长会话滚 ≥10s）。

**P2（弹层伪玻璃，独立可做）**
- 兜底黑板问题已被 v2 配方顺带解决（§7-1）：弹层直接用 `Modifier.frostedGlassSurface(GlassRole.Panel, shape)`，不用改 `LiquidGlass.kt`。
- 落点：`ModelSelector` / `ThinkingSelector` / `AttachmentSelector` / `ClaudeModeSheets` / `FileManagerDialog` / `SettingsDialog`（都是 Dialog 或 Popup，在独立 Window）。
- 做法：用 `Modifier.frostedGlassSurface(role = GlassRole.Panel, shape)`（`LiquidGlass.kt:70`），参数从 `LocalGlassTokens` 取，保持与主屏同一套观感；**不要**试图采样（采样不到）。
- 验收：弹层观感与主屏玻璃一致、不是"纯色卡片"。

**P3（其它四屏）**
- 每屏各自 `val backdrop = rememberGlassBackdrop()` + 外层 provider + `GlassWorld(backdrop)` 包内容，再把 topBar/dock 换成 `liquidGlass(...)`；顺序与规则见 §2。
- 先做 IDE（有 topBar + 底部工具条，收益最大），Browser / Terminal / 设置次之。

**P4（打磨）**
- 省电/流畅开关：`LocalGlassEnabled = false` 即退化为拟玻璃（`LiquidGlass.kt:44`），建议加设置项 + 2 个 i18n key；
- 按下折射放大（`pressProgress × lens`）：**已决定不做**（见 §7-4）；若重启，只走 B 方案（`rememberCombinedBackdrop` + 独立采样节点，仅适合模型/语言这类互斥选择器）；
- 无障碍：玻璃上的小字（chip 11sp、占位符）对比度检查；`pressSheen` 的 `pointerInput` 要有语义兜底。
- 性能：世界层录制可以按"滑动中"降频（例如滚动时用低频快照），需先量数据。

## 9. 约束与红线（项目级，来自 `AGENTS.md`）

- **只出 termux flavor**（`com.termux`）；`standard`（`com.antigem`）不构建、不出包。这是硬约束，与本地/云端无关。
- **本地不跑 Gradle**：构建一律 push 分支走 CI（`.github/workflows/ci.yml`）。不要新增出 `standard` 包的 workflow。
- **发版只认 `release.yml`**（仅 `version.json` 变更触发）；不要手改版本、不要手工发版。
- **签名**：release 证书 SHA-256 `6F:F1:E3:FF:…:7B:5C`（见 `docs/i18n/archive/HANDOFF.md` §10）。用户手机上已装的 `com.termux` 是另一把证书（`74:bc:bf:2b:…`），**覆盖安装必然失败**，装之前要先卸载（会清 Termux 数据）。debug 包每轮签名不同，跨轮次也不能覆盖。
- **i18n 四步校验**：改任何可见文案都要跑 `python3 scripts/i18n/verify.py`；新增 `R.string` 记得补 `import com.example.gemini.R`（若不在 `ui` 包内）。
- **不要动** `app/src/main/assets/` 下的副本（`preBuild` 会覆盖），要改脚本改 `scripts/*.sh`。
- 提交信息中文，格式 `类型: 简述`；远端用 SSH `git@github.com:743553649/agy-and-claude.git`；**禁止向 master 强推**。
- 设备能力：安装 APK 被策略禁止（`pm` 只允许查询、`am` 不可用、`/app/open` 不收 `file://`），所以"自测"必须由用户在真机完成。

## 10. 排查手册（玻璃"不像玻璃"时按顺序查）

1. `adb logcat | grep LiquidGlass` → 有 `LocalGlassBackdrop 为 null` 就说明 provider 没包住该节点（§2.2 规则 1）。
2. 采样层是否包含要折射的内容：世界层里只有环境底、没有内容 → 看起来像"透明"。
3. 形状类型：连续圆角要实现 `RoundedRectangularShape` 分支，否则折射被跳过（§6）。
4. 参数：`blur=0` + `tint=透明` 是"纯折射"，只有在有颜色的背景上才成立。
5. 弹层：独立 Window 永远采不到（§2.2 规则 3），别再浪费时间。
6. 底色问题：亮色下"白玻璃压白内容"必须靠内容底色或投影解决，加 tint 只会变瓷。

## 11. 验证记录（可复现）

| 日期 | 提交 | CI run | 结果 |
| --- | --- | --- | --- |
| 2026-10-24 | `2a4f819` | `40102062134` | success，产物 `antiGem-termux-2a4f819…` |
| 2026-10-24 | `ebb78e4` | `40164146217` | success（provider 修复后，用户确认模糊/折射/高光/抽屉亮度全部正确） |
| 2026-10-25 | `83ff5b4` | `40171387243` | success（A 组改水灵） |
| 2026-10-25 | `ab1124f` | `40174017029` | success（S 上游原味） |
| 2026-10-25 | `c12975b` | `40177252860` | success（世界底图 + S 组灰晕/描边修正） |
| 2026-10-25 | `4d7929d` | `40185134339` | success（**定稿 S「上游原味」** + 删除 A/B/C 组与长按预设切换条；真机已装，帧率主观通过） |
| 2026-10-25 | `ffa15ad` | `40190873626` | success（抽屉改磨砂 + Panel 亮暗分岔） |
| 2026-10-25 | `f5539e4` | `40191925831` | success（**玻璃改回上游 demo 胶囊配方**：40% 染色 + 8dp 模糊 + 24dp 折射，三面统一） |
| 2026-10-25 | `f963f3e` | `40198912016` | success，**已撤回**（`3985a50`）——按钮按压反馈被用户否掉 |
| 2026-10-25 | `c0bab8a` | `40199763480` | success（**输入栏内部控件改玻璃贴合色** + 撤回按钮玻璃；待装机复验配色） |

下载方式：`gh api repos/743553649/agy-and-claude/actions/runs/<run>/artifacts -q '.artifacts[].archive_download_url'`，或用运行页 Artifacts。
安装前确认签名（`apksigner verify --print-certs`）与包名（`pm list packages com.termux`）。
