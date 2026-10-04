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
        return try {
            val highlighted = SyntaxHighlighter.highlight(text.text, filePath)
            TransformedText(
                text = highlighted,
                offsetMapping = OffsetMapping.Identity
            )
        } catch (_: Throwable) {
            TransformedText(
                text = text,
                offsetMapping = OffsetMapping.Identity
            )
        }
    }
}

object SyntaxHighlighter {

    private const val MAX_HIGHLIGHT_CHARS = 60_000

    // Precompiled Static Regexes to avoid per-line instantiation overhead
    private val STRING_REGEX = Regex("(\"[^\"]*\"|'[^']*'|`[^`]*`)")
    private val TAG_REGEX = Regex("</?[A-Za-z0-9_-]+(\\s+[^>]*)?>?")
    private val DECORATOR_REGEX = Regex("(@[A-Za-z_][A-Za-z0-9_.]*|#[a-zA-Z_]+)")
    private val FUNC_CALL_REGEX = Regex("\\b([A-Za-z_][A-Za-z0-9_]*)(?=\\s*\\()")
    private val WORD_REGEX = Regex("\\b[A-Za-z_][A-Za-z0-9_]*\\b")
    private val NUMBER_REGEX = Regex("\\b(0x[0-9a-fA-F]+|\\d+(\\.\\d+)?)\\b")

    private val JS_KEYWORDS = setOf(
        "const", "let", "var", "function", "return", "if", "else", "for", "while", "do",
        "async", "await", "import", "export", "from", "require", "class", "new",
        "this", "try", "catch", "finally", "throw", "typeof", "instanceof", "switch", "case", "default",
        "module", "exports", "interface", "type", "extends", "implements", "super", "yield", "of", "in",
        "debugger", "static", "get", "set"
    )

    private val PY_KEYWORDS = setOf(
        "def", "class", "return", "if", "elif", "else", "for", "while",
        "async", "await", "import", "from", "as", "try", "except", "finally",
        "raise", "with", "lambda", "pass", "yield", "global", "nonlocal", "not", "and", "or", "in", "is", "self",
        "assert", "del", "match", "case"
    )

    private val KOTLIN_GRADLE_KEYWORDS = setOf(
        "val", "var", "fun", "class", "object", "interface", "plugins", "dependencies", "implementation",
        "apply", "import", "package", "return", "override", "private", "public", "protected", "internal",
        "companion", "data", "sealed", "when", "is", "as", "android", "defaultConfig", "buildTypes",
        "inline", "reified", "suspend", "constructor", "init", "by", "lazy", "where"
    )

    private val JAVA_KEYWORDS = setOf(
        "public", "private", "protected", "class", "interface", "enum", "extends", "implements",
        "static", "final", "void", "return", "new", "this", "super", "abstract", "synchronized",
        "try", "catch", "finally", "throw", "throws", "if", "else", "for", "while", "do", "switch", "case",
        "default", "break", "continue", "instanceof", "package", "import", "boolean", "int", "long", "float", "double"
    )

    private val RUST_KEYWORDS = setOf(
        "fn", "let", "mut", "pub", "struct", "enum", "impl", "trait", "match",
        "if", "else", "loop", "while", "for", "in", "return", "async", "await",
        "use", "mod", "crate", "type", "where", "self", "Self", "const", "ref", "static", "unsafe"
    )

    private val GO_KEYWORDS = setOf(
        "package", "import", "func", "return", "var", "type", "struct", "interface",
        "if", "else", "for", "range", "go", "chan", "select", "defer", "switch", "case", "fallthrough"
    )

    private val LUA_KEYWORDS = setOf(
        "local", "function", "end", "then", "do", "while", "repeat", "until", "if", "elseif", "else",
        "return", "break", "nil", "true", "false", "and", "or", "not", "for", "in", "goto"
    )

    private val PHP_KEYWORDS = setOf(
        "php", "echo", "function", "class", "public", "private", "protected", "return", "if", "else", "elseif",
        "foreach", "as", "while", "new", "extends", "implements", "static", "try", "catch", "finally", "throw",
        "use", "namespace", "include", "require", "require_once", "var", "const", "final", "abstract", "interface"
    )

    private val C_CPP_KEYWORDS = setOf(
        "int", "char", "float", "double", "void", "long", "short", "unsigned", "signed", "struct",
        "class", "public", "private", "protected", "virtual", "override", "const", "static", "auto",
        "template", "typename", "namespace", "using", "include", "typedef", "sizeof", "new", "delete",
        "return", "if", "else", "for", "while", "do", "switch", "case", "default", "break", "continue"
    )

    private val SQL_KEYWORDS = setOf(
        "select", "insert", "update", "delete", "from", "where", "and", "or", "not", "join", "left", "right",
        "inner", "outer", "group", "by", "order", "having", "limit", "offset", "create", "table", "drop",
        "alter", "index", "primary", "key", "foreign", "references", "null", "is", "like", "in", "values",
        "as", "distinct", "union", "all", "case", "when", "then", "end", "begin", "commit", "rollback"
    )

    private val SHELL_KEYWORDS = setOf(
        "if", "then", "elif", "else", "fi", "for", "while", "until", "do", "done", "case", "esac",
        "in", "function", "select", "echo", "export", "source", "alias", "read", "local", "return", "exit",
        "set", "unset", "chmod", "chown", "mkdir", "rm", "cp", "mv", "grep", "cat", "curl", "cd", "sudo"
    )

    private val HTML_XML_TAG_KEYWORDS = setOf(
        "html", "head", "body", "div", "span", "p", "a", "button", "input", "form", "h1", "h2", "h3",
        "h4", "h5", "h6", "ul", "ol", "li", "table", "tr", "td", "th", "section", "article", "header",
        "footer", "nav", "main", "script", "style", "link", "meta", "title", "img", "iframe", "svg", "path"
    )

    private val CSS_KEYWORDS = setOf(
        "color", "background", "margin", "padding", "border", "display", "flex", "grid", "width", "height",
        "font", "text", "align", "justify", "position", "top", "left", "right", "bottom", "z-index",
        "opacity", "transition", "transform", "animation", "box-shadow", "border-radius", "overflow", "none",
        "block", "inline", "auto", "important", "inherit", "root"
    )

    private val BOOLEANS_NULLS = setOf(
        "true", "false", "null", "undefined", "None", "True", "False", "nil", "TRUE", "FALSE", "NULL"
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

    private fun AnnotatedString.Builder.safeAddStyle(style: SpanStyle, start: Int, end: Int, maxLen: Int) {
        val s = start.coerceIn(0, maxLen)
        val e = end.coerceIn(0, maxLen)
        if (s < e) {
            try {
                addStyle(style, s, e)
            } catch (_: Throwable) {}
        }
    }

    fun highlight(code: String, filePath: String): AnnotatedString {
        if (code.isEmpty()) return AnnotatedString("")
        val extension = filePath.substringAfterLast('.', "").lowercase()

        return buildAnnotatedString {
            append(code)

            val codeLen = code.length
            val limit = minOf(codeLen, MAX_HIGHLIGHT_CHARS)

            var lineStart = 0
            while (lineStart < limit) {
                var lineEnd = code.indexOf('\n', lineStart)
                val nextLineStart = if (lineEnd != -1) lineEnd + 1 else codeLen
                if (lineEnd == -1 || lineEnd > limit) {
                    lineEnd = minOf(if (lineEnd == -1) codeLen else lineEnd, limit)
                }

                // Exclude '\r' if present before '\n'
                val actualLineEnd = if (lineEnd > lineStart && code[lineEnd - 1] == '\r') lineEnd - 1 else lineEnd
                val line = code.substring(lineStart, actualLineEnd)
                val trimmed = line.trimStart()

                // Special handling for Markdown (.md)
                if (extension == "md") {
                    highlightMarkdownLine(line, lineStart, this, codeLen)
                    lineStart = nextLineStart
                    continue
                }

                // Special handling for Environment files (.env)
                if (extension == "env" || extension == "properties") {
                    highlightEnvLine(line, lineStart, this, codeLen)
                    lineStart = nextLineStart
                    continue
                }

                // Check for single-line comments
                if (trimmed.startsWith("//") || trimmed.startsWith("#") || trimmed.startsWith("--") || trimmed.startsWith("<!--")) {
                    val start = lineStart + (line.length - trimmed.length)
                    val end = lineStart + line.length
                    safeAddStyle(SpanStyle(color = SyntaxComment, fontStyle = FontStyle.Italic), start, end, codeLen)
                    lineStart = nextLineStart
                    continue
                }

                // Check for inline comments
                val commentIndex = findInlineCommentIndex(line, extension)
                val codeSegment = if (commentIndex != -1) line.substring(0, commentIndex) else line

                if (commentIndex != -1) {
                    val start = lineStart + commentIndex
                    val end = lineStart + line.length
                    safeAddStyle(SpanStyle(color = SyntaxComment, fontStyle = FontStyle.Italic), start, end, codeLen)
                }

                // Match strings (single, double quotes, backticks)
                for (match in STRING_REGEX.findAll(codeSegment)) {
                    val start = lineStart + match.range.first
                    val end = lineStart + match.range.last + 1
                    safeAddStyle(SpanStyle(color = SyntaxString), start, end, codeLen)
                }

                // Match HTML/XML tags <tag> and </tag>
                if (extension in listOf("html", "htm", "xml", "svg", "jsx", "tsx")) {
                    for (match in TAG_REGEX.findAll(codeSegment)) {
                        val start = lineStart + match.range.first
                        val end = lineStart + match.range.last + 1
                        safeAddStyle(SpanStyle(color = SyntaxFunction, fontWeight = FontWeight.Bold), start, end, codeLen)
                    }
                }

                // Match Decorators / Directives (@decorator, #include, etc.)
                for (match in DECORATOR_REGEX.findAll(codeSegment)) {
                    val start = lineStart + match.range.first
                    val end = lineStart + match.range.last + 1
                    safeAddStyle(SpanStyle(color = SyntaxType, fontWeight = FontWeight.SemiBold), start, end, codeLen)
                }

                // Match function calls like client.on(...), interaction.reply(...), print(...)
                for (match in FUNC_CALL_REGEX.findAll(codeSegment)) {
                    val funcName = match.groupValues[1]
                    if (!isKeyword(funcName, extension)) {
                        val start = lineStart + match.range.first
                        val end = lineStart + match.range.first + funcName.length
                        safeAddStyle(SpanStyle(color = SyntaxFunction), start, end, codeLen)
                    }
                }

                // Match words: keywords, Discord entities, booleans, types
                for (match in WORD_REGEX.findAll(codeSegment)) {
                    val word = match.value
                    val start = lineStart + match.range.first
                    val end = lineStart + match.range.last + 1

                    when {
                        BOOLEANS_NULLS.contains(word) -> {
                            safeAddStyle(SpanStyle(color = SyntaxNumber, fontWeight = FontWeight.Bold), start, end, codeLen)
                        }
                        DISCORD_TERMS.contains(word) -> {
                            safeAddStyle(SpanStyle(color = SyntaxDiscord, fontWeight = FontWeight.Bold), start, end, codeLen)
                        }
                        isKeyword(word, extension) -> {
                            safeAddStyle(SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.Bold), start, end, codeLen)
                        }
                        word.firstOrNull()?.isUpperCase() == true -> {
                            safeAddStyle(SpanStyle(color = SyntaxType, fontWeight = FontWeight.Medium), start, end, codeLen)
                        }
                    }
                }

                // Match numbers (integers, floats, hex)
                for (match in NUMBER_REGEX.findAll(codeSegment)) {
                    val start = lineStart + match.range.first
                    val end = lineStart + match.range.last + 1
                    safeAddStyle(SpanStyle(color = SyntaxNumber), start, end, codeLen)
                }

                lineStart = nextLineStart
            }
        }
    }

    private fun highlightMarkdownLine(line: String, offset: Int, builder: AnnotatedString.Builder, maxLen: Int) {
        val trimmed = line.trimStart()
        val end = offset + line.length
        when {
            trimmed.startsWith("#") -> {
                builder.safeAddStyle(SpanStyle(color = SyntaxFunction, fontWeight = FontWeight.Bold), offset, end, maxLen)
            }
            trimmed.startsWith("```") -> {
                builder.safeAddStyle(SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.Bold), offset, end, maxLen)
            }
            trimmed.startsWith(">") -> {
                builder.safeAddStyle(SpanStyle(color = SyntaxComment, fontStyle = FontStyle.Italic), offset, end, maxLen)
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("1. ") -> {
                builder.safeAddStyle(SpanStyle(color = SyntaxType, fontWeight = FontWeight.Medium), offset, end, maxLen)
            }
        }
    }

    private fun highlightEnvLine(line: String, offset: Int, builder: AnnotatedString.Builder, maxLen: Int) {
        val trimmed = line.trim()
        val lineEnd = offset + line.length
        if (trimmed.startsWith("#")) {
            builder.safeAddStyle(SpanStyle(color = SyntaxComment, fontStyle = FontStyle.Italic), offset, lineEnd, maxLen)
            return
        }
        val eqIdx = line.indexOf('=')
        if (eqIdx != -1) {
            // Key
            builder.safeAddStyle(SpanStyle(color = SyntaxType, fontWeight = FontWeight.Bold), offset, offset + eqIdx, maxLen)
            // Value
            if (eqIdx + 1 < line.length) {
                builder.safeAddStyle(SpanStyle(color = SyntaxString), offset + eqIdx + 1, lineEnd, maxLen)
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
                } else if (c == '-' && i + 1 < line.length && line[i + 1] == '-' && (extension == "sql" || extension == "lua")) {
                    return i
                } else if (c == '#' && (extension in listOf("py", "sh", "bash", "zsh", "env", "properties", "yaml", "yml", "toml", "conf", "php"))) {
                    return i
                }
            }
        }
        return -1
    }

    private fun isKeyword(word: String, extension: String): Boolean {
        val lowerWord = word.lowercase()
        return when (extension) {
            "js", "ts", "jsx", "tsx", "mjs", "cjs" -> JS_KEYWORDS.contains(word)
            "py" -> PY_KEYWORDS.contains(word)
            "kt", "kts", "gradle" -> KOTLIN_GRADLE_KEYWORDS.contains(word)
            "java" -> JAVA_KEYWORDS.contains(word)
            "rs" -> RUST_KEYWORDS.contains(word)
            "go" -> GO_KEYWORDS.contains(word)
            "lua" -> LUA_KEYWORDS.contains(lowerWord)
            "php" -> PHP_KEYWORDS.contains(lowerWord)
            "c", "cpp", "h", "hpp", "cs" -> C_CPP_KEYWORDS.contains(word) || JS_KEYWORDS.contains(word)
            "sql" -> SQL_KEYWORDS.contains(lowerWord)
            "sh", "bash", "zsh" -> SHELL_KEYWORDS.contains(lowerWord)
            "html", "htm", "xml" -> HTML_XML_TAG_KEYWORDS.contains(lowerWord)
            "css", "scss" -> CSS_KEYWORDS.contains(lowerWord)
            "json" -> BOOLEANS_NULLS.contains(word)
            "yaml", "yml", "toml" -> BOOLEANS_NULLS.contains(word)
            else -> JS_KEYWORDS.contains(word) || PY_KEYWORDS.contains(word)
        }
    }
}
