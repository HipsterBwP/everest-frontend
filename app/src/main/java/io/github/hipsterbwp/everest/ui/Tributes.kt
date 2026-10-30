package io.github.hipsterbwp.everest.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GeoSize
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import io.github.hipsterbwp.everest.ui.theme.Palette
import io.github.hipsterbwp.everest.ui.theme.Size
import io.github.hipsterbwp.everest.ui.theme.Space
import io.github.hipsterbwp.everest.ui.theme.Type
import io.github.hipsterbwp.everest.ui.theme.u
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/*
 * Start-up tributes for the rest of the library, each an Everest-branded nod to the console's
 * power-on screen. [p] runs 0→1 over Motion.TRIBUTE_MS.
 */

/** 0→1 as [p] goes from [from] to [to]. */
private fun phase(p: Float, from: Float, to: Float) = ((p - from) / (to - from)).coerceIn(0f, 1f)

/** Game Boy Advance: the name on white with a light sweep across it, then a purple underline. */
@Composable
internal fun GbaTribute(p: Float) {
    val ink = Color(0xFF2A2AA8)
    Box(Modifier.fillMaxSize().background(Palette.white), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box {
                Txt("EVEREST", Type.hero, ink, Modifier.alpha(phase(p, 0f, 0.2f)))
                // The sweep: a bright band crossing the letters.
                Canvas(Modifier.matchParentSize()) {
                    val x = size.width * (phase(p, 0.15f, 0.55f) * 1.6f - 0.3f)
                    drawRect(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Palette.white.copy(alpha = 0.9f), Color.Transparent),
                            startX = x - size.width * 0.15f, endX = x + size.width * 0.15f,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(Space.MD.u))
            Canvas(Modifier.size(Size.TRIBUTE_MARK.u * 2, Space.SM.u)) {
                drawRect(ink, size = GeoSize(size.width * phase(p, 0.5f, 0.75f), size.height))
            }
        }
    }
}

/** Game Boy Color: letters drop in one by one, each in its own colour. */
@Composable
internal fun GbcTribute(p: Float) {
    val colors = listOf(0xFFE4002B, 0xFFFF8A00, 0xFFF5C400, 0xFF00A94F, 0xFF0072CE, 0xFF5B2C9F, 0xFFE4007C)
    Box(Modifier.fillMaxSize().background(Palette.white), contentAlignment = Alignment.Center) {
        Row {
            "EVEREST".forEachIndexed { i, c ->
                val drop = phase(p, i * 0.06f, i * 0.06f + 0.2f)
                val bounce = (1f - drop) * -60f + if (drop == 1f) 0f else abs(sin(drop * PI.toFloat())) * -8f
                Txt(c.toString(), Type.hero, Color(colors[i]), Modifier.offset(y = bounce.u).alpha(drop))
            }
        }
    }
}

/** Mega Drive / Genesis: a chrome-blue name on black with a glint running through it. */
@Composable
internal fun MegaDriveTribute(p: Float) {
    val blue = Color(0xFF1E5BFF)
    Box(Modifier.fillMaxSize().background(Palette.black), contentAlignment = Alignment.Center) {
        val reveal = phase(p, 0f, 0.3f)
        Box(Modifier.graphicsLayer { scaleX = 0.6f + 0.4f * reveal; scaleY = 0.6f + 0.4f * reveal; alpha = reveal }) {
            Txt("EVEREST", Type.hero, blue, glow = Color(0xFF7FA6FF))
            Canvas(Modifier.matchParentSize()) {
                val x = size.width * phase(p, 0.35f, 0.7f)
                drawLine(Palette.white, Offset(x - 12f, size.height), Offset(x + 12f, 0f), strokeWidth = 6f)
            }
        }
    }
}

/** Nintendo 3DS: the name in three layers that slide apart into depth. */
@Composable
internal fun ThreeDsTribute(p: Float) {
    val red = Color(0xFFD12230)
    val depth = FastOutSlowInEasing.transform(phase(p, 0.1f, 0.5f))
    Box(Modifier.fillMaxSize().background(Palette.white), contentAlignment = Alignment.Center) {
        Box {
            Txt("EVEREST 3D", Type.hero, red.copy(alpha = 0.2f), Modifier.offset(x = (depth * 10).u, y = (depth * 10).u))
            Txt("EVEREST 3D", Type.hero, red.copy(alpha = 0.45f), Modifier.offset(x = (depth * 5).u, y = (depth * 5).u))
            Txt("EVEREST 3D", Type.hero, red)
        }
    }
}

/**
 * Nintendo 64: a chunky 3D "E" in four colours spins in towards you and settles facing front, then
 * V-E-R-E-S-T drop in beside it one by one to spell EVEREST.
 */
@Composable
internal fun N64Tribute(p: Float) {
    val spin = FastOutSlowInEasing.transform(phase(p, 0f, 0.5f))
    Box(Modifier.fillMaxSize().background(Palette.black), contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.Bottom) {
            Canvas(
                Modifier
                    .size(Size.TRIBUTE_E.u)
                    .graphicsLayer {
                        val grow = 0.15f + 0.85f * spin
                        scaleX = grow
                        scaleY = grow
                    },
            ) {
                // Two and a half turns that slow to a stop almost facing you, tilted enough to show its depth.
                drawBlockE(yaw = (1f - spin) * 5f * PI.toFloat() - E_REST_YAW, pitch = 0.18f * (1f - spin) + E_REST_PITCH)
            }
            Spacer(Modifier.size(Space.MD.u))
            "VEREST".forEachIndexed { i, c ->
                val start = 0.5f + i * 0.06f
                val fall = phase(p, start, start + 0.12f)
                // Drops from above, lands with a little bounce.
                val y = if (fall < 1f) -(1f - fall * fall) * N64_DROP else -abs(sin(phase(p, start + 0.12f, start + 0.2f) * PI.toFloat())) * N64_BOUNCE
                Txt(
                    c.toString(), Type.hero, N64_COLORS[(i + 1) % N64_COLORS.size],
                    Modifier.offset(y = y.u).alpha(if (p < start) 0f else 1f),
                )
            }
        }
    }
}

private val N64_COLORS = listOf(Color(0xFF0046AD), Color(0xFFE4002B), Color(0xFF009B48), Color(0xFFFFD100))
private const val N64_DROP = 220f
private const val N64_BOUNCE = 8f

/** The "E" as four extruded blocks on a 4×5 grid: spine, top, middle and bottom arms. */
private val E_BLOCKS = listOf(
    floatArrayOf(0f, 0f, 1.2f, 5f) to N64_COLORS[0],
    floatArrayOf(1.2f, 0f, 4f, 1.2f) to N64_COLORS[1],
    floatArrayOf(1.2f, 1.9f, 3.4f, 3.1f) to N64_COLORS[2],
    floatArrayOf(1.2f, 3.8f, 4f, 5f) to N64_COLORS[3],
)
private const val E_DEPTH = 1.3f
private const val E_REST_YAW = 0.35f
private const val E_REST_PITCH = 0.22f

/** Draws the block "E" rotated by [yaw] (around the vertical axis) and [pitch], with simple perspective. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBlockE(yaw: Float, pitch: Float) {
    val unit = size.minDimension / 6f
    val cy = cos(yaw)
    val sy = sin(yaw)
    val cp = cos(pitch)
    val sp = sin(pitch)
    val camera = 14f
    data class P(val x: Float, val y: Float, val z: Float)
    fun turn(x: Float, y: Float, z: Float): P {
        // Centre the E on the origin, then yaw, then pitch.
        val px = x - 2f
        val py = y - 2.5f
        val pz = z - E_DEPTH / 2
        val x1 = px * cy + pz * sy
        val z1 = -px * sy + pz * cy
        val y2 = py * cp - z1 * sp
        val z2 = py * sp + z1 * cp
        return P(x1, y2, z2)
    }
    fun screen(q: P): Offset {
        val k = camera / (camera - q.z)
        return Offset(center.x + q.x * unit * k, center.y + q.y * unit * k)
    }
    class Face(val pts: List<P>, val color: Color)
    // Each block: only the faces turned towards the camera, then blocks drawn farthest first.
    val blocks = E_BLOCKS.map { (r, color) ->
        val (x0, y0, x1, y1) = r.toList()
        val c = Array(8) { i ->
            turn(if (i and 1 == 0) x0 else x1, if (i and 2 == 0) y0 else y1, if (i and 4 == 0) E_DEPTH else 0f)
        }
        val centre = P(c.map { it.x }.average().toFloat(), c.map { it.y }.average().toFloat(), c.map { it.z }.average().toFloat())
        val dark = Color(color.red * 0.55f, color.green * 0.55f, color.blue * 0.55f)
        val mid = Color(color.red * 0.75f, color.green * 0.75f, color.blue * 0.75f)
        val faces = listOf(
            intArrayOf(0, 1, 3, 2) to color, // front
            intArrayOf(4, 5, 7, 6) to dark, // back
            intArrayOf(0, 1, 5, 4) to mid, // top
            intArrayOf(2, 3, 7, 6) to dark, // bottom
            intArrayOf(0, 2, 6, 4) to mid, // left
            intArrayOf(1, 3, 7, 5) to mid, // right
        ).mapNotNull { (idx, shade) ->
            val pts = idx.map { c[it] }
            val fx = pts.sumOf { it.x.toDouble() }.toFloat() / 4
            val fy = pts.sumOf { it.y.toDouble() }.toFloat() / 4
            val fz = pts.sumOf { it.z.toDouble() }.toFloat() / 4
            // Box faces point away from the block's centre; keep the ones facing the camera.
            val facing = (fx - centre.x) * -fx + (fy - centre.y) * -fy + (fz - centre.z) * (camera - fz)
            if (facing > 0) Face(pts, shade) else null
        }
        centre.z to faces
    }
    for ((_, faces) in blocks.sortedBy { it.first }) {
        for (f in faces) {
            val path = Path().apply {
                f.pts.map(::screen).forEachIndexed { i, o -> if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) }
                close()
            }
            drawPath(path, f.color)
            drawPath(path, Palette.black.copy(alpha = 0.35f), style = Stroke(1.5f))
        }
    }
}

/** Nintendo DS: two stacked screens light up, the name on top, a touch prompt below. */
@Composable
internal fun NdsTribute(p: Float) {
    val grey = Color(0xFF8A8F98)
    Column(
        Modifier.fillMaxSize().background(Color(0xFFDADDE2)).padding(Space.XL.u),
        verticalArrangement = Arrangement.spacedBy(Space.LG.u),
    ) {
        listOf("EVEREST", "Touch the screen to continue.").forEachIndexed { i, text ->
            Box(
                Modifier.weight(1f).fillMaxWidth().background(Palette.white.copy(alpha = phase(p, i * 0.25f, i * 0.25f + 0.25f))),
                contentAlignment = Alignment.Center,
            ) {
                Txt(text, if (i == 0) Type.hero else Type.hint, grey, Modifier.alpha(phase(p, i * 0.25f + 0.15f, i * 0.25f + 0.4f)))
            }
        }
    }
}

/** NES: a black title card with a red name and the blinking PUSH START BUTTON. */
@Composable
internal fun NesTribute(p: Float) {
    Box(Modifier.fillMaxSize().background(Palette.black), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Txt("EVEREST", Type.hero, Color(0xFFE40018), Modifier.alpha(phase(p, 0f, 0.15f)))
            Spacer(Modifier.height(Space.SECTION.u))
            val blink = if (p > 0.3f && ((p * 10).toInt() % 2 == 0)) 1f else 0f
            Txt("PUSH START BUTTON", Type.hint, Palette.white, Modifier.alpha(blink))
            Spacer(Modifier.height(Space.SECTION_LARGE.u))
            Txt("© EVEREST FRONTEND", Type.hint, Color(0xFFBCBCBC), Modifier.alpha(phase(p, 0.2f, 0.35f)))
        }
    }
}

/** Super Nintendo: four coloured button dots pop in, then the name in italic-ish pixels. */
@Composable
internal fun SnesTribute(p: Float) {
    val dots = listOf(Color(0xFFE4002B), Color(0xFFFFC300), Color(0xFF009B48), Color(0xFF2E3192))
    Box(Modifier.fillMaxSize().background(Color(0xFFE6E6EB)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.MD.u)) {
                dots.forEachIndexed { i, c ->
                    val pop = phase(p, i * 0.08f, i * 0.08f + 0.15f)
                    Canvas(Modifier.size(Space.SECTION.u).graphicsLayer { scaleX = pop; scaleY = pop }) { drawCircle(c) }
                }
            }
            Spacer(Modifier.height(Space.SECTION.u))
            Txt("SUPER EVEREST", Type.screenTitle, Color(0xFF4F43AE), Modifier.alpha(phase(p, 0.4f, 0.6f)).graphicsLayer { rotationZ = -4f })
        }
    }
}

/** Switch: two controller halves slide in and click together, then the name. */
@Composable
internal fun SwitchTribute(p: Float) {
    val red = Color(0xFFFF4554)
    val blue = Color(0xFF00C3E3)
    val click = FastOutSlowInEasing.transform(phase(p, 0f, 0.35f))
    Box(Modifier.fillMaxSize().background(red), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(Modifier.size(Size.TRIBUTE_MARK.u)) {
                val w = size.width * 0.32f
                val h = size.height * 0.8f
                val gap = size.width * 0.6f * (1f - click)
                val top = (size.height - h) / 2
                val r = CornerRadius(w / 2)
                drawRoundRect(Palette.white, Offset(center.x - w - gap, top), GeoSize(w, h), r, style = Stroke(w * 0.18f))
                drawRoundRect(Palette.white, Offset(center.x + gap, top), GeoSize(w, h), r)
                drawCircle(blue, w * 0.18f, Offset(center.x + gap + w / 2, top + h * 0.65f))
                drawCircle(Palette.white, w * 0.16f, Offset(center.x - w - gap + w / 2, top + h * 0.3f))
            }
            Spacer(Modifier.height(Space.SECTION.u))
            Txt("EVEREST", Type.screenTitle, Palette.white, Modifier.alpha(phase(p, 0.4f, 0.55f)))
        }
    }
}

/**
 * PlayStation 2, after the original start-up: a light rushes in from the dark and bursts white, a
 * quiet "Everest Computer Entertainment" card, then the camera drifts over towers of glassy blue cubes
 * while specks of light rise, and the logo fades in over them.
 */
@Composable
internal fun Ps2Tribute(p: Float) {
    val void = Color(0xFF02041A)
    Box(Modifier.fillMaxSize().background(Palette.black), contentAlignment = Alignment.Center) {
        // 1. The rush and the burst.
        Canvas(Modifier.fillMaxSize()) {
            val rush = phase(p, 0f, 0.1f)
            val burst = phase(p, 0.09f, 0.2f)
            val fadeOut = 1f - phase(p, 0.18f, 0.26f)
            if (fadeOut > 0f) {
                val r = size.minDimension * (0.01f + 0.04f * rush * rush) + size.maxDimension * burst
                drawCircle(
                    Brush.radialGradient(listOf(Palette.white, Color(0xFFB8C8FF).copy(alpha = 0.6f), Color.Transparent), center, r),
                    r, center, alpha = fadeOut,
                )
            }
        }
        // 2. The company card.
        val card = phase(p, 0.22f, 0.28f) * (1f - phase(p, 0.37f, 0.42f))
        if (card > 0f) {
            Column(Modifier.alpha(card), horizontalAlignment = Alignment.CenterHorizontally) {
                Txt("EVEREST", Type.screenTitle, Palette.white, glow = Color(0xFF8FA8FF))
                Spacer(Modifier.height(Space.MD.u))
                Txt("Computer Entertainment", Type.label, Color(0xFFD8DEFF))
            }
        }
        // 3. The towers.
        val scene = phase(p, 0.4f, 0.5f)
        if (scene > 0f) {
            Canvas(Modifier.fillMaxSize().alpha(scene)) {
                drawRect(void)
                // Horizon glow.
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0x332A50FF), Color.Transparent),
                        startY = size.height * 0.15f, endY = size.height * 0.5f,
                    ),
                )
                drawTowers(drift = phase(p, 0.4f, 1f))
                // Rising specks of light.
                val rise = phase(p, 0.4f, 1f)
                for (k in 0 until PS2_SPECKS) {
                    val sx = ((k * 0.618034f) % 1f) * size.width
                    val sy = size.height * (1.1f - ((k * 0.381966f + rise * 0.7f) % 1.2f))
                    val twinkle = 0.4f + 0.6f * abs(sin((rise * 9f + k) * 1.3f))
                    drawCircle(Color(0xFFCFE0FF).copy(alpha = 0.7f * twinkle), 1.5f + (k % 3), Offset(sx, sy))
                }
            }
        }
        // 4. The logo.
        val logo = phase(p, 0.7f, 0.82f)
        if (logo > 0f) {
            Column(
                Modifier.alpha(logo).graphicsLayer { val z = 0.92f + 0.08f * logo; scaleX = z; scaleY = z },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Txt("EVEREST", Type.hero, Palette.white, glow = Color(0xFF4A7BFF))
                    Spacer(Modifier.size(Space.MD.u))
                    Txt("2", Type.hero, Color(0xFF6E9BFF), glow = Color(0xFF4A7BFF))
                }
                Spacer(Modifier.height(Space.SM.u))
                Txt("FRONTEND", Type.hint, Color(0xFFB8C8FF))
            }
        }
    }
}

private const val PS2_SPECKS = 28

/** Tower spots on a floor grid: x, depth and how many cubes tall (a fixed, scattered pattern). */
private val PS2_TOWERS: List<Triple<Float, Float, Int>> = buildList {
    for (gx in -9..9) {
        for (gz in 0 until 10) {
            val h = Math.floorMod(gx * 7 + gz * 13 + gx * gz * 3, 23)
            if (h < 6) add(Triple(gx * 2.2f, 9f + gz * 2.2f, 1 + (h * 5) % 6))
        }
    }
}.sortedByDescending { it.second }

/** Glassy cube towers in perspective, farthest first, the camera sliding sideways with [drift]. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTowers(drift: Float) {
    val camX = -3f + 6f * drift
    val camY = 7.5f
    val focal = size.width * 0.55f
    val horizon = size.height * 0.3f
    fun at(x: Float, y: Float, z: Float) = Offset(size.width / 2 + (x - camX) * focal / z, horizon - (y - camY) * focal / z)
    fun quad(a: Offset, b: Offset, c: Offset, d: Offset) = Path().apply {
        moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); lineTo(d.x, d.y); close()
    }
    val s = 0.5f
    for ((x, z, count) in PS2_TOWERS) {
        val fog = (1f - (z - 9f) / 24f).coerceAtLeast(0.08f)
        val side = if (x < camX) x + s else x - s
        for (k in 0 until count) {
            val y0 = k * 1.1f
            val y1 = y0 + 1f
            val front = quad(at(x - s, y0, z - s), at(x + s, y0, z - s), at(x + s, y1, z - s), at(x - s, y1, z - s))
            val top = quad(at(x - s, y1, z - s), at(x + s, y1, z - s), at(x + s, y1, z + s), at(x - s, y1, z + s))
            val wall = quad(at(side, y0, z - s), at(side, y0, z + s), at(side, y1, z + s), at(side, y1, z - s))
            drawPath(wall, Color(0xFF1E46BE).copy(alpha = 0.21f * fog))
            drawPath(wall, Color(0xFF5A8CFF).copy(alpha = 0.47f * fog), style = Stroke(1f))
            drawPath(front, Color(0xFF3264F0).copy(alpha = 0.35f * fog))
            drawPath(front, Color(0xFF8CBEFF).copy(alpha = 0.78f * fog), style = Stroke(1.2f))
            drawPath(top, Color(0xFF6EA0FF).copy(alpha = 0.46f * fog))
            drawPath(top, Color(0xFFB4D7FF).copy(alpha = 0.78f * fog), style = Stroke(1.2f))
        }
    }
}

/** PSP: a flowing ribbon of light across a dark screen, the name on top of it. */
@Composable
internal fun PspTribute(p: Float) {
    Box(Modifier.fillMaxSize().background(Color(0xFF0B0B14)), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            for (k in 0 until 3) {
                val path = Path()
                val steps = 60
                for (i in 0..steps) {
                    val x = size.width * i / steps
                    val y = size.height * 0.55f + sin(i / steps.toFloat() * 2 * PI.toFloat() + p * 6f + k) * size.height * (0.06f + k * 0.02f)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, Palette.white.copy(alpha = 0.35f - k * 0.08f), style = Stroke(width = 3f + k * 2f))
            }
        }
        Txt("EVEREST", Type.hero, Palette.white, Modifier.alpha(phase(p, 0.25f, 0.5f)))
    }
}

/** Wii: white screen, a soft blue ring pulsing out, the name in rounded grey and "Press A". */
@Composable
internal fun WiiTribute(p: Float) {
    val sky = Color(0xFF34BEED)
    val grey = Color(0xFF8C8C8C)
    BoxWithConstraints(Modifier.fillMaxSize().background(Palette.white), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            for (k in 0 until 2) {
                val ring = ((p * 1.5f + k * 0.5f) % 1f)
                drawCircle(sky.copy(alpha = (1f - ring) * 0.5f), size.minDimension * 0.15f + ring * size.minDimension * 0.35f, center, style = Stroke(4f))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Txt("everest", Type.hero, grey, Modifier.alpha(phase(p, 0.1f, 0.35f)))
            Spacer(Modifier.height(Space.SECTION.u))
            Txt("Press A", Type.hint, sky, Modifier.alpha(phase(p, 0.5f, 0.7f)))
        }
    }
}


/**
 * Xbox: green energy ribbons spiral in out of the dark and fuse into a glowing core with Everest's
 * peak carved into it, then the name lights up underneath.
 */
@Composable
internal fun XboxTribute(p: Float) {
    val green = Color(0xFF9BF00B)
    val deep = Color(0xFF107C10)
    val fuse = phase(p, 0.38f, 0.55f)
    Box(Modifier.fillMaxSize().background(Palette.black), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val core = Offset(size.width / 2, size.height * 0.4f)
            val reach = size.minDimension * 0.62f
            // The ribbons: each a spiral whose head races inward, leaving a fading trail.
            val head = phase(p, 0f, 0.5f)
            if (fuse < 1f) {
                for (k in 0 until XBOX_RIBBONS) {
                    val start = k * 2 * PI.toFloat() / XBOX_RIBBONS
                    val path = Path()
                    val steps = 40
                    for (i in 0..steps) {
                        val f = (head - 0.35f + 0.35f * i / steps).coerceIn(0f, 1f)
                        val r = reach * (1f - f)
                        val a = start + f * 5f + sin(f * 9f + k) * 0.25f
                        val pt = core + Offset(cos(a) * r, sin(a) * r * 0.8f)
                        if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                    }
                    drawPath(path, deep.copy(alpha = 0.5f * (1f - fuse)), style = Stroke(width = 18f))
                    drawPath(path, green.copy(alpha = 0.9f * (1f - fuse)), style = Stroke(width = 4f))
                }
            }
            // The core: a glowing orb with the peak cut into it.
            if (fuse > 0f) {
                val r = size.minDimension * 0.17f * FastOutSlowInEasing.transform(fuse)
                val flash = (1f - phase(p, 0.5f, 0.7f)) * fuse
                drawCircle(Brush.radialGradient(listOf(green.copy(alpha = 0.5f), Color.Transparent), core, r * 2.6f), r * 2.6f, core)
                drawCircle(Brush.radialGradient(listOf(Color(0xFFE9FFD0), green, deep), core, r), r, core)
                val peak = Path().apply {
                    moveTo(core.x - r * 0.62f, core.y + r * 0.42f)
                    lineTo(core.x - r * 0.12f, core.y - r * 0.5f)
                    lineTo(core.x + r * 0.08f, core.y - r * 0.14f)
                    lineTo(core.x + r * 0.26f, core.y - r * 0.36f)
                    lineTo(core.x + r * 0.62f, core.y + r * 0.42f)
                    close()
                }
                drawPath(peak, Color(0xFF0A2A0A))
                drawCircle(Palette.white.copy(alpha = 0.8f * flash), r * (1f + flash * 0.6f), core)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = Size.TRIBUTE_MARK.u * 2)) {
            Txt("EVEREST", Type.hero, Color(0xFFF2FFE6), Modifier.alpha(phase(p, 0.6f, 0.75f)), glow = green)
            Spacer(Modifier.height(Space.SM.u))
            Txt("FRONTEND", Type.hint, green, Modifier.alpha(phase(p, 0.7f, 0.85f)))
        }
    }
}

private const val XBOX_RIBBONS = 5
