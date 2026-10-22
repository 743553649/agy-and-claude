package com.example.gemini.ui.claude

import com.example.gemini.R
import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.gemini.data.agent.claude.ClaudeChatBackend
import com.example.gemini.data.agent.claude.ClaudeTranscript
import com.example.gemini.domain.model.AgentKind
import com.example.gemini.ui.agents.ApprovalChoice
import com.example.gemini.ui.agents.ApprovalItem
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Claude's permission prompts as approval-panel items: tool approvals (allow once / always allow from Claude's
 * suggestions / deny with feedback) and plan approval (ExitPlanMode). Questions (AskUserQuestion) go to the
 * question panel instead.
 */
fun claudeApprovalItems(
    context: Context,
    pending: List<ClaudeTranscript.PendingPermission>,
    backend: ClaudeChatBackend
): List<ApprovalItem> =
    pending.map { p ->
        if (p.toolName == "ExitPlanMode") {
            ApprovalItem(
                key = p.toolUseId,
                agent = AgentKind.CLAUDE,
                icon = Icons.Outlined.Checklist,
                title = context.getString(R.string.claude_review_the_plan),
                subtitle = context.getString(R.string.claude_plan_mode),
                markdown = (p.input["plan"] as? JsonPrimitive)?.contentOrNull.orEmpty().ifBlank { context.getString(R.string.claude_empty_plan) },
                primary = ApprovalChoice(context.getString(R.string.claude_approve_accept_edits)) { backend.approvePlan(p.toolUseId, "acceptEdits") },
                secondary = listOf(ApprovalChoice(context.getString(R.string.claude_approve_review_each)) { backend.approvePlan(p.toolUseId, "default") }),
                denyLabel = R.string.claude_keep_planning,
                denyPlaceholder = R.string.claude_plan_deny_placeholder,
                onDeny = { backend.keepPlanning(p.toolUseId, it.orEmpty()) }
            )
        } else {
            val (icon, title) = toolTitle(context, p.toolName)
            ApprovalItem(
                key = p.toolUseId,
                agent = AgentKind.CLAUDE,
                icon = icon,
                title = title,
                subtitle = if (p.toolName.startsWith("mcp__")) "MCP · " + p.toolName.removePrefix("mcp__").substringBefore("__") else p.toolName,
                target = toolTarget(p.toolName, p.input, p.blockedPath),
                preview = toolPreview(context, p.toolName, p.input),
                reason = p.reason,
                primary = ApprovalChoice(context.getString(R.string.agents_allow)) { backend.approveToolAlways(p.toolUseId, -1) },
                always = p.suggestions.orEmpty().mapIndexedNotNull { i, s ->
                    (s as? JsonObject)?.let { ApprovalChoice(describeSuggestion(context, it)) { backend.approveToolAlways(p.toolUseId, i) } }
                },
                denyPlaceholder = R.string.claude_deny_placeholder,
                onDeny = { backend.rejectToolById(p.toolUseId, it) }
            )
        }
    }

private fun toolTitle(context: Context, name: String): Pair<ImageVector, String> = when {
    name == "Bash" -> Icons.Outlined.Terminal to context.getString(R.string.agents_approval_run_command)
    name == "Write" -> Icons.AutoMirrored.Outlined.NoteAdd to context.getString(R.string.claude_approval_create_file)
    name == "Edit" || name == "MultiEdit" -> Icons.Outlined.EditNote to context.getString(R.string.agents_approval_edit_file)
    name == "NotebookEdit" -> Icons.Outlined.EditNote to context.getString(R.string.claude_approval_edit_notebook)
    name == "WebFetch" -> Icons.Outlined.Language to context.getString(R.string.claude_approval_fetch_web_page)
    name == "WebSearch" -> Icons.Outlined.TravelExplore to context.getString(R.string.agents_approval_search_web)
    name == "Read" || name == "Glob" || name == "Grep" || name == "LS" -> Icons.Outlined.FolderOpen to context.getString(R.string.claude_approval_read_outside_project)
    name.startsWith("mcp__") -> Icons.Outlined.Extension to context.getString(R.string.agents_approval_use_tool, name.substringAfterLast("__"))
    else -> Icons.Outlined.Shield to context.getString(R.string.agents_approval_allow_tool, name)
}

private fun toolTarget(name: String, input: JsonObject, blockedPath: String?): String? = when (name) {
    "Write", "Edit", "MultiEdit", "NotebookEdit" -> str(input, "file_path").ifBlank { str(input, "notebook_path") }.ifBlank { null }
    "WebFetch" -> str(input, "url").ifBlank { null }
    else -> blockedPath
}

private fun str(o: JsonObject, key: String) = (o[key] as? JsonPrimitive)?.contentOrNull.orEmpty()

private fun toolPreview(context: Context, name: String, input: JsonObject): String = when (name) {
    "Bash" -> "$ " + str(input, "command") + str(input, "description").takeIf { it.isNotBlank() }?.let { "\n# $it" }.orEmpty()
    "Write" -> str(input, "content").lines().take(40).joinToString("\n") { "+ $it" }
    "Edit" -> str(input, "old_string").lines().take(20).joinToString("\n") { "- $it" } + "\n" +
        str(input, "new_string").lines().take(20).joinToString("\n") { "+ $it" }
    "MultiEdit" -> context.getString(R.string.claude_edits_count, (input["edits"] as? JsonArray)?.size ?: 0)
    "WebFetch" -> str(input, "prompt")
    "WebSearch" -> str(input, "query")
    else -> input.entries.joinToString("\n") { (k, v) -> "$k: ${shortValue(v)}" }
}

private fun shortValue(v: JsonElement): String = when (v) {
    is JsonPrimitive -> v.contentOrNull.orEmpty().take(200)
    else -> v.toString().take(200)
}

/** Text for one `permission_suggestions` entry. */
private fun describeSuggestion(context: Context, s: JsonObject): String {
    val where = when (str(s, "destination")) {
        "session" -> context.getString(R.string.claude_scope_for_session)
        "localSettings" -> context.getString(R.string.claude_scope_local_settings)
        "projectSettings" -> context.getString(R.string.claude_scope_project_settings)
        "userSettings" -> context.getString(R.string.claude_scope_user_settings)
        else -> ""
    }
    return when (str(s, "type")) {
        "addRules", "replaceRules" -> {
            val rules = (s["rules"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }.joinToString(", ") { r ->
                val content = str(r, "ruleContent")
                if (content.isBlank()) str(r, "toolName") else "${str(r, "toolName")}($content)"
            }
            context.getString(R.string.claude_always_allow_rules, rules, where).trim()
        }
        "addDirectories" -> context.getString(R.string.claude_allow_access_to, (s["directories"] as? JsonArray).orEmpty().joinToString(", ") { (it as? JsonPrimitive)?.contentOrNull.orEmpty() }, where).trim()
        "setMode" -> context.getString(R.string.claude_switch_to_mode, permissionModeLabel(context, str(s, "mode")), where).trim()
        else -> str(s, "type")
    }
}
