package io.github.hipsterbwp.rp5launcher.ui

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.hipsterbwp.rp5launcher.R
import io.github.hipsterbwp.rp5launcher.ui.screens.DialogHost
import io.github.hipsterbwp.rp5launcher.ui.screens.GameDetailsScreen
import io.github.hipsterbwp.rp5launcher.ui.screens.GameGridScreen
import io.github.hipsterbwp.rp5launcher.ui.screens.HomeScreen
import io.github.hipsterbwp.rp5launcher.ui.screens.LoadingScreen
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

    // The loading screen stays at least SPLASH_MIN_MS so it never flashes, then fades away.
    var minTimeUp by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(Motion.SPLASH_MIN_MS)
        minTimeUp = true
    }
    val showLoading = !vm.ready || !minTimeUp
    val loadingAlpha by animateFloatAsState(
        if (showLoading) 1f else 0f, tween(Motion.SPLASH_FADE_MS), label = "loading-fade",
    )

    Box(Modifier.fillMaxSize().background(Palette.brandNavy)) {
        if (vm.ready) LauncherContent(vm, appName, pickFolder)
        if (loadingAlpha > 0f) {
            Box(Modifier.fillMaxSize().alpha(loadingAlpha)) { LoadingScreen(vm.loading) }
        }
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
                null -> Unit
            }
        }
        DialogHost(vm)
    }
}
