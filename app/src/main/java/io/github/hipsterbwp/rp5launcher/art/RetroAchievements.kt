package io.github.hipsterbwp.rp5launcher.art

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.net.URLEncoder

/** Your RetroAchievements username and web API key, kept in app-private storage (not the config file). */
class RetroAchievementsAccount(context: Context) {
    private val prefs = context.getSharedPreferences("retroachievements", Context.MODE_PRIVATE)

    data class Credentials(val user: String, val key: String) {
        val complete: Boolean get() = user.isNotBlank() && key.isNotBlank()
    }

    fun load() = Credentials(prefs.getString(USER, "").orEmpty(), prefs.getString(KEY, "").orEmpty())

    fun save(c: Credentials) {
        prefs.edit().putString(USER, c.user.trim()).putString(KEY, c.key.trim()).apply()
    }

    private companion object {
        const val USER = "user"
        const val KEY = "key"
    }
}

/** Unlocked and total achievements for one game. */
data class RaProgress(val raGameId: Int, val awarded: Int, val total: Int, val hardcore: Int)

data class RaUnlock(val title: String, val game: String, val points: Int, val date: String)

data class RaSummary(val points: Int, val rank: Int?, val recent: List<RaUnlock>)

/** One game in RetroAchievements' list for a console. */
data class RaGame(val id: Int, val title: String, val achievements: Int)

/**
 * RetroAchievements web API (retroachievements.org/API). Needs your username and the web API key
 * from your RetroAchievements settings page. Games are matched to RetroAchievements by title.
 * Blocking; call from Dispatchers.IO.
 */
class RetroAchievements(private val cacheDir: File, private val creds: RetroAchievementsAccount.Credentials) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun call(endpoint: String, params: Map<String, String> = emptyMap()): JsonElement {
        val query = (mapOf("y" to creds.key) + params).entries.joinToString("&") { (k, v) ->
            "$k=${URLEncoder.encode(v, "UTF-8")}"
        }
        val body = Http.get("$API/$endpoint.php?$query", "application/json") ?: throw Http.Status(404)
        val element = json.parseToJsonElement(body.toString(Charsets.UTF_8))
        // Bad keys come back as {"message": "...", "errors": [...]}.
        (element as? JsonObject)?.get("message")?.let { message ->
            if (element["errors"] != null || element.size == 1) throw Http.Status(401, message.str().orEmpty())
        }
        return element
    }

    fun summary(): RaSummary {
        val o = call("API_GetUserSummary", mapOf("u" to creds.user, "g" to "0", "a" to RECENT.toString())).jsonObject
        val recent = ArrayList<RaUnlock>()
        // RecentAchievements: { gameId: { achievementId: {...} } } (an array when empty).
        o["RecentAchievements"]?.let { r ->
            val games = (r as? JsonObject)?.values ?: (r as? JsonArray).orEmpty()
            for (game in games) {
                val achievements = (game as? JsonObject)?.values ?: (game as? JsonArray).orEmpty()
                for (a in achievements) {
                    val ach = a as? JsonObject ?: continue
                    if (ach["IsAwarded"]?.int() == 0) continue
                    recent += RaUnlock(
                        title = ach["Title"]?.str().orEmpty(),
                        game = ach["GameTitle"]?.str().orEmpty(),
                        points = ach["Points"]?.int() ?: 0,
                        date = ach["DateAwarded"]?.str().orEmpty(),
                    )
                }
            }
        }
        return RaSummary(
            points = o["TotalPoints"]?.int() ?: 0,
            rank = o["Rank"]?.int(),
            recent = recent.sortedByDescending { it.date }.take(RECENT),
        )
    }

    /** Your progress in every game you've played, by RetroAchievements game id. */
    fun progress(): Map<Int, RaProgress> {
        val out = HashMap<Int, RaProgress>()
        var offset = 0
        while (true) {
            val o = call(
                "API_GetUserCompletionProgress",
                mapOf("u" to creds.user, "c" to PAGE.toString(), "o" to offset.toString()),
            ).jsonObject
            val results = o["Results"] as? JsonArray ?: break
            for (r in results) {
                val g = r as? JsonObject ?: continue
                val id = g["GameID"]?.int() ?: continue
                out[id] = RaProgress(id, g["NumAwarded"]?.int() ?: 0, g["MaxPossible"]?.int() ?: 0, g["NumAwardedHardcore"]?.int() ?: 0)
            }
            offset += results.size
            val total = o["Total"]?.int() ?: 0
            if (results.isEmpty() || offset >= total) break
        }
        return out
    }

    /** Games with achievements for one console, cached for a week. */
    fun games(consoleId: Int): List<RaGame> {
        val cache = File(cacheDir, "console-$consoleId.json")
        val text = if (cache.exists() && System.currentTimeMillis() - cache.lastModified() < LIST_TTL_MS) {
            cache.readText()
        } else {
            runCatching {
                call("API_GetGameList", mapOf("i" to consoleId.toString(), "f" to "1", "h" to "0")).toString()
                    .also { cacheDir.mkdirs(); cache.writeText(it) }
            }.getOrElse { if (cache.exists()) cache.readText() else throw it }
        }
        return json.parseToJsonElement(text).jsonArray.mapNotNull { e ->
            val g = e as? JsonObject ?: return@mapNotNull null
            RaGame(g["ID"]?.int() ?: return@mapNotNull null, g["Title"]?.str().orEmpty(), g["NumAchievements"]?.int() ?: 0)
        }
    }

    companion object {
        private const val API = "https://retroachievements.org/API"
        private const val PAGE = 500
        private const val RECENT = 5
        private const val LIST_TTL_MS = 7L * 24 * 60 * 60 * 1000

        /** RetroAchievements console ids by ES-DE system name. */
        val CONSOLES = mapOf(
            "megadrive" to 1, "genesis" to 1, "n64" to 2, "snes" to 3, "sfc" to 3, "gb" to 4, "gba" to 5,
            "gbc" to 6, "nes" to 7, "famicom" to 7, "pcengine" to 8, "tg16" to 8, "segacd" to 9, "megacd" to 9,
            "sega32x" to 10, "mastersystem" to 11, "psx" to 12, "atarilynx" to 13, "ngp" to 14, "ngpc" to 14,
            "gamegear" to 15, "gc" to 16, "atarijaguar" to 17, "nds" to 18, "ps2" to 21, "pokemini" to 24,
            "atari2600" to 25, "arcade" to 27, "fbneo" to 27, "mame" to 27, "virtualboy" to 28, "msx" to 29,
            "sg-1000" to 33, "saturn" to 39, "dreamcast" to 40, "psp" to 41, "3do" to 43,
            "colecovision" to 44, "intellivision" to 45, "vectrex" to 46, "atari7800" to 51,
            "wonderswan" to 53, "wonderswancolor" to 53, "neogeocd" to 56, "pcenginecd" to 76, "tg-cd" to 76,
        )

        /** Picks the RetroAchievements game for a ROM title (alternate names after "|" count too). */
        fun match(games: List<RaGame>, title: String): RaGame? {
            val want = Scraper.key(title)
            if (want.isEmpty()) return null
            return games
                .filter { !it.title.startsWith("~") && "[Subset" !in it.title }
                .firstOrNull { g -> g.title.split("|").any { Scraper.key(it) == want } }
        }

        private fun JsonElement.str(): String? = (this as? JsonPrimitive)?.contentOrNull
        private fun JsonElement.int(): Int? = str()?.toIntOrNull() ?: str()?.toDoubleOrNull()?.toInt()
    }
}
