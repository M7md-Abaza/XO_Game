package com.m7mdabaza.xogame.presentation.purchase

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.m7mdabaza.xogame.presentation.R
import com.m7mdabaza.xogame.presentation.theme.XoTheme
import kotlinx.coroutines.launch

/**
 * The tappable "No ads" pill for a top bar; it opens the Remove-ads dialog.
 * Without [showLabel] it is just the crossed-out "AD", and screen readers
 * still call it "Remove ads". By default the label goes at large font sizes,
 * so a title next to the badge keeps its room.
 */
@Composable
fun RemoveAdsBadge(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showLabel: Boolean = LocalDensity.current.fontScale < ICON_ONLY_FONT_SCALE,
) {
    val removeAds = stringResource(R.string.remove_ads_title)
    val background = MaterialTheme.colorScheme.primary
    val content = MaterialTheme.colorScheme.onPrimary
    Row(
        modifier
            // A 48 dp touch area around the smaller pill.
            .minimumInteractiveComponentSize()
            .clip(CircleShape)
            .background(background)
            .clickable(onClickLabel = removeAds, role = Role.Button, onClick = onClick)
            .heightIn(min = 32.dp)
            .padding(horizontal = if (showLabel) 10.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val icon = if (showLabel) Modifier else Modifier.semantics { contentDescription = removeAds }
        NoAdsIcon(color = content, gapColor = background, modifier = icon.size(20.dp))
        if (showLabel) {
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.remove_ads_badge), color = content, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

private const val ICON_ONLY_FONT_SCALE = 1.5f

/**
 * A crossed-out "AD", drawn so it keeps its size at any font scale.
 * [gapColor] is the colour behind it; it cuts the slash out of the frame.
 */
@Composable
fun NoAdsIcon(color: Color, gapColor: Color, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = size.minDimension * 0.08f
        drawRoundRect(
            color,
            topLeft = Offset(stroke / 2, h * 0.18f),
            size = Size(w - stroke, h * 0.64f),
            cornerRadius = CornerRadius(h * 0.12f),
            style = Stroke(stroke),
        )
        // The slash runs through the centre; the gap cuts only the frame, and the
        // letters are spaced so the slash passes between them.
        val start = Offset(w * 0.14f, h * 0.06f)
        val end = Offset(w * 0.86f, h * 0.94f)
        drawLine(gapColor, start, end, strokeWidth = stroke * 2.5f, cap = StrokeCap.Round)
        val text = measurer.measure(
            "AD",
            TextStyle(color = color, fontSize = (h * 0.44f).toSp(), fontWeight = FontWeight.Bold, letterSpacing = (h * 0.1f).toSp()),
        )
        drawText(text, topLeft = Offset((w - text.size.width) / 2f, (h - text.size.height) / 2f))
        drawLine(color, start, end, strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

/** What Remove ads gives, the price, Buy, Restore purchases and Not now. */
@Composable
fun RemoveAdsDialog(state: RemoveAdsUiState, onBuy: () -> Unit, onRestore: () -> Unit, onDismiss: () -> Unit) {
    val price = state.price
    val secondaryText = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                NoAdsIcon(MaterialTheme.colorScheme.onPrimary, MaterialTheme.colorScheme.primary, Modifier.size(32.dp))
            }
        },
        title = { Text(stringResource(R.string.remove_ads_title), textAlign = TextAlign.Center) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Benefit(R.string.remove_ads_benefit_banners)
                Benefit(R.string.remove_ads_benefit_interstitials)
                Benefit(R.string.remove_ads_benefit_assists)
                Text(
                    stringResource(if (price != null) R.string.remove_ads_one_time else R.string.remove_ads_not_available),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
                TextButton(onClick = onRestore, enabled = !state.busy, colors = secondaryText, modifier = Modifier.padding(top = 4.dp)) {
                    Text(stringResource(R.string.restore_purchases))
                }
            }
        },
        confirmButton = {
            Button(onClick = onBuy, enabled = price != null && !state.busy, modifier = Modifier.heightIn(min = 48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    // The label stays (hidden) under the spinner, so the buttons don't jump.
                    Text(
                        if (price != null) stringResource(R.string.remove_ads_dialog_buy, price) else stringResource(R.string.remove_ads_title),
                        modifier = Modifier.alpha(if (state.busy) 0f else 1f),
                    )
                    if (state.busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = secondaryText) { Text(stringResource(R.string.remove_ads_not_now)) }
        },
    )
}

@Composable
private fun Benefit(@StringRes text: Int) {
    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(stringResource(text), style = MaterialTheme.typography.bodyLarge)
    }
}

/** Shows each purchase / restore result once as a snackbar on [snackbar]. */
@Composable
fun RemoveAdsMessages(message: RemoveAdsMessage?, snackbar: SnackbarHostState, onShown: () -> Unit) {
    val text = message?.let { stringResource(it.textRes()) }
    // Its own scope: clearing the message restarts this effect, which must not dismiss the snackbar.
    val scope = rememberCoroutineScope()
    LaunchedEffect(message) {
        if (text != null) {
            scope.launch { snackbar.showSnackbar(text) }
            onShown()
        }
    }
}

/** The Remove-ads dialog and its result messages, for a screen that shows the "No ads" badge. */
@Composable
fun RemoveAdsDialogHost(state: RemoveAdsUiState, viewModel: RemoveAdsViewModel, snackbar: SnackbarHostState) {
    if (state.showDialog) {
        RemoveAdsDialog(state, onBuy = viewModel::buy, onRestore = viewModel::restore, onDismiss = viewModel::dismissDialog)
    }
    RemoveAdsMessages(state.message, snackbar, viewModel::onMessageShown)
}

@StringRes
private fun RemoveAdsMessage.textRes(): Int = when (this) {
    RemoveAdsMessage.PURCHASED -> R.string.remove_ads_purchased
    RemoveAdsMessage.PENDING -> R.string.remove_ads_pending
    RemoveAdsMessage.ALREADY_OWNED -> R.string.remove_ads_already_owned
    RemoveAdsMessage.UNAVAILABLE -> R.string.remove_ads_unavailable
    RemoveAdsMessage.PURCHASE_ERROR -> R.string.remove_ads_error
    RemoveAdsMessage.RESTORED -> R.string.restore_restored
    RemoveAdsMessage.NOTHING_TO_RESTORE -> R.string.restore_nothing
    RemoveAdsMessage.RESTORE_ERROR -> R.string.restore_error
}

@Preview
@Composable
private fun RemoveAdsBadgePreview() {
    XoTheme {
        Row {
            RemoveAdsBadge(onClick = {})
            RemoveAdsBadge(onClick = {}, showLabel = false)
        }
    }
}

@Preview
@Composable
private fun RemoveAdsDialogPreview() {
    XoTheme { RemoveAdsDialog(RemoveAdsUiState(price = "$1.99"), {}, {}, {}) }
}
