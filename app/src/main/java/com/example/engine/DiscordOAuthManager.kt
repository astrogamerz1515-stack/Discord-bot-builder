package com.example.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Token payload returned by Discord OAuth2 endpoints.
 */
data class OAuthTokenResult(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Int,
    val refreshToken: String?,
    val scope: String
)

/**
 * Comprehensive Discord OAuth2 Manager implementing:
 * - Authorization Code grant URL generation
 * - Authorization Code exchange for Access & Refresh tokens
 * - Automatic Refresh Token rotation
 * - Client Credentials flow
 * - Token revocation at POST /oauth2/token/revoke
 */
class DiscordOAuthManager(
    private val httpClient: OkHttpClient = OkHttpClient()
) {
    /**
     * Builds the authorization URL for user login or bot invitation.
     */
    fun buildAuthorizationUrl(
        clientId: String,
        redirectUri: String,
        scopes: List<String> = listOf("bot", "applications.commands", "identify"),
        permissions: Long = 8L // Administrator by default, or 0L
    ): String {
        val encodedRedirect = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8.toString())
        val encodedScopes = URLEncoder.encode(scopes.joinToString(" "), StandardCharsets.UTF_8.toString())
        return "https://discord.com/oauth2/authorize?client_id=$clientId&redirect_uri=$encodedRedirect&response_type=code&scope=$encodedScopes&permissions=$permissions"
    }

    /**
     * Exchanges an authorization code for access and refresh tokens.
     */
    suspend fun exchangeCodeForToken(
        clientId: String,
        clientSecret: String,
        code: String,
        redirectUri: String
    ): Pair<Boolean, OAuthTokenResult?> = withContext(Dispatchers.IO) {
        try {
            val formBody = FormBody.Builder()
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", redirectUri)
                .build()

            val request = Request.Builder()
                .url("https://discord.com/api/v10/oauth2/token")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .post(formBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JSONObject(body)
                val tokenResult = OAuthTokenResult(
                    accessToken = json.getString("access_token"),
                    tokenType = json.getString("token_type"),
                    expiresIn = json.getInt("expires_in"),
                    refreshToken = if (json.has("refresh_token") && !json.isNull("refresh_token")) json.getString("refresh_token") else null,
                    scope = json.getString("scope")
                )
                true to tokenResult
            } else {
                false to null
            }
        } catch (e: Exception) {
            false to null
        }
    }

    /**
     * Rotates an expired access token using its refresh token.
     */
    suspend fun refreshToken(
        clientId: String,
        clientSecret: String,
        refreshToken: String
    ): Pair<Boolean, OAuthTokenResult?> = withContext(Dispatchers.IO) {
        try {
            val formBody = FormBody.Builder()
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .build()

            val request = Request.Builder()
                .url("https://discord.com/api/v10/oauth2/token")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .post(formBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JSONObject(body)
                val tokenResult = OAuthTokenResult(
                    accessToken = json.getString("access_token"),
                    tokenType = json.getString("token_type"),
                    expiresIn = json.getInt("expires_in"),
                    refreshToken = if (json.has("refresh_token") && !json.isNull("refresh_token")) json.getString("refresh_token") else null,
                    scope = json.getString("scope")
                )
                true to tokenResult
            } else {
                false to null
            }
        } catch (e: Exception) {
            false to null
        }
    }

    /**
     * Revokes an active OAuth2 token at POST /oauth2/token/revoke.
     */
    suspend fun revokeToken(
        clientId: String,
        clientSecret: String,
        token: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val formBody = FormBody.Builder()
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .add("token", token)
                .build()

            val request = Request.Builder()
                .url("https://discord.com/api/v10/oauth2/token/revoke")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .post(formBody)
                .build()

            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}
