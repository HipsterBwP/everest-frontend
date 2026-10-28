package io.github.hipsterbwp.rp5launcher.ui.screens

import io.github.hipsterbwp.rp5launcher.ui.tap
import io.github.hipsterbwp.rp5launcher.ui.Pad
import androidx.compose.animation.core.Easing
import kotlinx.coroutines.delay
import io.github.hipsterbwp.rp5launcher.ui.PadInput
import io.github.hipsterbwp.rp5launcher.BuildConfig
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import io.github.hipsterbwp.rp5launcher.R
import io.github.hipsterbwp.rp5launcher.ui.ChromeTxt
import io.github.hipsterbwp.rp5launcher.ui.LoadingStep
import io.github.hipsterbwp.rp5launcher.ui.RetroBackdrop
import io.github.hipsterbwp.rp5launcher.ui.Txt
import io.github.hipsterbwp.rp5launcher.ui.neonGlow
import io.github.hipsterbwp.rp5launcher.ui.theme.Motion
import io.github.hipsterbwp.rp5launcher.ui.theme.Palette
import io.github.hipsterbwp.rp5launcher.ui.theme.Radius
import io.github.hipsterbwp.rp5launcher.ui.theme.Size
import io.github.hipsterbwp.rp5launcher.ui.theme.Space
import io.github.hipsterbwp.rp5launcher.ui.theme.Type
import io.github.hipsterbwp.rp5launcher.ui.theme.rounded
import io.github.hipsterbwp.rp5launcher.ui.theme.u
import kotlin.math.roundToInt

/**
 * Everest loading screen: the mountain badge glowing above the horizon, EVEREST in sunset letters
 * on the neon grid, a segmented arcade loading bar that follows real start-up steps, and a blinking
 * INSERT COIN — then, once loaded, a blinking PRESS START that waits for the Start button.
 */
@Composable
fun LoadingScreen(
    step: LoadingStep,
    boot: Boolean,
    games: Int?,
    credits: Int,
    waitingForStart: Boolean,
    onStart: () -> Unit,
    onBootDone: () -> Unit,
) {
    // First an arcade BIOS check (skippable with any button), then the logo screen.
    var booting by remember { mutableStateOf(boot) }
    LaunchedEffect(booting) { if (!booting) onBootDone() }
    if (booting) {
        BootCheck(games) { booting = false }
        return
    }
    // Start-up is often done before this screen's first frame, so the bar fills over a fixed time
    // and never runs ahead of the real progress: it always visibly loads, and never lies.
    val fill = remember { Animatable(0f) }
    LaunchedEffect(Unit) { fill.animateTo(1f, tween(Motion.SPLASH_FILL_MS, easing = FastOutSlowInEasing)) }
    val real by animateFloatAsState(step.progress, tween(Motion.SPLASH_BAR_MS), label = "loading")
    val progress = minOf(fill.value, real)
    val blink by rememberInfiniteTransition(label = "coin").animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = Motion.BLINK_MS * 2
                1f at 0
                1f at Motion.BLINK_MS - 1
                0f at Motion.BLINK_MS
                0f at Motion.BLINK_MS * 2 - 1
            },
            RepeatMode.Restart,
            StartOffset(0),
        ),
        label = "coin-blink",
    )

    Box(Modifier.fillMaxSize().background(Palette.night)) {
        RetroBackdrop()
        Txt("1UP  000000", Type.hint, Palette.pinkSoft, Modifier.align(Alignment.TopStart).padding(Space.SECTION.u), glow = Palette.pink)
        Txt("CREDIT  ${credits.toString().padStart(2, '0')}", Type.hint, Palette.cyanSoft, Modifier.align(Alignment.TopEnd).padding(Space.SECTION.u), glow = Palette.cyan)
        Column(
            Modifier.fillMaxSize().padding(top = Size.SPLASH_LOGO_TOP.u),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val glow = Size.SPLASH_LOGO.u
            Image(
                painterResource(R.drawable.brand_logo),
                contentDescription = "Everest Frontend",
                modifier = Modifier
                    .size(Size.SPLASH_LOGO.u)
                    .drawBehind {
                        drawCircle(
                            Brush.radialGradient(listOf(Palette.brandGlow, Palette.transparent), center, glow.toPx() * 0.6f),
                            glow.toPx() * 0.6f,
                        )
                    },
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.height(Size.SPLASH_TITLE_GAP.u))
            ChromeTxt("Everest", Type.hero)
            Spacer(Modifier.height(Space.SM.u))
            Txt("FRONTEND", Type.hint, Palette.cyanSoft, glow = Palette.cyan)
            Spacer(Modifier.height(Space.LG.u))
            SegmentedBar(progress)
            Spacer(Modifier.height(Space.MD.u))
            Txt(
                if (waitingForStart) "READY" else "${step.message.uppercase()} ${(progress * 100).roundToInt()}%",
                Type.crtSmall, if (waitingForStart) Palette.cyanSoft else Palette.inkSecondary,
            )
            Spacer(Modifier.height(Space.GAP.u))
            if (waitingForStart) {
                // Title screen: only Start (or a tap) goes in.
                PadInput { button, _ ->
                    if (button == Pad.Start) onStart()
                    true
                }
                Txt("PRESS START", Type.screenTitle, Palette.sun1, Modifier.alpha(blink).tap(onStart), glow = Palette.sun2)
            } else {
                Txt("INSERT COIN", Type.buttonSmall, Palette.sun1, Modifier.alpha(blink), glow = Palette.sun2)
            }
        }
    }
}

/**
 * Arcade-board power-on check: lines typed out in VT323 on black, each ending in OK, with a blinking
 * block cursor. Takes about a second; any button skips it.
 */
@Composable
private fun BootCheck(games: Int?, done: () -> Unit) {
    val lines = listOf(
        "EVEREST ARCADE BIOS  v${BuildConfig.VERSION_CODE}",
        "(C) 2026 EVEREST FRONTEND",
        "",
        "RAM CHECK .............. 8192K  OK",
        "ROM CHECK .............. OK",
        "VIDEO .................. 1920x1080  OK",
        "SOUND .................. 8-BIT  OK",
        "CONTROLLER ............. P1  OK",
        "LIBRARY ................ ${games?.let { "$it GAMES" } ?: "SCANNING"}  OK",
        "",
        "STARTING...",
    )
    var shown by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (shown < lines.size) {
            delay(Motion.BOOT_LINE_MS.toLong())
            shown++
        }
        delay(Motion.BOOT_HOLD_MS.toLong())
        done()
    }
    PadInput { _, _ -> done(); true }
    val cursor by rememberInfiniteTransition(label = "cursor").animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(Motion.BLINK_MS / 2, easing = Easing { if (it < 0.5f) 0f else 1f }), RepeatMode.Reverse),
        label = "cursor-blink",
    )
    Column(Modifier.fillMaxSize().background(Palette.black).padding(Space.XL.u)) {
        lines.take(shown).forEach { Txt(it, Type.crt, if (it.endsWith("OK")) Palette.cyanSoft else Palette.ink) }
        Box(Modifier.size(Size.BOOT_CURSOR_W.u, Size.BOOT_CURSOR_H.u).alpha(cursor).background(Palette.cyanSoft))
    }
}

/** Arcade loading bar: a neon-edged track of blocks that light up in sunset colours. */
@Composable
private fun SegmentedBar(progress: Float) {
    val lit = (progress.coerceIn(0f, 1f) * Size.SPLASH_SEGMENTS).roundToInt()
    val edge = Size.NEON_EDGE.u
    val radius = Radius.GLYPH_RECT.u
    Row(
        Modifier
            .width(Size.SPLASH_BAR_W.u)
            .height(Size.SPLASH_BAR_H.u)
            .drawBehind { neonGlow(Palette.pink, CornerRadius(radius.toPx()), edge.toPx()) }
            .clip(rounded(Radius.GLYPH_RECT))
            .background(Palette.night.copy(alpha = 0.8f))
            .border(edge, Palette.pink, rounded(Radius.GLYPH_RECT))
            .padding(Space.XS.u),
        horizontalArrangement = Arrangement.spacedBy(Space.XS.u),
    ) {
        repeat(Size.SPLASH_SEGMENTS) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(rounded(Radius.STEP))
                    .background(
                        if (i < lit) Brush.verticalGradient(listOf(Palette.sun1, Palette.sun3))
                        else Brush.verticalGradient(listOf(Palette.brandTrack, Palette.brandTrack)),
                    ),
            )
        }
    }
}
