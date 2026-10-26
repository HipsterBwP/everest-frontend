package io.github.hipsterbwp.rp5launcher.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import io.github.hipsterbwp.rp5launcher.R
import io.github.hipsterbwp.rp5launcher.ui.LoadingStep
import io.github.hipsterbwp.rp5launcher.ui.Txt
import io.github.hipsterbwp.rp5launcher.ui.theme.Motion
import io.github.hipsterbwp.rp5launcher.ui.theme.Palette
import io.github.hipsterbwp.rp5launcher.ui.theme.Radius
import io.github.hipsterbwp.rp5launcher.ui.theme.Size
import io.github.hipsterbwp.rp5launcher.ui.theme.Space
import io.github.hipsterbwp.rp5launcher.ui.theme.Type
import io.github.hipsterbwp.rp5launcher.ui.theme.rounded
import io.github.hipsterbwp.rp5launcher.ui.theme.u

/** Sheen width as a fraction of the filled bar, each side of its center. */
private const val SHEEN_WIDTH = 0.18f

/**
 * ARCROW loading screen: the crow over a blue glow, the wordmark, a glowing progress bar that
 * follows real start-up steps, and what's loading right now.
 */
@Composable
fun LoadingScreen(step: LoadingStep) {
    // Start-up is often done before this screen's first frame, so the bar fills over a fixed time
    // and never runs ahead of the real progress: it always visibly loads, and never lies.
    val fill = remember { Animatable(0f) }
    LaunchedEffect(Unit) { fill.animateTo(1f, tween(Motion.SPLASH_FILL_MS, easing = FastOutSlowInEasing)) }
    val real by animateFloatAsState(step.progress, tween(Motion.SPLASH_BAR_MS), label = "loading")
    val progress = minOf(fill.value, real)
    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.brandNavy),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(contentAlignment = Alignment.Center) {
                // Blue light behind the logo.
                Box(
                    Modifier
                        .size(Size.SPLASH_GLOW.u)
                        .background(Brush.radialGradient(listOf(Palette.brandGlow, Palette.transparent))),
                )
                Image(
                    painterResource(R.drawable.brand_logo),
                    contentDescription = "ARCROW",
                    modifier = Modifier.size(Size.SPLASH_LOGO.u),
                    contentScale = ContentScale.Fit,
                )
            }
            Image(
                painterResource(R.drawable.brand_wordmark),
                contentDescription = null,
                modifier = Modifier.width(Size.SPLASH_WORDMARK_W.u).offset(y = (-Space.GAP).u),
                contentScale = ContentScale.FillWidth,
            )
            ProgressBar(progress)
            Spacer(Modifier.height(Space.LG.u))
            Txt(step.message, Type.label, Palette.brandText)
        }
    }
}

/** Pill track with a hairline blue edge, a glowing gradient fill and a light sheen sweeping along it. */
@Composable
private fun ProgressBar(progress: Float) {
    val shape = rounded(Radius.PILL)
    val sweep by rememberInfiniteTransition(label = "sheen").animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(Motion.SPLASH_SHEEN_MS, easing = LinearEasing)),
        label = "sheen-x",
    )
    Box(
        Modifier
            .width(Size.SPLASH_BAR_W.u)
            .height(Size.SPLASH_BAR_H.u)
            .clip(shape)
            .background(Palette.brandTrack)
            .border(Size.SPLASH_BAR_EDGE.u, Palette.brandEdge, shape),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .shadow(Space.GAP.u, shape, ambientColor = Palette.brandBlue, spotColor = Palette.brandBlue)
                .clip(shape)
                .background(Brush.horizontalGradient(listOf(Palette.brandBlue, Palette.brandBlueLight)))
                .drawWithContent {
                    drawContent()
                    val x = size.width * sweep
                    val half = size.width * SHEEN_WIDTH
                    drawRect(
                        Brush.horizontalGradient(
                            listOf(Palette.transparent, Palette.brandSheen, Palette.transparent),
                            startX = x - half,
                            endX = x + half,
                        ),
                    )
                },
        )
    }
}
