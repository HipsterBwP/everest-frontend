package io.github.hipsterbwp.everest.ui.screens

import io.github.hipsterbwp.everest.ui.Era
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import io.github.hipsterbwp.everest.data.Game
import io.github.hipsterbwp.everest.ui.Hint
import io.github.hipsterbwp.everest.ui.LauncherViewModel
import io.github.hipsterbwp.everest.ui.Pad
import io.github.hipsterbwp.everest.ui.PadInput
import io.github.hipsterbwp.everest.ui.Screen
import io.github.hipsterbwp.everest.ui.theme.Size

private val FILTERS = listOf("All", "Favorites", "Recently played")

/** One system's games. A plays straight away; X opens the options sheet. */
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
    val entries = remember(games, system) { games.map { system to it } }
    val key = "$systemName/$filter"
    val index = (vm.gridIndex[key] ?: 0).coerceIn(0, (games.size - 1).coerceAtLeast(0))
    val focusedGame = games.getOrNull(index)

    fun select(i: Int) {
        vm.gridIndex[key] = i.coerceIn(0, (games.size - 1).coerceAtLeast(0))
    }
    fun play(i: Int) {
        games.getOrNull(i)?.let { vm.play(systemName, it) }
    }
    fun options() {
        focusedGame?.let { vm.push(Screen.Details(systemName, it.id)) }
    }
    fun favorite() {
        focusedGame?.let(vm::toggleFavorite)
    }
    fun setFilter(f: Int) {
        vm.gridFilter[systemName] = f.mod(FILTERS.size)
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

    PadInput { button, repeat ->
        when (button) {
            Pad.A -> { play(index); true }
            Pad.X -> { options(); true }
            Pad.Y -> { favorite(); true }
            Pad.L1 -> { setFilter(filter - 1); true }
            Pad.R1 -> { setFilter(filter + 1); true }
            Pad.L2 -> { jumpLetter(false); true }
            Pad.R2 -> { jumpLetter(true); true }
            Pad.B -> vm.back()
            else -> browserMove(button, repeat, index, games.size, Size.COVER_COLUMNS, ::select)
        }
    }

    val hints = buildList {
        if (focusedGame != null) {
            add(Hint(Pad.A, "Play", onClick = { play(index) }))
            add(Hint(Pad.X, "Options", onClick = ::options))
            add(Hint(Pad.Y, if (vm.stats(focusedGame).favorite) "Unfavorite" else "Favorite", onClick = ::favorite))
        }
        add(Hint(Pad.B, "Back", onClick = { vm.back() }))
        add(Hint(listOf(Pad.L1, Pad.R1), "Filter", onClick = { setFilter(filter + 1) }))
        if (games.size > 1) add(Hint(listOf(Pad.L2, Pad.R2), "Jump letter", trailing = true, onClick = { jumpLetter(true) }))
        randomHint(games.size) { browserMove(Pad.Select, 0, index, games.size, Size.COVER_COLUMNS, ::select) }?.let { add(it) }
    }

    GameBrowser(
        vm = vm,
        entries = entries,
        index = index,
        header = {
            BrowserHeader(vm.logos[systemName], system.info.displayName, system.games.size, FILTERS, filter, ::setFilter)
        },
        emptyTitle = if (filter == 1) "No favorites here yet" else "Nothing played yet",
        emptyBody = if (filter == 1) "Press Y on a game to add it." else "Games you play on this system show up here.",
        hints = hints,
        onSelect = ::select,
        onPlay = ::play,
        era = if (vm.config.eraSkins) Era.forSystem(systemName) else null,
    )
}
