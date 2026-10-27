package io.github.hipsterbwp.rp5launcher.art

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.caverock.androidsvg.SVG
import java.io.File
import kotlin.math.abs

/**
 * Console logos for the system tiles, downloaded on demand from the RetroPie "Carbon" theme
 * (https://github.com/RetroPie/es-theme-carbon), falling back to ES-DE's "Slate" theme
 * (https://gitlab.com/es-de/themes/slate-es-de, CC BY-NC-SA) for newer systems such as Switch and
 * 3DS. Logos are trademarks of their owners; they're fetched to the device, never bundled.
 *
 * Each logo is stored as a white-on-transparent PNG mask so the UI can tint it: white on a
 * system-color tile, system color on the focused (white) tile.
 */
/** Outcome of fetching one piece of art: got it, it doesn't exist, or try again later. */
enum class Fetch { OK, MISSING, FAILED }

class LogoStore(context: Context) {
    private val dir = File(context.filesDir, "art/logos").apply { mkdirs() }

    /** The logo mask for a system, if it has been downloaded. */
    fun file(system: String): File? = File(dir, "$system.png").takeIf { it.exists() }

    /** Downloads logos not tried in the last week. Returns systems that gained a logo. */
    fun fetchMissing(systems: List<String>, force: Boolean = false): List<String> =
        systems.filter { system ->
            val png = File(dir, "$system.png")
            val miss = File(dir, "$system.none")
            // A "not found" marker only counts if it was written with the current list of sources.
            val fresh = miss.exists() && miss.readText() == SOURCES_VERSION &&
                System.currentTimeMillis() - miss.lastModified() < RETRY_MS
            if (!force && (png.exists() || fresh)) return@filter false
            when (runCatching { download(system, png) }.getOrDefault(Fetch.FAILED)) {
                Fetch.OK -> { miss.delete(); true }
                // Only a definite "no logo anywhere" is remembered; network trouble retries next launch.
                Fetch.MISSING -> { miss.writeText(SOURCES_VERSION); false }
                Fetch.FAILED -> false
            }
        }

    private fun download(system: String, out: File): Fetch {
        val urls = carbonNames(system).map { "$CARBON/$it/art/system.svg" } + "$SLATE/$system/images/logo.svg"
        var failed = false
        val svgText = urls.firstNotNullOfOrNull { url ->
            try {
                Http.get(url)?.toString(Charsets.UTF_8)
            } catch (e: java.io.IOException) {
                failed = true
                null
            }
        } ?: return if (failed) Fetch.FAILED else Fetch.MISSING
        val svg = SVG.getFromString(inlineEntities(svgText))
        val box = svg.documentViewBox
        val aspect = when {
            box != null && box.height() > 0 -> box.width() / box.height()
            svg.documentHeight > 0 -> svg.documentWidth / svg.documentHeight
            else -> 1f
        }
        val width = if (aspect >= 1f) SIZE else (SIZE * aspect).toInt().coerceAtLeast(1)
        val height = if (aspect >= 1f) (SIZE / aspect).toInt().coerceAtLeast(1) else SIZE
        svg.setDocumentWidth(width.toFloat())
        svg.setDocumentHeight(height.toFloat())
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        svg.renderToCanvas(Canvas(bitmap))
        val mask = toMask(bitmap)
        val tmp = File(out.parentFile, out.name + ".tmp")
        tmp.outputStream().use { mask.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return if (tmp.renameTo(out)) Fetch.OK else Fetch.FAILED
    }

    companion object {
        private const val CARBON = "https://raw.githubusercontent.com/RetroPie/es-theme-carbon/master"
        private const val SLATE = "https://gitlab.com/es-de/themes/slate-es-de/-/raw/master"
        private const val SOURCES_VERSION = "carbon+slate/2"
        private const val SIZE = 512
        private const val RETRY_MS = 7L * 24 * 60 * 60 * 1000

        /** ES-DE system name -> Carbon folder names to try. */
        private fun carbonNames(system: String): List<String> = listOfNotNull(
            when (system) {
                "tg16" -> "tg16"
                "genesis" -> "genesis"
                else -> null
            },
            system,
        ).distinct()

        /** Adobe-exported SVGs declare namespaces as DTD entities; inline them and drop the DOCTYPE. */
        internal fun inlineEntities(svg: String): String {
            val entities = Regex("""<!ENTITY\s+(\w+)\s+"([^"]*)"""").findAll(svg)
                .associate { it.groupValues[1] to it.groupValues[2] }
            var out = svg.replace(Regex("""<!DOCTYPE[^\[>]*(\[[\s\S]*?])?\s*>"""), "")
            entities.forEach { (name, value) -> out = out.replace("&$name;", value) }
            return out
        }

        private fun luminance(c: Int): Float =
            (0.2126f * Color.red(c) + 0.7152f * Color.green(c) + 0.0722f * Color.blue(c)) / 255f

        /**
         * Turns a full-color logo into a white alpha mask. Most logos are solid shapes and keep their
         * alpha; "plate" logos (lettering on a filled badge, e.g. GBA, Mega Drive) keep only the
         * parts that contrast with the plate, so the lettering stays readable.
         */
        internal fun toMask(src: Bitmap): Bitmap {
            val w = src.width
            val h = src.height
            val px = IntArray(w * h)
            src.getPixels(px, 0, w, 0, 0, w, h)

            var minX = w; var minY = h; var maxX = -1; var maxY = -1
            val counts = HashMap<Int, Int>()
            var opaque = 0
            for (y in 0 until h) for (x in 0 until w) {
                val c = px[y * w + x]
                if (Color.alpha(c) > 200) {
                    opaque++
                    counts.merge(c or 0xFF000000.toInt(), 1, Int::plus)
                    if (x < minX) minX = x; if (x > maxX) maxX = x
                    if (y < minY) minY = y; if (y > maxY) maxY = y
                }
            }
            val out = IntArray(w * h)
            if (opaque == 0) return Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888)

            val coverage = opaque.toFloat() / ((maxX - minX + 1) * (maxY - minY + 1))
            val plateLum = luminance(counts.maxBy { it.value }.key)
            var contrasting = 0
            counts.forEach { (c, n) -> if (abs(luminance(c) - plateLum) > 0.25f) contrasting += n }
            val plate = coverage > 0.58f && contrasting.toFloat() / opaque > 0.2f

            for (i in px.indices) {
                val c = px[i]
                val a = Color.alpha(c)
                val alpha = if (plate) (a * (abs(luminance(c) - plateLum) * 2.5f).coerceAtMost(1f)).toInt() else a
                out[i] = Color.argb(alpha, 255, 255, 255)
            }
            return Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888)
        }
    }
}
