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
import io.github.hipsterbwp.rp5launcher.ui.ListRow
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.hipsterbwp.rp5launcher.ui.theme.Blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import io.github.hipsterbwp.rp5launcher.ui.ArtImage
import io.github.hipsterbwp.rp5launcher.ui.Dialog
import io.github.hipsterbwp.rp5launcher.ui.GameplayBackdrop
import io.github.hipsterbwp.rp5launcher.ui.Hint
import io.github.hipsterbwp.rp5launcher.ui.HintBar
import io.github.hipsterbwp.rp5launcher.ui.LauncherViewModel
import io.github.hipsterbwp.rp5launcher.ui.Pad
import io.github.hipsterbwp.rp5launcher.ui.PadInput
import io.github.hipsterbwp.rp5launcher.ui.Pill
import io.github.hipsterbwp.rp5launcher.ui.PlayIcon
import io.github.hipsterbwp.rp5launcher.ui.PrimaryButton
import io.github.hipsterbwp.rp5launcher.ui.Txt
import io.github.hipsterbwp.rp5launcher.ui.theme.Alpha
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
    val command = remember(vm.emulatorsVersion, vm.config, stats) { vm.selectedCommand(systemName, game) }
    val systemCommand = remember(vm.emulatorsVersion, vm.config) { vm.selectedCommand(systemName) }
    val installed = remember(vm.emulatorsVersion, command) { command?.let(vm::isInstalled) ?: false }
    val inCollections = vm.customCollectionNames().count { vm.isInCollection(it, game) }

    fun play() = vm.play(systemName, game)

    // The action list (Apple-style grouped rows). Each: title, value on the right, action.
    val actions = listOf(
        OptionRow("Play", null, primary = true) { play() },
        OptionRow(if (stats.favorite) "Remove from Favorites" else "Add to Favorites", null) { vm.toggleFavorite(game) },
        OptionRow("Emulator for this game", stats.emulator?.let { command?.shortName } ?: "Same as system") {
            vm.dialog = Dialog.EmulatorPicker(systemName, game.id)
        },
        OptionRow("Emulator for all ${system.info.displayName} games", systemCommand?.shortName ?: "None") {
            vm.dialog = Dialog.EmulatorPicker(systemName)
        },
        OptionRow("Add to collection…", if (inCollections > 0) "In $inCollections" else null) {
            vm.dialog = Dialog.CollectionPicker(game.id)
        },
        OptionRow("Fix artwork match…", vm.artFor(game)?.matched) { vm.fixMatch(systemName, game) },
        OptionRow("Hide this game", "Unhide in Settings › Library") { vm.hideGame(game) },
    )
    var row by rememberSaveable(gameId) { mutableIntStateOf(0) }
    val current = row.coerceIn(0, actions.size - 1)

    PadInput { button, _ ->
        when (button) {
            Pad.Up -> { row = (current - 1).coerceAtLeast(0); true }
            Pad.Down -> { row = (current + 1).coerceAtMost(actions.size - 1); true }
            Pad.A -> { actions[current].action(); true }
            Pad.B -> vm.back()
            else -> false
        }
    }

    Box(Modifier.fillMaxSize().background(Palette.page)) {
        ArtImage(vm.fanart(game) ?: vm.snapArt(game), Modifier.fillMaxSize().blur(Blur.BACKDROP.u), ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Palette.black.copy(alpha = Alpha.BROWSER_DIM)))
        Row(Modifier.fillMaxSize()) {
            // Left: system-color panel with the box art (or a placeholder until it's scraped).
            Box(
                Modifier
                    .width(Size.DETAILS_PANEL.u)
                    .fillMaxHeight()
                    .background(accent),
                contentAlignment = Alignment.Center,
            ) {
                GameplayBackdrop(vm.snapArt(game), accent, Alpha.PANEL_BACKDROP_SCRIM)
                val box = vm.boxArt(game)
                if (box != null) {
                    ArtImage(
                        box,
                        Modifier
                            .padding(bottom = Size.HINT_BAR.u)
                            .size(Size.BOX_ART_W.u, Size.BOX_ART_H.u)
                            .clip(rounded(Radius.CARD)),
                    )
                } else {
                    Box(
                        Modifier
                            .padding(bottom = Size.HINT_BAR.u)
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
                Spacer(Modifier.height(Space.ROW_GAP.u))
                Txt(game.title, Type.section, Palette.ink, maxLines = 1)
                Spacer(Modifier.height(Space.SM.u))
                // "Square · 1995" once scraped; system and file size until then.
                val credits = listOfNotNull(info?.publisher ?: info?.developer, info?.year)
                val sub = credits.ifEmpty { listOf(system.info.displayName, formatSize(game.sizeBytes)).filter { it.isNotEmpty() } }
                val played = if (stats.playtimeMs > 0) "${formatPlaytime(stats.playtimeMs)} played" else "Not played yet"
                val emulatorNote = when {
                    command == null -> "no emulator"
                    installed -> command.shortName
                    else -> "${command.shortName} not installed"
                }
                Txt((sub + played + emulatorNote).joinToString(" · "), Type.label, if (installed) Palette.inkSecondary else Palette.problem)
                Spacer(Modifier.height(Space.SECTION_SMALL.u))
                Column(verticalArrangement = Arrangement.spacedBy(Space.LIST_GAP.u)) {
                    actions.forEachIndexed { i, option ->
                        ListRow(
                            focused = i == current,
                            accent = Palette.white,
                            background = if (option.primary) accent else Palette.glassStrong,
                            padV = Space.ROW_V_SMALL,
                            onClick = { if (i == current) option.action() else row = i },
                        ) {
                            if (option.primary) PlayIcon(Palette.white)
                            Txt(option.title, Type.rowTitle, Palette.ink, Modifier.weight(1f))
                            option.value?.let { Txt(it, Type.label, Palette.inkSecondary, Modifier.widthIn(max = Size.STAT_MAX_W.u)) }
                        }
                    }
                }
            }
        }

        HintBar(
            listOf(
                Hint(Pad.A, "Select", onClick = { actions[current].action() }),
                Hint(Pad.B, "Back", onClick = { vm.back() }),
            ),
            onSystem = false,
            glyphInk = Palette.inkOnLight,
        )
    }
}

/** One action on the Options page. */
private class OptionRow(val title: String, val value: String?, val primary: Boolean = false, val action: () -> Unit)
