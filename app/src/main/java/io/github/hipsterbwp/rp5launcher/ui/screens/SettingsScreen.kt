package io.github.hipsterbwp.rp5launcher.ui.screens

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Size as GeoSize
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import io.github.hipsterbwp.rp5launcher.ui.RetroBackdrop
import io.github.hipsterbwp.rp5launcher.ui.ChromeTxt
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.platform.LocalContext
import android.provider.Settings
import android.content.Intent
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import io.github.hipsterbwp.rp5launcher.BuildConfig
import io.github.hipsterbwp.rp5launcher.ui.ChevronIcon
import io.github.hipsterbwp.rp5launcher.ui.Dialog
import io.github.hipsterbwp.rp5launcher.ui.Hint
import io.github.hipsterbwp.rp5launcher.ui.HintBar
import io.github.hipsterbwp.rp5launcher.data.Progress
import io.github.hipsterbwp.rp5launcher.ui.LauncherViewModel
import io.github.hipsterbwp.rp5launcher.ui.MediaKind
import io.github.hipsterbwp.rp5launcher.ui.Unlock
import io.github.hipsterbwp.rp5launcher.ui.ListRow
import androidx.compose.ui.Alignment
import io.github.hipsterbwp.rp5launcher.ui.neonPanel
import io.github.hipsterbwp.rp5launcher.ui.RpgStatus
import io.github.hipsterbwp.rp5launcher.ui.UiSound
import io.github.hipsterbwp.rp5launcher.ui.Pad
import io.github.hipsterbwp.rp5launcher.ui.PadInput
import io.github.hipsterbwp.rp5launcher.ui.TextField
import io.github.hipsterbwp.rp5launcher.ui.Txt
import io.github.hipsterbwp.rp5launcher.ui.bringIntoView
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
import java.text.DateFormat
import java.util.Date

private enum class Page(val title: String) {
    Library("Library"), Emulators("Emulators"), Controls("Controls"), Appearance("Appearance"), Scraper("Scraper"),
    ScreenScraper("ScreenScraper"), RetroAchievements("RetroAchievements"), Sound("Sound"), Stats("Stats"), About("About")
}

/** One actionable row on a settings page. */
private data class Item(
    val key: String,
    val onA: (() -> Unit)? = null,
    val aLabel: String? = null,
    val onX: (() -> Unit)? = null,
    val xLabel: String? = null,
    /** Sliders: Left/Right adjust instead of leaving the page. */
    val onAdjust: ((Int) -> Unit)? = null,
    val content: @Composable (focused: Boolean) -> Unit,
)

@Composable
fun SettingsScreen(vm: LauncherViewModel, pickFolder: () -> Unit) {
    val artPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        vm.importMedia(MediaKind.Art, uris)
    }
    val importArt: () -> Unit = {
        runCatching { artPicker.launch(arrayOf("image/*", "application/zip", "application/x-zip-compressed")) }
    }
    val esdePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let(vm::onEsdeMediaPicked)
    }
    val pickEsde: () -> Unit = { runCatching { esdePicker.launch(null) } }
    val soundPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        vm.importMedia(MediaKind.Sounds, uris)
    }
    val importSounds: () -> Unit = {
        runCatching { soundPicker.launch(arrayOf("audio/*", "application/ogg", "application/zip", "application/x-zip-compressed")) }
    }
    var page by rememberSaveable { mutableStateOf(Page.Emulators) }
    var inContent by rememberSaveable { mutableStateOf(false) }
    var row by rememberSaveable { mutableIntStateOf(0) }
    // Settings-style screens use Apple blue: system colors are too dark to read as focus on black.
    val accent = Palette.accent
    val version = vm.emulatorsVersion

    val items: List<Item> = when (page) {
        Page.Library -> libraryItems(vm, accent, pickFolder, pickEsde)
        Page.Emulators -> emulatorItems(vm, accent, version)
        Page.Scraper -> scraperItems(vm, accent)
        Page.ScreenScraper -> screenScraperItems(vm, accent)
        Page.RetroAchievements -> retroAchievementsItems(vm, accent)
        Page.Sound -> soundItems(vm, accent, importSounds)
        Page.Appearance -> appearanceItems(vm, accent, importArt)
        Page.Controls -> controlsItems(vm, accent)
        Page.Stats -> statsItems(vm, accent)
        Page.About -> aboutItems(vm, accent)
    }
    val rowIndex = row.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    val focusedItem = items.getOrNull(rowIndex)?.takeIf { inContent }

    fun movePage(delta: Int) {
        page = Page.entries[(page.ordinal + delta).coerceIn(0, Page.entries.size - 1)]
        row = 0
    }

    PadInput { button, _ ->
        if (!inContent) {
            when (button) {
                Pad.Up -> { movePage(-1); true }
                Pad.Down -> { movePage(1); true }
                Pad.Right, Pad.A -> { if (items.isNotEmpty()) inContent = true; true }
                Pad.B -> vm.back()
                else -> false
            }
        } else {
            when (button) {
                Pad.Up -> { row = (rowIndex - 1).coerceAtLeast(0); true }
                Pad.Down -> { row = (rowIndex + 1).coerceAtMost(items.size - 1); true }
                Pad.Left -> { focusedItem?.onAdjust?.invoke(-1) ?: run { inContent = false }; true }
                Pad.Right -> { focusedItem?.onAdjust?.invoke(1); true }
                Pad.B -> { inContent = false; true }
                Pad.A -> { focusedItem?.onA?.invoke(); true }
                Pad.X -> { focusedItem?.onX?.invoke(); true }
                else -> false
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Palette.page)) {
        RetroBackdrop()
        Box(Modifier.fillMaxSize().background(Palette.night.copy(alpha = SETTINGS_DIM)))
        Row(Modifier.fillMaxSize()) {
            // Sidebar: a dark strip with a neon edge, the page list as an arcade menu.
            Column(
                Modifier
                    .width(Size.SETTINGS_SIDEBAR.u)
                    .fillMaxHeight()
                    .background(Palette.sidebar.copy(alpha = SIDEBAR_FILL))
                    .border(Size.HAIRLINE.u, Palette.glassEdge)
                    .padding(top = Space.SETTINGS_TOP.u, start = Space.SIDEBAR_H.u, end = Space.SIDEBAR_H.u),
                verticalArrangement = Arrangement.spacedBy(Space.LIST_GAP.u),
            ) {
                ChromeTxt("Settings", Type.screenTitle, Modifier.padding(start = Space.ROW_GAP.u, bottom = Space.TILE_GAP.u))
                Page.entries.forEach { p ->
                    val selected = p == page
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(rounded(Radius.CARD_SMALL))
                            .background(if (selected) Palette.pink else Palette.transparent)
                            .tap { page = p; row = 0; inContent = false }
                            .padding(vertical = Space.ROW_V.u, horizontal = Space.ROW_H.u),
                    ) {
                        Txt(
                            p.title.uppercase(),
                            if (selected) Type.navItemSelected else Type.navItem,
                            if (selected) Palette.white else Palette.inkSecondary,
                            glow = if (selected) Palette.chromeShadow else null,
                        )
                    }
                }
            }

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(top = Space.SETTINGS_TOP.u, start = Space.SCREEN_H.u, end = Space.SCREEN_H.u),
            ) {
                ChromeTxt(page.title, Type.screenTitleSmall)
                Spacer(Modifier.height(Space.SM.u))
                Txt(pageSubtitle(page), Type.bodySmall, Palette.inkSecondary, maxLines = 2)

                val state = rememberLazyListState()
                LaunchedEffect(rowIndex, page) { state.bringIntoView(rowIndex) }
                val ring = Size.FOCUS_RING_LIGHT + Size.FOCUS_OFFSET
                LazyColumn(
                    state = state,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = ring.u, end = ring.u,
                        top = (Space.SECTION_SMALL - ring).coerceAtLeast(ring).u,
                        bottom = (Size.HINT_BAR + Space.LG).u,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Space.LIST_GAP.u),
                ) {
                    rows(items, if (inContent) rowIndex else -1) { i -> row = i; inContent = true }
                }
            }
        }

        val hints = buildList {
            if (!inContent) {
                add(Hint(Pad.A, "Open", onClick = { inContent = true }))
                add(Hint(Pad.B, "Back", onClick = { vm.back() }))
            } else {
                focusedItem?.aLabel?.let { add(Hint(Pad.A, it, onClick = focusedItem.onA)) }
                focusedItem?.onAdjust?.let { adjust -> add(Hint(listOf(Pad.Left, Pad.Right), "Adjust", onClick = { adjust(1) })) }
                add(Hint(Pad.B, "Back", onClick = { inContent = false }))
                focusedItem?.xLabel?.let { add(Hint(Pad.X, it, onClick = focusedItem.onX)) }
            }
        }
        HintBar(hints, onSystem = false, glyphInk = Palette.inkOnLight)
    }
}

private fun LazyListScope.rows(items: List<Item>, focusedIndex: Int, onTap: (Int) -> Unit) {
    items.forEachIndexed { i, entry ->
        item(key = entry.key) {
            Box(Modifier.tap { if (i == focusedIndex) entry.onA?.invoke() else onTap(i) }) {
                entry.content(i == focusedIndex)
            }
        }
    }
}

private fun pageSubtitle(page: Page) = when (page) {
    Page.Library -> "Where your games live and when they were last scanned."
    Page.Emulators -> "Which app opens each system. Changes are saved to launcher-config.json."
    Page.Scraper -> "Box art, screenshots, game info and console logos, downloaded from free sources."
    Page.ScreenScraper -> "Sign in for gameplay videos, fanart, descriptions and ratings."
    Page.RetroAchievements -> "Your achievements, points and progress for each game."
    Page.Sound -> "Menu sounds, background music and gameplay video sound."
    Page.Appearance -> "What fills the console screen behind each system."
    Page.Controls -> "Buttons, the screensaver, and making Everest your home screen."
    Page.Stats -> "Your play time, favourites and streaks."
    Page.About -> "Version, config file and credits."
}

@Composable
private fun libraryItems(vm: LauncherViewModel, accent: Color, pickFolder: () -> Unit, pickEsde: () -> Unit): List<Item> {
    val lib = vm.library
    val scanning = vm.scan
    val games = vm.systems.sumOf { it.games.size }
    return listOf(
        Item("folder", onA = pickFolder, aLabel = "Change folder") { focused ->
            ListRow(focused, accent) {
                Column(Modifier.weight(1f)) {
                    Txt("ROMS FOLDER", Type.caps, Palette.inkSecondary)
                    Txt(vm.rootLabel() ?: "Not chosen", Type.rowTitle, Palette.ink)
                }
                ChevronIcon(Palette.ink)
            }
        },
        Item("esde", onA = pickEsde, aLabel = "Choose folder", onX = vm::clearEsdeMedia, xLabel = "Forget") { focused ->
            ListRow(focused, accent) {
                Column(Modifier.weight(1f)) {
                    Txt("ES-DE MEDIA FOLDER", Type.caps, Palette.inkSecondary)
                    Txt(vm.esdeMediaLabel() ?: "Not chosen", Type.rowTitle, Palette.ink)
                    Txt(
                        "Plays the gameplay videos ES-DE downloaded. Choose the ES-DE folder " +
                            "(usually Internal storage / ES-DE) or its downloaded_media folder.",
                        Type.label, Palette.inkSecondary, maxLines = 2,
                    )
                }
                ChevronIcon(Palette.ink)
            }
        },
        Item("rescan", onA = { vm.rescan() }, aLabel = "Rescan") { focused ->
            ListRow(focused, accent) {
                Txt("Rescan library", Type.rowTitle, Palette.ink, Modifier.weight(1f))
                val status = when {
                    scanning != null && scanning.total > 0 -> "Scanning ${scanning.done} of ${scanning.total}"
                    scanning != null -> "Scanning…"
                    lib != null -> "${vm.systems.size} systems · $games games · " +
                        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(lib.scannedAt))
                    else -> "Not scanned yet"
                }
                Txt(status, Type.label, Palette.inkSecondary)
            }
        },
    ) + listOf(
        Item("hiddengames", onA = { vm.unhideAllGames() }, aLabel = "Unhide all") { focused ->
            ListRow(focused, accent) {
                Txt("Hidden games", Type.rowTitle, Palette.ink, Modifier.weight(1f))
                val n = vm.hiddenGameCount()
                Txt(if (n == 0) "None" else "$n hidden · A to show them again", Type.label, Palette.inkSecondary)
            }
        },
    ) + vm.scannedSystems().map { (name, display) ->
        Item("show-$name", onA = { vm.toggleSystemHidden(name) }, aLabel = "Show / hide") { focused ->
            ListRow(focused, accent) {
                Txt(display, Type.rowTitle, Palette.ink, Modifier.weight(1f))
                val shown = name !in vm.config.hiddenSystems
                Txt(if (shown) "Shown" else "Hidden", Type.labelStrong, if (shown) Palette.ok else Palette.inkSecondary)
            }
        }
    } + lib?.unknownFolders.orEmpty().takeIf { it.isNotEmpty() }?.let { unknown ->
        listOf(
            Item("unknown") { _ ->
                ListRow(false, accent, background = Palette.surface) {
                    Txt("Not recognized: ${unknown.joinToString(", ")}", Type.label, Palette.inkSecondary, maxLines = 2)
                }
            },
        )
    }.orEmpty()
}

@Composable
private fun emulatorItems(vm: LauncherViewModel, accent: Color, version: Int): List<Item> =
    vm.systems.map { system ->
        Item(
            "sys-${system.name}",
            onA = { vm.dialog = Dialog.EmulatorPicker(system.name) },
            aLabel = "Change",
            onX = if (system.games.isNotEmpty()) ({ vm.testLaunch(system.name) }) else null,
            xLabel = if (system.games.isNotEmpty()) "Test launch" else null,
        ) { focused ->
            SystemStatusRow(
                vm, system, focused, accent, version, showCount = false,
                trailing = { ChevronIcon(Palette.ink) },
                onClick = { vm.dialog = Dialog.EmulatorPicker(system.name) },
            )
        }
    } + Item("redetect", onA = { vm.redetectEmulators() }, aLabel = "Re-detect") { focused ->
        ListRow(focused, accent, background = Palette.surface) {
            Txt("Re-detect installed emulators", Type.rowTitle, Palette.ink, Modifier.weight(1f))
            Txt("Also runs when you return to the launcher", Type.label, Palette.inkSecondary)
        }
    }

@Composable
private fun scraperItems(vm: LauncherViewModel, accent: Color): List<Item> {
    val progress = vm.scrape
    val missing = vm.scrapeTodo(force = false)
    return listOf(
        Item(
            "scrape",
            onA = { if (progress != null) vm.stopScrape() else vm.scrapeArt(force = false) },
            aLabel = if (progress != null) "Stop" else "Download",
        ) { focused ->
            ListRow(focused, accent) {
                Column(Modifier.weight(1f)) {
                    Txt("Download missing art", Type.rowTitle, Palette.ink)
                    Txt(
                        when {
                            progress != null -> "${progress.system} · ${progress.done} of ${progress.total}"
                            missing > 0 -> "$missing games without box art"
                            else -> "Every supported game has been scraped"
                        },
                        Type.label, Palette.inkSecondary,
                    )
                }
                ChevronIcon(Palette.ink)
            }
        },
        Item("auto", onA = { vm.setAutoScrape(!vm.config.autoScrape) }, aLabel = "Toggle") { focused ->
            ListRow(focused, accent) {
                Txt("Get art for new games automatically", Type.rowTitle, Palette.ink, Modifier.weight(1f))
                Txt(
                    if (vm.config.autoScrape) "On" else "Off",
                    Type.labelStrong,
                    if (vm.config.autoScrape) Palette.ok else Palette.inkSecondary,
                )
            }
        },
        Item("redo", onA = { vm.scrapeArt(force = true) }, aLabel = "Re-download") { focused ->
            ListRow(focused, accent, background = Palette.surface) {
                Txt("Re-download all art and logos", Type.rowTitle, Palette.ink, Modifier.weight(1f))
                Txt("Picks up newly added art", Type.label, Palette.inkSecondary)
            }
        },
        Item("sources") { _ ->
            ListRow(false, accent, background = Palette.surface) {
                Txt(
                    "Box art and screenshots: libretro-thumbnails. Game info: libretro-database. " +
                        "Console logos: RetroPie Carbon and ES-DE Slate themes. Matched by file name, " +
                        "so No-Intro/Redump names work best.",
                    Type.label, Palette.inkBody, maxLines = 3,
                )
            }
        },
    )
}

@Composable
private fun retroAchievementsItems(vm: LauncherViewModel, accent: Color): List<Item> {
    val r = vm.retroAchievements
    fun field(key: String, label: String, value: String, secret: Boolean, field: TextField) =
        Item(key, onA = { vm.editRetroAchievements(field) }, aLabel = "Edit") { focused ->
            ListRow(focused, accent) {
                Txt(label, Type.rowTitle, Palette.ink, Modifier.weight(1f))
                Txt(
                    when {
                        value.isEmpty() -> "Not set"
                        secret -> "••••••••"
                        else -> value
                    },
                    Type.label, Palette.inkSecondary,
                )
                ChevronIcon(Palette.inkSecondary)
            }
        }
    return listOf(
        field("rauser", "Username", r.user, secret = false, TextField.RaUser),
        field("rakey", "Web API key", r.key, secret = true, TextField.RaKey),
        Item("ratest", onA = { vm.refreshRetroAchievements(userInitiated = true) }, aLabel = "Check") { focused ->
            ListRow(focused, accent) {
                Column(Modifier.weight(1f)) {
                    Txt("Check sign-in and refresh", Type.rowTitle, Palette.ink)
                    Txt(
                        vm.raStatus ?: if (r.complete) "Refreshes by itself at start-up and after each game" else "Not signed in",
                        Type.label, Palette.inkSecondary, maxLines = 2,
                    )
                }
            }
        },
        Item("raabout") { _ ->
            ListRow(false, accent, background = Palette.surface) {
                Txt(
                    "Use your free retroachievements.org account. The web API key is on the site under " +
                        "Settings › Keys. Turn on RetroAchievements in RetroArch (or your emulator) too, " +
                        "with the same account, so unlocks are recorded while you play.",
                    Type.label, Palette.inkSecondary, maxLines = 4,
                )
            }
        },
    )
}

@Composable
private fun screenScraperItems(vm: LauncherViewModel, accent: Color): List<Item> {
    val c = vm.screenScraper
    fun field(key: String, label: String, value: String, secret: Boolean, field: TextField) =
        Item(key, onA = { vm.editScreenScraper(field) }, aLabel = "Edit") { focused ->
            ListRow(focused, accent) {
                Txt(label, Type.rowTitle, Palette.ink, Modifier.weight(1f))
                Txt(
                    when {
                        value.isEmpty() -> "Not set"
                        secret -> "••••••••"
                        else -> value
                    },
                    Type.label, Palette.inkSecondary,
                )
                ChevronIcon(Palette.inkSecondary)
            }
        }
    return listOf(
        field("user", "Username", c.user, secret = false, TextField.SsUser),
        field("password", "Password", c.password, secret = true, TextField.SsPassword),
        field("devid", "Developer ID", c.devId, secret = false, TextField.SsDevId),
        field("devpassword", "Developer password", c.devPassword, secret = true, TextField.SsDevPassword),
        Item("test", onA = { vm.testScreenScraper() }, aLabel = "Check") { focused ->
            ListRow(focused, accent) {
                Column(Modifier.weight(1f)) {
                    Txt("Check sign-in", Type.rowTitle, Palette.ink)
                    Txt(
                        vm.screenScraperStatus ?: if (c.complete) "Ready. Run Scraper › Download missing art." else "Not signed in",
                        Type.label, Palette.inkSecondary, maxLines = 2,
                    )
                }
            }
        },
        Item("about") { _ ->
            ListRow(false, accent, background = Palette.surface) {
                Txt(
                    "ScreenScraper needs your free account (screenscraper.fr) and a developer ID, which apps " +
                        "must request on the ScreenScraper forum. Credentials stay on this device.",
                    Type.label, Palette.inkBody, maxLines = 3,
                )
            }
        },
    )
}

@Composable
private fun appearanceItems(vm: LauncherViewModel, accent: Color, importArt: () -> Unit): List<Item> {
    val c = vm.config
    return listOf(
        Item("backdrop", onA = { vm.cycleBackdrop() }, aLabel = "Change") { focused ->
            ListRow(focused, accent) {
                Column(Modifier.weight(1f)) {
                    Txt("Console backdrop", Type.rowTitle, Palette.ink)
                    Txt(
                        when (c.backdrop) {
                            "logo" -> "The console's logo as a spinning 3D neon sign"
                            "cover" -> "The best-selling game's cover, over a blur of itself"
                            "console" -> "The console on its own"
                            else -> "Your own pictures first (add them below), then game fanart, then the 3D logo"
                        },
                        Type.label, Palette.inkSecondary, maxLines = 2,
                    )
                }
                Txt(
                    when (c.backdrop) {
                        "logo" -> "3D logo"
                        "cover" -> "Best-seller"
                        "console" -> "Console"
                        else -> "Your pictures"
                    },
                    Type.labelStrong, Palette.inkSecondary,
                )
            }
        },
        toggle("era", "Era skins", "Game lists dress up as their console: Game Boy green, Virtual Boy red, NES, PlayStation, Dreamcast", c.eraSkins, accent) {
            vm.setEraSkins(!c.eraSkins)
        },
        Item(
            "custom", onA = importArt, aLabel = "Import",
            onX = { vm.clearImported(MediaKind.Art) }, xLabel = "Clear imported",
        ) { focused ->
            ListRow(focused, accent) {
                Column(Modifier.weight(1f)) {
                    Txt("Your own pictures", Type.rowTitle, Palette.ink)
                    Txt(
                        "Import pictures or a .zip from Downloads, named after each system (snes.jpg, ps2.png…). " +
                            "Or put them in an \"_art\" folder in your ROMs folder and rescan.",
                        Type.label, Palette.inkSecondary, maxLines = 3,
                    )
                }
                val n = vm.systemCustom.size
                Txt(if (n == 0) "None" else "$n in use", Type.labelStrong, if (n > 0) Palette.ok else Palette.inkSecondary)
            }
        },
        Item("blur", onA = { vm.cycleBackdropBlur() }, aLabel = "Change") { focused ->
            ListRow(focused, accent) {
                Txt("Blur behind covers", Type.rowTitle, Palette.ink, Modifier.weight(1f))
                Txt(
                    when (c.backdropBlur) {
                        0 -> "Off"
                        1 -> "Soft"
                        else -> "Strong"
                    },
                    Type.labelStrong, Palette.inkSecondary,
                )
            }
        },
        Item("videos", onA = { vm.setConsoleVideos(!c.consoleVideos) }, aLabel = "Toggle") { focused ->
            ListRow(focused, accent) {
                Column(Modifier.weight(1f)) {
                    Txt("Gameplay videos on the console screen", Type.rowTitle, Palette.ink)
                    Txt(
                        "After a few seconds on a console, plays clips of its games (from ScreenScraper or ES-DE)",
                        Type.label, Palette.inkSecondary, maxLines = 2,
                    )
                }
                Txt(if (c.consoleVideos) "On" else "Off", Type.labelStrong, if (c.consoleVideos) Palette.ok else Palette.inkSecondary)
            }
        },
        toggle("livesky", "Live sky", "The sky follows the clock, the grid rolls, shooting stars at night", c.liveSky, accent) {
            vm.setLiveSky(!c.liveSky)
        },
        toggle("neon", "Console colours", "Each console lights the screen in its own neon", c.consoleNeon, accent) {
            vm.setConsoleNeon(!c.consoleNeon)
        },
        Item("credits") { _ ->
            ListRow(false, accent, background = Palette.surface) {
                Column(Modifier.weight(1f)) {
                    Txt("UNLOCK SHOP", Type.hint, Palette.sun1, glow = Palette.sun2)
                    Txt("Earn a credit per game you start and per 10 minutes played", Type.label, Palette.inkSecondary)
                }
                Txt("CREDIT ${vm.credits()}", Type.crt, Palette.sun1, glow = Palette.sun2)
            }
        },
        *Unlock.entries.map { unlock ->
            val owned = vm.owns(unlock)
            val on = if (unlock == Unlock.Gold) c.theme == "gold" else c.scene == unlock.scene
            Item("unlock-${unlock.key}", onA = { vm.buy(unlock) }, aLabel = if (owned) (if (on) "Switch off" else "Use") else "Buy") { focused ->
                ListRow(focused, accent) {
                    Column(Modifier.weight(1f)) {
                        Txt(unlock.title, Type.rowTitle, if (owned) Palette.ink else Palette.inkSecondary)
                        Txt(if (unlock == Unlock.Gold) "Everything in gold neon" else "Backdrop scene", Type.label, Palette.inkSecondary)
                    }
                    Txt(
                        when {
                            on -> "On"
                            owned -> "Owned"
                            else -> "${unlock.cost} CREDITS"
                        },
                        Type.labelStrong, if (on) Palette.ok else if (owned) Palette.inkSecondary else Palette.sun1,
                    )
                }
            }
        }.toTypedArray(),
        Item("crt", onA = { vm.setCrtEffect(!c.crtEffect) }, aLabel = "Toggle") { focused ->
            ListRow(focused, accent) {
                Column(Modifier.weight(1f)) {
                    Txt("CRT scanlines", Type.rowTitle, Palette.ink)
                    Txt("Faint scanlines and darker corners, like an arcade screen", Type.label, Palette.inkSecondary)
                }
                Txt(if (c.crtEffect) "On" else "Off", Type.labelStrong, if (c.crtEffect) Palette.ok else Palette.inkSecondary)
            }
        },
        Item("tint", onA = { vm.setBackdropTint(!c.backdropTint) }, aLabel = "Toggle") { focused ->
            ListRow(focused, accent) {
                Column(Modifier.weight(1f)) {
                    Txt("Console colour tint", Type.rowTitle, Palette.ink)
                    Txt("Shade backdrops with each system's colour", Type.label, Palette.inkSecondary)
                }
                Txt(if (c.backdropTint) "On" else "Off", Type.labelStrong, if (c.backdropTint) Palette.ok else Palette.inkSecondary)
            }
        },
    )
}

/** One row per music track: A switches it off or on, X plays it on repeat. */
private fun trackItems(vm: LauncherViewModel, accent: Color): List<Item> {
    val music = vm.music ?: return emptyList()
    return music.tracks.mapIndexed { i, track ->
        Item(
            "track-${track.key}",
            onA = { vm.toggleTrack(track.key) }, aLabel = "On/Off",
            onX = { vm.toggleRepeat(track.key) }, xLabel = "Repeat",
        ) { focused ->
            val off = track.key in vm.config.musicOff
            val repeating = vm.config.musicRepeat == track.key
            ListRow(focused, accent) {
                Column(Modifier.weight(1f)) {
                    Txt("BGM ${(i + 1).toString().padStart(2, '0')} · ${track.title}", Type.rowTitle, if (off) Palette.inkSecondary else Palette.ink)
                    Txt(
                        when {
                            music.nowPlaying == track.key && vm.config.music -> "Playing now"
                            repeating -> "Plays on repeat"
                            else -> "Track"
                        },
                        Type.label, Palette.inkSecondary,
                    )
                }
                Txt(
                    when {
                        repeating -> "Repeat"
                        off -> "Off"
                        else -> "On"
                    },
                    Type.labelStrong, if (off) Palette.inkSecondary else Palette.ok,
                )
            }
        }
    }
}

private fun soundItems(vm: LauncherViewModel, accent: Color, importSounds: () -> Unit): List<Item> {
    val on = vm.config.navigationSounds
    return listOf(
        Item("sounds", onA = { vm.setNavigationSounds(!on) }, aLabel = "Toggle") { focused ->
            ListRow(focused, accent) {
                Txt("Navigation sounds", Type.rowTitle, Palette.ink, Modifier.weight(1f))
                Txt(if (on) "On" else "Off", Type.labelStrong, if (on) Palette.ok else Palette.inkSecondary)
            }
        },
        Item("consolesounds", onA = { vm.setConsoleSounds(!vm.config.consoleSounds) }, aLabel = "Toggle") { focused ->
            ListRow(focused, accent) {
                Column(Modifier.weight(1f)) {
                    Txt("Console startup sounds", Type.rowTitle, Palette.ink)
                    val n = vm.systemSounds.size
                    Txt(
                        if (n == 0) "None added yet" else "$n ${if (n == 1) "console has" else "consoles have"} a sound",
                        Type.label, Palette.inkSecondary,
                    )
                }
                val c = vm.config.consoleSounds
                Txt(if (c) "On" else "Off", Type.labelStrong, if (c) Palette.ok else Palette.inkSecondary)
            }
        },
        Item(
            "importsounds", onA = importSounds, aLabel = "Import",
            onX = { vm.clearImported(MediaKind.Sounds) }, xLabel = "Clear imported",
        ) { focused ->
            ListRow(focused, accent) {
                Column(Modifier.weight(1f)) {
                    Txt("Add startup sounds", Type.rowTitle, Palette.ink)
                    Txt(
                        "Import sound files or a .zip named after each system (snes.ogg, ps2.mp3…). " +
                            "Or put them in a \"_sounds\" folder in your ROMs folder and rescan.",
                        Type.label, Palette.inkSecondary, maxLines = 3,
                    )
                }
                ChevronIcon(Palette.inkSecondary)
            }
        },
        Item("music", onA = { vm.setMusic(!vm.config.music) }, aLabel = "Toggle") { focused ->
            ListRow(focused, accent) {
                Column(Modifier.weight(1f)) {
                    Txt("Background music", Type.rowTitle, Palette.ink)
                    val (count, own) = vm.music?.source ?: (0 to false)
                    Txt(
                        if (own) "$count tracks from your music folder"
                        else "Everest's own tracks · or add MP3s to a \"music\" folder in your ROMs folder",
                        Type.label, Palette.inkSecondary, maxLines = 2,
                    )
                }
                Txt(if (vm.config.music) "On" else "Off", Type.labelStrong, if (vm.config.music) Palette.ok else Palette.inkSecondary)
            }
        },
        Item("musicvolume", onAdjust = vm::stepMusicLevel) { focused ->
            ListRow(focused, accent) {
                Txt("Music volume", Type.rowTitle, Palette.ink)
                Slider(vm.config.musicLevel / 100f, Modifier.weight(1f))
                Txt("${vm.config.musicLevel}%", Type.crt, Palette.cyanSoft, Modifier.width(Size.SLIDER_VALUE_W.u), glow = Palette.cyan)
            }
        },
        Item("skip", onA = { vm.music?.skip() }, aLabel = "Skip") { focused ->
            ListRow(focused, accent, background = Palette.surface) {
                Txt("Next track", Type.rowTitle, Palette.ink, Modifier.weight(1f))
                ChevronIcon(Palette.inkSecondary)
            }
        },
        *trackItems(vm, accent).toTypedArray(),
        Item("videosound", onA = { vm.setVideoSound(!vm.config.videoSound) }, aLabel = "Toggle") { focused ->
            ListRow(focused, accent) {
                Txt("Gameplay video sound", Type.rowTitle, Palette.ink, Modifier.weight(1f))
                val video = vm.config.videoSound
                Txt(if (video) "On" else "Off", Type.labelStrong, if (video) Palette.ok else Palette.inkSecondary)
            }
        },
        Item("volume", onA = { vm.cycleSoundVolume() }, aLabel = "Change") { focused ->
            ListRow(focused, accent) {
                Txt("Volume", Type.rowTitle, Palette.ink, Modifier.weight(1f))
                Txt(
                    when (vm.config.soundVolume) {
                        1 -> "Low"
                        3 -> "High"
                        else -> "Medium"
                    },
                    Type.labelStrong, Palette.inkSecondary,
                )
            }
        },
        Item("soundtest") { _ ->
            ListRow(false, accent, background = Palette.surface) {
                Txt("SOUND TEST", Type.hint, Palette.sun1, Modifier.weight(1f), glow = Palette.sun2)
                Txt("Ⓐ plays · BGM above, X repeats", Type.label, Palette.inkSecondary)
            }
        },
    ) + UiSound.entries.mapIndexed { i, sound ->
        Item("se-${sound.name}", onA = { vm.testSound(sound) }, aLabel = "Play") { focused ->
            ListRow(focused, accent) {
                Txt("SE ${(i + 1).toString().padStart(2, '0')}", Type.crt, Palette.cyanSoft, Modifier.width(Size.SE_NUM_W.u), glow = Palette.cyan)
                Txt(SOUND_NAMES[sound] ?: sound.name, Type.rowTitle, Palette.ink, Modifier.weight(1f))
                ChevronIcon(Palette.inkSecondary)
            }
        }
    }
}

private val SOUND_NAMES = mapOf(
    UiSound.Move to "Cursor", UiSound.Select to "Select", UiSound.Back to "Cancel", UiSound.Tab to "Page turn",
    UiSound.Launch to "Insert coin", UiSound.Error to "Buzzer", UiSound.Achievement to "Achievement",
    UiSound.Secret to "Secret found", UiSound.Challenger to "New challenger", UiSound.OneUp to "1UP",
    UiSound.BootGb to "Handheld ding", UiSound.BootPs to "Disc console chime", UiSound.BootGc to "Cube jingle",
    UiSound.BootDc to "Swirl chime",
    UiSound.BootGba to "Handheld sweep", UiSound.BootGbc to "Colour drop", UiSound.BootMd to "16-bit fanfare",
    UiSound.BootThreeDs to "Depth chime", UiSound.BootN64 to "Block tumble", UiSound.BootNds to "Dual-screen ding",
    UiSound.BootNes to "Title card", UiSound.BootSnes to "Super chord", UiSound.BootSwitch to "Click",
    UiSound.BootPs2 to "Tower drone", UiSound.BootPsp to "Light wave", UiSound.BootWii to "Channel chime",
)

@Composable
private fun controlsItems(vm: LauncherViewModel, accent: Color): List<Item> {
    val context = LocalContext.current
    val cfg = vm.config
    return listOf(
        toggle("launchfx", "Insert-coin launch", "Coin sound and a CRT switch-off when a game starts", cfg.launchEffects, accent) {
            vm.setLaunchEffects(!cfg.launchEffects)
        },
        toggle("boot", "Boot check", "Arcade BIOS lines before the logo when Everest starts", cfg.bootSequence, accent) {
            vm.setBootSequence(!cfg.bootSequence)
        },
        toggle("pressstart", "Press Start screen", "The loading screen waits for Start before the console screen", cfg.pressStart, accent) {
            vm.setPressStart(!cfg.pressStart)
        },
        Item("tributes", onA = { vm.cycleBootTributes() }, aLabel = "Change") { focused ->
            ListRow(focused, accent) {
                Column(Modifier.weight(1f)) {
                    Txt("Start-up tributes", Type.rowTitle, Palette.ink)
                    Txt("A nod to each console's power-on screen when you open it", Type.label, Palette.inkSecondary, maxLines = 2)
                }
                Txt(
                    when (cfg.bootTributes) {
                        "always" -> "Every time"
                        "off" -> "Off"
                        else -> "Once a day"
                    },
                    Type.labelStrong, Palette.ink,
                )
            }
        },
        toggle("mascot", "Yeti-1", "The pixel mascot walking along the bottom of the screen", cfg.mascot, accent) {
            vm.setMascot(!cfg.mascot)
        },
        Item("initials", onA = { vm.editInitials() }, aLabel = "Edit") { focused ->
            ListRow(focused, accent) {
                Column(Modifier.weight(1f)) {
                    Txt("Your initials", Type.rowTitle, Palette.ink)
                    Txt("Shown on the high-score table in attract mode", Type.label, Palette.inkSecondary)
                }
                Txt(cfg.initials, Type.crt, Palette.sun1, glow = Palette.sun2)
            }
        },
        Item("swap", onA = { vm.setSwapButtons(!cfg.swapButtons) }, aLabel = "Toggle") { focused ->
            ListRow(focused, accent) {
                Column(Modifier.weight(1f)) {
                    Txt("Confirm button", Type.rowTitle, Palette.ink)
                    Txt("Which face button selects; the opposite one goes back", Type.label, Palette.inkSecondary)
                }
                Txt(if (cfg.swapButtons) "Right" else "Bottom", Type.labelStrong, Palette.ink)
            }
        },
        Item("labels", onA = { vm.setButtonLabels(if (cfg.buttonLabels == "playstation") "letters" else "playstation") }, aLabel = "Toggle") { focused ->
            ListRow(focused, accent) {
                Column(Modifier.weight(1f)) {
                    Txt("Button labels", Type.rowTitle, Palette.ink)
                    Txt("What the hints at the bottom show", Type.label, Palette.inkSecondary)
                }
                Txt(if (cfg.buttonLabels == "playstation") "✕ ○ □ △" else "A B X Y", Type.labelStrong, Palette.ink)
            }
        },
        Item("screensaver", onA = { vm.cycleScreensaver() }, aLabel = "Change") { focused ->
            ListRow(focused, accent) {
                Column(Modifier.weight(1f)) {
                    Txt("Screensaver", Type.rowTitle, Palette.ink)
                    Txt("Game art drifts by when Everest is left alone", Type.label, Palette.inkSecondary)
                }
                Txt(if (cfg.screensaverMinutes == 0) "Off" else "After ${cfg.screensaverMinutes} min", Type.labelStrong, Palette.ink)
            }
        },
        Item("home", onA = {
            runCatching {
                context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }, aLabel = "Open") { focused ->
            ListRow(focused, accent) {
                Column(Modifier.weight(1f)) {
                    Txt("Use Everest as the home screen", Type.rowTitle, Palette.ink)
                    Txt("Pick Everest in Android's Home app setting; the Home button then opens it", Type.label, Palette.inkSecondary, maxLines = 2)
                }
                ChevronIcon(Palette.inkSecondary)
            }
        },
    )
}

@Composable
private fun statsItems(vm: LauncherViewModel, accent: Color): List<Item> {
    val summary = vm.playSummary()
    fun stat(key: String, label: String, value: String) = Item(key) { _ ->
        ListRow(false, accent) {
            Txt(label, Type.rowTitle, Palette.ink, Modifier.weight(1f))
            Txt(value, Type.labelStrong, Palette.ink)
        }
    }
    val rpg = vm.rpgStatus()
    return listOf(
        Item("rpg") { _ -> RpgSheet(rpg) },
        stat("total", "Total play time", if (summary.totalMs > 0) formatPlaytime(summary.totalMs) else "None yet"),
        stat("games", "Games played", summary.gamesPlayed.toString()),
        stat("launches", "Launches", summary.launches.toString()),
        stat("streak", "Current streak", "${summary.currentStreak} ${if (summary.currentStreak == 1) "day" else "days"}"),
        stat("best", "Longest streak", "${summary.longestStreak} ${if (summary.longestStreak == 1) "day" else "days"}"),
        *vm.raSummary?.let { ra ->
            arrayOf(stat("ra", "RetroAchievements points", ra.points.toString() + (ra.rank?.let { " · rank #$it" } ?: ""))) +
                ra.recent.mapIndexed { i, u -> stat("ra-$i", "🏆 ${u.title}", "${u.game} · ${u.points} pts") }
        }.orEmpty(),
        vm.statusCounts().let { counts ->
            stat(
                "progress", "Beaten · 100% · Playing · Backlog",
                listOf(Progress.BEATEN, Progress.COMPLETED, Progress.PLAYING, Progress.BACKLOG)
                    .joinToString(" · ") { (counts[it] ?: 0).toString() },
            )
        },
    ) + summary.top.mapIndexed { i, (game, s) ->
        Item("top-$i") { _ ->
            ListRow(false, accent, background = Palette.surface) {
                Txt("${i + 1}. ${game.title}", Type.rowTitle, Palette.ink, Modifier.weight(1f))
                Txt("${formatPlaytime(s.playtimeMs)} · ${timesPlayed(s.playCount)}", Type.label, Palette.inkSecondary)
            }
        }
    }
}

@Composable
private fun aboutItems(vm: LauncherViewModel, accent: Color): List<Item> {
    return listOf(
        Item("version", onA = { vm.checkForUpdate(userInitiated = true) }, aLabel = "Check for updates") { focused ->
            ListRow(focused, accent) {
                Column(Modifier.weight(1f)) {
                    Txt("Check for updates", Type.rowTitle, Palette.ink)
                    Txt("Also checks automatically every time the launcher starts", Type.label, Palette.inkSecondary)
                }
                Txt("Build ${vm.currentVersion}", Type.label, Palette.inkSecondary)
            }
        },
        Item("config", onA = { vm.reloadConfig() }, aLabel = "Reload") { focused ->
            ListRow(focused, accent) {
                Column(Modifier.weight(1f)) {
                    Txt("Reload config file", Type.rowTitle, Palette.ink)
                    Txt(vm.configPath, Type.label, Palette.inkSecondary, maxLines = 2)
                }
            }
        },
        Item("esde") { _ ->
            ListRow(false, accent, background = Palette.surface) {
                Txt(
                    "Emulator launch commands come from ES-DE's Android es_systems.xml and es_find_rules.xml " +
                        "(MIT License, © Northwestern Software AB, Leon Styhre, Alec Lofquist).",
                    Type.label, Palette.inkBody, maxLines = 3,
                )
            }
        },
        Item("font") { _ ->
            ListRow(false, accent, background = Palette.surface) {
                Txt(
                    "Press Start 2P, VT323 and Chakra Petch (SIL Open Font License 1.1).",
                    Type.label, Palette.inkBody, maxLines = 2,
                )
            }
        },
        Item("credits", onA = { vm.creditsRoll = true }, aLabel = "Roll") { focused ->
            ListRow(focused, accent) {
                Column(Modifier.weight(1f)) {
                    Txt("Credits", Type.rowTitle, Palette.ink)
                    Txt("Roll the end credits", Type.label, Palette.inkSecondary)
                }
                ChevronIcon(Palette.inkSecondary)
            }
        },
    )
}

/** Stats as an RPG status screen: name, level, HP, an EXP bar to the next credit, gold and items. */
@Composable
private fun RpgSheet(rpg: RpgStatus) {
    Column(
        Modifier
            .fillMaxWidth()
            .neonPanel(Radius.CARD, edge = Palette.sun1, corner = Palette.cyan, fill = Palette.glassStrong)
            .padding(Space.LG.u),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Txt(rpg.name, Type.screenTitle, Palette.sun1, Modifier.weight(1f), glow = Palette.sun2)
            Txt("LV ${rpg.level}", Type.screenTitleSmall, Palette.ink)
        }
        Spacer(Modifier.height(Space.MD.u))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.SECTION.u)) {
            Txt("HP ${rpg.hours}", Type.crt, Palette.ok)
            Txt("GOLD ${rpg.gold}G", Type.crt, Palette.sun1)
            Txt("ITEMS ${rpg.items}", Type.crt, Palette.cyanSoft)
        }
        Spacer(Modifier.height(Space.MD.u))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Txt("EXP", Type.hint, Palette.inkSecondary)
            Spacer(Modifier.width(Space.MD.u))
            Slider(rpg.exp, Modifier.weight(1f))
            Spacer(Modifier.width(Space.MD.u))
            Txt("NEXT CREDIT", Type.badge, Palette.inkSecondary)
        }
    }
}

/** Settings sit over a dimmed synthwave scene so long lists stay easy to read. */
private const val SETTINGS_DIM = 0.6f
private const val SIDEBAR_FILL = 0.85f

/** Neon slider: a dark track, a pink-to-yellow fill and a glowing knob. Left/Right move it. */
@Composable
private fun Slider(value: Float, modifier: Modifier = Modifier) {
    val knob = Size.SLIDER_KNOB.u
    val track = Size.SLIDER_TRACK.u
    Canvas(modifier.height(knob)) {
        val v = value.coerceIn(0f, 1f)
        val r = knob.toPx() / 2
        val y = size.height / 2
        val left = r
        val right = size.width - r
        val t = track.toPx()
        val x = left + (right - left) * v
        drawRoundRect(Palette.surface, Offset(left, y - t / 2), GeoSize(right - left, t), CornerRadius(t / 2))
        drawRoundRect(
            Brush.horizontalGradient(listOf(Palette.pink, Palette.sun1), startX = left, endX = right),
            Offset(left, y - t / 2), GeoSize(x - left, t), CornerRadius(t / 2),
        )
        drawCircle(Palette.pink.copy(alpha = 0.35f), r * 1.6f, Offset(x, y))
        drawCircle(Palette.white, r, Offset(x, y))
    }
}

/** An on/off row: title, a line of explanation, and On/Off on the right. */
private fun toggle(key: String, title: String, detail: String, on: Boolean, accent: Color, flip: () -> Unit) =
    Item(key, onA = flip, aLabel = "Toggle") { focused ->
        ListRow(focused, accent) {
            Column(Modifier.weight(1f)) {
                Txt(title, Type.rowTitle, Palette.ink)
                Txt(detail, Type.label, Palette.inkSecondary, maxLines = 2)
            }
            Txt(if (on) "On" else "Off", Type.labelStrong, if (on) Palette.ok else Palette.inkSecondary)
        }
    }
