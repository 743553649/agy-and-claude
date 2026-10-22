# P1 设置页语言切换 · 实施方案 A（appcompat）实施计划

> 读这份文档 + `docs/i18n/HANDOFF.md` + `docs/i18n/GLOSSARY.md` 就能直接开工，不需要上一轮对话的上下文。
> **本文件只是计划，尚未改动任何代码。**

## 0. 一句话

用户已选定 **方案 A**：加 `androidx.appcompat`，用 `AppCompatDelegate.setApplicationLocales()` 做应用内语言切换（跟随系统 / 简体中文 / English），切换后界面、Toast、通知、AI 回复语言一起跟随，重启后保持。

## 1. 完成定义（DoD）

- [ ] 设置 → 外观 里出现「界面语言」三选一：跟随系统 / 简体中文 / English
- [ ] 切换后 Compose 界面文案立即变（必要时自动 recreate 一次，无闪烁、无循环）
- [ ] Toast、安装通知（P3 / P3.7 的 228 处 `getString`）跟随
- [ ] AI 回复语言跟随（`AiLanguage` 的注入生效）
- [ ] 杀掉进程重进，语言保持
- [ ] 英文界面下不出现中文，中文界面下不残留英文（除不翻白名单）
- [ ] `python3 scripts/i18n/verify.py` 通过；CI 绿（`Verify i18n resources` + `Build termux APK` + `Verify APK signature`）
- [ ] 真机验证矩阵（第 7 节）全部通过
- [ ] HANDOFF / COVERAGE 更新，catalog 备份到 `/sdcard/汉化/i18n_state/`

## 2. 两条既有硬约束需要解除（用户已在 2026-10-22 口头同意选 A）

| 约束 | 方案 A 下 |
| --- | --- |
| 不改 `app/build.gradle.kts` | **解除**：要加 appcompat 依赖（改 `app/build.gradle.kts` + `gradle/libs.versions.toml`） |
| 不改主题资源 | **解除**：`res/values/themes.xml` 要把父主题换成 AppCompat 后代 |
| 不改 `AndroidManifest.xml` | **仍然禁止**。因此 appcompat 的 `autoStoreLocales`（自动持久化）**用不了**——它要求在 manifest 里声明 `androidx.appcompat.app.AppLocalesMetadataHolderService`（本机反编译核实：其实现即读该 service 的 meta-data）。改为自己用 SharedPreferences 存、启动时回设 |
| 不改 `version.json` / `release.yml` / `settings.gradle.kts` | 仍然禁止 |

## 3. 现状事实（都核实过，带证据）

| 事实 | 证据 |
| --- | --- |
| 无 appcompat 依赖 | `app/build.gradle.kts` 依赖块、`gradle/libs.versions.toml` 均无 appcompat/material |
| 4 个 Compose Activity 都是 `ComponentActivity` | `MainActivity.kt:36`、`TermuxActivity`、`FloatingChatActivity`、`ManageSpaceActivity` |
| 另有 `TermuxOpenActivity` 继承 Termux 库的 `CoreOpenActivity` | `com/termux/app/TermuxOpenActivity.kt:8`，无自有 UI（透传 URL），**不要动** |
| 主题父级是框架主题 | `Theme.Gemini` 与 `Theme.AntiGem.FloatingTranslucent` 均 `parent="android:Theme.Material.Light.NoActionBar"` |
| SDK / 工具链 | `minSdk 24` / `targetSdk 28` / `compileSdk 36`；AGP `9.0.1`、Kotlin `2.3.20`；`android.useAndroidX=true`、`android.nonTransitiveRClass=true` |
| localeConfig 已就绪 | manifest 有 `android:localeConfig="@xml/locales_config"`，含 `en` + `zh-CN` |
| 无自定义 Application | `<application>` 没有 `android:name`（因此 <API33 的资源包装只能到 Activity 层） |
| appcompat 有该 API | 本机 `appcompat-1.6.1.aar` javap：`public static void setApplicationLocales(LocaleListCompat)` |
| 主题不匹配会崩 | appcompat 内含字符串 `You need to use a Theme.AppCompat theme (or descendant) with this activity.` |
| AI 语言判定 | `AiLanguage.isChinese()` = `Locale.getDefault().language == "zh"`（要改，见 4.2） |
| 非 Composable 文案 228 处 | 全部 `getString(R.string.…)`，接收者多为 Activity context 或 `applicationContext` |

## 4. 实施步骤（拆 2 个批次，一批一 push，等 CI 绿再下一批）

### 批次 A-1：基础设施（依赖 + 主题 + Activity 基类），**不加任何入口**

改动面 4 个文件：

1. `gradle/libs.versions.toml`
   - `[versions]` 加 `androidxAppcompat = "1.7.0"`（实现时确认 CI 能解析到的最新稳定版）
   - `[libraries]` 加 `androidx-appcompat = { module = "androidx.appcompat:appcompat", version.ref = "androidxAppcompat" }`
2. `app/build.gradle.kts` 依赖块加 `implementation(libs.androidx.appcompat)`
3. `res/values/themes.xml`
   - `Theme.Gemini` parent → `Theme.AppCompat.Light.NoActionBar`
   - `Theme.AntiGem.FloatingTranslucent` parent → `Theme.AppCompat.Light.NoActionBar`（保留 `windowIsTranslucent` / `windowBackground` / `statusBarColor` 等既有条目）
   - 保留 `android:windowSplashScreen*`（API 31+ 属性，框架会忽略低版本）
4. 4 个 Compose Activity：`ComponentActivity()` → `AppCompatActivity()`，并 import `androidx.appcompat.app.AppCompatActivity`
   - `MainActivity`、`TermuxActivity`、`FloatingChatActivity`、`ManageSpaceActivity`
   - Compose 不受影响：`AppCompatActivity` 本身继承链上有 `ComponentActivity`

A-1 的验收：**只证明「换成 AppCompat 主题 + 依赖」不炸**，界面与功能与之前一致（真机对照，重点看下拉菜单 / 对话框 / 输入框 / 状态栏 / 浮窗主题）。

### 批次 A-2：语言策略 + 设置页入口 + AI 跟随

1. **偏好存储**：新增 `data/preferences/AppLanguagePreferences.kt`（照 `AuthPreferences` 的 SharedPreferences 风格）
   - key `app_language`，取值 `system` / `zh-CN` / `en`
2. **策略层**：新增 `domain/i18n/AppLocaleController.kt`
   - `fun apply(context: Context)`：读偏好 → `AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))`；`system` 时传 `LocaleListCompat.getEmptyLocaleList()`
   - 调用点：`MainActivity.onCreate` 里 **`setContent` 之前**调用一次（幂等，别放 `onResume`，否则可能 recreate 循环）
3. **设置页入口**：`SettingsDialog.kt` 的 `AppearanceSubScreen`（约 924 行起）里加一段，复用主题卡片的选中样式（三选一卡片：跟随系统 / 简体中文 / English）
   - 新增回调 `onSetAppLanguage: (String) -> Unit`，沿 `SettingsDialog` 的既有参数链路传下去（照 `onSetThemeMode` 的写法）
   - 选择后：先存偏好，再 `AppCompatDelegate.setApplicationLocales(...)`
4. **AI 语言适配（关键）**：`AiLanguage` 不要再依赖 `Locale.getDefault()`
   - 改为 `fun isChinese(context: Context) = context.resources.configuration.locales[0].language == "zh"`
   - 两个调用点同步改：`data/remote/AntigravityApiService.kt`（`suffix()`）、`data/agent/claude/ClaudeChatBackend.kt`（`appendSystemPromptOrNull()`）
   - 原因：API<33 时 appcompat 包装的是**资源 Config**，静态 `Locale.getDefault()` 是否同步变化未验证；读取真实资源 Config 才可靠
5. **资源文案**（严格走 `gen_resources.py`，双语 + `<!-- 原文: -->`）
   | key | en | zh-CN |
   | --- | --- | --- |
   | `settings_language_title` | Language | 界面语言 |
   | `settings_language_follow_system` | Follow system | 跟随系统 |
   | `settings_language_zh` | Simplified Chinese | 简体中文 |
   | `settings_language_en` | English | English |
   | `settings_language_hint` | Interface and AI replies use this language | 界面与 AI 回复使用该语言 |
   | `settings_language_unsupported` | This system version follows the app language only after restart | 该系统版本可能需重启应用后完全生效 |
6. **文案落库方式**：写进 `scripts/i18n/` 的对照表 + 复用现有脚本；若这几条是 Composable 内的 `Text(...)`，按既有流水线（scan → rebuild_catalog → merge → gen_resources → apply → verify）走，不要手改 XML

### 建议的提交切分

- `feat: 引入 appcompat 并切换到 AppCompat 主题（P1-A1）` → push → 等 CI 绿 + 真机对照
- `feat: 设置页语言切换与 AI 语言跟随（P1-A2）` → push → 等 CI 绿 + 真机矩阵
- `docs: 记录 P1 落地与真机验证结果`

## 5. 风险与对策

| 风险 | 说明 | 对策 |
| --- | --- | --- |
| 主题视觉回归（最大） | 框架 `android:Theme.Material` → `Theme.AppCompat`，所有 View 控件默认样式变了 | A-1 单独成批，真机逐屏对照；截图留档；只看崩溃不够，要看样式 |
| recreate 循环 | 在 `onResume` / 每次组合里 set locale 会反复重建 | 只在 `onCreate` 与用户点击时 set；一次 set 之后 `AppCompatDelegate.getApplicationLocales()` 相同就不再 set |
| <API33 下 `Locale.getDefault()` 不变 | 影响 `AiLanguage` | 已改为读资源 Config（4.2），真机验证 |
| <API33 下通知/Toast 是否跟随**未验证** | 通知走 `applicationContext`，appcompat 在低版本只在 Activity 层包装资源 | 真机验证；若不跟随，二选一并让用户拍板：①接受低版本通知为系统语言 ②改 `LocalEnvironmentManager` 等处改用能拿到当前语言的 context（代价大） |
| `autoStoreLocales` 不可用 | 需要 manifest service | 自己持久化（4.1 / 4.2） |
| AGP 9.0.1 + appcompat 1.7.0 兼容性 | 未验证 | CI 首次编译即暴露；失败则退 1.6.1 再试，并在 HANDOFF 记一笔 |
| R8 压缩后异常 | appcompat 自带 consumer rules | CI 产物真机跑一遍对话/终端/IDE |

## 6. 回滚

- A-1 与 A-2 分开提交 → 可只回滚 A-2（删入口与策略，主题/依赖保留，无害）
- 若 A-1 主题回归不可接受：`themes.xml` 父主题改回 `android:Theme.Material.Light.NoActionBar` + 4 个 Activity 改回 `ComponentActivity`（依赖可留，不影响）→ 回到今天的状态
- 不建议用 `git reset --hard` + 强推；用 revert 提交保留历史

## 7. 真机验证矩阵

| API | 路径 | 必看 |
| --- | --- | --- |
| 33 / 34 | 框架 `LocaleManager`（appcompat 转调） | 切换即时生效、通知跟随、AI 跟随、重启保持 |
| 28 | appcompat 资源包装（低版本路径） | 界面跟随、**通知是否跟随**、AI 是否跟随、是否有 recreate 闪烁 |
| 24 | 同上（最低版本） | 同上；Chrome/WebView 相关页面无异常 |

每个级别都要看：设置页、对话页、IDE、内置浏览器、终端引导、全屏安装页、Toast、安装通知、AI 首条回复。

## 8. 与汉化流水线的关系（别踩）

- 新增任何文案都走 `scripts/i18n/` 的流水线（`verify.py` 必跑），**不要手改 `values*/strings_*.xml`**——`gen_resources.py` 会整文件重写
- `gen_coverage.py` 第 4 节是脚本里硬编码的，改批次要同步改脚本再跑（HANDOFF 坑 15）
- 改写脚本产出新 `R.string.` 后**必须跑 `fix_imports.py`**（坑 17，已加进 `verify.py`）
- 每批结束把 `/tmp/i18n_catalog.json`、`/tmp/i18n_dynamic.json` 备份到 `/sdcard/汉化/i18n_state/`
- push 前确认 `getent hosts api.github.com` 不是 `198.18.*`（VPN fake-ip）

## 9. 本计划不覆盖的遗留（另开批次）

`AppearanceSubScreen`（`SettingsDialog.kt:924` 起）里仍有用户可见英文标签，实测：

- 主题卡：`Auto (System)` / `Light` / `Dark`（行 964-966）
- 字号卡：`Small` / `Normal` / `Medium` / `Large` / `Huge`（行 1067）
- 悬浮切换器方向：`VERTICAL` / `HORIZONTAL`（行 1337-1362，注意 `VERTICAL` 也是比较用的取值，需逐条判定）
- 自动收起：`Never`（行 1395）
- 悬浮切换器条目：`Chat` / `IDE (Code Editor)` / `Terminal` / `Web Browser`（行 1444-1447）

这些是 P3.6 漏掉的「`listOf(Triple("SYSTEM", "Auto (System)", …))` 元组第二项」盲区（坑 11 的变体），建议作为 P3.9 单独一批处理。

## 10. 新窗口开工清单

1. `cd /sdcard/汉化/agy-and-claude && git status`、`git log --oneline -3`，确认在 `feat/zh-cn-i18n` 且干净
2. 读 `docs/i18n/HANDOFF.md`（第 2、4、6 节）、`docs/i18n/GLOSSARY.md`、本文
3. `python3 scripts/i18n/verify.py` 确认基线绿
4. 做 A-1（第 4 节），`verify.py` → commit → push → 等 CI（约 9 分钟）
5. CI 绿后真机对照 A-1（第 7 节的界面清单，重点样式）
6. 再做 A-2，同样流程
7. 收尾：更新 HANDOFF（P1 状态 + 真机结论 + 新坑）、`gen_coverage.py` 第 4 节、catalog 备份
