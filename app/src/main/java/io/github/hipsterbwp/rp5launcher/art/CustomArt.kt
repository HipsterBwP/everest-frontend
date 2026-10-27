package io.github.hipsterbwp.rp5launcher.art

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import java.io.File

/**
 * Your own console backdrops: images in an "_art" folder inside the ROMs folder, named after the
 * system ("snes.jpg", "ps2.png"). They're copied into app storage so they load fast and survive
 * the SD card being slow to mount.
 */
object CustomArt {
    const val FOLDER = "_art"
    private val EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")

    private data class Child(val docId: String, val name: String, val isDir: Boolean, val size: Long, val modified: Long)

    /** Copies new or changed images into [dir] and returns lower-case base name -> local file. */
    fun sync(resolver: ContentResolver, root: Uri, dir: File): Map<String, File> {
        val folder = children(resolver, root, DocumentsContract.getTreeDocumentId(root))
            .firstOrNull { it.isDir && it.name.equals(FOLDER, ignoreCase = true) }
        if (folder == null) {
            dir.deleteRecursively()
            return emptyMap()
        }
        dir.mkdirs()
        val out = HashMap<String, File>()
        for (child in children(resolver, root, folder.docId)) {
            val ext = child.name.substringAfterLast('.', "").lowercase()
            if (child.isDir || ext !in EXTENSIONS || child.name.startsWith(".")) continue
            val key = child.name.substringBeforeLast('.').trim().lowercase()
            val local = File(dir, "$key.$ext")
            if (local.length() != child.size || local.lastModified() != child.modified) {
                val tmp = File(dir, "$key.tmp")
                val copied = runCatching {
                    resolver.openInputStream(DocumentsContract.buildDocumentUriUsingTree(root, child.docId))?.use { input ->
                        tmp.outputStream().use { input.copyTo(it) }
                    }
                }.getOrNull() != null
                if (!copied || !tmp.renameTo(local)) {
                    tmp.delete()
                    continue
                }
                if (child.modified > 0) local.setLastModified(child.modified)
            }
            out[key] = local
        }
        val keep = out.values.toSet()
        dir.listFiles()?.filter { it !in keep }?.forEach { it.delete() }
        return out
    }

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
        return out
    }
}
