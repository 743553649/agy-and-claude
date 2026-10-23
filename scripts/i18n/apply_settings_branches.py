#!/usr/bin/env python3
"""SettingsDialog 里成对的 when / if 分支文案（坑 11 的剩余部分）。

44 条候选里除 `ioLagMs ... else "ERR"`（技术值）与 `"Claude Code · "`（产品名前缀）
之外全部是界面文案：HUD 说明、Hub/Bridge/SSH 连通性结果、MCP 命令预览、
安装状态、备份按钮、rootfs 执行状态、设备管理器/防护/沙箱状态、权限规则列表。
另外把 `permissionRule(...)` 的 `actionLabel` 改成 `actionLabelRes: Int`（同坑 19 族），
渲染处按 0 兜底成原始动作名。

用法：apply_settings_branches.py --dry-run / apply_settings_branches.py → 流水线三步。
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

SITES = [
    (SD, 'subtitle = if (isFloatingDiagnosticsEnabled) "Floating live metrics HUD enabled" else "Live network connections, active streams & thread HUD",',
     'subtitle = if (isFloatingDiagnosticsEnabled) stringResource(R.string.settings_hud_enabled_subtitle) else stringResource(R.string.settings_section_live_network_connections_active_sub),', 1),
    (SD, 'hubTestStatus = if (ok) "✓ Hub Connected!" else "✗ Hub unreachable (${hubPort.trim()})"',
     'hubTestStatus = if (ok) stringResource(R.string.settings_hub_test_ok) else stringResource(R.string.settings_hub_test_fail, hubPort.trim())', 1),
    (SD, 'bridgeTestStatus = if (ok) "✓ Bridge Connected!" else "✗ Bridge unreachable (${bridgePort.trim()})"',
     'bridgeTestStatus = if (ok) stringResource(R.string.settings_bridge_test_ok) else stringResource(R.string.settings_bridge_test_fail, bridgePort.trim())', 1),
    (SD, 'Text(if (isRefreshingAll) "Testing All..." else "Test & Refresh", fontSize = 13.sp)',
     'Text(if (isRefreshingAll) stringResource(R.string.settings_testing_all) else stringResource(R.string.settings_test_and_refresh), fontSize = 13.sp)', 1),
    (SD, 'text = if (isSse) "URL: $cmdText" else "> $cmdText",',
     'text = if (isSse) stringResource(R.string.settings_mcp_command_url, cmdText) else stringResource(R.string.settings_mcp_command_raw, cmdText),', 1),
    (SD, 'text = if (initialSpec != null && initialSpec.serverName.isNotBlank()) "Edit MCP Server" else "Add MCP Server",',
     'text = if (initialSpec != null && initialSpec.serverName.isNotBlank()) stringResource(R.string.settings_edit_mcp_server) else stringResource(R.string.settings_add_mcp_server),', 1),
    (SD, 'text = if (useSshTerminal) "SSH Mode" else "Local Mode",',
     'text = if (useSshTerminal) stringResource(R.string.settings_ssh_mode) else stringResource(R.string.settings_local_mode),', 1),
    (SD, 'text = if (useSshTerminal) "Direct SSH to Termux sshd without local rootfs" else "Built-in chroot rootfs environment",',
     'text = if (useSshTerminal) stringResource(R.string.settings_ssh_mode_desc) else stringResource(R.string.settings_local_mode_desc),', 1),
    (SD, 'sshTestStatus = if (res.isSuccess) "✓ SSH connection successful!" else "✗ Failed: ${res.exceptionOrNull()?.localizedMessage ?: "Connection refused"}"',
     'sshTestStatus = if (res.isSuccess) stringResource(R.string.settings_ssh_test_ok) else stringResource(R.string.settings_ssh_test_fail, res.exceptionOrNull()?.localizedMessage ?: stringResource(R.string.settings_connection_refused))', 1),
    (SD, 'Text(text = if (isLocalToolsInstalled) "Installed and ready" else "Not installed (>30MB download)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))',
     'Text(text = if (isLocalToolsInstalled) stringResource(R.string.settings_installed_ready) else stringResource(R.string.settings_not_installed_download), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))', 1),
    (SD, 'text = if (isLocalToolsInstalled) "Installed ✓" else "Not Installed",',
     'text = if (isLocalToolsInstalled) stringResource(R.string.settings_installed_check) else stringResource(R.string.settings_not_installed),', 1),
    (SD, 'text = "$childProjectsCount ${if (childProjectsCount == 1) "sub-project" else "sub-projects"}",',
     'text = stringResource(R.string.settings_subproject_count, childProjectsCount),', 1),
    (SD, '''text = if (effectiveBackupPaths.size == selectedCount) "Backup $selectedCount Selected Projects"
                                       else "Backup $selectedCount Selected Projects (${effectiveBackupPaths.size} workspace roots)",''',
     '''text = if (effectiveBackupPaths.size == selectedCount) stringResource(R.string.settings_backup_selected, selectedCount)
                                       else stringResource(R.string.settings_backup_selected_roots, selectedCount, effectiveBackupPaths.size),''', 1),
    (SD, 'text = if (isRunning) "Executing in rootfs environment..." else if (exitCode == 0) "Completed in ${(durationMs / 1000.0)}s (Exit: 0)" else "Finished with error (Exit: $exitCode)",',
     'text = if (isRunning) stringResource(R.string.settings_rootfs_executing) else if (exitCode == 0) stringResource(R.string.settings_rootfs_completed, durationMs / 1000.0) else stringResource(R.string.settings_rootfs_finished_error, exitCode),', 1),
    (SD, 'text = if (isAdminActive) "Status: Protected (Device Admin Active)" else "Status: Inactive (Optional)",',
     'text = if (isAdminActive) stringResource(R.string.settings_admin_protected) else stringResource(R.string.settings_admin_inactive),', 1),
    (SD, 'text = if (isShieldEnabled) "Enabled (Replaces Clear Data with Space Manager)" else "Disabled (Standard OS Clear Data)",',
     'text = if (isShieldEnabled) stringResource(R.string.settings_shield_enabled) else stringResource(R.string.settings_shield_disabled),', 1),
    (SD, '''text = if (activeSandbox) "Enabled: Isolates commands in sandbox without host network access."
                            else "Disabled (Recommended for Hub): Commands execute directly in your Termux / Linux environment.",''',
     '''text = if (activeSandbox) stringResource(R.string.settings_sandbox_enabled_desc)
                            else stringResource(R.string.settings_sandbox_disabled_desc),''', 1),
    (SD, 'text = if (allRules.isEmpty()) "No custom permission rules configured." else "No rules match the selected filter.",',
     'text = if (allRules.isEmpty()) stringResource(R.string.settings_rules_none) else stringResource(R.string.settings_rules_no_match),', 1),
    (SD, 'text = if (project.isInheritingGlobal) "🌐 Inherits Global" else "⚠️ Custom Override",',
     'text = if (project.isInheritingGlobal) stringResource(R.string.settings_rules_inherit_global) else stringResource(R.string.settings_rules_custom_override),', 1),
    # ---- actionLabel 改成 @StringRes（同坑 19 族）----
    (SD, '''    val actionLabel: String
        get() = when (action.lowercase()) {
            "command" -> "Command"
            "mcp" -> "MCP Tool"
            "read_file" -> "Read File"
            "write_file" -> "Write File"
            else -> action
        }''',
     '''    /** 用户可见的动作名走资源；未识别的动作返回 0，渲染处原样显示技术值。 */
    val actionLabelRes: Int
        get() = when (action.lowercase()) {
            "command" -> R.string.settings_rule_action_command
            "mcp" -> R.string.settings_rule_action_mcp
            "read_file" -> R.string.settings_rule_action_read_file
            "write_file" -> R.string.settings_rule_action_write_file
            else -> 0
        }''', 1),
    (SD, 'text = stringResource(R.string.settings_rule_actionicon_rule_actionlabel, rule.actionIcon, rule.actionLabel),',
     'text = stringResource(R.string.settings_rule_actionicon_rule_actionlabel, rule.actionIcon, if (rule.actionLabelRes != 0) stringResource(rule.actionLabelRes) else rule.action),', 1),
]

RESOURCES = [
    ("settings_hud_enabled_subtitle", "Floating live metrics HUD enabled", "悬浮实时指标 HUD 已启用"),
    ("settings_hub_test_ok", "✓ Hub Connected!", "✓ Hub 已连接！"),
    ("settings_bridge_test_ok", "✓ Bridge Connected!", "✓ 桥接已连接！"),
    ("settings_testing_all", "Testing All...", "正在测试全部……"),
    ("settings_test_and_refresh", "Test & Refresh", "测试并刷新"),
    ("settings_edit_mcp_server", "Edit MCP Server", "编辑 MCP 服务器"),
    ("settings_add_mcp_server", "Add MCP Server", "添加 MCP 服务器"),
    ("settings_ssh_mode", "SSH Mode", "SSH 模式"),
    ("settings_local_mode", "Local Mode", "本地模式"),
    ("settings_ssh_mode_desc", "Direct SSH to Termux sshd without local rootfs",
     "直接 SSH 连接 Termux sshd，不经过本地 rootfs"),
    ("settings_local_mode_desc", "Built-in chroot rootfs environment", "内置 chroot rootfs 环境"),
    ("settings_ssh_test_ok", "✓ SSH connection successful!", "✓ SSH 连接成功！"),
    ("settings_connection_refused", "Connection refused", "连接被拒绝"),
    ("settings_installed_ready", "Installed and ready", "已安装就绪"),
    ("settings_not_installed_download", "Not installed (>30MB download)", "未安装（需下载 >30MB）"),
    ("settings_installed_check", "Installed ✓", "已安装 ✓"),
    ("settings_not_installed", "Not Installed", "未安装"),
    ("settings_rootfs_executing", "Executing in rootfs environment...", "正在 rootfs 环境中执行……"),
    ("settings_admin_protected", "Status: Protected (Device Admin Active)",
     "状态：已保护（设备管理器已启用）"),
    ("settings_admin_inactive", "Status: Inactive (Optional)", "状态：未启用（可选）"),
    ("settings_shield_enabled", "Enabled (Replaces Clear Data with Space Manager)",
     "已启用（「清除数据」改为空间管理）"),
    ("settings_shield_disabled", "Disabled (Standard OS Clear Data)", "未启用（使用系统标准的清除数据）"),
    ("settings_sandbox_enabled_desc",
     "Enabled: Isolates commands in sandbox without host network access.",
     "已启用：命令在沙箱中隔离运行，无宿主网络访问。"),
    ("settings_sandbox_disabled_desc",
     "Disabled (Recommended for Hub): Commands execute directly in your Termux / Linux environment.",
     "未启用（推荐 Hub 用户）：命令直接在 Termux / Linux 环境中执行。"),
    ("settings_rules_none", "No custom permission rules configured.", "尚未配置自定义权限规则。"),
    ("settings_rules_no_match", "No rules match the selected filter.", "没有符合当前筛选的规则。"),
    ("settings_rules_inherit_global", "🌐 Inherits Global", "🌐 继承全局"),
    ("settings_rules_custom_override", "⚠️ Custom Override", "⚠️ 自定义覆盖"),
    ("settings_rule_action_command", "Command", "命令"),
    ("settings_rule_action_mcp", "MCP Tool", "MCP 工具"),
    ("settings_rule_action_read_file", "Read File", "读文件"),
    ("settings_rule_action_write_file", "Write File", "写文件"),
]

DYNAMIC = {
    "settings_hub_test_fail": {"en": "✗ Hub unreachable ($port)", "zh": "✗ Hub 不可达（%1$s）",
                               "module": "settings", "args": ["port"]},
    "settings_bridge_test_fail": {"en": "✗ Bridge unreachable ($port)", "zh": "✗ 桥接不可达（%1$s）",
                                  "module": "settings", "args": ["port"]},
    "settings_ssh_test_fail": {"en": "✗ Failed: $error", "zh": "✗ 失败：%1$s",
                               "module": "settings", "args": ["error"]},
    "settings_mcp_command_url": {"en": "URL: $url", "zh": "URL：%1$s",
                                 "module": "settings", "args": ["url"]},
    "settings_mcp_command_raw": {"en": "> $text", "zh": "> %1$s",
                                 "module": "settings", "args": ["text"]},
    "settings_subproject_count": {"en": "$count sub-project(s)", "zh": "%1$s 个子项目",
                                  "module": "settings", "args": ["count"]},
    "settings_backup_selected": {"en": "Backup $count Selected Projects",
                                 "zh": "备份选中的 %1$s 个项目",
                                 "module": "settings", "args": ["count"]},
    "settings_backup_selected_roots": {
        "en": "Backup $count Selected Projects ($roots workspace roots)",
        "zh": "备份选中的 %1$s 个项目（%2$s 个工作区根目录）",
        "module": "settings", "args": ["count", "roots"]},
    "settings_rootfs_completed": {"en": "Completed in $seconds s (Exit: 0)",
                                  "zh": "已完成，用时 %1$s 秒（退出码：0）",
                                  "module": "settings", "args": ["seconds"]},
    "settings_rootfs_finished_error": {"en": "Finished with error (Exit: $exitCode)",
                                       "zh": "出错结束（退出码：%1$s）",
                                       "module": "settings", "args": ["exitCode"]},
}


def main():
    errors, changed = [], 0
    path = SRC + SD
    text = open(path, encoding="utf-8").read()
    for _rel, old, new, count in SITES:
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
        catalog.append({"key": key, "module": "settings", "en": en, "zh": zh,
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
