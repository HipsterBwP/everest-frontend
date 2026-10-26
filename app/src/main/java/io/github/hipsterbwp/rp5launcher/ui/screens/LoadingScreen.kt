package io.github.hipsterbwp.rp5launcher.ui.screens

import androidx.compose.animation.core.animateFloatAsState
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

/**
 * ARCROW loading screen: the crow over a blue glow, the wordmark, a glowing progress bar that
 * follows real start-up steps, and what's loading right now.
 */
@Composable
fun LoadingScreen(step: LoadingStep) {
    val progress by animateFloatAsState(step.progress, tween(Motion.SPLASH_BAR_MS), label = "loading")
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

/** Pill track with a hairline blue edge and a glowing gradient fill. */
@Composable
private fun ProgressBar(progress: Float) {
    val shape = rounded(Radius.PILL)
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
                .background(Brush.horizontalGradient(listOf(Palette.brandBlue, Palette.brandBlueLight))),
        )
    }
}
