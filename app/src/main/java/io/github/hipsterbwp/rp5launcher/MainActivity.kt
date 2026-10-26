package io.github.hipsterbwp.rp5launcher

import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import io.github.hipsterbwp.rp5launcher.ui.LauncherApp
import io.github.hipsterbwp.rp5launcher.ui.LauncherViewModel
import io.github.hipsterbwp.rp5launcher.ui.LocalPad
import io.github.hipsterbwp.rp5launcher.ui.Pad
import io.github.hipsterbwp.rp5launcher.ui.PadDispatcher
import io.github.hipsterbwp.rp5launcher.ui.theme.LauncherTheme

class MainActivity : ComponentActivity() {
    private val vm: LauncherViewModel by viewModels()
    private val pad = PadDispatcher()

    /** Analog triggers: once the device sends L2/R2 key events, ignore the trigger axes. */
    private var triggerKeysSeen = false
    private var l2Down = false
    private var r2Down = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()
        setContent {
            LauncherTheme {
                CompositionLocalProvider(LocalPad provides pad) {
                    LauncherApp(vm)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        vm.onResumed()
    }

    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    /**
     * All controller input goes through here rather than Compose focus, so every screen gets
     * the same predictable D-pad/button handling. The left stick and hat arrive as synthesized
     * D-pad key events from the framework.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val button = Pad.fromKeyCode(event.keyCode) ?: return super.dispatchKeyEvent(event)
        if (button == Pad.L2 || button == Pad.R2) triggerKeysSeen = true
        if (event.action == KeyEvent.ACTION_DOWN) pad.dispatch(button, event.repeatCount)
        return true
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (!triggerKeysSeen && event.isFromSource(InputDevice.SOURCE_JOYSTICK) &&
            event.action == MotionEvent.ACTION_MOVE
        ) {
            val l2 = maxOf(event.getAxisValue(MotionEvent.AXIS_LTRIGGER), event.getAxisValue(MotionEvent.AXIS_BRAKE)) > TRIGGER_THRESHOLD
            val r2 = maxOf(event.getAxisValue(MotionEvent.AXIS_RTRIGGER), event.getAxisValue(MotionEvent.AXIS_GAS)) > TRIGGER_THRESHOLD
            if (l2 && !l2Down) pad.dispatch(Pad.L2, 0)
            if (r2 && !r2Down) pad.dispatch(Pad.R2, 0)
            l2Down = l2
            r2Down = r2
        }
        return super.dispatchGenericMotionEvent(event)
    }

    private companion object {
        const val TRIGGER_THRESHOLD = 0.5f
    }
}
