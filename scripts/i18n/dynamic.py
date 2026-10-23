#!/usr/bin/env python3
"""处理含插值（$var / ${expr}）的文案：拆参数、生成占位符资源、改写调用点。

子命令：
  list   列出待处理的插值文案及其参数
  merge  读取 "key<TAB>中文(含 %1$s)" 与参数顺序表，写入 /tmp/i18n_dynamic.json
  apply  按已确认的映射改写 Kotlin 调用点并生成 values 资源
"""
import json
import os
import re
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SRC = os.path.join(ROOT, "app/src/main/java")
CAT = "/tmp/i18n_catalog.json"
DYN = "/tmp/i18n_dynamic.json"

PLURAL = re.compile(r'^\s*if\s*\(.*\)\s*""\s*else\s*"[s]?"\s*$', re.S)


def find_exprs(text):
    """按出现顺序解析 $var 与 ${expr}，跳过复数后缀表达式。返回 (exprs, rendered)。"""
    out = []
    i = 0
    n = len(text)
    while i < n:
        ch = text[i]
        if ch == "\\" and i + 1 < n:
            i += 2
            continue
        if ch == "$":
            if i + 1 < n and text[i + 1] == "{":
                depth = 1
                j = i + 2
                while j < n and depth:
                    if text[j] == "{":
                        depth += 1
                    elif text[j] == "}":
                        depth -= 1
                    j += 1
                expr = text[i + 2:j - 1]
                i = j
                if PLURAL.match(expr):
                    continue
                out.append(expr)
                continue
            m = re.match(r"[A-Za-z_][A-Za-z0-9_]*", text[i + 1:])
            if m:
                out.append(m.group(0))
                i += 1 + len(m.group(0))
                continue
        i += 1
    return out


def guess_kind(expr):
    if re.fullmatch(r"[A-Za-z_][A-Za-z0-9_.]*", expr):
        if expr.endswith((".size", ".length", ".count")) or ".size" in expr:
            return "d"
    return "s"


def load_cat():
    return json.load(open(CAT, encoding="utf-8"))


def cmd_list():
    cat = load_cat()
    interp = [c for c in cat if "$" in c["en"] and not c["zh"]]
    print(f"插值待处理: {len(interp)}", file=sys.stderr)
    for c in interp:
        exprs = find_exprs(c["en"])
        if not exprs:
            continue
        kinds = "".join(guess_kind(e) for e in exprs)
        print(f"{c['key']}\t{kinds}\t{c['en']}")


def cmd_merge():
    db = {}
    if os.path.exists(DYN):
        db = json.load(open(DYN, encoding="utf-8"))
    cat = {c["key"]: c for c in load_cat()}
    for line in sys.stdin:
        line = line.rstrip("\n")
        if not line.strip() or line.startswith("#"):
            continue
        key, zh = line.split("\t")[:2]
        key, zh = key.strip(), zh.strip()
        if key not in cat:
            print(f"UNKNOWN {key}", file=sys.stderr)
            continue
        exprs = find_exprs(cat[key]["en"])
        db[key] = {"en": cat[key]["en"], "zh": zh, "args": exprs, "module": cat[key]["module"]}
    json.dump(db, open(DYN, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    print(f"dynamic entries: {len(db)}")


if __name__ == "__main__":
    cmd = sys.argv[1] if len(sys.argv) > 1 else "list"
    {"list": cmd_list, "merge": cmd_merge}[cmd]()
