#!/usr/bin/env python3
"""用量条标签、网络面板详情、会话抽屉时间筛选、指令作用域 chip。

来源：`find_untranslated_args.py`（按调用实参扫描）——这些写法 `Hint(...)` / `UsageBar(...)` /
`DetailRow(...)` / `listOf(...)` 之前的语句扫描抓不到。

注意：会话抽屉的时间筛选里，**标签同时是筛选键**（`selectedTimeFilter == timeLabel`，
过滤逻辑用 `when (selectedTimeFilter) { "All" -> ... }` 比较英文键）。直接翻标签会破坏筛选，
所以拆成 `key to label`，键保持英文、只翻显示文本。

ClipData 标签（`newPlainText("Copied Code", ...)` 等）**不翻**：那是剪贴板元数据，
界面上不显示，翻了对用户无收益，还会让调试时对照困难（沿批次 3 的既定判据）。

用法：apply_widgets_labels.py --dry-run / apply_widgets_labels.py → 三步流水线。
"""
import json
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SRC = "app/src/main/java/com/example/gemini/"
CAT = "/tmp/i18n_catalog.json"
DYN = "/tmp/i18n_dynamic.json"
DRY = "--dry-run" in sys.argv

CCC = "ui/claude/ClaudeChatControls.kt"
NID = "ui/components/NetworkInspectorDialog.kt"
DR = "ui/drawer/ChatHistoryDrawer.kt"
CS = "ui/claude/ClaudeSettingsScreen.kt"

SITES = [
    # ---- 用量条 ----
    (CCC, 'UsageBar("Used", c.percentage / 100.0, null)',
     'UsageBar(stringResource(R.string.claude_usage_used), c.percentage / 100.0, null)', 1),
    (CCC, 'UsageBar("Current session (5-hour)"', 'UsageBar(stringResource(R.string.claude_usage_session_5h)', 1),
    (CCC, 'UsageBar("This week (all models)"', 'UsageBar(stringResource(R.string.claude_usage_week_all)', 1),
    (CCC, 'UsageBar("This week (Opus)"', 'UsageBar(stringResource(R.string.claude_usage_week_opus)', 1),
    (CCC, 'UsageBar("This week (Sonnet)"', 'UsageBar(stringResource(R.string.claude_usage_week_sonnet)', 1),
    # ---- 网络面板 ----
    (NID, 'val tabs = listOf("Overview", "Headers")',
     'val tabs = listOf(stringResource(R.string.component_net_tab_overview), stringResource(R.string.component_net_tab_headers))', 1),
    (NID, 'DetailRow("Full URL"', 'DetailRow(stringResource(R.string.component_net_full_url)', 1),
    (NID, 'DetailRow("Method"', 'DetailRow(stringResource(R.string.component_net_method)', 1),
    (NID, 'DetailRow("Service Target"', 'DetailRow(stringResource(R.string.component_net_service_target)', 1),
    (NID, 'DetailRow("Timestamp"', 'DetailRow(stringResource(R.string.component_net_timestamp)', 1),
    (NID, 'DetailRow("Duration"', 'DetailRow(stringResource(R.string.component_net_duration)', 1),
    (NID, 'DetailRow("gRPC Status"', 'DetailRow(stringResource(R.string.component_net_grpc_status)', 1),
    (NID, 'DetailRow("Error Message"', 'DetailRow(stringResource(R.string.component_net_error_message)', 1),
    # ---- 会话抽屉时间筛选：键与显示分离 ----
    (DR, '''                    listOf("All", "Today", "Past 7d", "Past 30d").forEach { timeLabel ->
                        val isSelected = selectedTimeFilter == timeLabel''',
     '''                    listOf(
                        "All" to stringResource(R.string.settings_all),
                        "Today" to stringResource(R.string.drawer_filter_today),
                        "Past 7d" to stringResource(R.string.drawer_filter_past_7d),
                        "Past 30d" to stringResource(R.string.drawer_filter_past_30d)
                    ).forEach { (timeKey, timeLabel) ->
                        val isSelected = selectedTimeFilter == timeKey''', 1),
    (DR, '.clickable { selectedTimeFilter = timeLabel }',
     '.clickable { selectedTimeFilter = timeKey }', 1),
    # ---- 指令作用域 chip ----
    (CS, 'ControlChip("All projects"', 'ControlChip(stringResource(R.string.claude_scope_all_projects)', 1),
]

RESOURCES = [
    ("claude_usage_used", "Used", "已用"),
    ("claude_usage_session_5h", "Current session (5-hour)", "当前会话（5 小时）"),
    ("claude_usage_week_all", "This week (all models)", "本周（全部模型）"),
    ("claude_usage_week_opus", "This week (Opus)", "本周（Opus）"),
    ("claude_usage_week_sonnet", "This week (Sonnet)", "本周（Sonnet）"),
    ("component_net_tab_overview", "Overview", "概览"),
    ("component_net_tab_headers", "Headers", "请求头"),
    ("component_net_full_url", "Full URL", "完整 URL"),
    ("component_net_method", "Method", "方法"),
    ("component_net_service_target", "Service Target", "服务目标"),
    ("component_net_timestamp", "Timestamp", "时间"),
    ("component_net_duration", "Duration", "耗时"),
    ("component_net_grpc_status", "gRPC Status", "gRPC 状态"),
    ("component_net_error_message", "Error Message", "错误信息"),
    ("drawer_filter_today", "Today", "今天"),
    ("drawer_filter_past_7d", "Past 7d", "近 7 天"),
    ("drawer_filter_past_30d", "Past 30d", "近 30 天"),
]


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
        module = "claude" if key.startswith("claude_") else ("drawer" if key.startswith("drawer_") else "component")
        catalog.append({"key": key, "module": module, "en": en, "zh": zh,
                        "count": 1, "sites": [], "orphan": False})
        keys.add(key); ens.add(en); added += 1

    if errors:
        print("\n错误（未写入）：")
        for e in errors:
            print("  " + e)
        return 1

    if not DRY:
        for path, text in files.items():
            open(path, "w", encoding="utf-8").write(text)
        json.dump(catalog, open(CAT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    print(f"新增静态资源 {added} 条")
    return 0


if __name__ == "__main__":
    sys.exit(main())
