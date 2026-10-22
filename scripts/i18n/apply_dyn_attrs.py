#!/usr/bin/env python3
"""改写「含插值但调用点没被 dynamic_apply 覆盖」的站点。

背景：dynamic_apply.py 按行匹配字面量，多行字面量会漏；且只处理 kind 为 Text / text
的站点。实测还有 contentDescription（Composable 内）与 message（非 Composable 的
snackbar 文案）两类需要处理，title / label 是 data class 参数，留给 P2。

规则：
- kind ∈ {Text, text, contentDescription} -> stringResource(R.string.key, 参数...)
- kind ∈ {message}                        -> context.getString(R.string.key, 参数...)
- 其余（title / label / message 里的未知上下文）跳过并报告

用法:
  apply_dyn_attrs.py --dry-run
  apply_dyn_attrs.py
"""
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from dynamic import find_exprs  # noqa: E402

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SRC = os.path.join(ROOT, "app/src/main/java")
CAT = "/tmp/i18n_catalog.json"
DYN = "/tmp/i18n_dynamic.json"

DRY = "--dry-run" in sys.argv

COMPOSE_KINDS = {"Text", "text", "contentDescription"}
CONTEXT_KINDS = {"message"}

# 发给模型的提示词脚手架：绝不能翻（会让模型看到中文标签，且属不翻白名单）
PROMPT_INTERNAL = re.compile(r"\[(ignore|Previous Context Summary)|^\[.*\]$")


def scan_literals(src):
    """扫描所有字符串字面量，跳过 // 与 /* */ 注释，处理 \\ 转义与 ${} 嵌套。

    返回 [(line, start, end, content)]，start 指向开头的引号，end 指向闭合引号的下一位。
    """
    out = []
    i = 0
    n = len(src)
    line = 1
    while i < n:
        c = src[i]
        if c == "\n":
            line += 1
            i += 1
            continue
        if c == "/" and i + 1 < n and src[i + 1] == "/":
            j = src.find("\n", i)
            i = n if j < 0 else j
            continue
        if c == "/" and i + 1 < n and src[i + 1] == "*":
            j = src.find("*/", i + 2)
            if j < 0:
                break
            line += src.count("\n", i, j)
            i = j + 2
            continue
        if c != '"':
            i += 1
            continue
        start = i
        start_line = line
        i += 1
        buf = []
        while i < n:
            ch = src[i]
            if ch == "\\":
                buf.append(src[i:i + 2])
                i += 2
                continue
            if ch == "$" and i + 1 < n and src[i + 1] == "{":
                buf.append("${")
                i += 2
                depth = 1
                while i < n and depth:
                    b = src[i]
                    if b == "\\":
                        buf.append(src[i:i + 2])
                        i += 2
                        continue
                    if b == '"':
                        buf.append(b)
                        i += 1
                        while i < n:
                            if src[i] == "\\":
                                buf.append(src[i:i + 2])
                                i += 2
                                continue
                            buf.append(src[i])
                            if src[i] == '"':
                                i += 1
                                break
                            i += 1
                        continue
                    if b == "{":
                        depth += 1
                    elif b == "}":
                        depth -= 1
                    if b == "\n":
                        line += 1
                    buf.append(b)
                    i += 1
                continue
            if ch == '"':
                i += 1
                break
            if ch == "\n":
                line += 1
            buf.append(ch)
            i += 1
        out.append((start_line, start, i, "".join(buf)))
    return out


def main():
    catalog = json.load(open(CAT, encoding="utf-8"))
    dyn = json.load(open(DYN, encoding="utf-8"))
    by_key = {c["key"]: c for c in catalog}

    plan = {}   # path -> [(start, end, new, note)]
    skipped = []
    for key, info in dyn.items():
        if not info.get("zh") or not info.get("args"):
            continue
        entry = by_key.get(key)
        if not entry:
            continue
        for site in entry.get("sites", []):
            kind = site.get("kind")
            if kind not in COMPOSE_KINDS | CONTEXT_KINDS:
                skipped.append((site["file"].split("gemini/")[-1], site["line"], kind, entry["en"][:40]))
                continue
            path = os.path.join(ROOT, site["file"])
            plan.setdefault(path, []).append((site, key, info, entry))

    total = 0
    changed = []
    for path, items in plan.items():
        src = open(path, encoding="utf-8").read()
        lits = scan_literals(src)
        repl = []
        for site, key, info, entry in items:
            match = [l for l in lits if l[0] == site["line"] and l[3] == entry["en"]]
            if not match:
                same = [l for l in lits if l[3] == entry["en"]]
                match = same if len(same) == 1 else []
            if not match:
                skipped.append((site["file"].split("gemini/")[-1], site["line"], "未定位", entry["en"][:40]))
                continue
            line, start, end, _txt = match[0]
            args = "".join(", " + a for a in info["args"])
            if site["kind"] in COMPOSE_KINDS:
                new = f"stringResource(R.string.{key}{args})"
            else:
                new = f"context.getString(R.string.{key}{args})"
            repl.append((start, end, new))
            print(f"{'[dry-run] ' if DRY else ''}{site['file'].split('gemini/')[-1]}:{line} [{site['kind']}] {key}  << {entry['en'][:50]!r}")
        if not repl:
            continue
        for start, end, new in sorted(repl, reverse=True):
            src = src[:start] + new + src[end:]
        total += len(repl)
        changed.append((site["file"].split("gemini/")[-1], len(repl)))
        if not DRY:
            open(path, "w", encoding="utf-8").write(src)

    for rel, cnt in changed:
        print(f"{'[dry-run] ' if DRY else ''}{rel}: {cnt}")
    if skipped:
        print("\n跳过（交给 P2 或人工）：")
        for rel, line, kind, en in skipped:
            print(f"  {rel}:{line} [{kind}] {en}")
    print(f"\n{'[dry-run] ' if DRY else ''}改写 {total} 处")


if __name__ == "__main__":
    main()
