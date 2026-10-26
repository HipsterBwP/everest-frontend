package io.github.hipsterbwp.rp5launcher.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import io.github.hipsterbwp.rp5launcher.ui.glass
import io.github.hipsterbwp.rp5launcher.ui.lightFocus
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.draw.clip
import io.github.hipsterbwp.rp5launcher.ui.Dialog
import io.github.hipsterbwp.rp5launcher.ui.Hint
import io.github.hipsterbwp.rp5launcher.ui.HintBar
import io.github.hipsterbwp.rp5launcher.ui.LauncherViewModel
import io.github.hipsterbwp.rp5launcher.ui.ListRow
import io.github.hipsterbwp.rp5launcher.ui.Pad
import io.github.hipsterbwp.rp5launcher.ui.PadInput
import io.github.hipsterbwp.rp5launcher.ui.Txt
import io.github.hipsterbwp.rp5launcher.ui.UpdateState
import io.github.hipsterbwp.rp5launcher.ui.bringIntoView
import io.github.hipsterbwp.rp5launcher.ui.tap
import io.github.hipsterbwp.rp5launcher.ui.theme.Motion
import io.github.hipsterbwp.rp5launcher.ui.theme.Palette
import io.github.hipsterbwp.rp5launcher.ui.theme.Radius
import io.github.hipsterbwp.rp5launcher.ui.theme.Size
import io.github.hipsterbwp.rp5launcher.ui.theme.Space
import io.github.hipsterbwp.rp5launcher.ui.theme.SystemColors
import io.github.hipsterbwp.rp5launcher.ui.theme.Type
import io.github.hipsterbwp.rp5launcher.ui.theme.rounded
import io.github.hipsterbwp.rp5launcher.ui.theme.u

@Composable
fun DialogHost(vm: LauncherViewModel) {
    when (val d = vm.dialog) {
        is Dialog.EmulatorPicker -> EmulatorPicker(vm, d)
        is Dialog.LaunchError -> LaunchErrorDialog(vm, d)
        is Dialog.Message -> MessageDialog(vm, d)
        Dialog.Update -> UpdateDialog(vm)
        is Dialog.TextInput -> TextInputDialog(vm, d)
        null -> Unit
    }
}

/** Scrim + centered light panel + the dialog's own hint bar. */
@Composable
private fun DialogFrame(hints: List<Hint>, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(Palette.scrim).tap { }) {
        Box(Modifier.fillMaxSize().padding(bottom = Size.HINT_BAR.u), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .width(Size.DIALOG_W.u)
                    .glass(Radius.PANEL, Palette.glassStrong)
                    .padding(Space.SECTION_LARGE.u),
            ) { content() }
        }
        HintBar(hints, onSystem = false, glyphInk = Palette.inkOnLight)
    }
}

@Composable
private fun EmulatorPicker(vm: LauncherViewModel, d: Dialog.EmulatorPicker) {
    val system = vm.system(d.system)
    val accent = system?.color ?: SystemColors.forSystem(d.system)
    val version = vm.emulatorsVersion
    val commands = remember(d.system, vm.config) { vm.commands(d.system) }
    val installed = remember(version, commands) { commands.map(vm::isInstalled) }
    val current = remember(version, vm.config) { vm.selectedCommand(d.system) }
    var index by remember { mutableIntStateOf(commands.indexOf(current).coerceAtLeast(0)) }

    fun choose(i: Int) {
        commands.getOrNull(i)?.let { vm.chooseEmulator(d.system, it) }
        vm.dialog = null
    }
    fun get(i: Int) {
        commands.getOrNull(i)?.let(vm::suggestedPackage)?.let(vm::openStore)
    }

    PadInput { button, _ ->
        when (button) {
            Pad.Up -> { index = (index - 1).coerceAtLeast(0); true }
            Pad.Down -> { index = (index + 1).coerceAtMost(commands.size - 1); true }
            Pad.A -> { choose(index); true }
            Pad.X -> { if (installed.getOrNull(index) == false) get(index); true }
            Pad.B -> { vm.dialog = null; true }
            else -> true // modal: swallow everything else
        }
    }

    val hints = buildList {
        add(Hint(Pad.A, "Use this", onClick = { choose(index) }))
        add(Hint(Pad.B, "Cancel", onClick = { vm.dialog = null }))
        if (installed.getOrNull(index) == false) add(Hint(Pad.X, "Get emulator", onClick = { get(index) }))
    }
    DialogFrame(hints) {
        Txt("Emulator for ${system?.info?.displayName ?: d.system}", Type.screenTitleSmall, Palette.ink, maxLines = 2)
        Spacer(Modifier.height(Space.SM.u))
        Txt("Applies to every game on this system.", Type.bodySmall, Palette.inkSecondary)
        Spacer(Modifier.height(Space.SECTION_SMALL.u))
        if (commands.isEmpty()) {
            Txt("ES-DE has no Android launch command for this system.", Type.body, Palette.inkBody, maxLines = 3)
        } else {
            val state = rememberLazyListState()
            LaunchedEffect(index) { state.bringIntoView(index) }
            val ring = Size.FOCUS_RING_LIGHT + Size.FOCUS_OFFSET
            LazyColumn(
                state = state,
                modifier = Modifier.fillMaxWidth().heightIn(max = Size.BOX_ART_H.u),
                contentPadding = PaddingValues(ring.u),
                verticalArrangement = Arrangement.spacedBy(Space.LIST_GAP.u),
            ) {
                itemsIndexed(commands) { i, command ->
                    ListRow(
                        focused = i == index,
                        accent = accent,
                        padV = Space.ROW_V_SMALL,
                        onClick = { if (i == index) choose(i) else index = i },
                    ) {
                        Txt(command.displayName, Type.rowTitle, Palette.ink, Modifier.weight(1f))
                        val ok = installed.getOrNull(i) == true
                        Txt(
                            when {
                                command == current && ok -> "In use"
                                command == current -> "In use · not installed"
                                ok -> "Installed"
                                else -> "Not installed"
                            },
                            Type.labelStrong,
                            if (ok) Palette.ok else Palette.problem,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LaunchErrorDialog(vm: LauncherViewModel, d: Dialog.LaunchError) {
    val problem = d.problem
    fun changeEmulator() {
        vm.dialog = Dialog.EmulatorPicker(d.system)
    }
    fun install() {
        problem.installPackage?.let(vm::openStore)
    }
    PadInput { button, _ ->
        when (button) {
            Pad.A, Pad.B -> { vm.dialog = null; true }
            Pad.X -> { changeEmulator(); true }
            Pad.Y -> { install(); true }
            else -> true
        }
    }
    val hints = buildList {
        add(Hint(Pad.A, "OK", onClick = { vm.dialog = null }))
        add(Hint(Pad.X, "Change emulator", onClick = ::changeEmulator))
        if (problem.installPackage != null) add(Hint(Pad.Y, "Get emulator", onClick = ::install))
    }
    DialogFrame(hints) {
        d.game?.let {
            Txt("Couldn't start ${it.title}", Type.labelStrong, Palette.problem, maxLines = 2)
            Spacer(Modifier.height(Space.GAP.u))
        }
        Txt(problem.title, Type.screenTitleSmall, Palette.ink, maxLines = 2)
        Spacer(Modifier.height(Space.MD.u))
        Txt(problem.detail, Type.bodySmall, Palette.inkBody, maxLines = 4)
        Spacer(Modifier.height(Space.ROW_GAP.u))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(rounded(Radius.CARD_SMALL))
                .background(Palette.surface)
                .padding(vertical = Space.ROW_V.u, horizontal = Space.ROW_H.u),
        ) {
            Column {
                Txt("HOW TO FIX", Type.caps, Palette.inkSecondary)
                Spacer(Modifier.height(Space.TINY.u))
                Txt(problem.fix, Type.bodySmall, Palette.ink, maxLines = 3)
            }
        }
    }
}

@Composable
private fun MessageDialog(vm: LauncherViewModel, d: Dialog.Message) {
    PadInput { button, _ ->
        if (button == Pad.A || button == Pad.B) vm.dialog = null
        true
    }
    DialogFrame(listOf(Hint(Pad.A, "OK", onClick = { vm.dialog = null }))) {
        Txt(d.title, Type.screenTitleSmall, Palette.ink, maxLines = 2)
        Spacer(Modifier.height(Space.MD.u))
        Txt(d.body, Type.bodySmall, Palette.inkBody, maxLines = 10)
    }
}

@Composable
private fun UpdateDialog(vm: LauncherViewModel) {
    val state = vm.update
    val accent = vm.systems.getOrNull(vm.homeSystemIndex)?.color ?: SystemColors.neutral
    val busy = state is UpdateState.Checking || state is UpdateState.Downloading || state is UpdateState.Installing
    val canInstall = state is UpdateState.Available || state is UpdateState.NeedsPermission ||
        (state is UpdateState.Failed && state.info != null)

    PadInput { button, _ ->
        when (button) {
            Pad.A -> if (canInstall) vm.installUpdate() else vm.dismissUpdate()
            Pad.B -> vm.dismissUpdate()
            else -> Unit
        }
        true
    }

    val hints = buildList {
        when {
            canInstall -> {
                val label = when (state) {
                    is UpdateState.NeedsPermission -> "Allow installs"
                    is UpdateState.Failed -> "Try again"
                    else -> "Install"
                }
                add(Hint(Pad.A, label, onClick = vm::installUpdate))
                add(Hint(Pad.B, "Later", onClick = vm::dismissUpdate))
            }
            !busy -> add(Hint(Pad.A, "OK", onClick = vm::dismissUpdate))
        }
    }
    DialogFrame(hints) {
        val title = when (state) {
            UpdateState.Checking -> "Checking for updates…"
            UpdateState.UpToDate, UpdateState.Idle -> "You're up to date"
            is UpdateState.Failed -> "Update didn't work"
            else -> "Update available"
        }
        Txt(title, Type.screenTitleSmall, Palette.ink, maxLines = 2)
        Spacer(Modifier.height(Space.MD.u))
        val body = when (state) {
            UpdateState.Checking -> "Asking GitHub for the newest build."
            UpdateState.UpToDate, UpdateState.Idle -> "Build ${vm.currentVersion} is the newest one."
            is UpdateState.Available -> "Build ${state.info.versionCode} is ready. You have build ${vm.currentVersion}."
            is UpdateState.NeedsPermission ->
                "Android needs your OK first: turn on \"Allow from this source\" for RP5 Launcher, then come back."
            is UpdateState.Downloading -> "Downloading build ${state.info.versionCode}…"
            is UpdateState.Installing -> "Installing. Confirm on Android's screen if it asks; the launcher closes when it's done."
            is UpdateState.Failed -> state.message
        }
        Txt(body, Type.bodySmall, Palette.inkBody, maxLines = 4)
        val notes = (state as? UpdateState.Available)?.info?.notes.orEmpty()
        if (notes.isNotEmpty()) {
            Spacer(Modifier.height(Space.ROW_GAP.u))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(rounded(Radius.CARD_SMALL))
                    .background(Palette.surface)
                    .padding(vertical = Space.ROW_V.u, horizontal = Space.ROW_H.u),
            ) {
                Column {
                    Txt("WHAT'S NEW", Type.caps, Palette.inkSecondary)
                    Spacer(Modifier.height(Space.TINY.u))
                    Txt(notes, Type.bodySmall, Palette.ink, maxLines = 5)
                }
            }
        }
        if (state is UpdateState.Downloading) {
            Spacer(Modifier.height(Space.ROW_GAP.u))
            val progress by animateFloatAsState(state.progress, Motion.focus(), label = "update-progress")
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(Size.PROGRESS_H.u)
                    .clip(rounded(Radius.PILL))
                    .background(Palette.surface),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .height(Size.PROGRESS_H.u)
                        .clip(rounded(Radius.PILL))
                        .background(accent),
                )
            }
        }
    }
}

@Composable
private fun TextInputDialog(vm: LauncherViewModel, d: Dialog.TextInput) {
    var value by remember(d) { mutableStateOf(TextFieldValue(d.initial, TextRange(d.initial.length))) }
    val focus = remember { FocusRequester() }
    fun save() = vm.submitText(d.field, value.text)
    fun cancel() {
        vm.dialog = null
    }

    DisposableEffect(d) {
        vm.textInputActive = true
        onDispose { vm.textInputActive = false }
    }
    LaunchedEffect(d) { focus.requestFocus() }
    PadInput { button, _ ->
        when (button) {
            Pad.Start -> save()
            Pad.B -> cancel()
            else -> Unit
        }
        true
    }

    DialogFrame(
        listOf(
            Hint(Pad.Start, "Save", onClick = ::save),
            Hint(Pad.B, "Cancel", onClick = ::cancel),
        ),
    ) {
        Txt(d.title, Type.screenTitleSmall, Palette.ink, maxLines = 2)
        Spacer(Modifier.height(Space.SECTION_SMALL.u))
        BasicTextField(
            value = value,
            onValueChange = { value = it },
            singleLine = true,
            textStyle = Type.body.copy(color = Palette.ink),
            cursorBrush = SolidColor(Palette.accent),
            visualTransformation = if (d.secret) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (d.secret) KeyboardType.Password else KeyboardType.Text,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { save() }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focus)
                .lightFocus(true, Palette.accent, Radius.CARD_SMALL)
                .clip(rounded(Radius.CARD_SMALL))
                .background(Palette.surface)
                .padding(vertical = Space.ROW_V.u, horizontal = Space.ROW_H.u),
        )
    }
}
