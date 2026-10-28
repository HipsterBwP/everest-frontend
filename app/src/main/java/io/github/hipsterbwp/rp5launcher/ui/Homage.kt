package io.github.hipsterbwp.rp5launcher.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import io.github.hipsterbwp.rp5launcher.ui.theme.Motion
import io.github.hipsterbwp.rp5launcher.ui.theme.Palette
import io.github.hipsterbwp.rp5launcher.ui.theme.Radius
import io.github.hipsterbwp.rp5launcher.ui.theme.Size
import io.github.hipsterbwp.rp5launcher.ui.theme.Space
import io.github.hipsterbwp.rp5launcher.ui.theme.Type
import io.github.hipsterbwp.rp5launcher.ui.theme.u
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// ---------------------------------------------------------------------------------------------
// Start-up tributes
// ---------------------------------------------------------------------------------------------

/**
 * A nod to a console's power-on screen, Everest-branded: the Game Boy logo scroll and ding, the
 * PlayStation diamond, the GameCube rolling cube, the Dreamcast swirl and the rest in Tributes.kt.
 * Any button skips it.
 */
@Composable
fun TributeIntro(system: String, vm: LauncherViewModel, onDone: () -> Unit) {
    val t = remember(system) { Animatable(0f) }
    LaunchedEffect(system) {
        if (system != "gb") vm.sound(bootSound(system))
        val length = when (system) {
            "n64" -> Motion.TRIBUTE_LONG_MS
            "ps2" -> Motion.TRIBUTE_PS2_MS
            else -> Motion.TRIBUTE_MS
        }
        t.animateTo(1f, tween(length, easing = LinearEasing))
        onDone()
    }
    // The Game Boy dings when the logo lands, not at power-on.
    LaunchedEffect(system) {
        if (system != "gb") return@LaunchedEffect
        delay((Motion.TRIBUTE_MS * GB_LAND).toLong())
        vm.sound(UiSound.BootGb)
    }
    PadInput { _, _ -> onDone(); true }
    val p = t.value
    when (system) {
        "gb" -> GameBoyTribute(p)
        "psx" -> PlayStationTribute(p)
        "gc" -> GameCubeTribute(p)
        "dreamcast" -> DreamcastTribute(p)
        "gba" -> GbaTribute(p)
        "gbc" -> GbcTribute(p)
        "megadrive", "genesis" -> MegaDriveTribute(p)
        "n3ds" -> ThreeDsTribute(p)
        "n64" -> N64Tribute(p)
        "nds" -> NdsTribute(p)
        "nes", "famicom" -> NesTribute(p)
        "snes", "sfc" -> SnesTribute(p)
        "switch" -> SwitchTribute(p)
        "ps2" -> Ps2Tribute(p)
        "psp" -> PspTribute(p)
        else -> WiiTribute(p)
    }
}

private fun bootSound(system: String) = when (system) {
    "psx" -> UiSound.BootPs
    "gc" -> UiSound.BootGc
    "dreamcast" -> UiSound.BootDc
    "gba" -> UiSound.BootGba
    "gbc" -> UiSound.BootGbc
    "megadrive", "genesis" -> UiSound.BootMd
    "n3ds" -> UiSound.BootThreeDs
    "n64" -> UiSound.BootN64
    "nds" -> UiSound.BootNds
    "nes", "famicom" -> UiSound.BootNes
    "snes", "sfc" -> UiSound.BootSnes
    "switch" -> UiSound.BootSwitch
    "ps2" -> UiSound.BootPs2
    "psp" -> UiSound.BootPsp
    "wii" -> UiSound.BootWii
    else -> UiSound.BootGb
}

private const val GB_LAND = 0.55f

@Composable
private fun GameBoyTribute(p: Float) {
    val lcd = Color(0xFFC4CFA1)
    val ink = Color(0xFF2B2F7A)
    BoxWithConstraints(Modifier.fillMaxSize().background(lcd), contentAlignment = Alignment.Center) {
        // Scrolls down from above the screen and stops in the middle.
        val land = (p / GB_LAND).coerceAtMost(1f)
        val y = -maxHeight / 2 * (1f - land)
        Txt("EVEREST®", Type.hero, ink, Modifier.offset(y = y))
    }
}

@Composable
private fun PlayStationTribute(p: Float) {
    // White flash in, then the four-colour diamond on black with the name under it.
    val flash = (1f - p * 4f).coerceIn(0f, 1f)
    val grow = ((p - 0.15f) / 0.35f).coerceIn(0f, 1f)
    Box(Modifier.fillMaxSize().background(Palette.black), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(Modifier.size(Size.TRIBUTE_MARK.u).graphicsLayer { scaleX = grow; scaleY = grow; alpha = grow }) {
                val c = center
                val r = size.minDimension / 2
                val quarters = listOf(Color(0xFFE60012), Color(0xFFF5B700), Color(0xFF00A650), Color(0xFF0070D1))
                quarters.forEachIndexed { i, color ->
                    rotate(90f * i, c) {
                        val path = Path().apply {
                            moveTo(c.x, c.y)
                            lineTo(c.x, c.y - r)
                            lineTo(c.x + r, c.y)
                            close()
                        }
                        drawPath(path, color)
                    }
                }
            }
            Spacer(Modifier.height(Space.SECTION.u))
            Txt("EVEREST", Type.screenTitle, Palette.white, Modifier.alpha(grow))
            Spacer(Modifier.height(Space.SM.u))
            Txt("Computer Entertainment Frontend", Type.label, Palette.inkSecondary, Modifier.alpha(grow))
        }
        Box(Modifier.fillMaxSize().background(Palette.white.copy(alpha = flash)))
    }
}

@Composable
private fun GameCubeTribute(p: Float) {
    val purple = Color(0xFF4B2C82)
    val cube = Color(0xFF8E6BD8)
    BoxWithConstraints(Modifier.fillMaxSize().background(purple), contentAlignment = Alignment.Center) {
        // The cube tumbles in from the left and settles in the middle, then the name fades in.
        val roll = (p / 0.6f).coerceAtMost(1f)
        val eased = FastOutSlowInEasing.transform(roll)
        val x = -maxWidth / 2 * (1f - eased)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(Modifier.size(Size.TRIBUTE_MARK.u).offset(x = x)) {
                val s = size.minDimension * 0.6f
                rotate(-360f * (1f - eased), center) {
                    drawRect(cube, Offset(center.x - s / 2, center.y - s / 2), androidx.compose.ui.geometry.Size(s, s))
                    drawRect(purple, Offset(center.x - s / 6, center.y - s / 6), androidx.compose.ui.geometry.Size(s / 3, s / 3))
                }
            }
            Spacer(Modifier.height(Space.SECTION.u))
            Txt("EVEREST", Type.screenTitle, Palette.white, Modifier.alpha(((p - 0.6f) / 0.2f).coerceIn(0f, 1f)))
        }
    }
}

@Composable
private fun DreamcastTribute(p: Float) {
    val orange = Color(0xFFF26522)
    val draw = (p / 0.6f).coerceAtMost(1f)
    Box(Modifier.fillMaxSize().background(Palette.white), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(Modifier.size(Size.TRIBUTE_MARK.u)) {
                // A spiral drawn from the outside in.
                val path = Path()
                val turns = 2.6f
                val steps = (SWIRL_STEPS * draw).roundToInt()
                val r = size.minDimension / 2
                for (i in 0..steps) {
                    val f = i / SWIRL_STEPS.toFloat()
                    val a = f * turns * 2 * PI.toFloat()
                    val rr = r * (1f - f * 0.85f)
                    val pt = Offset(center.x + rr * cos(a), center.y + rr * sin(a))
                    if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                }
                drawPath(path, orange, style = Stroke(width = r * 0.12f, cap = StrokeCap.Round))
            }
            Spacer(Modifier.height(Space.SECTION.u))
            Txt("everest", Type.screenTitle, orange, Modifier.alpha(((p - 0.55f) / 0.2f).coerceIn(0f, 1f)))
        }
    }
}

private const val SWIRL_STEPS = 140

// ---------------------------------------------------------------------------------------------
// Alerts
// ---------------------------------------------------------------------------------------------

/** "HERE COMES A NEW CHALLENGER!" when a second controller connects. */
@Composable
fun ChallengerFlash() {
    val strobe by rememberInfiniteTransition(label = "challenger").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(Motion.BLINK_MS / 3, easing = LinearEasing), RepeatMode.Reverse),
        label = "challenger-strobe",
    )
    Box(Modifier.fillMaxSize().background(Palette.black.copy(alpha = 0.72f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth().background(Color(0xFFB0001E).copy(alpha = 0.55f + 0.3f * strobe)).padding(vertical = Space.SECTION.u),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Txt("HERE COMES A", Type.screenTitle, Palette.white, glow = Palette.sun2)
            Spacer(Modifier.height(Space.LG.u))
            Txt("NEW CHALLENGER!", Type.hero, if (strobe > 0.5f) Palette.sun1 else Palette.white, glow = Palette.sun3)
        }
    }
}

/** "1UP" rising from the top right when a session earned credits. */
@Composable
fun OneUpPopup(credits: Int) {
    val rise = remember { Animatable(0f) }
    LaunchedEffect(credits) {
        if (credits > 0) {
            rise.snapTo(0f)
            rise.animateTo(1f, tween(Motion.ONE_UP_MS.toInt(), easing = LinearEasing))
        }
    }
    Box(Modifier.fillMaxSize().padding(Space.SECTION_LARGE.u), contentAlignment = Alignment.TopEnd) {
        AnimatedVisibility(credits > 0, enter = fadeIn(), exit = fadeOut()) {
            Column(
                Modifier.offset { IntOffset(0, (-rise.value * ONE_UP_RISE).roundToInt()) },
                horizontalAlignment = Alignment.End,
            ) {
                Txt("1UP", Type.hero, Color(0xFF39FF6A), glow = Color(0xFF00C040))
                Txt("+$credits CREDIT${if (credits == 1) "" else "S"}", Type.hint, Palette.sun1, glow = Palette.sun2)
            }
        }
    }
}

private const val ONE_UP_RISE = 60f

// ---------------------------------------------------------------------------------------------
// End credits
// ---------------------------------------------------------------------------------------------

/** The roll from Settings › About: who made what, ending in THANK YOU FOR PLAYING. B (or anything) exits. */
@Composable
fun CreditsRoll(initials: String, onDone: () -> Unit) {
    PadInput { _, _ -> onDone(); true }
    val blocks = listOf(
        "EVEREST FRONTEND" to listOf("A controller-first launcher", "for the Retroid Pocket 5"),
        "DIRECTOR" to listOf(initials),
        "PROGRAMMING" to listOf("Kotlin · Jetpack Compose", "with Claude Code"),
        "PIXEL FONTS" to listOf("Press Start 2P — CodeMan38", "VT323 — Peter Hull", "Chakra Petch — Cadson Demak"),
        "CONSOLE ART" to listOf("Slate theme — ES-DE project"),
        "GAME DATA" to listOf("ScreenScraper", "RetroAchievements", "ES-DE media"),
        "MUSIC & SOUND" to listOf("Neon Drift", "Pixel Garden", "After Hours", "After Hours (8-bit)", "Synthesized in Python"),
        "MASCOT" to listOf("Yeti-1"),
        "SPECIAL THANKS" to listOf("Every console that came before", "and everyone who played them"),
    )
    var height by remember { mutableIntStateOf(0) }
    val unitPx = with(androidx.compose.ui.platform.LocalDensity.current) { 1.u.toPx() }
    BoxWithConstraints(Modifier.fillMaxSize().background(Palette.black)) {
        val screen = constraints.maxHeight.toFloat()
        val scroll = remember { Animatable(0f) }
        LaunchedEffect(height) {
            if (height == 0) return@LaunchedEffect
            val distance = screen + height
            val ms = (distance / (Motion.CREDITS_SPEED * unitPx) * 1000).toInt()
            scroll.animateTo(distance, tween(ms, easing = LinearEasing))
            delay(CREDITS_HOLD_MS)
            onDone()
        }
        Column(
            Modifier
                .fillMaxWidth()
                .onSizeChanged { height = it.height }
                .offset { IntOffset(0, (screen - scroll.value.coerceAtMost(screen / 2 + height / 2)).roundToInt()) },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            blocks.forEach { (title, lines) ->
                Txt(title, Type.hint, Palette.cyanSoft, glow = Palette.cyan)
                Spacer(Modifier.height(Space.MD.u))
                lines.forEach { Txt(it, Type.crt, Palette.ink) }
                Spacer(Modifier.height(Space.XL.u))
            }
            Spacer(Modifier.height(Space.XL.u))
            ChromeTxt("Thank you for playing", Type.screenTitle)
            Spacer(Modifier.height(Space.LG.u))
            Txt(initials, Type.hero, Palette.sun1, glow = Palette.sun2)
        }
    }
}

private const val CREDITS_HOLD_MS = 2_500L

// ---------------------------------------------------------------------------------------------
// This Day in Gaming History
// ---------------------------------------------------------------------------------------------

data class HistoryFact(val year: Int, val text: String)

/** Facts from assets/history.json, for today (or the nearest day within a few days: "this week"). */
object GamingHistory {
    @Volatile private var facts: Map<String, List<HistoryFact>>? = null

    private fun load(context: Context): Map<String, List<HistoryFact>> = facts ?: runCatching {
        val root = Json.parseToJsonElement(context.assets.open("history.json").bufferedReader().use { it.readText() }) as JsonObject
        root.mapValues { (_, v) ->
            (v as? JsonArray).orEmpty().mapNotNull { e ->
                val o = e as? JsonObject ?: return@mapNotNull null
                val year = (o["year"] as? JsonPrimitive)?.contentOrNull?.toIntOrNull() ?: return@mapNotNull null
                HistoryFact(year, (o["text"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null)
            }
        }
    }.getOrDefault(emptyMap()).also { facts = it }

    /** ("THIS DAY IN GAMING", facts) for today, or ("THIS WEEK IN GAMING", facts) within ±3 days. */
    fun around(context: Context, today: LocalDate = LocalDate.now()): Pair<String, List<HistoryFact>>? {
        val all = load(context)
        fun at(d: LocalDate) = all["%02d-%02d".format(d.monthValue, d.dayOfMonth)].orEmpty()
        at(today).takeIf { it.isNotEmpty() }?.let { return "THIS DAY IN GAMING" to it }
        for (offset in 1L..3L) {
            for (d in listOf(today.minusDays(offset), today.plusDays(offset))) {
                at(d).takeIf { it.isNotEmpty() }?.let { return "THIS WEEK IN GAMING" to it }
            }
        }
        return null
    }
}

/** One gaming-history fact (a different one each time it's shown), in a small neon panel. */
@Composable
fun HistoryCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val entry = remember { GamingHistory.around(context)?.let { (title, facts) -> title to facts.random() } } ?: return
    val (title, fact) = entry
    Column(
        modifier
            .widthIn(max = Size.HISTORY_W.u)
            .neonPanel(Radius.CARD, edge = Palette.cyan, corner = Palette.sun1, fill = Palette.glassStrong)
            .padding(horizontal = Space.LG.u, vertical = Space.MD.u),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Txt(title, Type.hint, Palette.sun1, glow = Palette.sun2)
        Spacer(Modifier.height(Space.SM.u))
        androidx.compose.foundation.text.BasicText(
            "${fact.year} · ${fact.text}",
            style = Type.crtSmall.copy(color = Palette.ink, textAlign = TextAlign.Center),
            maxLines = 3,
        )
    }
}
