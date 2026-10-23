#!/usr/bin/env python3
"""把 Toast.makeText 里的英文字面量改成 context.getString(R.string.*)。

为什么单独一轮：Toast 都在非 Composable 上下文（Activity/Service/Receiver/helper），
不能用 stringResource；而且不少文案含 Kotlin 插值，需要按出现顺序转成占位符参数。

输入：
  scripts/i18n/toast_zh.tsv   英文原文<TAB>中文（含 %n$s 占位符）
  /tmp/i18n_catalog.json      静态条目（无插值）写这里，供 gen_resources.py 生成资源
  /tmp/i18n_dynamic.json      含插值条目写这里（args 为表达式顺序表）

用法:
  apply_toast.py --dry-run   只打印将要做的改写
  apply_toast.py             实际写入源码与 catalog
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
TSV = os.path.join(ROOT, "scripts/i18n/toast_zh.tsv")

DRY = "--dry-run" in sys.argv

MODULE_BY_DIR = [
    ("ui/settings/", "settings"), ("ui/ide/", "ide"), ("ui/claude/", "claude"),
    ("ui/browser/", "browser"), ("ui/drawer/", "drawer"), ("ui/chat/", "chat"),
    ("ui/agents/", "agents"), ("ui/components/", "component"), ("ui/", "ui"),
]

# 某些插值表达式本身产出英文词，需要就地换成已翻译的资源
SPECIAL_ARGS = {
    'if (isDir) "folder" else "file"':
        'if (isDir) {ctx}.getString(R.string.common_folder) else {ctx}.getString(R.string.common_file)',
}

# 供上面的特殊表达式使用的额外资源
EXTRA_RESOURCES = [
    ("common_folder", "common", "Folder", "文件夹"),
    ("common_file", "common", "File", "文件"),
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


def find_calls(src):
    """返回 [(start_of_call, start_of_literal, end_of_literal, ctx_expr, literal)]。

    逐字符解析：第一个参数是 Context 表达式，第二个参数若是字符串字面量才收录。
    字面量内部的 ${...} 允许嵌套引号，按花括号配对与转义处理（正则做不到，见 HANDOFF 坑 2）。
    """
    out = []
    needle = "Toast.makeText("
    idx = 0
    while True:
        i = src.find(needle, idx)
        if i < 0:
            break
        j = i + len(needle)
        # 第一个参数：扫到顶层逗号
        depth = 0
        k = j
        in_str = False
        while k < len(src):
            c = src[k]
            if in_str:
                if c == "\\":
                    k += 2
                    continue
                if c == '"':
                    in_str = False
                k += 1
                continue
            if c == '"':
                in_str = True
            elif c in "([{":
                depth += 1
            elif c in ")]}":
                if depth == 0:
                    break
                depth -= 1
            elif c == "," and depth == 0:
                break
            k += 1
        if k >= len(src) or src[k] != ",":
            idx = j
            continue
        ctx = src[j:k].strip()
        m = k + 1
        while m < len(src) and src[m] in " \t\n\r":
            m += 1
        if m >= len(src) or src[m] != '"':
            idx = j
            continue
        # 第二个参数是字符串字面量
        lit, end = scan_literal(src, m)
        if lit is None:
            idx = j
            continue
        out.append((i, m, end, ctx, lit))
        idx = end
    return out


def scan_literal(src, start):
    """从 start（指向开头的双引号）扫描完整字面量，返回 (内容, 结束位置后一位)。"""
    p = start + 1
    buf = []
    while p < len(src):
        c = src[p]
        if c == "\\":
            buf.append(src[p:p + 2])
            p += 2
            continue
        if c == "$" and p + 1 < len(src) and src[p + 1] == "{":
            buf.append("${")
            p += 2
            depth = 1
            while p < len(src) and depth:
                ch = src[p]
                if ch == "\\":
                    buf.append(src[p:p + 2])
                    p += 2
                    continue
                if ch == '"':
                    # 表达式里的嵌套字符串
                    buf.append(ch)
                    p += 1
                    while p < len(src):
                        if src[p] == "\\":
                            buf.append(src[p:p + 2])
                            p += 2
                            continue
                        buf.append(src[p])
                        if src[p] == '"':
                            p += 1
                            break
                        p += 1
                    continue
                if ch == "{":
                    depth += 1
                elif ch == "}":
                    depth -= 1
                buf.append(ch)
                p += 1
            continue
        if c == '"':
            return "".join(buf), p + 1
        buf.append(c)
        p += 1
    return None, None


def main():
    translations = load_tsv()
    catalog = json.load(open(CAT, encoding="utf-8"))
    dyn = json.load(open(DYN, encoding="utf-8")) if os.path.exists(DYN) else {}
    by_en = {c["en"]: c for c in catalog}
    used_keys = {c["key"] for c in catalog} | set(dyn)

    # ---- 1. 扫描所有调用点
    sites = []
    for dirpath, _dirs, files in os.walk(SRC):
        for fn in files:
            if not fn.endswith(".kt"):
                continue
            path = os.path.join(dirpath, fn)
            src = open(path, encoding="utf-8").read()
            for call in find_calls(src):
                sites.append((path, src, call))

    seen_ens = {call[4] for _p, _s, call in sites}
    untracked = sorted(seen_ens - set(translations))
    if untracked:
        print("以下字面量在 TSV 里没有译文，将保持英文：")
        for t in untracked:
            print("  !", t[:100])
    not_found = sorted(set(translations) - seen_ens)
    if not_found:
        print("以下 TSV 条目在源码里没找到对应调用点：")
        for t in not_found:
            print("  ?", t[:100])

    # ---- 2. 写入 catalog / dynamic（缺 entry 才补，已有译文沿用）
    key_by_en = {}
    for en, zh in translations.items():
        prev = by_en.get(en)
        if prev and prev.get("zh"):
            key_by_en[en] = prev["key"]
            continue
        mods = {module_of(p) for p, s, c in sites if c[4] == en}
        mod = mods.pop() if len(mods) == 1 else "common"
        key = f"{mod}_toast_{slug(en)}"
        n = 2
        while key in used_keys:
            key = f"{mod}_toast_{slug(en)}_{n}"
            n += 1
        used_keys.add(key)
        key_by_en[en] = key
        exprs = find_exprs(en)
        if exprs:
            dyn[key] = {"en": en, "zh": zh, "args": exprs, "module": mod}
            catalog.append({"key": key, "module": mod, "en": en, "zh": zh, "count": 0,
                            "sites": [], "orphan": False})
        else:
            catalog.append({"key": key, "module": mod, "en": en, "zh": zh, "count": 0,
                            "sites": [], "orphan": False})

    for key, mod, en, zh in EXTRA_RESOURCES:
        if key in used_keys:
            continue
        used_keys.add(key)
        catalog.append({"key": key, "module": mod, "en": en, "zh": zh, "count": 0,
                        "sites": [], "orphan": False})
        print(f"补充资源 {key}: {en} -> {zh}")

    # ---- 3. 改写调用点（从后往前替换，保持偏移有效）
    by_file = {}
    for path, src, call in sites:
        by_file.setdefault(path, []).append((src, call))

    total = skipped = 0
    changed = []
    for path, entries in by_file.items():
        src = entries[0][0]
        repl = []
        for _s, (call_start, lit_start, lit_end, ctx, lit) in entries:
            key = key_by_en.get(lit)
            if not key:
                skipped += 1
                continue
            exprs = find_exprs(lit)
            if exprs:
                args = []
                for e in exprs:
                    spec = SPECIAL_ARGS.get(e)
                    args.append(spec.replace("{ctx}", ctx) if spec else e)
                new = f"{ctx}.getString(R.string.{key}, " + ", ".join(args) + ")"
            else:
                new = f"{ctx}.getString(R.string.{key})"
            repl.append((lit_start, lit_end, new, ctx, lit, key))
        if not repl:
            continue
        for lit_start, lit_end, new, ctx, lit, key in sorted(repl, reverse=True):
            src = src[:lit_start] + new + src[lit_end:]
        total += len(repl)
        changed.append((path, len(repl)))
        if not DRY:
            open(path, "w", encoding="utf-8").write(src)

    for path, n in changed:
        print(f"{'[dry-run] ' if DRY else ''}{os.path.relpath(path, ROOT)}: {n}")
    print(f"\n{'[dry-run] ' if DRY else ''}改写 {total} 处，跳过 {skipped} 处，新增资源 {len(key_by_en)} 条")

    if not DRY:
        json.dump(catalog, open(CAT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
        json.dump(dyn, open(DYN, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
        print(f"catalog: {len(catalog)} 条 / dynamic: {len(dyn)} 条")


if __name__ == "__main__":
    main()
