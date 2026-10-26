package io.github.hipsterbwp.rp5launcher.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import io.github.hipsterbwp.rp5launcher.ui.ArtImage
import java.io.File
import io.github.hipsterbwp.rp5launcher.data.Game
import io.github.hipsterbwp.rp5launcher.ui.Hint
import io.github.hipsterbwp.rp5launcher.ui.HintBar
import io.github.hipsterbwp.rp5launcher.ui.LauncherViewModel
import io.github.hipsterbwp.rp5launcher.ui.Pad
import io.github.hipsterbwp.rp5launcher.ui.PadInput
import io.github.hipsterbwp.rp5launcher.ui.Pill
import io.github.hipsterbwp.rp5launcher.ui.Screen
import io.github.hipsterbwp.rp5launcher.ui.SystemEntry
import io.github.hipsterbwp.rp5launcher.ui.Txt
import io.github.hipsterbwp.rp5launcher.ui.bringIntoView
import io.github.hipsterbwp.rp5launcher.ui.focusRing
import io.github.hipsterbwp.rp5launcher.ui.tap
import io.github.hipsterbwp.rp5launcher.ui.theme.Alpha
import io.github.hipsterbwp.rp5launcher.ui.theme.Motion
import io.github.hipsterbwp.rp5launcher.ui.theme.Palette
import io.github.hipsterbwp.rp5launcher.ui.theme.Radius
import io.github.hipsterbwp.rp5launcher.ui.theme.Size
import io.github.hipsterbwp.rp5launcher.ui.theme.Space
import io.github.hipsterbwp.rp5launcher.ui.theme.SystemColors
import io.github.hipsterbwp.rp5launcher.ui.theme.Type
import io.github.hipsterbwp.rp5launcher.ui.theme.rounded
import io.github.hipsterbwp.rp5launcher.ui.theme.u

private val TABS = listOf("Systems", "Favorites", "Recent", "Collections")
private const val TAB_SYSTEMS = 0
private const val TAB_FAVORITES = 1
private const val TAB_RECENT = 2

@Composable
fun HomeScreen(vm: LauncherViewModel, appName: String) {
    val tab = vm.homeTab
    val systems = vm.systems
    val games: List<Pair<SystemEntry, Game>> = when (tab) {
        TAB_FAVORITES -> vm.favorites()
        TAB_RECENT -> vm.recent()
        else -> emptyList()
    }
    val count = if (tab == TAB_SYSTEMS) systems.size else games.size
    val index = (if (tab == TAB_SYSTEMS) vm.homeSystemIndex else vm.homeGameIndex[tab] ?: 0)
        .coerceIn(0, (count - 1).coerceAtLeast(0))
    val selectedSystem = systems.getOrNull(index)?.takeIf { tab == TAB_SYSTEMS }
    val selectedGame = games.getOrNull(index)

    val target = when (tab) {
        TAB_SYSTEMS -> selectedSystem?.color
        else -> selectedGame?.first?.color
    } ?: SystemColors.neutral
    val bg by animateColorAsState(target, Motion.colorFade(), label = "home-bg")

    fun select(i: Int) {
        if (tab == TAB_SYSTEMS) vm.homeSystemIndex = i else vm.homeGameIndex[tab] = i
    }
    fun open() {
        if (tab == TAB_SYSTEMS) selectedSystem?.let { vm.push(Screen.Games(it.name)) }
        else selectedGame?.let { (s, g) -> vm.push(Screen.Details(s.name, g.id)) }
    }
    fun switchTab(delta: Int) {
        vm.homeTab = (tab + delta).mod(TABS.size)
    }
    fun openSettings() = vm.push(Screen.Settings)

    PadInput { button, _ ->
        when (button) {
            Pad.Left -> { if (index > 0) select(index - 1); true }
            Pad.Right -> { if (index < count - 1) select(index + 1); true }
            Pad.L1 -> { switchTab(-1); true }
            Pad.R1 -> { switchTab(1); true }
            Pad.A -> { open(); true }
            Pad.Start -> { openSettings(); true }
            else -> false
        }
    }

    Box(Modifier.fillMaxSize().background(bg)) {
        Column(Modifier.fillMaxSize().padding(top = Space.SCREEN_TOP.u)) {
            // Top row: brand + tabs
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Space.SCREEN_H.u),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.ROW_GAP.u)) {
                    Txt(appName, Type.brand, Palette.white)
                    vm.scrape?.let { p ->
                        Txt("Getting art ${p.done}/${p.total}", Type.label, Palette.white.copy(alpha = Alpha.SECONDARY_ON_SYSTEM))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Space.GAP.u)) {
                    TABS.forEachIndexed { i, name ->
                        val selected = i == tab
                        Pill(
                            text = name,
                            background = if (selected) Palette.white else Palette.onSystemSurfaceStrong,
                            ink = if (selected) bg else Palette.white,
                            style = Type.tab,
                            padV = Space.PILL_V,
                            padH = Space.PILL_H,
                            modifier = Modifier.tap { vm.homeTab = i },
                        )
                    }
                }
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    tab == TAB_SYSTEMS && selectedSystem != null ->
                        SystemsTab(vm, systems, index, selectedSystem, bg, ::select, ::open)
                    tab == TAB_FAVORITES || tab == TAB_RECENT ->
                        if (selectedGame != null) GamesTab(vm, games, index, bg, ::select, ::open)
                        else EmptyState(
                            if (tab == TAB_FAVORITES) "No favorites yet" else "Nothing played yet",
                            if (tab == TAB_FAVORITES) "Press Y on any game to add it here."
                            else "Games you play show up here, newest first.",
                        )
                    tab == TAB_SYSTEMS -> EmptyState(
                        if (vm.scan != null) "Looking for games…" else "No games found",
                        if (vm.scan != null) "Scanning ${vm.rootLabel().orEmpty()}"
                        else "Put games in system folders like snes, gba or ps2 inside ${vm.rootLabel().orEmpty()}, " +
                            "then rescan from Settings › Library.",
                    )
                    else -> EmptyState("Collections", "Collections arrive in a later version.")
                }
            }
            Spacer(Modifier.height(Size.HINT_BAR.u))
        }

        val hints = buildList {
            if (count > 0) add(Hint(Pad.A, "Open", onClick = ::open))
            add(Hint(listOf(Pad.L1, Pad.R1), "Switch tab", onClick = { switchTab(1) }))
            add(Hint(Pad.Start, "Settings", trailing = true, onClick = ::openSettings))
        }
        HintBar(hints, onSystem = true, glyphInk = bg)
    }
}

@Composable
private fun SystemsTab(
    vm: LauncherViewModel,
    systems: List<SystemEntry>,
    index: Int,
    selected: SystemEntry,
    bg: Color,
    select: (Int) -> Unit,
    open: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Space.SCREEN_H.u).padding(top = Space.SECTION_LARGE.u),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.SECTION_LARGE.u),
        ) {
            Column(Modifier.weight(1f)) {
                val info = selected.info
                val maker = listOfNotNull(info.maker, info.year?.toString()).joinToString(" · ")
                if (maker.isNotEmpty()) {
                    Txt(maker, Type.tab, Palette.white.copy(alpha = Alpha.SECONDARY_ON_SYSTEM))
                    Spacer(Modifier.height(Space.GAP.u))
                }
                Txt(info.displayName, Type.hero, Palette.white, maxLines = 2)
                Spacer(Modifier.height(Space.SECTION_SMALL.u))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.GAP.u)) {
                    val n = selected.games.size
                    Pill("$n ${if (n == 1) "game" else "games"}", Palette.onSystemDarkChip, Palette.white, Type.label, Space.CHIP_V)
                    vm.lastPlayed(selected)?.let {
                        Pill("Last: ${it.title}", Palette.onSystemDarkChip, Palette.white, Type.label, Space.CHIP_V)
                    }
                }
            }
            // System artwork: the console logo, or its short name until the logo has downloaded.
            Box(
                Modifier
                    .size(Size.ARTWORK_W.u, Size.ARTWORK_H.u)
                    .clip(rounded(Radius.PANEL_LARGE))
                    .background(Palette.onSystemSurface)
                    .padding(Space.SECTION_LARGE.u),
                contentAlignment = Alignment.Center,
            ) {
                val logo = vm.logos[selected.name]
                if (logo != null) ArtImage(logo, Modifier.fillMaxSize(), tint = Palette.white)
                else Txt(selected.info.shortName, Type.hero, Palette.white)
            }
        }
        Spacer(Modifier.weight(1f))
        Carousel(systems.size, index, select, open) { i, focused ->
            SystemTile(systems[i], vm.logos[systems[i].name], focused, bg)
        }
    }
}

@Composable
private fun GamesTab(
    vm: LauncherViewModel,
    games: List<Pair<SystemEntry, Game>>,
    index: Int,
    bg: Color,
    select: (Int) -> Unit,
    open: () -> Unit,
) {
    val (system, game) = games[index]
    val stats = vm.stats(game)
    val info = vm.artFor(game)
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Space.SCREEN_H.u).padding(top = Space.SECTION_LARGE.u),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.SECTION_LARGE.u),
        ) {
            Column(Modifier.weight(1f)) {
                val line = listOfNotNull(system.info.displayName, info?.year).joinToString(" · ")
                Txt(line, Type.tab, Palette.white.copy(alpha = Alpha.SECONDARY_ON_SYSTEM))
                Spacer(Modifier.height(Space.GAP.u))
                Txt(game.title, Type.pageTitle, Palette.white, maxLines = 2)
                Spacer(Modifier.height(Space.SECTION_SMALL.u))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.GAP.u)) {
                    if (stats.playtimeMs > 0) {
                        Pill("${formatPlaytime(stats.playtimeMs)} played", Palette.onSystemDarkChip, Palette.white, Type.label, Space.CHIP_V)
                    }
                    if (stats.playCount > 0) {
                        Pill(timesPlayed(stats.playCount), Palette.onSystemDarkChip, Palette.white, Type.label, Space.CHIP_V)
                    }
                }
            }
            // Screenshot of the selected game, or its box art when there's no screenshot.
            Box(
                Modifier
                    .size(Size.ARTWORK_W.u, Size.ARTWORK_H.u)
                    .clip(rounded(Radius.PANEL_LARGE))
                    .background(Palette.onSystemSurface),
                contentAlignment = Alignment.Center,
            ) {
                val snap = vm.snapArt(game)
                if (snap != null) ArtImage(snap, Modifier.fillMaxSize(), ContentScale.Crop)
                else ArtImage(vm.boxArt(game), Modifier.fillMaxSize().padding(Space.CARD_PAD.u))
            }
        }
        Spacer(Modifier.weight(1f))
        Carousel(games.size, index, select, open) { i, focused ->
            GameTile(games[i].second.title, vm.boxArt(games[i].second), games[i].first.color, focused, bg)
        }
    }
}

/** Bottom row of tiles; the selected one grows and turns white. */
@Composable
private fun Carousel(
    count: Int,
    index: Int,
    select: (Int) -> Unit,
    open: () -> Unit,
    tile: @Composable (Int, Boolean) -> Unit,
) {
    val state = rememberLazyListState()
    LaunchedEffect(index) { state.bringIntoView(index) }
    val ring = Size.FOCUS_RING + Size.FOCUS_OFFSET
    LazyRow(
        state = state,
        modifier = Modifier.fillMaxWidth().padding(bottom = (Space.SECTION_SMALL - ring).coerceAtLeast(0).u),
        contentPadding = PaddingValues(horizontal = Space.SCREEN_H.u, vertical = ring.u),
        horizontalArrangement = Arrangement.spacedBy(Space.TILE_GAP.u),
        verticalAlignment = Alignment.Bottom,
    ) {
        itemsIndexed(List(count) { it }) { i, _ ->
            Box(Modifier.tap { if (i == index) open() else select(i) }) {
                tile(i, i == index)
            }
        }
    }
}

@Composable
private fun SystemTile(system: SystemEntry, logo: File?, focused: Boolean, bg: Color) {
    val size by animateDpAsState(
        if (focused) Size.SYSTEM_TILE_SELECTED.u else Size.SYSTEM_TILE.u, Motion.focus(), label = "tile-size",
    )
    val radius by animateDpAsState(
        if (focused) Radius.TILE_SELECTED.u else Radius.TILE.u, Motion.focus(), label = "tile-radius",
    )
    val ring by animateFloatAsState(if (focused) 1f else 0f, Motion.focus(), label = "tile-ring")
    Box(
        Modifier
            .size(size)
            .focusRing(ring, Palette.focusRingOnSystem, Size.FOCUS_RING.u, Size.FOCUS_OFFSET.u, radius)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(radius))
            .background(if (focused) Palette.white else system.color),
        contentAlignment = Alignment.Center,
    ) {
        val ink = if (focused) bg else Palette.white
        if (logo != null) {
            ArtImage(logo, Modifier.fillMaxSize().padding(Space.TILE_LOGO_PAD.u), tint = ink)
        } else {
            Txt(system.info.shortName, if (focused) Type.tileSelected else Type.tile, ink)
        }
    }
}

@Composable
private fun GameTile(title: String, box: File?, systemColor: Color, focused: Boolean, bg: Color) {
    val height by animateDpAsState(
        if (focused) Size.GAME_TILE_SELECTED_H.u else Size.GAME_TILE_H.u, Motion.focus(), label = "game-tile",
    )
    val ring by animateFloatAsState(if (focused) 1f else 0f, Motion.focus(), label = "game-ring")
    Box(
        Modifier
            .width(Size.GAME_TILE_W.u)
            .height(height)
            .focusRing(ring, Palette.focusRingOnSystemCard, Size.FOCUS_RING.u, Size.FOCUS_OFFSET.u, Radius.CARD.u)
            .clip(rounded(Radius.CARD))
            .background(if (focused) Palette.white else systemColor)
            .padding(Space.CARD_PAD.u),
        contentAlignment = Alignment.BottomStart,
    ) {
        if (box != null) ArtImage(box, Modifier.fillMaxSize())
        else Txt(title, Type.cardTitle, if (focused) bg else Palette.white, maxLines = 3)
    }
}

@Composable
private fun EmptyState(title: String, body: String) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = Space.SCREEN_H.u),
        verticalArrangement = Arrangement.Center,
    ) {
        Txt(title, Type.section, Palette.white, maxLines = 2)
        Spacer(Modifier.height(Space.MD.u))
        Txt(body, Type.body, Palette.white.copy(alpha = Alpha.SECONDARY_ON_SYSTEM), maxLines = 3)
    }
}
