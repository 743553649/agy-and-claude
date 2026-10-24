# 项目文档 — AntiGem

> 生成时间：2026-10-23T15:12:49Z ｜ 模式：**FULL**（首次扫描，仓库内此前没有 project-doc.md）
> 扫描基线：`git HEAD = d1e2a16`（分支 `master`，共 553 次提交）
> 输出路径：`docs/project-doc.md`（按本次指令覆盖 SKILL.md 默认的 `.claude/pipeline/` 路径）
> 工具链：scan 技能 Step 1 的 `understand-anything` / `context-mode` 在本环境不可用，全文走 **native fallback**（`find` / `grep` / `git` / `python3`）

**读数规则**：本文每个数字都标注了来源命令（见 §0）。凡是本次没有真实执行、或执行了但无法得出确定值的字段，一律写「未测量」，不做估算。

---

## 0. 数据来源与测量方法

以下命令均在项目根 `/sdcard/汉化/agy-and-claude` 执行，输出为本次实测值。

| 指标 | 命令 | 实测值 |
| --- | --- | --- |
| 文件总数（排除 `.git`） | `find . -path ./.git -prune -o -type f -print \| wc -l` | 437 |
| `app/src/main/java` Kotlin 文件数 | `find app/src/main/java -name '*.kt' \| wc -l` | 194 |
| `app/src/main/java` Kotlin 行数 | `find app/src/main/java -name '*.kt' -exec cat {} + \| wc -l` | 92855 |
| `app/src/main/java` Java 文件数 / 行数 | 同上，`-name '*.java'` | 8 / 3209 |
| Kotlin 测试文件（`test` + `androidTest`） | `find app/src/test app/src/androidTest -name '*.kt' \| wc -l` | 4 |
| Kotlin `@Test` 数量 | `grep -rhoE '@Test' app/src/test app/src/androidTest \| wc -l` | 13 |
| mjs 测试用例数 | `grep -rhoE '^\s*(test\|it)\(' app/src/test/browserextensions/*.mjs \| wc -l` | 23 |
| Go 测试文件 | `find . -name '*_test.go' -not -path './.git/*' \| wc -l` | 0 |
| Go 桥接规模 | `find agy_ide_bridge -name '*.go' -exec cat {} + \| wc -l` | 24 文件 / 9347 行 |
| Go daemon 规模 | `wc -l termux-daemon/server.go` | 788 行 |
| 浏览器扩展 JS 规模 | `find extensions -name '*.js' -exec cat {} + \| wc -l` | 6 文件 / 2618 行 |
| Proto 规模 | `wc -l app/src/main/proto/LanguageServerService.proto` | 10189 行（268839 字节） |
| Android 资源 XML 数 | `find app/src/main/res -name '*.xml' \| wc -l` | 43 |
| i18n 基线 key 数 | 见 §7.4 脚本 | 2061 |
| i18n 中文 key 数 | 同上 | 2055（缺 6，**99.7%**） |
| 分层规模 | 见下表 | ui 102/61139、data 72/27942、domain 9/2912、theme 3/207 |

分层规模的分层口径（仅 `app/src/main/java/com/example/gemini/<层>`，不含测试）：

```bash
for d in ui data domain theme; do
  echo "$d: $(find app/src/main/java/com/example/gemini/$d -name '*.kt' | wc -l) 个 .kt, \
$(find app/src/main/java/com/example/gemini/$d -name '*.kt' -exec cat {} + | wc -l) 行"
done
# 输出：ui: 102 个 .kt, 61139 行 / data: 72 个 .kt, 27942 行
#       domain: 9 个 .kt, 2912 行 / theme: 3 个 .kt, 207 行
```

> 注：`194 + 4(测试) = 198`，与 `find app -name '*.kt' | wc -l` 的 198 一致，两个口径互相印证。

---

## 1. 技术栈

| 维度 | 实测值 | 来源 |
| --- | --- | --- |
| 形态 | Android 应用（单 Gradle 模块 `:app`）+ 内置 Go 边车进程 | `settings.gradle.kts`（`include(":app")`）、`app/build.gradle.kts` 的 `buildAgyIdeBridge` |
| 语言 | Kotlin 为主（194 个源文件 / 92855 行），Java 少量（8 / 3209），Go（桥接 + daemon），Python（工具脚本），JavaScript（浏览器扩展） | §0 计数 |
| 构建系统 | Gradle **9.1.0**（wrapper） | `gradle/wrapper/gradle-wrapper.properties` → `gradle-9.1.0-bin.zip` |
| Android Gradle Plugin | **9.0.1** | `gradle/libs.versions.toml` → `androidGradlePlugin` |
| Kotlin | **2.3.20**（Compose 编译器与 serialization 插件同版本） | `libs.versions.toml` → `kotlin` |
| SDK | `compileSdk = 36`、`minSdk = 24`、`targetSdk = 28` | `app/build.gradle.kts` `android {}` |
| JVM | `sourceCompatibility/targetCompatibility = 17`，Kotlin `jvmToolchain(21)` | `app/build.gradle.kts` |
| UI | Jetpack Compose（BOM **2026.03.01**）+ Material3 | `libs.versions.toml` → `androidxComposeBom`、`androidx-compose-material3` |
| 导航 | androidx **navigation3** 1.0.1 + `kotlinx.serialization` 的 `NavKey` | `NavigationKeys.kt`（`@Serializable data object Main : NavKey`）、`libs.versions.toml` → `nav3Core` |
| 网络 | OkHttp **4.12.0** + okhttp-sse（**无 Retrofit**，`grep import retrofit2` 结果为空） | `libs.versions.toml`、grep |
| RPC | Square **Wire 7.0.4**，gRPC 客户端角色、`suspending` 调用风格；proto 包名 `exa.language_server_pb` | `app/build.gradle.kts` `wire {}`、`LanguageServerService.proto` 首行 `package` |
| 持久化 | AndroidX **DataStore Preferences 1.1.1**；**Kotlin 侧无 Room / 无 SQLite**（grep `androidx.room\|RoomDatabase\|SQLiteOpenHelper` 为空） | `libs.versions.toml`、grep |
| DI | **无**（grep `hilt\|dagger\|koin` 为空），用 `object` 单例 + `by lazy` 手工装配 | grep |
| 并发 | kotlinx-coroutines 1.10.2 + `StateFlow`（源码中出现 588 次） | `libs.versions.toml`、grep |
| 序列化 | kotlinx-serialization-json 1.7.3 | `libs.versions.toml` |
| 应用 ID | `standard` → `com.antigem`；`termux` → `com.termux` | `app/build.gradle.kts` `productFlavors` |

**命名不一致（实测，易踩坑）**：`rootProject.name = "Gemini"`、Kotlin 包名 `com.example.gemini`、归档名 `archivesName = "antiGem"`、`applicationId` 却是 `com.antigem` / `com.termux`，而仓库名是 `agy-and-claude`。四处不同名，改包名/引用时不能想当然。

---

## 2. 依赖

版本号全部来自 `gradle/libs.versions.toml` 与 `app/build.gradle.kts` 的 `dependencies {}`。

### 核心运行时（core）

| 依赖 | 版本 | 用途（按代码引用点判断） |
| --- | --- | --- |
| androidx.core:core-ktx | 1.18.0 | 基础 |
| androidx.appcompat:appcompat | 1.7.0 | `MainActivity : AppCompatActivity` |
| androidx.activity:activity-compose | 1.13.0 | `setContent` |
| androidx.lifecycle:{runtime-ktx, runtime-compose, viewmodel-compose} | 2.10.0 | ViewModel / 生命周期收集 |
| androidx.compose BOM | 2026.03.01 | 统一 Compose 版本 |
| androidx.compose.material3 / material-icons-extended | 随 BOM | 界面 |
| androidx.navigation3:{runtime, ui} + lifecycle-viewmodel-navigation3 | 1.0.1 / 2.10.0 | 导航 |
| androidx.datastore:datastore-preferences | 1.1.1 | 设置项持久化 |
| com.squareup.okhttp3:{okhttp, okhttp-sse} | 4.12.0 | HTTP / SSE / WebSocket（`ClaudeBridgeClient`） |
| org.jetbrains.kotlinx:kotlinx-serialization-json | 1.7.3 | JSON 协议 |
| com.squareup.wire:{wire-runtime, wire-grpc-client} | 7.0.4 | agy gRPC |
| io.coil-kt:{coil-compose, coil-svg} | 2.7.0 | 图片（含 SVG） |
| io.noties.markwon:ext-latex + ru.noties:jlatexmath-android | 4.6.2 / 0.2.0 | Markdown / LaTeX 原生渲染 |
| com.github.mwiede:jsch | 0.2.20 | SSH 连接池（`TermuxSshManager`） |
| org.jsoup:jsoup | 1.18.3 | 网页抓取（`WebSearchManager`） |
| io.github.Rosemoe.sora-editor:{editor, language-textmate, language-java} | 0.23.6 | IDE 代码编辑器 |
| org.apache.commons:commons-compress + org.tukaani:xz | 1.26.1 / 1.9 | tar/xz/deb 解包 |
| com.github.termux.termux-app:terminal-emulator | v0.118.1 | 终端 PTY |
| androidx.swiperefreshlayout | 1.1.0 | 下拉刷新 |

### 开发 / 构建（dev）

| 依赖 | 版本 | 说明 |
| --- | --- | --- |
| com.android.application | 9.0.1 | AGP |
| org.jetbrains.kotlin.plugin.compose / .serialization | 2.3.20 | 编译器插件 |
| com.squareup.wire | 7.0.4 | proto 代码生成 |
| org.gradle.toolchains.foojay-resolver-convention | 1.0.0 | `settings.gradle.kts` 中的 toolchain 解析 |
| Go（CI） | `go-version: '1.22'` | `.github/workflows/ci.yml` |
| Go（`go.mod` 声明） | `go 1.26.5` | 与 CI 的 1.22 **不一致**，见 §12 |

### 测试（testing）

| 依赖 | 版本 |
| --- | --- |
| junit:junit | 4.13.2 |
| org.jetbrains.kotlinx:kotlinx-coroutines-test | 1.10.2 |
| androidx.test:{core, runner} | 1.7.0 |
| androidx.test.ext:junit | 1.3.0 |
| androidx.test.espresso:espresso-core | 3.7.0 |
| androidx.compose.ui:ui-test-junit4 / ui-test-manifest | 随 BOM |

### Go 侧依赖（`agy_ide_bridge/go.mod`）

`module gemini-server`，全部列在 `require` 中且标注 `// indirect`：`gorilla/websocket 1.5.3`、`modernc.org/sqlite 1.60.1`、`modernc.org/libc 1.77.1`、`golang.org/x/sys 0.48.0`、`google/uuid 1.6.0`、`dustin/go-humanize 1.0.1`、`mattn/go-isatty 0.0.24`、`ncruces/go-strftime 1.0.0`、`remyoudompheng/bigfft`、`modernc.org/mathutil`、`modernc.org/memory`。

> `go.mod` 里 `go 1.26.5` 是**最低要求**（不是建议值）；CI 的 `go-version: '1.22'` 只是引导版本，实际编译由 Go 自动切换到 go1.26.5 完成 —— 机制与证据见 §12。

---

## 3. 架构模式

**单模块分层 + 双运行时边车（sidecar）**。不是 Clean Architecture 的完整三层数据流（没有 repository 接口层），而是「Compose UI → ViewModel → Agent 后端 → 传输层 → Go 桥接进程 → 外部 CLI/守护进程」。

```
Compose UI (ui/**)
   │  StateFlow / 事件回调
   ▼
ChatViewModel (ui/chat)  ── 持有 ──▶ ChatSessionStore（内存态单一事实源）
   │
   ▼
AgentChatBackend（interface，按 AgentKind 二分）
   ├── AgyChatBackend   (2162 行) ──▶ data/remote/services/* ──Wire gRPC──▶ agy LS hub
   └── ClaudeChatBackend(1309 行) ──▶ ClaudeBridgeClient ──WebSocket/HTTP──▶ 桥接
                                                                              │
                                                          agy_ide_bridge (Go, 独立进程)
                                                          ├─ pkg/hub     → agy gRPC hub 监管
                                                          ├─ pkg/claude  → claude CLI stream-json
                                                          ├─ pkg/ws      → 对 App 的 WebSocket
                                                          ├─ pkg/git     → Git 服务
                                                          └─ pkg/security→ 命令/路径白名单
```

关键实测依据：

- **Agent 抽象**：`domain/model/AgentKind.kt` 定义 `enum class AgentKind { AGY, CLAUDE }`，`data/agent/AgentChatBackend` 为共同接口，`AgyChatBackend` / `ClaudeChatBackend` 分别实现。
- **UI 状态**：`ChatSessionStore` 暴露 `MutableStateFlow`（`conversations` / `currentConversation` / `messages` / `conversationError` / `userEmail` / `attachments`），整棵树 `StateFlow` 出现 588 次。
- **无 Application 子类**：`grep ': Application()'` 与宽匹配均无结果，`AndroidManifest.xml` 的 `<application>` 也没有 `android:name`。因此语言回设只能放在每个入口 Activity（`MainActivity.kt` 注释原文：「manifest 不能加自定义 Application，所以放在各入口 Activity」）。
- **双 flavor 端口隔离**（`app/build.gradle.kts`，注释说明是为了两个包可并存）：`standard` = BRIDGE 1236 / HUB 1237 / MCP 8767；`termux` = 1234 / 1235 / 8765。端口经 `BuildConfig` 注入，`AuthPreferences` 用 `http://127.0.0.1:${BuildConfig.HUB_PORT}` 组默认地址。

---

## 4. 目录结构

| 路径 | 文件数 | 作用（实测） |
| --- | --- | --- |
| `app/` | 272 | Android 模块全部内容 |
| `app/src/main/java/com/example/gemini/` | 190 `.kt` | 应用主体 |
| ├ `ui/` | 102 `.kt` / 61139 行 | Compose 界面；`components/` 46 个文件（最大子目录） |
| ├ `data/` | 72 / 27942 | 后端、传输、本地环境、偏好 |
| ├ `domain/` | 9 / 2912 | 模型与协议无关类型（`model/`、`chat/`、`i18n/`、`context/`） |
| └ `theme/` | 3 / 207 | `GeminiTheme` 等 |
| `app/src/main/java/com/termux/app/` | 4 `.kt` + Java | 从 termux-app fork 的终端/服务类（`TermuxActivity`、`TermuxService` 等） |
| `app/src/main/proto/` | 1 | `LanguageServerService.proto`（10189 行） |
| `app/src/main/assets/` | — | `bin/`（脚本资产 32K）、`browser-runtime/`（96K，5 个 `.mjs`）、`glibc-min-arm64.tar`（3.9M） |
| `app/src/main/res/` | 43 XML | `values/` 14 文件、`values-zh-rCN/` 12 文件 |
| `agy_ide_bridge/` | 26 | Go IDE 桥接（`main.go` + `pkg/{androiddns,claude,config,git,handlers,hub,models,scanner,security,transcript,ws}`） |
| `extensions/` | 8 | 内置 WebView 扩展 `google-flow`（manifest + 6 个 js） |
| `extension_docs/` | 11 | 扩展开发文档 + MCP 示例，构建时打进 assets（`bundleExtensionDocs`） |
| `proto_generator/` | 19 | 逆向 proto 的 Python 工具 + 已抽取的 3 个 `.proto` |
| `scripts/` | 61 | 备份/恢复 shell、`build_all_apks.sh`、`verify_apk_signature.py`、`i18n/`（`verify.py` + `archive/`） |
| `termux-daemon/` | 1 | `server.go`（788 行）本地 HTTP 文件/项目服务 |
| `docs/` | — | 项目官网静态页（`index.html`/`main.js`/`style.css`/`resources/`）+ `i18n/archive/` 汉化档案 + 本文件 |
| `.github/workflows/` | 2 | `ci.yml`（每次 push 构建 termux APK）、`release.yml`（`version.json` 变更时发版） |
| 根文件 | — | `version.json`（1.0.13 / code 113）、`CLAUDE_CLI_PROTOCOL.md`（45K）、`google-flow-mcp-spec.md`（54K）、`README.md` / `README.en.md` |

---

## 5. 代码风格约定（从实际代码归纳，非臆测）

- **缩进 4 空格**：抽样 `MainActivity.kt` 有 465 行以 4 空格起始。Kotlin 官方风格（`kotlin.code.style=official`，见 `gradle.properties`）。
- **文件命名 = PascalCase 单类名**，后缀表意：`*Screen.kt` 7 个、`*ViewModel.kt` 2 个、`*Manager*.kt` 23 个。
- **共享状态用 `object` 单例**：46 个文件含顶层 `object`；惰性实例常见 `companion object { val instance by lazy { X() } }`（`AgyCsrfManager`）。
- **日志**：统一 `android.util.Log`，41 处 `private const val TAG`，源码中 529 次 `Log.x(`。TAG 用类名，消息前缀常带 emoji 与方括号，如 `Log.d(TAG, "⚡ [AgyCsrfManager] CSRF token updated (${newToken.take(8)}...)")`。
- **import 顺序**：`android.*` → `androidx.*` → `kotlinx.*` → 第三方 → `com.example.gemini.*` → `java.*`（`MainActivity.kt`、`AgyChatBackend.kt` 一致）；偶有未排序（`AgyChatBackend.kt` 尾部 `android.util.Log`、`org.json.JSONObject` 落在 `java.util.UUID` 之后）。
- **注释双语并存**：KDoc 用英文（`AgyChatBackend` 类注释）、行内注释用中文（`MainActivity.kt`）。不要强行统一。
- **`data class` 占比高**：384 处，DTO/模型层几乎全是 data class。
- **协程调度**：`Dispatchers.IO` 212 次、`Dispatchers.Main` 89 次 + `Dispatchers.Default` 5 次；阻塞 IO 切 IO 是本项目实际习惯。
- **字符串全部走 `R.string.*`**（见 §7.4），不在 Compose 里硬写中文。

---

## 6. 模块化实践

- **分层边界**：`ui/` 只放 Compose 与 ViewModel；跨界面的协议/模型放 `domain/model/`；网络、进程、文件、偏好全部在 `data/`。`ui/` 102 个文件说明**没有**按 feature 再拆 Gradle 模块，仍是单模块 + 包分组。
- **共享 UI 组件集中在 `ui/components/`（46 个）**，命名按用途（`*Dialog`、`*Helper`、`*BottomSheet`、`*Overlay`）。
- **每个 Agent 一个后端包**：`data/agent/agy/`（`AgyChatBackend`、`AgyMcpManager`、`AgyPluginsManager`、`AgySecuritySettingsManager`）与 `data/agent/claude/`（`ClaudeChatBackend`、`ClaudeProtocol`、`ClaudeTranscript`、`ClaudeConfigManager`、`ClaudeAccountManager`）。新增第三个 Agent 应对齐这个包形状。
- **服务层按 agy 服务拆分**：`data/remote/services/` 下 9 个文件对应 proto 里的不同 service（`AgyLanguageService`、`AgyChatService`、`AgyAuthService`、`AgyModelService`、`AgyProjectService`、`AgyMcpService`、`AgyPluginService`、`AgyConversationService`、`AgyAudioService`）。
- **Go 侧按职责分包**：`pkg/hub`（hub 监管）、`pkg/claude`（10 个文件，CLI 生命周期/附件/回滚/转录/语音）、`pkg/handlers` + `pkg/models`（HTTP 层）、`pkg/security`（校验）、`pkg/ws`（推送）。
- **资产双路径**：`scripts/*.sh` 与 `extension_docs/` 不进仓库内 assets，构建期由 `copyScriptsToAssets` / `bundleExtensionDocs` 复制到 `build/generated/` 再并入 assets —— 改脚本要改 `scripts/`，不要直接改 `app/src/main/assets/`。

---

## 7. 数据架构

### 7.1 无数据库层（Kotlin 侧）

`app/build.gradle.kts` 与源码中都没有 `androidx.room` / `RoomDatabase` / `SQLiteOpenHelper`。会话与消息是**内存态 + StateFlow**，由 `ChatSessionStore` 持有：

```kotlin
class ChatSessionStore {
    val conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val currentConversation = MutableStateFlow<Conversation?>(null)
    val messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    ...
}
```

### 7.2 设置与凭据

`data/preferences/` 下 `AuthPreferences`、`AppLanguagePreferences`、`AgentPreferences`、`ClaudePreferences` 使用 DataStore / SharedPreferences 系（源码中出现 `SharedPreferences|DataStore|preferencesDataStore|filesDir` 的文件即上述目录）。

### 7.3 会话真实数据源在外部进程

Claude 侧读 CLI 自己的 transcript：`data/agent/claude/ClaudeTranscript.kt`（915 行）与 Go 侧 `pkg/transcript/parser.go`。agy 侧走 gRPC 的 conversation/trajectory 服务（`AgyConversationService`、`CortexTrajectoryDto`）。即：**应用不自己保存聊天历史的主副本**。

### 7.4 字符串资源（唯一有精确覆盖率的指标）

实测脚本（只读，逐 key 比对 `values/` 与 `values-zh-rCN/`）：

```bash
python3 - <<'PY'
import os, xml.etree.ElementTree as ET
base="app/src/main/res/values"; zh="app/src/main/res/values-zh-rCN"
def keys(d):
    ks=set()
    for fn in sorted(os.listdir(d)):
        if fn.endswith(".xml"):
            for el in ET.parse(os.path.join(d,fn)).getroot():
                if el.get("name"): ks.add(el.get("name"))
    return ks
b, z = keys(base), keys(zh)
print(len(b), len(z), len(b-z), len(z-b), f"{len(b&z)/len(b)*100:.1f}%")
PY
# 输出：2061 2055 6 0 99.7%
```

- `values/` 14 个 XML → 2061 个 key；`values-zh-rCN/` 12 个 XML → 2055 个 key。
- 缺失 6 个，且**全部是白名单性质**：`Theme.Gemini`、`Theme.Gemini.Splash`、`Theme.AntiGem.FloatingTranslucent`（主题/样式名）、`app_name`、`anti_term_name`、`termux_name`（产品名）。
- 零个「中文侧超出基线」的 key（`values-zh-rCN` 无越界定义），与 `scripts/i18n/verify.py` 第 3 条校验目标一致。

---

## 8. 横切关注点

### 8.1 鉴权

| 通道 | 机制 | 依据 |
| --- | --- | --- |
| Google / agy | `GoogleOAuthManager` + `AgyCsrfManager`（CSRF token 由 Go Hub Supervisor 经 WebSocket 推送） | `data/remote/GoogleOAuthManager.kt`、`data/remote/core/AgyCsrfManager.kt` |
| Claude | 桥接的 `/auth/login`、`/auth/logout` 端点 | `ClaudeBridgeClient` 端点字符串 |
| 应用间调用 | 自定义权限 `${applicationId}.permission.RUN_COMMAND`、`.permission.API` | `AndroidManifest.xml` |

### 8.2 错误处理（实测模式，无统一 Result 类型）

`try {` 618 次、`catch (` 586 次、`runCatching` 仅 40 次。即**以 try/catch + 局部降级为主**，没有跨层的 `Result<T>` 或 sealed error 管道。新增代码应与所在文件保持一致，不要引入第二套错误体系。

### 8.3 日志

`android.util.Log`，41 个类定义了 `private const val TAG`。无 Timber / 无结构化上报；另有 `AntiGemLiveDiagnostics`、`FloatingDiagnosticsOverlay`、`NetworkInspectorManager` 做界面内诊断（`NetworkInspectorInterceptor` 挂在 OkHttp 链上）。

### 8.4 国际化

- 机制：`AppLocaleController.apply(activity)` 在每个入口 Activity 的 `setContent` 之前调用；`AndroidManifest.xml` 用 `android:localeConfig="@xml/locales_config"`。
- 校验：`python3 scripts/i18n/verify.py` 在 CI 编译前跑，检查 ①XML 合法性 ②`R.string.*` 是否都有定义 ③中文侧不越界 ④`stringResource` 不得进非 Composable 上下文。
- 覆盖率：99.7%（§7.4 实测）。

### 8.5 安全（实测到的现状，含风险项）

| 项 | 现状 | 位置 |
| --- | --- | --- |
| Keystore 口令硬编码 | `storePassword = "123456"`、`keyPassword = "123456"`、alias `antigem` | `app/build.gradle.kts` `signingConfigs` |
| Keystore 本体 | `app/release.jks` 不入库，CI 由 secret `RELEASE_KEYSTORE_BASE64` 还原 | `.github/workflows/*.yml` |
| 明文流量 | `android:usesCleartextTraffic="true"`（本地 127.0.0.1 通信所需） | `AndroidManifest.xml` |
| 高权限 | `MANAGE_EXTERNAL_STORAGE`、`QUERY_ALL_PACKAGES`、`REQUEST_INSTALL_PACKAGES`、`SYSTEM_ALERT_WINDOW` | `AndroidManifest.xml` |
| 桥接侧校验 | Go `pkg/security/security.go` | `agy_ide_bridge/pkg/security/` |

> 硬编码口令是**实测事实**，是否可接受取决于仓库是否公开；本文只记录，不做判定。

---

## 9. 服务通信

| 链路 | 传输 | 实现（实测） | 端点/端口 |
| --- | --- | --- | --- |
| App ↔ agy 语言服务器 | **二进制 gRPC**（Wire, suspending） | `data/remote/services/AgyLanguageService.kt`：`GrpcClient.Builder()`、`GrpcStreamingCall`、`GrpcException/GrpcStatus` | HUB_PORT（termux 1235 / standard 1237） |
| App ↔ agy 认证/模型/会话 | HTTP（同一 Wire 客户端或 OkHttp） | `AgyAuthService`、`AgyModelService`、`AgyConversationService` 等 | 同上 |
| App ↔ Go 桥接（Claude） | **WebSocket + HTTP** | `ClaudeBridgeClient`：`okhttp3.WebSocketListener`、`wsClient.newWebSocket(...)`；`AuthPreferences` 做 `http://`→`ws://` 改写 | BRIDGE_PORT（termux 1234 / standard 1236） |
| 桥接 ↔ Claude CLI | **stream-json（NDJSON）**，每会话一个常驻进程 | `CLAUDE_CLI_PROTOCOL.md`；Go `pkg/claude/manager.go` | 进程内管道 |
| 桥接 HTTP 路由 | REST-ish | 实测字符串：`/api/claude`、`/status`、`/info`、`/sessions`、`/usage`、`/auth/login`、`/auth/logout`、`/settings`、`/memory/auto` | 同上 |
| Browser MCP | 本地 HTTP | `AndroidLocalBridgeServer`，`DEFAULT_PORT = BuildConfig.BROWSER_MCP_PORT` | termux 8765 / standard 8767 |
| Termux daemon | 本地 HTTP | `termux-daemon/server.go`（项目创建/文件读写/保存） | 未测量（未读取监听段） |
| 终端 | 原生 PTY + SSH | `TermuxService`、`data/ssh/TermuxSshManager`（JSch） | — |

**没有** GraphQL，**没有** 消息队列/事件总线（除 Kotlin Flow 外无 EventBus 类总线）。

---

## 10. 测试覆盖

- **整体覆盖率：未测量。** 依据：`build.gradle.kts`、`libs.versions.toml`、CI 里都搜不到 `jacoco` / `kover`（`grep -rniE 'jacoco|kover'` 空），项目未配置覆盖率采集，因此任何百分比都无从产生，本文不做估算。
- **测试框架**：JUnit 4（`junit:junit 4.13.2`）+ kotlinx-coroutines-test；Android 侧 `androidx.test` + Espresso + Compose `ui-test-junit4`；浏览器运行时用 Node 原生 `node:test`（`.mjs`）。
- **现有测试规模（实测）**：Kotlin 测试 4 个文件 / 13 个 `@Test`（`MainScreenTest.kt`、`MainScreenViewModelTest.kt`、`TrajectoryErrorIngestionTest.kt`、`WebViewExtensionArchiveTest.kt`）；`app/src/test/browserextensions/` 2 个 mjs、23 个用例；Go 侧 **0** 个 `_test.go`。
- **形态**：单元测试为主（domain/chat、ui/main、ui/browser），1 个 instrumented（`MainScreenTest`），有 Node 侧的扩展运行时/工作流测试。**无端到端测试**。
- **明显未覆盖区**（仅陈述规模对比事实）：`data/` 72 个文件无对应测试；`AgyChatBackend`（2162 行）、`ClaudeChatBackend`（1309 行）、`data/remote/*` gRPC 层、`agy_ide_bridge/` 全部 24 个 Go 文件均无测试。
- **执行命令**：仓库内**没有**出现任何单元测试执行命令（CI 只有 `assembleTermuxRelease|Debug` + `scripts/i18n/verify.py` + `verify_apk_signature.py`），故测试任务名本次未验证 —— 见 §12。

---

## 11. 入口点

### 11.1 应用入口

| 文件 | 作用 |
| --- | --- |
| `app/src/main/java/com/example/gemini/MainActivity.kt`（548 行） | 主入口；`AppViewMode` 枚举（CHAT / IDE / TERMINAL / BROWSER）；先 `AppLocaleController.apply(this)` 再 `setContent`；单例 `currentInstance` / `showToast` |
| `app/src/main/java/com/example/gemini/TermuxActivity.kt` | 终端 Activity |
| `app/src/main/java/com/example/gemini/Navigation.kt` + `NavigationKeys.kt` | navigation3 路由（`@Serializable data object Main : NavKey`） |
| `app/src/main/java/com/termux/app/*.kt` + Java | fork 的 Termux 终端/服务实现 |
| `app/src/main/AndroidManifest.xml`（283 行） | 组件与权限声明；`<application>` **无** `android:name` |
| `agy_ide_bridge/main.go` | Go 桥接进程入口 |
| `termux-daemon/server.go` | 本地文件/项目 HTTP 服务入口 |

### 11.2 构建期入口（`app/build.gradle.kts`，全部挂在 `preBuild`）

1. `buildAgyIdeBridge`：在 `agy_ide_bridge/` 下执行 `GOOS=android GOARCH=arm64 CGO_ENABLED=0 go build -ldflags=-s -w -o ../app/src/main/assets/bin/agy_ide_bridge main.go`。
2. `copyScriptsToAssets`：复制 `scripts/{backup_bootstrap,backup_chats,restore_chats,backup_projects,restore_projects,backup_proot_ubuntu}.sh` 到 `app/src/main/assets/bin/`。
3. `bundleBrowserExtensions`：`extensions/` → `build/generated/browserExtensionAssets/browser-extensions`（排除 `test/`、`node_modules`、`.storage`、`.antigem-state`）。
4. `bundleExtensionDocs`：`extension_docs/` → `build/generated/extensionDocAssets/extension-docs`。

> 现场事实：`app/src/main/assets/bin/` 目前只有 3 个脚本（`backup_projects`、`backup_proot_ubuntu`、`restore_projects`），**没有** `agy_ide_bridge` 二进制 —— 它是构建期产物，未提交。

### 11.3 已实测存在的命令清单

| 命令 | 来源 | 用途 |
| --- | --- | --- |
| `./gradlew :app:assembleTermuxRelease --stacktrace --no-daemon` | `ci.yml`（有 keystore 时） | 出 release 签名包 |
| `./gradlew :app:assembleTermuxDebug --stacktrace --no-daemon` | `ci.yml`（无 keystore 时） | 出 debug 包 |
| `python3 scripts/i18n/verify.py` | `ci.yml`「Verify i18n resources」步 | 编译前静态校验 |
| `python3 scripts/verify_apk_signature.py <apk>` | `ci.yml`「Verify APK signature」步 | 签名指纹校验 |
| `./scripts/build_all_apks.sh --release` | `release.yml` | 发版只出 termux 包（standard 分支已停用） |
| `python3 -c "import json;print(json.load(open('version.json'))['version_name'])"` | `ci.yml` | 取版本号 |

**关于 build 变体的项目约束**：仓库根 `AGENTS.md` 明确「只为 termux flavor（`com.termux`）构建出包，`standard` flavor 不构建、不出包」，因此上表里的 `assembleTermux*` 是唯一应执行的构建目标；且本项目实际只在云端 CI 里执行它（本地不跑 Gradle），`standard` flavor 不在任何一条出包路径上。

---

## 12. 未测量 / 不确定项

| 项 | 状态 | 原因 |
| --- | --- | --- |
| 单元/集成测试覆盖率 % | **未测量** | 项目无 jacoco/kover 配置 |
| 构建是否能通过 | **未测量** | 本次未执行 Gradle（设备上全量构建代价高，且项目规则要求先确认无并行构建任务） |
| APK 产物体积、签名指纹 | **未测量** | 未构建、未拿到 APK |
| 单元测试执行任务名（如 `testTermuxDebugUnitTest`） | **未验证** | 仓库内无任何执行测试的命令可引用 |
| `termux-daemon` 监听端口 | **未测量** | 按抽样原则未读取 `server.go` 的监听段 |
| Go 侧 `modernc.org/sqlite` 是否真正写库 | **不确定** | 依赖在 `go.mod` 中标为 `// indirect`，未追踪调用点 |
| `go.mod` 声明 `go 1.26.5` vs CI `go-version: '1.22'` | **已查明（2026-10-23）**：`go 1.26.5` 是最低要求，实际编译用的就是 go1.26.5；CI 的 1.22 仅在自动切换前充当引导 | ①`go.dev/doc/toolchain`：`go` 行声明最低 Go 版本，省略 `toolchain` 行时等价于隐式 `toolchain go1.26.5`；`GOTOOLCHAIN=auto`（标准发行版默认）会去找/下载 `go1.26.5` 并改用它，只有 `local` 才直接拒绝。②仓库内 `GOTOOLCHAIN` 在任何 workflow / 脚本 / go.mod 中都未设置（grep 为空）。③实证：GitHub API `runs/39971226640`（HEAD d1e2a16，2026-10-23）**conclusion=success**，其中 `Build termux APK` 步 success —— 该步必然触发 `preBuild → buildAgyIdeBridge → go build`。④旁证：`docs/i18n/archive/HANDOFF.md` 记录 master 构建 `BUILD SUCCESSFUL`。**风险**：CI 每次都要下载约 50MB 的 go1.26.5 工具链且依赖 proxy/GOSUMDB 可用；若有人把 GOTOOLCHAIN 设成 local 会直接构建失败。**建议**（未执行，需你确认）：把两处 `go-version: '1.22'` 改为 `'1.26.5'`，或改用 `go-version-file: agy_ide_bridge/go.mod`（该写法在 setup-go v5 的读取行为我未实测，v6 才明确支持 `toolchain` 指令）；README 的「Go 1.23+」也应改为 1.26.5+ |
| `docs/` 是否就是 GitHub Pages 站点根 | **本仓库未启用 Pages（2026-10-23 已核实）**，把文档写进 `docs/` 不会被当成站点资源发布 | ①`GET https://api.github.com/repos/743553649/agy-and-claude/pages` → **HTTP 404**，响应体 `{"message":"Not Found",...}`；②仓库内无任何 Pages 部署 workflow（`grep -i pages .github/workflows/` 为空）；③`docs/index.html` 是**上游** `Santoshkurmi/antigem` 的官网首页，本仓库只是 fork 时继承；④上游是否用 Pages 发布本次**未查**（`https://santoshkurmi.github.io/antigem/` 与 `github.io` 均被本机网络阻断，curl 返回 000） |
| `targetSdk = 28` 是否影响上架 | **不构成问题：本项目自用，不发布 Google Play**（用户 2026-10-23 明确告知；此为口述事实，非本文实测） | `targetSdk = 28` 与 `lint { disable += "ExpiredTargetSdkVersion" }` 均为有意保留，不要为「合规」擅自提升 |

---

## 13. Changed Files

FULL 扫描，不适用（本次无 delta 基线）。

---

## 14. Last Scanned

`2026-10-23T15:12:49Z`（基线 `d1e2a16`）

> 补充修订（2026-10-23）：查清 Go 版本机制、核实本仓库未启用 GitHub Pages、并记录「自用不上架 Play」这一用户口述事实（见 §12）。