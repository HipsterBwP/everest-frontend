package io.github.hipsterbwp.rp5launcher.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
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
 * Direction: retro arcade / synthwave. A deep purple night, hot pink and cyan neon frames, sunset
 * gradient titles in a pixel font (Press Start 2P), readable body text in Chakra Petch, CRT numbers
 * in VT323, arcade-button hints and faint scanlines over everything.
 *
 * Sizes are in mockup px ("units"). The mockups are 960×540, so one unit is 1/960 of the
 * screen width: 2 device px on the RP5's 1920×1080 panel, whatever display density is set.
 */

// ---------------------------------------------------------------------------------------------
// Color
// ---------------------------------------------------------------------------------------------

/** The two neon colours everything is lit with. Gold is the secret theme (Konami code, or bought with credits). */
enum class NeonTheme(val pink: Color, val pinkSoft: Color, val cyan: Color, val cyanSoft: Color) {
    Neon(Color(0xFFFF2A9D), Color(0xFFFF6FC3), Color(0xFF2DE2E6), Color(0xFF9FF7F8)),
    Gold(Color(0xFFFFB31A), Color(0xFFFFD877), Color(0xFFFFF1C2), Color(0xFFFFF8E1)),
    /** Seasonal: Halloween orange and witchy purple; winter ice. */
    Pumpkin(Color(0xFFFF7A1A), Color(0xFFFFAE5C), Color(0xFFB45CFF), Color(0xFFD9A8FF)),
    Frost(Color(0xFF5CC8FF), Color(0xFFA8E4FF), Color(0xFFFFFFFF), Color(0xFFEAF6FF)),
}

object Palette {
    /** Current neon theme; reading the neon colours below follows it. */
    var theme by mutableStateOf(NeonTheme.Neon)

    val white = Color(0xFFFFFFFF)
    val black = Color(0xFF000000)
    val transparent = Color(0x00000000)

    // Synthwave base colors
    val night = Color(0xFF0B0420)
    val deep = Color(0xFF160935)
    val violet = Color(0xFF2A0E5C)
    val pink: Color get() = theme.pink
    val pinkSoft: Color get() = theme.pinkSoft
    val cyan: Color get() = theme.cyan
    val cyanSoft: Color get() = theme.cyanSoft
    /** Sunset gradient stops, top to bottom (titles, the sun, the loading bar). */
    val sun1 = Color(0xFFFFE45E)
    val sun2 = Color(0xFFFF9A3D)
    val sun3 = Color(0xFFFF3C8E)
    val sun4 = Color(0xFFC32AFF)
    val sun5 = Color(0xFF6A2CFF)
    /** Hard drop shadow under sunset titles. */
    val chromeShadow = Color(0xFF2A0650)

    /** Page background: details, setup, settings. */
    val page = night
    /** Chips and secondary rows. */
    val surface = Color(0xFF24104A)
    /** List rows and sidebars. */
    val card = Color(0xFF170A36)
    val sidebar = Color(0xFF0E0526)

    /** Text on dark. Secondary keeps 4.5:1 on [card]. */
    val ink = Color(0xFFF6ECFF)
    val inkSecondary = Color(0xFFBDA6E6)
    val inkBody = Color(0xFFE2D6F5)
    val inkFaint = Color(0xFF7F6AA8)
    /** Text on light fills (button glyphs, status badges). */
    val inkOnLight = night

    /** Controls that aren't tied to a system. */
    val accent: Color get() = pink

    /** Panels: dark violet fill with a neon edge, drawn over art. */
    val glass = Color(0xCC0E0522)
    val glassStrong = Color(0xE60E0522)
    val glassEdge: Color get() = pink.copy(alpha = 0.5f)
    /** Darkens art under text (top and bottom of full-screen backdrops). */
    val shade = Color(0x8C0B0420)

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

    /** Modal scrim behind dialogs. */
    val scrim = Color(0xB30B0420)

    val ok = Color(0xFF2BD96B)
    val problem = Color(0xFFFF4D6D)
    /** Rating stars. */
    val star = sun1

    /** Arcade buttons in the hint bar (like the candy buttons on a cabinet). */
    val buttonA = Color(0xFFFF3B5C)
    val buttonB = Color(0xFFFFC93C)
    val buttonX = Color(0xFF3C8DFF)
    val buttonY = Color(0xFF2BD96B)
    val buttonOther = inkSecondary
    /** PlayStation-style glyphs take the colours printed on the RP5's face buttons. */
    val buttonCross = Color(0xFF3C8DFF)
    val buttonCircle = Color(0xFFFF7A2E)
    val buttonSquare = Color(0xFFFF3B5C)
    val buttonTriangle = Color(0xFF2BD96B)

    /** Everest brand on the loading screen: the glow behind the mountain badge. */
    val brandNavy = night
    val brandBlue: Color get() = pink
    val brandBlueLight: Color get() = pinkSoft
    val brandGlow: Color get() = pink.copy(alpha = 0.5f)
    val brandTrack: Color get() = pink.copy(alpha = 0.2f)
    val brandEdge: Color get() = pink.copy(alpha = 0.7f)
    val brandText = inkSecondary
    val brandSheen = Color(0x8CFFFFFF)
}

/** Gradients shared across screens. */
object Retro {
    /** Sunset fill for titles and the sun. */
    val sunset = Brush.verticalGradient(listOf(Palette.sun1, Palette.sun2, Palette.sun3, Palette.sun4, Palette.sun5))
    /** Focused row: pink fading to nothing, left to right. */
    val focusFill: Brush get() = Brush.horizontalGradient(listOf(Palette.pink.copy(alpha = 0.35f), Palette.pink.copy(alpha = 0.05f)))
    /** Colour wash over backdrop art (blend mode Color): pink top-left to cyan bottom-right. */
    val duotone: Brush get() = Brush.linearGradient(listOf(Palette.pink.copy(alpha = 0.22f), Palette.cyan.copy(alpha = 0.14f)))
}

/** Each console's own neon on the console screen (grid, selection, sign, panel edge). */
object SystemNeon {
    private val colors: Map<String, Color> = mapOf(
        "gb" to Color(0xFF9BE564), "gbc" to Color(0xFFB05CFF), "gba" to Color(0xFF7A7CFF),
        "nes" to Color(0xFFFF4040), "famicom" to Color(0xFFFF4040), "snes" to Color(0xFFB69CFF), "sfc" to Color(0xFFB69CFF),
        "n64" to Color(0xFF2BD96B), "gc" to Color(0xFF8A5CFF), "wii" to Color(0xFF9FF7F8), "wiiu" to Color(0xFF2DB8E6),
        "switch" to Color(0xFFFF4D4D), "nds" to Color(0xFF7FB2FF), "n3ds" to Color(0xFFFF4D6A),
        "psx" to Color(0xFF9AA8FF), "ps2" to Color(0xFF3C8DFF), "ps3" to Color(0xFF6E8BFF), "psp" to Color(0xFFC6CCFF), "psvita" to Color(0xFF4DA3FF),
        "dreamcast" to Color(0xFFFF8A3D), "saturn" to Color(0xFF6E7BFF), "megadrive" to Color(0xFF3C6DFF), "genesis" to Color(0xFF3C6DFF),
        "mastersystem" to Color(0xFFFF4D4D), "gamegear" to Color(0xFF5CC8FF), "segacd" to Color(0xFF6E7BFF),
        "arcade" to Color(0xFFFFC93C), "mame" to Color(0xFFFFC93C), "fbneo" to Color(0xFFFFC93C), "neogeo" to Color(0xFFFFC93C),
        "atari2600" to Color(0xFFFF8A3D), "pcengine" to Color(0xFFFF6FC3), "tg16" to Color(0xFFFF6FC3),
    )

    /** The console's neon, or the theme's pink. In the gold theme everything stays gold. */
    fun forSystem(name: String): Color = if (Palette.theme == NeonTheme.Gold) Palette.pink else colors[name] ?: Palette.pink
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

/** Press Start 2P (SIL OFL): titles, menus, tabs and labels. Crispest at multiples of 4 px. */
val PixelFont = FontFamily(Font(R.font.pressstart2p_regular, FontWeight.W400))

/** Chakra Petch (SIL OFL): body text, descriptions and list rows. */
val AppFont = FontFamily(
    Font(R.font.chakrapetch_regular, FontWeight.W400),
    Font(R.font.chakrapetch_semibold, FontWeight.W600),
    Font(R.font.chakrapetch_bold, FontWeight.W700),
)

/** VT323 (SIL OFL): CRT-style numbers (clock, scores, counts). */
val CrtFont = FontFamily(Font(R.font.vt323_regular, FontWeight.W400))

object Weight {
    val body = FontWeight.W400
    val label = FontWeight.W600
    val heading = FontWeight.W700
}

/** Type scale in mockup px. Pixel-font sizes are smaller: Press Start 2P is wide and tall. */
object TypeScale {
    const val BODY = 15
    const val BODY_SMALL = 14
    const val LABEL = 13
    const val LABEL_SMALL = 12
    const val MICRO = 11

    // Press Start 2P
    const val PIXEL_HERO = 40
    const val PIXEL_PAGE = 20
    const val PIXEL_SECTION = 18
    const val PIXEL_SETUP = 16
    const val PIXEL_SCREEN = 14
    const val PIXEL_TILE_SELECTED = 12
    const val PIXEL_LARGE = 10
    const val PIXEL_MENU = 9
    const val PIXEL_BUTTON = 8
    const val PIXEL_LABEL = 7
    const val PIXEL_SMALL = 6
    const val PIXEL_MICRO = 5

    // VT323
    const val CRT_LARGE = 24
    const val CRT = 20
    const val CRT_SMALL = 15

    const val PIXEL_LINE_HEIGHT = 1.35f
    const val CAPS_TRACKING = 0.08f
    const val EYEBROW_TRACKING = 0.12f
    const val BODY_LINE_HEIGHT = 1.45f
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
    family: FontFamily = AppFont,
): TextStyle = TextStyle(
    fontFamily = family,
    fontSize = size.us,
    fontWeight = weight,
    letterSpacing = tracking.em,
    lineHeight = lineHeight?.em ?: TextUnit.Unspecified,
    lineHeightStyle = if (lineHeight != null) trimmed else null,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

@Composable
@ReadOnlyComposable
private fun pixel(size: Int, tracking: Float = 0f) =
    textStyle(size, FontWeight.W400, tracking, TypeScale.PIXEL_LINE_HEIGHT, PixelFont)

@Composable
@ReadOnlyComposable
private fun crt(size: Int) = textStyle(size, FontWeight.W400, family = CrtFont)

object Type {
    val hero @Composable @ReadOnlyComposable get() = pixel(TypeScale.PIXEL_HERO)
    val pageTitle @Composable @ReadOnlyComposable get() = pixel(TypeScale.PIXEL_PAGE)
    val setupTitle @Composable @ReadOnlyComposable get() = pixel(TypeScale.PIXEL_SETUP)
    val section @Composable @ReadOnlyComposable get() = pixel(TypeScale.PIXEL_SECTION)
    val screenTitle @Composable @ReadOnlyComposable get() = pixel(TypeScale.PIXEL_SCREEN)
    val screenTitleSmall @Composable @ReadOnlyComposable get() = pixel(TypeScale.PIXEL_LARGE)
    val stat @Composable @ReadOnlyComposable get() = crt(TypeScale.CRT_LARGE)
    val tileSelected @Composable @ReadOnlyComposable get() = pixel(TypeScale.PIXEL_TILE_SELECTED)
    val tile @Composable @ReadOnlyComposable get() = pixel(TypeScale.PIXEL_MENU)
    val brand @Composable @ReadOnlyComposable get() = pixel(TypeScale.PIXEL_MENU)
    val button @Composable @ReadOnlyComposable get() = pixel(TypeScale.PIXEL_MENU)
    val buttonSmall @Composable @ReadOnlyComposable get() = pixel(TypeScale.PIXEL_BUTTON)
    val body @Composable @ReadOnlyComposable get() =
        textStyle(TypeScale.BODY, Weight.body, lineHeight = TypeScale.BODY_LINE_HEIGHT)
    val bodySmall @Composable @ReadOnlyComposable get() =
        textStyle(TypeScale.BODY_SMALL, Weight.body, lineHeight = TypeScale.BODY_LINE_HEIGHT)
    val rowTitle @Composable @ReadOnlyComposable get() = textStyle(TypeScale.BODY_SMALL, Weight.label)
    /** Arcade menu entries ("PLAY", "PROGRESS"). */
    val menu @Composable @ReadOnlyComposable get() = pixel(TypeScale.PIXEL_MENU)
    val navItem @Composable @ReadOnlyComposable get() = pixel(TypeScale.PIXEL_LABEL)
    val navItemSelected @Composable @ReadOnlyComposable get() = pixel(TypeScale.PIXEL_LABEL)
    val tab @Composable @ReadOnlyComposable get() = pixel(TypeScale.PIXEL_LABEL)
    val hint @Composable @ReadOnlyComposable get() = pixel(TypeScale.PIXEL_LABEL)
    val label @Composable @ReadOnlyComposable get() = textStyle(TypeScale.LABEL, Weight.label)
    val labelStrong @Composable @ReadOnlyComposable get() = textStyle(TypeScale.LABEL, Weight.heading)
    val cardTitle @Composable @ReadOnlyComposable get() =
        textStyle(TypeScale.LABEL, Weight.heading, lineHeight = TypeScale.CARD_TITLE_LINE_HEIGHT)
    val caption @Composable @ReadOnlyComposable get() = textStyle(TypeScale.LABEL_SMALL, Weight.body)
    val pill @Composable @ReadOnlyComposable get() = pixel(TypeScale.PIXEL_SMALL)
    val glyph @Composable @ReadOnlyComposable get() = pixel(TypeScale.PIXEL_LABEL)
    val glyphWide @Composable @ReadOnlyComposable get() = pixel(TypeScale.PIXEL_MICRO)
    val micro @Composable @ReadOnlyComposable get() = textStyle(TypeScale.MICRO, Weight.label)
    val caps @Composable @ReadOnlyComposable get() = pixel(TypeScale.PIXEL_SMALL, TypeScale.CAPS_TRACKING)
    val badge @Composable @ReadOnlyComposable get() = pixel(TypeScale.PIXEL_MICRO)
    /** Small label above a title ("SONY · 2000"), in neon cyan. */
    val eyebrow @Composable @ReadOnlyComposable get() = pixel(TypeScale.PIXEL_LABEL, TypeScale.EYEBROW_TRACKING)
    /** CRT numbers: clock, scores. */
    val crt @Composable @ReadOnlyComposable get() = crt(TypeScale.CRT)
    val crtSmall @Composable @ReadOnlyComposable get() = crt(TypeScale.CRT_SMALL)
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
    /** Start-up tribute emblem, and the gaming-history card's width. */
    const val TRIBUTE_MARK = 120
    /** The console birthday banner sits under the Game of the Day banner. */
    const val BIRTHDAY_OFFSET = 34
    /** Insert-the-game: the stage, cartridges, disc, the console body and how far media falls from. */
    const val INSERT_STAGE_W = 420
    const val INSERT_STAGE_H = 340
    const val CART_W = 130
    const val CART_H = 150
    const val CART_SMALL_W = 90
    const val CART_SMALL_H = 80
    const val DISC = 170
    const val CONSOLE_BODY_H = 110
    const val INSERT_DROP = 320f
    /** Base Camp: a wardrobe preview, a Game Genie key and code slot. */
    const val WARDROBE_YETI = 64
    const val GENIE_KEY = 34
    const val GENIE_SLOT_W = 28
    /** The N64 tribute's spinning "E", sized to sit with the hero letters. */
    const val TRIBUTE_E = 72
    const val HISTORY_W = 380
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
    /** Spinning 3D logo sign on the console screen: face height, extrusion depth and glow. */
    const val SIGN_H = 150
    const val SIGN_DEPTH = 22
    const val SIGN_EDGE = 1
    const val SIGN_GLOW = 22
    /** Pixel battery next to the clock. */
    const val BATTERY_W = 24
    const val BATTERY_H = 12
    /** Settings sliders: knob, track thickness and the value label. */
    const val SLIDER_KNOB = 14
    const val SLIDER_TRACK = 5
    const val SLIDER_VALUE_W = 40
    /** "SE 01" column in the Sound Test. */
    const val SE_NUM_W = 56
    /** Attract mode high-score table and its rank column. */
    const val HIGH_SCORES_W = 520
    const val RANK_W = 36
    /** Game of the Day title on the console screen. */
    const val DAILY_TITLE_W = 200
    /** Boot check cursor. */
    const val BOOT_CURSOR_W = 10
    const val BOOT_CURSOR_H = 16
    /** Era skins: dot-matrix / scanline spacing. */
    const val ERA_DOT = 3
    /** Yeti-1: size of one sprite pixel, and how far it sinks into the hint bar. */
    const val YETI_PIXEL = 2
    const val YETI_SINK = 4
    /** Pixel banner (achievements, unlocks). */
    const val TOAST_W = 420
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

    // Loading screen: the Everest badge above the horizon, the name on the grid
    const val SPLASH_LOGO = 250
    const val SPLASH_LOGO_TOP = 40
    const val SPLASH_TITLE_GAP = 22
    const val SPLASH_BAR_W = 380
    const val SPLASH_BAR_H = 22
    const val SPLASH_SEGMENTS = 20
    const val STATUS_H = 26
    const val PREVIEW_COVER_W = 140

    // Neon frames
    const val NEON_EDGE = 1.5
    const val NEON_BRACKET = 22
    const val NEON_BRACKET_W = 2
    const val NEON_BRACKET_OFFSET = 6
    const val TEXT_GLOW = 5
    const val CHROME_DROP = 2
    /** Grid floor and star size on the synthwave backdrop. */
    const val GRID_LINE = 1
    /** Space for the ▶ cursor in front of list rows. */
    const val CURSOR_W = 9

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
    /** CRT switch-off before a game starts, and switch-on when you come back. */
    const val CRT_OFF_MS = 520
    const val CRT_ON_MS = 480
    /** Arcade BIOS check on the loading screen: one line every BOOT_LINE_MS, then a short hold. */
    const val BOOT_LINE_MS = 120
    const val BOOT_HOLD_MS = 350
    /** Yeti-1 walks across the screen and back in YETI_STROLL_MS; one step takes YETI_STEP_MS. */
    const val YETI_STROLL_MS = 40_000
    const val YETI_STEP_MS = 420
    /** Start-up tribute length, the challenger alert, the 1UP pop-up. */
    const val TRIBUTE_MS = 2_200
    /** The N64 tribute spins the E in and then drops six letters, so it runs longer. */
    const val TRIBUTE_LONG_MS = 3_400
    /** The PS2 tribute plays the whole start-up: burst, title card, towers, logo. */
    const val TRIBUTE_PS2_MS = 6_000
    const val CHALLENGER_MS = 2_400L
    const val ONE_UP_MS = 2_000L
    /** Seasonal weather loops over this long. */
    const val SEASON_LOOP_MS = 30_000
    /** Turning a game case around on the details page. */
    const val BOX_FLIP_MS = 450
    /** Insert-the-game timings: media going in, a bad-cart glitch, blowing on it. */
    const val INSERT_MS = 1_000
    const val GLITCH_MS = 800
    const val BLOW_MS = 1_100
    /** CONTINUE? 9… 8… one number per tick. */
    const val CONTINUE_TICK_MS = 1_000L
    /** End credits scroll speed: units per second. */
    const val CREDITS_SPEED = 28f
    /** Pixel banners stay up this long. */
    const val TOAST_MS = 3_500L
    /** One full turn of the console logo sign. */
    const val SIGN_SPIN_MS = 9_000
    /** The grid floor rolls one row towards you in this long. */
    const val GRID_ROLL_MS = 2_400
    /** Focus glows breathe in and out over this long. */
    const val GLOW_PULSE_MS = 1_100
    /** INSERT COIN blink: on and off for this long each. */
    const val BLINK_MS = 600
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
