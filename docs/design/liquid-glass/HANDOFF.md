# 液态玻璃 UI 重构 · 交接文档

> 面向「下一个接手的人/agent」。读完这一篇就能继续干，不需要翻对话记录。
> 最后更新：分支 `feat/liquid-glass`，HEAD `c12975b`，CI run `40177252860`（success）。

## 0. 一页速览

| 项 | 值 |
| --- | --- |
| 分支 | `feat/liquid-glass`（`master` 未被改动，未合并） |
| 范围 | P0–P1 已完成：聊天主屏的顶栏 / 输入栏 / 抽屉 + 世界层 + 世界底图 |
| 构建 | **只能走云端 CI**：push 该分支 → `.github/workflows/ci.yml` → `:app:assembleTermuxRelease` → 下载 artifact |
| 产物 | `antiGem-termux-v<版本>-release-<短sha>.apk`（release 签名，包名 `com.termux`） |
| 未做 | P2 弹层伪玻璃、P3 其它四屏、P4 省电开关 / DESIGN.md / 性能与无障碍收口、参数定稿 |
| 临时物 | 长按顶栏的「玻璃预设切换条」（调试入口，定稿后要删，见 §4.3） |
| 待决策 | 最终观感选 A / B / S 哪一组（用户已倾向 A「水灵」与 S「上游原味」，未定稿） |

**继续干活的最短路径**：读 §2（作用域规则）→ §3（代码地图）→ §4（参数系统）→ §8（下一步）。

## 1. 现状（实测，不是估计）

- 提交：`master..HEAD` 共 16 个提交，`git diff --stat master..HEAD` = 18 文件 / +1448 / −38。
- 新增 Kotlin 包：`com.example.gemini.ui.glass`（5 个文件，见 §3）。
- 依赖：新增 `io.github.kyant0:backdrop:1.0.6`；显式声明 `io.github.kyant0:shapes:1.2.0`（backdrop 以 runtime 传递，runtime 不进编译类路径，必须显式）。**没有其它新依赖**（Coil 2.7.0 本来就在）。
- i18n：`python3 scripts/i18n/verify.py` 通过，`values: 2063 / values-zh-rCN: 2060 / 源码引用: 2030`。
- 用户实测结论（真机，release 包）：
  - ✅ 顶栏/输入栏背后的内容被模糊、四条圆角有折射、边缘有亮线、按下有高光跟随；
  - ✅ 抽屉玻璃里的聊天内容与身后页面的明暗一致；
  - ✅ 滚动、键盘、抽屉、四屏、性能（D 组）通过；
  - ⚠️ 观感仍待定稿：用户认为「参数观感还不够好」，并且明确要「更接近上游 demo」。
- 用户明确要求（未完成）：**自选相册图作为世界底** —— 已实现（§5.2），实机效果待用户确认。

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
| `LiquidGlass.kt` | `Modifier.liquidGlass()` L39、`frostedGlassSurface()` L70、`glassWithBackdrop()` L76、`frostedGlass()` L160 | 唯一渲染入口；`effects`（vibrancy+blur+lens）、`highlight`、`shadow`、`innerShadow`（α≤0 时不挂）、`onDrawSurface`（shade/tint/topSheen/innerGlow） |
| `GlassTokens.kt` | `GlassSpec` L17、`GlassPresets` L116、`names` L118、`DEFAULT` L121、`upstream()` L285 | 参数系统：字段语义 + 4 组预设 + 亮/暗两套 |
| `AmbientBackdrop.kt` | `AmbientBackdrop()` L65、图片分支 L71、`buildGrainBitmap()` L约95 | 世界底：极光渐变 + 64×64 可平铺颗粒 + 用户自选图片 + 可读性罩 |
| `GlassSheen.kt` | `Modifier.pressSheen()` L35 | 手指跟随的镜面高光（上游 `InteractiveHighlight` 的等价实现，1.0.6 没有 RuntimeShader API） |

改动（既有文件）：

| 文件 | 锚点 | 改了什么 |
| --- | --- | --- |
| `ui/chat/ChatScreen.kt` | L934 `rememberGlassBackdrop`、L948 `LocalGlassTokens`/`LocalGlassBackdrop` provides、L1107 长按、L1110 顶栏玻璃、L1294 `GlassWorld`、L1298 `AmbientBackdrop`、L1981 世界内 scrim、L2221 `SettingsDialog` | 组装整屏：录制层 / 玻璃面 / 让位尺寸 / 预设切换 |
| `ui/components/ChatInputBar.kt` | L609 玻璃 + `RoundedRectangle(30.dp)`、L610 `pressSheen()`（必须在玻璃**内层**） | 输入栏 |
| `ui/drawer/ChatHistoryDrawer.kt` | L571 `ModalDrawerSheet`、L575 玻璃 | 抽屉（`drawerContainerColor = Transparent`） |
| `ui/chat/SlashCommandOverlay.kt` | `listMaxHeight` 参数 | `/` 面板按「顶栏到输入栏剩余高度」限高 |
| `data/preferences/AuthPreferences.kt` | key `glass_world_image_uri`、`glassWorldImageUri` flow、`saveGlassWorldImageUri()` | 世界底图持久化（DataStore） |
| `ui/chat/ChatViewModel.kt` | `glassWorldImageUri: StateFlow`、`setGlassWorldImageUri()` | 透传 |
| `ui/settings/SettingsDialog.kt` | 参数 L122、`AppearanceSubScreen` L954、选图 launcher L960、UI 行 L1156 | 「世界底图」入口 |
| `theme/Theme.kt` | `LocalGlassTokens` / `LocalIsDarkTheme` | 主题层给默认 tokens（屏幕内可覆盖） |
| `gradle/libs.versions.toml`、`app/build.gradle.kts` | `kyantBackdrop` 1.0.6、`kyantShapes` 1.2.0 | 依赖 |

## 4. 参数系统

### 4.1 `GlassSpec` 字段语义（`GlassTokens.kt:17`）

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
| `chromaticAberration` | 折射边色散（API 33+） | 仅给"水灵"类预设开 |

### 4.2 现有 4 组预设

| 预设 | 取向 | 关键值（暗） |
| --- | --- | --- |
| A 水灵 | 最薄、带色散 | blur 5–10dp，tint α0.07，lens 14–16/36–42dp，色散开，topSheen 0.16–0.20，innerGlow 0.10–0.12 |
| B 厚玻璃（`DEFAULT`） | 有厚度 | blur 12–18dp，tint α0.14–0.18，lens 20–24/36–42dp，innerShadow 0.20–0.28 |
| C 磨砂 | 可读性优先 | blur 18–22dp，tint α0.24–0.40，lens 10–12/16–20dp |
| S 上游原味 | 逐项照抄上游 | blur 0，tint 透明，lens 8/20dp，highlight 0.5dp，shadow 16dp/α0.06，无内阴影 |

亮色三套不是暗色反相：染色偏冷（`0x4DE8EEF9` 一类）+ 更强投影 + 更弱内阴影。

### 4.3 临时调试入口（**定稿后必须删**）

- 长按顶栏 → 输入栏上方出现 chip 条 → 点选即时切换（`ChatScreen.kt:936-940`、L1104-1109、L2004-2030）；
- 删除清单：`glassPresetIndex` / `glassPresetPickerVisible` / `LocalGlassTokens provides glassPresetTokens` / 长按 `pointerInput` / chip 条 / `GlassPresets` 里未选中的预设 / `names`；
- 保留：选中那组直接变成 `DarkGlassTokens` / `LightGlassTokens`（`Theme.kt` 只依赖这两个名字，不用改）。

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
| 参考配方 | `ScrollContainerContent.kt` | `shape = RoundedRectangle(32.dp)`, `effects = { vibrancy(); lens(16.dp, 32.dp) }`，其余全用默认 |

其它实测结论：

- `updateEffects()` 在 `SDK >= 31` 时把 `renderEffect` 挂到采样图层，glide 生效没问题；`lens()` 需要 `SDK >= 33`；
- `lens()` 接受的形状：`RoundedRectangularShape`（kyant shapes，连续圆角）**或** `CornerBasedShape`（Compose 普通圆角）——两者都要在 `LiquidGlass.kt` 里判断，漏一个折射会被**静默跳过**；
- 1.0.6 **没有** `RuntimeShader` / `asComposeShader` / `isRuntimeShaderSupported`（那是 2.0.x API），不要照抄 demo 的 utils。

## 7. 已知问题 / 技术债（按优先级）

1. **观感未定稿**：4 组预设各有取舍，用户尚未拍板。默认仍是 B，但用户两次表达偏好 A「水灵」/ S「上游原味」。
2. **每帧重录世界层**：`LayerBackdropNode.draw()` = `drawContent()` + `recordLayer(...)`，滚动时每帧重录全屏内容。这是当前最大性能开销，也是 P4「省电模式」的主要理由。量化未做（`docs/project-doc.md` §12 里没有帧率数据）。
3. **`pressProgress` 未接**：上游按下时折射会放大，我们只有高光跟随。
4. **亮色主题物理限制**：白玻璃压白内容，靠冷调染色 + 投影勉强立住；如果用户仍嫌"平"，应当改**内容底色**（给消息区加淡色底）而不是继续加 tint。
5. **弹层仍是伪玻璃**（P2）。
6. **其它四屏未接入**（P3，`SettingsDialog` / IDE / Browser / Terminal 仍是既有样式）。
7. **`ui-preview/` 仍是未跟踪文件**（早期 HTML 原型，刻意不入库）。
8. **调试入口还在**（§4.3），发布前必须删。
9. **`GlassSheen` 用 `pointerInput` 劫持输入栏的按压事件**：目前不影响功能，但若后续输入栏要加长按菜单需复核事件消费顺序。

## 8. 下一步（建议顺序）

**P0（收尾必做，1 次构建）**
1. 定稿观感：让用户看 A / S，选一组 → 按 §4.3 固化 + 删除调试入口（同一次提交里做，避免残留）。
2. 补 `DESIGN.md`（哪些面用玻璃、亮暗两套规则、对比度下限、性能红线）；可用 `impeccable` skill 的产出格式。
3. 量一次帧率（滚动长会话，`adb shell dumpsys gfxinfo <pkg> framestats`），把数字写进 `docs/project-doc.md` §12 的「实测」栏。

**P2（弹层伪玻璃，独立可做）**
- 落点：`ModelSelector` / `ThinkingSelector` / `AttachmentSelector` / `ClaudeModeSheets` / `FileManagerDialog` / `SettingsDialog`（都是 Dialog 或 Popup，在独立 Window）。
- 做法：用 `Modifier.frostedGlassSurface(role = GlassRole.Panel, shape)`（`LiquidGlass.kt:70`），参数从 `LocalGlassTokens` 取，保持与主屏同一套观感；**不要**试图采样（采样不到）。
- 验收：弹层观感与主屏玻璃一致、不是"纯色卡片"。

**P3（其它四屏）**
- 每屏各自 `val backdrop = rememberGlassBackdrop()` + 外层 provider + `GlassWorld(backdrop)` 包内容，再把 topBar/dock 换成 `liquidGlass(...)`；顺序与规则见 §2。
- 先做 IDE（有 topBar + 底部工具条，收益最大），Browser / Terminal / 设置次之。

**P4（打磨）**
- 省电/流畅开关：`LocalGlassEnabled = false` 即退化为拟玻璃（`LiquidGlass.kt:44`），建议加设置项 + 2 个 i18n key；
- `pressProgress` 接进 `lens`（按下 1.3–1.5×，`Animatable` + `dampedDrag`）；
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

下载方式：`gh api repos/743553649/agy-and-claude/actions/runs/<run>/artifacts -q '.artifacts[].archive_download_url'`，或用运行页 Artifacts。
安装前确认签名（`apksigner verify --print-certs`）与包名（`pm list packages com.termux`）。
