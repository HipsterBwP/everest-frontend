package io.github.hipsterbwp.rp5launcher.art

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import com.caverock.androidsvg.SVG
import java.io.File

/**
 * Console illustrations and fact sheets from ES-DE's "Slate" theme
 * (https://gitlab.com/es-de/themes/slate-es-de, CC BY-NC-SA), fetched on demand:
 * - images/controller.svg (drawn sideways for Slate's vertical layout, so rotated upright here),
 *   falling back to images/consolegame.svg (console with a game) when a controller won't render
 * - systeminfo.xml ("Manufacturer: Sony Electronics", "Units sold: 155 million", …)
 */
class SystemArtStore(context: Context) {
    private val dir = File(context.filesDir, "art/system").apply {
        mkdirs()
        // Drop images from older art versions.
        listFiles { f -> f.name.endsWith(".png") && !f.name.endsWith("-$ART_VERSION.png") }?.forEach { it.delete() }
    }

    fun image(system: String): File? = png(system).takeIf { it.exists() }

    /** Versioned name, so art rendered by older builds (sideways controllers) is redrawn. */
    private fun png(system: String) = File(dir, "$system-$ART_VERSION.png")

    /** Label/value pairs, e.g. ("Units sold", "155 million"). */
    fun facts(system: String): List<Pair<String, String>> {
        val file = File(dir, "$system.facts")
        if (!file.exists()) return emptyList()
        return file.readLines().mapNotNull { line ->
            val label = line.substringBefore(':', "").trim()
            val value = line.substringAfter(':', "").trim()
            if (label.isEmpty() || value.isEmpty()) null else label to value
        }
    }

    /** Downloads what's missing; returns systems that gained an image or facts. */
    fun fetchMissing(systems: List<String>, force: Boolean = false): List<String> = systems.filter { system ->
        val png = png(system)
        val facts = File(dir, "$system.facts")
        val miss = File(dir, "$system.none")
        // Markers from older builds were also written on network errors; only trust current ones.
        val recentMiss = miss.exists() && miss.readText() == MARKER &&
            System.currentTimeMillis() - miss.lastModified() < RETRY_MS
        if (!force && ((png.exists() && facts.exists()) || recentMiss)) return@filter false
        var gained = false
        var failed = false
        if (force || !facts.exists()) {
            val xmlBytes = try {
                Http.get("$SLATE/$system/systeminfo.xml")
            } catch (e: java.io.IOException) {
                failed = true
                null
            }
            xmlBytes?.toString(Charsets.UTF_8)?.let { xml ->
                val lines = Regex("""<text>([^<]+)</text>""").findAll(xml).map { it.groupValues[1].trim() }
                    .filter { ':' in it }.toList()
                if (lines.isNotEmpty()) {
                    facts.writeText(lines.joinToString("\n"))
                    gained = true
                }
            }
        }
        if (force || !png.exists()) {
            val controller = render("$SLATE/$system/images/controller.svg", png, rotate = true)
            val result = if (controller == Fetch.OK) {
                Fetch.OK
            } else {
                val console = render("$SLATE/$system/images/consolegame.svg", png, rotate = false)
                when {
                    console == Fetch.OK -> Fetch.OK
                    controller == Fetch.FAILED || console == Fetch.FAILED -> Fetch.FAILED
                    else -> Fetch.MISSING
                }
            }
            if (result == Fetch.OK) gained = true
            if (result == Fetch.FAILED) failed = true
        }
        when {
            png.exists() -> miss.delete()
            // Remember "nothing here" only when the server said so, never after a network error.
            !gained && !failed -> miss.writeText(MARKER)
        }
        gained
    }

    private fun render(url: String, out: File, rotate: Boolean): Fetch {
        val text = try {
            Http.get(url)?.toString(Charsets.UTF_8)
        } catch (e: java.io.IOException) {
            return Fetch.FAILED
        } ?: return Fetch.MISSING
        // An SVG this renderer can't draw counts as missing, so the other image is tried.
        return if (draw(text, out, rotate)) Fetch.OK else Fetch.MISSING
    }

    private fun draw(text: String, out: File, rotate: Boolean): Boolean = runCatching {
        val svg = SVG.getFromString(LogoStore.inlineEntities(text))
        val box = svg.documentViewBox
        val aspect = if (box != null && box.height() > 0) box.width() / box.height() else 1f
        val height = HEIGHT
        val width = (height * aspect).toInt().coerceIn(1, height * 4)
        svg.setDocumentWidth(width.toFloat())
        svg.setDocumentHeight(height.toFloat())
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        svg.renderToCanvas(Canvas(bitmap))
        val upright = if (rotate) {
            // Slate draws controllers turned 90° clockwise; turn them back.
            Bitmap.createBitmap(bitmap, 0, 0, width, height, Matrix().apply { postRotate(-90f) }, true)
        } else {
            bitmap
        }
        val tmp = File(out.parentFile, out.name + ".tmp")
        tmp.outputStream().use { upright.compress(Bitmap.CompressFormat.PNG, 100, it) }
        tmp.renameTo(out)
    }.getOrDefault(false)

    companion object {
        private const val SLATE = "https://gitlab.com/es-de/themes/slate-es-de/-/raw/master"
        private const val HEIGHT = 480
        private const val ART_VERSION = 2
        private const val MARKER = "slate/2"
        private const val RETRY_MS = 7L * 24 * 60 * 60 * 1000
    }
}
