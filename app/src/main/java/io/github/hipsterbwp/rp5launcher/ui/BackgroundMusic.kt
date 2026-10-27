@file:OptIn(UnstableApi::class)

package io.github.hipsterbwp.rp5launcher.ui

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import io.github.hipsterbwp.rp5launcher.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Background music while browsing: ARCROW's own tracks, or the player's MP3/OGG/… files from a
 * "music" folder in the ROMs folder. Shuffled, looping, faded in and out, and paused while a game
 * runs (the activity pauses then).
 */
class BackgroundMusic(private val context: Context, private val scope: CoroutineScope) {
    private val player = ExoPlayer.Builder(context).build().apply {
        setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_GAME).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
            /* handleAudioFocus = */ true,
        )
        repeatMode = Player.REPEAT_MODE_ALL
        shuffleModeEnabled = true
        volume = 0f
    }
    private var enabled = false
    private var target = 0f
    private var resumed = false
    private var loadedFrom: String? = null
    private var fade: Job? = null

    /** How many tracks are queued and whether they're the player's own. */
    var source: Pair<Int, Boolean> = 0 to false
        private set

    /** Applies settings; (re)builds the playlist when the ROMs folder changes. */
    fun configure(on: Boolean, volume: Float, romsRoot: Uri?) {
        enabled = on
        target = volume
        val key = romsRoot?.toString().orEmpty()
        if (on && loadedFrom != key) {
            loadedFrom = key
            scope.launch {
                val user = withContext(Dispatchers.IO) { romsRoot?.let(::userTracks).orEmpty() }
                val items = user.ifEmpty { BUILT_IN.map { RawResourceDataSource.buildRawResourceUri(it) } }
                source = items.size to user.isNotEmpty()
                player.setMediaItems(items.map(MediaItem::fromUri))
                player.prepare()
                update()
            }
        } else {
            update()
        }
    }

    fun skip() {
        if (player.mediaItemCount > 1) player.seekToNextMediaItem()
    }

    fun onResume() {
        resumed = true
        update()
    }

    fun onPause() {
        resumed = false
        // A game is starting: stop at once rather than fading over its intro.
        fade?.cancel()
        player.volume = 0f
        player.pause()
    }

    fun release() = player.release()

    private fun update() {
        val play = enabled && resumed && player.mediaItemCount > 0
        fade?.cancel()
        fade = scope.launch {
            if (play && !player.isPlaying) player.play()
            val from = player.volume
            val to = if (play) target else 0f
            val steps = FADE_STEPS
            for (i in 1..steps) {
                player.volume = from + (to - from) * i / steps
                delay(FADE_MS / steps)
            }
            if (!play) player.pause()
        }
    }

    /** Audio files directly inside <ROMs>/music (or _music). */
    private fun userTracks(root: Uri): List<Uri> = runCatching {
        val resolver = context.contentResolver
        val rootId = DocumentsContract.getTreeDocumentId(root)
        fun children(docId: String): List<Triple<String, String, String>> {
            val uri = DocumentsContract.buildChildDocumentsUriUsingTree(root, docId)
            val cols = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
            )
            val out = ArrayList<Triple<String, String, String>>()
            resolver.query(uri, cols, null, null, null)?.use { c ->
                while (c.moveToNext()) out += Triple(c.getString(0), c.getString(1) ?: "", c.getString(2) ?: "")
            }
            return out
        }
        val folder = children(rootId).firstOrNull { (_, name, mime) ->
            mime == DocumentsContract.Document.MIME_TYPE_DIR && name.lowercase() in MUSIC_FOLDERS
        } ?: return emptyList()
        children(folder.first)
            .filter { (_, name, _) -> name.substringAfterLast('.', "").lowercase() in AUDIO_EXTENSIONS }
            .map { (id, _, _) -> DocumentsContract.buildDocumentUriUsingTree(root, id) }
    }.getOrDefault(emptyList())

    companion object {
        /** Folder names (in the ROMs folder) that hold the player's own music. */
        val MUSIC_FOLDERS = setOf("music", "_music")
        private val AUDIO_EXTENSIONS = setOf("mp3", "ogg", "m4a", "aac", "flac", "wav", "opus")
        private val BUILT_IN = listOf(R.raw.music_neon_drift, R.raw.music_pixel_garden, R.raw.music_after_hours)
        private const val FADE_MS = 700L
        private const val FADE_STEPS = 14

        /** Config volume level (1–3) to player volume: background, never loud. */
        fun volume(level: Int): Float = when (level) {
            1 -> 0.18f
            3 -> 0.55f
            else -> 0.32f
        }
    }
}
