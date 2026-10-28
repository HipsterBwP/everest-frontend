package io.github.hipsterbwp.rp5launcher.ui

import android.content.ActivityNotFoundException
import io.github.hipsterbwp.rp5launcher.ui.theme.NeonTheme
import androidx.compose.runtime.SideEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import io.github.hipsterbwp.rp5launcher.ui.theme.Space
import io.github.hipsterbwp.rp5launcher.ui.theme.u
import androidx.compose.ui.res.stringResource
import io.github.hipsterbwp.rp5launcher.R
import io.github.hipsterbwp.rp5launcher.ui.screens.DialogHost
import io.github.hipsterbwp.rp5launcher.ui.screens.GameDetailsScreen
import io.github.hipsterbwp.rp5launcher.ui.screens.GameGridScreen
import io.github.hipsterbwp.rp5launcher.ui.screens.HomeScreen
import io.github.hipsterbwp.rp5launcher.ui.screens.BaseCampScreen
import io.github.hipsterbwp.rp5launcher.ui.screens.CollectionScreen
import io.github.hipsterbwp.rp5launcher.ui.screens.LoadingScreen
import io.github.hipsterbwp.rp5launcher.ui.screens.QuickMenu
import io.github.hipsterbwp.rp5launcher.ui.screens.Screensaver
import io.github.hipsterbwp.rp5launcher.ui.screens.SearchScreen
import io.github.hipsterbwp.rp5launcher.ui.screens.SettingsScreen
import io.github.hipsterbwp.rp5launcher.ui.screens.SetupScreen
import io.github.hipsterbwp.rp5launcher.ui.theme.Motion
import io.github.hipsterbwp.rp5launcher.ui.theme.Palette

@Composable
fun LauncherApp(vm: LauncherViewModel) {
    val appName = stringResource(R.string.app_name)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let(vm::onFolderPicked)
    }
    val pickFolder: () -> Unit = {
        try {
            picker.launch(vm.rootUri())
        } catch (_: ActivityNotFoundException) {
            vm.dialog = Dialog.Message(
                "No folder picker available",
                "This device has no app to choose folders with (the Android Files app may be disabled).",
            )
        }
    }

    // The loading screen stays at least SPLASH_MIN_MS after the boot check so it never flashes, then fades away.
    var bootDone by remember { mutableStateOf(false) }
    var minTimeUp by remember { mutableStateOf(false) }
    LaunchedEffect(bootDone) {
        if (!bootDone) return@LaunchedEffect
        delay(Motion.SPLASH_MIN_MS)
        minTimeUp = true
    }
    // Title screen: once loaded, wait for Start (if that's on) before showing the console screen.
    var started by remember { mutableStateOf(false) }
    val waitingForStart = vm.ready && minTimeUp && vm.config.pressStart && !started
    val showLoading = !vm.ready || !minTimeUp || waitingForStart
    val loadingAlpha by animateFloatAsState(
        if (showLoading) 1f else 0f, tween(Motion.SPLASH_FADE_MS), label = "loading-fade",
    )

    val labelStyle = vm.config.buttonLabels
    val swapped = vm.config.swapButtons
    val labels = remember(labelStyle, swapped) { { pad: Pad -> buttonLabel(pad, labelStyle, swapped) } }

    // Screensaver after the configured idle time (not during first-run setup or while typing).
    val idleMinutes = vm.config.screensaverMinutes
    LaunchedEffect(idleMinutes, vm.lastInputAt) {
        if (idleMinutes <= 0) return@LaunchedEffect
        delay(idleMinutes * 60_000L)
        if (vm.screen !is Screen.Setup && !vm.textInputActive && !showLoading) vm.screensaver = true
    }

    val season = vm.season
    val theme = when {
        vm.config.theme == "gold" -> NeonTheme.Gold
        season == Season.Halloween -> NeonTheme.Pumpkin
        season == Season.Winter -> NeonTheme.Frost
        else -> NeonTheme.Neon
    }
    SideEffect { Palette.theme = theme }
    val sky = SkySettings(vm.config.scene, vm.config.liveSky)

    Box(Modifier.fillMaxSize().background(Palette.night)) {
        if (vm.ready) {
            CompositionLocalProvider(LocalButtonLabel provides labels, LocalSky provides sky) {
                CrtPower(vm.crt) {
                    LauncherContent(vm, appName, pickFolder)
                    if (season != null && (vm.screen == Screen.Home || vm.screen == Screen.BaseCamp)) SeasonalSky(season)
                    if (vm.config.mascot && vm.screen !is Screen.Setup) Mascot(vm.mascotMood, vm.outfit, vm.config.yetiSize)
                }
                vm.tribute?.let { system -> TributeIntro(system, vm) { vm.tribute = null } }
                vm.insert?.let { InsertOverlay(vm, it) }
                if (vm.creditsRoll) {
                    CreditsRoll(vm.config.initials, summit = vm.summitRoll) {
                        vm.creditsRoll = false
                        vm.summitRoll = false
                    }
                }
            }
        }
        ToastHost(vm)
        OneUpPopup(vm.oneUp)
        if (vm.challenger) ChallengerFlash()
        if (vm.quickMenu && !vm.screensaver) vm.quickControls?.let { QuickMenu(vm, it) }
        if (vm.screensaver) Screensaver(vm)
        if (loadingAlpha > 0f) {
            Box(Modifier.fillMaxSize().alpha(loadingAlpha)) {
                LoadingScreen(
                    vm.loading,
                    boot = vm.config.bootSequence,
                    games = vm.systems.sumOf { it.games.size }.takeIf { it > 0 },
                    credits = vm.credits(),
                    waitingForStart = waitingForStart,
                    onStart = {
                        vm.sound(UiSound.Launch)
                        started = true
                    },
                    onBootDone = { bootDone = true },
                )
                if (waitingForStart) {
                    HistoryCard(Modifier.align(Alignment.BottomCenter).padding(bottom = Space.SECTION.u))
                }
            }
        }
        if (vm.config.crtEffect) CrtOverlay()
    }
}

@Composable
private fun LauncherContent(vm: LauncherViewModel, appName: String, pickFolder: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Crossfade(targetState = vm.screen, animationSpec = Motion.screenFade(), label = "screen") { screen ->
            when (screen) {
                is Screen.Setup -> SetupScreen(vm, screen.step, appName, pickFolder)
                Screen.Home -> HomeScreen(vm, appName)
                is Screen.Games -> GameGridScreen(vm, screen.system)
                is Screen.Details -> GameDetailsScreen(vm, screen.system, screen.gameId)
                Screen.Settings -> SettingsScreen(vm, pickFolder)
                Screen.Search -> SearchScreen(vm)
                is Screen.Collection -> CollectionScreen(vm, screen.key)
                Screen.BaseCamp -> BaseCampScreen(vm)
                null -> Unit
            }
        }
        DialogHost(vm)
    }
}
