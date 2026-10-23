#!/usr/bin/env python3
"""P4 批次：浏览器扩展相关界面文案（设置页对话框 + 数据层错误 + 内置扩展展示名）。

背景：`extensions/google-flow/` 的 6 个 JS 文件经逐条核实**没有任何浮层 UI**
（page-helpers.js 是 chrome.scripting 注入的页面自动化库，不建 DOM 文本；
manifest 无 popup/options/notifications）。真正用户可见的扩展文案只有两处：
① `manifest.json` 的 name/description 由设置页直接 Text() 渲染；
② 扩展设置页自己的 Kotlin 文案与 BrowserExtensionManager 的可见错误。
所以本批次**不改任何 JS**，只处理 ① ②。

约定：
- Composable 作用域 → stringResource；非 Composable（runOperation 回调）→ context.getString；
- 数据层异常消息走 `appContext?.getString(…) ?: 英文兜底`（本项目既有写法，见 P2 批次）；
- 内置扩展的展示名/描述用 id 判定走资源，**不改** BrowserExtension.name/description 字段
  （它们会经 toJsonObject 发给扩展宿主，改了会影响宿主侧标识）。

用法：apply_p4_extension.py --dry-run / apply_p4_extension.py，然后依次
gen_resources.py → fix_imports.py → verify.py。
"""
import json
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SRC = "app/src/main/java/com/example/gemini/"
CAT = "/tmp/i18n_catalog.json"
DYN = "/tmp/i18n_dynamic.json"
DRY = "--dry-run" in sys.argv

SCREEN = "ui/settings/WebViewExtensionsScreen.kt"
MANAGER = "ui/browser/BrowserExtensionManager.kt"

# (文件, 原文子串, 替换子串, 期望次数)
SITES = [
    # --- 扩展设置页：操作失败兜底（runOperation 回调，非 Composable） ---
    (SCREEN,
     'onFailure { error = it.message ?: "Extension operation failed" }',
     "onFailure { error = it.message ?: context.getString(R.string.settings_extension_operation_failed) }", 1),
    # --- 扩展卡片：内置扩展展示名（description 在下方单独处理） ---
    (SCREEN,
     "Text(stringResource(R.string.settings_webview_ext_summary, extension.name, extension.version, extension.toolCount)",
     "Text(stringResource(R.string.settings_webview_ext_summary, extensionDisplayName(extension), extension.version, extension.toolCount)", 1),
    (SCREEN,
     "if (extension.description.isNotBlank()) Text(extension.description, style = MaterialTheme.typography.bodySmall)",
     "val description = extensionDisplayDescription(extension)\n"
     "                    if (description.isNotBlank()) Text(description, style = MaterialTheme.typography.bodySmall)", 1),
    # --- MCP 选择器里的同一处展示名 ---
    (SCREEN,
     "Text(stringResource(R.string.settings_extension_name_extension_toolcount_tools, extension.name, extension.toolCount)",
     "Text(stringResource(R.string.settings_extension_name_extension_toolcount_tools, extensionDisplayName(extension), extension.toolCount)", 1),
    # --- 安装/更新对话框：标题 ---
    (SCREEN,
     'title = { Text(if (update == null) "Install extension" else "Update extension") }',
     "title = { Text(if (update == null) stringResource(R.string.settings_extension_install_title) else stringResource(R.string.settings_extension_update_title)) }", 1),
    # --- 安装/更新对话框：说明段落 ---
    (SCREEN,
     'Text(if (update == null) "The ZIP may contain manifest.json directly or inside one enclosing folder." '
     'else "The extension\'s ID and stored data are preserved. The previous package is restored if loading fails.")',
     "Text(if (update == null) stringResource(R.string.settings_extension_zip_hint) "
     "else stringResource(R.string.settings_extension_update_hint))", 1),
    # --- 安装/更新对话框：确认按钮 ---
    (SCREEN,
     '{ Text(if (update == null) "Install" else "Update") }',
     "{ Text(if (update == null) stringResource(R.string.common_install) else stringResource(R.string.common_update)) }", 1),
    # --- 数据层：宿主启动相关（lastError 会显示在设置页） ---
    (MANAGER,
     'throw IllegalStateException("Extension manager is not initialized")',
     'throw IllegalStateException(appContext?.getString(R.string.data_err_manager_not_initialized) ?: "Extension manager is not initialized")', 1),
    (MANAGER,
     'throw IllegalStateException("Node.js is required in the local terminal to run browser extensions")',
     'throw IllegalStateException(appContext?.getString(R.string.data_err_node_required) ?: "Node.js is required in the local terminal to run browser extensions")', 1),
    (MANAGER,
     'throw IllegalStateException("Extension host exited; see BrowserExtensionManager logs")',
     'throw IllegalStateException(appContext?.getString(R.string.data_err_extension_host_exited) ?: "Extension host exited; see BrowserExtensionManager logs")', 1),
    (MANAGER,
     'throw IllegalStateException("Extension host did not start in time")',
     'throw IllegalStateException(appContext?.getString(R.string.data_err_extension_host_timeout) ?: "Extension host did not start in time")', 1),
    # --- 数据层：配置读写 ---
    (MANAGER,
     '{ "Cannot create extension directory" }',
     '{ appContext?.getString(R.string.data_err_create_extension_dir) ?: "Cannot create extension directory" }', 1),
    (MANAGER,
     '{ "Cannot save extension settings" }',
     '{ appContext?.getString(R.string.data_err_save_extension_settings) ?: "Cannot save extension settings" }', 1),
    # --- 数据层：包校验 ---
    (MANAGER,
     '{ "Use 1–64 letters, numbers, dots, underscores or hyphens for the folder name" }',
     '{ appContext?.getString(R.string.data_err_folder_name_rule) ?: "Use 1–64 letters, numbers, dots, underscores or hyphens for the folder name" }', 1),
    (MANAGER,
     '{ "The folder name runtime is reserved" }',
     '{ appContext?.getString(R.string.data_err_folder_name_reserved) ?: "The folder name runtime is reserved" }', 1),
    (MANAGER,
     '{ "Only manifest versions 2 and 3 are supported" }',
     '{ appContext?.getString(R.string.data_err_manifest_version) ?: "Only manifest versions 2 and 3 are supported" }', 1),
    (MANAGER,
     '{ "Extension name is required" }',
     '{ appContext?.getString(R.string.data_err_extension_name_required) ?: "Extension name is required" }', 1),
    (MANAGER,
     'error("manifest.json must declare mcp_tools")',
     'error(appContext?.getString(R.string.data_err_manifest_tools_required) ?: "manifest.json must declare mcp_tools")', 1),
    (MANAGER,
     '{ "Extension must declare at least one MCP tool" }',
     '{ appContext?.getString(R.string.data_err_extension_tools_required) ?: "Extension must declare at least one MCP tool" }', 1),
    (MANAGER,
     '{ "Duplicate or empty MCP tool name" }',
     '{ appContext?.getString(R.string.data_err_tool_name_duplicate) ?: "Duplicate or empty MCP tool name" }', 1),
    (MANAGER,
     'error("Extension background worker is required")',
     'error(appContext?.getString(R.string.data_err_background_required) ?: "Extension background worker is required")', 1),
    (MANAGER,
     '{ "Extension background worker is required" }',
     '{ appContext?.getString(R.string.data_err_background_required) ?: "Extension background worker is required" }', 1),
    (MANAGER,
     '{ "Missing or invalid background file: $relative" }',
     '{ appContext?.getString(R.string.data_err_background_file_missing, relative) ?: "Missing or invalid background file: $relative" }', 1),
    # --- 数据层：安装 / 更新 / 重命名流程 ---
    (MANAGER,
     'error("Extension manager is not initialized")',
     'error(appContext?.getString(R.string.data_err_manager_not_initialized) ?: "Extension manager is not initialized")', 1),
    (MANAGER,
     'error("Extension not found")',
     'error(appContext?.getString(R.string.data_err_extension_not_found) ?: "Extension not found")', 4),
    (MANAGER,
     'error("Cannot read ZIP")',
     'error(appContext?.getString(R.string.data_err_read_zip) ?: "Cannot read ZIP")', 1),
    (MANAGER,
     '{ "Use Rename folder to change the folder before updating" }',
     '{ appContext?.getString(R.string.data_err_rename_before_update) ?: "Use Rename folder to change the folder before updating" }', 1),
    (MANAGER,
     '{ "Invalid extension ID" }',
     '{ appContext?.getString(R.string.data_err_invalid_extension_id) ?: "Invalid extension ID" }', 1),
    (MANAGER,
     '{ "Extension already exists; use Update ZIP" }',
     '{ appContext?.getString(R.string.data_err_extension_exists) ?: "Extension already exists; use Update ZIP" }', 1),
    (MANAGER,
     '{ "Folder already exists" }',
     '{ appContext?.getString(R.string.data_err_folder_exists) ?: "Folder already exists" }', 2),
    (MANAGER,
     '{ "Folder belongs to another extension" }',
     '{ appContext?.getString(R.string.data_err_folder_belongs_to_other) ?: "Folder belongs to another extension" }', 1),
    (MANAGER,
     '{ "Cannot stage previous extension" }',
     '{ appContext?.getString(R.string.data_err_stage_previous_extension) ?: "Cannot stage previous extension" }', 1),
    (MANAGER,
     '{ "Failed to restore previous extension" }',
     '{ appContext?.getString(R.string.data_err_restore_previous_extension) ?: "Failed to restore previous extension" }', 1),
    (MANAGER,
     '{ "Cannot rename extension folder" }',
     '{ appContext?.getString(R.string.data_err_rename_extension_folder) ?: "Cannot rename extension folder" }', 1),
]

RESOURCES = [
    # 设置页：扩展安装/更新对话框
    ("settings_extension_operation_failed", "Extension operation failed", "扩展操作失败"),
    ("settings_extension_install_title", "Install extension", "安装扩展"),
    ("settings_extension_update_title", "Update extension", "更新扩展"),
    ("settings_extension_zip_hint",
     "The ZIP may contain manifest.json directly or inside one enclosing folder.",
     "ZIP 内可以直接放 manifest.json，也可以放在一层文件夹里。"),
    ("settings_extension_update_hint",
     "The extension's ID and stored data are preserved. The previous package is restored if loading fails.",
     "扩展的 ID 与已存数据都会保留；加载失败时会还原为上一个版本。"),
    # 内置扩展（google-flow）的展示名与描述：manifest 里只有英文，展示时按 id 走资源
    ("settings_extension_google_flow_name", "Google Flow Extension", "Google Flow 扩展"),
    ("settings_extension_google_flow_description",
     "Google Flow MCP workflows, fast verified batch generation and Chrome Flow tool compatibility for AntiGem WebView.",
     "面向 AntiGem WebView 的 Google Flow MCP 工作流、可快速验证的批量生成，以及 Chrome Flow 工具兼容。"),
    # 数据层：宿主启动
    ("data_err_manager_not_initialized", "Extension manager is not initialized", "扩展管理器尚未初始化"),
    ("data_err_node_required",
     "Node.js is required in the local terminal to run browser extensions",
     "运行浏览器扩展需要本地终端里的 Node.js"),
    ("data_err_extension_host_exited",
     "Extension host exited; see BrowserExtensionManager logs",
     "扩展宿主进程已退出，详见 BrowserExtensionManager 日志"),
    ("data_err_extension_host_timeout", "Extension host did not start in time", "扩展宿主未在规定时间内启动"),
    # 数据层：配置读写
    ("data_err_create_extension_dir", "Cannot create extension directory", "无法创建扩展目录"),
    ("data_err_save_extension_settings", "Cannot save extension settings", "无法保存扩展设置"),
    # 数据层：包校验
    ("data_err_folder_name_rule",
     "Use 1–64 letters, numbers, dots, underscores or hyphens for the folder name",
     "文件夹名只能使用 1–64 个字母、数字、点、下划线或连字符"),
    ("data_err_folder_name_reserved", "The folder name runtime is reserved", "runtime 是保留的文件夹名"),
    ("data_err_manifest_version", "Only manifest versions 2 and 3 are supported", "只支持 manifest 版本 2 和 3"),
    ("data_err_extension_name_required", "Extension name is required", "扩展必须填写名称"),
    ("data_err_manifest_tools_required", "manifest.json must declare mcp_tools", "manifest.json 必须声明 mcp_tools"),
    ("data_err_extension_tools_required", "Extension must declare at least one MCP tool",
     "扩展至少要声明一个 MCP 工具"),
    ("data_err_tool_name_duplicate", "Duplicate or empty MCP tool name", "MCP 工具名重复或为空"),
    ("data_err_background_required", "Extension background worker is required", "扩展必须提供后台工作脚本"),
    # 数据层：安装 / 更新 / 重命名
    ("data_err_extension_not_found", "Extension not found", "未找到该扩展"),
    ("data_err_read_zip", "Cannot read ZIP", "无法读取 ZIP"),
    ("data_err_rename_before_update", "Use Rename folder to change the folder before updating",
     "更新前请先用「重命名文件夹」改好文件夹名"),
    ("data_err_invalid_extension_id", "Invalid extension ID", "扩展 ID 无效"),
    ("data_err_extension_exists", "Extension already exists; use Update ZIP",
     "扩展已存在，请改用「更新 ZIP」"),
    ("data_err_folder_exists", "Folder already exists", "文件夹已存在"),
    ("data_err_folder_belongs_to_other", "Folder belongs to another extension", "该文件夹属于其他扩展"),
    ("data_err_stage_previous_extension", "Cannot stage previous extension", "无法暂存上一个扩展"),
    ("data_err_restore_previous_extension", "Failed to restore previous extension", "还原上一个扩展失败"),
    ("data_err_rename_extension_folder", "Cannot rename extension folder", "无法重命名扩展文件夹"),
]

DYNAMIC = {
    "data_err_background_file_missing": {
        "en": "Missing or invalid background file: $relative",
        "zh": "后台文件缺失或无效：%1$s",
        "module": "data",
        "args": ["relative"],
    },
}

# 追加到设置页末尾的展示辅助函数（按扩展 id 取资源，第三方扩展保持 manifest 原文）
APPEND_TO_SCREEN = '''
/** 内置扩展的展示名/描述取资源；第三方扩展仍显示 manifest 里的原文。 */
@Composable
private fun extensionDisplayName(extension: BrowserExtension): String =
    if (extension.id == "google-flow") stringResource(R.string.settings_extension_google_flow_name) else extension.name

@Composable
private fun extensionDisplayDescription(extension: BrowserExtension): String =
    if (extension.id == "google-flow") stringResource(R.string.settings_extension_google_flow_description) else extension.description
'''


def main():
    errors, changed = [], 0
    for rel, old, new, count in SITES:
        path = SRC + rel
        text = open(path, encoding="utf-8").read()
        found = text.count(old)
        if found != count:
            errors.append(f"[{rel}] 期望 {count} 处，实际 {found}：{old[:70]}")
            continue
        if not DRY:
            open(path, "w", encoding="utf-8").write(text.replace(old, new))
        changed += count
        print(f"  {rel}: {count} 处  {old[:56]}")

    # 追加展示辅助函数（幂等）：调用点已由 SITES 写入，这里只补定义
    screen_path = SRC + SCREEN
    screen_text = open(screen_path, encoding="utf-8").read()
    if "private fun extensionDisplayName" in screen_text:
        print(f"  {SCREEN}: 展示辅助函数已存在，跳过")
    else:
        if not DRY:
            open(screen_path, "a", encoding="utf-8").write(APPEND_TO_SCREEN)
        print(f"  {SCREEN}: 追加 extensionDisplayName/extensionDisplayDescription")

    catalog = json.load(open(CAT, encoding="utf-8"))
    keys = {c["key"] for c in catalog}
    ens = {c["en"] for c in catalog}
    added = 0
    for key, en, zh in RESOURCES:
        if key in keys:
            continue
        if en in ens:
            errors.append(f"英文重复: {en}")
            continue
        module = "settings" if key.startswith("settings_") else "data" if key.startswith("data_") else "component"
        catalog.append({"key": key, "module": module, "en": en, "zh": zh,
                        "count": 1, "sites": [], "orphan": False})
        keys.add(key)
        ens.add(en)
        added += 1
    if not DRY:
        json.dump(catalog, open(CAT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)

    dyn = json.load(open(DYN, encoding="utf-8")) if os.path.exists(DYN) else {}
    dyn_added = 0
    for key, info in DYNAMIC.items():
        if key in dyn:
            continue
        dyn[key] = info
        dyn_added += 1
    if not DRY:
        json.dump(dyn, open(DYN, "w", encoding="utf-8"), ensure_ascii=False, indent=1)

    print(f"\n改写 {changed} 处；新增静态资源 {added} 条、插值资源 {dyn_added} 条")
    if errors:
        print("\n错误：")
        for e in errors:
            print("  " + e)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
