#!/usr/bin/env python3
"""把含插值文案的调用点改写为 stringResource(R.string.key, 参数...) 形式。

只处理两类站点：
- kind == "Text"：Text("...") 里直接是字面量
- kind == "text" 且向上 6 行内能确认位于 Text( 调用中（Compose 上下文安全）
其余站点跳过并计数，留给人工确认。
"""
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from kotlin_scan import read_literal  # noqa: E402

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
CAT = "/tmp/i18n_catalog.json"
DYN = "/tmp/i18n_dynamic.json"

dry = "--dry-run" in sys.argv

dyn = json.load(open(DYN, encoding="utf-8"))
cat = {c["key"]: c for c in json.load(open(CAT, encoding="utf-8"))}

by_file = {}
skipped = 0
for key, info in dyn.items():
    entry = cat.get(key)
    if not entry or not info["zh"] or not info["args"]:
        continue
    for site in entry["sites"]:
        if site["kind"] not in ("Text", "text"):
            skipped += 1
            continue
        by_file.setdefault(site["file"], []).append((site, key, info, entry))

total = 0
changed = []
for rel, items in by_file.items():
    path = os.path.join(ROOT, rel)
    lines = open(path, encoding="utf-8").read().split("\n")
    hits = 0
    for site, key, info, entry in items:
        ln = site["line"] - 1
        if ln < 0 or ln >= len(lines):
            skipped += 1
            continue
        if site["kind"] == "text":
            ctx = "\n".join(lines[max(0, ln - 6):ln + 1])
            if "Text(" not in ctx:
                skipped += 1
                continue
        line = lines[ln]
        # 在行内找到目标字面量
        pos = 0
        done = False
        while True:
            q = line.find('"', pos)
            if q < 0:
                break
            content, end = read_literal(line, q)
            if content is None:
                break
            if content == entry["en"]:
                args = "".join(", " + a for a in info["args"])
                repl = f"stringResource(R.string.{key}{args})"
                line = line[:q] + repl + line[end:]
                hits += 1
                done = True
                break
            pos = end
        if not done:
            skipped += 1
        else:
            lines[ln] = line
    if hits:
        changed.append((rel, hits))
        total += hits
        if not dry:
            open(path, "w", encoding="utf-8").write("\n".join(lines))

for rel, hits in sorted(changed, key=lambda x: -x[1]):
    print(f"{hits:4d}  {rel}")
print(f"\n{'[dry-run] ' if dry else ''}files={len(changed)} replacements={total} skipped_sites={skipped}")
