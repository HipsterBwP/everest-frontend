package io.github.hipsterbwp.everest.ui

import android.media.AudioAttributes
import android.media.MediaPlayer
import java.io.File

/**
 * Plays a console's startup sound when you land on it. One at a time: moving on cuts the last one
 * short. [onPlaying] lets the background music duck underneath.
 */
class ConsoleSounds(private val onPlaying: (Boolean) -> Unit) {
    private var player: MediaPlayer? = null

    fun play(file: File, volume: Float) {
        stop()
        val p = MediaPlayer()
        player = p
        runCatching {
            p.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            p.setDataSource(file.path)
            p.setVolume(volume, volume)
            p.setOnPreparedListener {
                onPlaying(true)
                it.start()
            }
            p.setOnCompletionListener { if (player === it) stop() }
            p.setOnErrorListener { mp, _, _ ->
                if (player === mp) stop()
                true
            }
            p.prepareAsync()
        }.onFailure { stop() }
    }

    fun stop() {
        val p = player ?: return
        player = null
        runCatching { p.stop() }
        p.release()
        onPlaying(false)
    }

    fun release() = stop()
}
