package io.github.hipsterbwp.rp5launcher.ui.screens

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
import io.github.hipsterbwp.rp5launcher.ui.LauncherViewModel
import io.github.hipsterbwp.rp5launcher.ui.ListRow
import io.github.hipsterbwp.rp5launcher.ui.Pad
import io.github.hipsterbwp.rp5launcher.ui.PadInput
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

private enum class Page(val title: String) { Library("Library"), Emulators("Emulators"), About("About") }

/** One actionable row on a settings page. */
private data class Item(
    val key: String,
    val onA: (() -> Unit)? = null,
    val aLabel: String? = null,
    val onX: (() -> Unit)? = null,
    val xLabel: String? = null,
    val content: @Composable (focused: Boolean) -> Unit,
)

@Composable
fun SettingsScreen(vm: LauncherViewModel, pickFolder: () -> Unit) {
    var page by rememberSaveable { mutableStateOf(Page.Emulators) }
    var inContent by rememberSaveable { mutableStateOf(false) }
    var row by rememberSaveable { mutableIntStateOf(0) }
    val accentTarget = vm.systems.getOrNull(vm.homeSystemIndex)?.color ?: SystemColors.neutral
    val accent by animateColorAsState(accentTarget, Motion.colorFade(), label = "settings-accent")
    val version = vm.emulatorsVersion

    val items: List<Item> = when (page) {
        Page.Library -> libraryItems(vm, accent, pickFolder)
        Page.Emulators -> emulatorItems(vm, accent, version)
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
                Pad.Left, Pad.B -> { inContent = false; true }
                Pad.A -> { focusedItem?.onA?.invoke(); true }
                Pad.X -> { focusedItem?.onX?.invoke(); true }
                else -> false
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Palette.page)) {
        Row(Modifier.fillMaxSize()) {
            // Sidebar in the system color.
            Column(
                Modifier
                    .width(Size.SETTINGS_SIDEBAR.u)
                    .fillMaxHeight()
                    .background(accent)
                    .padding(top = Space.SETTINGS_TOP.u, start = Space.SIDEBAR_H.u, end = Space.SIDEBAR_H.u),
                verticalArrangement = Arrangement.spacedBy(Space.LIST_GAP.u),
            ) {
                Txt("Settings", Type.screenTitle, Palette.white, Modifier.padding(start = Space.ROW_GAP.u, bottom = Space.TILE_GAP.u))
                Page.entries.forEach { p ->
                    val selected = p == page
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(rounded(Radius.CARD_SMALL))
                            .background(if (selected) Palette.white else Palette.transparent)
                            .tap { page = p; row = 0; inContent = false }
                            .padding(vertical = Space.ROW_V.u, horizontal = Space.ROW_H.u),
                    ) {
                        Txt(
                            p.title,
                            if (selected) Type.navItemSelected else Type.navItem,
                            if (selected) accent else Palette.white.copy(alpha = Alpha.NAV_ON_SYSTEM),
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
                Txt(page.title, Type.screenTitleSmall, Palette.ink)
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
                add(Hint(Pad.B, "Back", onClick = { inContent = false }))
                focusedItem?.xLabel?.let { add(Hint(Pad.X, it, onClick = focusedItem.onX)) }
            }
        }
        HintBar(hints, onSystem = false, glyphInk = Palette.ink)
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
    Page.About -> "Version, config file and credits."
}

@Composable
private fun libraryItems(vm: LauncherViewModel, accent: Color, pickFolder: () -> Unit): List<Item> {
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
    ) + lib?.unknownFolders.orEmpty().takeIf { it.isNotEmpty() }?.let { unknown ->
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
                    "Bricolage Grotesque by The Bricolage Grotesque Project Authors (SIL Open Font License 1.1).",
                    Type.label, Palette.inkBody, maxLines = 2,
                )
            }
        },
    )
}
