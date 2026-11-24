package com.egolite.mobile.mcp

import android.util.Base64
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap

data class AuthCodeRecord(
    val clientId: String,
    val redirectUri: String,
    val codeChallenge: String,
    val codeChallengeMethod: String,
    val createdAtMs: Long = System.currentTimeMillis()
)

object OAuth21Manager {
    private val random = SecureRandom()
    private val registeredClients = ConcurrentHashMap<String, String>()
    private val authCodes = ConcurrentHashMap<String, AuthCodeRecord>()
    private val accessTokens = ConcurrentHashMap<String, Long>()

    @Volatile
    var staticBearerToken: String = generateToken("ego_muse_")

    @Volatile
    var publicBaseUrl: String = "http://127.0.0.1:8787"

    fun rotateBearerToken(): String {
        staticBearerToken = generateToken("ego_muse_")
        return staticBearerToken
    }

    fun protectedResourceMetadata(): JsonObject {
        val base = publicBaseUrl.trimEnd('/')
        return buildJsonObject {
            put("resource", "$base/mcp")
            put("authorization_servers", buildJsonArray { add(kotlinx.serialization.json.JsonPrimitive(base)) })
            put("bearer_methods_supported", buildJsonArray { add(kotlinx.serialization.json.JsonPrimitive("header")) })
            put("scopes_supported", buildJsonArray { add(kotlinx.serialization.json.JsonPrimitive("mcp:tools")) })
        }
    }

    fun authorizationServerMetadata(): JsonObject {
        val base = publicBaseUrl.trimEnd('/')
        return buildJsonObject {
            put("issuer", base)
            put("authorization_endpoint", "$base/oauth/authorize")
            put("token_endpoint", "$base/oauth/token")
            put("registration_endpoint", "$base/oauth/register")
            put("response_types_supported", buildJsonArray { add(kotlinx.serialization.json.JsonPrimitive("code")) })
            put("grant_types_supported", buildJsonArray {
                add(kotlinx.serialization.json.JsonPrimitive("authorization_code"))
                add(kotlinx.serialization.json.JsonPrimitive("refresh_token"))
            })
            put("code_challenge_methods_supported", buildJsonArray { add(kotlinx.serialization.json.JsonPrimitive("S256")) })
            put("token_endpoint_auth_methods_supported", buildJsonArray {
                add(kotlinx.serialization.json.JsonPrimitive("none"))
                add(kotlinx.serialization.json.JsonPrimitive("client_secret_post"))
            })
        }
    }

    fun registerClient(clientName: String, redirectUri: String): JsonObject {
        val clientId = "ego_client_${generateRandomHex(8)}"
        registeredClients[clientId] = redirectUri
        return buildJsonObject {
            put("client_id", clientId)
            put("client_name", clientName.ifBlank { "MCP Client" })
            put("token_endpoint_auth_method", "none")
            put("redirect_uris", buildJsonArray { add(kotlinx.serialization.json.JsonPrimitive(redirectUri)) })
            put("grant_types", buildJsonArray {
                add(kotlinx.serialization.json.JsonPrimitive("authorization_code"))
                add(kotlinx.serialization.json.JsonPrimitive("refresh_token"))
            })
            put("response_types", buildJsonArray { add(kotlinx.serialization.json.JsonPrimitive("code")) })
        }
    }

    fun issueAuthCode(
        clientId: String,
        redirectUri: String,
        codeChallenge: String,
        codeChallengeMethod: String
    ): String {
        val code = generateRandomHex(24)
        authCodes[code] = AuthCodeRecord(
            clientId = clientId,
            redirectUri = redirectUri,
            codeChallenge = codeChallenge,
            codeChallengeMethod = codeChallengeMethod.ifBlank { "S256" }
        )
        return code
    }

    fun exchangeCodeForToken(
        code: String,
        clientId: String,
        codeVerifier: String?
    ): JsonObject? {
        val record = authCodes.remove(code) ?: return null
        if (record.clientId != clientId) return null
        if (System.currentTimeMillis() - record.createdAtMs > 5 * 60 * 1000L) return null

        if (record.codeChallenge.isNotBlank()) {
            if (codeVerifier.isNullOrBlank()) return null
            val expected = if (record.codeChallengeMethod.equals("S256", ignoreCase = true)) {
                sha256Base64Url(codeVerifier)
            } else {
                codeVerifier
            }
            if (expected != record.codeChallenge) return null
        }

        val accessToken = generateToken("ego_oa_")
        val refreshToken = generateToken("ego_rf_")
        accessTokens[accessToken] = System.currentTimeMillis() + 24 * 60 * 60 * 1000L
        return buildJsonObject {
            put("access_token", accessToken)
            put("token_type", "Bearer")
            put("expires_in", 86400)
            put("refresh_token", refreshToken)
            put("scope", "mcp:tools")
        }
    }

    fun validateAuthorizationHeader(header: String?): Boolean {
        if (header.isNullOrBlank()) return false
        val token = header.removePrefix("Bearer ").removePrefix("bearer ").trim()
        if (token.isBlank()) return false
        if (token == staticBearerToken) return true
        val expiresAt = accessTokens[token] ?: return false
        return System.currentTimeMillis() < expiresAt
    }

    private fun generateToken(prefix: String): String = "$prefix${generateRandomHex(20)}"

    private fun generateRandomHex(bytesLen: Int): String {
        val bytes = ByteArray(bytesLen)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun sha256Base64Url(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }
}
