package io.github.hipsterbwp.rp5launcher.ui.screens

import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.graphics.graphicsLayer
import io.github.hipsterbwp.rp5launcher.ui.theme.SystemNeon
import io.github.hipsterbwp.rp5launcher.ui.rememberGlowPulse
import io.github.hipsterbwp.rp5launcher.ui.LogoSign
import io.github.hipsterbwp.rp5launcher.ui.theme.Retro
import io.github.hipsterbwp.rp5launcher.ui.neonPanel
import io.github.hipsterbwp.rp5launcher.ui.neonGlow
import io.github.hipsterbwp.rp5launcher.ui.RetroBackdrop
import io.github.hipsterbwp.rp5launcher.ui.ChromeTxt
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.geometry.Size as GeoSize
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import io.github.hipsterbwp.rp5launcher.ui.ButtonGlyph
import io.github.hipsterbwp.rp5launcher.ui.GameVideo
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import io.github.hipsterbwp.rp5launcher.data.Game
import io.github.hipsterbwp.rp5launcher.ui.ArtImage
import io.github.hipsterbwp.rp5launcher.ui.Hint
import io.github.hipsterbwp.rp5launcher.ui.HintBar
import io.github.hipsterbwp.rp5launcher.ui.LauncherViewModel
import io.github.hipsterbwp.rp5launcher.ui.ordinalOf
import io.github.hipsterbwp.rp5launcher.ui.Season
import io.github.hipsterbwp.rp5launcher.ui.SeasonalSky
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

    fun select(i: Int) {
        vm.homeSystemIndex = i.coerceIn(0, (systems.size - 1).coerceAtLeast(0))
    }
    fun open() {
        selected?.let { vm.openSystem(it.name) }
    }
    val last = selected?.let(vm::lastPlayed)
    // Startup sound once the selection settles, so scrolling past consoles stays quiet.
    LaunchedEffect(selected?.name) {
        val name = selected?.name ?: return@LaunchedEffect
        delay(Motion.CONSOLE_SOUND_DELAY_MS)
        vm.landedOn(name)
    }
    fun resume() {
        if (selected != null && last != null) vm.play(selected.name, last)
    }

    val daily = vm.gameOfTheDay()
    fun openDaily() {
        daily?.let { (system, game) -> vm.push(Screen.Details(system.name, game.id)) }
    }

    PadInput { button, _ ->
        // ↑↑↓↓←→←→ B A: the last A is used up by the secret instead of opening a console.
        if (vm.secretInput(button)) return@PadInput true
        when (button) {
            Pad.R2 -> { openDaily(); true }
            Pad.L2 -> { vm.push(Screen.BaseCamp); true }
            Pad.Up, Pad.Left -> { if (!collections) select(index - 1); true }
            Pad.Down, Pad.Right -> { if (!collections) select(index + 1); true }
            Pad.L1 -> { switchTab(-1); true }
            Pad.R1 -> { switchTab(1); true }
            Pad.A -> { open(); true }
            Pad.X -> { resume(); true }
            Pad.Y -> { vm.push(Screen.Search); true }
            Pad.Start -> { openSettings(); true }
            else -> false
        }
    }

    // Each console lights the screen in its own neon.
    val neon by animateColorAsState(
        if (vm.config.consoleNeon && selected != null) SystemNeon.forSystem(selected.name) else Palette.pink,
        Motion.colorFade(), label = "neon",
    )
    Box(Modifier.fillMaxSize().background(Palette.night)) {
        RetroBackdrop(accent = neon)
        val backdrop = vm.config.backdrop
        // Hero, best first (in "auto"): your own picture from the "_art" folder, widescreen fanart of
        // one of your games, an HD cover of the best-selling game over a blur of itself, the console.
        val hero = selected?.takeIf { backdrop == "auto" }?.let { vm.systemCustom[it.name] ?: vm.systemHero(it) }
        val feature = selected?.takeIf { backdrop == "cover" }?.let { vm.systemFeature[it.name] }
        val sign = selected?.takeIf { backdrop != "console" }?.let { vm.logos[it.name] }
        val product = selected?.let { vm.systemImages[it.name] }
        val shade = if (vm.config.backdropTint && selected != null) {
            lerp(Palette.night, selected.color, Alpha.BACKDROP_TINT)
        } else {
            Palette.night
        }

        Crossfade(targetState = Hero(hero, feature, sign, product), animationSpec = Motion.colorFade(), label = "hero") { (art, cover, logo, console) ->
            Box(Modifier.fillMaxSize()) {
                if (art == null && cover == null && logo != null) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(
                                start = (Size.RAIL_W + Size.SYSTEM_PANEL_W + Space.SECTION_LARGE).u,
                                end = Space.SCREEN_H.u,
                                top = (Space.SCREEN_TOP + Size.STATUS_H).u,
                                bottom = Size.HINT_BAR.u,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        LogoSign(logo, Modifier.fillMaxWidth(SIGN_WIDTH), neon)
                    }
                } else if (art == null && cover != null) {
                    FeatureCover(cover, selected?.let { vm.bestSeller(it.name) }, vm.config.backdropBlur, shade)
                } else if (art != null) {
                    ArtImage(art, Modifier.fillMaxSize(), ContentScale.Crop)
                    // Synthwave colour wash (pink to cyan) when tinting is on.
                    if (vm.config.backdropTint) {
                        Canvas(Modifier.fillMaxSize()) { drawRect(Retro.duotone, blendMode = BlendMode.Color) }
                    }
                    // Soft shade behind the rail and panel, and along the top under the tabs and clock,
                    // so text stays readable over any art.
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.horizontalGradient(
                                0f to shade.copy(alpha = Alpha.HERO_SHADE),
                                0.62f to Palette.transparent,
                            ),
                        ),
                    )
                    Box(
                        Modifier.fillMaxWidth().height(Size.TOP_SHADE_H.u).background(
                            Brush.verticalGradient(
                                0f to Palette.black.copy(alpha = Alpha.TOP_SHADE),
                                1f to Palette.transparent,
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

        if (selected != null && vm.config.consoleVideos) ConsoleMontage(vm, selected, shade)

        Row(Modifier.fillMaxSize()) {
            LogoRail(vm, systems, if (collections) -1 else index, neon, ::select, ::open)
            if (selected != null) {
                SystemPanel(vm, selected, neon)
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

        // Game of the Day, under the top bar, with your streak.
        daily?.let { (system, game) ->
            Row(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = (Space.SCREEN_TOP + Size.STATUS_H + Space.LG).u, end = Space.SCREEN_H.u)
                    .glass(Radius.CARD_SMALL)
                    .tap(::openDaily)
                    .padding(vertical = Space.CHIP_V.u, horizontal = Space.CHIP_H.u),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.GAP.u),
            ) {
                Txt("★ GAME OF THE DAY", Type.badge, Palette.sun1, glow = Palette.sun2)
                Txt("${game.title} · ${system.info.shortName}", Type.labelStrong, Palette.ink, Modifier.widthIn(max = Size.DAILY_TITLE_W.u))
                val streak = vm.dailyStreak()
                if (streak > 0) Txt("🔥$streak", Type.labelStrong, Palette.sun2)
            }
        }

        // A console in your library turns a year older today: a banner, and fireworks while it's selected.
        vm.birthday()?.let { (system, age) ->
            if (selected?.name == system.name) SeasonalSky(Season.NewYear)
            Row(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = (Space.SCREEN_TOP + Size.STATUS_H + Space.LG + Size.BIRTHDAY_OFFSET).u, end = Space.SCREEN_H.u)
                    .glass(Radius.CARD_SMALL)
                    .padding(vertical = Space.CHIP_V.u, horizontal = Space.CHIP_H.u),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Txt("★ HAPPY ${ordinalOf(age).uppercase()} BIRTHDAY, ${system.info.displayName.uppercase()}!", Type.badge, Palette.sun1, glow = Palette.sun2)
            }
        }

        val hints = buildList {
            if (selected != null) add(Hint(Pad.A, "Open", onClick = ::open))
            if (last != null) add(Hint(Pad.X, "Continue", onClick = ::resume))
            add(Hint(Pad.Y, "Search", onClick = { vm.push(Screen.Search) }))
            add(Hint(listOf(Pad.L1, Pad.R1), "Switch tab", onClick = { switchTab(1) }))
            if (daily != null) add(Hint(Pad.R2, "Today's game", onClick = ::openDaily))
            add(Hint(Pad.L2, "Base Camp", onClick = { vm.push(Screen.BaseCamp) }))
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
    neon: Color,
    select: (Int) -> Unit,
    open: () -> Unit,
) {
    val edge = Size.NEON_EDGE.u
    BoxWithConstraints(
        Modifier
            .width(Size.RAIL_W.u)
            .fillMaxHeight()
            .background(Brush.horizontalGradient(listOf(Palette.night.copy(alpha = RAIL_FILL), Palette.deep.copy(alpha = RAIL_FILL_EDGE))))
            .drawBehind {
                // Neon edge on the right, with a soft glow.
                val x = size.width
                drawLine(Palette.pink.copy(alpha = 0.3f), Offset(x, 0f), Offset(x, size.height), edge.toPx() * 6)
                drawLine(Palette.pink, Offset(x, 0f), Offset(x, size.height), edge.toPx() * 2)
            },
    ) {
        Txt("P1 · CREDIT ${vm.credits().toString().padStart(2, '0')}", Type.badge, Palette.pinkSoft, Modifier.padding(Space.LG.u), glow = Palette.pink)
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
                val highlight by animateFloatAsState(if (focused) 1f else 0f, Motion.focus(), label = "rail-hl")
                val pulse = rememberGlowPulse()
                val radius = Radius.CARD.u
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .padding(start = Space.LG.u, end = Space.MD.u, top = Space.SM.u, bottom = Space.SM.u)
                        .drawBehind {
                            // Selected console: cyan neon box.
                            if (highlight <= 0f) return@drawBehind
                            val cr = CornerRadius(radius.toPx())
                            neonGlow(neon.copy(alpha = highlight), cr, edge.toPx(), pulse.value)
                            drawRoundRect(neon.copy(alpha = 0.08f * highlight), cornerRadius = cr)
                            drawRoundRect(neon.copy(alpha = highlight), cornerRadius = cr, style = Stroke(edge.toPx() * 1.5f))
                        }
                        .padding(horizontal = Space.SECTION_SMALL.u - Space.SM.u)
                        .alpha(alpha)
                        .tap { if (focused) open() else select(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (focused) {
                        Txt(
                            "▶", Type.tileSelected, Palette.pink,
                            Modifier.align(Alignment.CenterStart).offset(x = -Space.LG.u).graphicsLayer { this.alpha = 0.5f + pulse.value / 2 },
                            glow = Palette.pink,
                        )
                    }
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

private const val CONTINUE_FROM = 9

/** Neon panel: maker · year, the name in sunset letters, a scoreboard, facts, Continue and the console. */
@Composable
private fun SystemPanel(vm: LauncherViewModel, system: SystemEntry, neon: Color) {
    val info = system.info
    val favorites = system.games.count { vm.stats(it).favorite }
    val facts = vm.systemFacts[system.name].orEmpty().filter { it.first !in SKIP_FACTS }.take(MAX_FACTS)
    Column(
        Modifier
            .width(Size.SYSTEM_PANEL_W.u)
            .fillMaxHeight()
            .padding(start = Space.SECTION.u, top = Space.SCREEN_TOP.u, bottom = (Size.HINT_BAR + Space.LG).u)
            .neonPanel(Radius.PANEL, edge = neon)
            .padding(Space.SECTION.u),
    ) {
        val eyebrow = listOfNotNull(info.maker, info.year?.toString()).joinToString(" · ").uppercase()
        if (eyebrow.isNotEmpty()) {
            Txt(eyebrow, Type.eyebrow, Palette.cyanSoft, glow = Palette.cyan)
            Spacer(Modifier.height(Space.GAP.u))
        }
        ChromeTxt(info.displayName, Type.section, maxLines = 2)
        Spacer(Modifier.height(Space.MD.u))
        // Scoreboard.
        val played = system.games.sumOf { vm.stats(it).playtimeMs }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.SECTION.u)) {
            Score("GAMES", system.games.size.toString().padStart(3, '0'))
            Score("FAVS", favorites.toString().padStart(3, '0'))
            Score(
                "PLAYED",
                when {
                    played >= HOUR_MS -> "${played / HOUR_MS}H"
                    played > 0 -> "${played / MINUTE_MS}M"
                    else -> "--"
                },
            )
        }
        Spacer(Modifier.height(Space.ROW_GAP.u))
        facts.forEach { (label, value) ->
            Row(
                Modifier.fillMaxWidth().dashedBottom().padding(vertical = Space.TAG_V.u),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.GAP.u),
            ) {
                Txt(label.uppercase(), Type.badge, Palette.inkSecondary, Modifier.weight(1f, fill = false))
                Spacer(Modifier.weight(1f))
                Txt(value, Type.labelStrong, Palette.ink, Modifier.weight(2f, fill = false), maxLines = 1)
            }
        }
        vm.lastPlayed(system)?.let { last ->
            // Arcade continue screen: counts down from 9 each time you land on the console.
            var count by remember(system.name) { mutableIntStateOf(CONTINUE_FROM) }
            LaunchedEffect(system.name) {
                while (count > 0) {
                    delay(Motion.CONTINUE_TICK_MS)
                    count--
                }
            }
            Spacer(Modifier.height(Space.ROW_GAP.u))
            val radius = Radius.CARD.u
            val edge = Size.NEON_EDGE.u
            Row(
                Modifier
                    .fillMaxWidth()
                    .drawBehind { neonGlow(Palette.pink, CornerRadius(radius.toPx()), edge.toPx() / 2) }
                    .clip(rounded(Radius.CARD))
                    .background(Palette.pink.copy(alpha = CONTINUE_FILL))
                    .border(edge, Palette.pink, rounded(Radius.CARD))
                    .tap { vm.play(system.name, last) }
                    .padding(Space.MD.u),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.MD.u),
            ) {
                vm.boxArt(last)?.let {
                    ArtImage(it, Modifier.height(Size.CONTINUE_ART_H.u).width(Size.CONTINUE_ART_H.u))
                }
                Column(Modifier.weight(1f)) {
                    Txt("▶ CONTINUE? $count", Type.badge, Palette.pinkSoft, glow = Palette.pink)
                    Spacer(Modifier.height(Space.SM.u))
                    Txt(last.title, Type.labelStrong, Palette.ink, maxLines = 1)
                }
                ButtonGlyph(listOf(Pad.X), Palette.inkOnLight)
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
            .neonPanel(Radius.PANEL)
            .padding(Space.SECTION.u),
        verticalArrangement = Arrangement.Center,
    ) {
        ChromeTxt(title, Type.section, maxLines = 2)
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
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.MD.u)) {
        Txt(DateFormat.getTimeFormat(context).format(now).uppercase(), Type.crt, Palette.cyanSoft, glow = Palette.cyan)
        battery?.let { Battery(it) }
    }
}

/** Pixel battery: outline, nub and a fill to the level (pink when low). */
@Composable
private fun Battery(level: Int) {
    val stroke = Size.NEON_EDGE.u
    val color = if (level <= LOW_BATTERY) Palette.pink else Palette.cyanSoft
    Canvas(Modifier.width(Size.BATTERY_W.u).height(Size.BATTERY_H.u)) {
        val s = stroke.toPx()
        val nub = size.width * 0.08f
        val body = GeoSize(size.width - nub, size.height)
        drawRoundRect(color, Offset(s / 2, s / 2), GeoSize(body.width - s, body.height - s), CornerRadius(s), Stroke(s))
        drawRect(color, Offset(body.width, size.height * 0.3f), GeoSize(nub, size.height * 0.4f))
        val inset = s * 2
        drawRect(color, Offset(inset, inset), GeoSize((body.width - inset * 2) * level / 100f, body.height - inset * 2))
    }
}

/** Scoreboard entry: cyan label over a big yellow CRT number. */
@Composable
private fun Score(label: String, value: String) {
    Column {
        Txt(label, Type.badge, Palette.cyanSoft, glow = Palette.cyan)
        Txt(value, Type.stat, Palette.sun1, glow = Palette.sun2)
    }
}

/** Dashed divider under a fact row. */
@Composable
private fun Modifier.dashedBottom(): Modifier {
    val line = Size.HAIRLINE.u
    val dash = Space.SM.u
    return drawBehind {
        drawLine(
            Palette.inkFaint, Offset(0f, size.height), Offset(size.width, size.height), line.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), dash.toPx())),
        )
    }
}

/** What fills the right of the console screen, best first: art, cover, 3D logo, console. */
private data class Hero(val art: File?, val cover: File?, val logo: File?, val console: File?)

/** The logo sign spans most of the space right of the panel. */
private const val SIGN_WIDTH = 0.82f
private const val STATUS_REFRESH_MS = 30_000L
private const val LOW_BATTERY = 15
private const val HOUR_MS = 3_600_000L
private const val MINUTE_MS = 60_000L
private const val RAIL_FILL = 0.95f
private const val RAIL_FILL_EDGE = 0.85f
private const val CONTINUE_FILL = 0.12f

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
private fun FeatureCover(cover: File, title: String?, blurLevel: Int, shade: Color) {
    val radius = when (blurLevel) {
        0 -> 0
        1 -> Blur.BACKDROP / 2
        else -> Blur.BACKDROP
    }
    Box(Modifier.fillMaxSize()) {
        ArtImage(cover, Modifier.fillMaxSize().then(if (radius > 0) Modifier.blur(radius.u) else Modifier), ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(shade.copy(alpha = Alpha.BROWSER_DIM)))
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

/**
 * After a few seconds on a console, gameplay clips of its games play full screen over the backdrop,
 * one after another, muted, with the game's name in a glass caption.
 */
@Composable
private fun ConsoleMontage(vm: LauncherViewModel, system: SystemEntry, shade: Color) {
    LaunchedEffect(system.name) { vm.loadEsdeVideos(system.name) }
    val clips = remember(system.name, vm.esdeVideos[system.name], vm.art.size) { vm.consoleClips(system) }
    var index by remember(system.name) { mutableStateOf(-1) }
    LaunchedEffect(system.name, clips) {
        index = -1
        if (clips.isEmpty()) return@LaunchedEffect
        delay(Motion.MONTAGE_DELAY_MS)
        while (true) {
            index++
            delay(Motion.MONTAGE_CLIP_MS)
        }
    }
    val clip = clips.takeIf { index >= 0 }?.let { it[index % it.size] } ?: return
    Box(Modifier.fillMaxSize()) {
        GameVideo(clip.second, withSound = false, modifier = Modifier.fillMaxSize())
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(0f to shade.copy(alpha = Alpha.HERO_SHADE), 0.62f to Palette.transparent),
            ),
        )
        Box(
            Modifier.fillMaxWidth().height(Size.TOP_SHADE_H.u).background(
                Brush.verticalGradient(0f to Palette.black.copy(alpha = Alpha.TOP_SHADE), 1f to Palette.transparent),
            ),
        )
        Crossfade(clip.first.title, animationSpec = Motion.colorFade(), label = "clip-title",
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = Space.SCREEN_H.u, bottom = (Size.HINT_BAR + Space.LG).u),
        ) { title ->
            Column(Modifier.glass(Radius.PILL).padding(vertical = Space.CHIP_V.u, horizontal = Space.LG.u)) {
                Txt("NOW SHOWING", Type.eyebrow, Palette.inkSecondary)
                Txt(title, Type.labelStrong, Palette.ink, maxLines = 1)
            }
        }
    }
}
