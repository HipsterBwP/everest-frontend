package io.github.hipsterbwp.rp5launcher

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.lifecycleScope
import io.github.hipsterbwp.rp5launcher.ui.BackgroundMusic
import io.github.hipsterbwp.rp5launcher.ui.ConsoleSounds
import io.github.hipsterbwp.rp5launcher.ui.QuickControls
import io.github.hipsterbwp.rp5launcher.ui.Screen
import kotlinx.coroutines.launch
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import io.github.hipsterbwp.rp5launcher.ui.LauncherApp
import io.github.hipsterbwp.rp5launcher.ui.LauncherViewModel
import io.github.hipsterbwp.rp5launcher.ui.LocalPad
import io.github.hipsterbwp.rp5launcher.ui.Pad
import io.github.hipsterbwp.rp5launcher.ui.PadDispatcher
import io.github.hipsterbwp.rp5launcher.ui.UiSounds
import io.github.hipsterbwp.rp5launcher.ui.theme.LauncherTheme

private data class MusicSettings(val on: Boolean, val volume: Float, val root: String?, val off: Set<String>, val repeat: String?)

class MainActivity : ComponentActivity() {
    private val vm: LauncherViewModel by viewModels()
    private val pad = PadDispatcher()
    private lateinit var sounds: UiSounds
    private lateinit var music: BackgroundMusic
    private lateinit var consoleSounds: ConsoleSounds

    /** Analog triggers: once the device sends L2/R2 key events, ignore the trigger axes. */
    private var triggerKeysSeen = false
    private var l2Down = false
    private var r2Down = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sounds = UiSounds(this)
        vm.soundPlayer = { sounds.play(it, UiSounds.volume(vm.config.soundVolume)) }
        music = BackgroundMusic(this, lifecycleScope)
        vm.music = music
        consoleSounds = ConsoleSounds { playing -> music.duck(playing) }
        vm.quickControls = QuickControls(this)
        vm.consoleSoundPlayer = { consoleSounds.play(it, (UiSounds.volume(vm.config.soundVolume) * CONSOLE_SOUND_GAIN).coerceAtMost(1f)) }
        // Follow the music settings; play quieter on screens with gameplay video when its sound is on.
        lifecycleScope.launch {
            snapshotFlow {
                val cfg = vm.config
                val videoScreen = vm.screen.let { it is Screen.Games || it is Screen.Search || it is Screen.Collection }
                val duck = if (cfg.videoSound && videoScreen) VIDEO_DUCK else 1f
                MusicSettings(vm.ready && cfg.music, BackgroundMusic.volume(cfg.musicLevel) * duck, cfg.romsRoot, cfg.musicOff, cfg.musicRepeat)
            }.collect { m -> music.configure(m.on, m.volume, m.root?.let(Uri::parse), m.off, m.repeat) }
        }
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

    override fun onPause() {
        super.onPause()
        consoleSounds.stop()
        music.onPause()
    }

    override fun onResume() {
        super.onResume()
        music.onResume()
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
        val mapped = Pad.fromKeyCode(event.keyCode) ?: return super.dispatchKeyEvent(event)
        val button = if (vm.config.swapButtons) swapped(event.keyCode, mapped) else mapped
        // While typing, keys belong to the text field; only B (cancel) and Start (save) reach us.
        if (vm.textInputActive && button != Pad.B && button != Pad.Start) return super.dispatchKeyEvent(event)
        if (button == Pad.L2 || button == Pad.R2) triggerKeysSeen = true
        // Select: a tap does what the screen says (e.g. random game); holding it opens the quick menu.
        if (button == Pad.Select && !vm.quickMenu) {
            when {
                event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0 -> {
                    selectHeld = true
                    quickOpened = false
                    handler.postDelayed(openQuickMenu, QUICK_MENU_HOLD_MS)
                }
                event.action == KeyEvent.ACTION_UP -> {
                    selectHeld = false
                    handler.removeCallbacks(openQuickMenu)
                    if (!quickOpened) press(Pad.Select, 0)
                }
            }
            return true
        }
        if (event.action == KeyEvent.ACTION_DOWN) press(button, event.repeatCount)
        return true
    }

    private val handler = Handler(Looper.getMainLooper())
    private var selectHeld = false
    private var quickOpened = false
    private val openQuickMenu = Runnable {
        if (!selectHeld) return@Runnable
        quickOpened = true
        if (vm.onInput()) return@Runnable
        UiSounds.forButton(Pad.Start)?.let(vm::sound)
        vm.quickMenu = true
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (!triggerKeysSeen && event.isFromSource(InputDevice.SOURCE_JOYSTICK) &&
            event.action == MotionEvent.ACTION_MOVE
        ) {
            val l2 = maxOf(event.getAxisValue(MotionEvent.AXIS_LTRIGGER), event.getAxisValue(MotionEvent.AXIS_BRAKE)) > TRIGGER_THRESHOLD
            val r2 = maxOf(event.getAxisValue(MotionEvent.AXIS_RTRIGGER), event.getAxisValue(MotionEvent.AXIS_GAS)) > TRIGGER_THRESHOLD
            if (l2 && !l2Down) press(Pad.L2, 0)
            if (r2 && !r2Down) press(Pad.R2, 0)
            l2Down = l2
            r2Down = r2
        }
        return super.dispatchGenericMotionEvent(event)
    }

    /** Swaps the gamepad face buttons (A/B, X/Y) for confirm-on-the-right layouts; keyboards stay as they are. */
    private fun swapped(keyCode: Int, button: Pad): Pad = when (keyCode) {
        KeyEvent.KEYCODE_BUTTON_A -> Pad.B
        KeyEvent.KEYCODE_BUTTON_B -> Pad.A
        KeyEvent.KEYCODE_BUTTON_X -> Pad.Y
        KeyEvent.KEYCODE_BUTTON_Y -> Pad.X
        else -> button
    }

    /** The device's Home button while Everest is the home app: back to the console screen. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) vm.goHome()
    }

    private fun press(button: Pad, repeat: Int) {
        // A press that only wakes the screensaver does nothing else.
        if (vm.onInput()) return
        // Click first, so a launch or error sound triggered by this press can replace it.
        UiSounds.forButton(button)?.let(vm::sound)
        pad.dispatch(button, repeat)
    }

    override fun onDestroy() {
        vm.soundPlayer = null
        vm.music = null
        vm.consoleSoundPlayer = null
        vm.quickControls = null
        handler.removeCallbacks(openQuickMenu)
        consoleSounds.release()
        sounds.release()
        music.release()
        super.onDestroy()
    }

    private companion object {
        const val TRIGGER_THRESHOLD = 0.5f
        const val VIDEO_DUCK = 0.35f
        /** Startup sounds play a little louder than menu clicks. */
        const val CONSOLE_SOUND_GAIN = 1.6f
        const val QUICK_MENU_HOLD_MS = 500L
    }
}
