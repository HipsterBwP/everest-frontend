package io.github.hipsterbwp.rp5launcher.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GeoSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import io.github.hipsterbwp.rp5launcher.ui.theme.Motion
import io.github.hipsterbwp.rp5launcher.ui.theme.Palette
import io.github.hipsterbwp.rp5launcher.ui.theme.Size
import io.github.hipsterbwp.rp5launcher.ui.theme.u
import kotlin.math.abs
import kotlin.math.sin

/** What Yeti-1, the mascot, is doing. */
enum class Mood { Walk, Jump, Cheer, Sleep }

/** Yeti-1's 16×16 frames: W fur, S fur shade, B face, E eyes, M mouth; an outline is added when drawn. */
object YetiSprites {
    val walk1 = listOf(
        "................",
        ".....WWWWWW.....",
        "....WWWWWWWW....",
        "...WWWWWWWWWW...",
        "...WWBBBBBBWW...",
        "...WBBEBBEBBW...",
        "...WBBBBBBBBW...",
        "...WWBBMMBBWW...",
        "..WWWWWWWWWWWW..",
        ".WWSWWWWWWWWSWW.",
        ".WW.WWWWWWWW.WW.",
        "....WWWWWWWW....",
        "....WSWWWWSW....",
        "....WWW..WWW....",
        "...WWW....WWW...",
        "................",
    )
    val walk2 = listOf(
        "................",
        "................",
        ".....WWWWWW.....",
        "....WWWWWWWW....",
        "...WWWWWWWWWW...",
        "...WWBBBBBBWW...",
        "...WBBEBBEBBW...",
        "...WBBBBBBBBW...",
        "...WWBBMMBBWW...",
        "..WWWWWWWWWWWW..",
        ".WWSWWWWWWWWSWW.",
        ".WW.WWWWWWWW.WW.",
        "....WSWWWWSW....",
        ".....WWWWWW.....",
        ".....WW..WW.....",
        "................",
    )
    val jump = listOf(
        ".WW..........WW.",
        ".WW..WWWWWW..WW.",
        ".WWWWWWWWWWWWWW.",
        "..WWWWWWWWWWWW..",
        "...WWBBBBBBWW...",
        "...WBBEBBEBBW...",
        "...WBBBBBBBBW...",
        "...WWBBMMBBWW...",
        "....WWWWWWWW....",
        "....WSWWWWSW....",
        "....WWWWWWWW....",
        "....WWWWWWWW....",
        "...WWW....WWW...",
        "..WWW......WWW..",
        "................",
        "................",
    )
    val cheer = listOf(
        ".WW..........WW.",
        ".WW..WWWWWW..WW.",
        ".WWWWWWWWWWWWWW.",
        "..WWWWWWWWWWWW..",
        "...WWBBBBBBWW...",
        "...WBBEBBEBBW...",
        "...WBBBBBBBBW...",
        "...WWBMMMMBWW...",
        "....WWBMMBWW....",
        "....WSWWWWSW....",
        "....WWWWWWWW....",
        "....WWWWWWWW....",
        "....WSWWWWSW....",
        "....WWW..WWW....",
        "...WWW....WWW...",
        "................",
    )
    val sleep = listOf(
        "................",
        "................",
        "................",
        "................",
        ".....WWWWWW.....",
        "....WWWWWWWW....",
        "...WWBBBBBBWW...",
        "...WBBBBBBBBW...",
        "...WBEEBBEEBW...",
        "...WWBBBBBBWW...",
        "..WWWWWWWWWWWW..",
        ".WWWWWWWWWWWWWW.",
        ".WWSWWWWWWWWSWW.",
        ".WWWWWWWWWWWWWW.",
        "..WWWW....WWWW..",
        "................",
    )
}

private val FUR = Color(0xFFF6ECFF)
private val FUR_SHADE = Color(0xFFBDA6E6)
private val FACE = Color(0xFF7FB2FF)
private val EYES = Color(0xFF1B1035)
private val MOUTH = Color(0xFFFF6FC3)

/** Draws one sprite frame [px] pixels per sprite pixel, outlined, optionally mirrored. [colors] overrides the yeti palette. */
fun DrawScope.drawSprite(rows: List<String>, px: Float, origin: Offset, mirror: Boolean = false, colors: Map<Char, Color>? = null) {
    val h = rows.size
    val w = rows.maxOfOrNull { it.length } ?: 0
    fun filled(x: Int, y: Int) = y in 0 until h && x in 0 until rows[y].length && rows[y][x] != '.'
    for (y in -1..h) for (x in -1..w) {
        val c = if (filled(x, y)) {
            val ch = rows[y][x]
            colors?.get(ch) ?: when (ch) {
                'S' -> FUR_SHADE
                'B' -> FACE
                'E' -> EYES
                'M' -> MOUTH
                else -> FUR
            }
        } else if (filled(x + 1, y) || filled(x - 1, y) || filled(x, y + 1) || filled(x, y - 1)) EYES else continue
        val dx = if (mirror) w - 1 - x else x
        drawRect(c, Offset(origin.x + dx * px, origin.y + y * px), GeoSize(px + 0.5f, px + 0.5f))
    }
}

/** Draws [outfit] on a yeti [frame] drawn at [origin]: on top of the head, or across the eyes. */
fun DrawScope.drawOutfit(outfit: Outfit, frame: List<String>, px: Float, origin: Offset, mirror: Boolean) {
    val w = outfit.rows.maxOf { it.length }
    val headTop = frame.indexOfFirst { row -> row.substring(5, 11).any { it != '.' } }.coerceAtLeast(0)
    val eyes = frame.indexOfFirst { 'E' in it }.coerceAtLeast(headTop)
    val top = if (outfit.onEyes) eyes - outfit.rows.size / 2 else headTop + 2 - outfit.rows.size
    val left = (YETI_SIZE - w) / 2
    val rows = if (mirror) outfit.rows.map { it.padEnd(w, '.').reversed() } else outfit.rows
    val x = if (mirror) YETI_SIZE - left - w else left
    drawSprite(rows, px, Offset(origin.x + x * px, origin.y + top * px), colors = outfit.colors)
}

/**
 * Yeti-1 strolling back and forth along the top of the hint bar: two-frame walk, a hop when a game
 * starts, arms up when a banner shows. Purely decorative; touch passes through.
 */
@Composable
fun Mascot(mood: Mood, outfit: Outfit? = null, scale: String = "normal") {
    val time = rememberInfiniteTransition(label = "yeti")
    val stroll = time.animateFloat(0f, 1f, infiniteRepeatable(tween(Motion.YETI_STROLL_MS, easing = LinearEasing)), label = "yeti-x")
    val step = time.animateFloat(0f, 1f, infiniteRepeatable(tween(Motion.YETI_STEP_MS, easing = LinearEasing)), label = "yeti-step")
    val pixel = when (scale) {
        "big" -> Size.YETI_PIXEL * 3
        "tiny" -> 1
        else -> Size.YETI_PIXEL
    }.u
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val sprite = pixel * YETI_SIZE
        Canvas(
            Modifier
                .align(Alignment.BottomStart)
                .offset(y = -(Size.HINT_BAR.u - Size.YETI_SINK.u))
                .size(maxWidth, sprite + pixel * 10),
        ) {
            val px = pixel.toPx()
            val w = sprite.toPx()
            // Back and forth across the screen: 0→1→0 over one stroll.
            val t = stroll.value
            val along = if (t < 0.5f) t * 2 else (1 - t) * 2
            val x = (size.width - w) * along
            val facingLeft = t >= 0.5f
            val walking = mood == Mood.Walk
            val hop = when (mood) {
                Mood.Jump -> abs(sin(step.value * Math.PI * 2)).toFloat() * px * 5
                Mood.Cheer -> abs(sin(step.value * Math.PI * 4)).toFloat() * px * 2
                else -> 0f
            }
            val frame = when (mood) {
                Mood.Walk -> if (step.value < 0.5f) YetiSprites.walk1 else YetiSprites.walk2
                Mood.Jump -> YetiSprites.jump
                Mood.Cheer -> YetiSprites.cheer
                Mood.Sleep -> YetiSprites.sleep
            }
            // Asleep in attract mode: tucked into the bottom-right corner.
            val at = if (mood == Mood.Sleep) size.width - w - px * 8 else x
            val origin = Offset(at, size.height - w - hop)
            val mirror = facingLeft && walking
            drawSprite(frame, px, origin, mirror = mirror)
            outfit?.let { drawOutfit(it, frame, px, origin, mirror) }
        }
    }
}

private const val YETI_SIZE = 16
