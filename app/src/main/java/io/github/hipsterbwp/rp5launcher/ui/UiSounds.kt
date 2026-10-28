package io.github.hipsterbwp.rp5launcher.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock
import io.github.hipsterbwp.rp5launcher.R

enum class UiSound { Move, Select, Back, Tab, Launch, Error, Achievement, Secret }

/** Short 8-bit navigation sounds (synthesized by tools/music/sfx.py). Toggle and volume live in config. */
class UiSounds(context: Context) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val ids = mapOf(
        UiSound.Move to pool.load(context, R.raw.ui_move, 1),
        UiSound.Select to pool.load(context, R.raw.ui_select, 1),
        UiSound.Back to pool.load(context, R.raw.ui_back, 1),
        UiSound.Tab to pool.load(context, R.raw.ui_tab, 1),
        UiSound.Launch to pool.load(context, R.raw.ui_launch, 1),
        UiSound.Error to pool.load(context, R.raw.ui_error, 1),
        UiSound.Achievement to pool.load(context, R.raw.ui_achievement, 1),
        UiSound.Secret to pool.load(context, R.raw.ui_secret, 1),
    )

    private var lastMoveAt = 0L
    private var lastSelectStream = 0
    private var lastSelectAt = 0L

    fun play(sound: UiSound, volume: Float) {
        val now = SystemClock.uptimeMillis()
        // Held directions repeat fast; keep the ticks from turning into a buzz.
        if (sound == UiSound.Move) {
            if (now - lastMoveAt < MOVE_GAP_MS) return
            lastMoveAt = now
        }
        // A launch or error replaces the click of the button press that caused it.
        if ((sound == UiSound.Launch || sound == UiSound.Error) && now - lastSelectAt < REPLACE_WINDOW_MS) {
            pool.stop(lastSelectStream)
        }
        val stream = pool.play(ids.getValue(sound), volume, volume, 1, 0, 1f)
        if (sound == UiSound.Select) {
            lastSelectStream = stream
            lastSelectAt = now
        }
    }

    fun release() = pool.release()

    companion object {
        private const val MOVE_GAP_MS = 45L
        private const val REPLACE_WINDOW_MS = 150L

        fun forButton(button: Pad): UiSound? = when (button) {
            Pad.Up, Pad.Down, Pad.Left, Pad.Right -> UiSound.Move
            Pad.A, Pad.X, Pad.Y, Pad.Start -> UiSound.Select
            Pad.B -> UiSound.Back
            Pad.L1, Pad.R1, Pad.L2, Pad.R2 -> UiSound.Tab
            Pad.Select -> null
        }

        /** Config volume level (1–3) to SoundPool volume. */
        fun volume(level: Int): Float = when (level) {
            1 -> 0.3f
            3 -> 1f
            else -> 0.6f
        }
    }
}
