package com.example.ai

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

enum class AiProvider(
    val id: String,
    val displayName: String,
    val defaultModel: String,
    val availableModels: List<String>,
    val docUrl: String
) {
    GEMINI(
        id = "gemini",
        displayName = "Google Gemini AI",
        defaultModel = "gemini-2.5-flash",
        availableModels = listOf("gemini-2.5-flash", "gemini-2.5-pro", "gemini-2.0-flash", "gemini-1.5-flash"),
        docUrl = "https://aistudio.google.com"
    ),
    OPENAI(
        id = "openai",
        displayName = "OpenAI",
        defaultModel = "gpt-4o-mini",
        availableModels = listOf("gpt-4o-mini", "gpt-4o", "o1-mini"),
        docUrl = "https://platform.openai.com/api-keys"
    ),
    ANTHROPIC(
        id = "anthropic",
        displayName = "Anthropic Claude",
        defaultModel = "claude-3-5-haiku",
        availableModels = listOf("claude-3-5-haiku", "claude-3-5-sonnet"),
        docUrl = "https://console.anthropic.com"
    ),
    GROQ(
        id = "groq",
        displayName = "Groq Ultra-Fast",
        defaultModel = "llama-3.3-70b-versatile",
        availableModels = listOf("llama-3.3-70b-versatile", "mixtral-8x7b-32768", "gemma2-9b-it"),
        docUrl = "https://console.groq.com/keys"
    ),
    LOCAL_FALLBACK(
        id = "fallback",
        displayName = "Offline Bot Engine (No API Key Required)",
        defaultModel = "built-in-bot-rules",
        availableModels = listOf("built-in-bot-rules"),
        docUrl = ""
    )
}

data class ApiKeyEntry(
    val provider: AiProvider,
    val apiKey: String,
    val isPrimary: Boolean = false,
    val lastUsedTimestamp: Long = 0L,
    val status: String = "Active", // "Active", "Quota Exceeded", "Invalid Key", "Rate Limited"
    val requestsCount: Int = 0
)

object AiKeyManager {
    private const val PREFS_NAME = "ai_api_keys_prefs"
    private var sharedPreferences: SharedPreferences? = null

    private val _configuredKeys = MutableStateFlow<List<ApiKeyEntry>>(emptyList())
    val configuredKeys: StateFlow<List<ApiKeyEntry>> = _configuredKeys.asStateFlow()

    private val _selectedProvider = MutableStateFlow(AiProvider.GEMINI)
    val selectedProvider: StateFlow<AiProvider> = _selectedProvider.asStateFlow()

    private val _selectedModel = MutableStateFlow("gemini-2.5-flash")
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _autoFallbackEnabled = MutableStateFlow(true)
    val autoFallbackEnabled: StateFlow<Boolean> = _autoFallbackEnabled.asStateFlow()

    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadKeysFromStorage()
    }

    private fun loadKeysFromStorage() {
        val prefs = sharedPreferences ?: return
        val list = mutableListOf<ApiKeyEntry>()

        // Check if buildConfig contains a gemini key
        val defaultGeminiKey = BuildConfig.GEMINI_API_KEY
        val hasEnvGemini = defaultGeminiKey.isNotBlank() && defaultGeminiKey != "MY_GEMINI_API_KEY"

        for (provider in AiProvider.values()) {
            if (provider == AiProvider.LOCAL_FALLBACK) continue
            val key = prefs.getString("key_${provider.id}", "") ?: ""
            val status = prefs.getString("status_${provider.id}", "Active") ?: "Active"
            val count = prefs.getInt("count_${provider.id}", 0)

            val effectiveKey = when {
                key.isNotBlank() -> key
                provider == AiProvider.GEMINI && hasEnvGemini -> defaultGeminiKey
                else -> ""
            }

            if (effectiveKey.isNotBlank()) {
                list.add(
                    ApiKeyEntry(
                        provider = provider,
                        apiKey = effectiveKey,
                        isPrimary = prefs.getString("primary_provider", "gemini") == provider.id,
                        status = status,
                        requestsCount = count
                    )
                )
            }
        }

        _configuredKeys.value = list
        val savedProvider = prefs.getString("primary_provider", "gemini")
        _selectedProvider.value = AiProvider.values().find { it.id == savedProvider } ?: AiProvider.GEMINI
        _selectedModel.value = prefs.getString("selected_model", _selectedProvider.value.defaultModel) ?: _selectedProvider.value.defaultModel
        _autoFallbackEnabled.value = prefs.getBoolean("auto_fallback_enabled", true)
    }

    fun setKey(provider: AiProvider, key: String) {
        val prefs = sharedPreferences ?: return
        prefs.edit().putString("key_${provider.id}", key.trim()).apply()
        loadKeysFromStorage()
    }

    fun removeKey(provider: AiProvider) {
        val prefs = sharedPreferences ?: return
        prefs.edit().remove("key_${provider.id}").remove("status_${provider.id}").apply()
        loadKeysFromStorage()
    }

    fun markKeyStatus(provider: AiProvider, status: String) {
        val prefs = sharedPreferences ?: return
        prefs.edit().putString("status_${provider.id}", status).apply()
        loadKeysFromStorage()
    }

    fun incrementUsage(provider: AiProvider) {
        val prefs = sharedPreferences ?: return
        val current = prefs.getInt("count_${provider.id}", 0)
        prefs.edit()
            .putInt("count_${provider.id}", current + 1)
            .putLong("last_used_${provider.id}", System.currentTimeMillis())
            .apply()
        loadKeysFromStorage()
    }

    fun selectModel(model: String) {
        _selectedModel.value = model
        sharedPreferences?.edit()?.putString("selected_model", model)?.apply()
    }

    fun selectProvider(provider: AiProvider) {
        _selectedProvider.value = provider
        _selectedModel.value = provider.defaultModel
        sharedPreferences?.edit()
            ?.putString("primary_provider", provider.id)
            ?.putString("selected_model", provider.defaultModel)
            ?.apply()
    }

    fun setAutoFallback(enabled: Boolean) {
        _autoFallbackEnabled.value = enabled
        sharedPreferences?.edit()?.putBoolean("auto_fallback_enabled", enabled)?.apply()
    }

    fun getActiveKeyForProvider(provider: AiProvider): String? {
        val entry = _configuredKeys.value.find { it.provider == provider }
        if (entry != null && entry.apiKey.isNotBlank()) return entry.apiKey

        if (provider == AiProvider.GEMINI) {
            val defaultKey = BuildConfig.GEMINI_API_KEY
            if (defaultKey.isNotBlank() && defaultKey != "MY_GEMINI_API_KEY") return defaultKey
        }
        return null
    }

    /**
     * Gets a backup key if the primary key failed due to quota/rate limit
     */
    fun getBackupProvider(failedProvider: AiProvider): Pair<AiProvider, String>? {
        if (!_autoFallbackEnabled.value) return null

        val available = _configuredKeys.value.filter {
            it.provider != failedProvider && it.status != "Quota Exceeded" && it.status != "Invalid Key"
        }
        val next = available.firstOrNull()
        return if (next != null) {
            Pair(next.provider, next.apiKey)
        } else {
            null
        }
    }
}
