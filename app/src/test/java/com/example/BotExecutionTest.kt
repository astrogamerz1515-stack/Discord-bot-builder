package com.example

import com.example.data.model.BotFile
import com.example.data.model.BotProject
import com.example.engine.BotCodeExecutor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BotExecutionTest {

    @Test
    fun testCustomCodeExecutionExactMatch() = runBlocking {
        val project = BotProject(
            name = "TestBot",
            description = "Test bot",
            prefix = "!"
        )

        val files = listOf(
            BotFile(
                projectId = 1,
                filePath = "index.js",
                isEntrypoint = true,
                content = """
                    client.on('messageCreate', (message) => {
                        if (message.content === '!hello') {
                            message.reply('Hello from my custom code!');
                        }
                    });
                """.trimIndent()
            )
        )

        val result = BotCodeExecutor.executeIncomingMessage(
            messageContent = "!hello",
            authorUsername = "TestUser",
            authorId = "12345",
            project = project,
            files = files,
            gatewayPingMs = 20
        )

        assertTrue(result.isHandled)
        assertEquals("Hello from my custom code!", result.replyText)
    }

    @Test
    fun testBuiltInPingCommand() = runBlocking {
        val project = BotProject(
            name = "Aegis",
            description = "Test bot",
            prefix = "!"
        )

        val result = BotCodeExecutor.executeIncomingMessage(
            messageContent = "!ping",
            authorUsername = "Tester",
            authorId = "12345",
            project = project,
            files = emptyList(),
            gatewayPingMs = 24
        )

        assertTrue(result.isHandled)
        assertTrue(result.replyText.contains("Pong"))
    }

    @Test
    fun testTokenSanitization() {
        val rawWithQuotes = "\"MTIzNDU2Nzg5MDEyMzQ1Ng.Gxyz12.aBcDeFgHiJkLmNoPqRsTuVwXyZ0123456789_\""
        assertEquals("MTIzNDU2Nzg5MDEyMzQ1Ng.Gxyz12.aBcDeFgHiJkLmNoPqRsTuVwXyZ0123456789_", com.example.engine.BotRuntimeEngine.sanitizeToken(rawWithQuotes))

        val rawWithBotPrefix = "Bot MTIzNDU2Nzg5MDEyMzQ1Ng.Gxyz12.aBcDeFgHiJkLmNoPqRsTuVwXyZ0123456789_ "
        assertEquals("MTIzNDU2Nzg5MDEyMzQ1Ng.Gxyz12.aBcDeFgHiJkLmNoPqRsTuVwXyZ0123456789_", com.example.engine.BotRuntimeEngine.sanitizeToken(rawWithBotPrefix))

        val rawWithEnv = "DISCORD_TOKEN = 'MTIzNDU2Nzg5MDEyMzQ1Ng.Gxyz12.aBcDeFgHiJkLmNoPqRsTuVwXyZ0123456789_'"
        assertEquals("MTIzNDU2Nzg5MDEyMzQ1Ng.Gxyz12.aBcDeFgHiJkLmNoPqRsTuVwXyZ0123456789_", com.example.engine.BotRuntimeEngine.sanitizeToken(rawWithEnv))
    }

    @Test
    fun testTokenDiagnosis() {
        // 32 chars hex = Client Secret
        val secretDiag = com.example.engine.BotRuntimeEngine.diagnoseToken("a1b2c3d4e5f67890a1b2c3d4e5f67890")
        assertTrue(secretDiag != null && secretDiag.contains("Client Secret"))

        // 18 digits = App ID
        val appIdDiag = com.example.engine.BotRuntimeEngine.diagnoseToken("118923456789012345")
        assertTrue(appIdDiag != null && appIdDiag.contains("Application / Client ID"))
    }

    @Test
    fun testSyntaxHighlightTransformationIdentityMapping() {
        val transformation = com.example.ui.editor.SyntaxHighlightTransformation("index.js")
        val sampleCode = "const client = new Client();\nconsole.log('Online');"
        val input = androidx.compose.ui.text.AnnotatedString(sampleCode)
        val result = transformation.filter(input)

        // Text content length must match exactly to prevent IME offset desynchronization
        assertEquals(sampleCode.length, result.text.length)
        assertEquals(sampleCode, result.text.text)

        // OffsetMapping must preserve exact 1:1 bijection
        for (i in 0..sampleCode.length) {
            assertEquals(i, result.offsetMapping.originalToTransformed(i))
            assertEquals(i, result.offsetMapping.transformedToOriginal(i))
        }
    }

    @Test
    fun testVisualBuilderBlockMovement() {
        val vm = com.example.ui.visualbuilder.VisualBuilderViewModel()
        val initialBlocks = vm.blocks.value
        assertTrue(initialBlocks.size >= 3)

        val firstBlockId = initialBlocks[0].id
        val secondBlockId = initialBlocks[1].id

        // Test move down
        vm.moveBlockDown(firstBlockId)
        assertEquals(secondBlockId, vm.blocks.value[0].id)
        assertEquals(firstBlockId, vm.blocks.value[1].id)

        // Test move to top
        vm.moveBlockToTop(firstBlockId)
        assertEquals(firstBlockId, vm.blocks.value[0].id)

        // Test move to bottom
        vm.moveBlockToBottom(firstBlockId)
        assertEquals(firstBlockId, vm.blocks.value.last().id)

        // Test move to specific position
        vm.moveBlockToPosition(firstBlockId, 1)
        assertEquals(firstBlockId, vm.blocks.value[1].id)

        // Test indent and outdent
        val origIndent = vm.blocks.value[1].indentLevel
        vm.indentBlock(firstBlockId)
        assertEquals(origIndent + 1, vm.blocks.value[1].indentLevel)
        vm.outdentBlock(firstBlockId)
        assertEquals(origIndent, vm.blocks.value[1].indentLevel)
    }

    @Test
    fun testMultiLanguageInlineAndIntelliSense() {
        // JavaScript inline suggestion
        val jsCode = "client.on("
        val jsSuggestion = com.example.ui.editor.intellisense.InlineSuggestionEngine.computeInlineSuggestion(
            code = jsCode,
            cursorIndex = jsCode.length,
            filePath = "index.js"
        )
        assertNotNull(jsSuggestion)
        assertTrue(jsSuggestion!!.completionText.contains("messageCreate"))

        // Python inline suggestion
        val pyCode = "@bot.command"
        val pySuggestion = com.example.ui.editor.intellisense.InlineSuggestionEngine.computeInlineSuggestion(
            code = pyCode,
            cursorIndex = pyCode.length,
            filePath = "bot.py"
        )
        assertNotNull(pySuggestion)
        assertTrue(pySuggestion!!.completionText.contains("ping"))

        // Kotlin inline suggestion
        val ktCode = "fun main()"
        val ktSuggestion = com.example.ui.editor.intellisense.InlineSuggestionEngine.computeInlineSuggestion(
            code = ktCode,
            cursorIndex = ktCode.length,
            filePath = "Main.kt"
        )
        assertNotNull(ktSuggestion)
        assertTrue(ktSuggestion!!.completionText.contains("Bot started"))

        // Lua inline suggestion
        val luaCode = "local discordia = "
        val luaSuggestion = com.example.ui.editor.intellisense.InlineSuggestionEngine.computeInlineSuggestion(
            code = luaCode,
            cursorIndex = luaCode.length,
            filePath = "bot.lua"
        )
        assertNotNull(luaSuggestion)
        assertTrue(luaSuggestion!!.completionText.contains("discordia"))

        // Multi-language IntelliSense completions
        val pyCompletions = com.example.ui.editor.intellisense.IntelliSenseEngine.computeCompletions(
            code = "embed = discord.",
            cursorIndex = "embed = discord.".length,
            filePath = "bot.py"
        )
        assertTrue(pyCompletions.isNotEmpty())

        val luaCompletions = com.example.ui.editor.intellisense.IntelliSenseEngine.computeCompletions(
            code = "client:",
            cursorIndex = "client:".length,
            filePath = "bot.lua"
        )
        assertTrue(luaCompletions.isNotEmpty())

        // In-file identifier dynamic extraction
        val customFileCode = "const myCustomBotHelper = 42;\nmyCust"
        val inIdSuggestion = com.example.ui.editor.intellisense.InlineSuggestionEngine.computeInlineSuggestion(
            code = customFileCode,
            cursorIndex = customFileCode.length,
            filePath = "index.js"
        )
        assertNotNull(inIdSuggestion)
        assertEquals("omBotHelper", inIdSuggestion!!.completionText)
    }

    @Test
    fun testCustomSlashCommandWithNestedEmbed() = runBlocking {
        val project = BotProject(name = "CustomBot", description = "Test", prefix = "!")
        val files = listOf(
            BotFile(
                projectId = 2,
                filePath = "index.js",
                isEntrypoint = true,
                content = """
                    client.on('interactionCreate', async (interaction) => {
                        if (interaction.commandName === 'serverinfo') {
                            const embed = new EmbedBuilder()
                                .setTitle('My Custom Server Info')
                                .setDescription('Welcome to our community!')
                                .setColor(0x57F287)
                                .setFooter({ text: 'Custom System' });
                            await interaction.reply({ embeds: [embed] });
                        }
                    });
                """.trimIndent()
            )
        )

        val result = BotCodeExecutor.executeIncomingMessage(
            messageContent = "/serverinfo",
            authorUsername = "AdminUser",
            authorId = "99999",
            project = project,
            files = files,
            gatewayPingMs = 15
        )

        assertTrue(result.isHandled)
        assertEquals("My Custom Server Info", result.embedTitle)
        assertEquals("Welcome to our community!", result.embedDescription)
        assertEquals("Custom System", result.embedFooter)
    }

    @Test
    fun testUserCustomPingOverridesBuiltin() = runBlocking {
        val project = BotProject(name = "CustomPingBot", description = "Test", prefix = "!")
        val files = listOf(
            BotFile(
                projectId = 3,
                filePath = "index.js",
                isEntrypoint = true,
                content = """
                    client.on('messageCreate', (message) => {
                        if (message.content === '!ping') {
                            message.reply('My custom pong response 123!');
                        }
                    });
                """.trimIndent()
            )
        )

        val result = BotCodeExecutor.executeIncomingMessage(
            messageContent = "!ping",
            authorUsername = "Tester",
            authorId = "111",
            project = project,
            files = files,
            gatewayPingMs = 20
        )

        assertTrue(result.isHandled)
        assertEquals("My custom pong response 123!", result.replyText)
    }

    @Test
    fun testRegexCommandExecutionWithEmbedFields() = runBlocking {
        val project = BotProject(name = "RegexBot", description = "Test", prefix = "!")
        val files = listOf(
            BotFile(
                projectId = 4,
                filePath = "index.js",
                isEntrypoint = true,
                content = """
                    client.on('messageCreate', async (message) => {
                        if (/^!serverinfo/i.test(message.content)) {
                            const embed = new EmbedBuilder()
                                .setTitle(`${'$'}{message.guild.name} Server Info`)
                                .setDescription('Welcome to the server!')
                                .addFields(
                                    { name: 'Server Name', value: message.guild.name, inline: true },
                                    { name: 'Total Members', value: `${'$'}{message.guild.memberCount}`, inline: true }
                                );
                            await message.reply({ embeds: [embed] });
                        }
                    });
                """.trimIndent()
            )
        )

        val result = BotCodeExecutor.executeIncomingMessage(
            messageContent = "!serverinfo",
            authorUsername = "Owner",
            authorId = "1001",
            project = project,
            files = files,
            gatewayPingMs = 18,
            guildName = "Aegis Realm",
            guildId = "987654321",
            guildMemberCount = 250
        )

        assertTrue(result.isHandled)
        assertTrue(result.embedTitle?.contains("Aegis Realm") == true)
        assertEquals("Welcome to the server!", result.embedDescription)
        assertTrue(result.embedFields.isNotEmpty())
        assertEquals("Server Name", result.embedFields[0].first)
        assertEquals("Aegis Realm", result.embedFields[0].second)
        assertEquals("Total Members", result.embedFields[1].first)
        assertEquals("250", result.embedFields[1].second)
    }

    @Test
    fun testPythonRegexAndAddField() = runBlocking {
        val project = BotProject(name = "PyBot", description = "Test", prefix = "!")
        val files = listOf(
            BotFile(
                projectId = 5,
                filePath = "bot.py",
                isEntrypoint = true,
                content = """
                    import re
                    @client.event
                    async def on_message(message):
                        if re.search(r"^!serverinfo", message.content):
                            embed = discord.Embed(title=f"{message.guild.name} Stats")
                            embed.add_field(name="Owner", value="Admin", inline=True)
                            await message.channel.send(embed=embed)
                """.trimIndent()
            )
        )

        val result = BotCodeExecutor.executeIncomingMessage(
            messageContent = "!serverinfo",
            authorUsername = "Tester",
            authorId = "2002",
            project = project,
            files = files,
            gatewayPingMs = 22,
            guildName = "Python Guild"
        )

        assertTrue(result.isHandled)
        assertTrue(result.embedTitle?.contains("Python Guild") == true)
        assertTrue(result.embedFields.any { it.first == "Owner" && it.second == "Admin" })
    }

    @Test
    fun testNeverOutputGenericFallbackForServerInfo() = runBlocking {
        val project = BotProject(name = "FallbackCheckBot", description = "Test", prefix = "!")
        // Even if user only wrote an empty or minimal serverinfo handler
        val files = listOf(
            BotFile(
                projectId = 6,
                filePath = "index.js",
                isEntrypoint = true,
                content = """
                    client.on('messageCreate', (message) => {
                        if (message.content === '!serverinfo') {
                            // Custom user code without hardcoded string
                        }
                    });
                """.trimIndent()
            )
        )

        val result = BotCodeExecutor.executeIncomingMessage(
            messageContent = "!serverinfo",
            authorUsername = "User",
            authorId = "3003",
            project = project,
            files = files,
            gatewayPingMs = 25,
            guildName = "My Discord Server",
            guildMemberCount = 105
        )

        assertTrue(result.isHandled)
        // Must NOT return the old annoying string
        assertTrue(!result.replyText.contains("Executed serverinfo from your bot code"))
        // Must return rich server info embed with guild properties
        assertTrue(result.embedTitle != null && result.embedTitle!!.contains("My Discord Server"))
        assertTrue(result.embedFields.any { it.first == "Server Name" && it.second == "My Discord Server" })
        assertTrue(result.embedFields.any { it.first == "Total Members" && it.second.contains("105") })
    }

    @Test
    fun testBalancedBlockExtractorHandlesNestedObjects() {
        val code = """
            if (commandName === 'test') {
                const config = { opt: { deep: true }, text: 'inside' };
                await interaction.reply('Finished test!');
            }
        """.trimIndent()
        val body = BotCodeExecutor.extractBalancedBlock(code, 0)
        assertTrue(body.contains("Finished test!"))
        assertTrue(body.contains("deep: true"))
    }

    @Test
    fun testPackageManagerDiscordCatalogContainsAllRequestedPackages() {
        val allPkgs = com.example.packages.PackageManager.allPackages.value

        // Verify Discord bot packages
        assertTrue("discord.js should be present", allPkgs.any { it.name.contains("discord.js", ignoreCase = true) })
        assertTrue("@discordjs/voice should be present", allPkgs.any { it.name.contains("@discordjs/voice", ignoreCase = true) })
        assertTrue("dotenv should be present", allPkgs.any { it.name.contains("dotenv", ignoreCase = true) })
        assertTrue("discord.py should be present", allPkgs.any { it.name.contains("discord.py", ignoreCase = true) })
        assertTrue("nyxx should be present", allPkgs.any { it.name.contains("nyxx", ignoreCase = true) })
        assertTrue("JDA should be present", allPkgs.any { it.name.contains("JDA", ignoreCase = true) })
        assertTrue("Discord4J should be present", allPkgs.any { it.name.contains("Discord4J", ignoreCase = true) })
        assertTrue("create-dna-discord-bot should be present", allPkgs.any { it.name.contains("create-dna-discord-bot", ignoreCase = true) })
        assertTrue("@dookdiks/discord-bot-builder should be present", allPkgs.any { it.name.contains("discord-bot-builder", ignoreCase = true) })

        // Verify Code Editor packages
        assertTrue("CodeView should be present", allPkgs.any { it.id == "editor_codeview" })
        assertTrue("react-native-codepad should be present", allPkgs.any { it.id == "editor_rn_codepad" })
        assertTrue("zuraffa_code_editor should be present", allPkgs.any { it.id == "editor_flutter_zuraffa" })
        assertTrue("@actualwave/react-native-codeditor should be present", allPkgs.any { it.id == "editor_codemirror_rn" })
        assertTrue("flutter_monaco should be present", allPkgs.any { it.id == "editor_flutter_monaco" })

        // Verify Termux environment package
        assertTrue("Termux runtime bundle should be present", allPkgs.any { it.id == "env_termux_runtime" })
    }

    @Test
    fun testInstallCustomPackageAndStatus() = runBlocking {
        val customPkg = com.example.packages.PackageManager.installCustomPackage("discord-player", "npm")
        assertNotNull(customPkg)
        assertTrue(com.example.packages.PackageManager.isInstalled(customPkg.id))
    }

    @Test
    fun testPythonFStringExecutionUptimeCalculation() = runBlocking {
        val project = BotProject(name = "PyBot", description = "Python Bot", prefix = "!")
        val files = listOf(
            BotFile(
                projectId = 1,
                filePath = "bot.py",
                isEntrypoint = true,
                content = """
                    @bot.command()
                    async def uptime(ctx):
                        await ctx.send("{int(r//3600)}h")
                """.trimIndent()
            )
        )

        val result = BotCodeExecutor.executeIncomingMessage(
            messageContent = "!uptime",
            authorUsername = "Tester",
            authorId = "111",
            project = project,
            files = files,
            gatewayPingMs = 25
        )

        assertTrue("Command should be handled", result.isHandled)
        assertEquals("Should evaluate expression dynamically to 2h, not raw text", "2h", result.replyText)
    }

    @Test
    fun testPythonFStringPrefixedExecution() = runBlocking {
        val project = BotProject(name = "PyBot", description = "Python Bot", prefix = "!")
        val files = listOf(
            BotFile(
                projectId = 1,
                filePath = "bot.py",
                isEntrypoint = true,
                content = """
                    @bot.command()
                    async def uptime(ctx):
                        r = 7200
                        await ctx.send(f"Uptime: {int(r//3600)}h {int((r%3600)//60)}m")
                """.trimIndent()
            )
        )

        val result = BotCodeExecutor.executeIncomingMessage(
            messageContent = "!uptime",
            authorUsername = "Tester",
            authorId = "111",
            project = project,
            files = files,
            gatewayPingMs = 25
        )

        assertTrue(result.isHandled)
        assertEquals("Uptime: 2h 0m", result.replyText)
    }

    @Test
    fun testPythonDotFormatExecution() = runBlocking {
        val project = BotProject(name = "PyBot", description = "Python Bot", prefix = "!")
        val files = listOf(
            BotFile(
                projectId = 1,
                filePath = "bot.py",
                isEntrypoint = true,
                content = """
                    @bot.command()
                    async def greet(ctx):
                        await ctx.send("Hello {}".format(ctx.author.name))
                """.trimIndent()
            )
        )

        val result = BotCodeExecutor.executeIncomingMessage(
            messageContent = "!greet",
            authorUsername = "Alex",
            authorId = "222",
            project = project,
            files = files,
            gatewayPingMs = 25
        )

        assertTrue(result.isHandled)
        assertEquals("Hello Alex", result.replyText)
    }

    @Test
    fun testPythonAutoFixerAddsFPrefix() {
        val rawPythonCode = """
            @bot.command()
            async def uptime(ctx):
                await ctx.send("{int(r//3600)}h")
        """.trimIndent()

        val fixResult = com.example.util.BotCodeAutoFixer.ensurePythonFStrings(rawPythonCode)
        assertTrue(fixResult.wasModified)
        assertTrue(fixResult.fixedCode.contains("f\"{int(r//3600)}h\""))
    }

    @Test
    fun testPythonAutoFixerRepairsMissingDotInFormat() {
        val rawPythonCode = """
            msg = "Hello {}"format(user)
        """.trimIndent()

        val fixResult = com.example.util.BotCodeAutoFixer.ensurePythonFStrings(rawPythonCode)
        assertTrue(fixResult.wasModified)
        assertTrue(fixResult.fixedCode.contains("\"Hello {}\".format(user)"))
    }
}

