# 汉化项目交接文档

> 本文档写给接手的人（或 AI）。读完它不需要询问上一轮对话的任何上下文。

## 1. 任务目标

把 `agy-and-claude`（AntiGem，Android 应用，Kotlin + Compose）的英文界面汉化成简体中文，**中英双语**（不是替换成纯中文），AI 回复语言跟随界面语言。

- 仓库：`/sdcard/汉化/agy-and-claude`
- 远端：`git@github.com:743553649/agy-and-claude.git`（SSH，已授权，`gh` 已登录同账号）
- 分支：`feat/zh-cn-i18n`（从 `origin/master` 切出，**未开 PR**）
- 上游是国外开发者项目，`origin/master` 仍在跟进

## 2. 用户明确要求的硬性约束（务必遵守）

1. **不走本地构建**：所有编译一律通过 GitHub Actions 云端完成。除非是"硬性必须"（如读源码、跑静态校验脚本），不讨论本机环境问题。
2. **全程 push 云端构建**：改完即 push 到 `feat/zh-cn-i18n`，用 CI 的编译结果作验证证据。
3. **不碰 `version.json`**：它的变更会触发 `release.yml` 自动发布 GitHub Release。
4. **不改构建配置**：`AndroidManifest.xml`、`settings.gradle.kts`、`version.json`、`release.yml` 一律不动。
   **唯一例外（2026-10-22 用户同意，见 P1 计划第 2 节）**：P1 方案 A 引入了 `androidx.appcompat`，
   因此已改 `app/build.gradle.kts`（加依赖）、`gradle/libs.versions.toml`（版本目录）、
   `res/values/themes.xml`（两个主题父级换成 AppCompat 后代）。**manifest 仍未改**，
   所以 appcompat 的 `autoStoreLocales` 用不了，语言偏好是自己用 SharedPreferences 存的。
5. **保留双语**：用 `values/` + `values-zh-rCN/`，不硬替换成中文。
6. **AI 回复语言跟随界面**：中文界面注入中文指令，英文界面不注入。

### 2.1 仓库 `AGENTS.md` 与本项目做法的冲突（已由用户拍板）

仓库自带 `AGENTS.md` 写着「不要 git push / commit」「只编 termuxBuild，绝不编 Standard」，与用户指示冲突时**以用户指示为准**。当前 CI 已按用户选择的方案改为 **termux flavor（`com.termux`）**，与上游发版包名一致；注意**包名一致 ≠ 可覆盖安装**，覆盖还要求签名一致，见第 10 节。

## 3. 已完成的工作

分支相对 `origin/master` 共 **28** 个提交。下表只列早期批次，后续批次（P3 / P3.5 / P3.6 / P3.7）见第 4 节：

| 提交 | 内容 |
| --- | --- |
| `4d559f7` | CI workflow、术语表 `docs/i18n/GLOSSARY.md`、`values-zh-rCN/strings.xml` |
| `7d74b41` | 51 条通用文案 + `scripts/i18n/` 工具链 |
| `cd7fd69` | component 模块 209 条 |
| `c013c6a` | settings 模块 378 条 |
| `5642c10` | ide / claude / drawer / browser / chat / agents / ui 共 307 条 |
| `fb256bb` | 修 `newPlainText(` 误匹配导致非 Composable 调用 `stringResource` |
| `5d66e3e` | 正则 → 逐字符扫描，修嵌套引号截断 |
| `5a9234c` | 170 条含插值文案 + 149 处带参 `stringResource` |
| `05b375f` | 修可空参数、英文侧占位符、`%` 转义 |
| `337a8d7` | 属性形式静态文案 690 处（`text = / contentDescription =`） |
| `e2514fc` | 还原 `title = / label =` 67 处（data class 参数，不能放 `stringResource`） |
| `37fffea` | `docs/i18n/COVERAGE.md` 覆盖清单 |

### 当前覆盖率（P1 落地后，实测）

- 资源条目：`values/` **1546** 条，`values-zh-rCN/` **1542** 条（差的 4 条是 `app_name`
  / `anti_term_name` / `termux_name` / `accessibility_service_name`，属不翻白名单）
- 源码引用：**1513 个 key**（`verify.py` 口径；P2 收尾批新增 62 条静态 + 12 条插值资源）
- 源码调用点：`stringResource` **1496 处 / 72 个文件**，`getString(R.string.…)` **228 处**
- `python3 scripts/i18n/verify.py` 通过
- `docs/i18n/COVERAGE.md` 由 `gen_coverage.py` 重新生成（旧文档的按界面数字是 P3.5/P3.6
  之前的，别再引用旧值）

> ⚠️ 旧的「705 条已译 676 条（95.9%）」指标**已失效，别再引用**：绝大多数英文字面量
> 已被改写成 `stringResource`，扫描器现在只能扫到 140 条源码字面量，这个百分比没有意义。
> 现在的权威口径是上面两条（资源 key 差集 + 剩余源码字面量）。

## 4. 未完成的工作（按建议优先级）

### P0：AI 中文回复注入（✅ 已落地，commit `8fdd016`，CI run 39716135832 通过）
`app/src/main/java/com/example/gemini/domain/i18n/AiLanguage.kt` 的接入已完成。落地内容：

1. `data/remote/AntigravityApiService.kt`：`buildRequestPayload` 里把
   `"$SYSTEM_INSTRUCTION$customPart"` 改成 `"$sysBase$customPart"`，其中
   `val sysBase = SYSTEM_INSTRUCTION + AiLanguage.suffix()`。放在 `customPart` 之前，
   这样用户自定义提示词覆盖不掉语言指令。
2. `data/agent/claude/ClaudeProtocol.kt`：给 `ClaudeControlRequestBody` 加
   `val appendSystemPrompt: String? = null`（`ClaudeJson` 已设 `explicitNulls = false`，null 不会序列化）。
3. `data/agent/claude/ClaudeChatBackend.kt`：
   - 加字段 `private var languageInstructionSentFor: String? = null`
   - 在 `sendPrompt()` 里 `ensureConnected(conv.id)` 之后、`send(frame)` 之前调用新方法
   - 新方法用 `sendControl(ClaudeControlRequestBody(subtype = "initialize", appendSystemPrompt = AiLanguage.appendSystemPromptOrNull()))`（**fire-and-forget**，不要用 `request()` 等待）
   - `detachLocked()` 里把它置空

**落地时与原计划的两点差异**：
- 原方案只写了一句「把 `"$SYSTEM_INSTRUCTION$customPart"` 改成 `"$sysBase$customPart"`」，
  实际该表达式有 **2 处**（`toolInstruction` 非空/为空两条分支），两处都改才不漏注入。
- 新方法命名 `sendLanguageInstruction`，**不加 `Locked` 后缀**：它不持有 `_Locked` 系列
  的锁，`sendPrompt` 的协程体也不在 synchronized 块里，沿用邻居的 `@Volatile` 更贴合现有风格。
  英文界面下**不标记已发送**（`?: return` 在赋值之前），这样会话中途切到中文仍能补发。

**验证边界**：CI 只能证明编译通过。协议层已证实支持——
`CLAUDE_CLI_PROTOCOL.md:234` 明确列出 `initialize` 的可选参数含 `appendSystemPrompt` 且标记 ✅；
剩下的是这个 CLI 版本是否真的应用它，必须真机实测。若不被接受，最坏情况是"Claude 仍回英文"，
不会挂会话——依据：`ClaudeChatBackend.deliverReply()` 是 `replies.remove(id)?.complete(...)`，
未匹配的响应被忽略。

**另一个待真机确认的点**：`connect()` 的 while 循环在 socket 关闭后会自动重连，
但**不重置 `attachedId`、不调用 `detachLocked()`**，所以 `languageInstructionSentFor`
在重连后仍然保留。在「bridge 保持同一 CLI 进程、按 seq 续传」的前提下这是对的（同一进程不必重发）；
只有 bridge 在重连时换了新 CLI 进程才会漏发，届时表现为 AI 又回英文，需要重开会话。

### P1：设置页语言切换（✅ 已落地：A-1 `ef5a244` / A-2 `1519dc6`）

**用户 2026-10-22 拍板：走方案 A（加 `androidx.appcompat`）。**
实施计划见 `docs/i18n/p1-language-switch-plan.md`，三方案对比见 `docs/i18n/language-switch-options.html`。

落地内容：

| 批次 | 提交 | 内容 |
| --- | --- | --- |
| A-1 | `ef5a244` | `libs.versions.toml` 加 `androidxAppcompat = "1.7.0"`、`app/build.gradle.kts` 加 `libs.androidx.appcompat`、`themes.xml` 两个父主题换 `Theme.AppCompat.Light.NoActionBar`、4 个 Compose Activity 改 `AppCompatActivity` |
| A-2 | `1519dc6` | 新增 `AppLanguagePreferences`（system / zh-CN / en，SharedPreferences）、`AppLocaleController`（幂等回设 + 先落盘再应用）、设置 → 外观「界面语言」三选一卡片、6 条双语资源、`AiLanguage` 读 appcompat 的应用语言 |

**appcompat 1.7.0 在 AGP 9.0.1 + Gradle 9.1 + Kotlin 2.3.20 下编译通过**（A-1 的 CI 已证明），
不需要按计划的风险项退到 1.6.1。

**两处与计划不同的实现（都已核实原因，别再按计划原文改回去）**：

1. **`AiLanguage` 没有改成 `context.resources.configuration.locales[0]`**。
   两个调用点都拿不到语言正确的 Context：`AntigravityApiService` 根本没有 Context；
   `ClaudeChatBackend` 只有 `Application`，而 API<33 时 appcompat 只包装 **Activity** 的资源，
   Application 的资源仍是系统语言——按计划原文写，会在「系统英文 + 应用内选中文」时漏注入。
   现在改为读 `AppCompatDelegate.getApplicationLocales()`：API 33+ 来自框架 `LocaleManager`，
   API<33 来自 `setApplicationLocales` 的进程内请求值（本机 javap appcompat 1.7.0 核实：
   `getApplicationLocales()` 的低版本分支就是 `return sRequestedAppLocales`），
   两者都为空（跟随系统）时才回落 `Locale.getDefault()`。因此调用点无需改签名。
2. **`AppLocaleController.apply()` 放在 4 个入口 Activity，不是只放 `MainActivity`**。
   `TermuxActivity` 有自己的 launcher alias（`TermuxAntiTermAlias` / `TermuxClassicAlias`），
   `ManageSpaceActivity` 可由系统「应用信息 → 存储」冷启动，`FloatingChatActivity` 可能由通知
   拉起进程；只放 MainActivity 会让这些入口回到系统语言。调用有 `appliedTag` 幂等保护，
   重复调用直接返回，不会形成 recreate 循环。

**CI 证据**：

- A-1（run 39748654578，commit `ef5a244`）：`Verify i18n resources` 通过
  （1453 / 1449 / 1383，与基线一致，本批未动文案）、`:app:assembleTermuxRelease` 成功
  （7m9s）、产物 `antiGem-termux-v1.0.13-release-ef5a244.apk`（19M）、
  `Verify APK signature` 指纹 `6F:F1:E3:FF:…:7B:5C` 与标准值一致。
- A-2（run 39750147539，commit `1519dc6`）：`Verify i18n resources` 通过
  （`values: 1459 / values-zh-rCN: 1455 / 引用 1389 key`）、`:app:assembleTermuxRelease`
  成功（9m12s）、产物 `antiGem-termux-v1.0.13-release-1519dc6.apk`（19M）、
  `Verify APK signature` 指纹与标准值一致。

**验证边界（必须真机，别写成结论）**：

- CI 只证明编译通过；主题从框架 `android:Theme.Material` 换成 AppCompat 后，
  全应用样式是否有回归**只能真机逐屏对照**（好消息：本仓库**没有任何 XML layout**，
  界面基本全是 Compose，AndroidView 里只有 WebView / Sora 编辑器 / TerminalView，
  都是代码构造、不走 AppCompat 的 inflater，理论上受影响面很小）。
- **待验证①**：API<33 下通知文案（`LocalEnvironmentManager` 走 `applicationContext`）是否跟随
  界面语言。appcompat 在低版本只包装 Activity，通知很可能仍是系统语言。
- **待验证②**：API<33 下静态 `Locale.getDefault()` 是否随 appcompat 包装变化。
  `AiLanguage` 已改为读 appcompat 的应用语言来规避，但这条事实本身仍未在真机确认。
- 真机矩阵见计划第 7 节（API 33/34 走框架 `LocaleManager`；28/24 走 appcompat 资源包装）。
- 已知行为：API 33+ 上若用户从**系统设置**里单独给本应用选语言，应用内设置页会把它显示为
  已选中（读的是框架值）；此时点「跟随系统」会清掉该选择。

### P2：`title = / label =`（✅ 已处理 50 处，剩余全是白名单 / 待改造）

本批（`apply_p2_titles.py`）回写了 **50 处**：对话遥测（4 标题 + 6 行标签）、文件详情（6）、
附件选择（2）、语音模式（2）、图片预览、问卷「其他」、输入栏附件预览、权限卡（3）、
Git 面板 4 个分区标题、模型选择标题、浏览器新标签页（1 处 Composable + 2 处 WebView 回调）、
设置首页 3 张卡、`ScriptExecutionInfo` 的 5 个脚本标题（Rootfs / 对话备份 / 对话恢复 /
项目备份 / 项目还原）、数据层 5 处对话标题（`application.getString`）、
插件文档标题与 fork 标题（2 处插值，走 `i18n_dynamic.json`）。

关键做法（照坑 7 的判据）：
- Composable 参数 / Composable 作用域 → 直接 `stringResource(...)`；
- `ScriptExecutionInfo(title = ...)` 这类 data class 构造在 onClick 里 → 用作用域内的
  `context.getString(...)`（这些 composable 都已经有 `val context = LocalContext.current`）；
- 后端（`AgyChatBackend` / `ClaudeChatBackend`）有 `application` → `application.getString(...)`；
- `BrowserSessionManager` 的 WebView 回调只有 `appContext: Context?` → `appContext?.getString(...) ?: 英文兜底`。

**剩余 58 处未翻，全部有明确理由**（别再当成漏翻）：动画/诊断 label 30、终端按键 22、产品名 4、待模型改造 2。
- 动画 / 诊断 label（`rememberInfiniteTransition(label=…)`、`animateFloat(..., label=…)`、
  `"progress"`、`"extract_progress"`、`"question_page"`、`"drawerChatListFade"` 等）；
- 终端按键 `TermuxKey/IdeTermKey(label = "ESC"/"CTRL"/"ALT"/TAB/HOME/方向键)`（终端与日志不翻）；
- 产品名：速度拨号 `"Google"`/`"YouTube"`、模型分组 `"Google Gemini"`/`"Anthropic Claude"`；
- **待模型改造的 2 处**：`ClaudePermissionPanel` 的 `ApprovalItem(title = "Review the plan")`
  （模型里还有 `"Approve · auto-accept edits"`、`toolTitle()` 的 `"Run a command?"` 等一整族，
  要改成 `@StringRes`）、`ConversationShareHelper` 的 `"Shared Conversation"`（解析函数没有 Context）。

**同族盲区（新发现，见坑 19）**：`subtitle =`（6）、`badgeText =`（4）、`description =`（5）、
`value =`（11）共 **26 处**仍是英文，例如设置卡副标题、权限卡 `"Required"/"Optional"` 与说明、
遥测 `"${n} turns"/"Active"/"None"/"Configured"/"Default"`。建议单独一批处理。

### P2 收尾批：坑 19 同族 + 零散残留 + 审批面板（✅ 已落地）

三块一起做，工具是 `apply_p2_final.py`（字面量）+ 一次性结构化脚本（审批面板）：

1. **坑 19 同族 21 处**：设置卡 `subtitle` / `Shield` 徽标、权限卡 `Required`/`Optional` 与三条说明、
   附件选择与语音模式副标题、两处通知渠道 `description`；另修数据层可见状态 4 处
   （Claude 启动 3 条、Agy 无消息错误）。`value =` 里的 `primary`/`pro`/`rising-fact-p41fc`/`"."`
   是技术值，保留英文。
2. **审批面板整族**：`ApprovalItem.denyLabel/denyPlaceholder` 改 `@StringRes Int`（渲染器改
   `stringResource`）；`agyApprovalItems` / `claudeApprovalItems` 增加 `Context` 参数——它们是在
   `ChatScreen` 的 `remember { }` 里构建的，**不能**用 `stringResource`（坑 1 变体）；
   标题（Run a command? 等 10 条）、选择项（Allow / 三个作用范围）、拒绝占位符、
   Claude 建议范围（`Always allow $rules $where` 等 5 条插值）全部取资源；
   `permissionModeLabel/Description` 改成带 `Context` 版本（12 条，Claude 控件/模式表/设置页三处调用）。
3. **零散残留**：Claude 设置页 4 处插值 `Text`、WebView 扩展摘要、思考预算 `e.g. 2048` 占位符、
   导出/存图 snackbar 的 `message` 与 `Open` 动作（`common_open`）、`Shared Conversation`
   （`parseSharedConversation` 增加 `Context` 参数，`ChatViewModel` 传 `getApplication()`）。

**仍未翻，留给下一批**：
- `AgyMcpManager` 与 `ClaudeAccountManager` 的错误/状态文案——**已完成**，见下方「数据层错误族」小节；
- `ClaudeStatusViews` 的 7 个 `actionLabel`（Sign in / Open sign-in / Set up Claude Code /
  Restart server / Retry / Check again）；
- `when`/`if` 分支字面量（约 391 行 `-> "…"`、268 行单行三元，绝大多数是技术值）；
- 浏览器扩展文案（P4）——**已完成**，但实际范围与原先设想不同，见 4 节 P4 与坑 20。

**CI 证据**（run 39764615030，commit `99e0aac`）：`Verify i18n resources` 通过
（1546 / 1542 / 1513）、`:app:assembleTermuxRelease` 成功（6m53s）、产物
`antiGem-termux-v1.0.13-release-99e0aac.apk`、签名指纹一致。首次推送 `ada6ac8` 的
run 39764130892 **失败**：`ClaudeSettingsScreen.DefaultsPage` 没有 `context` 变量，
`permissionModeDescription(context, value)` 里的 `context` 被解析成 Kotlin 上下文参数关键字
（`Function invocation 'context(...)' expected`），已补 `val context = LocalContext.current`。

**CI 证据**（run 39761475073，commit `dbb7c9c`）：`Verify i18n resources` 通过
（`values: 1472 / values-zh-rCN: 1468 / 引用 1437 key`）、`:app:assembleTermuxRelease` 成功、
产物 `antiGem-termux-v1.0.13-release-dbb7c9c.apk`（19M）、签名指纹与标准值一致。
⚠️ 首次推送 `015e540` 的 run 39760852496 **失败**：`BrowserScreen.kt:153` 的 `title =`
写在 `LaunchedEffect` 里，改成 `stringResource` 触发 `@Composable invocations` 报错
（坑 1 变体，已在该坑下补记）。`verify.py` 的按文件检查抓不到这种嵌套误用，只能靠 CI。

### P3：Toast 与包装的提示（✅ 主体已完成，commit `6310f1c`，CI run 39726355921 通过）
`Toast.makeText(context, "英文", ...)` 必须用 `context.getString(R.string.x)`（非 Composable）。
实测 **104 处**（文档旧写的「约 7 处」严重低估），已全部改写并新增 88 条 `*_toast_*` 资源；
含插值的按出现顺序转成 `%n$s` 参数，嵌套引号与多行字面量都能正确处理。

### P3.6：设置首页 + 环境安装页（✅ 已完成，commit `5d55bc3`，CI run 39730503843 通过）
这两页曾经整页英文，但**不是设计**，是流水线的两处盲区：

- **`title = "..."` 的 Composable 参数**：当初因为「title 多为 data class 参数」把整类
  排除了（坑 7），结果设置首页 14 张卡片的标题/副标题也一并留成英文；
- **表达式分支里的字面量**：`when (state) { Downloading -> "..." }`、
  `if (isTermux) "A" else "B"`、`when (hasAtomics) { true -> "Supported" }` ——
  `apply.py` 只匹配 `Text("字面量")`，这些从没进过扫描，连未译清单都不出现（坑 11）。

已改 135 处、新增 107 条 `settings_ui_*` 资源；工具是 `apply_settings_ui.py` +
`settings_ui_zh.tsv`（116 条对照），按「文件 + 行区间 + 表内原文」三重限定，表外一律不碰。
**仍有英文但属于白名单**：`EAGER`/`AUTO`/`DARK`/`LIGHT`（比较用的取值）、
`48-bit (VA48)`/`39-bit (VA39)`/`N/A`/`*/*`/`progress`/`extract_progress`、
以及日志染色键 `Fail`/`Error`/`Success`/`ready`/`Extracted`（被 `logLine.contains()` 用于选色）。

**第三处盲区：枚举构造参数**（commit `4d25d0f` 已修）
设置首页顶栏的 "Settings & Preferences" 来自
`enum class SettingsSection(val title: String, val subtitle: String)` —— 枚举参数既不是
`Text("...")` 也不是 `title = "..."`。已把 `SettingsSection`（15 条）与
`ClaudeSettingsPage`（7 条）改成 `@StringRes val titleRes/subtitleRes: Int`，
在 Composable 里 `stringResource(...)` 取；顶栏 2 处 + Claude 导航卡 14 处调用同步改写。
注意：`ifBlank { stringResource(...) }` 这种「在 lambda 里调 Composable」的写法不要赌，
已改为在 Column 之前先取值。

**同类盲区还剩**：全仓库 `when`/`else ->` 分支返回字面量 **70 行**、单行
`if (…) "A" else "B"` **243 行**，需逐条判定白名单后再改（很多是 `"bash"`、`"error"` 这类技术值）。

### P3.7：数据层产出的通知与进度文案（✅ 已完成，commit `6203d58`，CI run 39739137128 通过）
`data/local/LocalEnvironmentManager.kt` 不在 Composable 里，但它生成的字符串**会显示给用户**。
本轮共改 **77 处**，共 **55 条**文案资源：新增 **54 条** `data_install_*`，
`Termux Linux environment verified and installed successfully!` 复用既有 key
`data_termux_linux_environment_verified_and_in`。

改的是四类站点：

1. `updateInstallNotification(appContext, "标题", "正文", …)` **34 处调用、46 处文案**：通知标题/正文
   （旧文档记的「35 处」把函数定义也算进去了；另有 1 处传空串取消通知，无需翻译）。
   标题里的 `$titlePrefix` / `$pct%` / `${index + 1}` 按出现顺序转 `%1$s` / `%2$s`，
   孤立 `%` 写成 `%%`（`gen_resources.py` 的 `normalize_percent`）。
2. 安装页状态字段：`speedText` **5 处**（`Connecting...` 就是用户截图里那条）、
   `currentFileName` 2 处、`stepDescription`、`testName`、`message`、`currentPackageName`
   （`"Local Archive"`）。这些字段被 `FullScreenLocalToolsInstaller` /
   `LocalToolsInstallDialog` 直接 `Text(state.xxx)` 渲染，翻不翻直接影响安装页。
3. 通知渠道名 `"Rootfs Installation"` 与 `description`：系统通知设置里可见。注意接收者是
   `setupInstallNotificationChannel` 的形参 **`context`**，不是 `appContext`。
4. `errorMsg` **15 处** / `errorMessage` **2 处**：安装页错误态 + 失败通知正文。异常尾串
   （`${e.localizedMessage ?: e.message}`）按术语表第 5 条保留英文，只翻外层句式；
   该表达式统一补 `.orEmpty()`（坑 8）。

**故意保留英文**：`bootstrap-$arch.zip`、`"$speedFormatted • $targetAsset"`（无词的模板/文件名）、
全部 `Log.d/w/e`、shell 脚本与配置文件内容、
`throw IOException("Could not open input stream from chosen file")`（异常 message）。

**看过但没动的**：`LocalInstallerState.Discovering` 的默认 message
（`"Searching GitHub for compatible Termux bootstrap release..."`）全仓库没有使用点；
`data_execution_failure`（`"Execution Failure"`）是 `AgyChatBackend.kt:1577` 的 `title =`，属 P2 批次。

工具：`apply_local_env.py` + `local_env_zh.tsv`（55 条），按「语法框架 + 对照表」双重限定，
表外一律不动；表里每条都必须能找到站点，否则脚本直接退出（防止写出没人用的资源）。

**CI 证据**（run 39739137128，commit `6203d58`）：`Verify i18n resources` 通过
（`values: 1421 / values-zh-rCN: 1417 / 引用 1351 key`）、`Build termux APK` 成功、
产物 `antiGem-termux-v1.0.13-release-6203d58.apk`、`Verify APK signature` 指纹与标准值一致。
**验证边界**：CI 只证明编译通过；通知进度文案、渠道名、安装页状态字段的实际显示效果
（尤其中文标题在通知栏的截断、`%2$s%%` 的百分比渲染）必须真机看。

### P3.8：分享/导出 Toast 与系统选择器标题（✅ 已完成，commit `0bbe075`，CI run 39742625188 通过）
`AppToastHelper.showToast("...")` 是项目自己的 Toast 包装，不叫 `Toast.makeText`，所以
`apply_toast.py` 扫不到。实测 **28 处**字面量站点，连同
`Intent.createChooser(intent, "标题")` 的 **8 处**标题（系统选择器标题也会显示给用户）一起改，
共 **36 处 / 32 条**资源（`*_apptoast_*` + `*_chooser_*`）。

- 接收者按文件显式登记（`apply_showtoast.py` 的 `RECEIVERS`）：Compose 界面用已有的
  `val context = LocalContext.current`、`ChatViewModel` 用 `getApplication<Application>()`、
  组件 object 用方法签名里的 `context: Context`；**未登记的文件直接报错**，不猜。
- 这些调用点都在 onClick / LaunchedEffect / object 方法里，**不是 @Composable 上下文**，
  所以不能用 `stringResource`（坑 1 的同类）。
- 重要耦合：`AppToastHelper` 在调用方不传 `ChatToastType` 时，靠**英文关键词**推断类型
  （`success`/`fail`/`error`/`copied`…）。本轮 28 处**全都已显式传类型**，所以翻译安全；
  以后翻这类文案必须显式传类型，否则样式会掉成 INFO（见坑 16）。

**CI 证据**（run 39742625188，commit `0bbe075`）：`Verify i18n resources` 通过
（`values: 1453 / values-zh-rCN: 1449 / 引用 1383 key`）、`Build termux APK` 成功、
产物 `antiGem-termux-v1.0.13-release-0bbe075.apk`、`Verify APK signature` 指纹与标准值一致。
⚠️ 首次推送 `df798c3` 的 run **39742025990 失败**：`ChatViewModel` 缺
`import com.example.gemini.R`（漏跑 `fix_imports.py`），已在 `0bbe075` 修复，并给 `verify.py`
加了 import 检查（坑 17）。
**验证边界**：Toast 与系统选择器标题的实际显示、`ChatToastType` 的成功/失败配色，必须真机看。


### P3.5：含插值但调用点未被回写的文案（✅ 已完成，同一提交）
16 处译文本已在 `/tmp/i18n_dynamic.json` 里，只是回写没覆盖到：`contentDescription`
（Composable 内）、`message`（snackbar，非 Composable）和多行字面量。
新增 `scripts/i18n/apply_dyn_attrs.py` 处理，`title` / `label` 两类仍交给 P2。

### P3.9：外观子页残留英文标签（✅ 已做，13 个标签）

`SettingsDialog.kt` 的 `AppearanceSubScreen` 里，卡片的**显示标签**原先仍是英文
（不是 `Text("...")` 也不是 `title =`，而是 `listOf(Triple("SYSTEM", "Auto (System)", Icons…))`
元组的**第二项**，随后 `Text(label)` 渲染）。已随 P2 批次一起改完：

- 主题卡 `Auto (System)` / `Light` / `Dark` → 复用 `settings_ui_auto_system` + 新增
  `settings_theme_light`（浅色）/ `settings_theme_dark`（深色）
- 字号卡 `Small` / `Normal` / `Medium` / `Large` / `Huge` → 新增 `settings_font_size_*`（小/标准/中/大/特大），
  并把原来一行 5 个 `to "字面量"` 拆成多行 `stringResource`
- 自动收起 `Never` → `settings_auto_collapse_never`（从不）
- 悬浮切换器条目 `Chat` / `IDE (Code Editor)` / `Web Browser` → `settings_switcher_item_*`；
  `Terminal` 复用 `common_terminal`
- **方向 `VERTICAL` / `HORIZONTAL` 本来就已经用 `settings_vertical` / `settings_horizontal`**，
  剩下的 `"VERTICAL"` 字样都是比较值与写偏好用的技术值，不能翻

判据：元组第一项是写入偏好的技术值（**不能翻**），第二项是显示标签（**要翻**）。

### P4：浏览器扩展文案（✅ 已落地，范围与最初设想不同）

**先纠正一个错误前提**（原文档写「文案在 `page-helpers.js` 注入的浮层里」，不成立）：

| 核实项 | 证据 |
| --- | --- |
| JS 文件数 | 6 个 `.js` 共 2618 行（constants 27 / flow-client 269 / flow-workflows 232 / page-helpers 1301 / service-worker 96 / storyboard-analyzer 693）+ `manifest.json` |
| `page-helpers.js` 实际用途 | `flow-client.js:17` `chrome.scripting.executeScript({target:{tabId}, world:'MAIN', files:['page-helpers.js']})` 注入的**页面自动化库**，只被 `s.xxx()` 调用并返回数据对象，**不创建任何 UI** |
| 是否写 DOM 文本 | 全文仅 1 处 `createElement`（`page-helpers.js:190` 的 `<meta name="viewport">`，用于冻结缩放）；无 `textContent=` / `innerHTML=` / UI 容器 |
| popup / 通知 | `manifest.json` 无 `action` / `options_page` / `notifications`；6 个 JS 里无 `alert` / `chrome.notifications` / `setBadgeText` |

**因此扩展 JS 的界面可见文案是 0 条。** 用 `scripts/i18n/scan_ext_js.py` 逐字符扫描出
319 条英文句子候选，逐条判定后全是白名单：

| 判定 | 条数 | 说明 |
| --- | --- | --- |
| 模型提示词（风格/镜头/主体预设） | 171 | `storyboard-analyzer.js`，会进 API payload，坑 10 禁止翻 |
| 错误/校验 | 111 | `throw new Error(...)`、守卫消息；GLOSSARY §2.5 留英文便于搜 issue |
| 模型返回值（`job.stage`） | 14 | `waiting in queue` / `generating image`，只回给模型 |
| 模型名 | 12 | `Nano Banana 2.1` / `Pro` / `2 Lite` |
| **选择器**（匹配 Flow 页面英文 UI） | 11 | `Agent instructions` / `Settings trigger` / `Add to prompt` / `Add clip` —— 靠页面英文文字定位元素，**翻了自动化直接失效** |

**真正落地的改动**（`apply_p4_extension.py`，一批，**一行 JS 未动**）：

1. 扩展设置页（`WebViewExtensionsScreen.kt`）6 条：安装/更新对话框标题与说明段落、
   确认按钮（复用 `common_install` / `common_update`）、`runOperation` 失败兜底
   （`context.getString`，非 Composable）；
2. `BrowserExtensionManager.kt` 25 条可显示错误（`require` / `error` / `check` 消息，
   经设置页 `Text(error)` 与 `Text(lastError)` 显示）：包校验、安装/更新/重命名流程、
   宿主启动失败；统一 `appContext?.getString(…) ?: 英文兜底`；
   `Missing or invalid background file: $relative` 走插值资源（`%1$s`）；
3. **内置扩展的展示名/描述**：`manifest.json` 只有英文，且 `BrowserExtension.name/description`
   会经 `toJsonObject()` 发给扩展宿主（改了会影响宿主侧标识），所以**不动数据字段**，
   只在设置页渲染处按 `id == "google-flow"` 取资源
   （`settings_extension_google_flow_name/description`，两个 `@Composable` 辅助函数）。
   第三方扩展仍显示 manifest 原文。

**边界（别再当成漏翻）**：`manifest.json` 的 65 条 `mcp_tools[].description` 只给模型读、
不进界面；`WebViewExtensionDeveloper.kt` 的消息是开发者 MCP 的工具返回值（给模型看），
均属白名单，保持英文。

**CI 证据**（run 39791704630，commit `af15088`）：`Verify i18n resources` 通过
（`values: 1577 / values-zh-rCN: 1573 / 引用 1544 key`，基线 1546/1542/1513，+31 三条对齐）、
`:app:assembleTermuxRelease` 成功（`BUILD SUCCESSFUL in 6m 59s`）、
`Verify APK signature` 指纹 `6F:F1:E3:FF:…:7B:5C` 与标准值一致（`OK: 签名与标准指纹一致，可以覆盖安装`）、
产物 `antiGem-termux-af15088f057dfa5edac7b9a1e4360b591c2c65ec.apk`（15.8MB）。

**真机验证安排**：本轮开发机（Xiaomi 22041211AC / Android 14 / SDK 34）**只用于写代码**，
用户明确要求不要在这台上安装（该机 `com.termux` 被 Play 版 Termux Ultra 118.3.50 占用，
卸载会连带删掉它的数据）。因此 P4 的真机验证与 P1 的待验证①②一并移交用户另一台设备执行，
**下面第 4 节 P1 的两条「待验证」保持待验证，未在真机确认前不得改写为结论**。

### 数据层错误族：`AgyMcpManager` + `ClaudeAccountManager`（✅ 已落地）

两个类原本都没有 `Context`，本批按用户要求**整批一起改**（`apply_data_errors.py`）：

1. 构造注入：`AgyMcpManager(backendScope, agyHubClient, appContext: Context)`、
   `ClaudeAccountManager(scope, client, appContext: Context)`；两个构造点都在 `ChatViewModel`
   （继承 `AndroidViewModel`），直接传 `application`，与同文件 `AuthPreferences(application)` 一致。
   属性名用 `appContext` 而不是 `context`，避开 Kotlin 2.x 把 `context` 当上下文参数关键字的老问题。
2. 改写 **36 处**可见文案（12 条静态 + 20 条插值）：
   - `AgyMcpManager`：连不上 Hub / 守护进程 / 刷新失败、服务器错误详情、刷新结果、
     启用停用、保存删除、插件安装成功与失败；`_mcpErrorMessage/_mcpStatusMessage`
     由设置页 MCP 区块 `Text(...)` 直接显示；
   - `ClaudeAccountManager`：状态检查失败、桥接不可达、未返回用量、登录/登出/验证码全流程；
     `_statusError/_usageError` 由 Claude 面板显示，`_messages` 走
     `AppToastHelper.showToast(it, ChatToastType.INFO)`——**调用点已显式传类型**，
     不受坑 16 的关键词推断影响。
3. 一律 `appContext.getString(...)`；可空实参补 `.orEmpty()`（坑 8）；
   `Log.*` 里的文案**没有动**（GLOSSARY §2.5）。

**CI 证据**（run 39801533790，commit `d43fb64`）：`Verify i18n resources` 通过
（`values: 1609 / values-zh-rCN: 1605 / 引用 1576 key`）、`BUILD SUCCESSFUL in 7m 10s`、
`Verify APK signature` 指纹与标准值一致（`OK: 签名与标准指纹一致，可以覆盖安装`）。

**首推失败与根因**（run 39799994047，commit `8333fee`）：`Build termux APK` 报
`AgyMcpManager.kt:4:24 Conflicting import: imported name 'Context' is ambiguous`
（第 4 行与脚本新加的第 34 行）。根因是 `apply_data_errors.py` 的导入插入**没先检查
文件已有同名导入**，而该文件第 4 行本来就 `import android.content.Context`。
→ 教训：**改写脚本插入 import 前必须先 `grep` 或在脚本里做存在性判断**，
不要假设「这个文件没导入过 X」。已把脚本那条插入改成注释警示，`fix_imports.py`
之所以没救回来，是因为它只补 `R` 与 `stringResource`，不处理普通类导入。

## 5. 工具链（`scripts/i18n/`）

| 脚本 | 作用 |
| --- | --- |
| `kotlin_scan.py` | 逐字符扫描 Kotlin 字面量，处理 `\` 转义与 `${...}` 花括号配对，跳过注释；`Text(` 要求左侧非标识符字符；行号按位置现算。输出 `/tmp/i18n_scan2.json` |
| `rebuild_catalog.py` | 用扫描结果重建 `/tmp/i18n_catalog.json`，按英文原文匹配沿用已有 key 与译文，无引用的条目标 `orphan` |
| `merge_zh.py` | 读 `key<TAB>中文` 标准输入，写入 catalog；中文写 `-` 表示不译 |
| `dynamic.py` | 插值文案专用：`find_exprs()` 拆参数，`PLURAL` 识别英文复数后缀；子命令 `list` / `merge` |
| `gen_resources.py` | 生成 `values/与 values-zh-rCN/` 的各模块资源；负责 `--` 注释打断、`%` → `%%` 转义、英文侧 `$x` → `%n$s` |
| `apply.py` | 改写 `Text("原文")` → `Text(stringResource(...))` |
| `apply_attrs.py` | 改写 `text = "原文"` / `contentDescription = "原文"`（**KINDS 已收窄，勿再加 title/label**） |
| `dynamic_apply.py` | 改写含插值的调用点为带参 `stringResource` |
| `apply_toast.py` | 改写 `Toast.makeText(ctx, "原文", ...)` → `ctx.getString(...)`；逐字符解析，能处理嵌套引号与第二参数换行；读 `toast_zh.tsv`，静态条目写 catalog、含插值写 DYN |
| `toast_zh.tsv` | Toast 文案对照表（`英文原文<TAB>中文`），88 条，便于复核与重放 |
| `apply_dyn_attrs.py` | 改写 `contentDescription`（Composable 内）与 `message`（非 Composable）两类插值站点；**内置两道闸**：非 `@Composable` 文件不做 `stringResource`、提示词内部标记一律跳过 |
| `apply_settings_ui.py` | 改写设置首页的 `title = "..."` 与安装页 `when`/`if` 分支里的字面量；按「文件 + 行区间 + 表内原文」三重限定，只动 `settings_ui_zh.tsv` 里列出的文案 |
| `settings_ui_zh.tsv` | 设置首页 / 环境安装页对照表（116 条） |
| `apply_settings_enums.py` | 把 `SettingsSection` / `ClaudeSettingsPage` 两个枚举改成 `@StringRes` 并改写使用点；配套 `settings_enums_zh.tsv`（44 条） |
| `apply_local_env.py` | 改写数据层 `LocalEnvironmentManager` 的通知标题/正文、`speedText`/`currentFileName` 等安装页状态字段、通知渠道名与描述、`errorMsg`/`errorMessage`；按「语法框架 + 对照表」双重限定，非 Composable 走 `appContext` / `context` 的 `getString`；表内条目找不到站点直接退出 |
| `local_env_zh.tsv` | 数据层文案对照表（55 条） |
| `apply_showtoast.py` | 改写 `AppToastHelper.showToast` 首参与 `Intent.createChooser` 标题；接收者按文件显式登记（`RECEIVERS`），未登记直接报错 |
| `showtoast_zh.tsv` | 分享/导出流程文案对照表（32 条） |
| `apply_p2_final.py` | P2 收尾批：坑 19 同族 + 零散可见残留；同样按「文件 + 表内原文 + 期望次数」三重限定 |
| `apply_p2_titles.py` | P2 + P3.9：把 `title = / label =` 字面量回写成 `stringResource` / `getString`；按「文件 + 表内原文 + 期望出现次数」三重限定，表外一律不碰 |
| `fix_imports.py` | 补齐 `import com.example.gemini.R` 与 `stringResource` 导入 |
| `verify.py` | **CI 编译前必跑**：资源 XML 合法性、`R.string` 引用完整性、占位符/参数一致性、非 Composable 误用、英文资源残留 Kotlin 插值、裸 `%`、**用了裸 `R.string.` 却没有 `import com.example.gemini.R`**（坑 17） |
| `gen_coverage.py` | 生成 `docs/i18n/COVERAGE.md` |

### 标准操作顺序

```bash
cd /sdcard/汉化/agy-and-claude
python3 scripts/i18n/kotlin_scan.py          # 重新扫描（改了源码后必跑）
python3 scripts/i18n/rebuild_catalog.py      # 重建目录（保留已有译文）
# 用 merge_zh.py（静态）或 dynamic.py merge（插值）补充译文
python3 scripts/i18n/gen_resources.py        # 生成资源
python3 scripts/i18n/apply.py                # 改写 Text("...")
python3 scripts/i18n/apply_attrs.py          # 改写 text = / contentDescription =
python3 scripts/i18n/dynamic_apply.py        # 改写插值调用点
python3 scripts/i18n/apply_toast.py          # 改写 Toast.makeText 字面量
python3 scripts/i18n/apply_dyn_attrs.py      # 改写 contentDescription / snackbar message
python3 scripts/i18n/apply_settings_ui.py    # 改写设置首页 title= 与安装页表达式分支
python3 scripts/i18n/apply_local_env.py      # 改写数据层通知与安装页进度文案
python3 scripts/i18n/apply_showtoast.py      # 改写分享/导出 Toast 与系统选择器标题
python3 scripts/i18n/fix_imports.py          # 补 import
python3 scripts/i18n/verify.py               # 必须通过
python3 scripts/i18n/gen_coverage.py          # 更新覆盖文档
git add -A && git commit && git push         # 触发云端 CI
```

**注意（2026-10-22 更正）**：`/tmp/i18n_*.json` 是中间状态，容器重启或换机器会丢。
原文写的「已有译文会按英文原文自动匹配回来」**不准确**——`rebuild_catalog.py` 只拿旧
catalog 做匹配，**不会**从 `values-zh-rCN/*.xml` 反查译文。所以 catalog 一丢，下次
`rebuild_catalog.py` 会生成一批空译文条目，再跑 `gen_resources.py` 就会把 XML 里的中文
**全部写空**。

**因此：每批结束都要把 catalog 备份到仓库之外**（`/sdcard/汉化/i18n_state/`），
XML 是产物但不是译文的可恢复来源。

## 6. 踩过的坑（全是真实报错，别再踩）

1. **`Text\(` 正则会误匹配 `newPlainText(`**
   → 在 `onClick` 里调用 `stringResource`，报 `@Composable invocations can only happen from the context of a @Composable function`。
   修法：正则加左侧边界 `(?<![A-Za-z0-9_$])`；`ClipData.newPlainText` 用 `context.getString`。

   **2026-10-22 P2 批次再踩一次（变体）**：`title = "..."` 写在
   `LaunchedEffect(isVisible) { ... }` 里（`BrowserScreen.kt:153`），改成 `stringResource`
   后 CI 报同一个错——`LaunchedEffect` / `remember` / `onClick` 的 lambda 都不是 Composable
   上下文；而 `verify.py` 的「非 Composable 误用」检查是**按文件**判断的，文件里有
   `@Composable` 就放行，抓不到这种嵌套情况。此类站点一律用 `context.getString(R.string.x)`
   （作用域内有 `context` 时）。

2. **正则无法解析插值里的嵌套引号**
   `Text("${if (x == 1) "" else "s"}")` 会在地一对 `""` 处截断，收录成半截英文。
   → 必须用 `kotlin_scan.py` 的逐字符扫描。

3. **行号会因插入 import 而漂移**
   扫描器若维护累加的行号变量，遇到跨行字符串/注释就错。
   → 已改为 `src.count("\n", 0, pos) + 1` 现算。

4. **XML 注释里不能出现 `--`**
   `<!-- 原文: ...(--bin for AGY...) -->` 会让 `values-zh-rCN/strings_settings.xml` 解析失败。
   → `gen_resources.py` 的 `xml_comment()` 把 `--` 打断成 `- -`。

5. **英文资源也必须是占位符**
   第一版把 Kotlin 插值原文直接写进 `values/`，切英文界面会看到 `${logs.size}` 字面量。
   → `en_with_placeholders()` 转 `%n$s`；`verify.py` 加了"英文残留 Kotlin 插值"检查。

6. **含占位符的字符串里孤立 `%` 会在运行时崩**
   `(%3$s%)` 会让 `String.format` 抛 `UnknownFormatConversionException`（编译期发现不了）。
   → `normalize_percent()` 转义为 `%%`；`verify.py` 加了"裸 `%`"检查。

7. **`title = / label =` 大量是 data class 参数**
   `ScriptExecutionInfo(title = ...)`、`sessionManager.addNewTab(title = ...)` 不是 Compose 参数，
   放 `stringResource` 必编译失败。→ 已从 `apply_attrs.KINDS` 移除。

8. **可空参数不能传给 `stringResource`**
   `stringResource(id, vararg formatArgs: Any)`，传 `String?` 报 `Argument type mismatch`。
   → `.orEmpty()`。（`context.getString` 的形参是平台类型，允许传可空；但异常 message 为 null
   时 Toast 会显示 "null"，所以那批统一补了 `.orEmpty()`。）

9. **脚本没有 `main` 保护，`import` 一下就会真改源码**
   2026-10-22 为了「核对」而 `import` 了 `apply_dyn_attrs.py`，它的顶层代码直接执行，
   一口气改了 18 处：2 处把发给模型的提示词改成了 `stringResource`（服务类不是 Composable，
   编译必挂），1 处把 `buildString {}` 里的拼接改成 `stringResource`（同样编译不过）。
   → 改写脚本必须包 `def main()` + `if __name__ == "__main__":`；
   改完必须逐条 `git diff` 复核，不能只看「改写 N 处」这种汇总数字。

10. **发给模型的提示词脚手架不能翻**
    `[Previous Context Summary: ...]`、`Please ignore the following [ignore]...[/ignore]`
    这类字符串会进 API payload，不是用户可见文案。上一轮误译过，本轮已清空译文并从资源移除。
    → `apply_dyn_attrs.py` 内置 `PROMPT_INTERNAL` 正则兜底，另有「文件里没有 `@Composable`
    就不做 `stringResource`」的闸。

11. **表达式分支里的字面量不在任何扫描范围内**
    `when (state) { Downloading -> "Downloading Toolchain..." }`、`if (isTermux) "A" else "B"`、
    `when (x) { true -> "Supported" }` —— 这些字面量求值后才传给 `Text(...)`，
    `apply.py` 只认 `Text("字面量")`，所以它们既不改写、也**不进未译清单**，
    表现为「覆盖率很高但整页英文」。→ 用 `apply_settings_ui.py` 的区间+对照表模式处理，
    见 P3.6；全仓库同类还有 70 行 when 分支 + 243 行单行三元。

12. **`title =` 不能整类排除**
    坑 7 的结论只对「data class 构造参数」成立；设置首页的
    `SettingsCategoryCard(title = "...")` 是 Composable 参数，完全可以翻。
    → 判据是**接收方是否是 @Composable**，不是参数名。

13. **数据层的 state 字段 / 通知渠道名也是用户可见，但不在任何清单里**
    `LocalInstallerState` 的 `speedText`、`currentFileName`、`stepDescription`、`testName`、
    `message`、`currentPackageName` 由 `LocalEnvironmentManager` 赋值、由安装页直接
    `Text(state.xxx)` 显示；`NotificationChannel` 的名字与 `description` 在系统通知设置里可见。
    这些既不是 `Text("...")`、也不是 `title =`、也不是 Toast，`apply.py` / `apply_attrs.py` /
    `apply_toast.py` 全都扫不到 → 表现为「通知和安装页整片英文」。
    → 判据：**字符串最终会不会被渲染/系统展示**，而不是它出现在哪个语法位置。
    同批处理过的：`LocalEnvironmentManager`（见 P3.7）。

14. **实参是表达式时，字面量嵌在表达式中间**
    `updateInstallNotification(appContext, "Installation Failed", e.localizedMessage ?: "Installation error", …)`
    的第 3 个实参不是字面量，而是表达式；里面的 `"Installation error"` 既不在
    `Toast.makeText` 第二参数位置，也不是赋值右值，按「字面量站点」扫不到。
    → `apply_local_env.py` 的做法：实参不是单个字面量时，把该实参区间内的字面量逐个改写。

15. **`gen_coverage.py` 的第 4 节是硬编码在脚本里的**
    它每次运行都会**整段重写** `docs/i18n/COVERAGE.md` 的「尚未处理的部分」，用的是脚本里的
    固定行。批次做完只改文档、不改脚本，下次一跑就把「✅ 已改」写回「❌ 未实现」，把已完成的
    记录抹掉（本轮实测踩到）。
    → 落地一个批次后，**同时改脚本里的第 4 节**，再跑 `gen_coverage.py`。
    另外它统计的是 `stringResource(` 的调用点总数，不是 `stringResource(R.string.`（多行调用会
    漏计 18 处），改口径要交叉核对 `grep -ro` 的总数。

16. **`AppToastHelper` 的 Toast 类型靠英文关键词推断**
    调用方不传 `ChatToastType` 时，`showToast` 用 `lowercase().contains("success"/"fail"/"error"/
    "copied"/…)` 推断成功/失败样式（`AppToastHelper.kt` 与 `ChatScreen.kt:311` 各有一份）。
    把消息翻成中文后这些关键词不再命中，样式会静默降级成 INFO。
    → 翻这类文案时**必须显式传 `ChatToastType`**；本轮 28 处原本就都显式传了。
    仍依赖推断的调用点（`MainActivity.showToast(message)`、`ChatScreen` 里
    `authFeedbackMessage` 的 `showToast(msg)`）之所以没受影响，是因为它们上游的文案
    （`AgyChatBackend` 的登录反馈）**目前还是英文**——下一批翻它们时必须同步传类型。

17. **改写脚本产出 `R.string.` 后必须跑 `fix_imports.py`，否则 CI 才报红**
    `apply_showtoast.py` 把 `ChatViewModel.kt` 的 8 处 Toast 改成
    `getApplication<Application>().getString(R.string.…)`，但该文件原本没用过资源，
    没有 `import com.example.gemini.R` → CI 报 `Unresolved reference 'R'`（run 39742025990 红）。
    根因不是脚本错，而是**漏跑了标准顺序里的 `fix_imports.py`**（`verify.py` 当时不查 import，
    本地一路绿灯）。
    → 两个补救：① 改写批次照标准顺序跑 `fix_imports.py`；
    ② `verify.py` 已加检查：任何含**裸** `R.string.` 的文件必须自己 `import com.example.gemini.R`
    （正则排除 `com.example.gemini.R.string.` 这种全限定写法），已用失败提交 `df798c3` 的文件内容回放确认会报错。

### 18. appcompat 的 `setApplicationLocales` 在 API 33+ 必须有「活跃的 AppCompat delegate」
`AppCompatDelegate.getLocaleManagerForApplication()` 的实现是：遍历 `sActivityDelegates`，
取第一个 delegate 的 Context，再 `context.getSystemService("locale")`；**没有活跃 delegate 就返回
null，`setApplicationLocales` 直接静默返回**（javap appcompat 1.7.0 已核实字节码）。
而 delegate 是在 `AppCompatDelegateImpl.onCreate` 里注册的，也就是 `AppCompatActivity.onCreate`
的 `super.onCreate(...)` 之后。
→ 结论：语言回设**只能放在 Activity 的 `onCreate`（super 之后）**；放 `Application.onCreate`
（本项目本来也没有自定义 Application）或任何没有 AppCompat Activity 的时机都会静默失效。
另：API<33 时 appcompat 只包装 **Activity** 的资源，`applicationContext.getString(...)`
（P3.7 的通知文案）不会跟随应用内语言——这就是待验证①。

### 19. `title = / label =` 只是同族盲区里的一类：`subtitle = / badgeText = / description = / value =` 仍会漏
P2 批次把 `title =` 与 `label =` 两类回写完了，但同一批组件还有四种「命名参数传字面量」：
设置卡副标题 `subtitle = "..."`、徽标 `badgeText = "Shield"/"Required"/"Optional"`、
权限卡 `description = "..."`、遥测行 `value = "${n} turns"/"Active"/"None"/"Configured"/"Default"`。
它们既不是 `Text("...")`（`apply.py` 扫不到）、也不是 `text =`（`apply_attrs.py` 扫不到），
所以从 P3 到 P2 全都没进过任何清单——表现为「对话框标题中文、下面每行说明仍英文」。
2026-10-22 实测：subtitle 6 处、badgeText 4 处、description 5 处、value 11 处，共 26 处。
→ 判据仍然是「字符串最终会不会被渲染」，不是参数名。
**已修**（P2 收尾批）：21 处 UI 文案 + 4 处数据层可见状态；`value =` 里剩下的是技术值
（`primary`/`pro`/`rising-fact-p41fc`/`"."`），不翻。

### 20. 「扩展里有浮层 UI」是没核实的假设——扩展 JS 里一条可见文案都没有
P4 开工前文档写「文案在 `page-helpers.js` 注入的浮层里，约 40 处浮层可见文案」。
2026-10-22 逐条核实后**完全不成立**：`page-helpers.js` 是 `chrome.scripting.executeScript`
注入到 Flow 页面的自动化库，只读取/点击 Google 自己的 DOM 并返回数据对象，**不创建任何 UI**；
`manifest.json` 没有 `action` / `options_page` / `notifications`；6 个 JS 里没有
`alert` / `chrome.notifications` / `setBadgeText`。扫描出 319 条英文句子候选，判定后
**界面可见 0 条**（详见第 4 节 P4）。

→ 教训：**「这段英文会不会被渲染」必须证到渲染路径**（谁 `Text()` 它、在哪个界面），
不能从「文件名听着像 UI 层」推断。为模型/日志准备的字符串即使翻译了，界面上也不会有任何变化。
本批次最终只改了真正可见的三块：扩展设置页对话框 6 条、管理器可显示错误 25 条、
内置扩展展示名/描述 2 条；**JS 一行未动**。

→ 同族提示：`manifest.json` 的 `name`/`description` 是运行时数据、不是 Android 资源，
且会经 `BrowserExtension.toJsonObject()` 发给扩展宿主，**不要在数据层本地化**——
只在渲染处按扩展 id 取资源（`extensionDisplayName/Description`），否则宿主侧标识会跟着变。

## 7. 关键决策记录

| 事项 | 结论 |
| --- | --- |
| 范围 | App UI + 系统可见层（无障碍服务名/通知/Toast）+ AI 回复 + 浏览器扩展；**终端与日志不翻** |
| 交付 | 只改源码 + 云端 CI 编译，不本地构建；分支 + PR 由用户审 |
| 双语 | 正式 i18n，`values/` + `values-zh-rCN/`，设置页提供切换（待做） |
| prompt | AI 回复语言**跟随界面语言** |
| 质量 | 机翻 + 术语表统一，不引入 `plurals`（中文无单复数） |
| 存疑项 | 无法判定用户可见的一律**保留英文**，并在提交信息里列出 |
| 资源组织 | 按模块拆 `strings_settings/ide/claude/browser/drawer/chat/agents/components/ui/data/common.xml` |
| 原文留档 | `values-zh-rCN/` 每条译文上方 `<!-- 原文: ... -->` |
| flavor | **termux**（`com.termux`），与上游发版同包名；覆盖安装另需签名一致，见第 10 节 |
| 签名 | 需要仓库 secret `RELEASE_KEYSTORE_BASE64`；缺失时自动退回 debug 签名并出 warning |

术语表见 `docs/i18n/GLOSSARY.md`（Agent=智能体、Session=会话、Workspace=工作区、
Extension=扩展、Bridge=桥接、Skill=技能、Trajectory=执行轨迹、Prompt=提示词、Hub=中枢）。
**改译法前先改术语表。**

## 8. 验收方法

1. GitHub → Actions → `CI Build` → 某次运行 → 底部 Artifacts → `antiGem-termux-<sha>` → 解压得 APK
2. 装到手机，**系统语言设为简体中文**
3. 按 `docs/i18n/COVERAGE.md` 逐项核对
4. 每次 CI 约 7–9 分钟；`verify i18n resources` 步骤失败会 10 秒内红，比等 Gradle 快
5. **装之前先确认 `com.termux` 没被别的应用占着**（Termux / Termux Ultra 也用这个包名），
   以及手上这个包的签名是不是标准指纹，见第 10 节

### 当前签名密钥的来源与保管（重要）

仓库原本**一个 secret 都没有**（`gh api .../actions/secrets` 显示 `total_count: 0`），
上游发版用的 `release.jks` 也**从未进入过 git 历史**（`.gitignore:18` 屏蔽），
本机磁盘上也没有——所以**拿不到上游那把私钥**。

2026-10-22 已用 keytool **新生成**一把密钥并写入仓库 secret：

```
Name:   RELEASE_KEYSTORE_BASE64
来源:   新生成（不是上游那把）
keystore: /sdcard/汉化/keystore/antigem-release.jks（另有一份 /tmp/antigem-release.jks）
别名:   antigem   口令: 123456（与 app/build.gradle.kts 里写死的签名配置一致）
有效期: 2026-10-22 ~ 2056-10-14
SHA256: 6F:F1:E3:FF:A0:C1:5F:2F:64:74:74:FB:04:F4:78:FD:1D:06:C0:88:40:CC:6B:6E:6E:72:ED:6A:94:2A:7B:5C
```

**后果**：CI 从此产出 release 签名的包，但签名身份是这把新密钥，
**仍然覆盖不了手机上官方版本**。要真正覆盖官方版，需要用户提供上游的 `release.jks`，
然后用 `gh secret set RELEASE_KEYSTORE_BASE64 --body "$(base64 -w0 release.jks)"` 覆盖即可，
其它都不用改。

**千万保管好这个文件**：一旦丢失，之后所有更新都无法覆盖已安装的版本（只能卸载重装）。

### 没有上游私钥时怎么迁移数据

App 自带备份（设置页：「备份 AGY 对话」「Rootfs 备份」「项目备份」）：

1. 在官方版里导出备份到 `/sdcard/Download/Antigem/backups/`
2. 卸载官方版
3. 装汉化版
4. 用「恢复 AGY 对话」「恢复项目」把数据导回来

### 配 release 签名（实现"覆盖安装官方版本"）

```bash
# 本地把上游发版用的 release.jks 转 base64
base64 -w0 release.jks > keystore.b64
# GitHub 仓库 Settings → Secrets and variables → Actions → New repository secret
#   Name: RELEASE_KEYSTORE_BASE64    Value: keystore.b64 的内容
```

`app/build.gradle.kts` 里的签名口令是明文写死的：
`storePassword = "123456"`、`keyAlias = "antigem"`、`keyPassword = "123456"`。

**没有这个 secret 时只能产出 debug 签名包，无法覆盖安装官方版本**（签名不匹配，只能卸载重装，数据全丢）。

### 当前产物的实测数据（commit `715f480`，run 39690787215）

```
$ aapt2 dump badging antiGem-termux-v1.0.13-debug-715f480.apk
package: name='com.termux' versionCode='113' versionName='1.0.13'
sdkVersion:'24' targetSdkVersion:'28'
```

CI 日志：
```
##[warning]未配置 RELEASE_KEYSTORE_BASE64，本次只能产出 debug 签名包，无法覆盖安装官方版本
==> :app:assembleTermuxDebug (debug 签名)
artifact: antiGem-termux-715f4807eae3a029cadcedc42ab194c452dd56e5  (39 MB)
```

即：**包名已与上游一致（`com.termux`），但签名仍是 debug**。
下一轮第一件事就是让用户配置 `RELEASE_KEYSTORE_BASE64`，然后 CI 会自动切到
`:app:assembleTermuxRelease`，产物文件名里的 `debug` 会变成 `release`。

## 8.5 已解除的上游耦合（2026-10-22）

fork 用自己的签名密钥后，**应用内更新必须指向 fork 仓库**，否则会去拉上游签名的官方包：
签名不同导致安装失败；若上游把 `is_critical` 置真或抬高 `min_supported_version_code`，
`MainActivity` 启动时的自动检查会弹出**不可关闭**的更新框（`AppUpdateDialog` 里
`dismissOnBackPress/dismissOnClickOutside` 在 critical 时为 false），应用直接不可用。

已改：

```kotlin
// data/updater/AppUpdateManager.kt
private const val UPDATE_REPO = "743553649/agy-and-claude"
private const val UPDATE_BRANCH = "master"
private const val DEFAULT_VERSION_URL =
    "https://raw.githubusercontent.com/$UPDATE_REPO/$UPDATE_BRANCH/version.json"
// 下载地址同样改用 $UPDATE_REPO
```

`ui/updater/AppUpdateDialog.kt` 的「访问仓库」按钮也改指 fork。

**未改（有意保留）**：
- `ui/settings/SettingsDialog.kt:9155 / 9203` 的 GitHub 仓库与 releases 链接——属于「关于」页信息，不是更新检查
- `data/local/LocalEnvironmentManager.kt:64` 的 `HARDCODED_ROOTFS_URL`——rootfs 是数据文件，从上游下载是正确的

## 8.6 会话收尾时的确切状态（2026-10-22）

分支 `feat/zh-cn-i18n`，最后一次提交 `f3d15f4`，**已全部推送**，工作区干净。

| 项 | 状态 |
| --- | --- |
| 汉化覆盖 | 705 条有引用文案已译 676 条（95.9%），1324 处 `stringResource`，71 个文件 |
| 资源 | `values/` 1143 条，`values-zh-rCN/` 1139 条，22 个模块文件 |
| CI | 已改 termux flavor；`verify` 通过；release 签名成功；产物 14MB |
| 签名 | 新生成密钥已入 secret，release 包签名路径打通 |
| 更新检查 | 已改指 fork 仓库，解除上游耦合 |
| localeConfig | 已补，Android 13+ 可单列中文 |

### 产物字节级校验结果（已通过，commit `f3d15f4`）

```
$ aapt2 dump badging antiGem-termux-v1.0.13-release-f3d15f4.apk
package: name='com.termux' versionCode='113' versionName='1.0.13'
sdkVersion:'24' targetSdkVersion:'28'

$ 解析 APK Signing Block（v2，无 v1 签名）
v2 签名存在，证书长度 868
证书 SHA-256: 6FF1E3FFA0C15F2F647474FB04F478FD1D06C08840CC6B6E6E72ED6A942A7B5C
```

与 `app/release.jks` 的证书指纹逐位一致 →
**包名 `com.termux` ✅、release 签名确实用的是我们配的那把密钥 ✅**。

> 注：本机 `apksigner` 因 JDK 的 `LoggerFinderLoader` 初始化失败无法运行
> （`ExceptionInInitializerError`），以上签名是通过解析 APK Signing Block 的
> v2 签名结构、取证书做 SHA-256 得到的，结论等价。

### 唯一未完成的验证：release 包的运行时行为

release 版开了 `isMinifyEnabled = true` / `isShrinkResources = true`
（debug 44MB → release 19MB）。R8 可能裁剪反射使用的类
（kotlinx.serialization / wire protobuf / WebView JS 桥）。
**首装后必须人工确认**：对话能发消息、终端能起、IDE 能打开、Claude 能连。

### 网络排障经验（会再遇到）

容器内部访问 GitHub 时，若用户开了 VPN（Clash/Surge 一类），DNS 会把
`api.github.com` 解析成 `198.18.x.x`（fake-ip 段），但容器流量不走代理数据面，
表现为 `gh` 报 `EOF`、`curl` 报 `SSL_ERROR_SYSCALL`，而访问国内站点正常。
**判断方法**：`getent hosts api.github.com` 若返回 `198.18.*` 即是此问题。
**处理**：让用户临时关闭 VPN，或改用浏览器手动下载 artifact。

## 8.7 CI 构建耗时与缓存（2026-10-22 实测）

**为什么以前每次都要 6.5–9 分钟**：`gradle/actions/setup-gradle@v4` 默认**只在默认分支写缓存**，
其它分支只读；本仓库默认分支是 `master`，而 master 从没跑过这个 workflow，
所以仓库缓存条目是 **0 条**，每次 push 到 feat 分支都是空 `~/.gradle` 从零开始——
日志证据：`cache-read-only: true`、`Gradle User Home cache not found. Will initialize empty.`，
连「只改文档」的提交也是 53 个任务全部 executed（零 UP-TO-DATE）。

**改法（commit `03a24b0`，只动 `ci.yml`）**：

1. `Setup Gradle` 加 `cache-read-only: false` —— 允许在 feat 分支写 Gradle User Home 缓存
   （含依赖、artifact transform 和**本地 build cache**，见官方文档 "Which content is cached"）；
2. 新增 `actions/cache@v4` 缓存项目级 `.gradle`（配置缓存），key 含构建脚本 hash；
3. `setup-go` 加 `cache-dependency-path: agy_ide_bridge/go.sum`（`go.sum` 不在仓库根，
   原来每次都在报 `Restore cache failed: Dependencies file is not found`）。

**实测（同一 commit `03a24b0`，全部 success）**：

| 场景 | Build 步骤 | 整个 job | 任务分布 |
| --- | --- | --- | --- |
| 基线：文档提交 `45399df`（无缓存） | 385s | ~408s | 53 executed |
| 基线：A-2 提交 `1519dc6`（无缓存） | 552s | ~580s | 53 executed |
| 冷跑 attempt 1（首次写缓存，多写 ~900MB） | 550s | **597s** | 全量下载 + 写 build cache，比原来更慢 |
| 热跑 attempt 2（缓存命中） | **30s** | **70s** | `26 executed, 25 from cache, 2 up-to-date` |

热跑里两个大头都是从 build cache 恢复的：
`> Task :app:compileTermuxReleaseKotlin FROM-CACHE`、
`> Task :app:minifyTermuxReleaseWithR8 FROM-CACHE`（原来分别 ~96s / ~113s）。

**边界（别夸大）**：

- 30s 只适用于**构建输入没变**的提交（文档/脚本/注释改动、或重跑同一 SHA）。
  真实改 Kotlin 的提交要重跑 Kotlin(96s) + R8(113s) + Compose mapping(27s)，
  只省掉依赖下载与配置那 ~70s → **估算 4.5–5.5 分钟**（按分段耗时估算，尚未实测）。
- 冷跑比原来更慢是正常的：第一次要上传 ~900MB 缓存（当前 10 条 / 906MB；GitHub 上限 10GB，
  action 自带 `cache-cleanup: on-success`）。
- 缓存 key 内含 SHA，跨提交靠 `restore-keys` 前缀恢复最近一条，所以**下一个提交也会命中**，
  不只是重跑同一 SHA。

**已知未生效项（别再重复踩）**：`actions/cache` 缓存 `.gradle` 那步恢复了 2MB
（日志 `Cache restored from key: cc-Linux-…`），但 Gradle 仍报
`Calculating task graph as no cached configuration is available` —— 配置缓存没有真正复用，
**原因未查明**，目前零收益、只多花 1 秒。要么留着等排查，要么删掉这一步。

**怎么复测**：`gh run rerun <run-id>` 重跑同一 SHA（热跑），再用
`gh run view --job=<job-id> --log` 拉日志，按每行的时间戳拆各步骤耗时；
缓存是否写出看 `gh api repos/<owner>/<repo>/actions/caches`。

## 9. 关键文件索引

| 路径 | 说明 |
| --- | --- |
| `docs/i18n/GLOSSARY.md` | 术语表、不翻白名单、占位符约定、AI 语言指令的唯一措辞 |
| `docs/i18n/COVERAGE.md` | 按界面的覆盖清单（核对用） |
| `docs/i18n/HANDOFF.md` | 本文档 |
| `docs/i18n/p1-language-switch-plan.md` | **P1 方案 A 实施计划**（语言切换开工前必读） |
| `docs/i18n/language-switch-options.html` | P1 三方案对比（A/B/C）与决策依据，含本机核实证据 |
| `scripts/i18n/` | 全部汉化工具 |
| `.github/workflows/ci.yml` | 云端构建（termux flavor + verify） |
| `.github/workflows/release.yml` | 上游发版流程，**不要动** |
| `app/src/main/res/values*/strings_*.xml` | 资源文件 |
| `app/src/main/java/com/example/gemini/domain/i18n/AiLanguage.kt` | AI 语言策略（P0 已接入，见第 4 节） |

## 10. 签名标准（唯一版本，不得更换）

一句话：**本项目只能有一把签名密钥**。换了它，任何已装好的旧版都无法被覆盖更新，
用户只能卸载重装、数据全丢。所以先把它钉死，再让 CI 每次构建自动校验。

### 标准值（2026-10-22 定版）

| 项 | 值 |
| --- | --- |
| 证书 SHA-256 | `6F:F1:E3:FF:A0:C1:5F:2F:64:74:74:FB:04:F4:78:FD:1D:06:C0:88:40:CC:6B:6E:6E:72:ED:6A:94:2A:7B:5C` |
| 密钥库 | `/sdcard/汉化/keystore/antigem-release.jks`（sha256 `cafb0a2a2815d4376974c97e63062eac7ec4c0e2ba16365e8cce4723428e3a55`，`/tmp/antigem-release.jks` 与它逐字节相同） |
| 别名 / 口令 | `antigem` / `123456` / `123456`（与 `app/build.gradle.kts` 里写死的签名配置一致） |
| 证书主体 | `CN=Antigem, OU=Antigem, O=Antigem, L=NA, ST=NA, C=CN` |
| 有效期 | 2026-10-22 ~ 2056-10-14 |
| 仓库 secret | `RELEASE_KEYSTORE_BASE64`（2026-10-22T01:57:44Z 写入） |

**三方已闭合验证**（2026-10-22）：

1. 本地密钥库：`keytool -list -v` 实测指纹 = 上表
2. GitHub secret 还原后构建的包：`f3d15f4` 产物实测指纹 = 上表（证书 868 字节，v2 签名）
3. 因此 CI 产出的 release 包可以覆盖本项目此前用同一密钥签的任何版本

### 为什么不是上游那把

上游 `release.jks` 从未进入 git 历史，磁盘上也没有，**拿不到**。所以这把是新建的。
后果：我们的包覆盖不了官方版，官方版也覆盖不了我们的包；二者只能装一个，切换必须
卸载重装（数据用 App 自带备份迁移，见 8.5）。

### 每次构建自动校验

`scripts/verify_apk_signature.py` 解析 APK Signing Block 的 v2 签名结构取证书 DER 算
SHA-256（`apksigner` 在本机因 JDK `LoggerFinderLoader` 初始化失败不可用，这条路结论等价），
`ci.yml` 的 `Verify APK signature` 步骤把它打印进构建摘要，指纹不符直接红。

本地手工核对：

```bash
python3 scripts/verify_apk_signature.py /path/to/antiGem-termux-*.apk
# 退出码 0 = 与标准指纹一致；1 = 不一致（装不上旧版）；2 = 没有 v2 签名
```

### 安装前提（踩过一次，2026-10-22）

`com.termux` 这个包名很抢手，装之前必须确认手机上没被别的应用占着：

- 官方 AntiGem（termux flavor）——签名 `CC:F2:C6:81:...`，不同，装不上
- **Termux / Termux Ultra 等真正的终端应用**——它们也用 `com.termux`，签名更不同。
  实测手机上一度装着 Termux Ultra 118.3.50（versionCode 1043，Play 商店装的），
  我们的包是 113，签名与版本号双重不相容
- 本项目早期的 **debug 签名**包——GitHub runner 每次都是全新环境，`~/.android/debug.keystore`
  是现生成的，所以**两个 debug 包之间也互不覆盖**

卸载占用包名的应用会**连带删除它的数据**（Termux 的 home 在 `/data/data/com.termux`），
动手前先备份。

### 不要做的事

- 不要重新生成密钥（`keytool -genkey`），除非接受所有用户卸载重装
- 不要删改 secret `RELEASE_KEYSTORE_BASE64`
- 不要把 `app/release.jks` 提交进仓库（`.gitignore` 已屏蔽）；也不要把密钥库只留在手机上
