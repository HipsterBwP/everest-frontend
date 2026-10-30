package io.github.hipsterbwp.everest.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GeoSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import io.github.hipsterbwp.everest.ui.theme.Motion
import io.github.hipsterbwp.everest.ui.theme.Palette
import io.github.hipsterbwp.everest.ui.theme.Radius
import io.github.hipsterbwp.everest.ui.theme.Size
import io.github.hipsterbwp.everest.ui.theme.Space
import io.github.hipsterbwp.everest.ui.theme.Type
import io.github.hipsterbwp.everest.ui.theme.rounded
import io.github.hipsterbwp.everest.ui.theme.u
import kotlin.math.sin
import kotlin.random.Random

private enum class Stage { Insert, Glitch, Blow }

/**
 * The game going in before a launch: a cartridge drops into its slot, a disc spins onto its tray, a
 * UMD clicks into the PSP. Sometimes a cartridge glitches and Yeti-1 blows on it, like we all did.
 * Input is swallowed while it plays; the launch carries on when it's done.
 */
@Composable
fun InsertOverlay(vm: LauncherViewModel, fx: InsertFx) {
    var stage by remember(fx) { mutableStateOf(if (fx.blow) Stage.Blow else Stage.Insert) }
    val t = remember(fx) { Animatable(0f) }
    suspend fun run(next: Stage, ms: Int) {
        stage = next
        t.snapTo(0f)
        when (next) {
            Stage.Insert -> vm.sound(if (fx.media.cartridge) UiSound.Select else UiSound.Disc)
            Stage.Glitch -> vm.sound(UiSound.Error)
            Stage.Blow -> vm.sound(UiSound.Blow)
        }
        t.animateTo(1f, tween(ms, easing = LinearEasing))
    }
    LaunchedEffect(fx) {
        if (fx.blow) run(Stage.Blow, Motion.BLOW_MS)
        run(Stage.Insert, Motion.INSERT_MS)
        if (fx.glitch) {
            run(Stage.Glitch, Motion.GLITCH_MS)
            run(Stage.Blow, Motion.BLOW_MS)
            run(Stage.Insert, Motion.INSERT_MS)
        }
        vm.insertDone()
    }
    // The cartridge lands with a clunk.
    val landed = t.value >= LAND
    LaunchedEffect(fx, stage, landed) {
        if (stage == Stage.Insert && landed && fx.media.cartridge) vm.sound(UiSound.Insert)
    }
    PadInput { _, _ -> true }
    val art = remember(fx) { vm.boxArt(fx.game) }
    Box(Modifier.fillMaxSize().background(Palette.black.copy(alpha = 0.88f)), contentAlignment = Alignment.Center) {
        when (stage) {
            Stage.Insert -> when (fx.media) {
                Media.Cartridge, Media.HandheldCart -> CartridgeIn(fx, art, t.value)
                Media.Disc -> DiscIn(art, t.value)
                Media.Umd -> UmdIn(art, t.value)
            }
            Stage.Glitch -> Glitch(t.value)
            Stage.Blow -> Blow(fx, art, t.value)
        }
        Txt(fx.game.title, Type.hint, Palette.inkSecondary, Modifier.align(Alignment.BottomCenter).padding(bottom = Space.SECTION_LARGE.u))
    }
}

/** When the falling cartridge reaches the slot. */
private const val LAND = 0.62f

@Composable
private fun CartridgeIn(fx: InsertFx, art: java.io.File?, p: Float) {
    val small = fx.media == Media.HandheldCart
    val w = if (small) Size.CART_SMALL_W else Size.CART_W
    val h = if (small) Size.CART_SMALL_H else Size.CART_H
    val fall = FastOutSlowInEasing.transform((p / LAND).coerceAtMost(1f))
    // Drop, then a little settle; NES carts go in and get pushed down.
    val settle = if (p > LAND) sin(((p - LAND) / (1 - LAND)) * Math.PI.toFloat()) * 6f else 0f
    val y = -Size.INSERT_DROP * (1f - fall) + settle + (if (fx.system == "nes" && p > 0.8f) (p - 0.8f) * 40f else 0f)
    Box(Modifier.size(Size.INSERT_STAGE_W.u, Size.INSERT_STAGE_H.u), contentAlignment = Alignment.BottomCenter) {
        // The cartridge.
        Column(
            Modifier
                .padding(bottom = Size.CONSOLE_BODY_H.u - (h * 0.45f).u)
                .offset(y = y.u)
                .size(w.u, h.u)
                .clip(rounded(Radius.CARD_SMALL))
                .background(Media.shell(fx.system))
                .padding(horizontal = Space.MD.u, vertical = Space.SM.u),
        ) {
            // Grip ridges, then the label.
            Canvas(Modifier.fillMaxWidth().height(Space.MD.u)) {
                for (i in 0 until 5) drawRect(Palette.black.copy(alpha = 0.25f), Offset(0f, i * size.height / 5), GeoSize(size.width, 1.5f))
            }
            Spacer(Modifier.height(Space.SM.u))
            ArtImage(art, Modifier.fillMaxWidth().weight(1f).clip(rounded(Radius.CARD_SMALL)), ContentScale.Crop)
            Spacer(Modifier.height(Space.MD.u))
        }
        // The console in front of it, hiding the part that's in the slot.
        ConsoleBody(Media.shell(fx.system))
    }
}

@Composable
private fun ConsoleBody(shell: Color) {
    Canvas(Modifier.fillMaxWidth().height(Size.CONSOLE_BODY_H.u)) {
        val r = CornerRadius(18f)
        drawRoundRect(Color(0xFF1C1C22), cornerRadius = r)
        drawRoundRect(shell.copy(alpha = 0.6f), Offset(0f, 0f), GeoSize(size.width, 10f), r)
        // Slot lip, power LED.
        drawRect(Palette.black, Offset(size.width * 0.25f, 2f), GeoSize(size.width * 0.5f, 8f))
        drawCircle(Color(0xFFE5395B), 6f, Offset(size.width * 0.12f, size.height * 0.55f))
    }
}

@Composable
private fun DiscIn(art: java.io.File?, p: Float) {
    // Slides onto the tray, spinning up, then the tray takes it in.
    val slide = FastOutSlowInEasing.transform((p / 0.5f).coerceAtMost(1f))
    val take = ((p - 0.6f) / 0.4f).coerceIn(0f, 1f)
    Box(Modifier.size(Size.INSERT_STAGE_W.u, Size.INSERT_STAGE_H.u), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .offset(x = (-Size.INSERT_DROP * (1f - slide)).u, y = (-take * 40f).u)
                .size(Size.DISC.u)
                .graphicsLayer {
                    rotationZ = p * 900f
                    val s = 1f - take * 0.6f
                    scaleX = s
                    scaleY = s
                    alpha = 1f - take
                }
                .clip(CircleShape)
                .background(Color(0xFFCFD3DA)),
            contentAlignment = Alignment.Center,
        ) {
            ArtImage(art, Modifier.fillMaxSize(), ContentScale.Crop)
            // Shine and the centre hole.
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Palette.white.copy(alpha = 0.18f), size.minDimension / 2)
                drawCircle(Color(0xFF8A8F98), size.minDimension * 0.12f)
                drawCircle(Palette.black, size.minDimension * 0.06f)
            }
        }
        Box(Modifier.align(Alignment.BottomCenter)) { ConsoleBody(Color(0xFF9AA0AA)) }
    }
}

@Composable
private fun UmdIn(art: java.io.File?, p: Float) {
    val fall = FastOutSlowInEasing.transform((p / LAND).coerceAtMost(1f))
    Box(Modifier.size(Size.INSERT_STAGE_W.u, Size.INSERT_STAGE_H.u), contentAlignment = Alignment.BottomCenter) {
        Box(
            Modifier
                .padding(bottom = Size.CONSOLE_BODY_H.u - 30.u)
                .offset(y = (-Size.INSERT_DROP * (1f - fall)).u)
                .size(Size.CART_SMALL_W.u, Size.CART_SMALL_H.u)
                .clip(rounded(Radius.CARD))
                .background(Color(0xFF2A2A30)),
            contentAlignment = Alignment.Center,
        ) {
            ArtImage(art, Modifier.size((Size.CART_SMALL_W * 0.7f).u).clip(CircleShape), ContentScale.Crop)
        }
        ConsoleBody(Color(0xFF2A2A30))
    }
}

/** A failed boot: garbage tiles and a scrambled screen. */
@Composable
private fun Glitch(p: Float) {
    val seed = (p * 12).toInt()
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val rnd = Random(seed)
            val cell = size.width / 24
            for (y in 0 until (size.height / cell).toInt() + 1) for (x in 0 until 24) {
                if (rnd.nextFloat() < 0.55f) {
                    val c = GLITCH_COLORS[rnd.nextInt(GLITCH_COLORS.size)]
                    drawRect(c, Offset(x * cell, y * cell), GeoSize(cell, cell))
                }
            }
        }
        Txt("▓▒░ BAD CART?? ░▒▓", Type.screenTitle, Palette.white, glow = Palette.black)
    }
}

private val GLITCH_COLORS = listOf(
    Color(0xFF0F380F), Color(0xFF8BAC0F), Color(0xFFE5395B), Color(0xFF39E0FF), Color(0xFFFFD84A), Color(0xFF1B1035), Color(0xFFFFFFFF),
)

/** Yeti-1 blowing on the cartridge's contacts. */
@Composable
private fun Blow(fx: InsertFx, art: java.io.File?, p: Float) {
    Box(Modifier.size(Size.INSERT_STAGE_W.u, Size.INSERT_STAGE_H.u)) {
        Canvas(Modifier.fillMaxSize()) {
            val px = size.height / 26f
            val origin = Offset(size.width * 0.04f, size.height * 0.25f)
            drawSprite(YetiSprites.walk1, px, origin)
            // Puffs travel from the mouth to the cartridge.
            val mouth = origin + Offset(9 * px, 8 * px)
            val target = Offset(size.width * 0.72f, size.height * 0.55f)
            for (k in 0 until 5) {
                val f = ((p * 1.6f) - k * 0.12f).coerceIn(0f, 1f)
                if (f in 0.01f..0.99f) {
                    drawCircle(Palette.white.copy(alpha = 0.7f * (1f - f)), px * (1.2f + f * 2.5f), mouth + (target - mouth) * f)
                }
            }
        }
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .size(Size.CART_SMALL_W.u, Size.CART_SMALL_H.u)
                .graphicsLayer { rotationZ = -90f; translationX = sin(p * 40f) * 3f }
                .clip(rounded(Radius.CARD_SMALL))
                .background(Media.shell(fx.system))
                .padding(Space.SM.u),
        ) {
            ArtImage(art, Modifier.fillMaxSize(), ContentScale.Crop)
        }
        Txt("FFFFFT!", Type.screenTitle, Palette.cyanSoft, Modifier.align(Alignment.TopCenter), glow = Palette.cyan)
    }
}
