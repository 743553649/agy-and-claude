#!/usr/bin/env python3
"""项目侧栏（ProjectSidebar）的空态与新建对话框文案（6 处，纯 UI）。

用法：apply_sidebar.py --dry-run / apply_sidebar.py → 三步流水线。
"""
import json
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SRC = "app/src/main/java/com/example/gemini/"
CAT = "/tmp/i18n_catalog.json"
DYNF = "/tmp/i18n_dynamic.json"
DRY = "--dry-run" in sys.argv

PS = "ui/ide/ProjectSidebar.kt"

SITES = [
    (PS, 'text = if (activeProject == null) "Open or create a project to view files" else "Project is empty",',
     'text = if (activeProject == null) stringResource(R.string.ide_sidebar_no_project) else stringResource(R.string.ide_sidebar_empty_project),', 1),
    (PS, 'title = { Text(if (isNewFolderMode) "Create Folder" else "Create File", fontWeight = FontWeight.Bold) },',
     'title = { Text(if (isNewFolderMode) stringResource(R.string.ide_create_folder) else stringResource(R.string.ide_create_file), fontWeight = FontWeight.Bold) },', 1),
    (PS, 'label = { Text(if (isNewFolderMode) "Folder Name" else "File Name") },',
     'label = { Text(if (isNewFolderMode) stringResource(R.string.component_folder_name) else stringResource(R.string.ide_file_name)) },', 1),
]

RESOURCES = [
    ("ide_sidebar_no_project", "Open or create a project to view files",
     "打开或新建一个项目以查看文件"),
    ("ide_sidebar_empty_project", "Project is empty", "项目为空"),
    ("ide_create_folder", "Create Folder", "新建文件夹"),
    ("ide_create_file", "Create File", "新建文件"),
    ("ide_file_name", "File Name", "文件名称"),
]


def main():
    errors, changed = [], 0
    files = {}
    for rel, old, new, count in SITES:
        path = SRC + rel
        text = files.get(path) or open(path, encoding="utf-8").read()
        found = text.count(old)
        if found != count:
            errors.append(f"[{rel}] 期望 {count} 处，实际 {found}：{old[:70]}")
            files[path] = text
            continue
        files[path] = text.replace(old, new)
        changed += count
    if not DRY and not errors:
        for path, text in files.items():
            open(path, "w", encoding="utf-8").write(text)
    print(f"改写 {changed} 处")

    catalog = json.load(open(CAT, encoding="utf-8"))
    keys = {c["key"] for c in catalog}
    ens = {c["en"] for c in catalog}
    added = 0
    for key, en, zh in RESOURCES:
        if key in keys:
            continue
        if en in ens:
            errors.append(f"英文重复（应复用既有 key）: {en}")
            continue
        catalog.append({"key": key, "module": "ide", "en": en, "zh": zh,
                        "count": 1, "sites": [], "orphan": False})
        keys.add(key); ens.add(en); added += 1
    if not DRY:
        json.dump(catalog, open(CAT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    print(f"新增静态资源 {added} 条")
    if errors:
        for e in errors:
            print("  " + e)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
