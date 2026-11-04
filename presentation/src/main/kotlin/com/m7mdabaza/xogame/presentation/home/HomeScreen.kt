package com.m7mdabaza.xogame.presentation.home

import androidx.activity.compose.LocalActivity
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.Level
import com.m7mdabaza.xogame.presentation.R
import com.m7mdabaza.xogame.presentation.ads.BannerAd
import com.m7mdabaza.xogame.presentation.ads.ConsentManager
import com.m7mdabaza.xogame.presentation.components.BrandBackground
import com.m7mdabaza.xogame.presentation.components.XoLogo
import com.m7mdabaza.xogame.presentation.theme.XoTheme

/** Stateful entry point used by the navigation graph. */
@Composable
fun HomeRoute(
    onStartGame: (GameMode, Level) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current

    // Consent can change while the app is in the background (or on first launch).
    LifecycleResumeEffect(Unit) {
        viewModel.refreshConsentState()
        onPauseOrDispose { }
    }

    HomeScreen(
        state = state,
        onStartGame = onStartGame,
        onPrivacySettings = {
            activity?.let { ConsentManager.showPrivacyOptionsForm(it) { viewModel.refreshConsentState() } }
        },
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onStartGame: (GameMode, Level) -> Unit,
    onPrivacySettings: () -> Unit,
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
                horizontalArrangement = Arrangement.End,
            ) {
                if (state.privacyOptionsRequired) {
                    TextButton(onClick = onPrivacySettings) {
                        Text(
                            text = stringResource(R.string.home_privacy_settings),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
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

            Spacer(Modifier.weight(1.3f))
            if (state.canRequestAds) {
                BannerAd(adUnitId = state.bannerAdUnitId)
            }
        }
    }

    pickerMode?.let { mode ->
        LevelPickerSheet(
            mode = mode,
            onDismiss = { pickerMode = null },
            onLevelSelected = { level ->
                pickerMode = null
                onStartGame(mode, level)
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
            .height(64.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))
        Text(text = text, style = MaterialTheme.typography.titleLarge)
    }
}

@Preview(showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    XoTheme {
        HomeScreen(
            state = HomeUiState(bannerAdUnitId = "", canRequestAds = true, privacyOptionsRequired = true),
            onStartGame = { _, _ -> },
            onPrivacySettings = {},
        )
    }
}
