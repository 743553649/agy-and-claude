package com.example.gemini.data.agent.claude

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import com.example.gemini.R

/**
 * Claude Code configuration through the bridge: `~/.claude/settings.json` (defaults, permission rules, everything
 * else), CLAUDE.md memory, MCP servers, plugins & marketplaces, and CLI install/update.
 */
class ClaudeConfigManager(
    private val scope: CoroutineScope,
    private val client: ClaudeBridgeClient,
    private val appContext: Context
) {
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    // ---------------------------------------------------------------- settings.json

    private val _settings = MutableStateFlow<JsonObject?>(null)
    val settings: StateFlow<JsonObject?> = _settings.asStateFlow()

    private val _settingsPath = MutableStateFlow("")
    val settingsPath: StateFlow<String> = _settingsPath.asStateFlow()

    private val _settingsError = MutableStateFlow<String?>(null)
    val settingsError: StateFlow<String?> = _settingsError.asStateFlow()

    /** Called after settings change so the chat picks up new defaults. */
    var onSettingsChanged: () -> Unit = {}

    private val _sandbox = MutableStateFlow<ClaudeSandboxInfo?>(null)
    /** Whether Claude Code's command sandbox can run on this device (checked by the bridge) and is turned on. */
    val sandbox: StateFlow<ClaudeSandboxInfo?> = _sandbox.asStateFlow()

    fun loadSandbox() {
        scope.launch {
            client.sandbox()
                .onSuccess { _sandbox.value = it }
                .onFailure { _sandbox.value = ClaudeSandboxInfo(reason = "Could not check: ${it.message}") }
        }
    }

    /**
     * Turns the command sandbox on or off in settings.json: commands then run in an isolated container and the
     * ones that stay inside it are approved without asking.
     */
    fun setSandboxEnabled(enabled: Boolean) {
        val cur = (_settings.value?.get("sandbox") as? JsonObject).orEmpty()
        val next = cur + ("enabled" to JsonPrimitive(enabled)) +
            (if (enabled && "autoAllowBashIfSandboxed" !in cur) mapOf("autoAllowBashIfSandboxed" to JsonPrimitive(true)) else emptyMap())
        setValue("sandbox", JsonObject(next))
        _sandbox.value = _sandbox.value?.copy(enabled = enabled)
    }

    fun loadSettings() {
        scope.launch {
            client.settings()
                .onSuccess {
                    _settingsPath.value = it.path
                    if (it.success) {
                        _settings.value = it.settings
                        _settingsError.value = null
                    } else _settingsError.value = it.error
                }
                .onFailure { _settingsError.value = it.message }
        }
    }

    fun saveSettings(newSettings: JsonObject, feedback: String = "Saved") {
        scope.launch {
            client.saveSettings(newSettings)
                .onSuccess {
                    if (it.success) {
                        _settings.value = newSettings
                        _settingsError.value = null
                        _messages.tryEmit(feedback)
                        onSettingsChanged()
                    } else _messages.tryEmit(appContext.getString(R.string.claude_msg_save_failed, it.error))
                }
                .onFailure { _messages.tryEmit(appContext.getString(R.string.claude_msg_save_failed, it.message)) }
        }
    }

    /** Sets (or removes, when [value] is null) a top-level key. */
    fun setValue(key: String, value: JsonElement?) {
        val cur = _settings.value ?: JsonObject(emptyMap())
        val next = if (value == null) JsonObject(cur - key) else JsonObject(cur + (key to value))
        saveSettings(next)
    }

    fun stringValue(key: String): String? = (_settings.value?.get(key) as? JsonPrimitive)?.contentOrNull

    /** permissions.allow / deny / ask */
    fun rules(behavior: String): List<String> =
        ((_settings.value?.get("permissions") as? JsonObject)?.get(behavior) as? JsonArray).orEmpty()
            .mapNotNull { (it as? JsonPrimitive)?.contentOrNull }

    fun addRule(behavior: String, rule: String) {
        val clean = rule.trim()
        if (clean.isEmpty()) return
        updatePermissions { perms ->
            val updated = perms.toMutableMap()
            for (b in listOf("allow", "deny", "ask")) {
                val list = (perms[b] as? JsonArray).orEmpty().filter { (it as? JsonPrimitive)?.contentOrNull != clean }
                updated[b] = JsonArray(if (b == behavior) list + JsonPrimitive(clean) else list)
            }
            updated
        }
    }

    fun removeRule(behavior: String, rule: String) = updatePermissions { perms ->
        val updated = perms.toMutableMap()
        updated[behavior] = JsonArray((perms[behavior] as? JsonArray).orEmpty().filter { (it as? JsonPrimitive)?.contentOrNull != rule })
        updated
    }

    /** permissions.defaultMode */
    fun defaultMode(): String? = ((_settings.value?.get("permissions") as? JsonObject)?.get("defaultMode") as? JsonPrimitive)?.contentOrNull

    fun setDefaultMode(mode: String?) = updatePermissions { perms ->
        val updated = perms.toMutableMap()
        if (mode == null) updated.remove("defaultMode") else updated["defaultMode"] = JsonPrimitive(mode)
        updated
    }

    private fun updatePermissions(change: (Map<String, JsonElement>) -> Map<String, JsonElement>) {
        val cur = _settings.value ?: JsonObject(emptyMap())
        val perms = (cur["permissions"] as? JsonObject).orEmpty()
        val next = JsonObject(cur + ("permissions" to JsonObject(change(perms))))
        saveSettings(next)
    }

    // ---------------------------------------------------------------- memory (CLAUDE.md)

    private val _memory = MutableStateFlow<ClaudeMemoryResponse?>(null)
    val memory: StateFlow<ClaudeMemoryResponse?> = _memory.asStateFlow()

    fun loadMemory(scopeName: String, cwd: String?) {
        scope.launch {
            _memory.value = null
            client.memory(scopeName, cwd)
                .onSuccess { _memory.value = it }
                .onFailure { _messages.tryEmit(appContext.getString(R.string.claude_msg_read_md_failed, it.message)) }
        }
    }

    fun saveMemory(scopeName: String, cwd: String?, content: String) {
        scope.launch {
            client.saveMemory(scopeName, cwd, content)
                .onSuccess {
                    if (it.success) {
                        _messages.tryEmit(appContext.getString(R.string.claude_msg_md_saved))
                        _memory.value = _memory.value?.copy(content = content, exists = true)
                    } else _messages.tryEmit(appContext.getString(R.string.claude_msg_save_failed, it.error))
                }
                .onFailure { _messages.tryEmit(appContext.getString(R.string.claude_msg_save_failed, it.message)) }
        }
    }

    private val _autoMemory = MutableStateFlow<ClaudeAutoMemoryResponse?>(null)
    /** Notes Claude saved by itself for the project (null while loading). */
    val autoMemory: StateFlow<ClaudeAutoMemoryResponse?> = _autoMemory.asStateFlow()

    fun loadAutoMemory(cwd: String?) {
        scope.launch {
            _autoMemory.value = null
            client.autoMemory(cwd)
                .onSuccess { _autoMemory.value = it }
                .onFailure { _autoMemory.value = ClaudeAutoMemoryResponse(error = it.message ?: "Cannot reach the bridge") }
        }
    }

    fun deleteAutoMemory(cwd: String?, name: String) {
        scope.launch {
            client.deleteAutoMemory(cwd, name)
                .onSuccess {
                    if (it.success) {
                        _messages.tryEmit(appContext.getString(R.string.claude_msg_deleted, name))
                        _autoMemory.value = _autoMemory.value?.let { m -> m.copy(files = m.files.filter { f -> f.name != name }) }
                    } else _messages.tryEmit(appContext.getString(R.string.claude_msg_delete_failed, it.error))
                }
                .onFailure { _messages.tryEmit(appContext.getString(R.string.claude_msg_delete_failed, it.message)) }
        }
    }

    // ---------------------------------------------------------------- MCP

    private val _mcpServers = MutableStateFlow<List<ClaudeMcpServer>>(emptyList())
    val mcpServers: StateFlow<List<ClaudeMcpServer>> = _mcpServers.asStateFlow()

    private val _mcpLoading = MutableStateFlow(false)
    val mcpLoading: StateFlow<Boolean> = _mcpLoading.asStateFlow()

    private val _mcpOps = MutableStateFlow<Map<String, String>>(emptyMap())
    /** Server name → what is being done to it right now ("Adding…", "Removing…"). */
    val mcpOps: StateFlow<Map<String, String>> = _mcpOps.asStateFlow()

    private val _mcpAddError = MutableStateFlow<Pair<String, String>?>(null)
    /** The last add that failed: server name → the CLI's error (shown on the add sheet / list). */
    val mcpAddError: StateFlow<Pair<String, String>?> = _mcpAddError.asStateFlow()

    private fun setMcpOp(name: String, op: String?) {
        _mcpOps.value = if (op == null) _mcpOps.value - name else _mcpOps.value + (name to op)
    }

    fun loadMcp(cwd: String?) {
        scope.launch { loadMcpNow(cwd) }
    }

    fun addMcp(req: ClaudeMcpAddRequest, cwd: String?) {
        scope.launch {
            _mcpAddError.value = null
            setMcpOp(req.name, "Adding…")
            val error = client.addMcpServer(req).fold(
                { if (it.success) null else it.error?.lines()?.lastOrNull { l -> l.isNotBlank() } ?: "Add failed" },
                { it.message ?: "Add failed" }
            )
            if (error != null) {
                setMcpOp(req.name, null)
                _mcpAddError.value = req.name to error
                _messages.tryEmit(appContext.getString(R.string.claude_msg_add_failed, req.name, error))
                return@launch
            }
            // added: the status check connects to it (can take a few seconds)
            setMcpOp(req.name, "Connecting…")
            loadMcpNow(cwd)
            setMcpOp(req.name, null)
            val added = _mcpServers.value.find { it.name == req.name }
            _messages.tryEmit(
                when (added?.status) {
                    "connected" -> appContext.getString(R.string.claude_msg_connected_tools, req.name, added.tools.size)
                    "failed" -> appContext.getString(R.string.claude_msg_added_no_connect, req.name)
                    else -> appContext.getString(R.string.claude_msg_added, req.name)
                }
            )
        }
    }

    fun removeMcp(name: String, scopeName: String?, cwd: String?) {
        scope.launch {
            setMcpOp(name, "Removing…")
            client.removeMcpServer(name, scopeName, cwd)
                .onSuccess {
                    if (it.success) {
                        _messages.tryEmit(appContext.getString(R.string.claude_msg_removed, name))
                        _mcpServers.value = _mcpServers.value.filter { s -> s.name != name }
                    } else _messages.tryEmit(it.error ?: appContext.getString(R.string.claude_msg_remove_failed))
                }
                .onFailure { _messages.tryEmit(appContext.getString(R.string.claude_msg_remove_failed_detail, it.message)) }
            setMcpOp(name, null)
            loadMcpNow(cwd)
        }
    }

    private suspend fun loadMcpNow(cwd: String?) {
        _mcpLoading.value = true
        client.mcpServers(cwd)
            .onSuccess { if (it.success) _mcpServers.value = it.servers else _messages.tryEmit(appContext.getString(R.string.claude_msg_mcp_status_failed, it.error)) }
            .onFailure { _messages.tryEmit(appContext.getString(R.string.claude_msg_mcp_status_failed, it.message)) }
        _mcpLoading.value = false
    }

    // ---------------------------------------------------------------- plugins

    private val _plugins = MutableStateFlow<ClaudePluginsResponse?>(null)
    val plugins: StateFlow<ClaudePluginsResponse?> = _plugins.asStateFlow()

    private val _pluginOps = MutableStateFlow<Map<String, String>>(emptyMap())
    /** Plugin key (or marketplace name / source) → its pending or running action ("install", "uninstall", …). */
    val pluginOps: StateFlow<Map<String, String>> = _pluginOps.asStateFlow()

    // the CLI changes one shared plugin config: actions run one after another
    private val pluginMutex = kotlinx.coroutines.sync.Mutex()

    fun loadPlugins() {
        scope.launch {
            client.plugins()
                .onSuccess { _plugins.value = it }
                .onFailure { _messages.tryEmit(appContext.getString(R.string.claude_msg_plugin_list_failed, it.message)) }
        }
    }

    /** action: install | uninstall | enable | disable | update | marketplace-add | marketplace-remove | marketplace-update */
    fun pluginAction(action: String, body: ClaudePluginAction) {
        val key = body.plugin.ifBlank { body.name ?: body.source ?: action }
        if (key in _pluginOps.value) return
        _pluginOps.value = _pluginOps.value + (key to action)
        scope.launch {
            val label = key.substringBefore('@')
            pluginMutex.withLock {
                client.pluginAction(action, body)
                    .onSuccess {
                        if (it.success) _messages.tryEmit(appContext.getString(R.string.claude_msg_plugin_done, pluginActionDone(action), label))
                        else _messages.tryEmit(appContext.getString(R.string.claude_msg_label_detail, label, it.error?.lines()?.lastOrNull { l -> l.isNotBlank() } ?: appContext.getString(R.string.claude_msg_action_failed, actionLabel(action))))
                    }
                    .onFailure { _messages.tryEmit(appContext.getString(R.string.claude_msg_action_failed_detail, label, actionLabel(action), it.message)) }
                client.plugins().onSuccess { _plugins.value = it }
            }
            _pluginOps.value = _pluginOps.value - key
        }
    }

    private fun pluginActionDone(action: String) = when (action) {
        "install" -> appContext.getString(R.string.common_installed)
        "uninstall" -> appContext.getString(R.string.claude_plugin_done_uninstalled)
        "enable" -> appContext.getString(R.string.claude_plugin_done_enabled)
        "disable" -> appContext.getString(R.string.settings_ui_disabled)
        "update" -> appContext.getString(R.string.claude_plugin_done_updated)
        "marketplace-add" -> appContext.getString(R.string.claude_plugin_done_market_added)
        "marketplace-remove" -> appContext.getString(R.string.claude_plugin_done_market_removed)
        "marketplace-update" -> appContext.getString(R.string.claude_plugin_done_market_updated)
        else -> appContext.getString(R.string.claude_plugin_done_other)
    }

    /** 协议动作名 -> 中文动词；未识别的原样返回（技术值）。 */
    private fun actionLabel(action: String) = when (action) {
        "install" -> appContext.getString(R.string.claude_action_install)
        "uninstall" -> appContext.getString(R.string.claude_action_uninstall)
        "update" -> appContext.getString(R.string.claude_action_update)
        "enable" -> appContext.getString(R.string.claude_action_enable)
        "disable" -> appContext.getString(R.string.claude_action_disable)
        else -> action
    }

    // ---------------------------------------------------------------- CLI install / update

    private val _cliJob = MutableStateFlow<ClaudeCliJob?>(null)
    val cliJob: StateFlow<ClaudeCliJob?> = _cliJob.asStateFlow()

    /** Called when an install/update finishes. */
    var onCliChanged: () -> Unit = {}

    fun startCliJob(action: String) {
        scope.launch {
            client.startCliJob(action)
                .onSuccess { resp ->
                    if (!resp.success) {
                        _messages.tryEmit(resp.error ?: appContext.getString(R.string.claude_msg_action_failed_start, actionLabel(action)))
                        return@onSuccess
                    }
                    _cliJob.value = resp.job
                    while (true) {
                        delay(1500)
                        val job = client.cliJob().getOrNull()?.job ?: break
                        _cliJob.value = job
                        if (!job.running) {
                            _messages.tryEmit(if (job.exit_code == 0) appContext.getString(R.string.claude_msg_cli_finished, actionLabel(action)) else appContext.getString(R.string.claude_msg_cli_failed, actionLabel(action), job.exit_code))
                            onCliChanged()
                            break
                        }
                    }
                }
                .onFailure { _messages.tryEmit(appContext.getString(R.string.claude_msg_action_detail, actionLabel(action), it.message)) }
        }
    }
}
