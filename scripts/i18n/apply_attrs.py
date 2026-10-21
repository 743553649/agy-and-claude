#!/usr/bin/env python3
"""改写属性形式的静态文案：text = "..." / contentDescription = "..." / title = "..." / label = "..."

与 apply.py（只处理 Text("...")）互补。安全约束：
- 只处理无插值且已有译文的条目
- 文件必须含 @Composable
- kind == text 时要求上方 6 行内能确认位于 Text( 调用中
- message 类不在此处理（多为 Toast / 非 Composable 上下文），留给人工
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from kotlin_scan import read_literal  # noqa: E402

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
CAT = "/tmp/i18n_catalog.json"
KINDS = ("text", "contentDescription", "title", "label")
dry = "--dry-run" in sys.argv

cat = json.load(open(CAT, encoding="utf-8"))
by_text = {c["en"]: c["key"] for c in cat if c["zh"] and "$" not in c["en"]}

changed = {}
total = 0
skipped = 0

for dirpath, _dirs, files in os.walk(os.path.join(ROOT, "app/src/main/java")):
    for fn in files:
        if not fn.endswith(".kt"):
            continue
        path = os.path.join(dirpath, fn)
        src = open(path, encoding="utf-8").read()
        if "@Composable" not in src:
            continue
        lines = src.split("\n")
        hits = 0
        dirty = False
        for idx, line in enumerate(lines):
            stripped = line.strip()
            if stripped.startswith("//") or stripped.startswith("*"):
                continue
            for kind in KINDS:
                marker = kind + " = \""
                pos = line.find(marker)
                if pos < 0:
                    continue
                q = pos + len(kind) + 3
                content, end = read_literal(line, q)
                if content is None:
                    continue
                key = by_text.get(content)
                if key is None:
                    continue
                if kind == "text":
                    ctx = "\n".join(lines[max(0, idx - 6):idx + 1])
                    if "Text(" not in ctx:
                        skipped += 1
                        continue
                lines[idx] = line[:q] + f"stringResource(R.string.{key})" + line[end:]
                line = lines[idx]
                hits += 1
                dirty = True
        if dirty:
            changed[os.path.relpath(path, ROOT)] = hits
            total += hits
            if not dry:
                open(path, "w", encoding="utf-8").write("\n".join(lines))

for rel, hits in sorted(changed.items(), key=lambda x: -x[1]):
    print(f"{hits:4d}  {rel}")
print(f"\n{'[dry-run] ' if dry else ''}files={len(changed)} replacements={total} skipped={skipped}")
