#!/usr/bin/env python3
"""文件详情弹窗（FileDetailsDialog）与一处多行拼接残留（AgentsSetupUi）。

FileDetailsDialog 的要点：
- 文件类型名原先写在 `remember(isDir, ext) { ... }` 里——**记住的键不含语言**，
  切语言不会重算，所以这里必须去掉 remember 改成直接在组合作用域求值，
  才能用 stringResource（坑 1 的变体）。
- 权限串与目录项数在 remember 里且要做文件 IO，保留 remember，
  改用 `context.getString(...)`（context 已在函数开头取到）。
- `formatDetailedFileSize` 是顶层函数，加 `context` 形参才能取资源（仅本文件 2 处调用）。

AgentsSetupUi 那处是 `"..." + "..."` 多行拼接，扫描器（逐字符但按单行字面量收集）漏掉了。

用法：apply_files_ui.py --dry-run / apply_files_ui.py → gen_resources → fix_imports → verify。
"""
import json
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SRC = "app/src/main/java/com/example/gemini/"
CAT = "/tmp/i18n_catalog.json"
DYN = "/tmp/i18n_dynamic.json"
DRY = "--dry-run" in sys.argv

FD = "ui/components/FileDetailsDialog.kt"
AS = "ui/agents/AgentsSetupUi.kt"

SITES = [
    # ---- 导入 ----
    (FD, 'import android.widget.Toast\n', 'import android.content.Context\nimport android.widget.Toast\n', 1),
    # ---- 权限串（remember 内，需 context）----
    (FD, 'val readStr = if (localFile.canRead()) "Yes" else "No"',
     'val readStr = if (localFile.canRead()) context.getString(R.string.component_yes) else context.getString(R.string.component_no)', 1),
    (FD, 'val writeStr = if (localFile.canWrite()) "Yes" else "No"',
     'val writeStr = if (localFile.canWrite()) context.getString(R.string.component_yes) else context.getString(R.string.component_no)', 1),
    (FD, 'val execStr = if (localFile.canExecute()) "Yes" else "No"',
     'val execStr = if (localFile.canExecute()) context.getString(R.string.component_yes) else context.getString(R.string.component_no)', 1),
    (FD, '"$r$w$x (Read: $readStr, Write: $writeStr, Exec: $execStr)"',
     'context.getString(R.string.component_permissions_line, r, w, x, readStr, writeStr, execStr)', 1),
    (FD, '"Read/Write Access (Bridge Managed)"',
     'context.getString(R.string.component_bridge_managed_access)', 2),
    # ---- 文件类型名：去掉 remember，改用 stringResource ----
    (FD, '    val typeName = remember(isDir, ext) {\n        if (isDir) "Folder (Directory)"',
     '    val typeName = if (isDir) stringResource(R.string.component_file_type_folder)', 1),
    (FD, '"kt", "kts" -> "Kotlin Source File (.${ext})"',
     '"kt", "kts" -> stringResource(R.string.component_file_type_kt, ext)', 1),
    (FD, '"java" -> "Java Source File (.java)"',
     '"java" -> stringResource(R.string.component_file_type_java)', 1),
    (FD, '"py" -> "Python Script (.py)"',
     '"py" -> stringResource(R.string.component_file_type_py)', 1),
    (FD, '"js" -> "JavaScript File (.js)"',
     '"js" -> stringResource(R.string.component_file_type_js)', 1),
    (FD, '"ts" -> "TypeScript File (.ts)"',
     '"ts" -> stringResource(R.string.component_file_type_ts)', 1),
    (FD, '"jsx", "tsx" -> "React Component (.${ext})"',
     '"jsx", "tsx" -> stringResource(R.string.component_file_type_react, ext)', 1),
    (FD, '"html", "htm" -> "HTML Document (.${ext})"',
     '"html", "htm" -> stringResource(R.string.component_file_type_html, ext)', 1),
    (FD, '"css", "scss", "sass" -> "Stylesheet (.${ext})"',
     '"css", "scss", "sass" -> stringResource(R.string.component_file_type_css, ext)', 1),
    (FD, '"json" -> "JSON Data (.json)"',
     '"json" -> stringResource(R.string.component_file_type_json)', 1),
    (FD, '"xml" -> "XML Document (.xml)"',
     '"xml" -> stringResource(R.string.component_file_type_xml)', 1),
    (FD, '"yaml", "yml" -> "YAML Configuration (.${ext})"',
     '"yaml", "yml" -> stringResource(R.string.component_file_type_yaml, ext)', 1),
    (FD, '"toml" -> "TOML Configuration (.toml)"',
     '"toml" -> stringResource(R.string.component_file_type_toml)', 1),
    (FD, '"md", "markdown" -> "Markdown Document (.${ext})"',
     '"md", "markdown" -> stringResource(R.string.component_file_type_md, ext)', 1),
    (FD, '"txt", "log" -> "Text Document (.${ext})"',
     '"txt", "log" -> stringResource(R.string.component_file_type_txt, ext)', 1),
    (FD, '"sh", "bash", "zsh" -> "Shell Script (.${ext})"',
     '"sh", "bash", "zsh" -> stringResource(R.string.component_file_type_shell, ext)', 1),
    (FD, '"c", "cpp", "h", "hpp" -> "C/C++ Source (.${ext})"',
     '"c", "cpp", "h", "hpp" -> stringResource(R.string.component_file_type_c, ext)', 1),
    (FD, '"go" -> "Go Source File (.go)"',
     '"go" -> stringResource(R.string.component_file_type_go)', 1),
    (FD, '"rs" -> "Rust Source File (.rs)"',
     '"rs" -> stringResource(R.string.component_file_type_rs)', 1),
    (FD, '"png", "jpg", "jpeg", "webp", "gif", "svg" -> "Image Asset (.${ext})"',
     '"png", "jpg", "jpeg", "webp", "gif", "svg" -> stringResource(R.string.component_file_type_image, ext)', 1),
    (FD, '"zip", "tar", "gz", "rar" -> "Archive File (.${ext})"',
     '"zip", "tar", "gz", "rar" -> stringResource(R.string.component_file_type_archive, ext)', 1),
    (FD, '"" -> "File"', '"" -> stringResource(R.string.common_file)', 1),
    (FD, 'else -> "${ext.uppercase()} File (.$ext)"',
     'else -> stringResource(R.string.component_file_type_other, ext.uppercase(), ext)', 1),
    # 去掉 remember 的收尾括号（原来是 when 后两个 }）
    (FD, '''            else -> stringResource(R.string.component_file_type_other, ext.uppercase(), ext)
        }
    }
''', '''            else -> stringResource(R.string.component_file_type_other, ext.uppercase(), ext)
        }
''', 1),
    # ---- 徽标 / 复制路径 ----
    (FD, 'text = if (isDir) "DIRECTORY" else "FILE",',
     'text = if (isDir) stringResource(R.string.component_badge_directory) else stringResource(R.string.component_badge_file),', 1),
    (FD, 'actionDescription = "Copy Path",',
     'actionDescription = stringResource(R.string.component_copy_path),', 1),
    # ---- 大小行（remember 内，需 context）----
    (FD, 'if (childCount != null) "$childCount items"',
     'if (childCount != null) context.getString(R.string.component_child_items, childCount)', 1),
    (FD, 'else "Directory"', 'else context.getString(R.string.component_dir_label)', 1),
    (FD, 'formatDetailedFileSize(resolvedSize)', 'formatDetailedFileSize(context, resolvedSize)', 2),
    # ---- 尺寸格式化（顶层函数加 context 形参）----
    (FD, 'fun formatDetailedFileSize(size: Long): String {\n    if (size <= 0) return "0 B (0 bytes)"',
     'fun formatDetailedFileSize(context: Context, size: Long): String {\n    if (size <= 0) return context.getString(R.string.component_size_zero)', 1),
    (FD, 'return if (digitGroups == 0) "$rawFormatted bytes" else "$formattedVal ${units[digitGroups]} ($rawFormatted bytes)"',
     'return if (digitGroups == 0) context.getString(R.string.component_size_bytes, rawFormatted) else context.getString(R.string.component_size_with_raw, "$formattedVal ${units[digitGroups]}", rawFormatted)', 1),
    # ---- AgentsSetupUi：多行拼接残留 ----
    (AS, '''                    "The local server stops and the app restarts with the new agents, so nothing of a turned-off agent keeps running. " +
                        "Replies in progress stop, and a chat of a turned-off agent is closed.",''',
     '''                    stringResource(R.string.agents_restart_apply_desc),''', 1),
]

RESOURCES = [
    ("component_yes", "Yes", "是"),
    ("component_no", "No", "否"),
    ("component_bridge_managed_access", "Read/Write Access (Bridge Managed)", "读/写权限（由桥接管理）"),
    ("component_badge_directory", "DIRECTORY", "目录"),
    ("component_badge_file", "FILE", "文件"),
    ("component_dir_label", "Directory", "目录"),
    ("component_file_type_folder", "Folder (Directory)", "文件夹（目录）"),
    ("component_file_type_java", "Java Source File (.java)", "Java 源文件（.java）"),
    ("component_file_type_py", "Python Script (.py)", "Python 脚本（.py）"),
    ("component_file_type_js", "JavaScript File (.js)", "JavaScript 文件（.js）"),
    ("component_file_type_ts", "TypeScript File (.ts)", "TypeScript 文件（.ts）"),
    ("component_file_type_json", "JSON Data (.json)", "JSON 数据（.json）"),
    ("component_file_type_xml", "XML Document (.xml)", "XML 文档（.xml）"),
    ("component_file_type_toml", "TOML Configuration (.toml)", "TOML 配置（.toml）"),
    ("component_file_type_go", "Go Source File (.go)", "Go 源文件（.go）"),
    ("component_file_type_rs", "Rust Source File (.rs)", "Rust 源文件（.rs）"),
    ("component_size_zero", "0 B (0 bytes)", "0 B（0 字节）"),
    ("agents_restart_apply_desc",
     "The local server stops and the app restarts with the new agents, so nothing of a turned-off agent keeps running. Replies in progress stop, and a chat of a turned-off agent is closed.",
     "本地服务器会停止，应用会带着新的智能体重启，因此已关闭的智能体不会有任何进程残留。正在进行的回复会中断，已关闭智能体的对话会被关闭。"),
]

DYNAMIC = {
    "component_permissions_line": {
        "en": "$r$w$x (Read: $read, Write: $write, Exec: $exec)",
        "zh": "%1$s%2$s%3$s（读取：%4$s，写入：%5$s，执行：%6$s）",
        "module": "component", "args": ["r", "w", "x", "read", "write", "exec"]},
    "component_child_items": {"en": "$count items", "zh": "%1$s 个项目",
                              "module": "component", "args": ["count"]},
    "component_file_type_kt": {"en": "Kotlin Source File (.$ext)", "zh": "Kotlin 源文件（.%1$s）",
                               "module": "component", "args": ["ext"]},
    "component_file_type_react": {"en": "React Component (.$ext)", "zh": "React 组件（.%1$s）",
                                  "module": "component", "args": ["ext"]},
    "component_file_type_html": {"en": "HTML Document (.$ext)", "zh": "HTML 文档（.%1$s）",
                                 "module": "component", "args": ["ext"]},
    "component_file_type_css": {"en": "Stylesheet (.$ext)", "zh": "样式表（.%1$s）",
                                "module": "component", "args": ["ext"]},
    "component_file_type_yaml": {"en": "YAML Configuration (.$ext)", "zh": "YAML 配置（.%1$s）",
                                 "module": "component", "args": ["ext"]},
    "component_file_type_md": {"en": "Markdown Document (.$ext)", "zh": "Markdown 文档（.%1$s）",
                               "module": "component", "args": ["ext"]},
    "component_file_type_txt": {"en": "Text Document (.$ext)", "zh": "文本文档（.%1$s）",
                                "module": "component", "args": ["ext"]},
    "component_file_type_shell": {"en": "Shell Script (.$ext)", "zh": "Shell 脚本（.%1$s）",
                                  "module": "component", "args": ["ext"]},
    "component_file_type_c": {"en": "C/C++ Source (.$ext)", "zh": "C/C++ 源文件（.%1$s）",
                              "module": "component", "args": ["ext"]},
    "component_file_type_image": {"en": "Image Asset (.$ext)", "zh": "图片资源（.%1$s）",
                                  "module": "component", "args": ["ext"]},
    "component_file_type_archive": {"en": "Archive File (.$ext)", "zh": "压缩包（.%1$s）",
                                    "module": "component", "args": ["ext"]},
    "component_file_type_other": {"en": "$upper File (.$ext)", "zh": "%1$s 文件（.%2$s）",
                                  "module": "component", "args": ["upper", "ext"]},
    "component_size_bytes": {"en": "$value bytes", "zh": "%1$s 字节",
                             "module": "component", "args": ["value"]},
    "component_size_with_raw": {"en": "$formatted ($raw bytes)", "zh": "%1$s（%2$s 字节）",
                                "module": "component", "args": ["formatted", "raw"]},
}


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
        module = "agents" if key.startswith("agents_") else "component"
        catalog.append({"key": key, "module": module, "en": en, "zh": zh,
                        "count": 1, "sites": [], "orphan": False})
        keys.add(key); ens.add(en); added += 1

    dyn = json.load(open(DYN, encoding="utf-8")) if os.path.exists(DYN) else {}
    dyn_added = 0
    for key, info in DYNAMIC.items():
        if key in dyn:
            continue
        dyn[key] = info
        dyn_added += 1

    if not DRY:
        json.dump(catalog, open(CAT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
        json.dump(dyn, open(DYN, "w", encoding="utf-8"), ensure_ascii=False, indent=1)

    print(f"新增静态资源 {added} 条、插值资源 {dyn_added} 条")
    if errors:
        print("\n错误：")
        for e in errors:
            print("  " + e)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
