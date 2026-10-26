package io.github.hipsterbwp.rp5launcher.art

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import io.github.hipsterbwp.rp5launcher.data.Game
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.security.MessageDigest

/** Scraped art and info for one game. File names are relative to the art directory. */
@Serializable
data class GameArt(
    val box: String? = null,
    val snap: String? = null,
    val year: String? = null,
    val publisher: String? = null,
    val developer: String? = null,
    val genre: String? = null,
    /** The libretro name the game matched, e.g. "Chrono Trigger (USA)". */
    val matched: String? = null,
    val scrapedAt: Long = 0,
)

@Serializable
data class ArtIndex(val games: Map<String, GameArt> = emptyMap())

/**
 * Built-in scraper using libretro's free, account-less sources:
 * - libretro-thumbnails (https://github.com/libretro-thumbnails) for box art and screenshots
 * - libretro-database metadat (https://github.com/libretro/libretro-database) for year,
 *   publisher, developer and genre
 *
 * Games are matched by their No-Intro/Redump file name, falling back to a normalized title.
 */
class Scraper(context: Context) {
    val dir: File = File(context.filesDir, "art").apply { mkdirs() }
    private val cacheDir = File(dir, "cache").apply { mkdirs() }
    private val indexFile = File(dir, "art.json")
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): ArtIndex = runCatching {
        json.decodeFromString(ArtIndex.serializer(), indexFile.readText())
    }.getOrDefault(ArtIndex())

    fun save(index: ArtIndex) {
        val tmp = File(dir, "art.json.tmp")
        tmp.writeText(json.encodeToString(ArtIndex.serializer(), index))
        tmp.renameTo(indexFile)
    }

    fun file(name: String?): File? = name?.let { File(dir, it) }?.takeIf { it.exists() }

    fun supports(system: String): Boolean = LIBRETRO_NAMES.containsKey(system)

    /** A game is worth (re)scraping until it has box art. */
    fun needsArt(art: GameArt?): Boolean = art == null || art.scrapedAt == 0L || art.box == null

    /**
     * Scrapes [games] of one system, calling [onGame] as each finishes. Skips games already
     * scraped unless [force]. Call from Dispatchers.IO.
     */
    suspend fun scrapeSystem(
        system: String,
        games: List<Game>,
        existing: Map<String, GameArt>,
        force: Boolean,
        onGame: (Game, GameArt) -> Unit,
    ) {
        val name = LIBRETRO_NAMES[system] ?: return
        val todo = games.filter { force || needsArt(existing[it.id]) }
        if (todo.isEmpty()) return

        val repo = name.replace(' ', '_')
        val boxes = Matcher(index(repo, BOXARTS))
        val snaps = Matcher(index(repo, SNAPS))
        val previous = existing
        val meta = MetaFields.entries.associateWith { metadata(it, name) }

        val semaphore = Semaphore(PARALLEL)
        coroutineScope {
            todo.map { game ->
                async {
                    semaphore.withPermit {
                        ensureActive()
                        onGame(game, scrapeGame(game, repo, boxes, snaps, meta, previous[game.id]))
                    }
                }
            }.awaitAll()
        }
    }

    private fun scrapeGame(
        game: Game,
        repo: String,
        boxes: Matcher,
        snaps: Matcher,
        meta: Map<MetaFields, Map<String, String>>,
        previous: GameArt?,
    ): GameArt {
        val base = game.fileName.substringBeforeLast('.')
        val boxName = boxes.best(base, game.tag)
        val snapName = snaps.best(base, game.tag) ?: boxName
        val id = sha1(game.id)
        // A network hiccup (not a missing image) leaves the game marked unscraped so it's retried.
        var transient = false
        fun fetch(folder: String, name: String?, target: String, maxHeight: Int): String? =
            if (name == null) null
            else try {
                download(repo, folder, name, target, maxHeight)
            } catch (e: java.io.IOException) {
                transient = true
                null
            }
        val box = fetch(BOXARTS, boxName, "box/$id.webp", BOX_MAX_H) ?: previous?.box
        val snap = fetch(SNAPS, snapName, "snap/$id.webp", SNAP_MAX_H) ?: previous?.snap
        val metaName = boxName ?: snapName ?: base
        fun lookup(field: MetaFields) = meta[field]?.let { m -> m[metaName] ?: m[key(metaName)] ?: m[key(base)] }
        return GameArt(
            box = box,
            snap = snap,
            year = lookup(MetaFields.Year),
            publisher = lookup(MetaFields.Publisher),
            developer = lookup(MetaFields.Developer),
            genre = lookup(MetaFields.Genre),
            matched = boxName ?: snapName,
            scrapedAt = if (transient) 0 else System.currentTimeMillis(),
        )
    }

    /** Downloads, downsizes and stores one image. Returns its path relative to [dir], or null. */
    private fun download(repo: String, folder: String, name: String, target: String, maxHeight: Int): String? {
        val url = "$RAW/$repo/master/$folder/${Http.segment(name)}.png"
        val bytes = Http.get(url) ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outHeight / (sample * 2) >= maxHeight) sample *= 2
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return null
        val scaled = if (decoded.height > maxHeight) {
            Bitmap.createScaledBitmap(decoded, decoded.width * maxHeight / decoded.height, maxHeight, true)
        } else {
            decoded
        }
        val out = File(dir, target).apply { parentFile?.mkdirs() }
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP
        }
        out.outputStream().use { scaled.compress(format, WEBP_QUALITY, it) }
        return target
    }

    /** Thumbnail names in one libretro-thumbnails folder, cached for a week; null if unavailable. */
    private fun index(repo: String, folder: String): List<String>? {
        val cache = File(cacheDir, "$repo-$folder.txt")
        if (cache.exists() && System.currentTimeMillis() - cache.lastModified() < INDEX_TTL_MS) {
            return cache.readLines()
        }
        val names = runCatching {
            val root = tree("$API/$repo/git/trees/HEAD") ?: return@runCatching null
            val sha = root.firstOrNull { it.first == folder }?.second ?: return@runCatching emptyList()
            tree("$API/$repo/git/trees/$sha").orEmpty()
                .map { it.first }
                .filter { it.endsWith(".png") }
                .map { it.removeSuffix(".png") }
        }.getOrNull()
        if (names == null) return if (cache.exists()) cache.readLines() else null
        cache.writeText(names.joinToString("\n"))
        return names
    }

    /** (path, sha) entries of a GitHub git tree. */
    private fun tree(url: String): List<Pair<String, String>>? {
        val body = Http.get(url, "application/vnd.github+json") ?: return null
        return json.parseToJsonElement(body.toString(Charsets.UTF_8)).jsonObject["tree"]?.jsonArray?.map {
            val o = it.jsonObject
            o["path"]!!.jsonPrimitive.content to o["sha"]!!.jsonPrimitive.content
        }
    }

    /** One metadat field for a system: game name (and normalized key) -> value. Cached for a week. */
    private fun metadata(field: MetaFields, system: String): Map<String, String> {
        val cache = File(cacheDir, "${field.folder}-$system.dat")
        val fresh = cache.exists() && System.currentTimeMillis() - cache.lastModified() < INDEX_TTL_MS
        if (!fresh) {
            runCatching { Http.get("$METADAT/${field.folder}/${Http.segment(system)}.dat") }.getOrNull()
                ?.let { cache.writeBytes(it) }
        }
        if (!cache.exists()) return emptyMap()
        return parseDat(cache.readText(), field.key)
    }

    private enum class MetaFields(val folder: String, val key: String) {
        Year("releaseyear", "releaseyear"),
        Publisher("publisher", "publisher"),
        Developer("developer", "developer"),
        Genre("genre", "genre"),
    }

    /** Picks the best thumbnail for a ROM: exact name first, then same title preferring its region. */
    private class Matcher(names: List<String>?) {
        /** Without an index (e.g. GitHub's hourly API limit), guess the exact No-Intro file name. */
        private val guess = names == null
        private val exact = names.orEmpty().toHashSet()
        private val byKey = names.orEmpty().groupBy(::key)

        fun best(base: String, tag: String?): String? {
            val sanitized = sanitize(base)
            if (guess || sanitized in exact) return sanitized
            val candidates = byKey[key(base)] ?: return null
            val region = tag?.lowercase()
            return candidates.minByOrNull { c ->
                val lower = c.lowercase()
                var score = c.length
                if (region != null && region in lower) score -= 1000
                if ("(usa" in lower || "(world" in lower) score -= 500
                if ("(europe" in lower) score -= 300
                if (UNWANTED.any { it in lower }) score += 2000
                score
            }
        }
    }

    companion object {
        private const val RAW = "https://raw.githubusercontent.com/libretro-thumbnails"
        private const val API = "https://api.github.com/repos/libretro-thumbnails"
        private const val METADAT = "https://raw.githubusercontent.com/libretro/libretro-database/master/metadat"
        private const val BOXARTS = "Named_Boxarts"
        private const val SNAPS = "Named_Snaps"
        private const val PARALLEL = 4
        private const val BOX_MAX_H = 640
        private const val SNAP_MAX_H = 420
        private const val WEBP_QUALITY = 85
        private const val INDEX_TTL_MS = 7L * 24 * 60 * 60 * 1000

        private val UNWANTED = listOf("(beta", "(proto", "(demo", "(sample", "(pirate", "(hack", "[b]")
        private val TAGS = Regex("""\s*[(\[][^)\]]*[)\]]""")
        private val THE = Regex("""\bthe\b""")

        /** libretro replaces these characters with "_" in thumbnail file names. */
        private fun sanitize(name: String) = name.replace(Regex("""[&*/:`<>?\\|]"""), "_")

        /** Normalized title used for fuzzy matching: no tags, no "the", letters and digits only. */
        fun key(name: String): String = name.lowercase()
            .replace(TAGS, "")
            .replace("&", "and")
            .replace(THE, "")
            .filter { it.isLetterOrDigit() }

        /** Parses clrmamepro .dat blocks: game ( comment "Name" field "Value" ... ). */
        internal fun parseDat(text: String, field: String): Map<String, String> {
            val out = HashMap<String, String>()
            val names = ArrayList<String>()
            var value: String? = null
            val line = Regex("""^\s*(\w+)\s+"(.*)"\s*$""")
            for (raw in text.lineSequence()) {
                val trimmed = raw.trim()
                if (trimmed.startsWith("game (")) {
                    names.clear(); value = null
                    continue
                }
                if (trimmed == ")") {
                    value?.let { v ->
                        names.forEach { n ->
                            out.putIfAbsent(n, v)
                            out.putIfAbsent(key(n), v)
                        }
                    }
                    names.clear(); value = null
                    continue
                }
                val m = line.find(raw) ?: continue
                when (m.groupValues[1]) {
                    "comment", "name" -> names += m.groupValues[2]
                    field -> value = m.groupValues[2]
                }
            }
            return out
        }

        private fun sha1(s: String): String =
            MessageDigest.getInstance("SHA-1").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

        /** ES-DE system name -> libretro playlist/database name. */
        val LIBRETRO_NAMES = mapOf(
            "nes" to "Nintendo - Nintendo Entertainment System",
            "famicom" to "Nintendo - Nintendo Entertainment System",
            "fds" to "Nintendo - Family Computer Disk System",
            "snes" to "Nintendo - Super Nintendo Entertainment System",
            "sfc" to "Nintendo - Super Nintendo Entertainment System",
            "gb" to "Nintendo - Game Boy",
            "gbc" to "Nintendo - Game Boy Color",
            "gba" to "Nintendo - Game Boy Advance",
            "n64" to "Nintendo - Nintendo 64",
            "nds" to "Nintendo - Nintendo DS",
            "n3ds" to "Nintendo - Nintendo 3DS",
            "gc" to "Nintendo - GameCube",
            "wii" to "Nintendo - Wii",
            "virtualboy" to "Nintendo - Virtual Boy",
            "psx" to "Sony - PlayStation",
            "ps2" to "Sony - PlayStation 2",
            "psp" to "Sony - PlayStation Portable",
            "mastersystem" to "Sega - Master System - Mark III",
            "megadrive" to "Sega - Mega Drive - Genesis",
            "genesis" to "Sega - Mega Drive - Genesis",
            "segacd" to "Sega - Mega-CD - Sega CD",
            "sega32x" to "Sega - 32X",
            "saturn" to "Sega - Saturn",
            "dreamcast" to "Sega - Dreamcast",
            "gamegear" to "Sega - Game Gear",
            "pcengine" to "NEC - PC Engine - TurboGrafx 16",
            "tg16" to "NEC - PC Engine - TurboGrafx 16",
            "ngp" to "SNK - Neo Geo Pocket",
            "ngpc" to "SNK - Neo Geo Pocket Color",
            "atari2600" to "Atari - 2600",
            "atari7800" to "Atari - 7800",
            "atarilynx" to "Atari - Lynx",
            "wonderswan" to "Bandai - WonderSwan",
            "wonderswancolor" to "Bandai - WonderSwan Color",
        )
    }
}
