package com.m7mdabaza.xogame.presentation.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.m7mdabaza.xogame.presentation.ads.AdUnitIds
import com.m7mdabaza.xogame.presentation.ads.ConsentManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class HomeUiState(
    val bannerAdUnitId: String,
    /** UMP allows ad requests (consent given, or not required in this region). */
    val canRequestAds: Boolean,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val application: Application,
    adUnitIds: AdUnitIds,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            bannerAdUnitId = adUnitIds.homeBanner,
            canRequestAds = ConsentManager.canRequestAds(application),
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** Call when the screen resumes: consent may have changed (e.g. in Settings). */
    fun refreshConsentState() {
        _uiState.update { it.copy(canRequestAds = ConsentManager.canRequestAds(application)) }
    }
}
