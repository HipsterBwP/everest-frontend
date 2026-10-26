package io.github.hipsterbwp.rp5launcher.art

import io.github.hipsterbwp.rp5launcher.BuildConfig
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Minimal blocking HTTP GET for art and metadata downloads. Call from Dispatchers.IO. */
internal object Http {
    private const val TIMEOUT_MS = 20_000

    class Status(val code: Int) : IOException("HTTP $code")

    /** Returns the body, or null on 404. Throws [Status] for other errors. */
    fun get(url: String, accept: String? = null): ByteArray? {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "rp5-launcher/${BuildConfig.VERSION_NAME}")
            accept?.let { setRequestProperty("Accept", it) }
        }
        try {
            return when (val code = conn.responseCode) {
                HttpURLConnection.HTTP_OK -> conn.inputStream.use { it.readBytes() }
                HttpURLConnection.HTTP_NOT_FOUND -> null
                else -> throw Status(code)
            }
        } finally {
            conn.disconnect()
        }
    }

    /** Percent-encodes one path segment (spaces as %20, not +). */
    fun segment(s: String): String = URLEncoder.encode(s, "UTF-8").replace("+", "%20")
}
