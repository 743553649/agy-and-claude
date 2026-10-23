#!/usr/bin/env python3
"""扫描 Kotlin 源码里的硬编码用户可见文案，输出 JSON 供翻译与改写使用。

只做识别，不改文件。规则：
- Text("...") / label = { Text("...") } 等 Composable 文案
- label = "..." / title = "..." / placeholder = "..." / message = "..."
排除：Log.*、纯符号、路径、URL、数字、产品名、模型名、占位符样例
"""
import json
import os
import re
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SRC = os.path.join(ROOT, "app/src/main/java")

# 明显不是文案的
SKIP_EXACT = {
    "A", "×", "—", "-", "+", "•", "OK", "…", "|", "/", "\\",
}
SKIP_PREFIX = ("http://", "https://", "ws://", "wss://", "/", "./", "~/")
# 白名单：产品名 / 模型名 / 标识符
SKIP_PATTERN = re.compile(
    r"^(AntiGem|AntiTerm|Termux|Antigravity|Gemini|Claude|Google Flow|"
    r"Nano Banana.*|Veo .*|Omni .*|gemini-.*|claude-.*)$"
)
INLINE_TEXT = re.compile(r'\bText\(\s*"((?:[^"\\]|\\.)*)"')
ATTR_TEXT = re.compile(r'\b(label|title|placeholder|message|text|contentDescription)\s*=\s*"((?:[^"\\]|\\.)*)"')

rows = []
for dirpath, _dirnames, filenames in os.walk(SRC):
    for fn in filenames:
        if not fn.endswith(".kt"):
            continue
        path = os.path.join(dirpath, fn)
        rel = os.path.relpath(path, ROOT)
        with open(path, encoding="utf-8") as f:
            for i, line in enumerate(f, 1):
                stripped = line.lstrip()
                if stripped.startswith(("//", "*", "/*")):
                    continue
                if "Log." in line:
                    continue
                for m in INLINE_TEXT.finditer(line):
                    rows.append((rel, i, "Text", m.group(1)))
                for m in ATTR_TEXT.finditer(line):
                    rows.append((rel, i, m.group(1), m.group(2)))

out = []
seen = set()
for rel, line, kind, val in rows:
    if val in SKIP_EXACT or val.startswith(SKIP_PREFIX) or SKIP_PATTERN.match(val):
        continue
    if not re.search(r"[A-Za-z]{2,}", val):
        continue
    key = (rel, line, val)
    if key in seen:
        continue
    seen.add(key)
    out.append({"file": rel, "line": line, "kind": kind, "text": val})

json.dump(out, sys.stdout, ensure_ascii=False, indent=1)
print(f"\n# total: {len(out)}", file=sys.stderr)
