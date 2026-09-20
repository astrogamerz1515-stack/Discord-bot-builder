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

object CodeLinter {

    fun lintCode(code: String, filePath: String): List<CodeDiagnostic> {
        val diagnostics = mutableListOf<CodeDiagnostic>()
        val lines = code.lines()
        val extension = filePath.substringAfterLast('.', "").lowercase()

        val openParenStack = mutableListOf<Pair<Int, Int>>() // line, col
        val openBraceStack = mutableListOf<Pair<Int, Int>>()
        val openBracketStack = mutableListOf<Pair<Int, Int>>()

        var inBlockComment = false

        for ((idx, line) in lines.withIndex()) {
            val lineNum = idx + 1
            val trimmed = line.trim()

            // Check JS/TS/Python specific patterns
            if (extension in listOf("js", "ts", "jsx", "tsx")) {
                // Check missing await on interaction replies
                if ((line.contains("interaction.reply(") || line.contains("interaction.response.send_message(")) &&
                    !line.contains("await") && !trimmed.startsWith("//")
                ) {
                    diagnostics.add(
                        CodeDiagnostic(
                            lineNumber = lineNum,
                            columnNumber = line.indexOf("interaction") + 1,
                            message = "Missing 'await' keyword before interaction response call",
                            severity = DiagnosticSeverity.WARNING,
                            sourceLine = trimmed
                        )
                    )
                }

                // Check undefined Discord client token
                if (line.contains("client.login()") || line.contains("client.login('')") || line.contains("client.login(\"\")")) {
                    diagnostics.add(
                        CodeDiagnostic(
                            lineNumber = lineNum,
                            columnNumber = line.indexOf("client.login") + 1,
                            message = "Bot token is empty. Pass process.env.DISCORD_TOKEN or your bot secret.",
                            severity = DiagnosticSeverity.ERROR,
                            sourceLine = trimmed
                        )
                    )
                }

                // Check missing new EmbedBuilder()
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
            } else if (extension == "py") {
                // Check Python async def missing on bot.tree.command or @client.event
                if ((trimmed.startsWith("@bot.tree.command") || trimmed.startsWith("@client.event") || trimmed.startsWith("@bot.command")) &&
                    idx + 1 < lines.size
                ) {
                    val nextLine = lines[idx + 1].trim()
                    if (nextLine.startsWith("def ") && !nextLine.startsWith("async def ")) {
                        diagnostics.add(
                            CodeDiagnostic(
                                lineNumber = lineNum + 1,
                                columnNumber = 1,
                                message = "Discord event / command handlers in discord.py must be defined with 'async def'",
                                severity = DiagnosticSeverity.ERROR,
                                sourceLine = nextLine
                            )
                        )
                    }
                }

                // Check missing colon on def, if, for, while, class
                val pyBlockKeywords = listOf("def ", "class ", "if ", "elif ", "else:", "for ", "while ", "try:", "except")
                for (kw in pyBlockKeywords) {
                    if (trimmed.startsWith(kw) && !trimmed.endsWith(":") && !trimmed.contains("#") && !trimmed.endsWith("\\")) {
                        diagnostics.add(
                            CodeDiagnostic(
                                lineNumber = lineNum,
                                columnNumber = line.length,
                                message = "SyntaxError: expected ':' at end of '$kw' statement",
                                severity = DiagnosticSeverity.ERROR,
                                sourceLine = trimmed
                            )
                        )
                    }
                }
            }

            // Balanced brackets and quotes validator across all languages
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

            // Unclosed string on same line (unless template literal ` in JS)
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
                    message = "Unclosed curly brace '{' was opened here",
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
                    message = "Unclosed parenthesis '(' was opened here",
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
                    message = "Unclosed bracket '[' was opened here",
                    severity = DiagnosticSeverity.ERROR,
                    sourceLine = lines.getOrNull(l - 1)?.trim() ?: ""
                )
            )
        }

        return diagnostics
    }
}
