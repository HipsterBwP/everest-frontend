package io.github.hipsterbwp.rp5launcher.ui.screens

import io.github.hipsterbwp.rp5launcher.ui.theme.Retro
import io.github.hipsterbwp.rp5launcher.ui.LocalTitleBrush
import io.github.hipsterbwp.rp5launcher.ui.EraBackdrop
import io.github.hipsterbwp.rp5launcher.ui.Era
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import io.github.hipsterbwp.rp5launcher.data.Progress
import io.github.hipsterbwp.rp5launcher.ui.neonGlow
import io.github.hipsterbwp.rp5launcher.ui.rememberGlowPulse
import io.github.hipsterbwp.rp5launcher.ui.neonPanel
import io.github.hipsterbwp.rp5launcher.ui.ChromeTxt
import io.github.hipsterbwp.rp5launcher.ui.RetroBackdrop
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
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
import kotlin.random.Random
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
 * Moves through a cover grid of [count] items in [columns] columns, or jumps to a random one
 * (Select). Returns true if handled. Up/Down accelerate while held.
 */
fun browserMove(button: Pad, repeat: Int, index: Int, count: Int, columns: Int, select: (Int) -> Unit): Boolean {
    if (count == 0) return button in setOf(Pad.Up, Pad.Down, Pad.Left, Pad.Right, Pad.Select)
    when (button) {
        // Random game.
        Pad.Select -> if (count > 1) select((index + 1 + Random.nextInt(count - 1)) % count)
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
 * Game browser, arcade style: the synthwave grid behind everything; on the left a neon-framed card
 * with the focused game's gameplay video (or screenshots) and its info; on the right a grid of
 * covers where the focused one lifts toward you with a pink glow.
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
    era: Era? = null,
) {
    val focused = entries.getOrNull(index)
    CompositionLocalProvider(LocalTitleBrush provides (era?.title ?: Retro.sunset), LocalEra provides era) {

    Box(Modifier.fillMaxSize().background(Palette.page)) {
        if (era != null) EraBackdrop(era) else RetroBackdrop()
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Palette.night.copy(alpha = Alpha.BROWSER_DIM), Palette.transparent))))

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
}

@Composable
private fun InfoColumn(vm: LauncherViewModel, system: SystemEntry, game: Game) {
    val info = vm.artFor(game)
    val stats = vm.stats(game)
    Column(
        Modifier
            .padding(top = Space.SM.u, bottom = Space.LG.u)
            .width(Size.BROWSER_INFO_W.u)
            .fillMaxSize()
            .neonPanel(Radius.PANEL, edge = LocalEra.current?.edge ?: Palette.cyan, corner = LocalEra.current?.corner ?: Palette.pink)
            .padding(Space.LG.u),
    ) {
        // Media: gameplay video over its screenshot, in a TV-like inset.
        Box(
            Modifier
                .fillMaxWidth()
                .height(Size.MEDIA_H.u)
                .clip(rounded(Radius.CARD))
                .background(Palette.night)
                .border(Size.NEON_EDGE.u, Palette.violet, rounded(Radius.CARD)),
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
            LaunchedEffect(system.name) { vm.loadEsdeVideos(system.name) }
            GameVideo(vm.videoFor(system.name, game), vm.config.videoSound && !vm.config.muted, Modifier.fillMaxSize())
        }
        Spacer(Modifier.height(Space.LG.u))

        Column(Modifier.fillMaxWidth()) {
            val eyebrow = listOfNotNull(system.info.displayName, info?.year).joinToString(" · ").uppercase()
            Txt(eyebrow, Type.eyebrow, Palette.cyanSoft, glow = Palette.cyan)
            Spacer(Modifier.height(Space.GAP.u))
            ChromeTxt(game.title, Type.screenTitle)
            Spacer(Modifier.height(Space.GAP.u))
            val credits = listOfNotNull(info?.publisher ?: info?.developer, info?.genre).joinToString(" · ")
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (credits.isNotEmpty()) Txt("$credits · ", Type.label, Palette.inkSecondary)
                Txt(
                    if (stats.playtimeMs > 0) "${formatPlaytime(stats.playtimeMs)} played".uppercase() else "NOT PLAYED YET",
                    Type.labelStrong, Palette.sun1,
                )
            }
            Spacer(Modifier.height(Space.MD.u))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.GAP.u), verticalAlignment = Alignment.CenterVertically) {
                info?.rating?.let { Stars(it) }
                vm.raProgress[game.id]?.takeIf { it.total > 0 }?.let {
                    Pill("🏆 ${it.awarded}/${it.total}", Palette.cyan.copy(alpha = CHIP_FILL), Palette.cyanSoft, Type.pill)
                }
                stats.status?.let { Pill(Progress.label(it), Palette.pink.copy(alpha = CHIP_FILL), Palette.pinkSoft, Type.pill) }
                info?.players?.let { Pill("$it P", Palette.cyan.copy(alpha = CHIP_FILL), Palette.cyanSoft, Type.pill) }
                if (stats.favorite) Pill("★ Fav", Palette.sun1.copy(alpha = CHIP_FILL), Palette.sun1, Type.pill)
            }
            Spacer(Modifier.height(Space.MD.u))
            Txt(
                info?.description ?: game.fileName,
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
                val stats = vm.stats(game)
                Cover(vm.boxArt(game), game.title, focused = i == index, favorite = stats.favorite, status = stats.status)
            }
        }
    }
}

/**
 * A cover sized to its own proportions, lifting with a glowing pink edge when focused, with a star
 * for favorites and a progress tag (Beaten, Playing…).
 */
@Composable
private fun Cover(file: java.io.File?, title: String, focused: Boolean, favorite: Boolean, status: String?) {
    val progress by animateFloatAsState(if (focused) 1f else 0f, Motion.focus(), label = "cover")
    val painter = if (file != null) rememberAsyncImagePainter(file) else null
    val loaded = painter?.state as? AsyncImagePainter.State.Success
    val ratio = loaded?.painter?.intrinsicSize?.let { if (it.height > 0) it.width / it.height else null }
        ?: DEFAULT_COVER_RATIO
    val radius = Radius.CARD_SMALL.u
    val edge = Size.FOCUS_RING_CARD.u
    val pulse = rememberGlowPulse()
    Box(
        Modifier
            .aspectRatio(ratio)
            .lift(progress, Focus.COVER_SCALE, Radius.CARD_SMALL)
            .then(if (focused) Modifier.drawBehind { neonGlow(Palette.pink, CornerRadius(radius.toPx()), edge.toPx(), pulse.value) } else Modifier)
            .clip(rounded(Radius.CARD_SMALL))
            .background(Palette.card)
            .alpha(COVER_DIM + (1f - COVER_DIM) * progress)
            .then(
                if (focused) Modifier.border(edge, Palette.pink, rounded(Radius.CARD_SMALL))
                else Modifier.border(Size.HAIRLINE.u, Palette.inkFaint, rounded(Radius.CARD_SMALL)),
            ),
        contentAlignment = Alignment.BottomStart,
    ) {
        if (painter != null && loaded != null) {
            Image(painter, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, colorFilter = LocalEra.current?.covers)
        } else {
            if (painter != null) {
                // Keeps the request alive until it loads.
                Image(painter, contentDescription = null, modifier = Modifier.fillMaxSize())
            }
            Txt(title, Type.cardTitle, Palette.ink, Modifier.padding(Space.CARD_PAD.u), maxLines = 4)
        }
        if (favorite) {
            Txt("★", Type.labelStrong, Palette.sun1, Modifier.align(Alignment.TopEnd).padding(Space.GAP.u), glow = Palette.sun2)
        }
        status?.let {
            Pill(
                Progress.label(it).substringBefore(" "),
                when (it) {
                    Progress.BEATEN -> Palette.ok
                    Progress.COMPLETED -> Palette.sun1
                    Progress.PLAYING -> Palette.cyan
                    else -> Palette.inkSecondary
                },
                Palette.inkOnLight, Type.badge, Space.SM, Space.SM, Modifier.padding(Space.GAP.u),
            )
        }
    }
}

/** The era skin of the game list being shown, if any. */
private val LocalEra = staticCompositionLocalOf<Era?> { null }

/** Unfocused covers sit back a little. */
private const val COVER_DIM = 0.82f
private const val CHIP_FILL = 0.12f

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
                    ChromeTxt(title, Type.screenTitle)
                }
            }
            Txt("$count ${if (count == 1) "game" else "games"}".uppercase(), Type.hint, Palette.cyanSoft, glow = Palette.cyan)
        }
        Spacer(Modifier.height(Space.ROW_GAP.u))
        TabPills(pills, selected, onPill)
    }
}

/** Arcade tab strip: pink-edged box, the selected tab lit up in pink. */
@Composable
fun TabPills(items: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    if (items.isEmpty()) return
    Row(
        Modifier.glass(Radius.CARD_SMALL).padding(Space.SM.u),
        horizontalArrangement = Arrangement.spacedBy(Space.SM.u),
    ) {
        val pulse = rememberGlowPulse()
        val radius = Radius.GLYPH_RECT.u
        val edge = Size.NEON_EDGE.u
        items.forEachIndexed { i, name ->
            val on = i == selected
            Box(
                Modifier
                    .then(if (on) Modifier.drawBehind { neonGlow(Palette.pink, CornerRadius(radius.toPx()), edge.toPx(), pulse.value) } else Modifier)
                    .clip(rounded(Radius.GLYPH_RECT))
                    .background(if (on) Palette.pink else Palette.transparent)
                    .tap { onSelect(i) }
                    .padding(vertical = Space.CHIP_V.u, horizontal = Space.PILL_H_SMALL.u),
            ) {
                Txt(name.uppercase(), Type.tab, if (on) Palette.white else Palette.inkSecondary, glow = if (on) Palette.chromeShadow else null)
            }
        }
    }
}

/** Trailing "Random" hint for game lists with more than one game. */
fun randomHint(count: Int, onRandom: () -> Unit): Hint? =
    if (count > 1) Hint(Pad.Select, "Random", trailing = true, onClick = onRandom) else null
