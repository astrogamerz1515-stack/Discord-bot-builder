package com.example.ui.editor

data class CodeDiagnostic(
    val lineNumber: Int,
    val columnNumber: Int = 1,
    val message: String,
    val severity: DiagnosticSeverity = DiagnosticSeverity.ERROR,
    val sourceLine: String = ""
)

enum class DiagnosticSeverity {
    ERROR,
    WARNING,
    INFO
}

/**
 * Real-time syntax and semantic validator that checks if code is wrong,
 * pinpointing exactly what is wrong and the line & column number where it is wrong.
 */
object CodeLinter {

    fun lintCode(code: String, filePath: String): List<CodeDiagnostic> {
        val diagnostics = mutableListOf<CodeDiagnostic>()
        if (code.isBlank()) return diagnostics
        return try {
            val lines = code.lines()
            val extension = filePath.substringAfterLast('.', "").lowercase()

        // 1. JSON-specific validation with exact line & column detection
        if (extension == "json") {
            lintJson(code, lines, diagnostics)
            return diagnostics
        }

        // 2. Environment (.env) validation
        if (extension == "env") {
            lintEnv(lines, diagnostics)
            return diagnostics
        }

        // 3. YAML / TOML validation
        if (extension in listOf("yaml", "yml", "toml")) {
            lintYaml(lines, diagnostics)
            return diagnostics
        }

        // 4. HTML / XML validation
        if (extension in listOf("html", "htm", "xml", "svg")) {
            lintHtmlXml(lines, diagnostics)
        }

        // 5. Shell script (.sh) validation
        if (extension in listOf("sh", "bash", "zsh")) {
            lintShell(lines, diagnostics)
        }

        // 6. General Bracket & Parenthesis Stacks for JS, TS, PY, KT, JAVA, RS, C, CPP, etc.
        val openParenStack = mutableListOf<Pair<Int, Int>>() // line, col
        val openBraceStack = mutableListOf<Pair<Int, Int>>()
        val openBracketStack = mutableListOf<Pair<Int, Int>>()

        for ((idx, line) in lines.withIndex()) {
            val lineNum = idx + 1
            val trimmed = line.trim()

            // Skip empty lines or pure comments
            if (trimmed.isEmpty() || trimmed.startsWith("//") || trimmed.startsWith("#") || trimmed.startsWith("/*") || trimmed.startsWith("*")) {
                continue
            }

            // JavaScript & TypeScript checks
            if (extension in listOf("js", "ts", "jsx", "tsx", "mjs", "cjs")) {
                // Check missing await on interaction replies
                if ((line.contains("interaction.reply(") || line.contains("interaction.deferReply(") || line.contains("interaction.followUp(")) &&
                    !line.contains("await") && !trimmed.startsWith("//")
                ) {
                    diagnostics.add(
                        CodeDiagnostic(
                            lineNumber = lineNum,
                            columnNumber = line.indexOf("interaction") + 1,
                            message = "Missing 'await' keyword before asynchronous interaction call",
                            severity = DiagnosticSeverity.WARNING,
                            sourceLine = trimmed
                        )
                    )
                }

                // Check undefined Discord client login
                if (line.contains("client.login()") || line.contains("client.login('')") || line.contains("client.login(\"\")")) {
                    diagnostics.add(
                        CodeDiagnostic(
                            lineNumber = lineNum,
                            columnNumber = line.indexOf("client.login") + 1,
                            message = "Bot token is empty. Supply process.env.DISCORD_TOKEN or your bot secret.",
                            severity = DiagnosticSeverity.ERROR,
                            sourceLine = trimmed
                        )
                    )
                }

                // Check missing new before EmbedBuilder() / ActionRowBuilder()
                if (line.contains("EmbedBuilder()") && !line.contains("new EmbedBuilder()") && !trimmed.startsWith("//")) {
                    diagnostics.add(
                        CodeDiagnostic(
                            lineNumber = lineNum,
                            columnNumber = line.indexOf("EmbedBuilder") + 1,
                            message = "Class constructor EmbedBuilder cannot be invoked without 'new'",
                            severity = DiagnosticSeverity.ERROR,
                            sourceLine = trimmed
                        )
                    )
                }
                if (line.contains("ActionRowBuilder()") && !line.contains("new ActionRowBuilder()") && !trimmed.startsWith("//")) {
                    diagnostics.add(
                        CodeDiagnostic(
                            lineNumber = lineNum,
                            columnNumber = line.indexOf("ActionRowBuilder") + 1,
                            message = "Class constructor ActionRowBuilder cannot be invoked without 'new'",
                            severity = DiagnosticSeverity.ERROR,
                            sourceLine = trimmed
                        )
                    )
                }
            }

            // Python checks
            if (extension == "py") {
                // Check missing async def on discord.py command/event handlers
                if ((trimmed.startsWith("@bot.tree.command") || trimmed.startsWith("@client.event") || trimmed.startsWith("@bot.command") || trimmed.startsWith("@bot.event")) &&
                    idx + 1 < lines.size
                ) {
                    val nextLine = lines[idx + 1].trim()
                    if (nextLine.startsWith("def ") && !nextLine.startsWith("async def ")) {
                        diagnostics.add(
                            CodeDiagnostic(
                                lineNumber = lineNum + 1,
                                columnNumber = 1,
                                message = "Discord event/command handler must be defined with 'async def' in discord.py",
                                severity = DiagnosticSeverity.ERROR,
                                sourceLine = nextLine
                            )
                        )
                    }
                }

                // Check missing colon on def, class, if, elif, else, for, while, try, except, finally
                val pyKeywords = listOf("def ", "class ", "if ", "elif ", "else:", "for ", "while ", "try:", "except:", "except ", "finally:")
                for (kw in pyKeywords) {
                    val pattern = kw.trimEnd()
                    if ((trimmed.startsWith("$pattern ") || trimmed == pattern || trimmed == "$pattern:") &&
                        !trimmed.endsWith(":") && !trimmed.contains("#") && !trimmed.endsWith("\\")
                    ) {
                        diagnostics.add(
                            CodeDiagnostic(
                                lineNumber = lineNum,
                                columnNumber = line.length,
                                message = "SyntaxError: Expected ':' at end of '$pattern' statement",
                                severity = DiagnosticSeverity.ERROR,
                                sourceLine = trimmed
                            )
                        )
                    }
                }
            }

            // Balanced brackets and quotes validator across code languages
            var inString = false
            var stringChar = ' '

            for ((cIdx, ch) in line.withIndex()) {
                if (!inString) {
                    if (ch == '"' || ch == '\'' || ch == '`') {
                        inString = true
                        stringChar = ch
                    } else if (ch == '(') {
                        openParenStack.add(lineNum to (cIdx + 1))
                    } else if (ch == ')') {
                        if (openParenStack.isEmpty()) {
                            diagnostics.add(
                                CodeDiagnostic(
                                    lineNumber = lineNum,
                                    columnNumber = cIdx + 1,
                                    message = "Unmatched closing parenthesis ')'",
                                    severity = DiagnosticSeverity.ERROR,
                                    sourceLine = trimmed
                                )
                            )
                        } else {
                            openParenStack.removeAt(openParenStack.size - 1)
                        }
                    } else if (ch == '{') {
                        openBraceStack.add(lineNum to (cIdx + 1))
                    } else if (ch == '}') {
                        if (openBraceStack.isEmpty()) {
                            diagnostics.add(
                                CodeDiagnostic(
                                    lineNumber = lineNum,
                                    columnNumber = cIdx + 1,
                                    message = "Unmatched closing brace '}'",
                                    severity = DiagnosticSeverity.ERROR,
                                    sourceLine = trimmed
                                )
                            )
                        } else {
                            openBraceStack.removeAt(openBraceStack.size - 1)
                        }
                    } else if (ch == '[') {
                        openBracketStack.add(lineNum to (cIdx + 1))
                    } else if (ch == ']') {
                        if (openBracketStack.isEmpty()) {
                            diagnostics.add(
                                CodeDiagnostic(
                                    lineNumber = lineNum,
                                    columnNumber = cIdx + 1,
                                    message = "Unmatched closing bracket ']'",
                                    severity = DiagnosticSeverity.ERROR,
                                    sourceLine = trimmed
                                )
                            )
                        } else {
                            openBracketStack.removeAt(openBracketStack.size - 1)
                        }
                    }
                } else {
                    if (ch == stringChar && (cIdx == 0 || line[cIdx - 1] != '\\')) {
                        inString = false
                    }
                }
            }

            // Unclosed string literal on same line
            if (inString && stringChar != '`') {
                diagnostics.add(
                    CodeDiagnostic(
                        lineNumber = lineNum,
                        columnNumber = line.length,
                        message = "Unterminated string literal ($stringChar)",
                        severity = DiagnosticSeverity.ERROR,
                        sourceLine = trimmed
                    )
                )
            }
        }

        // Report unclosed brackets at end of file
        openBraceStack.forEach { (l, c) ->
            diagnostics.add(
                CodeDiagnostic(
                    lineNumber = l,
                    columnNumber = c,
                    message = "Unclosed curly brace '{' was opened here and never closed",
                    severity = DiagnosticSeverity.ERROR,
                    sourceLine = lines.getOrNull(l - 1)?.trim() ?: ""
                )
            )
        }
        openParenStack.forEach { (l, c) ->
            diagnostics.add(
                CodeDiagnostic(
                    lineNumber = l,
                    columnNumber = c,
                    message = "Unclosed parenthesis '(' was opened here and never closed",
                    severity = DiagnosticSeverity.ERROR,
                    sourceLine = lines.getOrNull(l - 1)?.trim() ?: ""
                )
            )
        }
        openBracketStack.forEach { (l, c) ->
            diagnostics.add(
                CodeDiagnostic(
                    lineNumber = l,
                    columnNumber = c,
                    message = "Unclosed bracket '[' was opened here and never closed",
                    severity = DiagnosticSeverity.ERROR,
                    sourceLine = lines.getOrNull(l - 1)?.trim() ?: ""
                )
            )
        }

            diagnostics
        } catch (_: Throwable) {
            diagnostics
        }
    }

    private fun lintJson(code: String, lines: List<String>, diagnostics: MutableList<CodeDiagnostic>) {
        if (code.isBlank()) return
        try {
            if (code.trim().startsWith("[")) {
                org.json.JSONArray(code)
            } else {
                org.json.JSONObject(code)
            }
        } catch (e: org.json.JSONException) {
            // Find approximate line where the error occurred
            var errLine = lines.size
            var errCol = 1
            val msg = e.message ?: "Invalid JSON syntax"

            for ((idx, line) in lines.withIndex()) {
                val trimmed = line.trim()
                // Check single quotes in JSON
                if (trimmed.contains("'")) {
                    diagnostics.add(
                        CodeDiagnostic(
                            lineNumber = idx + 1,
                            columnNumber = line.indexOf('\'') + 1,
                            message = "JSON strings must use double quotes (\"), not single quotes",
                            severity = DiagnosticSeverity.ERROR,
                            sourceLine = trimmed
                        )
                    )
                    return
                }
                // Check trailing comma before closing brace/bracket
                if (trimmed.endsWith(",}") || trimmed.endsWith(",]")) {
                    diagnostics.add(
                        CodeDiagnostic(
                            lineNumber = idx + 1,
                            columnNumber = line.length,
                            message = "Trailing comma before closing bracket is illegal in JSON",
                            severity = DiagnosticSeverity.ERROR,
                            sourceLine = trimmed
                        )
                    )
                    return
                }
                // Check unquoted keys
                val unquotedKeyRegex = Regex("^[a-zA-Z0-9_]+\\s*:")
                if (unquotedKeyRegex.find(trimmed) != null) {
                    diagnostics.add(
                        CodeDiagnostic(
                            lineNumber = idx + 1,
                            columnNumber = 1,
                            message = "JSON object keys must be enclosed in double quotes",
                            severity = DiagnosticSeverity.ERROR,
                            sourceLine = trimmed
                        )
                    )
                    return
                }
            }

            diagnostics.add(
                CodeDiagnostic(
                    lineNumber = errLine,
                    columnNumber = errCol,
                    message = "JSON Syntax Error: $msg",
                    severity = DiagnosticSeverity.ERROR,
                    sourceLine = lines.lastOrNull()?.trim() ?: ""
                )
            )
        }
    }

    private fun lintEnv(lines: List<String>, diagnostics: MutableList<CodeDiagnostic>) {
        for ((idx, line) in lines.withIndex()) {
            val lineNum = idx + 1
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

            if (!trimmed.contains("=")) {
                diagnostics.add(
                    CodeDiagnostic(
                        lineNumber = lineNum,
                        columnNumber = 1,
                        message = "Invalid .env format: Expected 'KEY=VALUE' assignment",
                        severity = DiagnosticSeverity.ERROR,
                        sourceLine = trimmed
                    )
                )
            } else {
                val parts = trimmed.split("=", limit = 2)
                val key = parts[0]
                if (key.contains(" ")) {
                    diagnostics.add(
                        CodeDiagnostic(
                            lineNumber = lineNum,
                            columnNumber = 1,
                            message = "Invalid .env key: Variable names cannot contain spaces (use KEY=VALUE without spaces)",
                            severity = DiagnosticSeverity.WARNING,
                            sourceLine = trimmed
                        )
                    )
                }
            }
        }
    }

    private fun lintYaml(lines: List<String>, diagnostics: MutableList<CodeDiagnostic>) {
        for ((idx, line) in lines.withIndex()) {
            val lineNum = idx + 1
            // In YAML, tabs are strictly prohibited for indentation
            if (line.startsWith("\t") || (line.contains("\t") && !line.contains("\"") && !line.contains("'"))) {
                diagnostics.add(
                    CodeDiagnostic(
                        lineNumber = lineNum,
                        columnNumber = line.indexOf('\t') + 1,
                        message = "YAML syntax error: Tab characters are forbidden for indentation. Use spaces instead.",
                        severity = DiagnosticSeverity.ERROR,
                        sourceLine = line.trim()
                    )
                )
            }
        }
    }

    private fun lintHtmlXml(lines: List<String>, diagnostics: MutableList<CodeDiagnostic>) {
        val openTags = mutableListOf<Pair<String, Int>>() // tag name, line
        for ((idx, line) in lines.withIndex()) {
            val lineNum = idx + 1
            val trimmed = line.trim()
            if (trimmed.startsWith("<!--") || trimmed.startsWith("<!DOCTYPE")) continue

            val openMatch = Regex("<([a-zA-Z0-9]+)(\\s+[^>]*)*>").findAll(line)
            for (m in openMatch) {
                val tag = m.groupValues[1].lowercase()
                if (tag !in listOf("img", "br", "hr", "input", "meta", "link")) {
                    openTags.add(tag to lineNum)
                }
            }

            val closeMatch = Regex("</([a-zA-Z0-9]+)>").findAll(line)
            for (m in closeMatch) {
                val tag = m.groupValues[1].lowercase()
                val lastIdx = openTags.indexOfLast { it.first == tag }
                if (lastIdx != -1) {
                    openTags.removeAt(lastIdx)
                } else {
                    diagnostics.add(
                        CodeDiagnostic(
                            lineNumber = lineNum,
                            columnNumber = m.range.first + 1,
                            message = "Unexpected closing tag '</$tag>' without matching opening tag",
                            severity = DiagnosticSeverity.WARNING,
                            sourceLine = trimmed
                        )
                    )
                }
            }
        }
    }

    private fun lintShell(lines: List<String>, diagnostics: MutableList<CodeDiagnostic>) {
        var ifCount = 0
        var caseCount = 0
        var doCount = 0

        for ((idx, line) in lines.withIndex()) {
            val lineNum = idx + 1
            val words = line.trim().split(Regex("\\s+"))
            if (words.contains("if") && !line.trim().startsWith("#")) ifCount++
            if (words.contains("fi") && !line.trim().startsWith("#")) ifCount = (ifCount - 1).coerceAtLeast(0)
            if (words.contains("case") && !line.trim().startsWith("#")) caseCount++
            if (words.contains("esac") && !line.trim().startsWith("#")) caseCount = (caseCount - 1).coerceAtLeast(0)
            if (words.contains("do") && !line.trim().startsWith("#")) doCount++
            if (words.contains("done") && !line.trim().startsWith("#")) doCount = (doCount - 1).coerceAtLeast(0)
        }

        if (ifCount > 0) {
            diagnostics.add(
                CodeDiagnostic(
                    lineNumber = lines.size,
                    columnNumber = 1,
                    message = "Unclosed 'if' statement in shell script (missing matching 'fi')",
                    severity = DiagnosticSeverity.ERROR,
                    sourceLine = lines.lastOrNull()?.trim() ?: ""
                )
            )
        }
        if (caseCount > 0) {
            diagnostics.add(
                CodeDiagnostic(
                    lineNumber = lines.size,
                    columnNumber = 1,
                    message = "Unclosed 'case' statement in shell script (missing matching 'esac')",
                    severity = DiagnosticSeverity.ERROR,
                    sourceLine = lines.lastOrNull()?.trim() ?: ""
                )
            )
        }
        if (doCount > 0) {
            diagnostics.add(
                CodeDiagnostic(
                    lineNumber = lines.size,
                    columnNumber = 1,
                    message = "Unclosed loop in shell script (missing matching 'done')",
                    severity = DiagnosticSeverity.ERROR,
                    sourceLine = lines.lastOrNull()?.trim() ?: ""
                )
            )
        }
    }
}
