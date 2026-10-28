package io.github.hipsterbwp.rp5launcher.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import io.github.hipsterbwp.rp5launcher.ui.theme.Motion
import io.github.hipsterbwp.rp5launcher.ui.theme.Palette
import io.github.hipsterbwp.rp5launcher.ui.theme.Retro
import io.github.hipsterbwp.rp5launcher.ui.theme.Size
import io.github.hipsterbwp.rp5launcher.ui.theme.Space
import io.github.hipsterbwp.rp5launcher.ui.theme.u
import java.io.File
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * A console's logo as a spinning 3D arcade sign: sunset-gradient face with a cyan top edge, a thick
 * pink-to-violet extrusion behind it, a pulsing pink glow and a faint reflection on the grid floor.
 * It turns like a coin, so the face never shows mirrored; the extrusion layers slide sideways as it
 * turns, which is what makes it read as solid.
 */
@Composable
fun LogoSign(logo: File, modifier: Modifier = Modifier) {
    val painter = rememberAsyncImagePainter(logo)
    val spin = rememberInfiniteTransition(label = "sign").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(Motion.SIGN_SPIN_MS, easing = LinearEasing)),
        label = "sign-angle",
    )
    val pulse = rememberGlowPulse()
    val depth = Size.SIGN_DEPTH.u
    val edge = Size.SIGN_EDGE.u

    // Everything reads the angle in graphicsLayer blocks, so spinning only redraws, never recomposes.
    fun Modifier.turn(layer: Float, lift: Dp = 0.dp) = graphicsLayer {
        val a = Math.toRadians(spin.value.toDouble())
        scaleX = abs(cos(a)).toFloat().coerceAtLeast(EDGE_ON)
        translationX = sin(a).toFloat() * depth.toPx() * layer
        translationY = depth.toPx() * layer * DROP + lift.toPx()
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().height(Size.SIGN_H.u)) {
            // Glow behind, breathing.
            Image(
                painter, null,
                Modifier.matchParentSize().turn(0.5f).graphicsLayer { alpha = GLOW_ALPHA * pulse.value }.blur(Size.SIGN_GLOW.u),
                colorFilter = ColorFilter.tint(Palette.pink), contentScale = ContentScale.Fit,
            )
            // Extrusion, back to front: deep violet fading up to hot pink.
            for (i in LAYERS downTo 1) {
                val t = i / LAYERS.toFloat()
                Image(
                    painter, null, Modifier.matchParentSize().turn(t),
                    colorFilter = ColorFilter.tint(lerp(Palette.pink, Palette.violet, t)), contentScale = ContentScale.Fit,
                )
            }
            // Cyan rim peeking above the face.
            Image(
                painter, null, Modifier.matchParentSize().turn(0f, lift = -edge * 2),
                colorFilter = ColorFilter.tint(Palette.cyanSoft), contentScale = ContentScale.Fit,
            )
            // Face: the logo filled with the sunset gradient.
            Image(
                painter, null,
                Modifier
                    .matchParentSize()
                    .turn(0f)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(Retro.sunset, blendMode = BlendMode.SrcIn)
                    },
                contentScale = ContentScale.Fit,
            )
        }
        Spacer(Modifier.height(Space.SECTION.u))
        // Reflection on the floor: upside down, pink, fading out.
        Image(
            painter, null,
            Modifier
                .fillMaxWidth()
                .height(Size.SIGN_REFLECTION_H.u)
                .turn(0f)
                .graphicsLayer { scaleY = -1f; alpha = REFLECTION_ALPHA; compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(Brush.verticalGradient(listOf(Palette.transparent, Palette.black)), blendMode = BlendMode.DstIn)
                },
            colorFilter = ColorFilter.tint(Palette.pinkSoft), contentScale = ContentScale.Fit,
        )
    }
}

/** Extrusion slices: more looks smoother when the sign is edge-on. */
private const val LAYERS = 14
/** Thinnest the face gets when edge-on, so it never vanishes. */
private const val EDGE_ON = 0.04f
/** The extrusion also drops a little, like 3D lettering lit from above. */
private const val DROP = 0.6f
private const val GLOW_ALPHA = 0.85f
private const val REFLECTION_ALPHA = 0.22f
