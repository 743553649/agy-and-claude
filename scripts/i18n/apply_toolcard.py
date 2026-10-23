#!/usr/bin/env python3
"""聊天里的工具调用卡片（AgentToolCallCard.kt）三段 when 分支文案。

坑 11 的典型形态：字面量在 `when {}` 分支里求值后才喂给 `Text(text=...)`，
既不在 `Text("...")` 里也不在命名参数位置，所以历次扫描都没收录。
三处：头部动作前缀（actionPrefix）、命令前缀（commandPrefix）、空输出占位（emptyMessage）。

判据：三者最终都进 `Text(...)`（actionPrefix → `text = actionPrefix`，
commandPrefix → 命令栏文本，emptyMessage → `text = emptyMessage`），全部可见。
`ClipData.newPlainText("Tool Output", ...)` 是剪贴板标签，属技术值不翻。

用法：apply_toolcard.py --dry-run / apply_toolcard.py → gen_resources → fix_imports → verify。
"""
import json
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SRC = "app/src/main/java/com/example/gemini/"
CAT = "/tmp/i18n_catalog.json"
DYN = "/tmp/i18n_dynamic.json"
DRY = "--dry-run" in sys.argv

FILE = "ui/components/AgentToolCallCard.kt"

# (原文子串, 替换子串, 期望次数)
SITES = [
    # ---- actionPrefix：待批准 / 执行中 ----
    ('"Approve MCP:"', 'stringResource(R.string.component_tool_approve_mcp)', 1),
    ('"Approve Command:"', 'stringResource(R.string.component_tool_approve_command)', 1),
    ('"Searching web..."', 'stringResource(R.string.component_tool_searching_web)', 1),
    ('"Fetching page..."', 'stringResource(R.string.component_tool_fetching_page)', 1),
    ('"Evaluating math..."', 'stringResource(R.string.component_tool_evaluating_math)', 1),
    ('"Reading file..."', 'stringResource(R.string.component_tool_reading_file)', 1),
    ('"Editing file..."', 'stringResource(R.string.component_tool_editing_file)', 1),
    ('"Listing directory..."', 'stringResource(R.string.component_tool_listing_directory)', 1),
    ('"Searching code..."', 'stringResource(R.string.component_tool_searching_code)', 1),
    ('"Generating image..."', 'stringResource(R.string.component_tool_generating_image)', 2),
    ('"Executing MCP tool..."', 'stringResource(R.string.component_tool_executing_mcp)', 2),
    ('"System event..."', 'stringResource(R.string.component_tool_system_event)', 1),
    ('"Executing in Termux..."', 'stringResource(R.string.component_tool_executing_termux)', 1),
    # ---- actionPrefix：成功 ----
    ('"System:"', 'stringResource(R.string.component_tool_system)', 1),
    ('"Web search"', 'stringResource(R.string.component_tool_web_search)', 1),
    ('"Read page"', 'stringResource(R.string.component_tool_read_page)', 1),
    ('"Math evaluated"', 'stringResource(R.string.component_tool_math_evaluated)', 1),
    ('"Read file"', 'stringResource(R.string.component_tool_read_file)', 1),
    ('"Edited file"', 'stringResource(R.string.component_tool_edited_file)', 1),
    ('"Listed directory"', 'stringResource(R.string.component_tool_listed_directory)', 1),
    ('"Searched code"', 'stringResource(R.string.component_tool_searched_code)', 1),
    ('"Generated image"', 'stringResource(R.string.component_tool_generated_image)', 1),
    ('"MCP tool"', 'stringResource(R.string.component_tool_mcp_tool)', 1),
    ('"Ran bash"', 'stringResource(R.string.component_tool_ran_bash)', 1),
    # ---- actionPrefix：终止 / 失败 ----
    ('"Stopped:"', 'stringResource(R.string.component_tool_stopped)', 1),
    ('"Rejected:"', 'stringResource(R.string.component_tool_rejected)', 1),
    ('"Failed:"', 'stringResource(R.string.component_tool_failed)', 1),
    ('"Tool:"', 'stringResource(R.string.component_tool_tool)', 1),
    # ---- commandPrefix：带命令内容的插值 ----
    ('"Notification: ${toolCall.command}"', 'stringResource(R.string.component_tool_cmd_notification, toolCall.command)', 1),
    ('"Search Query: \\"${toolCall.command}\\""', 'stringResource(R.string.component_tool_cmd_search, toolCall.command)', 1),
    ('"URL: ${toolCall.command}"', 'stringResource(R.string.component_tool_cmd_url, toolCall.command)', 1),
    ('"CAS Expr: ${toolCall.command}"', 'stringResource(R.string.component_tool_cmd_math, toolCall.command)', 1),
    ('"File: ${toolCall.command}"', 'stringResource(R.string.component_tool_cmd_file, toolCall.command)', 2),
    ('"Directory: ${toolCall.command}"', 'stringResource(R.string.component_tool_cmd_directory, toolCall.command)', 1),
    ('"Grep: ${toolCall.command}"', 'stringResource(R.string.component_tool_cmd_grep, toolCall.command)', 1),
    ('"Find: ${toolCall.command}"', 'stringResource(R.string.component_tool_cmd_find, toolCall.command)', 1),
    ('"Prompt: ${toolCall.command}"', 'stringResource(R.string.component_tool_cmd_prompt, toolCall.command)', 1),
    # ---- emptyMessage：空输出占位 ----
    ('"Reading file contents..."', 'stringResource(R.string.component_tool_empty_reading_file)', 1),
    ('"Applying file modifications..."', 'stringResource(R.string.component_tool_empty_applying_edits)', 1),
    ('"Scanning directory..."', 'stringResource(R.string.component_tool_empty_scanning_directory)', 1),
    ('"Searching..."', 'stringResource(R.string.component_tool_empty_searching)', 1),
    ('"Running in Termux..."', 'stringResource(R.string.component_tool_empty_running_termux)', 1),
    ('"Processing..."', 'stringResource(R.string.component_tool_empty_processing)', 1),
    ('"(Waiting for approval)"', 'stringResource(R.string.component_tool_empty_waiting_approval)', 1),
    ('"(File inspected by model)"', 'stringResource(R.string.component_tool_empty_file_inspected)', 1),
    ('"(File modification completed)"', 'stringResource(R.string.component_tool_empty_edit_done)', 1),
    ('"(Directory contents scanned)"', 'stringResource(R.string.component_tool_empty_dir_scanned)', 1),
    ('"(Search completed - no output)"', 'stringResource(R.string.component_tool_empty_search_no_output)', 1),
    ('"(Image generation completed)"', 'stringResource(R.string.component_tool_empty_image_done)', 1),
    ('"(MCP tool executed - no output)"', 'stringResource(R.string.component_tool_empty_mcp_done)', 1),
    ('"(No output returned)"', 'stringResource(R.string.component_tool_empty_no_output)', 1),
    # ---- 展开/收起（无障碍文案，复用既有 key） ----
    ('contentDescription = if (isExpanded) "Collapse" else "Expand"',
     'contentDescription = if (isExpanded) stringResource(R.string.component_collapse) else stringResource(R.string.component_expand)', 1),
]

RESOURCES = [
    ("component_tool_approve_mcp", "Approve MCP:", "批准 MCP："),
    ("component_tool_approve_command", "Approve Command:", "批准命令："),
    ("component_tool_searching_web", "Searching web...", "正在搜索网页……"),
    ("component_tool_fetching_page", "Fetching page...", "正在抓取网页……"),
    ("component_tool_evaluating_math", "Evaluating math...", "正在计算表达式……"),
    ("component_tool_reading_file", "Reading file...", "正在读取文件……"),
    ("component_tool_editing_file", "Editing file...", "正在修改文件……"),
    ("component_tool_listing_directory", "Listing directory...", "正在列出目录……"),
    ("component_tool_searching_code", "Searching code...", "正在搜索代码……"),
    ("component_tool_generating_image", "Generating image...", "正在生成图片……"),
    ("component_tool_executing_mcp", "Executing MCP tool...", "正在调用 MCP 工具……"),
    ("component_tool_system_event", "System event...", "系统事件……"),
    ("component_tool_executing_termux", "Executing in Termux...", "正在 Termux 中执行……"),
    ("component_tool_system", "System:", "系统："),
    ("component_tool_web_search", "Web search", "网页搜索"),
    ("component_tool_read_page", "Read page", "读取网页"),
    ("component_tool_math_evaluated", "Math evaluated", "已计算表达式"),
    ("component_tool_read_file", "Read file", "读取文件"),
    ("component_tool_edited_file", "Edited file", "已修改文件"),
    ("component_tool_listed_directory", "Listed directory", "列出目录"),
    ("component_tool_searched_code", "Searched code", "搜索代码"),
    ("component_tool_generated_image", "Generated image", "已生成图片"),
    ("component_tool_mcp_tool", "MCP tool", "MCP 工具"),
    ("component_tool_ran_bash", "Ran bash", "执行了命令"),
    ("component_tool_stopped", "Stopped:", "已停止："),
    ("component_tool_rejected", "Rejected:", "已拒绝："),
    ("component_tool_failed", "Failed:", "失败："),
    ("component_tool_tool", "Tool:", "工具："),
    ("component_tool_empty_reading_file", "Reading file contents...", "正在读取文件内容……"),
    ("component_tool_empty_applying_edits", "Applying file modifications...", "正在应用文件修改……"),
    ("component_tool_empty_scanning_directory", "Scanning directory...", "正在扫描目录……"),
    ("component_tool_empty_searching", "Searching...", "正在搜索……"),
    ("component_tool_empty_running_termux", "Running in Termux...", "正在 Termux 中运行……"),
    ("component_tool_empty_processing", "Processing...", "正在处理……"),
    ("component_tool_empty_waiting_approval", "(Waiting for approval)", "（等待批准）"),
    ("component_tool_empty_file_inspected", "(File inspected by model)", "（模型已查看文件）"),
    ("component_tool_empty_edit_done", "(File modification completed)", "（文件修改已完成）"),
    ("component_tool_empty_dir_scanned", "(Directory contents scanned)", "（目录内容已扫描）"),
    ("component_tool_empty_search_no_output", "(Search completed - no output)", "（搜索已完成，无输出）"),
    ("component_tool_empty_image_done", "(Image generation completed)", "（图片生成已完成）"),
    ("component_tool_empty_mcp_done", "(MCP tool executed - no output)", "（MCP 工具已执行，无输出）"),
    ("component_tool_empty_no_output", "(No output returned)", "（无输出返回）"),
]

DYNAMIC = {
    "component_tool_cmd_notification": {"en": "Notification: $command", "zh": "通知：%1$s",
                                        "module": "component", "args": ["command"]},
    "component_tool_cmd_search": {"en": "Search Query: \"$command\"", "zh": "搜索词：「%1$s」",
                                  "module": "component", "args": ["command"]},
    "component_tool_cmd_url": {"en": "URL: $command", "zh": "URL：%1$s",
                               "module": "component", "args": ["command"]},
    "component_tool_cmd_math": {"en": "CAS Expr: $command", "zh": "表达式：%1$s",
                                "module": "component", "args": ["command"]},
    "component_tool_cmd_file": {"en": "File: $command", "zh": "文件：%1$s",
                                "module": "component", "args": ["command"]},
    "component_tool_cmd_directory": {"en": "Directory: $command", "zh": "目录：%1$s",
                                     "module": "component", "args": ["command"]},
    "component_tool_cmd_grep": {"en": "Grep: $command", "zh": "Grep：%1$s",
                                "module": "component", "args": ["command"]},
    "component_tool_cmd_find": {"en": "Find: $command", "zh": "查找：%1$s",
                                "module": "component", "args": ["command"]},
    "component_tool_cmd_prompt": {"en": "Prompt: $command", "zh": "提示词：%1$s",
                                  "module": "component", "args": ["command"]},
}


def main():
    errors, changed = [], 0
    path = SRC + FILE
    text = open(path, encoding="utf-8").read()
    for old, new, count in SITES:
        found = text.count(old)
        if found != count:
            errors.append(f"期望 {count} 处，实际 {found}：{old[:70]}")
            continue
        text = text.replace(old, new)
        changed += count
    if not DRY and not errors:
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
        catalog.append({"key": key, "module": "component", "en": en, "zh": zh,
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
