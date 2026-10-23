#!/usr/bin/env python3
"""命令与权限区（安全预设 / 默认批准范围 / 规则徽标与筛选）+ 保存提示 + 服务通知。

用户实机反馈的漏项：安全预设四张卡、项目覆盖与继承、默认批准范围、
「保存服务器与安全设置」后的提示、通知弹窗的 Exit/Float 等。
这些写在 `listOf(...)` / `Triple(...)` 实参里，按语句扫描抓不到，逐条列出。

用法：apply_cmd_perm.py --dry-run / apply_cmd_perm.py → 三步流水线。
"""
import json
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SRC = "app/src/main/java/com/example/gemini/"
CAT = "/tmp/i18n_catalog.json"
DYN = "/tmp/i18n_dynamic.json"
DRY = "--dry-run" in sys.argv

SD = "ui/settings/SettingsDialog.kt"
TS = "data/service/TermuxService.kt"

SITES = [
    # ---- 安全预设四张卡 ----
    (SD, '"🛡️ Default Mode"', 'stringResource(R.string.settings_preset_default_title)', 1),
    (SD, '"Terminal commands require confirmation. Files outside workspace require confirmation."',
     'stringResource(R.string.settings_preset_default_desc)', 1),
    (SD, '("Recommended" to Color(0xFF00ACC1))',
     '(stringResource(R.string.settings_ui_recommended) to Color(0xFF00ACC1))', 1),
    (SD, '"💻 Full Machine Mode"', 'stringResource(R.string.settings_preset_full_title)', 1),
    (SD, '"Terminal commands require confirmation. Full read/write access to all files on device."',
     'stringResource(R.string.settings_preset_full_desc)', 1),
    (SD, '("Open Files" to Color(0xFFF59E0B))',
     '(stringResource(R.string.settings_preset_full_badge) to Color(0xFFF59E0B))', 1),
    (SD, '"⚡ Turbo Mode"', 'stringResource(R.string.settings_preset_turbo_title)', 1),
    (SD, '"Full auto-run: terminal commands execute immediately and all files can be accessed."',
     'stringResource(R.string.settings_preset_turbo_desc)', 1),
    (SD, '("Full Auto" to QuotaGreen)', '(stringResource(R.string.settings_preset_turbo_badge) to QuotaGreen)', 1),
    (SD, '"⚙️ Custom Granular Mode"', 'stringResource(R.string.settings_preset_custom_title)', 1),
    (SD, '"Configure terminal command execution and external filesystem permissions separately."',
     'stringResource(R.string.settings_preset_custom_desc)', 1),
    (SD, '("Custom" to Color(0xFF8B5CF6))',
     '(stringResource(R.string.settings_preset_custom_badge) to Color(0xFF8B5CF6))', 1),
    # ---- 默认批准范围三个卡 ----
    (SD, '''                Triple("PERMISSION_SCOPE_ONCE", "Run Once", "Single call"),
                Triple("PERMISSION_SCOPE_CONVERSATION", "This Chat", "Current session"),
                Triple("PERMISSION_SCOPE_WORKSPACE", "Workspace", "Always")''',
     '''                Triple("PERMISSION_SCOPE_ONCE", stringResource(R.string.settings_scope_once), stringResource(R.string.settings_scope_once_sub)),
                Triple("PERMISSION_SCOPE_CONVERSATION", stringResource(R.string.settings_scope_chat), stringResource(R.string.settings_scope_chat_sub)),
                Triple("PERMISSION_SCOPE_WORKSPACE", stringResource(R.string.chat_workspace_fallback), stringResource(R.string.settings_scope_workspace_sub))''', 1),
    # ---- 规则决策徽标 ----
    (SD, '''                                    Triple("ALLOW", "✅ Allow", QuotaGreen),
                                    Triple("DENY", "🚫 Deny", MaterialTheme.colorScheme.error),
                                    Triple("ASK", "❓ Ask", Color(0xFFF59E0B))''',
     '''                                    Triple("ALLOW", stringResource(R.string.settings_allow), QuotaGreen),
                                    Triple("DENY", stringResource(R.string.settings_deny), MaterialTheme.colorScheme.error),
                                    Triple("ASK", stringResource(R.string.settings_rule_dec_ask), Color(0xFFF59E0B))''', 1),
    # ---- 规则筛选 chip ----
    (SD, '''                val filters = listOf(
                    "ALL" to "All (${allRules.size})",
                    "command" to "⌨️ Command (${allRules.count { it.action.equals("command", ignoreCase = true) }})",
                    "mcp" to "🔌 MCP (${allRules.count { it.action.equals("mcp", ignoreCase = true) }})",
                    "read_file" to "📖 Read (${allRules.count { it.action.equals("read_file", ignoreCase = true) }})",
                    "write_file" to "✏️ Write (${allRules.count { it.action.equals("write_file", ignoreCase = true) }})"
                )''',
     '''                val filters = listOf(
                    "ALL" to stringResource(R.string.settings_filter_all, allRules.size),
                    "command" to stringResource(R.string.settings_filter_command, allRules.count { it.action.equals("command", ignoreCase = true) }),
                    "mcp" to stringResource(R.string.settings_filter_mcp, allRules.count { it.action.equals("mcp", ignoreCase = true) }),
                    "read_file" to stringResource(R.string.settings_filter_read, allRules.count { it.action.equals("read_file", ignoreCase = true) }),
                    "write_file" to stringResource(R.string.settings_filter_write, allRules.count { it.action.equals("write_file", ignoreCase = true) })
                )''', 1),
    # ---- 保存提示 ----
    (SD, 'saveFeedback = "✓ Server, Binaries and Security settings saved!"',
     'saveFeedback = "✓ " + context.getString(R.string.settings_saved_server_binaries)', 1),
    (SD, 'sshSaveFeedback = "✓ SSH settings saved successfully!"',
     'sshSaveFeedback = "✓ " + context.getString(R.string.settings_saved_ssh)', 1),
    # ---- 前台服务通知 ----
    (TS, 'val name = "AntiGem Terminal & Execution Service"',
     'val name = getString(R.string.data_service_channel_name)', 1),
    (TS, 'val descriptionText = "Terminal sessions and background execution"',
     'val descriptionText = getString(R.string.data_service_channel_desc)', 1),
    (TS, 'val sessionText = "$currentSessionCount " + if (currentSessionCount == 1) "session" else "sessions"',
     'val sessionText = getString(R.string.data_service_session_count, currentSessionCount)', 1),
    (TS, 'val notificationText = if (wakeLockHeld) "$sessionText (wake lock held)" else sessionText',
     'val notificationText = if (wakeLockHeld) getString(R.string.data_service_wakelock_held, sessionText) else sessionText', 1),
    (TS, 'val wakeActionTitle = if (wakeLockHeld) "Release wakelock" else "Acquire wakelock"',
     'val wakeActionTitle = if (wakeLockHeld) getString(R.string.data_service_wakelock_release) else getString(R.string.data_service_wakelock_acquire)', 1),
    (TS, 'android.R.drawable.ic_menu_close_clear_cancel, "Exit", exitPendingIntent',
     'android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.data_service_action_exit), exitPendingIntent', 1),
    (TS, 'android.R.drawable.ic_menu_view, "Float", floatPendingIntent',
     'android.R.drawable.ic_menu_view, getString(R.string.data_service_action_float), floatPendingIntent', 1),
]

RESOURCES = [
    ("settings_preset_default_title", "🛡️ Default Mode", "🛡️ 默认模式"),
    ("settings_preset_default_desc",
     "Terminal commands require confirmation. Files outside workspace require confirmation.",
     "终端命令需要确认，工作区外的文件也需要确认。"),
    ("settings_preset_full_title", "💻 Full Machine Mode", "💻 整机模式"),
    ("settings_preset_full_desc",
     "Terminal commands require confirmation. Full read/write access to all files on device.",
     "终端命令需要确认，可读写设备上的所有文件。"),
    ("settings_preset_full_badge", "Open Files", "开放文件"),
    ("settings_preset_turbo_title", "⚡ Turbo Mode", "⚡ 极速模式"),
    ("settings_preset_turbo_desc",
     "Full auto-run: terminal commands execute immediately and all files can be accessed.",
     "全自动：终端命令直接执行，所有文件均可访问。"),
    ("settings_preset_turbo_badge", "Full Auto", "全自动"),
    ("settings_preset_custom_title", "⚙️ Custom Granular Mode", "⚙️ 自定义细粒度模式"),
    ("settings_preset_custom_desc",
     "Configure terminal command execution and external filesystem permissions separately.",
     "分别配置终端命令的执行策略与外部文件系统权限。"),
    ("settings_preset_custom_badge", "Custom", "自定义"),
    ("settings_scope_once", "Run Once", "仅一次"),
    ("settings_scope_once_sub", "Single call", "单次调用"),
    ("settings_scope_chat", "This Chat", "本会话"),
    ("settings_scope_chat_sub", "Current session", "当前会话"),
    ("settings_scope_workspace_sub", "Always", "始终"),
    ("settings_rule_dec_ask", "❓ Ask", "❓ 询问"),
    ("settings_saved_server_binaries", "Server, Binaries and Security settings saved!",
     "服务器、二进制与安全设置已保存！"),
    ("settings_saved_ssh", "SSH settings saved successfully!", "SSH 设置已保存！"),
    ("data_service_channel_name", "AntiGem Terminal & Execution Service", "AntiGem 终端与执行服务"),
    ("data_service_channel_desc", "Terminal sessions and background execution", "终端会话与后台执行"),
    ("data_service_wakelock_release", "Release wakelock", "释放唤醒锁"),
    ("data_service_wakelock_acquire", "Acquire wakelock", "获取唤醒锁"),
    ("data_service_action_exit", "Exit", "退出"),
    ("data_service_action_float", "Float", "浮窗"),
]

DYNAMIC = {
    "settings_filter_all": {"en": "All ($count)", "zh": "全部（%1$s）",
                            "module": "settings", "args": ["count"]},
    "settings_filter_command": {"en": "⌨️ Command ($count)", "zh": "⌨️ 命令（%1$s）",
                                "module": "settings", "args": ["count"]},
    "settings_filter_mcp": {"en": "🔌 MCP ($count)", "zh": "🔌 MCP（%1$s）",
                            "module": "settings", "args": ["count"]},
    "settings_filter_read": {"en": "📖 Read ($count)", "zh": "📖 读（%1$s）",
                             "module": "settings", "args": ["count"]},
    "settings_filter_write": {"en": "✏️ Write ($count)", "zh": "✏️ 写（%1$s）",
                              "module": "settings", "args": ["count"]},
    "data_service_session_count": {"en": "$count session(s)", "zh": "%1$s 个会话",
                                   "module": "data", "args": ["count"]},
    "data_service_wakelock_held": {"en": "$sessions (wake lock held)",
                                   "zh": "%1$s（已持有唤醒锁）",
                                   "module": "data", "args": ["sessions"]},
}


def main():
    errors, changed = [], 0
    files = {}
    for rel, old, new, count in SITES:
        path = SRC + rel
        text = files.get(path) or open(path, encoding="utf-8").read()
        found = text.count(old)
        if found != count:
            errors.append(f"[{rel}] 期望 {count} 处，实际 {found}：{old[:70]}")
            files[path] = text
            continue
        files[path] = text.replace(old, new)
        changed += count
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
        module = "settings" if key.startswith("settings_") else "data"
        catalog.append({"key": key, "module": module, "en": en, "zh": zh,
                        "count": 1, "sites": [], "orphan": False})
        keys.add(key); ens.add(en); added += 1

    dyn = json.load(open(DYN, encoding="utf-8")) if os.path.exists(DYN) else {}
    dyn_added = 0
    for key, info in DYNAMIC.items():
        if key in dyn:
            continue
        dyn[key] = info
        dyn_added += 1

    if errors:
        print("\n错误（未写入）：")
        for e in errors:
            print("  " + e)
        return 1

    if not DRY:
        for path, text in files.items():
            open(path, "w", encoding="utf-8").write(text)
        json.dump(catalog, open(CAT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
        json.dump(dyn, open(DYN, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    print(f"新增静态资源 {added} 条、插值资源 {dyn_added} 条")
    return 0


if __name__ == "__main__":
    sys.exit(main())
