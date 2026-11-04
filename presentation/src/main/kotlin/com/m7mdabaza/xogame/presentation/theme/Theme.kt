package com.m7mdabaza.xogame.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = BrandYellow,
    onPrimary = BrandBlack,
    primaryContainer = BrandYellowFaded,
    onPrimaryContainer = BrandBlack,
    secondary = BrandBlack,
    onSecondary = Color.White,
    // Selected segmented buttons, tonal buttons: brand tint instead of the Material default purple.
    secondaryContainer = BrandYellowFaded,
    onSecondaryContainer = BrandBlack,
    background = SurfaceWhite,
    onBackground = OnSurfaceDark,
    surface = SurfaceWhite,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceContainerLowest = SurfaceWhite,
    surfaceContainerLow = SurfaceVariantLight,
    surfaceContainer = SurfaceVariantLight,
    // Dialogs (AlertDialog uses surfaceContainerHigh).
    surfaceContainerHigh = SurfaceWhite,
    surfaceContainerHighest = SurfaceVariantLight,
)

private val DarkColors = darkColorScheme(
    primary = BrandYellow,
    onPrimary = BrandBlack,
    primaryContainer = YellowContainerDark,
    onPrimaryContainer = BrandYellowFaded,
    // "Black" buttons become light on a dark background.
    secondary = OMarkDark,
    onSecondary = BrandBlack,
    secondaryContainer = YellowContainerDark,
    onSecondaryContainer = BrandYellowFaded,
    background = BackgroundDark,
    onBackground = OnSurfaceLight,
    surface = BackgroundDark,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceContainerLowest = BackgroundDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerLowDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceVariantDark,
)

/** Colours of the X and O marks; O can't stay black on a dark background. */
@Immutable
data class XoColors(
    val x: Color,
    val o: Color,
    /** The decorative background marks on Home. */
    val fadedX: Color,
    val fadedO: Color,
)

private val LightXoColors = XoColors(x = BrandYellow, o = BrandBlack, fadedX = BrandYellowFaded, fadedO = BrandGrayFaded)
private val DarkXoColors = XoColors(x = BrandYellow, o = OMarkDark, fadedX = FadedYellowDark, fadedO = FadedGrayDark)

private val LocalXoColors = staticCompositionLocalOf { LightXoColors }

/** The brand theme. [darkTheme] follows the theme setting (system, light or dark). */
@Composable
fun XoTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalXoColors provides if (darkTheme) DarkXoColors else LightXoColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = XoTypography,
            content = content,
        )
    }
}

/** Access with `XoTheme.colors` inside [XoTheme]. */
object XoTheme {
    val colors: XoColors
        @Composable @ReadOnlyComposable get() = LocalXoColors.current
}
