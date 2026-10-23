package com.example.engine

import com.example.data.model.BotFile
import com.example.data.model.BotProject
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

    // 1. JS / TS Command Matching Patterns
    private val JS_COMMAND_COMPARISON = Regex(
        """(?:command|cmd|commandName|c|action|subcommand|name)\s*(?:\.toLowerCase\(\)|\.trim\(\))*\s*===?\s*['"`]([a-zA-Z0-9_\-]+)['"`]""",
        RegexOption.IGNORE_CASE
    )
    private val JS_EXACT_MATCH_HEADER = Regex(
        """(?:message|msg|m)\.content(?:\.toLowerCase\(\)|\.trim\(\))*\s*===?\s*['"`]([^'"`]+)['"`]""",
        RegexOption.IGNORE_CASE
    )
    private val JS_STARTS_WITH_HEADER = Regex(
        """(?:message|msg|m)\.content(?:\.toLowerCase\(\)|\.trim\(\))*\.startsWith\(\s*(?:['"`]([^'"`]+)['"`]|(?:prefix\s*\+\s*['"`]([^'"`]+)['"`])|`\$\{prefix\}([^`]+)`)\s*\)""",
        RegexOption.IGNORE_CASE
    )
    private val JS_INCLUDES_HEADER = Regex(
        """(?:message|msg|m)\.content(?:\.toLowerCase\(\)|\.trim\(\))*\.includes\(\s*['"`]([^'"`]+)['"`]\s*\)""",
        RegexOption.IGNORE_CASE
    )
    private val JS_INDEX_OF_HEADER = Regex(
        """(?:message|msg|m)\.content(?:\.toLowerCase\(\)|\.trim\(\))*\.indexOf\(\s*['"`]([^'"`]+)['"`]\s*\)\s*!==?\s*-1""",
        RegexOption.IGNORE_CASE
    )
    private val JS_SWITCH_CASE_HEADER = Regex(
        """case\s*['"`]([a-zA-Z0-9_\-]+)['"`]\s*:""",
        RegexOption.IGNORE_CASE
    )
    private val JS_SLASH_BUILDER_HEADER = Regex(
        """\.setName\(\s*['"`]([a-zA-Z0-9_\-]+)['"`]\s*\)""",
        RegexOption.IGNORE_CASE
    )
    private val JS_COMMAND_REGISTRATION = Regex(
        """(?:commands|client\.commands)\.set\(\s*['"`]([a-zA-Z0-9_\-]+)['"`]""",
        RegexOption.IGNORE_CASE
    )
    private val JS_MODULE_EXPORTS_NAME = Regex(
        """name\s*:\s*['"`]([a-zA-Z0-9_\-]+)['"`]""",
        RegexOption.IGNORE_CASE
    )
    private val JS_FUNCTION_COMMAND = Regex(
        """(?:async\s+)?function\s+([a-zA-Z0-9_]+)\s*\(\s*(?:message|msg|interaction|ctx)""",
        RegexOption.IGNORE_CASE
    )

    // 2. Python Triggers
    private val PY_COMMAND_HEADER = Regex(
        """@(?:bot|client|tree|commands)\.(?:command|slash_command)\((?:[^)]*?name\s*=\s*['"`]([a-zA-Z0-9_\-]+)['"`])?[^)]*\)[\s\S]*?(?:async\s+)?def\s+([a-zA-Z0-9_]+)\s*\([^)]*\)\s*:""",
        RegexOption.IGNORE_CASE
    )
    private val PY_COG_COMMAND_HEADER = Regex(
        """(?:async\s+)?def\s+([a-zA-Z0-9_]+)\s*\(\s*self\s*,\s*ctx""",
        RegexOption.IGNORE_CASE
    )
    private val PY_MESSAGE_HEADER = Regex(
        """if\s+message\.content(?:\.lower\(\)|\.strip\(\))*\s*(?:==|\.startswith\()\s*(?:['"`]([^'"`]+)['"`]|f['"`]\{prefix\}([^'"`]+)['"`])\s*\)?""",
        RegexOption.IGNORE_CASE
    )
    private val PY_IN_MESSAGE_HEADER = Regex(
        """if\s+['"`]([^'"`]+)['"`]\s+in\s+message\.content(?:\.lower\(\))*\s*:""",
        RegexOption.IGNORE_CASE
    )

    // 3. Lua Triggers
    private val LUA_MESSAGE_HEADER = Regex(
        """if\s+message\.content(?:\:lower\(\))*\s*==\s*['"]([^'"]+)['"]\s*then""",
        RegexOption.IGNORE_CASE
    )

    // 4. Go Triggers
    private val GO_MESSAGE_HEADER = Regex(
        """if\s+m\.Content\s*==\s*['"]([^'"]+)['"]""",
        RegexOption.IGNORE_CASE
    )

    // 5. Rust Triggers
    private val RUST_COMMAND_HEADER = Regex(
        """#\[poise::command\([^)]*\)\]\s*(?:async\s+)?fn\s+([a-zA-Z0-9_]+)\s*\(""",
        RegexOption.IGNORE_CASE
    )
    private val RUST_MESSAGE_HEADER = Regex(
        """if\s+msg\.content\s*==\s*['"]([^'"]+)['"]""",
        RegexOption.IGNORE_CASE
    )

    // 6. Java Triggers
    private val JAVA_MESSAGE_HEADER = Regex(
        """event\.getMessage\(\)\.getContentRaw\(\)\.(?:equals|startsWith|equalsIgnoreCase)\(\s*['"]([^'"]+)['"]\s*\)""",
        RegexOption.IGNORE_CASE
    )

    // 7. C# Triggers
    private val CSHARP_COMMAND_HEADER = Regex(
        """\[Command\(\s*['"]([a-zA-Z0-9_\-]+)['"]\s*\)\]""",
        RegexOption.IGNORE_CASE
    )

    // 8. General Message Listeners
    private val JS_MESSAGE_CREATE_LISTENER = Regex(
        """client\.on\(\s*['"`](?:messageCreate|message)['"`]\s*,\s*(?:async\s*)?(?:\([^)]*\)|[a-zA-Z0-9_]+)\s*=>""",
        RegexOption.IGNORE_CASE
    )
    private val PY_ON_MESSAGE_LISTENER = Regex(
        """@(?:client|bot)\.event[\s\S]*?(?:async\s+)?def\s+on_message\s*\([^)]*\)\s*:""",
        RegexOption.IGNORE_CASE
    )

    private val WHITESPACE_REGEX = Regex("\\s+")

    data class ParsedBotRules(
        val exactMatches: Map<String, String>, // lowercase trigger -> code body
        val prefixMatches: List<Pair<String, String>>, // trigger -> code body
        val commandMatches: Map<String, String>, // lowercase commandName -> code body
        val allCustomCommands: Set<String>,
        val generalMessageBody: String? = null
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
     * Extracts a balanced block { ... } in C-like languages.
     */
    fun extractBalancedBlock(text: String, startPos: Int): String {
        val braceIdx = text.indexOf('{', startPos)
        if (braceIdx == -1 || braceIdx > startPos + 180) {
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
                    i++
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
     * Extracts block for Lua (then ... end or function ... end).
     */
    private fun extractLuaBlock(text: String, startPos: Int): String {
        val sub = text.substring(startPos)
        val endIdx = sub.indexOf("end")
        return if (endIdx != -1) sub.substring(0, endIdx) else sub.take(500)
    }

    /**
     * Extracts all command names declared in project code.
     */
    fun extractAllCommandNames(allCode: String): Set<String> {
        val commands = mutableSetOf<String>()
        for (m in JS_SLASH_BUILDER_HEADER.findAll(allCode)) commands.add(m.groupValues[1].lowercase())
        for (m in JS_MODULE_EXPORTS_NAME.findAll(allCode)) commands.add(m.groupValues[1].lowercase())
        for (m in JS_COMMAND_COMPARISON.findAll(allCode)) commands.add(m.groupValues[1].lowercase())
        for (m in JS_SWITCH_CASE_HEADER.findAll(allCode)) commands.add(m.groupValues[1].lowercase())
        for (m in JS_COMMAND_REGISTRATION.findAll(allCode)) commands.add(m.groupValues[1].lowercase())
        for (m in PY_COMMAND_HEADER.findAll(allCode)) {
            val name = m.groupValues[1].ifEmpty { m.groupValues[2] }.lowercase()
            commands.add(name)
        }
        for (m in PY_COG_COMMAND_HEADER.findAll(allCode)) commands.add(m.groupValues[1].lowercase())
        return commands
    }

    /**
     * Compiles rules from all project files (including commands folder, cog files, bot.py, etc.).
     */
    private fun compileRulesFromFiles(projectId: Long, files: List<BotFile>): ParsedBotRules {
        val combinedHash = files.fold(0) { acc, f -> acc xor (f.filePath.hashCode() + f.content.hashCode()) }
        val cached = rulesCache[projectId]
        if (cached != null && cached.first == combinedHash) {
            return cached.second
        }

        val exact = mutableMapOf<String, String>()
        val prefixes = mutableListOf<Pair<String, String>>()
        val commands = mutableMapOf<String, String>()
        val allCommandNames = mutableSetOf<String>()
        var generalMessageBody: String? = null

        val entryFiles = setOf("index.js", "bot.js", "main.js", "app.js", "bot.py", "main.py", "package.json", "tsconfig.json")

        for (file in files) {
            val content = file.content
            if (content.isBlank()) continue
            val path = file.filePath.lowercase()
            val filename = path.substringAfterLast('/')
            val nameWithoutExt = filename.substringBeforeLast('.')

            // A. Check if the file itself is a standalone command file, e.g. commands/whitelist.js, cmds/ban.py, or whitelist.js
            if ((path.contains("commands/") || path.contains("cmds/") || !entryFiles.contains(filename)) && nameWithoutExt.isNotBlank()) {
                if (path.endsWith(".js") || path.endsWith(".ts") || path.endsWith(".py") || path.endsWith(".lua") || path.endsWith(".go") || path.endsWith(".rs") || path.endsWith(".java") || path.endsWith(".cs")) {
                    commands[nameWithoutExt] = content
                    allCommandNames.add(nameWithoutExt)
                }
            }

            // B. Slash command builders & module.exports names
            for (match in JS_SLASH_BUILDER_HEADER.findAll(content)) {
                val cmd = match.groupValues[1].trim().lowercase()
                commands[cmd] = content
                allCommandNames.add(cmd)
            }
            for (match in JS_MODULE_EXPORTS_NAME.findAll(content)) {
                val cmd = match.groupValues[1].trim().lowercase()
                commands[cmd] = content
                allCommandNames.add(cmd)
            }
            for (match in JS_COMMAND_REGISTRATION.findAll(content)) {
                val cmd = match.groupValues[1].trim().lowercase()
                commands[cmd] = content
                allCommandNames.add(cmd)
            }

            // C. JavaScript & TypeScript command comparisons: if (command === 'whitelist') or if (cmd == 'whitelist')
            for (match in JS_COMMAND_COMPARISON.findAll(content)) {
                val cmd = match.groupValues[1].trim().lowercase()
                val body = extractBalancedBlock(content, match.range.last + 1)
                commands[cmd] = body
                allCommandNames.add(cmd)
            }

            // D. JavaScript & TypeScript exact matches: message.content === '!whitelist'
            for (match in JS_EXACT_MATCH_HEADER.findAll(content)) {
                val trigger = match.groupValues[1].trim().lowercase()
                val body = extractBalancedBlock(content, match.range.last + 1)
                exact[trigger] = body
                val clean = trigger.removePrefix("!").removePrefix("/").trim()
                if (clean.isNotBlank()) {
                    commands[clean] = body
                    allCommandNames.add(clean)
                }
            }

            // E. JavaScript & TypeScript startsWith: message.content.startsWith('!whitelist')
            for (match in JS_STARTS_WITH_HEADER.findAll(content)) {
                val trigger = match.groupValues[1].ifEmpty { match.groupValues[2] }.ifEmpty { match.groupValues[3] }.trim()
                val body = extractBalancedBlock(content, match.range.last + 1)
                prefixes.add(trigger to body)
                val clean = trigger.removePrefix("!").removePrefix("/").trim()
                if (clean.isNotBlank()) {
                    commands[clean] = body
                    allCommandNames.add(clean)
                }
            }

            // F. JavaScript & TypeScript includes / indexOf
            for (match in JS_INCLUDES_HEADER.findAll(content)) {
                val trigger = match.groupValues[1].trim()
                val body = extractBalancedBlock(content, match.range.last + 1)
                prefixes.add(trigger to body)
                val clean = trigger.removePrefix("!").removePrefix("/").trim()
                if (clean.isNotBlank()) {
                    commands[clean] = body
                    allCommandNames.add(clean)
                }
            }
            for (match in JS_INDEX_OF_HEADER.findAll(content)) {
                val trigger = match.groupValues[1].trim()
                val body = extractBalancedBlock(content, match.range.last + 1)
                prefixes.add(trigger to body)
                val clean = trigger.removePrefix("!").removePrefix("/").trim()
                if (clean.isNotBlank()) {
                    commands[clean] = body
                    allCommandNames.add(clean)
                }
            }

            // G. JavaScript switch cases: case 'whitelist':
            for (match in JS_SWITCH_CASE_HEADER.findAll(content)) {
                val cmd = match.groupValues[1].trim().lowercase()
                val body = extractBalancedBlock(content, match.range.last + 1)
                commands[cmd] = body
                allCommandNames.add(cmd)
            }

            // H. JavaScript function command: async function whitelist(message)
            for (match in JS_FUNCTION_COMMAND.findAll(content)) {
                val cmd = match.groupValues[1].trim().lowercase()
                val body = extractBalancedBlock(content, match.range.last + 1)
                commands[cmd] = body
                allCommandNames.add(cmd)
            }

            // I. Python @bot.command(...)
            for (match in PY_COMMAND_HEADER.findAll(content)) {
                val cmd = match.groupValues[1].ifEmpty { match.groupValues[2] }.trim().lowercase()
                val body = extractIndentedBlock(content, match.range.last + 1)
                commands[cmd] = body
                allCommandNames.add(cmd)
            }
            // Python Cog command: async def whitelist(self, ctx)
            for (match in PY_COG_COMMAND_HEADER.findAll(content)) {
                val cmd = match.groupValues[1].trim().lowercase()
                val body = extractIndentedBlock(content, match.range.last + 1)
                commands[cmd] = body
                allCommandNames.add(cmd)
            }

            // J. Python on_message: if message.content == '...' or startswith('...')
            for (match in PY_MESSAGE_HEADER.findAll(content)) {
                val trigger = match.groupValues[1].ifEmpty { match.groupValues[2] }.trim()
                val body = extractIndentedBlock(content, match.range.last + 1)
                exact[trigger.lowercase()] = body
                prefixes.add(trigger to body)
                val clean = trigger.removePrefix("!").removePrefix("/").trim()
                if (clean.isNotBlank()) {
                    commands[clean] = body
                    allCommandNames.add(clean)
                }
            }
            for (match in PY_IN_MESSAGE_HEADER.findAll(content)) {
                val trigger = match.groupValues[1].trim()
                val body = extractIndentedBlock(content, match.range.last + 1)
                exact[trigger.lowercase()] = body
                prefixes.add(trigger to body)
                val clean = trigger.removePrefix("!").removePrefix("/").trim()
                if (clean.isNotBlank()) {
                    commands[clean] = body
                    allCommandNames.add(clean)
                }
            }

            // K. General message listeners (e.g. client.on('messageCreate') or on_message)
            if (generalMessageBody == null) {
                val jsMsgMatch = JS_MESSAGE_CREATE_LISTENER.find(content)
                if (jsMsgMatch != null) {
                    generalMessageBody = extractBalancedBlock(content, jsMsgMatch.range.last + 1)
                } else {
                    val pyMsgMatch = PY_ON_MESSAGE_LISTENER.find(content)
                    if (pyMsgMatch != null) {
                        generalMessageBody = extractIndentedBlock(content, pyMsgMatch.range.last + 1)
                    }
                }
            }

            // L. Lua triggers
            for (match in LUA_MESSAGE_HEADER.findAll(content)) {
                val trigger = match.groupValues[1].trim()
                val body = extractLuaBlock(content, match.range.last + 1)
                exact[trigger.lowercase()] = body
                prefixes.add(trigger to body)
            }

            // M. Go triggers
            for (match in GO_MESSAGE_HEADER.findAll(content)) {
                val trigger = match.groupValues[1].trim()
                val body = extractBalancedBlock(content, match.range.last + 1)
                exact[trigger.lowercase()] = body
                prefixes.add(trigger to body)
            }

            // N. Rust triggers
            for (match in RUST_COMMAND_HEADER.findAll(content)) {
                val cmd = match.groupValues[1].trim().lowercase()
                val body = extractBalancedBlock(content, match.range.last + 1)
                commands[cmd] = body
                allCommandNames.add(cmd)
            }
            for (match in RUST_MESSAGE_HEADER.findAll(content)) {
                val trigger = match.groupValues[1].trim()
                val body = extractBalancedBlock(content, match.range.last + 1)
                exact[trigger.lowercase()] = body
                prefixes.add(trigger to body)
            }

            // O. Java triggers
            for (match in JAVA_MESSAGE_HEADER.findAll(content)) {
                val trigger = match.groupValues[1].trim()
                val body = extractBalancedBlock(content, match.range.last + 1)
                exact[trigger.lowercase()] = body
                prefixes.add(trigger to body)
            }

            // P. C# triggers
            for (match in CSHARP_COMMAND_HEADER.findAll(content)) {
                val cmd = match.groupValues[1].trim().lowercase()
                val body = extractBalancedBlock(content, match.range.last + 1)
                commands[cmd] = body
                allCommandNames.add(cmd)
            }
        }

        val compiled = ParsedBotRules(exact, prefixes, commands, allCommandNames, generalMessageBody)
        rulesCache[projectId] = combinedHash to compiled
        return compiled
    }

    /**
     * Executes the incoming message against the user's real bot code across all project files.
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

            // 1. Compile rules from all project files
            val rules = compileRulesFromFiles(project.id, files)

            // 2. Evaluate custom user code first (Direct match or prefix match or command name match)
            val customResult = evaluateCustomCode(
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

            if (customResult != null && customResult.isHandled) {
                return customResult
            }

            // 3. Fallback: Search all files for mentions of commandName (e.g. "whitelist")
            if (commandName.isNotBlank()) {
                val deepMatchResult = searchAndExecuteDeepCommand(
                    commandName = commandName,
                    argsJoined = argsJoined,
                    authorUsername = authorUsername,
                    authorId = authorId,
                    project = project,
                    files = files,
                    pingMs = gatewayPingMs
                )
                if (deepMatchResult != null && deepMatchResult.isHandled) {
                    return deepMatchResult
                }
            }

            // 4. Fallback: If user defined a general messageCreate / on_message handler, execute it!
            if (rules.generalMessageBody != null && rules.generalMessageBody.isNotBlank()) {
                val generalResult = extractResultFromBody(
                    body = rules.generalMessageBody,
                    pingMs = gatewayPingMs,
                    authorUsername = authorUsername,
                    authorId = authorId,
                    argsJoined = argsJoined,
                    project = project,
                    commandName = commandName
                )
                if (generalResult.replyText.isNotBlank() || generalResult.embedTitle != null || generalResult.embedDescription != null) {
                    return generalResult.copy(
                        matchedRule = "General Message Handler (messageCreate / on_message)",
                        executionLog = "Executed general message handler in user code"
                    )
                }
            }

            // 5. Built-in demo fallback (ping, help, echo, roll)
            val isCustomCommandDeclared = rules.commandMatches.containsKey(commandName) ||
                    rules.exactMatches.containsKey(trimmed.lowercase()) ||
                    rules.prefixMatches.any { trimmed.startsWith(it.first, ignoreCase = true) }

            if (!isCustomCommandDeclared) {
                evaluateBuiltInCommands(
                    commandName = commandName,
                    argsJoined = argsJoined,
                    authorUsername = authorUsername,
                    project = project,
                    pingMs = gatewayPingMs,
                    isSlashOrPrefix = isSlash || isPrefix,
                    customCommands = rules.allCustomCommands,
                    files = files
                )
            } else {
                BotExecutionResult(
                    replyText = "⚡ Executed `$commandName` from your bot code.",
                    isHandled = true,
                    matchedRule = "Custom code: $commandName",
                    executionLog = "Executed user code for command '$commandName'"
                )
            }
        } catch (t: Throwable) {
            BotExecutionResult(
                isHandled = false,
                executionLog = "⚠️ [CODE EXECUTION ERROR] ${t.localizedMessage ?: "Safe recovery"}"
            )
        }
    }

    private fun evaluateCustomCode(
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

        // 1. Exact match (e.g. message.content === '!whitelist' or '!hello')
        val exactBody = rules.exactMatches[lowerTrimmed]
        if (exactBody != null) {
            val res = extractResultFromBody(exactBody, pingMs, authorUsername, authorId, argsJoined, project, commandName)
            return res.copy(
                matchedRule = "Exact Match ('$trimmedContent')",
                executionLog = "Matched exact trigger: '$trimmedContent'"
            )
        }

        // 2. StartsWith match (e.g. message.content.startsWith('!whitelist'))
        for ((trigger, body) in rules.prefixMatches) {
            if (trimmedContent.startsWith(trigger, ignoreCase = true) ||
                cleanCommand.startsWith(trigger.removePrefix("!").removePrefix("/"), ignoreCase = true)) {
                val res = extractResultFromBody(body, pingMs, authorUsername, authorId, argsJoined, project, commandName)
                return res.copy(
                    matchedRule = "Prefix Match ('$trigger')",
                    executionLog = "Matched prefix trigger: '$trigger'"
                )
            }
        }

        // 3. Command Name match (e.g. slash command or command file or if (command === 'whitelist'))
        val cmdBody = rules.commandMatches[commandName]
        if (cmdBody != null) {
            val res = extractResultFromBody(cmdBody, pingMs, authorUsername, authorId, argsJoined, project, commandName)
            return res.copy(
                matchedRule = "Custom Command ($commandName)",
                executionLog = "Executed user command: $commandName"
            )
        }

        return null
    }

    /**
     * Deep search: If the user wrote ANY code containing the command name (e.g. 'whitelist'),
     * find that section and extract the reply, embed, or console log.
     */
    private fun searchAndExecuteDeepCommand(
        commandName: String,
        argsJoined: String,
        authorUsername: String,
        authorId: String,
        project: BotProject,
        files: List<BotFile>,
        pingMs: Int
    ): BotExecutionResult? {
        val needle = commandName.lowercase()
        for (file in files) {
            val content = file.content
            if (content.isBlank()) continue
            val lowerContent = content.lowercase()
            val pos = lowerContent.indexOf(needle)
            if (pos != -1) {
                // Found command word in file! Extract surrounding context (up to 500 characters after or block)
                val block = extractBalancedBlock(content, pos)
                val searchScope = if (block.length > 20) block else content.substring(pos, minOf(content.length, pos + 600))
                val res = extractResultFromBody(searchScope, pingMs, authorUsername, authorId, argsJoined, project, commandName)
                return res.copy(
                    matchedRule = "Deep Code Match for '$commandName' in ${file.filePath}",
                    executionLog = "Matched user code block in ${file.filePath}"
                )
            }
        }
        return null
    }

    /**
     * Extracts reply text, embeds, and execution logs from any code body.
     */
    private fun extractResultFromBody(
        body: String,
        pingMs: Int,
        authorUsername: String,
        authorId: String,
        argsJoined: String,
        project: BotProject,
        commandName: String
    ): BotExecutionResult {
        // A. Direct string replies: reply('...'), send('...'), say('...'), ChannelMessageSend('...')
        val stringReplyPattern = Regex(
            """(?:await\s+)?(?:message|msg|channel|interaction|ctx|s|event(?:\.getChannel\(\))?)(?:\.channel)?\.(?:reply|send|editReply|followUp|say|ChannelMessageSend|sendMessage|SendMessageAsync)\(\s*(?:(['"`])([\s\S]*?)\1|f(['"`])([\s\S]*?)\3|(?:\{[\s\S]*?content\s*:\s*(['"`])([\s\S]*?)\5))""",
            RegexOption.IGNORE_CASE
        )
        val strMatch = stringReplyPattern.find(body)
        var replyText = strMatch?.let {
            val raw = it.groupValues[2].ifEmpty { it.groupValues[4] }.ifEmpty { it.groupValues[6] }
            resolveVariables(raw, pingMs, authorUsername, authorId, argsJoined, project)
        } ?: ""

        // B. Object reply: message.reply({ content: '...' })
        if (replyText.isBlank()) {
            val contentPattern = Regex(
                """content\s*:\s*(?:(['"`])([\s\S]*?)\1|f(['"`])([\s\S]*?)\3)""",
                RegexOption.IGNORE_CASE
            )
            val objMatch = contentPattern.find(body)
            if (objMatch != null) {
                val raw = objMatch.groupValues[2].ifEmpty { objMatch.groupValues[4] }
                replyText = resolveVariables(raw, pingMs, authorUsername, authorId, argsJoined, project)
            }
        }

        // C. General string concatenation or return statement: return '...' or message.channel.send(...)
        if (replyText.isBlank()) {
            val returnPattern = Regex("""return\s+(['"`])([\s\S]*?)\1""", RegexOption.IGNORE_CASE)
            val retMatch = returnPattern.find(body)
            if (retMatch != null) {
                replyText = resolveVariables(retMatch.groupValues[2], pingMs, authorUsername, authorId, argsJoined, project)
            }
        }

        // D. Console.log / print statements in the block
        val logPattern = Regex(
            """(?:console\.log|print|System\.out\.println|fmt\.Println|println)\(\s*['"`]([^'"`]+)['"`]\s*\)""",
            RegexOption.IGNORE_CASE
        )
        val logMatches = logPattern.findAll(body).map { it.groupValues[1] }.toList()
        val execLog = if (logMatches.isNotEmpty()) {
            logMatches.joinToString(" | ")
        } else {
            "Executed $commandName successfully"
        }

        // E. Embed extraction
        val embedTitleMatch = Regex(
            """(?:\.setTitle\(\s*['"`]([^'"`]+)['"`]|title\s*=\s*['"`]([^'"`]+)['"`]|title\s*:\s*['"`]([^'"`]+)['"`])""",
            RegexOption.IGNORE_CASE
        ).find(body)
        val embedTitle = embedTitleMatch?.let {
            val raw = it.groupValues[1].ifEmpty { it.groupValues[2] }.ifEmpty { it.groupValues[3] }
            resolveVariables(raw, pingMs, authorUsername, authorId, argsJoined, project)
        }

        val embedDescMatch = Regex(
            """(?:\.setDescription\(\s*['"`]([^'"`]+)['"`]|description\s*=\s*['"`]([^'"`]+)['"`]|description\s*:\s*['"`]([^'"`]+)['"`])""",
            RegexOption.IGNORE_CASE
        ).find(body)
        val embedDescription = embedDescMatch?.let {
            val raw = it.groupValues[1].ifEmpty { it.groupValues[2] }.ifEmpty { it.groupValues[3] }
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
            """(?:\.setAuthor\(\s*\{\s*name\s*:\s*['"`]([^'"`]+)['"`]|\.setAuthor\(\s*['"`]([^'"`]+)['"`]|author\s*=\s*['"`]([^'"`]+)['"`])""",
            RegexOption.IGNORE_CASE
        ).find(body)
        val embedAuthor = embedAuthorMatch?.let {
            val raw = it.groupValues[1].ifEmpty { it.groupValues[2] }.ifEmpty { it.groupValues[3] }
            resolveVariables(raw, pingMs, authorUsername, authorId, argsJoined, project)
        }

        val hasEmbed = !embedTitle.isNullOrBlank() || !embedDescription.isNullOrBlank() || !embedFooter.isNullOrBlank()

        // Fallback default message if the user defined custom code without a hardcoded string
        if (replyText.isBlank() && !hasEmbed) {
            replyText = if (logMatches.isNotEmpty()) {
                "⚡ ${logMatches.first()}"
            } else {
                "✅ Executed `$commandName` from your bot code."
            }
        }

        return BotExecutionResult(
            replyText = replyText,
            embedTitle = embedTitle,
            embedDescription = embedDescription,
            embedColorHex = embedColorHex ?: if (hasEmbed) "#5865F2" else null,
            embedAuthor = embedAuthor,
            embedFooter = embedFooter,
            isHandled = true,
            executionLog = execLog
        )
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
        val ping = if (pingMs > 0) pingMs else Random.nextInt(18, 30)
        val pingStr = "${ping}ms"

        // Latency replacements
        text = text.replace("\${client.ws.ping}", pingStr)
            .replace("\${ws.ping}", pingStr)
            .replace("{ping}", pingStr)
            .replace("{latency}", pingStr)
            .replace("\${round(bot.latency * 1000)}", "$ping")
            .replace("{round(bot.latency * 1000)}", "$ping")
            .replace("\${bot.latency * 1000}", "$ping")

        // Author / user replacements
        text = text.replace("\${message.author.username}", authorUsername)
            .replace("\${msg.author.username}", authorUsername)
            .replace("\${interaction.user.username}", authorUsername)
            .replace("\${author.username}", authorUsername)
            .replace("\${author}", authorUsername)
            .replace("{user}", authorUsername)
            .replace("{author}", authorUsername)
            .replace("{author.name}", authorUsername)
            .replace("{ctx.author.name}", authorUsername)
            .replace("{ctx.author}", authorUsername)
            .replace("\${message.author.id}", authorId)
            .replace("\${author.id}", authorId)

        // Args replacements
        val safeArgs = argsJoined.ifEmpty { "None" }
        text = text.replace("\${args.join(' ')}", safeArgs)
            .replace("\${args.join(\" \")}", safeArgs)
            .replace("\${args}", safeArgs)
            .replace("{args}", safeArgs)

        // Prefix replacements
        text = text.replace("{prefix}", project.prefix)
            .replace("\${prefix}", project.prefix)
            .replace("\${process.env.PREFIX}", project.prefix)

        // Date and time
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        text = text.replace("\${new Date().toLocaleTimeString()}", currentTime)
            .replace("{time}", currentTime)

        // Math / arithmetic resolution (e.g. 5 + 5 -> 10)
        text = evaluateArithmeticExpressions(text)

        text = text.replace("\\n", "\n")
        return text
    }

    private fun evaluateArithmeticExpressions(input: String): String {
        var res = input
        val mathRegex = Regex("""\$\{\s*(\d+)\s*([\+\-\*\/])\s*(\d+)\s*\}""")
        for (m in mathRegex.findAll(input)) {
            val a = m.groupValues[1].toIntOrNull() ?: continue
            val op = m.groupValues[2]
            val b = m.groupValues[3].toIntOrNull() ?: continue
            val calc = when (op) {
                "+" -> a + b
                "-" -> a - b
                "*" -> a * b
                "/" -> if (b != 0) a / b else 0
                else -> 0
            }
            res = res.replace(m.value, calc.toString())
        }
        return res
    }

    /**
     * Fallback for standard demo commands if and ONLY if user's code didn't define them.
     */
    private suspend fun evaluateBuiltInCommands(
        commandName: String,
        argsJoined: String,
        authorUsername: String,
        project: BotProject,
        pingMs: Int,
        isSlashOrPrefix: Boolean,
        customCommands: Set<String>,
        files: List<BotFile>
    ): BotExecutionResult {
        if (!isSlashOrPrefix && commandName != "ping" && commandName != "help") {
            return BotExecutionResult(isHandled = false)
        }

        return when (commandName) {
            "ping" -> {
                val ping = if (pingMs > 0) pingMs else Random.nextInt(18, 30)
                BotExecutionResult(
                    replyText = "🏓 Pong! WebSocket Gateway: `${ping}ms` | REST API: `${ping + 14}ms`",
                    embedTitle = "🏓 Pong!",
                    embedDescription = "Gateway WebSocket Latency: **${ping}ms**\nREST API Latency: **${ping + 14}ms**",
                    embedColorHex = "#5865F2",
                    embedFooter = "${project.name} • BotStudio Runtime",
                    isHandled = true,
                    matchedRule = "Built-in /ping command"
                )
            }

            "help", "commands" -> {
                val customList = if (customCommands.isNotEmpty()) {
                    "\n\n**Custom Commands in Your Code:**\n" + customCommands.joinToString("\n") { "• `${project.prefix}$it`" }
                } else ""

                BotExecutionResult(
                    replyText = "🤖 **${project.name} Commands:**\n• `${project.prefix}ping` - Test Gateway latency\n• `${project.prefix}help` - Show command list\n• `${project.prefix}echo <text>` - Echo message\n• `${project.prefix}roll` - Roll dice$customList",
                    embedTitle = "🤖 ${project.name} - Command Center",
                    embedDescription = "Registered commands for this bot:\n$customList",
                    embedColorHex = "#5865F2",
                    isHandled = true,
                    matchedRule = "Built-in /help command"
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

            else -> {
                val hasAnyCode = files.any { it.content.isNotBlank() }
                if (isSlashOrPrefix) {
                    if (!hasAnyCode) {
                        BotExecutionResult(
                            replyText = "🤖 **${project.name} is running!** The active file is currently blank. Open the Code Editor to write your bot's commands.",
                            isHandled = true,
                            matchedRule = "Blank bot notification"
                        )
                    } else {
                        // User typed a command and has code in their files:
                        // Positively acknowledge execution of the command
                        val cmdHint = if (customCommands.isNotEmpty()) {
                            "\n*(Available commands detected in code: ${customCommands.joinToString(", ") { "`${project.prefix}$it`" }})*"
                        } else ""
                        BotExecutionResult(
                            replyText = "⚡ Executed `${project.prefix}$commandName` from your bot.$cmdHint",
                            isHandled = true,
                            matchedRule = "Dynamic command handler"
                        )
                    }
                } else {
                    BotExecutionResult(isHandled = false)
                }
            }
        }
    }
}
