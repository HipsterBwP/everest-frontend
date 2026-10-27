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
    /** Left out of lists (BIOS files, duplicates, games you don't want to see). */
    val hidden: Boolean = false,
    /** Launch command label for this game only; null = the system's choice. */
    val emulator: String? = null,
)

@Serializable
data class StatsFile(
    val games: Map<String, GameStats> = emptyMap(),
    /** Days with at least one launch ("2026-10-27"), for streaks. */
    val playDays: Set<String> = emptySet(),
    /** Your own collections: name -> game ids, in the order added. */
    val collections: Map<String, List<String>> = emptyMap(),
)

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
        val folders = children(rootUri, rootDocId).filter {
            it.isDir && !it.name.startsWith(".") && it.name.lowercase() !in NON_SYSTEM_FOLDERS
        }
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
        val entries = children(root, dirDocId)
        // Multi-disc and multi-track games: show the playlist/sheet once, not every file it lists.
        val referenced = entries
            .filter { !it.isDir && it.name.substringAfterLast('.', "").lowercase() in SHEET_EXTENSIONS && it.size in 1..SHEET_MAX_BYTES }
            .flatMap { sheet -> referencedFiles(root, sheet) }
            .toHashSet()
        for (child in entries) {
            if (child.name.startsWith(".")) continue
            if (child.isDir) {
                if (depth < MAX_DEPTH) collectGames(root, child.docId, extensions, out, depth + 1)
                continue
            }
            val ext = child.name.substringAfterLast('.', "").lowercase()
            if (ext !in extensions) continue
            if (child.name.lowercase() in referenced) continue
            if (BIOS.containsMatchIn(child.name)) continue
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

    /** File names (lower case) listed by an .m3u playlist, .cue sheet or .gdi track list. */
    private fun referencedFiles(root: Uri, sheet: Child): List<String> {
        val uri = DocumentsContract.buildDocumentUriUsingTree(root, sheet.docId)
        val text = runCatching { resolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
            ?: return emptyList()
        val ext = sheet.name.substringAfterLast('.').lowercase()
        return text.lineSequence().mapNotNull { raw ->
            val line = raw.trim()
            when {
                line.isEmpty() || line.startsWith("#") -> null
                ext == "m3u" -> line
                ext == "cue" -> CUE_FILE.find(line)?.groupValues?.get(1)
                ext == "gdi" -> GDI_TRACK.find(line)?.groupValues?.let { it[1].ifEmpty { it[2] } }
                else -> null
            }
        }.map { it.replace('\\', '/').substringAfterLast('/').lowercase() }.toList()
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
        /** Folders in the ROMs folder that aren't systems (your own background music). */
        private val NON_SYSTEM_FOLDERS = setOf("music", "_music")
        private val SHEET_EXTENSIONS = setOf("m3u", "cue", "gdi")
        private const val SHEET_MAX_BYTES = 64L * 1024
        private val CUE_FILE = Regex("""^FILE\s+"([^"]+)"""", RegexOption.IGNORE_CASE)
        private val GDI_TRACK = Regex("""^\d+\s+\d+\s+\d+\s+\d+\s+(?:"([^"]+)"|(\S+))""")
        /** BIOS and firmware files that share extensions with games. */
        private val BIOS = Regex("""(?i)(^|[^a-z])bios([^a-z]|$)""")
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
