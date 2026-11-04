package com.m7mdabaza.xogame.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.m7mdabaza.xogame.presentation.R

/** The app's brand font. It covers both Latin and Arabic letters. */
val Sukar = FontFamily(Font(R.font.sukar))

private val base = Typography()

private fun TextStyle.sukar() = copy(fontFamily = Sukar)

val XoTypography = Typography(
    displayLarge = base.displayLarge.sukar(),
    displayMedium = base.displayMedium.sukar(),
    displaySmall = base.displaySmall.sukar(),
    headlineLarge = base.headlineLarge.sukar(),
    headlineMedium = base.headlineMedium.sukar(),
    headlineSmall = base.headlineSmall.sukar(),
    titleLarge = base.titleLarge.sukar(),
    titleMedium = base.titleMedium.sukar(),
    titleSmall = base.titleSmall.sukar(),
    bodyLarge = base.bodyLarge.sukar(),
    bodyMedium = base.bodyMedium.sukar(),
    bodySmall = base.bodySmall.sukar(),
    labelLarge = base.labelLarge.sukar(),
    labelMedium = base.labelMedium.sukar(),
    labelSmall = base.labelSmall.sukar(),
)
