package com.m7mdabaza.xogame.presentation.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.m7mdabaza.xogame.domain.model.BoardSize
import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.Level
import com.m7mdabaza.xogame.presentation.R
import com.m7mdabaza.xogame.presentation.ads.BannerAd
import com.m7mdabaza.xogame.presentation.components.BrandBackground
import com.m7mdabaza.xogame.presentation.components.XoLogo
import com.m7mdabaza.xogame.presentation.purchase.RemoveAdsBadge
import com.m7mdabaza.xogame.presentation.purchase.RemoveAdsDialogHost
import com.m7mdabaza.xogame.presentation.purchase.RemoveAdsViewModel
import com.m7mdabaza.xogame.presentation.reminder.AskNotificationPermissionOnce
import com.m7mdabaza.xogame.presentation.theme.XoTheme
import com.m7mdabaza.xogame.presentation.update.InAppUpdates

/** Stateful entry point used by the navigation graph. */
@Composable
fun HomeRoute(
    onStartGame: (GameMode, BoardSize, Level?) -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenChallenge: () -> Unit,
    onOpenAchievements: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    removeAdsViewModel: RemoveAdsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val removeAds by removeAdsViewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    RemoveAdsDialogHost(removeAds, removeAdsViewModel, snackbar)
    InAppUpdates(snackbar, shouldPrompt = viewModel::shouldOfferUpdate, onPrompted = viewModel::onUpdateOffered)
    // Once ever, at startup (user decision): the daily-challenge reminder needs it on Android 13+.
    AskNotificationPermissionOnce(shouldAsk = viewModel::shouldAskForNotifications, onAsked = viewModel::onNotificationsAsked)

    // Consent can change while the app is in the background (or on first launch).
    LifecycleResumeEffect(Unit) {
        viewModel.refreshConsentState()
        onPauseOrDispose { }
    }

    HomeScreen(
        state = state,
        onStartGame = { mode, boardSize, level ->
            viewModel.onGameChosen(mode, boardSize, level)
            onStartGame(mode, boardSize, level)
        },
        onOpenStats = onOpenStats,
        onOpenSettings = onOpenSettings,
        onOpenChallenge = onOpenChallenge,
        onOpenAchievements = onOpenAchievements,
        snackbar = snackbar,
        showRemoveAdsBadge = removeAds.showBadge,
        onRemoveAds = removeAdsViewModel::openDialog,
    )
}

/** @param showRemoveAdsBadge show the "No ads" badge (see `RemoveAdsUiState.showBadge`); [onRemoveAds] opens its dialog. */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onStartGame: (GameMode, BoardSize, Level?) -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenChallenge: () -> Unit = {},
    onOpenAchievements: () -> Unit = {},
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
    showRemoveAdsBadge: Boolean = false,
    onRemoveAds: () -> Unit = {},
) {
    var pickerMode by rememberSaveable { mutableStateOf<GameMode?>(null) }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        BrandBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // There is no title here, so the badge keeps its label at every font size.
                if (showRemoveAdsBadge) RemoveAdsBadge(onClick = onRemoveAds, showLabel = true)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onOpenStats) {
                    Icon(painterResource(R.drawable.ic_bar_chart), contentDescription = stringResource(R.string.stats_title))
                }
                IconButton(onClick = onOpenAchievements) {
                    Icon(painterResource(R.drawable.ic_trophy), contentDescription = stringResource(R.string.achievements_title))
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(painterResource(R.drawable.ic_settings), contentDescription = stringResource(R.string.settings_title))
                }
            }

            Spacer(Modifier.weight(1f))
            XoLogo()
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.home_tagline),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(48.dp))

            ModeButton(
                text = stringResource(R.string.home_one_player),
                icon = R.drawable.ic_person,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                onClick = { pickerMode = GameMode.VS_COMPUTER },
            )
            Spacer(Modifier.height(16.dp))
            ModeButton(
                text = stringResource(R.string.home_two_players),
                icon = R.drawable.ic_group,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                onClick = { pickerMode = GameMode.TWO_PLAYERS },
            )
            Spacer(Modifier.height(16.dp))
            ChallengeButton(solvedToday = state.challengeSolvedToday, streak = state.challengeStreak, onClick = onOpenChallenge)

            // The free space above the banner also holds messages (such as "update ready"),
            // so they never cover the ad; a tall one may grow upwards over the buttons.
            Box(Modifier.weight(1.3f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                SnackbarHost(snackbar, Modifier.wrapContentHeight(Alignment.Bottom, unbounded = true))
            }
            if (state.canRequestAds && !state.adsRemoved) {
                BannerAd(adUnitId = state.bannerAdUnitId)
            }
        }
    }

    pickerMode?.let { mode ->
        GamePickerSheet(
            mode = mode,
            initialBoardSize = state.lastBoardSize,
            initialLevel = state.lastLevel,
            onDismiss = { pickerMode = null },
            onStart = { boardSize, level ->
                pickerMode = null
                onStartGame(mode, boardSize, level)
            },
        )
    }
}

@Composable
private fun ModeButton(
    text: String,
    @DrawableRes icon: Int,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 1.dp),
        modifier = Modifier
            .widthIn(min = 260.dp)
            .heightIn(min = 64.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))
        Text(text = text, style = MaterialTheme.typography.titleLarge)
    }
}

/** Opens today's puzzle; shows whether it's solved and the current streak. */
@Composable
private fun ChallengeButton(solvedToday: Boolean, streak: Int, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        shape = CircleShape,
        // A shadow like the mode buttons, so it stands out from the pale background shapes.
        elevation = ButtonDefaults.filledTonalButtonElevation(defaultElevation = 4.dp, pressedElevation = 1.dp),
        modifier = Modifier
            .widthIn(min = 260.dp)
            .heightIn(min = 64.dp),
    ) {
        Icon(painterResource(R.drawable.ic_calendar), contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(stringResource(R.string.challenge_home_button), style = MaterialTheme.typography.titleMedium)
            val status = stringResource(if (solvedToday) R.string.challenge_home_solved else R.string.challenge_home_new)
            Text(
                if (streak > 0) stringResource(R.string.challenge_home_streak, status, streak) else status,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    XoTheme {
        HomeScreen(
            state = HomeUiState(bannerAdUnitId = "", canRequestAds = true),
            onStartGame = { _, _, _ -> },
            onOpenStats = {},
            onOpenSettings = {},
        )
    }
}
