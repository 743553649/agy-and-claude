#!/usr/bin/env python3
"""改写分享/导出流程里会直接显示给用户的文案（非 Composable 上下文，走 getString）。

为什么单独一轮：`AppToastHelper.showToast` 是项目自己的 Toast 包装，不叫 `Toast.makeText`
（apply_toast.py 扫不到，HANDOFF 第 4 节 P3 遗留）；`Intent.createChooser(intent, "标题")`
的标题也是系统弹给用户看的。两者的字面量都在 Compose 文件里，但调用点位于
onClick / LaunchedEffect / object 方法中，**不是 @Composable 上下文，不能用 stringResource**。

站点框架（两道限定：只认这两种调用、只改表内原文）：
  1) `showToast("<原文>", …)` 的第一个实参
  2) `Intent.createChooser(intent, "<原文>")` 的第二个实参

接收者按文件显式映射（RECEIVERS），未列出的文件直接报错，避免猜：
  - Compose 界面文件：函数里已有 `val context = LocalContext.current`
  - ChatViewModel：`getApplication<Application>()`
  - 组件/助手 object：方法签名里的 `context: Context`

必须包 main 保护（HANDOFF 坑 9）。

用法:
  apply_showtoast.py --dry-run   只打印将要做的改写
  apply_showtoast.py             实际写入源码与 catalog
"""
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from dynamic import find_exprs  # noqa: E402

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SRC = os.path.join(ROOT, "app/src/main/java")
PREFIX = os.path.join(SRC, "com/example/gemini")
CAT = "/tmp/i18n_catalog.json"
DYN = "/tmp/i18n_dynamic.json"
TSV = os.path.join(ROOT, "scripts/i18n/showtoast_zh.tsv")

DRY = "--dry-run" in sys.argv

# 每种站点的接收者（未列出的文件直接报错，不做猜测）
RECEIVERS = {
    "ui/chat/ChatScreen.kt": "context",
    "ui/chat/ChatViewModel.kt": "getApplication<Application>()",
    "ui/drawer/ChatHistoryDrawer.kt": "context",
    "ui/ide/ProjectSidebar.kt": "context",
    "ui/components/ConversationExportHelper.kt": "context",
    "ui/components/ConversationShareHelper.kt": "context",
    "ui/components/ArtifactDownloadHelper.kt": "context",
    "ui/components/ImageDownloadHelper.kt": "context",
    "ui/components/ChatInputBar.kt": "context",
    "ui/components/MessageBubble.kt": "context",
}

# 两种框架：(调用名, 取第几个实参, key 前缀)
FRAMES = [
    ("showToast(", 0, "apptoast"),
    ("createChooser(", 1, "chooser"),
]

# 可空实参补 .orEmpty()（HANDOFF 坑 8）
SPECIAL_ARGS = {
    "stepsRes.exceptionOrNull()?.message": "stepsRes.exceptionOrNull()?.message.orEmpty()",
    "e.message": "e.message.orEmpty()",
}

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


def scan_literal(src, start):
    """逐字符扫描完整字面量（${...} 允许嵌套引号，见 HANDOFF 坑 2）。"""
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


def find_call_args(src, needle):
    out = []
    idx = 0
    while True:
        i = src.find(needle, idx)
        if i < 0:
            break
        j = i + len(needle)
        args = []
        depth = 0
        start = k = j
        while k < len(src):
            c = src[k]
            if c == '"':
                lit, end = scan_literal(src, k)
                if lit is None:
                    break
                k = end
                continue
            if c in "([{":
                depth += 1
            elif c in ")]}":
                if depth == 0:
                    args.append((start, k))
                    break
                depth -= 1
            elif c == "," and depth == 0:
                args.append((start, k))
                start = k + 1
            k += 1
        out.append((i, args))
        idx = k + 1
    return out


def sole_literal(src, a, b):
    s = a
    while s < b and src[s] in " \t\n\r":
        s += 1
    if s >= b or src[s] != '"':
        return None
    lit, end = scan_literal(src, s)
    if lit is None:
        return None
    t = end
    while t < b and src[t] in " \t\n\r":
        t += 1
    if t != b:
        return None
    return (s, end, lit)


def main():
    translations = load_tsv()
    catalog = json.load(open(CAT, encoding="utf-8"))
    dyn = json.load(open(DYN, encoding="utf-8")) if os.path.exists(DYN) else {}
    by_en = {c["en"]: c for c in catalog}
    used_keys = {c["key"] for c in catalog} | set(dyn)

    sites = []  # (file, start, end, literal, receiver, frame)
    for dirpath, _dirs, files in os.walk(PREFIX):
        for fn in files:
            if not fn.endswith(".kt"):
                continue
            path = os.path.join(dirpath, fn)
            rel = os.path.relpath(path, PREFIX)
            src = open(path, encoding="utf-8").read()
            for needle, arg_idx, _prefix in FRAMES:
                for pos, args in find_call_args(src, needle):
                    if len(args) <= arg_idx:
                        continue
                    line_start = src.rfind("\n", 0, pos) + 1
                    if needle == "showToast(" and re.search(r"fun\s+showToast\s*$", src[line_start:pos].rstrip()):
                        continue  # 函数定义
                    found = sole_literal(src, *args[arg_idx])
                    if not found:
                        continue
                    sites.append((rel, found[0], found[1], found[2], needle))

    seen_ens = {s[3] for s in sites}
    untracked = sorted(seen_ens - set(translations))
    missing = sorted(set(translations) - seen_ens)
    if untracked:
        print("以下站点的字面量不在 TSV 里，保持英文：")
        for t in untracked:
            print("  !", t[:100])
    if missing:
        for t in missing:
            print("  ? TSV 条目无站点：", t[:100])
        raise SystemExit("TSV 里有条目在源码中找不到站点，先核对再跑")

    # ---- 1. 资源 key
    key_by_en = {}
    for en, zh in translations.items():
        prev = by_en.get(en)
        if prev and prev.get("zh"):
            key_by_en[en] = prev["key"]
            continue
        mods = {module_of(f"com/example/gemini/{rel}") for rel, _s, _e, lit, frame in sites
                if lit == en and frame == "showToast("}
        mods |= {module_of(f"com/example/gemini/{rel}") for rel, _s, _e, lit, frame in sites
                 if lit == en and frame == "createChooser("}
        mod = mods.pop() if len(mods) == 1 else "common"
        prefix = "apptoast" if any(lit == en and frame == "showToast(" for _r, _s, _e, lit, frame in sites) else "chooser"
        key = f"{mod}_{prefix}_{slug(en)}"
        n = 2
        while key in used_keys:
            key = f"{mod}_{prefix}_{slug(en)}_{n}"
            n += 1
        used_keys.add(key)
        key_by_en[en] = key
        exprs = find_exprs(en)
        catalog.append({"key": key, "module": mod, "en": en, "zh": zh, "count": 0,
                        "sites": [], "orphan": False})
        if exprs:
            dyn[key] = {"en": en, "zh": zh, "args": exprs, "module": mod}

    # ---- 2. 组装替换
    by_file = {}
    for rel, start, end, lit, frame in sites:
        key = key_by_en.get(lit)
        if not key:
            continue
        if rel not in RECEIVERS:
            raise SystemExit(f"文件未在 RECEIVERS 里登记，先确认接收者：{rel}")
        receiver = RECEIVERS[rel]
        exprs = find_exprs(lit)
        if exprs:
            args = [SPECIAL_ARGS.get(e, e) for e in exprs]
            new = f"{receiver}.getString(R.string.{key}, " + ", ".join(args) + ")"
        else:
            new = f"{receiver}.getString(R.string.{key})"
        by_file.setdefault(rel, []).append((start, end, new, lit))

    total = 0
    for rel, items in sorted(by_file.items()):
        path = os.path.join(PREFIX, rel)
        src = open(path, encoding="utf-8").read()
        spans = sorted((s, e) for s, e, _n, _l in items)
        for (_a, b), (c, _d) in zip(spans, spans[1:]):
            if b > c:
                raise SystemExit(f"替换区间重叠：{rel} {b} > {c}")
        for start, end, new, lit in sorted(items, reverse=True):
            if not DRY:
                ln = src.count("\n", 0, start) + 1
                print(f"  {rel}:{ln}  {lit[:48]!r} -> {new[:70]}")
            src = src[:start] + new + src[end:]
        total += len(items)
        if not DRY:
            open(path, "w", encoding="utf-8").write(src)

    print(f"\n{'[dry-run] ' if DRY else ''}改写 {total} 处，资源 {len(key_by_en)} 条")
    if DRY:
        return
    json.dump(catalog, open(CAT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    json.dump(dyn, open(DYN, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    print(f"catalog: {len(catalog)} 条 / dynamic: {len(dyn)} 条")


if __name__ == "__main__":
    main()
