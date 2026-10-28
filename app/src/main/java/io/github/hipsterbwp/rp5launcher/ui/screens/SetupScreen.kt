package io.github.hipsterbwp.rp5launcher.ui.screens

import io.github.hipsterbwp.rp5launcher.ui.RetroBackdrop
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import io.github.hipsterbwp.rp5launcher.ui.Dialog
import io.github.hipsterbwp.rp5launcher.ui.Hint
import io.github.hipsterbwp.rp5launcher.ui.HintBar
import io.github.hipsterbwp.rp5launcher.ui.LauncherViewModel
import io.github.hipsterbwp.rp5launcher.ui.ListRow
import io.github.hipsterbwp.rp5launcher.ui.Pad
import io.github.hipsterbwp.rp5launcher.ui.PadInput
import io.github.hipsterbwp.rp5launcher.ui.PrimaryButton
import io.github.hipsterbwp.rp5launcher.ui.Screen
import io.github.hipsterbwp.rp5launcher.ui.SystemBadge
import io.github.hipsterbwp.rp5launcher.ui.SystemEntry
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

private const val STEPS = 3

@Composable
fun SetupScreen(vm: LauncherViewModel, step: Int, appName: String, pickFolder: () -> Unit) {
    // Settings-style screens use Apple blue: system colors are too dark to read as focus on black.
    val accent = Palette.accent

    // Step 2 state: rows are one per system, then the Continue button.
    val systems = vm.systems
    val count = systems.size + 1
    val index = vm.setupIndex.coerceIn(0, count - 1)
    val focusedSystem = if (step == 2) systems.getOrNull(index) else null
    val version = vm.emulatorsVersion
    val missing = remember(version, focusedSystem, vm.config) {
        focusedSystem?.let { s -> vm.selectedCommand(s.name)?.takeIf { !vm.isInstalled(it) } }
    }

    fun goTo(n: Int) {
        vm.backStack[vm.backStack.lastIndex] = Screen.Setup(n)
    }
    fun finish() = vm.finishSetup()
    fun toArt() = goTo(3)
    fun backToStep1() = goTo(1)
    fun backToStep2() = goTo(2)
    fun downloadArt() {
        vm.setAutoScrape(true)
        vm.scrapeArt(force = false)
        finish()
    }
    fun activate() {
        if (focusedSystem != null) vm.dialog = Dialog.EmulatorPicker(focusedSystem.name) else toArt()
    }
    fun getEmulator() {
        missing?.let { vm.suggestedPackage(it) }?.let(vm::openStore)
    }

    PadInput { button, _ ->
        if (step == 1) {
            when (button) {
                Pad.A, Pad.Start -> { pickFolder(); true }
                Pad.B -> true // first run: nowhere to go back to
                else -> false
            }
        } else if (step == 3) {
            when (button) {
                Pad.A -> { downloadArt(); true }
                Pad.Start -> { finish(); true }
                Pad.B -> { backToStep2(); true }
                else -> false
            }
        } else {
            when (button) {
                Pad.Up -> { vm.setupIndex = (index - 1).coerceAtLeast(0); true }
                Pad.Down -> { vm.setupIndex = (index + 1).coerceAtMost(count - 1); true }
                Pad.A -> { activate(); true }
                Pad.X -> { getEmulator(); true }
                Pad.Y -> { pickFolder(); true }
                Pad.Start -> { toArt(); true }
                Pad.B -> { backToStep1(); true }
                else -> false
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Palette.page)) {
        RetroBackdrop()
        Box(Modifier.fillMaxSize().background(Palette.night.copy(alpha = SETUP_DIM)))
        Row(Modifier.fillMaxSize()) {
            SetupPanel(step, accent, appName)
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(top = Space.SETTINGS_TOP.u, start = Space.SCREEN_H.u, end = Space.SCREEN_H.u),
            ) {
                FolderCard(vm.rootLabel() ?: "Not chosen yet", showChange = step == 2, onChange = pickFolder)
                when (step) {
                    1 -> PickStep(accent, pickFolder)
                    2 -> FoundStep(vm, accent, index, version, onFinish = ::toArt)
                    else -> ArtStep(vm, accent, onDownload = ::downloadArt)
                }
            }
        }
        val hints = if (step == 1) {
            listOf(Hint(Pad.A, "Choose folder", onClick = pickFolder))
        } else if (step == 3) {
            listOf(
                Hint(Pad.A, "Download art", onClick = ::downloadArt),
                Hint(Pad.B, "Back", onClick = ::backToStep2),
                Hint(Pad.Start, "Skip", trailing = true, onClick = ::finish),
            )
        } else {
            buildList {
                add(Hint(Pad.A, if (focusedSystem != null) "Pick emulator" else "Continue", onClick = ::activate))
                if (missing != null) add(Hint(Pad.X, "Get emulator", onClick = ::getEmulator))
                add(Hint(Pad.Y, "Change folder", onClick = pickFolder))
                add(Hint(Pad.B, "Back", onClick = ::backToStep1))
                add(Hint(Pad.Start, "Continue", trailing = true, onClick = ::toArt))
            }
        }
        HintBar(hints, onSystem = false, glyphInk = Palette.inkOnLight)
    }
}

@Composable
private fun SetupPanel(step: Int, accent: Color, appName: String) {
    Column(
        Modifier
            .width(Size.SETUP_PANEL.u)
            .fillMaxHeight()
            .background(Palette.sidebar)
            .padding(top = Space.SCREEN_TOP_LIGHT.u, start = Space.PANEL_H.u, end = Space.PANEL_H.u),
    ) {
        Txt(appName, Type.brand, Palette.white)
        Spacer(Modifier.height(Space.XL.u))
        Txt("Step $step of $STEPS", Type.label, Palette.white.copy(alpha = Alpha.SECONDARY_ON_SYSTEM))
        Spacer(Modifier.height(Space.GAP.u))
        Txt(
            when (step) {
                1 -> "Pick your ROMs folder"
                2 -> "We found your games"
                else -> "Get box art"
            },
            Type.setupTitle, Palette.white, maxLines = 3,
        )
        Spacer(Modifier.height(Space.LG.u))
        Txt(
            when (step) {
                1 -> "Choose the folder that holds one subfolder per system, like snes, gba and ps2."
                2 -> "Scanned your ROMs folder and matched each system to an emulator on this device."
                else -> "Covers, screenshots and game info from libretro's free database. " +
                    "It downloads in the background, so you can start playing right away."
            },
            Type.bodySmall, Palette.white.copy(alpha = Alpha.NAV_ON_SYSTEM), maxLines = 5,
        )
        Spacer(Modifier.height(Space.SECTION_LARGE.u))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.LIST_GAP.u)) {
            repeat(STEPS) { i ->
                Box(
                    Modifier
                        .size(Size.STEP_W.u, Size.STEP_H.u)
                        .clip(rounded(Radius.STEP))
                        .background(if (i < step) Palette.white else Palette.onSystemStepInactive),
                )
            }
        }
    }
}

@Composable
private fun FolderCard(label: String, showChange: Boolean, onChange: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(rounded(Radius.CARD))
            .background(Palette.surface)
            .padding(vertical = Space.CARD_PAD.u, horizontal = Space.LG.u),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Txt("ROMS FOLDER", Type.caps, Palette.inkSecondary)
            Spacer(Modifier.height(Space.TINY.u))
            Txt(label, Type.navItemSelected, Palette.ink)
        }
        if (showChange) Txt("Change", Type.labelStrong, Palette.ink, Modifier.tap(onChange))
    }
}

@Composable
private fun PickStep(accent: Color, pickFolder: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = Space.SECTION_SMALL.u)) {
        Txt("Expected layout", Type.labelStrong, Palette.inkSecondary)
        Spacer(Modifier.height(Space.MD.u))
        Column(verticalArrangement = Arrangement.spacedBy(Space.LIST_GAP.u)) {
            listOf("snes" to "Super Nintendo", "gba" to "Game Boy Advance", "ps2" to "PlayStation 2").forEach { (folder, name) ->
                ListRow(focused = false, accent = accent, padV = Space.ROW_V_SMALL) {
                    SystemBadge(folder.uppercase(), SystemColors.forSystem(folder))
                    Txt("ROMs / $folder", Type.rowTitle, Palette.ink, Modifier.weight(1f))
                    Txt(name, Type.label, Palette.inkSecondary)
                }
            }
        }
        Spacer(Modifier.height(Space.SECTION.u))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            PrimaryButton(
                "Choose folder", accent, focused = true,
                height = Size.BUTTON_SMALL, padH = Space.BUTTON_H_SMALL, style = Type.buttonSmall,
                onClick = pickFolder,
            )
        }
    }
}

@Composable
private fun FoundStep(vm: LauncherViewModel, accent: Color, index: Int, version: Int, onFinish: () -> Unit) {
    val systems = vm.systems
    val scanning = vm.scan
    Column(Modifier.fillMaxSize()) {
        val total = systems.sumOf { it.games.size }
        val summary = when {
            scanning != null && scanning.total > 0 -> "Scanning… ${scanning.done} of ${scanning.total} folders"
            scanning != null -> "Scanning…"
            systems.isEmpty() -> "No games found yet. Check the folder layout, or pick another folder."
            else -> "${systems.size} ${if (systems.size == 1) "system" else "systems"} · $total games"
        }
        Spacer(Modifier.height(Space.SECTION_SMALL.u))
        Txt(summary, Type.labelStrong, Palette.inkSecondary, maxLines = 2)
        val unknown = vm.library?.unknownFolders.orEmpty()
        if (unknown.isNotEmpty() && scanning == null) {
            Spacer(Modifier.height(Space.SM.u))
            Txt("Not recognized: ${unknown.joinToString(", ")}", Type.label, Palette.inkSecondary)
        }
        Spacer(Modifier.height(Space.MD.u))

        val state = rememberLazyListState()
        LaunchedEffect(index) { state.bringIntoView(index) }
        val ring = Size.FOCUS_RING_LIGHT + Size.FOCUS_OFFSET
        LazyColumn(
            state = state,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = ring.u, end = ring.u, top = ring.u, bottom = (Size.HINT_BAR + Space.LG).u),
            verticalArrangement = Arrangement.spacedBy(Space.LIST_GAP.u),
        ) {
            itemsIndexed(systems, key = { _, s -> s.name }) { i, system ->
                SystemStatusRow(vm, system, focused = i == index, accent = accent, version = version) {
                    vm.setupIndex = i
                    vm.dialog = Dialog.EmulatorPicker(system.name)
                }
            }
            item(key = "continue") {
                Row(Modifier.fillMaxWidth().padding(top = Space.MD.u), horizontalArrangement = Arrangement.End) {
                    PrimaryButton(
                        "Continue", accent, focused = index == systems.size,
                        height = Size.BUTTON_SMALL, padH = Space.BUTTON_H_SMALL, style = Type.buttonSmall,
                        onClick = onFinish,
                    )
                }
            }
        }
    }
}

/** System row with its emulator match: green when installed, red with a fix when not. */
@Composable
fun SystemStatusRow(
    vm: LauncherViewModel,
    system: SystemEntry,
    focused: Boolean,
    accent: Color,
    version: Int,
    showCount: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    val command = remember(version, vm.config, system.name) { vm.selectedCommand(system.name) }
    val installed = remember(version, command) { command?.let(vm::isInstalled) ?: false }
    ListRow(focused = focused, accent = accent, padV = Space.ROW_V_SMALL, onClick = onClick) {
        SystemBadge(system.info.shortName, system.color)
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Txt(system.info.displayName, Type.rowTitle, Palette.ink, Modifier.weight(1f, fill = false))
            if (showCount) Txt(" · ${system.games.size}", Type.bodySmall, Palette.inkSecondary)
        }
        val status = when {
            command == null -> "No emulator"
            installed -> command.displayName
            else -> "${command.shortName} missing · Get it"
        }
        Txt(status, Type.labelStrong, if (installed) Palette.ok else Palette.problem)
        trailing?.invoke()
    }
}

@Composable
private fun ArtStep(vm: LauncherViewModel, accent: Color, onDownload: () -> Unit) {
    val todo = vm.scrapeTodo(force = false)
    Column(Modifier.fillMaxWidth().padding(top = Space.SECTION_SMALL.u)) {
        Txt(
            if (todo > 0) "$todo games can get art" else "No supported games to scrape yet",
            Type.labelStrong, Palette.inkSecondary,
        )
        Spacer(Modifier.height(Space.MD.u))
        Column(verticalArrangement = Arrangement.spacedBy(Space.LIST_GAP.u)) {
            listOf(
                "Box art" to "Covers on every game card",
                "Screenshots" to "On Favorites and Recent",
                "Game info" to "Year, publisher and genre",
                "Console logos" to "On the system tiles",
            ).forEach { (what, where) ->
                ListRow(focused = false, accent = accent, padV = Space.ROW_V) {
                    Txt(what, Type.rowTitle, Palette.ink, Modifier.weight(1f))
                    Txt(where, Type.label, Palette.inkSecondary)
                }
            }
        }
        Spacer(Modifier.height(Space.SECTION.u))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            PrimaryButton(
                "Download art", accent, focused = true,
                height = Size.BUTTON_SMALL, padH = Space.BUTTON_H_SMALL, style = Type.buttonSmall,
                onClick = onDownload,
            )
        }
    }
}

private const val SETUP_DIM = 0.55f
