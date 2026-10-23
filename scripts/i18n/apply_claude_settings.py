#!/usr/bin/env python3
"""Claude 设置页（ClaudeSettingsScreen）与 Markdown 错误详情按钮的分支文案。

判据：都在 @Composable 作用域（`val x = when {...}` 或 Text(...) 参数），
可用 stringResource；带插值的走 i18n_dynamic.json。
记忆列表那行原文是「短语（含尾随空格）+ 路径 +（可选）后缀」的三段拼接，
改成显式 `" · "` 与 `" "` 拼接，避免把首尾空格写进资源（Android 会裁剪）。

用法：apply_claude_settings.py --dry-run / apply_claude_settings.py → 三步流水线。
"""
import json
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SRC = "app/src/main/java/com/example/gemini/"
CAT = "/tmp/i18n_catalog.json"
DYN = "/tmp/i18n_dynamic.json"
DRY = "--dry-run" in sys.argv

CS = "ui/claude/ClaudeSettingsScreen.kt"
MD = "ui/components/MarkdownContent.kt"

SITES = [
    # ---- 账号徽标 ----
    (CS, '?: "Signed in"', '?: stringResource(R.string.claude_account_badge_signed_in)', 2),
    (CS, 'ClaudeStatus.SIGNED_OUT -> "Signed out"',
     'ClaudeStatus.SIGNED_OUT -> stringResource(R.string.claude_account_badge_signed_out)', 1),
    # ---- 登录按钮 ----
    (CS, 'Text(if (state == ClaudeStatus.SIGNING_IN) "Signing in…" else "Sign in")',
     'Text(if (state == ClaudeStatus.SIGNING_IN) stringResource(R.string.claude_signing_in_short) else stringResource(R.string.claude_status_action_sign_in))', 1),
    # ---- 沙箱状态 ----
    (CS, 'sandbox == null -> "Checking whether the sandbox can run on this device…"',
     'sandbox == null -> stringResource(R.string.claude_sandbox_checking)', 1),
    (CS, 'else -> "Not available here: ${sandbox?.reason ?: "unknown reason"}. Auto mode already asks rarely."',
     'else -> stringResource(R.string.claude_sandbox_unavailable, sandbox?.reason ?: stringResource(R.string.claude_sandbox_unknown_reason))', 1),
    # ---- MCP 服务器作用域提示 ----
    (CS, 'Hint(if (cwd != null) "Includes servers of the open project ($cwd)." else "Open a project to also see its project-scoped servers.")',
     'Hint(if (cwd != null) stringResource(R.string.claude_mcp_includes_project, cwd) else stringResource(R.string.claude_mcp_open_project_hint))', 1),
    (CS, 'Hint(if (op.startsWith("Connecting")) "Added · checking that it starts and listing its tools…" else op)',
     'Hint(if (op.startsWith("Connecting")) stringResource(R.string.claude_mcp_added_checking) else op)', 1),
    (CS, 'else "URL: $target"', 'else stringResource(R.string.claude_mcp_server_url, target)', 1),
    # ---- 添加 MCP 服务器对话框标题 ----
    (CS, 'Text(if (initial.name.isBlank()) "Add MCP server" else "Add ${initial.name}", fontWeight = FontWeight.Bold)',
     'Text(if (initial.name.isBlank()) stringResource(R.string.claude_mcp_add_title) else stringResource(R.string.claude_mcp_add_named, initial.name), fontWeight = FontWeight.Bold)', 1),
    # ---- 插件计数 ----
    (CS, 'Hint(if (searching) "Searching…" else "${results.size} plugin(s)")',
     'Hint(if (searching) stringResource(R.string.claude_plugin_searching) else stringResource(R.string.claude_plugin_count, results.size))', 1),
    # ---- 记忆列表作用域 ----
    (CS, '''(if (scope == "user") "Used in every project · " else "Used only in $projectName · ") +
                    m.path + if (!m.exists) " (created when you save)" else ""''',
     '''(if (scope == "user") stringResource(R.string.claude_memory_scope_every) else stringResource(R.string.claude_memory_scope_project, projectName)) +
                    " · " + m.path + if (!m.exists) " " + stringResource(R.string.claude_memory_created_on_save) else ""''', 1),
    # ---- Markdown 错误详情按钮 ----
    (MD, '"Hide Error Details & Stack Trace"',
     'stringResource(R.string.component_md_hide_error_details)', 1),
    (MD, '"View Error Details & Stack Trace"',
     'stringResource(R.string.component_md_view_error_details)', 1),
]

RESOURCES = [
    ("claude_account_badge_signed_in", "Signed in", "已登录"),
    ("claude_account_badge_signed_out", "Signed out", "未登录"),
    ("claude_signing_in_short", "Signing in…", "正在登录……"),
    ("claude_sandbox_checking", "Checking whether the sandbox can run on this device…",
     "正在检查此设备能否运行沙箱……"),
    ("claude_sandbox_unknown_reason", "unknown reason", "原因未知"),
    ("claude_mcp_open_project_hint", "Open a project to also see its project-scoped servers.",
     "打开一个项目即可看到它作用域内的服务器。"),
    ("claude_mcp_added_checking", "Added · checking that it starts and listing its tools…",
     "已添加 · 正在确认能否启动并列出工具……"),
    ("claude_mcp_add_title", "Add MCP server", "添加 MCP 服务器"),
    ("claude_plugin_searching", "Searching…", "正在搜索……"),
    ("claude_memory_scope_every", "Used in every project", "在所有项目中使用"),
    ("claude_memory_created_on_save", "(created when you save)", "（保存时创建）"),
    ("component_md_hide_error_details", "Hide Error Details & Stack Trace", "收起错误详情与堆栈"),
    ("component_md_view_error_details", "View Error Details & Stack Trace", "查看错误详情与堆栈"),
]

DYNAMIC = {
    "claude_sandbox_unavailable": {
        "en": "Not available here: $reason. Auto mode already asks rarely.",
        "zh": "此设备不可用：%1$s。自动模式本来就很少询问。",
        "module": "claude", "args": ["reason"]},
    "claude_mcp_includes_project": {
        "en": "Includes servers of the open project ($cwd).",
        "zh": "包含当前打开项目（%1$s）的服务器。",
        "module": "claude", "args": ["cwd"]},
    "claude_mcp_server_url": {"en": "URL: $target", "zh": "URL：%1$s",
                              "module": "claude", "args": ["target"]},
    "claude_mcp_add_named": {"en": "Add $name", "zh": "添加 %1$s",
                             "module": "claude", "args": ["name"]},
    "claude_plugin_count": {"en": "$count plugin(s)", "zh": "%1$s 个插件",
                            "module": "claude", "args": ["count"]},
    "claude_memory_scope_project": {"en": "Used only in $projectName", "zh": "仅在 %1$s 中使用",
                                    "module": "claude", "args": ["projectName"]},
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
        module = "component" if key.startswith("component_") else "claude"
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
