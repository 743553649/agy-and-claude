#!/usr/bin/env python3
"""把已翻译的文案回写到 Kotlin 源码。

安全策略：
- 只处理「无插值」且已有中文译文的条目
- 只替换 Text("...") 形式（属性字符串另走一轮人工确认）
- 只在含 @Composable 的文件里替换，避免非 Compose 上下文误用 stringResource
- 自动补 import
用法: apply.py [--dry-run]
"""
import json
import os
import re
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SRC = os.path.join(ROOT, "app/src/main/java")
CAT = "/tmp/i18n_catalog.json"

DRY = "--dry-run" in sys.argv

catalog = json.load(open(CAT, encoding="utf-8"))
by_text = {c["en"]: c["key"] for c in catalog if c["zh"] and "$" not in c["en"]}

R_IMPORT = "import com.example.gemini.R"
SR_IMPORT = "import androidx.compose.ui.res.stringResource"


def escape(text):
    return re.escape(text)


changed_files = []
total = 0

for dirpath, _dirs, files in os.walk(SRC):
    for fn in files:
        if not fn.endswith(".kt"):
            continue
        path = os.path.join(dirpath, fn)
        src = open(path, encoding="utf-8").read()
        if "@Composable" not in src:
            continue
        hits = 0
        for text, key in by_text.items():
            # 匹配 Text("原文") 以及 Text("原文", modifier = ...) 这类带参形式
            pat = r'(?<![A-Za-z0-9_$])Text\(\s*"' + escape(text) + r'"\s*(?=[,)])'
            new = f"Text(stringResource(R.string.{key})"
            src, n = re.subn(pat, new, src)
            hits += n
        if not hits:
            continue
        # 补 import
        if SR_IMPORT not in src:
            src = re.sub(r"(?m)^(import androidx\.compose\.material3\..*)$", r"\1\n" + SR_IMPORT, src, count=1)
            if SR_IMPORT not in src:
                src = re.sub(r"(?m)^(import androidx\.compose\..*)$", r"\1\n" + SR_IMPORT, src, count=1)
        if R_IMPORT not in src:
            src = re.sub(r"(?m)^(import com\.example\.gemini\..*)$", r"\1\n" + R_IMPORT, src, count=1)
        rel = os.path.relpath(path, ROOT)
        changed_files.append((rel, hits))
        total += hits
        if not DRY:
            open(path, "w", encoding="utf-8").write(src)

for rel, hits in sorted(changed_files, key=lambda x: -x[1]):
    print(f"{hits:4d}  {rel}")
print(f"\n{'[dry-run] ' if DRY else ''}files={len(changed_files)} replacements={total}")
