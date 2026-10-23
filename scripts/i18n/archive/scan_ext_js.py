#!/usr/bin/env python3
"""扫描 extensions/ 下 JS 文件的字符串字面量，输出「文件 + 行号 + 原文」清单。

逐字符解析：单引号 / 双引号 / 反引号（模板串），处理反斜杠转义与注释，
行号按位置现算（坑 3：不能靠累加变量）。

用法：python3 scripts/i18n/scan_ext_js.py [扩展目录] > /tmp/ext_strings.tsv
"""
import os
import sys


def scan(path):
    src = open(path, encoding='utf-8').read()
    n = len(src)
    i = 0
    out = []
    while i < n:
        c = src[i]
        if c == '/' and i + 1 < n and src[i + 1] == '/':
            j = src.find('\n', i)
            i = n if j < 0 else j + 1
            continue
        if c == '/' and i + 1 < n and src[i + 1] == '*':
            j = src.find('*/', i + 2)
            i = n if j < 0 else j + 2
            continue
        if c in '"\'':
            quote = c
            start = i
            i += 1
            buf = []
            while i < n:
                ch = src[i]
                if ch == '\\':
                    buf.append(src[i:i + 2])
                    i += 2
                    continue
                if ch == quote:
                    i += 1
                    break
                if ch == '\n':
                    break
                buf.append(ch)
                i += 1
            line = src.count('\n', 0, start) + 1
            out.append((path, line, quote, ''.join(buf)))
            continue
        if c == '`':
            start = i
            i += 1
            buf = []
            while i < n:
                ch = src[i]
                if ch == '\\':
                    buf.append(src[i:i + 2])
                    i += 2
                    continue
                if ch == '`':
                    i += 1
                    break
                buf.append(ch)
                i += 1
            line = src.count('\n', 0, start) + 1
            out.append((path, line, '`', ''.join(buf)))
            continue
        i += 1
    return out


def main():
    root = sys.argv[1] if len(sys.argv) > 1 else 'extensions'
    rows = []
    for base, _dirs, files in os.walk(root):
        for f in sorted(files):
            if f.endswith('.js'):
                rows.extend(scan(os.path.join(base, f)))
    for path, line, quote, text in rows:
        text = text.replace('\t', '\\t').replace('\n', '\\n')
        print(f'{path}\t{line}\t{quote}\t{text}')


if __name__ == '__main__':
    main()
