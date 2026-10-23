#!/usr/bin/env python3
"""找「自定义组合函数实参里的英文」——上一轮漏翻的主因。

为什么需要：`Hint("...")` / `SectionTitle("...")` / `Text("...")` / `ControlChip("...")`
这类实参不走 `text =`，行内正则按语句匹配时会被漏掉；`listOf("a" to "b")` / `Triple(...)`
里的英文同理。本脚本按「调用实参」收集，专门抓这些形态。

用法：find_untranslated_args.py [文件...]
输出：文件:行号 \t 调用名 \t 英文原文
"""
import os
import re
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
# 这些是已知会渲染给用户的组合函数 / 构造器
CALLS = r'(Hint|SectionTitle|ControlChip|DetailRow|Badge|UsageBar|Text|listOf|Triple|Icon)'
PAT = re.compile(CALLS + r'\s*\(\s*"([^"\\]{3,200})"')
TECH = re.compile(r'^(https?://|/|\.|[A-Za-z0-9_\-]+\.(kt|java|json|xml|md)|[a-z_]{2,}[A-Za-z_]*$)')
CJK = re.compile(r'[\u4e00-\u9fff]')


def main():
    files = sys.argv[1:]
    if not files:
        base = os.path.join(ROOT, "app", "src", "main", "java")
        files = [os.path.join(dp, f) for dp, _, fs in os.walk(base) for f in fs if f.endswith(".kt")]
    total = 0
    for path in files:
        for i, line in enumerate(open(path, encoding="utf-8"), 1):
            if 'R.string.' in line:
                continue
            for m in PAT.finditer(line):
                t = m.group(2)
                if CJK.search(t) or TECH.match(t):
                    continue
                if not re.search(r'[A-Za-z]{3}', t):
                    continue
                print(f"{os.path.relpath(path, ROOT)}:{i}\t{m.group(1)}\t{t[:100]}")
                total += 1
    print(f"\n候选 {total} 条")
    return 0


if __name__ == "__main__":
    sys.exit(main())
