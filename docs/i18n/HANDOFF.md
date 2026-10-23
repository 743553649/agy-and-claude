# 汉化项目交接文档

> 本文档写给接手的人（或 AI）。读完它不需要询问上一轮对话的任何上下文。
>
> **2026-10-23 裁剪版**：只保留四节——第 0 节（完结状态）、第 6 节（踩过的坑）、
> 第 9 节（关键文件索引）、第 10 节（签名标准）。各批次进度、决策记录与验收细节
> 不在本文，查 `COVERAGE.md` 与 `git log`。

## 0. 完结状态（2026-10-23）

**结论：代码侧的可见文案已归零，工程完结。** 剩余英文都是按判据刻意保留的技术值 / 协议值 / 产品名。

### 最终数字

| 指标 | 汉化前 | 完结时 |
|---|---|---|
| `values` | 1546 | **2058** |
| `values-zh-rCN` | 1542 | **2055** |
| 源码引用 key | 1513 | **2025** |

`values` 比 `values-zh-rCN` 多 3 条，是**设计如此**：`app_name` / `anti_term_name` / `termux_name`
（纯产品名，翻了等于改应用名）。`values-zh-rCN/strings.xml` 文件头已写明
「产品名按 `GLOSSARY.md` 白名单保持英文原文，不在此重复定义」。
`accessibility_service_name` 已按用户要求从 `AntiGem AI Automation` 译为「AntiGem AI 自动化」
（品牌保留、描述部分翻译），因此不再计入这 3 条差额。

### 落地情况

- 全部改动经 **PR #1** 合并进 `master`（合并提交 `9d59843`），**默认分支现在就是中文界面 + 中文 README**；
- 每批推送都单独跑 CI（构建 + `verify.py` + APK 签名校验），最终 master 构建：
  `values: 2058 / 2055 / 2025`、`BUILD SUCCESSFUL`、`OK: 签名与标准指纹一致，可以覆盖安装`；
- 最终提交：`3e678ce`（补 rootfs 备份说明与媒体通知两处漏翻 + 修正 `AiLanguage` 注释）。

### 完结时的四个扫描器终值（可随时复跑）

```bash
python3 scripts/i18n/verify.py                    # 校验通过（资源完整性 / 占位符 / R 导入）
python3 scripts/i18n/check_composable_scope.py    # 可疑处 0 个（remember / 非组合函数 / onClick 回调）
python3 scripts/i18n/kotlin_scan.py               # 60 条，全为白名单
python3 scripts/i18n/find_untranslated_args.py    # 40 条，全为白名单
```

两类 100 条候选的白名单构成：产品名 17、终端按键 11、动画 label 12、代码标识符 8、
路径与变量 7、**提示词 5（要发给模型，翻了会污染模型输入）**、剪贴板元数据标签 17、
协议键与示例串。

### 真机验证（已完成）

- ✅ 通知文案跟随应用内语言（①）；
- ✅ 静态 `Locale.getDefault()` 跟随应用内语言（②；两个结论都与坑 18 的字节码推导相反，
  原推导已标注「被实测推翻」，机制待查证）；
- ✅ 用户逐屏走查：设置页、对话页、Claude 页、浏览器、IDE、通知弹窗，均无英文残留。

### 已收口的判断项

`accessibility_service_name`（系统无障碍设置里显示的服务名）原先按产品名白名单保留英文；
2026-10-23 用户确认译为「**AntiGem AI 自动化**」，已加到 `values-zh-rCN/strings.xml`
（这是手工维护文件，不经 `gen_resources` 流水线，故直接改）。

### 分支现状

- `master`：已包含全部汉化，**这是唯一的工作基准**；
- `feat/zh-cn-i18n`：已并入 master，2026-10-23 经用户确认**已删除**（远端与本地都已删）。
  需要找回旧提交时用 `git log` 或 GitHub 的 PR #1 页面，提交对象不会因为删分支而丢失。


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
另：API<33 时按字节码看 appcompat 只包装 **Activity** 的资源，`applicationContext.getString(...)`
（P3.7 的通知文案）与静态 `Locale.getDefault()` 理应都不跟随应用内语言——但 **2026-10-23 真机实测
把这两条都推翻了**：切换应用语言到 English 后，①通知文案也变英文；②同一个文件的详情里
「最后修改」从 `10月 22, 2026 · 10:17:33 下午` 变成 `Oct 22, 2026 · 10:17:33 PM`（该行取
`Locale.getDefault()`）。**以实测为准**，此处的机制推导待重新查证
（见第 0 节「真机验证」）。

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
**界面可见 0 条**（详见 `COVERAGE.md` 的「浏览器扩展文案」行）。

→ 教训：**「这段英文会不会被渲染」必须证到渲染路径**（谁 `Text()` 它、在哪个界面），
不能从「文件名听着像 UI 层」推断。为模型/日志准备的字符串即使翻译了，界面上也不会有任何变化。
本批次最终只改了真正可见的三块：扩展设置页对话框 6 条、管理器可显示错误 25 条、
内置扩展展示名/描述 2 条；**JS 一行未动**。

→ 同族提示：`manifest.json` 的 `name`/`description` 是运行时数据、不是 Android 资源，
且会经 `BrowserExtension.toJsonObject()` 发给扩展宿主，**不要在数据层本地化**——
只在渲染处按扩展 id 取资源（`extensionDisplayName/Description`），否则宿主侧标识会跟着变。


## 9. 关键文件索引

| 路径 | 说明 |
| --- | --- |
| `docs/i18n/GLOSSARY.md` | 术语表、不翻白名单、占位符约定、AI 语言指令的唯一措辞 |
| `docs/i18n/COVERAGE.md` | 按界面的覆盖清单（核对用） |
| `docs/i18n/HANDOFF.md` | 本文档 |
| `docs/i18n/archive/p1-language-switch-plan.md` | P1 方案 A 实施计划，**已落地并归档**（仅供追溯，不再是开工指南） |
| `docs/i18n/archive/language-switch-options.html` | P1 三方案对比（A/B/C）与决策依据，含本机核实证据（**已归档**，仅供追溯） |
| `scripts/i18n/` | 全部汉化工具 |
| `.github/workflows/ci.yml` | 云端构建（termux flavor + verify） |
| `.github/workflows/release.yml` | 上游发版流程，**不要动** |
| `app/src/main/res/values*/strings_*.xml` | 资源文件 |
| `app/src/main/java/com/example/gemini/domain/i18n/AiLanguage.kt` | AI 语言策略（P0 已接入） |

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
卸载重装（数据用 App 自带备份迁移）。

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