#!/usr/bin/env python3
"""汉化改动的静态校验，供 CI 在编译前快速失败。

1. 资源文件必须是合法 XML
2. 每个 R.string.<key> 都必须在 values/ 中定义
3. values-zh-rCN 中的 key 不得超出 values/ 的范围
4. 不得把 stringResource 放进非 Composable 上下文（形如 xxxText(stringResource(...))）
"""
import os
import re
import sys
import xml.etree.ElementTree as ET

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
RES = os.path.join(ROOT, "app/src/main/res")
SRC = os.path.join(ROOT, "app/src/main/java")

errors = []


def load_keys(folder):
    keys = set()
    files = []
    d = os.path.join(RES, folder)
    if not os.path.isdir(d):
        return keys, files
    for fn in sorted(os.listdir(d)):
        if not fn.endswith(".xml"):
            continue
        path = os.path.join(d, fn)
        files.append(path)
        try:
            tree = ET.parse(path)
        except ET.ParseError as e:
            errors.append(f"XML 解析失败 {folder}/{fn}: {e}")
            continue
        for el in tree.getroot():
            if el.tag == "string" and el.get("name"):
                keys.add(el.get("name"))
    return keys, files


en_keys, en_files = load_keys("values")
zh_keys, zh_files = load_keys("values-zh-rCN")

used = {}
for dirpath, _dirs, files in os.walk(SRC):
    for fn in files:
        if not fn.endswith(".kt"):
            continue
        path = os.path.join(dirpath, fn)
        rel = os.path.relpath(path, ROOT)
        text = open(path, encoding="utf-8").read()
        for m in re.finditer(r"R\.string\.([A-Za-z0-9_]+)", text):
            used.setdefault(m.group(1), set()).add(rel)
        for m in re.finditer(r"[A-Za-z0-9_$]Text\(\s*stringResource\(", text):
            line = text[:m.start()].count("\n") + 1
            errors.append(f"非 Composable 上下文误用 stringResource: {rel}:{line}")

for key, files in sorted(used.items()):
    if key not in en_keys:
        errors.append(f"R.string.{key} 未在 values/ 中定义（{sorted(files)[0]}）")

# 用了裸 `R.string.` 的文件必须自己 import R，否则编译期 Unresolved reference 'R'
# （2026-10-22 踩过：apply_showtoast.py 改写 ChatViewModel 后漏跑 fix_imports.py，CI 才报红）
BARE_R = re.compile(r"(?<![.\w])R\.string\.")
R_IMPORT_LINE = "import com.example.gemini.R"
for dirpath, _dirs, files in os.walk(SRC):
    for fn in files:
        if not fn.endswith(".kt"):
            continue
        path = os.path.join(dirpath, fn)
        text = open(path, encoding="utf-8").read()
        if not BARE_R.search(text):
            continue
        if R_IMPORT_LINE not in text.split("\n"):
            rel = os.path.relpath(path, ROOT)
            errors.append(f"用到 R.string 但没有 import com.example.gemini.R: {rel}")

# 英文资源里不允许残留 Kotlin 插值语法（\$var / \${expr}），否则英文界面会显示原始代码
for path in en_files:
    try:
        tree = ET.parse(path)
    except ET.ParseError:
        continue
    for el in tree.getroot():
        if el.tag != "string":
            continue
        body = el.text or ""
        # 先剔除合法的位置化占位符 %1$s，再检查是否残留 Kotlin 插值
        probe = re.sub(r"%\d+\$[sd]", "", body)
        if "${" in probe or re.search(r"(?<!\\)\$[A-Za-z_]", probe):
            errors.append(f"英文资源残留 Kotlin 插值: {os.path.basename(path)} / {el.get('name')}")

# 含位置化占位符的字符串里不允许出现裸 %（必须写成 %%，否则 String.format 抛异常）
for folder, files in (("values", en_files), ("values-zh-rCN", zh_files)):
    for path in files:
        try:
            tree = ET.parse(path)
        except ET.ParseError:
            continue
        for el in tree.getroot():
            if el.tag != "string":
                continue
            body = el.text or ""
            if not re.search(r"%\d+\$[sd]", body):
                continue
            probe = re.sub(r"%\d+\$[sd]", "", body).replace("%%", "")
            if "%" in probe:
                errors.append(f"占位符字符串里有裸 %: {folder}/{os.path.basename(path)} / {el.get('name')}")

extra = zh_keys - en_keys
if extra:
    errors.append(f"values-zh-rCN 存在 values/ 中没有的 key: {sorted(extra)[:5]}")


# 占位符校验：中文资源里用了 %2$s 就必须在调用点至少传 2 个参数
MAXN = re.compile(r"%(\d+)\$")
call_args = {}


def count_args(text, start):
    """从 R.string.<key> 之后开始，数这一层调用一共传了几个参数。"""
    depth = 0
    commas = 0
    i = start
    n = len(text)
    while i < n:
        ch = text[i]
        if ch == '"':
            j = i + 1
            while j < n and text[j] != '"':
                j += 2 if text[j] == "\\" else 1
            i = j + 1
            continue
        if ch in "([{":
            depth += 1
        elif ch in ")]}":
            if depth == 0:
                break
            depth -= 1
        elif ch == "," and depth == 0:
            commas += 1
        elif ch == "\n":
            break
        i += 1
    return commas


for dirpath, _dirs, files in os.walk(SRC):
    for fn in files:
        if not fn.endswith(".kt"):
            continue
        path = os.path.join(dirpath, fn)
        rel = os.path.relpath(path, ROOT)
        text = open(path, encoding="utf-8").read()
        for m in re.finditer(r"R\.string\.([A-Za-z0-9_]+)", text):
            key = m.group(1)
            call_args.setdefault(key, set()).add(count_args(text, m.end()))

zh_map = {}
for path in zh_files:
    try:
        tree = ET.parse(path)
    except ET.ParseError:
        continue
    for el in tree.getroot():
        if el.tag == "string" and el.get("name") is not None:
            zh_map[el.get("name")] = el.text or ""

for key, counts in sorted(call_args.items()):
    zh = zh_map.get(key, "")
    found = [int(x) for x in MAXN.findall(zh)]
    if not found:
        continue
    need = max(found)
    if min(counts) < need:
        errors.append(f"R.string.{key} 需要 {need} 个参数，但存在只传 {min(counts)} 个的调用点")

print(f"values: {len(en_keys)} 条 / values-zh-rCN: {len(zh_keys)} 条 / 源码引用: {len(used)} 个 key")
if errors:
    print(f"\n发现 {len(errors)} 个问题：")
    for e in errors[:60]:
        print("  - " + e)
    sys.exit(1)
print("校验通过")
