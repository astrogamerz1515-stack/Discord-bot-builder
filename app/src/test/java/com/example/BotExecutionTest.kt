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
}
