package io.github.hipsterbwp.everest.ui.screens

import io.github.hipsterbwp.everest.ui.neonPanel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import io.github.hipsterbwp.everest.ui.Hint
import io.github.hipsterbwp.everest.ui.HintBar
import io.github.hipsterbwp.everest.ui.LauncherViewModel
import io.github.hipsterbwp.everest.ui.ListRow
import io.github.hipsterbwp.everest.ui.Pad
import io.github.hipsterbwp.everest.ui.PadInput
import io.github.hipsterbwp.everest.ui.QuickControls
import io.github.hipsterbwp.everest.ui.Txt
import io.github.hipsterbwp.everest.ui.glass
import io.github.hipsterbwp.everest.ui.tap
import io.github.hipsterbwp.everest.ui.theme.Alpha
import io.github.hipsterbwp.everest.ui.theme.Palette
import io.github.hipsterbwp.everest.ui.theme.Radius
import io.github.hipsterbwp.everest.ui.theme.Size
import io.github.hipsterbwp.everest.ui.theme.Space
import io.github.hipsterbwp.everest.ui.theme.Type
import io.github.hipsterbwp.everest.ui.theme.rounded
import io.github.hipsterbwp.everest.ui.theme.u
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private enum class QuickRow { Brightness, Volume, Music, Wifi, Bluetooth, Settings }

/**
 * Hold Select anywhere: a glass sheet on the right with brightness, volume, music, Wi-Fi,
 * Bluetooth and Android settings. Left/Right adjust, A opens or toggles, B or Select closes.
 */
@Composable
fun QuickMenu(vm: LauncherViewModel, controls: QuickControls) {
    var row by remember { mutableIntStateOf(0) }
    // Device values aren't observable; re-read them after every change and every few seconds.
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(REFRESH_MS)
            tick++
        }
    }
    var brightness by remember { mutableStateOf(controls.brightness) }
    val rows = QuickRow.entries
    val current = rows[row]

    fun adjust(step: Int) {
        when (current) {
            QuickRow.Brightness -> {
                brightness = (brightness + step * BRIGHTNESS_STEP).coerceIn(0f, 1f)
                controls.setBrightness(brightness)
            }
            QuickRow.Volume -> controls.setVolume(controls.volume + step)
            QuickRow.Music -> if (step > 0) vm.music?.skip()
            else -> Unit
        }
        tick++
    }

    fun activate() {
        when (current) {
            QuickRow.Brightness -> if (!controls.systemWide) controls.allowSystemWide()
            QuickRow.Volume -> Unit
            QuickRow.Music -> vm.setMusic(!vm.config.music)
            QuickRow.Wifi -> controls.openWifi()
            QuickRow.Bluetooth -> controls.openBluetooth()
            QuickRow.Settings -> controls.openSettings()
        }
        tick++
    }

    PadInput { button, _ ->
        when (button) {
            Pad.Up -> { row = (row - 1).coerceAtLeast(0); true }
            Pad.Down -> { row = (row + 1).coerceAtMost(rows.size - 1); true }
            Pad.Left -> { adjust(-1); true }
            Pad.Right -> { adjust(1); true }
            Pad.A -> { activate(); true }
            Pad.B, Pad.Select, Pad.Start -> { vm.quickMenu = false; true }
            // Swallow everything else so the screen underneath doesn't react.
            else -> true
        }
    }

    Box(Modifier.fillMaxSize().background(Palette.black.copy(alpha = Alpha.BROWSER_DIM)).tap { vm.quickMenu = false }) {
        Column(
            Modifier
                .align(Alignment.CenterEnd)
                .width(Size.QUICK_MENU_W.u)
                .fillMaxHeight()
                .padding(top = Space.SCREEN_TOP.u, bottom = (Size.HINT_BAR + Space.LG).u, end = Space.SCREEN_H.u)
                .neonPanel(Radius.PANEL, fill = Palette.glassStrong)
                .padding(Space.SECTION.u),
            verticalArrangement = Arrangement.spacedBy(Space.LIST_GAP.u),
        ) {
            val (battery, charging) = remember(tick) { controls.battery }
            val volume = remember(tick) { controls.volume }
            val wifi = remember(tick) { controls.wifiConnected }
            Txt("QUICK MENU", Type.eyebrow, Palette.cyanSoft, glow = Palette.cyan)
            Txt(
                listOfNotNull(battery?.let { "Battery $it%" }, if (charging) "charging" else null).joinToString(" · ").ifEmpty { "EVEREST" },
                Type.screenTitleSmall, Palette.ink,
            )
            Spacer(Modifier.height(Space.SM.u))
            rows.forEachIndexed { i, r ->
                ListRow(
                    focused = i == row,
                    accent = Palette.white,
                    background = Palette.glassStrong,
                    padV = Space.ROW_V_SMALL,
                    onClick = { row = i; activate() },
                ) {
                    Column(Modifier.weight(1f)) {
                        when (r) {
                            QuickRow.Brightness -> {
                                Txt("Brightness", Type.rowTitle, Palette.ink)
                                Meter(brightness)
                                if (!controls.systemWide) Txt("Only while Everest is open · A to allow everywhere", Type.label, Palette.inkSecondary, maxLines = 2)
                            }
                            QuickRow.Volume -> {
                                Txt("Volume", Type.rowTitle, Palette.ink)
                                Meter(volume / controls.maxVolume.coerceAtLeast(1).toFloat())
                            }
                            QuickRow.Music -> {
                                Txt("Music", Type.rowTitle, Palette.ink)
                                val playing = vm.music?.let { m -> m.tracks.firstOrNull { it.key == m.nowPlaying }?.title }
                                Txt(
                                    if (!vm.config.music) "Off · A to turn on" else "${playing ?: "On"} · → next track",
                                    Type.label, Palette.inkSecondary,
                                )
                            }
                            QuickRow.Wifi -> {
                                Txt("Wi-Fi", Type.rowTitle, Palette.ink)
                                Txt(if (wifi) "Connected" else "Not connected", Type.label, Palette.inkSecondary)
                            }
                            QuickRow.Bluetooth -> Txt("Bluetooth", Type.rowTitle, Palette.ink)
                            QuickRow.Settings -> Txt("Android settings", Type.rowTitle, Palette.ink)
                        }
                    }
                    when (r) {
                        QuickRow.Brightness -> Txt("${(brightness * 100).roundToInt()}%", Type.labelStrong, Palette.ink)
                        QuickRow.Volume -> Txt("$volume", Type.labelStrong, Palette.ink)
                        QuickRow.Music -> Txt(if (vm.config.music) "On" else "Off", Type.labelStrong, if (vm.config.music) Palette.ok else Palette.inkSecondary)
                        else -> Unit
                    }
                }
            }
        }
        HintBar(
            listOf(
                Hint(Pad.A, "Select", onClick = { activate() }),
                Hint(listOf(Pad.Left, Pad.Right), "Adjust", onClick = { adjust(1) }),
                Hint(Pad.B, "Close", onClick = { vm.quickMenu = false }),
            ),
            onSystem = false,
            glyphInk = Palette.inkOnLight,
        )
    }
}

/** Thin level bar for brightness and volume. */
@Composable
private fun Meter(level: Float) {
    Box(
        Modifier
            .padding(top = Space.SM.u)
            .fillMaxWidth()
            .height(Size.PROGRESS_H.u)
            .clip(rounded(Radius.PILL))
            .background(Palette.surface),
    ) {
        Box(Modifier.fillMaxWidth(level.coerceIn(0f, 1f)).fillMaxHeight().background(Palette.white))
    }
}

private const val REFRESH_MS = 3_000L
private const val BRIGHTNESS_STEP = 0.05f
