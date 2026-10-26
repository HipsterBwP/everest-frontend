package io.github.hipsterbwp.rp5launcher.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import io.github.hipsterbwp.rp5launcher.data.Game
import io.github.hipsterbwp.rp5launcher.ui.ArtImage
import io.github.hipsterbwp.rp5launcher.ui.GameVideo
import io.github.hipsterbwp.rp5launcher.ui.Hint
import io.github.hipsterbwp.rp5launcher.ui.HintBar
import io.github.hipsterbwp.rp5launcher.ui.LauncherViewModel
import io.github.hipsterbwp.rp5launcher.ui.Pad
import io.github.hipsterbwp.rp5launcher.ui.Pill
import io.github.hipsterbwp.rp5launcher.ui.SystemEntry
import io.github.hipsterbwp.rp5launcher.ui.Txt
import io.github.hipsterbwp.rp5launcher.ui.accelerated
import io.github.hipsterbwp.rp5launcher.ui.bringIntoView
import io.github.hipsterbwp.rp5launcher.ui.glass
import io.github.hipsterbwp.rp5launcher.ui.lift
import io.github.hipsterbwp.rp5launcher.ui.tap
import io.github.hipsterbwp.rp5launcher.ui.theme.Alpha
import io.github.hipsterbwp.rp5launcher.ui.theme.Blur
import io.github.hipsterbwp.rp5launcher.ui.theme.Elevation
import io.github.hipsterbwp.rp5launcher.ui.theme.Focus
import io.github.hipsterbwp.rp5launcher.ui.theme.Motion
import io.github.hipsterbwp.rp5launcher.ui.theme.Palette
import io.github.hipsterbwp.rp5launcher.ui.theme.Radius
import io.github.hipsterbwp.rp5launcher.ui.theme.Size
import io.github.hipsterbwp.rp5launcher.ui.theme.Space
import io.github.hipsterbwp.rp5launcher.ui.theme.Type
import io.github.hipsterbwp.rp5launcher.ui.theme.rounded
import io.github.hipsterbwp.rp5launcher.ui.theme.u

/** Portrait box-art proportion used until the real cover has loaded. */
private const val DEFAULT_COVER_RATIO = 0.72f

/**
 * Moves through a cover grid of [count] items in [columns] columns. Returns true if handled.
 * Up/Down accelerate while held.
 */
fun browserMove(button: Pad, repeat: Int, index: Int, count: Int, columns: Int, select: (Int) -> Unit): Boolean {
    if (count == 0) return button in setOf(Pad.Up, Pad.Down, Pad.Left, Pad.Right)
    when (button) {
        Pad.Left -> if (index > 0) select(index - 1)
        Pad.Right -> if (index < count - 1) select(index + 1)
        Pad.Up -> {
            val rows = minOf(accelerated(repeat), index / columns)
            if (rows > 0) select(index - columns * rows)
        }
        Pad.Down -> {
            val rows = minOf(accelerated(repeat), (count - 1) / columns - index / columns)
            if (rows > 0) select((index + columns * rows).coerceAtMost(count - 1))
        }
        else -> return false
    }
    return true
}

/**
 * Game browser in the style of the reference themes, Apple-ized: blurred art of the focused game
 * fills the screen; on the left its gameplay video (or screenshot) and a glass info card; on the
 * right a grid of covers where the focused one lifts toward you.
 */
@Composable
fun GameBrowser(
    vm: LauncherViewModel,
    entries: List<Pair<SystemEntry, Game>>,
    index: Int,
    header: @Composable () -> Unit,
    emptyTitle: String,
    emptyBody: String,
    hints: List<Hint>,
    onSelect: (Int) -> Unit,
    onPlay: (Int) -> Unit,
) {
    val focused = entries.getOrNull(index)
    val game = focused?.second
    val backdrop = game?.let { vm.fanart(it) ?: vm.snapArt(it) ?: vm.boxArt(it) }

    Box(Modifier.fillMaxSize().background(Palette.page)) {
        Crossfade(targetState = backdrop, animationSpec = Motion.colorFade(), label = "browser-backdrop") { file ->
            ArtImage(file, Modifier.fillMaxSize().blur(Blur.BACKDROP.u), ContentScale.Crop)
        }
        Box(Modifier.fillMaxSize().background(Palette.black.copy(alpha = Alpha.BROWSER_DIM)))

        Row(
            Modifier
                .fillMaxSize()
                .padding(start = Space.SCREEN_H.u, end = Space.SCREEN_H.u, top = Space.SCREEN_TOP.u, bottom = Size.HINT_BAR.u),
        ) {
            if (focused != null) InfoColumn(vm, focused.first, focused.second)
            Spacer(Modifier.width(Space.SECTION_LARGE.u))
            Column(Modifier.weight(1f).fillMaxSize()) {
                header()
                if (entries.isEmpty()) {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                        Txt(emptyTitle, Type.section, Palette.ink, maxLines = 2)
                        Spacer(Modifier.height(Space.MD.u))
                        Txt(emptyBody, Type.body, Palette.inkSecondary, maxLines = 3)
                    }
                } else {
                    CoverGrid(vm, entries, index, onSelect, onPlay)
                }
            }
        }
        HintBar(hints, onSystem = true, glyphInk = Palette.inkOnLight)
    }
}

@Composable
private fun InfoColumn(vm: LauncherViewModel, system: SystemEntry, game: Game) {
    val info = vm.artFor(game)
    Column(Modifier.width(Size.BROWSER_INFO_W.u).fillMaxSize()) {
        // Media: gameplay video over its screenshot, like a trailer card.
        Box(
            Modifier
                .fillMaxWidth()
                .height(Size.MEDIA_H.u)
                .shadow(Elevation.MEDIA.u, rounded(Radius.CARD), clip = false)
                .clip(rounded(Radius.CARD))
                .background(Palette.card),
        ) {
            val frames = listOfNotNull(vm.snapArt(game), vm.titleArt(game), vm.fanart(game))
            if (frames.isNotEmpty()) {
                Slideshow(game.id, frames)
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val logo = vm.logos[system.name]
                    if (logo != null) ArtImage(logo, Modifier.fillMaxWidth(0.5f).height(Size.RAIL_LOGO_SELECTED_H.u), tint = Palette.inkSecondary)
                    else Txt(system.info.shortName, Type.section, Palette.inkSecondary)
                }
            }
            GameVideo(info?.video, vm.config.videoSound, Modifier.fillMaxSize())
        }
        Spacer(Modifier.height(Space.TILE_GAP.u))

        Column(
            Modifier
                .fillMaxWidth()
                .glass(Radius.CARD)
                .padding(Space.LG.u),
        ) {
            val eyebrow = listOfNotNull(system.info.displayName, info?.year).joinToString(" · ").uppercase()
            Txt(eyebrow, Type.eyebrow, Palette.inkSecondary)
            Spacer(Modifier.height(Space.SM.u))
            Txt(game.title, Type.screenTitle, Palette.ink, maxLines = 1)
            Spacer(Modifier.height(Space.MD.u))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.GAP.u), verticalAlignment = Alignment.CenterVertically) {
                info?.rating?.let { Stars(it) }
                info?.genre?.let { Pill(it, Palette.surface, Palette.ink, Type.pill) }
                info?.players?.let { Pill("$it P", Palette.surface, Palette.ink, Type.pill) }
                if (vm.stats(game).favorite) Pill("Favorite", Palette.surface, Palette.ink, Type.pill)
            }
            Spacer(Modifier.height(Space.MD.u))
            val credits = listOfNotNull(info?.publisher ?: info?.developer).joinToString()
            Txt(
                info?.description ?: credits.ifEmpty { game.fileName },
                Type.bodySmall,
                Palette.inkBody,
                maxLines = 3,
            )
        }
    }
}

/**
 * Gameplay stills (screenshot, title screen, fanart) cross-fading with a slow Ken Burns zoom,
 * standing in for video when there is none (or until it starts).
 */
@Composable
private fun Slideshow(key: String, frames: List<java.io.File>) {
    var frame by remember(key) { mutableIntStateOf(0) }
    LaunchedEffect(key, frames.size) {
        while (frames.size > 1) {
            delay(Motion.SLIDE_MS.toLong())
            frame = (frame + 1) % frames.size
        }
    }
    Crossfade(targetState = frames[frame % frames.size], animationSpec = Motion.colorFade(), label = "slide") { file ->
        val zoom = remember(file) { Animatable(1f) }
        LaunchedEffect(file) {
            zoom.animateTo(Focus.KEN_BURNS_SCALE, tween(Motion.KEN_BURNS_MS, easing = LinearEasing))
        }
        ArtImage(
            file,
            Modifier.fillMaxSize().graphicsLayer { scaleX = zoom.value; scaleY = zoom.value },
            ContentScale.Crop,
        )
    }
}

/** Five stars, filled to the rating (0..1). */
@Composable
private fun Stars(rating: Float) {
    val filled = (rating * 5 + 0.25f).toInt().coerceIn(0, 5)
    Row {
        Txt("★".repeat(filled), Type.labelStrong, Palette.star)
        Txt("★".repeat(5 - filled), Type.labelStrong, Palette.surface)
    }
}

@Composable
private fun CoverGrid(
    vm: LauncherViewModel,
    entries: List<Pair<SystemEntry, Game>>,
    index: Int,
    onSelect: (Int) -> Unit,
    onPlay: (Int) -> Unit,
) {
    val state = rememberLazyGridState()
    LaunchedEffect(index, entries.size) { state.bringIntoView(index) }
    val lift = Space.SECTION_SMALL
    LazyVerticalGrid(
        columns = GridCells.Fixed(Size.COVER_COLUMNS),
        state = state,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = lift.u, end = lift.u, top = lift.u, bottom = (Space.SECTION_SMALL + lift).u),
        horizontalArrangement = Arrangement.spacedBy(Space.TILE_GAP.u),
        verticalArrangement = Arrangement.spacedBy(Space.SECTION_SMALL.u),
    ) {
        itemsIndexed(entries, key = { _, e -> e.second.id }) { i, (_, game) ->
            Box(
                Modifier
                    .height(Size.COVER_H.u)
                    .tap { if (i == index) onPlay(i) else onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Cover(vm.boxArt(game), game.title, focused = i == index)
            }
        }
    }
}

/** A cover sized to its own proportions, lifting with a white edge when focused. */
@Composable
private fun Cover(file: java.io.File?, title: String, focused: Boolean) {
    val progress by animateFloatAsState(if (focused) 1f else 0f, Motion.focus(), label = "cover")
    val painter = if (file != null) rememberAsyncImagePainter(file) else null
    val loaded = painter?.state as? AsyncImagePainter.State.Success
    val ratio = loaded?.painter?.intrinsicSize?.let { if (it.height > 0) it.width / it.height else null }
        ?: DEFAULT_COVER_RATIO
    Box(
        Modifier
            .aspectRatio(ratio)
            .lift(progress, Focus.COVER_SCALE, Radius.CARD_SMALL)
            .clip(rounded(Radius.CARD_SMALL))
            .background(Palette.card)
            .then(
                if (focused) Modifier.border(Size.FOCUS_RING_CARD.u, Palette.white, rounded(Radius.CARD_SMALL))
                else Modifier,
            ),
        contentAlignment = Alignment.BottomStart,
    ) {
        if (painter != null && loaded != null) {
            Image(painter, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            if (painter != null) {
                // Keeps the request alive until it loads.
                Image(painter, contentDescription = null, modifier = Modifier.fillMaxSize())
            }
            Txt(title, Type.cardTitle, Palette.ink, Modifier.padding(Space.CARD_PAD.u), maxLines = 4)
        }
    }
}

/** Header for a browser: optional logo (else [title]), a count, and pills (filters or tabs). */
@Composable
fun BrowserHeader(
    logo: java.io.File?,
    title: String,
    count: Int,
    pills: List<String>,
    selected: Int,
    onPill: (Int) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(start = Space.SECTION_SMALL.u, end = Space.SECTION_SMALL.u)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                if (logo != null) {
                    ArtImage(logo, Modifier.height(Size.LOGO_SMALL_H.u).fillMaxWidth(0.6f), tint = Palette.ink)
                } else {
                    Txt(title, Type.screenTitle, Palette.ink)
                }
            }
            Txt("$count ${if (count == 1) "game" else "games"}", Type.label, Palette.inkSecondary)
        }
        Spacer(Modifier.height(Space.ROW_GAP.u))
        TabPills(pills, selected, onPill)
    }
}

/** Glass segmented control (Apple-style): selected item white, others translucent. */
@Composable
fun TabPills(items: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.glass(Radius.PILL).padding(Space.SM.u),
        horizontalArrangement = Arrangement.spacedBy(Space.SM.u),
    ) {
        items.forEachIndexed { i, name ->
            val on = i == selected
            Pill(
                name,
                if (on) Palette.white else Palette.transparent,
                if (on) Palette.inkOnLight else Palette.ink,
                Type.tab,
                Space.CHIP_V,
                Space.PILL_H_SMALL,
                Modifier.tap { onSelect(i) },
            )
        }
    }
}
