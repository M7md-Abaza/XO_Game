package com.m7mdabaza.xogame.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.m7mdabaza.xogame.presentation.R
import com.m7mdabaza.xogame.presentation.theme.XoTheme

/**
 * Draws an X filling [size] (0..1 of [progress] draws it partially, used for
 * the placement animation on the board).
 */
fun DrawScope.drawX(color: Color, progress: Float = 1f, strokeFraction: Float = 0.2f) {
    val stroke = size.minDimension * strokeFraction
    val inset = stroke / 2
    val first = (progress * 2).coerceIn(0f, 1f)
    val second = (progress * 2 - 1).coerceIn(0f, 1f)
    val start1 = Offset(inset, inset)
    val end1 = Offset(size.width - inset, size.height - inset)
    drawLine(color, start1, lerp(start1, end1, first), stroke, StrokeCap.Round)
    if (second > 0f) {
        val start2 = Offset(size.width - inset, inset)
        val end2 = Offset(inset, size.height - inset)
        drawLine(color, start2, lerp(start2, end2, second), stroke, StrokeCap.Round)
    }
}

/** Draws an O ring filling [size]; [progress] sweeps it in for the animation. */
fun DrawScope.drawO(color: Color, progress: Float = 1f, strokeFraction: Float = 0.16f) {
    val stroke = size.minDimension * strokeFraction
    val diameter = size.minDimension - stroke
    val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
    drawArc(
        color = color,
        startAngle = -90f,
        sweepAngle = 360f * progress.coerceIn(0f, 1f),
        useCenter = false,
        topLeft = topLeft,
        size = Size(diameter, diameter),
        style = Stroke(width = stroke, cap = StrokeCap.Round),
    )
}

private fun lerp(a: Offset, b: Offset, t: Float) = Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)

@Composable
fun XMark(modifier: Modifier = Modifier, color: Color = XoTheme.colors.x) {
    Canvas(modifier) { drawX(color) }
}

@Composable
fun OMark(modifier: Modifier = Modifier, color: Color = XoTheme.colors.o) {
    Canvas(modifier) { drawO(color) }
}

/** The "XO" brand logo. Always left-to-right, even in Arabic. */
@Composable
fun XoLogo(modifier: Modifier = Modifier, markSize: Dp = 96.dp) {
    val description = stringResource(R.string.cd_logo)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = modifier.semantics { contentDescription = description },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            XMark(Modifier.size(markSize))
            Spacer(Modifier.width(markSize / 6))
            OMark(Modifier.size(markSize * 0.95f))
        }
    }
}

@Preview
@Composable
private fun XoLogoPreview() {
    XoLogo()
}
