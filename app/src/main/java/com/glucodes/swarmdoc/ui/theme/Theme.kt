package com.glucodes.swarmdoc.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Standard Light Color Scheme
private val SwarmDocLightColorScheme = lightColorScheme(
    primary = ForestGreen,
    onPrimary = White,
    primaryContainer = SageGreenLight,
    onPrimaryContainer = ForestGreen,
    secondary = TurmericGold,
    onSecondary = CharcoalBlack,
    secondaryContainer = TurmericGoldLight,
    onSecondaryContainer = CharcoalBlack,
    tertiary = SageGreen,
    onTertiary = White,
    tertiaryContainer = SageGreenLight,
    onTertiaryContainer = ForestGreen,
    error = CoralRed,
    onError = White,
    errorContainer = CoralRedLight,
    onErrorContainer = White,
    background = Parchment,
    onBackground = CharcoalBlack,
    surface = White,
    onSurface = CharcoalBlack,
    surfaceVariant = ParchmentDark,
    onSurfaceVariant = WarmGrey,
    outline = LightGrey,
    outlineVariant = ParchmentDark,
)

// Dark Color Scheme
private val SwarmDocDarkColorScheme = darkColorScheme(
    primary = TurmericGold,
    onPrimary = CharcoalBlack,
    primaryContainer = ForestGreen,
    onPrimaryContainer = TurmericGoldLight,
    secondary = TurmericGoldLight,
    onSecondary = CharcoalBlack,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = TurmericGoldLight,
    tertiary = SageGreenLight,
    onTertiary = CharcoalBlack,
    tertiaryContainer = DarkSurfaceVariant,
    onTertiaryContainer = SageGreenLight,
    error = CoralRedLight,
    onError = CharcoalBlack,
    errorContainer = CoralRed,
    onErrorContainer = White,
    background = DarkBackground,
    onBackground = Parchment,
    surface = DarkSurface,
    onSurface = Parchment,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = LightGrey,
    outline = WarmGrey,
    outlineVariant = DarkSurfaceVariant,
)

// Female Privacy Mode Light Scheme
private val PrivacyLightColorScheme = lightColorScheme(
    primary = PrivacyIndigo,
    onPrimary = White,
    primaryContainer = PrivacyLavender,
    onPrimaryContainer = PrivacyIndigo,
    secondary = PrivacyIndigoLight,
    onSecondary = White,
    secondaryContainer = PrivacyLavenderLight,
    onSecondaryContainer = PrivacyIndigo,
    tertiary = PrivacyIndigoLight,
    onTertiary = White,
    tertiaryContainer = PrivacyLavenderLight,
    onTertiaryContainer = PrivacyIndigo,
    error = CoralRed,
    onError = White,
    errorContainer = CoralRedLight,
    onErrorContainer = White,
    background = PrivacySurface,
    onBackground = CharcoalBlack,
    surface = White,
    onSurface = CharcoalBlack,
    surfaceVariant = PrivacyLavenderLight,
    onSurfaceVariant = WarmGrey,
    outline = PrivacyLavender,
    outlineVariant = PrivacyLavenderLight,
)

val LocalPrivacyMode = compositionLocalOf { mutableStateOf(false) }

@Composable
fun SwarmDocTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    privacyMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        privacyMode -> PrivacyLightColorScheme
        darkTheme -> SwarmDocDarkColorScheme
        else -> SwarmDocLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = if (privacyMode) {
                PrivacyIndigo.toArgb()
            } else if (darkTheme) {
                DarkBackground.toArgb()
            } else {
                ForestGreen.toArgb()
            }
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SwarmDocTypography,
        shapes = SwarmDocShapes,
        content = content,
    )
}
