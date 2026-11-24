package com.mimente.app.ui.glass

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import com.mimente.app.data.Prefs

/** Resortes y curvas con nombre: nada de tween(300) suelto en una pantalla. */
object Motion {
    fun <T> pop() = spring<T>(dampingRatio = 0.5f, stiffness = 520f)
    fun <T> unfold() = spring<T>(dampingRatio = 0.68f, stiffness = 380f)
    fun <T> fold() = spring<T>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 900f)
    fun <T> press() = spring<T>(dampingRatio = 0.55f, stiffness = 700f)
    fun <T> ease() = tween<T>(380, easing = FastOutSlowInEasing)
    fun <T> quick() = tween<T>(180, easing = FastOutSlowInEasing)
    val slide = spring(dampingRatio = 0.86f, stiffness = 360f, visibilityThreshold = IntOffset(1, 1))
}

enum class Feel { Tap, Tick, ToggleOn, ToggleOff, Rise, Fall, Press, Confirm, Reject }

/** Hápticos que respetan el ajuste de vibración del teléfono y el de la app. */
@Composable
fun rememberFeel(): (Feel) -> Unit {
    val view = LocalView.current
    return remember(view) {
        { feel ->
            if (Prefs.haptics) {
                val c = when (feel) {
                    Feel.Tap -> HapticFeedbackConstants.VIRTUAL_KEY
                    Feel.Tick -> HapticFeedbackConstants.CLOCK_TICK
                    Feel.Press -> HapticFeedbackConstants.LONG_PRESS
                    Feel.ToggleOn -> if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.CLOCK_TICK
                    Feel.ToggleOff -> if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.TOGGLE_OFF else HapticFeedbackConstants.CLOCK_TICK
                    Feel.Rise, Feel.Confirm -> if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY
                    Feel.Fall -> HapticFeedbackConstants.KEYBOARD_TAP
                    Feel.Reject -> if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS
                }
                view.performHapticFeedback(c)
            }
        }
    }
}

/** Cualquier cosa tocable se encoge bajo el dedo con un resorte. */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.pressable(
    pressedScale: Float = 0.92f,
    feel: Feel? = Feel.Tap,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) pressedScale else 1f, Motion.press(), label = "press")
    val haptic = rememberFeel()
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .combinedClickable(
            interactionSource = source,
            indication = null,
            onLongClick = onLongClick?.let { lc -> { haptic(Feel.Press); lc() } },
            onClick = { feel?.let(haptic); onClick() },
        )
}
