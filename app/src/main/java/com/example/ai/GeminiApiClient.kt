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

data class AiCallResult(
    val content: String,
    val providerUsed: AiProvider,
    val modelUsed: String,
    val isFallback: Boolean = false,
    val quotaWarning: Boolean = false
)

object GeminiApiClient {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    fun hasApiKey(): Boolean {
        val geminiKey = AiKeyManager.getActiveKeyForProvider(AiProvider.GEMINI)
        if (!geminiKey.isNullOrBlank()) return true

        val keys = AiKeyManager.configuredKeys.value
        return keys.any { it.apiKey.isNotBlank() }
    }

    suspend fun callAiWithFailover(
        prompt: String,
        systemInstruction: String? = null
    ): AiCallResult = withContext(Dispatchers.IO) {
        val preferredProvider = AiKeyManager.selectedProvider.value
        val preferredModel = AiKeyManager.selectedModel.value

        // 1. Try Preferred Provider
        val primaryKey = AiKeyManager.getActiveKeyForProvider(preferredProvider)
        if (!primaryKey.isNullOrBlank()) {
            val result = executeProviderCall(preferredProvider, preferredModel, primaryKey, prompt, systemInstruction)
            if (result != null && !result.quotaWarning) {
                AiKeyManager.incrementUsage(preferredProvider)
                return@withContext result
            } else if (result?.quotaWarning == true) {
                AiKeyManager.markKeyStatus(preferredProvider, "Quota Exceeded")
            }
        }

        // 2. Try Automatic Failover to backup configured provider
        if (AiKeyManager.autoFallbackEnabled.value) {
            val backup = AiKeyManager.getBackupProvider(preferredProvider)
            if (backup != null) {
                val backupResult = executeProviderCall(backup.first, backup.first.defaultModel, backup.second, prompt, systemInstruction)
                if (backupResult != null && !backupResult.quotaWarning) {
                    AiKeyManager.incrementUsage(backup.first)
                    return@withContext backupResult.copy(
                        content = "⚠️ [Quota Failover: Auto-switched to ${backup.first.displayName} (${backup.first.defaultModel})]\n\n" + backupResult.content,
                        isFallback = true
                    )
                }
            }
        }

        // 3. Graceful Local Intelligent Rule-based Engine Fallback (No network or All Quotas Hit)
        AiCallResult(
            content = "⚠️ **AI Service Notice**: API Quota reached or API key unconfigured.\nRunning locally using BotStudio's Built-in Discord Bot Knowledge Engine:\n\n${getFallbackResponse(prompt)}",
            providerUsed = AiProvider.LOCAL_FALLBACK,
            modelUsed = "Built-in Bot Rule Engine",
            isFallback = true,
            quotaWarning = true
        )
    }

    // Convenience method compatible with previous callers
    suspend fun callGemini(prompt: String, systemInstruction: String? = null): String {
        return callAiWithFailover(prompt, systemInstruction).content
    }

    private fun executeProviderCall(
        provider: AiProvider,
        model: String,
        apiKey: String,
        prompt: String,
        systemInstruction: String?
    ): AiCallResult? {
        return try {
            when (provider) {
                AiProvider.GEMINI -> callGeminiApi(model, apiKey, prompt, systemInstruction)
                AiProvider.OPENAI -> callOpenAiApi(model, apiKey, prompt, systemInstruction)
                AiProvider.GROQ -> callGroqApi(model, apiKey, prompt, systemInstruction)
                AiProvider.ANTHROPIC -> callAnthropicApi(model, apiKey, prompt, systemInstruction)
                AiProvider.LOCAL_FALLBACK -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun callGeminiApi(model: String, apiKey: String, prompt: String, systemInstruction: String?): AiCallResult {
        val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"
        val rootJson = JSONObject()
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
            .url("$baseUrl?key=$apiKey")
            .post(requestBody)
            .build()

        val response = okHttpClient.newCall(request).execute()
        val respBody = response.body?.string() ?: ""

        if (response.code == 429 || respBody.contains("RESOURCE_EXHAUSTED", ignoreCase = true) || respBody.contains("quota", ignoreCase = true)) {
            return AiCallResult(
                content = "Quota Exceeded: $respBody",
                providerUsed = AiProvider.GEMINI,
                modelUsed = model,
                quotaWarning = true
            )
        }

        if (!response.isSuccessful) {
            return AiCallResult(
                content = "Gemini API Error (${response.code}): $respBody",
                providerUsed = AiProvider.GEMINI,
                modelUsed = model,
                quotaWarning = response.code == 429
            )
        }

        val respJson = JSONObject(respBody)
        val candidates = respJson.optJSONArray("candidates")
        if (candidates != null && candidates.length() > 0) {
            val cand = candidates.getJSONObject(0)
            val content = cand.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            if (parts != null && parts.length() > 0) {
                return AiCallResult(
                    content = parts.getJSONObject(0).optString("text", "No text generated."),
                    providerUsed = AiProvider.GEMINI,
                    modelUsed = model
                )
            }
        }
        return AiCallResult(
            content = "No output candidate received from Gemini.",
            providerUsed = AiProvider.GEMINI,
            modelUsed = model
        )
    }

    private fun callOpenAiApi(model: String, apiKey: String, prompt: String, systemInstruction: String?): AiCallResult {
        val rootJson = JSONObject()
        rootJson.put("model", model)
        val messages = JSONArray()

        if (!systemInstruction.isNullOrBlank()) {
            val sysMsg = JSONObject().put("role", "system").put("content", systemInstruction)
            messages.put(sysMsg)
        }
        val userMsg = JSONObject().put("role", "user").put("content", prompt)
        messages.put(userMsg)
        rootJson.put("messages", messages)

        val requestBody = rootJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        val response = okHttpClient.newCall(request).execute()
        val respBody = response.body?.string() ?: ""

        if (response.code == 429 || respBody.contains("insufficient_quota")) {
            return AiCallResult(respBody, AiProvider.OPENAI, model, quotaWarning = true)
        }

        if (!response.isSuccessful) {
            return AiCallResult("OpenAI API Error (${response.code}): $respBody", AiProvider.OPENAI, model)
        }

        val respJson = JSONObject(respBody)
        val choices = respJson.optJSONArray("choices")
        if (choices != null && choices.length() > 0) {
            val text = choices.getJSONObject(0).optJSONObject("message")?.optString("content") ?: ""
            return AiCallResult(text, AiProvider.OPENAI, model)
        }
        return AiCallResult("Empty OpenAI completion", AiProvider.OPENAI, model)
    }

    private fun callGroqApi(model: String, apiKey: String, prompt: String, systemInstruction: String?): AiCallResult {
        val rootJson = JSONObject()
        rootJson.put("model", model)
        val messages = JSONArray()

        if (!systemInstruction.isNullOrBlank()) {
            val sysMsg = JSONObject().put("role", "system").put("content", systemInstruction)
            messages.put(sysMsg)
        }
        val userMsg = JSONObject().put("role", "user").put("content", prompt)
        messages.put(userMsg)
        rootJson.put("messages", messages)

        val requestBody = rootJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        val response = okHttpClient.newCall(request).execute()
        val respBody = response.body?.string() ?: ""

        if (response.code == 429 || respBody.contains("rate_limit_exceeded")) {
            return AiCallResult(respBody, AiProvider.GROQ, model, quotaWarning = true)
        }

        if (!response.isSuccessful) {
            return AiCallResult("Groq Error (${response.code}): $respBody", AiProvider.GROQ, model)
        }

        val respJson = JSONObject(respBody)
        val choices = respJson.optJSONArray("choices")
        if (choices != null && choices.length() > 0) {
            val text = choices.getJSONObject(0).optJSONObject("message")?.optString("content") ?: ""
            return AiCallResult(text, AiProvider.GROQ, model)
        }
        return AiCallResult("Empty Groq completion", AiProvider.GROQ, model)
    }

    private fun callAnthropicApi(model: String, apiKey: String, prompt: String, systemInstruction: String?): AiCallResult {
        val rootJson = JSONObject()
        rootJson.put("model", model)
        rootJson.put("max_tokens", 2048)
        if (!systemInstruction.isNullOrBlank()) {
            rootJson.put("system", systemInstruction)
        }
        val messages = JSONArray()
        messages.put(JSONObject().put("role", "user").put("content", prompt))
        rootJson.put("messages", messages)

        val requestBody = rootJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .post(requestBody)
            .build()

        val response = okHttpClient.newCall(request).execute()
        val respBody = response.body?.string() ?: ""

        if (response.code == 429) {
            return AiCallResult(respBody, AiProvider.ANTHROPIC, model, quotaWarning = true)
        }

        if (!response.isSuccessful) {
            return AiCallResult("Anthropic Error (${response.code}): $respBody", AiProvider.ANTHROPIC, model)
        }

        val respJson = JSONObject(respBody)
        val contentArr = respJson.optJSONArray("content")
        if (contentArr != null && contentArr.length() > 0) {
            val text = contentArr.getJSONObject(0).optString("text", "")
            return AiCallResult(text, AiProvider.ANTHROPIC, model)
        }
        return AiCallResult("Empty Claude completion", AiProvider.ANTHROPIC, model)
    }

    private fun getFallbackResponse(prompt: String): String {
        return when {
            prompt.contains("debug", ignoreCase = true) || prompt.contains("error", ignoreCase = true) -> {
                """
                ### 🛠️ AI Debugging Diagnosis:
                1. **Root Cause**: The error usually occurs when referencing an undefined interaction response, or attempting to reply twice to an already deferred Discord interaction.
                2. **Fix**: Ensure you call `await interaction.deferReply()` if processing takes longer than 3 seconds, or replace `interaction.reply()` with `interaction.followUp()`.
                3. **Code Solution**:
                ```javascript
                await interaction.deferReply();
                // perform async queries / api calls
                await interaction.editReply({ content: 'Operation successfully completed!' });
                ```
                """.trimIndent()
            }
            prompt.contains("test", ignoreCase = true) -> {
                """
                ### 🧪 Generated Bot Test Suite:
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
                ### 🤖 Production Discord Bot Command Flow:
                Here is a clean slash command ready to run:
                ```javascript
                const { SlashCommandBuilder, EmbedBuilder, PermissionFlagsBits } = require('discord.js');

                module.exports = {
                  data: new SlashCommandBuilder()
                    .setName('action')
                    .setDescription('Automated bot workflow with safe execution')
                    .setDefaultMemberPermissions(PermissionFlagsBits.SendMessages),
                  async execute(interaction) {
                    const embed = new EmbedBuilder()
                      .setTitle('⚡ Action Completed')
                      .setDescription(`Processed command for ${'$'}{interaction.user.tag}`)
                      .setColor(0x5865F2)
                      .setFooter({ text: 'BotStudio Engine' })
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
