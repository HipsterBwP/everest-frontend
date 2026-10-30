package io.github.hipsterbwp.everest.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GeoSize
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import io.github.hipsterbwp.everest.ui.ArtImage
import io.github.hipsterbwp.everest.ui.CAMPS
import io.github.hipsterbwp.everest.ui.ChromeTxt
import io.github.hipsterbwp.everest.ui.GENIE_LETTERS
import io.github.hipsterbwp.everest.ui.GenieCode
import io.github.hipsterbwp.everest.ui.Hint
import io.github.hipsterbwp.everest.ui.HintBar
import io.github.hipsterbwp.everest.ui.Issue
import io.github.hipsterbwp.everest.ui.LauncherViewModel
import io.github.hipsterbwp.everest.ui.Outfit
import io.github.hipsterbwp.everest.ui.Pad
import io.github.hipsterbwp.everest.ui.PadInput
import io.github.hipsterbwp.everest.ui.RetroBackdrop
import io.github.hipsterbwp.everest.ui.Screen
import io.github.hipsterbwp.everest.ui.Txt
import io.github.hipsterbwp.everest.ui.UiSound
import io.github.hipsterbwp.everest.ui.YetiSprites
import io.github.hipsterbwp.everest.ui.drawOutfit
import io.github.hipsterbwp.everest.ui.drawSprite
import io.github.hipsterbwp.everest.ui.neonPanel
import io.github.hipsterbwp.everest.ui.tap
import io.github.hipsterbwp.everest.ui.theme.Motion
import io.github.hipsterbwp.everest.ui.theme.Palette
import io.github.hipsterbwp.everest.ui.theme.Radius
import io.github.hipsterbwp.everest.ui.theme.Size
import io.github.hipsterbwp.everest.ui.theme.Space
import io.github.hipsterbwp.everest.ui.theme.Type
import io.github.hipsterbwp.everest.ui.theme.rounded
import io.github.hipsterbwp.everest.ui.theme.u
import io.github.hipsterbwp.everest.ui.weekKey
import kotlin.math.sin

private val TABS = listOf("Summit", "Quests", "Wardrobe", "Memory card", "Codes", "Magazine")

/**
 * Base Camp: the Summit climb, this week's quests, Yeti-1's wardrobe, the memory card of games you've
 * played, the Game Genie code sheet and the magazine. L1/R1 switch tabs; B goes back.
 */
@Composable
fun BaseCampScreen(vm: LauncherViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { vm.checkSummit() }
    fun switch(delta: Int) {
        tab = (tab + delta).mod(TABS.size)
        vm.sound(UiSound.Tab)
    }
    Box(Modifier.fillMaxSize().background(Palette.night)) {
        RetroBackdrop()
        Column(Modifier.fillMaxSize().padding(start = Space.SCREEN_H.u, end = Space.SCREEN_H.u, top = Space.SCREEN_TOP.u, bottom = Size.HINT_BAR.u)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChromeTxt("Base Camp", Type.pageTitle, Modifier.weight(1f))
                TabPills(TABS, tab) { tab = it }
            }
            Spacer(Modifier.height(Space.SECTION.u))
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (tab) {
                    0 -> SummitTab(vm, ::switch)
                    1 -> QuestsTab(vm, ::switch)
                    2 -> WardrobeTab(vm, ::switch)
                    3 -> MemoryCardTab(vm, ::switch)
                    4 -> CodesTab(vm, ::switch)
                    else -> MagazineTab(vm, ::switch)
                }
            }
        }
        val hints = buildList {
            when (tab) {
                1 -> add(Hint(Pad.A, "Claim"))
                2 -> add(Hint(Pad.A, "Buy / wear"))
                3 -> add(Hint(Pad.A, "Open game"))
                4 -> {
                    add(Hint(Pad.A, "Letter"))
                    add(Hint(Pad.X, "Delete"))
                    add(Hint(Pad.Start, "Enter code"))
                }
                5 -> add(Hint(listOf(Pad.Left, Pad.Right), "Month / year"))
            }
            add(Hint(listOf(Pad.L1, Pad.R1), "Tabs", onClick = { switch(1) }))
            add(Hint(Pad.B, "Back", trailing = true, onClick = { vm.back() }))
        }
        HintBar(hints, onSystem = true, glyphInk = Palette.inkOnLight)
    }
}

/** Shared pad handling: L1/R1 switch tabs, B goes back, the rest goes to [onPad]. */
@Composable
private fun TabInput(vm: LauncherViewModel, switch: (Int) -> Unit, onPad: (Pad) -> Boolean) {
    PadInput { button, _ ->
        when (button) {
            Pad.L1 -> { switch(-1); true }
            Pad.R1 -> { switch(1); true }
            Pad.B -> vm.back()
            else -> onPad(button)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Summit
// ---------------------------------------------------------------------------------------------

@Composable
private fun SummitTab(vm: LauncherViewModel, switch: (Int) -> Unit) {
    val climb = vm.climb()
    TabInput(vm, switch) { false }
    val step by rememberInfiniteTransition(label = "climber").animateFloat(
        0f, 1f, infiniteRepeatable(tween(Motion.YETI_STEP_MS * 2, easing = LinearEasing)), label = "climber-step",
    )
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(Space.SECTION.u)) {
        Box(Modifier.weight(1.4f).fillMaxHeight().neonPanel(Radius.PANEL, edge = Palette.cyan, fill = Palette.glassStrong)) {
            Canvas(Modifier.fillMaxSize().padding(Space.LG.u)) {
                val w = size.width
                val h = size.height
                // The mountain: a big peak with a snow cap and a smaller one behind.
                val back = Path().apply { moveTo(w * 0.05f, h); lineTo(w * 0.3f, h * 0.35f); lineTo(w * 0.55f, h); close() }
                drawPath(back, Color(0xFF2A1A5E))
                val peak = Path().apply { moveTo(w * 0.12f, h); lineTo(w * 0.6f, h * 0.06f); lineTo(w * 0.98f, h); close() }
                drawPath(peak, Brush.verticalGradient(listOf(Color(0xFF6B4FC8), Color(0xFF231147)), startY = 0f, endY = h))
                val snow = Path().apply {
                    moveTo(w * 0.6f, h * 0.06f); lineTo(w * 0.72f, h * 0.3f); lineTo(w * 0.66f, h * 0.27f)
                    lineTo(w * 0.6f, h * 0.33f); lineTo(w * 0.54f, h * 0.26f); lineTo(w * 0.48f, h * 0.29f); close()
                }
                drawPath(snow, Palette.white.copy(alpha = 0.9f))
                // The route: a zig-zag from base to summit.
                val route = ROUTE.map { (x, y) -> Offset(w * x, h * y) }
                val path = Path().apply { route.forEachIndexed { i, o -> if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) } }
                drawPath(path, Palette.sun1.copy(alpha = 0.6f), style = Stroke(width = 3f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))
                // Camp flags.
                CAMPS.forEachIndexed { i, camp ->
                    val at = pointAlong(route, campProgress(i))
                    val reached = i <= climb.camp
                    drawLine(Palette.white, at, at + Offset(0f, -22f), strokeWidth = 2f)
                    drawRect(if (reached) Palette.sun1 else Palette.inkFaint, at + Offset(0f, -22f), GeoSize(14f, 9f))
                }
                // Yeti-1, climbing.
                val px = 3f
                val me = pointAlong(route, climb.progress)
                val frame = if (step < 0.5f) YetiSprites.walk1 else YetiSprites.walk2
                val origin = me + Offset(-8 * px, -16 * px)
                drawSprite(frame, px, origin)
                vm.outfit?.let { drawOutfit(it, frame, px, origin, mirror = false) }
            }
        }
        Column(
            Modifier.weight(1f).fillMaxHeight().neonPanel(Radius.PANEL, fill = Palette.glassStrong).padding(Space.SECTION.u),
        ) {
            Txt("ALTITUDE", Type.eyebrow, Palette.cyanSoft, glow = Palette.cyan)
            Txt("${"%,d".format(climb.metres)} m", Type.stat, Palette.sun1, glow = Palette.sun2)
            Txt(CAMPS[climb.camp].name.uppercase(), Type.screenTitleSmall, Palette.ink)
            Spacer(Modifier.height(Space.SM.u))
            Txt(
                climb.toNext?.let { "$it climb points to ${CAMPS[climb.camp + 1].name}" } ?: "You stood on top of the world.",
                Type.label, Palette.inkSecondary, maxLines = 2,
            )
            Spacer(Modifier.height(Space.MD.u))
            CAMPS.forEachIndexed { i, camp ->
                Row(Modifier.fillMaxWidth().padding(vertical = Space.XS.u), verticalAlignment = Alignment.CenterVertically) {
                    Txt(if (i <= climb.camp) "▲" else "△", Type.hint, if (i <= climb.camp) Palette.sun1 else Palette.inkFaint, Modifier.width(Space.SECTION.u))
                    Txt(camp.name, Type.labelStrong, if (i <= climb.camp) Palette.ink else Palette.inkSecondary, Modifier.weight(1f))
                    Txt(camp.rewardTitle ?: "${"%,d".format(camp.metres)} m", Type.label, Palette.inkSecondary, maxLines = 1)
                }
            }
            Spacer(Modifier.weight(1f))
            Txt("Climb by playing: every credit earned, +5 per game beaten, +10 per 100%.", Type.caption, Palette.inkFaint, maxLines = 2)
        }
    }
}

/** The route up the mountain, in 0..1 of the drawing. */
private val ROUTE = listOf(
    0.2f to 0.97f, 0.42f to 0.86f, 0.3f to 0.74f, 0.52f to 0.62f, 0.42f to 0.5f, 0.58f to 0.4f, 0.52f to 0.28f, 0.6f to 0.08f,
)

private fun campProgress(i: Int): Float =
    (CAMPS[i].metres - CAMPS.first().metres).toFloat() / (CAMPS.last().metres - CAMPS.first().metres)

private fun pointAlong(route: List<Offset>, f: Float): Offset {
    val lengths = route.zipWithNext { a, b -> (b - a).getDistance() }
    var left = lengths.sum() * f.coerceIn(0f, 1f)
    route.zipWithNext().forEachIndexed { i, (a, b) ->
        if (left <= lengths[i]) return a + (b - a) * (left / lengths[i])
        left -= lengths[i]
    }
    return route.last()
}

// ---------------------------------------------------------------------------------------------
// Quests
// ---------------------------------------------------------------------------------------------

@Composable
private fun QuestsTab(vm: LauncherViewModel, switch: (Int) -> Unit) {
    val quests = vm.quests()
    var row by remember { mutableIntStateOf(0) }
    TabInput(vm, switch) { button ->
        when (button) {
            Pad.Up -> { row = (row - 1).coerceAtLeast(0); true }
            Pad.Down -> { row = (row + 1).coerceAtMost(quests.lastIndex); true }
            Pad.A -> { quests.getOrNull(row)?.let { vm.claimQuest(it.first) }; true }
            else -> false
        }
    }
    Column(Modifier.fillMaxSize()) {
        Txt("QUEST BOARD · ${weekKey().replace("-W", " · WEEK ")} · NEW QUESTS EVERY MONDAY", Type.eyebrow, Palette.cyanSoft, glow = Palette.cyan)
        Spacer(Modifier.height(Space.LG.u))
        quests.forEachIndexed { i, (quest, progress) ->
            val claimed = vm.questClaimed(quest)
            val done = progress >= quest.goal
            val focused = i == row
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = Space.SM.u)
                    .neonPanel(Radius.CARD, edge = if (focused) Palette.pink else Palette.glassEdge, fill = Palette.glassStrong)
                    .tap { row = i; vm.claimQuest(quest) }
                    .padding(Space.LG.u),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.LG.u),
            ) {
                Txt(if (claimed) "✓" else if (done) "!" else "◆", Type.screenTitle, if (done) Palette.sun1 else Palette.inkFaint, glow = if (done) Palette.sun2 else null)
                Column(Modifier.weight(1f)) {
                    Txt(quest.title, Type.labelStrong, Palette.ink)
                    Spacer(Modifier.height(Space.SM.u))
                    ProgressBar(progress.toFloat() / quest.goal)
                }
                Txt("$progress/${quest.goal}", Type.crt, Palette.cyanSoft)
                Txt(
                    when {
                        claimed -> "CLAIMED"
                        done -> "Ⓐ +${quest.reward}"
                        else -> "+${quest.reward}"
                    },
                    Type.hint, if (done && !claimed) Palette.sun1 else Palette.inkSecondary,
                )
            }
        }
    }
}

@Composable
private fun ProgressBar(value: Float) {
    Canvas(Modifier.fillMaxWidth().height(Space.GAP.u)) {
        drawRect(Palette.surface)
        drawRect(Brush.horizontalGradient(listOf(Palette.pink, Palette.sun1)), size = GeoSize(size.width * value.coerceIn(0f, 1f), size.height))
    }
}

// ---------------------------------------------------------------------------------------------
// Wardrobe
// ---------------------------------------------------------------------------------------------

@Composable
private fun WardrobeTab(vm: LauncherViewModel, switch: (Int) -> Unit) {
    val outfits = Outfit.entries
    var index by remember { mutableIntStateOf(0) }
    val cols = 5
    TabInput(vm, switch) { button ->
        when (button) {
            Pad.Left -> { index = (index - 1).coerceAtLeast(0); true }
            Pad.Right -> { index = (index + 1).coerceAtMost(outfits.lastIndex); true }
            Pad.Up -> { index = (index - cols).coerceAtLeast(0); true }
            Pad.Down -> { index = (index + cols).coerceAtMost(outfits.lastIndex); true }
            Pad.A -> { vm.pickOutfit(outfits[index]); true }
            else -> false
        }
    }
    Column(Modifier.fillMaxSize()) {
        Txt("YETI-1'S WARDROBE · CREDITS ${vm.credits()}", Type.eyebrow, Palette.cyanSoft, glow = Palette.cyan)
        Spacer(Modifier.height(Space.LG.u))
        outfits.chunked(cols).forEachIndexed { r, line ->
            Row(Modifier.fillMaxWidth().padding(vertical = Space.SM.u), horizontalArrangement = Arrangement.spacedBy(Space.LG.u)) {
                line.forEachIndexed { c, outfit ->
                    val i = r * cols + c
                    val owned = vm.ownsOutfit(outfit)
                    val wearing = vm.outfit == outfit
                    Column(
                        Modifier
                            .weight(1f)
                            .neonPanel(Radius.CARD, edge = if (i == index) Palette.pink else Palette.glassEdge, fill = Palette.glassStrong)
                            .tap { index = i; vm.pickOutfit(outfit) }
                            .padding(Space.MD.u),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Canvas(Modifier.size(Size.WARDROBE_YETI.u)) {
                            val px = size.width / 20f
                            val origin = Offset(px * 2, px * 4)
                            drawSprite(YetiSprites.walk1, px, origin)
                            drawOutfit(outfit, YetiSprites.walk1, px, origin, mirror = false)
                        }
                        Txt(outfit.title, Type.labelStrong, if (owned) Palette.ink else Palette.inkSecondary, maxLines = 1)
                        Txt(
                            when {
                                wearing -> "WEARING"
                                owned -> "OWNED"
                                outfit.cost != null -> "${outfit.cost} CREDITS"
                                outfit.season != null -> outfit.season.title.uppercase()
                                else -> "SUMMIT REWARD"
                            },
                            Type.badge, if (wearing) Palette.sun1 else Palette.inkSecondary,
                        )
                    }
                }
                repeat(cols - line.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Memory card
// ---------------------------------------------------------------------------------------------

@Composable
private fun MemoryCardTab(vm: LauncherViewModel, switch: (Int) -> Unit) {
    val saves = remember { vm.memoryCard() }
    var index by remember { mutableIntStateOf(0) }
    val cols = 8
    val float by rememberInfiniteTransition(label = "icons").animateFloat(
        0f, 1f, infiniteRepeatable(tween(MEMORY_SPIN_MS, easing = LinearEasing)), label = "icon-spin",
    )
    TabInput(vm, switch) { button ->
        when (button) {
            Pad.Left -> { index = (index - 1).coerceAtLeast(0); true }
            Pad.Right -> { index = (index + 1).coerceAtMost((saves.size - 1).coerceAtLeast(0)); true }
            Pad.Up -> { index = (index - cols).coerceAtLeast(0); true }
            Pad.Down -> { index = (index + cols).coerceAtMost((saves.size - 1).coerceAtLeast(0)); true }
            Pad.A -> { saves.getOrNull(index)?.let { (s, g) -> vm.push(Screen.Details(s.name, g.id)) }; true }
            else -> false
        }
    }
    Column(
        Modifier.fillMaxSize().clip(rounded(Radius.PANEL))
            .background(Brush.verticalGradient(listOf(Color(0xFF0A1B4D), Color(0xFF02061A))))
            .padding(Space.SECTION.u),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Txt("MEMORY CARD (8MB) · SLOT 1", Type.hint, Color(0xFFB8C8FF), Modifier.weight(1f))
            Txt("BLOCKS USED ${saves.size}", Type.crt, Color(0xFFB8C8FF))
        }
        Spacer(Modifier.height(Space.LG.u))
        if (saves.isEmpty()) {
            Txt("No data. Play a game to save your first block.", Type.body, Palette.inkSecondary)
            return@Column
        }
        val start = (index / cols - 1).coerceAtLeast(0) * cols
        saves.drop(start).take(cols * 3).chunked(cols).forEachIndexed { r, line ->
            Row(Modifier.fillMaxWidth().padding(vertical = Space.SM.u), horizontalArrangement = Arrangement.spacedBy(Space.MD.u)) {
                line.forEachIndexed { c, (_, game) ->
                    val i = start + r * cols + c
                    val selected = i == index
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .border(Size.NEON_EDGE.u, if (selected) Color(0xFF8CBEFF) else Color(0x332A50FF), rounded(Radius.CARD_SMALL))
                            .tap { index = i },
                        contentAlignment = Alignment.Center,
                    ) {
                        // The selected save's icon turns slowly, like the PS2 browser; the rest bob.
                        val phase = (float + i * 0.13f) % 1f
                        ArtImage(
                            vm.boxArt(game),
                            Modifier.fillMaxSize(0.8f).graphicsLayer {
                                rotationY = if (selected) phase * 360f else sin(phase * 6.283f) * 12f
                                translationY = sin(phase * 6.283f) * 4f
                                cameraDistance = 12f * density
                            },
                            ContentScale.Fit,
                        )
                    }
                }
                repeat(cols - line.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Spacer(Modifier.weight(1f))
        saves.getOrNull(index)?.let { (system, game) ->
            val s = vm.stats(game)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Txt(game.title, Type.labelStrong, Palette.white)
                    Txt(system.info.displayName, Type.label, Color(0xFFB8C8FF))
                }
                Txt(formatPlaytime(s.playtimeMs), Type.crt, Color(0xFFB8C8FF))
            }
        }
    }
}

private const val MEMORY_SPIN_MS = 6_000

// ---------------------------------------------------------------------------------------------
// Game Genie
// ---------------------------------------------------------------------------------------------

@Composable
private fun CodesTab(vm: LauncherViewModel, switch: (Int) -> Unit) {
    var code by remember { mutableStateOf("") }
    var cursor by remember { mutableIntStateOf(0) }
    val cols = 8
    fun enter() {
        if (code.length == 6 || code.length == 8) {
            vm.enterCode(code)
            code = ""
        } else {
            vm.sound(UiSound.Error)
        }
    }
    TabInput(vm, switch) { button ->
        when (button) {
            Pad.Left -> { cursor = (cursor - 1).mod(GENIE_LETTERS.length); true }
            Pad.Right -> { cursor = (cursor + 1).mod(GENIE_LETTERS.length); true }
            Pad.Up, Pad.Down -> { cursor = (cursor + cols).mod(GENIE_LETTERS.length); true }
            Pad.A -> { if (code.length < 8) code += GENIE_LETTERS[cursor]; true }
            Pad.X -> { code = code.dropLast(1); true }
            Pad.Start, Pad.Y -> { enter(); true }
            else -> false
        }
    }
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(Space.SECTION.u)) {
        Column(
            Modifier.weight(1.3f).fillMaxHeight().clip(rounded(Radius.PANEL)).background(Color(0xFFE8E2D0)).padding(Space.SECTION.u),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Txt("GAME GENIE", Type.screenTitle, Color(0xFFC8102E))
            Txt("VIDEO GAME ENHANCER · CODE SHEET", Type.badge, Color(0xFF5A5040))
            Spacer(Modifier.height(Space.LG.u))
            GENIE_LETTERS.chunked(cols).forEachIndexed { r, line ->
                Row(horizontalArrangement = Arrangement.spacedBy(Space.SM.u)) {
                    line.forEachIndexed { c, letter ->
                        val i = r * cols + c
                        Box(
                            Modifier
                                .size(Size.GENIE_KEY.u)
                                .clip(rounded(Radius.CARD_SMALL))
                                .background(if (i == cursor) Color(0xFFC8102E) else Color(0xFF2B2B2B))
                                .tap { cursor = i; if (code.length < 8) code += letter },
                            contentAlignment = Alignment.Center,
                        ) {
                            Txt(letter.toString(), Type.screenTitle, Palette.white)
                        }
                    }
                }
                Spacer(Modifier.height(Space.SM.u))
            }
            Spacer(Modifier.height(Space.LG.u))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.SM.u)) {
                repeat(8) { i ->
                    Box(
                        Modifier.size(Size.GENIE_SLOT_W.u, Size.GENIE_KEY.u).border(Size.NEON_EDGE.u, Color(0xFF2B2B2B), rounded(Radius.CARD_SMALL)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Txt(code.getOrNull(i)?.toString() ?: "", Type.screenTitle, Color(0xFF2B2B2B))
                    }
                }
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight().neonPanel(Radius.PANEL, fill = Palette.glassStrong).padding(Space.SECTION.u)) {
            Txt("CODES FOUND ${vm.codesFound()}/${GenieCode.entries.size}", Type.eyebrow, Palette.cyanSoft, glow = Palette.cyan)
            Spacer(Modifier.height(Space.MD.u))
            Txt("Enter a 6 or 8 letter code, then press Start. Codes hide in the end credits, the history facts and the old magazines.", Type.bodySmall, Palette.inkBody, maxLines = 5)
            Spacer(Modifier.height(Space.MD.u))
            GenieCode.entries.filter { vm.codeFound(it) }.forEach { Txt("${it.code} · ${it.title}", Type.label, Palette.sun1) }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Magazine
// ---------------------------------------------------------------------------------------------

@Composable
private fun MagazineTab(vm: LauncherViewModel, switch: (Int) -> Unit) {
    var wrapped by remember { mutableStateOf(false) }
    TabInput(vm, switch) { button ->
        when (button) {
            Pad.Left, Pad.Right, Pad.X -> { wrapped = !wrapped; true }
            else -> false
        }
    }
    val issue = if (wrapped) vm.yearIssue() else vm.monthIssue()
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(Space.SECTION.u)) {
        MagazineCover(vm, issue, wrapped, Modifier.fillMaxHeight().aspectRatio(0.75f))
        Column(Modifier.weight(1f).fillMaxHeight().neonPanel(Radius.PANEL, fill = Palette.glassStrong).padding(Space.SECTION.u)) {
            Txt(if (wrapped) "EVEREST WRAPPED ${issue.title}" else "THIS MONTH", Type.eyebrow, Palette.cyanSoft, glow = Palette.cyan)
            Spacer(Modifier.height(Space.MD.u))
            @Composable
            fun line(label: String, value: String) = Row(Modifier.fillMaxWidth().padding(vertical = Space.XS.u)) {
                Txt(label, Type.label, Palette.inkSecondary, Modifier.weight(1f))
                Txt(value, Type.labelStrong, Palette.ink)
            }
            line("Time played", hoursMinutes(issue.minutes))
            line("Sessions", issue.sessions.toString())
            line("Games played", issue.games.toString())
            line("Consoles", issue.consoles.toString())
            line("Longest session", hoursMinutes(issue.longestMinutes))
            vm.gameById(issue.topGame)?.let { (_, g) -> line("Top game", g.title) }
            issue.topSystem?.let { vm.system(it) }?.let { line("Top console", it.info.displayName) }
            issue.busiestDay?.let { line("Busiest day", it) }
            if (wrapped) line("Games beaten", vm.beatenThisYear().toString())
            Spacer(Modifier.weight(1f))
            Txt("←→ switch between this month's issue and the year in review", Type.caption, Palette.inkFaint, maxLines = 2)
        }
    }
}

/** A gaming-magazine cover from your own play: masthead, cover game, and shouty headlines. */
@Composable
private fun MagazineCover(vm: LauncherViewModel, issue: Issue, wrapped: Boolean, modifier: Modifier) {
    val top = vm.gameById(issue.topGame)
    Box(modifier.clip(rounded(Radius.CARD)).background(Color(0xFF14082E))) {
        ArtImage(top?.second?.let(vm::boxArt), Modifier.fillMaxSize(), ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xCC000000), Color.Transparent, Color(0xDD000000)))))
        Column(Modifier.fillMaxSize().padding(Space.LG.u)) {
            ChromeTxt(if (wrapped) "Wrapped" else "Everest Power", Type.screenTitle)
            Txt("${issue.title.uppercase()} · \$4.95", Type.badge, Palette.ink)
            Spacer(Modifier.weight(1f))
            val headlines = buildList {
                if (issue.minutes > 0) add("${hoursMinutes(issue.minutes).uppercase()} PLAYED!")
                top?.let { add("COVER STORY: ${it.second.title.uppercase()}") }
                if (issue.consoles > 0) add("${issue.consoles} CONSOLES CONQUERED")
                if (issue.sessions == 0) add("NO PLAY YET — GO PLAY SOMETHING!")
                add("SECRET CODE INSIDE: SUNSET")
            }
            headlines.forEachIndexed { i, text ->
                Txt(
                    text, Type.hint, if (i % 2 == 0) Palette.sun1 else Palette.white,
                    Modifier.graphicsLayer { rotationZ = if (i % 2 == 0) -3f else 2f }.padding(vertical = Space.XS.u),
                    maxLines = 2, glow = Palette.black,
                )
            }
        }
    }
}

private fun hoursMinutes(minutes: Long): String =
    if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m"
