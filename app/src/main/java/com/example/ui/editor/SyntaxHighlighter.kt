package com.example.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.SyntaxComment
import com.example.ui.theme.SyntaxDiscord
import com.example.ui.theme.SyntaxFunction
import com.example.ui.theme.SyntaxKeyword
import com.example.ui.theme.SyntaxNumber
import com.example.ui.theme.SyntaxString
import com.example.ui.theme.SyntaxType

object SyntaxHighlighter {

    private val JS_KEYWORDS = setOf(
        "const", "let", "var", "function", "return", "if", "else", "for", "while",
        "async", "await", "import", "export", "from", "require", "class", "new",
        "this", "try", "catch", "finally", "throw", "typeof", "instanceof", "switch", "case", "default"
    )

    private val PY_KEYWORDS = setOf(
        "def", "class", "return", "if", "elif", "else", "for", "while",
        "async", "await", "import", "from", "as", "try", "except", "finally",
        "raise", "with", "lambda", "pass", "yield", "global", "not", "and", "or", "in", "is"
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

    private val DISCORD_TERMS = setOf(
        "Client", "GatewayIntentBits", "Partials", "EmbedBuilder", "ActionRowBuilder",
        "ButtonBuilder", "ButtonStyle", "SlashCommandBuilder", "Collection", "Intents",
        "commands", "app_commands", "interaction", "message", "channel", "guild",
        "discord", "serenity", "poise", "discordgo", "JDA", "JDABuilder", "DiscordSocketClient"
    )

    fun highlight(code: String, filePath: String): AnnotatedString {
        val extension = filePath.substringAfterLast('.', "").lowercase()
        return buildAnnotatedString {
            append(code)

            val lines = code.lines()
            var currentOffset = 0

            for (line in lines) {
                val trimmed = line.trimStart()

                // Check for full-line comments
                if (trimmed.startsWith("//") || trimmed.startsWith("#")) {
                    val start = currentOffset + (line.length - trimmed.length)
                    val end = currentOffset + line.length
                    addStyle(SpanStyle(color = SyntaxComment, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), start, end)
                    currentOffset += line.length + 1
                    continue
                }

                // Match strings
                val stringRegex = Regex("(\"[^\"]*\"|'[^']*'|`[^`]*`)")
                for (match in stringRegex.findAll(line)) {
                    val start = currentOffset + match.range.first
                    val end = currentOffset + match.range.last + 1
                    addStyle(SpanStyle(color = SyntaxString), start, end)
                }

                // Match keywords and identifiers
                val wordRegex = Regex("\\b[A-Za-z_][A-Za-z0-9_]*\\b")
                for (match in wordRegex.findAll(line)) {
                    val word = match.value
                    val start = currentOffset + match.range.first
                    val end = currentOffset + match.range.last + 1

                    when {
                        DISCORD_TERMS.contains(word) -> {
                            addStyle(SpanStyle(color = SyntaxDiscord, fontWeight = FontWeight.SemiBold), start, end)
                        }
                        isKeyword(word, extension) -> {
                            addStyle(SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.Bold), start, end)
                        }
                        word.firstOrNull()?.isUpperCase() == true -> {
                            addStyle(SpanStyle(color = SyntaxType), start, end)
                        }
                    }
                }

                // Match numbers
                val numberRegex = Regex("\\b\\d+(\\.\\d+)?\\b")
                for (match in numberRegex.findAll(line)) {
                    val start = currentOffset + match.range.first
                    val end = currentOffset + match.range.last + 1
                    addStyle(SpanStyle(color = SyntaxNumber), start, end)
                }

                currentOffset += line.length + 1
            }
        }
    }

    private fun isKeyword(word: String, extension: String): Boolean {
        return when (extension) {
            "js", "ts", "jsx", "tsx" -> JS_KEYWORDS.contains(word)
            "py" -> PY_KEYWORDS.contains(word)
            "rs" -> RUST_KEYWORDS.contains(word)
            "go" -> GO_KEYWORDS.contains(word)
            "java", "cs" -> JS_KEYWORDS.contains(word) || word == "class" || word == "public" || word == "private" || word == "static" || word == "void"
            else -> JS_KEYWORDS.contains(word) || PY_KEYWORDS.contains(word)
        }
    }
}
