package de.openkleinanzeigen.core.api

import de.openkleinanzeigen.core.common.AppLogger
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong

/**
 * Logs complete HTTP exchanges to [AppLogger] when debug logging is enabled.
 * Redacts credentials; includes status, timing, headers, and response body previews.
 */
class HttpDebugInterceptor : Interceptor {

    private val nextId = AtomicLong(1)

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!AppLogger.isEnabled()) {
            return chain.proceed(chain.request())
        }

        val id = nextId.getAndIncrement()
        val request = chain.request()
        val startMs = System.currentTimeMillis()
        logRequest(id, request)

        return try {
            val response = chain.proceed(request)
            logResponse(id, request, response, startMs)
            response
        } catch (e: IOException) {
            val elapsed = System.currentTimeMillis() - startMs
            val kind = if (e.message?.contains("cancel", ignoreCase = true) == true) {
                "CANCELLED"
            } else {
                "FAILED"
            }
            AppLogger.e(
                "HTTP",
                "#$id $kind ${request.method} ${request.url} (${elapsed}ms): ${e.message}",
                e,
            )
            throw e
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startMs
            AppLogger.e(
                "HTTP",
                "#$id ERROR ${request.method} ${request.url} (${elapsed}ms): ${e.message}",
                e,
            )
            throw e
        }
    }

    private fun logRequest(id: Long, request: okhttp3.Request) {
        AppLogger.d("HTTP", "#$id → ${request.method} ${request.url}")
        request.headers.forEach { (name, value) ->
            AppLogger.d("HTTP", "#$id   Request-Header: $name: ${redactHeader(name, value)}")
        }
        request.body?.let { body ->
            val length = if (body.contentLength() >= 0) body.contentLength().toString() else "unknown"
            AppLogger.d(
                "HTTP",
                "#$id   Request-Body: type=${body.contentType()} length=$length" +
                    if (body.isOneShot()) " (one-shot)" else "",
            )
        }
    }

    private fun logResponse(id: Long, request: okhttp3.Request, response: Response, startMs: Long): Response {
        val elapsed = System.currentTimeMillis() - startMs
        val peek = response.peekBody(MAX_BODY_LOG_BYTES)
        val bodyText = peek.string()
        AppLogger.d(
            "HTTP",
            "#$id ← ${response.code} ${response.message} ${request.url} (${elapsed}ms, ${bodyText.length} chars logged)",
        )
        response.headers.forEach { (name, value) ->
            AppLogger.d("HTTP", "#$id   Response-Header: $name: $value")
        }
        if (bodyText.isNotEmpty()) {
            logBodyChunks(id, "Response-Body", bodyText)
        } else {
            AppLogger.d("HTTP", "#$id   Response-Body: <empty>")
        }
        if (!response.isSuccessful) {
            AppLogger.e("HTTP", "#$id   HTTP error ${response.code} for ${request.url}")
        }
        return response
    }

    private fun logBodyChunks(id: Long, label: String, body: String) {
        if (body.length <= CHUNK_SIZE) {
            AppLogger.d("HTTP", "#$id   $label: $body")
            return
        }
        var offset = 0
        var part = 1
        while (offset < body.length) {
            val end = minOf(offset + CHUNK_SIZE, body.length)
            AppLogger.d("HTTP", "#$id   $label[$part]: ${body.substring(offset, end)}")
            offset = end
            part++
        }
        if (body.length >= MAX_BODY_LOG_BYTES) {
            AppLogger.d("HTTP", "#$id   $label: … truncated at $MAX_BODY_LOG_BYTES bytes")
        }
    }

    private fun redactHeader(name: String, value: String): String = when {
        name.equals("Authorization", ignoreCase = true) -> redactAuth(value)
        name.equals("Cookie", ignoreCase = true) -> "<redacted cookies>"
        else -> value
    }

    private fun redactAuth(value: String): String = when {
        value.startsWith("Bearer ", ignoreCase = true) ->
            "Bearer <token len=${value.length - 7}>"
        value.startsWith("Basic ", ignoreCase = true) -> "Basic <redacted>"
        else -> "<redacted>"
    }

    private fun redactBody(raw: String): String {
        var out = raw
        out = PASSWORD_JSON.replace(out) { "password\":\"***\"" }
        out = PASSWORD_JSON_SINGLE.replace(out) { "password':'***'" }
        if (out.length > CHUNK_SIZE) {
            out = out.take(CHUNK_SIZE) + "… (${out.length} chars total)"
        }
        return out
    }

    companion object {
        private const val MAX_BODY_LOG_BYTES = 32L * 1024
        private const val CHUNK_SIZE = 3500
        private val PASSWORD_JSON = Regex(""""password"\s*:\s*"[^"]*"""", RegexOption.IGNORE_CASE)
        private val PASSWORD_JSON_SINGLE = Regex(""""password'\s*:\s*'[^']*'""", RegexOption.IGNORE_CASE)
    }
}
