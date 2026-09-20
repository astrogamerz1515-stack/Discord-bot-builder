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

    init {
        // Initialize default welcome message in simulator
        _simulatorMessages.value = listOf(
            DiscordSimulatorMessage(
                authorName = "System",
                isBot = false,
                content = "👋 Welcome to the #bot-testing channel! Run your bot in the Terminal, then test slash commands like `/ping`, `/embed`, `/userinfo`, or prefix commands like `!help` here."
            )
        )
    }

    fun setProject(projectId: Long) {
        currentProjectId = projectId
    }

    fun startBot(project: BotProject) {
        if (_isRunning.value) return
        currentProjectId = project.id
        _isRunning.value = true
        _botStatusText.value = "Running (${project.language})"
        _gatewayPingMs.value = Random.nextInt(16, 29)

        scope.launch(Dispatchers.IO) {
            repository.addTerminalLog(project.id, "$ [PROCESS] Launching runtime container for ${project.language}...", "SYSTEM")
            delay(300)
            repository.addTerminalLog(project.id, "[GATEWAY] Connecting to wss://gateway.discord.gg/?v=10&encoding=json", "STDOUT")
            delay(400)
            repository.addTerminalLog(project.id, "[GATEWAY] Handshake complete. Shard 0/1 identified with intents: 0x${if (project.intentMessageContent) "983" else "21"}", "STDOUT")
            delay(300)
            repository.addTerminalLog(project.id, "[READY] Connected as ${project.name}#1337 (ID: ${project.clientId})", "SUCCESS")
            repository.addTerminalLog(project.id, "[PRESENCE] Status: ${project.status} | Activity: ${project.activityType} '${project.activityText}'", "SUCCESS")
            repository.addTerminalLog(project.id, "[COMMANDS] Registered global application commands: /ping, /embed, /userinfo, /help, /ban", "STDOUT")

            // Announce in simulator
            val botMsg = DiscordSimulatorMessage(
                authorName = project.name,
                isBot = true,
                content = "🟢 **Bot online!** Logged in as `${project.name}#1337`. Running ${project.activityType.lowercase()} **${project.activityText}**.\nType `/ping` or `${project.prefix}help` to test me!"
            )
            _simulatorMessages.value = _simulatorMessages.value + botMsg
        }

        startHeartbeat(project.id)
    }

    fun stopBot(projectId: Long) {
        if (!_isRunning.value) return
        heartbeatJob?.cancel()
        _isRunning.value = false
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
            delay(600)
            startBot(project)
        }
    }

    private fun startHeartbeat(projectId: Long) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch(Dispatchers.IO) {
            while (isActive && _isRunning.value) {
                delay(30000) // 30s heartbeat interval
                val ping = Random.nextInt(15, 34)
                _gatewayPingMs.value = ping
                repository.addTerminalLog(projectId, "[HEARTBEAT] Shard 0 acknowledged. WebSocket ping: ${ping}ms", "STDOUT")
            }
        }
    }

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
                    repository.addTerminalLog(project.id, "  node <file>      Run Node.js script or bot", "STDOUT")
                    repository.addTerminalLog(project.id, "  python <file>    Run Python script or bot", "STDOUT")
                    repository.addTerminalLog(project.id, "  npm start/dev    Start bot via package.json script", "STDOUT")
                    repository.addTerminalLog(project.id, "  npm install <p>  Simulate installing npm packages", "STDOUT")
                    repository.addTerminalLog(project.id, "  pip install <p>  Simulate installing pip packages", "STDOUT")
                    repository.addTerminalLog(project.id, "  start / run      Start the Discord bot process", "STDOUT")
                    repository.addTerminalLog(project.id, "  stop / kill      Stop the active bot process", "STDOUT")
                    repository.addTerminalLog(project.id, "  restart          Restart the Discord bot process", "STDOUT")
                    repository.addTerminalLog(project.id, "  status           Check bot runtime & gateway status", "STDOUT")
                    repository.addTerminalLog(project.id, "  ls / dir         List project files", "STDOUT")
                    repository.addTerminalLog(project.id, "  cat <file>       Print content of a project file", "STDOUT")
                    repository.addTerminalLog(project.id, "  env              View environment variables", "STDOUT")
                    repository.addTerminalLog(project.id, "  ping             Test gateway latency", "STDOUT")
                    repository.addTerminalLog(project.id, "  clear            Clear terminal screen", "STDOUT")
                }
                "start", "run" -> {
                    if (!com.example.packages.PackageManager.isLanguageInstalled(project.language)) {
                        repository.addTerminalLog(project.id, "⚠️ Runtime for ${project.language} is not installed! Go to the 'Install' tab to download the language pack (-8MB base APK savings).", "WARN")
                    } else {
                        startBot(project)
                    }
                }
                "stop", "kill" -> {
                    stopBot(project.id)
                }
                "restart" -> {
                    restartBot(project)
                }
                "node", "python", "cargo", "go", "ts-node" -> {
                    val lang = when (cmd) {
                        "python" -> "PYTHON"
                        "cargo" -> "RUST"
                        "go" -> "GO"
                        else -> "JAVASCRIPT"
                    }
                    if (!com.example.packages.PackageManager.isLanguageInstalled(lang)) {
                        repository.addTerminalLog(project.id, "⚠️ $cmd: command not found. Please install the $lang runtime pack from the 'Install' tab.", "STDERR")
                    } else {
                        startBot(project)
                    }
                }
                "npm" -> {
                    val sub = args.firstOrNull()?.lowercase() ?: ""
                    if (sub == "start" || sub == "run") {
                        startBot(project)
                    } else if (sub == "test" || sub == "t") {
                        repository.addTerminalLog(project.id, "> jest --colors --verbose", "STDOUT")
                        delay(400)
                        repository.addTerminalLog(project.id, " PASS  __tests__/commands.test.js", "SUCCESS")
                        repository.addTerminalLog(project.id, "  ✓ Slash command /ping responds with pong (14 ms)", "SUCCESS")
                        repository.addTerminalLog(project.id, "  ✓ Slash command /embed constructs rich payload (8 ms)", "SUCCESS")
                        repository.addTerminalLog(project.id, "  ✓ Gateway event interactionCreate dispatches correctly (4 ms)", "SUCCESS")
                        repository.addTerminalLog(project.id, "Tests:       3 passed, 3 total\nSnapshots:   0 total\nTime:        0.824 s", "SUCCESS")
                    } else if (sub == "install" || sub == "i") {
                        val pkg = args.getOrNull(1) ?: "dependencies"
                        repository.addTerminalLog(project.id, "npm WARN idealTree ready in 210ms", "WARN")
                        delay(500)
                        repository.addTerminalLog(project.id, "added 42 packages, and audited 180 packages in 1s", "SUCCESS")
                        repository.addTerminalLog(project.id, "found 0 vulnerabilities", "SUCCESS")
                    } else {
                        repository.addTerminalLog(project.id, "npm command executed successfully.", "STDOUT")
                    }
                }
                "pip" -> {
                    val sub = args.firstOrNull()?.lowercase() ?: ""
                    if (sub == "install") {
                        val pkg = args.getOrNull(1) ?: "-r requirements.txt"
                        repository.addTerminalLog(project.id, "Collecting $pkg...", "STDOUT")
                        delay(400)
                        repository.addTerminalLog(project.id, "Downloading $pkg (1.2 MB)...", "STDOUT")
                        delay(300)
                        repository.addTerminalLog(project.id, "Successfully installed $pkg", "SUCCESS")
                    }
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
                        repository.addTerminalLog(project.id, "● Process: ACTIVE (PID: 41829)", "SUCCESS")
                        repository.addTerminalLog(project.id, "  Bot User: ${project.name}#1337", "STDOUT")
                        repository.addTerminalLog(project.id, "  Gateway Ping: ${_gatewayPingMs.value}ms", "STDOUT")
                        repository.addTerminalLog(project.id, "  Memory Usage: 38.4 MB (Heap: 24.1 MB)", "STDOUT")
                        repository.addTerminalLog(project.id, "  Uptime: Active", "STDOUT")
                    } else {
                        repository.addTerminalLog(project.id, "○ Process: INACTIVE (stopped)", "WARN")
                        repository.addTerminalLog(project.id, "Type 'node index.js' or 'start' to launch bot.", "STDOUT")
                    }
                }
                "ping" -> {
                    val ping = if (_isRunning.value) _gatewayPingMs.value else Random.nextInt(18, 30)
                    repository.addTerminalLog(project.id, "🏓 Discord Gateway Ping: ${ping}ms | HTTP REST: ${ping + 12}ms", "SUCCESS")
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

    fun handleSimulatorUserMessage(userMessageText: String, project: BotProject) {
        val trimmed = userMessageText.trim()
        if (trimmed.isEmpty()) return

        // Post user message to chat
        val userMsg = DiscordSimulatorMessage(
            authorName = "Developer",
            isBot = false,
            content = trimmed
        )
        _simulatorMessages.value = _simulatorMessages.value + userMsg

        scope.launch(Dispatchers.IO) {
            // If bot is offline:
            if (!_isRunning.value) {
                delay(300)
                val offlineNotice = DiscordSimulatorMessage(
                    authorName = "Clyde (System)",
                    isBot = true,
                    content = "⚠️ **${project.name} is currently offline.** Start the bot in the **Terminal** tab or press the **Run Bot** button to test interactions."
                )
                _simulatorMessages.value = _simulatorMessages.value + offlineNotice
                return@launch
            }

            // Bot is online! Simulate typing:
            _isTyping.value = true
            delay(500)
            _isTyping.value = false

            // Log interaction in terminal
            repository.addTerminalLog(
                project.id,
                "[INTERACTION] Received message: '$trimmed' from @Developer in #bot-testing",
                "STDOUT"
            )

            // Parse command
            val isSlash = trimmed.startsWith("/")
            val cleanCmd = if (isSlash) trimmed.substring(1) else trimmed.removePrefix(project.prefix)
            val parts = cleanCmd.split(" ")
            val commandName = parts.firstOrNull()?.lowercase() ?: ""
            val commandArgs = parts.drop(1).joinToString(" ")

            when (commandName) {
                "ping" -> {
                    val ping = _gatewayPingMs.value.coerceAtLeast(18)
                    val pingEmbed = DiscordSimulatorEmbed(
                        title = "🏓 Pong!",
                        description = "Simulated Gateway WebSocket Latency: **${ping}ms**\nREST API Latency: **${ping + 14}ms**",
                        colorHex = "#5865F2",
                        fields = listOf(
                            "Status" to "🟢 Excellent",
                            "Memory" to "41.2 MB",
                            "Uptime" to "99.99%"
                        ),
                        footerText = "${project.name} • BotStudio Runtime"
                    )
                    val buttons = listOf(
                        DiscordSimulatorButton("btn_refresh", "Refresh Ping", "PRIMARY", "🔄"),
                        DiscordSimulatorButton("btn_stats", "System Stats", "SECONDARY", "📊")
                    )
                    val reply = DiscordSimulatorMessage(
                        authorName = project.name,
                        isBot = true,
                        embed = pingEmbed,
                        buttons = buttons
                    )
                    _simulatorMessages.value = _simulatorMessages.value + reply
                }

                "embed" -> {
                    val sampleEmbed = DiscordSimulatorEmbed(
                        title = "✨ Discord Bot Studio Embed",
                        description = "Here is a multi-field rich embed with Discord color bar, inline fields, and interactive action buttons.",
                        colorHex = "#57F287",
                        authorName = "BotStudio Designer",
                        fields = listOf(
                            "Language" to project.language,
                            "Prefix" to project.prefix,
                            "Intents" to "Guilds, Messages, Members"
                        ),
                        footerText = "Designed with Discord Bot Studio"
                    )
                    val buttons = listOf(
                        DiscordSimulatorButton("btn_like", "Like (12)", "SUCCESS", "⭐"),
                        DiscordSimulatorButton("btn_delete", "Dismiss", "DANGER", "🗑️")
                    )
                    val reply = DiscordSimulatorMessage(
                        authorName = project.name,
                        isBot = true,
                        embed = sampleEmbed,
                        buttons = buttons
                    )
                    _simulatorMessages.value = _simulatorMessages.value + reply
                }

                "userinfo", "user" -> {
                    val userEmbed = DiscordSimulatorEmbed(
                        title = "👤 User Profile: Developer",
                        description = "Member of the BotStudio Server since September 2024.",
                        colorHex = "#FEE75C",
                        fields = listOf(
                            "User ID" to "7489234891238912",
                            "Roles" to "🛡️ Administrator, 💻 Bot Developer",
                            "Badges" to "Active Developer, HypeSquad Bravery"
                        ),
                        footerText = "Requested by Developer"
                    )
                    val reply = DiscordSimulatorMessage(
                        authorName = project.name,
                        isBot = true,
                        embed = userEmbed
                    )
                    _simulatorMessages.value = _simulatorMessages.value + reply
                }

                "help", "menu" -> {
                    val helpEmbed = DiscordSimulatorEmbed(
                        title = "🤖 ${project.name} - Interactive Command Center",
                        description = "Select a command category from the drop-down menu below or try slash commands:",
                        colorHex = "#5865F2",
                        fields = listOf(
                            "/ping" to "Check WebSocket and API latency",
                            "/embed" to "Preview rich Discord embed with interactive buttons",
                            "/userinfo" to "View user account stats and roles",
                            "/ban <user>" to "Simulate moderation action",
                            "/roll" to "Roll a random number from 1 to 100",
                            "${project.prefix}echo <text>" to "Repeat back your message"
                        ),
                        footerText = "BotStudio Simulator • Select menu & action buttons active"
                    )
                    val selectMenu = DiscordSimulatorSelectMenu(
                        customId = "help_category_select",
                        placeholder = "Choose a command module...",
                        options = listOf(
                            DiscordSelectMenuOption("Moderation Module", "moderation", "Ban, kick, mute, auto-mod filters", "🛡️"),
                            DiscordSelectMenuOption("Utility & Storage", "utility", "Database KV, ping, user stats, server info", "⚙️"),
                            DiscordSelectMenuOption("Fun & Engagement", "fun", "Roll, trivia, economy, memes", "🎉")
                        )
                    )
                    val buttons = listOf(
                        DiscordSimulatorButton("btn_refresh", "Refresh Status", "PRIMARY", "🔄"),
                        DiscordSimulatorButton("btn_stats", "Cluster Stats", "SECONDARY", "📊")
                    )
                    val reply = DiscordSimulatorMessage(
                        authorName = project.name,
                        isBot = true,
                        embed = helpEmbed,
                        selectMenu = selectMenu,
                        buttons = buttons
                    )
                    _simulatorMessages.value = _simulatorMessages.value + reply
                }

                "ban", "kick" -> {
                    val target = if (commandArgs.isNotBlank()) commandArgs else "@Troublemaker"
                    val modEmbed = DiscordSimulatorEmbed(
                        title = "🔨 Moderation Action: ${commandName.uppercase()}",
                        description = "Successfully executed **$commandName** on **$target**.",
                        colorHex = "#ED4245",
                        fields = listOf(
                            "Reason" to "Violation of server rule 4 (Spam)",
                            "Moderator" to "Developer"
                        ),
                        footerText = "Audit log updated"
                    )
                    val reply = DiscordSimulatorMessage(
                        authorName = project.name,
                        isBot = true,
                        embed = modEmbed
                    )
                    _simulatorMessages.value = _simulatorMessages.value + reply
                }

                "roll" -> {
                    val result = Random.nextInt(1, 101)
                    val reply = DiscordSimulatorMessage(
                        authorName = project.name,
                        isBot = true,
                        content = "🎲 You rolled a **$result** (1-100)!"
                    )
                    _simulatorMessages.value = _simulatorMessages.value + reply
                }

                "echo" -> {
                    val reply = DiscordSimulatorMessage(
                        authorName = project.name,
                        isBot = true,
                        content = "📢 " + if (commandArgs.isNotBlank()) commandArgs else "*(Empty message)*"
                    )
                    _simulatorMessages.value = _simulatorMessages.value + reply
                }

                "ai", "ask", "gemini" -> {
                    val prompt = if (commandArgs.isNotBlank()) commandArgs else "Hello! What can you do as a Discord bot?"
                    val thinkingMsg = DiscordSimulatorMessage(
                        authorName = project.name,
                        isBot = true,
                        content = "✨ *Thinking with Gemini AI...*"
                    )
                    _simulatorMessages.value = _simulatorMessages.value + thinkingMsg

                    val aiText = com.example.ai.GeminiApiClient.callGemini(
                        prompt = prompt,
                        systemInstruction = "You are an intelligent Discord bot assistant. Answer helpfully and concisely using Discord markdown."
                    )

                    val aiEmbed = DiscordSimulatorEmbed(
                        title = "🤖 Gemini AI Response",
                        description = aiText.take(2000),
                        colorHex = "#5865F2",
                        fields = listOf("Prompt" to prompt.take(100)),
                        footerText = "Powered by Gemini 3.5 Flash"
                    )

                    val reply = DiscordSimulatorMessage(
                        authorName = project.name,
                        isBot = true,
                        embed = aiEmbed
                    )
                    _simulatorMessages.value = _simulatorMessages.value + reply
                }

                else -> {
                    val reply = DiscordSimulatorMessage(
                        authorName = project.name,
                        isBot = true,
                        content = "❓ Unknown command `$trimmed`. Type `/help` or `${project.prefix}help` to see available commands!"
                    )
                    _simulatorMessages.value = _simulatorMessages.value + reply
                }
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
                "btn_refresh" -> "⚡ Ping refreshed! WebSocket: **${Random.nextInt(15, 26)}ms** (ACK)"
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
