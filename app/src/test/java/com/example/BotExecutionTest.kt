package com.example

import com.example.data.model.BotFile
import com.example.data.model.BotProject
import com.example.engine.BotCodeExecutor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
}
