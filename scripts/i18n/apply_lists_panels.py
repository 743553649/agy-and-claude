#!/usr/bin/env python3
"""会话抽屉、IDE 外壳、Claude effort 说明的分支文案（纯 UI，组合作用域）。

用法：apply_lists_panels.py --dry-run / apply_lists_panels.py → 三步流水线。
"""
import json
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SRC = "app/src/main/java/com/example/gemini/"
CAT = "/tmp/i18n_catalog.json"
DYN = "/tmp/i18n_dynamic.json"
DRY = "--dry-run" in sys.argv

DR = "ui/drawer/ChatHistoryDrawer.kt"
IDE = "ui/ide/IdeScreen.kt"
CMS = "ui/claude/ClaudeModeSheets.kt"

SITES = [
    # ---- 会话抽屉 ----
    (DR, 'contentDescription = if (groupByWorkspace) "Disable Workspace Grouping" else "Group by Workspace",',
     'contentDescription = if (groupByWorkspace) stringResource(R.string.drawer_disable_workspace_grouping) else stringResource(R.string.drawer_group_by_workspace),', 1),
    (DR, 'text = if (isExpanded) "Show less" else "Show ${chats.size - 5} more",',
     'text = if (isExpanded) stringResource(R.string.drawer_show_less) else stringResource(R.string.drawer_show_more, chats.size - 5),', 1),
    (DR, 'contentDescription = if (isSubagentsExpanded) "Collapse subagents" else "Expand subagents",',
     'contentDescription = if (isSubagentsExpanded) stringResource(R.string.drawer_collapse_subagents) else stringResource(R.string.drawer_expand_subagents),', 1),
    (DR, 'text = if (query.isNotBlank()) "No chats match \\"$query\\". Try different keywords or reset filters." else "No chats in this category.",',
     'text = if (query.isNotBlank()) stringResource(R.string.drawer_no_chats_match, query) else stringResource(R.string.drawer_no_chats_category),', 1),
    # ---- IDE 外壳 ----
    (IDE, 'text = if (showSvgSource) "Preview SVG Graphic" else "Edit SVG XML Source",',
     'text = if (showSvgSource) stringResource(R.string.ide_preview_svg) else stringResource(R.string.ide_edit_svg_source),', 1),
    (IDE, 'contentDescription = if (isExternal) "Save As..." else "Save File",',
     'contentDescription = if (isExternal) stringResource(R.string.ide_save_as) else stringResource(R.string.ide_save_file),', 1),
    (IDE, 'contentDescription = if (isCustomMode) "Run Profile: $selectedProfileName" else "Run Active File",',
     'contentDescription = if (isCustomMode) stringResource(R.string.ide_run_profile, selectedProfileName) else stringResource(R.string.ide_run_active_file),', 1),
    (IDE, 'text = if (activeFile != null) activeFile.name else "(No file open)",',
     'text = if (activeFile != null) activeFile.name else stringResource(R.string.ide_no_file_open),', 1),
    # ---- Claude effort 说明 ----
    (CMS, 'null -> "The model\'s own default"', 'null -> stringResource(R.string.claude_effort_default)', 1),
    (CMS, '"low" -> "Quick answers with light reasoning"', '"low" -> stringResource(R.string.claude_effort_low)', 1),
    (CMS, '"medium" -> "Balanced speed and depth"', '"medium" -> stringResource(R.string.claude_effort_medium)', 1),
    (CMS, '"high" -> "Thorough work on hard tasks"', '"high" -> stringResource(R.string.claude_effort_high)', 1),
    (CMS, '"xhigh" -> "Extra depth for complex changes"', '"xhigh" -> stringResource(R.string.claude_effort_xhigh)', 1),
    (CMS, '"max" -> "Most thorough; uses plan limits fastest"', '"max" -> stringResource(R.string.claude_effort_max)', 1),
]

RESOURCES = [
    ("drawer_disable_workspace_grouping", "Disable Workspace Grouping", "取消按工作区分组"),
    ("drawer_group_by_workspace", "Group by Workspace", "按工作区分组"),
    ("drawer_show_less", "Show less", "收起"),
    ("drawer_collapse_subagents", "Collapse subagents", "收起子智能体"),
    ("drawer_expand_subagents", "Expand subagents", "展开子智能体"),
    ("drawer_no_chats_category", "No chats in this category.", "此分类下暂无会话。"),
    ("ide_preview_svg", "Preview SVG Graphic", "预览 SVG 图形"),
    ("ide_edit_svg_source", "Edit SVG XML Source", "编辑 SVG XML 源码"),
    ("ide_save_as", "Save As...", "另存为……"),
    ("ide_save_file", "Save File", "保存文件"),
    ("ide_run_active_file", "Run Active File", "运行当前文件"),
    ("ide_no_file_open", "(No file open)", "（未打开文件）"),
    ("claude_effort_default", "The model's own default", "模型自身默认值"),
    ("claude_effort_low", "Quick answers with light reasoning", "快速回答，轻量推理"),
    ("claude_effort_medium", "Balanced speed and depth", "速度与深度均衡"),
    ("claude_effort_high", "Thorough work on hard tasks", "难题上做更彻底的处理"),
    ("claude_effort_xhigh", "Extra depth for complex changes", "为复杂改动提供额外深度"),
    ("claude_effort_max", "Most thorough; uses plan limits fastest", "最彻底；最快消耗套餐额度"),
]

DYNAMIC = {
    "drawer_show_more": {"en": "Show $count more", "zh": "再显示 %1$s 个",
                         "module": "drawer", "args": ["count"]},
    "drawer_no_chats_match": {
        "en": "No chats match \"$query\". Try different keywords or reset filters.",
        "zh": "没有会话匹配「%1$s」。换个关键词或重置筛选试试。",
        "module": "drawer", "args": ["query"]},
    "ide_run_profile": {"en": "Run Profile: $name", "zh": "运行配置：%1$s",
                        "module": "ide", "args": ["name"]},
}

MODULE_BY_PREFIX = [("drawer_", "drawer"), ("ide_", "ide"), ("claude_", "claude")]


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
