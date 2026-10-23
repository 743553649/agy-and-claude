#!/usr/bin/env python3
"""补齐 stringResource / R 的 import：任何用到 R.string.<key> 的 Kotlin 文件都必须有这两条 import。"""
import os
import re
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SRC = os.path.join(ROOT, "app/src/main/java")

R_IMPORT = "import com.example.gemini.R"
SR_IMPORT = "import androidx.compose.ui.res.stringResource"

dry = "--dry-run" in sys.argv
fixed = []

for dirpath, _dirs, files in os.walk(SRC):
    for fn in files:
        if not fn.endswith(".kt"):
            continue
        path = os.path.join(dirpath, fn)
        src = open(path, encoding="utf-8").read()
        need_r = re.search(r"\bR\.string\.", src)
        need_sr = "stringResource(" in src
        add = []
        if need_sr and "\n" + SR_IMPORT + "\n" not in src:
            add.append(SR_IMPORT)
        if need_r and "\n" + R_IMPORT + "\n" not in src:
            add.append(R_IMPORT)
        if not add:
            continue
        lines = src.split("\n")
        # 插到最后一个 import 行之后，保证落在 import 块内
        last = max(i for i, l in enumerate(lines) if l.startswith("import "))
        lines[last + 1:last + 1] = add
        new = "\n".join(lines)
        fixed.append((os.path.relpath(path, ROOT), add))
        if not dry:
            open(path, "w", encoding="utf-8").write(new)

for rel, add in fixed:
    print(f"{rel} <- {', '.join(a.split('.')[-1] for a in add)}")
print(f"\n{'[dry-run] ' if dry else ''}fixed={len(fixed)}")
