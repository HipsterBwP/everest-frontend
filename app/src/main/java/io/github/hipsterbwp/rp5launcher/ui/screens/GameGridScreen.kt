package io.github.hipsterbwp.rp5launcher.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import io.github.hipsterbwp.rp5launcher.ui.ArtImage
import io.github.hipsterbwp.rp5launcher.ui.GameplayBackdrop
import java.io.File
import io.github.hipsterbwp.rp5launcher.data.Game
import io.github.hipsterbwp.rp5launcher.data.GameStats
import io.github.hipsterbwp.rp5launcher.ui.Hint
import io.github.hipsterbwp.rp5launcher.ui.HintBar
import io.github.hipsterbwp.rp5launcher.ui.LauncherViewModel
import io.github.hipsterbwp.rp5launcher.ui.Pad
import io.github.hipsterbwp.rp5launcher.ui.PadInput
import io.github.hipsterbwp.rp5launcher.ui.Pill
import io.github.hipsterbwp.rp5launcher.ui.Screen
import io.github.hipsterbwp.rp5launcher.ui.Txt
import io.github.hipsterbwp.rp5launcher.ui.accelerated
import io.github.hipsterbwp.rp5launcher.ui.bringIntoView
import io.github.hipsterbwp.rp5launcher.ui.focusRing
import io.github.hipsterbwp.rp5launcher.ui.tap
import io.github.hipsterbwp.rp5launcher.ui.theme.Alpha
import io.github.hipsterbwp.rp5launcher.ui.theme.Focus
import io.github.hipsterbwp.rp5launcher.ui.theme.Motion
import io.github.hipsterbwp.rp5launcher.ui.theme.Palette
import io.github.hipsterbwp.rp5launcher.ui.theme.Radius
import io.github.hipsterbwp.rp5launcher.ui.theme.Size
import io.github.hipsterbwp.rp5launcher.ui.theme.Space
import io.github.hipsterbwp.rp5launcher.ui.theme.Type
import io.github.hipsterbwp.rp5launcher.ui.theme.rounded
import io.github.hipsterbwp.rp5launcher.ui.theme.u

private val FILTERS = listOf("All", "Favorites", "Recently played")

@Composable
fun GameGridScreen(vm: LauncherViewModel, systemName: String) {
    val system = vm.system(systemName)
    if (system == null) {
        // The system vanished after a rescan; go back rather than show an empty page.
        LaunchedEffect(Unit) { vm.back() }
        return
    }
    val filter = vm.gridFilter[systemName] ?: 0
    val allStats = vm.stats
    val games: List<Game> = remember(system.games, filter, allStats) {
        when (filter) {
            1 -> system.games.filter { vm.stats(it).favorite }
            2 -> system.games.filter { vm.stats(it).lastPlayed > 0 }.sortedByDescending { vm.stats(it).lastPlayed }
            else -> system.games
        }
    }
    val key = "$systemName/$filter"
    val index = (vm.gridIndex[key] ?: 0).coerceIn(0, (games.size - 1).coerceAtLeast(0))
    val bg by animateColorAsState(system.color, Motion.colorFade(), label = "grid-bg")
    val columns = Size.GRID_COLUMNS
    val focusedGame = games.getOrNull(index)

    fun select(i: Int) {
        vm.gridIndex[key] = i.coerceIn(0, (games.size - 1).coerceAtLeast(0))
    }
    fun jumpLetter(forward: Boolean) {
        if (games.isEmpty()) return
        fun letter(g: Game) = g.title.firstOrNull()?.uppercaseChar() ?: ' '
        val current = letter(games[index])
        val target = if (forward) {
            (index + 1 until games.size).firstOrNull { letter(games[it]) != current }
        } else {
            // Start of the previous letter group (or of this one, if not already there).
            val groupStart = (index downTo 0).last { letter(games[it]) == current }
            if (groupStart < index) groupStart
            else (groupStart - 1).takeIf { it >= 0 }?.let { prev ->
                val prevLetter = letter(games[prev])
                (prev downTo 0).last { letter(games[it]) == prevLetter }
            }
        }
        target?.let(::select)
    }
    fun openDetails() = focusedGame?.let { vm.push(Screen.Details(systemName, it.id)) }
    fun favorite() = focusedGame?.let(vm::toggleFavorite)
    fun setFilter(delta: Int) {
        vm.gridFilter[systemName] = (filter + delta).mod(FILTERS.size)
    }

    PadInput { button, repeat ->
        when (button) {
            Pad.Left -> { select(index - 1); true }
            Pad.Right -> { select(index + 1); true }
            Pad.Up -> {
                val rows = minOf(accelerated(repeat), index / columns)
                if (rows > 0) select(index - columns * rows)
                true
            }
            Pad.Down -> {
                val rows = minOf(accelerated(repeat), (games.size - 1) / columns - index / columns)
                if (rows > 0) select(index + columns * rows)
                true
            }
            Pad.L1 -> { setFilter(-1); true }
            Pad.R1 -> { setFilter(1); true }
            Pad.L2 -> { jumpLetter(false); true }
            Pad.R2 -> { jumpLetter(true); true }
            Pad.A -> { openDetails(); true }
            Pad.Y -> { favorite(); true }
            Pad.B -> vm.back()
            else -> false
        }
    }

    Box(Modifier.fillMaxSize().background(bg)) {
        // Gameplay backdrop: the focused game's screenshot under a translucent layer of system color.
        GameplayBackdrop(focusedGame?.let(vm::snapArt), bg, Alpha.BACKDROP_SCRIM)
        Column(Modifier.fillMaxSize().padding(top = Space.SCREEN_TOP.u)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Space.SCREEN_H.u),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(Space.TILE_GAP.u),
            ) {
                Txt(system.info.displayName, Type.section, Palette.white, Modifier.weight(1f))
                val n = system.games.size
                Txt("$n ${if (n == 1) "game" else "games"}", Type.tab, Palette.white.copy(alpha = Alpha.SECONDARY_ON_SYSTEM))
            }
            Row(
                Modifier.padding(horizontal = Space.SCREEN_H.u).padding(top = Space.TILE_GAP.u),
                horizontalArrangement = Arrangement.spacedBy(Space.GAP.u),
            ) {
                FILTERS.forEachIndexed { i, name ->
                    val selected = i == filter
                    Pill(
                        name,
                        if (selected) Palette.white else Palette.onSystemSurfaceStrong,
                        if (selected) bg else Palette.white,
                        Type.label,
                        Space.PILL_V,
                        Space.PILL_H_SMALL,
                        Modifier.tap { vm.gridFilter[systemName] = i },
                    )
                }
            }

            if (games.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = Space.SCREEN_H.u), Alignment.CenterStart) {
                    Txt(
                        if (filter == 1) "No favorites here yet. Press Y on a game to add it."
                        else "Nothing played on this system yet.",
                        Type.body, Palette.white,
                    )
                }
            } else {
                val state = rememberLazyGridState()
                LaunchedEffect(index, filter) { state.bringIntoView(index) }
                val ring = Size.FOCUS_RING_CARD + Size.FOCUS_OFFSET
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    state = state,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = Space.SCREEN_H.u,
                        end = Space.SCREEN_H.u,
                        top = (Space.SECTION - ring).u,
                        bottom = (Size.HINT_BAR + Space.SECTION_SMALL).u,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(Space.TILE_GAP.u),
                    verticalArrangement = Arrangement.spacedBy(Space.TILE_GAP.u),
                ) {
                    itemsIndexed(games, key = { _, g -> g.id }) { i, game ->
                        GameCard(
                            game = game,
                            stats = vm.stats(game),
                            year = vm.artFor(game)?.year,
                            box = vm.boxArt(game),
                            focused = i == index,
                            bg = bg,
                            modifier = Modifier.tap { if (i == index) openDetails() else select(i) },
                        )
                    }
                }
            }
        }

        val hints = buildList {
            if (focusedGame != null) add(Hint(Pad.A, "Details", onClick = { openDetails() }))
            add(Hint(Pad.B, "Back", onClick = { vm.back() }))
            if (focusedGame != null) {
                add(Hint(Pad.Y, if (vm.stats(focusedGame).favorite) "Unfavorite" else "Favorite", onClick = { favorite() }))
            }
            add(Hint(listOf(Pad.L1, Pad.R1), "Filter", onClick = { setFilter(1) }))
            if (games.size > 1) add(Hint(listOf(Pad.L2, Pad.R2), "Jump letter", trailing = true, onClick = { jumpLetter(true) }))
        }
        HintBar(hints, onSystem = true, glyphInk = bg)
    }
}

@Composable
private fun GameCard(
    game: Game,
    stats: GameStats,
    year: String?,
    box: File?,
    focused: Boolean,
    bg: Color,
    modifier: Modifier,
) {
    val progress by animateFloatAsState(if (focused) 1f else 0f, Motion.focus(), label = "card-focus")
    val scale = 1f + (Focus.CARD_SCALE - 1f) * progress
    val meta = buildList {
        add(year ?: game.tag ?: game.extension.uppercase())
        if (focused && stats.playtimeMs > 0) add("${formatPlaytime(stats.playtimeMs)} played")
        if (stats.favorite) add("★")
    }.joinToString(" · ")
    val ink = if (focused) bg else Palette.white
    Column(
        modifier
            .zIndex(if (focused) 1f else 0f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .focusRing(progress, Palette.focusRingOnSystemCard, Size.FOCUS_RING_CARD.u, Size.FOCUS_OFFSET.u, Radius.CARD.u)
            .height(Size.GRID_CARD_H.u)
            .fillMaxWidth()
            .clip(rounded(Radius.CARD))
            .background(if (focused) Palette.white else Palette.onSystemSurfaceWeak)
            .padding(if (box != null) Space.GAP.u else Space.CARD_PAD.u),
        verticalArrangement = Arrangement.Bottom,
    ) {
        if (box != null) {
            // Cover fills the card above a one-line title.
            ArtImage(box, Modifier.weight(1f).fillMaxWidth())
            Spacer(Modifier.height(Space.LIST_GAP.u))
            Txt(game.title, Type.cardTitle, ink)
        } else {
            Txt(game.title, Type.cardTitle, ink, maxLines = 4)
        }
        Spacer(Modifier.height(Space.XS.u))
        Txt(meta, Type.micro, if (focused) ink else Palette.white.copy(alpha = Alpha.CARD_META))
    }
}
