package com.example.ai

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiApiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun hasApiKey(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY"
    }

    suspend fun callGemini(prompt: String, systemInstruction: String? = null): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (!hasApiKey()) {
            return@withContext "⚠️ **Gemini API Key missing or placeholder.**\nPlease configure your Gemini API Key in the AI Studio Secrets panel. Below is simulated output based on standard Discord bot design practices:\n\n${getFallbackResponse(prompt)}"
        }

        try {
            val rootJson = JSONObject()

            // contents
            val contentsArr = JSONArray()
            val userContent = JSONObject()
            userContent.put("role", "user")
            val partsArr = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", prompt)
            partsArr.put(partObj)
            userContent.put("parts", partsArr)
            contentsArr.put(userContent)
            rootJson.put("contents", contentsArr)

            // systemInstruction
            if (!systemInstruction.isNullOrBlank()) {
                val sysContent = JSONObject()
                sysContent.put("role", "system")
                val sysParts = JSONArray()
                val sysPartObj = JSONObject()
                sysPartObj.put("text", systemInstruction)
                sysParts.put(sysPartObj)
                sysContent.put("parts", sysParts)
                rootJson.put("systemInstruction", sysContent)
            }

            val requestBody = rootJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val respBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext "Gemini API Error (${response.code}): $respBody\n\nFallback suggestion:\n${getFallbackResponse(prompt)}"
            }

            val respJson = JSONObject(respBody)
            val candidates = respJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val cand = candidates.getJSONObject(0)
                val content = cand.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return@withContext parts.getJSONObject(0).optString("text", "No text generated.")
                }
            }
            "No output received from Gemini model."
        } catch (e: Exception) {
            "Request failed: ${e.localizedMessage ?: "Unknown error"}\n\nFallback:\n${getFallbackResponse(prompt)}"
        }
    }

    private fun getFallbackResponse(prompt: String): String {
        return when {
            prompt.contains("debug", ignoreCase = true) || prompt.contains("error", ignoreCase = true) -> {
                """
                ### 🛠️ AI Debugging Diagnosis:
                1. **Root Cause**: The error usually occurs when referencing an undefined interaction response, or attempting to reply twice to an already deferred Discord interaction.
                2. **Fix**: Ensure you call `await interaction.deferReply()` if processing takes longer than 3 seconds, or replace `interaction.reply()` with `interaction.followUp()`.
                3. **Example**:
                ```javascript
                await interaction.deferReply();
                // do async work...
                await interaction.editReply({ content: 'Done!' });
                ```
                """.trimIndent()
            }
            prompt.contains("test", ignoreCase = true) -> {
                """
                ### 🧪 Generated Test Suite:
                ```javascript
                describe('Bot Command Tests', () => {
                  test('should respond to /ping with pong and latency', async () => {
                    const interaction = mockInteraction('ping');
                    await handleCommand(interaction);
                    expect(interaction.replied).toBe(true);
                    expect(interaction.lastReply.embeds[0].title).toContain('Pong');
                  });

                  test('should reject unauthorized member on /ban', async () => {
                    const interaction = mockInteraction('ban', { memberPermissions: [] });
                    await handleCommand(interaction);
                    expect(interaction.lastReply.content).toMatch(/permission/i);
                  });
                });
                ```
                """.trimIndent()
            }
            prompt.contains("migration", ignoreCase = true) || prompt.contains("v13", ignoreCase = true) || prompt.contains("v14", ignoreCase = true) -> {
                """
                ### 🔄 Discord.js v13 ➔ v14 Migration Plan:
                1. **Intents**: Replace `Intents.FLAGS.GUILDS` with `GatewayIntentBits.Guilds`.
                2. **Embeds**: Replace `new MessageEmbed()` with `new EmbedBuilder()`.
                3. **Constants**: All string enums are replaced with PascalCase constants from `discord.js`.
                ```javascript
                // v14 syntax
                const { Client, GatewayIntentBits, EmbedBuilder } = require('discord.js');
                const client = new Client({ intents: [GatewayIntentBits.Guilds, GatewayIntentBits.GuildMessages, GatewayIntentBits.MessageContent] });
                ```
                """.trimIndent()
            }
            else -> {
                """
                ### 🤖 AI Bot Command Generator:
                Here is a production-ready Discord slash command implementation:
                ```javascript
                const { SlashCommandBuilder, EmbedBuilder } = require('discord.js');

                module.exports = {
                  data: new SlashCommandBuilder()
                    .setName('action')
                    .setDescription('Execute automated bot action'),
                  async execute(interaction) {
                    const embed = new EmbedBuilder()
                      .setTitle('⚡ Action Executed')
                      .setDescription(`Processed command for ${'$'}{interaction.user.tag}`)
                      .setColor(0x5865F2)
                      .setTimestamp();
                    await interaction.reply({ embeds: [embed] });
                  }
                };
                ```
                """.trimIndent()
            }
        }
    }
}
