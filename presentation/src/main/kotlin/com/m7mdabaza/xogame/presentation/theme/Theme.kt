package com.m7mdabaza.xogame.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dark theme + the theme setting arrive in milestone 5.
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

@Composable
fun XoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = XoTypography,
        content = content,
    )
}
