package io.github.hipsterbwp.rp5launcher.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import io.github.hipsterbwp.rp5launcher.ui.theme.Motion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Seasonal weather over the console screen and Base Camp: bats for Halloween, snow in winter,
 * fireworks at New Year. Decorative; touch passes through.
 */
@Composable
fun SeasonalSky(season: Season) {
    val t by rememberInfiniteTransition(label = "season").animateFloat(
        0f, 1f, infiniteRepeatable(tween(Motion.SEASON_LOOP_MS, easing = LinearEasing)), label = "season-t",
    )
    Canvas(Modifier.fillMaxSize()) {
        when (season) {
            Season.Halloween -> bats(t)
            Season.Winter -> snow(t)
            Season.NewYear -> fireworks(t)
        }
    }
}

/** Golden-ratio spread so particles never line up. */
private fun spread(i: Int) = (i * 0.618034f) % 1f

private fun DrawScope.snow(t: Float) {
    for (i in 0 until SNOWFLAKES) {
        val speed = 0.6f + spread(i * 7) * 0.8f
        val y = ((spread(i) + t * speed * 3f) % 1f) * size.height
        val x = spread(i * 3) * size.width + sin((t * 6f + i) * 2f) * 14f
        drawCircle(Color.White.copy(alpha = 0.35f + spread(i * 5) * 0.45f), 1.5f + spread(i * 11) * 2.5f, Offset(x, y))
    }
}

private val BAT = listOf(
    "W.........W",
    "WW..W.W..WW",
    "WWWWWWWWWWW",
    ".WWWWWWWWW.",
    "..W..W..W..",
)
private val BAT_UP = listOf(
    "...........",
    "....W.W....",
    "WWWWWWWWWWW",
    "WWWWWWWWWWW",
    "W.........W",
)

private fun DrawScope.bats(t: Float) {
    val px = 3f
    for (i in 0 until BATS) {
        val f = (t * (1.2f + spread(i) * 0.8f) + spread(i * 5)) % 1f
        val x = -40f + (size.width + 80f) * f
        val y = size.height * (0.1f + spread(i * 3) * 0.35f) + sin(f * 18f + i) * 18f
        val flap = ((t * 40f + i).toInt() % 2 == 0)
        val rows = if (flap) BAT else BAT_UP
        val c = Color(0xFF1B0B2E)
        rows.forEachIndexed { ry, row ->
            row.forEachIndexed { rx, ch -> if (ch != '.') drawRect(c, Offset(x + rx * px, y + ry * px), androidx.compose.ui.geometry.Size(px, px)) }
        }
    }
    // A low orange moon glow.
    drawCircle(Color(0x22FF7A1A), size.minDimension * 0.25f, Offset(size.width * 0.85f, size.height * 0.15f))
}

private val FIREWORK_COLORS = listOf(Color(0xFFFFD84A), Color(0xFFFF4FD8), Color(0xFF39E0FF), Color(0xFF7CFF6A), Color(0xFFFF7A1A))

private fun DrawScope.fireworks(t: Float) {
    for (i in 0 until FIREWORKS) {
        val f = (t * 3f + spread(i * 7)) % 1f
        val cx = size.width * (0.15f + spread(i) * 0.7f)
        val cy = size.height * (0.12f + spread(i * 3) * 0.3f)
        val color = FIREWORK_COLORS[i % FIREWORK_COLORS.size]
        if (f < 0.25f) {
            // Rocket going up.
            val y = size.height - (size.height - cy) * (f / 0.25f)
            drawCircle(color, 2.5f, Offset(cx, y))
        } else {
            val burst = (f - 0.25f) / 0.75f
            val r = size.minDimension * 0.12f * burst
            for (k in 0 until SPARKS) {
                val a = k * 2 * PI.toFloat() / SPARKS
                drawCircle(color.copy(alpha = (1f - burst).coerceIn(0f, 1f)), 2.5f, Offset(cx + cos(a) * r, cy + sin(a) * r + burst * burst * 30f))
            }
        }
    }
}

private const val SNOWFLAKES = 90
private const val BATS = 6
private const val FIREWORKS = 4
private const val SPARKS = 16
