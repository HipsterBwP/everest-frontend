package io.github.hipsterbwp.rp5launcher.art

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import com.caverock.androidsvg.SVG
import java.io.File

/**
 * Console illustrations and fact sheets from ES-DE's "Slate" theme
 * (https://gitlab.com/es-de/themes/slate-es-de, CC BY-NC-SA), fetched on demand:
 * - images/consolegame.svg (console with a game), falling back to images/controller.svg
 * - systeminfo.xml ("Manufacturer: Sony Electronics", "Units sold: 155 million", …)
 */
class SystemArtStore(context: Context) {
    private val dir = File(context.filesDir, "art/system").apply { mkdirs() }

    fun image(system: String): File? = File(dir, "$system.png").takeIf { it.exists() }

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
        val png = File(dir, "$system.png")
        val facts = File(dir, "$system.facts")
        val miss = File(dir, "$system.none")
        val recentMiss = miss.exists() && System.currentTimeMillis() - miss.lastModified() < RETRY_MS
        if (!force && ((png.exists() && facts.exists()) || recentMiss)) return@filter false
        var gained = false
        if (force || !facts.exists()) {
            runCatching { Http.get("$SLATE/$system/systeminfo.xml") }.getOrNull()?.toString(Charsets.UTF_8)?.let { xml ->
                val lines = Regex("""<text>([^<]+)</text>""").findAll(xml).map { it.groupValues[1].trim() }
                    .filter { ':' in it }.toList()
                if (lines.isNotEmpty()) {
                    facts.writeText(lines.joinToString("\n"))
                    gained = true
                }
            }
        }
        if (force || !png.exists()) {
            gained = listOf("consolegame.svg", "controller.svg").any { name -> render("$SLATE/$system/images/$name", png) } || gained
        }
        if (!gained && !png.exists()) miss.writeText("") else miss.delete()
        gained
    }

    private fun render(url: String, out: File): Boolean = runCatching {
        val text = Http.get(url)?.toString(Charsets.UTF_8) ?: return false
        val svg = SVG.getFromString(LogoStore.inlineEntities(text))
        val box = svg.documentViewBox
        val aspect = if (box != null && box.height() > 0) box.width() / box.height() else 1f
        val height = HEIGHT
        val width = (height * aspect).toInt().coerceIn(1, height * 4)
        svg.setDocumentWidth(width.toFloat())
        svg.setDocumentHeight(height.toFloat())
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        svg.renderToCanvas(Canvas(bitmap))
        val tmp = File(out.parentFile, out.name + ".tmp")
        tmp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        tmp.renameTo(out)
    }.getOrDefault(false)

    companion object {
        private const val SLATE = "https://gitlab.com/es-de/themes/slate-es-de/-/raw/master"
        private const val HEIGHT = 480
        private const val RETRY_MS = 7L * 24 * 60 * 60 * 1000
    }
}
