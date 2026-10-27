package io.github.hipsterbwp.rp5launcher.art

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Your own files per console, named after the system ("snes.jpg", "ps2.ogg"): backdrops ([ART]) and
 * startup sounds ([SOUNDS]). They come from two places:
 *  - a folder in the ROMs folder such as "_art" (also found inside an "arcrow-art" folder, one level
 *    of subfolders deep, or as an "arcrow-art….zip" there or in the ROMs folder itself);
 *  - files or a zip imported from Settings.
 * Both are copied into app storage so they load fast and survive the SD card being slow to mount.
 */
class CustomMedia private constructor(
    private val folder: String,
    private val packPrefix: String,
    private val extensions: Set<String>,
) {
    companion object {
        val ART = CustomMedia("_art", "arcrow-art", setOf("jpg", "jpeg", "png", "webp"))
        val SOUNDS = CustomMedia("_sounds", "arcrow-sounds", setOf("ogg", "mp3", "wav", "m4a", "aac", "flac", "opus"))
        private const val STAMP = ".stamp"
    }

    private data class Child(val docId: String, val name: String, val isDir: Boolean, val size: Long, val modified: Long) {
        val ext: String get() = name.substringAfterLast('.', "").lowercase()
    }

    /** Copies new or changed pictures from the ROMs folder into [dir]; returns system key -> local file. */
    fun sync(resolver: ContentResolver, root: Uri, dir: File): Map<String, File> {
        val top = children(resolver, root, DocumentsContract.getTreeDocumentId(root))
        val folders = top.filter { it.isDir && (it.name.equals(folder, true) || it.name.lowercase().startsWith(packPrefix)) }
        // Pictures directly in those folders, and one level down (zip apps often add a folder of their own).
        val files = top.filter { isPack(it) } + folders.flatMap { found ->
            val inside = children(resolver, root, found.docId)
            inside.filter { !it.isDir } + inside.filter { it.isDir }.flatMap { children(resolver, root, it.docId) }.filter { !it.isDir }
        }
        if (files.isEmpty()) {
            dir.deleteRecursively()
            return emptyMap()
        }
        dir.mkdirs()
        val out = HashMap<String, File>()
        val keep = HashSet<File>()
        fun open(child: Child): InputStream? =
            resolver.openInputStream(DocumentsContract.buildDocumentUriUsingTree(root, child.docId))
        // Zips first, so loose pictures with the same name win.
        for (zip in files.filter { isPack(it) }) {
            val stamp = File(dir, zip.name + STAMP)
            val signature = "${zip.size},${zip.modified}"
            val lines = stamp.takeIf { it.exists() }?.readLines()
            val previous = lines?.drop(1)?.map { File(dir, it) }
            val extracted = if (lines?.firstOrNull() == signature && previous != null && previous.all { it.exists() }) {
                previous
            } else {
                runCatching { open(zip)?.use { unzip(it, dir) } }.getOrNull()?.also { got ->
                    stamp.writeText((listOf(signature) + got.map { it.name }).joinToString("\n"))
                }.orEmpty()
            }
            keep += stamp
            extracted.forEach { out[key(it.name)] = it; keep += it }
        }
        for (child in files.filter { it.ext in extensions && !it.name.startsWith(".") }) {
            val local = File(dir, "${key(child.name)}.${child.ext}")
            if (local.length() != child.size || local.lastModified() != child.modified) {
                val copied = runCatching { open(child)?.use { copy(it, local) } }.getOrNull() == true
                if (!copied) continue
                if (child.modified > 0) local.setLastModified(child.modified)
            }
            out[key(child.name)] = local
            keep += local
        }
        dir.listFiles()?.filter { it !in keep }?.forEach { it.delete() }
        return out
    }

    /** Pictures imported from Settings, in [dir]. */
    fun imported(dir: File): Map<String, File> =
        dir.listFiles().orEmpty()
            .filter { it.extension.lowercase() in extensions }
            .associateBy { key(it.name) }

    /** Copies picked pictures, or the pictures inside picked zips, into [dir]. Returns how many were added. */
    fun import(resolver: ContentResolver, uris: List<Uri>, dir: File): Int {
        dir.mkdirs()
        var count = 0
        for (uri in uris) {
            val name = displayName(resolver, uri) ?: continue
            val ext = name.substringAfterLast('.', "").lowercase()
            runCatching {
                resolver.openInputStream(uri)?.use { input ->
                    when {
                        ext == "zip" -> count += unzip(input, dir).size
                        ext in extensions -> {
                            // Replace any earlier picture for the same system, whatever its format.
                            dir.listFiles()?.filter { key(it.name) == key(name) }?.forEach { it.delete() }
                            if (copy(input, File(dir, "${key(name)}.$ext"))) count++
                        }
                    }
                }
            }
        }
        return count
    }

    private fun key(fileName: String) = fileName.substringBeforeLast('.').trim().lowercase()

    private fun isPack(child: Child) =
        !child.isDir && child.ext == "zip" && child.name.lowercase().startsWith(packPrefix)

    /** Extracts the pictures in a zip (folders inside it are ignored) into [dir]. */
    private fun unzip(input: InputStream, dir: File): List<File> {
        val out = ArrayList<File>()
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name.substringAfterLast('/')
                val ext = name.substringAfterLast('.', "").lowercase()
                if (entry.isDirectory || ext !in extensions || name.startsWith(".")) continue
                dir.listFiles()?.filter { key(it.name) == key(name) && it.extension.lowercase() in extensions }
                    ?.forEach { it.delete() }
                val file = File(dir, "${key(name)}.$ext")
                if (copy(zip, file)) out += file
            }
        }
        return out
    }

    /** Writes [input] to [target] through a temp file, so a failed copy never leaves half a picture. */
    private fun copy(input: InputStream, target: File): Boolean {
        val tmp = File(target.parentFile, target.name + ".tmp")
        tmp.outputStream().use { input.copyTo(it) }
        if (tmp.renameTo(target)) return true
        tmp.delete()
        return false
    }

    private fun displayName(resolver: ContentResolver, uri: Uri): String? =
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        } ?: uri.lastPathSegment?.substringAfterLast('/')

    private fun children(resolver: ContentResolver, root: Uri, docId: String): List<Child> {
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(root, docId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
        val out = ArrayList<Child>()
        runCatching {
            resolver.query(uri, projection, null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    out += Child(
                        docId = c.getString(0),
                        name = c.getString(1) ?: continue,
                        isDir = c.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR,
                        size = if (c.isNull(3)) 0 else c.getLong(3),
                        modified = if (c.isNull(4)) 0 else c.getLong(4),
                    )
                }
            }
        }
        return out
    }
}
