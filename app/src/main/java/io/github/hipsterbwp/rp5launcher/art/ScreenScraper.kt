package io.github.hipsterbwp.rp5launcher.art

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * ScreenScraper.fr credentials, kept in app-private preferences (never in the editable config
 * file). The API needs both a developer ID/password and the player's own free account.
 */
class ScreenScraperAccount(context: Context) {
    private val prefs = context.getSharedPreferences("screenscraper", Context.MODE_PRIVATE)

    data class Credentials(val user: String, val password: String, val devId: String, val devPassword: String) {
        val complete: Boolean get() = listOf(user, password, devId, devPassword).all { it.isNotBlank() }
    }

    fun load() = Credentials(
        prefs.getString(USER, "").orEmpty(),
        prefs.getString(PASSWORD, "").orEmpty(),
        prefs.getString(DEV_ID, "").orEmpty(),
        prefs.getString(DEV_PASSWORD, "").orEmpty(),
    )

    fun save(c: Credentials) {
        prefs.edit()
            .putString(USER, c.user.trim())
            .putString(PASSWORD, c.password)
            .putString(DEV_ID, c.devId.trim())
            .putString(DEV_PASSWORD, c.devPassword)
            .apply()
    }

    companion object {
        private const val USER = "user"
        private const val PASSWORD = "password"
        private const val DEV_ID = "devId"
        private const val DEV_PASSWORD = "devPassword"
    }
}

/** What ScreenScraper knows about one game. URLs carry the credentials ScreenScraper needs. */
data class ScreenScraperGame(
    /** Front box cover (box-2D). */
    val boxUrl: String?,
    val fanartUrl: String?,
    val videoUrl: String?,
    val screenshotUrl: String?,
    val description: String?,
    /** 0..1 (ScreenScraper rates out of 20). */
    val rating: Float?,
    val year: String?,
    val publisher: String?,
    val developer: String?,
    val genre: String?,
    val players: String?,
)

/** ScreenScraper refused the whole session (bad login, quota, API closed): stop asking. */
class ScreenScraperStop(message: String) : Exception(message)

/** Minimal client for ScreenScraper's API v2 (https://www.screenscraper.fr/webapi2.php). */
class ScreenScraper(private val creds: ScreenScraperAccount.Credentials) {
    private val json = Json { ignoreUnknownKeys = true }

    private fun url(endpoint: String, extra: Map<String, String>): String {
        val params = linkedMapOf(
            "devid" to creds.devId,
            "devpassword" to creds.devPassword,
            "softname" to SOFTNAME,
            "output" to "json",
            "ssid" to creds.user,
            "sspassword" to creds.password,
        ) + extra
        return "$BASE/$endpoint?" + params.entries.joinToString("&") { (k, v) -> "$k=${Http.segment(v)}" }
    }

    /** Checks the login. Returns a short status line, or throws [ScreenScraperStop]. */
    fun testLogin(): String {
        val body = request(url("ssuserInfos.php", emptyMap()))
            ?: throw ScreenScraperStop("ScreenScraper didn't recognise this account.")
        val user = json.parseToJsonElement(body).jsonObject["response"]?.jsonObject?.get("ssuser")?.jsonObject
            ?: throw ScreenScraperStop("Unexpected answer from ScreenScraper.")
        val today = user.text("requeststoday")
        val max = user.text("maxrequestsperday")
        return "Signed in as ${user.text("id") ?: creds.user}" +
            if (today != null && max != null) " · $today of $max requests used today" else ""
    }

    /** Looks a ROM up. Returns null when ScreenScraper doesn't know it. */
    fun lookup(systemId: Int, fileName: String, sizeBytes: Long, crc: String?): ScreenScraperGame? {
        val params = buildMap {
            put("systemeid", systemId.toString())
            put("romtype", "rom")
            put("romnom", fileName)
            if (sizeBytes > 0) put("romtaille", sizeBytes.toString())
            crc?.let { put("crc", it) }
        }
        val body = request(url("jeuInfos.php", params)) ?: return null
        val game = runCatching { json.parseToJsonElement(body).jsonObject["response"]?.jsonObject?.get("jeu")?.jsonObject }
            .getOrNull() ?: return null
        val medias = game["medias"]?.jsonArray?.mapNotNull { it as? JsonObject }.orEmpty()
        return ScreenScraperGame(
            boxUrl = media(medias, "box-2D"),
            fanartUrl = media(medias, "fanart"),
            videoUrl = media(medias, "video-normalized") ?: media(medias, "video"),
            screenshotUrl = media(medias, "ss"),
            description = localized(game["synopsis"]),
            rating = game["note"]?.jsonObject?.text("text")?.toFloatOrNull()?.let { (it / 20f).coerceIn(0f, 1f) },
            year = byRegion(game["dates"])?.take(4)?.takeIf { it.all(Char::isDigit) },
            publisher = game["editeur"]?.jsonObject?.text("text"),
            developer = game["developpeur"]?.jsonObject?.text("text"),
            genre = game["genres"]?.jsonArray?.firstOrNull()?.jsonObject?.get("noms")?.let(::localized),
            players = game["joueurs"]?.jsonObject?.text("text"),
        )
    }

    /** GET with ScreenScraper's error codes turned into a stop (account-wide) or null (not found). */
    private fun request(url: String): String? = try {
        Http.get(url)?.toString(Charsets.UTF_8)
    } catch (e: Http.Status) {
        when (e.code) {
            400 -> null // ScreenScraper uses 400 for "missing/unknown ROM" too
            401, 403 -> throw ScreenScraperStop("Login refused. Check the username, password and developer ID.")
            423 -> throw ScreenScraperStop("ScreenScraper's API is closed right now. Try again later.")
            426 -> throw ScreenScraperStop("ScreenScraper blocked this app version.")
            429, 430 -> throw ScreenScraperStop("ScreenScraper's daily limit for this account is used up.")
            431 -> throw ScreenScraperStop("Too many unrecognised ROMs today; ScreenScraper paused lookups.")
            else -> throw ScreenScraperStop("ScreenScraper error ${e.code}${if (e.body.isNotBlank()) ": ${e.body}" else ""}")
        }
    }

    private fun JsonObject.text(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

    private fun regionRank(region: String?): Int = REGIONS.indexOf(region).let { if (it < 0) REGIONS.size else it }

    private fun media(medias: List<JsonObject>, type: String): String? =
        medias.filter { it.text("type") == type }.minByOrNull { regionRank(it.text("region")) }?.text("url")

    /** Picks the English entry from ScreenScraper's [{"langue": "en", "text": …}] lists. */
    private fun localized(element: JsonElement?): String? {
        val items = (element as? JsonArray)?.mapNotNull { it as? JsonObject } ?: return null
        val pick = items.firstOrNull { it.text("langue") == "en" } ?: items.firstOrNull()
        return pick?.text("text")
    }

    /** Picks the best-region entry from [{"region": "us", "text": …}] lists. */
    private fun byRegion(element: JsonElement?): String? =
        (element as? JsonArray)?.mapNotNull { it as? JsonObject }?.minByOrNull { regionRank(it.text("region")) }?.text("text")

    companion object {
        private const val BASE = "https://api.screenscraper.fr/api2"
        private const val SOFTNAME = "rp5-launcher"
        private val REGIONS = listOf("us", "wor", "ss", "eu", "uk", "jp")

        /** ES-DE system name -> ScreenScraper system id. */
        val SYSTEM_IDS = mapOf(
            "nes" to 3, "famicom" to 3, "fds" to 106, "snes" to 4, "sfc" to 4,
            "gb" to 9, "gbc" to 10, "gba" to 12, "virtualboy" to 11,
            "n64" to 14, "nds" to 15, "n3ds" to 17, "gc" to 13, "wii" to 16, "wiiu" to 18, "switch" to 225,
            "psx" to 57, "ps2" to 58, "ps3" to 59, "psp" to 61, "psvita" to 62,
            "mastersystem" to 2, "megadrive" to 1, "genesis" to 1, "segacd" to 20, "sega32x" to 19,
            "saturn" to 22, "dreamcast" to 23, "gamegear" to 21,
            "pcengine" to 31, "tg16" to 31, "ngp" to 25, "ngpc" to 82, "neogeo" to 142, "arcade" to 75,
            "atari2600" to 26, "atari7800" to 41, "atarilynx" to 28, "atarijaguar" to 27,
            "wonderswan" to 45, "wonderswancolor" to 46,
        )
    }
}
