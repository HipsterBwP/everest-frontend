package io.github.hipsterbwp.rp5launcher.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import io.github.hipsterbwp.rp5launcher.data.Game
import io.github.hipsterbwp.rp5launcher.ui.ArtImage
import io.github.hipsterbwp.rp5launcher.ui.Hint
import io.github.hipsterbwp.rp5launcher.ui.HintBar
import io.github.hipsterbwp.rp5launcher.ui.LauncherViewModel
import io.github.hipsterbwp.rp5launcher.ui.ListRow
import io.github.hipsterbwp.rp5launcher.ui.Pad
import io.github.hipsterbwp.rp5launcher.ui.PadInput
import io.github.hipsterbwp.rp5launcher.ui.Screen
import io.github.hipsterbwp.rp5launcher.ui.SystemEntry
import io.github.hipsterbwp.rp5launcher.ui.Txt
import io.github.hipsterbwp.rp5launcher.ui.bringIntoView
import io.github.hipsterbwp.rp5launcher.ui.glass
import io.github.hipsterbwp.rp5launcher.ui.tap
import io.github.hipsterbwp.rp5launcher.ui.theme.Alpha
import io.github.hipsterbwp.rp5launcher.ui.theme.Blur
import io.github.hipsterbwp.rp5launcher.ui.theme.Focus
import io.github.hipsterbwp.rp5launcher.ui.theme.Motion
import io.github.hipsterbwp.rp5launcher.ui.theme.Palette
import io.github.hipsterbwp.rp5launcher.ui.theme.Radius
import io.github.hipsterbwp.rp5launcher.ui.theme.Size
import io.github.hipsterbwp.rp5launcher.ui.theme.Space
import io.github.hipsterbwp.rp5launcher.ui.theme.Type
import io.github.hipsterbwp.rp5launcher.ui.theme.rounded
import io.github.hipsterbwp.rp5launcher.ui.theme.u
import kotlinx.coroutines.delay
import kotlin.random.Random

/** Best art to show for a game, sharpest first. */
private fun LauncherViewModel.anyArt(game: Game) = fanart(game) ?: snapArt(game) ?: boxArt(game)

// ---------------------------------------------------------------------------------------------
// Collections tab: list of collections | cover preview
// ---------------------------------------------------------------------------------------------

@Composable
fun CollectionsTab(
    vm: LauncherViewModel,
    tabs: List<String>,
    tab: Int,
    switchTab: (Int) -> Unit,
    openSettings: () -> Unit,
) {
    val collections = vm.collections()
    val index = (vm.homeGameIndex[tab] ?: 0).coerceIn(0, (collections.size - 1).coerceAtLeast(0))
    val selected = collections.getOrNull(index)
    fun select(i: Int) {
        vm.homeGameIndex[tab] = i.coerceIn(0, (collections.size - 1).coerceAtLeast(0))
    }
    fun open() {
        selected?.let { vm.push(Screen.Collection(it.key)) }
    }

    PadInput { button, _ ->
        when (button) {
            Pad.Up -> { select(index - 1); true }
            Pad.Down -> { select(index + 1); true }
            Pad.A -> { open(); true }
            Pad.L1 -> { switchTab(-1); true }
            Pad.R1 -> { switchTab(1); true }
            Pad.Start -> { openSettings(); true }
            else -> false
        }
    }

    Box(Modifier.fillMaxSize().background(Palette.page)) {
        val backdrop = selected?.entries?.firstNotNullOfOrNull { vm.anyArt(it.second) }
        Crossfade(targetState = backdrop, animationSpec = Motion.colorFade(), label = "collections-bg") { file ->
            ArtImage(file, Modifier.fillMaxSize().blur(Blur.BACKDROP.u), ContentScale.Crop)
        }
        Box(Modifier.fillMaxSize().background(Palette.black.copy(alpha = Alpha.BROWSER_DIM)))

        Row(
            Modifier
                .fillMaxSize()
                .padding(start = Space.SCREEN_H.u, end = Space.SCREEN_H.u, top = (Space.SCREEN_TOP + Size.STATUS_H + Space.LG).u, bottom = Size.HINT_BAR.u),
        ) {
            val state = rememberLazyListState()
            LaunchedEffect(index) { state.bringIntoView(index) }
            val ring = Size.FOCUS_RING_LIGHT + Size.FOCUS_OFFSET
            LazyColumn(
                state = state,
                modifier = Modifier.width(Size.SYSTEM_PANEL_W.u).fillMaxHeight(),
                contentPadding = PaddingValues(ring.u),
                verticalArrangement = Arrangement.spacedBy(Space.LIST_GAP.u),
            ) {
                itemsIndexed(collections, key = { _, c -> c.key }) { i, c ->
                    ListRow(
                        focused = i == index,
                        accent = Palette.white,
                        background = Palette.glassStrong,
                        onClick = { if (i == index) open() else select(i) },
                    ) {
                        Txt(c.name, Type.rowTitle, Palette.ink, Modifier.weight(1f))
                        Txt(c.entries.size.toString(), Type.label, Palette.inkSecondary)
                    }
                }
            }
            Spacer(Modifier.width(Space.SECTION_LARGE.u))
            Column(Modifier.weight(1f).fillMaxHeight()) {
                if (selected == null) {
                    Txt("No collections yet", Type.section, Palette.ink)
                    Spacer(Modifier.height(Space.MD.u))
                    Txt("Add games from a game's Options (X).", Type.body, Palette.inkSecondary)
                } else {
                    Txt(if (selected.custom) "YOUR COLLECTION" else "SMART COLLECTION", Type.eyebrow, Palette.inkSecondary)
                    Spacer(Modifier.height(Space.SM.u))
                    Txt(selected.name, Type.pageTitle, Palette.ink, maxLines = 2)
                    Spacer(Modifier.height(Space.GAP.u))
                    val n = selected.entries.size
                    Txt("$n ${if (n == 1) "game" else "games"}", Type.label, Palette.inkSecondary)
                    Spacer(Modifier.height(Space.SECTION.u))
                    CoverStrip(vm, selected.entries)
                }
            }
        }

        Row(
            Modifier.align(Alignment.TopEnd).padding(top = Space.SCREEN_TOP.u, end = Space.SCREEN_H.u),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.ROW_GAP.u),
        ) {
            TabPills(tabs, tab) { vm.homeTab = it }
            StatusPill()
        }

        val hints = buildList {
            if (selected != null) add(Hint(Pad.A, "Open", onClick = ::open))
            add(Hint(listOf(Pad.L1, Pad.R1), "Switch tab", onClick = { switchTab(1) }))
            add(Hint(Pad.Start, "Settings", trailing = true, onClick = openSettings))
        }
        HintBar(hints, onSystem = true, glyphInk = Palette.inkOnLight)
    }
}

/** A row of the first few covers in a collection. */
@Composable
private fun CoverStrip(vm: LauncherViewModel, entries: List<Pair<SystemEntry, Game>>) {
    Row(horizontalArrangement = Arrangement.spacedBy(Space.TILE_GAP.u)) {
        entries.take(PREVIEW_COVERS).forEach { (_, game) ->
            Box(
                Modifier
                    .width(Size.PREVIEW_COVER_W.u)
                    .height(Size.COVER_H.u)
                    .clip(rounded(Radius.CARD_SMALL))
                    .background(Palette.card),
                contentAlignment = Alignment.BottomStart,
            ) {
                val box = vm.boxArt(game)
                if (box != null) ArtImage(box, Modifier.fillMaxSize(), ContentScale.Crop)
                else Txt(game.title, Type.cardTitle, Palette.ink, Modifier.padding(Space.CARD_PAD.u), maxLines = 4)
            }
        }
    }
}

private const val PREVIEW_COVERS = 3

// ---------------------------------------------------------------------------------------------
// One collection, search results: the game browser
// ---------------------------------------------------------------------------------------------

/** Shared input and hints for game lists that aren't a single system. */
@Composable
private fun GameListScreen(
    vm: LauncherViewModel,
    entries: List<Pair<SystemEntry, Game>>,
    indexKey: String,
    header: @Composable () -> Unit,
    emptyTitle: String,
    emptyBody: String,
    extraHints: List<Hint> = emptyList(),
    onY: (() -> Unit)? = null,
) {
    val index = (vm.gridIndex[indexKey] ?: 0).coerceIn(0, (entries.size - 1).coerceAtLeast(0))
    val focused = entries.getOrNull(index)
    fun select(i: Int) {
        vm.gridIndex[indexKey] = i
    }
    fun play(i: Int) {
        entries.getOrNull(i)?.let { (s, g) -> vm.play(s.name, g) }
    }
    fun options() {
        focused?.let { (s, g) -> vm.push(Screen.Details(s.name, g.id)) }
    }
    fun favorite() {
        focused?.let { vm.toggleFavorite(it.second) }
    }

    PadInput { button, repeat ->
        when (button) {
            Pad.A -> { play(index); true }
            Pad.X -> { options(); true }
            Pad.Y -> { (onY ?: ::favorite)(); true }
            Pad.B -> vm.back()
            else -> browserMove(button, repeat, index, entries.size, Size.COVER_COLUMNS, ::select)
        }
    }

    val hints = buildList {
        if (focused != null) {
            add(Hint(Pad.A, "Play", onClick = { play(index) }))
            add(Hint(Pad.X, "Options", onClick = ::options))
        }
        add(Hint(Pad.B, "Back", onClick = { vm.back() }))
        if (onY == null && focused != null) {
            add(Hint(Pad.Y, if (vm.stats(focused.second).favorite) "Unfavorite" else "Favorite", onClick = ::favorite))
        }
        addAll(extraHints)
        randomHint(entries.size) { browserMove(Pad.Select, 0, index, entries.size, Size.COVER_COLUMNS, ::select) }?.let { add(it) }
    }
    GameBrowser(
        vm = vm,
        entries = entries,
        index = index,
        header = header,
        emptyTitle = emptyTitle,
        emptyBody = emptyBody,
        hints = hints,
        onSelect = ::select,
        onPlay = ::play,
    )
}

@Composable
fun CollectionScreen(vm: LauncherViewModel, key: String) {
    val collection = vm.collection(key)
    if (collection == null) {
        LaunchedEffect(Unit) { vm.back() }
        return
    }
    GameListScreen(
        vm = vm,
        entries = collection.entries,
        indexKey = "collection:$key",
        header = { BrowserHeader(null, collection.name, collection.entries.size, emptyList(), 0) {} },
        emptyTitle = "Nothing here yet",
        emptyBody = "Add games from a game's Options (X).",
    )
}

@Composable
fun SearchScreen(vm: LauncherViewModel) {
    LaunchedEffect(Unit) { if (vm.searchQuery.isBlank()) vm.editSearch() }
    val results = vm.searchResults()
    val query = vm.searchQuery
    GameListScreen(
        vm = vm,
        entries = results,
        indexKey = "search:$query",
        header = {
            BrowserHeader(null, "Search", results.size, listOf(if (query.isBlank()) "Type to search" else "“$query”"), 0) {
                vm.editSearch()
            }
        },
        emptyTitle = if (query.isBlank()) "Search your games" else "No matches",
        emptyBody = if (query.isBlank()) "Press Y to type a name." else "Try fewer or different words. Press Y to edit.",
        extraHints = listOf(Hint(Pad.Y, "Edit search", onClick = vm::editSearch)),
        onY = vm::editSearch,
    )
}

// ---------------------------------------------------------------------------------------------
// Screensaver
// ---------------------------------------------------------------------------------------------

/** Apple-TV-style screensaver: random game art drifting slowly, with its title. Any button wakes. */
@Composable
fun Screensaver(vm: LauncherViewModel) {
    val pool = remember(vm.systems) {
        vm.systems.flatMap { s -> s.games.map { s to it } }.filter { vm.anyArt(it.second) != null }
    }
    var pick by remember { mutableIntStateOf(if (pool.isEmpty()) 0 else Random.nextInt(pool.size)) }
    LaunchedEffect(pool.size) {
        while (pool.size > 1) {
            delay(Motion.SCREENSAVER_SLIDE_MS)
            pick = Random.nextInt(pool.size)
        }
    }
    Box(Modifier.fillMaxSize().background(Palette.black).tap { vm.onInput() }) {
        val entry = pool.getOrNull(pick)
        Crossfade(targetState = entry, animationSpec = tween(Motion.SCREENSAVER_FADE_MS), label = "screensaver") { current ->
            if (current != null) {
                val zoom = remember(current) { Animatable(1f) }
                LaunchedEffect(current) {
                    zoom.animateTo(Focus.KEN_BURNS_SCALE, tween(Motion.SCREENSAVER_SLIDE_MS.toInt() + Motion.SCREENSAVER_FADE_MS, easing = LinearEasing))
                }
                Box(Modifier.fillMaxSize()) {
                    ArtImage(
                        vm.anyArt(current.second),
                        Modifier.fillMaxSize().graphicsLayer { scaleX = zoom.value; scaleY = zoom.value },
                        ContentScale.Crop,
                    )
                    Column(
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(Space.SCREEN_H.u)
                            .glass(Radius.CARD)
                            .padding(Space.LG.u),
                    ) {
                        val (system, game) = current
                        val line = listOfNotNull(system.info.displayName, vm.artFor(game)?.year).joinToString(" · ").uppercase()
                        Txt(line, Type.eyebrow, Palette.inkSecondary)
                        Spacer(Modifier.height(Space.SM.u))
                        Txt(game.title, Type.screenTitle, Palette.ink, Modifier.width(Size.BROWSER_INFO_W.u))
                    }
                }
            }
        }
        if (entry == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Txt("ARCROW", Type.hero, Palette.inkSecondary)
            }
        }
    }
}
