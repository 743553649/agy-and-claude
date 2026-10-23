#!/usr/bin/env python3
"""内置浏览器（BrowserScreen）与对话页（ChatScreen）里成对的分支/三元文案。

判据：都进 Text(...) / contentDescription / TextButton 文本，可在组合作用域用 stringResource；
含插值的（诊断日志计数、历史建议数）走 i18n_dynamic.json。

用法：apply_browser_chat.py --dry-run / apply_browser_chat.py → 三步流水线。
"""
import json
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SRC = "app/src/main/java/com/example/gemini/"
CAT = "/tmp/i18n_catalog.json"
DYN = "/tmp/i18n_dynamic.json"
DRY = "--dry-run" in sys.argv

BS = "ui/browser/BrowserScreen.kt"
CS = "ui/chat/ChatScreen.kt"

SITES = [
    # ---- 浏览器 ----
    (BS, 'contentDescription = if (isDesktopMode) "Desktop mode enabled" else "Mobile mode enabled",',
     'contentDescription = if (isDesktopMode) stringResource(R.string.browser_desktop_mode_enabled) else stringResource(R.string.browser_mobile_mode_enabled),', 1),
    (BS, 'text = { Text(if (activeTab?.isDesktopMode == true) "Mobile Site" else "Desktop Site") },',
     'text = { Text(if (activeTab?.isDesktopMode == true) stringResource(R.string.browser_mobile_site) else stringResource(R.string.browser_desktop_site)) },', 1),
    (BS, 'text = if (trimmed.isBlank()) "Recent Searches" else "History Suggestions",',
     'text = if (trimmed.isBlank()) stringResource(R.string.browser_recent_searches) else stringResource(R.string.browser_history_suggestions),', 1),
    (BS, 'isStalled -> "Page Loading Stalled"',
     'isStalled -> stringResource(R.string.browser_error_stalled_title)', 1),
    (BS, '== true -> "SSL Security Warning"',
     '== true -> stringResource(R.string.browser_error_ssl_warning)', 1),
    (BS, 'lastErr?.errorCode == -1 -> "Renderer Process Crashed"',
     'lastErr?.errorCode == -1 -> stringResource(R.string.browser_error_renderer_crashed)', 1),
    (BS, 'else -> "Unable to Load Page"',
     'else -> stringResource(R.string.browser_error_unable_to_load)', 1),
    (BS, 'isStalled -> "The page is taking unusually long to respond (timeout > 15s). The local server or remote site might be unresponsive."',
     'isStalled -> stringResource(R.string.browser_error_stalled_desc)', 1),
    (BS, 'else -> "Could not connect to the server or display the requested URL."',
     'else -> stringResource(R.string.browser_error_connect_desc)', 1),
    (BS, 'text = if (showDetails) "Hide Diagnostic Logs" else "Show Diagnostic Logs (${recentLogs.size})",',
     'text = if (showDetails) stringResource(R.string.browser_hide_diagnostic_logs) else stringResource(R.string.browser_show_diagnostic_logs, recentLogs.size),', 1),
    (BS, 'text = if (busy) "AI is working…" else "Controlled by AI",',
     'text = if (busy) stringResource(R.string.browser_ai_working) else stringResource(R.string.browser_controlled_by_ai),', 1),
    # ---- 对话页 ----
    (CS, 'claudeModelsRefreshing -> "Loading models…"',
     'claudeModelsRefreshing -> stringResource(R.string.chat_models_loading)', 1),
    (CS, 'claudeModelsError != null -> "Models unavailable"',
     'claudeModelsError != null -> stringResource(R.string.chat_models_unavailable)', 1),
    (CS, 'else -> "Choose model"',
     'else -> stringResource(R.string.chat_choose_model)', 1),
    (CS, 'if (isChatStarted) "Outside" else "Select Project"',
     'if (isChatStarted) stringResource(R.string.chat_project_outside) else stringResource(R.string.chat_select_project)', 1),
    (CS, 'contentDescription = if (showUpArrow) "Jump to Top" else "Jump to Bottom",',
     'contentDescription = if (showUpArrow) stringResource(R.string.chat_jump_to_top) else stringResource(R.string.chat_jump_to_bottom),', 1),
    (CS, 'text = if (isReconnecting) "Connecting to Antigravity..." else "Antigravity Disconnected",',
     'text = if (isReconnecting) stringResource(R.string.chat_connecting_antigravity) else stringResource(R.string.chat_antigravity_disconnected),', 1),
    (CS, 'com.example.gemini.data.agent.claude.ClaudeStatus.NOT_INSTALLED -> "Claude Code is not installed. Set it up in Settings."',
     'com.example.gemini.data.agent.claude.ClaudeStatus.NOT_INSTALLED -> stringResource(R.string.chat_claude_not_installed_hint)', 1),
    (CS, 'com.example.gemini.data.agent.claude.ClaudeStatus.BRIDGE_OUTDATED -> "Restart the local server to use Claude Code."',
     'com.example.gemini.data.agent.claude.ClaudeStatus.BRIDGE_OUTDATED -> stringResource(R.string.chat_claude_restart_server_hint)', 1),
    (CS, 'else -> "The local server is offline. Start it to send messages."',
     'else -> stringResource(R.string.chat_claude_server_offline_hint)', 1),
    (CS, 'text = if (action.type == MessageActionType.EDIT) "Edit Message?" else "Regenerate Response?",',
     'text = if (action.type == MessageActionType.EDIT) stringResource(R.string.chat_edit_message_title) else stringResource(R.string.chat_regenerate_response_title),', 1),
    (CS, 'action.type == MessageActionType.EDIT -> "Edit & Delete"',
     'action.type == MessageActionType.EDIT -> stringResource(R.string.chat_edit_and_delete)', 1),
]

RESOURCES = [
    ("browser_desktop_mode_enabled", "Desktop mode enabled", "已启用桌面模式"),
    ("browser_mobile_mode_enabled", "Mobile mode enabled", "已启用移动模式"),
    ("browser_mobile_site", "Mobile Site", "移动版网站"),
    ("browser_desktop_site", "Desktop Site", "桌面版网站"),
    ("browser_recent_searches", "Recent Searches", "最近搜索"),
    ("browser_history_suggestions", "History Suggestions", "历史记录建议"),
    ("browser_error_stalled_title", "Page Loading Stalled", "页面加载停滞"),
    ("browser_error_ssl_warning", "SSL Security Warning", "SSL 安全警告"),
    ("browser_error_renderer_crashed", "Renderer Process Crashed", "渲染进程已崩溃"),
    ("browser_error_unable_to_load", "Unable to Load Page", "无法加载页面"),
    ("browser_error_stalled_desc",
     "The page is taking unusually long to respond (timeout > 15s). The local server or remote site might be unresponsive.",
     "页面响应异常缓慢（超时超过 15 秒）。本地服务器或远端站点可能无响应。"),
    ("browser_error_connect_desc",
     "Could not connect to the server or display the requested URL.",
     "无法连接服务器或显示请求的网址。"),
    ("browser_hide_diagnostic_logs", "Hide Diagnostic Logs", "隐藏诊断日志"),
    ("browser_ai_working", "AI is working…", "AI 正在工作……"),
    ("browser_controlled_by_ai", "Controlled by AI", "由 AI 控制"),
    ("chat_models_loading", "Loading models…", "正在加载模型……"),
    ("chat_models_unavailable", "Models unavailable", "模型不可用"),
    ("chat_choose_model", "Choose model", "选择模型"),
    ("chat_project_outside", "Outside", "外部"),
    ("chat_select_project", "Select Project", "选择项目"),
    ("chat_jump_to_top", "Jump to Top", "跳到顶部"),
    ("chat_jump_to_bottom", "Jump to Bottom", "跳到底部"),
    ("chat_connecting_antigravity", "Connecting to Antigravity...", "正在连接 Antigravity……"),
    ("chat_antigravity_disconnected", "Antigravity Disconnected", "Antigravity 已断开"),
    ("chat_claude_not_installed_hint", "Claude Code is not installed. Set it up in Settings.",
     "未安装 Claude Code，请在设置中完成配置。"),
    ("chat_claude_restart_server_hint", "Restart the local server to use Claude Code.",
     "重启本地服务器以使用 Claude Code。"),
    ("chat_claude_server_offline_hint", "The local server is offline. Start it to send messages.",
     "本地服务器离线，启动后才能发送消息。"),
    ("chat_edit_message_title", "Edit Message?", "编辑消息？"),
    ("chat_regenerate_response_title", "Regenerate Response?", "重新生成回复？"),
    ("chat_edit_and_delete", "Edit & Delete", "编辑并删除"),
]

DYNAMIC = {
    "browser_show_diagnostic_logs": {"en": "Show Diagnostic Logs ($count)", "zh": "显示诊断日志（%1$s）",
                                     "module": "browser", "args": ["count"]},
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
        module = "browser" if key.startswith("browser_") else "chat"
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
