package com.recapflow.ai.media.ai

import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

enum class GeminiFailure {
    CANCELLED, NETWORK, TIMEOUT, KEY, REGION, QUOTA, MODEL, BLOCKED, SERVER,
    INVALID_RESPONSE, MEDIA_LIMIT, PROCESSING, REQUEST, STALE_SOURCE,
}

/** Never retain provider bodies, request URLs or API keys in a displayed/logged exception. */
class GeminiException(val failure: GeminiFailure) : IOException(failure.name) {
    var cleanupFailed: Boolean = false
}

class GeminiCancellation {
    private val cancelled = AtomicBoolean(false)
    @Volatile private var connection: HttpURLConnection? = null
    fun cancel() { cancelled.set(true); connection?.disconnect() }
    fun check() { if (cancelled.get() || Thread.currentThread().isInterrupted) throw GeminiException(GeminiFailure.CANCELLED) }
    internal fun attach(value: HttpURLConnection) { connection = value; check() }
    internal fun detach() { connection = null }
}

data class GeminiHttpResponse(val status: Int, val body: String, val headers: Map<String, String> = emptyMap())

/** Injectable wire boundary; production accepts only Google's HTTPS origin, without redirects. */
fun interface GeminiTransport {
    fun send(request: GeminiHttpRequest, cancellation: GeminiCancellation): GeminiHttpResponse
}

// Intentionally not a data class: generated toString/copy must not expose credentials or media.
class GeminiHttpRequest(
    val method: String,
    val url: String,
    val key: String,
    val headers: Map<String, String> = emptyMap(),
    val json: String? = null,
    val file: File? = null,
)

class GoogleGeminiTransport : GeminiTransport {
    override fun send(request: GeminiHttpRequest, cancellation: GeminiCancellation): GeminiHttpResponse {
        cancellation.check()
        val url = URL(request.url)
        if (url.protocol != "https" || url.host != HOST || url.port !in listOf(-1, 443) || url.userInfo != null) {
            throw GeminiException(GeminiFailure.INVALID_RESPONSE)
        }
        val connection = url.openConnection() as HttpURLConnection
        val timedOut = AtomicBoolean(false)
        val deadline = java.util.Timer("gemini-request-deadline", true)
        deadline.schedule(object : java.util.TimerTask() {
            override fun run() { timedOut.set(true); connection.disconnect() }
        }, if (request.method == "DELETE") 15_000L else 120_000L)
        try {
            cancellation.attach(connection)
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15_000
            connection.readTimeout = 90_000
            connection.requestMethod = request.method
            connection.setRequestProperty("x-goog-api-key", request.key)
            request.headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
            val bytes = request.json?.toByteArray(Charsets.UTF_8)
            if (bytes != null || request.file != null) {
                connection.doOutput = true
                connection.setFixedLengthStreamingMode(request.file?.length() ?: bytes!!.size.toLong())
                connection.outputStream.use { output ->
                    if (request.file != null) request.file.inputStream().use { input ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            cancellation.check()
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                        }
                    } else output.write(bytes!!)
                }
            }
            val status = connection.responseCode
            val input = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = input?.use { stream ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    cancellation.check()
                    val count = stream.read(buffer)
                    if (count < 0) break
                    if (output.size() + count > 2 * 1024 * 1024) throw GeminiException(GeminiFailure.INVALID_RESPONSE)
                    output.write(buffer, 0, count)
                }
                output.toString("UTF-8")
            }.orEmpty()
            return GeminiHttpResponse(status, body, connection.headerFields.entries
                .filter { it.key != null }.associate { it.key.lowercase() to it.value.firstOrNull().orEmpty() })
        } catch (error: IOException) {
            cancellation.check()
            if (timedOut.get()) throw GeminiException(GeminiFailure.TIMEOUT)
            if (error is GeminiException) throw error
            throw GeminiException(if (error is SocketTimeoutException) GeminiFailure.TIMEOUT else GeminiFailure.NETWORK)
        } finally {
            deadline.cancel()
            cancellation.detach()
            connection.disconnect()
        }
    }

    companion object { const val HOST = "generativelanguage.googleapis.com" }
}

object GeminiErrorPolicy {
    fun classify(status: Int, providerBody: String): GeminiFailure {
        val text = providerBody.lowercase()
        return when {
            status in listOf(400, 403) && ("user location is not supported" in text ||
                "unsupported location" in text || "not available in your country" in text) -> GeminiFailure.REGION
            status == 401 || (status in listOf(400, 403) && ("api_key_invalid" in text ||
                "api key not valid" in text || "reported as leaked" in text)) -> GeminiFailure.KEY
            status == 429 -> GeminiFailure.QUOTA
            status == 404 -> GeminiFailure.MODEL
            status == 408 || status == 504 -> GeminiFailure.TIMEOUT
            status in 500..599 -> GeminiFailure.SERVER
            else -> GeminiFailure.REQUEST
        }
    }
}
