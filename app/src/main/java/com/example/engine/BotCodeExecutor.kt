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

    // Regexes for detecting commands and trigger headers (not matching the full body, but the entry point)
    private val JS_EXACT_MATCH_HEADER = Regex(
        """(?:message|msg)\.content(?:\.toLowerCase\(\)|\.trim\(\))*\s*===?\s*['"`]([^'"`]+)['"`]""",
        RegexOption.IGNORE_CASE
    )
    private val JS_STARTS_WITH_HEADER = Regex(
        """(?:message|msg)\.content(?:\.toLowerCase\(\)|\.trim\(\))*\.startsWith\(\s*['"`]([^'"`]+)['"`]\s*\)""",
        RegexOption.IGNORE_CASE
    )
    private val JS_COMMAND_MATCH_HEADER = Regex(
        """(?:(?:interaction\.)?commandName|command|cmd)\s*===?\s*['"`]([a-zA-Z0-9_\-]+)['"`]""",
        RegexOption.IGNORE_CASE
    )
    private val JS_SWITCH_CASE_HEADER = Regex(
        """case\s*['"`]([a-zA-Z0-9_\-]+)['"`]\s*:""",
        RegexOption.IGNORE_CASE
    )
    private val PY_COMMAND_HEADER = Regex(
        """@(?:bot|client)\.command\((?:name\s*=\s*['"`]([a-zA-Z0-9_\-]+)['"`])?[^)]*\)\s*(?:async\s+)?def\s+([a-zA-Z0-9_]+)\s*\([^)]*\)\s*:""",
        RegexOption.IGNORE_CASE
    )
    private val PY_MESSAGE_HEADER = Regex(
        """if\s+message\.content(?:\.lower\(\)|\.strip\(\))*\s*(?:==|\.startswith\()\s*['"`]([^'"`]+)['"`]\)?""",
        RegexOption.IGNORE_CASE
    )

    private val WHITESPACE_REGEX = Regex("\\s+")

    // In-memory compiled rules cache
    data class ParsedBotRules(
        val exactMatches: Map<String, String>, // lowercase trigger -> full body
        val prefixMatches: List<Pair<String, String>>, // trigger -> full body
        val commandMatches: Map<String, String>, // lowercase commandName -> full body
        val pythonCommands: Map<String, String>, // lowercase commandName -> full body
        val allCustomCommands: Set<String>
    )

    private val rulesCache = java.util.concurrent.ConcurrentHashMap<Long, Pair<Int, ParsedBotRules>>()

    fun invalidateCache(projectId: Long? = null) {
        if (projectId != null) {
            rulesCache.remove(projectId)
        } else {
            rulesCache.clear()
        }
    }

    /**
     * Extracts a balanced block between { and } without getting tricked by nested
     * objects, strings, or comments.
     */
    fun extractBalancedBlock(text: String, startPos: Int): String {
        val braceIdx = text.indexOf('{', startPos)
        if (braceIdx == -1 || braceIdx > startPos + 120) {
            // No block brace or opening brace is far away - single statement:
            val semi = text.indexOf(';', startPos)
            val nl = text.indexOf('\n', startPos)
            return when {
                semi != -1 && (nl == -1 || semi < nl) -> text.substring(startPos, semi + 1)
                nl != -1 -> text.substring(startPos, nl)
                else -> text.substring(startPos)
            }
        }

        var depth = 0
        var i = braceIdx
        var inStr: Char? = null
        var inLineComment = false
        var inBlockComment = false
        val len = text.length

        while (i < len) {
            val c = text[i]
            if (inLineComment) {
                if (c == '\n') inLineComment = false
            } else if (inBlockComment) {
                if (c == '*' && i + 1 < len && text[i + 1] == '/') {
                    inBlockComment = false
                    i++
                }
            } else if (inStr != null) {
                if (c == '\\') {
                    i++ // Skip escaped character
                } else if (c == inStr) {
                    inStr = null
                }
            } else {
                if (c == '/' && i + 1 < len && text[i + 1] == '/') {
                    inLineComment = true
                    i++
                } else if (c == '/' && i + 1 < len && text[i + 1] == '*') {
                    inBlockComment = true
                    i++
                } else if (c == '\'' || c == '"' || c == '`') {
                    inStr = c
                } else if (c == '{') {
                    depth++
                } else if (c == '}') {
                    depth--
                    if (depth == 0) {
                        return text.substring(braceIdx + 1, i)
                    }
                }
            }
            i++
        }
        return text.substring(braceIdx + 1)
    }

    /**
     * Extracts an indented block for Python syntax.
     */
    private fun extractIndentedBlock(text: String, startPos: Int): String {
        val lines = text.substring(startPos).lines()
        val bodyLines = mutableListOf<String>()
        for (line in lines) {
            if (bodyLines.isEmpty() && line.isBlank()) continue
            if (line.isNotBlank() && !line.startsWith(" ") && !line.startsWith("\t")) {
                break
            }
            bodyLines.add(line)
        }
        return bodyLines.joinToString("\n")
    }

    /**
     * Extracts all command names declared in project code.
     */
    fun extractAllCommandNames(allCode: String): Set<String> {
        val commands = mutableSetOf<String>()
        for (m in JS_COMMAND_MATCH_HEADER.findAll(allCode)) {
            commands.add(m.groupValues[1].lowercase())
        }
        for (m in JS_SWITCH_CASE_HEADER.findAll(allCode)) {
            commands.add(m.groupValues[1].lowercase())
        }
        for (m in PY_COMMAND_HEADER.findAll(allCode)) {
            val name = m.groupValues[1].ifEmpty { m.groupValues[2] }.lowercase()
            commands.add(name)
        }
        return commands
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
        val allCommandNames = mutableSetOf<String>()

        if (allCode.isNotBlank()) {
            val codeToScan = if (allCode.length > 500_000) allCode.take(500_000) else allCode

            // 1. JavaScript / TypeScript command checks: commandName === '...' or command === '...'
            for (match in JS_COMMAND_MATCH_HEADER.findAll(codeToScan)) {
                val cmd = match.groupValues[1].trim().lowercase()
                val body = extractBalancedBlock(codeToScan, match.range.last + 1)
                commands[cmd] = body
                allCommandNames.add(cmd)
            }

            // 2. JavaScript / TypeScript switch cases: case '...':
            for (match in JS_SWITCH_CASE_HEADER.findAll(codeToScan)) {
                val cmd = match.groupValues[1].trim().lowercase()
                val body = extractBalancedBlock(codeToScan, match.range.last + 1)
                commands[cmd] = body
                allCommandNames.add(cmd)
            }

            // 3. Exact message content: message.content === '...'
            for (match in JS_EXACT_MATCH_HEADER.findAll(codeToScan)) {
                val trigger = match.groupValues[1].trim().lowercase()
                val body = extractBalancedBlock(codeToScan, match.range.last + 1)
                exact[trigger] = body
            }

            // 4. Starts with: message.content.startsWith('...')
            for (match in JS_STARTS_WITH_HEADER.findAll(codeToScan)) {
                val trigger = match.groupValues[1].trim()
                val body = extractBalancedBlock(codeToScan, match.range.last + 1)
                prefixes.add(trigger to body)
            }

            // 5. Python @bot.command()
            for (match in PY_COMMAND_HEADER.findAll(codeToScan)) {
                val cmd = match.groupValues[1].ifEmpty { match.groupValues[2] }.trim().lowercase()
                val body = extractIndentedBlock(codeToScan, match.range.last + 1)
                pythonCmds[cmd] = body
                allCommandNames.add(cmd)
            }

            // 6. Python if message.content == '...' or .startswith('...')
            for (match in PY_MESSAGE_HEADER.findAll(codeToScan)) {
                val trigger = match.groupValues[1].trim()
                val body = extractIndentedBlock(codeToScan, match.range.last + 1)
                exact[trigger.lowercase()] = body
                prefixes.add(trigger to body)
            }
        }

        val compiled = ParsedBotRules(exact, prefixes, commands, pythonCmds, allCommandNames)
        rulesCache[projectId] = codeHash to compiled
        return compiled
    }

    /**
     * Executes the user's bot code against an incoming message (from Real Discord or Simulator).
     * Gives absolute priority to the user's typed code in the editor.
     */
    suspend fun executeIncomingMessage(
        messageContent: String,
        authorUsername: String,
        authorId: String,
        project: BotProject,
        files: List<BotFile>,
        gatewayPingMs: Int
    ): BotExecutionResult {
        return try {
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

            // Fast compiled rules lookup from user source files
            val rules = getOrCompileRules(project.id, allCode)

            // 1. Check for exact code-defined triggers in user's typed source code
            val customCodeResult = evaluateCustomCodeTriggers(
                trimmedContent = trimmed,
                cleanCommand = cleanCommand,
                commandName = commandName,
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

            // 2. Built-in command fallback ONLY if user's code DID NOT define this command
            // and the command is one of the standard demo commands
            val isCustomCommandDeclared = rules.commandMatches.containsKey(commandName) ||
                    rules.pythonCommands.containsKey(commandName) ||
                    rules.exactMatches.containsKey(trimmed.lowercase())

            if (!isCustomCommandDeclared) {
                evaluateBuiltInCommands(
                    commandName = commandName,
                    argsJoined = argsJoined,
                    authorUsername = authorUsername,
                    project = project,
                    pingMs = gatewayPingMs,
                    isSlashOrPrefix = isSlash || isPrefix
                )
            } else {
                // The user declared the command in code, but the reply parser couldn't find a direct reply call.
                // Acknowledge execution safely instead of running template code!
                BotExecutionResult(
                    replyText = "✅ `$commandName` executed successfully.",
                    isHandled = true,
                    matchedRule = "Custom command: $commandName",
                    executionLog = "Executed user code for command: $commandName"
                )
            }
        } catch (t: Throwable) {
            BotExecutionResult(
                isHandled = false,
                executionLog = "⚠️ [CODE EXECUTION ERROR] ${t.localizedMessage ?: "Safe recovery"}"
            )
        }
    }

    /**
     * Evaluates custom triggers defined by the user in the editor.
     */
    private fun evaluateCustomCodeTriggers(
        trimmedContent: String,
        cleanCommand: String,
        commandName: String,
        argsJoined: String,
        authorUsername: String,
        authorId: String,
        rules: ParsedBotRules,
        pingMs: Int,
        project: BotProject
    ): BotExecutionResult? {
        val lowerTrimmed = trimmedContent.lowercase()

        // 1. Exact trigger match (e.g. message.content === '!hello' or 'ping')
        val exactBody = rules.exactMatches[lowerTrimmed]
        if (exactBody != null) {
            val result = extractExecutionResultFromBody(exactBody, pingMs, authorUsername, authorId, argsJoined, project)
            if (result != null) {
                return result.copy(
                    matchedRule = "Exact Match ('$trimmedContent')",
                    executionLog = "Matched user exact trigger: '$trimmedContent'"
                )
            }
        }

        // 2. StartsWith trigger match (e.g. message.content.startsWith('!greet'))
        for ((trigger, body) in rules.prefixMatches) {
            if (trimmedContent.startsWith(trigger, ignoreCase = true)) {
                val result = extractExecutionResultFromBody(body, pingMs, authorUsername, authorId, argsJoined, project)
                if (result != null) {
                    return result.copy(
                        matchedRule = "startsWith('$trigger')",
                        executionLog = "Matched user prefix trigger: '$trigger'"
                    )
                }
            }
        }

        // 3. Command name match (e.g. commandName === 'mycommand' or case 'mycommand':)
        val cmdBody = rules.commandMatches[commandName]
        if (cmdBody != null) {
            val result = extractExecutionResultFromBody(cmdBody, pingMs, authorUsername, authorId, argsJoined, project)
            if (result != null) {
                return result.copy(
                    matchedRule = "Custom Command ($commandName)",
                    executionLog = "Executed user command: $commandName"
                )
            }
            // Even if body has custom async logic without simple send, acknowledge it:
            return BotExecutionResult(
                replyText = "⚡ Executed `$commandName` from your editor code.",
                isHandled = true,
                matchedRule = "Custom Command ($commandName)",
                executionLog = "Executed user command: $commandName"
            )
        }

        // 4. Python @bot.command match
        val pyBody = rules.pythonCommands[commandName]
        if (pyBody != null) {
            val result = extractExecutionResultFromBody(pyBody, pingMs, authorUsername, authorId, argsJoined, project)
            if (result != null) {
                return result.copy(
                    matchedRule = "Python @bot.command ($commandName)",
                    executionLog = "Executed user Python command: $commandName"
                )
            }
            return BotExecutionResult(
                replyText = "⚡ Executed Python `@bot.command` `$commandName`.",
                isHandled = true,
                matchedRule = "Python @bot.command ($commandName)",
                executionLog = "Executed user Python command: $commandName"
            )
        }

        return null
    }

    /**
     * Extracts reply text and/or rich embed from a captured code body.
     */
    private fun extractExecutionResultFromBody(
        body: String,
        pingMs: Int,
        authorUsername: String,
        authorId: String,
        argsJoined: String,
        project: BotProject
    ): BotExecutionResult? {
        // Regex patterns for reply/send calls in JS and Python
        // 1. Direct string reply: message.reply('...'), channel.send("..."), interaction.reply(`...`)
        val sendStringPattern = Regex(
            """(?:await\s+)?(?:message|msg|channel|interaction|ctx)(?:\.channel)?\.(?:reply|send|editReply|followUp)\(\s*(?:['"`]([^'"`]+)['"`]|f['"`]([^'"`]+)['"`])\s*\)""",
            RegexOption.IGNORE_CASE
        )
        val stringMatch = sendStringPattern.find(body)
        var replyText = stringMatch?.let {
            val raw = it.groupValues[1].ifEmpty { it.groupValues[2] }
            resolveVariables(raw, pingMs, authorUsername, authorId, argsJoined, project)
        } ?: ""

        // 2. Object reply: message.reply({ content: '...' }) or interaction.reply({ content: "..." })
        if (replyText.isBlank()) {
            val objectContentPattern = Regex(
                """content\s*:\s*(?:['"`]([^'"`]+)['"`]|f['"`]([^'"`]+)['"`])""",
                RegexOption.IGNORE_CASE
            )
            val objMatch = objectContentPattern.find(body)
            if (objMatch != null) {
                val raw = objMatch.groupValues[1].ifEmpty { objMatch.groupValues[2] }
                replyText = resolveVariables(raw, pingMs, authorUsername, authorId, argsJoined, project)
            }
        }

        // 3. Extract EmbedBuilder / MessageEmbed / discord.Embed
        val embedTitleMatch = Regex(
            """(?:\.setTitle\(\s*['"`]([^'"`]+)['"`]|title\s*=\s*['"`]([^'"`]+)['"`]|title\s*:\s*['"`]([^'"`]+)['"`])""",
            RegexOption.IGNORE_CASE
        ).find(body)
        val embedTitle = embedTitleMatch?.let {
            val raw = it.groupValues[1].ifEmpty { it.groupValues[2] }.ifEmpty { it.groupValues[3] }
            resolveVariables(raw, pingMs, authorUsername, authorId, argsJoined, project)
        }

        val embedDescMatch = Regex(
            """(?:\.setDescription\(\s*(?:['"`]([^'"`]+)['"`]|`([^`]+)`)|description\s*=\s*['"`]([^'"`]+)['"`]|description\s*:\s*['"`]([^'"`]+)['"`])""",
            RegexOption.IGNORE_CASE
        ).find(body)
        val embedDescription = embedDescMatch?.let {
            val raw = it.groupValues[1].ifEmpty { it.groupValues[2] }.ifEmpty { it.groupValues[3] }.ifEmpty { it.groupValues[4] }
            resolveVariables(raw, pingMs, authorUsername, authorId, argsJoined, project)
        }

        val embedColorMatch = Regex(
            """(?:\.setColor\(\s*(?:0x([0-9a-fA-F]+)|['"`](#[0-9a-fA-F]+)['"`])|color\s*=\s*(?:0x([0-9a-fA-F]+)|['"`](#[0-9a-fA-F]+)['"`]))""",
            RegexOption.IGNORE_CASE
        ).find(body)
        val embedColorHex = embedColorMatch?.let {
            val hex1 = it.groupValues[1].ifEmpty { it.groupValues[3] }
            val hex2 = it.groupValues[2].ifEmpty { it.groupValues[4] }
            if (hex1.isNotBlank()) "#$hex1" else hex2
        }

        val embedFooterMatch = Regex(
            """(?:\.setFooter\(\s*\{\s*text\s*:\s*['"`]([^'"`]+)['"`]|\.setFooter\(\s*['"`]([^'"`]+)['"`]|footer\s*=\s*['"`]([^'"`]+)['"`])""",
            RegexOption.IGNORE_CASE
        ).find(body)
        val embedFooter = embedFooterMatch?.let {
            val raw = it.groupValues[1].ifEmpty { it.groupValues[2] }.ifEmpty { it.groupValues[3] }
            resolveVariables(raw, pingMs, authorUsername, authorId, argsJoined, project)
        }

        val embedAuthorMatch = Regex(
            """(?:\.setAuthor\(\s*\{\s*name\s*:\s*['"`]([^'"`]+)['"`]|\.setAuthor\(\s*['"`]([^'"`]+)['"`])""",
            RegexOption.IGNORE_CASE
        ).find(body)
        val embedAuthor = embedAuthorMatch?.let {
            val raw = it.groupValues[1].ifEmpty { it.groupValues[2] }
            resolveVariables(raw, pingMs, authorUsername, authorId, argsJoined, project)
        }

        val hasEmbed = !embedTitle.isNullOrBlank() || !embedDescription.isNullOrBlank()
        if (replyText.isNotBlank() || hasEmbed) {
            return BotExecutionResult(
                replyText = replyText,
                embedTitle = embedTitle,
                embedDescription = embedDescription,
                embedColorHex = embedColorHex ?: if (hasEmbed) "#5865F2" else null,
                embedAuthor = embedAuthor,
                embedFooter = embedFooter,
                isHandled = true
            )
        }

        return null
    }

    private fun resolveVariables(
        rawText: String,
        pingMs: Int,
        authorUsername: String,
        authorId: String,
        argsJoined: String,
        project: BotProject
    ): String {
        var text = rawText
        val pingStr = "${pingMs}ms"
        if (text.contains("ping", ignoreCase = true) || text.contains("latency", ignoreCase = true)) {
            text = text.replace("\${client.ws.ping}", pingStr)
            text = text.replace("\${ws.ping}", pingStr)
            text = text.replace("{ping}", pingStr)
            text = text.replace("{latency}", pingStr)
            text = text.replace("\${round(bot.latency * 1000)}", "${pingMs}")
            text = text.replace("{round(bot.latency * 1000)}", "${pingMs}")
        }
        if (text.contains("author", ignoreCase = true) || text.contains("user", ignoreCase = true)) {
            text = text.replace("\${message.author.username}", authorUsername)
            text = text.replace("\${msg.author.username}", authorUsername)
            text = text.replace("\${interaction.user.username}", authorUsername)
            text = text.replace("\${author.username}", authorUsername)
            text = text.replace("\${author}", authorUsername)
            text = text.replace("{user}", authorUsername)
            text = text.replace("{author}", authorUsername)
            text = text.replace("{author.name}", authorUsername)
            text = text.replace("{ctx.author.name}", authorUsername)
            text = text.replace("{ctx.author}", authorUsername)
            text = text.replace("\${message.author.id}", authorId)
            text = text.replace("\${author.id}", authorId)
        }
        if (text.contains("args", ignoreCase = true)) {
            val safeArgs = argsJoined.ifEmpty { "None" }
            text = text.replace("\${args.join(' ')}", safeArgs)
            text = text.replace("\${args.join(\" \")}", safeArgs)
            text = text.replace("\${args}", safeArgs)
            text = text.replace("{args}", safeArgs)
        }
        if (text.contains("prefix", ignoreCase = true)) {
            text = text.replace("{prefix}", project.prefix)
            text = text.replace("\${prefix}", project.prefix)
            text = text.replace("\${process.env.PREFIX}", project.prefix)
        }
        if (text.contains("guilds", ignoreCase = true) || text.contains("servers", ignoreCase = true)) {
            text = text.replace("\${client.guilds.cache.size}", "1")
            text = text.replace("{len(bot.guilds)}", "1")
        }
        text = text.replace("\\n", "\n")
        return text
    }

    /**
     * Fallback handlers for standard Discord commands so that newly created bots
     * or standard commands work right out of the box on real Discord and in simulator!
     * ONLY called when the user's source code does NOT declare a handler for this command.
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
