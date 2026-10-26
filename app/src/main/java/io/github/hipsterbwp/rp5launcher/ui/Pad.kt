package io.github.hipsterbwp.rp5launcher.ui

import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.hipsterbwp.rp5launcher.ui.theme.Motion

/** Controller buttons. Keyboard keys map onto them so the app also works with a keyboard. */
enum class Pad(val glyph: String) {
    Up("↑"), Down("↓"), Left("←"), Right("→"),
    A("A"), B("B"), X("X"), Y("Y"),
    L1("L1"), R1("R1"), L2("L2"), R2("R2"),
    Start("START"), Select("SELECT");

    companion object {
        fun fromKeyCode(code: Int): Pad? = when (code) {
            KeyEvent.KEYCODE_DPAD_UP -> Up
            KeyEvent.KEYCODE_DPAD_DOWN -> Down
            KeyEvent.KEYCODE_DPAD_LEFT -> Left
            KeyEvent.KEYCODE_DPAD_RIGHT -> Right
            KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_SPACE -> A
            KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> B
            KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_X -> X
            KeyEvent.KEYCODE_BUTTON_Y, KeyEvent.KEYCODE_Y -> Y
            KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_Q, KeyEvent.KEYCODE_PAGE_UP -> L1
            KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_E, KeyEvent.KEYCODE_PAGE_DOWN -> R1
            KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_1 -> L2
            KeyEvent.KEYCODE_BUTTON_R2, KeyEvent.KEYCODE_3 -> R2
            KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_MENU -> Start
            KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_TAB -> Select
            else -> null
        }
    }
}

/** Steps to move for a held direction: accelerates the longer it's held. */
fun accelerated(repeat: Int): Int = when {
    repeat >= Motion.ACCEL_REPEATS_FASTER -> 3
    repeat >= Motion.ACCEL_REPEATS_FAST -> 2
    else -> 1
}

fun interface PadHandler {
    /** Returns true if the press was handled. [repeat] is the key-repeat count (0 = first press). */
    fun onPad(button: Pad, repeat: Int): Boolean
}

/** Routes controller presses to the most recently composed handler (the top screen or dialog). */
class PadDispatcher {
    private val handlers = ArrayList<PadHandler>()

    fun push(handler: PadHandler) {
        handlers += handler
    }

    fun remove(handler: PadHandler) {
        handlers -= handler
    }

    fun dispatch(button: Pad, repeat: Int): Boolean = handlers.lastOrNull()?.onPad(button, repeat) ?: false
}

val LocalPad = staticCompositionLocalOf<PadDispatcher> { error("No PadDispatcher") }

/** Registers [handler] for as long as the caller is composed. */
@Composable
fun PadInput(handler: (Pad, Int) -> Boolean) {
    val dispatcher = LocalPad.current
    val current by rememberUpdatedState(handler)
    DisposableEffect(dispatcher) {
        val h = PadHandler { button, repeat -> current(button, repeat) }
        dispatcher.push(h)
        onDispose { dispatcher.remove(h) }
    }
}
