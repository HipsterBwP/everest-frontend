package io.github.hipsterbwp.rp5launcher.ui.screens

import io.github.hipsterbwp.rp5launcher.ui.theme.Retro
import io.github.hipsterbwp.rp5launcher.ui.tap
import io.github.hipsterbwp.rp5launcher.ui.neonPanel
import io.github.hipsterbwp.rp5launcher.ui.neonGlow
import io.github.hipsterbwp.rp5launcher.ui.ChromeTxt
import androidx.compose.foundation.border
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.animateColorAsState
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
import io.github.hipsterbwp.rp5launcher.data.Progress
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
        OptionRow("Progress", Progress.label(stats.status)) { vm.cycleStatus(game) },
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
        ArtImage(vm.fanart(game) ?: vm.snapArt(game), Modifier.fillMaxSize().blur(Blur.BACKDROP.u).alpha(BACKDROP_ALPHA), ContentScale.Crop)
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(listOf(Palette.violet.copy(alpha = 0.55f), Palette.night.copy(alpha = 0.95f))),
            ),
        )
        Row(
            Modifier.fillMaxSize().padding(bottom = Size.HINT_BAR.u),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left: the box art, turned slightly towards the menu, in a cyan glow.
            Box(Modifier.width(Size.DETAILS_PANEL.u).fillMaxHeight(), contentAlignment = Alignment.Center) {
                val box = vm.boxArt(game)
                val radius = Radius.CARD.u
                val edge = Size.NEON_EDGE.u
                val art = Modifier
                    .size(Size.BOX_ART_W.u, Size.BOX_ART_H.u)
                    .graphicsLayer { rotationY = BOX_TURN; cameraDistance = BOX_CAMERA * density }
                if (box != null) {
                    ArtImage(box, art.drawBehind { neonGlow(Palette.cyan, CornerRadius(radius.toPx()), edge.toPx()) }.clip(rounded(Radius.CARD)))
                } else {
                    Box(art.neonPanel(Radius.CARD, edge = Palette.cyan, corner = Palette.pink).padding(Space.SECTION_LARGE.u), contentAlignment = Alignment.Center) {
                        val logo = vm.logos[system.name]
                        if (logo != null) ArtImage(logo, Modifier.fillMaxSize(), tint = Palette.white)
                        else ChromeTxt(system.info.shortName, Type.pageTitle)
                    }
                }
            }

            // Right: an arcade "press start" menu.
            Column(
                Modifier
                    .weight(1f)
                    .padding(top = Space.SCREEN_TOP_LIGHT.u, end = Space.DETAILS_H.u, bottom = Space.SECTION.u)
                    .neonPanel(Radius.PANEL)
                    .padding(horizontal = Space.SECTION_LARGE.u, vertical = Space.SECTION.u),
            ) {
                val info = vm.artFor(game)
                Txt("— 1 PLAYER START —", Type.hint, Palette.sun1, glow = Palette.sun2)
                Spacer(Modifier.height(Space.MD.u))
                ChromeTxt(game.title, Type.section)
                Spacer(Modifier.height(Space.GAP.u))
                // "Square · 1995" once scraped; system and file size until then.
                val credits = listOfNotNull(info?.publisher ?: info?.developer, info?.year)
                val sub = credits.ifEmpty { listOf(system.info.displayName, formatSize(game.sizeBytes)).filter { it.isNotEmpty() } }
                val played = if (stats.playtimeMs > 0) "${formatPlaytime(stats.playtimeMs)} played" else "Not played yet"
                val emulatorNote = when {
                    command == null -> "no emulator"
                    installed -> command.shortName
                    else -> "${command.shortName} not installed"
                }
                Row {
                    Txt((sub + emulatorNote).joinToString(" · ") + " · ", Type.label, if (installed) Palette.inkSecondary else Palette.problem)
                    Txt(played, Type.labelStrong, Palette.sun1)
                }
                Spacer(Modifier.height(Space.MD.u))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.GAP.u)) {
                    vm.raProgress[game.id]?.takeIf { it.total > 0 }?.let {
                        Pill("🏆 ${it.awarded}/${it.total}", Palette.cyan.copy(alpha = CHIP_FILL), if (it.awarded == it.total) Palette.ok else Palette.cyanSoft)
                    }
                    stats.status?.let { Pill(Progress.label(it), Palette.ok.copy(alpha = CHIP_FILL), Palette.ok) }
                    info?.genre?.let { Pill(it, Palette.cyan.copy(alpha = CHIP_FILL), Palette.cyanSoft) }
                    game.tag?.let { Pill(it, Palette.cyan.copy(alpha = CHIP_FILL), Palette.cyanSoft) }
                    if (stats.favorite) Pill("★ Favorite", Palette.sun1.copy(alpha = CHIP_FILL), Palette.sun1)
                }
                Spacer(Modifier.height(Space.ROW_GAP.u))
                Column(verticalArrangement = Arrangement.spacedBy(Space.SM.u)) {
                    actions.forEachIndexed { i, option ->
                        MenuEntry(option, focused = i == current) { if (i == current) option.action() else row = i }
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

/** A menu line: pixel-font title, value on the right; focused gets the pink bar and ▶ cursor. */
@Composable
private fun MenuEntry(option: OptionRow, focused: Boolean, onClick: () -> Unit) {
    val shape = rounded(Radius.CARD_SMALL)
    val edge = Size.NEON_EDGE.u
    val radius = Radius.CARD_SMALL.u
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (focused) Modifier.drawBehind { neonGlow(Palette.pink, CornerRadius(radius.toPx()), edge.toPx() / 2) } else Modifier)
            .clip(shape)
            .then(if (focused) Modifier.background(Retro.focusFill).border(edge, Palette.pink, shape) else Modifier)
            .tap(onClick)
            .padding(vertical = Space.ROW_V_SMALL.u, horizontal = Space.ROW_H.u),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(Size.CURSOR_W.u * 2)) {
            if (focused) Txt("▶", Type.menu, Palette.sun1, glow = Palette.sun2)
        }
        Txt(
            option.title.uppercase(),
            if (option.primary) Type.screenTitleSmall else Type.menu,
            if (focused) Palette.white else Palette.inkSecondary,
            Modifier.weight(1f),
        )
        option.value?.let { Txt(it, Type.labelStrong, Palette.inkFaint, Modifier.widthIn(max = Size.STAT_MAX_W.u)) }
    }
}

private const val BACKDROP_ALPHA = 0.5f
private const val CHIP_FILL = 0.12f
/** Box art turns this many degrees towards the menu. */
private const val BOX_TURN = 12f
private const val BOX_CAMERA = 12f
