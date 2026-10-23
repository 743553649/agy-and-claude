#!/usr/bin/env python3
"""聊天后端（AgyChatBackend）用户可见的错误与提示文案。

判据：
- `_conversationError.value` / `formatStructuredErrorMessage(title/userMessage)` 进对话页错误卡片；
- `Toast.makeText(application, reason, ...)` 是发送拦截提示；
- 转写 onError 直接把错误抛给语音输入界面。

**不翻**（本批刻意跳过，见 commit message）：
- 工具结果/下行 payload：`output = "Command terminated by user."`、
  `content = "[Terminal Command ...]"`、`denyInstruction`、`skipSummary` 等，它们会作为
  工具结果回给模型；
- `"Voice note"/"Voice message"/"Image"/"Attachment"` 既是消息正文又与
  `cleanText == "Voice note"` 比较，翻了会破坏该比较；
- 会话标题 `"New Chat"`（数据层标题，同族风险）；
- 所有 Log.* 文案。

用法：apply_chat_errors.py --dry-run / apply_chat_errors.py → gen_resources → fix_imports → verify。
"""
import json
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SRC = "app/src/main/java/com/example/gemini/"
CAT = "/tmp/i18n_catalog.json"
DYN = "/tmp/i18n_dynamic.json"
DRY = "--dry-run" in sys.argv

FILE = "data/agent/agy/AgyChatBackend.kt"

SITES = [
    # ---- 连接与分叉 ----
    ('_conversationError.value = "Antigravity Hub failed to start: ${status.error ?: "Check server logs"}"',
     '_conversationError.value = application.getString(R.string.data_err_hub_failed_to_start, status.error ?: application.getString(R.string.data_err_check_server_logs))', 1),
    ('val rawErr = e.message ?: "Connection failed"',
     'val rawErr = e.message ?: application.getString(R.string.data_err_connection_failed)', 1),
    ('"Cannot connect to Antigravity Hub (${AuthPreferences.currentHubUrl}). Make sure \'agy --hub\' is running."',
     'application.getString(R.string.data_err_cannot_connect_hub, AuthPreferences.currentHubUrl)', 1),
    ('"Antigravity Hub unreachable: $rawErr"',
     'application.getString(R.string.data_err_hub_unreachable, rawErr)', 1),
    ('_conversationError.value = "Fork failed: ${res.exceptionOrNull()?.message}"',
     '_conversationError.value = application.getString(R.string.data_err_fork_failed, res.exceptionOrNull()?.message.orEmpty())', 1),
    ('_conversationError.value = "Fork error: ${e.message}"',
     '_conversationError.value = application.getString(R.string.data_err_fork_error, e.message.orEmpty())', 1),
    # ---- 发送拦截（Toast）----
    ('-> "Server is offline. Please start the server."',
     '-> application.getString(R.string.data_status_server_offline)', 1),
    ('-> "Server is starting up. Please wait..."',
     '-> application.getString(R.string.data_status_server_starting)', 1),
    ('-> "Acquiring security token. Please wait..."',
     '-> application.getString(R.string.data_status_acquiring_token)', 1),
    ('-> "Verifying authentication. Please wait..."',
     '-> application.getString(R.string.data_status_verifying_auth)', 1),
    ('-> "Please sign in to send messages."',
     '-> application.getString(R.string.data_err_sign_in_to_send)', 1),
    ('-> "Server error. Please check server logs."',
     '-> application.getString(R.string.data_err_server_error_check_logs)', 1),
    ('else -> "Cannot send message right now."',
     'else -> application.getString(R.string.data_err_cannot_send_now)', 1),
    # ---- 错误卡片标题与正文 ----
    ('?: "Failed to start conversation on daemon"',
     '?: application.getString(R.string.data_err_start_conversation)', 1),
    ('"Authentication Required"', 'application.getString(R.string.data_title_authentication_required)', 2),
    ('"Failed to Start Conversation"', 'application.getString(R.string.data_title_start_conversation_failed)', 1),
    ('err.contains("quota", ignoreCase = true) || err.contains("credit", ignoreCase = true) || err.contains("429") -> "Quota Limit Exceeded"',
     'err.contains("quota", ignoreCase = true) || err.contains("credit", ignoreCase = true) || err.contains("429") -> application.getString(R.string.data_title_quota_exceeded)', 1),
    ('-> "Model Not Found"', '-> application.getString(R.string.data_title_model_not_found)', 1),
    ('-> "Server Connection Error"', '-> application.getString(R.string.data_title_server_connection_error)', 1),
    ('else -> "Message Delivery Error"', 'else -> application.getString(R.string.data_title_message_delivery_error)', 1),
    ('?: "Failed to send message"', '?: application.getString(R.string.data_err_failed_to_send)', 2),
    ('userMessage = e.message ?: "Unknown error"',
     'userMessage = e.message ?: application.getString(R.string.data_err_unknown_error)', 1),
    # ---- 审批 / 取消 / 转写 ----
    ('?: "Failed to approve tool"', '?: application.getString(R.string.data_err_failed_to_approve)', 1),
    ('_conversationError.value = "Approval failed: $err"',
     '_conversationError.value = application.getString(R.string.data_err_approval_failed, err)', 1),
    ('?: "Failed to reject tool"', '?: application.getString(R.string.data_err_failed_to_reject)', 1),
    ('_conversationError.value = "Rejection failed: $err"',
     '_conversationError.value = application.getString(R.string.data_err_rejection_failed, err)', 1),
    ('?: "Failed to cancel step"', '?: application.getString(R.string.data_err_failed_to_cancel_step)', 1),
    ('onError("No speech recognized in recording")',
     'onError(application.getString(R.string.data_err_no_speech_recognized))', 1),
    ('?: "Transcription failed"', '?: application.getString(R.string.data_err_transcription_failed)', 1),
    ('onError(e.message ?: "Transcription error")',
     'onError(e.message ?: application.getString(R.string.data_err_transcription_error))', 1),
]

RESOURCES = [
    ("data_err_check_server_logs", "Check server logs", "查看服务器日志"),
    ("data_err_connection_failed", "Connection failed", "连接失败"),
    ("data_status_server_offline", "Server is offline. Please start the server.", "服务器离线，请先启动服务器。"),
    ("data_status_server_starting", "Server is starting up. Please wait...", "服务器正在启动，请稍候……"),
    ("data_status_acquiring_token", "Acquiring security token. Please wait...", "正在获取安全令牌，请稍候……"),
    ("data_status_verifying_auth", "Verifying authentication. Please wait...", "正在验证身份，请稍候……"),
    ("data_err_sign_in_to_send", "Please sign in to send messages.", "请先登录再发送消息。"),
    ("data_err_server_error_check_logs", "Server error. Please check server logs.", "服务器出错，请查看服务器日志。"),
    ("data_err_cannot_send_now", "Cannot send message right now.", "当前无法发送消息。"),
    ("data_err_start_conversation", "Failed to start conversation on daemon", "在守护进程上启动会话失败"),
    ("data_title_authentication_required", "Authentication Required", "需要登录"),
    ("data_title_start_conversation_failed", "Failed to Start Conversation", "启动会话失败"),
    ("data_title_quota_exceeded", "Quota Limit Exceeded", "超出配额限制"),
    ("data_title_model_not_found", "Model Not Found", "未找到模型"),
    ("data_title_server_connection_error", "Server Connection Error", "服务器连接错误"),
    ("data_title_message_delivery_error", "Message Delivery Error", "消息发送错误"),
    ("data_err_failed_to_send", "Failed to send message", "发送消息失败"),
    ("data_err_unknown_error", "Unknown error", "未知错误"),
    ("data_err_failed_to_approve", "Failed to approve tool", "批准工具失败"),
    ("data_err_failed_to_reject", "Failed to reject tool", "拒绝工具失败"),
    ("data_err_failed_to_cancel_step", "Failed to cancel step", "取消步骤失败"),
    ("data_err_no_speech_recognized", "No speech recognized in recording", "录音里没有识别到语音"),
    ("data_err_transcription_failed", "Transcription failed", "转写失败"),
    ("data_err_transcription_error", "Transcription error", "转写出错"),
]

DYNAMIC = {
    "data_err_hub_failed_to_start": {"en": "Antigravity Hub failed to start: $error",
                                     "zh": "Antigravity Hub 启动失败：%1$s",
                                     "module": "data", "args": ["error"]},
    "data_err_cannot_connect_hub": {
        "en": "Cannot connect to Antigravity Hub ($hubUrl). Make sure 'agy --hub' is running.",
        "zh": "无法连接 Antigravity Hub（%1$s）。请确认 'agy --hub' 正在运行。",
        "module": "data", "args": ["hubUrl"]},
    "data_err_hub_unreachable": {"en": "Antigravity Hub unreachable: $error",
                                 "zh": "Antigravity Hub 不可达：%1$s",
                                 "module": "data", "args": ["error"]},
    "data_err_fork_failed": {"en": "Fork failed: $error", "zh": "分叉失败：%1$s",
                             "module": "data", "args": ["error"]},
    "data_err_fork_error": {"en": "Fork error: $message", "zh": "分叉出错：%1$s",
                            "module": "data", "args": ["message"]},
    "data_err_approval_failed": {"en": "Approval failed: $error", "zh": "批准失败：%1$s",
                                 "module": "data", "args": ["error"]},
    "data_err_rejection_failed": {"en": "Rejection failed: $error", "zh": "拒绝失败：%1$s",
                                  "module": "data", "args": ["error"]},
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
    if not DRY and not errors:
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
