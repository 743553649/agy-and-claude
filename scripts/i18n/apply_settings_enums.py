#!/usr/bin/env python3
"""把设置分组的两个枚举改成 @StringRes，让顶栏标题/副标题可汉化。

为什么要动结构：`enum class SettingsSection(val title: String, ...)` 里的标题是**枚举构造参数**，
既不在 `Text("...")` 里也不是 `title = "..."`，任何字面量流水线都碰不到它，
所以设置首页顶栏一直显示 "Settings & Preferences"。做法是让枚举只存资源 id，
在 Composable 里 `stringResource(...)` 取文案。

同样处理 `ClaudeSettingsPage`（Claude 设置子页，顶栏会显示 "Claude Code · Account & CLI"）。

输入：scripts/i18n/settings_enums_zh.tsv（英文原文<TAB>中文）
用法: apply_settings_enums.py [--dry-run]
"""
import json
import os
import re
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SRC = "app/src/main/java/com/example/gemini/"
CAT = "/tmp/i18n_catalog.json"
TSV = os.path.join(ROOT, "scripts/i18n/settings_enums_zh.tsv")

DRY = "--dry-run" in sys.argv

ENUMS = [
    # (文件, 枚举名, 资源 key 前缀, 模块)
    ("ui/settings/SettingsDialog.kt", "SettingsSection", "settings_section", "settings"),
    ("ui/claude/ClaudeSettingsScreen.kt", "ClaudeSettingsPage", "claude_page", "claude"),
]

USAGE_REPLACEMENTS = [
    ("ui/settings/SettingsDialog.kt",
     '"Claude Code · ${claudePage.title}"',
     '"Claude Code · " + stringResource(claudePage.titleRes)'),
    ("ui/settings/SettingsDialog.kt",
     "currentSection.title,",
     "stringResource(currentSection.titleRes),"),
    ("ui/settings/SettingsDialog.kt",
     "claudePage.subtitle else currentSection.subtitle",
     "stringResource(claudePage.subtitleRes) else stringResource(currentSection.subtitleRes)"),
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
            en, zh = line.split("\t", 1)
            table[en] = zh
    return table


def parse_enum(src, name):
    """解析 `enum class Name(val title: String, ...) { A("t", "s"), ... }`，返回 [(条目名, 标题, 副标题)]。"""
    m = re.search(r"enum class " + name + r"\([^)]*\)\s*\{(.*?)\n\}", src, re.S)
    if not m:
        raise SystemExit(f"没找到枚举 {name}")
    body = m.group(1)
    rows = []
    for em in re.finditer(r'^\s*([A-Z_0-9]+)\("((?:[^"\\]|\\.)*)",\s*"((?:[^"\\]|\\.)*)"\)',
                          body, re.M):
        rows.append((em.group(1), em.group(2), em.group(3)))
    if not rows:
        raise SystemExit(f"枚举 {name} 解析出 0 条")
    return rows, m.start(), m.end()


def main():
    translations = load_tsv()
    catalog = json.load(open(CAT, encoding="utf-8"))
    by_en = {c["en"]: c for c in catalog}
    used = {c["key"] for c in catalog}
    added = 0

    def ensure_key(en, preferred):
        nonlocal added
        prev = by_en.get(en)
        if prev and prev.get("zh"):
            return prev["key"]
        if en not in translations:
            raise SystemExit(f"缺少译文：{en}")
        key = preferred
        n = 2
        while key in used:
            key = f"{preferred}_{n}"
            n += 1
        used.add(key)
        catalog.append({"key": key, "module": "settings", "en": en,
                        "zh": translations[en], "count": 0, "sites": [], "orphan": False})
        added += 1
        return key

    # ---- 1. 逐文件重写枚举定义与使用点
    for rel, enum_name, prefix, _mod in ENUMS:
        path = os.path.join(SRC, rel)
        src = open(path, encoding="utf-8").read()
        rows, start, end = parse_enum(src, enum_name)

        body_lines = []
        for name, title, subtitle in rows:
            t_key = ensure_key(title, f"{prefix}_{slug(title, 32)}")
            s_key = ensure_key(subtitle, f"{prefix}_{slug(subtitle, 32)}_sub")
            body_lines.append(f'    {name}(R.string.{t_key}, R.string.{s_key})')
        new_enum = (
            f"enum class {enum_name}(@StringRes val titleRes: Int, @StringRes val subtitleRes: Int) {{\n"
            + ",\n".join(body_lines) + "\n}"
        )
        src = src[:start] + new_enum + src[end:]
        if "import androidx.annotation.StringRes" not in src:
            src = re.sub(r"(?m)^(import android[x]?\..*)$", r"\1\nimport androidx.annotation.StringRes", src, count=1)
            if "import androidx.annotation.StringRes" not in src:
                src = re.sub(r"(?m)^(import .*)$", r"\1\nimport androidx.annotation.StringRes", src, count=1)
        if not DRY:
            open(path, "w", encoding="utf-8").write(src)
        print(f"{'[dry-run] ' if DRY else ''}{rel}: 枚举 {enum_name} 改写 {len(rows)} 条")

    # ---- 2. 重写使用点
    for rel, old, new in USAGE_REPLACEMENTS:
        path = os.path.join(SRC, rel)
        src = open(path, encoding="utf-8").read()
        if old not in src:
            print(f"! 未找到待替换片段（可能已改过）：{old[:50]}")
            continue
        src = src.replace(old, new, 1)
        if not DRY:
            open(path, "w", encoding="utf-8").write(src)
        print(f"{'[dry-run] ' if DRY else ''}{rel}: {old[:40]} -> {new[:40]}")

    # Claude 子页里 ClaudeSettingsPage.X.title / .subtitle 的 13 处调用
    path = os.path.join(SRC, "ui/claude/ClaudeSettingsScreen.kt")
    src = open(path, encoding="utf-8").read()
    src, n1 = re.subn(r"ClaudeSettingsPage\.([A-Z_0-9]+)\.title\b",
                      r"stringResource(ClaudeSettingsPage.\1.titleRes)", src)
    src, n2 = re.subn(r"ClaudeSettingsPage\.([A-Z_0-9]+)\.subtitle\b",
                      r"stringResource(ClaudeSettingsPage.\1.subtitleRes)", src)
    if not DRY:
        open(path, "w", encoding="utf-8").write(src)
    print(f"{'[dry-run] ' if DRY else ''}ClaudeSettingsScreen.kt: title {n1} 处 / subtitle {n2} 处")

    if not DRY:
        json.dump(catalog, open(CAT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    print(f"\n{'[dry-run] ' if DRY else ''}新增资源 {added} 条，catalog {len(catalog)} 条")


if __name__ == "__main__":
    main()
