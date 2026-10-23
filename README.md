# <div align="center">⚡ AntiGem</div>

<div align="center">
  <strong>面向 Android 的下一代智能体编程工作区、原生 Termux 终端与语言服务器中枢</strong>
</div>

<br/>

<div align="center">

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-Jetpack%20Compose-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/jetpack/compose)
[![gRPC](https://img.shields.io/badge/gRPC-Square%20Wire-244F7D?style=for-the-badge&logo=grpc&logoColor=white)](https://square.github.io/wire/)
[![Go](https://img.shields.io/badge/Bridge-Go%201.23+-00ADD8?style=for-the-badge&logo=go&logoColor=white)](https://golang.org)
[![Termux](https://img.shields.io/badge/Termux-Native%20Subsystem-000000?style=for-the-badge&logo=gnubash&logoColor=white)](https://termux.dev)

</div>

<div align="center">

**简体中文** | [English](README.en.md)

</div>

---

## 🌟 概述

**AntiGem** 把你的 Android 设备变成一套完整的智能体 AI 开发环境。它把现代 **Jetpack Compose** 界面、原生 **Termux Linux 子系统**、内置 **Go IDE 桥接**，以及两个编程智能体组合在一起：

- **Antigravity（`agy`）** —— 通过**二进制 gRPC 连接**直接对接 `agy` 语言服务器守护进程。
- **Claude Code** —— Anthropic 官方 `claude` CLI，运行在应用的 Linux 环境内。

首次启动时选择一个或两个都用，之后可随时在**设置 → 智能体**中切换。被你关掉的智能体完全不会运行。

与抓取 CLI 输出的传统 AI 封装不同，AntiGem 通过各自的结构化协议与智能体通信：`agy` 走 **gRPC hub 模式**，Claude Code 走 **stream-json** 协议（每个会话一个长驻 CLI 进程）。由此获得完整的双向交互能力——实时 token 流式输出、多步子智能体执行、实时问答提示、工具执行确认，以及即时文件同步。

---

## 📦 应用变体：该下载哪个？

AntiGem 提供两个构建变体：

| 变体 | 包名（`applicationId`） | 是否推荐 | 工作方式 |
| :--- | :--- | :---: | :--- |
| **`termux`** | `com.termux` | **✅ 推荐** | **一体化独立环境。**你**不需要**安装 Termux。应用在自己的私有沙箱里原生运行完整的 Linux 环境、包管理器（`apt`/`pkg`）、shell 与各类二进制。 |
| **`standard`** | `com.antigem` | 可选 | 需要手机已安装独立的 **Termux** 应用。你在 Termux 里运行 `agy_ide_bridge`，再让 AntiGem 通过 localhost 连上它。*（注意：目前未使用 PRoot）* |

> [!TIP]
> **下载提示：**最新版本我们主要构建并发布 **`termux`** 变体（`antiGem-termux-v*-release.apk`）。如果你确实想用 `standard` 变体，可以查找较旧的发布版，或者直接 fork 本仓库，用 Gradle 或 GitHub Actions 自行构建。

---

## 🏗️ 工作原理

```
┌─────────────────────────────────────────────────────────┐
│              📱 AntiGem Android 应用                    │
│        （Jetpack Compose 界面 + 原生终端）              │
└────────────────────────┬────────────────────────────────┘
                         │  二进制 Protobuf / gRPC 与 WebSocket
┌────────────────────────▼────────────────────────────────┐
│               🐹 内置 Go IDE 桥接                       │
│                  (agy_ide_bridge)                       │
└──────────────┬────────────────────────┬─────────────────┘
               │                        │
      gRPC Hub 直连             stream-json（NDJSON 中继）
               │                        │
┌──────────────▼──────────┐  ┌──────────▼─────────────────┐
│  ⚡ Antigravity (agy)   │  │  ✻ Claude Code CLI         │
│  语言服务器守护进程     │  │  每个会话一个进程          │
│  模型与 MCP 智能体      │  │  模型、工具与 MCP          │
└─────────────────────────┘  └────────────────────────────┘
```

- **类型安全的 gRPC 协议**：通过 Square Wire 直接使用 `agy` 的 protobuf schema 通信，而不是抓取命令行文本。
- **原生 Claude Code 协议**：桥接以 stream-json 模式运行官方 `claude` CLI，并通过 WebSocket 转给应用，因此模型、effort、思考与权限模式可以实时切换，无需重启会话。
- **深度智能体交互**：在聊天界面内原生支持复杂的智能体工作流、交互式选择题与权限确认。
- **原生 Android PTY 引擎**：与后台执行服务并存，运行完整 PTY 终端，支撑长时间编译、服务与脚本。

---

## 📸 应用截图

<div align="center">

### 💬 智能体对话与工具执行
<img src="docs/resources/chat_screen.jpg" alt="智能体对话与工具执行" width="90%" style="border-radius: 12px; margin-bottom: 24px;" />

---

### 💻 完整 IDE 代码编辑器
<img src="docs/resources/ide_code_editor.jpg" alt="IDE 代码编辑器" width="90%" style="border-radius: 12px; margin-bottom: 24px;" />

---

### 🐧 原生 Linux 终端
<img src="docs/resources/linux_terminal.jpg" alt="原生 Linux 终端" width="90%" style="border-radius: 12px; margin-bottom: 24px;" />

---

### 📂 侧栏与工作区历史
<img src="docs/resources/sidebar_navigation.jpg" alt="侧栏导航" width="90%" style="border-radius: 12px; margin-bottom: 24px;" />

---

### ⚡ 额度与账号信息
<img src="docs/resources/account_profile_quota.jpg" alt="账号资料与额度" width="90%" style="border-radius: 12px; margin-bottom: 24px;" />

---

### ⚙️ 设置、MCP 与备份
<img src="docs/resources/settings_preferences.jpg" alt="设置与备份" width="90%" style="border-radius: 12px; margin-bottom: 24px;" />

---

### 🌐 内置浏览器与自动化
<img src="docs/resources/integrated_browser.jpg" alt="内置浏览器" width="90%" style="border-radius: 12px; margin-bottom: 24px;" />

</div>

---

## ✨ 主要功能

### 🤖 1. 自主智能体
- **Wire gRPC hub 直连**：使用 Square Wire 与 `agy` 进行类型安全、低延迟的通信。
- **实时流式推理**：实时 token 流、思考折叠/展开、工具执行卡片与子智能体跟踪。
- **模型切换与额度**：在 Gemini 3.7 Flash、Pro、Ultra、Claude 3.5 Sonnet、GPT-4o 与自定义模型之间切换，并实时显示提示额度。
- **交互式提示**：支持 AI 智能体生成的交互式确认弹窗与选择题。

### ✻ 2. Claude Code 智能体
- **官方 Claude Code**：在应用内运行 Anthropic 的 `claude` CLI——可在**设置 → Claude Code**中安装或更新它，并用你的 Claude 订阅或 Anthropic Console 账号登录。
- **完整聊天体验**：带思考摘要的流式输出、带审批（仅此次 / 始终允许）的工具卡片、计划审批、Claude 的提问、子智能体、图片与文件附件，以及语音听写。
- **实时聊天控制**：会话进行中即可切换模型、effort 档位、思考与权限模式（手动、接受编辑、计划、自动、不再询问）；应用会把每次改动与 CLI 实际运行的状态做核对。
- **用量一目了然**：提示缓存计时器、上下文窗口占用、5 小时与每周套餐限额，以及每条回复的 token 数与作答模型。
- **会话管理**：斜杠命令（含 `/compact`）、就地编辑或重新生成消息、分叉、重命名与删除会话——全部保存在 Claude Code 自己的历史中。
- **设置**：会话默认值、权限规则与命令沙箱、MCP 服务器（含应用内浏览器与终端自动化服务器）、插件与插件市场、`CLAUDE.md` 指令与 Claude 保存的记忆。

### 💻 3. 原生 Linux 终端子系统
- **高性能 PTY**：快速终端模拟器，支持自定义字号、主题与交互式快捷键。
- **完整 Linux 生态**：在 `$HOME` 内预置 bash、coreutils、Python、Node.js、Git、Go 与 Glibc 工具链。
- **实时终端 MCP**：让 AI 智能体运行命令、跑测试、构建项目，并以持续前台执行的方式流式回传输出。

### 📝 4. 移动端 IDE 代码编辑器
- **语法高亮**：快速编辑器，支持 Python、Go、Kotlin、Java、JS/TS、Rust、C/C++、HTML/CSS、JSON、YAML 与 Markdown。
- **效率操作**：多标签切换、行号、查找/替换、撤销/重做、自动缩进与文件浏览器。

### 🌐 5. 内置浏览器与自动化 MCP
- **内置浏览器**：应用内网页浏览器，可预览本地开发服务器、测试 Web 应用。
- **浏览器自动化**：让 AI 智能体检查实时页面、与 DOM 元素交互、测试 UI 流程并验证前端组件。

### 🎨 6. 动态聊天产物与图表
- **交互式 Markdown 与 diff**：可展开的 diff 块、步骤轮播与可复制的代码块。
- **Mermaid 图表**：为流程图、架构图与状态机提供硬件加速的原生渲染。
- **浮窗聊天（PIP）**：用画中画浮层在 Android 上多任务并行。

### 🛡️ 7. 完整备份套件与应用内更新
- **rootfs 备份**：一键把整套已安装的 Linux rootfs、dotfiles 与二进制备份到手机存储。
- **会话与凭据备份**：导出会话数据库、brain 转录与登录凭据。
- **一键更新**：应用内检查版本并直接更新 APK。

---

## 🛠️ 从源码构建

### 前置要求
- **JDK 17+**
- **Android SDK**（API Level 35，Build Tools 35.0.0+）
- **Go 1.23+**（用于编译 `agy_ide_bridge`）
- **Gradle 8.11+**

### 构建命令
```bash
# 克隆仓库
git clone https://github.com/743553649/agy-and-claude.git
cd agy-and-claude

# 构建两个变体（Debug 与 Release）
./scripts/build_all_apks.sh

# 或直接用 Gradle 构建推荐的 Termux 变体：
./gradlew assembleTermuxDebug
./gradlew assembleTermuxRelease
```

编译出的 APK 位于：
- `build/outputs/apk_all/`
- `app/build/outputs/apk/<flavor>/<buildType>/`

---

## 📂 项目结构

```text
agy-and-claude/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/gemini/
│   │   │   ├── data/
│   │   │   │   ├── agent/        # 智能体后端：Antigravity (agy/) 与 Claude Code (claude/)
│   │   │   │   ├── local/        # 终端、存储与环境管理器
│   │   │   │   ├── receiver/     # Termux Intent 与 API 广播接收器
│   │   │   │   ├── remote/       # Wire gRPC 服务与 WebSocket 客户端
│   │   │   │   ├── service/      # TermuxService（前台执行）
│   │   │   │   └── updater/      # 应用内自动更新
│   │   │   ├── ui/               # Jetpack Compose 页面、弹窗与浮窗聊天
│   │   │   └── theme/            # 设计系统与主题引擎
│   │   └── res/                  # Android XML 资源与 FileProvider 路径
├── agy_ide_bridge/               # 内置 Go 桥接（连接 Android、AGY 守护进程与 Claude Code）
├── proto_generator/              # Square Wire Protobuf 定义与生成器
├── scripts/                      # 构建与备份脚本
└── version.json                  # 统一的版本元数据与发布清单
```

---

## 🤝 参与贡献

欢迎贡献代码、提出功能建议或提交 PR！
1. Fork 本仓库
2. 创建分支（`git checkout -b feature/amazing-feature`）
3. 提交改动（`git commit -m 'feat: Add amazing feature'`）
4. 推送到分支（`git push origin feature/amazing-feature`）
5. 发起 Pull Request

---

## 📄 许可证

AntiGem 是基于 **Apache License 2.0** 许可的开源软件。
