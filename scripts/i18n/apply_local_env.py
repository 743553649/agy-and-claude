#!/usr/bin/env python3
"""把数据层（LocalEnvironmentManager）里会显示给用户的文案改成 getString(R.string.*)。

为什么单独一轮：这些字符串在非 Composable 的 Manager 里，最终进系统通知
（NotificationCompat 标题/正文）和安装页（FullScreenLocalToolsInstaller /
LocalToolsInstallDialog 读 LocalInstallerState 的 speedText/currentFileName/
stepDescription/testName/errorMessage/message 字段）。所以不能用 stringResource。

与 apply_toast.py 的差异：
- 站点不是统一的 `Toast.makeText(ctx, lit)`，而是几类固定语法位置，用「语法框架 + 对照表」
  双重限定：只改 local_env_zh.tsv 里列出的原文，且只改下面三类位置，其它字面量一律不动：
    1) updateInstallNotification(<ctx>, "<标题>", "<正文>", ...) 的第 2、3 个实参
    2) 固定字段赋值：speedText / errorMsg / currentFileName / stepDescription /
       testName / message / currentPackageName / errorMessage = "<原文>"
    3) 通知渠道名（NotificationChannel 第 2 个实参）与 `description = "<原文>"`
- 接收者不是固定名字：通知标题/正文沿用调用点自己的第一个实参（当前是 appContext），
  字段赋值用 appContext，通知渠道那两处用 setupInstallNotificationChannel 的形参 context。
- 表达式分支里的标题前缀（`val titlePrefix = if (isTermux) "AntiGem Rootfs" else ...`）
  不在上面任何框架里，用 SNIPPETS 定点替换（见 HANDOFF 坑 11）。

必须包 main 保护（HANDOFF 坑 9：import 一下就会真改源码）。

用法:
  apply_local_env.py --dry-run   只打印将要做的改写
  apply_local_env.py             实际写入源码与 catalog
"""
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from dynamic import find_exprs  # noqa: E402

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
TARGET = os.path.join(
    ROOT, "app/src/main/java/com/example/gemini/data/local/LocalEnvironmentManager.kt"
)
CAT = "/tmp/i18n_catalog.json"
DYN = "/tmp/i18n_dynamic.json"
TSV = os.path.join(ROOT, "scripts/i18n/local_env_zh.tsv")

DRY = "--dry-run" in sys.argv

# 站点框架：只认这些左值，其它 `xxx = "..."` 一律不碰
ASSIGN_LHS_CTX = ("speedText", "errorMsg", "currentFileName", "stepDescription",
                  "testName", "message", "currentPackageName", "errorMessage")
ASSIGN_LHS_CONTEXT = ("description",)  # setupInstallNotificationChannel 内，接收者是 context

# 参数表达式里内嵌的字面量，需要就地换成资源（否则中文界面里会露出英文）
SPECIAL_ARGS = {
    'e.message ?: "Network error"':
        'e.message ?: {ctx}.getString(R.string.data_install_network_error)',
    # getString 的形参是平台类型，允许传可空；但两个都为空时会显示字面 "null"
    # （HANDOFF 坑 8 的既有做法：统一补 .orEmpty()）
    'e.localizedMessage ?: e.message':
        '(e.localizedMessage ?: e.message).orEmpty()',
}

# 只作参数/表达式片段用、源码里没有独立站点的文案
#   Network error：在 `e.message ?: "Network error"` 里，由 SPECIAL_ARGS 引用
#   AntiGem Rootfs / Ubuntu Rootfs：在 `if (isTermux) ... else ...` 里，由 SNIPPETS 覆盖
EXTRA_LITERALS = {"Network error", "AntiGem Rootfs", "Ubuntu Rootfs"}

# 表达式分支里的站点（HANDOFF 坑 11：when/if 分支的字面量不在任何扫描框架内）
SNIPPETS = [
    (
        'val titlePrefix = if (isTermux) "AntiGem Rootfs" else "Ubuntu Rootfs"',
        'val titlePrefix = if (isTermux) appContext.getString(R.string.data_install_antigem_rootfs) '
        'else appContext.getString(R.string.data_install_ubuntu_rootfs)',
    ),
]


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
    """从 start（指向开头的双引号）扫描完整字面量，返回 (内容, 结束位置后一位)。

    字面量内部的 ${...} 允许嵌套引号，按花括号配对与转义处理（正则做不到，见 HANDOFF 坑 2）。
    """
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


def scan_literals(src):
    """返回 [(start, end, literal)]，跳过行注释、块注释与三引号原始字符串。"""
    out = []
    i, n = 0, len(src)
    while i < n:
        c = src[i]
        if c == "/" and i + 1 < n and src[i + 1] == "/":
            while i < n and src[i] != "\n":
                i += 1
            continue
        if c == "/" and i + 1 < n and src[i + 1] == "*":
            j = src.find("*/", i + 2)
            i = n if j < 0 else j + 2
            continue
        if src.startswith('"""', i):
            j = src.find('"""', i + 3)
            i = n if j < 0 else j + 3
            continue
        if c == '"':
            lit, end = scan_literal(src, i)
            if lit is None:
                break
            out.append((i, end, lit))
            i = end
            continue
        i += 1
    return out


def find_call_args(src, needle):
    """返回 [(call_start, [(arg_start, arg_end), ...])]，参数按顶层逗号切分。"""
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
    """实参区间若只包含一个字面量，返回 (start, end, literal)。"""
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


def slug(text, limit=40):
    s = re.sub(r"[^a-z0-9]+", "_", text.lower()).strip("_")
    return s[:limit].rstrip("_") or "empty"


def collect_sites(src):
    """返回 [(start, end, literal, receiver)]，只收三类框架内的字面量。"""
    sites = []
    lits = scan_literals(src)
    for _call, args in find_call_args(src, "updateInstallNotification("):
        if len(args) < 3:
            continue
        ctx = src[args[0][0]:args[0][1]].strip()
        for idx in (1, 2):  # 标题、正文
            a, b = args[idx]
            found = sole_literal(src, a, b)
            if found:
                sites.append((found[0], found[1], found[2], ctx))
                continue
            # 实参是表达式：把区间内的字面量逐个改写（如 e.localizedMessage ?: "Installation error"）
            for start, end, lit in lits:
                if a <= start and end <= b:
                    sites.append((start, end, lit, ctx))

    for start, end, lit in lits:
        line_start = src.rfind("\n", 0, start) + 1
        prefix = src[line_start:start]
        m = re.search(r"(?:^|[\s,(])([A-Za-z_][A-Za-z0-9_]*)\s*=\s*$", prefix)
        if m and m.group(1) in ASSIGN_LHS_CTX:
            sites.append((start, end, lit, "appContext"))
            continue
        if m and m.group(1) in ASSIGN_LHS_CONTEXT:
            sites.append((start, end, lit, "context"))
            continue
        head = src[max(0, start - 60):start].rstrip()
        if head.endswith("NOTIFICATION_CHANNEL_ID,"):
            sites.append((start, end, lit, "context"))

    # 去重（通知渠道名既可能被上面的框架命中，也不会重复，这里只是兜底）
    seen, uniq = set(), []
    for s in sites:
        if s[0] in seen:
            continue
        seen.add(s[0])
        uniq.append(s)
    return uniq


def main():
    translations = load_tsv()
    src = open(TARGET, encoding="utf-8").read()
    catalog = json.load(open(CAT, encoding="utf-8"))
    dyn = json.load(open(DYN, encoding="utf-8")) if os.path.exists(DYN) else {}
    by_en = {c["en"]: c for c in catalog}
    used_keys = {c["key"] for c in catalog} | set(dyn)

    sites = collect_sites(src)
    seen_ens = {s[2] for s in sites}
    untracked = sorted(seen_ens - set(translations))
    missing = sorted(set(translations) - seen_ens)
    if untracked:
        print("以下站点的字面量不在 TSV 里，保持英文：")
        for t in untracked:
            print("  !", t[:100])
    if missing:
        bad = [t for t in missing if t not in EXTRA_LITERALS]
        for t in missing:
            print(("  ? 仅作参数文案：" if t in EXTRA_LITERALS else "  ? TSV 条目无站点：") + t[:100])
        if bad:
            raise SystemExit("TSV 里有条目在源码中找不到站点，先核对再跑（避免写出无人使用的资源）")

    # ---- 1. 资源 key：已有译文沿用，新增用 data_install_<slug>
    key_by_en = {}
    for en, zh in translations.items():
        prev = by_en.get(en)
        if prev and prev.get("zh"):
            key_by_en[en] = prev["key"]
            continue
        key = f"data_install_{slug(en)}"
        n = 2
        while key in used_keys:
            key = f"data_install_{slug(en)}_{n}"
            n += 1
        used_keys.add(key)
        key_by_en[en] = key
        exprs = find_exprs(en)
        catalog.append({"key": key, "module": "data", "en": en, "zh": zh, "count": 0,
                        "sites": [], "orphan": False})
        if exprs:
            dyn[key] = {"en": en, "zh": zh, "args": exprs, "module": "data"}

    # ---- 2. 组装替换
    repl = []
    for start, end, lit, ctx in sites:
        key = key_by_en.get(lit)
        if not key:
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
        repl.append((start, end, new, lit, ctx))

    # ---- 3. 表达式分支的定点替换
    for old, new in SNIPPETS:
        n = src.count(old)
        if n != 1:
            raise SystemExit(f"SNIPPET 应恰好出现 1 次，实际 {n} 次：{old[:60]}")
        p = src.find(old)
        repl.append((p, p + len(old), new, old, "appContext"))

    # 重叠检查（同一位置被两套框架命中会写坏源码）
    spans = sorted((r[0], r[1]) for r in repl)
    for (_a, b), (c, _d) in zip(spans, spans[1:]):
        if b > c:
            raise SystemExit(f"替换区间重叠：{b} > {c}")

    for start, end, new, lit, ctx in sorted(repl, key=lambda r: r[0], reverse=True):
        src = src[:start] + new + src[end:]

    if DRY:
        print(f"\n[dry-run] 将改写 {len(repl)} 处")
        return

    open(TARGET, "w", encoding="utf-8").write(src)
    json.dump(catalog, open(CAT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    json.dump(dyn, open(DYN, "w", encoding="utf-8"), ensure_ascii=False, indent=1)

    print(f"\n改写 {len(repl)} 处，新增/复用资源 {len(key_by_en)} 条")
    print(f"catalog: {len(catalog)} 条 / dynamic: {len(dyn)} 条")


if __name__ == "__main__":
    main()
