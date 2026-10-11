package com.example.ui.editor.intellisense

object InlineSuggestionEngine {

    /**
     * Computes an inline code suggestion based on current code context and cursor position.
     */
    fun computeInlineSuggestion(
        code: String,
        cursorIndex: Int,
        filePath: String
    ): InlineSuggestion? {
        if (code.isEmpty()) return null
        val safeCursor = cursorIndex.coerceIn(0, code.length)
        val ext = filePath.substringAfterLast('.', "").lowercase()

        // Find the start of current line and text up to cursor
        val lineStart = code.lastIndexOf('\n', (safeCursor - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val currentLineBeforeCursor = code.substring(lineStart, safeCursor)
        val trimmedLine = currentLineBeforeCursor.trimStart()

        // 1. Contextual Pattern Predictions (Highest accuracy)
        val patternCompletion = when (ext) {
            "py" -> matchPythonPattern(trimmedLine)
            "ts", "tsx" -> matchTypeScriptPattern(trimmedLine) ?: matchJavaScriptPattern(trimmedLine)
            "json" -> matchJsonPattern(trimmedLine)
            "kt", "kts" -> matchKotlinPattern(trimmedLine)
            "lua" -> matchLuaPattern(trimmedLine)
            "sql" -> matchSqlPattern(trimmedLine)
            "sh", "bash" -> matchShellPattern(trimmedLine)
            "env", "properties" -> matchEnvPattern(trimmedLine)
            else -> matchJavaScriptPattern(trimmedLine)
        }

        val triggerSource = when (ext) {
            "py" -> "discord.py Copilot"
            "ts", "tsx" -> "TypeScript Copilot"
            "json" -> "JSON Copilot"
            "kt", "kts" -> "Kotlin Copilot"
            "lua" -> "Lua Copilot"
            "sql" -> "SQL Copilot"
            "sh", "bash" -> "Shell Copilot"
            "env", "properties" -> "ENV Copilot"
            else -> "Discord.js Copilot"
        }

        if (patternCompletion != null) {
            val preview = patternCompletion.takeWhile { it != '\n' }.take(60)
            val nextWord = extractNextWord(patternCompletion)
            return InlineSuggestion(
                completionText = patternCompletion,
                displayPreview = preview,
                triggerSource = triggerSource,
                cursorOffset = safeCursor,
                nextWord = nextWord
            )
        }

        // 2. Member Access Completion (e.g. "interaction.rep", "client.on", "embed.set", "client:on")
        val isLua = ext == "lua"
        val memberMatch = if (isLua) {
            Regex("([A-Za-z0-9_$]+)[:.]([A-Za-z0-9_$]*)$").find(currentLineBeforeCursor)
        } else {
            Regex("([A-Za-z0-9_$]+)\\.([A-Za-z0-9_$]*)$").find(currentLineBeforeCursor)
        }

        if (memberMatch != null) {
            val objectName = memberMatch.groupValues[1]
            val memberPrefix = memberMatch.groupValues[2]
            val separator = if (isLua && currentLineBeforeCursor.contains(':')) ":" else "."
            val fullPrefix = "$objectName$separator"

            val registryItems = IntelliSenseRegistry.getItemsForFile(filePath)

            val matchingItem = registryItems.firstOrNull { item ->
                (item.label.startsWith(fullPrefix, ignoreCase = true) || item.triggerPrefix.equals(fullPrefix, ignoreCase = true)) &&
                item.label.substringAfterLast(separator).startsWith(memberPrefix, ignoreCase = true)
            }

            if (matchingItem != null) {
                val memberName = matchingItem.label.substringAfterLast(separator)
                val remainder = memberName.removePrefix(memberPrefix)

                // Build full completion snippet with clean parameters
                val insertSnippet = matchingItem.insertText
                    .replace("$1", "")
                    .replace("$2", "")
                    .replace("$3", "")

                val completionText = if (insertSnippet.contains(fullPrefix)) {
                    insertSnippet.substringAfter(fullPrefix + memberPrefix, remainder)
                } else if (insertSnippet.startsWith(memberName)) {
                    insertSnippet.removePrefix(memberPrefix)
                } else {
                    remainder
                }

                if (completionText.isNotBlank()) {
                    val preview = completionText.takeWhile { it != '\n' }.take(50)
                    return InlineSuggestion(
                        completionText = completionText,
                        displayPreview = preview,
                        triggerSource = matchingItem.category,
                        cursorOffset = safeCursor,
                        nextWord = extractNextWord(completionText)
                    )
                }
            }
        }

        // 3. In-File Identifiers Completion (functions, constants, variables from same file)
        val tokenMatch = Regex("([A-Za-z0-9_$]{2,})$").find(currentLineBeforeCursor)
        if (tokenMatch != null) {
            val token = tokenMatch.value
            val inId = findInFileIdentifier(code, token)
            if (inId != null && inId.length > token.length && inId.startsWith(token, ignoreCase = false)) {
                val remainder = inId.removePrefix(token)
                return InlineSuggestion(
                    completionText = remainder,
                    displayPreview = remainder,
                    triggerSource = "File Identifier",
                    cursorOffset = safeCursor,
                    nextWord = remainder
                )
            }
        }

        return null
    }

    private fun matchJavaScriptPattern(line: String): String? {
        val trimmed = line.trim()
        return when {
            trimmed == "client.on(" || trimmed.startsWith("client.on(") -> "'messageCreate', async (message) => {\n  if (message.author.bot) return;\n  if (message.content === '!ping') {\n    await message.reply('🏓 Pong!');\n  }\n});"
            trimmed == "client.on" -> "('messageCreate', async (message) => {\n  if (message.author.bot) return;\n  if (message.content === '!ping') {\n    await message.reply('🏓 Pong!');\n  }\n});"
            trimmed == "client.once(" || trimmed.startsWith("client.once(") -> "'ready', (c) => {\n  console.log(`🤖 Logged in as \${c.user.tag}!`);\n});"
            trimmed == "client.once" -> "('ready', (c) => {\n  console.log(`🤖 Logged in as \${c.user.tag}!`);\n});"
            trimmed == "client.login(" -> "process.env.DISCORD_TOKEN);"
            trimmed == "client.login" -> "(process.env.DISCORD_TOKEN);"
            trimmed == "client.user." -> "tag"
            trimmed == "require('dotenv" || trimmed == "require(\"dotenv" -> "').config();"
            trimmed == "const { Client" || trimmed == "const {Client" -> ", GatewayIntentBits, Partials, EmbedBuilder, ActionRowBuilder, ButtonBuilder, ButtonStyle, SlashCommandBuilder } = require('discord.js');"
            trimmed == "const {" || trimmed == "const { " -> "Client, GatewayIntentBits, EmbedBuilder } = require('discord.js');"
            trimmed == "const client = new Client(" || trimmed == "const client = new Client({" -> "\n  intents: [\n    GatewayIntentBits.Guilds,\n    GatewayIntentBits.GuildMessages,\n    GatewayIntentBits.MessageContent\n  ]\n});"
            trimmed == "const client =" || trimmed == "const client = " -> "new Client({\n  intents: [\n    GatewayIntentBits.Guilds,\n    GatewayIntentBits.GuildMessages,\n    GatewayIntentBits.MessageContent\n  ]\n});"
            trimmed == "const embed = new " || trimmed == "const embed = new" || trimmed == "const embed =" -> "EmbedBuilder()\n  .setTitle('Bot Notification')\n  .setDescription('Operation completed successfully.')\n  .setColor('#5865F2')\n  .setTimestamp();"
            trimmed == "new EmbedBuilder(" -> ")\n  .setTitle('Title')\n  .setDescription('Details...')\n  .setColor('#5865F2')"
            trimmed == "new EmbedBuilder" -> "()\n  .setTitle('Title')\n  .setDescription('Details...')\n  .setColor('#5865F2')"
            trimmed == "embed.setTitle(" -> "'Bot Notification')"
            trimmed == "embed.setTitle" -> "('Bot Notification')"
            trimmed == "embed.setDescription(" -> "'Here is the response details...')"
            trimmed == "embed.setDescription" -> "('Here is the response details...')"
            trimmed == "embed.setColor(" -> "'#5865F2')"
            trimmed == "embed.setColor" -> "('#5865F2')"
            trimmed == "embed.addFields(" -> "{ name: 'Status', value: 'Active', inline: true })"
            trimmed == "embed.addFields" -> "({ name: 'Status', value: 'Active', inline: true })"
            trimmed == "embed.setFooter(" -> "{ text: 'Discord Bot Studio' })"
            trimmed == "embed.setTimestamp(" -> ")"
            trimmed == "message.reply(" -> "'🏓 Pong! Latency: ' + client.ws.ping + 'ms');"
            trimmed == "message.reply" -> "('🏓 Pong! Latency: ' + client.ws.ping + 'ms');"
            trimmed == "message.channel.send(" -> "{ content: 'Hello from BotStudio!' });"
            trimmed == "message.channel.send" -> "({ content: 'Hello from BotStudio!' });"
            trimmed == "message.react(" -> "'✅');"
            trimmed == "interaction.reply(" -> "{ content: '✅ Pong!', ephemeral: true });"
            trimmed == "interaction.reply" -> "({ content: '✅ Pong!', ephemeral: true });"
            trimmed == "interaction.deferReply(" -> "{ ephemeral: true });"
            trimmed == "interaction.deferReply" -> "({ ephemeral: true });"
            trimmed == "interaction.editReply(" -> "{ content: 'Updated response!' });"
            trimmed == "interaction.followUp(" -> "{ content: 'Follow-up message.', ephemeral: true });"
            trimmed == "interaction.isChatInputCommand(" -> ")"
            trimmed == "GatewayIntentBits." -> "Guilds, GatewayIntentBits.GuildMessages, GatewayIntentBits.MessageContent"
            trimmed == "if (message.author." -> "bot) return;"
            trimmed == "if (!message.content.startsWith(" -> "PREFIX)) return;"
            trimmed == "const args = message.content." -> "slice(PREFIX.length).trim().split(/ +/);"
            trimmed == "const command = args." -> "shift().toLowerCase();"
            trimmed == "if (command === '" || trimmed == "if (command === \"" -> "ping') {\n  await message.reply('🏓 Pong!');\n}"
            trimmed == "process.env." -> "DISCORD_TOKEN"
            trimmed == "console.log(" -> "`🤖 Bot is online as \${client.user.tag}`);"
            trimmed == "new SlashCommandBuilder(" -> ")\n  .setName('ping')\n  .setDescription('Replies with Pong!')"
            trimmed == "new ActionRowBuilder().addComponents(" -> "new ButtonBuilder().setCustomId('primary').setLabel('Click Me').setStyle(ButtonStyle.Primary))"
            trimmed == "new ButtonBuilder().setCustomId(" -> "'btn_action').setLabel('Submit').setStyle(ButtonStyle.Success)"
            trimmed == "new ButtonBuilder(" -> ")\n  .setCustomId('btn_ok')\n  .setLabel('Confirm')\n  .setStyle(ButtonStyle.Primary)"
            trimmed == "try {" || trimmed == "try { " -> "\n} catch (error) {\n  console.error('Error:', error);\n}"
            trimmed == "module.exports =" || trimmed == "module.exports = " -> "{\n  data: command,\n  async execute(interaction) {\n    await interaction.reply('Hello!');\n  }\n};"
            else -> null
        }
    }

    private fun matchPythonPattern(line: String): String? {
        val trimmed = line.trim()
        return when {
            trimmed == "@bot.command(" || trimmed == "@client.command(" -> "name='ping', help='Checks bot latency')\nasync def ping(ctx):\n    await ctx.send(f'🏓 Pong! Latency: {round(bot.latency * 1000)}ms')"
            trimmed == "@bot.command" || trimmed == "@client.command" -> "(name='ping')\nasync def ping(ctx):\n    await ctx.send('🏓 Pong!')"
            trimmed == "@bot.event" || trimmed == "@client.event" -> "\nasync def on_ready():\n    print(f'🤖 Logged in as {bot.user}')\n    await bot.change_presence(activity=discord.Game(name='Discord Bot Studio'))"
            trimmed == "@bot.tree.command(" -> "name='ping', description='Replies with Pong')\nasync def ping_slash(interaction: discord.Interaction):\n    await interaction.response.send_message('🏓 Pong!', ephemeral=True)"
            trimmed == "bot = commands.Bot(" -> "command_prefix='!', intents=intents)"
            trimmed == "intents = discord." -> "Intents.default()\nintents.message_content = True"
            trimmed == "intents.message_content =" || trimmed == "intents.message_content = " -> "True"
            trimmed == "await ctx.send(" -> "f'🏓 Pong! Latency: {round(bot.latency * 1000)}ms')"
            trimmed == "await ctx.send" -> "(f'🏓 Pong! Latency: {round(bot.latency * 1000)}ms')"
            trimmed == "await ctx.reply(" -> "'Hello from discord.py!')"
            trimmed == "await bot." -> "process_commands(message)"
            trimmed == "await interaction.response.send_message(" -> "'🏓 Pong!', ephemeral=True)"
            trimmed == "await interaction.response.defer(" -> "ephemeral=True)"
            trimmed == "TOKEN = os." -> "getenv('DISCORD_TOKEN')\nbot.run(TOKEN)"
            trimmed == "bot.run(" -> "os.getenv('DISCORD_TOKEN'))"
            trimmed == "embed = discord.Embed(" -> "title=\"Bot Status\", description=\"Running smoothly\", color=discord.Color.blue())"
            trimmed == "embed.add_field(" -> "name=\"Status\", value=\"Active\", inline=True)"
            trimmed == "from discord.ext import" || trimmed == "from discord.ext import " -> "commands"
            trimmed == "from dotenv import" || trimmed == "from dotenv import " -> "load_dotenv"
            trimmed == "load_dotenv(" -> ")"
            trimmed == "load_dotenv" -> "()"
            trimmed == "import discord" -> "\nfrom discord.ext import commands\nimport os"
            trimmed == "async def on_message(message):" -> "\n    if message.author.bot:\n        return\n    await bot.process_commands(message)"
            trimmed == "async def on_ready():" -> "\n    print(f'🤖 Bot is ready: {bot.user}')"
            trimmed == "try:" -> "\nexcept Exception as e:\n    print(f'Error: {e}')"
            else -> null
        }
    }

    private fun matchTypeScriptPattern(line: String): String? {
        val trimmed = line.trim()
        return when {
            trimmed == "interface " -> "BotCommand {\n  data: SlashCommandBuilder;\n  execute(interaction: ChatInputCommandInteraction): Promise<void>;\n}"
            trimmed == "type " -> "SlashExecute = (interaction: ChatInputCommandInteraction) => Promise<void>;"
            trimmed == "interaction: " -> "ChatInputCommandInteraction"
            trimmed == "client: " -> "Client<true>"
            trimmed == "const data: " -> "SlashCommandBuilder = new SlashCommandBuilder()"
            else -> null
        }
    }

    private fun matchJsonPattern(line: String): String? {
        val trimmed = line.trim()
        return when {
            trimmed == "\"scripts\":" || trimmed == "\"scripts\": " -> "{\n    \"start\": \"node index.js\",\n    \"dev\": \"nodemon index.js\"\n  }"
            trimmed == "\"dependencies\":" || trimmed == "\"dependencies\": " -> "{\n    \"discord.js\": \"^14.16.0\",\n    \"dotenv\": \"^16.4.5\"\n  }"
            trimmed == "\"token\":" || trimmed == "\"token\": " -> "\"YOUR_BOT_TOKEN_HERE\""
            trimmed == "\"prefix\":" || trimmed == "\"prefix\": " -> "\"!\""
            trimmed == "\"name\":" || trimmed == "\"name\": " -> "\"my-discord-bot\""
            else -> null
        }
    }

    private fun matchKotlinPattern(line: String): String? {
        val trimmed = line.trim()
        return when {
            trimmed == "fun main(" || trimmed == "fun main()" -> " {\n    println(\"🤖 Bot started!\")\n}"
            trimmed == "data class " -> "BotConfig(\n    val token: String,\n    val prefix: String = \"!\"\n)"
            trimmed == "suspend fun " -> "handleCommand(command: String, args: List<String>) {\n    when (command) {\n        \"ping\" -> println(\"Pong!\")\n    }\n}"
            trimmed == "kord.on<" -> "MessageCreateEvent> {\n    if (message.content == \"!ping\") {\n        message.respond(\"🏓 Pong!\")\n    }\n}"
            trimmed == "val kord = " -> "Kord(System.getenv(\"DISCORD_TOKEN\") ?: \"\")"
            else -> null
        }
    }

    private fun matchLuaPattern(line: String): String? {
        val trimmed = line.trim()
        return when {
            trimmed == "local discordia =" || trimmed == "local discordia = " -> "require('discordia')\nlocal client = discordia.Client()"
            trimmed == "client:on('ready'" || trimmed == "client:on(\"ready\"" -> ", function()\n    print('🤖 Bot ready: ' .. client.user.username)\nend)"
            trimmed == "client:on('messageCreate'" || trimmed == "client:on(\"messageCreate\"" -> ", function(message)\n    if message.author.bot then return end\n    if message.content == '!ping' then\n        message:reply('🏓 Pong!')\n    end\nend)"
            trimmed == "message:reply(" -> "'🏓 Pong!')"
            trimmed == "client:run(" -> "'Bot ' .. (os.getenv('DISCORD_TOKEN') or ''))"
            else -> null
        }
    }

    private fun matchSqlPattern(line: String): String? {
        val trimmed = line.trim()
        return when {
            trimmed == "SELECT * FROM" || trimmed == "SELECT * FROM " -> "users WHERE user_id = $1;"
            trimmed == "CREATE TABLE IF NOT EXISTS" || trimmed == "CREATE TABLE IF NOT EXISTS " -> "users (\n  user_id VARCHAR(32) PRIMARY KEY,\n  points INT DEFAULT 0,\n  level INT DEFAULT 1\n);"
            trimmed == "INSERT INTO" || trimmed == "INSERT INTO " -> "users (user_id, points) VALUES ($1, $2);"
            else -> null
        }
    }

    private fun matchShellPattern(line: String): String? {
        val trimmed = line.trim()
        return when {
            trimmed == "npm " || trimmed == "npm" -> "install discord.js dotenv"
            trimmed == "pip " || trimmed == "pip" -> "install discord.py python-dotenv"
            trimmed == "export DISCORD_TOKEN=" || trimmed == "export DISCORD_TOKEN =" -> "\"your_bot_token_here\""
            trimmed == "node " || trimmed == "node" -> "index.js"
            trimmed == "python " || trimmed == "python3 " -> "bot.py"
            else -> null
        }
    }

    private fun matchEnvPattern(line: String): String? {
        val trimmed = line.trim()
        return when {
            trimmed == "DISCORD_TOKEN=" || trimmed == "DISCORD_TOKEN =" -> "your_bot_token_here"
            trimmed == "CLIENT_ID=" || trimmed == "CLIENT_ID =" -> "123456789012345678"
            trimmed == "PREFIX=" || trimmed == "PREFIX =" -> "!"
            trimmed == "GUILD_ID=" || trimmed == "GUILD_ID =" -> "123456789012345678"
            else -> null
        }
    }

    private fun findInFileIdentifier(code: String, token: String): String? {
        val idRegex = Regex("""\b(?:const|let|var|function|def|class|val|fun|local|interface|type)\s+([A-Za-z0-9_$]+)""")
        val matches = idRegex.findAll(code)
            .map { it.groupValues[1] }
            .filter { it.startsWith(token, ignoreCase = false) && it != token }
            .distinct()
            .toList()

        return matches.minByOrNull { it.length }
    }

    private fun extractNextWord(suggestion: String): String {
        val trimmed = suggestion.trimStart()
        val match = Regex("""^([A-Za-z0-9_$]+|[^\sA-Za-z0-9_$]+|\s+)""").find(trimmed)
        return match?.value ?: trimmed.take(5)
    }
}
