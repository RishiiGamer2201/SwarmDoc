package com.glucodes.swarmdoc.util

import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalView

/**
 * A Modifier that plays a click sound and provides haptic feedback when tapped.
 * Uses the platform's built-in click sound effect.
 */
fun Modifier.clickWithSound(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    val view = LocalView.current
    this.clickable(
        enabled = enabled,
        interactionSource = remember { MutableInteractionSource() },
        indication = null, // use ripple from parent if present
    ) {
        view.playSoundEffect(SoundEffectConstants.CLICK)
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        onClick()
    }
}

/**
 * Play a click sound on the current view.
 * Call this inside a Composable scope before delegating to onClick.
 */
fun android.view.View.playClickSound() {
    playSoundEffect(SoundEffectConstants.CLICK)
    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
}
