package com.m7mdabaza.xogame.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.rotate
import com.m7mdabaza.xogame.presentation.theme.BrandGrayFaded
import com.m7mdabaza.xogame.presentation.theme.BrandYellowFaded

/**
 * The faded X and O in the lower part of the screen, redrawn from the original
 * background.png so they stay sharp on every screen density.
 */
@Composable
fun BrandBackground(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        val unit = size.minDimension

        // Faded O, lower end side.
        drawInBox(center = Offset(size.width * 0.70f, size.height * 0.72f), side = unit * 0.36f) {
            drawO(BrandGrayFaded, strokeFraction = 0.12f)
        }

        // Faded, tilted X, lower start side.
        val xCenter = Offset(size.width * 0.30f, size.height * 0.80f)
        rotate(degrees = -30f, pivot = xCenter) {
            drawInBox(center = xCenter, side = unit * 0.36f) {
                drawX(BrandYellowFaded)
            }
        }
    }
}

/** Runs [block] with the drawing area resized to a [side]×[side] box around [center]. */
private fun DrawScope.drawInBox(center: Offset, side: Float, block: DrawScope.() -> Unit) {
    inset(
        left = center.x - side / 2,
        top = center.y - side / 2,
        right = size.width - (center.x + side / 2),
        bottom = size.height - (center.y + side / 2),
        block = block,
    )
}
