package io.github.hipsterbwp.rp5launcher.art

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** A game's Wikipedia intro and the article's lead image (usually the box cover). */
data class WikiGame(val description: String?, val imageUrl: String?)

/**
 * Game descriptions and covers from English Wikipedia's search API (no account needed). Only
 * accepts an article whose title matches the game and whose intro says it's a video game.
 */
object Wikipedia {
    private const val API = "https://en.wikipedia.org/w/api.php"
    private const val SENTENCES = 3
    private const val COVER_PX = 640
    private val json = Json { ignoreUnknownKeys = true }

    /** Returns a short intro, or null if no matching article. Throws IOException on network trouble. */
    fun describe(title: String, platform: String): String? = lookup(title, platform)?.description

    /** The matching article's intro and lead image, or null. Throws IOException on network trouble. */
    fun lookup(title: String, platform: String): WikiGame? {
        val clean = title.replace(Regex(""",\s*The\b"""), "").replace(" - ", ": ")
        val query = "$clean $platform video game"
        val url = "$API?action=query&format=json&formatversion=2&redirects=1" +
            "&generator=search&gsrlimit=3&gsrsearch=${Http.segment(query)}" +
            "&prop=extracts%7Cpageimages&exintro=1&explaintext=1&exsentences=$SENTENCES" +
            // Game covers are non-free images, so ask for any licence.
            "&piprop=thumbnail&pithumbsize=$COVER_PX&pilicense=any"
        val body = Http.get(url)?.toString(Charsets.UTF_8) ?: return null
        val pages = runCatching {
            json.parseToJsonElement(body).jsonObject["query"]?.jsonObject?.get("pages")?.jsonArray
        }.getOrNull() ?: return null
        val want = Scraper.key(title)
        return pages.map { it.jsonObject }
            .sortedBy { it["index"]?.jsonPrimitive?.intOrNull ?: Int.MAX_VALUE }
            .firstNotNullOfOrNull { page ->
                val pageTitle = page["title"]?.jsonPrimitive?.contentOrNull ?: return@firstNotNullOfOrNull null
                val extract = page["extract"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                val have = Scraper.key(pageTitle)
                val titleMatches = want.isNotEmpty() && (have.startsWith(want) || want.startsWith(have))
                if (!titleMatches || !extract.contains("video game", ignoreCase = true)) return@firstNotNullOfOrNull null
                val image = page["thumbnail"]?.jsonObject?.get("source")?.jsonPrimitive?.contentOrNull
                WikiGame(extract, image)
            }
    }
}
