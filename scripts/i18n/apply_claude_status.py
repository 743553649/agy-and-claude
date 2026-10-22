#!/usr/bin/env python3
"""Claude 未就绪空态：ClaudeStatusViews.kt 整块文案。

HANDOFF 原本只记「7 个 actionLabel」，实际核实该文件 170 行里**整块空态都是英文**：
ClaudeStatusPrompt 的标题/说明/主次按钮，以及 ClaudeStatusBanner 的提示文字与药丸按钮。
本脚本按单个字面量精确替换（每个字面量先统计出现次数），能复用的既有 key 一律复用。

判据：AgentSetupPrompt/AgentStatusBanner 的文字与按钮都直接 Text(...) 渲染。

用法：apply_claude_status.py --dry-run / apply_claude_status.py，
然后 gen_resources.py → fix_imports.py → verify.py。
"""
import json
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SRC = "app/src/main/java/com/example/gemini/"
CAT = "/tmp/i18n_catalog.json"
DYN = "/tmp/i18n_dynamic.json"
DRY = "--dry-run" in sys.argv

FILE = "ui/claude/ClaudeStatusViews.kt"

# (原文子串, 替换子串, 期望次数)
SITES = [
    # ---- ClaudeStatusPrompt：状态标题与说明 ----
    ('"Starting the local server…"', 'stringResource(R.string.claude_status_server_starting_title)', 1),
    ('"Claude Code will be ready in a moment."', 'stringResource(R.string.claude_status_server_starting_desc)', 1),
    ('"Checking Claude Code…"', 'stringResource(R.string.chat_apptoast_checking_claude_code)', 1),
    ('"Looking for the Claude Code CLI and your sign-in."', 'stringResource(R.string.claude_status_checking_desc)', 1),
    ('"Sign in to Claude Code"', 'stringResource(R.string.claude_sign_in_to_claude_code)', 1),
    ('"Use your Claude Pro or Max subscription, or an Anthropic Console account, to chat with Claude Code."',
     'stringResource(R.string.claude_status_sign_in_desc)', 1),
    ('"Finishing sign-in…"', 'stringResource(R.string.claude_status_signing_in_title)', 1),
    ('"Complete the sign-in in your browser, then come back here."',
     'stringResource(R.string.claude_status_signing_in_desc)', 1),
    ('"Open sign-in"', 'stringResource(R.string.claude_status_action_open_sign_in)', 1),
    ('"Claude Code not found"', 'stringResource(R.string.claude_status_not_installed_title)', 1),
    ('"The claude CLI is not installed on this device yet. You can install it from Settings in one tap."',
     'stringResource(R.string.claude_status_not_installed_desc)', 1),
    ('"Set up Claude Code"', 'stringResource(R.string.claude_status_action_setup)', 1),
    ('"Check again"', 'stringResource(R.string.claude_status_action_check_again)', 3),
    ('"Restart the local server"', 'stringResource(R.string.claude_status_restart_title)', 1),
    ('error ?: "The running local server cannot run Claude Code yet. Restart it to load Claude.",',
     'error ?: stringResource(R.string.claude_status_restart_desc),', 1),
    ('"Restart server"', 'stringResource(R.string.claude_status_action_restart_server)', 1),
    ('"Local server offline"', 'stringResource(R.string.claude_status_offline_title)', 2),
    ('"Claude Code runs through the local server. Start it to chat with Claude."',
     'stringResource(R.string.claude_status_offline_desc)', 1),
    ('"Retry"', 'stringResource(R.string.common_retry)', 2),
    ('"Server logs"', 'stringResource(R.string.agents_server_logs)', 1),
    ('"Couldn\'t check Claude Code"', 'stringResource(R.string.claude_status_error_title)', 1),
    # 复合句：原文是「(error ?: 兜底) + 后缀」两段拼接，合并成一条带参资源
    ('(error ?: "The status check failed.") + " You can still send a message."',
     'stringResource(R.string.claude_status_error_body, error ?: stringResource(R.string.claude_status_check_failed))', 1),
    # ---- ClaudeStatusBanner：提示文字与药丸按钮 ----
    ('"Claude Code is signed out"', 'stringResource(R.string.claude_banner_signed_out)', 1),
    ('"Finishing Claude sign-in…"', 'stringResource(R.string.claude_banner_signing_in)', 1),
    ('"Claude Code not found on this device"', 'stringResource(R.string.claude_banner_not_installed)', 1),
    ('"Restart the local server to use Claude Code"', 'stringResource(R.string.claude_banner_restart)', 1),
    ('"Sign in"', 'stringResource(R.string.claude_status_action_sign_in)', 2),
    ('"Open"', 'stringResource(R.string.common_open)', 1),
    ('"Set up"', 'stringResource(R.string.claude_status_action_set_up)', 1),
    ('"Restart"', 'stringResource(R.string.ide_restart)', 1),
]

RESOURCES = [
    # ClaudeStatusPrompt
    ("claude_status_server_starting_title", "Starting the local server…", "正在启动本地服务器……"),
    ("claude_status_server_starting_desc", "Claude Code will be ready in a moment.", "Claude Code 马上就好。"),
    ("claude_status_checking_desc", "Looking for the Claude Code CLI and your sign-in.",
     "正在查找 Claude Code CLI 与登录状态。"),
    ("claude_status_sign_in_desc",
     "Use your Claude Pro or Max subscription, or an Anthropic Console account, to chat with Claude Code.",
     "用 Claude Pro / Max 订阅或 Anthropic Console 账号与 Claude Code 对话。"),
    ("claude_status_signing_in_title", "Finishing sign-in…", "正在完成登录……"),
    ("claude_status_signing_in_desc", "Complete the sign-in in your browser, then come back here.",
     "请在浏览器里完成登录，然后回到这里。"),
    ("claude_status_action_open_sign_in", "Open sign-in", "打开登录页"),
    ("claude_status_not_installed_title", "Claude Code not found", "未找到 Claude Code"),
    ("claude_status_not_installed_desc",
     "The claude CLI is not installed on this device yet. You can install it from Settings in one tap.",
     "此设备尚未安装 claude CLI，可在设置里一键安装。"),
    ("claude_status_action_setup", "Set up Claude Code", "设置 Claude Code"),
    ("claude_status_action_check_again", "Check again", "重新检查"),
    ("claude_status_restart_title", "Restart the local server", "重启本地服务器"),
    ("claude_status_restart_desc",
     "The running local server cannot run Claude Code yet. Restart it to load Claude.",
     "当前运行的本地服务器还不能运行 Claude Code，重启后即可加载 Claude。"),
    ("claude_status_action_restart_server", "Restart server", "重启服务器"),
    ("claude_status_offline_title", "Local server offline", "本地服务器离线"),
    ("claude_status_offline_desc",
     "Claude Code runs through the local server. Start it to chat with Claude.",
     "Claude Code 通过本地服务器运行，启动后即可与 Claude 对话。"),
    ("claude_status_error_title", "Couldn't check Claude Code", "无法检查 Claude Code"),
    ("claude_status_check_failed", "The status check failed.", "状态检查失败。"),
    # ClaudeStatusBanner
    ("claude_banner_signed_out", "Claude Code is signed out", "Claude Code 已退出登录"),
    ("claude_banner_signing_in", "Finishing Claude sign-in…", "正在完成 Claude 登录……"),
    ("claude_banner_not_installed", "Claude Code not found on this device", "此设备上未找到 Claude Code"),
    ("claude_banner_restart", "Restart the local server to use Claude Code",
     "重启本地服务器以使用 Claude Code"),
    ("claude_status_action_sign_in", "Sign in", "登录"),
    ("claude_status_action_set_up", "Set up", "设置"),
]

DYNAMIC = {
    "claude_status_error_body": {
        "en": "$error You can still send a message.",
        "zh": "%1$s 你仍然可以发送消息。",
        "module": "claude", "args": ["error"]},
}


def main():
    errors, changed = [], 0
    path = SRC + FILE
    text = open(path, encoding="utf-8").read()
    for old, new, count in SITES:
        found = text.count(old)
        if found != count:
            errors.append(f"期望 {count} 处，实际 {found}：{old[:70]}")
            continue
        text = text.replace(old, new)
        changed += count
        print(f"  {count} 处  {old[:66]}")
    if not DRY and not errors:
        open(path, "w", encoding="utf-8").write(text)

    catalog = json.load(open(CAT, encoding="utf-8"))
    keys = {c["key"] for c in catalog}
    ens = {c["en"] for c in catalog}
    added = 0
    for key, en, zh in RESOURCES:
        if key in keys:
            continue
        if en in ens:
            errors.append(f"英文重复（应复用既有 key）: {en[:60]}")
            continue
        catalog.append({"key": key, "module": "claude", "en": en, "zh": zh,
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

    print(f"\n改写 {changed} 处；新增静态资源 {added} 条、插值资源 {dyn_added} 条")
    if errors:
        print("\n错误：")
        for e in errors:
            print("  " + e)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
