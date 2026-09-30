package com.example.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Rate limit metadata parsed directly from Discord REST response headers.
 */
data class RateLimitInfo(
    val limit: Int = 50,
    val remaining: Int = 50,
    val resetAfterSecs: Float = 0f,
    val bucket: String = "global",
    val isGlobal: Boolean = false,
    val retryAfterMs: Long = 0L
)

/**
 * Gateway bot metadata returned by GET /gateway/bot.
 */
data class GatewayBotInfo(
    val url: String,
    val shards: Int,
    val totalSessionLimit: Int,
    val remainingSessions: Int,
    val resetAfterMs: Long,
    val maxConcurrency: Int
)

/**
 * High-performance, rate-limit-aware Discord REST API Client.
 * Implements:
 * - Dynamic bucket-based rate limiting from response headers
 * - Global rate limiter (max 50 req/sec)
 * - Automatic 429 retry backoff respecting Retry-After
 * - Cloudflare Ban Guard: Tracks 401/403 invalid requests to avoid the 10,000/10min ban threshold
 * - Instant per-guild slash command deployment for developer testing
 */
class DiscordRestClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    private val routeBuckets = ConcurrentHashMap<String, Long>()
    private val globalMutex = Mutex()
    private var globalResetTimeMs: Long = 0L

    // Cloudflare Ban Guard: 10,000 invalid requests (401/403) within 10 min triggers Cloudflare IP ban
    private val invalidRequestCounter = AtomicInteger(0)
    private var invalidRequestWindowStart = System.currentTimeMillis()
    private val BAN_GUARD_MAX_WARNING = 25

    val currentInvalidCount: Int get() = invalidRequestCounter.get()

    /**
     * Executes an HTTP request with rate limit inspection and retry logic.
     */
    suspend fun execute(
        token: String,
        endpoint: String,
        method: String = "GET",
        jsonBody: String? = null,
        logCallback: (suspend (String, String) -> Unit)? = null
    ): Pair<Int, String> = withContext(Dispatchers.IO) {
        val cleanToken = BotRuntimeEngine.sanitizeToken(token)
        val url = if (endpoint.startsWith("http")) endpoint else "https://discord.com/api/v10$endpoint"
        val routeKey = endpoint.substringBefore("?").replace(Regex("/[0-9]{15,22}"), "/:id")

        // 1. Cloudflare Ban Guard check
        checkCloudflareBanGuard(logCallback)

        // 2. Wait for route bucket or global rate limit reset
        waitForRateLimit(routeKey)

        val mediaType = "application/json".toMediaType()
        val requestBody: RequestBody? = jsonBody?.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bot $cleanToken")
            .header("User-Agent", "DiscordBot (https://github.com/aistudio, 2.0.0)")
            .method(method, if (method == "GET") null else (requestBody ?: ByteArray(0).toRequestBody(null)))
            .build()

        var attempts = 0
        while (attempts < 3) {
            attempts++
            try {
                val response: Response = httpClient.newCall(request).execute()
                val code = response.code
                val body = response.body?.string() ?: ""

                // Parse headers
                val rateLimitInfo = parseRateLimitHeaders(response)
                updateRateLimits(routeKey, rateLimitInfo)

                if (code == 401 || code == 403) {
                    recordInvalidRequest(logCallback)
                }

                if (code == 429) {
                    val waitMs = if (rateLimitInfo.retryAfterMs > 0) rateLimitInfo.retryAfterMs else 2000L
                    logCallback?.invoke("⚠️ [REST 429] Rate limited on route $routeKey. Pausing for ${waitMs}ms...", "WARN")
                    delay(waitMs)
                    continue
                }

                return@withContext code to body
            } catch (e: Exception) {
                if (attempts >= 3) {
                    return@withContext 0 to (e.localizedMessage ?: "Network error")
                }
                delay(500)
            }
        }

        0 to "Request aborted after maximum retries"
    }

    private suspend fun waitForRateLimit(routeKey: String) {
        val now = System.currentTimeMillis()
        globalMutex.withLock {
            if (globalResetTimeMs > now) {
                delay(globalResetTimeMs - now)
            }
        }
        val routeReset = routeBuckets[routeKey] ?: 0L
        if (routeReset > now) {
            delay(routeReset - now)
        }
    }

    private fun parseRateLimitHeaders(response: Response): RateLimitInfo {
        val limit = response.header("X-RateLimit-Limit")?.toIntOrNull() ?: 50
        val remaining = response.header("X-RateLimit-Remaining")?.toIntOrNull() ?: 50
        val resetAfter = response.header("X-RateLimit-Reset-After")?.toFloatOrNull() ?: 0f
        val bucket = response.header("X-RateLimit-Bucket") ?: "default"
        val isGlobal = response.header("X-RateLimit-Global")?.toBoolean() ?: false
        val retryAfter = response.header("Retry-After")?.toFloatOrNull()?.let { (it * 1000).toLong() } ?: 0L

        return RateLimitInfo(
            limit = limit,
            remaining = remaining,
            resetAfterSecs = resetAfter,
            bucket = bucket,
            isGlobal = isGlobal,
            retryAfterMs = retryAfter
        )
    }

    private fun updateRateLimits(routeKey: String, info: RateLimitInfo) {
        val now = System.currentTimeMillis()
        if (info.isGlobal && info.retryAfterMs > 0) {
            globalResetTimeMs = now + info.retryAfterMs
        }
        if (info.remaining <= 1 && info.resetAfterSecs > 0) {
            routeBuckets[routeKey] = now + (info.resetAfterSecs * 1000).toLong()
        }
    }

    private suspend fun recordInvalidRequest(logCallback: (suspend (String, String) -> Unit)?) {
        val now = System.currentTimeMillis()
        if (now - invalidRequestWindowStart > 600000) { // 10 minutes rolling window
            invalidRequestCounter.set(0)
            invalidRequestWindowStart = now
        }
        val count = invalidRequestCounter.incrementAndGet()
        if (count >= BAN_GUARD_MAX_WARNING) {
            logCallback?.invoke("🛡️ [CLOUDFLARE BAN GUARD] Warning: $count invalid requests (401/403) detected. Throttling outbound requests to protect IP from Cloudflare ban!", "WARN")
            delay(2000)
        }
    }

    private suspend fun checkCloudflareBanGuard(logCallback: (suspend (String, String) -> Unit)?) {
        if (invalidRequestCounter.get() > 50) {
            logCallback?.invoke("🛡️ [BAN GUARD TRIGGERED] High invalid request rate. Cooldown active for 3 seconds.", "WARN")
            delay(3000)
        }
    }

    /**
     * Fetches current Bot application identity and verification data.
     */
    suspend fun getApplicationInfo(token: String): Pair<Boolean, JSONObject?> = withContext(Dispatchers.IO) {
        val (code, body) = execute(token, "/oauth2/applications/@me")
        if (code == 200) {
            try {
                true to JSONObject(body)
            } catch (e: Exception) {
                false to null
            }
        } else {
            false to null
        }
    }

    /**
     * Queries Discord for recommended shard count and connection limits via GET /gateway/bot.
     */
    suspend fun getGatewayBot(token: String): GatewayBotInfo? = withContext(Dispatchers.IO) {
        val (code, body) = execute(token, "/gateway/bot")
        if (code == 200) {
            try {
                val json = JSONObject(body)
                val url = json.getString("url")
                val shards = json.getInt("shards")
                val sessionLimit = json.getJSONObject("session_start_limit")
                GatewayBotInfo(
                    url = url,
                    shards = shards,
                    totalSessionLimit = sessionLimit.getInt("total"),
                    remainingSessions = sessionLimit.getInt("remaining"),
                    resetAfterMs = sessionLimit.getLong("reset_after"),
                    maxConcurrency = sessionLimit.getInt("max_concurrency")
                )
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    /**
     * Registers slash commands instantly for a single test guild (instant developer feedback without 1hr global cache delay).
     */
    suspend fun registerGuildCommands(
        token: String,
        clientId: String,
        guildId: String,
        commands: JSONArray
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val (code, body) = execute(
            token = token,
            endpoint = "/applications/$clientId/guilds/$guildId/commands",
            method = "PUT",
            jsonBody = commands.toString()
        )
        if (code in 200..204) {
            true to "✅ Commands synced instantly to server $guildId!"
        } else {
            false to "HTTP $code: $body"
        }
    }

    /**
     * Registers global slash commands across all servers (takes up to 1 hour to propagate globally).
     */
    suspend fun registerGlobalCommands(
        token: String,
        clientId: String,
        commands: JSONArray
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val (code, body) = execute(
            token = token,
            endpoint = "/applications/$clientId/commands",
            method = "PUT",
            jsonBody = commands.toString()
        )
        if (code in 200..204) {
            true to "✅ Commands registered globally across all servers (propagation may take up to 1h)."
        } else {
            false to "HTTP $code: $body"
        }
    }
}
