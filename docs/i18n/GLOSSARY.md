# 汉化术语表（zh-CN）

本文件是本次简体中文汉化的唯一译法基准。新增或修改界面文案时，**必须先查本表**；表里没有的词，先补进本表再用。

## 1. 固定译法

| 原文 | 中文 | 说明 |
| --- | --- | --- |
| Agent | 智能体 | 不译"代理"（会与 HTTP proxy 混淆） |
| Subagent | 子智能体 | |
| Session | 会话 | |
| Workspace | 工作区 | |
| Extension | 扩展 | 不译"插件"，与 Android/Chrome 文档保持一致 |
| Bridge | 桥接 | 名词修饰时用"桥接服务" |
| Skill | 技能 | |
| Trajectory | 执行轨迹 | |
| Prompt | 提示词 | |
| System Prompt | 系统提示词 | |
| Model | 模型 | |
| Context | 上下文 | |
| Hub | 中枢 | |
| Backend | 后端 | |
| Token | Token | 技术名词不译 |
| Provider | 提供方 | |
| Sandbox | 沙箱 | |
| Permission | 权限 | |
| Approval | 批准 | |
| Snapshot | 快照 | |
| Branch | 分支 | |
| Commit | 提交 | |
| Repository | 仓库 | |
| Terminal | 终端 | |
| Settings | 设置 | |
| Preferences | 偏好设置 | |

## 2. 不翻白名单（保持英文原文）

1. **产品名 / 应用名**：`AntiGem`、`AntiTerm`、`Termux`、`Antigravity`、`Google Flow`、`Gemini`、`Claude`
2. **系统标识**：桌面应用名 `app_name`、无障碍服务名 `accessibility_service_name`
3. **模型名**：`gemini-*`、`claude-*`、`Nano Banana*`、`Veo *`、`Omni *` 等
4. **技术字面量**：包名、类名、函数名、变量名、shell 命令、文件路径、URL、JSON key、正则
5. **日志与异常**：`Log.d/i/w/e` 里的文案、`Exception` / `Error` 的 message —— 保持英文，便于搜索 issue 与上报上游
6. **上游文档**：`README.md`、`AGENTS.md`、`CLAUDE_CLI_PROTOCOL.md`、`docs/` 下英文文档（给人或给 AI 读的指令文件不动）
7. **代码注释**：保持原有语言，不做翻译

## 3. 文案风格

- 用简体中文，全角标点
- 按钮/菜单用短词（≤6 字），不加句号
- 提示句用完整句，句末加句号
- 中英文之间留一个半角空格（如 `开启 Bridge 服务`）
- 阿拉伯数字与中文之间不留空格（如 `3 个会话` 保留空格，`第3步` 不留）——统一按"数字与中文之间留空格"处理，与项目现状无关，便于统一
- 不用"您"，统一用"你"

## 4. 占位符约定

- 插值改写为 Android 格式占位符：字符串用 `%1$s`、整数用 `%1$d`
- 一条文案含多个变量时必须带位置序号（`%1$s` / `%2$s`），禁止裸 `%s`
- 中文语序与英文不同时，按中文语序重排并调整序号
- 不引入 `plurals`（中文无单复数变化）

## 5. 收集到的资产改法

- Composable 内：`Text(stringResource(R.string.xxx))`
- 带参数：`stringResource(R.string.xxx, count)`
- 非 Composable（Toast / Activity / ViewModel 回调）：`context.getString(R.string.xxx)`
- 资源文件名按模块拆分：`strings_settings.xml`、`strings_ide.xml`、`strings_claude.xml`、`strings_browser.xml`，通用项留在 `strings.xml`
- 每条译文上方保留 `<!-- 原文: ... -->` 注释，便于上游变更后定位过期译文

## 6. AI 回复语言

- 语言偏好跟随 App 语言：中文界面 → 要求 AI 用简体中文；英文界面 → 不注入任何语言指令
- 注入措辞（唯一版本，勿改写）：
  > Always write your explanatory prose in Simplified Chinese. Keep all code, identifiers, commands, file paths, and commit messages in their original form; do not translate technical terms that have no well-established Chinese equivalent.
- 注入位置：
  - Gemini 直连：`data/remote/AntigravityApiService.kt` 的 `SYSTEM_INSTRUCTION` 常量末尾
  - Claude：会话建立后发送 `initialize` 控制请求，`appendSystemPrompt` 携带上述文案
