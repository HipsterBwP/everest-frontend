package io.github.hipsterbwp.everest.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import io.github.hipsterbwp.everest.ui.theme.Motion
import io.github.hipsterbwp.everest.ui.theme.Palette
import io.github.hipsterbwp.everest.ui.theme.Radius
import io.github.hipsterbwp.everest.ui.theme.Size
import io.github.hipsterbwp.everest.ui.theme.Space
import io.github.hipsterbwp.everest.ui.theme.Type
import io.github.hipsterbwp.everest.ui.theme.u
import kotlinx.coroutines.delay

/**
 * Old-TV power effect around [content]: switching off squashes the picture to a bright line, then
 * to a dot, then black; switching on runs it backwards. Nothing happens while [fx] is null.
 */
@Composable
fun CrtPower(fx: CrtFx?, content: @Composable () -> Unit) {
    // 0 = picture on, 1 = switched off.
    val off = remember { Animatable(0f) }
    LaunchedEffect(fx) {
        when (fx) {
            CrtFx.Off -> off.animateTo(1f, tween(Motion.CRT_OFF_MS, easing = FastOutLinearInEasing))
            CrtFx.On -> {
                off.snapTo(1f)
                off.animateTo(0f, tween(Motion.CRT_ON_MS, easing = LinearOutSlowInEasing))
            }
            null -> off.snapTo(0f)
        }
    }
    Box(Modifier.fillMaxSize().background(Palette.black)) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val v = off.value
                    // First the picture collapses to a line, then the line shrinks to a dot.
                    scaleY = if (v < LINE_AT) (1f - v / LINE_AT).coerceAtLeast(LINE) else LINE
                    scaleX = if (v < LINE_AT) 1f else (1f - (v - LINE_AT) / (1f - LINE_AT)).coerceAtLeast(0f)
                },
        ) {
            content()
            // The picture brightens to white as it collapses.
            Canvas(Modifier.fillMaxSize()) {
                val v = off.value
                if (v > 0f) drawRect(Palette.white.copy(alpha = (v * 1.4f).coerceAtMost(1f)))
            }
        }
    }
}

private const val LINE_AT = 0.7f
private const val LINE = 0.004f

/** Shows the oldest pixel banner at the top for a few seconds, then the next. */
@Composable
fun ToastHost(vm: LauncherViewModel) {
    val head = vm.toasts.firstOrNull()
    LaunchedEffect(head) {
        if (head == null) return@LaunchedEffect
        delay(Motion.TOAST_MS)
        vm.dismissToast()
    }
    Box(Modifier.fillMaxSize().padding(top = Space.SCREEN_TOP.u), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visible = head != null,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
        ) {
            val toast = head ?: return@AnimatedVisibility
            Row(
                Modifier
                    .widthIn(max = Size.TOAST_W.u)
                    .neonPanel(Radius.CARD, edge = Palette.sun1, corner = Palette.pink, fill = Palette.glassStrong)
                    .padding(horizontal = Space.SECTION.u, vertical = Space.LG.u),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Txt("★", Type.screenTitle, Palette.sun1, glow = Palette.sun2)
                Spacer(Modifier.width(Space.LG.u))
                Column {
                    Txt(toast.eyebrow.uppercase(), Type.hint, Palette.sun1, glow = Palette.sun2)
                    Spacer(Modifier.height(Space.SM.u))
                    Txt(toast.title, Type.labelStrong, Palette.ink, maxLines = 2)
                    toast.detail?.let { Txt(it, Type.label, Palette.inkSecondary) }
                }
            }
        }
    }
}
