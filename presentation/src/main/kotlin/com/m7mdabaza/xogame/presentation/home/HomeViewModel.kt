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
    /** UMP requires an entry point to change consent (EU users). */
    val privacyOptionsRequired: Boolean,
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
            privacyOptionsRequired = ConsentManager.isPrivacyOptionsRequired(application),
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** Call after the consent form closes, or when the screen resumes. */
    fun refreshConsentState() {
        _uiState.update {
            it.copy(
                canRequestAds = ConsentManager.canRequestAds(application),
                privacyOptionsRequired = ConsentManager.isPrivacyOptionsRequired(application),
            )
        }
    }
}
