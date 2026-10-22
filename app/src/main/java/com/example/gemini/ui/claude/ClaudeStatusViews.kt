package com.example.gemini.ui.claude

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gemini.data.agent.claude.ClaudeStatus
import com.example.gemini.R
import com.example.gemini.domain.model.AgentKind
import com.example.gemini.ui.agents.AgentActionPill
import com.example.gemini.ui.agents.AgentSetupPrompt
import com.example.gemini.ui.agents.AgentStatusDot
import com.example.gemini.ui.components.ClaudeAccent

/** Empty Claude chat when Claude Code is not ready: what is wrong and the one action that fixes it. */
@Composable
fun ClaudeStatusPrompt(
    state: ClaudeStatus,
    error: String?,
    onSignIn: () -> Unit,
    onSetup: () -> Unit,
    onRestartServer: () -> Unit,
    onRetry: () -> Unit,
    onServerLogs: () -> Unit,
    modifier: Modifier = Modifier,
    /** The local server is (re)starting: wait instead of reporting it offline. */
    serverStarting: Boolean = false
) {
    val dot = state.dotColor
    if (serverStarting) {
        AgentSetupPrompt(
            AgentKind.CLAUDE, stringResource(R.string.claude_status_server_starting_title), stringResource(R.string.claude_status_server_starting_desc), modifier, ClaudeStatus.CHECKING.dotColor
        )
        return
    }
    when (state) {
        ClaudeStatus.CHECKING -> AgentSetupPrompt(
            AgentKind.CLAUDE, stringResource(R.string.chat_apptoast_checking_claude_code), stringResource(R.string.claude_status_checking_desc), modifier, dot
        )
        ClaudeStatus.SIGNED_OUT -> AgentSetupPrompt(
            AgentKind.CLAUDE,
            stringResource(R.string.claude_sign_in_to_claude_code),
            stringResource(R.string.claude_status_sign_in_desc),
            modifier, dot,
            actionLabel = stringResource(R.string.claude_status_action_sign_in), onAction = onSignIn
        )
        ClaudeStatus.SIGNING_IN -> AgentSetupPrompt(
            AgentKind.CLAUDE,
            stringResource(R.string.claude_status_signing_in_title),
            stringResource(R.string.claude_status_signing_in_desc),
            modifier, dot,
            actionLabel = stringResource(R.string.claude_status_action_open_sign_in), onAction = onSignIn
        )
        ClaudeStatus.NOT_INSTALLED -> AgentSetupPrompt(
            AgentKind.CLAUDE,
            stringResource(R.string.claude_status_not_installed_title),
            stringResource(R.string.claude_status_not_installed_desc),
            modifier, dot,
            actionLabel = stringResource(R.string.claude_status_action_setup), onAction = onSetup,
            secondaryLabel = stringResource(R.string.claude_status_action_check_again), onSecondary = onRetry
        )
        ClaudeStatus.BRIDGE_OUTDATED -> AgentSetupPrompt(
            AgentKind.CLAUDE,
            stringResource(R.string.claude_status_restart_title),
            error ?: stringResource(R.string.claude_status_restart_desc),
            modifier, dot,
            actionLabel = stringResource(R.string.claude_status_action_restart_server), onAction = onRestartServer,
            secondaryLabel = stringResource(R.string.claude_status_action_check_again), onSecondary = onRetry
        )
        ClaudeStatus.BRIDGE_OFFLINE -> AgentSetupPrompt(
            AgentKind.CLAUDE,
            stringResource(R.string.claude_status_offline_title),
            stringResource(R.string.claude_status_offline_desc),
            modifier, dot,
            actionLabel = stringResource(R.string.common_retry), onAction = onRetry,
            secondaryLabel = stringResource(R.string.agents_server_logs), onSecondary = onServerLogs
        )
        ClaudeStatus.ERROR -> AgentSetupPrompt(
            AgentKind.CLAUDE,
            stringResource(R.string.claude_status_error_title),
            stringResource(R.string.claude_status_error_body, error ?: stringResource(R.string.claude_status_check_failed)),
            modifier, dot,
            actionLabel = stringResource(R.string.claude_status_action_check_again), onAction = onRetry
        )
        ClaudeStatus.READY -> Unit
    }
}

/** One-line notice above the input of a Claude chat that has messages but Claude Code is not ready. */
@Composable
fun ClaudeStatusBanner(
    state: ClaudeStatus,
    onSignIn: () -> Unit,
    onSetup: () -> Unit,
    onRestartServer: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (text, action) = when (state) {
        ClaudeStatus.SIGNED_OUT -> stringResource(R.string.claude_banner_signed_out) to (stringResource(R.string.claude_status_action_sign_in) to onSignIn)
        ClaudeStatus.SIGNING_IN -> stringResource(R.string.claude_banner_signing_in) to (stringResource(R.string.common_open) to onSignIn)
        ClaudeStatus.NOT_INSTALLED -> stringResource(R.string.claude_banner_not_installed) to (stringResource(R.string.claude_status_action_set_up) to onSetup)
        ClaudeStatus.BRIDGE_OUTDATED -> stringResource(R.string.claude_banner_restart) to (stringResource(R.string.ide_restart) to onRestartServer)
        ClaudeStatus.BRIDGE_OFFLINE -> stringResource(R.string.claude_status_offline_title) to (stringResource(R.string.common_retry) to onRetry)
        else -> return
    }
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, state.dotColor.copy(alpha = 0.45f)),
        shadowElevation = 2.dp
    ) {
        Row(Modifier.padding(start = 12.dp, end = 8.dp, top = 7.dp, bottom = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            AgentStatusDot(state.dotColor, null, size = 9.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            AgentActionPill(action.first, ClaudeAccent, action.second)
        }
    }
}

/** Above the input while a sent message waits for the claude process to boot. */
@Composable
fun ClaudeStartupNotice(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        color = ClaudeAccent.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, ClaudeAccent.copy(alpha = 0.3f))
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.padding(end = 10.dp).size(14.dp),
                strokeWidth = 2.dp,
                color = ClaudeAccent
            )
            androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.claude_the_first_message_of_a_chat_starts_claud),
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
