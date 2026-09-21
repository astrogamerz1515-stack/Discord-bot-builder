package com.example.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.example.ui.theme.SyntaxComment
import com.example.ui.theme.SyntaxDiscord
import com.example.ui.theme.SyntaxFunction
import com.example.ui.theme.SyntaxKeyword
import com.example.ui.theme.SyntaxNumber
import com.example.ui.theme.SyntaxString
import com.example.ui.theme.SyntaxType

class SyntaxHighlightTransformation(private val filePath: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val highlighted = SyntaxHighlighter.highlight(text.text, filePath)
        return TransformedText(
            text = highlighted,
            offsetMapping = OffsetMapping.Identity
        )
    }
}

object SyntaxHighlighter {

    private val JS_KEYWORDS = setOf(
        "const", "let", "var", "function", "return", "if", "else", "for", "while", "do",
        "async", "await", "import", "export", "from", "require", "class", "new",
        "this", "try", "catch", "finally", "throw", "typeof", "instanceof", "switch", "case", "default",
        "module", "exports", "interface", "type", "extends", "implements", "super", "yield", "of", "in"
    )

    private val PY_KEYWORDS = setOf(
        "def", "class", "return", "if", "elif", "else", "for", "while",
        "async", "await", "import", "from", "as", "try", "except", "finally",
        "raise", "with", "lambda", "pass", "yield", "global", "not", "and", "or", "in", "is", "self"
    )

    private val KOTLIN_GRADLE_KEYWORDS = setOf(
        "val", "var", "fun", "class", "object", "interface", "plugins", "dependencies", "implementation",
        "apply", "import", "package", "return", "override", "private", "public", "protected", "internal",
        "companion", "data", "sealed", "when", "is", "as", "android", "defaultConfig", "buildTypes"
    )

    private val RUST_KEYWORDS = setOf(
        "fn", "let", "mut", "pub", "struct", "enum", "impl", "trait", "match",
        "if", "else", "loop", "while", "for", "in", "return", "async", "await",
        "use", "mod", "crate", "type", "where", "self", "Self"
    )

    private val GO_KEYWORDS = setOf(
        "package", "import", "func", "return", "var", "type", "struct", "interface",
        "if", "else", "for", "range", "go", "chan", "select", "defer", "switch", "case"
    )

    private val BOOLEANS_NULLS = setOf(
        "true", "false", "null", "undefined", "None", "True", "False", "nil"
    )

    private val DISCORD_TERMS = setOf(
        "Client", "GatewayIntentBits", "Partials", "EmbedBuilder", "ActionRowBuilder",
        "ButtonBuilder", "ButtonStyle", "SlashCommandBuilder", "StringSelectMenuBuilder",
        "Collection", "Intents", "Events", "PermissionsBitField", "ActivityType",
        "commands", "app_commands", "interaction", "Interaction", "message", "Message",
        "channel", "Channel", "guild", "Guild", "member", "Member", "user", "User",
        "discord", "serenity", "poise", "discordgo", "JDA", "JDABuilder", "DiscordSocketClient",
        "ChatInputCommandInteraction", "ButtonInteraction", "SelectMenuInteraction"
    )

    fun highlight(code: String, filePath: String): AnnotatedString {
        if (code.isEmpty()) return AnnotatedString("")
        val extension = filePath.substringAfterLast('.', "").lowercase()

        return buildAnnotatedString {
            append(code)

            val lines = code.lines()
            var currentOffset = 0

            for (line in lines) {
                val trimmed = line.trimStart()

                // Check for single-line comments
                if (trimmed.startsWith("//") || trimmed.startsWith("#")) {
                    val start = currentOffset + (line.length - trimmed.length)
                    val end = currentOffset + line.length
                    addStyle(SpanStyle(color = SyntaxComment, fontStyle = FontStyle.Italic), start, end)
                    currentOffset += line.length + 1
                    continue
                }

                // Check for inline comments //
                val commentIndex = findInlineCommentIndex(line, extension)
                val codeSegment = if (commentIndex != -1) line.substring(0, commentIndex) else line

                if (commentIndex != -1) {
                    val start = currentOffset + commentIndex
                    val end = currentOffset + line.length
                    addStyle(SpanStyle(color = SyntaxComment, fontStyle = FontStyle.Italic), start, end)
                }

                // Match strings
                val stringRegex = Regex("(\"[^\"]*\"|'[^']*'|`[^`]*`)")
                for (match in stringRegex.findAll(codeSegment)) {
                    val start = currentOffset + match.range.first
                    val end = currentOffset + match.range.last + 1
                    addStyle(SpanStyle(color = SyntaxString), start, end)
                }

                // Match Python / JS / Kotlin decorators (@client.event, @bot.command, @Composable)
                val decoratorRegex = Regex("@[A-Za-z_][A-Za-z0-9_.]*")
                for (match in decoratorRegex.findAll(codeSegment)) {
                    val start = currentOffset + match.range.first
                    val end = currentOffset + match.range.last + 1
                    addStyle(SpanStyle(color = SyntaxType, fontWeight = FontWeight.SemiBold), start, end)
                }

                // Match function calls like client.on(...), interaction.reply(...), print(...)
                val funcCallRegex = Regex("\\b([A-Za-z_][A-Za-z0-9_]*)(?=\\s*\\()")
                for (match in funcCallRegex.findAll(codeSegment)) {
                    val funcName = match.groupValues[1]
                    if (!isKeyword(funcName, extension)) {
                        val start = currentOffset + match.range.first
                        val end = currentOffset + match.range.first + funcName.length
                        addStyle(SpanStyle(color = SyntaxFunction), start, end)
                    }
                }

                // Match words: keywords, Discord entities, booleans, types
                val wordRegex = Regex("\\b[A-Za-z_][A-Za-z0-9_]*\\b")
                for (match in wordRegex.findAll(codeSegment)) {
                    val word = match.value
                    val start = currentOffset + match.range.first
                    val end = currentOffset + match.range.last + 1

                    when {
                        BOOLEANS_NULLS.contains(word) -> {
                            addStyle(SpanStyle(color = SyntaxNumber, fontWeight = FontWeight.Bold), start, end)
                        }
                        DISCORD_TERMS.contains(word) -> {
                            addStyle(SpanStyle(color = SyntaxDiscord, fontWeight = FontWeight.Bold), start, end)
                        }
                        isKeyword(word, extension) -> {
                            addStyle(SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.Bold), start, end)
                        }
                        word.firstOrNull()?.isUpperCase() == true -> {
                            addStyle(SpanStyle(color = SyntaxType, fontWeight = FontWeight.Medium), start, end)
                        }
                    }
                }

                // Match numbers (integers, floats, hex)
                val numberRegex = Regex("\\b(0x[0-9a-fA-F]+|\\d+(\\.\\d+)?)\\b")
                for (match in numberRegex.findAll(codeSegment)) {
                    val start = currentOffset + match.range.first
                    val end = currentOffset + match.range.last + 1
                    addStyle(SpanStyle(color = SyntaxNumber), start, end)
                }

                currentOffset += line.length + 1
            }
        }
    }

    private fun findInlineCommentIndex(line: String, extension: String): Int {
        var inQuote = false
        var quoteChar = ' '
        for (i in line.indices) {
            val c = line[i]
            if (inQuote) {
                if (c == quoteChar && (i == 0 || line[i - 1] != '\\')) {
                    inQuote = false
                }
            } else {
                if (c == '"' || c == '\'' || c == '`') {
                    inQuote = true
                    quoteChar = c
                } else if (c == '/' && i + 1 < line.length && line[i + 1] == '/') {
                    return i
                } else if (c == '#' && (extension == "py" || extension == "sh" || extension == "env" || extension == "properties")) {
                    return i
                }
            }
        }
        return -1
    }

    private fun isKeyword(word: String, extension: String): Boolean {
        return when (extension) {
            "js", "ts", "jsx", "tsx" -> JS_KEYWORDS.contains(word)
            "py" -> PY_KEYWORDS.contains(word)
            "kt", "kts", "gradle" -> KOTLIN_GRADLE_KEYWORDS.contains(word)
            "rs" -> RUST_KEYWORDS.contains(word)
            "go" -> GO_KEYWORDS.contains(word)
            "java", "cs" -> JS_KEYWORDS.contains(word) || word == "class" || word == "public" || word == "private" || word == "static" || word == "void"
            else -> JS_KEYWORDS.contains(word) || PY_KEYWORDS.contains(word)
        }
    }
}

