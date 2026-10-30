package io.github.hipsterbwp.rp5launcher.ui

import androidx.compose.ui.graphics.Color
import io.github.hipsterbwp.rp5launcher.data.SessionLog
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.MonthDay
import java.time.temporal.IsoFields

// ---------------------------------------------------------------------------------------------
// The Summit: climb Everest by playing
// ---------------------------------------------------------------------------------------------

/** One camp on the climb: its height, the climb points needed and what reaching it unlocks. */
data class Camp(val name: String, val metres: Int, val points: Int, val reward: String?, val rewardTitle: String?)

val CAMPS = listOf(
    Camp("Base Camp", 5364, 0, null, null),
    Camp("Camp I", 6065, 25, "outfit:beanie", "Woolly beanie for Yeti-1"),
    Camp("Camp II", 6400, 75, "scene_ocean", "Ocean sunset backdrop"),
    Camp("Camp III", 7162, 150, "outfit:goggles", "Snow goggles for Yeti-1"),
    Camp("Camp IV", 7950, 300, "theme_gold", "Golden theme"),
    Camp("Hillary Step", 8790, 450, "scene_space", "Deep space backdrop"),
    Camp("Summit", 8849, 600, "outfit:crown", "The Summit crown"),
)

/** Where you are on the mountain. */
data class Climb(val points: Int, val camp: Int, val metres: Int, val toNext: Int?) {
    val summit: Boolean get() = camp == CAMPS.lastIndex

    /** 0 at Base Camp, 1 at the Summit, by height. */
    val progress: Float
        get() = (metres - CAMPS.first().metres).toFloat() / (CAMPS.last().metres - CAMPS.first().metres)
}

/** Climb points: every credit ever earned (spent or not), plus 5 per game beaten and 10 per 100%. */
fun climb(earnedCredits: Int, beaten: Int, completed: Int): Climb {
    val points = earnedCredits + beaten * 5 + completed * 10
    val camp = CAMPS.indexOfLast { points >= it.points }
    val here = CAMPS[camp]
    val next = CAMPS.getOrNull(camp + 1)
    val metres = if (next == null) here.metres else {
        here.metres + ((next.metres - here.metres) * (points - here.points).toFloat() / (next.points - here.points)).toInt()
    }
    return Climb(points, camp, metres, next?.let { it.points - points })
}

// ---------------------------------------------------------------------------------------------
// Weekly quests
// ---------------------------------------------------------------------------------------------

/** What quests can look at: this week's sessions, games beaten this week, and console release years. */
class QuestContext(
    val week: List<SessionLog>,
    val beatenThisWeek: Int,
    val consoleYear: (String) -> Int?,
)

data class Quest(
    val id: String,
    val title: String,
    val goal: Int,
    val reward: Int,
    val progress: (QuestContext) -> Int,
)

private val ALWAYS = listOf(
    Quest("consoles", "Play 3 different consoles", 3, 5) { c -> c.week.map { it.system }.distinct().size },
    Quest("minutes", "Play for 2 hours", 120, 5) { c -> (c.week.sumOf { it.ms } / 60_000).toInt() },
)

private val ROTATING = listOf(
    Quest("beat", "Beat or 100% a game", 1, 10) { it.beatenThisWeek },
    Quest("variety", "Play 5 different games", 5, 4) { c -> c.week.map { it.game }.distinct().size },
    Quest("marathon", "One session of 45 minutes", 1, 4) { c -> if (c.week.any { it.ms >= 45 * 60_000 }) 1 else 0 },
    Quest("retro", "Play a console from before 1995", 1, 4) { c ->
        if (c.week.any { (c.consoleYear(it.system) ?: 9999) < 1995 }) 1 else 0
    },
    Quest("everyday", "Play on 4 different days", 4, 5) { c -> c.week.map { it.day }.distinct().size },
    Quest("handheld", "Play a handheld", 1, 3) { c -> if (c.week.any { it.system in HANDHELDS }) 1 else 0 },
)

private val HANDHELDS = setOf("gb", "gbc", "gba", "nds", "n3ds", "psp", "gamegear", "ngp", "ngpc", "atarilynx", "wonderswan", "wonderswancolor", "virtualboy")

/** "2026-W44" for the ISO week containing [day]. */
fun weekKey(day: LocalDate = LocalDate.now()): String =
    "%d-W%02d".format(day.get(IsoFields.WEEK_BASED_YEAR), day.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR))

/** Monday of [day]'s week. */
fun weekStart(day: LocalDate = LocalDate.now()): LocalDate = day.with(DayOfWeek.MONDAY)

/** This week's four quests: the two regulars plus two that rotate with the week. */
fun questsFor(week: String): List<Quest> {
    val seed = week.hashCode().let { if (it < 0) -it else it }
    val first = ROTATING[seed % ROTATING.size]
    val second = ROTATING[(seed / ROTATING.size + 1 + seed % ROTATING.size) % ROTATING.size].takeIf { it != first }
        ?: ROTATING[(seed + 1) % ROTATING.size]
    return ALWAYS + first + second
}

// ---------------------------------------------------------------------------------------------
// Seasons and console birthdays
// ---------------------------------------------------------------------------------------------

enum class Season(val title: String) { Halloween("Halloween"), Winter("Winter"), NewYear("New Year") }

fun seasonOn(day: LocalDate = LocalDate.now()): Season? {
    val md = MonthDay.from(day)
    return when {
        md >= MonthDay.of(10, 15) && md <= MonthDay.of(11, 1) -> Season.Halloween
        md >= MonthDay.of(12, 1) && md <= MonthDay.of(12, 30) -> Season.Winter
        md >= MonthDay.of(12, 31) || md <= MonthDay.of(1, 2) -> Season.NewYear
        else -> null
    }
}

/** First release dates (usually Japan), for "Happy birthday" banners. */
val CONSOLE_BIRTHDAYS: Map<String, LocalDate> = mapOf(
    "nes" to LocalDate.of(1983, 7, 15), "famicom" to LocalDate.of(1983, 7, 15),
    "snes" to LocalDate.of(1990, 11, 21), "sfc" to LocalDate.of(1990, 11, 21),
    "n64" to LocalDate.of(1996, 6, 23), "gb" to LocalDate.of(1989, 4, 21),
    "gbc" to LocalDate.of(1998, 10, 21), "gba" to LocalDate.of(2001, 3, 21),
    "nds" to LocalDate.of(2004, 11, 21), "n3ds" to LocalDate.of(2011, 2, 26),
    "gc" to LocalDate.of(2001, 9, 14), "wii" to LocalDate.of(2006, 11, 19),
    "switch" to LocalDate.of(2017, 3, 3), "virtualboy" to LocalDate.of(1995, 7, 21),
    "psx" to LocalDate.of(1994, 12, 3), "ps2" to LocalDate.of(2000, 3, 4), "psp" to LocalDate.of(2004, 12, 12),
    "megadrive" to LocalDate.of(1988, 10, 29), "genesis" to LocalDate.of(1988, 10, 29),
    "mastersystem" to LocalDate.of(1985, 10, 20), "gamegear" to LocalDate.of(1990, 10, 6),
    "saturn" to LocalDate.of(1994, 11, 22), "dreamcast" to LocalDate.of(1998, 11, 27),
    "xbox" to LocalDate.of(2001, 11, 15), "xbox360" to LocalDate.of(2005, 11, 22),
)

/** (console, age) for a console in [systems] born on [day], if any. */
fun birthdayOn(systems: List<String>, day: LocalDate = LocalDate.now()): Pair<String, Int>? =
    systems.firstNotNullOfOrNull { name ->
        CONSOLE_BIRTHDAYS[name]?.takeIf { MonthDay.from(it) == MonthDay.from(day) }?.let { name to day.year - it.year }
    }

fun ordinalOf(n: Int): String = n.toString() + when {
    n % 100 in 11..13 -> "th"
    n % 10 == 1 -> "st"
    n % 10 == 2 -> "nd"
    n % 10 == 3 -> "rd"
    else -> "th"
}

// ---------------------------------------------------------------------------------------------
// Yeti-1's wardrobe
// ---------------------------------------------------------------------------------------------

/**
 * Outfits drawn over Yeti-1. [rows] are sprite rows ('.' empty, other letters from [colors]); [onEyes]
 * places them on the eye line instead of on top of the head. Owned by unlock key "outfit:<key>",
 * bought for [cost] credits, earned on the Summit ([camp]) or worn free in their [season].
 */
enum class Outfit(
    val key: String,
    val title: String,
    val cost: Int?,
    val season: Season?,
    val onEyes: Boolean,
    val colors: Map<Char, Color>,
    val rows: List<String>,
) {
    Beanie(
        "beanie", "Woolly beanie", null, null, false,
        mapOf('R' to Color(0xFFE5395B), 'W' to Color(0xFFFFFFFF)),
        listOf("......WW......", "....RRRRRR....", "...RRRRRRRR...", "..WWWWWWWWWW.."),
    ),
    Goggles(
        "goggles", "Snow goggles", null, null, true,
        mapOf('K' to Color(0xFF1B1035), 'G' to Color(0xFF39E0FF)),
        listOf("KKKKKKKKKK", "KGGKKKKGGK"),
    ),
    Crown(
        "crown", "Summit crown", null, null, false,
        mapOf('Y' to Color(0xFFFFD84A), 'R' to Color(0xFFE5395B)),
        listOf("Y..Y..Y..Y", "YY.YY.YY.Y", "YYYYYYYYYY", "YRYYRYYRYY"),
    ),
    Shades(
        "shades", "Cool shades", 10, null, true,
        mapOf('K' to Color(0xFF111111), 'W' to Color(0xFF9FA8C0)),
        listOf("KKKKKKKKKK", ".KWK..KWK."),
    ),
    PlumberCap(
        "plumber", "Plumber's cap", 15, null, false,
        mapOf('R' to Color(0xFFE21B1B), 'W' to Color(0xFFFFFFFF)),
        listOf("...RRRRRR...", "..RRRWWRRRR.", ".RRRRRRRRRRR", "RRRRRRRRRRRRRR"),
    ),
    HeroCap(
        "hero", "Hero's cap", 15, null, false,
        mapOf('G' to Color(0xFF2BA84A), 'D' to Color(0xFF17702F)),
        listOf("..........GG", "......GGGGG.", "...GGGGGGD..", "..GGGGGGGD..", "..DDDDDDDD.."),
    ),
    Quills(
        "quills", "Blue quills", 20, null, false,
        mapOf('B' to Color(0xFF1F5BFF), 'D' to Color(0xFF0E2E99)),
        listOf("B....B....B.", "BB..BB...BB.", "BBBBBBBBBBB.", "DBBBBBBBBBBD"),
    ),
    Pumpkin(
        "pumpkin", "Pumpkin hat", null, Season.Halloween, false,
        mapOf('O' to Color(0xFFFF8A1F), 'G' to Color(0xFF2BA84A), 'K' to Color(0xFF1B1035)),
        listOf(".....GG.....", "...OOOOOO...", "..OKOOOOKO..", "..OOOKKOOO..", "...OOOOOO..."),
    ),
    Santa(
        "santa", "Santa hat", null, Season.Winter, false,
        mapOf('R' to Color(0xFFE21B1B), 'W' to Color(0xFFFFFFFF)),
        listOf("..........WW", "......RRRR..", "....RRRRRR..", "..RRRRRRRR..", ".WWWWWWWWWW."),
    ),
    Party(
        "party", "Party hat", null, Season.NewYear, false,
        mapOf('P' to Color(0xFFFF4FD8), 'Y' to Color(0xFFFFD84A)),
        listOf(".....Y.....", "....PPP....", "...PYPYP...", "..PPPPPPP..", ".YPYPYPYPY."),
    ),
    ;

    companion object {
        fun of(key: String?): Outfit? = entries.firstOrNull { it.key == key }
    }
}

// ---------------------------------------------------------------------------------------------
// Game Genie codes
// ---------------------------------------------------------------------------------------------

/** The sixteen letters on a Game Genie code sheet. */
const val GENIE_LETTERS = "APZLGITYEOXUKSVN"

enum class GenieCode(val code: String, val title: String) {
    BigYeti("YETIYETI", "Giant Yeti-1"),
    TinyYeti("TINYYETI", "Tiny Yeti-1"),
    Sunset("SUNSET", "+10 credits"),
    Pixels("PIXELS", "Scanlines on/off"),
    Toasty("TOASTY", "Toasty!"),
    Snooze("SNOOZE", "Yeti-1 takes a nap"),
    ;

    companion object {
        fun of(code: String): GenieCode? = entries.firstOrNull { it.code == code }
    }
}

// ---------------------------------------------------------------------------------------------
// The magazine: monthly issues and the year in review
// ---------------------------------------------------------------------------------------------

/** One issue's numbers, from the session log. */
data class Issue(
    val title: String,
    val minutes: Long,
    val sessions: Int,
    val games: Int,
    val consoles: Int,
    /** Most-played game id and console, by time. */
    val topGame: String?,
    val topSystem: String?,
    val longestMinutes: Long,
    val busiestDay: String?,
)

fun issueOf(title: String, sessions: List<SessionLog>): Issue = Issue(
    title = title,
    minutes = sessions.sumOf { it.ms } / 60_000,
    sessions = sessions.size,
    games = sessions.map { it.game }.distinct().size,
    consoles = sessions.map { it.system }.distinct().size,
    topGame = sessions.groupBy { it.game }.maxByOrNull { (_, s) -> s.sumOf { it.ms } }?.key,
    topSystem = sessions.groupBy { it.system }.maxByOrNull { (_, s) -> s.sumOf { it.ms } }?.key,
    longestMinutes = (sessions.maxOfOrNull { it.ms } ?: 0) / 60_000,
    busiestDay = sessions.groupBy { it.day }.maxByOrNull { (_, s) -> s.sumOf { it.ms } }?.key,
)
