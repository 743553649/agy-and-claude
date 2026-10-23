#!/usr/bin/env python3
"""P2 + P3.9：把 `title = / label =` 的字面量回写成资源引用。

为什么单独一轮：这些站点早在 P3.x 就译好了（资源 key 都在 catalog 里），但当初
因为「title 多为 data class 参数、label 多为动画标签」整体排除（HANDOFF 坑 7），
调用点从没回写过，表现为设置/遥测/文件详情/审批等界面仍是英文。

表外一律不碰。明确**不翻**的类别（本脚本不含它们）：
- 动画/诊断 label：`rememberInfiniteTransition(label=...)`、`animateFloat(..., label=...)`、
  `AnimatedContent(..., label=...)`、`"progress"`、`"extract_progress"`、`"question_page"` 等；
- 终端按键：`TermuxKey/IdeTermKey(label = "ESC"/"CTRL"/"ALT"/方向键/...)`（终端不翻）；
- 产品名：速度拨号 `"Google"`/`"YouTube"`、模型分组 `"Google Gemini"`/`"Anthropic Claude"`；
- 需要改数据模型的：`ClaudePermissionPanel` 的 `ApprovalItem(title = ...)`、
  `ConversationShareHelper` 的 `"Shared Conversation"`（留待下一批，已记入 HANDOFF）。

用法:
  apply_p2_titles.py --dry-run
  apply_p2_titles.py
然后：gen_resources.py -> fix_imports.py -> verify.py
"""
import json
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SRC = "app/src/main/java/com/example/gemini/"
CAT = "/tmp/i18n_catalog.json"
DYN = "/tmp/i18n_dynamic.json"

DRY = "--dry-run" in sys.argv

# (文件, 原行内容(strip 后), 新行内容(strip 后), 期望出现次数)
SITES = [
    # --- 对话遥测 ---
    ("ui/components/ChatTelemetryDialog.kt", 'title = "Input Tokens",',
     "title = stringResource(R.string.component_input_tokens),", 1),
    ("ui/components/ChatTelemetryDialog.kt", 'title = "Output Tokens",',
     "title = stringResource(R.string.component_output_tokens),", 1),
    ("ui/components/ChatTelemetryDialog.kt", 'title = "Cache Read Hits",',
     "title = stringResource(R.string.component_cache_read_hits),", 1),
    ("ui/components/ChatTelemetryDialog.kt", 'title = "Total Consumption",',
     "title = stringResource(R.string.component_total_consumption),", 1),
    ("ui/components/ChatTelemetryDialog.kt", 'TelemetryRow(label = "Active Model",',
     "TelemetryRow(label = stringResource(R.string.component_active_model),", 1),
    ("ui/components/ChatTelemetryDialog.kt", 'TelemetryRow(label = "User Turns",',
     "TelemetryRow(label = stringResource(R.string.component_user_turns),", 1),
    ("ui/components/ChatTelemetryDialog.kt", 'TelemetryRow(label = "Model Responses",',
     "TelemetryRow(label = stringResource(R.string.component_model_responses),", 1),
    ("ui/components/ChatTelemetryDialog.kt", 'TelemetryRow(label = "Tools Executed",',
     "TelemetryRow(label = stringResource(R.string.component_tools_executed),", 1),
    ("ui/components/ChatTelemetryDialog.kt", 'TelemetryRow(label = "Active Context Summary",',
     "TelemetryRow(label = stringResource(R.string.component_active_context_summary),", 1),
    ("ui/components/ChatTelemetryDialog.kt", 'TelemetryRow(label = "Custom System Prompt",',
     "TelemetryRow(label = stringResource(R.string.component_custom_system_prompt),", 1),
    # --- 文件详情 ---
    ("ui/components/FileDetailsDialog.kt", 'label = "Type",',
     "label = stringResource(R.string.component_type),", 1),
    ("ui/components/FileDetailsDialog.kt", 'label = "Location",',
     "label = stringResource(R.string.component_location),", 1),
    ("ui/components/FileDetailsDialog.kt", 'label = "Parent Directory",',
     "label = stringResource(R.string.component_parent_directory),", 1),
    ("ui/components/FileDetailsDialog.kt", 'label = "Size",',
     "label = stringResource(R.string.component_size),", 1),
    ("ui/components/FileDetailsDialog.kt", 'label = "Permissions",',
     "label = stringResource(R.string.component_permissions),", 1),
    ("ui/components/FileDetailsDialog.kt", 'label = "Last Modified",',
     "label = stringResource(R.string.component_last_modified),", 1),
    # --- 附件选择 / 语音模式 / 图片预览 / 问卷 / 输入栏 ---
    ("ui/components/AttachmentSelectorBottomSheet.kt", 'title = "Photo / Image",',
     "title = stringResource(R.string.component_photo_image),", 1),
    ("ui/components/AttachmentSelectorBottomSheet.kt", 'title = "Document / File",',
     "title = stringResource(R.string.component_document_file),", 1),
    ("ui/components/SpeechToTextModeDialog.kt", 'title = "Auto Transcribe",',
     "title = stringResource(R.string.component_auto_transcribe),", 1),
    ("ui/components/SpeechToTextModeDialog.kt", 'title = "Live Dictation",',
     "title = stringResource(R.string.component_live_dictation),", 1),
    ("ui/components/MessageBubble.kt", 'title = "Image Preview",',
     "title = stringResource(R.string.component_image_preview),", 1),
    ("ui/components/ChoiceQuestionnaireCard.kt", 'label = "Other (write your answer)",',
     "label = stringResource(R.string.component_other_write_your_answer),", 1),
    ("ui/components/ChatInputBar.kt", 'title = "Attachment Preview",',
     "title = stringResource(R.string.component_attachment_preview),", 1),
    # --- 权限说明卡 ---
    ("ui/components/AppPermissionsDialog.kt", 'title = "Notifications & Service",',
     "title = stringResource(R.string.component_notifications_service),", 1),
    ("ui/components/AppPermissionsDialog.kt", 'title = "Storage & /sdcard",',
     "title = stringResource(R.string.component_storage_sdcard),", 1),
    ("ui/components/AppPermissionsDialog.kt", 'title = "Microphone & Voice",',
     "title = stringResource(R.string.component_microphone_voice),", 1),
    # --- Git 面板分区标题 ---
    ("ui/ide/GitSourceControlView.kt", 'title = "STAGED CHANGES",',
     "title = stringResource(R.string.ide_staged_changes),", 1),
    ("ui/ide/GitSourceControlView.kt", 'title = "CHANGES",',
     "title = stringResource(R.string.ide_changes),", 1),
    ("ui/ide/GitSourceControlView.kt", 'title = "UNTRACKED",',
     "title = stringResource(R.string.ide_untracked),", 1),
    ("ui/ide/GitSourceControlView.kt", 'title = "COMMIT HISTORY",',
     "title = stringResource(R.string.ide_commit_history),", 1),
    # --- 模型选择 ---
    ("ui/models/ModelSelectorBottomSheet.kt", 'title = "Select model",',
     "title = stringResource(R.string.ui_select_model),", 1),
    # --- 浏览器新标签页标题 ---
    ("ui/browser/BrowserScreen.kt", 'title = "New Tab", activate = true)',
     "title = context.getString(R.string.common_new_tab), activate = true)", 1),  # LaunchedEffect 里，非 Composable（坑 1 变体）
    ("ui/browser/BrowserSessionManager.kt", 'tab.title = "New Tab"',
     'tab.title = appContext?.getString(R.string.common_new_tab) ?: "New Tab"', 2),
    # --- 插件文档卡片标题（含插值）---
    ("ui/settings/CascadeMcpCatalogDialog.kt", 'title = "$title Documentation",',
     "title = stringResource(R.string.settings_ui_plugin_documentation, title),", 1),
    # --- 设置首页三张卡 ---
    ("ui/settings/SettingsDialog.kt", 'title = "Data Loss Protection",',
     "title = stringResource(R.string.settings_data_loss_protection),", 1),
    ("ui/settings/SettingsDialog.kt", 'title = "Diagnostics & Performance",',
     "title = stringResource(R.string.settings_diagnostics_performance),", 1),
    ("ui/settings/SettingsDialog.kt", 'title = "About & Info",',
     "title = stringResource(R.string.settings_about_info),", 1),
    # --- 备份/还原脚本的对话标题（data class 构造，作用域内有 context）---
    ("ui/settings/SettingsDialog.kt", 'title = "Projects Backup Packager",',
     "title = context.getString(R.string.settings_projects_backup_packager),", 1),
    ("ui/settings/SettingsDialog.kt", 'title = "Projects Archive Restorer",',
     "title = context.getString(R.string.settings_projects_archive_restorer),", 1),
    ("ui/settings/SettingsDialog.kt", 'title = "Rootfs System Packager",',
     "title = context.getString(R.string.settings_rootfs_system_packager),", 1),
    ("ui/settings/SettingsDialog.kt", 'title = "AGY Chats & Credentials Backup",',
     "title = context.getString(R.string.settings_agy_chats_credentials_backup),", 1),
    ("ui/settings/SettingsDialog.kt", 'title = "Restore AGY Chats & Credentials",',
     "title = context.getString(R.string.settings_restore_agy_chats_credentials),", 1),
    # --- P3.9：外观子页残留标签 ---
    ("ui/settings/SettingsDialog.kt", 'Triple("SYSTEM", "Auto (System)", Icons.Outlined.BrightnessAuto),',
     'Triple("SYSTEM", stringResource(R.string.settings_ui_auto_system), Icons.Outlined.BrightnessAuto),', 1),
    ("ui/settings/SettingsDialog.kt", 'Triple("LIGHT", "Light", Icons.Outlined.LightMode),',
     'Triple("LIGHT", stringResource(R.string.settings_theme_light), Icons.Outlined.LightMode),', 1),
    ("ui/settings/SettingsDialog.kt", 'Triple("DARK", "Dark", Icons.Outlined.DarkMode)',
     'Triple("DARK", stringResource(R.string.settings_theme_dark), Icons.Outlined.DarkMode)', 1),
    ("ui/settings/SettingsDialog.kt",
     'val presets = listOf(0.85f to "Small", 1.00f to "Normal", 1.15f to "Medium", 1.30f to "Large", 1.50f to "Huge")',
     "val presets = listOf(\n"
     "                    0.85f to stringResource(R.string.settings_font_size_small),\n"
     "                    1.00f to stringResource(R.string.settings_font_size_normal),\n"
     "                    1.15f to stringResource(R.string.settings_font_size_medium),\n"
     "                    1.30f to stringResource(R.string.settings_font_size_large),\n"
     "                    1.50f to stringResource(R.string.settings_font_size_huge)\n"
     "                )", 1),
    ("ui/settings/SettingsDialog.kt",
     'val timeouts = listOf(0 to "Never", 3 to "3s", 5 to "5s", 10 to "10s", 15 to "15s")',
     'val timeouts = listOf(0 to stringResource(R.string.settings_auto_collapse_never), 3 to "3s", 5 to "5s", 10 to "10s", 15 to "15s")', 1),
    ("ui/settings/SettingsDialog.kt", '"chat" -> "Chat"',
     '"chat" -> stringResource(R.string.settings_switcher_item_chat)', 1),
    ("ui/settings/SettingsDialog.kt", '"ide" -> "IDE (Code Editor)"',
     '"ide" -> stringResource(R.string.settings_switcher_item_ide)', 1),
    ("ui/settings/SettingsDialog.kt", '"terminal" -> "Terminal"',
     '"terminal" -> stringResource(R.string.common_terminal)', 1),
    ("ui/settings/SettingsDialog.kt", '"browser" -> "Web Browser"',
     '"browser" -> stringResource(R.string.settings_switcher_item_browser)', 1),
    # --- 数据层：对话标题与错误标题（有 Application，用 getString）---
    ("data/agent/agy/AgyChatBackend.kt", 'title = "New Chat",',
     "title = application.getString(R.string.common_new_chat),", 2),
    ("data/agent/agy/AgyChatBackend.kt", 'title = "Antigravity Chat"',
     "title = application.getString(R.string.data_antigravity_chat)", 1),
    ("data/agent/agy/AgyChatBackend.kt", 'title = "Execution Failure",',
     "title = application.getString(R.string.data_execution_failure),", 1),
    ("data/agent/claude/ClaudeChatBackend.kt", 'title = "New Chat",',
     "title = application.getString(R.string.common_new_chat),", 1),
    ("data/agent/claude/ClaudeChatBackend.kt", 'title = "Claude Chat"',
     "title = application.getString(R.string.data_claude_chat)", 1),
    ("data/agent/claude/ClaudeChatBackend.kt", 'val title = "${source.title} (fork)"',
     "val title = application.getString(R.string.data_fork_title, source.title)", 1),
]

# 新增静态资源（已存在的复用，不重复登记）
RESOURCES = [
    ("settings_theme_light", "Light", "浅色"),
    ("settings_theme_dark", "Dark", "深色"),
    ("settings_font_size_small", "Small", "小"),
    ("settings_font_size_normal", "Normal", "标准"),
    ("settings_font_size_medium", "Medium", "中"),
    ("settings_font_size_large", "Large", "大"),
    ("settings_font_size_huge", "Huge", "特大"),
    ("settings_auto_collapse_never", "Never", "从不"),
    ("settings_switcher_item_chat", "Chat", "对话"),
    ("settings_switcher_item_ide", "IDE (Code Editor)", "IDE（代码编辑器）"),
    ("settings_switcher_item_browser", "Web Browser", "网页浏览器"),
]

# 新增插值资源（写 /tmp/i18n_dynamic.json，由 gen_resources 处理占位符）
DYNAMIC = {
    "settings_ui_plugin_documentation": {
        "en": "$title Documentation", "zh": "%1$s 文档",
        "module": "settings", "args": ["title"],
    },
    "data_fork_title": {
        "en": "${source.title} (fork)", "zh": "%1$s（副本）",
        "module": "data", "args": ["source.title"],
    },
}


def main():
    changed = 0
    errors = []
    for rel, old, new, count in SITES:
        path = SRC + rel
        with open(path, encoding="utf-8") as f:
            text = f.read()
        found = text.count(old)
        if found != count:
            errors.append(f"[{rel}] 期望 {count} 处，实际 {found} 处：{old[:60]}")
            continue
        # 纯子串替换（保留原有缩进与前后文）
        new_text = text.replace(old, new)
        if not DRY:
            with open(path, "w", encoding="utf-8") as f:
                f.write(new_text)
        changed += count
        print(f"  {rel}: {count} 处  {old[:50]}")

    # 资源入库
    catalog = json.load(open(CAT, encoding="utf-8"))
    keys = {c["key"] for c in catalog}
    ens = {c["en"] for c in catalog}
    added = 0
    for key, en, zh in RESOURCES:
        if key in keys:
            continue
        if en in ens:
            errors.append(f"英文重复（已有条目）: {en}")
            continue
        catalog.append({"key": key, "module": "settings", "en": en, "zh": zh,
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
    if DRY:
        print("（dry-run，未写盘）")
    if errors:
        print("\n有错误，未完成：")
        for e in errors:
            print("  " + e)
        return 1
    print("下一步：gen_resources.py -> fix_imports.py -> verify.py")
    return 0


if __name__ == "__main__":
    sys.exit(main())
