package com.m7mdabaza.xogame.presentation.settings

import androidx.activity.compose.LocalActivity
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.m7mdabaza.xogame.domain.model.MatchLength
import com.m7mdabaza.xogame.domain.model.ThemeMode
import com.m7mdabaza.xogame.presentation.R
import com.m7mdabaza.xogame.presentation.ads.ConsentManager
import com.m7mdabaza.xogame.presentation.components.ScreenTopBar
import com.m7mdabaza.xogame.presentation.components.SegmentedChoices
import com.m7mdabaza.xogame.presentation.theme.XoTheme

/** Stateful entry point used by the navigation graph. */
@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val context = LocalContext.current
    // UMP decides whether an entry point to change consent is required (EU users).
    var privacyRequired by remember { mutableStateOf(ConsentManager.isPrivacyOptionsRequired(context)) }
    LifecycleResumeEffect(Unit) {
        privacyRequired = ConsentManager.isPrivacyOptionsRequired(context)
        onPauseOrDispose { }
    }

    val snackbar = remember { SnackbarHostState() }
    val messageText = state.message?.let { stringResource(it.textRes()) }
    LaunchedEffect(state.message) {
        if (messageText != null) {
            snackbar.showSnackbar(messageText)
            viewModel.onMessageShown()
        }
    }

    SettingsScreen(
        state = state,
        showPrivacySettings = privacyRequired,
        snackbar = snackbar,
        actions = SettingsActions(
            onBack = onBack,
            onSoundChange = viewModel::setSound,
            onVibrationChange = viewModel::setVibration,
            onThemeChange = viewModel::setTheme,
            onLanguageChange = viewModel::setLanguage,
            onMatchLengthChange = viewModel::setMatchLength,
            onPrivacySettings = {
                activity?.let {
                    ConsentManager.showPrivacyOptionsForm(it) {
                        privacyRequired = ConsentManager.isPrivacyOptionsRequired(context)
                    }
                }
            },
            onRemoveAds = viewModel::onRemoveAds,
            onRestorePurchases = viewModel::onRestorePurchases,
            onSimulateRemoveAds = viewModel::onSimulateRemoveAds,
        ),
    )
}

@StringRes
private fun SettingsMessage.textRes(): Int = when (this) {
    SettingsMessage.PURCHASED -> R.string.remove_ads_purchased
    SettingsMessage.PENDING -> R.string.remove_ads_pending
    SettingsMessage.ALREADY_OWNED -> R.string.remove_ads_already_owned
    SettingsMessage.UNAVAILABLE -> R.string.remove_ads_unavailable
    SettingsMessage.PURCHASE_ERROR -> R.string.remove_ads_error
    SettingsMessage.RESTORED -> R.string.restore_restored
    SettingsMessage.NOTHING_TO_RESTORE -> R.string.restore_nothing
    SettingsMessage.RESTORE_ERROR -> R.string.restore_error
}

data class SettingsActions(
    val onBack: () -> Unit = {},
    val onSoundChange: (Boolean) -> Unit = {},
    val onVibrationChange: (Boolean) -> Unit = {},
    val onThemeChange: (ThemeMode) -> Unit = {},
    val onLanguageChange: (AppLanguage) -> Unit = {},
    val onMatchLengthChange: (MatchLength) -> Unit = {},
    val onPrivacySettings: () -> Unit = {},
    val onRemoveAds: () -> Unit = {},
    val onRestorePurchases: () -> Unit = {},
    val onSimulateRemoveAds: (Boolean) -> Unit = {},
)

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    showPrivacySettings: Boolean,
    actions: SettingsActions,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
) {
    val settings = state.settings
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .safeDrawingPadding(),
        ) {
            ScreenTopBar(stringResource(R.string.settings_title), onBack = actions.onBack)
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
            ) {
                RemoveAdsSection(state, actions)

                SectionTitle(stringResource(R.string.settings_section_feedback))
                SwitchRow(stringResource(R.string.settings_sound), settings.soundEnabled, actions.onSoundChange)
                SwitchRow(stringResource(R.string.settings_vibration), settings.vibrationEnabled, actions.onVibrationChange)

                SectionTitle(stringResource(R.string.settings_section_appearance))
                Label(stringResource(R.string.settings_theme))
                SegmentedChoices(ThemeMode.entries, settings.theme, actions.onThemeChange) { theme ->
                    Text(
                        stringResource(
                            when (theme) {
                                ThemeMode.SYSTEM -> R.string.settings_system
                                ThemeMode.LIGHT -> R.string.settings_theme_light
                                ThemeMode.DARK -> R.string.settings_theme_dark
                            },
                        ),
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.height(16.dp))
                Label(stringResource(R.string.settings_language))
                SegmentedChoices(AppLanguage.entries, state.language, actions.onLanguageChange) { language ->
                    Text(
                        stringResource(
                            when (language) {
                                AppLanguage.SYSTEM -> R.string.settings_system
                                AppLanguage.ENGLISH -> R.string.language_english
                                AppLanguage.ARABIC -> R.string.language_arabic
                            },
                        ),
                        maxLines = 1,
                    )
                }

                SectionTitle(stringResource(R.string.settings_section_game))
                Label(stringResource(R.string.settings_default_match))
                SegmentedChoices(MatchLength.entries, settings.matchLength, actions.onMatchLengthChange) { length ->
                    Text(
                        if (length == MatchLength.SINGLE) {
                            stringResource(R.string.setup_match_single)
                        } else {
                            stringResource(R.string.setup_match_best_of, length.rounds)
                        },
                        maxLines = 1,
                    )
                }
                Text(
                    stringResource(R.string.settings_default_match_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )

                if (showPrivacySettings) {
                    SectionTitle(stringResource(R.string.settings_section_privacy))
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clickable(role = Role.Button, onClick = actions.onPrivacySettings)
                            .padding(vertical = 8.dp),
                    ) {
                        Text(stringResource(R.string.home_privacy_settings), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.settings_privacy_summary),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).safeDrawingPadding())
    }
}

/** The Remove-ads offer (or a thank-you once bought) and Restore purchases. */
@Composable
private fun RemoveAdsSection(state: SettingsUiState, actions: SettingsActions) {
    SectionTitle(stringResource(R.string.remove_ads_title), divider = false)
    if (state.adsRemoved) {
        Text(stringResource(R.string.remove_ads_thanks), style = MaterialTheme.typography.bodyLarge)
    } else {
        Text(
            stringResource(R.string.remove_ads_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        val price = state.removeAdsPrice
        Button(
            onClick = actions.onRemoveAds,
            enabled = price != null && !state.busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) {
            if (state.busy) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text(
                    if (price != null) stringResource(R.string.remove_ads_buy, price) else stringResource(R.string.remove_ads_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        if (price == null) {
            Text(
                stringResource(R.string.remove_ads_not_available),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
    TextButton(
        onClick = actions.onRestorePurchases,
        enabled = !state.busy,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
        modifier = Modifier.padding(top = 4.dp),
    ) { Text(stringResource(R.string.restore_purchases)) }
    if (state.canSimulatePurchase) {
        SwitchRow(stringResource(R.string.remove_ads_simulate), state.adsRemoved, actions.onSimulateRemoveAds)
    }
}

@Composable
private fun SectionTitle(text: String, divider: Boolean = true) {
    Column(Modifier.padding(top = if (divider) 24.dp else 0.dp)) {
        if (divider) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(top = 16.dp, bottom = 8.dp)
                .semantics { heading() },
        )
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
}

/** A whole-row toggle: tapping the text toggles too, and screen readers read it as one switch. */
@Composable
private fun SwitchRow(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Preview(showSystemUi = true)
@Composable
private fun SettingsScreenPreview() {
    XoTheme {
        SettingsScreen(SettingsUiState(), showPrivacySettings = true, actions = SettingsActions())
    }
}
