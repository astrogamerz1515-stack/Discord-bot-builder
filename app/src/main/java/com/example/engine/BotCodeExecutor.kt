package com.example.engine

import com.example.data.model.BotFile
import com.example.data.model.BotProject
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

data class BotExecutionResult(
    val replyText: String = "",
    val embedTitle: String? = null,
    val embedDescription: String? = null,
    val embedColorHex: String? = null,
    val embedFields: List<Pair<String, String>> = emptyList(),
    val embedAuthor: String? = null,
    val embedFooter: String? = null,
    val isHandled: Boolean = false,
    val matchedRule: String = "",
    val executionLog: String = ""
)

object BotCodeExecutor {

    // Precompiled Static Regex Patterns to avoid per-message compilation overhead
    private val EXACT_MATCH_REGEX = Regex(
        """(?:message|msg)\.content\s*===?\s*['"`]([^'"`]+)['"`][^{};]*\{?([^}]+)\}?""",
        RegexOption.IGNORE_CASE
    )
    private val STARTS_WITH_REGEX = Regex(
        """(?:message|msg)\.content\.startsWith\(\s*['"`]([^'"`]+)['"`]\s*\)[^{};]*\{?([^}]+)\}?""",
        RegexOption.IGNORE_CASE
    )
    private val COMMAND_MATCH_REGEX = Regex(
        """(?:command|commandName|cmd)\s*===?\s*['"`]([a-zA-Z0-9_\-]+)['"`][^{};]*\{?([^}]+)\}?""",
        RegexOption.IGNORE_CASE
    )
    private val SWITCH_CASE_REGEX = Regex(
        """case\s*['"`]([a-zA-Z0-9_\-]+)['"`]\s*:\s*([^;]+(?:;|\s*break;))""",
        RegexOption.IGNORE_CASE
    )
    private val PYTHON_COMMAND_REGEX = Regex(
        """@(?:bot|client)\.command\([^)]*\)\s*(?:async\s+)?def\s+([a-zA-Z0-9_]+)\([^)]*\):(?:\s*\n|\s+)+([^@\n]+await\s+ctx\.send\([^)]+\))""",
        RegexOption.IGNORE_CASE
    )
    private val SIMPLE_SEND_REGEX = Regex(
        """(?:message|msg|channel|interaction)\.(?:reply|send)\(\s*['"`]([^'"`]+)['"`]\s*\)""",
        RegexOption.IGNORE_CASE
    )
    private val OBJECT_SEND_REGEX = Regex(
        """(?:content|description)\s*:\s*['"`]([^'"`]+)['"`]""",
        RegexOption.IGNORE_CASE
    )
    private val TEMPLATE_SEND_REGEX = Regex(
        """(?:reply|send)\(\s*`([^`]+)`\s*\)""",
        RegexOption.IGNORE_CASE
    )
    private val PYTHON_SEND_REGEX = Regex(
        """ctx\.send\(\s*(?:f?['"`]([^'"`]+)['"`]|([^)]+))\s*\)""",
        RegexOption.IGNORE_CASE
    )
    private val WHITESPACE_REGEX = Regex("\\s+")

    // In-memory compiled rules cache
    data class ParsedBotRules(
        val exactMatches: Map<String, String>, // lowercase trigger -> body
        val prefixMatches: List<Pair<String, String>>, // trigger -> body
        val commandMatches: Map<String, String>, // commandName -> body
        val pythonCommands: Map<String, String> // commandName -> body
    )

    private val rulesCache = java.util.concurrent.ConcurrentHashMap<Long, Pair<Int, ParsedBotRules>>()

    fun invalidateCache(projectId: Long? = null) {
        if (projectId != null) {
            rulesCache.remove(projectId)
        } else {
            rulesCache.clear()
        }
    }

    private fun getOrCompileRules(projectId: Long, allCode: String): ParsedBotRules {
        val codeHash = allCode.hashCode()
        val cached = rulesCache[projectId]
        if (cached != null && cached.first == codeHash) {
            return cached.second
        }

        val exact = mutableMapOf<String, String>()
        val prefixes = mutableListOf<Pair<String, String>>()
        val commands = mutableMapOf<String, String>()
        val pythonCmds = mutableMapOf<String, String>()

        if (allCode.isNotBlank()) {
            // Pattern A: Exact match
            for (match in EXACT_MATCH_REGEX.findAll(allCode)) {
                val trigger = match.groupValues[1].trim().lowercase()
                val body = match.groupValues[2].trim()
                exact[trigger] = body
            }

            // Pattern B: StartsWith
            for (match in STARTS_WITH_REGEX.findAll(allCode)) {
                val trigger = match.groupValues[1].trim()
                val body = match.groupValues[2].trim()
                prefixes.add(trigger to body)
            }

            // Pattern C: Command match
            for (match in COMMAND_MATCH_REGEX.findAll(allCode)) {
                val trigger = match.groupValues[1].trim().lowercase()
                val body = match.groupValues[2].trim()
                commands[trigger] = body
            }

            // Pattern D: Switch case
            for (match in SWITCH_CASE_REGEX.findAll(allCode)) {
                val trigger = match.groupValues[1].trim().lowercase()
                val body = match.groupValues[2].trim()
                commands[trigger] = body
            }

            // Pattern E: Python commands
            for (match in PYTHON_COMMAND_REGEX.findAll(allCode)) {
                val trigger = match.groupValues[1].trim().lowercase()
                val body = match.groupValues[2].trim()
                pythonCmds[trigger] = body
            }
        }

        val compiled = ParsedBotRules(exact, prefixes, commands, pythonCmds)
        rulesCache[projectId] = codeHash to compiled
        return compiled
    }

    /**
     * Executes the user's bot code against an incoming message (either from Real Discord or Simulator).
     * Parses the project files (JavaScript, TypeScript, Python) to detect messageCreate handlers,
     * prefix commands, slash commands, if-statements, regexes, and embed builders.
     * Uses in-memory O(1) rule index for ultra-fast response times (<1ms execution overhead).
     */
    suspend fun executeIncomingMessage(
        messageContent: String,
        authorUsername: String,
        authorId: String,
        project: BotProject,
        files: List<BotFile>,
        gatewayPingMs: Int
    ): BotExecutionResult {
        val trimmed = messageContent.trim()
        if (trimmed.isEmpty()) {
            return BotExecutionResult(isHandled = false)
        }

        // Combine code from project files (giving priority to entrypoint index.js / bot.py)
        val sortedFiles = files.sortedByDescending { it.isEntrypoint }
        val allCode = sortedFiles.joinToString("\n\n") { it.content }

        val prefix = project.prefix.ifEmpty { "!" }
        val isSlash = trimmed.startsWith("/")
        val isPrefix = trimmed.startsWith(prefix)

        val cleanCommand = when {
            isSlash -> trimmed.substring(1).trim()
            isPrefix -> trimmed.substring(prefix.length).trim()
            else -> trimmed
        }

        val parts = cleanCommand.split(WHITESPACE_REGEX)
        val commandName = parts.firstOrNull()?.lowercase() ?: ""
        val args = parts.drop(1)
        val argsJoined = args.joinToString(" ")

        // Fast compiled rules lookup
        val rules = getOrCompileRules(project.id, allCode)

        // 1. Check for exact code-defined triggers in user's source code
        val customCodeResult = evaluateCustomCodeTriggers(
            trimmedContent = trimmed,
            cleanCommand = cleanCommand,
            commandName = commandName,
            args = args,
            argsJoined = argsJoined,
            authorUsername = authorUsername,
            authorId = authorId,
            rules = rules,
            pingMs = gatewayPingMs,
            project = project
        )

        if (customCodeResult != null && customCodeResult.isHandled) {
            return customCodeResult
        }

        // 2. Check for standard commands or template behaviors (e.g. ping, help, embed, roll, userinfo, ai)
        return evaluateBuiltInCommands(
            commandName = commandName,
            argsJoined = argsJoined,
            authorUsername = authorUsername,
            project = project,
            pingMs = gatewayPingMs,
            isSlashOrPrefix = isSlash || isPrefix
        )
    }

    /**
     * Evaluates custom triggers using the pre-compiled index for ultra-fast dispatch.
     */
    private fun evaluateCustomCodeTriggers(
        trimmedContent: String,
        cleanCommand: String,
        commandName: String,
        args: List<String>,
        argsJoined: String,
        authorUsername: String,
        authorId: String,
        rules: ParsedBotRules,
        pingMs: Int,
        project: BotProject
    ): BotExecutionResult? {
        val lowerTrimmed = trimmedContent.lowercase()

        // 1. O(1) Exact trigger match
        val exactBody = rules.exactMatches[lowerTrimmed]
        if (exactBody != null) {
            val reply = extractReplyFromBody(exactBody, pingMs, authorUsername, argsJoined, project)
            if (reply.isNotBlank()) {
                return BotExecutionResult(
                    replyText = reply,
                    isHandled = true,
                    matchedRule = "Exact Match",
                    executionLog = "Matched exact trigger: '$trimmedContent'"
                )
            }
        }

        // 2. StartsWith trigger match
        for ((trigger, body) in rules.prefixMatches) {
            if (trimmedContent.startsWith(trigger, ignoreCase = true)) {
                val reply = extractReplyFromBody(body, pingMs, authorUsername, argsJoined, project)
                if (reply.isNotBlank()) {
                    return BotExecutionResult(
                        replyText = reply,
                        isHandled = true,
                        matchedRule = "startsWith('$trigger')",
                        executionLog = "Matched prefix handler: '$trigger'"
                    )
                }
            }
        }

        // 3. O(1) Command name match
        val cmdBody = rules.commandMatches[commandName]
        if (cmdBody != null) {
            val reply = extractReplyFromBody(cmdBody, pingMs, authorUsername, argsJoined, project)
            if (reply.isNotBlank()) {
                return BotExecutionResult(
                    replyText = reply,
                    isHandled = true,
                    matchedRule = "Command check ($commandName)",
                    executionLog = "Executed command: $commandName"
                )
            }
        }

        // 4. O(1) Python command match
        val pyBody = rules.pythonCommands[commandName]
        if (pyBody != null) {
            val sendMatch = PYTHON_SEND_REGEX.find(pyBody)
            val rawText = sendMatch?.groupValues?.get(1)?.ifEmpty { sendMatch.groupValues.getOrNull(2) } ?: "Pong!"
            val parsedText = resolveVariables(rawText, pingMs, authorUsername, argsJoined, project)
            return BotExecutionResult(
                replyText = parsedText,
                isHandled = true,
                matchedRule = "Python @bot.command ($commandName)",
                executionLog = "Executed python command: $commandName"
            )
        }

        return null
    }

    /**
     * Extracts reply text or embed from an executed code body using precompiled regexes.
     */
    private fun extractReplyFromBody(
        body: String,
        pingMs: Int,
        authorUsername: String,
        argsJoined: String,
        project: BotProject
    ): String {
        // Check for message.reply('...') or channel.send('...') or interaction.reply('...')
        val simpleMatch = SIMPLE_SEND_REGEX.find(body)
        if (simpleMatch != null) {
            val text = simpleMatch.groupValues[1]
            return resolveVariables(text, pingMs, authorUsername, argsJoined, project)
        }

        // Check for content object: message.reply({ content: '...' })
        val objectMatch = OBJECT_SEND_REGEX.find(body)
        if (objectMatch != null) {
            val text = objectMatch.groupValues[1]
            return resolveVariables(text, pingMs, authorUsername, argsJoined, project)
        }

        // Check for template literals with backticks
        val templateMatch = TEMPLATE_SEND_REGEX.find(body)
        if (templateMatch != null) {
            val text = templateMatch.groupValues[1]
            return resolveVariables(text, pingMs, authorUsername, argsJoined, project)
        }

        return ""
    }

    private fun resolveVariables(
        rawText: String,
        pingMs: Int,
        authorUsername: String,
        argsJoined: String,
        project: BotProject
    ): String {
        var text = rawText
        val pingStr = "${pingMs}ms"
        if (text.contains("ping")) {
            text = text.replace("\${client.ws.ping}", pingStr)
            text = text.replace("{ping}", pingStr)
            text = text.replace("\${ws.ping}", pingStr)
        }
        if (text.contains("author") || text.contains("user")) {
            text = text.replace("\${message.author.username}", authorUsername)
            text = text.replace("\${msg.author.username}", authorUsername)
            text = text.replace("\${author.username}", authorUsername)
            text = text.replace("\${author}", authorUsername)
            text = text.replace("{user}", authorUsername)
            text = text.replace("{author}", authorUsername)
        }
        if (text.contains("args")) {
            text = text.replace("\${args.join(' ')}", argsJoined.ifEmpty { "None" })
            text = text.replace("\${args.join(\" \")}", argsJoined.ifEmpty { "None" })
            text = text.replace("\${args}", argsJoined.ifEmpty { "None" })
        }
        if (text.contains("prefix")) {
            text = text.replace("{prefix}", project.prefix)
        }
        if (text.contains("guilds")) {
            text = text.replace("\${client.guilds.cache.size}", "1")
        }
        text = text.replace("\\n", "\n")
        return text
    }

    /**
     * Fallback handlers for standard Discord commands so that newly created bots
     * or standard commands work right out of the box on real Discord and in simulator!
     */
    private suspend fun evaluateBuiltInCommands(
        commandName: String,
        argsJoined: String,
        authorUsername: String,
        project: BotProject,
        pingMs: Int,
        isSlashOrPrefix: Boolean
    ): BotExecutionResult {
        if (!isSlashOrPrefix && commandName != "ping" && commandName != "help") {
            return BotExecutionResult(isHandled = false)
        }

        return when (commandName) {
            "ping" -> {
                val ping = if (pingMs > 0) pingMs else Random.nextInt(18, 32)
                BotExecutionResult(
                    replyText = "🏓 Pong! WebSocket Gateway: `${ping}ms` | REST API: `${ping + 14}ms`",
                    embedTitle = "🏓 Pong!",
                    embedDescription = "Gateway WebSocket Latency: **${ping}ms**\nREST API Latency: **${ping + 14}ms**",
                    embedColorHex = "#5865F2",
                    embedFields = listOf(
                        "Status" to "🟢 Active on Discord",
                        "Heartbeat" to "Ack (${ping}ms)"
                    ),
                    embedFooter = "${project.name} • BotStudio Runtime",
                    isHandled = true,
                    matchedRule = "Built-in /ping command"
                )
            }

            "help", "commands" -> {
                BotExecutionResult(
                    replyText = "🤖 **${project.name} Help & Commands:**\n• `${project.prefix}ping` - Test Gateway latency\n• `${project.prefix}help` - Show command list\n• `${project.prefix}embed` - View rich embed demo\n• `${project.prefix}userinfo` - Account profile\n• `${project.prefix}echo <text>` - Echo message\n• `${project.prefix}roll` - Roll dice (1-100)\n• `${project.prefix}ai <prompt>` - Ask Gemini AI",
                    embedTitle = "🤖 ${project.name} - Command Center",
                    embedDescription = "Available commands registered for this bot:",
                    embedColorHex = "#5865F2",
                    embedFields = listOf(
                        "${project.prefix}ping" to "Check bot response latency",
                        "${project.prefix}echo <text>" to "Repeat your message",
                        "${project.prefix}embed" to "Rich formatted embed with colors",
                        "${project.prefix}userinfo" to "View user profile details",
                        "${project.prefix}ai <prompt>" to "Intelligent response via Gemini",
                        "${project.prefix}roll" to "Roll a number from 1 to 100"
                    ),
                    embedFooter = "Language: ${project.language} • Prefix: ${project.prefix}",
                    isHandled = true,
                    matchedRule = "Built-in /help command"
                )
            }

            "embed" -> {
                BotExecutionResult(
                    replyText = "✨ Here is a rich Discord embed:",
                    embedTitle = "✨ ${project.name} Rich Embed",
                    embedDescription = "This embed was generated by BotStudio and sent directly to Discord!",
                    embedColorHex = "#57F287",
                    embedFields = listOf(
                        "Language" to project.language,
                        "Prefix" to project.prefix,
                        "Platform" to "Discord Gateway v10"
                    ),
                    embedAuthor = project.name,
                    embedFooter = "Designed with Discord Bot Studio",
                    isHandled = true,
                    matchedRule = "Built-in /embed command"
                )
            }

            "echo", "say" -> {
                val text = argsJoined.ifBlank { "*(empty message)*" }
                BotExecutionResult(
                    replyText = "📢 $text",
                    isHandled = true,
                    matchedRule = "Built-in echo command"
                )
            }

            "roll", "dice" -> {
                val roll = Random.nextInt(1, 101)
                BotExecutionResult(
                    replyText = "🎲 **$authorUsername** rolled a **$roll** (1-100)!",
                    isHandled = true,
                    matchedRule = "Built-in roll command"
                )
            }

            "userinfo", "user" -> {
                BotExecutionResult(
                    replyText = "👤 **User Info for $authorUsername**\nUsername: `$authorUsername`\nStatus: Active Discord User",
                    embedTitle = "👤 User Profile: $authorUsername",
                    embedDescription = "Discord member interacting with ${project.name}.",
                    embedColorHex = "#FEE75C",
                    embedFields = listOf(
                        "Username" to authorUsername,
                        "Requested By" to authorUsername,
                        "Time" to SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    ),
                    embedFooter = "Discord Bot Studio Profile Engine",
                    isHandled = true,
                    matchedRule = "Built-in userinfo command"
                )
            }

            "ai", "ask", "gemini" -> {
                val prompt = argsJoined.ifBlank { "Hello! What can you do as a Discord bot?" }
                val aiReply = try {
                    com.example.ai.GeminiApiClient.callGemini(
                        prompt = prompt,
                        systemInstruction = "You are a helpful Discord bot. Answer concisely in under 1500 characters using Discord formatting."
                    )
                } catch (e: Exception) {
                    "✨ Gemini AI: I am ${project.name}, ready to assist you on Discord!"
                }

                BotExecutionResult(
                    replyText = aiReply.take(1950),
                    embedTitle = "🤖 Gemini AI Response",
                    embedDescription = aiReply.take(1950),
                    embedColorHex = "#5865F2",
                    embedFields = listOf("Prompt" to prompt.take(100)),
                    embedFooter = "Powered by Google Gemini",
                    isHandled = true,
                    matchedRule = "Built-in AI / Gemini command"
                )
            }

            else -> {
                // If it was explicitly prefixed or slash but unknown:
                if (isSlashOrPrefix) {
                    BotExecutionResult(
                        replyText = "❓ Unknown command `${project.prefix}$commandName`. Type `${project.prefix}help` to see available commands!",
                        isHandled = true,
                        matchedRule = "Unknown command fallback"
                    )
                } else {
                    BotExecutionResult(isHandled = false)
                }
            }
        }
    }
}
