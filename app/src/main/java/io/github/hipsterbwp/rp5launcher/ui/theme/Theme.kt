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
 * Direction: an Apple-TV-like dark UI. Near-black pages, grouped dark cards, frosted-glass panels
 * over game art, system color as an accent, Inter type (the free cousin of San Francisco).
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

    /** Page background: details, setup, settings (Apple dark "system background"). */
    val page = Color(0xFF000000)
    /** Chips and secondary rows (Apple "tertiary grouped background"). */
    val surface = Color(0xFF2C2C2E)
    /** Grouped list cards and sidebars (Apple "secondary grouped background"). */
    val card = Color(0xFF1C1C1E)
    val sidebar = Color(0xFF111113)

    /** Text on dark. Secondary keeps 4.5:1 on [card]. */
    val ink = Color(0xFFFFFFFF)
    val inkSecondary = Color(0xFFA1A1A8)
    val inkBody = Color(0xFFD9D9DE)
    /** Text on white (focused cards, button glyphs). */
    val inkOnLight = Color(0xFF1C1C1E)

    /** Apple system blue, for controls that aren't tied to a system. */
    val accent = Color(0xFF0A84FF)

    /** Frosted glass: translucent dark fill plus a hairline edge, drawn over blurred art. */
    val glass = Color(0x9E161618) // ~62%
    val glassStrong = Color(0xCC161618) // 80%
    val glassEdge = Color(0x1FFFFFFF) // 12%
    /** Darkens art under text (top and bottom of full-screen backdrops). */
    val shade = Color(0x8C000000) // 55%

    val hintBarLight = glassStrong
    val hintBarOnSystem = glass

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

    /** Apple dark-mode green/red, both readable on [card]. */
    val ok = Color(0xFF30D158)
    val problem = Color(0xFFFF6961)
    /** Rating stars. */
    val star = Color(0xFFFFD60A)

    /** ARCROW brand: navy ground, electric blue glow (from the logo). */
    val brandNavy = Color(0xFF050B18)
    val brandBlue = Color(0xFF1E6BFF)
    val brandBlueLight = Color(0xFF4FA8FF)
    val brandGlow = Color(0x801E6BFF)
    val brandTrack = Color(0x332F7BFF)
    val brandEdge = Color(0xB32F7BFF)
    val brandText = Color(0xFFC9D6EE)
    val brandSheen = Color(0x8CFFFFFF)
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
        Color(0xFF00838F),
        Color(0xFF4527A0),
        Color(0xFFC62828),
        Color(0xFF558B2F),
    )

    /** Color shown before any system is known (setup, empty library). */
    val neutral = Color(0xFF2E2370)

    fun forSystem(name: String): Color =
        defaults[name] ?: fallbacks[(name.hashCode() and 0x7fffffff) % fallbacks.size]

    /**
     * Colors for a whole library: DESIGN.md defaults first, then fallbacks handed out in order so
     * neighbouring systems never share a color (a tile matching the background would vanish).
     */
    fun assign(names: List<String>): Map<String, Color> {
        val used = names.mapNotNull { defaults[it] }.toMutableSet()
        val pool = fallbacks.filter { it !in used }.ifEmpty { fallbacks }
        var next = 0
        return names.associateWith { name ->
            defaults[name] ?: pool[next++ % pool.size]
        }
    }
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

/** Inter (SIL OFL), static instances tuned for text (400/600) and display (700) sizes. */
val AppFont = FontFamily(
    Font(R.font.inter_regular, FontWeight.W400),
    Font(R.font.inter_semibold, FontWeight.W600),
    Font(R.font.inter_bold, FontWeight.W700),
)

object Weight {
    val body = FontWeight.W400
    val label = FontWeight.W600
    val heading = FontWeight.W700
}

/** Type scale in mockup px. */
object TypeScale {
    const val HERO = 60
    const val PAGE_TITLE = 44
    const val SETUP_TITLE = 38
    const val SECTION = 30
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

    const val HEADING_TRACKING = -0.025f
    const val TITLE_TRACKING = -0.015f
    const val HEADING_LINE_HEIGHT = 1.02f
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
    fontFamily = AppFont,
    fontSize = size.us,
    fontWeight = weight,
    letterSpacing = tracking.em,
    lineHeight = lineHeight?.em ?: TextUnit.Unspecified,
    lineHeightStyle = if (lineHeight != null) trimmed else null,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

object Type {
    val hero @Composable @ReadOnlyComposable get() =
        textStyle(TypeScale.HERO, Weight.heading, TypeScale.HEADING_TRACKING, TypeScale.HEADING_LINE_HEIGHT)
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
    /** Small caps label above a title ("PLAYSTATION 2 · 2000"). */
    val eyebrow @Composable @ReadOnlyComposable get() =
        textStyle(TypeScale.LABEL_SMALL, Weight.label, TypeScale.CAPS_TRACKING)
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

    /** Inset of a console logo inside its tile. */
    const val TILE_LOGO_PAD = 12

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
    const val BOX_ART_W = 300
    const val BOX_ART_H = 340
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
    const val HAIRLINE = 1
    const val PROGRESS_H = 8

    // Console screen: logo rail | glass info panel | hero art
    const val RAIL_W = 168
    const val RAIL_LOGO_H = 30
    const val RAIL_LOGO_SELECTED_H = 50
    const val RAIL_ITEM_GAP = 26
    const val SYSTEM_PANEL_W = 300
    /** Box art thumbnail on the panel's Continue card. */
    const val CONTINUE_ART_H = 44
    /** Quick menu sheet (hold Select). */
    const val QUICK_MENU_W = 340
    /** Top-shade height on full-screen backdrops. */
    const val TOP_SHADE_H = 150

    // Game browser: media + info column | cover grid
    const val BROWSER_INFO_W = 410
    const val MEDIA_H = 230
    const val COVER_COLUMNS = 3
    const val COVER_H = 196
    const val LOGO_SMALL_H = 30

    // Loading screen (proportions from the ARCROW mockup)
    const val SPLASH_LOGO = 300
    const val SPLASH_GLOW = 520
    const val SPLASH_WORDMARK_W = 420
    const val SPLASH_BAR_W = 360
    const val SPLASH_BAR_H = 12
    const val SPLASH_BAR_EDGE = 1
    const val STATUS_H = 26
    const val PREVIEW_COVER_W = 140

    const val FOCUS_RING = 4
    const val FOCUS_RING_CARD = 3
    const val FOCUS_RING_LIGHT = 3
    const val FOCUS_OFFSET = 4
}

object Focus {
    /** Grid cards grow slightly when focused (tiles grow via their size tokens). */
    const val CARD_SCALE = 1.05f
    /** tvOS-style lift for a focused cover. */
    const val COVER_SCALE = 1.08f
    /** Slow zoom on slideshow stills. */
    const val KEN_BURNS_SCALE = 1.12f
    /** Unfocused rail logos fade back. */
    const val RAIL_DIM = 0.3f
}

/** Soft shadows (in units) under lifted and floating elements. */
object Elevation {
    const val CARD = 10
    const val LIFTED = 24
    const val MEDIA = 18
}

/** Blur radius (units) for art used as a backdrop behind glass. */
object Blur {
    const val BACKDROP = 26
}

object Alpha {
    const val SECONDARY_ON_SYSTEM = 0.85f
    const val NAV_ON_SYSTEM = 0.9f
    const val CARD_META = 0.8f
    /** System color over a gameplay screenshot behind the game grid (keeps white text at 4.5:1). */
    const val BACKDROP_SCRIM = 0.84f
    /** Lighter layer on the details panel, where only box art sits on top. */
    const val PANEL_BACKDROP_SCRIM = 0.55f
    /** Darkest point of the shade behind the console screen's rail and panel. */
    const val HERO_SHADE = 0.85f
    /** Shade along the top of full-screen backdrops, under the tabs and clock. */
    const val TOP_SHADE = 0.45f
    /** How much of the system colour mixes into backdrop shades when tinting is on. */
    const val BACKDROP_TINT = 0.4f
    /** Highlight behind the selected logo in the rail. */
    const val RAIL_SELECTION = 0.12f
    /** Dark layer over the blurred backdrop of the game browser. */
    const val BROWSER_DIM = 0.45f
}

// ---------------------------------------------------------------------------------------------
// Motion
// ---------------------------------------------------------------------------------------------

object Motion {
    const val COLOR_FADE_MS = 250
    const val SCREEN_FADE_MS = 180
    /** Loading screen: shortest time on screen (so it never flashes), and its fade out. */
    const val SPLASH_MIN_MS = 1400L
    const val SPLASH_FADE_MS = 450
    const val SPLASH_BAR_MS = 350
    /** The bar's minimum fill time (kept under SPLASH_MIN_MS so it reaches the end on screen). */
    const val SPLASH_FILL_MS = 1200
    const val SPLASH_SHEEN_MS = 1100
    /** Screensaver: time per picture and the cross-fade between pictures. */
    const val SCREENSAVER_SLIDE_MS = 9000L
    const val SCREENSAVER_FADE_MS = 1500
    /** Pause on a console before its startup sound plays. */
    const val CONSOLE_SOUND_DELAY_MS = 450L
    /** Pause on a console before its gameplay montage starts, and how long each clip plays. */
    const val MONTAGE_DELAY_MS = 4_000L
    const val MONTAGE_CLIP_MS = 9_000L
    /** Gameplay stills slideshow: time per still, and its zoom duration. */
    const val SLIDE_MS = 4500
    const val KEN_BURNS_MS = 6000

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
