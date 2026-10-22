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
4. **不改构建配置**：`app/build.gradle.kts`、`AndroidManifest.xml`、`settings.gradle.kts` 一律不动（已确认目前未被改动）。
5. **保留双语**：用 `values/` + `values-zh-rCN/`，不硬替换成中文。
6. **AI 回复语言跟随界面**：中文界面注入中文指令，英文界面不注入。

### 2.1 仓库 `AGENTS.md` 与本项目做法的冲突（已由用户拍板）

仓库自带 `AGENTS.md` 写着「不要 git push / commit」「只编 termuxBuild，绝不编 Standard」，与用户指示冲突时**以用户指示为准**。当前 CI 已按用户选择的方案改为 **termux flavor（`com.termux`）**，与上游发版包名一致，可覆盖安装。

## 3. 已完成的工作

分支上共 10 个提交，5 个功能批次 + 4 次修复 + 1 次文档：

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

### 当前覆盖率（准数）

- 有源码引用的文案：**705 条**
- 已翻译：**676 条（95.9%）**
- 源码中 `stringResource` 调用点：**1324 处**，覆盖 **71 个文件**
- 资源条目：`values/` 1143 条，`values-zh-rCN/` 1139 条
- 剩余 29 条全部是有意不翻的（VS Code 变量模板、示例路径/URL、动画测试标识）

## 4. 未完成的工作（按建议优先级）

### P0：AI 中文回复注入（用户已定方案，代码未落地）
`app/src/main/java/com/example/gemini/domain/i18n/AiLanguage.kt` **已创建但没有任何地方引用**。需要：

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

**验证边界**：CI 只能证明编译通过。Claude CLI 是否真的接受 `initialize` + `appendSystemPrompt`
必须真机实测。若不被接受，最坏情况是"Claude 仍回英文"，不会挂会话——
依据：`ClaudeChatBackend.deliverReply()` 是 `replies.remove(id)?.complete(...)`，未匹配的响应被忽略。

### P1：设置页语言切换
`AppCompatDelegate.setApplicationLocales`（Android 13+ 原生；项目 `targetSdk = 28`，
13 以下降级为跟随系统）。加在 `ui/settings/SettingsDialog.kt` 里，
`AiLanguage` 读的是 `Locale.getDefault()`，切换后自然生效。

### P2：`title = / label =` 剩余 67 处
已还原成英文。这些多为 data class 构造参数（如 `ScriptExecutionInfo(title = ...)`、
`sessionManager.addNewTab(title = ...)`），不能直接放 `stringResource`。正确做法：
在 Composable 作用域内先 `val t = stringResource(R.string.x)`，再把 `t` 传进构造函数。

### P3：Toast 的 `message` 参数
`Toast.makeText(context, "英文", ...)` 这类要点：非 Composable 上下文，必须用
`context.getString(R.string.x)`。约 7 处。

### P4：浏览器扩展文案
`extensions/google-flow/` 7 个 JS 文件、2618 行，无 HTML popup，文案在
`page-helpers.js` 注入的浮层里。约定方案：JS 字典对象 + `navigator.language` 判断，
只覆盖约 40 处浮层可见文案，不改模型名/错误信息。

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
| `fix_imports.py` | 补齐 `import com.example.gemini.R` 与 `stringResource` 导入 |
| `verify.py` | **CI 编译前必跑**：资源 XML 合法性、`R.string` 引用完整性、擦作符/参数一致性、非 Composable 误用、英文资源残留 Kotlin 插值、裸 `%` |
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
python3 scripts/i18n/fix_imports.py          # 补 import
python3 scripts/i18n/verify.py               # 必须通过
python3 scripts/i18n/gen_coverage.py          # 更新覆盖文档
git add -A && git commit && git push         # 触发云端 CI
```

**注意**：`/tmp/i18n_*.json` 是中间状态，容器重启或换机器会丢。丢了就先跑
`kotlin_scan.py` + `rebuild_catalog.py`，已有译文会按英文原文自动匹配回来；
但**已提交到 `values-zh-rCN/` 的译文不会丢**，那才是最终产物。

## 6. 踩过的坑（全是真实报错，别再踩）

1. **`Text\(` 正则会误匹配 `newPlainText(`**
   → 在 `onClick` 里调用 `stringResource`，报 `@Composable invocations can only happen from the context of a @Composable function`。
   修法：正则加左侧边界 `(?<![A-Za-z0-9_$])`；`ClipData.newPlainText` 用 `context.getString`。

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
   → `.orEmpty()`。

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
| flavor | **termux**（`com.termux`），与上游发版同包名，可覆盖安装 |
| 签名 | 需要仓库 secret `RELEASE_KEYSTORE_BASE64`；缺失时自动退回 debug 签名并出 warning |

术语表见 `docs/i18n/GLOSSARY.md`（Agent=智能体、Session=会话、Workspace=工作区、
Extension=扩展、Bridge=桥接、Skill=技能、Trajectory=执行轨迹、Prompt=提示词、Hub=中枢）。
**改译法前先改术语表。**

## 8. 验收方法

1. GitHub → Actions → `CI Build` → 某次运行 → 底部 Artifacts → `antiGem-termux-<sha>` → 解压得 APK
2. 装到手机，**系统语言设为简体中文**
3. 按 `docs/i18n/COVERAGE.md` 逐项核对
4. 每次 CI 约 7–9 分钟；`verify i18n resources` 步骤失败会 10 秒内红，比等 Gradle 快

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

## 9. 关键文件索引

| 路径 | 说明 |
| --- | --- |
| `docs/i18n/GLOSSARY.md` | 术语表、不翻白名单、占位符约定、AI 语言指令的唯一措辞 |
| `docs/i18n/COVERAGE.md` | 按界面的覆盖清单（核对用） |
| `docs/i18n/HANDOFF.md` | 本文档 |
| `scripts/i18n/` | 全部汉化工具 |
| `.github/workflows/ci.yml` | 云端构建（termux flavor + verify） |
| `.github/workflows/release.yml` | 上游发版流程，**不要动** |
| `app/src/main/res/values*/strings_*.xml` | 资源文件 |
| `app/src/main/java/com/example/gemini/domain/i18n/AiLanguage.kt` | AI 语言策略（已建，未引用） |
