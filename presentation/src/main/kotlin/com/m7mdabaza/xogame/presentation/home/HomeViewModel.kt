package com.m7mdabaza.xogame.presentation.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.m7mdabaza.xogame.domain.model.BoardSize
import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.Level
import com.m7mdabaza.xogame.domain.usecase.EvaluateAchievementsUseCase
import com.m7mdabaza.xogame.domain.usecase.ObserveAdsRemovedUseCase
import com.m7mdabaza.xogame.domain.usecase.ObserveChallengeProgressUseCase
import com.m7mdabaza.xogame.domain.usecase.ObserveSettingsUseCase
import com.m7mdabaza.xogame.domain.usecase.RecordNotificationPermissionAskedUseCase
import com.m7mdabaza.xogame.domain.usecase.RecordUpdatePromptedUseCase
import com.m7mdabaza.xogame.domain.usecase.ShouldAskNotificationPermissionUseCase
import com.m7mdabaza.xogame.domain.usecase.ShouldPromptForUpdateUseCase
import com.m7mdabaza.xogame.domain.usecase.UpdateSettingsUseCase
import com.m7mdabaza.xogame.presentation.ads.AdUnitIds
import com.m7mdabaza.xogame.presentation.ads.ConsentManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val bannerAdUnitId: String,
    /** UMP allows ad requests (consent given, or not required in this region). */
    val canRequestAds: Boolean,
    /** Remove ads was bought: no banner. */
    val adsRemoved: Boolean = false,
    val challengeSolvedToday: Boolean = false,
    /** Current daily-challenge streak in days. */
    val challengeStreak: Int = 0,
    /** The last board and level picked against the phone, preselected next time. */
    val lastBoardSize: BoardSize = BoardSize.THREE,
    val lastLevel: Level = Level.EASY,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val application: Application,
    adUnitIds: AdUnitIds,
    observeAdsRemoved: ObserveAdsRemovedUseCase,
    observeChallengeProgress: ObserveChallengeProgressUseCase,
    evaluateAchievements: EvaluateAchievementsUseCase,
    observeSettings: ObserveSettingsUseCase,
    private val updateSettings: UpdateSettingsUseCase,
    private val shouldPromptForUpdate: ShouldPromptForUpdateUseCase,
    private val recordUpdatePrompted: RecordUpdatePromptedUseCase,
    private val shouldAskNotificationPermission: ShouldAskNotificationPermissionUseCase,
    private val recordNotificationPermissionAsked: RecordNotificationPermissionAskedUseCase,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            bannerAdUnitId = adUnitIds.homeBanner,
            canRequestAds = ConsentManager.canRequestAds(application),
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Quietly unlocks anything already earned (e.g. an achievement added in an
        // update); only unlocks that happen during play are announced.
        viewModelScope.launch { evaluateAchievements() }
        viewModelScope.launch {
            observeAdsRemoved().collect { removed -> _uiState.update { it.copy(adsRemoved = removed) } }
        }
        viewModelScope.launch {
            observeChallengeProgress().collect { p ->
                _uiState.update { it.copy(challengeSolvedToday = p.solvedToday, challengeStreak = p.currentStreak) }
            }
        }
        viewModelScope.launch {
            observeSettings().collect { s -> _uiState.update { it.copy(lastBoardSize = s.boardSize, lastLevel = s.level) } }
        }
    }

    /** A game was picked: against the phone, its board and level become next time's choice. */
    fun onGameChosen(mode: GameMode, boardSize: BoardSize, level: Level?) {
        if (mode != GameMode.VS_COMPUTER || level == null) return
        viewModelScope.launch { updateSettings { it.copy(boardSize = boardSize, level = level) } }
    }

    /** Whether a waiting Play update may be offered now (at most every few days). */
    suspend fun shouldOfferUpdate(): Boolean = shouldPromptForUpdate()

    suspend fun onUpdateOffered() = recordUpdatePrompted()

    /** Whether to ask for the notification permission now (Android 13+): once ever, for the daily-challenge reminder. */
    suspend fun shouldAskForNotifications(): Boolean = shouldAskNotificationPermission()

    suspend fun onNotificationsAsked() = recordNotificationPermissionAsked()

    /** Call when the screen resumes: consent may have changed (e.g. in Settings). */
    fun refreshConsentState() {
        _uiState.update { it.copy(canRequestAds = ConsentManager.canRequestAds(application)) }
    }
}
