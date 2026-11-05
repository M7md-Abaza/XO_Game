package com.m7mdabaza.xogame.presentation.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.m7mdabaza.xogame.presentation.components.TextChoices
import com.m7mdabaza.xogame.presentation.purchase.RemoveAdsMessages
import com.m7mdabaza.xogame.presentation.purchase.RemoveAdsUiState
import com.m7mdabaza.xogame.presentation.purchase.RemoveAdsViewModel
import com.m7mdabaza.xogame.presentation.reminder.hasNotificationPermission
import com.m7mdabaza.xogame.presentation.reminder.openNotificationSettings
import com.m7mdabaza.xogame.presentation.reminder.reminderNotificationsAllowed
import com.m7mdabaza.xogame.presentation.theme.XoTheme
import kotlinx.coroutines.launch

/** Stateful entry point used by the navigation graph. */
@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    removeAdsViewModel: RemoveAdsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val removeAds by removeAdsViewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val context = LocalContext.current
    // UMP decides whether an entry point to change consent is required (EU users).
    var privacyRequired by remember { mutableStateOf(ConsentManager.isPrivacyOptionsRequired(context)) }
    // The user can turn notifications on or off in the system settings at any time.
    var notificationsAllowed by remember { mutableStateOf(reminderNotificationsAllowed(context)) }
    LifecycleResumeEffect(Unit) {
        privacyRequired = ConsentManager.isPrivacyOptionsRequired(context)
        notificationsAllowed = reminderNotificationsAllowed(context)
        onPauseOrDispose { }
    }

    val snackbar = remember { SnackbarHostState() }
    RemoveAdsMessages(removeAds.message, snackbar, removeAdsViewModel::onMessageShown)

    // Reminders switched on while notifications are blocked: point to the system settings.
    val scope = rememberCoroutineScope()
    val blockedText = stringResource(R.string.settings_reminder_blocked)
    val openSettingsText = stringResource(R.string.settings_reminder_open_settings)
    fun showNotificationsBlocked() {
        scope.launch {
            val result = snackbar.showSnackbar(blockedText, actionLabel = openSettingsText, duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) openNotificationSettings(context)
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsAllowed = reminderNotificationsAllowed(context)
        if (!notificationsAllowed) showNotificationsBlocked()
    }

    SettingsScreen(
        state = state,
        removeAds = removeAds,
        showPrivacySettings = privacyRequired,
        notificationsAllowed = notificationsAllowed,
        snackbar = snackbar,
        actions = SettingsActions(
            onBack = onBack,
            onSoundChange = viewModel::setSound,
            onVibrationChange = viewModel::setVibration,
            onThemeChange = viewModel::setTheme,
            onLanguageChange = viewModel::setLanguage,
            onMatchLengthChange = viewModel::setMatchLength,
            onChallengeReminderChange = { enabled ->
                viewModel.setChallengeReminder(enabled)
                if (enabled && !notificationsAllowed) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission(context)) {
                        // Android shows its dialog, or answers "denied" at once after two refusals.
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        showNotificationsBlocked()
                    }
                }
            },
            onPrivacySettings = {
                activity?.let {
                    ConsentManager.showPrivacyOptionsForm(it) {
                        privacyRequired = ConsentManager.isPrivacyOptionsRequired(context)
                    }
                }
            },
            onRemoveAds = removeAdsViewModel::buy,
            onRestorePurchases = removeAdsViewModel::restore,
            onSimulateRemoveAds = removeAdsViewModel::simulate,
        ),
    )
}

data class SettingsActions(
    val onBack: () -> Unit = {},
    val onSoundChange: (Boolean) -> Unit = {},
    val onVibrationChange: (Boolean) -> Unit = {},
    val onThemeChange: (ThemeMode) -> Unit = {},
    val onLanguageChange: (AppLanguage) -> Unit = {},
    val onMatchLengthChange: (MatchLength) -> Unit = {},
    val onChallengeReminderChange: (Boolean) -> Unit = {},
    val onPrivacySettings: () -> Unit = {},
    val onRemoveAds: () -> Unit = {},
    val onRestorePurchases: () -> Unit = {},
    val onSimulateRemoveAds: (Boolean) -> Unit = {},
)

/**
 * @param notificationsAllowed the system lets the reminder show (permission,
 *   app notifications, channel). The reminder switch is on only if both the
 *   setting and the system allow it, so it shows what will actually happen.
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    showPrivacySettings: Boolean,
    actions: SettingsActions,
    removeAds: RemoveAdsUiState = RemoveAdsUiState(),
    notificationsAllowed: Boolean = true,
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
                RemoveAdsSection(removeAds, actions)

                SectionTitle(stringResource(R.string.settings_section_feedback))
                SwitchRow(stringResource(R.string.settings_sound), settings.soundEnabled, actions.onSoundChange)
                SwitchRow(stringResource(R.string.settings_vibration), settings.vibrationEnabled, actions.onVibrationChange)

                SectionTitle(stringResource(R.string.settings_section_notifications))
                SwitchRow(
                    stringResource(R.string.settings_reminder),
                    settings.challengeReminder && notificationsAllowed,
                    actions.onChallengeReminderChange,
                    summary = stringResource(R.string.settings_reminder_summary),
                )

                SectionTitle(stringResource(R.string.settings_section_appearance))
                Label(stringResource(R.string.settings_theme))
                TextChoices(ThemeMode.entries, settings.theme, actions.onThemeChange) { theme ->
                    stringResource(
                        when (theme) {
                            ThemeMode.SYSTEM -> R.string.settings_system
                            ThemeMode.LIGHT -> R.string.settings_theme_light
                            ThemeMode.DARK -> R.string.settings_theme_dark
                        },
                    )
                }
                Spacer(Modifier.height(16.dp))
                Label(stringResource(R.string.settings_language))
                TextChoices(AppLanguage.entries, state.language, actions.onLanguageChange) { language ->
                    stringResource(
                        when (language) {
                            AppLanguage.SYSTEM -> R.string.settings_system
                            AppLanguage.ENGLISH -> R.string.language_english
                            AppLanguage.ARABIC -> R.string.language_arabic
                        },
                    )
                }

                SectionTitle(stringResource(R.string.settings_section_game))
                Label(stringResource(R.string.settings_default_match))
                TextChoices(MatchLength.entries, settings.matchLength, actions.onMatchLengthChange) { length ->
                    if (length == MatchLength.SINGLE) {
                        stringResource(R.string.setup_match_single)
                    } else {
                        stringResource(R.string.setup_match_best_of, length.rounds)
                    }
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
private fun RemoveAdsSection(state: RemoveAdsUiState, actions: SettingsActions) {
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
        val price = state.price
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
private fun SwitchRow(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, summary: String? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = if (summary != null) 8.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            Text(text, style = MaterialTheme.typography.titleMedium)
            if (summary != null) {
                Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
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
