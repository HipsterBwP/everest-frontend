package io.github.hipsterbwp.everest.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GeoSize
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import io.github.hipsterbwp.everest.ui.theme.Retro
import io.github.hipsterbwp.everest.ui.theme.Size
import io.github.hipsterbwp.everest.ui.theme.u
import kotlin.math.cos
import kotlin.math.sin

/** Fill for big titles (`ChromeTxt`); era skins swap the sunset for their own colours. */
val LocalTitleBrush = staticCompositionLocalOf<Brush> { Retro.sunset }

/**
 * Era skins for a console's game list, each a nod to the hardware: the original Game Boy's
 * four-shade green dot-matrix screen, the Virtual Boy's red on black, the NES's grey case and red
 * stripe, the PlayStation's grey and blue, the Dreamcast's orange swirl.
 */
enum class Era(
    val title: Brush,
    val edge: Color,
    val corner: Color,
    /** Tints cover art to the screen's colours (null leaves covers as they are). */
    val covers: ColorFilter?,
) {
    GameBoy(
        Brush.verticalGradient(listOf(Color(0xFF9BBC0F), Color(0xFF8BAC0F), Color(0xFF306230))),
        Color(0xFF8BAC0F), Color(0xFF9BBC0F),
        tintByLight(Color(0xFF0F380F), Color(0xFF9BBC0F)),
    ),
    VirtualBoy(
        Brush.verticalGradient(listOf(Color(0xFFFF5050), Color(0xFFE00000), Color(0xFF700000))),
        Color(0xFFE00000), Color(0xFFFF5050),
        tintByLight(Color(0xFF000000), Color(0xFFFF2020)),
    ),
    Nes(
        Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFD0D0D0), Color(0xFFE53935))),
        Color(0xFFE53935), Color(0xFFBDBDBD), null,
    ),
    PlayStation(
        Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFB0B6C3), Color(0xFF4A6CF7))),
        Color(0xFF4A6CF7), Color(0xFFB0B6C3), null,
    ),
    Dreamcast(
        Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFFFC08A), Color(0xFFFF6A00))),
        Color(0xFFFF8A3D), Color(0xFFFFFFFF), null,
    ),
    Xbox(
        Brush.verticalGradient(listOf(Color(0xFFF2FFE6), Color(0xFF9BF00B), Color(0xFF107C10))),
        Color(0xFF6BD62E), Color(0xFFC8FF8A), null,
    ),
    ;

    companion object {
        fun forSystem(name: String): Era? = when (name) {
            "gb" -> GameBoy
            "virtualboy" -> VirtualBoy
            "nes", "famicom", "fds" -> Nes
            "psx" -> PlayStation
            "dreamcast" -> Dreamcast
            "xbox" -> Xbox
            else -> null
        }
    }
}

/** Maps brightness onto a two-colour ramp, like a monochrome screen ([dark] for black, [light] for white). */
private fun tintByLight(dark: Color, light: Color): ColorFilter {
    fun row(d: Float, l: Float): FloatArray {
        val k = (l - d) // per unit of brightness
        return floatArrayOf(0.3f * k, 0.59f * k, 0.11f * k, 0f, d * 255f)
    }
    return ColorFilter.colorMatrix(
        ColorMatrix(row(dark.red, light.red) + row(dark.green, light.green) + row(dark.blue, light.blue) + floatArrayOf(0f, 0f, 0f, 1f, 0f)),
    )
}

/** The backdrop for an era skin, in place of the synthwave scene. */
@Composable
fun EraBackdrop(era: Era) {
    val dot = Size.ERA_DOT.u
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        when (era) {
            Era.GameBoy -> {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF306230), Color(0xFF0F380F))))
                // Dot-matrix: a fine grid of darker gaps between "pixels".
                val step = dot.toPx()
                var x = 0f
                while (x < w) { drawLine(Color(0x400F380F), Offset(x, 0f), Offset(x, h), 1f); x += step }
                var y = 0f
                while (y < h) { drawLine(Color(0x400F380F), Offset(0f, y), Offset(w, y), 1f); y += step }
            }
            Era.VirtualBoy -> {
                drawRect(Color.Black)
                var y = 0f
                while (y < h) { drawLine(Color(0x33E00000), Offset(0f, y), Offset(w, y), 1f); y += dot.toPx() }
                drawCircle(Brush.radialGradient(listOf(Color(0x40E00000), Color.Transparent), Offset(w / 2, h / 2), w / 2), w / 2, Offset(w / 2, h / 2))
            }
            Era.Nes -> {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF3A3A3A), Color(0xFF1E1E1E))))
                // Grey ridges and the red stripe of the console's front.
                for (i in 0 until 6) drawRect(Color(0x14FFFFFF), Offset(0f, h * (0.08f + i * 0.025f)), GeoSize(w, h * 0.008f))
                drawRect(Color(0xFFB71C1C), Offset(0f, h * 0.9f), GeoSize(w, h * 0.012f))
            }
            Era.PlayStation -> {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF2B3140), Color(0xFF14161C))))
                drawRect(Brush.radialGradient(listOf(Color(0x334A6CF7), Color.Transparent), Offset(w * 0.7f, h * 0.3f), w * 0.6f))
            }
            Era.Dreamcast -> {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF0B1A3A), Color(0xFF050A18))))
                // A big faint swirl.
                val c = Offset(w * 0.72f, h * 0.5f)
                val path = Path()
                var a = 0f
                while (a < SWIRL_TURNS * 2 * Math.PI) {
                    val r = h * 0.05f + a * h * 0.022f
                    val p = Offset(c.x + cos(a) * r, c.y + sin(a) * r)
                    if (a == 0f) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
                    a += 0.05f
                }
                drawPath(path, Color(0x40FF8A3D), style = Stroke(h * 0.035f))
            }
            Era.Xbox -> {
                // Black with the green core glowing through, and a faint X of light behind it.
                drawRect(Color(0xFF050805))
                val c = Offset(w * 0.7f, h * 0.45f)
                drawCircle(Brush.radialGradient(listOf(Color(0x5582E600), Color(0x22107C10), Color.Transparent), c, h * 0.6f), h * 0.6f, c)
                val arm = h * 0.55f
                drawLine(Color(0x1A9BF00B), c + Offset(-arm, -arm), c + Offset(arm, arm), h * 0.08f)
                drawLine(Color(0x1A9BF00B), c + Offset(-arm, arm), c + Offset(arm, -arm), h * 0.08f)
            }
        }
    }
}

private const val SWIRL_TURNS = 3
