package com.m7mdabaza.xogame.presentation.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.m7mdabaza.xogame.domain.usecase.EvaluateAchievementsUseCase
import com.m7mdabaza.xogame.domain.usecase.ObserveAdsRemovedUseCase
import com.m7mdabaza.xogame.domain.usecase.ObserveChallengeProgressUseCase
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
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val application: Application,
    adUnitIds: AdUnitIds,
    observeAdsRemoved: ObserveAdsRemovedUseCase,
    observeChallengeProgress: ObserveChallengeProgressUseCase,
    evaluateAchievements: EvaluateAchievementsUseCase,
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
    }

    /** Call when the screen resumes: consent may have changed (e.g. in Settings). */
    fun refreshConsentState() {
        _uiState.update { it.copy(canRequestAds = ConsentManager.canRequestAds(application)) }
    }
}
