package com.example.gemini.ui.settings

import com.example.gemini.ui.claude.SettingsGroupHeader
import com.example.gemini.R
import com.example.gemini.ui.components.ClaudeAccent
import android.Manifest
import androidx.annotation.StringRes
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.example.gemini.theme.isAppInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.gemini.domain.model.AiModel
import com.example.gemini.domain.model.ModelQuota
import com.example.gemini.data.preferences.AuthPreferences
import com.example.gemini.data.local.LocalEnvironmentManager
import com.example.gemini.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.gemini.BuildConfig
import com.example.gemini.data.updater.AppUpdateManager
import com.example.gemini.data.updater.AppUpdateInfo
import com.example.gemini.ui.updater.AppUpdateDialog
import com.example.gemini.data.local.LocalServerManager
import android.app.DownloadManager
import android.widget.Toast
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import android.util.Log
import com.example.gemini.data.admin.AntiGemDeviceAdminReceiver
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipFile
import org.json.JSONObject
import org.json.JSONArray
import com.example.gemini.data.daemon.ProjectItem
import com.example.gemini.data.daemon.TermuxDaemonManager
import com.example.gemini.data.daemon.IdeApiClient

enum class SettingsSection(@StringRes val titleRes: Int, @StringRes val subtitleRes: Int) {
    MAIN(R.string.settings_section_settings_preferences, R.string.settings_section_configure_your_antigem_experienc_sub),
    APPEARANCE(R.string.settings_appearance_theme, R.string.settings_section_theme_dark_mode_and_chat_font_sc_sub),
    SERVERS(R.string.settings_servers_connectivity, R.string.settings_section_configure_agy_hub_and_ide_bridge_sub),
    WEBVIEW_EXTENSIONS(R.string.settings_webview_extensions, R.string.settings_section_install_and_manage_mcp_extension_sub),
    MCP(R.string.settings_section_mcp_servers, R.string.settings_ui_model_context_protocol_tools_integration),
    SKILLS_PLUGINS(R.string.settings_skills_plugins, R.string.settings_section_agent_capabilities_google_plugin_sub),
    AUTOMATION(R.string.settings_automation_device_tools, R.string.settings_section_browser_terminal_ai_agent_permis_sub),
    TERMINAL(R.string.settings_section_terminal_shell, R.string.settings_section_ssh_configuration_local_tools_an_sub),
    BACKUPS(R.string.settings_backups_restore, R.string.settings_ui_backup_rootfs_environment_and_agy_chat_h),
    DATA_PROTECTION(R.string.settings_data_loss_protection, R.string.settings_section_uninstall_shield_app_info_clear_sub),
    COMMANDS(R.string.settings_commands_permissions, R.string.settings_section_auto_run_policy_sandbox_mode_and_sub),
    CLAUDE(R.string.settings_section_claude_code, R.string.settings_section_account_models_permissions_mcp_p_sub),
    AGENTS(R.string.settings_agents, R.string.settings_section_run_antigravity_claude_code_or_b_sub),
    DIAGNOSTICS(R.string.settings_diagnostics_performance, R.string.settings_section_live_network_connections_active_sub),
    ABOUT(R.string.settings_about_info, R.string.settings_section_app_version_package_details_and_sub)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsDialog(
    userEmail: String?,
    projectId: String,
    tier: String,
    availableModels: List<AiModel> = AiModel.DEFAULT_MODELS,
    enabledModelIds: Set<String>?,
    quotas: List<ModelQuota>,
    isServerListening: Boolean = false,
    isServerLoading: Boolean = false,
    contextWindowLimit: Int = 10,
    summaryModelId: String = "always_ask",
    isDevModeEnabled: Boolean = false,
    chatFontScale: Float = 1.0f,
    worldImageUri: String? = null,
    onPickWorldImage: (String) -> Unit = {},
    onClearWorldImage: () -> Unit = {},
    themeMode: String = "SYSTEM",
    appLanguage: String = "system",
    agyHubUrl: String = com.example.gemini.data.preferences.AuthPreferences.currentHubUrl,
    agyBridgeHttpUrl: String = com.example.gemini.data.preferences.AuthPreferences.currentBridgeHttpUrl,
    isServerOnline: Boolean = true,
    isBridgeOnline: Boolean = false,
    useSshTerminal: Boolean = false,
    sshHost: String = "127.0.0.1",
    sshPort: Int = 8022,
    sshUser: String = "root",
    sshPass: String = "root",
    terminalFontSize: Int = 14,
    terminalCursorStyle: String = "BAR",
    terminalBufferSize: Int = 20000,
    terminalTheme: String = "DEFAULT",
    isLocalToolsEnabled: Boolean = false,
    isLocalToolsInstalled: Boolean = false,
    onLoginWithGoogle: () -> Unit,
    onToggleServer: (Boolean) -> Unit = {},
    onLogout: () -> Unit = {},
    onManualTokenEntered: (String) -> Unit,
    onToggleModelEnabled: (String, Boolean) -> Unit,
    onEnableAllModels: () -> Unit,
    onRefreshQuotas: () -> Unit,
    onSetContextWindowLimit: (Int) -> Unit = {},
    onSetSummaryModelId: (String) -> Unit = {},
    onSetChatFontScale: (Float) -> Unit = {},
    onSetThemeMode: (String) -> Unit = {},
    onSetAppLanguage: (String) -> Unit = {},
    onSaveServerUrls: (String, String) -> Unit = { _, _ -> },
    onSaveTerminalPreferences: (Int, String, Int, String) -> Unit = { _, _, _, _ -> },
    onToggleDevMode: (Boolean) -> Unit = {},
    onToggleUseSshTerminal: (Boolean) -> Unit = {},
    onSaveSshSettings: (String, Int, String, String) -> Unit = { _, _, _, _ -> },
    onToggleLocalTools: (Boolean) -> Unit = {},
    onInstallLocalTools: () -> Unit = {},
    onOpenLocalTerminal: () -> Unit = {},
    onResetLocalTools: () -> Unit = {},
    mcpServers: List<com.example.gemini.domain.model.McpServerState> = emptyList(),
    isMcpLoading: Boolean = false,
    isMcpRefreshing: Boolean = false,
    refreshingMcpServer: String? = null,
    mcpErrorMessage: String? = null,
    mcpStatusMessage: String? = null,
    onClearMcpStatus: () -> Unit = {},
    onRefreshMcpServers: () -> Unit = {},
    onRefreshMcpServer: (String) -> Unit = {},
    onToggleMcpServer: (String, Boolean) -> Unit = { _, _ -> },
    onSaveMcpServer: (com.example.gemini.domain.model.McpServerSpec, String?) -> Unit = { _, _ -> },
    onDeleteMcpServer: (String) -> Unit = {},
    availableCascadePlugins: List<com.example.gemini.data.remote.dto.AvailableCascadePluginDto> = emptyList(),
    isCascadePluginsLoading: Boolean = false,
    installingCascadePluginId: String? = null,
    onSearchCascadePlugins: (String) -> Unit = {},
    onInstallCascadePlugin: (com.example.gemini.data.remote.dto.AvailableCascadePluginDto) -> Unit = {},
    allSkills: List<com.example.gemini.data.remote.dto.SkillDefinitionDto> = emptyList(),
    isSkillsLoading: Boolean = false,
    skillsFilterScope: String = "GLOBAL",
    onSetSkillsFilterScope: (String) -> Unit = {},
    onRefreshSkills: () -> Unit = {},
    installedPlugins: List<com.example.gemini.data.remote.dto.InstalledPluginDto> = emptyList(),
    isInstalledPluginsLoading: Boolean = false,
    onRefreshInstalledPlugins: () -> Unit = {},
    googlePluginsCatalog: List<com.example.gemini.data.remote.dto.BuildWithGooglePluginItemDto> = emptyList(),
    isGooglePluginsLoading: Boolean = false,
    installingGooglePluginId: String? = null,
    deletingPluginId: String? = null,
    onRefreshGooglePlugins: () -> Unit = {},
    onInstallGooglePlugin: (String, String) -> Unit = { _, _ -> },
    onDeletePlugin: (String, String) -> Unit = { _, _ -> },
    pluginActionStatusMessage: String? = null,
    pluginActionErrorMessage: String? = null,
    onClearPluginStatus: () -> Unit = {},
    isBrowserAutomationEnabled: Boolean = true,
    isTerminalAutomationEnabled: Boolean = true,
    onToggleBrowserAutomation: (Boolean) -> Unit = {},
    onToggleTerminalAutomation: (Boolean) -> Unit = {},
    isFlowAutomationEnabled: Boolean = false,
    onToggleFlowAutomation: (Boolean) -> Unit = {},
    isFloatingSwitcherEnabled: Boolean = true,
    floatingSwitcherOrientation: String = "HORIZONTAL",
    floatingSwitcherItems: List<String> = listOf("chat", "ide", "terminal", "browser"),
    floatingSwitcherAutoCollapseSec: Int = 0,
    onToggleFloatingSwitcher: (Boolean) -> Unit = {},
    onSetFloatingSwitcherOrientation: (String) -> Unit = {},
    onSetFloatingSwitcherItems: (List<String>) -> Unit = {},
    onSetFloatingSwitcherAutoCollapseSec: (Int) -> Unit = {},
    onResetFloatingSwitcherPosition: () -> Unit = {},
    commandAutoExecutionPolicy: String = "CASCADE_COMMANDS_AUTO_EXECUTION_EAGER",
    commandSandboxEnabled: Boolean = false,
    requireApprovalForFileEdits: Boolean = false,
    defaultApprovalScope: String = "PERMISSION_SCOPE_ONCE",
    groupChatsByWorkspace: Boolean = false,
    onToggleGroupChatsByWorkspace: (Boolean) -> Unit = {},
    globalSecuritySettings: com.example.gemini.data.remote.AgyHubClient.GlobalUserSettings? = null,
    globalSettingsError: String? = null,
    isGlobalSettingsLoading: Boolean = false,
    projectsList: List<com.example.gemini.data.remote.AgyHubClient.ProjectItem> = emptyList(),
    isProjectsLoading: Boolean = false,
    onSetGlobalArtifactReviewMode: (String) -> Unit = {},
    onSetGlobalSecurityPreset: (autoExec: String, fileAccess: String) -> Unit = { _, _ -> },
    onSetGlobalCustomTerminalPolicy: (String) -> Unit = {},
    onSetGlobalCustomFileAccessPolicy: (String) -> Unit = {},
    onSetGlobalTerminalSandbox: (Boolean) -> Unit = {},
    onSetProjectInheritGlobal: (com.example.gemini.data.remote.AgyHubClient.ProjectItem) -> Unit = {},
    onSetProjectPreset: (com.example.gemini.data.remote.AgyHubClient.ProjectItem, autoExec: String, fileAccess: String) -> Unit = { _, _, _ -> },
    onRefreshSecurityAndProjects: () -> Unit = {},
    onSetCommandAutoExecutionPolicy: (String) -> Unit = {},
    onSetCommandSandboxEnabled: (Boolean) -> Unit = {},
    onSetRequireApprovalForFileEdits: (Boolean) -> Unit = {},
    onSetDefaultApprovalScope: (String) -> Unit = {},
    onAddPermissionRule: (action: String, pattern: String, decision: String) -> Unit = { _, _, _ -> },
    onRemovePermissionRule: (rawRule: String) -> Unit = {},
    onChangePermissionRuleDecision: (rawRule: String, newDecision: String) -> Unit = { _, _ -> },
    isFloatingDiagnosticsEnabled: Boolean = false,
    onToggleFloatingDiagnostics: (Boolean) -> Unit = {},
    isNetworkInspectorEnabled: Boolean = false,
    onToggleNetworkInspector: (Boolean) -> Unit = {},
    isFloatingNetworkInspectorEnabled: Boolean = false,
    onToggleFloatingNetworkInspector: (Boolean) -> Unit = {},
    onOpenNetworkInspector: () -> Unit = {},
    claude: com.example.gemini.ui.claude.ClaudeSettingsDeps? = null,
    initialClaudePage: com.example.gemini.ui.claude.ClaudeSettingsPage? = null,
    enabledAgents: Set<com.example.gemini.domain.model.AgentKind> = setOf(com.example.gemini.domain.model.AgentKind.AGY),
    isApplyingAgents: Boolean = false,
    onApplyAgents: (Set<com.example.gemini.domain.model.AgentKind>) -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentSection by remember { mutableStateOf(if (initialClaudePage != null && claude != null) SettingsSection.CLAUDE else SettingsSection.MAIN) }
    var claudePage by remember { mutableStateOf(initialClaudePage ?: com.example.gemini.ui.claude.ClaudeSettingsPage.ACCOUNT) }
    // Hoisted so the main list keeps its position while a sub-section is open
    val mainMenuScrollState = rememberScrollState()

    // Intercept system Back button
    BackHandler(enabled = true) {
        if (currentSection != SettingsSection.MAIN) {
            currentSection = SettingsSection.MAIN
        } else {
            onDismiss()
        }
    }

    val isDark = isAppInDarkTheme()
    val cardBg = if (isDark) ClaudeDarkSurface else Color.White
    val cardBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .windowInsetsPadding(WindowInsets.imeAnimationTarget)
        ) {
            // Top App Bar
            TopAppBar(
                title = {
                    // a Claude Code page names itself here (it has no header of its own)
                    val isClaudePage = currentSection == SettingsSection.CLAUDE && claude != null
                    Column {
                        Text(
                            text = if (isClaudePage) "Claude Code · " + stringResource(claudePage.titleRes) else stringResource(currentSection.titleRes),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (currentSection != SettingsSection.MAIN) {
                            Text(
                                text = if (isClaudePage) stringResource(claudePage.subtitleRes) else stringResource(currentSection.subtitleRes),
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentSection != SettingsSection.MAIN) {
                            currentSection = SettingsSection.MAIN
                        } else {
                            onDismiss()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = stringResource(R.string.common_done),
                            color = ClaudeTerracotta,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )

            HorizontalDivider(
                thickness = 0.8.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )

            // Animated Screen Content
            AnimatedContent(
                targetState = currentSection,
                transitionSpec = {
                    if (targetState != SettingsSection.MAIN) {
                        (slideInHorizontally { width -> width } + fadeIn(animationSpec = tween(220)))
                            .togetherWith(slideOutHorizontally { width -> -width / 4 } + fadeOut(animationSpec = tween(180)))
                    } else {
                        (slideInHorizontally { width -> -width / 4 } + fadeIn(animationSpec = tween(220)))
                            .togetherWith(slideOutHorizontally { width -> width } + fadeOut(animationSpec = tween(180)))
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                label = "SettingsSectionTransition"
            ) { section ->
                when (section) {
                    SettingsSection.MAIN -> MainSettingsMenu(
                        themeMode = themeMode,
                        chatFontScale = chatFontScale,
                        isServerOnline = isServerOnline,
                        isBridgeOnline = isBridgeOnline,
                        useSshTerminal = useSshTerminal,
                        mcpServers = mcpServers,
                        allSkills = allSkills,
                        installedPlugins = installedPlugins,
                        isBrowserAutomationEnabled = isBrowserAutomationEnabled,
                        isTerminalAutomationEnabled = isTerminalAutomationEnabled,
                        isFlowAutomationEnabled = isFlowAutomationEnabled,
                        commandAutoExecutionPolicy = commandAutoExecutionPolicy,
                        commandSandboxEnabled = commandSandboxEnabled,
                        isFloatingDiagnosticsEnabled = isFloatingDiagnosticsEnabled,
                        isLocalToolsInstalled = isLocalToolsInstalled,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        scrollState = mainMenuScrollState,
                        enabledAgents = enabledAgents,
                        claudeGroup = claude?.let { deps ->
                            {
                                com.example.gemini.ui.claude.ClaudeSettingsGroup(deps) { page ->
                                    claudePage = page
                                    currentSection = SettingsSection.CLAUDE
                                }
                            }
                        },
                        onNavigate = { currentSection = it }
                    )

                    SettingsSection.APPEARANCE -> AppearanceSubScreen(
                        themeMode = themeMode,
                        appLanguage = appLanguage,
                        chatFontScale = chatFontScale,
                        groupChatsByWorkspace = groupChatsByWorkspace,
                        isFloatingSwitcherEnabled = isFloatingSwitcherEnabled,
                        floatingSwitcherOrientation = floatingSwitcherOrientation,
                        floatingSwitcherItems = floatingSwitcherItems,
                        floatingSwitcherAutoCollapseSec = floatingSwitcherAutoCollapseSec,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        onSetThemeMode = onSetThemeMode,
                        onSetAppLanguage = onSetAppLanguage,
                        onSetChatFontScale = onSetChatFontScale,
                        onToggleGroupChatsByWorkspace = onToggleGroupChatsByWorkspace,
                        onToggleFloatingSwitcher = onToggleFloatingSwitcher,
                        onSetFloatingSwitcherOrientation = onSetFloatingSwitcherOrientation,
                        onSetFloatingSwitcherItems = onSetFloatingSwitcherItems,
                        onSetFloatingSwitcherAutoCollapseSec = onSetFloatingSwitcherAutoCollapseSec,
                        onResetFloatingSwitcherPosition = onResetFloatingSwitcherPosition,
                        worldImageUri = worldImageUri,
                        onPickWorldImage = onPickWorldImage,
                        onClearWorldImage = onClearWorldImage
                    )

                    SettingsSection.SERVERS -> ServersSubScreen(
                        currentHubUrl = agyHubUrl,
                        currentBridgeUrl = agyBridgeHttpUrl,
                        isServerOnline = isServerOnline,
                        isBridgeOnline = isBridgeOnline,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        onSaveServerUrls = onSaveServerUrls
                    )

                    SettingsSection.WEBVIEW_EXTENSIONS -> WebViewExtensionsScreen(onChanged = onRefreshMcpServers)

                    SettingsSection.MCP -> McpSubScreen(
                        mcpServers = mcpServers,
                        isLoading = isMcpLoading,
                        isRefreshing = isMcpRefreshing,
                        refreshingServerName = refreshingMcpServer,
                        errorMessage = mcpErrorMessage,
                        statusMessage = mcpStatusMessage,
                        onClearStatus = onClearMcpStatus,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        onRefreshAll = onRefreshMcpServers,
                        onRefreshServer = onRefreshMcpServer,
                        onToggleServer = onToggleMcpServer,
                        onSaveServer = onSaveMcpServer,
                        onDeleteServer = onDeleteMcpServer,
                        availableCascadePlugins = availableCascadePlugins,
                        isCascadePluginsLoading = isCascadePluginsLoading,
                        installingCascadePluginId = installingCascadePluginId,
                        onSearchCascadePlugins = onSearchCascadePlugins,
                        onInstallCascadePlugin = onInstallCascadePlugin
                    )

                    SettingsSection.SKILLS_PLUGINS -> SkillsAndPluginsSubScreen(
                        skills = allSkills,
                        isSkillsLoading = isSkillsLoading,
                        skillsFilterScope = skillsFilterScope,
                        onSetSkillsFilterScope = onSetSkillsFilterScope,
                        onRefreshSkills = onRefreshSkills,
                        installedPlugins = installedPlugins,
                        isInstalledPluginsLoading = isInstalledPluginsLoading,
                        onRefreshInstalledPlugins = onRefreshInstalledPlugins,
                        googlePlugins = googlePluginsCatalog,
                        isGooglePluginsLoading = isGooglePluginsLoading,
                        installingGooglePluginId = installingGooglePluginId,
                        deletingPluginId = deletingPluginId,
                        onRefreshGooglePlugins = onRefreshGooglePlugins,
                        onInstallGooglePlugin = onInstallGooglePlugin,
                        onDeletePlugin = onDeletePlugin,
                        statusMessage = pluginActionStatusMessage,
                        errorMessage = pluginActionErrorMessage,
                        onClearStatus = onClearPluginStatus,
                        cardBg = cardBg,
                        cardBorder = cardBorder
                    )

                    SettingsSection.AUTOMATION -> AutomationSubScreen(
                        isBrowserAutomationEnabled = isBrowserAutomationEnabled,
                        isTerminalAutomationEnabled = isTerminalAutomationEnabled,
                        onToggleBrowserAutomation = onToggleBrowserAutomation,
                        onToggleTerminalAutomation = onToggleTerminalAutomation,
                        isFlowAutomationEnabled = isFlowAutomationEnabled,
                        onToggleFlowAutomation = onToggleFlowAutomation,
                        cardBg = cardBg,
                        cardBorder = cardBorder
                    )

                    SettingsSection.TERMINAL -> TerminalSubScreen(
                        useSshTerminal = useSshTerminal,
                        sshHost = sshHost,
                        sshPort = sshPort,
                        sshUser = sshUser,
                        sshPass = sshPass,
                        isLocalToolsEnabled = isLocalToolsEnabled,
                        isLocalToolsInstalled = isLocalToolsInstalled,
                        terminalFontSize = terminalFontSize,
                        terminalCursorStyle = terminalCursorStyle,
                        terminalBufferSize = terminalBufferSize,
                        terminalTheme = terminalTheme,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        onToggleUseSshTerminal = onToggleUseSshTerminal,
                        onSaveSshSettings = onSaveSshSettings,
                        onToggleLocalTools = onToggleLocalTools,
                        onInstallLocalTools = onInstallLocalTools,
                        onResetLocalTools = onResetLocalTools,
                        onSaveTerminalPreferences = onSaveTerminalPreferences,
                        onOpenLocalTerminal = onOpenLocalTerminal
                    )

                    SettingsSection.BACKUPS -> BackupsSubScreen(
                        isServerOnline = isServerOnline,
                        isBridgeOnline = isBridgeOnline,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        onStopServer = {
                            LocalServerManager.stopServer()
                            onToggleServer(false)
                        }
                    )

                    SettingsSection.DATA_PROTECTION -> DataProtectionSubScreen(
                        cardBg = cardBg,
                        cardBorder = cardBorder
                    )

                    SettingsSection.COMMANDS -> CommandsSubScreen(
                        commandAutoExecutionPolicy = commandAutoExecutionPolicy,
                        commandSandboxEnabled = commandSandboxEnabled,
                        requireApprovalForFileEdits = requireApprovalForFileEdits,
                        defaultApprovalScope = defaultApprovalScope,
                        globalSecuritySettings = globalSecuritySettings,
                        globalSettingsError = globalSettingsError,
                        isGlobalSettingsLoading = isGlobalSettingsLoading,
                        projectsList = projectsList,
                        isProjectsLoading = isProjectsLoading,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        onSetGlobalArtifactReviewMode = onSetGlobalArtifactReviewMode,
                        onSetGlobalSecurityPreset = onSetGlobalSecurityPreset,
                        onSetGlobalCustomTerminalPolicy = onSetGlobalCustomTerminalPolicy,
                        onSetGlobalCustomFileAccessPolicy = onSetGlobalCustomFileAccessPolicy,
                        onSetGlobalTerminalSandbox = onSetGlobalTerminalSandbox,
                        onSetProjectInheritGlobal = onSetProjectInheritGlobal,
                        onSetProjectPreset = onSetProjectPreset,
                        onRefreshSecurityAndProjects = onRefreshSecurityAndProjects,
                        onSetCommandAutoExecutionPolicy = onSetCommandAutoExecutionPolicy,
                        onSetCommandSandboxEnabled = onSetCommandSandboxEnabled,
                        onSetRequireApprovalForFileEdits = onSetRequireApprovalForFileEdits,
                        onSetDefaultApprovalScope = onSetDefaultApprovalScope,
                        onAddPermissionRule = onAddPermissionRule,
                        onRemovePermissionRule = onRemovePermissionRule,
                        onChangePermissionRuleDecision = onChangePermissionRuleDecision
                    )

                    SettingsSection.DIAGNOSTICS -> DiagnosticsSubScreen(
                        isFloatingDiagnosticsEnabled = isFloatingDiagnosticsEnabled,
                        onToggleFloatingDiagnostics = onToggleFloatingDiagnostics,
                        isNetworkInspectorEnabled = isNetworkInspectorEnabled,
                        onToggleNetworkInspector = onToggleNetworkInspector,
                        isFloatingNetworkInspectorEnabled = isFloatingNetworkInspectorEnabled,
                        onToggleFloatingNetworkInspector = onToggleFloatingNetworkInspector,
                        onOpenNetworkInspector = onOpenNetworkInspector,
                        cardBg = cardBg,
                        cardBorder = cardBorder
                    )

                    SettingsSection.AGENTS -> com.example.gemini.ui.agents.AgentsSettingsPage(
                        enabled = enabledAgents,
                        isApplying = isApplyingAgents,
                        onApply = onApplyAgents
                    )
                    SettingsSection.CLAUDE -> if (claude != null) {
                        com.example.gemini.ui.claude.ClaudeSettingsScreen(claude, claudePage) { currentSection = SettingsSection.MAIN }
                    }
                    SettingsSection.ABOUT -> AboutSubScreen(
                        cardBg = cardBg,
                        cardBorder = cardBorder
                    )
                }
            }
        }
    }
}

@Composable
private fun MainSettingsMenu(
    themeMode: String,
    chatFontScale: Float,
    isServerOnline: Boolean,
    isBridgeOnline: Boolean,
    useSshTerminal: Boolean,
    mcpServers: List<com.example.gemini.domain.model.McpServerState>,
    allSkills: List<com.example.gemini.data.remote.dto.SkillDefinitionDto> = emptyList(),
    installedPlugins: List<com.example.gemini.data.remote.dto.InstalledPluginDto> = emptyList(),
    isBrowserAutomationEnabled: Boolean,
    isTerminalAutomationEnabled: Boolean,
    isFlowAutomationEnabled: Boolean = false,
    commandAutoExecutionPolicy: String,
    commandSandboxEnabled: Boolean,
    isFloatingDiagnosticsEnabled: Boolean = false,
    isLocalToolsInstalled: Boolean = false,
    cardBg: Color,
    cardBorder: BorderStroke,
    scrollState: androidx.compose.foundation.ScrollState,
    claudeGroup: (@Composable () -> Unit)? = null,
    enabledAgents: Set<com.example.gemini.domain.model.AgentKind> = setOf(com.example.gemini.domain.model.AgentKind.AGY),
    onNavigate: (SettingsSection) -> Unit
) {
    val agyOn = com.example.gemini.domain.model.AgentKind.AGY in enabledAgents
    val claudeOn = com.example.gemini.domain.model.AgentKind.CLAUDE in enabledAgents
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsGroupHeader(stringResource(R.string.settings_agents), MaterialTheme.colorScheme.primary, stringResource(R.string.settings_ui_which_agents_run_and_the_local_server_th))
        SettingsCategoryCard(
            icon = Icons.Outlined.SmartToy,
            iconTint = MaterialTheme.colorScheme.primary,
            title = stringResource(R.string.settings_agents),
            subtitle = when {
                agyOn && claudeOn -> stringResource(R.string.settings_ui_antigravity_and_claude_code)
                claudeOn -> stringResource(R.string.settings_ui_claude_code_only)
                else -> stringResource(R.string.settings_ui_antigravity_only)
            },
            badgeText = stringResource(R.string.settings_ui_enabledagents_size_on, enabledAgents.size),
            badgeColor = MaterialTheme.colorScheme.primary,
            cardBg = cardBg,
            cardBorder = cardBorder,
            onClick = { onNavigate(SettingsSection.AGENTS) }
        )
        // Section 2: Servers & Network
        val (serverBadge, serverColor) = when {
            isServerOnline && isBridgeOnline -> stringResource(R.string.settings_ui_online) to QuotaGreen
            isServerOnline || isBridgeOnline -> stringResource(R.string.settings_ui_partial) to Color(0xFFFFA000)
            else -> stringResource(R.string.settings_ui_offline) to Color.Red
        }
        SettingsCategoryCard(
            icon = Icons.Outlined.Dns,
            iconTint = serverColor,
            title = stringResource(R.string.settings_servers_connectivity),
            subtitle = stringResource(R.string.settings_ui_agy_hub_ide_bridge_endpoints),
            badgeText = serverBadge,
            badgeColor = serverColor,
            cardBg = cardBg,
            cardBorder = cardBorder,
            onClick = { onNavigate(SettingsSection.SERVERS) }
        )

        SettingsCategoryCard(
            icon = Icons.Outlined.Extension,
            iconTint = MaterialTheme.colorScheme.primary,
            title = stringResource(R.string.settings_webview_extensions),
            subtitle = stringResource(R.string.settings_ui_install_zip_update_rename_enable_or_disa),
            cardBg = cardBg,
            cardBorder = cardBorder,
            onClick = { onNavigate(SettingsSection.WEBVIEW_EXTENSIONS) }
        )

        if (agyOn) {
        SettingsGroupHeader("Antigravity", GeminiBlue, stringResource(R.string.settings_ui_google_antigravity_agent_agy_hub))
        // Section 3: MCP Servers & Tools
        val activeMcpCount = mcpServers.count { it.isEnabled }
        val totalMcpCount = mcpServers.size
        val mcpBadge = when {
            totalMcpCount == 0 -> stringResource(R.string.settings_ui_0_configured)
            activeMcpCount > 0 -> stringResource(R.string.settings_ui_activemcpcount_active, activeMcpCount)
            else -> stringResource(R.string.settings_ui_disabled)
        }
        val mcpColor = when {
            activeMcpCount > 0 -> QuotaGreen
            totalMcpCount > 0 -> Color(0xFFFFA000)
            else -> ClaudeTerracotta
        }
        SettingsCategoryCard(
            icon = Icons.Outlined.Extension,
            iconTint = mcpColor,
            title = stringResource(R.string.settings_mcp_servers_tools),
            subtitle = if (totalMcpCount > 0) stringResource(R.string.settings_ui_totalmcpcount_server_s_global_mcp_config, totalMcpCount) else stringResource(R.string.settings_ui_model_context_protocol_tools_integration),
            badgeText = mcpBadge,
            badgeColor = mcpColor,
            cardBg = cardBg,
            cardBorder = cardBorder,
            onClick = { onNavigate(SettingsSection.MCP) }
        )

        // Section 3.5: Skills & Plugins
        val totalSkillsCount = allSkills.size
        val totalPluginsCount = installedPlugins.size
        val skillsBadge = stringResource(R.string.settings_ui_totalskillscount_skills_totalpluginscoun, totalSkillsCount, totalPluginsCount)
        SettingsCategoryCard(
            icon = Icons.Outlined.AutoAwesome,
            iconTint = GeminiBlue,
            title = stringResource(R.string.settings_skills_plugins),
            subtitle = stringResource(R.string.settings_ui_installed_skills_google_plugins_catalog),
            badgeText = skillsBadge,
            badgeColor = GeminiBlue,
            cardBg = cardBg,
            cardBorder = cardBorder,
            onClick = { onNavigate(SettingsSection.SKILLS_PLUGINS) }
        )

        // Section 5: Commands & Permissions
        val (policyBadge, policyColor) = when {
            commandAutoExecutionPolicy.contains("EAGER", ignoreCase = true) -> stringResource(R.string.settings_ui_auto_run) to QuotaGreen
            commandAutoExecutionPolicy.contains("AUTO", ignoreCase = true) -> stringResource(R.string.settings_ui_smart_safety) to Color(0xFF00ACC1)
            else -> stringResource(R.string.settings_ui_ask_user) to Color(0xFFF59E0B)
        }
        val sandboxSummary = if (commandSandboxEnabled) stringResource(R.string.settings_ui_sandbox_on) else stringResource(R.string.settings_ui_sandbox_off)
        SettingsCategoryCard(
            icon = Icons.Outlined.Security,
            iconTint = policyColor,
            title = stringResource(R.string.settings_commands_permissions),
            subtitle = stringResource(R.string.settings_ui_policy_policybadge_sandboxsummary, policyBadge, sandboxSummary),
            badgeText = policyBadge,
            badgeColor = policyColor,
            cardBg = cardBg,
            cardBorder = cardBorder,
            onClick = { onNavigate(SettingsSection.COMMANDS) }
        )

        }

        if (claudeGroup != null && claudeOn) {
            SettingsGroupHeader("Claude Code", ClaudeAccent, stringResource(R.string.settings_ui_anthropic_claude_code_cli))
            claudeGroup()
        }

        SettingsGroupHeader(stringResource(R.string.component_app), MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), stringResource(R.string.settings_ui_shared_by_both_agents))
        // Section 1: Appearance & Display
        val themeLabel = when (themeMode) {
            "DARK" -> stringResource(R.string.settings_ui_dark_mode)
            "LIGHT" -> stringResource(R.string.settings_ui_light_mode)
            else -> stringResource(R.string.settings_ui_auto_system)
        }
        SettingsCategoryCard(
            icon = Icons.Outlined.Palette,
            iconTint = ClaudeTerracotta,
            title = stringResource(R.string.settings_appearance_theme),
            subtitle = stringResource(R.string.settings_ui_themelabel_chatfontscale_100_toint_text, themeLabel, (chatFontScale * 100).toInt()),
            badgeText = themeLabel,
            cardBg = cardBg,
            cardBorder = cardBorder,
            onClick = { onNavigate(SettingsSection.APPEARANCE) }
        )

        // Section 4: Automation & Device Bridge
        val autoActiveCount = (if (isBrowserAutomationEnabled) 1 else 0) + (if (isTerminalAutomationEnabled) 1 else 0) + (if (isFlowAutomationEnabled) 1 else 0)
        val (autoBadge, autoColor) = when (autoActiveCount) {
            3 -> stringResource(R.string.settings_ui_full_access) to QuotaGreen
            0 -> stringResource(R.string.settings_ui_disabled) to Color(0xFFFFA000)
            else -> stringResource(R.string.settings_ui_partial) to Color(0xFF00ACC1)
        }
        val autoSummary = if (autoActiveCount == 0) {
            stringResource(R.string.settings_ui_all_automation_disabled)
        } else {
            stringResource(R.string.settings_ui_browser_if_isbrowserautomationenabled_on, if (isBrowserAutomationEnabled) stringResource(R.string.settings_state_on) else stringResource(R.string.settings_state_off), if (isTerminalAutomationEnabled) stringResource(R.string.settings_state_on) else stringResource(R.string.settings_state_off), if (isFlowAutomationEnabled) stringResource(R.string.settings_state_on) else stringResource(R.string.settings_state_off))
        }
        SettingsCategoryCard(
            icon = Icons.Outlined.SmartToy,
            iconTint = autoColor,
            title = stringResource(R.string.settings_automation_device_tools),
            subtitle = autoSummary,
            badgeText = autoBadge,
            badgeColor = autoColor,
            cardBg = cardBg,
            cardBorder = cardBorder,
            onClick = { onNavigate(SettingsSection.AUTOMATION) }
        )

        // Section 5: Terminal & Shell
        SettingsCategoryCard(
            icon = Icons.Outlined.Terminal,
            iconTint = Color(0xFF00ACC1),
            title = stringResource(R.string.settings_terminal_shell_execution),
            subtitle = if (useSshTerminal) stringResource(R.string.settings_ui_ssh_mode_termux_sshd) else stringResource(R.string.settings_ui_local_linux_environment),
            badgeText = if (useSshTerminal) "SSH" else "Local",
            cardBg = cardBg,
            cardBorder = cardBorder,
            onClick = { onNavigate(SettingsSection.TERMINAL) }
        )

        // Section 5.5: Backups & Restore (only for rootfs installed app)
        if (isLocalToolsInstalled) {
            SettingsCategoryCard(
                icon = Icons.Outlined.Backup,
                iconTint = GeminiBlue,
                title = stringResource(R.string.settings_backups_restore),
                subtitle = stringResource(R.string.settings_ui_backup_rootfs_environment_and_agy_chat_h),
                badgeText = stringResource(R.string.settings_ui_rootfs_active),
                badgeColor = GeminiBlue,
                cardBg = cardBg,
                cardBorder = cardBorder,
                onClick = { onNavigate(SettingsSection.BACKUPS) }
            )
        }

        // Section 5.6: Data Loss Protection
        SettingsCategoryCard(
            icon = Icons.Outlined.Security,
            iconTint = Color(0xFF10B981),
            title = stringResource(R.string.settings_data_loss_protection),
            subtitle = stringResource(R.string.settings_data_loss_protection_desc),
            badgeText = stringResource(R.string.settings_badge_shield),
            badgeColor = Color(0xFF10B981),
            cardBg = cardBg,
            cardBorder = cardBorder,
            onClick = { onNavigate(SettingsSection.DATA_PROTECTION) }
        )

        // Section 6: Diagnostics & Performance
        val diagBadge = if (isFloatingDiagnosticsEnabled) "HUD ON" else "HUD OFF"
        val diagColor = if (isFloatingDiagnosticsEnabled) QuotaGreen else Color.Gray
        SettingsCategoryCard(
            icon = Icons.Outlined.Speed,
            iconTint = if (isFloatingDiagnosticsEnabled) QuotaGreen else Color(0xFF00ACC1),
            title = stringResource(R.string.settings_diagnostics_performance),
            subtitle = if (isFloatingDiagnosticsEnabled) stringResource(R.string.settings_hud_enabled_subtitle) else stringResource(R.string.settings_section_live_network_connections_active_sub),
            badgeText = diagBadge,
            badgeColor = diagColor,
            cardBg = cardBg,
            cardBorder = cardBorder,
            onClick = { onNavigate(SettingsSection.DIAGNOSTICS) }
        )

        // Section 7: About & Info
        SettingsCategoryCard(
            icon = Icons.Outlined.Info,
            iconTint = Color(0xFF8B5CF6),
            title = stringResource(R.string.settings_about_info),
            subtitle = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) • ${BuildConfig.APPLICATION_ID}",
            badgeText = "v${BuildConfig.VERSION_NAME}",
            badgeColor = Color(0xFF8B5CF6),
            cardBg = cardBg,
            cardBorder = cardBorder,
            onClick = { onNavigate(SettingsSection.ABOUT) }
        )
    }
}

@Composable
private fun SettingsCategoryCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    badgeText: String? = null,
    badgeColor: Color = ClaudeTerracotta,
    cardBg: Color,
    cardBorder: BorderStroke,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = cardBorder
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (badgeText != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ==========================================
// SUB-SCREEN 1: APPEARANCE & THEME
// ==========================================
@Composable
private fun AppearanceSubScreen(
    themeMode: String,
    appLanguage: String,
    chatFontScale: Float,
    groupChatsByWorkspace: Boolean,
    isFloatingSwitcherEnabled: Boolean,
    floatingSwitcherOrientation: String,
    floatingSwitcherItems: List<String>,
    floatingSwitcherAutoCollapseSec: Int,
    cardBg: Color,
    cardBorder: BorderStroke,
    onSetThemeMode: (String) -> Unit,
    onSetAppLanguage: (String) -> Unit,
    onSetChatFontScale: (Float) -> Unit,
    onToggleGroupChatsByWorkspace: (Boolean) -> Unit,
    onToggleFloatingSwitcher: (Boolean) -> Unit,
    onSetFloatingSwitcherOrientation: (String) -> Unit,
    onSetFloatingSwitcherItems: (List<String>) -> Unit,
    onSetFloatingSwitcherAutoCollapseSec: (Int) -> Unit,
    onResetFloatingSwitcherPosition: () -> Unit,
    worldImageUri: String? = null,
    onPickWorldImage: (String) -> Unit = {},
    onClearWorldImage: () -> Unit = {},
) {
    // 选图：OpenDocument 才能拿持久化读权限（重启后世界底图仍可用）
    val pickerContext = androidx.compose.ui.platform.LocalContext.current
    val worldImagePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                pickerContext.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onPickWorldImage(uri.toString())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Theme Selector Header
        Text(
            text = stringResource(R.string.settings_color_theme),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // 3 Theme Options Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val themes = listOf(
                Triple("SYSTEM", stringResource(R.string.settings_ui_auto_system), Icons.Outlined.BrightnessAuto),
                Triple("LIGHT", stringResource(R.string.settings_theme_light), Icons.Outlined.LightMode),
                Triple("DARK", stringResource(R.string.settings_theme_dark), Icons.Outlined.DarkMode)
            )

            themes.forEach { (mode, label, icon) ->
                val isSelected = themeMode.equals(mode, ignoreCase = true)
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSetThemeMode(mode) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) ClaudeTerracotta.copy(alpha = 0.12f) else cardBg,
                    border = BorderStroke(
                        1.5.dp,
                        if (isSelected) ClaudeTerracotta else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (isSelected) ClaudeTerracotta else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = label,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) ClaudeTerracotta else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // App Language Section（P1）：复用主题卡片的选中样式，三选一
        Text(
            text = stringResource(R.string.settings_language_title),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val languages = listOf(
                Triple("system", R.string.settings_language_follow_system, Icons.Outlined.Language),
                Triple("zh-CN", R.string.settings_language_zh, Icons.Outlined.Language),
                Triple("en", R.string.settings_language_en, Icons.Outlined.Language)
            )

            languages.forEach { (tag, labelRes, icon) ->
                val isSelected = when (tag) {
                    "system" -> appLanguage == "system"
                    "zh-CN" -> appLanguage.startsWith("zh", ignoreCase = true)
                    else -> appLanguage.startsWith("en", ignoreCase = true)
                }
                val label = stringResource(labelRes)
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSetAppLanguage(tag) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) ClaudeTerracotta.copy(alpha = 0.12f) else cardBg,
                    border = BorderStroke(
                        1.5.dp,
                        if (isSelected) ClaudeTerracotta else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (isSelected) ClaudeTerracotta else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = label,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) ClaudeTerracotta else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Text(
            text = stringResource(R.string.settings_language_hint),
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        if (Build.VERSION.SDK_INT < 33) {
            Text(
                text = stringResource(R.string.settings_language_unsupported),
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        // Font Scaling Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.settings_chat_text_scaling_chatfontscale_100_toin, (chatFontScale * 100).toInt()),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.settings_scales_messages_markdown_and_code_blocks),
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            if (chatFontScale != 1.0f) {
                TextButton(
                    onClick = { onSetChatFontScale(1.0f) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(stringResource(R.string.settings_reset_1_0x), fontSize = 12.sp, color = ClaudeTerracotta)
                }
            }
        }

        // 液态玻璃的世界底图：玻璃折射的是这张图
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = cardBorder
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_world_image_title),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (worldImageUri.isNullOrBlank()) {
                            stringResource(R.string.settings_world_image_subtitle)
                        } else {
                            stringResource(R.string.settings_world_image_selected)
                        },
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                if (!worldImageUri.isNullOrBlank()) {
                    TextButton(
                        onClick = onClearWorldImage,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_world_image_clear),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                TextButton(
                    onClick = { worldImagePicker.launch(arrayOf("image/*")) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_world_image_pick),
                        fontSize = 12.sp,
                        color = ClaudeTerracotta
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = cardBorder
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("A", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.width(10.dp))
                    Slider(
                        value = chatFontScale,
                        onValueChange = onSetChatFontScale,
                        valueRange = 0.75f..1.60f,
                        steps = 16,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = ClaudeTerracotta,
                            activeTrackColor = ClaudeTerracotta
                        )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("A", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }

                Spacer(modifier = Modifier.height(8.dp))

                val presets = listOf(
                    0.85f to stringResource(R.string.settings_font_size_small),
                    1.00f to stringResource(R.string.settings_font_size_normal),
                    1.15f to stringResource(R.string.settings_font_size_medium),
                    1.30f to stringResource(R.string.settings_font_size_large),
                    1.50f to stringResource(R.string.settings_font_size_huge)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { (scale, label) ->
                        val isSelected = kotlin.math.abs(chatFontScale - scale) < 0.04f
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSetChatFontScale(scale) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) ClaudeTerracotta else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (isSelected) ClaudeTerracotta else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live Chat Message Preview Box (Directly under Chat Text Scaling)
        Text(
            text = stringResource(R.string.settings_live_preview),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = cardBg,
            border = cardBorder
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Simulated User message
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ClaudeTerracotta.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_how_does_antigem_connect_to_agy_hub),
                            fontSize = (13 * chatFontScale).sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                // Simulated Assistant reply
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = stringResource(R.string.settings_antigem_connects_via_grpc_web_to_stream),
                                fontSize = (13 * chatFontScale).sp,
                                lineHeight = (18 * chatFontScale).sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Group Chats By Workspace Switch
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = cardBg,
            border = cardBorder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = stringResource(R.string.settings_group_chats_by_workspace),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.settings_categorize_chat_history_in_drawer_under),
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Switch(
                    checked = groupChatsByWorkspace,
                    onCheckedChange = onToggleGroupChatsByWorkspace,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = ClaudeTerracotta
                    )
                )
            }
        }

        // Working animation (Claude Code style) switch
        val appearanceContext = LocalContext.current
        val appearancePrefs = remember { AuthPreferences(appearanceContext.applicationContext) }
        val workingAnimation by appearancePrefs.workingAnimationEnabled.collectAsState(initial = true)
        val appearanceScope = rememberCoroutineScope()
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = cardBg,
            border = cardBorder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = stringResource(R.string.settings_working_animation),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.settings_claude_code_style_animated_star_and_stat),
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Switch(
                    checked = workingAnimation,
                    onCheckedChange = { enabled -> appearanceScope.launch { appearancePrefs.saveWorkingAnimationEnabled(enabled) } },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = ClaudeTerracotta
                    )
                )
            }
        }

        // Date & model dividers switch
        val chatDividers by appearancePrefs.chatDividersEnabled.collectAsState(initial = true)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = cardBg,
            border = cardBorder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = stringResource(R.string.settings_date_model_dividers),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.settings_a_wavy_line_divider_in_chats_when_a_new),
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Switch(
                    checked = chatDividers,
                    onCheckedChange = { enabled -> appearanceScope.launch { appearancePrefs.saveChatDividersEnabled(enabled) } },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = ClaudeTerracotta
                    )
                )
            }
        }

        // Floating App Switcher Card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = cardBg,
            border = cardBorder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = stringResource(R.string.settings_floating_app_switcher),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.settings_a_glassy_floating_pill_across_the_whole),
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = isFloatingSwitcherEnabled,
                        onCheckedChange = onToggleFloatingSwitcher,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ClaudeTerracotta
                        )
                    )
                }

                if (isFloatingSwitcherEnabled) {
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Layout Orientation Selector
                    Text(
                        text = stringResource(R.string.settings_layout_orientation),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isVert = floatingSwitcherOrientation.equals("VERTICAL", ignoreCase = true)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSetFloatingSwitcherOrientation("VERTICAL") },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isVert) ClaudeTerracotta.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (isVert) ClaudeTerracotta else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Outlined.ExpandLess, contentDescription = null, tint = if (isVert) ClaudeTerracotta else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.settings_vertical), fontSize = 12.sp, fontWeight = if (isVert) FontWeight.Bold else FontWeight.Normal, color = if (isVert) ClaudeTerracotta else MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSetFloatingSwitcherOrientation("HORIZONTAL") },
                            shape = RoundedCornerShape(10.dp),
                            color = if (!isVert) ClaudeTerracotta.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (!isVert) ClaudeTerracotta else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = if (!isVert) ClaudeTerracotta else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.settings_horizontal), fontSize = 12.sp, fontWeight = if (!isVert) FontWeight.Bold else FontWeight.Normal, color = if (!isVert) ClaudeTerracotta else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Auto-Collapse Timeout Selector
                    Text(
                        text = stringResource(R.string.settings_auto_collapse_timeout),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.settings_automatically_collapses_into_a_small_arr),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val timeouts = listOf(0 to stringResource(R.string.settings_auto_collapse_never), 3 to "3s", 5 to "5s", 10 to "10s", 15 to "15s")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        timeouts.forEach { (sec, label) ->
                            val isSelected = floatingSwitcherAutoCollapseSec == sec
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onSetFloatingSwitcherAutoCollapseSec(sec) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) ClaudeTerracotta else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (isSelected) ClaudeTerracotta else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Customize Items Order & Visibility
                    Text(
                        text = stringResource(R.string.settings_customize_icons_order),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val allAvailable = listOf("chat", "ide", "terminal", "browser")
                    val currentItems = floatingSwitcherItems.toMutableList()
                    val displayedList = currentItems + allAvailable.filterNot { currentItems.contains(it) }

                    displayedList.forEach { itemId ->
                        val isEnabled = currentItems.contains(itemId)
                        val itemLabel = when (itemId) {
                            "chat" -> stringResource(R.string.settings_switcher_item_chat)
                            "ide" -> stringResource(R.string.settings_switcher_item_ide)
                            "terminal" -> stringResource(R.string.common_terminal)
                            "browser" -> stringResource(R.string.settings_switcher_item_browser)
                            else -> itemId
                        }
                        val itemIcon = when (itemId) {
                            "chat" -> Icons.Outlined.ChatBubbleOutline
                            "ide" -> Icons.Outlined.Code
                            "terminal" -> Icons.Outlined.Terminal
                            "browser" -> Icons.Outlined.Language
                            else -> Icons.Outlined.ChatBubbleOutline
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = isEnabled,
                                        onCheckedChange = { checked ->
                                            val updated = if (checked) {
                                                currentItems + itemId
                                            } else {
                                                if (currentItems.size > 1) currentItems.filter { it != itemId } else currentItems
                                            }
                                            onSetFloatingSwitcherItems(updated)
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = ClaudeTerracotta),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(itemIcon, contentDescription = null, tint = if (isEnabled) ClaudeTerracotta else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = itemLabel,
                                        fontSize = 12.sp,
                                        color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                    )
                                }

                                if (isEnabled) {
                                    val itemOrderIndex = currentItems.indexOf(itemId)
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        IconButton(
                                            onClick = {
                                                if (itemOrderIndex > 0) {
                                                    val copy = currentItems.toMutableList()
                                                    val temp = copy[itemOrderIndex]
                                                    copy[itemOrderIndex] = copy[itemOrderIndex - 1]
                                                    copy[itemOrderIndex - 1] = temp
                                                    onSetFloatingSwitcherItems(copy)
                                                }
                                            },
                                            enabled = itemOrderIndex > 0,
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(Icons.Outlined.ExpandLess, contentDescription = stringResource(R.string.settings_move_up), modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(
                                            onClick = {
                                                if (itemOrderIndex < currentItems.size - 1) {
                                                    val copy = currentItems.toMutableList()
                                                    val temp = copy[itemOrderIndex]
                                                    copy[itemOrderIndex] = copy[itemOrderIndex + 1]
                                                    copy[itemOrderIndex + 1] = temp
                                                    onSetFloatingSwitcherItems(copy)
                                                }
                                            },
                                            enabled = itemOrderIndex < currentItems.size - 1,
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(Icons.Outlined.ExpandMore, contentDescription = stringResource(R.string.settings_move_down), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Reset Position Button
                    OutlinedButton(
                        onClick = onResetFloatingSwitcherPosition,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ClaudeTerracotta)
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.settings_reset_screen_position_to_right_center), fontSize = 12.sp)
                    }
                }
            }
        }

        // Standalone Terminal Launcher Icon & Style Settings (PackageManager as source of truth)
        val context = LocalContext.current
        val authPrefs = remember(context) { com.example.gemini.data.preferences.AuthPreferences(context) }
        var isTerminalLauncherEnabled by remember(context) {
            mutableStateOf(com.example.gemini.data.preferences.TerminalLauncherManager.isLauncherEnabled(context))
        }
        var terminalLauncherStyle by remember(context) {
            mutableStateOf(com.example.gemini.data.preferences.TerminalLauncherManager.getLauncherStyle(context))
        }
        val coroutineScope = rememberCoroutineScope()

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = cardBg,
            border = cardBorder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = stringResource(R.string.settings_separate_terminal_app_icon),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.settings_show_a_dedicated_terminal_icon_on_your_a),
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = isTerminalLauncherEnabled,
                        onCheckedChange = { enable ->
                            isTerminalLauncherEnabled = enable
                            com.example.gemini.data.preferences.TerminalLauncherManager.applyLauncherSetting(
                                context = context,
                                enabled = enable,
                                style = terminalLauncherStyle
                            )
                            coroutineScope.launch {
                                authPrefs.setTerminalLauncherEnabled(enable)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ClaudeTerracotta
                        )
                    )
                }

                AnimatedVisibility(
                    visible = isTerminalLauncherEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                        )

                        Text(
                            text = stringResource(R.string.settings_launcher_icon_name_style),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Option 1: AntiTerm
                        val isAntiTerm = terminalLauncherStyle == com.example.gemini.data.preferences.TerminalLauncherManager.STYLE_ANTITERM
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isAntiTerm) ClaudeTerracotta.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            border = BorderStroke(
                                1.dp,
                                if (isAntiTerm) ClaudeTerracotta else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    terminalLauncherStyle = com.example.gemini.data.preferences.TerminalLauncherManager.STYLE_ANTITERM
                                    com.example.gemini.data.preferences.TerminalLauncherManager.applyLauncherSetting(
                                        context = context,
                                        enabled = true,
                                        style = com.example.gemini.data.preferences.TerminalLauncherManager.STYLE_ANTITERM
                                    )
                                    coroutineScope.launch {
                                        authPrefs.setTerminalLauncherStyle(com.example.gemini.data.preferences.TerminalLauncherManager.STYLE_ANTITERM)
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                        .padding(3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    androidx.compose.foundation.Image(
                                        painter = androidx.compose.ui.res.painterResource(id = com.example.gemini.R.drawable.splash_logo),
                                        contentDescription = "AntiTerm",
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "AntiTerm",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.settings_modern_antigem_styled_icon_name),
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }

                                RadioButton(
                                    selected = isAntiTerm,
                                    onClick = {
                                        terminalLauncherStyle = com.example.gemini.data.preferences.TerminalLauncherManager.STYLE_ANTITERM
                                        com.example.gemini.data.preferences.TerminalLauncherManager.applyLauncherSetting(
                                            context = context,
                                            enabled = true,
                                            style = com.example.gemini.data.preferences.TerminalLauncherManager.STYLE_ANTITERM
                                        )
                                        coroutineScope.launch {
                                            authPrefs.setTerminalLauncherStyle(com.example.gemini.data.preferences.TerminalLauncherManager.STYLE_ANTITERM)
                                        }
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = ClaudeTerracotta)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Option 2: Termux Classic
                        val isTermux = terminalLauncherStyle == com.example.gemini.data.preferences.TerminalLauncherManager.STYLE_TERMUX
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isTermux) ClaudeTerracotta.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            border = BorderStroke(
                                1.dp,
                                if (isTermux) ClaudeTerracotta else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    terminalLauncherStyle = com.example.gemini.data.preferences.TerminalLauncherManager.STYLE_TERMUX
                                    com.example.gemini.data.preferences.TerminalLauncherManager.applyLauncherSetting(
                                        context = context,
                                        enabled = true,
                                        style = com.example.gemini.data.preferences.TerminalLauncherManager.STYLE_TERMUX
                                    )
                                    coroutineScope.launch {
                                        authPrefs.setTerminalLauncherStyle(com.example.gemini.data.preferences.TerminalLauncherManager.STYLE_TERMUX)
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black)
                                        .padding(3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    androidx.compose.foundation.Image(
                                        painter = androidx.compose.ui.res.painterResource(id = com.example.gemini.R.drawable.ic_termux_foreground),
                                        contentDescription = "Termux",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Termux",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.settings_classic_prompt_icon_name),
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }

                                RadioButton(
                                    selected = isTermux,
                                    onClick = {
                                        terminalLauncherStyle = com.example.gemini.data.preferences.TerminalLauncherManager.STYLE_TERMUX
                                        com.example.gemini.data.preferences.TerminalLauncherManager.applyLauncherSetting(
                                            context = context,
                                            enabled = true,
                                            style = com.example.gemini.data.preferences.TerminalLauncherManager.STYLE_TERMUX
                                        )
                                        coroutineScope.launch {
                                            authPrefs.setTerminalLauncherStyle(com.example.gemini.data.preferences.TerminalLauncherManager.STYLE_TERMUX)
                                        }
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = ClaudeTerracotta)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SUB-SCREEN 2: SERVERS & CONNECTIVITY
// ==========================================
@Composable
private fun ServersSubScreen(
    currentHubUrl: String,
    currentBridgeUrl: String,
    isServerOnline: Boolean,
    isBridgeOnline: Boolean = false,
    cardBg: Color,
    cardBorder: BorderStroke,
    onSaveServerUrls: (String, String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // Helper to split "http://host:port" into host and port
    fun parseHostPort(url: String, fallbackUrl: String): Pair<String, String> {
        val target = url.ifBlank { fallbackUrl }
        val clean = target.replace("http://", "").replace("https://", "").trimEnd('/')
        val parts = clean.split(":")
        val host = parts.getOrNull(0)?.ifBlank { "127.0.0.1" } ?: "127.0.0.1"
        val port = parts.getOrNull(1) ?: ""
        return host to port
    }

    val context = LocalContext.current
    val authPrefs = remember(context) { AuthPreferences(context) }
    val initSecurityToken = remember { authPrefs.getSecurityTokenSync() }
    var securityToken by remember { mutableStateOf(initSecurityToken) }

    val (initHubHost, initHubPort) = remember { parseHostPort(currentHubUrl, AuthPreferences.DEFAULT_HUB_URL) }
    val (initBridgeHost, initBridgePort) = remember { parseHostPort(currentBridgeUrl, AuthPreferences.DEFAULT_BRIDGE_HTTP_URL) }

    val initialSharedHost = remember { initBridgeHost.ifBlank { initHubHost.ifBlank { "127.0.0.1" } } }
    var sharedHost by remember { mutableStateOf(initialSharedHost) }
    var hubPort by remember { mutableStateOf(initHubPort.ifBlank { com.example.gemini.BuildConfig.HUB_PORT.toString() }) }
    var bridgePort by remember { mutableStateOf(initBridgePort.ifBlank { com.example.gemini.BuildConfig.BRIDGE_PORT.toString() }) }

    val initialBridgeBinPath = remember { authPrefs.getAgyBridgeBinaryPathSync() }
    val initialAgyBinPath = remember { authPrefs.getAgyBinaryPathSync() }
    val initialClaudeBinPath = remember { authPrefs.getClaudeBinaryPathSync() }
    var bridgeBinPath by remember { mutableStateOf(initialBridgeBinPath) }
    var agyBinPath by remember { mutableStateOf(initialAgyBinPath) }
    var claudeBinPath by remember { mutableStateOf(initialClaudeBinPath) }

    var hubTestStatus by remember { mutableStateOf<String?>(null) }
    var isTestingHub by remember { mutableStateOf(false) }

    var bridgeTestStatus by remember { mutableStateOf<String?>(null) }
    var isTestingBridge by remember { mutableStateOf(false) }

    var saveFeedback by remember { mutableStateOf<String?>(null) }

    var showRestartDialog by remember { mutableStateOf(false) }
    var showUnsavedDialog by remember { mutableStateOf(false) }

    val hasUnsavedChanges = remember(sharedHost, hubPort, bridgePort, bridgeBinPath, agyBinPath, claudeBinPath, securityToken) {
        sharedHost.trim() != initialSharedHost.trim() ||
            hubPort.trim() != initHubPort.trim() ||
            bridgePort.trim() != initBridgePort.trim() ||
            bridgeBinPath.trim() != initialBridgeBinPath.trim() ||
            agyBinPath.trim() != initialAgyBinPath.trim() ||
            claudeBinPath.trim() != initialClaudeBinPath.trim() ||
            securityToken.trim() != initSecurityToken.trim()
    }

    androidx.activity.compose.BackHandler(enabled = hasUnsavedChanges) {
        showUnsavedDialog = true
    }

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = ClaudeTerracotta,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.settings_unsaved_server_settings),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.settings_you_have_unsaved_changes_in_server_conne),
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
            },
            confirmButton = {
                Button(
                    onClick = { showUnsavedDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ClaudeTerracotta),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.settings_keep_editing), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showUnsavedDialog = false
                        sharedHost = initialSharedHost
                        hubPort = initHubPort.ifBlank { com.example.gemini.BuildConfig.HUB_PORT.toString() }
                        bridgePort = initBridgePort.ifBlank { com.example.gemini.BuildConfig.BRIDGE_PORT.toString() }
                        bridgeBinPath = initialBridgeBinPath
                        agyBinPath = initialAgyBinPath
                        claudeBinPath = initialClaudeBinPath
                        securityToken = initSecurityToken
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.common_discard_changes), color = MaterialTheme.colorScheme.error)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.RestartAlt,
                    contentDescription = null,
                    tint = ClaudeTerracotta,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.settings_reopen_antigem),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.settings_server_settings_and_security_configurati),
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestartDialog = false
                        val pm = context.packageManager
                        val intent = pm.getLaunchIntentForPackage(context.packageName)
                        if (intent != null) {
                            val restartIntent = Intent.makeRestartActivityTask(intent.component)
                            context.startActivity(restartIntent)
                            Runtime.getRuntime().exit(0)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ClaudeTerracotta),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.settings_reopen_now), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRestartDialog = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.common_later), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Unified Server Endpoints (Shared Host & Individual Ports)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = cardBorder
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Outlined.Dns, contentDescription = null, tint = QuotaGreen, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.settings_server_connectivity_endpoints), fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(text = stringResource(R.string.settings_shared_server_host_address_with_dedicate), fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = sharedHost,
                    onValueChange = { sharedHost = it; saveFeedback = null },
                    label = { Text(stringResource(R.string.settings_server_host_ip_address_shared)) },
                    placeholder = { Text("127.0.0.1") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hubPort,
                        onValueChange = { hubPort = it; saveFeedback = null },
                        label = { Text(stringResource(R.string.settings_agy_hub_port)) },
                        placeholder = { Text(com.example.gemini.BuildConfig.HUB_PORT.toString()) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = bridgePort,
                        onValueChange = { bridgePort = it; saveFeedback = null },
                        label = { Text(stringResource(R.string.settings_ide_bridge_port)) },
                        placeholder = { Text(com.example.gemini.BuildConfig.BRIDGE_PORT.toString()) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Endpoints status & preview
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isServerOnline || hubTestStatus?.startsWith("✓") == true) QuotaGreen else Color.Red)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.settings_hub_http_sharedhost_trim_hubport_trim, sharedHost.trim(), hubPort.trim()),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                if (hubTestStatus != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = hubTestStatus ?: "",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (hubTestStatus?.startsWith("✓") == true) QuotaGreen else Color.Red
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isBridgeOnline || bridgeTestStatus?.startsWith("✓") == true) QuotaGreen else Color.Red)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.settings_bridge_http_sharedhost_trim_bridgeport_t, sharedHost.trim(), bridgePort.trim()),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                if (bridgeTestStatus != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = bridgeTestStatus ?: "",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (bridgeTestStatus?.startsWith("✓") == true) QuotaGreen else Color.Red
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            isTestingHub = true
                            hubTestStatus = "Pinging http://${sharedHost.trim()}:${hubPort.trim()}..."
                            coroutineScope.launch {
                                val ok = withContext(Dispatchers.IO) {
                                    try {
                                        val u = URL("http://${sharedHost.trim()}:${hubPort.trim()}")
                                        val conn = u.openConnection() as HttpURLConnection
                                        conn.connectTimeout = 3000
                                        conn.readTimeout = 3000
                                        val code = conn.responseCode
                                        conn.disconnect()
                                        code in 200..499
                                    } catch (e: Exception) {
                                        false
                                    }
                                }
                                isTestingHub = false
                                hubTestStatus = if (ok) context.getString(R.string.settings_hub_test_ok) else context.getString(R.string.settings_hub_test_fail, hubPort.trim())
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isTestingHub
                    ) {
                        if (isTestingHub) {
                            CircularProgressIndicator(modifier = Modifier.size(13.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(stringResource(R.string.settings_test_hub), fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            isTestingBridge = true
                            bridgeTestStatus = "Pinging http://${sharedHost.trim()}:${bridgePort.trim()}..."
                            coroutineScope.launch {
                                val base = "http://${sharedHost.trim()}:${bridgePort.trim()}"
                                val ok = com.example.gemini.data.remote.AgyBridgeService.instance.checkServerHealth(base)
                                isTestingBridge = false
                                bridgeTestStatus = if (ok) context.getString(R.string.settings_bridge_test_ok) else context.getString(R.string.settings_bridge_test_fail, bridgePort.trim())
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isTestingBridge
                    ) {
                        if (isTestingBridge) {
                            CircularProgressIndicator(modifier = Modifier.size(13.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(stringResource(R.string.settings_test_bridge), fontSize = 12.sp)
                    }
                }
            }
        }

        // Binary Executable Paths Section (Auto-Launch & --bin arguments)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = cardBorder
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Outlined.Terminal, contentDescription = null, tint = ClaudeTerracotta, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.settings_binary_executable_paths), fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(text = stringResource(R.string.settings_custom_paths_used_when_auto_starting_ide), fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = bridgeBinPath,
                    onValueChange = { bridgeBinPath = it; saveFeedback = null },
                    label = { Text(stringResource(R.string.settings_ide_bridge_binary_path)) },
                    placeholder = { Text(authPrefs.getDefaultBridgeBinaryPath()) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = agyBinPath,
                    onValueChange = { agyBinPath = it; saveFeedback = null },
                    label = { Text(stringResource(R.string.settings_agy_hub_binary_path)) },
                    placeholder = { Text(authPrefs.getDefaultAgyBinaryPath()) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = claudeBinPath,
                    onValueChange = { claudeBinPath = it; saveFeedback = null },
                    label = { Text(stringResource(R.string.settings_claude_code_binary_path)) },
                    placeholder = { Text(authPrefs.getDefaultClaudeBinaryPath()) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.settings_passed_to_the_bridge_on_launch_bin_for_a),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                if (com.example.gemini.data.local.LocalEnvironmentManager.isTermuxPackage(LocalContext.current)) {
                    Text(
                        text = stringResource(R.string.settings_claude_code_the_default_is_antigem_s_lau),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        sharedHost = "127.0.0.1"
                        hubPort = com.example.gemini.BuildConfig.HUB_PORT.toString()
                        bridgePort = com.example.gemini.BuildConfig.BRIDGE_PORT.toString()
                        bridgeBinPath = authPrefs.getDefaultBridgeBinaryPath()
                        agyBinPath = authPrefs.getDefaultAgyBinaryPath()
                        claudeBinPath = authPrefs.getDefaultClaudeBinaryPath()
                        saveFeedback = "Settings reset to defaults (click Save to apply)."
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.RestartAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.settings_reset_all_to_defaults), fontSize = 12.sp)
                }
            }
        }

        // Security Header & CSRF Obfuscation Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = cardBorder
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Outlined.Security, contentDescription = null, tint = ClaudeTerracotta, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.settings_security_token_header_obfuscation), fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(text = stringResource(R.string.settings_12_char_token_framing_localhost_requests), fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = securityToken,
                    onValueChange = {
                        val filtered = it.take(AuthPreferences.TOKEN_LENGTH).filter { c -> c.isLetterOrDigit() }
                        securityToken = filtered
                        saveFeedback = null
                    },
                    label = { Text(stringResource(R.string.settings_12_character_secret_token)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    trailingIcon = {
                        Text(
                            text = stringResource(R.string.settings_securitytoken_length_12, securityToken.length),
                            fontSize = 11.sp,
                            color = if (securityToken.length == AuthPreferences.TOKEN_LENGTH) QuotaGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                val framedHeader = AuthPreferences.buildFramedHeader(securityToken)
                Text(
                    text = stringResource(R.string.settings_http_framed_header_framedheader, framedHeader),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            securityToken = AuthPreferences.generateRandomToken()
                            saveFeedback = null
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Outlined.Shuffle, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.settings_randomize), fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Framed Security Header", framedHeader)
                            clipboard?.setPrimaryClip(clip)
                            Toast.makeText(context, context.getString(R.string.settings_toast_copied_framedheader_to_clipboard, framedHeader), Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.settings_copy_header), fontSize = 12.sp)
                    }
                }
            }
        }

        if (saveFeedback != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = QuotaGreen.copy(alpha = 0.12f)
            ) {
                Text(
                    text = saveFeedback ?: "",
                    fontSize = 12.sp,
                    color = QuotaGreen,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        Button(
            onClick = {
                val cleanHost = sharedHost.trim().ifBlank { "127.0.0.1" }
                val cleanHubPort = hubPort.trim().ifBlank { com.example.gemini.BuildConfig.HUB_PORT.toString() }
                val cleanBridgePort = bridgePort.trim().ifBlank { com.example.gemini.BuildConfig.BRIDGE_PORT.toString() }
                val fullHub = "http://$cleanHost:$cleanHubPort"
                val fullBridge = "http://$cleanHost:$cleanBridgePort"
                onSaveServerUrls(fullHub, fullBridge)
                val tokenToSave = if (securityToken.length == AuthPreferences.TOKEN_LENGTH) securityToken else AuthPreferences.generateRandomToken()
                coroutineScope.launch {
                    authPrefs.saveSecurityToken(tokenToSave)
                    authPrefs.saveAgyBridgeBinaryPath(bridgeBinPath.trim().ifBlank { authPrefs.getDefaultBridgeBinaryPath() })
                    authPrefs.saveAgyBinaryPath(agyBinPath.trim().ifBlank { authPrefs.getDefaultAgyBinaryPath() })
                    authPrefs.saveClaudeBinaryPath(claudeBinPath.trim().ifBlank { authPrefs.getDefaultClaudeBinaryPath() })
                }
                saveFeedback = "✓ " + context.getString(R.string.settings_saved_server_binaries)
                showRestartDialog = true
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ClaudeTerracotta)
        ) {
            Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.settings_save_server_security_settings), fontWeight = FontWeight.Bold)
        }
    }
}

// ==========================================
// SUB-SCREEN 3: MCP SERVERS & TOOLS SETTINGS
// ==========================================
@Composable
private fun McpSubScreen(
    mcpServers: List<com.example.gemini.domain.model.McpServerState>,
    isLoading: Boolean,
    isRefreshing: Boolean = false,
    refreshingServerName: String? = null,
    errorMessage: String? = null,
    statusMessage: String? = null,
    onClearStatus: () -> Unit = {},
    cardBg: Color,
    cardBorder: BorderStroke,
    onRefreshAll: () -> Unit,
    onRefreshServer: (String) -> Unit,
    onToggleServer: (String, Boolean) -> Unit,
    onSaveServer: (com.example.gemini.domain.model.McpServerSpec, String?) -> Unit,
    onDeleteServer: (String) -> Unit,
    availableCascadePlugins: List<com.example.gemini.data.remote.dto.AvailableCascadePluginDto> = emptyList(),
    isCascadePluginsLoading: Boolean = false,
    installingCascadePluginId: String? = null,
    onSearchCascadePlugins: (String) -> Unit = {},
    onInstallCascadePlugin: (com.example.gemini.data.remote.dto.AvailableCascadePluginDto) -> Unit = {}
) {
    var showDialog by remember { mutableStateOf(false) }
    var chooseWebViewExtension by remember { mutableStateOf(false) }
    var showCascadeCatalogDialog by remember { mutableStateOf(false) }
    var serverToEdit by remember { mutableStateOf<com.example.gemini.domain.model.McpServerSpec?>(null) }
    var serverToDelete by remember { mutableStateOf<String?>(null) }
    var expandedTools by remember { mutableStateOf(setOf<String>()) }
    var expandedErrors by remember { mutableStateOf(setOf<String>()) }

    // Auto-dismiss status toast after 3.5 seconds
    LaunchedEffect(statusMessage) {
        if (!statusMessage.isNullOrBlank()) {
            delay(3500)
            onClearStatus()
        }
    }

    LaunchedEffect(Unit) {
        if (mcpServers.isEmpty()) {
            onRefreshAll()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .padding(bottom = 68.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Error banner when servers are already loaded in the list
            if (!errorMessage.isNullOrBlank() && mcpServers.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = errorMessage,
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onClearStatus,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.common_dismiss),
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

        // Banner card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = cardBorder
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ClaudeTerracotta.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Extension,
                            contentDescription = null,
                            tint = ClaudeTerracotta,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_model_context_protocol_mcp),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.settings_global_config_gemini_config_mcp_config_j),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.settings_configure_local_process_stdio_or_remote),
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )

                // Top action row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isRefreshingAll = isRefreshing && refreshingServerName == null
                    OutlinedButton(
                        onClick = onRefreshAll,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isRefreshing && !isLoading,
                        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 12.dp)
                    ) {
                        if (isRefreshingAll) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = ClaudeTerracotta
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isRefreshingAll) stringResource(R.string.settings_testing_all) else stringResource(R.string.settings_test_and_refresh), fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            serverToEdit = null
                            showDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ClaudeTerracotta),
                        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.settings_add_server), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Browse Cascade MCP Marketplace button
                OutlinedButton(
                    onClick = { showCascadeCatalogDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ClaudeTerracotta),
                    border = BorderStroke(1.dp, ClaudeTerracotta.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(vertical = 10.dp, horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Storefront,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = ClaudeTerracotta
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.settings_browse_mcp_catalog_marketplace_1_click), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ClaudeTerracotta)
                }
            }
        }

        // Quick template chips
        Text(
            text = stringResource(R.string.settings_quick_presets_templates),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val context = LocalContext.current
            val deviceId = remember(context) {
                try {
                    android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: ""
                } catch (_: Exception) { "" }
            }
            val extensionManager = remember { com.example.gemini.ui.browser.BrowserExtensionManager.instance }
            if (chooseWebViewExtension) WebViewExtensionMcpPicker(onDismiss = { chooseWebViewExtension = false }) { extension ->
                serverToEdit = com.example.gemini.domain.model.McpServerSpec(
                    serverName = extensionManager.mcpServerName(extension.id),
                    serverUrl = extensionManager.mcpUrl(extension.id),
                    headers = if (deviceId.isNotBlank()) mapOf("X-Device-Id" to deviceId) else emptyMap()
                )
                chooseWebViewExtension = false
                showDialog = true
            }
            val presets = listOf(
                Triple("WebView Extension Developer", "SSE", com.example.gemini.domain.model.McpServerSpec(
                    serverName = "webview_extension_developer_mcp",
                    serverUrl = "http://127.0.0.1:${com.example.gemini.data.remote.AndroidLocalBridgeServer.DEFAULT_PORT}/extension_developer",
                    headers = if (deviceId.isNotBlank()) mapOf("X-Device-Id" to deviceId) else emptyMap()
                )),
                Triple("Browser Automation", "SSE", com.example.gemini.domain.model.McpServerSpec(
                    serverName = "browser_mcp",
                    serverUrl = "http://127.0.0.1:${com.example.gemini.data.remote.AndroidLocalBridgeServer.DEFAULT_PORT}/browser",
                    headers = if (deviceId.isNotBlank()) mapOf("X-Device-Id" to deviceId) else emptyMap()
                )),
                Triple("Terminal Automation", "SSE", com.example.gemini.domain.model.McpServerSpec(
                    serverName = "terminal_mcp",
                    serverUrl = "http://127.0.0.1:${com.example.gemini.data.remote.AndroidLocalBridgeServer.DEFAULT_PORT}/terminal",
                    headers = if (deviceId.isNotBlank()) mapOf("X-Device-Id" to deviceId) else emptyMap()
                )),
                Triple("Stitch", "SSE", com.example.gemini.domain.model.McpServerSpec(
                    serverName = "stitch",
                    serverUrl = "https://stitch.googleapis.com/mcp",
                    headers = mapOf("X-Goog-Api-Key" to "")
                )),
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { chooseWebViewExtension = true }
            ) { Text(stringResource(R.string.common_webview_extension_mcp), modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), fontSize = 11.5.sp, fontWeight = FontWeight.Medium) }
            presets.forEach { (pName, typeBadge, spec) ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            serverToEdit = spec
                            showDialog = true
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_pname, pName),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = if (typeBadge == "SSE") Color(0xFF1976D2).copy(alpha = 0.15f) else Color(0xFF7B1FA2).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = typeBadge,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (typeBadge == "SSE") Color(0xFF1976D2) else Color(0xFF7B1FA2),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }

        // Server list header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.settings_configured_servers_mcpservers_size, mcpServers.size),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (mcpServers.isNotEmpty()) {
                val totalTools = mcpServers.sumOf { it.tools.size }
                Text(
                    text = stringResource(R.string.settings_totaltools_tools_available, totalTools),
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        // Empty / Error / Loading state
        if (mcpServers.isEmpty()) {
            if (isLoading) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = cardBorder
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.5.dp,
                            color = ClaudeTerracotta
                        )
                        Text(
                            text = stringResource(R.string.settings_loading_mcp_servers),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else if (!errorMessage.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(42.dp)
                        )
                        Text(
                            text = stringResource(R.string.settings_unable_to_load_mcp_servers),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = errorMessage,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = stringResource(R.string.settings_could_not_retrieve_mcp_server_states_ens),
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = onRefreshAll,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ClaudeTerracotta),
                            border = BorderStroke(1.dp, ClaudeTerracotta)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.settings_retry_connection), fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = cardBorder
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Extension,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = stringResource(R.string.settings_no_mcp_servers_configured),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.settings_add_an_mcp_server_to_connect_sqlite_file),
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Server Cards
        mcpServers.forEach { server ->
            val isExpanded = expandedTools.contains(server.name)
            val isErrorExpanded = expandedErrors.contains(server.name)
            val isSse = server.spec?.serverUrl?.isNotBlank() == true

            val statusColor = when {
                !server.isEnabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                server.status.contains("RUNNING", ignoreCase = true) || server.status.contains("OK", ignoreCase = true) || server.tools.isNotEmpty() -> QuotaGreen
                server.status.contains("ERROR", ignoreCase = true) || !server.error.isNullOrBlank() -> Color.Red
                else -> Color(0xFFFFA000)
            }
            val statusLabel = when {
                !server.isEnabled -> "Disabled"
                server.status.contains("RUNNING", ignoreCase = true) || server.status.contains("OK", ignoreCase = true) || server.tools.isNotEmpty() -> "Ready"
                server.status.contains("ERROR", ignoreCase = true) || !server.error.isNullOrBlank() -> "Error"
                else -> server.status.removePrefix("MCP_SERVER_STATUS_").lowercase().replaceFirstChar { it.uppercase() }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = cardBorder
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Title and toggle row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSse) Color(0xFF1976D2).copy(alpha = 0.12f) else Color(0xFF7B1FA2).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Extension,
                                contentDescription = null,
                                tint = if (isSse) Color(0xFF1976D2) else Color(0xFF7B1FA2),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = server.name,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Transport badge
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isSse) Color(0xFF1976D2).copy(alpha = 0.15f) else Color(0xFF7B1FA2).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (isSse) "SSE" else "Stdio",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSse) Color(0xFF1976D2) else Color(0xFF7B1FA2),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }

                                // Status badge
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = statusColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = statusLabel,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        Switch(
                            checked = server.isEnabled,
                            onCheckedChange = { onToggleServer(server.name, it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = ClaudeTerracotta, checkedTrackColor = ClaudeTerracotta.copy(alpha = 0.4f))
                        )
                    }

                    // Command or URL info
                    val cmdText = if (isSse) {
                        server.spec?.serverUrl ?: ""
                    } else {
                        val baseCmd = server.spec?.command ?: ""
                        val argsJoined = server.spec?.args?.joinToString(" ") ?: ""
                        "$baseCmd $argsJoined".trim()
                    }
                    if (cmdText.isNotBlank()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ) {
                            Text(
                                text = if (isSse) stringResource(R.string.settings_mcp_command_url, cmdText) else stringResource(R.string.settings_mcp_command_raw, cmdText),
                                fontSize = 11.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    if (isSse && !server.spec?.headers.isNullOrEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF1976D2).copy(alpha = 0.08f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = Color(0xFF1976D2)
                                )
                                Text(
                                    text = stringResource(R.string.settings_headers_server_spec_headers_keys_jointos_2, server.spec!!.headers.keys.joinToString(", ")),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF1976D2)
                                )
                            }
                        }
                    } else if (!isSse && !server.spec?.cwd.isNullOrBlank()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = stringResource(R.string.settings_cwd_server_spec_cwd, server.spec!!.cwd),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                )
                            }
                        }
                    }

                    // Error banner
                    if (!server.error.isNullOrBlank()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    expandedErrors = if (isErrorExpanded) expandedErrors - server.name else expandedErrors + server.name
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Red.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(14.dp))
                                        Text(stringResource(R.string.settings_connection_issue), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                                    }
                                    Icon(
                                        imageVector = if (isErrorExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = Color.Red,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = server.error,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Red,
                                    maxLines = if (isErrorExpanded) 10 else 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    // Discovered tools accordion
                    if (server.tools.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    expandedTools = if (isExpanded) expandedTools - server.name else expandedTools + server.name
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(imageVector = Icons.Default.Build, contentDescription = null, tint = ClaudeTerracotta, modifier = Modifier.size(14.dp))
                                        Text(
                                            text = stringResource(R.string.settings_server_tools_size_discovered_tool_if_ser_2, server.tools.size, if (server.tools.size > 1) "s" else ""),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                if (isExpanded) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                    server.tools.forEach { tool ->
                                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                            Text(
                                                text = tool.name,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = ClaudeTerracotta
                                            )
                                            if (tool.description.isNotBlank()) {
                                                Text(
                                                    text = tool.description,
                                                    fontSize = 11.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Card Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isThisRefreshing = isRefreshing && (refreshingServerName == server.name)
                        TextButton(
                            onClick = { onRefreshServer(server.name) },
                            enabled = !isRefreshing && !isLoading,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            if (isThisRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 1.5.dp,
                                    color = ClaudeTerracotta
                                )
                            } else {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isThisRefreshing) "Testing..." else "Refresh", fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        TextButton(
                            onClick = {
                                serverToEdit = server.spec ?: com.example.gemini.domain.model.McpServerSpec(serverName = server.name)
                                showDialog = true
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.common_edit), fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        TextButton(
                            onClick = { serverToDelete = server.name },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Red),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.common_remove), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Floating Success Toast (Zero Layout Shift)
    AnimatedVisibility(
        visible = !statusMessage.isNullOrBlank(),
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, QuotaGreen.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = QuotaGreen,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = statusMessage.orEmpty(),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                IconButton(
                    onClick = onClearStatus,
                    modifier = Modifier.size(22.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.common_dismiss),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

    // Delete confirmation dialog
    if (serverToDelete != null) {
        val targetName = serverToDelete ?: ""
        AlertDialog(
            onDismissRequest = { serverToDelete = null },
            title = { Text(stringResource(R.string.settings_remove_mcp_server)) },
            text = {
                Text(stringResource(R.string.settings_are_you_sure_you_want_to_remove_targetna, targetName))
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteServer(targetName)
                        serverToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text(stringResource(R.string.common_remove), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { serverToDelete = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    // Add / Edit Dialog
    if (showDialog) {
        AddEditMcpServerDialog(
            initialSpec = serverToEdit,
            onDismiss = {
                showDialog = false
                serverToEdit = null
            },
            onSave = { spec, rawJson ->
                onSaveServer(spec, rawJson)
                showDialog = false
                serverToEdit = null
            }
        )
    }

    // Cascade Marketplace Dialog
    if (showCascadeCatalogDialog) {
        CascadeMcpCatalogDialog(
            availablePlugins = availableCascadePlugins,
            isLoading = isCascadePluginsLoading,
            installingPluginId = installingCascadePluginId,
            existingServers = mcpServers,
            onSearch = onSearchCascadePlugins,
            onInstallPlugin = onInstallCascadePlugin,
            onDismiss = { showCascadeCatalogDialog = false }
        )
    }
}

private enum class McpEditMode {
    STDIO,
    SSE,
    RAW
}

@Composable
private fun AddEditMcpServerDialog(
    initialSpec: com.example.gemini.domain.model.McpServerSpec?,
    onDismiss: () -> Unit,
    onSave: (com.example.gemini.domain.model.McpServerSpec, String?) -> Unit
) {
    var mode by remember {
        mutableStateOf(
            if (initialSpec?.serverUrl?.isNotBlank() == true) McpEditMode.SSE
            else McpEditMode.STDIO
        )
    }
    var name by remember { mutableStateOf(initialSpec?.serverName ?: "") }
    var command by remember { mutableStateOf(initialSpec?.command ?: "") }
    var argsText by remember { mutableStateOf(initialSpec?.args?.joinToString(" ") ?: "") }
    var cwdText by remember { mutableStateOf(initialSpec?.cwd ?: "") }
    var serverUrl by remember { mutableStateOf(initialSpec?.serverUrl ?: "") }
    var headersText by remember { mutableStateOf(initialSpec?.headers?.map { "${it.key}: ${it.value}" }?.joinToString("\n") ?: "") }
    var envText by remember { mutableStateOf(initialSpec?.env?.map { "${it.key}=${it.value}" }?.joinToString("\n") ?: "") }
    var validationError by remember { mutableStateOf<String?>(null) }

    fun buildJsonFromFields(): String {
        val obj = org.json.JSONObject()
        if (mode == McpEditMode.SSE || serverUrl.isNotBlank()) {
            obj.put("serverUrl", serverUrl.trim())
            val headersMap = mutableMapOf<String, String>()
            headersText.lines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotBlank()) {
                    val delimiter = if (trimmed.contains(':')) ':' else if (trimmed.contains('=')) '=' else null
                    if (delimiter != null) {
                        val parts = trimmed.split(delimiter, limit = 2)
                        val k = parts[0].trim()
                        val v = parts[1].trim()
                        if (k.isNotBlank()) headersMap[k] = v
                    }
                }
            }
            if (headersMap.isNotEmpty()) {
                val hObj = org.json.JSONObject()
                headersMap.forEach { (k, v) -> hObj.put(k, v) }
                obj.put("headers", hObj)
            }
        } else {
            obj.put("command", command.trim())
            val argsList = mutableListOf<String>()
            val regex = """[^\s"']+|"([^"]*)"|'([^']*)'""".toRegex()
            regex.findAll(argsText.trim()).forEach { m ->
                val arg = m.groups[1]?.value ?: m.groups[2]?.value ?: m.value
                if (arg.isNotBlank()) argsList.add(arg)
            }
            if (argsList.isNotEmpty()) {
                val arr = org.json.JSONArray()
                argsList.forEach { arr.put(it) }
                obj.put("args", arr)
            }
            if (cwdText.trim().isNotBlank()) {
                obj.put("cwd", cwdText.trim())
            }
            val envMap = mutableMapOf<String, String>()
            envText.lines().forEach { line ->
                val parts = line.split("=", limit = 2)
                if (parts.size == 2 && parts[0].trim().isNotBlank()) {
                    envMap[parts[0].trim()] = parts[1].trim()
                }
            }
            if (envMap.isNotEmpty()) {
                val envObj = org.json.JSONObject()
                envMap.forEach { (k, v) -> envObj.put(k, v) }
                obj.put("env", envObj)
            }
        }
        if (initialSpec?.disabled == true) {
            obj.put("disabled", true)
        }
        return obj.toString(2)
    }

    fun parseFieldsFromJson(rawJson: String) {
        try {
            val obj = org.json.JSONObject(rawJson)
            val sUrl = obj.optString("serverUrl", "")
            if (sUrl.isNotBlank()) {
                serverUrl = sUrl
                val hObj = obj.optJSONObject("headers")
                if (hObj != null) {
                    val lines = mutableListOf<String>()
                    val keys = hObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        lines.add("$k: ${hObj.optString(k)}")
                    }
                    headersText = lines.joinToString("\n")
                }
            } else {
                val cmd = obj.optString("command", "")
                if (cmd.isNotBlank()) command = cmd
                val argsArr = obj.optJSONArray("args")
                if (argsArr != null) {
                    val list = mutableListOf<String>()
                    for (i in 0 until argsArr.length()) {
                        list.add(argsArr.optString(i))
                    }
                    argsText = list.joinToString(" ")
                }
                val cwd = obj.optString("cwd", "")
                if (cwd.isNotBlank()) cwdText = cwd
                val envObj = obj.optJSONObject("env")
                if (envObj != null) {
                    val lines = mutableListOf<String>()
                    val keys = envObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        lines.add("$k=${envObj.optString(k)}")
                    }
                    envText = lines.joinToString("\n")
                }
            }
        } catch (_: Exception) {}
    }

    var rawJsonText by remember {
        mutableStateOf(
            if (initialSpec != null) {
                val obj = org.json.JSONObject()
                if (initialSpec.serverUrl.isNotBlank()) {
                    obj.put("serverUrl", initialSpec.serverUrl)
                    if (initialSpec.headers.isNotEmpty()) {
                        val h = org.json.JSONObject()
                        initialSpec.headers.forEach { (k, v) -> h.put(k, v) }
                        obj.put("headers", h)
                    }
                } else {
                    obj.put("command", initialSpec.command)
                    if (initialSpec.args.isNotEmpty()) {
                        val arr = org.json.JSONArray()
                        initialSpec.args.forEach { arr.put(it) }
                        obj.put("args", arr)
                    }
                    if (initialSpec.cwd.isNotBlank()) obj.put("cwd", initialSpec.cwd)
                    if (initialSpec.env.isNotEmpty()) {
                        val envObj = org.json.JSONObject()
                        initialSpec.env.forEach { (k, v) -> envObj.put(k, v) }
                        obj.put("env", envObj)
                    }
                }
                if (initialSpec.disabled) obj.put("disabled", true)
                obj.toString(2)
            } else {
                """{
  "command": "npx",
  "args": [
    "-y",
    "@modelcontextprotocol/server-filesystem",
    "."
  ]
}"""
            }
        )
    }

    LaunchedEffect(initialSpec) {
        if (initialSpec != null && initialSpec.serverName.isNotBlank()) {
            try {
                val configRaw = com.example.gemini.data.daemon.IdeApiClient.getMcpConfig()
                if (!configRaw.isNullOrBlank()) {
                    val cfgJson = org.json.JSONObject(configRaw)
                    val sObj = cfgJson.optJSONObject("mcpServers")?.optJSONObject(initialSpec.serverName)
                    if (sObj != null) {
                        rawJsonText = sObj.toString(2)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialSpec != null && initialSpec.serverName.isNotBlank()) stringResource(R.string.settings_edit_mcp_server) else stringResource(R.string.settings_add_mcp_server),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Mode Selector: Stdio | SSE | Raw
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        McpEditMode.STDIO to "Stdio",
                        McpEditMode.SSE to "SSE",
                        McpEditMode.RAW to "Raw"
                    ).forEach { (m, label) ->
                        val isSelected = mode == m
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (mode != m) {
                                        if (m == McpEditMode.RAW) {
                                            rawJsonText = buildJsonFromFields()
                                        } else if (mode == McpEditMode.RAW) {
                                            parseFieldsFromJson(rawJsonText)
                                        }
                                        mode = m
                                        validationError = null
                                    }
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) ClaudeTerracotta else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, if (isSelected) ClaudeTerracotta else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        ) {
                            Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Server Name
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        validationError = null
                    },
                    label = { Text(stringResource(R.string.settings_server_name)) },
                    placeholder = { Text(stringResource(R.string.settings_e_g_stitch_filesystem_github)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (mode == McpEditMode.RAW) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.settings_server_configuration_json),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            TextButton(
                                onClick = {
                                    try {
                                        val parsed = org.json.JSONObject(rawJsonText)
                                        rawJsonText = parsed.toString(2)
                                        validationError = null
                                    } catch (e: Exception) {
                                        validationError = "Invalid JSON: ${e.message}"
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(stringResource(R.string.settings_format), fontSize = 11.sp, color = ClaudeTerracotta)
                            }
                        }

                        OutlinedTextField(
                            value = rawJsonText,
                            onValueChange = {
                                rawJsonText = it
                                validationError = null
                            },
                            placeholder = {
                                Text(stringResource(R.string.settings_n_command_npx_n_args_n))
                            },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            ),
                            minLines = 8,
                            maxLines = 14,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else if (mode == McpEditMode.STDIO) {
                    // Command
                    OutlinedTextField(
                        value = command,
                        onValueChange = {
                            command = it
                            validationError = null
                        },
                        label = { Text(stringResource(R.string.settings_command)) },
                        placeholder = { Text(stringResource(R.string.settings_e_g_npx_python3_node_uvx)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Arguments
                    OutlinedTextField(
                        value = argsText,
                        onValueChange = { argsText = it },
                        label = { Text(stringResource(R.string.settings_arguments_space_separated)) },
                        placeholder = { Text(stringResource(R.string.settings_e_g_y_modelcontextprotocol_server_filesy)) },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Working Directory
                    OutlinedTextField(
                        value = cwdText,
                        onValueChange = { cwdText = it },
                        label = { Text(stringResource(R.string.settings_working_directory_optional)) },
                        placeholder = { Text(stringResource(R.string.settings_e_g_projects_my_app)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Environment Variables
                    OutlinedTextField(
                        value = envText,
                        onValueChange = { envText = it },
                        label = { Text(stringResource(R.string.settings_environment_variables_key_value_per_line)) },
                        placeholder = { Text(stringResource(R.string.settings_api_key_xyz_ndebug_true)) },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Server URL
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = {
                            serverUrl = it
                            validationError = null
                        },
                        label = { Text(stringResource(R.string.settings_sse_endpoint_url)) },
                        placeholder = { Text(stringResource(R.string.settings_e_g_https_stitch_googleapis_com_mcp_or_h)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // HTTP Headers
                    OutlinedTextField(
                        value = headersText,
                        onValueChange = { headersText = it },
                        label = { Text(stringResource(R.string.settings_http_headers_header_value_per_line)) },
                        placeholder = { Text(stringResource(R.string.settings_x_goog_api_key_aq_ab8rn6_nauthorization)) },
                        minLines = 3,
                        maxLines = 6,
                        supportingText = {
                            Text(
                                text = stringResource(R.string.settings_set_custom_headers_like_api_keys_or_toke),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (validationError != null) {
                    Text(
                        text = validationError ?: "",
                        fontSize = 12.sp,
                        color = Color.Red,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = name.trim()
                    if (cleanName.isBlank()) {
                        validationError = "Server name is required"
                        return@Button
                    }

                    if (mode == McpEditMode.RAW) {
                        try {
                            val sObj = org.json.JSONObject(rawJsonText)
                            val sUrl = sObj.optString("serverUrl", "").trim()
                            val sCmd = sObj.optString("command", "").trim()
                            if (sUrl.isBlank() && sCmd.isBlank()) {
                                validationError = "JSON must specify either 'command' or 'serverUrl'"
                                return@Button
                            }
                            val isDis = sObj.optBoolean("disabled", initialSpec?.disabled ?: false)
                            val spec = com.example.gemini.domain.model.McpServerSpec(
                                serverName = cleanName,
                                command = sCmd,
                                serverUrl = sUrl,
                                disabled = isDis
                            )
                            onSave(spec, rawJsonText.trim())
                        } catch (e: Exception) {
                            validationError = "Invalid JSON: ${e.message}"
                            return@Button
                        }
                    } else if (mode == McpEditMode.STDIO) {
                        val cleanCmd = command.trim()
                        if (cleanCmd.isBlank()) {
                            validationError = "Command is required"
                            return@Button
                        }
                        // Parse arguments, supporting quoted strings
                        val argsList = mutableListOf<String>()
                        val regex = """[^\s"']+|"([^"]*)"|'([^']*)'""".toRegex()
                        regex.findAll(argsText.trim()).forEach { m ->
                            val arg = m.groups[1]?.value ?: m.groups[2]?.value ?: m.value
                            if (arg.isNotBlank()) argsList.add(arg)
                        }

                        val envMap = mutableMapOf<String, String>()
                        envText.lines().forEach { line ->
                            val parts = line.split("=", limit = 2)
                            if (parts.size == 2 && parts[0].trim().isNotBlank()) {
                                envMap[parts[0].trim()] = parts[1].trim()
                            }
                        }

                        val spec = com.example.gemini.domain.model.McpServerSpec(
                            serverName = cleanName,
                            command = cleanCmd,
                            args = argsList,
                            env = envMap,
                            cwd = cwdText.trim(),
                            disabled = initialSpec?.disabled ?: false
                        )
                        onSave(spec, null)
                    } else {
                        val cleanUrl = serverUrl.trim()
                        if (cleanUrl.isBlank()) {
                            validationError = "Server URL is required"
                            return@Button
                        }

                        val headersMap = mutableMapOf<String, String>()
                        headersText.lines().forEach { line ->
                            val trimmed = line.trim()
                            if (trimmed.isNotBlank()) {
                                val delimiter = if (trimmed.contains(':')) ':' else if (trimmed.contains('=')) '=' else null
                                if (delimiter != null) {
                                    val parts = trimmed.split(delimiter, limit = 2)
                                    val k = parts[0].trim()
                                    val v = parts[1].trim()
                                    if (k.isNotBlank()) {
                                        headersMap[k] = v
                                    }
                                }
                            }
                        }

                        val spec = com.example.gemini.domain.model.McpServerSpec(
                            serverName = cleanName,
                            serverUrl = cleanUrl,
                            headers = headersMap,
                            disabled = initialSpec?.disabled ?: false
                        )
                        onSave(spec, null)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ClaudeTerracotta)
            ) {
                Text(stringResource(R.string.common_save), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

// ==========================================
// SUB-SCREEN 4: TERMINAL & SHELL SETTINGS
// ==========================================
@Composable
private fun TerminalSubScreen(
    useSshTerminal: Boolean,
    sshHost: String,
    sshPort: Int,
    sshUser: String,
    sshPass: String,
    isLocalToolsEnabled: Boolean,
    isLocalToolsInstalled: Boolean,
    terminalFontSize: Int,
    terminalCursorStyle: String,
    terminalBufferSize: Int,
    terminalTheme: String,
    cardBg: Color,
    cardBorder: BorderStroke,
    onToggleUseSshTerminal: (Boolean) -> Unit,
    onSaveSshSettings: (String, Int, String, String) -> Unit,
    onToggleLocalTools: (Boolean) -> Unit,
    onInstallLocalTools: () -> Unit,
    onResetLocalTools: () -> Unit,
    onSaveTerminalPreferences: (Int, String, Int, String) -> Unit,
    onOpenLocalTerminal: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showResetWarningDialog by remember { mutableStateOf(false) }
    var showResetPasswordDialog by remember { mutableStateOf(false) }
    var resetPasswordInput by remember { mutableStateOf("") }
    var showShutdownCountdownDialog by remember { mutableStateOf(false) }
    var countdownSeconds by remember { mutableIntStateOf(5) }

    var hostState by remember { mutableStateOf(sshHost) }
    var portState by remember { mutableStateOf(sshPort.toString()) }
    var userState by remember { mutableStateOf(sshUser) }
    var passState by remember { mutableStateOf(sshPass) }
    var isTestingSsh by remember { mutableStateOf(false) }
    var sshTestStatus by remember { mutableStateOf<String?>(null) }
    var sshSaveFeedback by remember { mutableStateOf<String?>(null) }

    var fontSizeState by remember { mutableIntStateOf(terminalFontSize) }
    var cursorStyleState by remember { mutableStateOf(terminalCursorStyle) }
    var bufferSizeState by remember { mutableIntStateOf(terminalBufferSize) }
    var themeState by remember { mutableStateOf(terminalTheme) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mode Selector Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = cardBorder
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Terminal,
                        contentDescription = null,
                        tint = if (useSshTerminal) QuotaGreen else ClaudeTerracotta,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = stringResource(R.string.settings_use_terminal_via_ssh), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (useSshTerminal) QuotaGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = if (useSshTerminal) stringResource(R.string.settings_ssh_mode) else stringResource(R.string.settings_local_mode),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (useSshTerminal) QuotaGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = if (useSshTerminal) stringResource(R.string.settings_ssh_mode_desc) else stringResource(R.string.settings_local_mode_desc),
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = useSshTerminal,
                        onCheckedChange = { onToggleUseSshTerminal(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = QuotaGreen
                        )
                    )
                }

                if (useSshTerminal) {
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(thickness = 0.6.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = hostState,
                            onValueChange = {
                                hostState = it
                                sshTestStatus = null
                                sshSaveFeedback = null
                            },
                            label = { Text(stringResource(R.string.settings_ssh_host)) },
                            modifier = Modifier.weight(2f),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = portState,
                            onValueChange = {
                                portState = it
                                sshTestStatus = null
                                sshSaveFeedback = null
                            },
                            label = { Text(stringResource(R.string.settings_port)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = userState,
                            onValueChange = {
                                userState = it
                                sshTestStatus = null
                                sshSaveFeedback = null
                            },
                            label = { Text(stringResource(R.string.settings_user)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = passState,
                            onValueChange = {
                                passState = it
                                sshTestStatus = null
                                sshSaveFeedback = null
                            },
                            label = { Text(stringResource(R.string.settings_password)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    if (sshTestStatus != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = sshTestStatus ?: "",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (sshTestStatus?.startsWith("✓") == true) QuotaGreen else Color.Red
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            isTestingSsh = true
                            sshTestStatus = "Testing SSH connection to $hostState..."
                            coroutineScope.launch {
                                val res = com.example.gemini.data.ssh.TermuxSshManager.testConnection(
                                    host = hostState.trim(),
                                    port = portState.toIntOrNull() ?: sshPort,
                                    user = userState.trim(),
                                    pass = passState
                                )
                                isTestingSsh = false
                                sshTestStatus = if (res.isSuccess) context.getString(R.string.settings_ssh_test_ok) else context.getString(R.string.settings_ssh_test_fail, res.exceptionOrNull()?.localizedMessage ?: context.getString(R.string.settings_connection_refused))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isTestingSsh
                    ) {
                        if (isTestingSsh) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(stringResource(R.string.settings_test_ssh_connection), fontSize = 12.sp)
                    }

                    if (sshSaveFeedback != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = QuotaGreen.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = sshSaveFeedback ?: "",
                                fontSize = 12.sp,
                                color = QuotaGreen,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val p = portState.toIntOrNull() ?: sshPort
                            onSaveSshSettings(hostState.trim(), p, userState.trim(), passState)
                            sshSaveFeedback = "✓ " + context.getString(R.string.settings_saved_ssh)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ClaudeTerracotta)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_save_ssh_settings), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                } else {
                    // Local tools mode details
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(thickness = 0.6.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(stringResource(R.string.settings_embedded_linux_rootfs), fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(text = if (isLocalToolsInstalled) stringResource(R.string.settings_installed_ready) else stringResource(R.string.settings_not_installed_download), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isLocalToolsInstalled) QuotaGreen.copy(alpha = 0.15f) else ClaudeTerracotta.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (isLocalToolsInstalled) stringResource(R.string.settings_installed_check) else stringResource(R.string.settings_not_installed),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isLocalToolsInstalled) QuotaGreen else ClaudeTerracotta,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        if (isLocalToolsInstalled) {
                            OutlinedButton(
                                onClick = { showResetWarningDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.4f))
                            ) {
                                Icon(imageVector = Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.Red)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.settings_delete_all_factory_reset_2), fontSize = 12.5.sp, color = Color.Red, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Button(
                                onClick = onInstallLocalTools,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ClaudeTerracotta)
                            ) {
                                Text(stringResource(R.string.settings_download_install_local_tools), fontSize = 12.5.sp)
                            }
                        }
                    }
                }
            }
        }

        // Terminal Appearance Customization Card
        Text(
            text = stringResource(R.string.settings_terminal_customization),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = cardBorder
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Font Size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.settings_font_size), fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(stringResource(R.string.settings_fontsizestate_pt, fontSizeState), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ClaudeTerracotta)
                }
                Slider(
                    value = fontSizeState.toFloat(),
                    onValueChange = {
                        fontSizeState = it.toInt()
                        onSaveTerminalPreferences(fontSizeState, cursorStyleState, bufferSizeState, themeState)
                    },
                    valueRange = 10f..24f,
                    steps = 14,
                    colors = SliderDefaults.colors(thumbColor = ClaudeTerracotta, activeTrackColor = ClaudeTerracotta)
                )

                // Cursor Style
                Text(stringResource(R.string.settings_cursor_style), fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val cursorOptions = listOf("BLOCK" to "Block █", "UNDERLINE" to "Underline  ", "BAR" to "Bar |")
                    cursorOptions.forEach { (style, label) ->
                        val isSel = cursorStyleState == style
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    cursorStyleState = style
                                    onSaveTerminalPreferences(fontSizeState, style, bufferSizeState, themeState)
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) ClaudeTerracotta else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (isSel) ClaudeTerracotta else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                        ) {
                            Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(text = label, fontSize = 11.5.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium, color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                // Buffer Size
                Text(stringResource(R.string.settings_scrollback_buffer), fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val buffers = listOf(
                        1000 to "1k",
                        5000 to "5k",
                        10000 to "10k",
                        20000 to "20k",
                        50000 to "50k",
                        100000 to "Unlimited"
                    )
                    buffers.forEach { (buf, label) ->
                        val isSel = bufferSizeState == buf || (buf == 100000 && bufferSizeState >= 100000)
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    bufferSizeState = buf
                                    onSaveTerminalPreferences(fontSizeState, cursorStyleState, buf, themeState)
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) ClaudeTerracotta else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (isSel) ClaudeTerracotta else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(text = label, fontSize = 11.5.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium, color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }

        // Action Button: Open Terminal
        Button(
            onClick = onOpenLocalTerminal,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ClaudeTerracotta)
        ) {
            Icon(imageVector = Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.settings_launch_terminal_now), fontWeight = FontWeight.Bold)
        }
    }

    if (showResetWarningDialog) {
        AlertDialog(
            onDismissRequest = { showResetWarningDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.settings_delete_all_factory_reset),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.settings_warning_this_will_perform_an_android_os),
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetWarningDialog = false
                        resetPasswordInput = ""
                        showResetPasswordDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.settings_yes_continue), fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showResetWarningDialog = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showResetPasswordDialog) {
        val isMatch = resetPasswordInput.trim() == "iknowwhatiamdoing"
        AlertDialog(
            onDismissRequest = { showResetPasswordDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.settings_confirm_full_app_wipe),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.settings_to_confirm_and_permanently_wipe_all_app_2),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_iknowwhatiamdoing),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = ClaudeTerracotta,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    OutlinedTextField(
                        value = resetPasswordInput,
                        onValueChange = { resetPasswordInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        isError = resetPasswordInput.isNotEmpty() && !isMatch
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isMatch) {
                            showResetPasswordDialog = false
                            countdownSeconds = 5
                            showShutdownCountdownDialog = true
                        }
                    },
                    enabled = isMatch,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red,
                        disabledContainerColor = Color.Red.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.settings_delete_all_data), fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showResetPasswordDialog = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showShutdownCountdownDialog) {
        LaunchedEffect(Unit) {
            for (i in 5 downTo 1) {
                countdownSeconds = i
                delay(1000)
            }
            countdownSeconds = 0
            delay(300)
            LocalEnvironmentManager.clearAppDataAndReset(context)
        }

        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 10.dp,
                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.Red.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.settings_countdownseconds_s, countdownSeconds),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Red,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = stringResource(R.string.settings_factory_reset_in_progress),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = stringResource(R.string.settings_antigem_is_wiping_all_local_app_data_roo, countdownSeconds),
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 19.sp
                    )

                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp,
                        color = Color.Red
                    )
                }
            }
        }
    }
}

// ==========================================
// SUB-SCREEN: BACKUPS & RESTORE
// ==========================================

data class ScriptExecutionInfo(
    val title: String,
    val scriptName: String,
    val args: List<String> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackupsSubScreen(
    isServerOnline: Boolean,
    isBridgeOnline: Boolean,
    cardBg: Color,
    cardBorder: BorderStroke,
    onStopServer: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val systemConnectionState by com.example.gemini.data.remote.AgyBridgeService.instance.systemConnectionState.collectAsState()
    val isAnyServerRunning = systemConnectionState !is com.example.gemini.data.remote.SystemConnectionState.Offline

    var activeScriptExecution by remember { mutableStateOf<ScriptExecutionInfo?>(null) }
    var showServerRunningAlert by remember { mutableStateOf<String?>(null) }

    var showBackupChatsPasswordDialog by remember { mutableStateOf(false) }
    var backupChatsPassword by remember { mutableStateOf("") }
    var showBackupPasswordText by remember { mutableStateOf(false) }

    var showRestoreChatsDialog by remember { mutableStateOf(false) }
    var restoreFilePath by remember { mutableStateOf("") }
    var restorePassword by remember { mutableStateOf("") }
    var showRestorePasswordText by remember { mutableStateOf(false) }

    var showProjectsBackupRestoreDialog by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val destFile = File(context.cacheDir, "restore_payload_${System.currentTimeMillis()}.zip")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        restoreFilePath = destFile.absolutePath
                    }
                } catch (e: Exception) {
                    Log.e("BackupsSubScreen", "Failed copying selected backup file", e)
                }
            }
        }
    }

    val requestStoragePermission = { onGranted: () -> Unit ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    context.startActivity(intent)
                }
            } else {
                onGranted()
            }
        } else {
            val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (permission != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, context.getString(R.string.settings_toast_storage_permission_needed_to_save_to_sdc), Toast.LENGTH_SHORT).show()
            }
            onGranted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Server Online Guard / Warning Banner
        if (isAnyServerRunning) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_agy_server_is_running),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.settings_the_daemon_is_actively_accessing_databas),
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onStopServer,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.settings_stop_server_now), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Card 1: Rootfs Environment Backup
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = cardBorder
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF00ACC1).copy(alpha = 0.15f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Inventory2,
                                contentDescription = null,
                                tint = Color(0xFF00ACC1),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_rootfs_environment_backup),
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.settings_rootfs_backup_desc, "\$HOME"),
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.settings_creates_a_full_portable_backup_of_all_in),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Folder, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.settings_saves_to_sdcard_download_antigem_backups_2),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        if (isAnyServerRunning) {
                            showServerRunningAlert = "Backup Rootfs"
                        } else {
                            requestStoragePermission {
                                activeScriptExecution = ScriptExecutionInfo(
                                    title = context.getString(R.string.settings_rootfs_system_packager),
                                    scriptName = "backup_bootstrap",
                                    args = emptyList()
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00ACC1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Archive, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_run_rootfs_backup_backup_bootstrap), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Card 2: AGY Chats & Credentials Backup
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = cardBorder
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = ClaudeTerracotta.copy(alpha = 0.15f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Chat,
                                contentDescription = null,
                                tint = ClaudeTerracotta,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_agy_chats_credentials_backup),
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.settings_archive_conversations_transcripts_auth_t),
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.settings_safely_packages_your_entire_gemini_folde),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFEF4444).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                        Text(
                            text = stringResource(R.string.settings_security_warning_this_backup_includes_yo),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Folder, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.settings_saves_to_sdcard_download_antigem_backups),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        if (isAnyServerRunning) {
                            showServerRunningAlert = "Backup Chats"
                        } else {
                            showBackupChatsPasswordDialog = true
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ClaudeTerracotta),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_backup_agy_chats_backup_chats), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Card 3: Restore AGY Chats & Credentials
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = cardBorder
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = QuotaGreen.copy(alpha = 0.15f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.SettingsBackupRestore,
                                contentDescription = null,
                                tint = QuotaGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_restore_agy_chats_credentials),
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.settings_restore_previous_chats_and_login_from_a),
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.settings_restores_your_chats_conversations_index),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    onClick = {
                        if (isAnyServerRunning) {
                            showServerRunningAlert = "Restore Chats"
                        } else {
                            showRestoreChatsDialog = true
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, QuotaGreen),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = QuotaGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.SettingsBackupRestore, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_restore_agy_chats_restore_chats), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Card 4: IDE & AGY Projects Backup & Restore
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = cardBorder
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF6366F1).copy(alpha = 0.15f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Workspaces,
                                contentDescription = null,
                                tint = Color(0xFF6366F1),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_ide_agy_projects_backup_restore),
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.settings_archive_selected_projects_workspaces_res),
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.settings_export_and_backup_projects_open_in_the_i),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = { showProjectsBackupRestoreDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.FolderZip, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_manage_projects_backup_restore), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }

    // 1. Server Running Alert Dialog
    showServerRunningAlert?.let { actionName ->
        AlertDialog(
            onDismissRequest = { showServerRunningAlert = null },
            icon = { Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Color(0xFFF59E0B)) },
            title = { Text(stringResource(R.string.settings_server_must_be_stopped), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    stringResource(R.string.settings_the_agy_ide_bridge_and_hub_server_must_b, actionName),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onStopServer()
                        showServerRunningAlert = null
                        Toast.makeText(context, context.getString(R.string.settings_toast_stopping_server_please_re_run_actionname, actionName), Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                ) {
                    Text(stringResource(R.string.common_stop_server), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showServerRunningAlert = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    // 2. Backup Chats Password Dialog
    if (showBackupChatsPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showBackupChatsPasswordDialog = false },
            icon = { Icon(Icons.Outlined.CloudUpload, contentDescription = null, tint = ClaudeTerracotta) },
            title = { Text(stringResource(R.string.settings_backup_agy_chats), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.settings_optionally_enter_a_password_to_encrypt_y),
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                            Text(
                                text = stringResource(R.string.settings_do_not_share_this_file_anyone_with_it_ca),
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    OutlinedTextField(
                        value = backupChatsPassword,
                        onValueChange = { backupChatsPassword = it },
                        label = { Text(stringResource(R.string.settings_encryption_password_optional)) },
                        singleLine = true,
                        visualTransformation = if (showBackupPasswordText) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showBackupPasswordText = !showBackupPasswordText }) {
                                Icon(
                                    imageVector = if (showBackupPasswordText) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pass = backupChatsPassword.trim()
                        showBackupChatsPasswordDialog = false
                        backupChatsPassword = ""
                        requestStoragePermission {
                            activeScriptExecution = ScriptExecutionInfo(
                                title = context.getString(R.string.settings_agy_chats_credentials_backup),
                                scriptName = "backup_chats",
                                args = if (pass.isNotEmpty()) listOf("", pass) else emptyList()
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ClaudeTerracotta)
                ) {
                    Text(stringResource(R.string.settings_start_backup), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBackupChatsPasswordDialog = false
                    backupChatsPassword = ""
                }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    // 3. Restore Chats Dialog
    if (showRestoreChatsDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreChatsDialog = false },
            icon = { Icon(Icons.Outlined.SettingsBackupRestore, contentDescription = null, tint = QuotaGreen) },
            title = { Text(stringResource(R.string.settings_restore_agy_chats), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.settings_select_your_backup_zip_archive_to_restor),
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )

                    OutlinedTextField(
                        value = restoreFilePath,
                        onValueChange = { restoreFilePath = it },
                        label = { Text(stringResource(R.string.settings_backup_archive_path)) },
                        placeholder = { Text("/sdcard/Download/agy_chats_backup_....zip") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                                Icon(Icons.Outlined.FolderOpen, contentDescription = stringResource(R.string.settings_choose_file), tint = QuotaGreen)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = restorePassword,
                        onValueChange = { restorePassword = it },
                        label = { Text(stringResource(R.string.settings_archive_password_if_encrypted)) },
                        singleLine = true,
                        visualTransformation = if (showRestorePasswordText) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showRestorePasswordText = !showRestorePasswordText }) {
                                Icon(
                                    imageVector = if (showRestorePasswordText) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val path = restoreFilePath.trim()
                        val pass = restorePassword.trim()
                        showRestoreChatsDialog = false
                        restoreFilePath = ""
                        restorePassword = ""
                        activeScriptExecution = ScriptExecutionInfo(
                            title = context.getString(R.string.settings_restore_agy_chats_credentials),
                            scriptName = "restore_chats",
                            args = if (pass.isNotEmpty()) listOf(path, pass) else listOf(path)
                        )
                    },
                    enabled = restoreFilePath.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = QuotaGreen)
                ) {
                    Text(stringResource(R.string.settings_start_restore), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestoreChatsDialog = false
                    restoreFilePath = ""
                    restorePassword = ""
                }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    // 4. Live Script Execution Dialog
    activeScriptExecution?.let { executionInfo ->
        ScriptExecutionDialog(
            info = executionInfo,
            onDismiss = { activeScriptExecution = null }
        )
    }

    // 5. Projects Backup & Restore Dialog
    if (showProjectsBackupRestoreDialog) {
        ProjectsBackupRestoreDialog(
            onDismiss = { showProjectsBackupRestoreDialog = false },
            onRequestStoragePermission = requestStoragePermission,
            onExecuteScript = { executionInfo ->
                activeScriptExecution = executionInfo
            }
        )
    }
}

data class ProjectBackupManifestItem(
    val name: String,
    val originalPath: String,
    val backupSubpath: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectsBackupRestoreDialog(
    onDismiss: () -> Unit,
    onRequestStoragePermission: (() -> Unit) -> Unit,
    onExecuteScript: (ScriptExecutionInfo) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Backup, 1 = Restore
    // Projects list from TermuxDaemonManager
    val allProjects by TermuxDaemonManager.projects.collectAsState()
    val homeDir = remember { LocalEnvironmentManager.getHomeDir(context).absolutePath.trimEnd('/') }

    fun normalizeProjectPath(p: String, home: String): String {
        var clean = p.trim().removePrefix("file://").trim()
        if (clean.startsWith("~/")) {
            clean = home + clean.removePrefix("~")
        } else if (clean == "~") {
            clean = home
        }
        if (clean.startsWith("/data/user/0/")) {
            clean = "/data/data/" + clean.removePrefix("/data/user/0/")
        }
        clean = clean.replace(Regex("/+"), "/")
        return clean.trimEnd('/')
    }

    // Filter out root $HOME, root filesystem paths, and duplicates
    val validProjects = remember(allProjects, homeDir) {
        val normHome = normalizeProjectPath(homeDir, homeDir)
        allProjects.filter { proj ->
            val p = normalizeProjectPath(proj.path, normHome)
            p.isNotBlank() && p != normHome && p != "/" && p != "/data/data/com.termux/files" && p != "/data/data/com.termux"
        }.distinctBy { normalizeProjectPath(it.path, normHome) }
    }

    LaunchedEffect(Unit) {
        TermuxDaemonManager.loadProjects()
    }

    // --- Backup Tab State ---
    val selectedProjectPaths = remember { mutableStateListOf<String>() }
    var hasAutoSelected by remember { mutableStateOf(false) }
    var backupMode by remember { mutableStateOf("git") } // "git" or "full"
    var searchQuery by remember { mutableStateOf("") }

    // Auto-select valid projects by default only once on initial load
    LaunchedEffect(validProjects) {
        if (!hasAutoSelected && validProjects.isNotEmpty()) {
            selectedProjectPaths.clear()
            selectedProjectPaths.addAll(validProjects.map { it.path })
            hasAutoSelected = true
        }
    }

    val filteredProjects = remember(validProjects, searchQuery) {
        if (searchQuery.isBlank()) validProjects
        else validProjects.filter { it.name.contains(searchQuery, ignoreCase = true) || it.path.contains(searchQuery, ignoreCase = true) }
    }

    val selectedCount = remember(selectedProjectPaths.toList(), validProjects) {
        validProjects.count { selectedProjectPaths.contains(it.path) }
    }

    // Build nested tree list with path normalization
    val flattenedTree = remember(filteredProjects, homeDir) {
        val normHome = normalizeProjectPath(homeDir, homeDir)
        val normMap = filteredProjects.associateWith { normalizeProjectPath(it.path, normHome) }
        val sorted = filteredProjects.sortedBy { normMap[it]?.length ?: 0 }

        fun findParent(proj: ProjectItem): ProjectItem? {
            val childNorm = normMap[proj] ?: return null
            return sorted.filter { parent ->
                val parentNorm = normMap[parent] ?: ""
                parentNorm.isNotBlank() && parentNorm != childNorm && childNorm.startsWith("$parentNorm/")
            }.maxByOrNull { normMap[it]?.length ?: 0 }
        }

        val parentMap = mutableMapOf<String, ProjectItem?>()
        filteredProjects.forEach { proj ->
            parentMap[proj.path] = findParent(proj)
        }

        val result = mutableListOf<Triple<ProjectItem, Int, ProjectItem?>>() // item, level, parent
        fun addNodeAndChildren(proj: ProjectItem, level: Int) {
            result.add(Triple(proj, level, parentMap[proj.path]))
            val children = filteredProjects.filter { parentMap[it.path]?.path == proj.path }
            children.forEach { child ->
                addNodeAndChildren(child, level + 1)
            }
        }

        val roots = filteredProjects.filter { parentMap[it.path] == null }
        roots.forEach { root ->
            addNodeAndChildren(root, 0)
        }
        result
    }

    val effectiveBackupPaths = remember(selectedProjectPaths.toList(), validProjects, homeDir) {
        val normHome = normalizeProjectPath(homeDir, homeDir)
        val normMap = validProjects.associateWith { normalizeProjectPath(it.path, normHome) }
        val selected = validProjects.filter { selectedProjectPaths.contains(it.path) }
        selected.filter { item ->
            val itemNorm = normMap[item] ?: ""
            selected.none { other ->
                val otherNorm = normMap[other] ?: ""
                other.path != item.path && otherNorm.isNotBlank() && itemNorm.startsWith("$otherNorm/")
            }
        }.map { it.path }
    }

    // --- Restore Tab State ---
    var restoreZipPath by remember { mutableStateOf("") }
    var conflictStrategy by remember { mutableStateOf("bak") } // "bak" or "overwrite"
    var customRestoreDir by remember { mutableStateOf("") }
    var useCustomRestoreDir by remember { mutableStateOf(false) }
    var manifestProjects by remember { mutableStateOf<List<ProjectBackupManifestItem>>(emptyList()) }
    var manifestBackupMode by remember { mutableStateOf<String?>(null) }
    var isInspectingArchive by remember { mutableStateOf(false) }
    var archiveError by remember { mutableStateOf<String?>(null) }

    val inspectArchive: (String) -> Unit = { path ->
        if (path.isBlank()) {
            manifestProjects = emptyList()
            manifestBackupMode = null
            archiveError = null
        } else {
            scope.launch(Dispatchers.IO) {
                isInspectingArchive = true
                archiveError = null
                try {
                    val file = File(path)
                    if (!file.exists()) {
                        withContext(Dispatchers.Main) {
                            archiveError = "Archive file not found at: $path"
                            manifestProjects = emptyList()
                            isInspectingArchive = false
                        }
                    } else {
                        val zip = ZipFile(file)
                        val entry = zip.getEntry("antigem_project_manifest.json")
                            ?: zip.getEntry(".antigem_project_manifest.json")
                        if (entry != null) {
                            val stream = zip.getInputStream(entry)
                            val content = stream.bufferedReader().use { it.readText() }
                            val json = JSONObject(content)
                            val projectsArr = json.optJSONArray("projects") ?: JSONArray()
                            val list = mutableListOf<ProjectBackupManifestItem>()
                            for (i in 0 until projectsArr.length()) {
                                val obj = projectsArr.getJSONObject(i)
                                list.add(
                                    ProjectBackupManifestItem(
                                        name = obj.optString("name", "Project"),
                                        originalPath = obj.optString("original_path", ""),
                                        backupSubpath = obj.optString("backup_subpath", "")
                                    )
                                )
                            }
                            val mode = json.optString("backup_mode", "git")
                            zip.close()
                            withContext(Dispatchers.Main) {
                                manifestProjects = list
                                manifestBackupMode = mode
                                isInspectingArchive = false
                            }
                        } else {
                            val entries = zip.entries().asSequence().map { it.name }.toList()
                            zip.close()
                            val roots = entries.map { it.substringBefore('/') }.filter { it.isNotBlank() && !it.startsWith("__") }.distinct()
                            val list = roots.map { ProjectBackupManifestItem(name = it, originalPath = "", backupSubpath = it) }
                            withContext(Dispatchers.Main) {
                                manifestProjects = list
                                manifestBackupMode = "raw"
                                isInspectingArchive = false
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        archiveError = "Error reading archive: ${e.message}"
                        manifestProjects = emptyList()
                        isInspectingArchive = false
                    }
                }
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val destFile = File(context.cacheDir, "project_restore_payload_${System.currentTimeMillis()}.zip")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        restoreZipPath = destFile.absolutePath
                        inspectArchive(destFile.absolutePath)
                    }
                } catch (e: Exception) {
                    Log.e("ProjectsBackup", "Failed copying selected backup file", e)
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF6366F1).copy(alpha = 0.15f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Workspaces,
                                    contentDescription = null,
                                    tint = Color(0xFF6366F1),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.settings_projects_backup_restore),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.settings_backup_ide_agy_workspaces_or_restore_fro),
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_close), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Switcher
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    contentColor = Color(0xFF6366F1),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(stringResource(R.string.settings_backup_projects), fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                        icon = { Icon(Icons.Outlined.Archive, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(stringResource(R.string.settings_restore_projects), fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                        icon = { Icon(Icons.Outlined.SettingsBackupRestore, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedTab == 0) {
                    // ==========================================
                    // TAB 0: BACKUP PROJECTS
                    // ==========================================
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Backup Mode Selection Card
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = stringResource(R.string.settings_backup_format_packaging_mode),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Git-Aware (Clean)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (backupMode == "git") Color(0xFF6366F1).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, if (backupMode == "git") Color(0xFF6366F1) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { backupMode = "git" }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (backupMode == "git") Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked,
                                                contentDescription = null,
                                                tint = if (backupMode == "git") Color(0xFF6366F1) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Column {
                                                Text(stringResource(R.string.settings_git_aware_clean), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (backupMode == "git") Color(0xFF6366F1) else MaterialTheme.colorScheme.onSurface)
                                                Text(stringResource(R.string.settings_excludes_node_modules_build_caches), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }

                                    // Full Raw Archive
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (backupMode == "full") Color(0xFF6366F1).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, if (backupMode == "full") Color(0xFF6366F1) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { backupMode = "full" }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (backupMode == "full") Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked,
                                                contentDescription = null,
                                                tint = if (backupMode == "full") Color(0xFF6366F1) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Column {
                                                Text(stringResource(R.string.settings_full_raw_archive), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (backupMode == "full") Color(0xFF6366F1) else MaterialTheme.colorScheme.onSurface)
                                                Text(stringResource(R.string.settings_all_files_symlinks_permissions_as_is), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Select All / Deselect Bar + Count
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Select Projects ($selectedCount/${validProjects.size}):",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(
                                    onClick = {
                                        selectedProjectPaths.clear()
                                        selectedProjectPaths.addAll(validProjects.map { it.path })
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(stringResource(R.string.settings_select_all), fontSize = 11.5.sp, color = Color(0xFF6366F1))
                                }
                                TextButton(
                                    onClick = { selectedProjectPaths.clear() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(stringResource(R.string.settings_deselect_all), fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        // Project Items Tree List
                        if (validProjects.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_no_open_projects_or_workspaces_detected),
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            val normHome = remember(homeDir) { normalizeProjectPath(homeDir, homeDir) }
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(flattenedTree, key = { it.first.path }) { (proj, level, parentProj) ->
                                    val isParentSelected = parentProj != null && selectedProjectPaths.contains(parentProj.path)
                                    val isDirectlySelected = selectedProjectPaths.contains(proj.path)
                                    val isCoveredByParent = isParentSelected
                                    val isSelected = isDirectlySelected || isCoveredByParent
                                    val projNorm = normalizeProjectPath(proj.path, normHome)
                                    val childProjectsCount = validProjects.count {
                                        val otherNorm = normalizeProjectPath(it.path, normHome)
                                        it.path != proj.path && otherNorm.startsWith("$projNorm/")
                                    }

                                    val toggleNode: () -> Unit = {
                                        if (isCoveredByParent && parentProj != null) {
                                            selectedProjectPaths.remove(parentProj.path)
                                            val parentNorm = normalizeProjectPath(parentProj.path, normHome)
                                            val siblings = validProjects.filter {
                                                val otherNorm = normalizeProjectPath(it.path, normHome)
                                                it.path != parentProj.path && otherNorm.startsWith("$parentNorm/")
                                            }
                                            siblings.forEach { sib ->
                                                if (sib.path != proj.path && !selectedProjectPaths.contains(sib.path)) {
                                                    selectedProjectPaths.add(sib.path)
                                                }
                                            }
                                            selectedProjectPaths.remove(proj.path)
                                        } else {
                                            if (isDirectlySelected) {
                                                selectedProjectPaths.remove(proj.path)
                                                val descendants = validProjects.filter {
                                                    val otherNorm = normalizeProjectPath(it.path, normHome)
                                                    otherNorm.startsWith("$projNorm/")
                                                }
                                                descendants.forEach { selectedProjectPaths.remove(it.path) }
                                            } else {
                                                selectedProjectPaths.add(proj.path)
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = (level * 22).dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (level > 0) {
                                            Text(
                                                text = "└──",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color(0xFF6366F1).copy(alpha = 0.8f),
                                                modifier = Modifier.padding(end = 6.dp)
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSelected) Color(0xFF6366F1).copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isSelected) Color(0xFF6366F1).copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { toggleNode() }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Checkbox(
                                                    checked = isSelected,
                                                    onCheckedChange = { toggleNode() },
                                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF6366F1)),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = proj.name,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        if (childProjectsCount > 0) {
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = Color(0xFF6366F1).copy(alpha = 0.15f)
                                                            ) {
                                                                Text(
                                                                    text = stringResource(R.string.settings_subproject_count, childProjectsCount),
                                                                    fontSize = 9.5.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color(0xFF6366F1),
                                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                                )
                                                            }
                                                        }
                                                        if (isCoveredByParent && parentProj != null) {
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = Color(0xFF10B981).copy(alpha = 0.15f)
                                                            ) {
                                                                Text(
                                                                    text = "Included with ${parentProj.name}",
                                                                    fontSize = 9.5.sp,
                                                                    fontWeight = FontWeight.Medium,
                                                                    color = Color(0xFF10B981),
                                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                                )
                                                            }
                                                        } else if (parentProj != null) {
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = Color(0xFF6366F1).copy(alpha = 0.12f)
                                                            ) {
                                                                Text(
                                                                    text = "Inside ${parentProj.name}",
                                                                    fontSize = 9.5.sp,
                                                                    fontWeight = FontWeight.Medium,
                                                                    color = Color(0xFF6366F1),
                                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                    Text(
                                                        text = proj.path,
                                                        fontSize = 10.5.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Backup Action Button
                        Button(
                            onClick = {
                                if (effectiveBackupPaths.isEmpty()) {
                                    Toast.makeText(context, context.getString(R.string.settings_toast_please_select_at_least_one_project_to_ba), Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                onRequestStoragePermission {
                                    val targetsFile = File(context.cacheDir, "backup_targets_${System.currentTimeMillis()}.txt")
                                    targetsFile.writeText(effectiveBackupPaths.joinToString("\n"))
                                    onDismiss()
                                    onExecuteScript(
                                        ScriptExecutionInfo(
                                            title = context.getString(R.string.settings_projects_backup_packager),
                                            scriptName = "backup_projects",
                                            args = listOf("", backupMode, "--paths-file", targetsFile.absolutePath)
                                        )
                                    )
                                }
                            },
                            enabled = selectedCount > 0,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.Archive, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (effectiveBackupPaths.size == selectedCount) stringResource(R.string.settings_backup_selected, selectedCount)
                                       else stringResource(R.string.settings_backup_selected_roots, selectedCount, effectiveBackupPaths.size),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    // ==========================================
                    // TAB 1: RESTORE PROJECTS
                    // ==========================================
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_select_a_projects_backup_archive_zip_to),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )

                        // File Picker Input
                        OutlinedTextField(
                            value = restoreZipPath,
                            onValueChange = {
                                restoreZipPath = it
                                inspectArchive(it.trim())
                            },
                            label = { Text(stringResource(R.string.settings_projects_archive_file_path)) },
                            placeholder = { Text("/sdcard/Download/Antigem/backups/projects_backup_....zip") },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                                    Icon(Icons.Outlined.FolderOpen, contentDescription = stringResource(R.string.settings_choose_file), tint = Color(0xFF6366F1))
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Archive Inspection Summary
                        if (isInspectingArchive) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF6366F1))
                                Text(stringResource(R.string.settings_inspecting_archive_contents_manifest), fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        archiveError?.let { err ->
                            Text(text = err, color = Color(0xFFEF4444), fontSize = 11.5.sp)
                        }

                        if (manifestProjects.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.settings_projects_in_archive_manifestprojects_siz, manifestProjects.size),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        manifestBackupMode?.let { mode ->
                                            Text(stringResource(R.string.settings_mode_mode, mode), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    manifestProjects.forEach { item ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Outlined.Folder, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(16.dp))
                                            Column {
                                                Text(item.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                if (item.originalPath.isNotBlank()) {
                                                    Text(
                                                        stringResource(R.string.settings_target_item_originalpath, item.originalPath),
                                                        fontSize = 10.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Conflict Strategy Option
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = stringResource(R.string.settings_if_target_project_folder_already_exists),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                // Option A: Move to .bak
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { conflictStrategy = "bak" }
                                        .padding(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (conflictStrategy == "bak") Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (conflictStrategy == "bak") Color(0xFF6366F1) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column {
                                        Text(stringResource(R.string.settings_move_existing_to_bak_timestamp_safe_reco), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                        Text(stringResource(R.string.settings_preserves_existing_code_in_a_backup_fold), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                // Option B: Overwrite
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { conflictStrategy = "overwrite" }
                                        .padding(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (conflictStrategy == "overwrite") Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (conflictStrategy == "overwrite") Color(0xFF6366F1) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column {
                                        Text(stringResource(R.string.settings_overwrite_files_in_place), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                        Text(stringResource(R.string.settings_overwrites_matching_files_directly_insid), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }

                        // Optional: Custom Root Target Directory
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = useCustomRestoreDir,
                                onCheckedChange = { useCustomRestoreDir = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF6366F1)),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(stringResource(R.string.settings_restore_to_custom_parent_directory), fontSize = 12.sp)
                        }

                        if (useCustomRestoreDir) {
                            OutlinedTextField(
                                value = customRestoreDir,
                                onValueChange = { customRestoreDir = it },
                                label = { Text(stringResource(R.string.settings_custom_restore_directory)) },
                                placeholder = { Text("/data/data/com.termux/files/home/restored_projects") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Restore Action Button
                        Button(
                            onClick = {
                                val zipPath = restoreZipPath.trim()
                                if (zipPath.isBlank()) {
                                    Toast.makeText(context, context.getString(R.string.settings_toast_please_enter_or_pick_a_backup_zip_file), Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                onDismiss()
                                onExecuteScript(
                                    ScriptExecutionInfo(
                                        title = context.getString(R.string.settings_projects_archive_restorer),
                                        scriptName = "restore_projects",
                                        args = listOf(zipPath, conflictStrategy, if (useCustomRestoreDir) customRestoreDir.trim() else "")
                                    )
                                )
                                scope.launch(Dispatchers.IO) {
                                    delay(1500)
                                    TermuxDaemonManager.loadProjects()
                                }
                            },
                            enabled = restoreZipPath.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = QuotaGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.SettingsBackupRestore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_start_restore_restore_projects), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScriptExecutionDialog(
    info: ScriptExecutionInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val logs = remember { mutableStateListOf<String>() }
    var isRunning by remember { mutableStateOf(true) }
    var exitCode by remember { mutableStateOf<Int?>(null) }
    var durationMs by remember { mutableStateOf(0L) }
    val listState = rememberLazyListState()
    var executionJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val activity = (context as? android.app.Activity) ?: ((context as? android.content.ContextWrapper)?.baseContext as? android.app.Activity)
    DisposableEffect(isRunning) {
        val window = activity?.window
        if (isRunning) {
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LaunchedEffect(info) {
        executionJob?.cancel()
        executionJob = scope.launch {
            isRunning = true
            logs.clear()
            logs.add("⚡ Initializing ${info.scriptName}...")
            val result = LocalEnvironmentManager.executeScriptLive(
                context = context,
                scriptName = info.scriptName,
                args = info.args
            ) { line ->
                val trimmed = line.trim()
                val isProgressBar = (trimmed.startsWith("[") || trimmed.contains("%")) && trimmed.contains("/")
                val prevIsProgressBar = logs.isNotEmpty() && logs.last().let { (it.trim().startsWith("[") || it.contains("%")) && it.contains("/") }
                if (isProgressBar && prevIsProgressBar) {
                    logs[logs.size - 1] = line
                } else {
                    logs.add(line)
                }
            }
            exitCode = result.exitCode
            durationMs = result.durationMs
            isRunning = false
        }
    }

    LaunchedEffect(logs.size, if (logs.isNotEmpty()) logs.last() else "") {
        if (logs.isNotEmpty()) {
            listState.scrollToItem(logs.size - 1)
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isRunning) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !isRunning,
            dismissOnClickOutside = !isRunning,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.80f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isRunning) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = null,
                                tint = GeminiBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        } else if (exitCode == 0) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = QuotaGreen, modifier = Modifier.size(22.dp))
                        } else {
                            Icon(Icons.Default.Error, contentDescription = null, tint = Color.Red, modifier = Modifier.size(22.dp))
                        }

                        Column {
                            Text(
                                text = info.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isRunning) stringResource(R.string.settings_rootfs_executing) else if (exitCode == 0) stringResource(R.string.settings_rootfs_completed, durationMs / 1000.0) else stringResource(R.string.settings_rootfs_finished_error, exitCode ?: -1),
                                fontSize = 11.5.sp,
                                color = if (isRunning) GeminiBlue else if (exitCode == 0) QuotaGreen else Color.Red
                            )
                        }
                    }

                    if (!isRunning) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_close), modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Terminal output container
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF090D16),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp)
                    ) {
                        items(logs) { line ->
                            val cleanLine = line.replace(Regex("\u001B\\[[;\\d]*m"), "")
                            val lineColor = when {
                                cleanLine.startsWith("❌") || cleanLine.contains("Error", ignoreCase = true) -> Color(0xFFFF5252)
                                cleanLine.startsWith("✅") || cleanLine.startsWith("🎉") || cleanLine.contains("Success", ignoreCase = true) -> Color(0xFF4CAF50)
                                cleanLine.startsWith("⚠️") || cleanLine.contains("Note:", ignoreCase = true) || cleanLine.contains("Warning:", ignoreCase = true) -> Color(0xFFFFB74D)
                                cleanLine.startsWith("=") || cleanLine.startsWith("[") -> Color(0xFF64B5F6)
                                else -> Color(0xFFE2E8F0)
                            }
                            Text(
                                text = cleanLine,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = lineColor,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (isRunning) {
                        Button(
                            onClick = {
                                executionJob?.cancel()
                                isRunning = false
                                exitCode = 130
                                logs.add("🛑 Execution cancelled by user.")
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.settings_stop_execution), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    } else {
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (exitCode == 0) QuotaGreen else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(stringResource(R.string.common_done), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SUB-SCREEN: DATA LOSS PROTECTION
// ==========================================
@Composable
private fun DataProtectionSubScreen(
    cardBg: Color,
    cardBorder: BorderStroke
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isAdminActive by remember { mutableStateOf(AntiGemDeviceAdminReceiver.isAdminActive(context)) }
    var isShieldEnabled by remember { mutableStateOf(ManageSpaceActivity.isShieldEnabled(context)) }
    var showDeactivateAdminDialog by remember { mutableStateOf(false) }

    val adminLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        isAdminActive = AntiGemDeviceAdminReceiver.isAdminActive(context)
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isAdminActive = AntiGemDeviceAdminReceiver.isAdminActive(context)
                isShieldEnabled = ManageSpaceActivity.isShieldEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banner info
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF10B981).copy(alpha = 0.1f),
            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Security,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(26.dp)
                )
                Column {
                    Text(
                        text = stringResource(R.string.settings_data_loss_prevention),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.settings_protect_your_local_linux_environment_cli),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Feature 1: Accidental Uninstall Protection (Device Administrator)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = cardBorder
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isAdminActive) QuotaGreen.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isAdminActive) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                                contentDescription = null,
                                tint = if (isAdminActive) QuotaGreen else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_accidental_uninstall_protection),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isAdminActive) stringResource(R.string.settings_admin_protected) else stringResource(R.string.settings_admin_inactive),
                            fontSize = 12.sp,
                            color = if (isAdminActive) QuotaGreen else Color.Gray,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.settings_why_enable_this_antigem_hosts_a_full_loc),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))
                if (isAdminActive) {
                    OutlinedButton(
                        onClick = { showDeactivateAdminDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Outlined.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.settings_deactivate_uninstall_protection), fontSize = 13.sp)
                    }
                } else {
                    Button(
                        onClick = {
                            try {
                                val intent = AntiGemDeviceAdminReceiver.createActivationIntent(context)
                                adminLauncher.launch(intent)
                            } catch (e: Exception) {
                                Log.w("SettingsDialog", "Direct admin prompt failed, opening Device Admin settings", e)
                                try {
                                    context.startActivity(AntiGemDeviceAdminReceiver.createDeviceAdminSettingsIntent())
                                } catch (e2: Exception) {
                                    Toast.makeText(context, context.getString(R.string.settings_toast_could_not_open_device_admin_e2_message, e2.message.orEmpty()), Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = QuotaGreen)
                    ) {
                        Icon(Icons.Outlined.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.settings_activate_uninstall_protection), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            try {
                                context.startActivity(AntiGemDeviceAdminReceiver.createDeviceAdminSettingsIntent())
                            } catch (e: Exception) {
                                Toast.makeText(context, context.getString(R.string.settings_toast_could_not_open_settings_e_message, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Settings, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.settings_open_system_device_admin_apps_list), fontSize = 12.5.sp)
                    }

                    if (Build.VERSION.SDK_INT >= 33) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = stringResource(R.string.settings_sideloaded_app_tip_android_13_if_your_ph),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    lineHeight = 15.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                TextButton(
                                    onClick = {
                                        try {
                                            val appInfoIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = Uri.fromParts("package", context.packageName, null)
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(appInfoIntent)
                                        } catch (_: Exception) {}
                                    },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text(stringResource(R.string.settings_open_app_info), fontSize = 11.5.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Feature 2: Clear Data Shield (ManageSpaceActivity)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = cardBorder
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isShieldEnabled) GeminiBlue.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.CleaningServices,
                                contentDescription = null,
                                tint = if (isShieldEnabled) GeminiBlue else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_app_info_clear_data_shield),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isShieldEnabled) stringResource(R.string.settings_shield_enabled) else stringResource(R.string.settings_shield_disabled),
                            fontSize = 12.sp,
                            color = if (isShieldEnabled) GeminiBlue else Color.Gray
                        )
                    }
                    Switch(
                        checked = isShieldEnabled,
                        onCheckedChange = { enabled ->
                            ManageSpaceActivity.setShieldEnabled(context, enabled)
                            isShieldEnabled = ManageSpaceActivity.isShieldEnabled(context)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.settings_when_enabled_default_tapping_clear_data),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        try {
                            context.startActivity(Intent(context, ManageSpaceActivity::class.java))
                        } catch (e: Exception) {
                            Toast.makeText(context, context.getString(R.string.settings_toast_could_not_open_space_manager_e_message, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Storage, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.settings_open_space_manager_preview), fontSize = 13.sp)
                }
            }
        }
    }

    if (showDeactivateAdminDialog) {
        AlertDialog(
            onDismissRequest = { showDeactivateAdminDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.settings_disable_uninstall_protection)) },
            text = {
                Text(stringResource(R.string.settings_this_will_remove_antigem_s_device_admin),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeactivateAdminDialog = false
                        AntiGemDeviceAdminReceiver.deactivateAdmin(context)
                        isAdminActive = false
                        scope.launch {
                            delay(300)
                            isAdminActive = AntiGemDeviceAdminReceiver.isAdminActive(context)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.settings_deactivate))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeactivateAdminDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

// ==========================================
// SUB-SCREEN 5: COMMANDS & PERMISSIONS
// ==========================================
@Composable
private fun CommandsSubScreen(
    commandAutoExecutionPolicy: String,
    commandSandboxEnabled: Boolean,
    requireApprovalForFileEdits: Boolean,
    defaultApprovalScope: String,
    globalSecuritySettings: com.example.gemini.data.remote.AgyHubClient.GlobalUserSettings?,
    globalSettingsError: String? = null,
    isGlobalSettingsLoading: Boolean,
    projectsList: List<com.example.gemini.data.remote.AgyHubClient.ProjectItem>,
    isProjectsLoading: Boolean,
    cardBg: Color,
    cardBorder: BorderStroke,
    onSetGlobalArtifactReviewMode: (String) -> Unit,
    onSetGlobalSecurityPreset: (autoExec: String, fileAccess: String) -> Unit,
    onSetGlobalCustomTerminalPolicy: (String) -> Unit,
    onSetGlobalCustomFileAccessPolicy: (String) -> Unit,
    onSetGlobalTerminalSandbox: (Boolean) -> Unit,
    onSetProjectInheritGlobal: (com.example.gemini.data.remote.AgyHubClient.ProjectItem) -> Unit,
    onSetProjectPreset: (com.example.gemini.data.remote.AgyHubClient.ProjectItem, autoExec: String, fileAccess: String) -> Unit,
    onRefreshSecurityAndProjects: () -> Unit,
    onSetCommandAutoExecutionPolicy: (String) -> Unit,
    onSetCommandSandboxEnabled: (Boolean) -> Unit,
    onSetRequireApprovalForFileEdits: (Boolean) -> Unit,
    onSetDefaultApprovalScope: (String) -> Unit,
    onAddPermissionRule: (action: String, pattern: String, decision: String) -> Unit,
    onRemovePermissionRule: (rawRule: String) -> Unit,
    onChangePermissionRuleDecision: (rawRule: String, newDecision: String) -> Unit
) {
    if (globalSecuritySettings == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_global_security_policies),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.settings_live_daemon_settings_applied_across_all),
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onRefreshSecurityAndProjects,
                    modifier = Modifier.size(36.dp)
                ) {
                    if (isGlobalSettingsLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = ClaudeTerracotta
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.common_refresh),
                            tint = ClaudeTerracotta,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (isGlobalSettingsLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 2.5.dp,
                            color = ClaudeTerracotta
                        )
                        Text(
                            text = stringResource(R.string.settings_connecting_to_agy_hub_loading_permission),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = stringResource(R.string.settings_unable_to_load_permissions_from_agy_hub),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Text(
                            text = stringResource(R.string.settings_could_not_retrieve_daemon_permissions_an_2, globalSettingsError ?: "RPC connection failed"),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 17.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = onRefreshSecurityAndProjects,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ClaudeTerracotta)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_retry_connection), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        return
    }

    val activeAutoExec = globalSecuritySettings.autoExecutionPolicy
    val activeFileAccess = globalSecuritySettings?.nonWorkspaceFileAccessPolicy ?: "AGENT_SETTING_POLICY_ASK"
    val activeArtifactReview = globalSecuritySettings?.artifactReviewMode ?: "ARTIFACT_REVIEW_MODE_ALWAYS"
    val activeSandbox = globalSecuritySettings?.enableTerminalSandbox ?: commandSandboxEnabled

    val isDefaultMode = activeAutoExec.contains("OFF", ignoreCase = true) && activeFileAccess.contains("ASK", ignoreCase = true)
    val isMachineMode = activeAutoExec.contains("OFF", ignoreCase = true) && activeFileAccess.contains("ALLOW", ignoreCase = true)
    val isTurboMode = activeAutoExec.contains("EAGER", ignoreCase = true) && activeFileAccess.contains("ALLOW", ignoreCase = true)
    var customExpanded by remember { mutableStateOf(!isDefaultMode && !isMachineMode && !isTurboMode) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header with Sync Info & Refresh Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_global_security_policies),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.settings_live_daemon_settings_applied_across_all),
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onRefreshSecurityAndProjects,
                modifier = Modifier.size(36.dp)
            ) {
                if (isGlobalSettingsLoading || isProjectsLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = ClaudeTerracotta
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.common_refresh),
                        tint = ClaudeTerracotta,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // ==========================================
        // 1. ARTIFACT REVIEW POLICY
        // ==========================================
        Column {
            Text(
                text = stringResource(R.string.settings_artifact_review_policy),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.settings_controls_whether_the_assistant_asks_you),
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val isAutoReview = activeArtifactReview.contains("TURBO", ignoreCase = true)
            val isAlwaysReview = activeArtifactReview.contains("ALWAYS", ignoreCase = true) || !isAutoReview

            // Auto Option
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSetGlobalArtifactReviewMode("ARTIFACT_REVIEW_MODE_TURBO") },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isAutoReview) ClaudeTerracotta.copy(alpha = 0.08f) else cardBg
                ),
                border = BorderStroke(
                    if (isAutoReview) 1.5.dp else 1.dp,
                    if (isAutoReview) ClaudeTerracotta else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.settings_auto_turbo),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAutoReview) ClaudeTerracotta else MaterialTheme.colorScheme.onSurface
                        )
                        RadioButton(
                            selected = isAutoReview,
                            onClick = { onSetGlobalArtifactReviewMode("ARTIFACT_REVIEW_MODE_TURBO") },
                            colors = RadioButtonDefaults.colors(selectedColor = ClaudeTerracotta),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.settings_edits_and_creates_documents_immediately),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )
                }
            }

            // Always Ask Option
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSetGlobalArtifactReviewMode("ARTIFACT_REVIEW_MODE_ALWAYS") },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isAlwaysReview) ClaudeTerracotta.copy(alpha = 0.08f) else cardBg
                ),
                border = BorderStroke(
                    if (isAlwaysReview) 1.5.dp else 1.dp,
                    if (isAlwaysReview) ClaudeTerracotta else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.settings_always_ask),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAlwaysReview) ClaudeTerracotta else MaterialTheme.colorScheme.onSurface
                        )
                        RadioButton(
                            selected = isAlwaysReview,
                            onClick = { onSetGlobalArtifactReviewMode("ARTIFACT_REVIEW_MODE_ALWAYS") },
                            colors = RadioButtonDefaults.colors(selectedColor = ClaudeTerracotta),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.settings_suspends_and_requests_your_approval_befo),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ==========================================
        // 2. SECURITY PRESETS
        // ==========================================
        Column {
            Text(
                text = stringResource(R.string.settings_security_presets),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.settings_controls_terminal_command_execution_and),
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val presets = listOf(
            Triple(
                "DEFAULT",
                stringResource(R.string.settings_preset_default_title),
                stringResource(R.string.settings_preset_default_desc)
            ) to (stringResource(R.string.settings_ui_recommended) to Color(0xFF00ACC1)),
            Triple(
                "FULL_MACHINE",
                stringResource(R.string.settings_preset_full_title),
                stringResource(R.string.settings_preset_full_desc)
            ) to (stringResource(R.string.settings_preset_full_badge) to Color(0xFFF59E0B)),
            Triple(
                "TURBO",
                stringResource(R.string.settings_preset_turbo_title),
                stringResource(R.string.settings_preset_turbo_desc)
            ) to (stringResource(R.string.settings_preset_turbo_badge) to QuotaGreen),
            Triple(
                "CUSTOM",
                stringResource(R.string.settings_preset_custom_title),
                stringResource(R.string.settings_preset_custom_desc)
            ) to (stringResource(R.string.settings_preset_custom_badge) to Color(0xFF8B5CF6))
        )

        presets.forEach { (presetInfo, badgeInfo) ->
            val (presetKey, title, desc) = presetInfo
            val (badgeText, badgeColor) = badgeInfo
            val isSelected = when (presetKey) {
                "DEFAULT" -> isDefaultMode
                "FULL_MACHINE" -> isMachineMode
                "TURBO" -> isTurboMode
                "CUSTOM" -> customExpanded || (!isDefaultMode && !isMachineMode && !isTurboMode)
                else -> false
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        when (presetKey) {
                            "DEFAULT" -> {
                                customExpanded = false
                                onSetGlobalSecurityPreset("CASCADE_COMMANDS_AUTO_EXECUTION_OFF", "AGENT_SETTING_POLICY_ASK")
                            }
                            "FULL_MACHINE" -> {
                                customExpanded = false
                                onSetGlobalSecurityPreset("CASCADE_COMMANDS_AUTO_EXECUTION_OFF", "AGENT_SETTING_POLICY_ALLOW")
                            }
                            "TURBO" -> {
                                customExpanded = false
                                onSetGlobalSecurityPreset("CASCADE_COMMANDS_AUTO_EXECUTION_EAGER", "AGENT_SETTING_POLICY_ALLOW")
                            }
                            "CUSTOM" -> {
                                customExpanded = true
                            }
                        }
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) ClaudeTerracotta.copy(alpha = 0.08f) else cardBg
                ),
                border = BorderStroke(
                    if (isSelected) 1.5.dp else 1.dp,
                    if (isSelected) ClaudeTerracotta else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                when (presetKey) {
                                    "DEFAULT" -> {
                                        customExpanded = false
                                        onSetGlobalSecurityPreset("CASCADE_COMMANDS_AUTO_EXECUTION_OFF", "AGENT_SETTING_POLICY_ASK")
                                    }
                                    "FULL_MACHINE" -> {
                                        customExpanded = false
                                        onSetGlobalSecurityPreset("CASCADE_COMMANDS_AUTO_EXECUTION_OFF", "AGENT_SETTING_POLICY_ALLOW")
                                    }
                                    "TURBO" -> {
                                        customExpanded = false
                                        onSetGlobalSecurityPreset("CASCADE_COMMANDS_AUTO_EXECUTION_EAGER", "AGENT_SETTING_POLICY_ALLOW")
                                    }
                                    "CUSTOM" -> {
                                        customExpanded = true
                                    }
                                }
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = ClaudeTerracotta),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = title,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) ClaudeTerracotta else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = badgeColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = badgeText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = badgeColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = desc,
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(start = 32.dp)
                    )

                    // Granular Controls inside Custom Mode
                    if (presetKey == "CUSTOM" && isSelected) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Granular: Terminal Execution
                        Text(
                            text = stringResource(R.string.settings_terminal_command_execution),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val isTermOff = activeAutoExec.contains("OFF", ignoreCase = true)
                            val isTermEager = activeAutoExec.contains("EAGER", ignoreCase = true)

                            FilterChip(
                                selected = isTermOff,
                                onClick = { onSetGlobalCustomTerminalPolicy("CASCADE_COMMANDS_AUTO_EXECUTION_OFF") },
                                label = { Text(stringResource(R.string.settings_ask_every_time), fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = isTermEager,
                                onClick = { onSetGlobalCustomTerminalPolicy("CASCADE_COMMANDS_AUTO_EXECUTION_EAGER") },
                                label = { Text(stringResource(R.string.settings_auto_run_eager), fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Granular: Files Outside Workspace
                        Text(
                            text = stringResource(R.string.settings_files_outside_workspace),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val isFileAsk = activeFileAccess.contains("ASK", ignoreCase = true)
                            val isFileAllow = activeFileAccess.contains("ALLOW", ignoreCase = true)
                            val isFileDeny = activeFileAccess.contains("DENY", ignoreCase = true)

                            FilterChip(
                                selected = isFileAsk,
                                onClick = { onSetGlobalCustomFileAccessPolicy("AGENT_SETTING_POLICY_ASK") },
                                label = { Text(stringResource(R.string.settings_ask), fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = isFileAllow,
                                onClick = { onSetGlobalCustomFileAccessPolicy("AGENT_SETTING_POLICY_ALLOW") },
                                label = { Text(stringResource(R.string.settings_allow), fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = isFileDeny,
                                onClick = { onSetGlobalCustomFileAccessPolicy("AGENT_SETTING_POLICY_DENY") },
                                label = { Text(stringResource(R.string.settings_deny), fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ==========================================
        // 3. SECURITY & SANDBOX TOGGLE
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = cardBorder
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = stringResource(R.string.settings_terminal_sandbox_isolation),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (activeSandbox) stringResource(R.string.settings_sandbox_enabled_desc)
                            else stringResource(R.string.settings_sandbox_disabled_desc),
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                    Switch(
                        checked = activeSandbox,
                        onCheckedChange = { onSetGlobalTerminalSandbox(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ClaudeTerracotta
                        )
                    )
                }

                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = stringResource(R.string.settings_review_file_edits_writes),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.settings_prompt_for_confirmation_before_assistant),
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                    Switch(
                        checked = requireApprovalForFileEdits,
                        onCheckedChange = onSetRequireApprovalForFileEdits,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ClaudeTerracotta
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // ==========================================
        // 4. GRANULAR PERMISSION GRANTS (globalPermissionGrants)
        // ==========================================
        val grants = globalSecuritySettings.globalPermissionGrants
        val allRules = remember(grants) {
            val list = mutableListOf<PermissionGrantRule>()
            grants.allow.forEach { list.add(PermissionGrantRule.parse(it, "ALLOW")) }
            grants.deny.forEach { list.add(PermissionGrantRule.parse(it, "DENY")) }
            grants.ask.forEach { list.add(PermissionGrantRule.parse(it, "ASK")) }
            list
        }

        var selectedFilter by remember { mutableStateOf("ALL") }
        var showAddRuleDialog by remember { mutableStateOf(false) }

        val filteredRules = remember(allRules, selectedFilter) {
            if (selectedFilter == "ALL") allRules
            else allRules.filter { it.action.equals(selectedFilter, ignoreCase = true) }
        }

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_granular_permission_grants),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.settings_explicit_allow_deny_and_ask_rules_for_co),
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { showAddRuleDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ClaudeTerracotta),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.settings_add_rule), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val filters = listOf(
                    "ALL" to stringResource(R.string.settings_filter_all, allRules.size),
                    "command" to stringResource(R.string.settings_filter_command, allRules.count { it.action.equals("command", ignoreCase = true) }),
                    "mcp" to stringResource(R.string.settings_filter_mcp, allRules.count { it.action.equals("mcp", ignoreCase = true) }),
                    "read_file" to stringResource(R.string.settings_filter_read, allRules.count { it.action.equals("read_file", ignoreCase = true) }),
                    "write_file" to stringResource(R.string.settings_filter_write, allRules.count { it.action.equals("write_file", ignoreCase = true) })
                )

                filters.forEach { (key, label) ->
                    val isSel = selectedFilter == key
                    FilterChip(
                        selected = isSel,
                        onClick = { selectedFilter = key },
                        label = { Text(label, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }
        }

        if (filteredRules.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (allRules.isEmpty()) stringResource(R.string.settings_rules_none) else stringResource(R.string.settings_rules_no_match),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.settings_tools_and_file_accesses_will_use_your_gl),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 15.sp
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filteredRules.forEach { rule ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = cardBorder
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.settings_rule_actionicon_rule_actionlabel, rule.actionIcon, if (rule.actionLabelRes != 0) stringResource(rule.actionLabelRes) else rule.action),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = rule.pattern,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                IconButton(
                                    onClick = { onRemovePermissionRule(rule.rawString) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = stringResource(R.string.settings_delete_rule),
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Decision segmented selector: Allow | Deny | Ask
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val decisions = listOf(
                                    Triple("ALLOW", stringResource(R.string.settings_allow), QuotaGreen),
                                    Triple("DENY", stringResource(R.string.settings_deny), MaterialTheme.colorScheme.error),
                                    Triple("ASK", stringResource(R.string.settings_rule_dec_ask), Color(0xFFF59E0B))
                                )

                                decisions.forEach { (decKey, decLabel, decColor) ->
                                    val isSelected = rule.decision.equals(decKey, ignoreCase = true)
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                if (!isSelected) {
                                                    onChangePermissionRuleDecision(rule.rawString, decKey)
                                                }
                                            },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) decColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        border = BorderStroke(
                                            if (isSelected) 1.5.dp else 0.5.dp,
                                            if (isSelected) decColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                        )
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = decLabel,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) decColor else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddRuleDialog) {
            AddPermissionRuleDialog(
                onDismiss = { showAddRuleDialog = false },
                onAdd = { act, pat, dec ->
                    onAddPermissionRule(act, pat, dec)
                    showAddRuleDialog = false
                }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // ==========================================
        // 5. PROJECT OVERRIDES & INHERITANCE (NEW!)
        // ==========================================
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings_project_overrides_inheritance),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (projectsList.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.settings_projectslist_count_it_isinheritingglobal, projectsList.count { it.isInheritingGlobal }, projectsList.size),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.settings_projects_can_inherit_the_global_policy_a),
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isProjectsLoading && projectsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ClaudeTerracotta, modifier = Modifier.size(24.dp))
            }
        } else if (projectsList.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.settings_no_workspace_projects_registered_yet_on),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(14.dp)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                projectsList.forEach { project ->
                    var projectExpanded by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { projectExpanded = !projectExpanded },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = cardBorder
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = project.name.ifBlank { project.id },
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = project.id,
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (project.isInheritingGlobal) QuotaGreen.copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (project.isInheritingGlobal) stringResource(R.string.settings_rules_inherit_global) else stringResource(R.string.settings_rules_custom_override),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (project.isInheritingGlobal) QuotaGreen else Color(0xFFF59E0B),
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            if (projectExpanded) {
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                Spacer(modifier = Modifier.height(10.dp))

                                // Quick button to Inherit Global
                                if (!project.isInheritingGlobal) {
                                    Button(
                                        onClick = {
                                            onSetProjectInheritGlobal(project)
                                            projectExpanded = false
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = QuotaGreen)
                                    ) {
                                        Text(stringResource(R.string.settings_set_to_inherit_global_settings), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                Text(
                                    text = stringResource(R.string.settings_set_project_override),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            onSetProjectPreset(project, "CASCADE_COMMANDS_AUTO_EXECUTION_OFF", "AGENT_SETTING_POLICY_ASK")
                                            projectExpanded = false
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                    ) {
                                        Text(stringResource(R.string.settings_default), fontSize = 10.5.sp)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            onSetProjectPreset(project, "CASCADE_COMMANDS_AUTO_EXECUTION_OFF", "AGENT_SETTING_POLICY_ALLOW")
                                            projectExpanded = false
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                    ) {
                                        Text(stringResource(R.string.settings_machine), fontSize = 10.5.sp)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            onSetProjectPreset(project, "CASCADE_COMMANDS_AUTO_EXECUTION_EAGER", "AGENT_SETTING_POLICY_ALLOW")
                                            projectExpanded = false
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                    ) {
                                        Text(stringResource(R.string.settings_turbo), fontSize = 10.5.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ==========================================
        // 5. APPROVAL SCOPE & PERSISTENCE BANNER
        // ==========================================
        Column {
            Text(
                text = stringResource(R.string.settings_default_approval_scope),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.settings_when_you_click_approve_on_a_tool_prompt),
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val scopes = listOf(
                Triple("PERMISSION_SCOPE_ONCE", stringResource(R.string.settings_scope_once), stringResource(R.string.settings_scope_once_sub)),
                Triple("PERMISSION_SCOPE_CONVERSATION", stringResource(R.string.settings_scope_chat), stringResource(R.string.settings_scope_chat_sub)),
                Triple("PERMISSION_SCOPE_WORKSPACE", stringResource(R.string.chat_workspace_fallback), stringResource(R.string.settings_scope_workspace_sub))
            )

            scopes.forEach { (scopeKey, title, subtitle) ->
                val isSelected = defaultApprovalScope == scopeKey
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSetDefaultApprovalScope(scopeKey) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) ClaudeTerracotta.copy(alpha = 0.12f) else cardBg,
                    border = BorderStroke(
                        1.5.dp,
                        if (isSelected) ClaudeTerracotta else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) ClaudeTerracotta else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = QuotaGreen.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, QuotaGreen.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = QuotaGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.settings_live_synced_with_agy_hub_via_jetbox_proj),
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

private data class PermissionGrantRule(
    val action: String,
    val pattern: String,
    val decision: String,
    val rawString: String
) {
    /** 用户可见的动作名走资源；未识别的动作返回 0，渲染处原样显示技术值。 */
    val actionLabelRes: Int
        get() = when (action.lowercase()) {
            "command" -> R.string.settings_rule_action_command
            "mcp" -> R.string.settings_rule_action_mcp
            "read_file" -> R.string.settings_rule_action_read_file
            "write_file" -> R.string.settings_rule_action_write_file
            else -> 0
        }

    val actionIcon: String
        get() = when (action.lowercase()) {
            "command" -> "⌨️"
            "mcp" -> "🔌"
            "read_file" -> "📖"
            "write_file" -> "✏️"
            else -> "⚙️"
        }

    companion object {
        fun parse(raw: String, decision: String): PermissionGrantRule {
            val parenStart = raw.indexOf('(')
            val parenEnd = raw.lastIndexOf(')')
            if (parenStart != -1 && parenEnd > parenStart) {
                val action = raw.substring(0, parenStart).trim()
                val pattern = raw.substring(parenStart + 1, parenEnd).trim()
                return PermissionGrantRule(action, pattern, decision.uppercase(), raw)
            }
            return PermissionGrantRule("command", raw, decision.uppercase(), raw)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPermissionRuleDialog(
    onDismiss: () -> Unit,
    onAdd: (action: String, pattern: String, decision: String) -> Unit
) {
    var selectedAction by remember { mutableStateOf("command") }
    var patternText by remember { mutableStateOf("") }
    var selectedDecision by remember { mutableStateOf("ALLOW") }

    val quickTemplates = remember(selectedAction) {
        when (selectedAction) {
            "command" -> listOf("*", "npm*", "git*", "python3*", "cargo*", "rm*")
            "mcp" -> listOf("*", "local_tools/*", "filesystem/*", "test_memory/*")
            "read_file" -> listOf("*", "/home/cat/.gemini", "/opt/*", "/tmp/*")
            "write_file" -> listOf("*", "/home/cat/extra/*", "/tmp/*")
            else -> listOf("*")
        }
    }

    val actionOptions = listOf(
        Triple("command", "⌨️ Command", "Terminal bash command pattern"),
        Triple("mcp", "🔌 MCP Tool", "MCP server or tool pattern"),
        Triple("read_file", "📖 Read File", "Filesystem directory or file read path"),
        Triple("write_file", "✏️ Write File", "Filesystem directory or file write path")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.settings_add_permission_grant_rule), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Step 1: Select Action
                Text(stringResource(R.string.settings_1_operation_type), fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    actionOptions.forEach { (key, label, desc) ->
                        val isSel = selectedAction == key
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedAction = key
                                    if (patternText.isBlank() || patternText == "*") {
                                        patternText = if (key == "mcp" || key == "command") "*" else ""
                                    }
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) ClaudeTerracotta.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = BorderStroke(
                                if (isSel) 1.5.dp else 1.dp,
                                if (isSel) ClaudeTerracotta else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSel,
                                    onClick = { selectedAction = key },
                                    colors = RadioButtonDefaults.colors(selectedColor = ClaudeTerracotta),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text(desc, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // Step 2: Target Pattern
                Text(stringResource(R.string.settings_2_target_pattern), fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                OutlinedTextField(
                    value = patternText,
                    onValueChange = { patternText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            when (selectedAction) {
                                "command" -> "e.g. npm*, git*, *"
                                "mcp" -> "e.g. *, local_tools/*"
                                "read_file" -> "e.g. /home/cat/.gemini, *"
                                "write_file" -> "e.g. /home/cat/extra/*, *"
                                else -> "e.g. *"
                            },
                            fontSize = 12.sp
                        )
                    },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                    shape = RoundedCornerShape(10.dp)
                )

                // Quick Templates
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    quickTemplates.forEach { tpl ->
                        SuggestionChip(
                            onClick = { patternText = tpl },
                            label = { Text(tpl, fontSize = 10.5.sp, fontFamily = FontFamily.Monospace) }
                        )
                    }
                }

                // Step 3: Decision
                Text(stringResource(R.string.settings_3_policy_decision), fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val decs = listOf(
                        Triple("ALLOW", "✅ Allow", QuotaGreen),
                        Triple("DENY", "🚫 Deny", MaterialTheme.colorScheme.error),
                        Triple("ASK", "❓ Ask", Color(0xFFF59E0B))
                    )
                    decs.forEach { (key, label, col) ->
                        val isSel = selectedDecision == key
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedDecision = key },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) col.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = BorderStroke(
                                if (isSel) 1.5.dp else 0.5.dp,
                                if (isSel) col else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                        ) {
                            Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(label, fontSize = 11.5.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium, color = if (isSel) col else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                // Preview
                if (patternText.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Rule: ${selectedAction}(${patternText.trim()}) → $selectedDecision",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(selectedAction, patternText.trim(), selectedDecision) },
                enabled = patternText.isNotBlank(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ClaudeTerracotta)
            ) {
                Text(stringResource(R.string.settings_save_rule), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
private fun AutomationSubScreen(
    isBrowserAutomationEnabled: Boolean,
    isTerminalAutomationEnabled: Boolean,
    onToggleBrowserAutomation: (Boolean) -> Unit,
    onToggleTerminalAutomation: (Boolean) -> Unit,
    isFlowAutomationEnabled: Boolean,
    onToggleFlowAutomation: (Boolean) -> Unit,
    cardBg: Color,
    cardBorder: BorderStroke
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Info / Overview Banner
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = ClaudeTerracotta.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, ClaudeTerracotta.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.SmartToy,
                    contentDescription = null,
                    tint = ClaudeTerracotta,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_ai_device_automation_controls),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.settings_control_which_device_capabilities_the_ai),
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Warning / Notice: MCP Server Configuration Requirement
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF59E0B).copy(alpha = 0.12f),
            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_mcp_server_required),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = stringResource(R.string.settings_to_use_these_automation_tools_the_respec),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        lineHeight = 17.sp
                    )
                }
            }
        }

        // 1. Browser Automation Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = if (isBrowserAutomationEnabled) BorderStroke(1.dp, ClaudeTerracotta.copy(alpha = 0.4f)) else cardBorder
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isBrowserAutomationEnabled) ClaudeTerracotta.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Language,
                                    contentDescription = null,
                                    tint = if (isBrowserAutomationEnabled) ClaudeTerracotta else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = stringResource(R.string.settings_browser_automation),
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isBrowserAutomationEnabled) QuotaGreen.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (isBrowserAutomationEnabled) "ACTIVE" else "DISABLED",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isBrowserAutomationEnabled) QuotaGreen else Color.Gray,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                    )
                                }
                            }
                            Text(
                                text = stringResource(R.string.settings_webview_ui_dom_inspection_screenshots),
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    Switch(
                        checked = isBrowserAutomationEnabled,
                        onCheckedChange = onToggleBrowserAutomation,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ClaudeTerracotta
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.settings_allows_the_ai_to_open_web_urls_in_backgr),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.settings_exposed_mcp_tools_9),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val browserTools = listOf(
                        "browser_open_url", "browser_set_view_mode", "browser_screenshot", "browser_inspect_dom",
                        "browser_interact", "browser_eval_js", "browser_get_console_logs",
                        "browser_list_tabs", "browser_switch_tab", "browser_close_tab"
                    )
                    browserTools.forEach { tool ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        ) {
                            Text(
                                text = tool,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (isBrowserAutomationEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. Terminal Automation Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = if (isTerminalAutomationEnabled) BorderStroke(1.dp, Color(0xFF00ACC1).copy(alpha = 0.4f)) else cardBorder
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isTerminalAutomationEnabled) Color(0xFF00ACC1).copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Terminal,
                                    contentDescription = null,
                                    tint = if (isTerminalAutomationEnabled) Color(0xFF00ACC1) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = stringResource(R.string.settings_terminal_automation),
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isTerminalAutomationEnabled) QuotaGreen.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (isTerminalAutomationEnabled) "ACTIVE" else "DISABLED",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isTerminalAutomationEnabled) QuotaGreen else Color.Gray,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                    )
                                }
                            }
                            Text(
                                text = stringResource(R.string.settings_live_shell_execution_output_buffer_signa),
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    Switch(
                        checked = isTerminalAutomationEnabled,
                        onCheckedChange = onToggleTerminalAutomation,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF00ACC1)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.settings_allows_the_ai_to_inspect_active_in_app_t),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.settings_exposed_mcp_tools_6),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val terminalTools = listOf(
                        "terminal_list_sessions", "terminal_send_command", "terminal_read_output",
                        "terminal_kill_process", "terminal_create_session", "terminal_close_session"
                    )
                    terminalTools.forEach { tool ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        ) {
                            Text(
                                text = tool,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (isTerminalAutomationEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Flow Automation Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = if (isFlowAutomationEnabled) BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.4f)) else cardBorder
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isFlowAutomationEnabled) Color(0xFF8B5CF6).copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Movie,
                                    contentDescription = null,
                                    tint = if (isFlowAutomationEnabled) Color(0xFF8B5CF6) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = stringResource(R.string.settings_flow_automation),
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isFlowAutomationEnabled) QuotaGreen.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (isFlowAutomationEnabled) "ACTIVE" else "DISABLED",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFlowAutomationEnabled) QuotaGreen else Color.Gray,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                    )
                                }
                            }
                            Text(
                                text = stringResource(R.string.settings_google_flow_image_video_generation),
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    Switch(
                        checked = isFlowAutomationEnabled,
                        onCheckedChange = onToggleFlowAutomation,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF8B5CF6)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.settings_allows_the_ai_to_generate_images_and_vid),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.settings_this_switch_controls_the_legacy_google_f),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ==========================================
// SUB-SCREEN 8: DIAGNOSTICS & PERFORMANCE
// ==========================================
@Composable
private fun DiagnosticsSubScreen(
    isFloatingDiagnosticsEnabled: Boolean,
    onToggleFloatingDiagnostics: (Boolean) -> Unit,
    isNetworkInspectorEnabled: Boolean,
    onToggleNetworkInspector: (Boolean) -> Unit,
    isFloatingNetworkInspectorEnabled: Boolean,
    onToggleFloatingNetworkInspector: (Boolean) -> Unit,
    onOpenNetworkInspector: () -> Unit,
    cardBg: Color,
    cardBorder: BorderStroke
) {
    DisposableEffect(isFloatingDiagnosticsEnabled) {
        com.example.gemini.data.remote.core.AntiGemLiveDiagnostics.start()
        onDispose {
            if (!isFloatingDiagnosticsEnabled) {
                com.example.gemini.data.remote.core.AntiGemLiveDiagnostics.stop()
            }
        }
    }

    val snapshot by com.example.gemini.data.remote.core.AntiGemLiveDiagnostics.snapshot.collectAsState()
    val logs by com.example.gemini.data.remote.inspector.NetworkInspectorManager.logs.collectAsState()
    val activeStreams by com.example.gemini.data.remote.inspector.NetworkInspectorManager.activeStreamsCount.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var pendingInspectorRestart by remember { mutableStateOf<Boolean?>(null) }

    if (pendingInspectorRestart != null) {
        val targetState = pendingInspectorRestart!!
        AlertDialog(
            onDismissRequest = { pendingInspectorRestart = null },
            title = {
                Text(
                    text = stringResource(R.string.settings_restart_required),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = if (targetState) {
                        "Enabling Network Inspector requires restarting the app so OkHttp can attach live routing interceptors. Restart now?"
                    } else {
                        "Disabling Network Inspector requires restarting the app to restore 100% native network performance with zero debug interceptors. Restart now?"
                    },
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toEnable = targetState
                        pendingInspectorRestart = null
                        com.example.gemini.data.preferences.AuthPreferences(context).saveNetworkInspectorEnabledSync(toEnable)
                        val pm = context.packageManager
                        val intent = pm.getLaunchIntentForPackage(context.packageName)
                        if (intent != null) {
                            val restartIntent = android.content.Intent.makeRestartActivityTask(intent.component)
                            context.startActivity(restartIntent)
                            Runtime.getRuntime().exit(0)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.common_restart_app), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingInspectorRestart = null }
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(14.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Network Inspector Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = cardBorder
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = (if (isNetworkInspectorEnabled) Color(0xFF0284C7) else Color.Gray).copy(alpha = 0.12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = if (isNetworkInspectorEnabled) Color(0xFF0284C7) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = stringResource(R.string.settings_network_inspector_live_logs),
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isNetworkInspectorEnabled) Color(0xFF0284C7).copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (isNetworkInspectorEnabled) "ACTIVE" else "OFF",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isNetworkInspectorEnabled) Color(0xFF0284C7) else Color.Gray,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                    )
                                }
                            }
                            Text(
                                text = stringResource(R.string.settings_inspect_ide_bridge_http_agy_daemon_grpc),
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    Switch(
                        checked = isNetworkInspectorEnabled,
                        onCheckedChange = { targetState ->
                            pendingInspectorRestart = targetState
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF0284C7)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.settings_zero_overhead_architecture_when_disabled),
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    lineHeight = 16.sp
                )

                if (isNetworkInspectorEnabled) {
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Floating Inspector Bubble toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_floating_network_bubble),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.settings_show_on_screen_draggable_pill_with_live),
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Switch(
                            checked = isFloatingNetworkInspectorEnabled,
                            onCheckedChange = onToggleFloatingNetworkInspector,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF0284C7)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Open Inspector Button
                    Button(
                        onClick = onOpenNetworkInspector,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0284C7)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Open Network Inspector (${logs.size} calls recorded)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
        // Toggle Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = cardBorder
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = (if (isFloatingDiagnosticsEnabled) QuotaGreen else Color.Gray).copy(alpha = 0.12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Speed,
                                    contentDescription = null,
                                    tint = if (isFloatingDiagnosticsEnabled) QuotaGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = stringResource(R.string.settings_floating_diagnostics_hud),
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isFloatingDiagnosticsEnabled) QuotaGreen.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (isFloatingDiagnosticsEnabled) "VISIBLE" else "DISABLED",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFloatingDiagnosticsEnabled) QuotaGreen else Color.Gray,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                    )
                                }
                            }
                            Text(
                                text = stringResource(R.string.settings_live_on_screen_thread_queue_connection_m),
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    Switch(
                        checked = isFloatingDiagnosticsEnabled,
                        onCheckedChange = onToggleFloatingDiagnostics,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = QuotaGreen
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.settings_displays_a_draggable_real_time_floating),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    lineHeight = 17.sp
                )
            }
        }

        // Live Snapshot Metric Preview Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = cardBorder
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_current_telemetry_snapshot),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), thickness = 0.8.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.settings_active_jvm_threads),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${snapshot.activeJvmThreads} threads",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.settings_agyhub_stream_rpcs),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Run=${snapshot.agyRunning} | Q=${snapshot.agyQueued}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = if (snapshot.agyQueued > 0) Color(0xFFFF5252) else MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.settings_agyhub_connection_pool),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${snapshot.agyConns} sockets (${snapshot.agyIdleConns} idle)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.settings_ide_bridge_sockets),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${snapshot.ideConns} sockets (${snapshot.ideIdleConns} idle)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.settings_event_loop_io_latency),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = if (snapshot.ioLagMs >= 0) "${snapshot.ioLagMs} ms" else "ERR",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = if (snapshot.isLagging) Color(0xFFFF5252) else QuotaGreen
                    )
                }

                if (snapshot.threadGroupSummary.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.settings_thread_breakdown),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = snapshot.threadGroupSummary,
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutSubScreen(
    cardBg: Color,
    cardBorder: BorderStroke
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var updateCheckResultText by remember { mutableStateOf<String?>(null) }
    var isUpdateCheckError by remember { mutableStateOf(false) }

    // Bridge file tracking in public Downloads directory
    val downloadDir = remember { Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) }
    val bridgeFile = remember { File(downloadDir, "agy_ide_bridge") }
    var bridgeExists by remember { mutableStateOf(bridgeFile.exists() && bridgeFile.length() > 0) }
    var bridgeSize by remember { mutableStateOf(if (bridgeFile.exists()) bridgeFile.length() else 0L) }
    var bridgeLastModified by remember { mutableStateOf(if (bridgeFile.exists()) bridgeFile.lastModified() else 0L) }
    var isExportingBridge by remember { mutableStateOf(false) }

    fun refreshBridgeStatus() {
        val f = File(downloadDir, "agy_ide_bridge")
        bridgeExists = f.exists() && f.length() > 0
        bridgeSize = if (f.exists()) f.length() else 0L
        bridgeLastModified = if (f.exists()) f.lastModified() else 0L
    }

    if (updateInfo != null) {
        AppUpdateDialog(
            updateInfo = updateInfo!!,
            onDismiss = { updateInfo = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. App Header & Branding Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = cardBorder
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF8B5CF6).copy(alpha = 0.15f),
                    border = BorderStroke(1.5.dp, Color(0xFF8B5CF6).copy(alpha = 0.4f)),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "AntiGem",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.settings_antigravity_mobile_hub_ai_pair_programme),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF8B5CF6).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "Version ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF8B5CF6),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // 2. Package & Environment Details
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = cardBorder
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.settings_build_package_information),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings_application_id),
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = BuildConfig.APPLICATION_ID,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings_build_type),
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = BuildConfig.BUILD_TYPE.uppercase(Locale.ROOT),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings_native_architecture),
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // 3. Updates Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = cardBorder
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SystemUpdateAlt,
                            contentDescription = null,
                            tint = ClaudeTerracotta,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.settings_software_updates),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.settings_checks_github_releases_for_new_versions),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                if (updateCheckResultText != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isUpdateCheckError) Color(0xFFFF5252).copy(alpha = 0.12f) else QuotaGreen.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, if (isUpdateCheckError) Color(0xFFFF5252).copy(alpha = 0.3f) else QuotaGreen.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isUpdateCheckError) Icons.Default.Warning else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isUpdateCheckError) Color(0xFFFF5252) else QuotaGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = updateCheckResultText!!,
                                fontSize = 12.sp,
                                color = if (isUpdateCheckError) Color(0xFFFF5252) else QuotaGreen
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        if (!isCheckingUpdate) {
                            isCheckingUpdate = true
                            updateCheckResultText = null
                            isUpdateCheckError = false
                            coroutineScope.launch {
                                val updateManager = AppUpdateManager(context)
                                val checkResult = updateManager.checkForUpdates(forceCheck = true)
                                isCheckingUpdate = false
                                checkResult.onSuccess { info ->
                                    if (info != null) {
                                        updateInfo = info
                                        updateCheckResultText = null
                                    } else {
                                        isUpdateCheckError = false
                                        updateCheckResultText = "AntiGem is up to date (v${BuildConfig.VERSION_NAME})."
                                        Toast.makeText(context, context.getString(R.string.settings_toast_antigem_is_up_to_date), Toast.LENGTH_SHORT).show()
                                    }
                                }.onFailure { err ->
                                    isUpdateCheckError = true
                                    val msg = err.localizedMessage ?: "Could not connect to GitHub"
                                    updateCheckResultText = msg
                                    Toast.makeText(context, context.getString(R.string.settings_toast_update_check_failed_msg, msg), Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    enabled = !isCheckingUpdate,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ClaudeTerracotta)
                ) {
                    if (isCheckingUpdate) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_checking_github))
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_check_for_updates))
                    }
                }
            }
        }

        // 4. AGY IDE Bridge Server & Downloads Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = cardBorder
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FolderZip,
                            contentDescription = null,
                            tint = Color(0xFF00ACC1),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.settings_agy_ide_bridge_binary),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (bridgeExists) QuotaGreen.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (bridgeExists) "IN DOWNLOADS" else "NOT EXPORTED",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (bridgeExists) QuotaGreen else Color.Gray,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.settings_export_the_compiled_agy_ide_bridge_daemo),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Path: ${bridgeFile.absolutePath}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        )
                        if (bridgeExists) {
                            val sizeKb = bridgeSize / 1024
                            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(bridgeLastModified))
                            Text(
                                text = "Size: $sizeKb KB • Modified: $dateStr",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = QuotaGreen
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            isExportingBridge = true
                            coroutineScope.launch(Dispatchers.IO) {
                                val exported = LocalServerManager.saveBridgeToDownloads(context)
                                withContext(Dispatchers.Main) {
                                    isExportingBridge = false
                                    refreshBridgeStatus()
                                    if (exported != null && exported.exists()) {
                                        Toast.makeText(context, context.getString(R.string.settings_toast_saved_agy_ide_bridge_to_downloads), Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, context.getString(R.string.settings_toast_failed_to_export_bridge_binary), Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        enabled = !isExportingBridge,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00ACC1))
                    ) {
                        if (isExportingBridge) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.settings_export_to_downloads), fontSize = 12.5.sp)
                        }
                    }

                    if (bridgeExists) {
                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, context.getString(R.string.settings_toast_downloads_bridgefile_absolutepath, bridgeFile.absolutePath), Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.weight(0.7f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF00ACC1).copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                tint = Color(0xFF00ACC1),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.settings_view_file), fontSize = 12.5.sp, color = Color(0xFF00ACC1))
                        }
                    }
                }
            }
        }

        // 5. GitHub & External Links
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = cardBorder
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.settings_resources_repository),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/santoshkurmi/antigem"))
                            context.startActivity(intent)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_github_repository),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.settings_github_com_santoshkurmi_antigem),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/santoshkurmi/antigem/releases"))
                            context.startActivity(intent)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_release_changelogs),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.settings_view_tag_notes_apk_downloads),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
