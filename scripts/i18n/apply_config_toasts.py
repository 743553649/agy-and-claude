#!/usr/bin/env python3
"""ClaudeConfigManager 的用户可见 Toast（插件 / MCP / CLAUDE.md / CLI 任务）。

渲染路径已核实：`ClaudeSettingsScreen.kt:163` `deps.config.messages.collect { Toast.makeText(context, it, ...) }`，
所以这些字符串确实是给用户看的，不是协议值。

`$action` / `$done` 这类是协议动作名（`install` / `marketplace-add` …），中文界面要映射成
动词（安装 / 添加插件市场…），所以新增 `actionLabel()` 映射函数；未识别的动作原样回退。

用法：apply_config_toasts.py --dry-run / apply_config_toasts.py → 三步流水线。
"""
import json
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SRC = "app/src/main/java/com/example/gemini/"
CAT = "/tmp/i18n_catalog.json"
DYN = "/tmp/i18n_dynamic.json"
DRY = "--dry-run" in sys.argv

CM = "data/agent/claude/ClaudeConfigManager.kt"
VM = "ui/chat/ChatViewModel.kt"

SITES = [
    # ---- 注入 Context ----
    (CM, '''class ClaudeConfigManager(
    private val scope: CoroutineScope,
    private val client: ClaudeBridgeClient
) {''',
     '''class ClaudeConfigManager(
    private val scope: CoroutineScope,
    private val client: ClaudeBridgeClient,
    private val appContext: Context
) {''', 1),
    (CM, 'import android.content.Context\n', 'import android.content.Context\n', 0),  # 占位：下面单独处理导入
    # ---- 保存 / 删除 ----
    (CM, '_messages.tryEmit("Save failed: ${it.error}")',
     '_messages.tryEmit(appContext.getString(R.string.claude_msg_save_failed, it.error))', 2),
    (CM, '_messages.tryEmit("Save failed: ${it.message}")',
     '_messages.tryEmit(appContext.getString(R.string.claude_msg_save_failed, it.message))', 2),
    (CM, '_messages.tryEmit("Could not read CLAUDE.md: ${it.message}")',
     '_messages.tryEmit(appContext.getString(R.string.claude_msg_read_md_failed, it.message))', 1),
    (CM, '_messages.tryEmit("CLAUDE.md saved")',
     '_messages.tryEmit(appContext.getString(R.string.claude_msg_md_saved))', 1),
    (CM, '_messages.tryEmit("Deleted $name")',
     '_messages.tryEmit(appContext.getString(R.string.claude_msg_deleted, name))', 1),
    (CM, '_messages.tryEmit("Delete failed: ${it.error}")',
     '_messages.tryEmit(appContext.getString(R.string.claude_msg_delete_failed, it.error))', 1),
    (CM, '_messages.tryEmit("Delete failed: ${it.message}")',
     '_messages.tryEmit(appContext.getString(R.string.claude_msg_delete_failed, it.message))', 1),
    # ---- MCP 服务器 ----
    (CM, '_messages.tryEmit("Could not add \'${req.name}\': $error")',
     '_messages.tryEmit(appContext.getString(R.string.claude_msg_add_failed, req.name, error))', 1),
    (CM, '''                when (added?.status) {
                    "connected" -> "'${req.name}' connected · ${added.tools.size} tools"
                    "failed" -> "'${req.name}' was added but failed to connect"
                    else -> "Added '${req.name}'"
                }''',
     '''                when (added?.status) {
                    "connected" -> appContext.getString(R.string.claude_msg_connected_tools, req.name, added.tools.size)
                    "failed" -> appContext.getString(R.string.claude_msg_added_no_connect, req.name)
                    else -> appContext.getString(R.string.claude_msg_added, req.name)
                }''', 1),
    (CM, '_messages.tryEmit("Removed \'$name\'")',
     '_messages.tryEmit(appContext.getString(R.string.claude_msg_removed, name))', 1),
    (CM, '_messages.tryEmit(it.error ?: "Remove failed")',
     '_messages.tryEmit(it.error ?: appContext.getString(R.string.claude_msg_remove_failed))', 1),
    (CM, '_messages.tryEmit("Remove failed: ${it.message}")',
     '_messages.tryEmit(appContext.getString(R.string.claude_msg_remove_failed_detail, it.message))', 1),
    (CM, '_messages.tryEmit("MCP status failed: ${it.error}")',
     '_messages.tryEmit(appContext.getString(R.string.claude_msg_mcp_status_failed, it.error))', 1),
    (CM, '_messages.tryEmit("MCP status failed: ${it.message}")',
     '_messages.tryEmit(appContext.getString(R.string.claude_msg_mcp_status_failed, it.message))', 1),
    (CM, '_messages.tryEmit("Plugin list failed: ${it.message}")',
     '_messages.tryEmit(appContext.getString(R.string.claude_msg_plugin_list_failed, it.message))', 1),
    # ---- 插件动作 ----
    (CM, 'if (it.success) _messages.tryEmit("${pluginActionDone(action)} $label")',
     'if (it.success) _messages.tryEmit(appContext.getString(R.string.claude_msg_plugin_done, pluginActionDone(action), label))', 1),
    (CM, 'else _messages.tryEmit("$label: " + (it.error?.lines()?.lastOrNull { l -> l.isNotBlank() } ?: "$action failed"))',
     'else _messages.tryEmit(appContext.getString(R.string.claude_msg_label_detail, label, it.error?.lines()?.lastOrNull { l -> l.isNotBlank() } ?: appContext.getString(R.string.claude_msg_action_failed, actionLabel(action))))', 1),
    (CM, '.onFailure { _messages.tryEmit("$label: $action failed: ${it.message}") }',
     '.onFailure { _messages.tryEmit(appContext.getString(R.string.claude_msg_action_failed_detail, label, actionLabel(action), it.message)) }', 1),
    (CM, '''    private fun pluginActionDone(action: String) = when (action) {
        "install" -> "Installed"
        "uninstall" -> "Uninstalled"
        "enable" -> "Enabled"
        "disable" -> "Disabled"
        "update" -> "Updated"
        "marketplace-add" -> "Added marketplace"
        "marketplace-remove" -> "Removed marketplace"
        "marketplace-update" -> "Updated marketplace"
        else -> "Done:"
    }''',
     '''    private fun pluginActionDone(action: String) = when (action) {
        "install" -> appContext.getString(R.string.common_installed)
        "uninstall" -> appContext.getString(R.string.claude_plugin_done_uninstalled)
        "enable" -> appContext.getString(R.string.claude_plugin_done_enabled)
        "disable" -> appContext.getString(R.string.settings_ui_disabled)
        "update" -> appContext.getString(R.string.claude_plugin_done_updated)
        "marketplace-add" -> appContext.getString(R.string.claude_plugin_done_market_added)
        "marketplace-remove" -> appContext.getString(R.string.claude_plugin_done_market_removed)
        "marketplace-update" -> appContext.getString(R.string.claude_plugin_done_market_updated)
        else -> appContext.getString(R.string.claude_plugin_done_other)
    }

    /** 协议动作名 -> 中文动词；未识别的原样返回（技术值）。 */
    private fun actionLabel(action: String) = when (action) {
        "install" -> appContext.getString(R.string.claude_action_install)
        "uninstall" -> appContext.getString(R.string.claude_action_uninstall)
        "update" -> appContext.getString(R.string.claude_action_update)
        "enable" -> appContext.getString(R.string.claude_action_enable)
        "disable" -> appContext.getString(R.string.claude_action_disable)
        else -> action
    }''', 1),
    # ---- CLI 任务 ----
    (CM, '_messages.tryEmit(resp.error ?: "$action failed to start")',
     '_messages.tryEmit(resp.error ?: appContext.getString(R.string.claude_msg_action_failed_start, actionLabel(action)))', 1),
    (CM, '_messages.tryEmit(if (job.exit_code == 0) "Claude Code $action finished" else "Claude Code $action failed (exit ${job.exit_code})")',
     '_messages.tryEmit(if (job.exit_code == 0) appContext.getString(R.string.claude_msg_cli_finished, actionLabel(action)) else appContext.getString(R.string.claude_msg_cli_failed, actionLabel(action), job.exit_code))', 1),
    (CM, '.onFailure { _messages.tryEmit("$action failed: ${it.message}") }',
     '.onFailure { _messages.tryEmit(appContext.getString(R.string.claude_msg_action_detail, actionLabel(action), it.message)) }', 1),
    # ---- 调用点 ----
    (VM, 'com.example.gemini.data.agent.claude.ClaudeConfigManager(viewModelScope, claudeClient)',
     'com.example.gemini.data.agent.claude.ClaudeConfigManager(viewModelScope, claudeClient, application)', 1),
]

RESOURCES = [
    ("claude_msg_md_saved", "CLAUDE.md saved", "已保存 CLAUDE.md"),
    ("claude_msg_remove_failed", "Remove failed", "移除失败"),
    ("claude_plugin_done_uninstalled", "Uninstalled", "已卸载"),
    ("claude_plugin_done_enabled", "Enabled", "已启用"),
    ("claude_plugin_done_updated", "Updated", "已更新"),
    ("claude_plugin_done_market_added", "Added marketplace", "已添加插件市场"),
    ("claude_plugin_done_market_removed", "Removed marketplace", "已移除插件市场"),
    ("claude_plugin_done_market_updated", "Updated marketplace", "已更新插件市场"),
    ("claude_plugin_done_other", "Done:", "完成："),
    ("claude_action_install", "install", "安装"),
    ("claude_action_uninstall", "uninstall", "卸载"),
    ("claude_action_update", "update", "更新"),
    ("claude_action_enable", "enable", "启用"),
    ("claude_action_disable", "disable", "禁用"),
]

DYNAMIC = {
    "claude_msg_save_failed": {"en": "Save failed: $reason", "zh": "保存失败：%1$s",
                               "module": "claude", "args": ["reason"]},
    "claude_msg_read_md_failed": {"en": "Could not read CLAUDE.md: $error",
                                  "zh": "读取 CLAUDE.md 失败：%1$s",
                                  "module": "claude", "args": ["error"]},
    "claude_msg_deleted": {"en": "Deleted $name", "zh": "已删除 %1$s",
                           "module": "claude", "args": ["name"]},
    "claude_msg_delete_failed": {"en": "Delete failed: $reason", "zh": "删除失败：%1$s",
                                 "module": "claude", "args": ["reason"]},
    "claude_msg_add_failed": {"en": "Could not add '$name': $error",
                              "zh": "无法添加「%1$s」：%2$s",
                              "module": "claude", "args": ["name", "error"]},
    "claude_msg_connected_tools": {"en": "'$name' connected · $count tools",
                                   "zh": "「%1$s」已连接 · %2$s 个工具",
                                   "module": "claude", "args": ["name", "count"]},
    "claude_msg_added_no_connect": {"en": "'$name' was added but failed to connect",
                                    "zh": "「%1$s」已添加，但连接失败",
                                    "module": "claude", "args": ["name"]},
    "claude_msg_added": {"en": "Added '$name'", "zh": "已添加「%1$s」",
                         "module": "claude", "args": ["name"]},
    "claude_msg_removed": {"en": "Removed '$name'", "zh": "已移除「%1$s」",
                           "module": "claude", "args": ["name"]},
    "claude_msg_remove_failed_detail": {"en": "Remove failed: $error",
                                        "zh": "移除失败：%1$s",
                                        "module": "claude", "args": ["error"]},
    "claude_msg_mcp_status_failed": {"en": "MCP status failed: $error",
                                     "zh": "获取 MCP 状态失败：%1$s",
                                     "module": "claude", "args": ["error"]},
    "claude_msg_plugin_list_failed": {"en": "Plugin list failed: $error",
                                      "zh": "获取插件列表失败：%1$s",
                                      "module": "claude", "args": ["error"]},
    "claude_msg_plugin_done": {"en": "$done $name", "zh": "%1$s %2$s",
                               "module": "claude", "args": ["done", "name"]},
    "claude_msg_label_detail": {"en": "$label: $error", "zh": "%1$s：%2$s",
                                "module": "claude", "args": ["label", "error"]},
    "claude_msg_action_failed": {"en": "$action failed", "zh": "%1$s失败",
                                 "module": "claude", "args": ["action"]},
    "claude_msg_action_failed_detail": {"en": "$label: $action failed: $error",
                                        "zh": "%1$s：%2$s失败：%3$s",
                                        "module": "claude", "args": ["label", "action", "error"]},
    "claude_msg_action_failed_start": {"en": "$action failed to start",
                                       "zh": "%1$s失败，无法启动",
                                       "module": "claude", "args": ["action"]},
    "claude_msg_cli_finished": {"en": "Claude Code $action finished",
                                "zh": "Claude Code %1$s已完成",
                                "module": "claude", "args": ["action"]},
    "claude_msg_cli_failed": {"en": "Claude Code $action failed (exit $code)",
                              "zh": "Claude Code %1$s失败（退出码：%2$s）",
                              "module": "claude", "args": ["action", "code"]},
    "claude_msg_action_detail": {"en": "$action failed: $error",
                                 "zh": "%1$s失败：%2$s",
                                 "module": "claude", "args": ["action", "error"]},
}


def main():
    errors, changed = [], 0
    files = {}
    for rel, old, new, count in SITES:
        if count == 0:
            continue
        path = SRC + rel
        text = files.get(path) or open(path, encoding="utf-8").read()
        found = text.count(old)
        if found != count:
            errors.append(f"[{rel}] 期望 {count} 处，实际 {found}：{old[:70]}")
            files[path] = text
            continue
        files[path] = text.replace(old, new)
        changed += count

    # Context 导入（按需补，先查重——上次就是这里重复导入把 CI 打红的）
    cm_path = SRC + CM
    cm = files.get(cm_path) or open(cm_path, encoding="utf-8").read()
    if "import android.content.Context" not in cm:
        lines = cm.split("\n")
        for i, line in enumerate(lines):
            if line.startswith("import "):
                lines.insert(i, "import android.content.Context")
                break
        cm = "\n".join(lines)
        files[cm_path] = cm
        print("已补 android.content.Context 导入（原本没有）")
    else:
        files.setdefault(cm_path, cm)
        print("Context 导入已存在，跳过")

    if not DRY and not errors:
        for path, text in files.items():
            open(path, "w", encoding="utf-8").write(text)
    print(f"改写 {changed} 处")

    catalog = json.load(open(CAT, encoding="utf-8"))
    keys = {c["key"] for c in catalog}
    ens = {c["en"] for c in catalog}
    added = 0
    for key, en, zh in RESOURCES:
        if key in keys:
            continue
        if en in ens:
            errors.append(f"英文重复（应复用既有 key）: {en}")
            continue
        catalog.append({"key": key, "module": "claude", "en": en, "zh": zh,
                        "count": 1, "sites": [], "orphan": False})
        keys.add(key); ens.add(en); added += 1

    dyn = json.load(open(DYN, encoding="utf-8")) if os.path.exists(DYN) else {}
    dyn_added = 0
    for key, info in DYNAMIC.items():
        if key in dyn:
            continue
        dyn[key] = info
        dyn_added += 1

    if not DRY:
        json.dump(catalog, open(CAT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
        json.dump(dyn, open(DYN, "w", encoding="utf-8"), ensure_ascii=False, indent=1)

    print(f"新增静态资源 {added} 条、插值资源 {dyn_added} 条")
    if errors:
        print("\n错误：")
        for e in errors:
            print("  " + e)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
