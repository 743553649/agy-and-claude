#!/usr/bin/env python3
"""P2 收尾批：坑 19 同族（subtitle/badgeText/description）+ 零散可见残留 + 打开/保存提示。

审批面板（ApprovalItem 模型）另见 apply_p2_approvals.py。
用法：apply_p2_final.py --dry-run / apply_p2_final.py，然后 gen_resources -> fix_imports -> verify。
"""
import json
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SRC = "app/src/main/java/com/example/gemini/"
CAT = "/tmp/i18n_catalog.json"
DYN = "/tmp/i18n_dynamic.json"
DRY = "--dry-run" in sys.argv

# (文件, 原文子串, 替换子串, 期望次数)
SITES = [
    # --- 坑 19：设置卡片 subtitle / badgeText ---
    ("ui/settings/SettingsDialog.kt",
     'subtitle = "Accidental uninstall shield & App Info clear data protection",',
     "subtitle = stringResource(R.string.settings_data_loss_protection_desc),", 1),
    ("ui/settings/SettingsDialog.kt", 'badgeText = "Shield",',
     "badgeText = stringResource(R.string.settings_badge_shield),", 1),
    # --- 坑 19：权限卡 badgeText / description ---
    ("ui/components/AppPermissionsDialog.kt", 'badgeText = "Required",',
     "badgeText = stringResource(R.string.component_badge_required),", 1),
    ("ui/components/AppPermissionsDialog.kt", 'badgeText = "Optional",',
     "badgeText = stringResource(R.string.component_badge_optional),", 2),
    ("ui/components/AppPermissionsDialog.kt",
     'description = "Required for persistent background terminal service, live status alerts, updates, and rootfs package installer progress.",',
     "description = stringResource(R.string.component_perm_notifications_desc),", 1),
    ("ui/components/AppPermissionsDialog.kt",
     'description = "Allows AntiGem to access /sdcard to import local bootstrap packages, inspect files, and manage external code workspaces.",',
     "description = stringResource(R.string.component_perm_storage_desc),", 1),
    ("ui/components/AppPermissionsDialog.kt",
     'description = "Enables voice-to-text dictation and voice commands directly in chat.",',
     "description = stringResource(R.string.component_perm_microphone_desc),", 1),
    # --- 坑 19：附件 / 语音模式 subtitle ---
    ("ui/components/AttachmentSelectorBottomSheet.kt",
     'subtitle = "Attach an image for multimodal vision & analysis",',
     "subtitle = stringResource(R.string.component_attach_image_desc),", 1),
    ("ui/components/AttachmentSelectorBottomSheet.kt",
     'subtitle = "Attach a PDF, text, or binary file from your device",',
     "subtitle = stringResource(R.string.component_attach_file_desc),", 1),
    ("ui/components/SpeechToTextModeDialog.kt",
     'subtitle = "Record a voice clip and convert it into text using AGY transcription",',
     "subtitle = stringResource(R.string.component_stt_auto_desc),", 1),
    ("ui/components/SpeechToTextModeDialog.kt",
     'subtitle = "Stream words in real-time as you speak directly into the chat prompt",',
     "subtitle = stringResource(R.string.component_stt_live_desc),", 1),
    # --- 坑 19：通知渠道描述 / 数据层状态与错误 ---
    ("data/media/YouTubeMediaSessionManager.kt",
     'description = "Shows playback controls for videos in chat"',
     "description = context.getString(R.string.data_media_session_description)", 1),
    ("data/receiver/TermuxOpenReceiver.kt",
     'this.description = "Terminal open requests"',
     "this.description = context.getString(R.string.data_termux_open_description)", 1),
    ("data/agent/claude/ClaudeChatBackend.kt", '_startupStatus.value = "Starting Claude Code…"',
     "_startupStatus.value = application.getString(R.string.chat_status_starting_claude)", 1),
    ("data/agent/claude/ClaudeChatBackend.kt", '_startupStatus.value = "Compacting the conversation…"',
     "_startupStatus.value = application.getString(R.string.chat_status_compacting)", 1),
    ("data/agent/claude/ClaudeChatBackend.kt", '_startupStatus.value = "Claude Code is starting…"',
     "_startupStatus.value = application.getString(R.string.chat_status_claude_starting)", 1),
    ("data/agent/agy/AgyChatBackend.kt",
     '_conversationError.value = "No messages received from Antigravity. Tap Retry."',
     "_conversationError.value = application.getString(R.string.data_err_no_messages_from_antigravity)", 1),
    # --- 零散残留：Claude 设置页插值文本 ---
    ("ui/claude/ClaudeSettingsScreen.kt",
     'Text("Configured servers (${servers.size})"',
     "Text(stringResource(R.string.claude_configured_servers_count, servers.size)", 1),
    ("ui/claude/ClaudeSettingsScreen.kt",
     'Text("${srv.tools.size} tools"',
     "Text(stringResource(R.string.claude_tools_count, srv.tools.size)", 1),
    ("ui/claude/ClaudeSettingsScreen.kt",
     'Text("Remove ${srv.name}?"',
     "Text(stringResource(R.string.claude_remove_server_title, srv.name)", 1),
    ("ui/claude/ClaudeSettingsScreen.kt",
     'Text("Search ${available.size} plugins")',
     "Text(stringResource(R.string.claude_search_plugins, available.size))", 1),
    # --- 零散残留：WebView 扩展页 / 思考预算占位符 ---
    ("ui/settings/WebViewExtensionsScreen.kt",
     'Text("${extension.name} · ${extension.version} · ${extension.toolCount} MCP tools"',
     "Text(stringResource(R.string.settings_webview_ext_summary, extension.name, extension.version, extension.toolCount)", 1),
    ("ui/models/ThinkingSelectorBottomSheet.kt", 'placeholder = { Text("e.g. 2048") }',
     "placeholder = { Text(stringResource(R.string.settings_thinking_budget_placeholder)) }", 1),
    # --- 零散残留：导出/存图 snackbar 文案与动作 ---
    ("ui/components/ConversationExportHelper.kt",
     'message = "$savedFileName saved to Downloads/AntiGem/Exports"',
     "message = context.getString(R.string.component_saved_to_exports, savedFileName)", 1),
    ("ui/components/ImageDownloadHelper.kt", 'message = "Image saved to Downloads/AntiGem"',
     "message = context.getString(R.string.component_image_saved_to_downloads_antigem)", 1),
    ("ui/components/ConversationExportHelper.kt", 'actionLabel = "Open"',
     "actionLabel = context.getString(R.string.common_open)", 1),
    ("ui/components/ImageDownloadHelper.kt", 'actionLabel = "Open"',
     "actionLabel = context.getString(R.string.common_open)", 1),
    ("ui/components/ArtifactDownloadHelper.kt", 'actionLabel = "Open"',
     "actionLabel = context.getString(R.string.common_open)", 1),
]

RESOURCES = [
    # 审批面板通用（ApprovalItem 默认值与选择项）
    ("agents_deny", "Deny", "拒绝"),
    ("agents_allow", "Allow", "允许"),
    ("agents_deny_placeholder", "Tell the agent what to do instead (optional)",
     "告诉智能体你希望怎么做（可选）"),
    ("agents_deny_placeholder_antigravity", "Tell Antigravity what to do instead (optional)",
     "告诉 Antigravity 你希望怎么做（可选）"),
    ("agents_approval_run_command", "Run a command?", "运行命令？"),
    ("agents_approval_edit_file", "Edit a file?", "编辑文件？"),
    ("agents_approval_read_files", "Read files?", "读取文件？"),
    ("agents_approval_search_web", "Search the web?", "搜索网页？"),
    ("agents_approval_open_web_page", "Open a web page?", "打开网页？"),
    ("agents_approval_generate_image", "Generate an image?", "生成图片？"),
    ("agents_scope_conversation", "For this conversation", "本对话内"),
    ("agents_scope_conversation_desc", "Don't ask again in this chat", "不再在本对话中询问"),
    ("agents_scope_workspace", "In this workspace", "本工作区内"),
    ("agents_scope_workspace_desc", "Don't ask again in this project", "不再在此项目中询问"),
    ("agents_scope_everywhere", "Everywhere", "所有位置"),
    ("agents_scope_everywhere_desc", "Don't ask again anywhere", "不再在任何位置询问"),
    # Claude 计划审批与工具标题
    ("claude_plan_mode", "Plan mode", "计划模式"),
    ("claude_approve_accept_edits", "Approve · auto-accept edits", "批准并自动接受修改"),
    ("claude_approve_review_each", "Approve · review each edit", "批准并逐项审阅修改"),
    ("claude_keep_planning", "Keep planning", "继续规划"),
    ("claude_plan_deny_placeholder", "What should change in the plan?", "计划需要改动什么？"),
    ("claude_deny_placeholder", "Tell Claude what to do instead (optional)",
     "告诉 Claude 你希望怎么做（可选）"),
    ("claude_approval_create_file", "Create a file?", "创建文件？"),
    ("claude_approval_edit_notebook", "Edit a notebook?", "编辑笔记本？"),
    ("claude_approval_fetch_web_page", "Fetch a web page?", "抓取网页？"),
    ("claude_approval_read_outside_project", "Read outside the project?", "读取项目外的文件？"),
    # Claude 建议的作用范围
    ("claude_scope_for_session", "for this session", "仅本次会话"),
    ("claude_scope_local_settings", "in this project (only me)", "本项目内（仅我）"),
    ("claude_scope_project_settings", "in this project (shared)", "本项目内（共享）"),
    ("claude_scope_user_settings", "everywhere", "在所有位置"),
    # Claude 权限模式标签与说明（ClaudeUiKit）
    ("claude_mode_manual", "Manual", "手动"),
    ("claude_mode_accept_edits", "Accept edits", "接受修改"),
    ("claude_mode_plan", "Plan", "计划"),
    ("claude_mode_auto", "Auto", "自动"),
    ("claude_mode_dont_ask", "Don't ask", "不询问"),
    ("claude_mode_bypass", "Bypass", "绕过"),
    ("claude_mode_manual_desc", "Ask before edits and commands", "编辑与命令前先询问"),
    ("claude_mode_accept_edits_desc", "Apply file edits without asking", "不询问直接应用文件修改"),
    ("claude_mode_plan_desc", "Explore and propose a plan, no changes", "只探索并给出计划，不做修改"),
    ("claude_mode_auto_desc", "Claude decides what is safe to run", "由 Claude 判断哪些操作安全"),
    ("claude_mode_dont_ask_desc", "Never ask; deny anything not pre-allowed",
     "从不询问；未预先允许的一律拒绝"),
    ("claude_mode_bypass_desc", "Allow everything (sandboxes only)", "允许一切（仅限沙箱）"),
    # 组件层徽标与说明
    ("component_badge_required", "Required", "必需"),
    ("component_badge_optional", "Optional", "可选"),
    ("component_perm_notifications_desc",
     "Required for persistent background terminal service, live status alerts, updates, and rootfs package installer progress.",
     "后台终端服务常驻、实时状态提醒、更新与 rootfs 安装进度提示所必需。"),
    ("component_perm_storage_desc",
     "Allows AntiGem to access /sdcard to import local bootstrap packages, inspect files, and manage external code workspaces.",
     "允许 AntiGem 访问 /sdcard，以导入本地引导包、查看文件并管理外部代码工作区。"),
    ("component_perm_microphone_desc",
     "Enables voice-to-text dictation and voice commands directly in chat.",
     "在对话中直接使用语音转文字与语音指令。"),
    ("component_attach_image_desc", "Attach an image for multimodal vision & analysis",
     "附加图片以进行多模态视觉分析"),
    ("component_attach_file_desc", "Attach a PDF, text, or binary file from your device",
     "从设备附加 PDF、文本或二进制文件"),
    ("component_stt_auto_desc",
     "Record a voice clip and convert it into text using AGY transcription",
     "录制语音片段并用 AGY 转写为文字"),
    ("component_stt_live_desc",
     "Stream words in real-time as you speak directly into the chat prompt",
     "说话时实时转写并直接写入对话输入框"),
    ("common_open", "Open", "打开"),
    # 设置页
    ("settings_data_loss_protection_desc",
     "Accidental uninstall shield & App Info clear data protection",
     "防误卸载，并保护「应用信息 → 清除数据」"),
    ("settings_badge_shield", "Shield", "防护"),
    ("settings_thinking_budget_placeholder", "e.g. 2048", "例如 2048"),
    # 数据层通知渠道描述
    ("data_media_session_description", "Shows playback controls for videos in chat",
     "在对话中显示视频播放控件"),
    ("data_termux_open_description", "Terminal open requests", "终端打开请求"),
    ("data_err_no_messages_from_antigravity", "No messages received from Antigravity. Tap Retry.",
     "未收到 Antigravity 的消息，点击重试。"),
    # 对话状态
    ("chat_status_starting_claude", "Starting Claude Code…", "正在启动 Claude Code……"),
    ("chat_status_compacting", "Compacting the conversation…", "正在压缩会话……"),
    ("chat_status_claude_starting", "Claude Code is starting…", "Claude Code 正在启动……"),
]

DYNAMIC = {
    "agents_approval_use_tool": {"en": "Use $name?", "zh": "使用 %1$s？",
                               "module": "agents", "args": ["name"]},
    "agents_approval_allow_tool": {"en": "Allow $name?", "zh": "允许 %1$s？",
                                 "module": "agents", "args": ["name"]},
    "claude_always_allow_rules": {"en": "Always allow $rules $where", "zh": "始终允许 %1$s %2$s",
                                 "module": "claude", "args": ["rules", "where"]},
    "claude_allow_access_to": {"en": "Allow access to $dirs $where", "zh": "允许访问 %1$s %2$s",
                              "module": "claude", "args": ["dirs", "where"]},
    "claude_switch_to_mode": {"en": "Switch to $mode $where", "zh": "切换到 %1$s %2$s",
                             "module": "claude", "args": ["mode", "where"]},
    "claude_edits_count": {"en": "$count edits", "zh": "%1$s 处修改",
                          "module": "claude", "args": ["count"]},
    "claude_configured_servers_count": {"en": "Configured servers ($count)", "zh": "已配置的服务器（%1$s）",
                                       "module": "claude", "args": ["count"]},
    "claude_tools_count": {"en": "$count tools", "zh": "%1$s 个工具",
                          "module": "claude", "args": ["count"]},
    "claude_remove_server_title": {"en": "Remove $name?", "zh": "移除 %1$s？",
                                  "module": "claude", "args": ["name"]},
    "claude_search_plugins": {"en": "Search $count plugins", "zh": "搜索 %1$s 个插件",
                             "module": "claude", "args": ["count"]},
    "settings_webview_ext_summary": {"en": "$name · $version · $count MCP tools",
                                    "zh": "%1$s · %2$s · %3$s 个 MCP 工具",
                                    "module": "settings", "args": ["name", "version", "count"]},
    "component_saved_to_exports": {"en": "$fileName saved to Downloads/AntiGem/Exports",
                                   "zh": "%1$s 已保存到 Downloads/AntiGem/Exports",
                                   "module": "component", "args": ["fileName"]},
}

MODULE_OF = {}


def main():
    errors, changed = [], 0
    for rel, old, new, count in SITES:
        path = SRC + rel
        text = open(path, encoding="utf-8").read()
        found = text.count(old)
        if found != count:
            errors.append(f"[{rel}] 期望 {count} 处，实际 {found}：{old[:60]}")
            continue
        if not DRY:
            open(path, "w", encoding="utf-8").write(text.replace(old, new))
        changed += count
        print(f"  {rel}: {count} 处  {old[:56]}")

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
        module = ("agents" if key.startswith("agents_") else
                  "claude" if key.startswith("claude_") else
                  "settings" if key.startswith("settings_") else
                  "data" if key.startswith("data_") else
                  "chat" if key.startswith("chat_") else "component")
        catalog.append({"key": key, "module": module, "en": en, "zh": zh,
                        "count": 1, "sites": [], "orphan": False})
        keys.add(key)
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
