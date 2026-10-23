# 汉化覆盖清单（核对用）

数字为源码中 `stringResource` 调用点的实际数量（由 `scripts/i18n/gen_coverage.py` 统计）。

## 一、按界面核对

| 界面 | 覆盖内容 | 调用点 |
| --- | --- | --- |
| 设置（主设置页） | 所有分区的开关、说明段落、危险操作确认 | 471 |
| 设置 → 技能与插件 | 技能列表、插件安装与删除 | 35 |
| 设置 → WebView 扩展 | 扩展安装、重命名、更新 | 28 |
| 设置 → 本地工具安装 | 安装流程 | 65 |
| 设置 → 本地工具安装（全屏） | 步骤指引与日志 | 76 |
| 设置 → 空间管理 | 缓存与数据清理 | 21 |
| 侧边抽屉（对话历史） | 搜索、分组、批量操作、删除确认 | 75 |
| 抽屉 → 账号详情 | 额度、订阅、登出 | 15 |
| 抽屉 → 产物 | 生成文件列表 | 7 |
| 对话页 | 输入栏、工具卡片、空态提示 | 48 |
| 对话气泡 | 消息操作菜单、复制/引用 | 28 |
| Markdown 与代码块 | 代码块复制、Mermaid 图 | 31 |
| 对话遥测 | Token 与用量明细 | 16 |
| 上下文摘要卡 | 压缩提示与排队状态 | 18 |
| 智能体问卷卡 | 问卷选项 | 12 |
| IDE | 编辑器、运行配置、日志面板 | 58 |
| IDE → Git | 暂存、提交、拉取推送、冲突处理 | 87 |
| IDE → 项目侧栏 | 新建/重命名/删除文件与文件夹 | 40 |
| 内置浏览器 | 标签页、地址栏、DevTools | 65 |
| Claude 设置 | 账号、MCP 服务器、模型、用量 | 164 |
| Claude 对话控件 | 模式、权限、上下文、紧凑化 | 45 |
| Claude 登录 | 付费方式与验证码 | 11 |
| 智能体 → 批准面板 | 允许 / 拒绝 / 始终允许 | 10 |
| 智能体 → 提问 | 问卷与分页 | 4 |
| 智能体 → 引导 | 选择启用哪些智能体 | 11 |
| 终端引导 | 首次使用选择访问方式 | 11 |
| 模型选择 | 模型列表与推理参数 | 14 |
| 更新弹窗 | 版本说明与下载 | 13 |
| 主屏 | 加载态与错误态 | 2 |
| 文件管理器 | 浏览、复制、移动、重命名 | 47 |
| 本地终端 | 会话标签、命令输出 | 13 |
| 悬浮终端检查器 | 状态胶囊与详情 | 16 |
| 网络检查器 | 请求/响应头、筛选 | 25 |
| 自定义系统提示词 | 默认提示词展示与编辑 | 9 |
| 项目选择 | 项目列表 | 11 |
| 共享对话查看 | 只读对话 | 7 |
| 代码块 | 复制按钮 | 20 |
| 权限申请 | 权限说明 | 14 |
| 对话加载骨架 | 占位提示 | 13 |

## 二、组件与其余已汉化文件

| 文件 | 调用点 |
| --- | --- |
| `AgentToolCallCard.kt` | 70 |
| `ClaudeStatusViews.kt` | 37 |
| `FileDetailsDialog.kt` | 33 |
| `ChatInputBar.kt` | 20 |
| `CascadeMcpCatalogDialog.kt` | 15 |
| `WorkspaceFolderBrowserDialog.kt` | 13 |
| `FloatingDiagnosticsOverlay.kt` | 12 |
| `FileLinkHandler.kt` | 10 |
| `ConnectionStatusBadge.kt` | 9 |
| `SpeechToTextModeDialog.kt` | 8 |
| `ThinkingSelectorBottomSheet.kt` | 8 |
| `DaemonLogsDialog.kt` | 7 |
| `ImageAssetViewer.kt` | 7 |
| `ClaudeModeSheets.kt` | 6 |
| `InteractiveUiView.kt` | 6 |
| `RawPayloadDialog.kt` | 6 |
| `UnifiedDiffViewer.kt` | 6 |
| `AttachmentSelectorBottomSheet.kt` | 5 |
| `LocalServerOutputDialog.kt` | 5 |
| `SummaryModelPickerDialog.kt` | 5 |
| `NewProjectDialog.kt` | 5 |
| `FloatingNetworkInspectorBubble.kt` | 4 |
| `IdeExecutionRunner.kt` | 4 |
| `FloatingSwitcherWidget.kt` | 3 |
| `FloatingVideoPlayerOverlay.kt` | 3 |
| `FullScreenImageDialog.kt` | 3 |
| `ThinkingAccordion.kt` | 3 |
| `AccountsUi.kt` | 2 |
| `AgentStatusUi.kt` | 2 |
| `FloatingBubbleView.kt` | 1 |
| `SlashCommandOverlay.kt` | 1 |
| `ClaudeUiKit.kt` | 1 |
| `ChatAudioPlayer.kt` | 1 |
| `SelectTextSheet.kt` | 1 |

合计：**73 个文件、1978 处 `stringResource` 调用点**（另有 478 处 `getString`，用于非 Composable 上下文：Toast、通知、数据层）。

## 三、有意保留英文的内容

| 内容 | 说明 |
| --- | --- |
| 产品名 / 模型名 | `AntiGem`、`Antigem`、`Termux`、`Antigravity`、`Claude`、`Gemini`、`Nano Banana` 等 |
| VS Code 变量模板 | `${file}`、`${workspaceFolder}`、`${fileBasename}` 等 |
| 示例路径与 URL | `/sdcard/Download/...`、`https://github.com/...` |
| 动画 / 测试标识 | `shimmerTransition`、`drawerChatListFade`、`thought_chevron_rotation` 等 |
| 日志与异常 | `Log.d/i/w/e` 内容保持英文，便于搜 issue、上报上游 |
| 终端输出 / shell 命令 / 文件路径 / 代码块 | 翻译会造成误操作 |
| 上游文档 | README、AGENTS.md、CLAUDE_CLI_PROTOCOL.md |

## 四、尚未处理的部分

| 项目 | 状态 |
| --- | --- |
| AI 回复内容本身 | ✅ 已实现（Gemini 系统指令 + Claude `initialize`，commit `8fdd016`；CLI 是否真应用需真机验证） |
| `Toast.makeText` 的 104 处字面量 | ✅ 已改（`context.getString`，commit `6310f1c`，新增 88 条 `*_toast_*` 资源） |
| `AppToastHelper.showToast` 与系统选择器标题 | ✅ 已改（36 处：28 处 showToast 字面量 + 8 处 `Intent.createChooser` 标题；`context.getString`，32 条 `*_apptoast_*` / `*_chooser_*` 资源） |
| 含插值的 `contentDescription` / snackbar `message` | ✅ 已改（16 处，`apply_dyn_attrs.py`） |
| `title = / label =` 字面量 | ✅ 已改 50 处（`apply_p2_titles.py`：遥测/文件详情/附件/语音/权限卡/Git/模型选择/设置首页/5 个脚本标题/数据层对话标题/浏览器标签页 + 2 处插值）；剩余 58 处全是动画 label（30）、终端按键（22）、产品名（4）与 2 处待模型改造 |
| 设置首页卡片文案 | ✅ 已改（`title =` / `when` 分支，commit `5d55bc3`，135 处 / 107 条资源） |
| 环境安装页（全屏 + 对话框） | ✅ 已改（状态标题、硬件取值、下载选项、按钮，同上提交） |
| 设置顶栏 / 分组枚举 | ✅ 已改（`SettingsSection` 与 `ClaudeSettingsPage` 改 `@StringRes`，commit `4d25d0f`） |
| 安装通知与进度文案（数据层） | ✅ 已改（`LocalEnvironmentManager` 77 处：通知标题/正文、`speedText`、安装页状态字段、通知渠道名与描述、错误文案；`appContext.getString`，55 条 `data_install_*` 资源） |
| `when`/`if` 分支字面量 | ✅ 已清可见部分（工具卡片 55、聊天后端 32、文件详情 40、设置页 42、Claude 设置 14、浏览器与对话页 22、六面板 26、抽屉/IDE/effort 14、ConfigManager 26、项目侧栏 6）；剩余按判据留英文：轨迹拼装、HTTP 协议值、单位格式、产品名、数据兼显示、未证明渲染路径 |
| 设置页语言切换入口 | ✅ 已实现（方案 A：appcompat 1.7.0；A-1 `ef5a244` + A-2 `1519dc6`；API<33 的通知与静态 `Locale.getDefault()` 仍待真机验证） |
| 外观子页残留标签（Auto (System)/Light/Dark、字号、自动收起、悬浮切换器条目） | ✅ 已改（P3.9，13 个标签；方向 `VERTICAL`/`HORIZONTAL` 的技术值保留英文） |
| `subtitle =` / `badgeText =` / `description =` / `value =` 字面量 | ✅ 已改（P2 收尾批：21 处 UI 文案 + 4 处数据层可见状态 + 2 处通知渠道描述；`value =` 里技术值保留；见 HANDOFF 坑 19） |
| 审批面板 `ApprovalItem` 整族 | ✅ 已改（`denyLabel`/`denyPlaceholder` 改 `@StringRes`，两个生产者加 `Context`，标题/选择项/建议范围/权限模式标签共 30 余条资源化） |
| `ConversationShareHelper` 的 `Shared Conversation` | ✅ 已改（`parseSharedConversation` 增加 `Context` 参数） |
| 数据层错误族（`AgyMcpManager` / `ClaudeAccountManager`） | ✅ 已改（两个类构造函数注入 `appContext: Context`（`ChatViewModel` 传 `application`），共 36 处可见文案：MCP 加载/刷新/启停/保存/删除/插件安装 + Claude 状态检查/用量/登录登出；12 条静态 + 20 条插值资源，全部 `appContext.getString`） |
| `ClaudeStatusViews` 空态与状态横幅 | ✅ 已改（不止 7 个 label：整块空态 35 处——6 个状态的标题/说明/主次按钮 + Banner 文字与药丸按钮；能复用的既有 key 全部复用） |
| 浏览器扩展文案 | ✅ 已改（核实结论：6 个 JS 文件无浮层 UI、界面可见文案为 0，故一行 JS 未改；实际改的是设置页安装/更新对话框与操作失败兜底 6 条、`BrowserExtensionManager` 可显示错误 25 条、内置扩展展示名/描述 2 条；见 HANDOFF P4 与坑 20） |

## 五、怎么核对

1. 装 `antiGem-termux-*.apk`，手机系统语言设为简体中文
2. 按上表逐项打开界面，看是否还有英文
3. 「设置」文案最密集，先看它
4. 发现遗漏就把界面名 + 那行英文发我，可据此定位到文件与行号
