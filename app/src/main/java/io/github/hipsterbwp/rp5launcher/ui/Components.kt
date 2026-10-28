package io.github.hipsterbwp.rp5launcher.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.State
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import io.github.hipsterbwp.rp5launcher.ui.theme.Elevation
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GeoSize
import androidx.compose.ui.graphics.Color
import io.github.hipsterbwp.rp5launcher.ui.theme.Retro
import androidx.compose.foundation.layout.width
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import io.github.hipsterbwp.rp5launcher.ui.theme.Motion
import io.github.hipsterbwp.rp5launcher.ui.theme.Palette
import io.github.hipsterbwp.rp5launcher.ui.theme.Radius
import io.github.hipsterbwp.rp5launcher.ui.theme.Size
import io.github.hipsterbwp.rp5launcher.ui.theme.Space
import io.github.hipsterbwp.rp5launcher.ui.theme.Type
import io.github.hipsterbwp.rp5launcher.ui.theme.rounded
import io.github.hipsterbwp.rp5launcher.ui.theme.u

// ---------------------------------------------------------------------------------------------
// Text
// ---------------------------------------------------------------------------------------------

/** Single entry point for text: always truncates gracefully instead of clipping. [glow] adds a neon halo. */
@Composable
fun Txt(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    glow: Color? = null,
) {
    val blur = with(LocalDensity.current) { Size.TEXT_GLOW.u.toPx() }
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = color, shadow = glow?.let { Shadow(it, Offset.Zero, blur) }),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Big title in the sunset gradient with a hard purple drop shadow, like an arcade marquee. */
@Composable
fun ChromeTxt(text: String, style: TextStyle, modifier: Modifier = Modifier, maxLines: Int = 1) {
    val drop = with(LocalDensity.current) { Size.CHROME_DROP.u.toPx() }
    BasicText(
        text = text.uppercase(),
        modifier = modifier,
        style = style.copy(brush = Retro.sunset, shadow = Shadow(Palette.chromeShadow, Offset(0f, drop), 0f)),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

// ---------------------------------------------------------------------------------------------
// Images
// ---------------------------------------------------------------------------------------------

/** Scraped art or a logo from local storage; fades in, draws nothing while loading. */
@Composable
fun ArtImage(
    file: File?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    tint: Color? = null,
) {
    if (file == null) return
    val context = LocalContext.current
    val request = remember(file) {
        ImageRequest.Builder(context).data(file).crossfade(Motion.SCREEN_FADE_MS).build()
    }
    AsyncImage(
        model = request,
        contentDescription = null,
        modifier = modifier,
        contentScale = contentScale,
        colorFilter = tint?.let { ColorFilter.tint(it) },
    )
}

/**
 * Full-size screenshot behind a screen or panel, cross-fading as the selection changes, under a
 * flat translucent layer of [scrim] so text on top keeps its contrast.
 */
@Composable
fun GameplayBackdrop(snap: File?, scrim: Color, scrimAlpha: Float) {
    Crossfade(targetState = snap, animationSpec = Motion.colorFade(), label = "backdrop") { file ->
        Box(Modifier.fillMaxSize()) {
            ArtImage(file, Modifier.fillMaxSize(), ContentScale.Crop)
        }
    }
    Box(Modifier.fillMaxSize().background(scrim.copy(alpha = scrimAlpha)))
}

// ---------------------------------------------------------------------------------------------
// Glass and depth
// ---------------------------------------------------------------------------------------------

/** Dark violet surface with a thin neon edge (pills, captions, small panels). */
@Composable
fun Modifier.glass(radius: Int, fill: Color = Palette.glass): Modifier {
    val shape = rounded(radius)
    return clip(shape).background(fill).border(Size.HAIRLINE.u, Palette.glassEdge, shape)
}

/**
 * Main panels: dark fill, glowing neon [edge] and two corner brackets in [corner] (top-left and
 * bottom-right), like the frames in arcade menus. The glow and brackets draw outside the bounds.
 */
@Composable
fun Modifier.neonPanel(
    radius: Int,
    edge: Color = Palette.pink,
    corner: Color = Palette.cyan,
    fill: Color = Palette.glass,
): Modifier {
    val r = radius.u
    val width = Size.NEON_EDGE.u
    val bracket = Size.NEON_BRACKET.u
    val bracketWidth = Size.NEON_BRACKET_W.u
    val offset = Size.NEON_BRACKET_OFFSET.u
    return drawBehind {
        val cr = CornerRadius(r.toPx())
        neonGlow(edge, cr, width.toPx())
        drawRoundRect(fill, cornerRadius = cr)
    }.drawWithContent {
        drawContent()
        val w = width.toPx()
        drawRoundRect(edge, Offset(w / 2, w / 2), GeoSize(size.width - w, size.height - w), CornerRadius(r.toPx()), Stroke(w))
        val o = offset.toPx()
        val len = bracket.toPx()
        val rad = r.toPx().coerceAtMost(len * 0.6f)
        val brackets = Path().apply {
            moveTo(-o, -o + len); lineTo(-o, -o + rad); quadraticTo(-o, -o, -o + rad, -o); lineTo(-o + len, -o)
            val x = size.width + o
            val y = size.height + o
            moveTo(x, y - len); lineTo(x, y - rad); quadraticTo(x, y, x - rad, y); lineTo(x - len, y)
        }
        val bw = bracketWidth.toPx()
        drawPath(brackets, corner.copy(alpha = GLOW_ALPHA), style = Stroke(bw * 3, cap = StrokeCap.Round))
        drawPath(brackets, corner, style = Stroke(bw, cap = StrokeCap.Round))
    }
}

/**
 * Soft neon halo around a rounded rect: a few widening translucent strokes. [intensity] (0..1, from
 * [rememberGlowPulse] on focused items) makes it breathe: brighter and wider at the peak.
 */
fun DrawScope.neonGlow(color: Color, cornerRadius: CornerRadius, width: Float, intensity: Float = 1f) {
    val spread = width * (1f + intensity)
    for (i in GLOW_STEPS downTo 1) {
        drawRoundRect(
            color.copy(alpha = color.alpha * GLOW_ALPHA * (0.5f + intensity * 0.6f) / i),
            cornerRadius = cornerRadius,
            style = Stroke(width + i * spread * 1.5f),
        )
    }
}

/**
 * Breathing 0.35 → 1 → 0.35 for focus glows. Read `.value` inside draw blocks so the pulse only
 * redraws. Every caller runs on the same clock, so glows on screen pulse together.
 */
@Composable
fun rememberGlowPulse(): State<Float> = rememberInfiniteTransition(label = "glow").animateFloat(
    initialValue = PULSE_LOW,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(Motion.GLOW_PULSE_MS, easing = FastOutSlowInEasing), RepeatMode.Reverse),
    label = "glow-pulse",
)

private const val PULSE_LOW = 0.35f

private const val GLOW_STEPS = 3
private const val GLOW_ALPHA = 0.35f

/** 1-unit light line along the top edge, like a glass toolbar. */
@Composable
fun Modifier.hairlineTop(): Modifier {
    val width = Size.HAIRLINE.u
    return drawBehind {
        drawLine(Palette.glassEdge, Offset(0f, 0f), Offset(size.width, 0f), width.toPx())
    }
}

/**
 * tvOS-style focus lift: grows by [scale] at full [progress] and casts a soft shadow, drawn above
 * neighbours. Apply before clipping so the shadow isn't cut off.
 */
@Composable
fun Modifier.lift(progress: Float, scale: Float, radius: Int): Modifier {
    val elevation = (Elevation.CARD + (Elevation.LIFTED - Elevation.CARD) * progress).u
    val s = 1f + (scale - 1f) * progress
    return zIndex(progress)
        .graphicsLayer { scaleX = s; scaleY = s }
        .shadow(elevation, rounded(radius), clip = false)
}

// ---------------------------------------------------------------------------------------------
// Focus
// ---------------------------------------------------------------------------------------------

/** Draws a rounded outline [offset] outside the element's bounds, faded by [alpha]. */
fun Modifier.focusRing(alpha: Float, color: Color, width: Dp, offset: Dp, radius: Dp): Modifier =
    drawWithContent {
        drawContent()
        if (alpha <= 0f) return@drawWithContent
        val w = width.toPx()
        val inset = offset.toPx() + w / 2
        drawRoundRect(
            color = color.copy(alpha = color.alpha * alpha),
            topLeft = Offset(-inset, -inset),
            size = GeoSize(size.width + inset * 2, size.height + inset * 2),
            cornerRadius = CornerRadius(radius.toPx() + inset),
            style = Stroke(w),
        )
    }

/** Focus ring: neon pink outline (the retro theme highlights everything the same way). */
@Composable
fun Modifier.lightFocus(focused: Boolean, @Suppress("UNUSED_PARAMETER") accent: Color, radius: Int): Modifier {
    val alpha by animateFloatAsState(if (focused) 1f else 0f, Motion.focus(), label = "ring")
    return focusRing(alpha, Palette.pink, Size.FOCUS_RING_LIGHT.u, Size.FOCUS_OFFSET.u, radius.u)
}

/** Touch is a bonus: tap without ripples (no shadows/gradients per DESIGN.md). */
@Composable
fun Modifier.tap(onClick: () -> Unit): Modifier =
    clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)

// ---------------------------------------------------------------------------------------------
// Hint bar
// ---------------------------------------------------------------------------------------------

data class Hint(
    val buttons: List<Pad>,
    val label: String,
    val trailing: Boolean = false,
    val onClick: (() -> Unit)? = null,
) {
    constructor(button: Pad, label: String, trailing: Boolean = false, onClick: (() -> Unit)? = null) :
        this(listOf(button), label, trailing, onClick)
}

/**
 * Controller hint bar, always at the bottom. [onSystem] picks the translucent bar used over a
 * system color; otherwise the ink bar of light screens. [glyphInk] colors the glyph letters.
 */
@Composable
fun BoxScope.HintBar(hints: List<Hint>, onSystem: Boolean, glyphInk: Color) {
    Row(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth()
            .height(Size.HINT_BAR.u)
            .background(Brush.verticalGradient(listOf(Palette.transparent, Palette.night.copy(alpha = HINT_FADE))))
            .padding(horizontal = Space.SCREEN_H.u),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.HINT_GAP.u),
    ) {
        hints.filterNot { it.trailing }.forEach { HintItem(it, glyphInk) }
        Spacer(Modifier.weight(1f))
        hints.filter { it.trailing }.forEach { HintItem(it, glyphInk) }
    }
}

@Composable
private fun HintItem(hint: Hint, glyphInk: Color) {
    Row(
        modifier = if (hint.onClick != null) Modifier.tap(hint.onClick) else Modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.GAP.u),
    ) {
        ButtonGlyph(hint.buttons, glyphInk)
        Txt(hint.label.uppercase(), Type.hint, Palette.ink)
    }
}

/**
 * Arcade buttons: candy-coloured round buttons for A/B/X/Y (red, yellow, blue, green) with a soft
 * glow, and a lavender key cap for shoulder buttons, START and SELECT.
 */
@Composable
fun ButtonGlyph(buttons: List<Pad>, ink: Color) {
    val labelFor = LocalButtonLabel.current
    val label = buttons.joinToString(" ") { labelFor(it) }
    val face = buttons.singleOrNull()?.let { faceColor(it, label) }
    if (face != null) {
        Box(
            modifier = Modifier
                .size(Size.GLYPH.u)
                .drawBehind {
                    drawCircle(face.copy(alpha = GLOW_ALPHA), radius = size.minDimension * 0.72f)
                    drawCircle(Brush.verticalGradient(listOf(lerp(face, Palette.white, 0.35f), face, lerp(face, Palette.black, 0.3f))))
                },
            contentAlignment = Alignment.Center,
        ) {
            Txt(label, Type.glyph, Palette.white, glow = Palette.black)
        }
    } else {
        Box(
            modifier = Modifier
                .height(Size.GLYPH.u)
                .clip(rounded(Radius.GLYPH_RECT))
                .background(Brush.verticalGradient(listOf(Palette.buttonOther, lerp(Palette.buttonOther, Palette.black, 0.3f))))
                .padding(horizontal = Space.GLYPH_RECT_H.u),
            contentAlignment = Alignment.Center,
        ) {
            Txt(label, Type.glyphWide, ink)
        }
    }
}

/** Colour of a face button: by symbol for PlayStation glyphs (as printed on the device), else by letter. */
private fun faceColor(button: Pad, label: String): Color? = when (label) {
    "✕" -> Palette.buttonCross
    "○" -> Palette.buttonCircle
    "□" -> Palette.buttonSquare
    "△" -> Palette.buttonTriangle
    else -> letterColor(button)
}

private fun letterColor(button: Pad): Color? = when (button) {
    Pad.A -> Palette.buttonA
    Pad.B -> Palette.buttonB
    Pad.X -> Palette.buttonX
    Pad.Y -> Palette.buttonY
    else -> null
}

private const val HINT_FADE = 0.92f
private const val PILL_EDGE = 0.45f

// ---------------------------------------------------------------------------------------------
// Small pieces
// ---------------------------------------------------------------------------------------------

@Composable
fun Pill(
    text: String,
    background: Color,
    ink: Color,
    style: TextStyle = Type.pill,
    padV: Int = Space.TAG_V,
    padH: Int = Space.CHIP_H,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(rounded(Radius.GLYPH_RECT))
            .background(background)
            .border(Size.HAIRLINE.u, ink.copy(alpha = PILL_EDGE), rounded(Radius.GLYPH_RECT))
            .padding(vertical = padV.u, horizontal = padH.u),
    ) {
        Txt(text.uppercase(), style, ink)
    }
}

/** 34 px rounded square in the system color with its short name. */
@Composable
fun SystemBadge(shortName: String, color: Color) {
    Box(
        modifier = Modifier
            .size(Size.BADGE.u)
            .clip(rounded(Radius.BADGE))
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Txt(shortName, Type.badge, Palette.white)
    }
}

/**
 * List row like an arcade menu entry: dark card; when focused, a pink fade with a glowing pink edge
 * and a yellow ▶ cursor in front.
 */
@Composable
fun ListRow(
    focused: Boolean,
    @Suppress("UNUSED_PARAMETER") accent: Color,
    modifier: Modifier = Modifier,
    background: Color = Palette.card,
    padV: Int = Space.ROW_V,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = rounded(Radius.CARD_SMALL)
    val edge = Size.NEON_EDGE.u
    val radius = Radius.CARD_SMALL.u
    val pulse = rememberGlowPulse()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (focused) Modifier.drawBehind { neonGlow(Palette.pink, CornerRadius(radius.toPx()), edge.toPx() / 2, pulse.value) }
                else Modifier
            )
            .clip(shape)
            .background(background)
            .then(if (focused) Modifier.background(Retro.focusFill).border(edge, Palette.pink, shape) else Modifier)
            .then(if (onClick != null) Modifier.tap(onClick) else Modifier)
            .heightIn(min = Size.BADGE.u)
            .padding(vertical = padV.u, horizontal = Space.ROW_H.u),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.ROW_GAP.u),
    ) {
        Box(Modifier.width(Size.CURSOR_W.u)) {
            if (focused) Txt("▶", Type.menu, Palette.sun1, glow = Palette.sun2)
        }
        content()
    }
}

/** Filled pill button in the system color. */
@Composable
fun PrimaryButton(
    text: String,
    accent: Color,
    focused: Boolean,
    height: Int = Size.BUTTON,
    padH: Int = Space.BUTTON_H,
    style: TextStyle = Type.button,
    leading: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .lightFocus(focused, accent, height / 2)
            .height(height.u)
            .clip(rounded(Radius.CARD_SMALL))
            .background(Brush.verticalGradient(listOf(Palette.pinkSoft, Palette.pink)))
            .tap(onClick)
            .padding(horizontal = padH.u),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.BUTTON_GAP.u),
    ) {
        leading?.invoke()
        Txt(text.uppercase(), style, Palette.white, glow = Palette.chromeShadow)
    }
}

@Composable
fun PlayIcon(color: Color) {
    Canvas(Modifier.size(Size.PLAY_ICON.u)) {
        val w = size.width
        val path = Path().apply {
            moveTo(w * 7 / 24, w * 4 / 24)
            lineTo(w * 7 / 24, w * 20 / 24)
            lineTo(w * 20 / 24, w * 12 / 24)
            close()
        }
        drawPath(path, color)
    }
}

@Composable
fun ChevronIcon(color: Color) {
    Canvas(Modifier.size(Size.CHEVRON.u)) {
        val w = size.width
        val path = Path().apply {
            moveTo(w * 9 / 24, w * 6 / 24)
            lineTo(w * 15 / 24, w * 12 / 24)
            lineTo(w * 9 / 24, w * 18 / 24)
        }
        drawPath(path, color, style = Stroke(width = w / 12, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

// ---------------------------------------------------------------------------------------------
// Scrolling
// ---------------------------------------------------------------------------------------------

/** Smoothly scrolls just enough to show [index] fully. */
suspend fun LazyGridState.bringIntoView(index: Int) {
    val info = layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.index == index }
    if (item == null) {
        animateScrollToItem(index)
        return
    }
    val top = item.offset.y
    val bottom = top + item.size.height
    val end = info.viewportEndOffset - info.afterContentPadding
    when {
        top < 0 -> animateScrollBy(top.toFloat())
        bottom > end -> animateScrollBy((bottom - end).toFloat())
    }
}

suspend fun LazyListState.bringIntoView(index: Int) {
    val info = layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.index == index }
    if (item == null) {
        animateScrollToItem(index)
        return
    }
    val start = item.offset
    val endEdge = start + item.size
    val end = info.viewportEndOffset - info.afterContentPadding
    when {
        start < 0 -> animateScrollBy(start.toFloat())
        endEdge > end -> animateScrollBy((endEdge - end).toFloat())
    }
}
