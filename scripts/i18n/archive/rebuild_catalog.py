#!/usr/bin/env python3
"""用正确的扫描结果重建目录，保留已翻译条目的 key 与译文。

- 以 /tmp/i18n_scan2.json 为准（不含注释、不含 newPlainText 之类误匹配）
- 英文完全相同的条目沿用旧 key 与译文
- 新出现的文案分配新 key
- 旧目录中已无源码引用的条目保留（可能已被回写引用），标记 orphan
"""
import json
import re

CAT = "/tmp/i18n_catalog.json"
SCAN = "/tmp/i18n_scan2.json"

MODULE_BY_DIR = [
    ("ui/settings/", "settings"), ("ui/ide/", "ide"), ("ui/claude/", "claude"),
    ("ui/browser/", "browser"), ("ui/drawer/", "drawer"), ("ui/chat/", "chat"),
    ("ui/agents/", "agents"), ("ui/components/", "component"), ("ui/", "ui"),
]


def module_of(path):
    tail = path.split("com/example/gemini/")[-1]
    for prefix, name in MODULE_BY_DIR:
        if tail.startswith(prefix):
            return name
    return "data" if "/data/" in path else "common"


def slug(text, limit=40):
    s = re.sub(r"[^a-z0-9]+", "_", text.lower()).strip("_")
    return s[:limit].rstrip("_") or "empty"


old = json.load(open(CAT, encoding="utf-8"))
by_en = {c["en"]: c for c in old}
used_keys = {c["key"] for c in old}

rows = json.load(open(SCAN, encoding="utf-8"))
groups = {}
for r in rows:
    g = groups.setdefault(r["text"], {"modules": set(), "sites": []})
    g["modules"].add(module_of(r["file"]))
    g["sites"].append({"file": r["file"], "line": r["line"], "kind": r["kind"]})

new_cat = []
added = 0
for text, info in sorted(groups.items(), key=lambda kv: (-len(kv[0]), kv[0])):
    prev = by_en.get(text)
    if prev:
        entry = dict(prev)
        entry["sites"] = info["sites"]
        entry["count"] = len(info["sites"])
        entry["orphan"] = False
        new_cat.append(entry)
        continue
    mods = set(info["modules"])
    mod = mods.pop() if len(mods) == 1 else "common"
    base = f"{mod}_{slug(text)}"
    key = base
    n = 2
    while key in used_keys:
        key = f"{base}_{n}"
        n += 1
    used_keys.add(key)
    new_cat.append({"key": key, "module": mod, "en": text, "zh": "", "count": len(info["sites"]),
                    "sites": info["sites"], "orphan": False})
    added += 1

seen = {c["en"] for c in new_cat}
orphans = 0
for c in old:
    if c["en"] not in seen:
        e = dict(c)
        e["orphan"] = True
        new_cat.append(e)
        orphans += 1

json.dump(new_cat, open(CAT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
live = [c for c in new_cat if not c.get("orphan")]
translated = sum(1 for c in new_cat if c["zh"])
print(f"重建完成：有源码引用 {len(live)} 条（新增 {added}），无引用保留 {orphans} 条")
print(f"已翻译 {translated} / {len(new_cat)}")
print(f"其中有引用但未翻译：{sum(1 for c in live if not c['zh'])}")
print(f"  静态未译：{sum(1 for c in live if not c['zh'] and '$' not in c['en'])}")
print(f"  含插值未译：{sum(1 for c in live if not c['zh'] and '$' in c['en'])}")
