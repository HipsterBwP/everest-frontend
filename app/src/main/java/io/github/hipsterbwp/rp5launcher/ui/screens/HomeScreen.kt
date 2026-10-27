package io.github.hipsterbwp.rp5launcher.ui.screens

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.text.format.DateFormat
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import java.io.File
import io.github.hipsterbwp.rp5launcher.ui.theme.rounded
import io.github.hipsterbwp.rp5launcher.ui.theme.Elevation
import io.github.hipsterbwp.rp5launcher.ui.theme.Blur
import coil.compose.rememberAsyncImagePainter
import coil.compose.AsyncImagePainter
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import io.github.hipsterbwp.rp5launcher.data.Game
import io.github.hipsterbwp.rp5launcher.ui.ArtImage
import io.github.hipsterbwp.rp5launcher.ui.Hint
import io.github.hipsterbwp.rp5launcher.ui.HintBar
import io.github.hipsterbwp.rp5launcher.ui.LauncherViewModel
import io.github.hipsterbwp.rp5launcher.ui.Pad
import io.github.hipsterbwp.rp5launcher.ui.PadInput
import io.github.hipsterbwp.rp5launcher.ui.Screen
import io.github.hipsterbwp.rp5launcher.ui.SystemEntry
import io.github.hipsterbwp.rp5launcher.ui.Txt
import io.github.hipsterbwp.rp5launcher.ui.glass
import io.github.hipsterbwp.rp5launcher.ui.tap
import io.github.hipsterbwp.rp5launcher.ui.theme.Alpha
import io.github.hipsterbwp.rp5launcher.ui.theme.Focus
import io.github.hipsterbwp.rp5launcher.ui.theme.Motion
import io.github.hipsterbwp.rp5launcher.ui.theme.Palette
import io.github.hipsterbwp.rp5launcher.ui.theme.Radius
import io.github.hipsterbwp.rp5launcher.ui.theme.Size
import io.github.hipsterbwp.rp5launcher.ui.theme.Space
import io.github.hipsterbwp.rp5launcher.ui.theme.SystemColors
import io.github.hipsterbwp.rp5launcher.ui.theme.Type
import io.github.hipsterbwp.rp5launcher.ui.theme.u
import kotlinx.coroutines.delay
import java.util.Date

private val TABS = listOf("Systems", "Favorites", "Recent", "Collections")
private const val TAB_SYSTEMS = 0
private const val TAB_FAVORITES = 1
private const val TAB_RECENT = 2
private const val TAB_COLLECTIONS = 3

/** Facts already shown elsewhere on the panel. */
private val SKIP_FACTS = setOf("Manufacturer", "Released")
private const val MAX_FACTS = 4

@Composable
fun HomeScreen(vm: LauncherViewModel, appName: String) {
    val tab = vm.homeTab
    fun switchTab(delta: Int) {
        vm.homeTab = (tab + delta).mod(TABS.size)
    }
    fun openSettings() = vm.push(Screen.Settings)

    when (tab) {
        TAB_FAVORITES, TAB_RECENT -> GamesTab(vm, tab, ::switchTab, ::openSettings)
        TAB_COLLECTIONS -> CollectionsTab(vm, TABS, tab, ::switchTab, ::openSettings)
        else -> SystemsTab(vm, tab, ::switchTab, ::openSettings)
    }
}

// ---------------------------------------------------------------------------------------------
// Systems: logo rail | glass info panel | hero art
// ---------------------------------------------------------------------------------------------

@Composable
private fun SystemsTab(
    vm: LauncherViewModel,
    tab: Int,
    switchTab: (Int) -> Unit,
    openSettings: () -> Unit,
) {
    val systems = vm.systems
    val collections = tab == TAB_COLLECTIONS
    val index = vm.homeSystemIndex.coerceIn(0, (systems.size - 1).coerceAtLeast(0))
    val selected = systems.getOrNull(index)?.takeIf { !collections }
    val bg by animateColorAsState(selected?.color ?: SystemColors.neutral, Motion.colorFade(), label = "home-bg")

    fun select(i: Int) {
        vm.homeSystemIndex = i.coerceIn(0, (systems.size - 1).coerceAtLeast(0))
    }
    fun open() {
        selected?.let { vm.push(Screen.Games(it.name)) }
    }

    PadInput { button, _ ->
        when (button) {
            Pad.Up, Pad.Left -> { if (!collections) select(index - 1); true }
            Pad.Down, Pad.Right -> { if (!collections) select(index + 1); true }
            Pad.L1 -> { switchTab(-1); true }
            Pad.R1 -> { switchTab(1); true }
            Pad.A -> { open(); true }
            Pad.Y -> { vm.push(Screen.Search); true }
            Pad.Start -> { openSettings(); true }
            else -> false
        }
    }

    Box(Modifier.fillMaxSize().background(bg)) {
        val hero = selected?.let(vm::systemHero)
        val feature = selected?.let { vm.systemFeature[it.name] }
        val product = selected?.let { vm.systemImages[it.name] }

        // Hero, best first: widescreen fanart of one of your games (ScreenScraper), else an HD cover
        // of the system's best-selling game over a blur of itself, else the console as a product shot.
        Crossfade(targetState = Triple(hero, feature, product), animationSpec = Motion.colorFade(), label = "hero") { (art, cover, console) ->
            Box(Modifier.fillMaxSize()) {
                if (art == null && cover != null) {
                    FeatureCover(cover, selected?.let { vm.bestSeller(it.name) })
                } else if (art != null) {
                    ArtImage(art, Modifier.fillMaxSize(), ContentScale.Crop)
                    // Soft shade behind the rail and panel so text stays readable over any art.
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.horizontalGradient(
                                0f to Palette.black.copy(alpha = Alpha.HERO_SHADE),
                                0.62f to Palette.transparent,
                            ),
                        ),
                    )
                } else if (console != null) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(start = (Size.RAIL_W + Size.SYSTEM_PANEL_W + Space.SECTION_LARGE).u, bottom = Size.HINT_BAR.u),
                        contentAlignment = Alignment.Center,
                    ) {
                        ArtImage(console, Modifier.fillMaxWidth(0.82f).fillMaxHeight(0.62f))
                    }
                }
            }
        }

        Row(Modifier.fillMaxSize()) {
            LogoRail(vm, systems, if (collections) -1 else index, ::select, ::open)
            if (selected != null) {
                SystemPanel(vm, selected)
            } else {
                EmptyPanel(
                    if (collections) "Collections" else if (vm.scan != null) "Looking for games…" else "No games found",
                    when {
                        collections -> "Collections arrive in a later version."
                        vm.scan != null -> "Scanning ${vm.rootLabel().orEmpty()}"
                        else -> "Put games in system folders like snes, gba or ps2 inside ${vm.rootLabel().orEmpty()}, " +
                            "then rescan from Settings › Library."
                    },
                )
            }
        }

        // Top-right: tabs and status, like a tvOS top bar.
        Row(
            Modifier.align(Alignment.TopEnd).padding(top = Space.SCREEN_TOP.u, end = Space.SCREEN_H.u),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.ROW_GAP.u),
        ) {
            vm.scrape?.let { p -> Txt("Getting art ${p.done}/${p.total}", Type.label, Palette.inkSecondary) }
            TabPills(TABS, tab) { vm.homeTab = it }
            StatusPill()
        }

        val hints = buildList {
            if (selected != null) add(Hint(Pad.A, "Open", onClick = ::open))
            add(Hint(Pad.Y, "Search", onClick = { vm.push(Screen.Search) }))
            add(Hint(listOf(Pad.L1, Pad.R1), "Switch tab", onClick = { switchTab(1) }))
            add(Hint(Pad.Start, "Settings", trailing = true, onClick = openSettings))
        }
        HintBar(hints, onSystem = true, glyphInk = Palette.inkOnLight)
    }
}

/** Vertical rail of console logos; the selected one is centered, larger and fully lit. */
@Composable
private fun LogoRail(
    vm: LauncherViewModel,
    systems: List<SystemEntry>,
    index: Int,
    select: (Int) -> Unit,
    open: () -> Unit,
) {
    BoxWithConstraints(
        Modifier
            .width(Size.RAIL_W.u)
            .fillMaxHeight()
            .background(Palette.shade),
    ) {
        val itemHeight = (Size.RAIL_LOGO_SELECTED_H + Size.RAIL_ITEM_GAP).u
        val state = rememberLazyListState()
        LaunchedEffect(index) { if (index >= 0) state.animateScrollToItem(index) }
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            // Padding so the first and last items can sit in the middle.
            contentPadding = PaddingValues(vertical = (maxHeight - itemHeight) / 2),
            userScrollEnabled = false,
        ) {
            itemsIndexed(systems, key = { _, s -> s.name }) { i, system ->
                val focused = i == index
                val height by animateDpAsState(
                    if (focused) Size.RAIL_LOGO_SELECTED_H.u else Size.RAIL_LOGO_H.u, Motion.focus(), label = "rail-h",
                )
                val alpha by animateFloatAsState(if (focused) 1f else Focus.RAIL_DIM, Motion.focus(), label = "rail-a")
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .padding(horizontal = Space.SECTION_SMALL.u)
                        .alpha(alpha)
                        .tap { if (focused) open() else select(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    val logo = vm.logos[system.name]
                    if (logo != null) {
                        ArtImage(logo, Modifier.fillMaxWidth().height(height), tint = Palette.white)
                    } else {
                        Txt(system.info.shortName, if (focused) Type.tileSelected else Type.tile, Palette.white)
                    }
                }
            }
        }
    }
}

/** Frosted panel: console illustration, name, counts and a short fact sheet. */
@Composable
private fun SystemPanel(vm: LauncherViewModel, system: SystemEntry) {
    val info = system.info
    val favorites = system.games.count { vm.stats(it).favorite }
    val facts = vm.systemFacts[system.name].orEmpty().filter { it.first !in SKIP_FACTS }.take(MAX_FACTS)
    Column(
        Modifier
            .width(Size.SYSTEM_PANEL_W.u)
            .fillMaxHeight()
            .padding(start = Space.SECTION.u, top = Space.SCREEN_TOP.u, bottom = (Size.HINT_BAR + Space.LG).u)
            .glass(Radius.PANEL)
            .padding(Space.SECTION.u),
    ) {
        val eyebrow = listOfNotNull(info.maker, info.year?.toString()).joinToString(" · ").uppercase()
        if (eyebrow.isNotEmpty()) {
            Txt(eyebrow, Type.eyebrow, Palette.inkSecondary)
            Spacer(Modifier.height(Space.SM.u))
        }
        Txt(info.displayName, Type.section, Palette.ink, maxLines = 2)
        Spacer(Modifier.height(Space.GAP.u))
        val n = system.games.size
        Txt(
            "$n ${if (n == 1) "game" else "games"} · $favorites ${if (favorites == 1) "favorite" else "favorites"}",
            Type.label, Palette.inkSecondary,
        )
        Spacer(Modifier.height(Space.SECTION_SMALL.u))
        Column(verticalArrangement = Arrangement.spacedBy(Space.ROW_GAP.u)) {
            facts.forEach { (label, value) ->
                Column {
                    Txt(label, Type.caption, Palette.inkSecondary)
                    Txt(value, Type.label, Palette.ink, maxLines = 2)
                }
            }
            vm.lastPlayed(system)?.let { last ->
                Column {
                    Txt("Last played", Type.caption, Palette.inkSecondary)
                    Txt(last.title, Type.label, Palette.ink)
                }
            }
        }
        // The console (or its controller) fills the rest of the panel.
        Box(Modifier.weight(1f).fillMaxWidth().padding(top = Space.SECTION_SMALL.u), contentAlignment = Alignment.Center) {
            vm.systemImages[system.name]?.let { ArtImage(it, Modifier.fillMaxSize()) }
        }
    }
}

@Composable
private fun EmptyPanel(title: String, body: String) {
    Column(
        Modifier
            .width(Size.SYSTEM_PANEL_W.u)
            .fillMaxHeight()
            .padding(start = Space.SECTION.u, top = Space.SCREEN_TOP.u, bottom = (Size.HINT_BAR + Space.LG).u)
            .glass(Radius.PANEL)
            .padding(Space.SECTION.u),
        verticalArrangement = Arrangement.Center,
    ) {
        Txt(title, Type.section, Palette.ink, maxLines = 2)
        Spacer(Modifier.height(Space.MD.u))
        Txt(body, Type.bodySmall, Palette.inkSecondary, maxLines = 5)
    }
}

/** Clock and battery in a glass capsule, refreshed every half minute. */
@Composable
internal fun StatusPill() {
    val context = LocalContext.current
    var now by remember { mutableStateOf(Date()) }
    var battery by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.let {
                val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) level * 100 / scale else null
            }
            delay(STATUS_REFRESH_MS)
        }
    }
    Row(
        Modifier.glass(Radius.PILL).padding(vertical = Space.CHIP_V.u, horizontal = Space.CHIP_H.u),
        horizontalArrangement = Arrangement.spacedBy(Space.GAP.u),
    ) {
        Txt(DateFormat.getTimeFormat(context).format(now), Type.tab, Palette.ink)
        battery?.let { Txt("$it%", Type.tab, Palette.inkSecondary) }
    }
}

private const val STATUS_REFRESH_MS = 30_000L

// ---------------------------------------------------------------------------------------------
// Favorites / Recent: the game browser across all systems
// ---------------------------------------------------------------------------------------------

@Composable
private fun GamesTab(vm: LauncherViewModel, tab: Int, switchTab: (Int) -> Unit, openSettings: () -> Unit) {
    val games: List<Pair<SystemEntry, Game>> = if (tab == TAB_FAVORITES) vm.favorites() else vm.recent()
    val index = (vm.homeGameIndex[tab] ?: 0).coerceIn(0, (games.size - 1).coerceAtLeast(0))
    val focused = games.getOrNull(index)
    fun select(i: Int) {
        vm.homeGameIndex[tab] = i
    }
    fun play(i: Int) {
        games.getOrNull(i)?.let { (s, g) -> vm.play(s.name, g) }
    }
    fun options() {
        focused?.let { (s, g) -> vm.push(Screen.Details(s.name, g.id)) }
    }

    PadInput { button, repeat ->
        when (button) {
            Pad.A -> { play(index); true }
            Pad.X -> { options(); true }
            Pad.Y -> { focused?.let { vm.toggleFavorite(it.second) }; true }
            Pad.L1 -> { switchTab(-1); true }
            Pad.R1 -> { switchTab(1); true }
            Pad.Start -> { openSettings(); true }
            else -> browserMove(button, repeat, index, games.size, Size.COVER_COLUMNS, ::select)
        }
    }

    val hints = buildList {
        if (focused != null) {
            add(Hint(Pad.A, "Play", onClick = { play(index) }))
            add(Hint(Pad.X, "Options", onClick = ::options))
            add(Hint(Pad.Y, if (vm.stats(focused.second).favorite) "Unfavorite" else "Favorite", onClick = { vm.toggleFavorite(focused.second) }))
        }
        add(Hint(listOf(Pad.L1, Pad.R1), "Switch tab", onClick = { switchTab(1) }))
        add(Hint(Pad.Start, "Settings", trailing = true, onClick = openSettings))
        randomHint(games.size) { browserMove(Pad.Select, 0, index, games.size, Size.COVER_COLUMNS, ::select) }?.let { add(it) }
    }
    GameBrowser(
        vm = vm,
        entries = games,
        index = index,
        header = {
            BrowserHeader(null, TABS[tab], games.size, TABS, tab) { vm.homeTab = it }
        },
        emptyTitle = if (tab == TAB_FAVORITES) "No favorites yet" else "Nothing played yet",
        emptyBody = if (tab == TAB_FAVORITES) "Press Y on any game to add it here." else "Games you play show up here, newest first.",
        hints = hints,
        onSelect = ::select,
        onPlay = ::play,
    )
}

/**
 * Apple-TV-style feature: the best-selling game's cover as a large lifted card, over a blurred,
 * dimmed copy of itself that fills the screen.
 */
@Composable
private fun FeatureCover(cover: File, title: String?) {
    Box(Modifier.fillMaxSize()) {
        ArtImage(cover, Modifier.fillMaxSize().blur(Blur.BACKDROP.u), ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Palette.black.copy(alpha = Alpha.BROWSER_DIM)))
        Column(
            Modifier
                .fillMaxSize()
                .padding(
                    start = (Size.RAIL_W + Size.SYSTEM_PANEL_W + Space.SECTION * 2).u,
                    end = Space.SCREEN_H.u,
                    top = (Space.SCREEN_TOP + Size.STATUS_H + Space.SECTION).u,
                    bottom = (Size.HINT_BAR + Space.LG).u,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val painter = rememberAsyncImagePainter(cover)
            val ratio = (painter.state as? AsyncImagePainter.State.Success)?.painter?.intrinsicSize
                ?.let { if (it.height > 0) it.width / it.height else null } ?: FEATURE_RATIO
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Image(
                    painter,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .aspectRatio(ratio)
                        .shadow(Elevation.LIFTED.u, rounded(Radius.CARD), clip = false)
                        .clip(rounded(Radius.CARD)),
                )
            }
            if (title != null) {
                Spacer(Modifier.height(Space.ROW_GAP.u))
                Column(
                    Modifier.glass(Radius.PILL).padding(vertical = Space.CHIP_V.u, horizontal = Space.LG.u),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Txt("BEST-SELLING GAME", Type.eyebrow, Palette.inkSecondary)
                    Txt(title, Type.labelStrong, Palette.ink)
                }
            }
        }
    }
}

/** Box-art proportion used until the feature cover has loaded. */
private const val FEATURE_RATIO = 0.72f
