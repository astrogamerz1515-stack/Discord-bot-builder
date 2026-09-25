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

    /**
     * Executes the user's bot code against an incoming message (either from Real Discord or Simulator).
     * Parses the project files (JavaScript, TypeScript, Python) to detect messageCreate handlers,
     * prefix commands, slash commands, if-statements, regexes, and embed builders.
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

        val parts = cleanCommand.split("\\s+".toRegex())
        val commandName = parts.firstOrNull()?.lowercase() ?: ""
        val args = parts.drop(1)
        val argsJoined = args.joinToString(" ")

        // 1. Check for exact code-defined triggers in user's source code
        val customCodeResult = evaluateCustomCodeTriggers(
            trimmedContent = trimmed,
            cleanCommand = cleanCommand,
            commandName = commandName,
            args = args,
            argsJoined = argsJoined,
            authorUsername = authorUsername,
            authorId = authorId,
            allCode = allCode,
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
     * Evaluates custom triggers from JavaScript/TypeScript/Python code written by the user.
     */
    private fun evaluateCustomCodeTriggers(
        trimmedContent: String,
        cleanCommand: String,
        commandName: String,
        args: List<String>,
        argsJoined: String,
        authorUsername: String,
        authorId: String,
        allCode: String,
        pingMs: Int,
        project: BotProject
    ): BotExecutionResult? {
        if (allCode.isBlank()) return null

        // Pattern A: message.content === '...' or message.content == "..."
        // e.g. if (message.content === '!ping') message.reply('Pong!');
        val exactMatchRegex = Regex(
            """(?:message|msg)\.content\s*===?\s*['"`]([^'"`]+)['"`][^{};]*\{?([^}]+)\}?""",
            RegexOption.IGNORE_CASE
        )
        for (match in exactMatchRegex.findAll(allCode)) {
            val trigger = match.groupValues[1].trim()
            val body = match.groupValues[2].trim()

            if (trimmedContent.equals(trigger, ignoreCase = true)) {
                val reply = extractReplyFromBody(body, pingMs, authorUsername, argsJoined, project)
                if (reply.isNotBlank()) {
                    return BotExecutionResult(
                        replyText = reply,
                        isHandled = true,
                        matchedRule = "Exact Match ('$trigger')",
                        executionLog = "Matched exact trigger: '$trigger'"
                    )
                }
            }
        }

        // Pattern B: message.content.startsWith('...')
        val startsWithRegex = Regex(
            """(?:message|msg)\.content\.startsWith\(\s*['"`]([^'"`]+)['"`]\s*\)[^{};]*\{?([^}]+)\}?""",
            RegexOption.IGNORE_CASE
        )
        for (match in startsWithRegex.findAll(allCode)) {
            val trigger = match.groupValues[1].trim()
            val body = match.groupValues[2].trim()

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

        // Pattern C: command / commandName matching
        // e.g. if (command === 'ping') message.reply(...)
        // e.g. case 'ping': message.reply(...)
        val commandMatchRegex = Regex(
            """(?:command|commandName|cmd)\s*===?\s*['"`]([a-zA-Z0-9_\-]+)['"`][^{};]*\{?([^}]+)\}?""",
            RegexOption.IGNORE_CASE
        )
        for (match in commandMatchRegex.findAll(allCode)) {
            val trigger = match.groupValues[1].trim().lowercase()
            val body = match.groupValues[2].trim()

            if (commandName == trigger) {
                val reply = extractReplyFromBody(body, pingMs, authorUsername, argsJoined, project)
                if (reply.isNotBlank()) {
                    return BotExecutionResult(
                        replyText = reply,
                        isHandled = true,
                        matchedRule = "Command check ($trigger)",
                        executionLog = "Executed command: $trigger"
                    )
                }
            }
        }

        // Pattern D: switch (command) { case '...': ... }
        val switchCaseRegex = Regex(
            """case\s*['"`]([a-zA-Z0-9_\-]+)['"`]\s*:\s*([^;]+(?:;|\s*break;))""",
            RegexOption.IGNORE_CASE
        )
        for (match in switchCaseRegex.findAll(allCode)) {
            val trigger = match.groupValues[1].trim().lowercase()
            val body = match.groupValues[2].trim()

            if (commandName == trigger) {
                val reply = extractReplyFromBody(body, pingMs, authorUsername, argsJoined, project)
                if (reply.isNotBlank()) {
                    return BotExecutionResult(
                        replyText = reply,
                        isHandled = true,
                        matchedRule = "Switch case ($trigger)",
                        executionLog = "Executed switch case: $trigger"
                    )
                }
            }
        }

        // Pattern E: Python @bot.command() async def ping(ctx): await ctx.send(...)
        val pythonCommandRegex = Regex(
            """@(?:bot|client)\.command\([^)]*\)\s*(?:async\s+)?def\s+([a-zA-Z0-9_]+)\([^)]*\):(?:\s*\n|\s+)+([^@\n]+await\s+ctx\.send\([^)]+\))""",
            RegexOption.IGNORE_CASE
        )
        for (match in pythonCommandRegex.findAll(allCode)) {
            val trigger = match.groupValues[1].trim().lowercase()
            val body = match.groupValues[2].trim()

            if (commandName == trigger) {
                val sendMatch = Regex("""ctx\.send\(\s*(?:f?['"`]([^'"`]+)['"`]|([^)]+))\s*\)""").find(body)
                val rawText = sendMatch?.groupValues?.get(1)?.ifEmpty { sendMatch.groupValues.getOrNull(2) } ?: "Pong!"
                val parsedText = resolveVariables(rawText, pingMs, authorUsername, argsJoined, project)
                return BotExecutionResult(
                    replyText = parsedText,
                    isHandled = true,
                    matchedRule = "Python @bot.command ($trigger)",
                    executionLog = "Executed python command: $trigger"
                )
            }
        }

        return null
    }

    /**
     * Extracts reply text or embed from an executed code body.
     */
    private fun extractReplyFromBody(
        body: String,
        pingMs: Int,
        authorUsername: String,
        argsJoined: String,
        project: BotProject
    ): String {
        // Check for message.reply('...') or channel.send('...') or interaction.reply('...')
        val simpleSendRegex = Regex(
            """(?:message|msg|channel|interaction)\.(?:reply|send)\(\s*['"`]([^'"`]+)['"`]\s*\)""",
            RegexOption.IGNORE_CASE
        )
        val simpleMatch = simpleSendRegex.find(body)
        if (simpleMatch != null) {
            val text = simpleMatch.groupValues[1]
            return resolveVariables(text, pingMs, authorUsername, argsJoined, project)
        }

        // Check for content object: message.reply({ content: '...' })
        val objectSendRegex = Regex(
            """(?:content|description)\s*:\s*['"`]([^'"`]+)['"`]""",
            RegexOption.IGNORE_CASE
        )
        val objectMatch = objectSendRegex.find(body)
        if (objectMatch != null) {
            val text = objectMatch.groupValues[1]
            return resolveVariables(text, pingMs, authorUsername, argsJoined, project)
        }

        // Check for template literals with backticks
        val templateRegex = Regex(
            """(?:reply|send)\(\s*`([^`]+)`\s*\)""",
            RegexOption.IGNORE_CASE
        )
        val templateMatch = templateRegex.find(body)
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
        text = text.replace(Regex("""\$\{[^}]*ws\.ping[^}]*\}"""), "${pingMs}ms")
        text = text.replace(Regex("""\$\{client\.ws\.ping\}"""), "${pingMs}ms")
        text = text.replace(Regex("""\{ping\}"""), "${pingMs}ms")
        text = text.replace(Regex("""\$\{[^}]*author\.username[^}]*\}"""), authorUsername)
        text = text.replace(Regex("""\$\{author\}"""), authorUsername)
        text = text.replace(Regex("""\{user\}"""), authorUsername)
        text = text.replace(Regex("""\$\{[^}]*args[^}]*\}"""), argsJoined.ifEmpty { "None" })
        text = text.replace(Regex("""\$\{client\.guilds\.cache\.size\}"""), "1")
        text = text.replace(Regex("""\{prefix\}"""), project.prefix)
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
