package io.github.hipsterbwp.rp5launcher.art

import io.github.hipsterbwp.rp5launcher.BuildConfig
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Minimal blocking HTTP GET for art and metadata downloads. Call from Dispatchers.IO. */
internal object Http {
    private const val TIMEOUT_MS = 20_000

    /** Non-OK response; [body] is the server's error text, if any (ScreenScraper explains errors there). */
    class Status(val code: Int, val body: String = "") : IOException("HTTP $code${if (body.isNotBlank()) ": $body" else ""}")

    private val RETRY_DELAYS_MS = longArrayOf(1_000, 3_000, 8_000)

    /** GitLab throttles bursts from one device; space its requests out. */
    private const val GITLAB_GAP_MS = 400L
    private var lastGitLab = 0L

    @Synchronized
    private fun pace(url: String) {
        if ("gitlab.com" !in url) return
        val wait = lastGitLab + GITLAB_GAP_MS - System.currentTimeMillis()
        if (wait > 0) Thread.sleep(wait)
        lastGitLab = System.currentTimeMillis()
    }

    /**
     * Returns the body, or null on 404. Retries rate limits (429), server errors and dropped
     * connections three times with growing pauses, then throws ([Status] or the IOException).
     */
    fun get(url: String, accept: String? = null): ByteArray? {
        var attempt = 0
        while (true) {
            try {
                pace(url)
                return getOnce(url, accept)
            } catch (e: IOException) {
                val retryable = e !is Status || e.code == 429 || e.code >= 500
                if (!retryable || attempt >= RETRY_DELAYS_MS.size) throw e
                Thread.sleep(RETRY_DELAYS_MS[attempt++])
            }
        }
    }

    private fun getOnce(url: String, accept: String?): ByteArray? {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "rp5-launcher/${BuildConfig.VERSION_NAME} (https://github.com/HipsterBwP/rp5-launcher)")
            accept?.let { setRequestProperty("Accept", it) }
        }
        try {
            return when (val code = conn.responseCode) {
                HttpURLConnection.HTTP_OK -> conn.inputStream.use { it.readBytes() }
                HttpURLConnection.HTTP_NOT_FOUND -> null
                else -> throw Status(code, runCatching { conn.errorStream?.bufferedReader()?.use { it.readText() } }.getOrNull().orEmpty().trim().take(200))
            }
        } finally {
            conn.disconnect()
        }
    }

    /** Percent-encodes one path segment (spaces as %20, not +). */
    fun segment(s: String): String = URLEncoder.encode(s, "UTF-8").replace("+", "%20")
}
