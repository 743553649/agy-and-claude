#!/usr/bin/env python3
"""按模块生成 values/ 与 values-zh-rCN/ 资源文件。

values/strings_<module>.xml     英文原文
values-zh-rCN/strings_<module>.xml  中文，条目上方保留 <!-- 原文: ... -->
只输出已翻译的条目，避免出现「有 key 无译文」的半成品。
"""
import collections
import json
import os
import re
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
RES = os.path.join(ROOT, "app/src/main/res")
CAT = "/tmp/i18n_catalog.json"

MODULE_FILE = {
    "settings": "strings_settings.xml",
    "ide": "strings_ide.xml",
    "claude": "strings_claude.xml",
    "browser": "strings_browser.xml",
    "drawer": "strings_drawer.xml",
    "chat": "strings_chat.xml",
    "agents": "strings_agents.xml",
    "component": "strings_components.xml",
    "ui": "strings_ui.xml",
    "data": "strings_data.xml",
    "common": "strings_common.xml",
}

HEADER = """<?xml version="1.0" encoding="utf-8"?>
<!--
  由 scripts/i18n 生成，请勿手工调整条目顺序。
  译法基准见 docs/i18n/GLOSSARY.md
-->
<resources>
"""


def xml_comment(s):
    """XML 注释里不允许出现 --，需要打断，否则资源文件解析失败。"""
    return s.replace("--", "- -")


def xml_escape(s):
    return (s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
             .replace("'", "\\'").replace('"', '\\"'))


catalog = json.load(open(CAT, encoding="utf-8"))
by_module = collections.defaultdict(list)
for c in catalog:
    if not c["zh"]:
        continue
    if "$" in c["en"]:
        continue
    by_module[c["module"]].append(c)

# 含插值的条目：中文里带 %1$s 占位符，参数由调用点按位置传入
DYN = "/tmp/i18n_dynamic.json"
if os.path.exists(DYN):
    for key, info in json.load(open(DYN, encoding="utf-8")).items():
        if not info.get("zh") or info["zh"] == "-" or not info.get("args"):
            continue
        by_module[info["module"]].append({
            "key": key,
            "en": info["en"],
            "zh": info["zh"],
            "module": info["module"],
        })

written = []
for module, items in sorted(by_module.items()):
    fn = MODULE_FILE.get(module)
    if not fn:
        continue
    items.sort(key=lambda c: c["key"])
    en_lines = [HEADER]
    zh_lines = [HEADER]
    for c in items:
        en_lines.append(f'    <string name="{c["key"]}">{xml_escape(c["en"])}</string>')
        zh_lines.append(f'    <!-- 原文: {xml_comment(c["en"])} -->')
        zh_lines.append(f'    <string name="{c["key"]}">{xml_escape(c["zh"])}</string>')
    en_lines.append("</resources>\n")
    zh_lines.append("</resources>\n")
    for sub, lines in (("values", en_lines), ("values-zh-rCN", zh_lines)):
        d = os.path.join(RES, sub)
        os.makedirs(d, exist_ok=True)
        with open(os.path.join(d, fn), "w", encoding="utf-8") as f:
            f.write("\n".join(lines))
        written.append(f"{sub}/{fn}")

print(f"generated {len(written)} files, {sum(len(v) for v in by_module.values())} entries")
for w in written:
    print("  " + w)
