#!/usr/bin/env python3
"""Kotlin 源码文案扫描器（正确版本）。

正则无法处理 `${if (x == 1) "" else "s"}` 这类插值里嵌套引号的情况，
会截断成半截字符串。这里用逐字符扫描：
- 只在标识符边界上识别 Text( / newPlainText( 之外的 Text(
- 字符串字面量支持 \\ 转义与 ${...} 花括号配对（配对内部允许出现引号）
"""
import json
import os
import re
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SRC = os.path.join(ROOT, "app/src/main/java")
OUT = "/tmp/i18n_scan2.json"

IDENT = set("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_$")
ATTRS = ("label", "title", "placeholder", "message", "text", "contentDescription")


def read_literal(src, i):
    """src[i] 必须是 '"'。返回 (内容, 结束位置) 或 (None, i)。"""
    assert src[i] == '"'
    i += 1
    buf = []
    n = len(src)
    while i < n:
        ch = src[i]
        if ch == "\\":
            buf.append(src[i:i + 2])
            i += 2
            continue
        if ch == '"':
            return "".join(buf), i + 1
        if ch == "$" and i + 1 < n and src[i + 1] == "{":
            depth = 1
            j = i + 2
            while j < n and depth:
                if src[j] == "{":
                    depth += 1
                elif src[j] == "}":
                    depth -= 1
                j += 1
            buf.append(src[i:j])
            i = j
            continue
        buf.append(ch)
        i += 1
    return None, i


def scan_file(path):
    """逐字符扫描，行号按记录位置现算，避免跨行字符串/注释导致的偏移。"""
    src = open(path, encoding="utf-8").read()
    rows = []
    i = 0
    n = len(src)

    def line_of(pos):
        return src.count("\n", 0, pos) + 1

    while i < n:
        ch = src[i]
        # 跳过行注释
        if ch == "/" and i + 1 < n and src[i + 1] == "/":
            j = src.find("\n", i)
            i = n if j < 0 else j
            continue
        # 跳过块注释
        if ch == "/" and i + 1 < n and src[i + 1] == "*":
            j = src.find("*/", i)
            i = n if j < 0 else j + 2
            continue
        # Text( 调用（左侧不能是标识符字符，排除 newPlainText( 之类）
        if src.startswith("Text(", i) and (i == 0 or src[i - 1] not in IDENT):
            j = i + 5
            while j < n and src[j] in " \t\n":
                j += 1
            if j < n and src[j] == '"':
                content, end = read_literal(src, j)
                if content is not None:
                    rows.append((line_of(i), "Text", content))
                    i = end
                    continue
            i += 1
            continue
        # 属性名 = "字面量"
        matched = False
        for attr in ATTRS:
            if src.startswith(attr, i) and (i == 0 or src[i - 1] not in IDENT):
                j = i + len(attr)
                k = j
                while k < n and src[k] in " \t\n":
                    k += 1
                if k < n and src[k] == "=" and not src.startswith("==", k):
                    k += 1
                    while k < n and src[k] in " \t\n":
                        k += 1
                    if k < n and src[k] == '"':
                        content, end = read_literal(src, k)
                        if content is not None:
                            rows.append((line_of(i), attr, content))
                            i = end
                            matched = True
                            break
                break
        if matched:
            continue
        i += 1
    return rows


def main():
    out = []
    for dirpath, _dirs, files in os.walk(SRC):
        for fn in files:
            if not fn.endswith(".kt"):
                continue
            path = os.path.join(dirpath, fn)
            rel = os.path.relpath(path, ROOT)
            src = open(path, encoding="utf-8").read()
            if "Log." in src:
                pass
            for line, kind, text in scan_file(path):
                if not re.search(r"[A-Za-z]{2,}", text):
                    continue
                out.append({"file": rel, "line": line, "kind": kind, "text": text})
    json.dump(out, open(OUT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    print(f"scanned {len(out)} literals -> {OUT}", file=sys.stderr)


if __name__ == "__main__":
    main()
