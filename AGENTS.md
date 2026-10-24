## 沟通与用户交互规则

- **先回答问题**：当用户提问、指出问题或争论某个设计观点时，永远先在文字里直接回答并解释给用户。
- **不要偷改文件**：绝不要在沉默中开始修改代码，而不回答用户的问题或回应他们的直接反馈。
- **格式一致性**：JSON 和二进制 gRPC 从守护进程返回的 protobuf schema 和字段完全相同；永远不要假设或声称它们返回不同的键或格式。
- **只做用户让你做的事**：绝不碰用户没有让你动的那一行。用户说执行任务 X，那就只做那件事，别的什么都不做，连一个多余的空格都不要加。如果确实需要改动超出用户要求的范围，永远先向用户解释你也想这样做。

-- 运行新的构建等命令前，永远先检查是否已有同样的任务在跑：有的话要么先杀掉再起新的，要么等旧的结束（不要两个同时跑）。

-- 执行新任务前永远先检查正在运行的进程（免得同一个任务你执行两遍或更多遍）。

-- 永远把你即将做的每一处改动都详细解释给用户（不用逐行，但要说清楚），并且不要开始编辑，永远先解释，然后停在那里，用户会回复 continue、do it 或别的什么，或者继续问你更多问题、跟你争辩。

- 永远不要尝试为 Standard Build 编译或打包，永远只做 termuxBuild。

- 如果你对某件事怎么做没有 100% 把握，永远不要去做，这种情况下永远先问用户要指示，而不是在没理解用户想要什么的情况下动手。

## 项目事实（扫描实测，2026-10-23，基线 d1e2a16）

### 构建：只做 termux flavor

- 只有两条构建命令：`./gradlew :app:assembleTermuxDebug`（没有 `app/release.jks` 时）、`./gradlew :app:assembleTermuxRelease`（有 keystore 时，CI 走这条）。`standard` flavor（`com.antigem`）不构建、不出包。
- 上面两条会经 `preBuild` 自动带起 4 个生成任务：`buildAgyIdeBridge`（`GOOS=android GOARCH=arm64 CGO_ENABLED=0 go build`）、`copyScriptsToAssets`、`bundleBrowserExtensions`、`bundleExtensionDocs`，产物落在 `app/src/main/assets/bin/` 与 `build/generated/`。
- 所以：改备份脚本 → 改 `scripts/*.sh`；改内置扩展 → 改 `extensions/`；改扩展文档 → 改 `extension_docs/`。**不要直接改 `app/src/main/assets/` 下的副本**，构建会覆盖。
- 发版是 `release.yml` 的事（仅 `version.json` 变更触发），不要手工发版、不要手改 `version.json` 以外的版本来源。

### i18n：改任何可见文案都要过四步校验

- 必跑：`python3 scripts/i18n/verify.py`。CI 在编译前跑它，失败比整轮 Gradle 早十几秒暴露。
- 它查四件事：①资源 XML 必须合法；②每个 `R.string.<key>` 都要在 `values/` 里有定义；③`values-zh-rCN/` 的 key 不得超出 `values/`；④`stringResource(...)` 不得出现在非 Composable 上下文（`LaunchedEffect` / `remember` / `onClick` 的 lambda 里都算非 Composable）。
- 手工新增 `R.string.xxx` 时记得补 `import com.example.gemini.R`，否则 CI 报 `Unresolved reference 'R'`。
- 现状（实测）：`values/` 2061 个 key、`values-zh-rCN/` 2055 个 key，覆盖率 99.7%；缺的 6 个是主题名与产品名白名单。不要为了凑 100% 去翻主题名/应用名。
- 判据与历史坑在 `docs/i18n/archive/HANDOFF.md`。`scripts/i18n/archive/` 里是一次性源码改写工具，**不要重跑**。

### 命名：一个项目四套名字

| 层 | 名字 |
| --- | --- |
| Gradle 根项目 | `Gemini`（`settings.gradle.kts`） |
| Kotlin 包名 / namespace | `com.example.gemini` |
| applicationId（standard / termux） | `com.antigem` / `com.termux` |
| APK 归档名 | `antiGem` |
| 仓库名 | `agy-and-claude` |

- 说「包名」时必须先指明是哪一层：改 `applicationId` 会让已装机的旧版无法覆盖升级（签名+包名双重绑定），改 Kotlin 包名是全树重构，两者不是一回事。
- 端口全部来自 `BuildConfig`，代码里不得硬编码：`BRIDGE_PORT` / `HUB_PORT` / `BROWSER_MCP_PORT` = termux `1234 / 1235 / 8765`，standard `1236 / 1237 / 8767`。这套差异是为了 `com.antigem` 与 `com.termux` 能同时安装、不抢端口。

## 文档路由（先查这里，不要整篇读）

| 要解决的事 | 读哪个 | 怎么读 |
| --- | --- | --- |
| 技术栈 / 依赖版本 / 目录职责 / 层间调用 | `docs/project-doc.md` | 先看 `##` 标题定位，再 `read` 该节的 offset/limit |
| 某个数字的来源、哪些是「未测量」 | `docs/project-doc.md` §0、§12 | 只读表 |
| 签名指纹与安装前提（硬约束，不可重算） | `docs/i18n/archive/HANDOFF.md` §10 | 必读全节 |
| i18n 判据与历史坑 | `docs/i18n/archive/HANDOFF.md` §6、§0 | §6 有 20 条，先 grep 关键词再读 |
| 汉化改动范围 / 白名单 | `docs/i18n/archive/COVERAGE.md` | 只读表头结论 |
| Claude CLI / Google Flow 协议 | `CLAUDE_CLI_PROTOCOL.md`（45K）、`google-flow-mcp-spec.md`（54K） | **禁止整篇读**，先 `grep -n` 定小节 |
| 扩展与 MCP 开发 | `extension_docs/*.md` | 按需单篇 |

读取纪律：>15KB 或 >400 行的文件一律先 `grep -n` 定位再局部 `read`；不要为了「了解项目」通读源码或通读归档。
