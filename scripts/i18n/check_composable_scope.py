#!/usr/bin/env python3
"""静态检查「在非组合作用域里调用 stringResource」这一类错误（坑 1 及其变体）。

做法：找出所有 `remember(...)` / `LaunchedEffect(...)` / `DisposableEffect(...)` 的 lambda 体
（按括号配平），若体内出现 `stringResource(` 就报出来。不追求完备，但能覆盖
历史上反复踩到的那三种形态（remember 缓存文案、LaunchedEffect 里提示、onClick 里拼串）。
用法：check_composable_scope.py [文件...]，缺省扫全部 app/src/main/java 下的 .kt
"""
import os
import re
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
TARGETS = re.compile(r'\b(remember|rememberSaveable|LaunchedEffect|DisposableEffect)\s*\(')


def block_end(text, open_idx):
    """从 '(' 或 '{' 位置找到配平的收尾下标。"""
    depth = 0
    i = open_idx
    in_str = None
    while i < len(text):
        ch = text[i]
        if in_str:
            if ch == '\\':
                i += 2
                continue
            if ch == in_str:
                in_str = None
        elif ch in '"\'':
            in_str = ch
        elif ch == '(':
            depth += 1
        elif ch == ')':
            depth -= 1
        elif ch == '{':
            depth += 1
        elif ch == '}':
            depth -= 1
            if depth == 0:
                return i
        i += 1
    return -1


def main():
    files = sys.argv[1:]
    if not files:
        base = os.path.join(ROOT, "app", "src", "main", "java")
        files = [os.path.join(dp, f) for dp, _, fs in os.walk(base) for f in fs if f.endswith(".kt")]
    hits = 0
    for path in files:
        text = open(path, encoding="utf-8").read()
        for m in TARGETS.finditer(text):
            paren = text.index('(', m.end() - 1)
            end = block_end(text, paren)
            if end < 0:
                continue
            body = text[paren:end + 1]
            if "stringResource(" not in body:
                continue
            line = text.count("\n", 0, m.start()) + 1
            snippet = body.strip().split("\n")
            inner = next((l.strip() for l in snippet if "stringResource(" in l), "")
            print(f"{os.path.relpath(path, ROOT)}:{line}  {m.group(1)}(...) 内用了 stringResource → {inner[:110]}")
            hits += 1
    print(f"\n可疑处 {hits} 个")
    return 1 if hits else 0


if __name__ == "__main__":
    sys.exit(main())
