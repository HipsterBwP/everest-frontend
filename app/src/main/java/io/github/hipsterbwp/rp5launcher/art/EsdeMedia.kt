package io.github.hipsterbwp.rp5launcher.art

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract

/**
 * Media ES-DE already downloaded: <ES-DE>/downloaded_media/<system>/videos/<rom name>.mp4. You pick
 * the ES-DE folder (or its downloaded_media folder) once; Everest reads the files where they are.
 */
class EsdeMedia(private val resolver: ContentResolver) {
    private data class Child(val docId: String, val name: String, val isDir: Boolean)

    /** ROM base name (lower case, no extension) -> video URI, for one system folder. */
    fun videos(tree: Uri, systemFolders: List<String>): Map<String, Uri> = runCatching {
        var top = children(tree, DocumentsContract.getTreeDocumentId(tree))
        top.firstOrNull { it.isDir && it.name.equals(MEDIA_DIR, ignoreCase = true) }?.let { top = children(tree, it.docId) }
        val wanted = systemFolders.map { it.lowercase() }
        val system = wanted.firstNotNullOfOrNull { want -> top.firstOrNull { it.isDir && it.name.lowercase() == want } }
            ?: return@runCatching emptyMap()
        val videos = children(tree, system.docId).firstOrNull { it.isDir && it.name.equals(VIDEOS, ignoreCase = true) }
            ?: return@runCatching emptyMap()
        children(tree, videos.docId)
            .filter { !it.isDir && it.name.substringAfterLast('.', "").lowercase() in EXTENSIONS }
            .associate { it.name.substringBeforeLast('.').lowercase() to DocumentsContract.buildDocumentUriUsingTree(tree, it.docId) }
    }.getOrDefault(emptyMap())

    private fun children(tree: Uri, docId: String): List<Child> {
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, docId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )
        val out = ArrayList<Child>()
        resolver.query(uri, projection, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                out += Child(c.getString(0), c.getString(1) ?: continue, c.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR)
            }
        }
        return out
    }

    private companion object {
        const val MEDIA_DIR = "downloaded_media"
        const val VIDEOS = "videos"
        val EXTENSIONS = setOf("mp4", "mkv", "webm", "avi", "mov")
    }
}
