package com.example.gemini.ui.agents

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.TravelExplore
import com.example.gemini.R
import com.example.gemini.data.remote.dto.PermissionScopes
import com.example.gemini.domain.model.AgentKind
import com.example.gemini.domain.model.ToolCall
import com.example.gemini.domain.model.ToolType
import com.example.gemini.ui.chat.PendingToolApproval

/**
 * Antigravity's pending tool approvals as approval-panel items: allow once, always allow (conversation /
 * workspace / everywhere), or deny with a reason.
 *
 * 文案在 `remember` 里构建（非 Composable），所以由调用方传 Context，用 getString 取资源。
 */
fun agyApprovalItems(
    context: Context,
    pending: List<PendingToolApproval>,
    onApprove: (toolCall: ToolCall, messageId: String, scope: String) -> Unit,
    onReject: (toolCall: ToolCall, messageId: String, reason: String?) -> Unit
): List<ApprovalItem> = pending.map { approval ->
    val call = approval.toolCall
    val (icon, title) = when (call.toolType) {
        ToolType.BASH -> Icons.Outlined.Terminal to context.getString(R.string.agents_approval_run_command)
        ToolType.EDIT_FILE -> Icons.Outlined.EditNote to context.getString(R.string.agents_approval_edit_file)
        ToolType.VIEW_FILE, ToolType.LIST_DIR, ToolType.FIND, ToolType.GREP_SEARCH ->
            Icons.Outlined.FolderOpen to context.getString(R.string.agents_approval_read_files)
        ToolType.SEARCH_WEB -> Icons.Outlined.TravelExplore to context.getString(R.string.agents_approval_search_web)
        ToolType.READ_URL -> Icons.Outlined.Language to context.getString(R.string.agents_approval_open_web_page)
        ToolType.GENERATE_IMAGE -> Icons.Outlined.Image to context.getString(R.string.agents_approval_generate_image)
        ToolType.MCP -> Icons.Outlined.Extension to context.getString(
            R.string.agents_approval_use_tool, call.name.substringAfterLast("__")
        )
        else -> Icons.Outlined.Shield to context.getString(R.string.agents_approval_allow_tool, call.name)
    }
    val body = call.command.ifBlank { call.output }
    fun scope(label: String, description: String, value: String) =
        ApprovalChoice(label, description) { onApprove(call, approval.messageId, value) }
    ApprovalItem(
        key = call.id,
        agent = AgentKind.AGY,
        icon = icon,
        title = title,
        subtitle = call.name,
        preview = if (call.toolType == ToolType.BASH) "$ $body" else body,
        primary = ApprovalChoice(context.getString(R.string.agents_allow)) {
            onApprove(call, approval.messageId, PermissionScopes.ONCE)
        },
        always = listOf(
            scope(
                context.getString(R.string.agents_scope_conversation),
                context.getString(R.string.agents_scope_conversation_desc),
                PermissionScopes.CONVERSATION
            ),
            scope(
                context.getString(R.string.agents_scope_workspace),
                context.getString(R.string.agents_scope_workspace_desc),
                PermissionScopes.PROJECT
            ),
            scope(
                context.getString(R.string.agents_scope_everywhere),
                context.getString(R.string.agents_scope_everywhere_desc),
                PermissionScopes.GLOBAL
            )
        ),
        denyPlaceholder = R.string.agents_deny_placeholder_antigravity,
        onDeny = { onReject(call, approval.messageId, it) }
    )
}
