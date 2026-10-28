package io.github.hipsterbwp.rp5launcher.ui

import androidx.compose.foundation.Canvas
import kotlin.math.sin
import kotlin.math.PI
import java.util.Calendar
import kotlinx.coroutines.delay
import io.github.hipsterbwp.rp5launcher.ui.theme.Motion
import androidx.compose.ui.graphics.lerp
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GeoSize
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.toArgb
import io.github.hipsterbwp.rp5launcher.ui.theme.Palette
import io.github.hipsterbwp.rp5launcher.ui.theme.Size
import io.github.hipsterbwp.rp5launcher.ui.theme.u
import kotlin.random.Random

/** How the backdrop behaves: which scene, and whether it follows the clock and moves. */
data class SkySettings(val scene: String = SCENE_GRID, val live: Boolean = true)

val LocalSky = staticCompositionLocalOf { SkySettings() }

const val SCENE_GRID = "grid"
const val SCENE_CITY = "city"
const val SCENE_OCEAN = "ocean"
const val SCENE_SPACE = "space"

/** Colours of the sky at one moment of the day. */
private data class Sky(val top: Color, val mid: Color, val low: Color, val stars: Float, val sun: Float)

private val NIGHT = Sky(Color(0xFF04010D), Color(0xFF0D0430), Color(0xFF2A0A4E), 1f, 0f)
private val DAWN = Sky(Color(0xFF1B1450), Color(0xFF6B3A8C), Color(0xFFFF8FB1), 0.25f, 0.8f)
private val DAY = Sky(Color(0xFF1A0B45), Color(0xFF3A1680), Color(0xFF8A3AB0), 0.1f, 0f)
private val EVENING = Sky(Color(0xFF07021A), Color(0xFF140637), Color(0xFF3A0D5E), 0.7f, 1f)

/** Hour of day (0–24) → sky, blended between these key moments. */
private val DAY_CYCLE = listOf(
    0f to NIGHT, 5f to NIGHT, 6.5f to DAWN, 8.5f to DAY, 16.5f to DAY, 18f to EVENING, 21f to EVENING, 22.5f to NIGHT, 24f to NIGHT,
)

private fun skyAt(hour: Float): Sky {
    val (h0, a) = DAY_CYCLE.last { it.first <= hour }
    val (h1, b) = DAY_CYCLE.first { it.first >= hour }
    val t = if (h1 > h0) (hour - h0) / (h1 - h0) else 0f
    return Sky(lerp(a.top, b.top, t), lerp(a.mid, b.mid, t), lerp(a.low, b.low, t), a.stars + (b.stars - a.stars) * t, a.sun + (b.sun - a.sun) * t)
}

/**
 * The synthwave scene behind screens without game art. Live (the default), the sky follows the
 * clock — pastel dawn, purple day, a big striped sun at sunset, a deep starry night with the odd
 * shooting star — and the grid floor rolls towards you. [accent] colours the grid, horizon and haze
 * (each console's own neon on the console screen). Scenes: grid (mountains over a grid floor), city
 * (a lit skyline), ocean (the sun's reflection on the sea) and space (a ringed planet, no floor).
 * [sun] forces the sun out whatever the time.
 */
@Composable
fun RetroBackdrop(modifier: Modifier = Modifier, accent: Color = Palette.pink, sun: Boolean = false) {
    val settings = LocalSky.current
    val stars = remember {
        val r = Random(STAR_SEED)
        List(STAR_COUNT) { Triple(r.nextFloat(), r.nextFloat(), r.nextFloat()) }
    }
    val windows = remember {
        val r = Random(STAR_SEED + 1)
        List(BUILDINGS) { Triple(r.nextFloat(), 0.25f + r.nextFloat() * 0.75f, r.nextInt()) }
    }
    var hour by remember { mutableFloatStateOf(currentHour()) }
    LaunchedEffect(settings.live) {
        while (settings.live) {
            hour = currentHour()
            delay(SKY_REFRESH_MS)
        }
    }
    val sky = if (settings.live) skyAt(hour) else EVENING.copy(sun = 0f)
    val motion = rememberInfiniteTransition(label = "sky")
    val roll = motion.animateFloat(0f, 1f, infiniteRepeatable(tween(Motion.GRID_ROLL_MS, easing = LinearEasing)), label = "grid-roll")
    val line = Size.GRID_LINE.u
    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val space = settings.scene == SCENE_SPACE
        val horizon = if (space) h else h * HORIZON
        val px = line.toPx()
        val t = if (settings.live) roll.value else 0f
        drawRect(
            Brush.verticalGradient(
                0f to sky.top, SKY_MID_STOP to sky.mid, HORIZON to sky.low,
                HORIZON to if (space) sky.low else FLOOR_TOP, 1f to if (space) sky.mid else Palette.night,
            ),
        )
        if (!space) {
            drawRect(
                Brush.radialGradient(listOf(accent.copy(alpha = HAZE), Palette.transparent), center = Offset(w / 2, horizon), radius = w * 0.45f),
                topLeft = Offset(0f, horizon - h * 0.35f), size = GeoSize(w, h * 0.45f),
            )
        }
        stars.forEach { (x, y, z) ->
            val twinkle = if (settings.live) 0.75f + 0.25f * sin((t + z) * 2 * PI.toFloat()) else 1f
            drawCircle(
                Palette.white.copy(alpha = ((0.25f + z * 0.75f) * sky.stars.coerceAtLeast(if (space) 1f else 0f) * twinkle).coerceIn(0f, 1f)),
                radius = px * (if (z > 0.92f) 1.4f else 0.8f),
                center = Offset(x * w, y * (if (space) h else horizon * 0.85f)),
            )
        }
        // A shooting star now and then, when the sky is dark enough to see it.
        if (settings.live && sky.stars > 0.5f) {
            val now = System.currentTimeMillis()
            val cycle = now / SHOOTING_PERIOD_MS
            val phase = (now % SHOOTING_PERIOD_MS) / SHOOTING_MS.toFloat()
            if (phase < 1f) {
                val r = Random(cycle)
                val start = Offset(w * (0.2f + r.nextFloat() * 0.7f), horizon * (0.05f + r.nextFloat() * 0.3f))
                val dir = Offset(-w * 0.18f, horizon * 0.12f)
                val head = start + dir * phase
                val tail = start + dir * (phase - 0.25f).coerceAtLeast(0f)
                drawLine(Brush.linearGradient(listOf(Palette.transparent, Palette.white), tail, head), tail, head, px * 1.5f)
            }
        }
        if (space) {
            // Ringed planet and a nebula.
            drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.25f), Palette.transparent), Offset(w * 0.3f, h * 0.35f), w * 0.3f), w * 0.3f, Offset(w * 0.3f, h * 0.35f))
            val planet = Offset(w * 0.72f, h * 0.42f)
            val pr = h * 0.18f
            drawCircle(Brush.verticalGradient(listOf(Palette.sun3, Palette.sun4, Palette.violet), planet.y - pr, planet.y + pr), pr, planet)
            drawOval(Palette.cyan.copy(alpha = 0.7f), Offset(planet.x - pr * 1.9f, planet.y - pr * 0.28f), GeoSize(pr * 3.8f, pr * 0.56f), style = Stroke(px * 3))
            return@Canvas
        }
        val sunShown = if (sun) 1f else sky.sun
        val ocean = settings.scene == SCENE_OCEAN
        if (sunShown > 0f || ocean) {
            val radius = h * SUN_RADIUS
            val center = Offset(w / 2, horizon + radius * SUN_SINK)
            clipRect(bottom = horizon) {
                clipPath(Path().apply { addOval(Rect(center, radius)) }) {
                    // Solid top half, then bands that get thinner towards the horizon.
                    val top = center.y - radius
                    val sunBrush = Brush.verticalGradient(listOf(Palette.sun1, Palette.sun2, Palette.sun3, Palette.sun4), startY = top, endY = center.y + radius)
                    var y = top
                    var band = radius
                    var gap = radius * 0.03f
                    while (y < horizon) {
                        drawRect(sunBrush, Offset(center.x - radius, y), GeoSize(radius * 2, band), alpha = if (ocean) 1f else sunShown)
                        y += band + gap
                        band = (band * 0.55f).coerceAtLeast(radius * 0.08f)
                        gap *= 1.35f
                    }
                }
            }
        }
        if (settings.scene == SCENE_CITY) {
            // Skyline: blocks of different heights with lit windows.
            val bw = w / BUILDINGS
            windows.forEachIndexed { i, (_, height, seed) ->
                val bh = h * 0.22f * height
                val left = i * bw
                drawRect(Palette.deep, Offset(left, horizon - bh), GeoSize(bw * 0.92f, bh))
                drawLine(accent.copy(alpha = 0.5f), Offset(left, horizon - bh), Offset(left + bw * 0.92f, horizon - bh), px)
                val wr = Random(seed)
                var wy = horizon - bh + bw * 0.25f
                while (wy < horizon - bw * 0.2f) {
                    var wx = left + bw * 0.15f
                    while (wx < left + bw * 0.8f) {
                        if (wr.nextFloat() < 0.35f) drawRect(if (wr.nextBoolean()) Palette.sun1.copy(alpha = 0.7f) else Palette.cyanSoft.copy(alpha = 0.6f), Offset(wx, wy), GeoSize(bw * 0.12f, bw * 0.1f))
                        wx += bw * 0.22f
                    }
                    wy += bw * 0.22f
                }
            }
        } else {
            // Mountains with a cyan ridge line.
            val ridge = Path().apply {
                moveTo(0f, horizon)
                MOUNTAINS.forEach { (x, y) -> lineTo(x * w, horizon - y * h * 0.17f) }
                lineTo(w, horizon)
                close()
            }
            drawPath(ridge, Brush.verticalGradient(listOf(Palette.violet.copy(alpha = 0.6f), Palette.deep), startY = horizon - h * 0.17f, endY = horizon))
            drawPath(ridge, Palette.cyan.copy(alpha = 0.35f), style = Stroke(px))
        }
        if (ocean) {
            // Sea: the sun's reflection as shimmering stripes, dark water around it.
            drawRect(Brush.verticalGradient(listOf(Palette.violet, Palette.night), startY = horizon, endY = h), Offset(0f, horizon), GeoSize(w, h - horizon))
            val rows = OCEAN_ROWS
            for (i in 0 until rows) {
                val k = ((i + t) / rows)
                val y = horizon + (h - horizon) * k * k
                val half = w * (0.12f + 0.25f * k) * (0.7f + 0.3f * sin((i * 1.7f + t * 6f)))
                drawLine(
                    Brush.horizontalGradient(listOf(Palette.transparent, Palette.sun2, Palette.sun3, Palette.transparent), w / 2 - half, w / 2 + half),
                    Offset(w / 2 - half, y), Offset(w / 2 + half, y), px * (1 + 3 * k),
                )
            }
        } else {
            // Grid floor: lines fan out from the vanishing point; rows bunch up towards the horizon and roll in.
            val grid = accent.copy(alpha = GRID_ALPHA)
            for (i in -GRID_COLUMNS..GRID_COLUMNS) {
                val top = w / 2 + i * w * 0.035f
                val bottom = w / 2 + i * w * 0.16f
                drawLine(grid, Offset(top, horizon), Offset(bottom, h), px)
            }
            for (i in 0 until GRID_ROWS) {
                val k = (i + t) / GRID_ROWS
                val y = horizon + (h - horizon) * k * k
                drawLine(grid, Offset(0f, y), Offset(w, y), px)
            }
        }
        // Fade the floor into the haze near the horizon.
        drawRect(
            Brush.verticalGradient(listOf(FLOOR_TOP, Palette.transparent), startY = horizon, endY = horizon + (h - horizon) * 0.35f),
            topLeft = Offset(0f, horizon), size = GeoSize(w, (h - horizon) * 0.35f),
        )
        // Glowing horizon line.
        drawLine(accent.copy(alpha = 0.35f), Offset(0f, horizon), Offset(w, horizon), px * 6)
        drawLine(accent, Offset(0f, horizon), Offset(w, horizon), px * 1.5f)
    }
}

private fun currentHour(): Float {
    val c = Calendar.getInstance()
    return c.get(Calendar.HOUR_OF_DAY) + c.get(Calendar.MINUTE) / 60f
}

/**
 * CRT finish over the whole app: faint scanlines every other device pixel pair and darker corners.
 * Static, so it costs one tiled shader.
 */
@Composable
fun CrtOverlay() {
    val scan = remember {
        val bitmap = ImageBitmap(1, SCAN_PERIOD)
        val pixels = IntArray(SCAN_PERIOD) { if (it < SCAN_PERIOD / 2) SCAN_LINE.toArgb() else 0 }
        bitmap.asAndroidBitmap().setPixels(pixels, 0, 1, 0, 0, 1, SCAN_PERIOD)
        ShaderBrush(ImageShader(bitmap, TileMode.Repeated, TileMode.Repeated))
    }
    Box(Modifier.fillMaxSize().background(scan))
    Canvas(Modifier.fillMaxSize()) {
        drawRect(
            Brush.radialGradient(
                0.55f to Palette.transparent, 1f to Palette.black.copy(alpha = VIGNETTE),
                center = center, radius = size.maxDimension * 0.62f,
            ),
        )
    }
}

private val SKY_TOP = Color(0xFF07021A)
private val SKY_MID = Color(0xFF140637)
private val SKY_LOW = Color(0xFF3A0D5E)
private val FLOOR_TOP = Color(0xFF12042B)
private val SCAN_LINE = Color(0x2E000000)
private const val SCAN_PERIOD = 4
private const val VIGNETTE = 0.6f
private const val HORIZON = 0.63f
private const val SKY_MID_STOP = 0.4f
private const val HAZE = 0.45f
private const val SUN_RADIUS = 0.3f
private const val SUN_SINK = 0.28f
private const val GRID_ALPHA = 0.7f
private const val GRID_COLUMNS = 16
private const val GRID_ROWS = 12
private const val OCEAN_ROWS = 14
private const val BUILDINGS = 22
/** Re-read the clock this often for the live sky. */
private const val SKY_REFRESH_MS = 60_000L
/** A shooting star crosses for SHOOTING_MS once every SHOOTING_PERIOD_MS. */
private const val SHOOTING_PERIOD_MS = 14_000L
private const val SHOOTING_MS = 900
private const val STAR_COUNT = 140
private const val STAR_SEED = 7
private val MOUNTAINS = listOf(
    0.06f to 0.45f, 0.12f to 0.22f, 0.2f to 0.7f, 0.28f to 0.3f, 0.35f to 0.55f, 0.44f to 0.15f,
    0.52f to 0.6f, 0.6f to 0.28f, 0.68f to 0.8f, 0.77f to 0.34f, 0.85f to 0.62f, 0.93f to 0.25f, 1f to 0.5f,
)
