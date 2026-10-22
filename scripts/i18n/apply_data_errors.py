#!/usr/bin/env python3
"""数据层错误族：AgyMcpManager + ClaudeAccountManager 的用户可见文案。

两个类原本都没有 Context，本批次给构造函数注入 `appContext: Context`
（由 ChatViewModel 传 `application`，它继承 AndroidViewModel），
然后按「文件 + 表内原文 + 期望次数」三重限定改写可见文案。

判据：字符串最终会不会被渲染 → 这两个类的 `_mcpErrorMessage/_mcpStatusMessage`
由设置页 MCP 区块 `Text(errorMessage/statusMessage)` 显示；`_statusError/_usageError`
由 Claude 面板显示；`_messages` 经 `AppToastHelper.showToast(it, ChatToastType.INFO)`
显示（调用点已显式传类型，不受坑 16 影响）。`Log.*` 一律不动。

用法：apply_data_errors.py --dry-run / apply_data_errors.py，然后
gen_resources.py → fix_imports.py → verify.py。
"""
import json
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SRC = "app/src/main/java/com/example/gemini/"
CAT = "/tmp/i18n_catalog.json"
DYN = "/tmp/i18n_dynamic.json"
DRY = "--dry-run" in sys.argv

AGY = "data/agent/agy/AgyMcpManager.kt"
CLAUDE = "data/agent/claude/ClaudeAccountManager.kt"
VM = "ui/chat/ChatViewModel.kt"

SITES = [
    # ===== 构造注入 =====
    (AGY, 'import android.util.Log\n', 'import android.content.Context\nimport android.util.Log\n', 1),
    (AGY,
     'class AgyMcpManager(\n    private val backendScope: CoroutineScope,\n    private val agyHubClient: AgyHubClient\n) {',
     'class AgyMcpManager(\n    private val backendScope: CoroutineScope,\n    private val agyHubClient: AgyHubClient,\n    private val appContext: Context\n) {', 1),
    (CLAUDE, 'package com.example.gemini.data.agent.claude\n\n',
     'package com.example.gemini.data.agent.claude\n\nimport android.content.Context\n', 1),
    (CLAUDE,
     'class ClaudeAccountManager(\n    private val scope: CoroutineScope,\n    private val client: ClaudeBridgeClient\n) {',
     'class ClaudeAccountManager(\n    private val scope: CoroutineScope,\n    private val client: ClaudeBridgeClient,\n    private val appContext: Context\n) {', 1),
    (VM,
     'com.example.gemini.data.agent.agy.AgyMcpManager(viewModelScope, agyBackend.agyHubClient)',
     'com.example.gemini.data.agent.agy.AgyMcpManager(viewModelScope, agyBackend.agyHubClient, application)', 1),
    (VM,
     'com.example.gemini.data.agent.claude.ClaudeAccountManager(viewModelScope, claudeClient)',
     'com.example.gemini.data.agent.claude.ClaudeAccountManager(viewModelScope, claudeClient, application)', 1),

    # ===== AgyMcpManager：加载 / 刷新 =====
    (AGY,
     'val err = liveResult.exceptionOrNull()?.message ?: "Failed to connect to Antigravity Hub"',
     'val err = liveResult.exceptionOrNull()?.message ?: appContext.getString(R.string.data_err_connect_antigravity_hub)', 1),
    (AGY,
     '_mcpErrorMessage.value = "Cannot reach Antigravity daemon ($hubUrl): $fetchError"',
     '_mcpErrorMessage.value = appContext.getString(R.string.data_err_reach_antigravity_daemon, hubUrl, fetchError.orEmpty())', 1),
    (AGY,
     'val err = res.exceptionOrNull()?.message ?: "Refresh failed"',
     'val err = res.exceptionOrNull()?.message ?: appContext.getString(R.string.data_err_refresh_failed)', 1),
    (AGY,
     '_mcpErrorMessage.value = "Daemon refresh error ($hubUrl): $err"',
     '_mcpErrorMessage.value = appContext.getString(R.string.data_err_daemon_refresh_error, hubUrl, err)', 1),
    (AGY,
     '_mcpErrorMessage.value = "\'$targetServer\': ${s.error}"',
     '_mcpErrorMessage.value = appContext.getString(R.string.data_err_server_error_detail, targetServer, s.error.orEmpty())', 1),
    (AGY,
     '_mcpStatusMessage.value = "Refreshed \'$targetServer\': ${s.tools.size} tool(s) available"',
     '_mcpStatusMessage.value = appContext.getString(R.string.data_status_refreshed_server_tools, targetServer, s.tools.size)', 1),
    (AGY,
     '_mcpStatusMessage.value = "Refreshed \'$targetServer\'"',
     '_mcpStatusMessage.value = appContext.getString(R.string.data_status_refreshed_server, targetServer)', 1),
    (AGY,
     '_mcpStatusMessage.value = "Refreshed: $count server(s) configured, $toolsCount tool(s) discovered"',
     '_mcpStatusMessage.value = appContext.getString(R.string.data_status_refreshed_summary, count, toolsCount)', 1),

    # ===== AgyMcpManager：启用 / 保存 / 删除 / 插件 =====
    (AGY,
     '_mcpStatusMessage.value = if (enabled) "Enabled \'$serverName\'" else "Disabled \'$serverName\'"',
     '_mcpStatusMessage.value = if (enabled) appContext.getString(R.string.data_status_server_enabled, serverName) else appContext.getString(R.string.data_status_server_disabled, serverName)', 1),
    (AGY,
     '_mcpErrorMessage.value = "Failed to save configuration to mcp_config.json"',
     '_mcpErrorMessage.value = appContext.getString(R.string.data_err_save_mcp_config)', 1),
    (AGY,
     '_mcpStatusMessage.value = "Saved \'${spec.serverName}\'. Tap Refresh on the server to connect."',
     '_mcpStatusMessage.value = appContext.getString(R.string.data_status_server_saved, spec.serverName)', 1),
    (AGY,
     '_mcpStatusMessage.value = "Removed \'$serverName\'."',
     '_mcpStatusMessage.value = appContext.getString(R.string.data_status_server_removed, serverName)', 1),
    (AGY,
     '_mcpStatusMessage.value = "Installed and activated \'${plugin.title}\' MCP server"',
     '_mcpStatusMessage.value = appContext.getString(R.string.data_status_plugin_installed, plugin.title)', 1),
    (AGY,
     'val err = res.exceptionOrNull()?.message ?: "Installation failed"',
     'val err = res.exceptionOrNull()?.message ?: appContext.getString(R.string.data_err_installation_failed)', 1),
    (AGY,
     '_mcpErrorMessage.value = "Failed to install ${plugin.title}: $err"',
     '_mcpErrorMessage.value = appContext.getString(R.string.data_err_install_plugin_failed, plugin.title, err)', 1),
    (AGY,
     '_mcpErrorMessage.value = "Error installing ${plugin.title}: ${e.message}"',
     '_mcpErrorMessage.value = appContext.getString(R.string.data_err_install_plugin_error, plugin.title, e.message.orEmpty())', 1),

    # ===== ClaudeAccountManager：状态检查 =====
    (CLAUDE,
     '_statusError.value = it.auth_error?.let { e -> "Could not read the sign-in state: $e" }',
     '_statusError.value = it.auth_error?.let { e -> appContext.getString(R.string.data_err_read_sign_in_state, e) }', 1),
    (CLAUDE,
     'ClaudeStatus.BRIDGE_OFFLINE -> "Could not reach the bridge" + (it.message?.let { m -> " ($m)." } ?: ".")',
     'ClaudeStatus.BRIDGE_OFFLINE -> it.message?.let { m -> appContext.getString(R.string.data_err_could_not_reach_bridge_detail, m) } ?: appContext.getString(R.string.data_err_could_not_reach_bridge)', 1),
    (CLAUDE,
     'else -> it.message ?: "Status check failed"',
     'else -> it.message ?: appContext.getString(R.string.data_err_status_check_failed)', 1),
    (CLAUDE,
     '_statusError.value = "The bridge is not running."',
     '_statusError.value = appContext.getString(R.string.data_err_bridge_not_running)', 1),
    (CLAUDE,
     '} else _usageError.value = resp.error ?: "Claude Code did not return usage"',
     '} else _usageError.value = resp.error ?: appContext.getString(R.string.data_err_no_usage_returned)', 1),
    (CLAUDE,
     '.onFailure { _usageError.value = it.message ?: "Cannot reach the bridge" }',
     '.onFailure { _usageError.value = it.message ?: appContext.getString(R.string.data_err_cannot_reach_bridge) }', 1),

    # ===== ClaudeAccountManager：登录 / 登出 =====
    (CLAUDE,
     '_messages.tryEmit("Sign-in could not start: ${resp.error ?: "unknown error"}")',
     '_messages.tryEmit(appContext.getString(R.string.data_err_sign_in_could_not_start, resp.error ?: appContext.getString(R.string.data_err_unknown_error)))', 1),
    (CLAUDE,
     '_messages.tryEmit("Sign-in could not start: ${it.message}")',
     '_messages.tryEmit(appContext.getString(R.string.data_err_sign_in_could_not_start, it.message.orEmpty()))', 1),
    (CLAUDE,
     '.onSuccess { if (!it.success) _messages.tryEmit("Code rejected: ${it.error}") }',
     '.onSuccess { if (!it.success) _messages.tryEmit(appContext.getString(R.string.data_err_code_rejected, it.error.orEmpty())) }', 1),
    (CLAUDE,
     '.onFailure { _messages.tryEmit("Could not send code: ${it.message}") }',
     '.onFailure { _messages.tryEmit(appContext.getString(R.string.data_err_send_code_failed, it.message.orEmpty())) }', 1),
    (CLAUDE,
     'if (it.success) _messages.tryEmit("Signed out of Claude") else _messages.tryEmit("Sign-out failed: ${it.error}")',
     'if (it.success) _messages.tryEmit(appContext.getString(R.string.data_status_signed_out_claude)) else _messages.tryEmit(appContext.getString(R.string.data_err_sign_out_failed, it.error.orEmpty()))', 1),
    (CLAUDE,
     '.onFailure { _messages.tryEmit("Sign-out failed: ${it.message}") }',
     '.onFailure { _messages.tryEmit(appContext.getString(R.string.data_err_sign_out_failed, it.message.orEmpty())) }', 1),
    (CLAUDE,
     '_messages.tryEmit("Signed in to Claude")',
     '_messages.tryEmit(appContext.getString(R.string.data_status_signed_in_claude))', 1),
    (CLAUDE,
     '_messages.tryEmit("Sign-in failed: ${st.error ?: st.state}")',
     '_messages.tryEmit(appContext.getString(R.string.data_err_sign_in_failed, st.error ?: st.state))', 1),
]

RESOURCES = [
    # AgyMcpManager：静态
    ("data_err_connect_antigravity_hub", "Failed to connect to Antigravity Hub",
     "无法连接 Antigravity Hub"),
    ("data_err_refresh_failed", "Refresh failed", "刷新失败"),
    ("data_err_save_mcp_config", "Failed to save configuration to mcp_config.json",
     "无法把配置写入 mcp_config.json"),
    ("data_err_installation_failed", "Installation failed", "安装失败"),
    # ClaudeAccountManager：静态
    ("data_err_could_not_reach_bridge", "Could not reach the bridge.", "无法连接桥接服务。"),
    ("data_err_status_check_failed", "Status check failed", "状态检查失败"),
    ("data_err_bridge_not_running", "The bridge is not running.", "桥接服务未运行。"),
    ("data_err_no_usage_returned", "Claude Code did not return usage", "Claude Code 未返回用量信息"),
    ("data_err_cannot_reach_bridge", "Cannot reach the bridge", "无法连接桥接服务"),
    ("data_err_unknown_error", "unknown error", "未知错误"),
    ("data_status_signed_out_claude", "Signed out of Claude", "已退出 Claude 登录"),
    ("data_status_signed_in_claude", "Signed in to Claude", "已登录 Claude"),
]

DYNAMIC = {
    # AgyMcpManager
    "data_err_reach_antigravity_daemon": {
        "en": "Cannot reach Antigravity daemon ($hubUrl): $fetchError",
        "zh": "无法连接 Antigravity 守护进程（%1$s）：%2$s",
        "module": "data", "args": ["hubUrl", "fetchError"]},
    "data_err_daemon_refresh_error": {
        "en": "Daemon refresh error ($hubUrl): $err",
        "zh": "刷新 Antigravity 守护进程失败（%1$s）：%2$s",
        "module": "data", "args": ["hubUrl", "err"]},
    "data_err_server_error_detail": {
        "en": "'$targetServer': $error",
        "zh": "「%1$s」：%2$s",
        "module": "data", "args": ["targetServer", "error"]},
    "data_status_refreshed_server_tools": {
        "en": "Refreshed '$targetServer': $count tool(s) available",
        "zh": "已刷新「%1$s」：%2$s 个工具可用",
        "module": "data", "args": ["targetServer", "count"]},
    "data_status_refreshed_server": {
        "en": "Refreshed '$targetServer'",
        "zh": "已刷新「%1$s」",
        "module": "data", "args": ["targetServer"]},
    "data_status_refreshed_summary": {
        "en": "Refreshed: $count server(s) configured, $toolsCount tool(s) discovered",
        "zh": "已刷新：%1$s 个服务器已配置，发现 %2$s 个工具",
        "module": "data", "args": ["count", "toolsCount"]},
    "data_status_server_enabled": {
        "en": "Enabled '$serverName'", "zh": "已启用「%1$s」",
        "module": "data", "args": ["serverName"]},
    "data_status_server_disabled": {
        "en": "Disabled '$serverName'", "zh": "已停用「%1$s」",
        "module": "data", "args": ["serverName"]},
    "data_status_server_saved": {
        "en": "Saved '$serverName'. Tap Refresh on the server to connect.",
        "zh": "已保存「%1$s」。点击该服务器上的「刷新」以建立连接。",
        "module": "data", "args": ["serverName"]},
    "data_status_server_removed": {
        "en": "Removed '$serverName'.", "zh": "已移除「%1$s」。",
        "module": "data", "args": ["serverName"]},
    "data_status_plugin_installed": {
        "en": "Installed and activated '$title' MCP server",
        "zh": "已安装并启用「%1$s」MCP 服务器",
        "module": "data", "args": ["title"]},
    "data_err_install_plugin_failed": {
        "en": "Failed to install $title: $err", "zh": "安装「%1$s」失败：%2$s",
        "module": "data", "args": ["title", "err"]},
    "data_err_install_plugin_error": {
        "en": "Error installing $title: $message", "zh": "安装「%1$s」出错：%2$s",
        "module": "data", "args": ["title", "message"]},
    # ClaudeAccountManager
    "data_err_read_sign_in_state": {
        "en": "Could not read the sign-in state: $error", "zh": "无法读取登录状态：%1$s",
        "module": "data", "args": ["error"]},
    "data_err_could_not_reach_bridge_detail": {
        "en": "Could not reach the bridge ($message).", "zh": "无法连接桥接服务（%1$s）。",
        "module": "data", "args": ["message"]},
    "data_err_sign_in_could_not_start": {
        "en": "Sign-in could not start: $error", "zh": "无法开始登录：%1$s",
        "module": "data", "args": ["error"]},
    "data_err_code_rejected": {
        "en": "Code rejected: $error", "zh": "验证码被拒绝：%1$s",
        "module": "data", "args": ["error"]},
    "data_err_send_code_failed": {
        "en": "Could not send code: $message", "zh": "无法提交验证码：%1$s",
        "module": "data", "args": ["message"]},
    "data_err_sign_out_failed": {
        "en": "Sign-out failed: $error", "zh": "退出登录失败：%1$s",
        "module": "data", "args": ["error"]},
    "data_err_sign_in_failed": {
        "en": "Sign-in failed: $error", "zh": "登录失败：%1$s",
        "module": "data", "args": ["error"]},
}


def main():
    errors, changed = [], 0
    for rel, old, new, count in SITES:
        path = SRC + rel
        text = open(path, encoding="utf-8").read()
        found = text.count(old)
        if found != count:
            errors.append(f"[{rel}] 期望 {count} 处，实际 {found}：{old[:70]}")
            continue
        if not DRY:
            open(path, "w", encoding="utf-8").write(text.replace(old, new))
        changed += count
        print(f"  {rel}: {count} 处  {old[:60]}")

    catalog = json.load(open(CAT, encoding="utf-8"))
    keys = {c["key"] for c in catalog}
    ens = {c["en"] for c in catalog}
    added = 0
    for key, en, zh in RESOURCES:
        if key in keys:
            continue
        if en in ens:
            errors.append(f"英文重复: {en}")
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

    print(f"\n改写 {changed} 处；新增静态资源 {added} 条、插值资源 {dyn_added} 条")
    if errors:
        print("\n错误：")
        for e in errors:
            print("  " + e)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
