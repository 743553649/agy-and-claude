# 汉化覆盖清单（核对用）

数字为源码中 `stringResource` 调用点的实际数量（由 `scripts/i18n/gen_coverage.py` 统计）。

## 一、按界面核对

| 界面 | 覆盖内容 | 调用点 |
| --- | --- | --- |
| 设置（主设置页） | 所有分区的开关、说明段落、危险操作确认 | 327 |
| 设置 → 技能与插件 | 技能列表、插件安装与删除 | 35 |
| 设置 → WebView 扩展 | 扩展安装、重命名、更新 | 19 |
| 设置 → 本地工具安装 | 安装流程 | 27 |
| 设置 → 本地工具安装（全屏） | 步骤指引与日志 | 30 |
| 设置 → 空间管理 | 缓存与数据清理 | 21 |
| 侧边抽屉（对话历史） | 搜索、分组、批量操作、删除确认 | 63 |
| 抽屉 → 账号详情 | 额度、订阅、登出 | 15 |
| 抽屉 → 产物 | 生成文件列表 | 7 |
| 对话页 | 输入栏、工具卡片、空态提示 | 32 |
| 对话气泡 | 消息操作菜单、复制/引用 | 25 |
| Markdown 与代码块 | 代码块复制、Mermaid 图 | 29 |
| 对话遥测 | Token 与用量明细 | 6 |
| 上下文摘要卡 | 压缩提示与排队状态 | 18 |
| 智能体问卷卡 | 问卷选项 | 10 |
| IDE | 编辑器、运行配置、日志面板 | 51 |
| IDE → Git | 暂存、提交、拉取推送、冲突处理 | 73 |
| IDE → 项目侧栏 | 新建/重命名/删除文件与文件夹 | 34 |
| 内置浏览器 | 标签页、地址栏、DevTools | 49 |
| Claude 设置 | 账号、MCP 服务器、模型、用量 | 45 |
| Claude 对话控件 | 模式、权限、上下文、紧凑化 | 25 |
| Claude 登录 | 付费方式与验证码 | 11 |
| 智能体 → 批准面板 | 允许 / 拒绝 / 始终允许 | 5 |
| 智能体 → 提问 | 问卷与分页 | 4 |
| 智能体 → 引导 | 选择启用哪些智能体 | 10 |
| 终端引导 | 首次使用选择访问方式 | 11 |
| 模型选择 | 模型列表与推理参数 | 10 |
| 更新弹窗 | 版本说明与下载 | 13 |
| 主屏 | 加载态与错误态 | 2 |
| 文件管理器 | 浏览、复制、移动、重命名 | 47 |
| 本地终端 | 会话标签、命令输出 | 13 |
| 悬浮终端检查器 | 状态胶囊与详情 | 16 |
| 网络检查器 | 请求/响应头、筛选 | 10 |
| 自定义系统提示词 | 默认提示词展示与编辑 | 9 |
| 项目选择 | 项目列表 | 11 |
| 共享对话查看 | 只读对话 | 7 |
| 代码块 | 复制按钮 | 12 |
| 权限申请 | 权限说明 | 5 |
| 对话加载骨架 | 占位提示 | 13 |

## 二、组件与其余已汉化文件

| 文件 | 调用点 |
| --- | --- |
| `AgentToolCallCard.kt` | 14 |
| `CascadeMcpCatalogDialog.kt` | 14 |
| `ChatInputBar.kt` | 13 |
| `WorkspaceFolderBrowserDialog.kt` | 13 |
| `FloatingDiagnosticsOverlay.kt` | 12 |
| `FileLinkHandler.kt` | 10 |
| `ConnectionStatusBadge.kt` | 9 |
| `DaemonLogsDialog.kt` | 7 |
| `ImageAssetViewer.kt` | 7 |
| `ThinkingSelectorBottomSheet.kt` | 7 |
| `InteractiveUiView.kt` | 6 |
| `RawPayloadDialog.kt` | 6 |
| `UnifiedDiffViewer.kt` | 6 |
| `LocalServerOutputDialog.kt` | 5 |
| `SummaryModelPickerDialog.kt` | 5 |
| `NewProjectDialog.kt` | 5 |
| `FloatingNetworkInspectorBubble.kt` | 4 |
| `SpeechToTextModeDialog.kt` | 4 |
| `IdeExecutionRunner.kt` | 4 |
| `FloatingSwitcherWidget.kt` | 3 |
| `FloatingVideoPlayerOverlay.kt` | 3 |
| `FullScreenImageDialog.kt` | 3 |
| `ThinkingAccordion.kt` | 3 |
| `AccountsUi.kt` | 2 |
| `AgentStatusUi.kt` | 2 |
| `FloatingBubbleView.kt` | 1 |
| `SlashCommandOverlay.kt` | 1 |
| `ClaudeStatusViews.kt` | 1 |
| `AttachmentSelectorBottomSheet.kt` | 1 |
| `ChatAudioPlayer.kt` | 1 |
| `FileDetailsDialog.kt` | 1 |
| `SelectTextSheet.kt` | 1 |

合计：**72 个文件、1496 处 `stringResource` 调用点**（另有 113 处 `context.getString`）。

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
| `AppToastHelper.showToast` 27 处 | ❌ 未改（项目自定义 Toast 包装，不叫 `Toast.makeText`，下一步处理） |
| 含插值的 `contentDescription` / snackbar `message` | ✅ 已改（16 处，`apply_dyn_attrs.py`） |
| `title = / label =` 字面量 112 处 | ❌ 未改（多为 data class 构造参数，需逐条判定白名单后改造） |
| 设置首页卡片文案 | ✅ 已改（`title =` / `when` 分支，commit `5d55bc3`，135 处 / 107 条资源） |
| 环境安装页（全屏 + 对话框） | ✅ 已改（状态标题、硬件取值、下载选项、按钮，同上提交） |
| 设置顶栏 / 分组枚举 | ✅ 已改（`SettingsSection` 与 `ClaudeSettingsPage` 改 `@StringRes`，commit `4d25d0f`） |
| 其余 `when`/`if` 分支字面量 | ❌ 未改（实测 70 行 `when` 分支 + 243 行单行三元，需剔技术值白名单） |
| 安装通知与进度文案（数据层） | ❌ 未改（`LocalEnvironmentManager` 35 处通知 + 5 处 speedText，如 `Connecting...`） |
| 设置页语言切换入口 | ❌ 未实现（项目无 `appcompat` 依赖，方案待定） |
| 浏览器扩展浮层文案 | ❌ 未实现（约 40 处） |

## 五、怎么核对

1. 装 `antiGem-termux-*.apk`，手机系统语言设为简体中文
2. 按上表逐项打开界面，看是否还有英文
3. 「设置」文案最密集，先看它
4. 发现遗漏就把界面名 + 那行英文发我，可据此定位到文件与行号
