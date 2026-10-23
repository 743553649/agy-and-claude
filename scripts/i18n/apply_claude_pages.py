#!/usr/bin/env python3
"""Claude 设置页剩余英文：权限规则 / 已配置服务器 / 记忆 / 高级 / 插件 / 用量上限。

这些漏在 `Hint(...)` / `SectionTitle(...)` / `listOf(...)` / `Icon(..., "contentDesc")` 里，
按字面量语句扫描扫不到（都是自定义组合函数实参），故本脚本逐条手工列站点。

用法：apply_claude_pages.py --dry-run / apply_claude_pages.py → 三步流水线。
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

SITES = [
    # ---- 账号与用量 ----
    (CS, 'Icon(Icons.Outlined.Refresh, "Refresh")', 'Icon(Icons.Outlined.Refresh, stringResource(R.string.common_refresh))', 3),
    (CS, 'Text(status?.version ?: "Installed", fontSize = 13.sp)',
     'Text(status?.version ?: stringResource(R.string.common_installed), fontSize = 13.sp)', 1),
    (CS, 'auth.subscriptionType?.let { "Plan: ${it.replaceFirstChar { c -> c.uppercase() }}" }',
     'auth.subscriptionType?.let { stringResource(R.string.claude_plan_prefix, it.replaceFirstChar { c -> c.uppercase() }) }', 1),
    (CS, 'auth.authMethod?.let { "via $it" }',
     'auth.authMethod?.let { stringResource(R.string.claude_via_method, it) }', 1),
    (CS, 'Hint("Sign in with your Claude subscription or an Anthropic Console account.")',
     'Hint(stringResource(R.string.claude_sign_in_hint))', 1),
    (CS, 'usageRefreshing -> "Updating…"', 'usageRefreshing -> stringResource(R.string.claude_usage_updating)', 1),
    (CS, 'usageUpdatedAt > 0 -> "Updated " + java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(usageUpdatedAt))',
     'usageUpdatedAt > 0 -> stringResource(R.string.claude_usage_updated_at, java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(usageUpdatedAt)))', 1),
    (CS, 'Icon(Icons.Outlined.Refresh, "Refresh usage")',
     'Icon(Icons.Outlined.Refresh, stringResource(R.string.claude_usage_refresh))', 1),
    (CS, 'Hint("Plan limits are not available for this account (API billing has no 5-hour / weekly limits).")',
     'Hint(stringResource(R.string.claude_usage_limits_unavailable))', 1),
    (CS, 'UsageBar("5-hour limit"', 'UsageBar(stringResource(R.string.claude_limit_five_hour)', 1),
    (CS, 'UsageBar("Weekly limit (all models)"', 'UsageBar(stringResource(R.string.claude_limit_weekly_all)', 1),
    (CS, 'UsageBar("Weekly limit (Opus)"', 'UsageBar(stringResource(R.string.claude_limit_weekly_opus)', 1),
    (CS, 'UsageBar("Weekly limit (Sonnet)"', 'UsageBar(stringResource(R.string.claude_limit_weekly_sonnet)', 1),
    # ---- 设置项 ----
    (CS, 'Hint("These apply to new Claude chats (saved in ~/.claude/settings.json, shared with the terminal CLI).")',
     'Hint(stringResource(R.string.claude_settings_scope_hint))', 1),
    (CS, 'SectionTitle("Default model")', 'SectionTitle(stringResource(R.string.claude_sec_default_model))', 1),
    (CS, '.ifEmpty { listOf("default" to "Default") }',
     '.ifEmpty { listOf("default" to stringResource(R.string.claude_default_value)) }', 1),
    (CS, 'SectionTitle("Default effort")', 'SectionTitle(stringResource(R.string.claude_sec_default_effort))', 1),
    (CS, 'SectionTitle("Thinking")', 'SectionTitle(stringResource(R.string.common_thinking))', 1),
    (CS, 'Hint("Reason before answering")', 'Hint(stringResource(R.string.claude_thinking_hint))', 1),
    (CS, 'SectionTitle("Default permission mode")', 'SectionTitle(stringResource(R.string.claude_sec_default_permission_mode))', 1),
    (CS, 'SectionTitle("Output style")', 'SectionTitle(stringResource(R.string.claude_sec_output_style))', 1),
    (CS, 'Hint("How Claude writes its replies")', 'Hint(stringResource(R.string.claude_output_style_hint))', 1),
    (CS, 'SectionTitle("Dictation language")', 'SectionTitle(stringResource(R.string.claude_sec_dictation_language))', 1),
    (CS, 'Hint("Language code for voice input in Claude chats (e.g. en, es, de, ja)")',
     'Hint(stringResource(R.string.claude_dictation_language_hint))', 1),
    (CS, 'if (settings == null) Hint("Loading settings…")',
     'if (settings == null) Hint(stringResource(R.string.claude_settings_loading))', 1),
    (CS, 'SectionTitle("Sandbox commands")', 'SectionTitle(stringResource(R.string.claude_sec_sandbox))', 1),
    (CS, '''sandbox?.available == true -> "Run Claude's shell commands in an isolated container (no network, limited files). Commands that stay inside it run without asking."''',
     '''sandbox?.available == true -> stringResource(R.string.claude_sandbox_desc_enabled)''', 1),
    # ---- 权限规则 ----
    (CS, 'Hint("Rules use the CLI syntax: Bash(npm test), Bash(git log *), Read(./src/**), Edit(docs/**), WebFetch(domain:github.com), mcp__server__tool. Deny wins over ask, ask wins over allow.")',
     'Hint(stringResource(R.string.claude_rules_syntax_hint))', 1),
    (CS, 'SectionTitle("Add a rule")', 'SectionTitle(stringResource(R.string.claude_sec_add_rule))', 1),
    (CS, 'listOf("allow" to "Allowed without asking", "ask" to "Always ask", "deny" to "Never allowed").forEach { (b, title) ->',
     'listOf("allow" to stringResource(R.string.claude_rules_allowed), "ask" to stringResource(R.string.claude_rules_ask), "deny" to stringResource(R.string.claude_rules_denied)).forEach { (b, title) ->', 1),
    (CS, 'SectionTitle("$title (${rules.size})")',
     'SectionTitle(stringResource(R.string.claude_rules_group_title, title, rules.size))', 1),
    (CS, 'if (rules.isEmpty()) Hint("No rules")', 'if (rules.isEmpty()) Hint(stringResource(R.string.claude_rules_none))', 1),
    # ---- MCP 服务器 ----
    (CS, 'if (servers.isNotEmpty()) Hint("${servers.sumOf { it.tools.size }} tools")',
     'if (servers.isNotEmpty()) Hint(stringResource(R.string.claude_mcp_tools_count, servers.sumOf { it.tools.size }))', 1),
    (CS, 'Hint("Claude Code starts each server to list its tools; this can take a few seconds.")',
     'Hint(stringResource(R.string.claude_mcp_starting_hint))', 1),
    (CS, 'Hint("Add one from the presets above or a custom server to give Claude more tools.")',
     'Hint(stringResource(R.string.claude_mcp_add_hint))', 1),
    (CS, 'srv.status == "connected" -> QuotaGreen to "Connected"',
     'srv.status == "connected" -> QuotaGreen to stringResource(R.string.claude_srv_connected)', 1),
    (CS, 'srv.status == "pending" -> QuotaAmber to "Starting"',
     'srv.status == "pending" -> QuotaAmber to stringResource(R.string.claude_srv_starting)', 1),
    (CS, 'srv.status == "failed" -> QuotaRed to "Failed"',
     'srv.status == "failed" -> QuotaRed to stringResource(R.string.component_failed)', 1),
    (CS, 'srv.status == "needs-auth" -> QuotaAmber to "Needs sign-in"',
     'srv.status == "needs-auth" -> QuotaAmber to stringResource(R.string.claude_srv_needs_sign_in)', 1),
    (CS, 'srv.status == "disabled" -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f) to "Disabled"',
     'srv.status == "disabled" -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f) to stringResource(R.string.settings_ui_disabled)', 1),
    (CS, 'else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f) to srv.status.ifBlank { "Unknown" }.replaceFirstChar { it.uppercase() }',
     'else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f) to srv.status.ifBlank { stringResource(R.string.claude_srv_unknown) }.replaceFirstChar { it.uppercase() }', 1),
    (CS, 'private fun scopeLabel(scope: String) = when (scope) {',
     '@Composable\nprivate fun scopeLabel(scope: String) = when (scope) {', 1),
    (CS, '"user" -> "all projects"', '"user" -> stringResource(R.string.claude_scope_all_projects_lc)', 1),
    (CS, '"local" -> "this project (you)"', '"local" -> stringResource(R.string.claude_scope_this_project_you)', 1),
    (CS, '"project" -> "this project (.mcp.json)"', '"project" -> stringResource(R.string.claude_scope_this_project_mcp_json)', 1),
    (CS, 'listOf("user" to "All projects", "local" to "This project (me)", "project" to "This project (.mcp.json)").forEach { (sc, label) ->',
     'listOf("user" to stringResource(R.string.claude_scope_all_projects), "local" to stringResource(R.string.claude_scope_this_project_me), "project" to stringResource(R.string.claude_scope_this_project_mcp_json_title)).forEach { (sc, label) ->', 1),
    (CS, 'Text(if (open) "Hide" else "Show", fontSize = 11.5.sp',
     'Text(if (open) stringResource(R.string.claude_hide) else stringResource(R.string.claude_show), fontSize = 11.5.sp', 1),
    (CS, 'Icon(Icons.Outlined.Delete, "Remove", tint = QuotaRed.copy(alpha = 0.8f))',
     'Icon(Icons.Outlined.Delete, stringResource(R.string.common_remove), tint = QuotaRed.copy(alpha = 0.8f))', 3),
    # ---- 插件 ----
    (CS, 'listOf("Installed", "Discover", "Marketplaces").forEachIndexed { i, t ->',
     'listOf(stringResource(R.string.common_installed), stringResource(R.string.claude_tab_discover), stringResource(R.string.claude_tab_marketplaces)).forEachIndexed { i, t ->', 1),
    (CS, 'Hint("Loading plugins… (the catalog can take a few seconds)")',
     'Hint(stringResource(R.string.claude_plugins_loading))', 1),
    (CS, 'if (d.plugins.installed.isEmpty()) item { Hint("No plugins installed. Find some in Discover.") }',
     'if (d.plugins.installed.isEmpty()) item { Hint(stringResource(R.string.claude_plugins_empty)) }', 1),
    (CS, 'Hint("A marketplace is a catalog of plugins (a git repo with a marketplace.json). Discover lists the plugins of every marketplace added here; refresh one to get its newest plugins.")',
     'Hint(stringResource(R.string.claude_marketplace_explain))', 1),
    (CS, 'SectionTitle("Add a marketplace")', 'SectionTitle(stringResource(R.string.claude_sec_add_marketplace))', 1),
    (CS, 'Hint("GitHub owner/repo, a git URL, or a URL to marketplace.json")',
     'Hint(stringResource(R.string.claude_marketplace_source_hint))', 1),
    (CS, 'Icon(Icons.Outlined.Refresh, "Refresh catalog")',
     'Icon(Icons.Outlined.Refresh, stringResource(R.string.claude_marketplace_refresh))', 1),
    (CS, '''"install" -> "Installing…"''', '''"install" -> stringResource(R.string.claude_op_installing)''', 1),
    (CS, '''"uninstall" -> "Removing…"''', '''"uninstall" -> stringResource(R.string.claude_op_removing)''', 1),
    (CS, '''"enable" -> "Enabling…"''', '''"enable" -> stringResource(R.string.claude_op_enabling)''', 1),
    (CS, '''"disable" -> "Disabling…"''', '''"disable" -> stringResource(R.string.claude_op_disabling)''', 1),
    (CS, '''"update", "marketplace-update" -> "Updating…"''',
     '''"update", "marketplace-update" -> stringResource(R.string.claude_usage_updating)''', 1),
    (CS, '''"marketplace-remove" -> "Removing…"''',
     '''"marketplace-remove" -> stringResource(R.string.claude_op_removing)''', 1),
    (CS, '''else -> "Working…"''', '''else -> stringResource(R.string.claude_op_working)''', 1),
    # ---- 记忆 ----
    (CS, '?: "Default workspace"', '?: stringResource(R.string.claude_default_workspace)', 1),
    (CS, 'SectionTitle("Instructions (CLAUDE.md)")', 'SectionTitle(stringResource(R.string.claude_sec_instructions))', 1),
    (CS, 'Hint("Rules Claude follows in every chat. New chats read them right away; a chat that is already running reads them the next time its Claude process starts.")',
     'Hint(stringResource(R.string.claude_instructions_hint))', 1),
    (CS, 'SectionTitle("Claude\'s memory · $projectName")',
     'SectionTitle(stringResource(R.string.claude_memory_title, projectName))', 1),
    (CS, 'Hint("Notes Claude saved by itself while working in this project. This is what Claude checks when you ask what it remembers.")',
     'Hint(stringResource(R.string.claude_memory_hint))', 1),
    (CS, 'a.files.isEmpty() -> Hint("No notes yet. Ask Claude to remember something and it saves it here.")',
     'a.files.isEmpty() -> Hint(stringResource(R.string.claude_memory_empty))', 1),
    (CS, 'Icon(Icons.Outlined.Delete, "Delete", tint = QuotaRed.copy(alpha = 0.8f))',
     'Icon(Icons.Outlined.Delete, stringResource(R.string.common_delete), tint = QuotaRed.copy(alpha = 0.8f))', 1),
    # ---- 高级 ----
    (CS, 'Hint("Every Claude Code setting lives here: permissions, hooks, env, model, statusLine… Changes apply to new Claude chats.")',
     'Hint(stringResource(R.string.claude_advanced_hint))', 1),
    (CS, 'private fun AdvancedPage(deps: ClaudeSettingsDeps) {\n    val settings by deps.config.settings.collectAsState()',
     'private fun AdvancedPage(deps: ClaudeSettingsDeps) {\n    val context = androidx.compose.ui.platform.LocalContext.current\n    val settings by deps.config.settings.collectAsState()', 1),
    (CS, 'parseError = "Invalid JSON: ${it.message}"',
     'parseError = context.getString(R.string.claude_json_invalid, it.message)', 1),
    (CS, 'is JsonObject -> deps.config.saveSettings(parsed, "settings.json saved")',
     'is JsonObject -> deps.config.saveSettings(parsed, context.getString(R.string.claude_json_saved))', 1),
    (CS, 'else -> parseError = "settings.json must be a JSON object"',
     'else -> parseError = context.getString(R.string.claude_json_not_object)', 1),
]

RESOURCES = [
    ("claude_sign_in_hint", "Sign in with your Claude subscription or an Anthropic Console account.",
     "使用 Claude 订阅或 Anthropic Console 账号登录。"),
    ("claude_usage_updating", "Updating…", "正在更新……"),
    ("claude_usage_refresh", "Refresh usage", "刷新用量"),
    ("claude_usage_limits_unavailable",
     "Plan limits are not available for this account (API billing has no 5-hour / weekly limits).",
     "此账号没有套餐限额信息（按 API 计费没有 5 小时 / 每周限额）。"),
    ("claude_limit_five_hour", "5-hour limit", "5 小时限额"),
    ("claude_limit_weekly_all", "Weekly limit (all models)", "每周限额（全部模型）"),
    ("claude_limit_weekly_opus", "Weekly limit (Opus)", "每周限额（Opus）"),
    ("claude_limit_weekly_sonnet", "Weekly limit (Sonnet)", "每周限额（Sonnet）"),
    ("claude_settings_scope_hint",
     "These apply to new Claude chats (saved in ~/.claude/settings.json, shared with the terminal CLI).",
     "这些设置只对新的 Claude 会话生效（写入 ~/.claude/settings.json，与终端 CLI 共用）。"),
    ("claude_sec_default_model", "Default model", "默认模型"),
    ("claude_default_value", "Default", "默认"),
    ("claude_sec_default_effort", "Default effort", "默认推理档位"),
    ("claude_thinking_hint", "Reason before answering", "回答前先推理"),
    ("claude_sec_default_permission_mode", "Default permission mode", "默认权限模式"),
    ("claude_sec_output_style", "Output style", "输出风格"),
    ("claude_output_style_hint", "How Claude writes its replies", "Claude 撰写回复的方式"),
    ("claude_sec_dictation_language", "Dictation language", "听写语言"),
    ("claude_dictation_language_hint", "Language code for voice input in Claude chats (e.g. en, es, de, ja)",
     "Claude 会话中语音输入的语言代码（例如 en、es、de、ja）"),
    ("claude_settings_loading", "Loading settings…", "正在载入设置……"),
    ("claude_sec_sandbox", "Sandbox commands", "沙箱执行命令"),
    ("claude_sandbox_desc_enabled",
     "Run Claude's shell commands in an isolated container (no network, limited files). Commands that stay inside it run without asking.",
     "在隔离容器中运行 Claude 的 shell 命令（无网络、仅有限文件）。留在容器内的命令无需询问即可执行。"),
    ("claude_rules_syntax_hint",
     "Rules use the CLI syntax: Bash(npm test), Bash(git log *), Read(./src/**), Edit(docs/**), WebFetch(domain:github.com), mcp__server__tool. Deny wins over ask, ask wins over allow.",
     "规则使用 CLI 语法：Bash(npm test)、Bash(git log *)、Read(./src/**)、Edit(docs/**)、WebFetch(domain:github.com)、mcp__server__tool。拒绝优先于询问，询问优先于允许。"),
    ("claude_sec_add_rule", "Add a rule", "添加规则"),
    ("claude_rules_allowed", "Allowed without asking", "允许，无需询问"),
    ("claude_rules_ask", "Always ask", "总是询问"),
    ("claude_rules_denied", "Never allowed", "永不允许"),
    ("claude_rules_none", "No rules", "暂无规则"),
    ("claude_mcp_starting_hint", "Claude Code starts each server to list its tools; this can take a few seconds.",
     "Claude Code 会逐个启动服务器以列出其工具，可能需要几秒钟。"),
    ("claude_mcp_add_hint", "Add one from the presets above or a custom server to give Claude more tools.",
     "从上方预设中添加一个，或添加自定义服务器，让 Claude 拥有更多工具。"),
    ("claude_srv_connected", "Connected", "已连接"),
    ("claude_srv_starting", "Starting", "正在启动"),
    ("claude_srv_needs_sign_in", "Needs sign-in", "需要登录"),
    ("claude_srv_unknown", "Unknown", "未知"),
    ("claude_scope_all_projects_lc", "all projects", "全部项目"),
    ("claude_scope_this_project_you", "this project (you)", "本项目（你）"),
    ("claude_scope_this_project_mcp_json", "this project (.mcp.json)", "本项目（.mcp.json）"),
    ("claude_scope_all_projects", "All projects", "全部项目"),
    ("claude_scope_this_project_me", "This project (me)", "本项目（我）"),
    ("claude_scope_this_project_mcp_json_title", "This project (.mcp.json)", "本项目（.mcp.json）"),
    ("claude_hide", "Hide", "收起"),
    ("claude_show", "Show", "展开"),
    ("claude_tab_discover", "Discover", "发现"),
    ("claude_tab_marketplaces", "Marketplaces", "插件市场"),
    ("claude_plugins_loading", "Loading plugins… (the catalog can take a few seconds)",
     "正在加载插件……（目录可能需要几秒钟）"),
    ("claude_plugins_empty", "No plugins installed. Find some in Discover.",
     "尚未安装插件。可以在「发现」里挑一些。"),
    ("claude_marketplace_explain",
     "A marketplace is a catalog of plugins (a git repo with a marketplace.json). Discover lists the plugins of every marketplace added here; refresh one to get its newest plugins.",
     "插件市场就是插件目录（一个带 marketplace.json 的 git 仓库）。「发现」会列出此处添加的所有市场里的插件；刷新某个市场即可获取它的最新插件。"),
    ("claude_sec_add_marketplace", "Add a marketplace", "添加插件市场"),
    ("claude_marketplace_source_hint", "GitHub owner/repo, a git URL, or a URL to marketplace.json",
     "GitHub 的 owner/repo、git 地址，或 marketplace.json 的地址"),
    ("claude_marketplace_refresh", "Refresh catalog", "刷新目录"),
    ("claude_op_installing", "Installing…", "正在安装……"),
    ("claude_op_removing", "Removing…", "正在移除……"),
    ("claude_op_enabling", "Enabling…", "正在启用……"),
    ("claude_op_disabling", "Disabling…", "正在停用……"),
    ("claude_op_working", "Working…", "处理中……"),
    ("claude_default_workspace", "Default workspace", "默认工作区"),
    ("claude_sec_instructions", "Instructions (CLAUDE.md)", "指令（CLAUDE.md）"),
    ("claude_instructions_hint",
     "Rules Claude follows in every chat. New chats read them right away; a chat that is already running reads them the next time its Claude process starts.",
     "Claude 在每个会话中都遵循的规则。新会话会立即读取；已在运行的会话会在其 Claude 进程下次启动时读取。"),
    ("claude_memory_hint",
     "Notes Claude saved by itself while working in this project. This is what Claude checks when you ask what it remembers.",
     "Claude 在本项目工作时自行保存的记录。当你问它记得什么时，它查的就是这里。"),
    ("claude_memory_empty", "No notes yet. Ask Claude to remember something and it saves it here.",
     "还没有记录。让 Claude 记住某些内容，它就会保存到这里。"),
    ("claude_advanced_hint",
     "Every Claude Code setting lives here: permissions, hooks, env, model, statusLine… Changes apply to new Claude chats.",
     "所有 Claude Code 设置都在这里：权限、钩子、环境变量、模型、statusLine……改动对新会话生效。"),
    ("claude_json_saved", "settings.json saved", "已保存 settings.json"),
    ("claude_json_not_object", "settings.json must be a JSON object", "settings.json 必须是一个 JSON 对象"),
]

DYNAMIC = {
    "claude_plan_prefix": {"en": "Plan: $plan", "zh": "套餐：%1$s",
                           "module": "claude", "args": ["plan"]},
    "claude_via_method": {"en": "via $method", "zh": "经由 %1$s",
                          "module": "claude", "args": ["method"]},
    "claude_usage_updated_at": {"en": "Updated $time", "zh": "更新于 %1$s",
                                "module": "claude", "args": ["time"]},
    "claude_rules_group_title": {"en": "$title ($count)", "zh": "%1$s（%2$s）",
                                 "module": "claude", "args": ["title", "count"]},
    "claude_mcp_tools_count": {"en": "$count tools", "zh": "%1$s 个工具",
                               "module": "claude", "args": ["count"]},
    "claude_memory_title": {"en": "Claude's memory · $project", "zh": "Claude 的记忆 · %1$s",
                            "module": "claude", "args": ["project"]},
    "claude_json_invalid": {"en": "Invalid JSON: $error", "zh": "JSON 无效：%1$s",
                            "module": "claude", "args": ["error"]},
}


def main():
    errors, changed = [], 0
    path = SRC + CS
    text = open(path, encoding="utf-8").read()
    for _rel, old, new, count in SITES:
        found = text.count(old)
        if found != count:
            errors.append(f"期望 {count} 处，实际 {found}：{old[:70]}")
            continue
        text = text.replace(old, new)
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

    if errors:
        print("\n错误（未写入任何文件）：")
        for e in errors:
            print("  " + e)
        return 1

    if not DRY:
        open(path, "w", encoding="utf-8").write(text)
        json.dump(catalog, open(CAT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
        json.dump(dyn, open(DYN, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    print(f"新增静态资源 {added} 条、插值资源 {dyn_added} 条")
    return 0


if __name__ == "__main__":
    sys.exit(main())
