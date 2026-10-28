package io.github.hipsterbwp.rp5launcher.ui

import androidx.compose.foundation.Canvas
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

/**
 * The synthwave scene behind screens without game art: a night sky with stars, a pink haze on the
 * horizon, a line of cyan-edged mountains and a neon grid floor running to the horizon. [sun] adds
 * the striped sunset sun (loading screen).
 */
@Composable
fun RetroBackdrop(modifier: Modifier = Modifier, sun: Boolean = false) {
    val stars = remember {
        val r = Random(STAR_SEED)
        List(STAR_COUNT) { Triple(r.nextFloat(), r.nextFloat(), r.nextFloat()) }
    }
    val line = Size.GRID_LINE.u
    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val horizon = h * HORIZON
        // Sky above, dark floor below.
        drawRect(
            Brush.verticalGradient(
                0f to SKY_TOP, SKY_MID_STOP to SKY_MID, HORIZON to SKY_LOW,
                HORIZON to FLOOR_TOP, 1f to Palette.night,
            ),
        )
        drawRect(
            Brush.radialGradient(
                listOf(Palette.pink.copy(alpha = HAZE), Palette.transparent),
                center = Offset(w / 2, horizon), radius = w * 0.45f,
            ),
            topLeft = Offset(0f, horizon - h * 0.35f), size = GeoSize(w, h * 0.45f),
        )
        stars.forEach { (x, y, z) ->
            drawCircle(Palette.white.copy(alpha = 0.25f + z * 0.75f), radius = line.toPx() * (if (z > 0.92f) 1.4f else 0.8f), center = Offset(x * w, y * horizon * 0.85f))
        }
        if (sun) {
            val radius = h * SUN_RADIUS
            val center = Offset(w / 2, horizon + radius * SUN_SINK)
            clipRect(bottom = horizon) {
                clipPath(Path().apply { addOval(Rect(center, radius)) }) {
                    // Solid top half, then bands that get thinner towards the horizon.
                    val top = center.y - radius
                    val sunBrush = Brush.verticalGradient(
                        listOf(Palette.sun1, Palette.sun2, Palette.sun3, Palette.sun4),
                        startY = top, endY = center.y + radius,
                    )
                    var y = top
                    var band = radius
                    var gap = radius * 0.03f
                    while (y < horizon) {
                        drawRect(sunBrush, Offset(center.x - radius, y), GeoSize(radius * 2, band))
                        y += band + gap
                        band = (band * 0.55f).coerceAtLeast(radius * 0.08f)
                        gap *= 1.35f
                    }
                }
            }
        }
        // Mountains with a cyan ridge line.
        val ridge = Path().apply {
            moveTo(0f, horizon)
            MOUNTAINS.forEach { (x, y) -> lineTo(x * w, horizon - y * h * 0.17f) }
            lineTo(w, horizon)
            close()
        }
        drawPath(ridge, Brush.verticalGradient(listOf(Palette.violet.copy(alpha = 0.6f), Palette.deep), startY = horizon - h * 0.17f, endY = horizon))
        drawPath(ridge, Palette.cyan.copy(alpha = 0.35f), style = Stroke(line.toPx()))
        // Grid floor: lines fan out from the vanishing point; rows bunch up towards the horizon.
        val grid = Palette.pink.copy(alpha = GRID_ALPHA)
        val px = line.toPx()
        for (i in -GRID_COLUMNS..GRID_COLUMNS) {
            val top = w / 2 + i * w * 0.035f
            val bottom = w / 2 + i * w * 0.16f
            drawLine(grid, Offset(top, horizon), Offset(bottom, h), px)
        }
        for (i in 1..GRID_ROWS) {
            val t = i / GRID_ROWS.toFloat()
            val y = horizon + (h - horizon) * t * t
            drawLine(grid, Offset(0f, y), Offset(w, y), px)
        }
        // Fade the grid into the haze near the horizon.
        drawRect(
            Brush.verticalGradient(listOf(FLOOR_TOP, Palette.transparent), startY = horizon, endY = horizon + (h - horizon) * 0.35f),
            topLeft = Offset(0f, horizon), size = GeoSize(w, (h - horizon) * 0.35f),
        )
        // Glowing horizon line.
        drawLine(Palette.pink.copy(alpha = 0.35f), Offset(0f, horizon), Offset(w, horizon), px * 6)
        drawLine(Palette.pink, Offset(0f, horizon), Offset(w, horizon), px * 1.5f)
    }
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
private const val STAR_COUNT = 140
private const val STAR_SEED = 7
private val MOUNTAINS = listOf(
    0.06f to 0.45f, 0.12f to 0.22f, 0.2f to 0.7f, 0.28f to 0.3f, 0.35f to 0.55f, 0.44f to 0.15f,
    0.52f to 0.6f, 0.6f to 0.28f, 0.68f to 0.8f, 0.77f to 0.34f, 0.85f to 0.62f, 0.93f to 0.25f, 1f to 0.5f,
)
