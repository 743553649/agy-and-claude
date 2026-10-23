#!/usr/bin/env python3
"""把扫描结果整理成翻译目录 catalog.json：唯一文案 -> 资源键名。

键名规则：<module>_<english-slug>，同一段英文在多个模块出现时归入 common。
"""
import json
import os
import re
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SCAN = "/tmp/i18n_scan.json"
OUT = "/tmp/i18n_catalog.json"

MODULE_BY_DIR = [
    ("ui/settings/", "settings"),
    ("ui/ide/", "ide"),
    ("ui/claude/", "claude"),
    ("ui/browser/", "browser"),
    ("ui/drawer/", "drawer"),
    ("ui/chat/", "chat"),
    ("ui/agents/", "agents"),
    ("ui/components/", "component"),
    ("ui/", "ui"),
]


def module_of(path):
    for prefix, name in MODULE_BY_DIR:
        if path.split("com/example/gemini/")[-1].startswith(prefix):
            return name
    if "/data/" in path:
        return "data"
    return "common"


def slug(text, limit=40):
    s = re.sub(r"[^a-z0-9]+", "_", text.lower()).strip("_")
    return s[:limit].rstrip("_") or "empty"


rows = json.load(open(SCAN, encoding="utf-8"))
by_text = {}
for r in rows:
    mod = module_of(r["file"])
    entry = by_text.setdefault(r["text"], {"modules": set(), "sites": []})
    entry["modules"].add(mod)
    entry["sites"].append({"file": r["file"], "line": r["line"], "kind": r["kind"]})

used = set()
catalog = []
for text, info in sorted(by_text.items(), key=lambda kv: (-len(kv[0]), kv[0])):
    mods = info["modules"]
    mod = mods.pop() if len(mods) == 1 else "common"
    base = f"{mod}_{slug(text)}"
    key = base
    n = 2
    while key in used:
        key = f"{base}_{n}"
        n += 1
    used.add(key)
    catalog.append({
        "key": key,
        "module": mod,
        "en": text,
        "zh": "",
        "count": len(info["sites"]),
        "sites": info["sites"],
    })

json.dump(catalog, open(OUT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
mods = {}
for c in catalog:
    mods[c["module"]] = mods.get(c["module"], 0) + 1
print("唯一文案:", len(catalog))
for m, n in sorted(mods.items(), key=lambda kv: -kv[1]):
    print(f"  {m:10s} {n}")
