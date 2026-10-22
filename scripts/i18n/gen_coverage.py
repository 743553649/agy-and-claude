#!/usr/bin/env python3
"""生成 docs/i18n/COVERAGE.md：按界面统计 stringResource 调用点，供逐项核对。

数字直接来自源码统计，不依赖 /tmp 下的中间状态。
"""
import collections
import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SRC = os.path.join(ROOT, "app/src/main/java")
OUT = os.path.join(ROOT, "docs/i18n/COVERAGE.md")

SCREEN = [
 ('SettingsDialog.kt', '设置（主设置页）', '所有分区的开关、说明段落、危险操作确认'),
 ('SkillsAndPluginsSubScreen.kt', '设置 → 技能与插件', '技能列表、插件安装与删除'),
 ('WebViewExtensionsScreen.kt', '设置 → WebView 扩展', '扩展安装、重命名、更新'),
 ('LocalToolsInstallDialog.kt', '设置 → 本地工具安装', '安装流程'),
 ('FullScreenLocalToolsInstaller.kt', '设置 → 本地工具安装（全屏）', '步骤指引与日志'),
 ('ManageSpaceActivity.kt', '设置 → 空间管理', '缓存与数据清理'),
 ('ChatHistoryDrawer.kt', '侧边抽屉（对话历史）', '搜索、分组、批量操作、删除确认'),
 ('ProfileDetailDialog.kt', '抽屉 → 账号详情', '额度、订阅、登出'),
 ('ArtifactsDrawerContent.kt', '抽屉 → 产物', '生成文件列表'),
 ('ChatScreen.kt', '对话页', '输入栏、工具卡片、空态提示'),
 ('MessageBubble.kt', '对话气泡', '消息操作菜单、复制/引用'),
 ('MarkdownContent.kt', 'Markdown 与代码块', '代码块复制、Mermaid 图'),
 ('ChatTelemetryDialog.kt', '对话遥测', 'Token 与用量明细'),
 ('ContextSummaryCard.kt', '上下文摘要卡', '压缩提示与排队状态'),
 ('ChoiceQuestionnaireCard.kt', '智能体问卷卡', '问卷选项'),
 ('IdeScreen.kt', 'IDE', '编辑器、运行配置、日志面板'),
 ('GitSourceControlView.kt', 'IDE → Git', '暂存、提交、拉取推送、冲突处理'),
 ('ProjectSidebar.kt', 'IDE → 项目侧栏', '新建/重命名/删除文件与文件夹'),
 ('BrowserScreen.kt', '内置浏览器', '标签页、地址栏、DevTools'),
 ('ClaudeSettingsScreen.kt', 'Claude 设置', '账号、MCP 服务器、模型、用量'),
 ('ClaudeChatControls.kt', 'Claude 对话控件', '模式、权限、上下文、紧凑化'),
 ('ClaudeLoginDialog.kt', 'Claude 登录', '付费方式与验证码'),
 ('AgentApprovalPanel.kt', '智能体 → 批准面板', '允许 / 拒绝 / 始终允许'),
 ('AgentQuestionPanel.kt', '智能体 → 提问', '问卷与分页'),
 ('AgentsSetupUi.kt', '智能体 → 引导', '选择启用哪些智能体'),
 ('TerminalLauncherOnboardingDialog.kt', '终端引导', '首次使用选择访问方式'),
 ('ModelSelectorBottomSheet.kt', '模型选择', '模型列表与推理参数'),
 ('AppUpdateDialog.kt', '更新弹窗', '版本说明与下载'),
 ('MainScreen.kt', '主屏', '加载态与错误态'),
 ('FileManagerDialog.kt', '文件管理器', '浏览、复制、移动、重命名'),
 ('LocalTerminalDialog.kt', '本地终端', '会话标签、命令输出'),
 ('FloatingTerminalInspector.kt', '悬浮终端检查器', '状态胶囊与详情'),
 ('NetworkInspectorDialog.kt', '网络检查器', '请求/响应头、筛选'),
 ('CustomSystemPromptDialog.kt', '自定义系统提示词', '默认提示词展示与编辑'),
 ('ProjectPickerBottomSheet.kt', '项目选择', '项目列表'),
 ('SharedChatViewerModal.kt', '共享对话查看', '只读对话'),
 ('CodeBlock.kt', '代码块', '复制按钮'),
 ('AppPermissionsDialog.kt', '权限申请', '权限说明'),
 ('ConversationLoadingSkeleton.kt', '对话加载骨架', '占位提示'),
]

counts = collections.Counter()
getstring_sites = 0
for dirpath, _dirs, files in os.walk(SRC):
    for fn in files:
        if not fn.endswith(".kt"):
            continue
        src = open(os.path.join(dirpath, fn), encoding="utf-8").read()
        n = src.count("stringResource(")
        getstring_sites += src.count("getString(R.string.")
        if n:
            counts[fn] = n

out = ['# 汉化覆盖清单（核对用）\n',
       '数字为源码中 `stringResource` 调用点的实际数量（由 `scripts/i18n/gen_coverage.py` 统计）。\n',
       '## 一、按界面核对\n',
       '| 界面 | 覆盖内容 | 调用点 |', '| --- | --- | --- |']
seen = set()
for fn, name, desc in SCREEN:
    if counts.get(fn):
        out.append(f'| {name} | {desc} | {counts[fn]} |')
        seen.add(fn)

out += ['\n## 二、组件与其余已汉化文件\n', '| 文件 | 调用点 |', '| --- | --- |']
for f, n in sorted(((f, n) for f, n in counts.items() if f not in seen), key=lambda x: -x[1]):
    out.append(f'| `{f}` | {n} |')
out.append(f'\n合计：**{len(counts)} 个文件、{sum(counts.values())} 处 `stringResource` 调用点**'
           f'（另有 {getstring_sites} 处 `getString`，用于非 Composable 上下文：Toast、通知、数据层）。\n')

out += ['## 三、有意保留英文的内容\n',
        '| 内容 | 说明 |', '| --- | --- |',
        '| 产品名 / 模型名 | `AntiGem`、`Antigem`、`Termux`、`Antigravity`、`Claude`、`Gemini`、`Nano Banana` 等 |',
        '| VS Code 变量模板 | `${file}`、`${workspaceFolder}`、`${fileBasename}` 等 |',
        '| 示例路径与 URL | `/sdcard/Download/...`、`https://github.com/...` |',
        '| 动画 / 测试标识 | `shimmerTransition`、`drawerChatListFade`、`thought_chevron_rotation` 等 |',
        '| 日志与异常 | `Log.d/i/w/e` 内容保持英文，便于搜 issue、上报上游 |',
        '| 终端输出 / shell 命令 / 文件路径 / 代码块 | 翻译会造成误操作 |',
        '| 上游文档 | README、AGENTS.md、CLAUDE_CLI_PROTOCOL.md |',
        '',
        '## 四、尚未处理的部分\n',
        '| 项目 | 状态 |', '| --- | --- |',
        '| AI 回复内容本身 | ✅ 已实现（Gemini 系统指令 + Claude `initialize`，commit `8fdd016`；CLI 是否真应用需真机验证） |',
        '| `Toast.makeText` 的 104 处字面量 | ✅ 已改（`context.getString`，commit `6310f1c`，新增 88 条 `*_toast_*` 资源） |',
        '| `AppToastHelper.showToast` 与系统选择器标题 | ✅ 已改（36 处：28 处 showToast 字面量 + 8 处 `Intent.createChooser` 标题；`context.getString`，32 条 `*_apptoast_*` / `*_chooser_*` 资源） |',
        '| 含插值的 `contentDescription` / snackbar `message` | ✅ 已改（16 处，`apply_dyn_attrs.py`） |',
        '| `title = / label =` 字面量 | ✅ 已改 50 处（`apply_p2_titles.py`：遥测/文件详情/附件/语音/权限卡/Git/模型选择/设置首页/5 个脚本标题/数据层对话标题/浏览器标签页 + 2 处插值）；剩余 58 处全是动画 label（30）、终端按键（22）、产品名（4）与 2 处待模型改造 |',
        '| 设置首页卡片文案 | ✅ 已改（`title =` / `when` 分支，commit `5d55bc3`，135 处 / 107 条资源） |',
        '| 环境安装页（全屏 + 对话框） | ✅ 已改（状态标题、硬件取值、下载选项、按钮，同上提交） |',
        '| 设置顶栏 / 分组枚举 | ✅ 已改（`SettingsSection` 与 `ClaudeSettingsPage` 改 `@StringRes`，commit `4d25d0f`） |',
        '| 安装通知与进度文案（数据层） | ✅ 已改（`LocalEnvironmentManager` 77 处：通知标题/正文、`speedText`、安装页状态字段、通知渠道名与描述、错误文案；`appContext.getString`，55 条 `data_install_*` 资源） |',
        '| 其余 `when`/`if` 分支字面量 | ❌ 未改（实测 70 行 `when` 分支 + 243 行单行三元，需剔技术值白名单） |',
        '| 设置页语言切换入口 | ✅ 已实现（方案 A：appcompat 1.7.0；A-1 `ef5a244` + A-2 `1519dc6`；API<33 的通知与静态 `Locale.getDefault()` 仍待真机验证） |',
        '| 外观子页残留标签（Auto (System)/Light/Dark、字号、自动收起、悬浮切换器条目） | ✅ 已改（P3.9，13 个标签；方向 `VERTICAL`/`HORIZONTAL` 的技术值保留英文） |',
        '| `subtitle =` / `badgeText =` / `description =` / `value =` 字面量 | ✅ 已改（P2 收尾批：21 处 UI 文案 + 4 处数据层可见状态 + 2 处通知渠道描述；`value =` 里技术值保留；见 HANDOFF 坑 19） |',
        '| 审批面板 `ApprovalItem` 整族 | ✅ 已改（`denyLabel`/`denyPlaceholder` 改 `@StringRes`，两个生产者加 `Context`，标题/选择项/建议范围/权限模式标签共 30 余条资源化） |',
        '| `ConversationShareHelper` 的 `Shared Conversation` | ✅ 已改（`parseSharedConversation` 增加 `Context` 参数） |',
        '| 数据层错误族（`AgyMcpManager` 18 / `ClaudeAccountManager` 15） | ❌ 未改（两个类没有 Context，需注入 Application，建议整批处理） |',
        '| `ClaudeStatusViews` 的 7 个 `actionLabel` | ❌ 未改（Sign in / Retry / Restart server 等） |',
        '| 浏览器扩展文案 | ✅ 已改（核实结论：6 个 JS 文件无浮层 UI、界面可见文案为 0，故一行 JS 未改；实际改的是设置页安装/更新对话框与操作失败兜底 6 条、`BrowserExtensionManager` 可显示错误 25 条、内置扩展展示名/描述 2 条；见 HANDOFF P4 与坑 20） |',
        '',
        '## 五、怎么核对\n',
        '1. 装 `antiGem-termux-*.apk`，手机系统语言设为简体中文',
        '2. 按上表逐项打开界面，看是否还有英文',
        '3. 「设置」文案最密集，先看它',
        '4. 发现遗漏就把界面名 + 那行英文发我，可据此定位到文件与行号']

open(OUT, "w", encoding="utf-8").write("\n".join(out) + "\n")
print(f"COVERAGE.md: {len(counts)} files, {sum(counts.values())} call sites")
