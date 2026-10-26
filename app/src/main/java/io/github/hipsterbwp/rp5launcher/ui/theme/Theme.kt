package io.github.hipsterbwp.rp5launcher.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import io.github.hipsterbwp.rp5launcher.R

/**
 * Every design token from DESIGN.md lives here. Screens never hard-code colors, sizes or type.
 *
 * Sizes are in mockup px ("units"). The mockups are 960×540, so one unit is 1/960 of the
 * screen width: 2 device px on the RP5's 1920×1080 panel, whatever display density is set.
 */

// ---------------------------------------------------------------------------------------------
// Color
// ---------------------------------------------------------------------------------------------

object Palette {
    val white = Color(0xFFFFFFFF)
    val black = Color(0xFF000000)
    val transparent = Color(0x00000000)

    /** Light page: details, setup, settings. */
    val page = Color(0xFFF4F1EA)
    /** Light surface / chip. */
    val surface = Color(0xFFE4DFD3)
    val card = Color(0xFFFFFFFF)

    val ink = Color(0xFF1C1A24)
    val inkSecondary = Color(0xFF5A5666)
    val inkBody = Color(0xFF3A3646)

    val hintBarLight = ink
    val hintBarOnSystem = Color(0x47000000) // rgba(0,0,0,0.28)

    /** Translucent surfaces on system color (0.10–0.14). */
    val onSystemSurfaceWeak = Color(0x1AFFFFFF) // 0.10
    val onSystemSurface = Color(0x1FFFFFFF) // 0.12
    val onSystemSurfaceStrong = Color(0x24FFFFFF) // 0.14
    val onSystemDarkChip = Color(0x40000000) // rgba(0,0,0,0.25)
    val onSystemStepInactive = Color(0x4DFFFFFF) // 0.30

    /** Focus outline drawn around white items on system color. */
    val focusRingOnSystem = Color(0x59FFFFFF) // 0.35
    val focusRingOnSystemCard = Color(0x73FFFFFF) // 0.45

    /** Modal scrim behind dialogs (translucency, not a shadow). */
    val scrim = Color(0x99000000)

    val ok = Color(0xFF1E6B48)
    val problem = Color(0xFFA3361A)
}

/** Default system colors (DESIGN.md table). Keys are ES-DE system names. Overridable in config. */
object SystemColors {
    val defaults: Map<String, Color> = mapOf(
        "snes" to Color(0xFF2E2370),
        "nes" to Color(0xFFB3261E),
        "gb" to Color(0xFF3E6B1F),
        "gba" to Color(0xFF3949AB),
        "n64" to Color(0xFF1E7353),
        "psx" to Color(0xFF4D4D57),
        "ps2" to Color(0xFF1F4BA5),
        "psp" to Color(0xFF1A1A1A),
    )

    /** Used for systems without a defined color. All pass 4.5:1 against white text. */
    private val fallbacks = listOf(
        Color(0xFF6A1B9A),
        Color(0xFF00695C),
        Color(0xFF8E3B12),
        Color(0xFF283593),
        Color(0xFF5D4037),
        Color(0xFF37474F),
        Color(0xFFAD1457),
        Color(0xFF2E7D32),
    )

    /** Color shown before any system is known (setup, empty library). */
    val neutral = Color(0xFF2E2370)

    fun forSystem(name: String): Color =
        defaults[name] ?: fallbacks[(name.hashCode() and 0x7fffffff) % fallbacks.size]
}

// ---------------------------------------------------------------------------------------------
// Units
// ---------------------------------------------------------------------------------------------

object Mockup {
    const val WIDTH = 960f
    const val HEIGHT = 540f
}

private val LocalUnitDp = staticCompositionLocalOf { 1f }

/** One mockup px as Dp. */
val Number.u: Dp
    @Composable @ReadOnlyComposable
    get() = (toFloat() * LocalUnitDp.current).dp

/** One mockup px as a font size (ignores the system font scale so layouts never clip). */
val Number.us: TextUnit
    @Composable @ReadOnlyComposable
    get() = with(LocalDensity.current) { (toFloat() * LocalUnitDp.current).dp.toSp() }

// ---------------------------------------------------------------------------------------------
// Type
// ---------------------------------------------------------------------------------------------

val Bricolage = FontFamily(
    Font(R.font.bricolage_regular, FontWeight.W400),
    Font(R.font.bricolage_semibold, FontWeight.W600),
    Font(R.font.bricolage_extrabold, FontWeight.W800),
)

object Weight {
    val body = FontWeight.W400
    val label = FontWeight.W600
    val heading = FontWeight.W800
}

/** Type scale in mockup px. */
object TypeScale {
    const val HERO = 76
    const val PAGE_TITLE = 54
    const val SETUP_TITLE = 44
    const val SECTION = 34
    const val SCREEN_TITLE = 30
    const val SCREEN_TITLE_SMALL = 22
    const val STAT = 22
    const val TILE_SELECTED = 26
    const val TILE = 20
    const val BRAND = 18
    const val BUTTON = 17
    const val BODY = 15
    const val BODY_SMALL = 14
    const val LABEL = 13
    const val LABEL_SMALL = 12
    const val MICRO = 11
    const val MICRO_SMALL = 10

    const val HEADING_TRACKING = -0.03f
    const val TITLE_TRACKING = -0.02f
    const val HEADING_LINE_HEIGHT = 0.95f
    const val CAPS_TRACKING = 0.08f
    const val BODY_LINE_HEIGHT = 1.5f
    const val CARD_TITLE_LINE_HEIGHT = 1.15f
}

private val trimmed = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)

@Composable
@ReadOnlyComposable
fun textStyle(
    size: Int,
    weight: FontWeight,
    tracking: Float = 0f,
    lineHeight: Float? = null,
): TextStyle = TextStyle(
    fontFamily = Bricolage,
    fontSize = size.us,
    fontWeight = weight,
    letterSpacing = tracking.em,
    lineHeight = lineHeight?.em ?: TextUnit.Unspecified,
    lineHeightStyle = if (lineHeight != null) trimmed else null,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

object Type {
    val hero @Composable @ReadOnlyComposable get() =
        textStyle(TypeScale.HERO, Weight.heading, TypeScale.HEADING_TRACKING, 0.92f)
    val pageTitle @Composable @ReadOnlyComposable get() =
        textStyle(TypeScale.PAGE_TITLE, Weight.heading, TypeScale.HEADING_TRACKING, TypeScale.HEADING_LINE_HEIGHT)
    val setupTitle @Composable @ReadOnlyComposable get() =
        textStyle(TypeScale.SETUP_TITLE, Weight.heading, TypeScale.HEADING_TRACKING, TypeScale.HEADING_LINE_HEIGHT)
    val section @Composable @ReadOnlyComposable get() =
        textStyle(TypeScale.SECTION, Weight.heading, TypeScale.TITLE_TRACKING, TypeScale.HEADING_LINE_HEIGHT)
    val screenTitle @Composable @ReadOnlyComposable get() =
        textStyle(TypeScale.SCREEN_TITLE, Weight.heading, TypeScale.TITLE_TRACKING, TypeScale.HEADING_LINE_HEIGHT)
    val screenTitleSmall @Composable @ReadOnlyComposable get() =
        textStyle(TypeScale.SCREEN_TITLE_SMALL, Weight.heading)
    val stat @Composable @ReadOnlyComposable get() = textStyle(TypeScale.STAT, Weight.heading)
    val tileSelected @Composable @ReadOnlyComposable get() = textStyle(TypeScale.TILE_SELECTED, Weight.heading)
    val tile @Composable @ReadOnlyComposable get() = textStyle(TypeScale.TILE, Weight.heading)
    val brand @Composable @ReadOnlyComposable get() = textStyle(TypeScale.BRAND, Weight.heading)
    val button @Composable @ReadOnlyComposable get() = textStyle(TypeScale.BUTTON, Weight.heading)
    val buttonSmall @Composable @ReadOnlyComposable get() = textStyle(TypeScale.BODY, Weight.heading)
    val body @Composable @ReadOnlyComposable get() =
        textStyle(TypeScale.BODY, Weight.body, lineHeight = TypeScale.BODY_LINE_HEIGHT)
    val bodySmall @Composable @ReadOnlyComposable get() =
        textStyle(TypeScale.BODY_SMALL, Weight.body, lineHeight = TypeScale.BODY_LINE_HEIGHT)
    val rowTitle @Composable @ReadOnlyComposable get() = textStyle(TypeScale.BODY_SMALL, Weight.heading)
    val navItem @Composable @ReadOnlyComposable get() = textStyle(TypeScale.BODY, Weight.label)
    val navItemSelected @Composable @ReadOnlyComposable get() = textStyle(TypeScale.BODY, Weight.heading)
    val tab @Composable @ReadOnlyComposable get() = textStyle(TypeScale.BODY_SMALL, Weight.label)
    val label @Composable @ReadOnlyComposable get() = textStyle(TypeScale.LABEL, Weight.label)
    val labelStrong @Composable @ReadOnlyComposable get() = textStyle(TypeScale.LABEL, Weight.heading)
    val cardTitle @Composable @ReadOnlyComposable get() =
        textStyle(TypeScale.LABEL, Weight.heading, lineHeight = TypeScale.CARD_TITLE_LINE_HEIGHT)
    val caption @Composable @ReadOnlyComposable get() = textStyle(TypeScale.LABEL_SMALL, Weight.body)
    val pill @Composable @ReadOnlyComposable get() = textStyle(TypeScale.LABEL_SMALL, Weight.heading)
    val glyph @Composable @ReadOnlyComposable get() = textStyle(TypeScale.LABEL_SMALL, Weight.heading)
    val glyphWide @Composable @ReadOnlyComposable get() = textStyle(TypeScale.MICRO, Weight.heading)
    val micro @Composable @ReadOnlyComposable get() = textStyle(TypeScale.MICRO, Weight.label)
    val caps @Composable @ReadOnlyComposable get() =
        textStyle(TypeScale.MICRO, Weight.heading, TypeScale.CAPS_TRACKING)
    val badge @Composable @ReadOnlyComposable get() = textStyle(TypeScale.MICRO_SMALL, Weight.heading)
}

// ---------------------------------------------------------------------------------------------
// Shape, spacing, sizes
// ---------------------------------------------------------------------------------------------

object Radius {
    const val PILL = 999
    const val TILE = 24
    const val TILE_SELECTED = 30
    const val CARD = 16
    const val CARD_SMALL = 14
    const val PANEL = 22
    const val PANEL_LARGE = 28
    const val GLYPH_RECT = 8
    const val BADGE = 10
    const val STEP = 3
}

val PillShape = RoundedCornerShape(50)

@Composable
@ReadOnlyComposable
fun rounded(radius: Int) = if (radius >= Radius.PILL) PillShape else RoundedCornerShape(radius.u)

object Space {
    const val SCREEN_H = 40
    const val SCREEN_TOP = 26
    const val SCREEN_TOP_LIGHT = 36
    const val SETTINGS_TOP = 32
    const val PANEL_H = 36
    const val SIDEBAR_H = 24
    const val DETAILS_H = 44

    const val LIST_GAP = 6
    const val GAP = 8
    const val BUTTON_GAP = 10
    const val ROW_GAP = 12
    const val TILE_GAP = 14
    const val SECTION_SMALL = 18
    const val SECTION = 22
    const val HINT_GAP = 22
    const val SECTION_LARGE = 28
    const val CAROUSEL_TOP = 30

    const val TINY = 2
    const val XS = 3
    const val SM = 4
    const val MD = 10
    const val LG = 16
    const val XL = 40

    const val PILL_V = 7
    const val PILL_H = 16
    const val PILL_H_SMALL = 14
    const val CHIP_V = 6
    const val CHIP_H = 12
    const val TAG_V = 5
    const val ROW_V = 10
    const val ROW_V_SMALL = 8
    const val ROW_H = 14
    const val CARD_PAD = 12
    const val GLYPH_RECT_H = 8
    const val BUTTON_H = 30
    const val BUTTON_H_SMALL = 26
    const val BUTTON_H_OUTLINE = 22
}

object Size {
    const val HINT_BAR = 48
    const val GLYPH = 24
    const val SYSTEM_TILE = 92
    const val SYSTEM_TILE_SELECTED = 116
    const val GAME_TILE_W = 150
    const val GAME_TILE_H = 104
    const val GAME_TILE_SELECTED_H = 120
    const val GRID_CARD_H = 150
    const val GRID_COLUMNS = 6
    const val ARTWORK_W = 320
    const val ARTWORK_H = 210
    const val BOX_ART_W = 250
    const val BOX_ART_H = 320
    const val DETAILS_PANEL = 380
    const val SETUP_PANEL = 340
    const val SETTINGS_SIDEBAR = 260
    const val BUTTON = 52
    const val BUTTON_SMALL = 46
    const val BADGE = 34
    const val STEP_W = 28
    const val STEP_H = 6
    const val CHEVRON = 16
    const val PLAY_ICON = 16
    const val DIALOG_W = 520
    const val DESCRIPTION_MAX_W = 440
    const val STAT_MAX_W = 200
    const val BORDER = 2

    const val FOCUS_RING = 4
    const val FOCUS_RING_CARD = 3
    const val FOCUS_RING_LIGHT = 3
    const val FOCUS_OFFSET = 4
}

object Focus {
    /** Grid cards grow slightly when focused (tiles grow via their size tokens). */
    const val CARD_SCALE = 1.05f
}

object Alpha {
    const val SECONDARY_ON_SYSTEM = 0.85f
    const val NAV_ON_SYSTEM = 0.9f
    const val CARD_META = 0.8f
}

// ---------------------------------------------------------------------------------------------
// Motion
// ---------------------------------------------------------------------------------------------

object Motion {
    const val COLOR_FADE_MS = 250
    const val SCREEN_FADE_MS = 180

    fun <T> colorFade() = tween<T>(COLOR_FADE_MS)
    fun <T> screenFade() = tween<T>(SCREEN_FADE_MS)

    /** Focus moves: spring that settles in ~150–200 ms. */
    fun <T> focus() = spring<T>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 900f)

    /** Held-direction acceleration: steps per key repeat once held this long (repeat count). */
    const val ACCEL_REPEATS_FAST = 8
    const val ACCEL_REPEATS_FASTER = 20
}

// ---------------------------------------------------------------------------------------------
// Root
// ---------------------------------------------------------------------------------------------

@Composable
fun LauncherTheme(content: @Composable () -> Unit) {
    val config = LocalConfiguration.current
    // Fit the 960×540 mockup into the screen, whatever its density or exact aspect ratio.
    val unitDp = minOf(config.screenWidthDp / Mockup.WIDTH, config.screenHeightDp / Mockup.HEIGHT)
    CompositionLocalProvider(LocalUnitDp provides unitDp, content = content)
}
