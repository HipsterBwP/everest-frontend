package io.github.hipsterbwp.rp5launcher.ui.screens

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
 * ARCROW loading screen: the raven over a striped synthwave sun, ARCROW in sunset letters on the
 * neon grid, a segmented arcade loading bar that follows real start-up steps, and a blinking
 * INSERT COIN.
 */
@Composable
fun LoadingScreen(step: LoadingStep) {
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
        RetroBackdrop(sun = true)
        Txt("1UP  000000", Type.hint, Palette.pinkSoft, Modifier.align(Alignment.TopStart).padding(Space.SECTION.u), glow = Palette.pink)
        Txt("CREDIT  01", Type.hint, Palette.cyanSoft, Modifier.align(Alignment.TopEnd).padding(Space.SECTION.u), glow = Palette.cyan)
        Column(
            Modifier.fillMaxSize().padding(top = Size.SPLASH_LOGO_TOP.u),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val glow = Size.SPLASH_LOGO.u
            Image(
                painterResource(R.drawable.brand_logo),
                contentDescription = "ARCROW",
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
            ChromeTxt("ARCROW", Type.hero)
            Spacer(Modifier.height(Space.SM.u))
            Txt("RETRO GAME LAUNCHER", Type.hint, Palette.cyanSoft, glow = Palette.cyan)
            Spacer(Modifier.height(Space.LG.u))
            SegmentedBar(progress)
            Spacer(Modifier.height(Space.MD.u))
            Txt("${step.message.uppercase()} ${(progress * 100).roundToInt()}%", Type.crtSmall, Palette.inkSecondary)
            Spacer(Modifier.height(Space.GAP.u))
            Txt("INSERT COIN", Type.buttonSmall, Palette.sun1, Modifier.alpha(blink), glow = Palette.sun2)
        }
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
