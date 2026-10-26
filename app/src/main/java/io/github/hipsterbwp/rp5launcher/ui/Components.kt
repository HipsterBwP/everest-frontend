package io.github.hipsterbwp.rp5launcher.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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

/** Single entry point for text: always truncates gracefully instead of clipping. */
@Composable
fun Txt(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = color),
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

/** Focus ring for light screens: 3 px system-color outline. */
@Composable
fun Modifier.lightFocus(focused: Boolean, accent: Color, radius: Int): Modifier {
    val alpha by animateFloatAsState(if (focused) 1f else 0f, Motion.focus(), label = "ring")
    return focusRing(alpha, accent, Size.FOCUS_RING_LIGHT.u, Size.FOCUS_OFFSET.u, radius.u)
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
            .background(if (onSystem) Palette.hintBarOnSystem else Palette.hintBarLight)
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
        Txt(hint.label, Type.label, Palette.white)
    }
}

/** 24 px white circle for face buttons; rounded rect for shoulder/START/SELECT. */
@Composable
fun ButtonGlyph(buttons: List<Pad>, ink: Color) {
    val round = buttons.size == 1 && buttons[0].glyph.length == 1
    val label = buttons.joinToString(" ") { it.glyph }
    Box(
        modifier = Modifier
            .then(
                if (round) Modifier.size(Size.GLYPH.u).clip(rounded(Radius.PILL))
                else Modifier.height(Size.GLYPH.u).clip(rounded(Radius.GLYPH_RECT))
            )
            .background(Palette.white)
            .padding(horizontal = if (round) 0.u else Space.GLYPH_RECT_H.u),
        contentAlignment = Alignment.Center,
    ) {
        Txt(label, if (round) Type.glyph else Type.glyphWide, ink)
    }
}

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
            .clip(rounded(Radius.PILL))
            .background(background)
            .padding(vertical = padV.u, horizontal = padH.u),
    ) {
        Txt(text, style, ink)
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

/** White row card on light screens with a system-color focus outline. */
@Composable
fun ListRow(
    focused: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    background: Color = Palette.card,
    padV: Int = Space.ROW_V,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .lightFocus(focused, accent, Radius.CARD_SMALL)
            .clip(rounded(Radius.CARD_SMALL))
            .background(background)
            .then(if (onClick != null) Modifier.tap(onClick) else Modifier)
            .heightIn(min = Size.BADGE.u)
            .padding(vertical = padV.u, horizontal = Space.ROW_H.u),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.ROW_GAP.u),
    ) { content() }
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
            .clip(rounded(Radius.PILL))
            .background(accent)
            .tap(onClick)
            .padding(horizontal = padH.u),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.BUTTON_GAP.u),
    ) {
        leading?.invoke()
        Txt(text, style, Palette.white)
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
