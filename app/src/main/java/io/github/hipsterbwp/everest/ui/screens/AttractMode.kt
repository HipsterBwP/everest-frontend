package io.github.hipsterbwp.everest.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import io.github.hipsterbwp.everest.ui.ArtImage
import io.github.hipsterbwp.everest.ui.ChromeTxt
import io.github.hipsterbwp.everest.ui.GameVideo
import io.github.hipsterbwp.everest.ui.HistoryCard
import io.github.hipsterbwp.everest.ui.Mascot
import io.github.hipsterbwp.everest.ui.Mood
import io.github.hipsterbwp.everest.ui.LauncherViewModel
import io.github.hipsterbwp.everest.ui.Txt
import io.github.hipsterbwp.everest.ui.glass
import io.github.hipsterbwp.everest.ui.neonPanel
import io.github.hipsterbwp.everest.ui.tap
import io.github.hipsterbwp.everest.ui.theme.Focus
import io.github.hipsterbwp.everest.ui.theme.Motion
import io.github.hipsterbwp.everest.ui.theme.Palette
import io.github.hipsterbwp.everest.ui.theme.Radius
import io.github.hipsterbwp.everest.ui.theme.Size
import io.github.hipsterbwp.everest.ui.theme.Space
import io.github.hipsterbwp.everest.ui.theme.Type
import io.github.hipsterbwp.everest.ui.theme.u
import kotlinx.coroutines.delay

/**
 * Attract mode (the screensaver), like an arcade cabinet waiting for a player: gameplay videos from
 * your library (or slowly zooming art when there are none) with INSERT COIN blinking, and every few
 * slides a high-score table of your most-played games. Any button wakes it without doing anything.
 */
@Composable
fun Screensaver(vm: LauncherViewModel) {
    LaunchedEffect(Unit) { vm.systems.forEach { vm.loadEsdeVideos(it.name) } }
    val clips = remember(vm.esdeVideos.size, vm.art.size) {
        vm.systems.flatMap { s -> s.games.mapNotNull { g -> vm.videoFor(s.name, g)?.let { Triple(s, g, it) } } }.shuffled()
    }
    val stills = remember(vm.systems) {
        vm.systems.flatMap { s -> s.games.map { s to it } }.filter { vm.anyArt(it.second) != null }.shuffled()
    }
    val scores = remember { vm.highScores() }
    var slide by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Motion.SCREENSAVER_SLIDE_MS)
            slide++
        }
    }
    val showScores = scores.isNotEmpty() && slide % SCORE_EVERY == SCORE_EVERY - 1
    val blink by rememberInfiniteTransition(label = "coin").animateFloat(
        initialValue = 1f, targetValue = 0.15f,
        animationSpec = infiniteRepeatable(tween(Motion.BLINK_MS), RepeatMode.Reverse),
        label = "coin-blink",
    )

    Box(Modifier.fillMaxSize().background(Palette.black).tap { vm.onInput() }) {
        val caption: Pair<String, String>?
        if (clips.isNotEmpty()) {
            val (system, game, video) = clips[slide % clips.size]
            GameVideo(video, withSound = false, modifier = Modifier.fillMaxSize())
            caption = system.info.displayName to game.title
        } else if (stills.isNotEmpty()) {
            val current = stills[slide % stills.size]
            Crossfade(targetState = current, animationSpec = tween(Motion.SCREENSAVER_FADE_MS), label = "attract-art") { (_, game) ->
                val zoom = remember(game.id) { Animatable(1f) }
                LaunchedEffect(game.id) {
                    zoom.animateTo(Focus.KEN_BURNS_SCALE, tween(Motion.SCREENSAVER_SLIDE_MS.toInt() + Motion.SCREENSAVER_FADE_MS, easing = LinearEasing))
                }
                ArtImage(vm.anyArt(game), Modifier.fillMaxSize().graphicsLayer { scaleX = zoom.value; scaleY = zoom.value }, ContentScale.Crop)
            }
            caption = current.first.info.displayName to current.second.title
        } else {
            caption = null
        }

        Txt("EVEREST", Type.hint, Palette.pinkSoft, Modifier.align(Alignment.TopStart).padding(Space.SECTION.u), glow = Palette.pink)
        Txt("CREDIT ${vm.credits().toString().padStart(2, '0')}", Type.hint, Palette.cyanSoft, Modifier.align(Alignment.TopEnd).padding(Space.SECTION.u), glow = Palette.cyan)

        if (showScores) {
            HighScores(vm, scores, Modifier.align(Alignment.Center))
        } else if (caption != null) {
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(Space.SCREEN_H.u)
                    .glass(Radius.CARD)
                    .padding(Space.LG.u),
            ) {
                Txt(caption.first.uppercase(), Type.eyebrow, Palette.cyanSoft, glow = Palette.cyan)
                Spacer(Modifier.height(Space.SM.u))
                ChromeTxt(caption.second, Type.screenTitle, Modifier.width(Size.BROWSER_INFO_W.u))
            }
        } else {
            ChromeTxt("EVEREST", Type.hero, Modifier.align(Alignment.Center))
        }

        if (!showScores) HistoryCard(Modifier.align(Alignment.TopCenter).padding(top = Space.SECTION.u))
        if (vm.config.mascot) {
            Mascot(Mood.Sleep, vm.outfit, vm.config.yetiSize)
            Txt(
                "z Z", Type.hint, Palette.inkSecondary,
                Modifier.align(Alignment.BottomEnd).padding(end = Space.XL.u, bottom = Size.HINT_BAR.u + Space.SECTION_LARGE.u).alpha(blink),
            )
        }

        Txt(
            "INSERT COIN", Type.screenTitleSmall, Palette.sun1,
            Modifier.align(Alignment.BottomCenter).padding(bottom = Space.SECTION_LARGE.u).alpha(blink),
            glow = Palette.sun2,
        )
    }
}

/** Arcade high-score table: rank, your initials, score (minutes played × 10) and the game. */
@Composable
private fun HighScores(vm: LauncherViewModel, scores: List<Pair<io.github.hipsterbwp.everest.data.Game, Long>>, modifier: Modifier) {
    Column(
        modifier
            .width(Size.HIGH_SCORES_W.u)
            .neonPanel(Radius.PANEL, fill = Palette.glassStrong)
            .padding(Space.SECTION_LARGE.u),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ChromeTxt("High scores", Type.section)
        Spacer(Modifier.height(Space.LG.u))
        scores.forEachIndexed { i, (game, score) ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = Space.XS.u),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.LG.u),
            ) {
                val rankColor = when (i) {
                    0 -> Palette.sun1
                    1 -> Palette.cyanSoft
                    2 -> Palette.pinkSoft
                    else -> Palette.ink
                }
                Txt(ordinal(i + 1), Type.hint, rankColor, Modifier.width(Size.RANK_W.u))
                Txt(vm.config.initials, Type.crt, rankColor)
                Txt(score.toString().padStart(6, '0'), Type.crt, Palette.sun1)
                Txt(game.title, Type.labelStrong, Palette.inkSecondary, Modifier.weight(1f))
            }
        }
    }
}

private fun ordinal(n: Int) = when {
    n % 100 in 11..13 -> "${n}TH"
    n % 10 == 1 -> "${n}ST"
    n % 10 == 2 -> "${n}ND"
    n % 10 == 3 -> "${n}RD"
    else -> "${n}TH"
}

/** The high-score table replaces every SCORE_EVERY-th slide. */
private const val SCORE_EVERY = 4
