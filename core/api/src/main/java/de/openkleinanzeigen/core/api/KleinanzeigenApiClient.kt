package de.openkleinanzeigen.core.api

import de.openkleinanzeigen.core.common.AppLogger
import de.openkleinanzeigen.core.domain.ApiException
import de.openkleinanzeigen.core.domain.model.AdType
import de.openkleinanzeigen.core.domain.model.BackendSearchAgent
import de.openkleinanzeigen.core.domain.model.Category
import de.openkleinanzeigen.core.domain.model.ChatMessage
import de.openkleinanzeigen.core.domain.model.Conversation
import de.openkleinanzeigen.core.domain.model.Listing
import de.openkleinanzeigen.core.domain.model.Location
import de.openkleinanzeigen.core.domain.model.PosterType
import de.openkleinanzeigen.core.domain.model.SearchQuery
import de.openkleinanzeigen.core.domain.model.UserSession
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class KleinanzeigenApiClient(
    private val httpClient: OkHttpClient = defaultClient(),
    private val parser: KleinanzeigenJsonParser = KleinanzeigenJsonParser(),
) {
    companion object {
        private const val API_BASE = "https://api.kleinanzeigen.de/api"
        private const val GATEWAY_BASE = "https://gateway.kleinanzeigen.de"
        private const val BASIC_AUTH = "Basic YW5kcm9pZDpUYVI2MHBFdHRZ"
        private const val USER_AGENT = "okhttp/4.10.0"

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun search(query: SearchQuery): List<Listing> {
        val url = "$API_BASE/ads.json".toHttpUrl().newBuilder().apply {
            addQueryParameter("_in", "title,ad-address.state,ad-address.zip-code,price,pictures,link,description")
            if (query.query.isNotBlank()) addQueryParameter("q", query.query)
            addQueryParameter("page", query.page.toString())
            addQueryParameter("size", query.size.toString())
            if (query.pictureRequired) addQueryParameter("pictureRequired", "true")
            if (query.buyNowOnly) addQueryParameter("buyNowOnly", "true")
            query.locationId?.let { addQueryParameter("locationId", it.toString()) }
            query.radiusKm?.let { addQueryParameter("radius", it.toString()) }
            query.minPrice?.let { addQueryParameter("minPrice", it.toString()) }
            query.maxPrice?.let { addQueryParameter("maxPrice", it.toString()) }
            query.categoryId?.let { addQueryParameter("categoryId", it.toString()) }
            query.adType?.let { addQueryParameter("adType", it.name) }
            query.posterType?.let { addQueryParameter("posterType", it.name) }
        }.build()

        val body = executePublic(Request.Builder().url(url).get().build())
        return parser.parseSearchResponse(body)
    }

    fun getListing(id: String): Listing {
        val url = "$API_BASE/ads/$id.json".toHttpUrl().newBuilder()
            .addQueryParameter("_in", "title,description,ad-address.state,ad-address.zip-code,price,pictures,link")
            .build()
        val body = executePublic(Request.Builder().url(url).get().build())
        return parser.parseAdDetail(body) ?: throw ApiException("Listing not found: $id")
    }

    fun searchLocations(query: String, limit: Int = 15): List<Location> {
        if (query.isBlank()) return emptyList()
        val url = "$API_BASE/locations.json".toHttpUrl().newBuilder()
            .addQueryParameter("q", query.trim())
            .addQueryParameter("limit", limit.coerceIn(1, 50).toString())
            .build()
        val body = executePublic(Request.Builder().url(url).get().build())
        return parser.parseLocations(body).take(limit)
    }

    fun getTopLocations(): List<Location> {
        val url = "$API_BASE/locations/top-locations.json".toHttpUrl().newBuilder()
            .addQueryParameter("depth", "0")
            .build()
        val body = executePublic(Request.Builder().url(url).get().build())
        return parser.parseLocations(body)
    }

    fun getCategories(): List<Category> {
        val url = "$API_BASE/categories.json"
        val body = executePublic(Request.Builder().url(url).get().build())
        return parseCategories(body)
    }

    fun login(email: String, password: String): UserSession {
        val maskedEmail = email.trim().let { e ->
            if (e.length <= 3) "***" else e.take(2) + "***@" + e.substringAfter('@', "")
        }
        AppLogger.i("Auth", "Login attempt for $maskedEmail")
        val jsonBody = """{"username":"${email.escapeJson()}","password":"${password.escapeJson()}"}"""
        val request = Request.Builder()
            .url("$GATEWAY_BASE/auth/login")
            .header("Content-Type", "application/json")
            .header("User-Agent", USER_AGENT)
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()
        return try {
            val body = execute(request, auth = null, logBodies = true)
            val json = Json.parseToJsonElement(body).jsonObject
            val token = json["accessToken"]?.jsonPrimitive?.contentOrNull
                ?: json["access_token"]?.jsonPrimitive?.contentOrNull
                ?: throw ApiException("Login failed: no token in response body")
            val userId = json["userId"]?.jsonPrimitive?.contentOrNull
                ?: json["user_id"]?.jsonPrimitive?.contentOrNull
                ?: "unknown"
            val refresh = json["refreshToken"]?.jsonPrimitive?.contentOrNull
                ?: json["refresh_token"]?.jsonPrimitive?.contentOrNull
            AppLogger.i("Auth", "Login OK userId=$userId tokenLen=${token.length}")
            UserSession(userId = userId, email = email.trim(), accessToken = token, refreshToken = refresh)
        } catch (e: ApiException) {
            AppLogger.e("Auth", "Login failed: ${e.message}", e)
            throw e
        } catch (e: Exception) {
            AppLogger.e("Auth", "Login error: ${e.message}", e)
            throw e
        }
    }

    fun getConversations(session: UserSession): List<Conversation> {
        val url = "$GATEWAY_BASE/messagebox/api/users/${session.userId}/conversations?page=0&size=50"
        val body = execute(Request.Builder().url(url).get().build(), session.accessToken)
        return parseConversations(body)
    }

    fun getMessages(session: UserSession, conversationId: String): List<ChatMessage> {
        val url = "$GATEWAY_BASE/messagebox/api/users/${session.userId}/conversations/$conversationId/messages?page=0&size=100"
        val body = execute(Request.Builder().url(url).get().build(), session.accessToken)
        return parseMessages(body, conversationId)
    }

    fun sendMessage(session: UserSession, conversationId: String, text: String) {
        val jsonBody = """{"message":"${text.escapeJson()}"}"""
        val request = Request.Builder()
            .url("$GATEWAY_BASE/messagebox/api/users/${session.userId}/conversations/$conversationId/messages")
            .header("Content-Type", "application/json")
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()
        execute(request, session.accessToken)
    }

    fun sendMessageToAd(session: UserSession, adId: String, text: String) {
        val form = FormBody.Builder()
            .add("adId", adId)
            .add("message", text)
            .build()
        val request = Request.Builder()
            .url("$GATEWAY_BASE/messagebox/api/users/${session.userId}/conversations")
            .post(form)
            .build()
        execute(request, session.accessToken)
    }

    fun getBackendSearchAgents(session: UserSession): List<BackendSearchAgent> {
        val url = "$API_BASE/users/${session.userId}/watchlist.json"
        return try {
            val body = execute(
                Request.Builder().url(url).get().build(),
                session.accessToken,
            )
            parseBackendAgents(body)
        } catch (e: ApiException) {
            AppLogger.w("KleinanzeigenApi", "Backend agents unavailable: ${e.message}")
            emptyList()
        }
    }

    private fun executePublic(request: Request): String {
        val authed = request.newBuilder()
            .header("Authorization", BASIC_AUTH)
            .header("User-Agent", USER_AGENT)
            .build()
        return execute(authed, auth = null)
    }

    private fun execute(
        request: Request,
        auth: String?,
        logBodies: Boolean = false,
    ): String {
        val builder = request.newBuilder().header("User-Agent", USER_AGENT)
        if (auth != null) {
            builder.header("Authorization", "Bearer $auth")
            AppLogger.d("KleinanzeigenApi", "Bearer token length=${auth.length}")
        }
        val built = builder.build()
        val start = System.currentTimeMillis()
        AppLogger.d("KleinanzeigenApi", "→ ${built.method} ${built.url}")
        built.headers.forEach { (name, value) ->
            if (name.equals("Authorization", ignoreCase = true)) {
                AppLogger.d("KleinanzeigenApi", "  $name: ${value.take(20)}…")
            } else {
                AppLogger.d("KleinanzeigenApi", "  $name: $value")
            }
        }
        httpClient.newCall(built).execute().use { response ->
            val body = response.body?.string().orEmpty()
            val elapsed = System.currentTimeMillis() - start
            AppLogger.d(
                "KleinanzeigenApi",
                "← ${response.code} ${built.url} (${elapsed}ms, ${body.length} bytes)",
            )
            if (!response.isSuccessful) {
                val snippet = body.take(800)
                AppLogger.e("KleinanzeigenApi", "HTTP ${response.code} body: $snippet")
                throw ApiException("HTTP ${response.code}: ${snippet.take(200)}", response.code)
            }
            if (logBodies && body.isNotEmpty()) {
                AppLogger.d("KleinanzeigenApi", "Response body: ${body.take(600)}")
            }
            return body
        }
    }

    private fun parseCategories(body: String): List<Category> {
        return try {
            val root = Json.parseToJsonElement(body).jsonObject
            val key = root.keys.firstOrNull { it.contains("categories") } ?: return emptyList()
            root[key]?.jsonObject?.get("value")?.jsonObject?.get("category")
            emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseConversations(body: String): List<Conversation> = emptyList()

    private fun parseMessages(body: String, conversationId: String): List<ChatMessage> = emptyList()

    private fun parseBackendAgents(body: String): List<BackendSearchAgent> = emptyList()

    private fun String.escapeJson(): String = replace("\\", "\\\\").replace("\"", "\\\"")
}
