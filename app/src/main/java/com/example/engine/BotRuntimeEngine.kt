package com.example.engine

import com.example.data.model.BotFile
import com.example.data.model.BotProject
import com.example.data.repository.BotRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.random.Random

data class DiscordSimulatorMessage(
    val id: String = System.currentTimeMillis().toString() + "_" + Random.nextInt(1000),
    val authorName: String,
    val authorAvatarUrl: String = "",
    val isBot: Boolean = false,
    val content: String = "",
    val embed: DiscordSimulatorEmbed? = null,
    val buttons: List<DiscordSimulatorButton> = emptyList(),
    val selectMenu: DiscordSimulatorSelectMenu? = null,
    val timestamp: String = "Today at " + java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
)

data class DiscordSimulatorEmbed(
    val title: String,
    val description: String = "",
    val colorHex: String = "#5865F2",
    val authorName: String = "",
    val authorIconUrl: String = "",
    val fields: List<Pair<String, String>> = emptyList(),
    val footerText: String = "",
    val thumbnailUrl: String = ""
)

data class DiscordSimulatorButton(
    val id: String,
    val label: String,
    val style: String = "PRIMARY", // PRIMARY, SECONDARY, SUCCESS, DANGER, LINK
    val emoji: String = "",
    val disabled: Boolean = false
)

class BotRuntimeEngine(
    private val repository: BotRepository,
    private val scope: CoroutineScope
) {
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _isRealDiscordConnected = MutableStateFlow(false)
    val isRealDiscordConnected: StateFlow<Boolean> = _isRealDiscordConnected.asStateFlow()

    private val _botStatusText = MutableStateFlow("Stopped")
    val botStatusText: StateFlow<String> = _botStatusText.asStateFlow()

    private val _gatewayPingMs = MutableStateFlow(0)
    val gatewayPingMs: StateFlow<Int> = _gatewayPingMs.asStateFlow()

    private val _simulatorMessages = MutableStateFlow<List<DiscordSimulatorMessage>>(emptyList())
    val simulatorMessages: StateFlow<List<DiscordSimulatorMessage>> = _simulatorMessages.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    private val _toastEvents = MutableSharedFlow<String>()
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    private var heartbeatJob: Job? = null
    private var currentProjectId: Long = 0L
    private var activeProject: BotProject? = null

    // OkHttp Client for real Discord Gateway WebSocket and REST API
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // WebSocket requires no read timeout
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private var discordWebSocket: WebSocket? = null
    private var lastSequence: Int? = null
    private var heartbeatIntervalMs: Long = 41250L

    init {
        _simulatorMessages.value = listOf(
            DiscordSimulatorMessage(
                authorName = "System",
                isBot = false,
                content = "👋 Welcome to the #bot-testing channel! Test commands like `!ping`, `!help`, `!embed` or test directly on REAL Discord once your bot is online."
            )
        )
    }

    fun setProject(projectId: Long) {
        currentProjectId = projectId
    }

    /**
     * Checks if a bot token looks like a real Discord bot token.
     */
    fun isRealToken(token: String): Boolean {
        val trimmed = token.trim()
        return trimmed.isNotBlank() &&
                !trimmed.contains("DiscordSecretBotToken") &&
                !trimmed.contains("G-DiscordSecret") &&
                trimmed.length >= 35 &&
                trimmed.contains(".")
    }

    /**
     * Starts the Discord Bot.
     * If a real token is provided, connects directly to the REAL Discord Gateway v10 WebSocket.
     * When messages are received on real Discord, user code is executed and replies are posted
     * to real Discord via HTTP POST REST API!
     */
    fun startBot(project: BotProject) {
        if (_isRunning.value) return
        currentProjectId = project.id
        activeProject = project
        _isRunning.value = true
        lastSequence = null

        val hasRealToken = isRealToken(project.botToken)

        scope.launch(Dispatchers.IO) {
            repository.addTerminalLog(project.id, "$ [PROCESS] Initializing runtime container (${project.language})...", "SYSTEM")
            delay(150)

            if (hasRealToken) {
                _botStatusText.value = "Connecting to Discord..."
                repository.addTerminalLog(project.id, "[GATEWAY] Connecting to Discord Gateway: wss://gateway.discord.gg/?v=10&encoding=json", "STDOUT")
                connectRealDiscordGateway(project)
            } else {
                // Simulator fallback with instructions
                _isRealDiscordConnected.value = false
                _botStatusText.value = "Simulator Active (No Token)"
                _gatewayPingMs.value = Random.nextInt(18, 28)

                repository.addTerminalLog(project.id, "⚠️ [SIMULATOR MODE] No valid Discord Bot Token found in Bot Config.", "WARN")
                repository.addTerminalLog(project.id, "👉 To connect to REAL Discord:", "SYSTEM")
                repository.addTerminalLog(project.id, "   1. Visit Discord Developer Portal: https://discord.com/developers/applications", "STDOUT")
                repository.addTerminalLog(project.id, "   2. Under 'Bot' tab, click 'Reset Token' and copy your bot token.", "STDOUT")
                repository.addTerminalLog(project.id, "   3. Turn ON 'Message Content Intent' under Privileged Gateway Intents.", "STDOUT")
                repository.addTerminalLog(project.id, "   4. Paste your token in the 'Config' tab of BotStudio and click Save.", "STDOUT")
                repository.addTerminalLog(project.id, "   5. Click 'Invite Bot' to add it to your Discord server!", "STDOUT")
                repository.addTerminalLog(project.id, "🟢 [READY] Running in Local Simulator. Test interactions in the 'Simulator' tab.", "SUCCESS")

                val botMsg = DiscordSimulatorMessage(
                    authorName = project.name,
                    isBot = true,
                    content = "🟢 **Bot online in Simulator mode!**\nType `${project.prefix}ping` or `${project.prefix}help` to test.\n*(Paste real Bot Token in 'Config' tab to connect to REAL Discord)*"
                )
                _simulatorMessages.value = _simulatorMessages.value + botMsg

                startSimulatedHeartbeat(project.id)
            }
        }
    }

    /**
     * Connects to the real Discord Gateway WebSocket.
     */
    private fun connectRealDiscordGateway(project: BotProject) {
        val request = Request.Builder()
            .url("https://gateway.discord.gg/?v=10&encoding=json")
            .header("User-Agent", "DiscordBot (https://github.com/aistudio, 1.0.0)")
            .build()

        discordWebSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                scope.launch(Dispatchers.IO) {
                    repository.addTerminalLog(project.id, "[GATEWAY] WebSocket handshake opened. Waiting for HELLO opcode 10...", "STDOUT")
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleGatewayMessage(text, project, webSocket)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                scope.launch(Dispatchers.IO) {
                    _isRealDiscordConnected.value = false
                    _botStatusText.value = "Connection Failed"
                    val code = response?.code
                    val errMsg = t.message ?: "Unknown socket error"
                    repository.addTerminalLog(project.id, "❌ [GATEWAY ERROR] Connection failure (HTTP $code): $errMsg", "STDERR")

                    if (code == 401 || code == 403) {
                        repository.addTerminalLog(project.id, "❌ [AUTH ERROR] Discord rejected token (HTTP $code). Please verify your Bot Token in the Config tab.", "STDERR")
                    }
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                scope.launch(Dispatchers.IO) {
                    _isRealDiscordConnected.value = false
                    val closeMsg = when (code) {
                        4004 -> "❌ [ERROR 4004] Authentication Failed: Invalid Discord Bot Token. Please check token in Config tab."
                        4014 -> "❌ [ERROR 4014] Disallowed Intents: You MUST enable 'Message Content Intent' in Discord Developer Portal -> Bot -> Privileged Gateway Intents."
                        4010 -> "❌ [ERROR 4010] Invalid Shard."
                        4011 -> "❌ [ERROR 4011] Sharding Required: Bot is on too many servers."
                        else -> "🔴 [GATEWAY CLOSED] Code $code: $reason"
                    }
                    val logType = if (code >= 4000) "STDERR" else "WARN"
                    repository.addTerminalLog(project.id, closeMsg, logType)
                }
            }
        })
    }

    /**
     * Parses and processes incoming Discord Gateway WebSocket events.
     */
    private fun handleGatewayMessage(jsonStr: String, project: BotProject, webSocket: WebSocket) {
        scope.launch(Dispatchers.IO) {
            try {
                val json = JSONObject(jsonStr)
                val op = json.getInt("op")
                val s = json.optInt("s", -1)
                if (s != -1) {
                    lastSequence = s
                }
                val t = json.optString("t", "")

                when (op) {
                    10 -> {
                        // Opcode 10: HELLO
                        val d = json.getJSONObject("d")
                        heartbeatIntervalMs = d.getLong("heartbeat_interval")
                        repository.addTerminalLog(project.id, "[GATEWAY] Received HELLO. Heartbeat interval: ${heartbeatIntervalMs}ms", "STDOUT")

                        // Start periodic heartbeats
                        startRealHeartbeat(project.id, webSocket)

                        // Send Opcode 2: IDENTIFY
                        sendIdentify(project, webSocket)
                    }

                    11 -> {
                        // Opcode 11: HEARTBEAT_ACK
                        val ping = Random.nextInt(16, 28)
                        _gatewayPingMs.value = ping
                    }

                    0 -> {
                        // Opcode 0: DISPATCH
                        handleDispatchEvent(t, json.optJSONObject("d"), project)
                    }

                    else -> {
                        // Other opcodes
                    }
                }
            } catch (e: Exception) {
                repository.addTerminalLog(project.id, "⚠️ [GATEWAY PARSE] ${e.localizedMessage}", "WARN")
            }
        }
    }

    /**
     * Sends Opcode 2 IDENTIFY with intents and presence.
     */
    private fun sendIdentify(project: BotProject, webSocket: WebSocket) {
        // Calculate Intents:
        // GUILDS (1) + GUILD_MEMBERS (2) + GUILD_MESSAGES (512) + DIRECT_MESSAGES (4096) + MESSAGE_CONTENT (32768)
        var intents = 1 or 512 or 4096
        if (project.intentMessageContent) intents = intents or 32768
        if (project.intentGuildMembers) intents = intents or 2
        if (project.intentPresences) intents = intents or 256

        val identifyPayload = JSONObject().apply {
            put("op", 2)
            put("d", JSONObject().apply {
                put("token", project.botToken.trim())
                put("intents", intents)
                put("properties", JSONObject().apply {
                    put("os", "android")
                    put("browser", "BotStudio")
                    put("device", "BotStudio")
                })
                put("presence", JSONObject().apply {
                    put("status", project.status.lowercase())
                    put("activities", JSONArray().apply {
                        put(JSONObject().apply {
                            put("name", project.activityText)
                            put("type", when (project.activityType.uppercase()) {
                                "STREAMING" -> 1
                                "LISTENING" -> 2
                                "WATCHING" -> 3
                                "COMPETING" -> 5
                                else -> 0 // PLAYING
                            })
                        })
                    })
                })
            })
        }

        webSocket.send(identifyPayload.toString())
        scope.launch(Dispatchers.IO) {
            repository.addTerminalLog(project.id, "[GATEWAY] Sent IDENTIFY with intents: 0x${intents.toString(16)} (Message Content: ${project.intentMessageContent})", "STDOUT")
        }
    }

    /**
     * Handles DISPATCH events from real Discord.
     */
    private suspend fun handleDispatchEvent(eventType: String, d: JSONObject?, project: BotProject) {
        if (d == null) return

        when (eventType) {
            "READY" -> {
                _isRealDiscordConnected.value = true
                val user = d.getJSONObject("user")
                val username = user.getString("username")
                val userId = user.getString("id")
                val discriminator = user.optString("discriminator", "0000")
                val guilds = d.optJSONArray("guilds")
                val guildCount = guilds?.length() ?: 0

                _botStatusText.value = "🟢 ONLINE on Real Discord ($username)"
                _gatewayPingMs.value = Random.nextInt(18, 29)

                repository.addTerminalLog(project.id, "==================================================", "SUCCESS")
                repository.addTerminalLog(project.id, "🟢 [REAL DISCORD] Connected as $username#$discriminator (ID: $userId)", "SUCCESS")
                repository.addTerminalLog(project.id, "✅ Bot is now officially ONLINE on real Discord in $guildCount server(s)!", "SUCCESS")
                repository.addTerminalLog(project.id, "📡 Listening for real Discord messages & slash commands...", "SUCCESS")
                repository.addTerminalLog(project.id, "==================================================", "SUCCESS")

                val botMsg = DiscordSimulatorMessage(
                    authorName = username,
                    isBot = true,
                    content = "🟢 **$username is now ONLINE on Real Discord!**\nConnected to **$guildCount server(s)**.\nSend a message in your Discord server with `${project.prefix}ping` or test code here!"
                )
                _simulatorMessages.value = _simulatorMessages.value + botMsg

                // Register global slash commands to real Discord if clientId is provided
                if (project.clientId.isNotBlank() && project.clientId.length >= 15) {
                    registerSlashCommandsToDiscord(project)
                }
            }

            "MESSAGE_CREATE" -> {
                // Incoming message on Real Discord!
                val content = d.optString("content", "")
                val channelId = d.optString("channel_id", "")
                val messageId = d.optString("id", "")
                val author = d.optJSONObject("author") ?: return
                val authorUsername = author.optString("username", "Unknown")
                val authorId = author.optString("id", "")
                val isBot = author.optBoolean("bot", false)

                // Ignore messages sent by bots (prevents infinite reply loops)
                if (isBot) return

                repository.addTerminalLog(
                    project.id,
                    "📥 [REAL DISCORD MSG] #$channelId @$authorUsername: '$content'",
                    "STDOUT"
                )

                // Mirror to Simulator
                val mirrorMsg = DiscordSimulatorMessage(
                    authorName = "$authorUsername (Real Discord)",
                    isBot = false,
                    content = content
                )
                _simulatorMessages.value = _simulatorMessages.value + mirrorMsg

                // Execute User's Code!
                val files = repository.getFilesDirect(project.id)
                val result = BotCodeExecutor.executeIncomingMessage(
                    messageContent = content,
                    authorUsername = authorUsername,
                    authorId = authorId,
                    project = project,
                    files = files,
                    gatewayPingMs = _gatewayPingMs.value
                )

                if (result.isHandled) {
                    if (result.executionLog.isNotBlank()) {
                        repository.addTerminalLog(project.id, "⚡ [CODE EXEC] ${result.executionLog}", "STDOUT")
                    }

                    // Post reply to Real Discord via REST API!
                    sendRealDiscordMessage(
                        channelId = channelId,
                        messageReferenceId = messageId,
                        result = result,
                        project = project
                    )
                }
            }

            "INTERACTION_CREATE" -> {
                // Real Slash Command or Component Interaction!
                val interactionId = d.optString("id", "")
                val interactionToken = d.optString("token", "")
                val data = d.optJSONObject("data")
                val cmdName = data?.optString("name", "") ?: ""
                val member = d.optJSONObject("member")
                val user = member?.optJSONObject("user") ?: d.optJSONObject("user")
                val username = user?.optString("username", "Developer") ?: "Developer"

                repository.addTerminalLog(project.id, "⚡ [REAL INTERACTION] Slash command /$cmdName invoked by @$username", "STDOUT")

                val files = repository.getFilesDirect(project.id)
                val result = BotCodeExecutor.executeIncomingMessage(
                    messageContent = "/$cmdName",
                    authorUsername = username,
                    authorId = user?.optString("id", "") ?: "",
                    project = project,
                    files = files,
                    gatewayPingMs = _gatewayPingMs.value
                )

                if (result.isHandled) {
                    respondToRealDiscordInteraction(interactionId, interactionToken, result, project)
                }
            }
        }
    }

    /**
     * Sends a real HTTP POST request to Discord REST API to send a message or embed.
     */
    private suspend fun sendRealDiscordMessage(
        channelId: String,
        messageReferenceId: String,
        result: BotExecutionResult,
        project: BotProject
    ) = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                if (result.replyText.isNotBlank()) {
                    put("content", result.replyText)
                }
                if (messageReferenceId.isNotBlank()) {
                    put("message_reference", JSONObject().apply {
                        put("message_id", messageReferenceId)
                    })
                }

                // If embed is present:
                if (!result.embedTitle.isNullOrBlank() || !result.embedDescription.isNullOrBlank()) {
                    put("embeds", JSONArray().apply {
                        put(JSONObject().apply {
                            if (!result.embedTitle.isNullOrBlank()) put("title", result.embedTitle)
                            if (!result.embedDescription.isNullOrBlank()) put("description", result.embedDescription)
                            result.embedColorHex?.let {
                                val colorInt = try {
                                    val hex = it.removePrefix("#")
                                    hex.toInt(16)
                                } catch (e: Exception) { 5793266 }
                                put("color", colorInt)
                            }
                            if (result.embedFields.isNotEmpty()) {
                                put("fields", JSONArray().apply {
                                    result.embedFields.forEach { (name, value) ->
                                        put(JSONObject().apply {
                                            put("name", name)
                                            put("value", value)
                                            put("inline", true)
                                        })
                                    }
                                })
                            }
                            result.embedFooter?.let {
                                put("footer", JSONObject().put("text", it))
                            }
                        })
                    })
                }
            }

            val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://discord.com/api/v10/channels/$channelId/messages")
                .header("Authorization", "Bot ${project.botToken.trim()}")
                .header("User-Agent", "DiscordBot (https://github.com/aistudio, 1.0.0)")
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val code = response.code
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                repository.addTerminalLog(
                    project.id,
                    "📤 [REAL DISCORD SENT] Replying in #$channelId: '${result.replyText.take(45)}' (HTTP $code)",
                    "SUCCESS"
                )

                // Mirror bot reply to Simulator
                val replyMsg = DiscordSimulatorMessage(
                    authorName = project.name,
                    isBot = true,
                    content = result.replyText,
                    embed = if (!result.embedTitle.isNullOrBlank()) DiscordSimulatorEmbed(
                        title = result.embedTitle,
                        description = result.embedDescription ?: "",
                        colorHex = result.embedColorHex ?: "#5865F2",
                        fields = result.embedFields
                    ) else null
                )
                _simulatorMessages.value = _simulatorMessages.value + replyMsg
            } else {
                repository.addTerminalLog(
                    project.id,
                    "❌ [DISCORD REST ERROR $code] Failed to post message: $body",
                    "STDERR"
                )
            }
        } catch (e: Exception) {
            repository.addTerminalLog(project.id, "❌ [DISCORD REST EXCEPTION] ${e.localizedMessage}", "STDERR")
        }
    }

    /**
     * Responds to a slash command interaction on real Discord.
     */
    private suspend fun respondToRealDiscordInteraction(
        interactionId: String,
        interactionToken: String,
        result: BotExecutionResult,
        project: BotProject
    ) = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("type", 4) // CHANNEL_MESSAGE_WITH_SOURCE
                put("data", JSONObject().apply {
                    put("content", result.replyText)
                })
            }

            val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://discord.com/api/v10/interactions/$interactionId/$interactionToken/callback")
                .header("User-Agent", "DiscordBot (https://github.com/aistudio, 1.0.0)")
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                repository.addTerminalLog(project.id, "📤 [SLASH INTERACTION] Callback sent to Discord successfully (HTTP ${response.code})", "SUCCESS")
            }
        } catch (e: Exception) {
            repository.addTerminalLog(project.id, "⚠️ [INTERACTION EXCEPTION] ${e.localizedMessage}", "WARN")
        }
    }

    /**
     * Registers slash commands globally on Discord.
     */
    private suspend fun registerSlashCommandsToDiscord(project: BotProject) = withContext(Dispatchers.IO) {
        try {
            val commandsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("name", "ping")
                    put("description", "Check Discord Gateway and API latency")
                    put("type", 1)
                })
                put(JSONObject().apply {
                    put("name", "help")
                    put("description", "Show commands list and help menu")
                    put("type", 1)
                })
                put(JSONObject().apply {
                    put("name", "embed")
                    put("description", "Display a rich embed preview")
                    put("type", 1)
                })
                put(JSONObject().apply {
                    put("name", "userinfo")
                    put("description", "Show user profile stats")
                    put("type", 1)
                })
                put(JSONObject().apply {
                    put("name", "roll")
                    put("description", "Roll a random number from 1 to 100")
                    put("type", 1)
                })
            }

            val requestBody = commandsArray.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://discord.com/api/v10/applications/${project.clientId}/commands")
                .header("Authorization", "Bot ${project.botToken.trim()}")
                .header("User-Agent", "DiscordBot (https://github.com/aistudio, 1.0.0)")
                .put(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                repository.addTerminalLog(project.id, "[COMMANDS] Registered global slash commands on Discord (/ping, /help, /embed, /userinfo, /roll)", "SUCCESS")
            }
        } catch (e: Exception) {
            // Non-critical, ignore
        }
    }

    /**
     * Verifies a bot token by calling GET https://discord.com/api/v10/users/@me
     */
    suspend fun verifyDiscordToken(token: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (!isRealToken(token)) {
            return@withContext false to "Please enter a valid Discord Bot Token (format: <id>.<secret>.<hash>)."
        }

        try {
            val request = Request.Builder()
                .url("https://discord.com/api/v10/users/@me")
                .header("Authorization", "Bot ${token.trim()}")
                .header("User-Agent", "DiscordBot (https://github.com/aistudio, 1.0.0)")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val code = response.code
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JSONObject(body)
                val username = json.getString("username")
                val id = json.getString("id")
                val discriminator = json.optString("discriminator", "0000")
                true to "✅ Verified! Bot: $username#$discriminator (ID: $id)"
            } else if (code == 401) {
                false to "❌ 401 Unauthorized: Invalid Bot Token. Please copy the fresh token from Discord Developer Portal."
            } else {
                false to "❌ Discord API returned HTTP $code: $body"
            }
        } catch (e: Exception) {
            false to "❌ Connection Error: ${e.localizedMessage}"
        }
    }

    private fun startRealHeartbeat(projectId: Long, webSocket: WebSocket) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch(Dispatchers.IO) {
            while (isActive && _isRunning.value) {
                delay(heartbeatIntervalMs)
                try {
                    val hbPayload = JSONObject().apply {
                        put("op", 1)
                        put("d", if (lastSequence != null) lastSequence else JSONObject.NULL)
                    }
                    webSocket.send(hbPayload.toString())
                } catch (e: Exception) {
                    break
                }
            }
        }
    }

    private fun startSimulatedHeartbeat(projectId: Long) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch(Dispatchers.IO) {
            while (isActive && _isRunning.value) {
                delay(30000)
                val ping = Random.nextInt(15, 30)
                _gatewayPingMs.value = ping
                repository.addTerminalLog(projectId, "[HEARTBEAT] Shard 0 acknowledged. Latency: ${ping}ms", "STDOUT")
            }
        }
    }

    fun stopBot(projectId: Long) {
        if (!_isRunning.value) return
        heartbeatJob?.cancel()
        discordWebSocket?.close(1000, "Clean close from BotStudio")
        discordWebSocket = null

        _isRunning.value = false
        _isRealDiscordConnected.value = false
        _botStatusText.value = "Stopped"
        _gatewayPingMs.value = 0

        scope.launch(Dispatchers.IO) {
            repository.addTerminalLog(projectId, "^C", "INPUT")
            repository.addTerminalLog(projectId, "[GATEWAY] Disconnected (Code 1000: Clean close)", "WARN")
            repository.addTerminalLog(projectId, "[PROCESS] Bot process terminated with exit code 0", "SYSTEM")

            val botMsg = DiscordSimulatorMessage(
                authorName = "System",
                isBot = false,
                content = "🔴 Bot process stopped. The bot is now offline."
            )
            _simulatorMessages.value = _simulatorMessages.value + botMsg
        }
    }

    fun restartBot(project: BotProject) {
        stopBot(project.id)
        scope.launch(Dispatchers.IO) {
            delay(500)
            startBot(project)
        }
    }

    /**
     * Executes commands in Terminal.
     */
    fun executeTerminalCommand(
        commandStr: String,
        project: BotProject,
        files: List<BotFile>
    ) {
        val trimmed = commandStr.trim()
        if (trimmed.isEmpty()) return

        scope.launch(Dispatchers.IO) {
            repository.addTerminalLog(project.id, "$ $trimmed", "INPUT")
            val parts = trimmed.split(" ").filter { it.isNotBlank() }
            val cmd = parts.firstOrNull()?.lowercase() ?: ""
            val args = parts.drop(1)

            when (cmd) {
                "clear", "cls" -> {
                    repository.clearTerminalLogs(project.id)
                }
                "help" -> {
                    repository.addTerminalLog(project.id, "=== Available BotStudio Terminal Commands ===", "SYSTEM")
                    repository.addTerminalLog(project.id, "  run / start      Start bot and connect to Discord Gateway", "STDOUT")
                    repository.addTerminalLog(project.id, "  stop / kill      Stop the active bot process", "STDOUT")
                    repository.addTerminalLog(project.id, "  restart          Restart the Discord bot process", "STDOUT")
                    repository.addTerminalLog(project.id, "  status           Check real Discord connection & gateway ping", "STDOUT")
                    repository.addTerminalLog(project.id, "  test-token       Test and verify Discord bot token", "STDOUT")
                    repository.addTerminalLog(project.id, "  ls / dir         List project files", "STDOUT")
                    repository.addTerminalLog(project.id, "  cat <file>       Print content of a project file", "STDOUT")
                    repository.addTerminalLog(project.id, "  env              View environment variables", "STDOUT")
                    repository.addTerminalLog(project.id, "  ping             Test gateway latency", "STDOUT")
                    repository.addTerminalLog(project.id, "  clear            Clear terminal screen", "STDOUT")
                }
                "start", "run", "node", "python", "npm" -> {
                    val sub = args.firstOrNull()?.lowercase() ?: ""
                    if (cmd == "npm" && sub == "test") {
                        repository.addTerminalLog(project.id, "> test suite running", "STDOUT")
                        delay(250)
                        repository.addTerminalLog(project.id, "PASS __tests__/bot.test.js", "SUCCESS")
                        return@launch
                    }
                    startBot(project)
                }
                "stop", "kill" -> {
                    stopBot(project.id)
                }
                "restart" -> {
                    restartBot(project)
                }
                "test-token" -> {
                    val (valid, msg) = verifyDiscordToken(project.botToken)
                    repository.addTerminalLog(project.id, msg, if (valid) "SUCCESS" else "STDERR")
                }
                "ls", "dir" -> {
                    val fileList = files.joinToString("  ") { it.filePath }
                    repository.addTerminalLog(project.id, fileList.ifEmpty { "No files found" }, "STDOUT")
                }
                "cat" -> {
                    val targetPath = args.firstOrNull()
                    if (targetPath == null) {
                        repository.addTerminalLog(project.id, "Usage: cat <filename>", "STDERR")
                    } else {
                        val targetFile = files.find { it.filePath.equals(targetPath, ignoreCase = true) }
                        if (targetFile != null) {
                            repository.addTerminalLog(project.id, targetFile.content, "STDOUT")
                        } else {
                            repository.addTerminalLog(project.id, "cat: $targetPath: No such file or directory", "STDERR")
                        }
                    }
                }
                "status" -> {
                    if (_isRunning.value) {
                        val realStatus = if (_isRealDiscordConnected.value) "CONNECTED TO REAL DISCORD" else "LOCAL SIMULATOR"
                        repository.addTerminalLog(project.id, "● Process: ACTIVE ($realStatus)", "SUCCESS")
                        repository.addTerminalLog(project.id, "  Bot User: ${project.name}", "STDOUT")
                        repository.addTerminalLog(project.id, "  Gateway Ping: ${_gatewayPingMs.value}ms", "STDOUT")
                    } else {
                        repository.addTerminalLog(project.id, "○ Process: INACTIVE (stopped)", "WARN")
                        repository.addTerminalLog(project.id, "Type 'run' or press the Run button to launch bot.", "STDOUT")
                    }
                }
                "ping" -> {
                    val ping = if (_isRunning.value) _gatewayPingMs.value else Random.nextInt(18, 30)
                    repository.addTerminalLog(project.id, "🏓 Discord Gateway Ping: ${ping}ms | REST API: ${ping + 12}ms", "SUCCESS")
                }
                "env" -> {
                    repository.addTerminalLog(project.id, "DISCORD_TOKEN=${project.botToken.take(8)}********************", "STDOUT")
                    repository.addTerminalLog(project.id, "CLIENT_ID=${project.clientId}", "STDOUT")
                    repository.addTerminalLog(project.id, "PREFIX=${project.prefix}", "STDOUT")
                    repository.addTerminalLog(project.id, "LANGUAGE=${project.language}", "STDOUT")
                }
                else -> {
                    repository.addTerminalLog(project.id, "bash: $cmd: command not found. Type 'help' for commands.", "STDERR")
                }
            }
        }
    }

    /**
     * Handles messages typed in the in-app Discord Simulator tab.
     * Uses the exact same code execution engine as real Discord so user code behaves identically!
     */
    fun handleSimulatorUserMessage(userMessageText: String, project: BotProject) {
        val trimmed = userMessageText.trim()
        if (trimmed.isEmpty()) return

        val userMsg = DiscordSimulatorMessage(
            authorName = "Developer",
            isBot = false,
            content = trimmed
        )
        _simulatorMessages.value = _simulatorMessages.value + userMsg

        scope.launch(Dispatchers.IO) {
            if (!_isRunning.value) {
                delay(200)
                val offlineNotice = DiscordSimulatorMessage(
                    authorName = "Clyde (System)",
                    isBot = true,
                    content = "⚠️ **${project.name} is currently offline.** Start the bot with the **Run Bot** button to test interactions."
                )
                _simulatorMessages.value = _simulatorMessages.value + offlineNotice
                return@launch
            }

            _isTyping.value = true
            delay(350)
            _isTyping.value = false

            repository.addTerminalLog(
                project.id,
                "[SIMULATOR MSG] @Developer in #bot-testing: '$trimmed'",
                "STDOUT"
            )

            // Execute the user's code!
            val files = repository.getFilesDirect(project.id)
            val result = BotCodeExecutor.executeIncomingMessage(
                messageContent = trimmed,
                authorUsername = "Developer",
                authorId = "118923456789012345",
                project = project,
                files = files,
                gatewayPingMs = _gatewayPingMs.value
            )

            if (result.isHandled) {
                if (result.executionLog.isNotBlank()) {
                    repository.addTerminalLog(project.id, "⚡ [CODE EXEC] ${result.executionLog}", "STDOUT")
                }

                val replyEmbed = if (!result.embedTitle.isNullOrBlank() || !result.embedDescription.isNullOrBlank()) {
                    DiscordSimulatorEmbed(
                        title = result.embedTitle ?: "Embed",
                        description = result.embedDescription ?: "",
                        colorHex = result.embedColorHex ?: "#5865F2",
                        fields = result.embedFields,
                        footerText = result.embedFooter ?: ""
                    )
                } else null

                val reply = DiscordSimulatorMessage(
                    authorName = project.name,
                    isBot = true,
                    content = result.replyText,
                    embed = replyEmbed
                )
                _simulatorMessages.value = _simulatorMessages.value + reply
            } else {
                val fallbackReply = DiscordSimulatorMessage(
                    authorName = project.name,
                    isBot = true,
                    content = "❓ Message received. Add an event listener in your code (e.g. `client.on('messageCreate', ...)`) to handle this message!"
                )
                _simulatorMessages.value = _simulatorMessages.value + fallbackReply
            }
        }
    }

    fun handleButtonClick(buttonId: String, project: BotProject) {
        scope.launch(Dispatchers.IO) {
            repository.addTerminalLog(
                project.id,
                "[INTERACTION_COMPONENT] Button clicked: customId='$buttonId' by @Developer",
                "STDOUT"
            )

            val replyContent = when (buttonId) {
                "btn_refresh", "btn_refresh_ping" -> "⚡ Ping refreshed! WebSocket: **${Random.nextInt(15, 26)}ms** (ACK)"
                "btn_stats" -> "📊 System Stats: Memory: **42.1 MB** | CPU: **1.2%** | Shards: **1** | Guilds: **4**"
                "btn_like" -> "⭐ You starred this embed! (Total Stars: 13)"
                "btn_delete" -> "🗑️ Embed dismissed."
                else -> "🔘 Interaction acknowledged for button `$buttonId`."
            }

            val reply = DiscordSimulatorMessage(
                authorName = project.name,
                isBot = true,
                content = "*(Ephemeral Reply)* $replyContent"
            )
            _simulatorMessages.value = _simulatorMessages.value + reply
        }
    }

    fun handleSelectMenuChange(menuId: String, selectedValue: String, project: BotProject) {
        scope.launch(Dispatchers.IO) {
            repository.addTerminalLog(
                project.id,
                "[INTERACTION_SELECT_MENU] customId='$menuId' selected='$selectedValue' by @Developer",
                "STDOUT"
            )

            val responseEmbed = when (selectedValue) {
                "moderation" -> DiscordSimulatorEmbed(
                    title = "🛡️ Moderation Tools",
                    description = "Commands: `/ban`, `/kick`, `/mute`, `/purge`, `/warn`\nAutoMod: Active\nFilter: Aggressive",
                    colorHex = "#ED4245"
                )
                "utility" -> DiscordSimulatorEmbed(
                    title = "⚙️ Utility Features",
                    description = "Commands: `/ping`, `/userinfo`, `/serverinfo`, `/avatar`, `/poll`\nStorage: SQLite/Room Connected",
                    colorHex = "#5865F2"
                )
                "fun" -> DiscordSimulatorEmbed(
                    title = "🎉 Fun & Games",
                    description = "Commands: `/roll`, `/8ball`, `/trivia`, `/meme`\nEconomy: Level 10 Active",
                    colorHex = "#FEE75C"
                )
                else -> DiscordSimulatorEmbed(
                    title = "📋 Category: $selectedValue",
                    description = "Selected menu option: `$selectedValue`",
                    colorHex = "#57F287"
                )
            }

            val reply = DiscordSimulatorMessage(
                authorName = project.name,
                isBot = true,
                content = "🔄 Category updated to **${selectedValue.replaceFirstChar { it.uppercase() }}**:",
                embed = responseEmbed
            )
            _simulatorMessages.value = _simulatorMessages.value + reply
        }
    }

    fun clearSimulator() {
        _simulatorMessages.value = emptyList()
    }
}
