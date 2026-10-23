#!/usr/bin/env python3
"""最后两处可见漏翻（扫描器形态盲区之外的漏网）：

1. `SettingsDialog` 的 rootfs 备份说明：原文含 shell 变量 `$HOME`，静态资源会被
   `gen_resources` 的 `$` 规则误判成插值而跳过，所以改成**动态资源 + 参数**：
   en/zh 都把 `$HOME` 作为 `%1$s` 传入（调用点写 `"\\$HOME"` 转义），
   两种语言的显示文本与原文一字不差。
2. `YouTubeMediaSessionManager` 的媒体通知：`setContentText("YouTube Video")`、
   `playPauseTitle`（Pause/Play）、动作 `"Close"` —— 都显示在通知栏/锁屏上。
   `setSubText("Antigravity")` 是产品名，保持英文。

用法：apply_media_and_rootfs.py --dry-run / apply_media_and_rootfs.py → 三步流水线。
"""
import json
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SRC = "app/src/main/java/com/example/gemini/"
CAT = "/tmp/i18n_catalog.json"
DYN = "/tmp/i18n_dynamic.json"
DRY = "--dry-run" in sys.argv

SD = "ui/settings/SettingsDialog.kt"
YT = "data/media/YouTubeMediaSessionManager.kt"

SITES = [
    # ---- rootfs 备份说明（含 $HOME）----
    (SD, 'text = "Package entire Linux /usr system and \\$HOME into a bootable zip",',
     'text = stringResource(R.string.settings_rootfs_backup_desc, "\\$HOME"),', 1),
    # ---- 媒体通知 ----
    (YT, '.setContentText("YouTube Video")',
     '.setContentText(context.getString(R.string.data_media_youtube_video))', 1),
    (YT, 'val playPauseTitle = if (isPlaying) "Pause" else "Play"',
     'val playPauseTitle = if (isPlaying) context.getString(R.string.data_media_pause) else context.getString(R.string.component_play)', 1),
    (YT, '                    "Close",\n                    stopIntent',
     '                    context.getString(R.string.common_close),\n                    stopIntent', 1),
]

RESOURCES = [
    ("data_media_youtube_video", "YouTube Video", "YouTube 视频"),
    ("data_media_pause", "Pause", "暂停"),
]

DYNAMIC = {
    "settings_rootfs_backup_desc": {
        "en": "Package entire Linux /usr system and $home into a bootable zip",
        "zh": "把整个 Linux /usr 系统与 %1$s 打包成可启动的 zip",
        "module": "settings", "args": ["home"]},
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
        catalog.append({"key": key, "module": "data", "en": en, "zh": zh,
                        "count": 1, "sites": [], "orphan": False})
        keys.add(key); ens.add(en); added += 1

    dyn = json.load(open(DYN, encoding="utf-8")) if os.path.exists(DYN) else {}
    dyn_added = 0
    for key, info in DYNAMIC.items():
        if key in dyn:
            continue
        dyn[key] = info
        dyn_added += 1

    if errors:
        print("\n错误（未写入）：")
        for e in errors:
            print("  " + e)
        return 1

    if not DRY:
        for path, text in files.items():
            open(path, "w", encoding="utf-8").write(text)
        json.dump(catalog, open(CAT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
        json.dump(dyn, open(DYN, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    print(f"新增静态资源 {added} 条、插值资源 {dyn_added} 条")
    return 0


if __name__ == "__main__":
    sys.exit(main())
