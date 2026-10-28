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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Background music while browsing: Everest's own tracks, or the player's MP3/OGG/… files from a
 * "music" folder in the ROMs folder. Shuffled, looping, faded in and out, and paused while a game
 * runs (the activity pauses then). Tracks can be switched off one by one, or one put on repeat.
 */
class BackgroundMusic(private val context: Context, private val scope: CoroutineScope) {
    /** One playable track; [key] is what the config stores (built-in name or file name). */
    data class Track(val key: String, val title: String, val uri: Uri)

    private val player = ExoPlayer.Builder(context).build().apply {
        setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_GAME).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
            /* handleAudioFocus = */ true,
        )
        repeatMode = Player.REPEAT_MODE_ALL
        shuffleModeEnabled = true
        volume = 0f
        addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                nowPlaying = mediaItem?.mediaId
            }
        })
    }
    private var enabled = false
    private var target = 0f
    private var resumed = false
    private var ducked = false
    private var loadedFrom: String? = null
    private var queued: List<String>? = null
    private var off: Set<String> = emptySet()
    private var repeat: String? = null
    private var fade: Job? = null

    /** All tracks found (on or off), for the Sound settings page. */
    var tracks by mutableStateOf<List<Track>>(emptyList())
        private set
    /** Whether [tracks] are the player's own files rather than Everest's. */
    var ownTracks by mutableStateOf(false)
        private set
    /** Key of the track playing now. */
    var nowPlaying by mutableStateOf<String?>(null)
        private set

    /** How many tracks are queued and whether they're the player's own. */
    val source: Pair<Int, Boolean> get() = tracks.size to ownTracks

    /**
     * Applies settings; (re)loads the track list when the ROMs folder changes, and rebuilds the
     * queue when tracks are switched off or one is put on repeat.
     */
    fun configure(on: Boolean, volume: Float, romsRoot: Uri?, off: Set<String>, repeat: String?) {
        enabled = on
        target = volume
        this.off = off
        this.repeat = repeat
        val key = romsRoot?.toString().orEmpty()
        if (on && loadedFrom != key) {
            loadedFrom = key
            scope.launch {
                val user = withContext(Dispatchers.IO) { romsRoot?.let(::userTracks).orEmpty() }
                tracks = user.ifEmpty {
                    BUILT_IN.map { (key, title, res) -> Track(key, title, RawResourceDataSource.buildRawResourceUri(res)) }
                }
                ownTracks = user.isNotEmpty()
                requeue()
            }
        } else {
            requeue()
        }
    }

    /** Queues the tracks that are on: just the repeating one if set, else all of them shuffled. */
    private fun requeue() {
        val on = tracks.filter { it.key !in off }
        val pick = on.firstOrNull { it.key == repeat }?.let(::listOf) ?: on
        val keys = pick.map { it.key }
        if (keys != queued) {
            queued = keys
            val current = player.currentMediaItem?.mediaId
            player.setMediaItems(pick.map { MediaItem.Builder().setUri(it.uri).setMediaId(it.key).build() })
            // Keep the current song going if it's still in the queue.
            pick.indexOfFirst { it.key == current }.takeIf { it >= 0 }?.let { player.seekTo(it, player.currentPosition) }
            player.repeatMode = if (pick.size == 1) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_ALL
            player.prepare()
        }
        update()
    }

    fun skip() {
        if (player.mediaItemCount > 1) player.seekToNextMediaItem()
    }

    /** Quieter while a console's startup sound plays. */
    fun duck(on: Boolean) {
        if (ducked == on) return
        ducked = on
        update()
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
            val to = if (!play) 0f else if (ducked) target * DUCK else target
            val steps = FADE_STEPS
            for (i in 1..steps) {
                player.volume = from + (to - from) * i / steps
                delay(FADE_MS / steps)
            }
            if (!play) player.pause()
        }
    }

    /** Audio files directly inside <ROMs>/music (or _music). */
    private fun userTracks(root: Uri): List<Track> = runCatching {
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
            .sortedBy { (_, name, _) -> name.lowercase() }
            .map { (id, name, _) -> Track(name, name.substringBeforeLast('.'), DocumentsContract.buildDocumentUriUsingTree(root, id)) }
    }.getOrDefault(emptyList())

    companion object {
        /** Folder names (in the ROMs folder) that hold the player's own music. */
        val MUSIC_FOLDERS = setOf("music", "_music")
        private val AUDIO_EXTENSIONS = setOf("mp3", "ogg", "m4a", "aac", "flac", "wav", "opus")
        private val BUILT_IN = listOf(
            Triple("neon_drift", "Neon Drift", R.raw.music_neon_drift),
            Triple("pixel_garden", "Pixel Garden", R.raw.music_pixel_garden),
            Triple("after_hours", "After Hours", R.raw.music_after_hours),
            Triple("after_hours_8bit", "After Hours (8-bit)", R.raw.music_after_hours_8bit),
        )
        private const val DUCK = 0.3f
        private const val FADE_MS = 700L
        private const val FADE_STEPS = 14

        /** Loudest the music gets at 100%: it stays background, under game videos and sounds. */
        private const val MAX_VOLUME = 0.7f

        /** Config volume in percent to player volume. */
        fun volume(percent: Int): Float = percent.coerceIn(0, 100) / 100f * MAX_VOLUME
    }
}
