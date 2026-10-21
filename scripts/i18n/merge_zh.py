#!/usr/bin/env python3
"""把 "key<TAB>中文" 行合并进 /tmp/i18n_catalog.json。空中文表示跳过不译。"""
import json
import sys

CAT = "/tmp/i18n_catalog.json"
catalog = json.load(open(CAT, encoding="utf-8"))
index = {c["key"]: c for c in catalog}
ok = skip = miss = 0
for line in sys.stdin:
    line = line.rstrip("\n")
    if not line.strip() or line.startswith("#"):
        continue
    parts = line.split("\t")
    if len(parts) != 2:
        print(f"BAD LINE: {line[:60]}", file=sys.stderr)
        continue
    key, zh = parts[0].strip(), parts[1].strip()
    if key not in index:
        print(f"UNKNOWN KEY: {key}", file=sys.stderr)
        miss += 1
        continue
    if zh == "-":
        skip += 1
        continue
    index[key]["zh"] = zh
    ok += 1
json.dump(catalog, open(CAT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
done = sum(1 for c in catalog if c["zh"])
print(f"merged={ok} skipped={skip} unknown={miss} 总完成 {done}/{len(catalog)}")
