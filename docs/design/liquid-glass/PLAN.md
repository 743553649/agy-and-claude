# Android UI 重构 · 液态玻璃（Liquid Glass）落地计划

> 状态：**计划待确认**（未动任何 app 源码）
> 基线：`master @ 3a664a1`（工作区仅 `ui-preview/` 未跟踪）
> 参考库：<https://github.com/Kyant0/AndroidLiquidGlass>（Maven 坐标 `io.github.kyant0:backdrop`）
> 现有视觉探索：`ui-preview/index.html`（六种方向）、`ui-preview/liquid-glass.html`（液态玻璃 Demo，本计划的视觉母本）

---

## 0. 结论（先读这段）

> **状态：P0–P1 已落地，观感已定稿为上游 demo 胶囊配方（40% 染色 + 8dp 模糊 + 24dp 折射），交接见 [`HANDOFF.md`](HANDOFF.md)**（分支 `feat/liquid-glass`，HEAD `f5539e4`）。
> 本文档是当初的**计划**，进度、踩坑、代码地图与下一步都在 HANDOFF 里。
> 未完成：P2 弹层伪玻璃、P3 其它四屏、P4 省电开关 / DESIGN.md / 性能与无障碍。


1. **能落地，且不需要 fork 上游库**：上游 `backdrop` 已在 Maven Central，用法是「把一个容器录成 GraphicsLayer，玻璃组件再采样这一层做模糊/折射/高光」。
2. **依赖必须锁 `1.0.6`，不能用最新的 `2.0.1`**。理由见 §2：`2.0.1` 是 Kotlin 2.4.10 + Compose MPP 1.12 产物，本项目是 Kotlin 2.3.20 + Compose BOM 2026.03.01（ui/foundation 1.10.6）；`1.0.6` 是 Kotlin 2.3.10 + androidx compose 1.10.3，版本链干净。
3. **只做「chrome 玻璃」，不做「内容玻璃」**：顶栏、输入栏、抽屉、浮钮、部分面板。消息气泡、设置列表项、代码块一律不玻璃——这是 iOS 液态玻璃的真实用法，也是性能红线（每块玻璃都在实时重采样一个层）。
4. **真玻璃有个硬约束**：`Dialog` / `ModalBottomSheet`（M3 1.4 的 `ModalBottomSheetWindow`）跑在**独立 Window**，采不到主窗口的层。所以弹层只能：①伪玻璃（半透明+厚边+高光+阴影，不做实时模糊），或 ②关键弹层改成同窗口面板换取真折射。默认走 ①，需要 ② 时单独定。
5. **minSdk 24**：模糊要 API 31、折射（RuntimeShader）要 API 33。本机是 SDK 34，能吃满；老设备降级成「拟玻璃」而不是关掉效果（见 §4.3）。
6. **验证路径**：项目规则是不在本地跑 Gradle。因此编译验证 = 推分支触发 `ci.yml`；视觉验证 = CI 产出的 APK 装机截图。这两条都需要你点头（见 §10）。

---

## 1. 现状盘点（实测）

| 事实 | 数值 / 位置 |
| --- | --- |
| Kotlin / AGP / Gradle | 2.3.20 / 9.0.1 / 9.1.0 |
| Compose | BOM `2026.03.01` → ui、foundation `1.10.6`，material3 `1.4.0` |
| SDK | compileSdk 36、minSdk **24**、targetSdk 28 |
| flavor | 只有 `termux`（`com.termux`），`standard` 不出包 |
| Kotlin 文件 | 194 个，`ui/` 下 14 个子包 |
| 主题 | `theme/Theme.kt`（`GeminiTheme` 在 111 行）、`Color.kt`（陶土橙 `#D97706` + 暖炭黑 `#181816`）、`Type.kt`（仅覆盖 bodyLarge） |
| Material3 用量 | `Surface(` 382 处、`Button(` 128、`OutlinedTextField(` 68、`AlertDialog(` 68、`Card(` 58、`TopAppBar(` 6、`Scaffold(` 6、`ModalBottomSheet(` 6 |
| 单文件最大 | `settings/SettingsDialog.kt` 475 KB（98 个 Surface）、`chat/ChatScreen.kt` 157 KB |

**主屏结构**（改造关键）

- `MainActivity.kt:197` `Surface(color = colorScheme.background)` 包住全部屏幕；Chat / IDE / Terminal / Browser 四个 Screen 用 `graphicsLayer(alpha/translationX)` 常驻叠放（`:262-330`）。
- `ChatScreen.kt:929` `ModalNavigationDrawer` → `ChatHistoryDrawer`；`:1072` `TopAppBar`（背景色写死 `:1230-1235`）；`:1424` `LazyColumn`；`:1981` `ChatInputBar`。
- `ChatInputBar.kt:593` 外层 `Surface(color = background)`，`:603` 内层圆角输入卡（`RoundedCornerShape(24.dp)`，暗色 `#21211E`）。
- `ChatHistoryDrawer.kt:569` `ModalDrawerSheet(drawerContainerColor = surface)`。
- `IdeScreen.kt:893/897`、`BrowserScreen.kt` 同样用 `colorScheme.background` 铺底。

**结论**：现在没有「玻璃后面的世界」——四块屏幕都是纯色底。液态玻璃要好看，必须先有一个可被采样的世界层，否则玻璃只是「半透明灰块」。

---

## 2. 依赖决策（已核对 Maven Central）

| 候选 | Kotlin stdlib | Compose 依赖 | 判断 |
| --- | --- | --- | --- |
| `2.0.1`（最新） | 2.4.10 | `org.jetbrains.compose.*` 1.12.0 | ❌ 元数据比本项目编译器新一个 minor，且拉进 MPP 版本链 |
| `1.0.6` | 2.3.10 | `androidx.compose.{foundation,ui,ui-graphics}` **1.10.3** | ✅ 与本项目 BOM 同源，1.10.6 > 1.10.3 直接胜出 |

`1.0.6` 的公开 API 与 `2.0.1` 在本计划用到的部分**签名一致**：

```kotlin
fun Modifier.drawBackdrop(
    backdrop: Backdrop,
    shape: () -> Shape,
    effects: BackdropEffectScope.() -> Unit,
    highlight: (() -> Highlight?)? = { Highlight.Default },
    shadow: (() -> Shadow?)? = { Shadow.Default },
    innerShadow: (() -> InnerShadow?)? = null,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
    onDrawSurface: (DrawScope.() -> Unit)? = null, ...
): Modifier

// effects 域内可用：blur(radius) / lens(refractionHeight, refractionAmount, depthEffect, chromaticAberration)
//                vibrancy() / colorControls(brightness, contrast, saturation) / opacity(alpha)
// 世界层：rememberLayerBackdrop() + Modifier.layerBackdrop(backdrop)
```

需要新增的依赖只有一条（外加一个版本号）。`shapes`、`annotations` 是它的传递依赖，无需手动加。

另外：`app/build.gradle.kts` 里 `buildFeatures { shaders = false }` **不用改**——该库的 AGSL 是 Kotlin 字符串常量 + `RuntimeShader`，走不到 AGP 的 shader 编译。

---

## 3. 设计方向（Operate 模式，克制）

### 3.1 世界层（玻璃背后的东西）

品牌现有的暖炭黑 + 陶土橙不能丢，但它作为「世界」太安静，玻璃会看不到东西。方案：**「工作台暮色」静态环境底**（对应 `liquid-glass.html` 的 `.wp`，从多色霓虹降饱和到品牌域）：

- 底色 `#0B0E13`（暗）/ `#F6F7F9`（亮）。
- 四角低饱和径向光：陶土橙余晖（左上，α≈0.22）、青灰（右上，α≈0.16）、暗绿（左下）、朱红（右下）。
- 3px 点阵噪点，overlay 混合，抵消渐变的「塑料感」。
- 全部是静态 `Brush` / `drawWithCache`，**零 RenderEffect**，老设备也能画。

玻璃采样的是「世界 + 屏幕内容」，所以消息列表滚动时输入栏下方会有真实位移感——这是本方案唯一的「哇」时刻。

### 3.2 玻璃的三种角色（不搞第四种）

| 角色 | 用在哪 | 效果链 |
| --- | --- | --- |
| `Chrome` | 顶栏、IDE/Browser 工具条、抽屉顶部 | `blur(18dp) → colorControls(saturation 1.35)` + 细边缘高光 + 轻阴影 |
| `Dock` | 输入栏、悬浮切换器、圆形浮钮 | `vibrancy() → blur(6dp) → lens(12dp, 24dp, depthEffect=true)` + 强边缘高光 |
| `Panel` | 抽屉底板、弹层、设置容器 | `blur(28dp)` + 深阴影 + 无折射 |

亮色世界用同一套结构、换参数（tint 更白、高光更淡、阴影更浅），**不是反色**。

### 3.3 唯一强调色 & 唯一签名动效

- 强调色仍是陶土橙 `#D97706`，只出现在：主按钮、当前选中、运行中状态点。玻璃永不染橙（除了发送键的暖渐变）。
- 签名动效只有一个：**发送键按下时 Dock 的镜头轻微收敛 + 跟随指针的镜面高光**，其余一律 150–250ms 的 Material 过渡。系统「移除动画」/动画时长缩放为 0 时全部退化为瞬时。

### 3.4 明确不做

- 不做消息气泡 / 列表项 / 代码块玻璃（可读性 + 性能）。
- 不做「全屏毛玻璃背景」。
- 不引入新字体、不做渐变文字、不做卡片瀑布。

---

## 4. 技术架构

### 4.1 新增包：`ui/glass/`

```
ui/glass/
├── GlassTokens.kt      // 参数表（暗/亮两套）+ LocalGlassTokens
├── GlassWorld.kt       // GlassWorld{}：rememberLayerBackdrop + layerBackdrop + LocalGlassBackdrop
├── AmbientBackdrop.kt  // 暮色环境底（静态绘制，无 RenderEffect）
├── LiquidGlass.kt      // Modifier.liquidGlass(role, shape)：唯一入口，内部按 API 分级
└── GlassContainers.kt  // GlassTopBar / GlassDock / GlassPanel / GlassPill / GlassDialogSurface
```

### 4.2 关键 API（深模块，三个入口）

```kotlin
// 1) 提供世界（每个屏幕根部一次）
@Composable fun GlassWorld(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit)

// 2) 任何 chrome 声明自己是玻璃
fun Modifier.liquidGlass(
    role: GlassRole,                 // Chrome / Dock / Panel
    shape: Shape = RoundedCornerShape(20.dp),
    enabled: Boolean = LocalGlassEnabled.current
): Modifier

// 3) 现成容器（内部就是 2 + Material 语义/触控/无障碍）
@Composable fun GlassTopBar(title: @Composable () -> Unit, actions: @Composable RowScope.() -> Unit)
@Composable fun GlassDock(modifier: Modifier = Modifier, content: @Composable () -> Unit)
@Composable fun GlassPanel(shape: Shape, modifier: Modifier = Modifier, content: @Composable () -> Unit)
```

要点：

- 世界通过 `LocalGlassBackdrop`（`CompositionLocal`）下发，**不做 prop drilling**；`ChatInputBar` 这种深调用栈组件不用改签名。
- **玻璃必须画在录制层之外**。`GlassWorld` 的 `content` 只放「会被采样的东西」（环境底 + 列表/画布），顶栏/输入栏/抽屉作为 `Box` 的兄弟节点画在 `GlassWorld` 之后。这条决定 `ChatScreen` 要重排一次骨架（见 P1）。
- 同一个屏幕只允许 **1 个** 世界层（多次 `layerBackdrop` 会翻倍画）。

### 4.3 API 分级（`Modifier.liquidGlass` 内部）

| SDK | 效果 | 用户看到 |
| --- | --- | --- |
| ≥33 | blur + lens 折射 + AGSL 边缘高光 + 内阴影 | 满配液态玻璃 |
| 31–32 | blur + 平面高光（`HighlightStyle.Plain`）+ 内阴影 | 有模糊、无折射 |
| 24–30 | **拟玻璃**：`tint α≈0.86` + 1dp 双层描边 + 顶部内高光渐变 + 阴影 | 视觉上仍是玻璃卡，只是不透明 |

拟玻璃由我们自己画（`drawBehind` + 两个 `Brush`），不依赖库，保证不出现「什么都没画」的黑洞。

---

## 5. 分阶段落地

> 每一阶段结束都必须：`python3 scripts/i18n/verify.py` 通过 + 该阶段文件 review 完 + CI 出包可截图。

### P0 · 基础层（不动现有 UI）

| 文件 | 改动 |
| --- | --- |
| `gradle/libs.versions.toml` | 加 `backdrop = "1.0.6"` 与 `kyant-backdrop` 库坐标 |
| `app/build.gradle.kts` | 加 `implementation(libs.kyant.backdrop)` |
| `ui/glass/GlassTokens.kt` | 新建：暗/亮两套参数 |
| `ui/glass/AmbientBackdrop.kt` | 新建：暮色环境底 |
| `ui/glass/GlassWorld.kt` | 新建：世界层 + `LocalGlassBackdrop` |
| `ui/glass/LiquidGlass.kt` | 新建：`Modifier.liquidGlass` + 三级降级 |
| `ui/glass/GlassContainers.kt` | 新建：`GlassTopBar` / `GlassDock` / `GlassPanel` / `GlassPill` |
| `theme/Theme.kt` | 加 `LocalGlassTokens`、`LocalGlassEnabled`（默认 true） |

验收：编译过；现有界面截图与改造前一致（玻璃还没接线）。

### P1 · 聊天主屏（本计划的主体）

| 文件 | 改动 |
| --- | --- |
| `ui/chat/ChatScreen.kt` | 骨架重排：`Box { GlassWorld { AmbientBackdrop + Scaffold(content) }; GlassTopBar; ... }`；`TopAppBar` → `GlassTopBar`；`containerColor` 改透明 |
| `ui/components/ChatInputBar.kt` | 外层 `Surface` 透明；`:603` 输入卡 → `Modifier.liquidGlass(Dock, RoundedCornerShape(24.dp))`（保留全部现有内容） |
| `ui/drawer/ChatHistoryDrawer.kt` | `ModalDrawerSheet` 的 `drawerContainerColor` 透明 + 底板换成 `GlassPanel` |
| `ui/components/FloatingSwitcherWidget.kt` | 外壳 → `GlassPill`（若是自绘容器） |

验收：暗/亮两套 + 字体 1.3× + 抽屉开合 + 键盘弹出 + 滚动中截图各一张；滚动 10 秒无肉眼卡顿。

### P2 · 弹层体系

- 新增 `GlassDialogSurface`（伪玻璃）：先用于 `AlertDialog` 的高频三处（删除会话、模型选择、附件选择）。
- `ModalBottomSheet` 的 `containerColor` → 伪玻璃色（`ModelSelector` / `ThinkingSelector` / `AttachmentSelector` / `ClaudeModeSheets` / `FileManagerDialog`）。
- `SettingsDialog.kt`：只动顶层容器 + 顶栏，**内部 98 个 Surface 不动**。
- 若要求关键弹层「真玻璃」：把 `ModelSelector` / `AttachmentSelector` 从 `ModalBottomSheet` 改成同窗口面板（工作量 +1 子阶段，风险是返回键与手势）。

### P3 · 其他屏幕 chrome（可选）

- `IdeScreen.kt` 顶栏 + `ProjectSidebar` 面板；`BrowserScreen.kt` 顶栏；`LocalTerminalDialog.kt` 外框；`ClaudeSettingsScreen.kt`、`FullScreenLocalToolsInstaller.kt` 顶栏。

### P4 · 收尾

- 设置里加「视觉」开关：**省电流畅模式**（关闭所有 blur/lens，只保留 tint+描边）。需要 2 个 i18n key（中英）。
- `DESIGN.md`：把落地后的 token、角色、动效写成设计系统文档。
- 无障碍与性能过一遍：对比度（正文 4.5:1 / 大字 3:1）、48dp 触控、`contentDescription`、Reduce motion、边缘到边 insets。

---

## 6. 性能红线

| 项 | 预算 |
| --- | --- |
| 世界层录制 | 每屏 1 个，且只包「会被采样的内容」 |
| 同屏玻璃块 | ≤ 6（顶栏 1 + 输入栏 1 + 抽屉 1 + 浮钮 ≤2 + 胶囊 ≤1） |
| `lens` 折射 | 只给 ≤ 96dp 高的小控件；大面板只用 blur |
| 世界重录频率 | 跟列表滚动同步；滚动中若掉帧，输入栏降级为 `blur`（不是 lens） |
| 弹层 | 独立 Window，不做采样，零额外成本 |

---

## 7. i18n / 兼容

- 新增可见文案仅 P4 的开关（2 个 key，必须同时进 `values/` 与 `values-zh-rCN/`），其余沿用现有 `R.string.*`。
- 所有 `stringResource` 必须留在 Composable 作用域（`verify.py` 会查）。
- `targetSdk 28` 现状不动（与本次改造无关）。

---

## 8. 风险清单（按严重度）

| # | 风险 | 缓解 |
| --- | --- | --- |
| 1 | `ChatScreen.kt` 157KB，骨架重排容易碰坏 drawer/keyboard/常驻切换逻辑 | 先做 P0；P1 用最小 diff（只插玻璃，不搬 ViewModel 逻辑）；改完逐段 review |
| 2 | 每帧重录世界 → 掉帧 | §6 预算 + 省电开关 + 滚动降级 |
| 3 | 独立 Window 弹层采不到背景 | 默认伪玻璃；要真玻璃才改同窗口（P2 选项） |
| 4 | Kotlin/Compose 版本不匹配 | 锁 `1.0.6`；P0 第一件事就是 CI 编译验证 |
| 5 | 老机型（<31）无模糊，效果崩 | 拟玻璃降级，独立绘制路径，P0 一并做掉 |
| 6 | 玻璃压在文字上影响可读性 | 玻璃只包裹 chrome，正文永不进玻璃；`tint` 有下限（暗色 ≥0.06、亮色 ≥0.42） |

---

## 9. 验证

1. **编译**：推分支 `feat/liquid-glass` → `ci.yml` 出 `antiGem-termux-v<ver>-<debug|release>-<sha>.apk`（`docs/**` 与 `*.md` 改动不触发，源码改动才触发）。
2. **视觉**：装机后按阶段验收清单截图（暗/亮/字体 1.3/抽屉/键盘/滚动）。**需要你决定怎么装**（见问题 4）。
3. **回归**：`python3 scripts/i18n/verify.py`；关键路径手测（新会话、发送、中断、切换 IDE/Browser/Terminal、打开设置）。

---

## 10. 需要你决定的四件事

1. **范围**：P0–P2（聊天页 + 抽屉 + 弹层）先落地，P3 另外排？还是一次铺满所有屏幕？
2. **世界底**：暖色「工作台暮色」（推荐，接品牌）还是中性深灰（更保险、更 iOS）？
3. **老设备**：API<31 用拟玻璃降级（推荐）还是直接关掉液态玻璃？
4. **验证装机**：推 CI 出包你自己装？还是允许我临时构建 `standard`（`com.antigem`，不同包名，可与现装版本共存）debug 包装本机截图核对？（项目规则是只出 termux flavor 的包，所以这条需要你点头）

---

## 11. 已确认的决定（2026-10-24）

| 问题 | 决定 |
| --- | --- |
| 范围 | **P0 + P1 只做聊天主屏**；P2（弹层）/P3（IDE、Browser、Terminal、设置）/P4（省电开关、DESIGN.md）等主管确认后再排 |
| 世界底 | **中性深灰**（炭黑 + 三处低饱和径向光 + 底部压暗），见 `ui/glass/AmbientBackdrop.kt` |
| 老设备 | **拟玻璃降级**：API<31 或拿不到世界层时，用染色 + 双面描边 + 阴影，不采样 |
| 验证 | 推分支 → `ci.yml` 出 APK → 用户自行安装核对；本地不跑 Gradle |

## 12. P1 实际落地记录

结构（关键，改动集中在 `ChatScreen.kt` 一个文件内）：

```
Scaffold(containerColor = Transparent, topBar = { TopAppBar(Modifier.liquidGlass(Chrome)) })
└── Box(顶层，玻璃层与世界的共同父容器)
    ├── GlassWorld                     ← 录制层：环境底 + 聊天列表（会被玻璃采样）
    │   ├── AmbientBackdrop
    │   └── Column(fillMaxSize)  └── Box(weight 1f) { LazyColumn ... }
    └── Column(底部玻璃层)             ← 兄弟节点，不在录制层里
        ├── 审批面板 / 问题面板 / Claude 状态条
        └── ChatInputBar(内层卡片 = liquidGlass(Dock))
```

- 列表 `contentPadding` = 状态栏 + 64dp 顶栏高度 + 8dp（上）、输入栏实测高度 + 12dp（下）→ 内容能滚到顶栏与输入栏**后面**。
- 输入栏高度用 `onGloballyPositioned` 实测，不写死（附件预览/录音会改变高度）。
- `ModalNavigationDrawer` 的抽屉在 `GlassWorld` 外面，所以抽屉玻璃采到的是「被它挡住的聊天内容」。
- 已知取舍：审批/问题面板改成浮层后**不再压缩列表高度**（原来是挤在上面），列表内容可能被面板短暂遮住。
- 刻意没做：「省电流畅模式」开关（P4）、弹层真玻璃（独立 Window，P2）、输入栏的指针跟随高光（P2）。
