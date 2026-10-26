package io.github.hipsterbwp.rp5launcher.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import io.github.hipsterbwp.rp5launcher.ui.ArtImage
import io.github.hipsterbwp.rp5launcher.ui.Dialog
import io.github.hipsterbwp.rp5launcher.ui.Hint
import io.github.hipsterbwp.rp5launcher.ui.HintBar
import io.github.hipsterbwp.rp5launcher.ui.LauncherViewModel
import io.github.hipsterbwp.rp5launcher.ui.Pad
import io.github.hipsterbwp.rp5launcher.ui.PadInput
import io.github.hipsterbwp.rp5launcher.ui.Pill
import io.github.hipsterbwp.rp5launcher.ui.PlayIcon
import io.github.hipsterbwp.rp5launcher.ui.PrimaryButton
import io.github.hipsterbwp.rp5launcher.ui.Txt
import io.github.hipsterbwp.rp5launcher.ui.theme.Motion
import io.github.hipsterbwp.rp5launcher.ui.theme.Palette
import io.github.hipsterbwp.rp5launcher.ui.theme.Radius
import io.github.hipsterbwp.rp5launcher.ui.theme.Size
import io.github.hipsterbwp.rp5launcher.ui.theme.Space
import io.github.hipsterbwp.rp5launcher.ui.theme.Type
import io.github.hipsterbwp.rp5launcher.ui.theme.rounded
import io.github.hipsterbwp.rp5launcher.ui.theme.u

@Composable
fun GameDetailsScreen(vm: LauncherViewModel, systemName: String, gameId: String) {
    val system = vm.system(systemName)
    val game = vm.game(systemName, gameId)
    if (system == null || game == null) {
        LaunchedEffect(Unit) { vm.back() }
        return
    }
    val stats = vm.stats(game)
    val accent by animateColorAsState(system.color, Motion.colorFade(), label = "details-accent")
    // Keyed on emulatorsVersion so the status refreshes after installs and re-detects.
    val command = remember(vm.emulatorsVersion, vm.config) { vm.selectedCommand(systemName) }
    val installed = remember(vm.emulatorsVersion, command) { command?.let(vm::isInstalled) ?: false }

    fun play() = vm.play(systemName, game)
    fun changeEmulator() {
        vm.dialog = Dialog.EmulatorPicker(systemName)
    }

    PadInput { button, _ ->
        when (button) {
            Pad.A -> { play(); true }
            Pad.X -> { changeEmulator(); true }
            Pad.Y -> { vm.toggleFavorite(game); true }
            Pad.B -> vm.back()
            else -> false
        }
    }

    Box(Modifier.fillMaxSize().background(Palette.page)) {
        Row(Modifier.fillMaxSize()) {
            // Left: system-color panel with the box art (or a placeholder until it's scraped).
            Box(
                Modifier
                    .width(Size.DETAILS_PANEL.u)
                    .fillMaxHeight()
                    .background(accent)
                    .padding(bottom = Size.HINT_BAR.u),
                contentAlignment = Alignment.Center,
            ) {
                val box = vm.boxArt(game)
                if (box != null) {
                    ArtImage(box, Modifier.size(Size.BOX_ART_W.u, Size.BOX_ART_H.u).clip(rounded(Radius.CARD)))
                } else {
                    Box(
                        Modifier
                            .size(Size.BOX_ART_W.u, Size.BOX_ART_H.u)
                            .clip(rounded(Radius.PANEL))
                            .background(Palette.onSystemSurfaceStrong)
                            .padding(Space.SECTION_LARGE.u),
                        contentAlignment = Alignment.Center,
                    ) {
                        val logo = vm.logos[system.name]
                        if (logo != null) ArtImage(logo, Modifier.fillMaxSize(), tint = Palette.white)
                        else Txt(system.info.shortName, Type.pageTitle, Palette.white)
                    }
                }
            }

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(top = Space.SCREEN_TOP_LIGHT.u, start = Space.DETAILS_H.u, end = Space.DETAILS_H.u),
            ) {
                val info = vm.artFor(game)
                Row(horizontalArrangement = Arrangement.spacedBy(Space.GAP.u)) {
                    Pill(system.info.shortName, accent, Palette.white)
                    info?.genre?.let { Pill(it, Palette.surface, Palette.ink) }
                    game.tag?.let { Pill(it, Palette.surface, Palette.ink) }
                    if (stats.favorite) Pill("Favorite", Palette.surface, Palette.ink)
                }
                Spacer(Modifier.height(Space.LG.u))
                Txt(game.title, Type.pageTitle, Palette.ink, maxLines = 2)
                Spacer(Modifier.height(Space.MD.u))
                // "Square · 1995" once scraped; system and file size until then.
                val credits = listOfNotNull(info?.publisher ?: info?.developer, info?.year)
                val sub = credits.ifEmpty { listOf(system.info.displayName, formatSize(game.sizeBytes)).filter { it.isNotEmpty() } }
                Txt(sub.joinToString(" · "), Type.bodySmall, Palette.inkSecondary)
                Spacer(Modifier.height(Space.LG.u))
                val developer = info?.developer?.takeIf { it != info?.publisher }
                Txt(
                    developer?.let { "Developed by $it" } ?: game.fileName,
                    Type.body,
                    Palette.inkBody,
                    Modifier.widthIn(max = Size.DESCRIPTION_MAX_W.u),
                    maxLines = 2,
                )

                Spacer(Modifier.height(Space.SECTION.u))
                PrimaryButton("Play", accent, focused = true, leading = { PlayIcon(Palette.white) }, onClick = ::play)

                Spacer(Modifier.height(Space.SECTION.u))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.SECTION_LARGE.u)) {
                    Stat(
                        if (stats.playtimeMs > 0) formatPlaytime(stats.playtimeMs) else "—",
                        "played",
                    )
                    Stat(stats.playCount.toString(), if (stats.playCount == 1) "launch" else "launches")
                    if (command != null) {
                        Stat(
                            command.shortName,
                            if (installed) command.hostName else "not installed",
                            if (installed) Palette.inkSecondary else Palette.problem,
                        )
                    } else {
                        Stat("None", "no emulator", Palette.problem)
                    }
                }
            }
        }

        HintBar(
            listOf(
                Hint(Pad.A, "Play", onClick = ::play),
                Hint(Pad.B, "Back", onClick = { vm.back() }),
                Hint(Pad.X, "Change emulator", onClick = ::changeEmulator),
                Hint(Pad.Y, if (stats.favorite) "Unfavorite" else "Favorite", onClick = { vm.toggleFavorite(game) }),
            ),
            onSystem = false,
            glyphInk = Palette.ink,
        )
    }
}

@Composable
private fun Stat(value: String, label: String, labelColor: Color = Palette.inkSecondary) {
    Column(Modifier.widthIn(max = Size.STAT_MAX_W.u)) {
        Txt(value, Type.stat, Palette.ink)
        Txt(label, Type.caption, labelColor)
    }
}
