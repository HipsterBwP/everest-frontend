package io.github.hipsterbwp.rp5launcher.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class Game(
    /** SAF document URI string; stable id for stats. */
    val id: String,
    val fileName: String,
    val title: String,
    /** First bracketed tag from the file name, e.g. "USA". */
    val tag: String? = null,
    /** Real file system path when the folder is on local storage or an SD card. */
    val path: String? = null,
    val sizeBytes: Long = 0,
) {
    val extension: String get() = fileName.substringAfterLast('.', "").lowercase()
}

@Serializable
data class ScannedSystem(
    val name: String,
    val folderName: String,
    val games: List<Game>,
)

@Serializable
data class LibraryCache(
    val rootUri: String,
    val scannedAt: Long,
    val systems: List<ScannedSystem>,
    /** Subfolders that didn't match any known system. */
    val unknownFolders: List<String> = emptyList(),
)

@Serializable
data class GameStats(
    val favorite: Boolean = false,
    val lastPlayed: Long = 0,
    val playCount: Int = 0,
    val playtimeMs: Long = 0,
)

@Serializable
data class StatsFile(val games: Map<String, GameStats> = emptyMap())

/** Small JSON files in app-private storage: the last scan and play stats. */
class LibraryStore(context: Context) {
    private val libraryFile = File(context.filesDir, "library.json")
    private val statsFile = File(context.filesDir, "stats.json")

    fun loadLibrary(): LibraryCache? = runCatching {
        CompactJson.decodeFromString(LibraryCache.serializer(), libraryFile.readText())
    }.getOrNull()

    fun saveLibrary(cache: LibraryCache) = writeAtomic(libraryFile, CompactJson.encodeToString(LibraryCache.serializer(), cache))

    fun loadStats(): StatsFile = runCatching {
        CompactJson.decodeFromString(StatsFile.serializer(), statsFile.readText())
    }.getOrDefault(StatsFile())

    fun saveStats(stats: StatsFile) = writeAtomic(statsFile, CompactJson.encodeToString(StatsFile.serializer(), stats))

    private fun writeAtomic(file: File, text: String) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(text)
        tmp.renameTo(file)
    }
}

/** Walks the ROMs folder through the Storage Access Framework. Each subfolder is a system. */
class LibraryScanner(private val resolver: ContentResolver, private val catalog: EsdeCatalog) {

    private data class Child(val docId: String, val name: String, val isDir: Boolean, val size: Long)

    fun scan(rootUri: Uri, onProgress: (systemsDone: Int, systemsTotal: Int) -> Unit = { _, _ -> }): LibraryCache {
        val rootDocId = DocumentsContract.getTreeDocumentId(rootUri)
        val folders = children(rootUri, rootDocId).filter { it.isDir && !it.name.startsWith(".") }
        val systems = ArrayList<ScannedSystem>()
        val unknown = ArrayList<String>()
        folders.forEachIndexed { i, folder ->
            val def = catalog.systemForFolder(folder.name)
            if (def == null) {
                unknown += folder.name
            } else {
                val games = ArrayList<Game>()
                collectGames(rootUri, folder.docId, def.extensions, games, depth = 0)
                if (games.isNotEmpty()) {
                    games.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
                    systems += ScannedSystem(def.name, folder.name, games)
                }
            }
            onProgress(i + 1, folders.size)
        }
        systems.sortBy { SystemMeta.info(catalog.system(it.name)!!).displayName.lowercase() }
        return LibraryCache(rootUri.toString(), System.currentTimeMillis(), systems, unknown.sorted())
    }

    private fun collectGames(root: Uri, dirDocId: String, extensions: Set<String>, out: MutableList<Game>, depth: Int) {
        for (child in children(root, dirDocId)) {
            if (child.name.startsWith(".")) continue
            if (child.isDir) {
                if (depth < MAX_DEPTH) collectGames(root, child.docId, extensions, out, depth + 1)
                continue
            }
            val ext = child.name.substringAfterLast('.', "").lowercase()
            if (ext !in extensions) continue
            val (title, tag) = cleanTitle(child.name)
            out += Game(
                id = DocumentsContract.buildDocumentUriUsingTree(root, child.docId).toString(),
                fileName = child.name,
                title = title,
                tag = tag,
                path = realPath(child.docId),
                sizeBytes = child.size,
            )
        }
    }

    private fun children(root: Uri, docId: String): List<Child> {
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(root, docId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
        )
        val out = ArrayList<Child>()
        resolver.query(uri, projection, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                out += Child(
                    docId = c.getString(0),
                    name = c.getString(1) ?: continue,
                    isDir = c.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR,
                    size = if (c.isNull(3)) 0 else c.getLong(3),
                )
            }
        }
        return out
    }

    companion object {
        private const val MAX_DEPTH = 2
        private val TAG = Regex("""\s*[(\[]([^)\]]*)[)\]]""")

        /** "Chrono Trigger (USA) [!].sfc" -> ("Chrono Trigger", "USA"). */
        fun cleanTitle(fileName: String): Pair<String, String?> {
            val base = fileName.substringBeforeLast('.')
            val tag = TAG.find(base)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
            val title = base.replace(TAG, "").replace('_', ' ').trim().ifEmpty { base }
            return title to tag
        }

        /**
         * Maps an external-storage document id ("primary:ROMs/snes/x.sfc" or "1234-ABCD:ROMs/x.sfc")
         * to a real path. Emulators like RetroArch need a path, not a content URI.
         */
        fun realPath(docId: String): String? {
            val volume = docId.substringBefore(':', "")
            if (volume.isEmpty()) return null
            val rel = docId.substringAfter(':')
            val base = if (volume == "primary") Environment.getExternalStorageDirectory().path else "/storage/$volume"
            return if (rel.isEmpty()) base else "$base/$rel"
        }
    }
}
