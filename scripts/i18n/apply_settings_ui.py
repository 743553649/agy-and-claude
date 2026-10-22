#!/usr/bin/env python3
"""汉化「设置首页」与「环境安装页」——这两处是既有一切脚本的盲区。

为什么单独一轮：
- 设置首页的标题/副标题是 `title = "..."` 形式的 Composable 参数，之前因为
  「title 多为 data class 参数」被整体排除（HANDOFF 坑 7），结果这些也留成了英文；
- 环境安装页的文案是 `when (state) { Downloading -> "..." }`、`if (isTermux) "A" else "B"`
  这类**表达式分支**里返回的字面量，`apply.py` 只匹配 `Text("字面量")`，从没覆盖到。

输入：scripts/i18n/settings_ui_zh.tsv（英文原文<TAB>中文），只改该表里列出的文案，
表外的字面量（日志键 "Fail"/"Success"、技术值 "48-bit"、"*/*"、"progress" 等）一律不碰。

用法:
  apply_settings_ui.py --dry-run
  apply_settings_ui.py
"""
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from dynamic import find_exprs  # noqa: E402
from apply_dyn_attrs import scan_literals  # noqa: E402

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SRC = "app/src/main/java/com/example/gemini/"
CAT = "/tmp/i18n_catalog.json"
DYN = "/tmp/i18n_dynamic.json"
TSV = os.path.join(ROOT, "scripts/i18n/settings_ui_zh.tsv")

DRY = "--dry-run" in sys.argv

MODULE = "settings"

# 只在这些文件的这些行区间内改写，避免碰到同名的 data class 参数或日志字符串
TARGETS = [
    ("ui/settings/SettingsDialog.kt", 596, 790),
    ("ui/settings/FullScreenLocalToolsInstaller.kt", 180, 900),
    ("ui/settings/LocalToolsInstallDialog.kt", 140, 960),
]

# 表达式里的 ON/OFF 也要跟着翻，否则会出现「浏览器：ON」
SPECIAL_ARGS = {
    'if (isBrowserAutomationEnabled) "ON" else "OFF"':
        'if (isBrowserAutomationEnabled) stringResource(R.string.settings_state_on) else stringResource(R.string.settings_state_off)',
    'if (isTerminalAutomationEnabled) "ON" else "OFF"':
        'if (isTerminalAutomationEnabled) stringResource(R.string.settings_state_on) else stringResource(R.string.settings_state_off)',
    'if (isFlowAutomationEnabled) "ON" else "OFF"':
        'if (isFlowAutomationEnabled) stringResource(R.string.settings_state_on) else stringResource(R.string.settings_state_off)',
}

EXTRA_RESOURCES = [
    ("settings_state_on", "On", "开启"),
    ("settings_state_off", "Off", "关闭"),
]


def slug(text, limit=40):
    s = re.sub(r"[^a-z0-9]+", "_", text.lower()).strip("_")
    return s[:limit].rstrip("_") or "empty"


def load_tsv():
    table = {}
    with open(TSV, encoding="utf-8") as f:
        for line in f:
            line = line.rstrip("\n")
            if not line.strip() or line.lstrip().startswith("#"):
                continue
            if "\t" not in line:
                raise SystemExit(f"TSV 行缺少 TAB: {line[:60]}")
            en, zh = line.split("\t", 1)
            table[en] = zh
    return table


def main():
    translations = load_tsv()
    catalog = json.load(open(CAT, encoding="utf-8"))
    dyn = json.load(open(DYN, encoding="utf-8")) if os.path.exists(DYN) else {}
    by_en = {c["en"]: c for c in catalog}
    used = {c["key"] for c in catalog} | set(dyn)

    # ---- 1. 分配 key（同一英文全文复用同一个 key）
    key_by_en = {}
    new_res = 0
    for en, zh in translations.items():
        prev = by_en.get(en)
        if prev and prev.get("zh"):
            key_by_en[en] = prev["key"]
            continue
        key = f"{MODULE}_ui_{slug(en)}"
        n = 2
        while key in used:
            key = f"{MODULE}_ui_{slug(en)}_{n}"
            n += 1
        used.add(key)
        key_by_en[en] = key
        exprs = find_exprs(en)
        if exprs:
            dyn[key] = {"en": en, "zh": zh, "args": exprs, "module": MODULE}
        catalog.append({"key": key, "module": MODULE, "en": en, "zh": zh,
                        "count": 0, "sites": [], "orphan": False})
        new_res += 1

    for key, en, zh in EXTRA_RESOURCES:
        if key in used:
            continue
        used.add(key)
        catalog.append({"key": key, "module": MODULE, "en": en, "zh": zh,
                        "count": 0, "sites": [], "orphan": False})
        new_res += 1
        print(f"补充资源 {key}: {en} -> {zh}")

    # ---- 2. 改写目标区间内的字面量
    total = 0
    changed = []
    for rel, lo, hi in TARGETS:
        path = os.path.join(SRC, rel)
        src = open(path, encoding="utf-8").read()
        repl = []
        for line, start, end, content in scan_literals(src):
            if not (lo <= line <= hi):
                continue
            key = key_by_en.get(content)
            if not key:
                continue
            exprs = find_exprs(content)
            if exprs:
                args = "".join(
                    ", " + SPECIAL_ARGS.get(e, e) for e in exprs
                )
                new = f"stringResource(R.string.{key}{args})"
            else:
                new = f"stringResource(R.string.{key})"
            repl.append((start, end, new, line, content))
        if not repl:
            continue
        for start, end, new, line, content in sorted(repl, reverse=True):
            src = src[:start] + new + src[end:]
            print(f"{'[dry-run] ' if DRY else ''}{rel}:{line}  {content[:60]!r}")
        total += len(repl)
        changed.append((rel, len(repl)))
        if not DRY:
            open(path, "w", encoding="utf-8").write(src)

    print(f"\n{'[dry-run] ' if DRY else ''}改写 {total} 处，新增资源 {new_res} 条")
    if not DRY:
        json.dump(catalog, open(CAT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
        json.dump(dyn, open(DYN, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
        print(f"catalog: {len(catalog)} 条 / dynamic: {len(dyn)} 条")


if __name__ == "__main__":
    main()
