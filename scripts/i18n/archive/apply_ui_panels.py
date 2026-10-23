#!/usr/bin/env python3
"""模型选择、Git 面板、Claude 控制条、代码块、网络面板、输入栏的分支文案。

判据：都在 @Composable 作用域（Text / contentDescription / AgentSheetHeader 参数），
唯一例外是 Git 的 stash Toast——它在 onClick 回调里，用 `context.getString`。
技术值保留英文：配额剩余时长的 `3d 5h` 与网络面板的 `%.1f s (Active)`（单位缩写，
且后者要过 String.format，翻成中文会改变格式串语义）。

用法：apply_ui_panels.py --dry-run / apply_ui_panels.py → 三步流水线。
"""
import json
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SRC = "app/src/main/java/com/example/gemini/"
CAT = "/tmp/i18n_catalog.json"
DYN = "/tmp/i18n_dynamic.json"
DRY = "--dry-run" in sys.argv

MSB = "ui/models/ModelSelectorBottomSheet.kt"
GIT = "ui/ide/GitSourceControlView.kt"
CCC = "ui/claude/ClaudeChatControls.kt"
CB = "ui/components/CodeBlock.kt"
NID = "ui/components/NetworkInspectorDialog.kt"
CIB = "ui/components/ChatInputBar.kt"

SITES = [
    # ---- 模型选择 ----
    (MSB, 'detail = if (availableModelsCount > 0) "$availableModelsCount models" else "no models loaded",',
     'detail = if (availableModelsCount > 0) stringResource(R.string.models_available_count, availableModelsCount) else stringResource(R.string.models_none_loaded),', 1),
    # ---- Git 面板 ----
    (GIT, 'Toast.makeText(context, if (ok) "Stash applied!" else "Stash pop failed", Toast.LENGTH_SHORT).show()',
     'Toast.makeText(context, if (ok) context.getString(R.string.git_stash_applied) else context.getString(R.string.git_stash_pop_failed), Toast.LENGTH_SHORT).show()', 1),
    (GIT, 'text = if (configPromptReason != null) "Git Author Setup" else "Git Configuration",',
     'text = if (configPromptReason != null) stringResource(R.string.git_author_setup) else stringResource(R.string.git_configuration),', 1),
    (GIT, '"true" -> "Rebase (true)"', '"true" -> stringResource(R.string.ide_rebase_true)', 1),
    (GIT, '"merges" -> "Preserve merges (merges)"', '"merges" -> stringResource(R.string.ide_preserve_merges_merges)', 1),
    (GIT, '"false" -> "Merge / Default (false)"', '"false" -> stringResource(R.string.ide_merge_default_false)', 1),
    (GIT, 'else -> if (configPullRebase.isNotBlank()) configPullRebase else "Merge / Default (false)"',
     'else -> if (configPullRebase.isNotBlank()) configPullRebase else stringResource(R.string.ide_merge_default_false)', 1),
    (GIT, 'Text(if (configPromptReason != null) "Save & Continue" else "Save")',
     'Text(if (configPromptReason != null) stringResource(R.string.ui_save_continue) else stringResource(R.string.common_save))', 1),
    (GIT, 'Text(if (unpushedCount > 0) "Push ($unpushedCount)" else "Push")',
     'Text(if (unpushedCount > 0) stringResource(R.string.ide_push_with_count, unpushedCount) else stringResource(R.string.ide_push))', 1),
    # ---- Claude 控制条 ----
    (CCC, 'text = if (thinking) "Thinking" else "No thinking",',
     'text = if (thinking) stringResource(R.string.common_thinking) else stringResource(R.string.claude_thinking_off),', 1),
    (CCC, 'text = if (warm) "Cache ${((left + 59_999) / 60_000)}m" else "Cache cold",',
     'text = if (warm) stringResource(R.string.claude_cache_warm_short, (left + 59_999) / 60_000) else stringResource(R.string.claude_cache_cold),', 1),
    (CCC, 'c == null -> "No cache information yet."',
     'c == null -> stringResource(R.string.claude_cache_no_info)', 1),
    (CCC, 'c.compacted -> "The conversation was compacted, so your next message will re-cache it."',
     'c.compacted -> stringResource(R.string.claude_cache_compacted)', 1),
    (CCC, '''left > 0 -> "Cache warm, about ${(left + 59_999) / 60_000} min left (${if (c.ttlMs >= 3_600_000) "1-hour" else "5-minute"} cache). " +
                            "Messages sent before it expires reuse it and cost less."''',
     '''left > 0 -> stringResource(R.string.claude_cache_warm, (left + 59_999) / 60_000, stringResource(if (c.ttlMs >= 3_600_000) R.string.claude_cache_ttl_one_hour else R.string.claude_cache_ttl_five_min)) + " " +
                            stringResource(R.string.claude_cache_warm_note)''', 1),
    (CCC, 'else -> "The prompt cache has likely expired, so your next message will re-cache about ${compactTokens(c.recacheTokens)} tokens."',
     'else -> stringResource(R.string.claude_cache_expired, compactTokens(c.recacheTokens))', 1),
    (CCC, 'rows.isNotEmpty() -> "${rows.size} models"',
     'rows.isNotEmpty() -> stringResource(R.string.models_available_count, rows.size)', 1),
    # ---- 代码块 ----
    (CB, 'contentDescription = if (isExpanded) "Collapse code block" else "Expand full height",',
     'contentDescription = if (isExpanded) stringResource(R.string.component_code_collapse) else stringResource(R.string.component_code_expand),', 1),
    (CB, 'text = if (isExpanded) "Collapse code block ▲" else "Show all $lineCount lines ($hiddenLines more) ▼",',
     'text = if (isExpanded) stringResource(R.string.component_code_collapse_arrow) else stringResource(R.string.component_code_show_all, lineCount, hiddenLines),', 1),
    (CB, 'text = if (selectedTab == 1 && isPreviewable) "Preview: ${language.uppercase()}" else language.ifEmpty { "Code" }.uppercase(),',
     'text = if (selectedTab == 1 && isPreviewable) stringResource(R.string.component_code_preview, language.uppercase()) else language.ifEmpty { stringResource(R.string.component_code) }.uppercase(),', 1),
    (CB, 'text = if (selectedTab == 1 && isPreviewable) "Interactive Artifact" else "$lineCount lines",',
     'text = if (selectedTab == 1 && isPreviewable) stringResource(R.string.component_code_interactive_artifact) else stringResource(R.string.component_code_line_count, lineCount),', 1),
    # ---- 网络面板 ----
    (NID, 'text = if (logs.isEmpty()) "No network calls captured yet" else "No matching calls found",',
     'text = if (logs.isEmpty()) stringResource(R.string.component_network_no_calls) else stringResource(R.string.component_network_no_match),', 1),
    (NID, 'isConnected && elapsed >= 2000L -> "Live Stream"',
     'isConnected && elapsed >= 2000L -> stringResource(R.string.component_network_live_stream)', 1),
    (NID, 'DetailRow("HTTP Status", if (entry.statusCode > 0) "${entry.statusCode}" else if (isConnected) "Connected / Streaming" else "Pending")',
     'DetailRow(stringResource(R.string.component_network_http_status), if (entry.statusCode > 0) "${entry.statusCode}" else if (isConnected) stringResource(R.string.component_network_connected_streaming) else stringResource(R.string.component_network_pending))', 1),
    # ---- 输入栏 ----
    (CIB, 'text = if (isDictationPaused) "Dictation paused (tap resume to speak)" else "Listening... Speak now",',
     'text = if (isDictationPaused) stringResource(R.string.chat_dictation_paused) else stringResource(R.string.chat_dictation_listening),', 1),
    (CIB, 'contentDescription = if (isLiveDictating) "Stop Dictation" else "Live Dictation",',
     'contentDescription = if (isLiveDictating) stringResource(R.string.chat_dictation_stop) else stringResource(R.string.component_live_dictation),', 1),
    (CIB, 'contentDescription = if (voiceNotesAsText) "Record and transcribe" else "Record voice note",',
     'contentDescription = if (voiceNotesAsText) stringResource(R.string.chat_record_and_transcribe) else stringResource(R.string.chat_record_voice_note),', 1),
]

RESOURCES = [
    ("models_none_loaded", "no models loaded", "未加载模型"),
    ("git_stash_applied", "Stash applied!", "已应用贮藏！"),
    ("git_stash_pop_failed", "Stash pop failed", "恢复贮藏失败"),
    ("git_author_setup", "Git Author Setup", "Git 作者设置"),
    ("git_configuration", "Git Configuration", "Git 配置"),
    ("claude_thinking_off", "No thinking", "不思考"),
    ("claude_cache_cold", "Cache cold", "缓存已冷"),
    ("claude_cache_no_info", "No cache information yet.", "暂无缓存信息。"),
    ("claude_cache_compacted", "The conversation was compacted, so your next message will re-cache it.",
     "会话已被压缩，因此你的下一条消息会重新建立缓存。"),
    ("claude_cache_warm_note", "Messages sent before it expires reuse it and cost less.",
     "在过期前发送的消息会复用它，成本更低。"),
    ("claude_cache_ttl_one_hour", "1-hour", "1 小时"),
    ("claude_cache_ttl_five_min", "5-minute", "5 分钟"),
    ("component_code_collapse", "Collapse code block", "收起代码块"),
    ("component_code_expand", "Expand full height", "展开为完整高度"),
    ("component_code_collapse_arrow", "Collapse code block ▲", "收起代码块 ▲"),
    ("component_code_interactive_artifact", "Interactive Artifact", "可交互产物"),
    ("component_network_no_calls", "No network calls captured yet", "尚未捕获任何网络请求"),
    ("component_network_no_match", "No matching calls found", "没有匹配的请求"),
    ("component_network_live_stream", "Live Stream", "实时流"),
    ("component_network_http_status", "HTTP Status", "HTTP 状态"),
    ("component_network_connected_streaming", "Connected / Streaming", "已连接 / 流式传输中"),
    ("component_network_pending", "Pending", "等待中"),
    ("chat_dictation_paused", "Dictation paused (tap resume to speak)", "听写已暂停（点继续后说话）"),
    ("chat_dictation_listening", "Listening... Speak now", "正在聆听……请说话"),
    ("chat_dictation_stop", "Stop Dictation", "停止听写"),
    ("chat_record_and_transcribe", "Record and transcribe", "录音并转写"),
    ("chat_record_voice_note", "Record voice note", "录制语音"),
]

DYNAMIC = {
    "models_available_count": {"en": "$count models", "zh": "%1$s 个模型",
                               "module": "models", "args": ["count"]},
    "ide_push_with_count": {"en": "Push ($count)", "zh": "推送（%1$s）",
                            "module": "ide", "args": ["count"]},
    "claude_cache_warm_short": {"en": "Cache $minutes m", "zh": "缓存 %1$s 分",
                                "module": "claude", "args": ["minutes"]},
    "claude_cache_warm": {"en": "Cache warm, about $minutes min left ($ttl cache).",
                          "zh": "缓存仍是热的，还剩约 %1$s 分钟（%2$s 缓存）。",
                          "module": "claude", "args": ["minutes", "ttl"]},
    "claude_cache_expired": {
        "en": "The prompt cache has likely expired, so your next message will re-cache about $tokens tokens.",
        "zh": "提示缓存很可能已过期，你的下一条消息将重建约 %1$s 个 token 的缓存。",
        "module": "claude", "args": ["tokens"]},
    "component_code_show_all": {"en": "Show all $lines lines ($hidden more) ▼",
                                "zh": "展开全部 %1$s 行（还有 %2$s 行）▼",
                                "module": "component", "args": ["lines", "hidden"]},
    "component_code_preview": {"en": "Preview: $language", "zh": "预览：%1$s",
                               "module": "component", "args": ["language"]},
    "component_code_line_count": {"en": "$count lines", "zh": "%1$s 行",
                                  "module": "component", "args": ["count"]},
}

MODULE_BY_PREFIX = [("models_", "ui"), ("git_", "ide"), ("claude_", "claude"),
                    ("component_", "component"), ("chat_", "chat")]


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
        module = next((m for pfx, m in MODULE_BY_PREFIX if key.startswith(pfx)), "ui")
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
