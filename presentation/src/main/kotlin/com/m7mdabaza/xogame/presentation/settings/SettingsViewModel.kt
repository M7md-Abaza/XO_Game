package com.m7mdabaza.xogame.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m7mdabaza.xogame.domain.model.MatchLength
import com.m7mdabaza.xogame.domain.model.PurchaseResult
import com.m7mdabaza.xogame.domain.model.RestoreResult
import com.m7mdabaza.xogame.domain.model.Settings
import com.m7mdabaza.xogame.domain.model.ThemeMode
import com.m7mdabaza.xogame.domain.usecase.ObserveAdsRemovedUseCase
import com.m7mdabaza.xogame.domain.usecase.ObserveRemoveAdsOfferUseCase
import com.m7mdabaza.xogame.domain.usecase.ObserveSettingsUseCase
import com.m7mdabaza.xogame.domain.usecase.PurchaseRemoveAdsUseCase
import com.m7mdabaza.xogame.domain.usecase.RestorePurchasesUseCase
import com.m7mdabaza.xogame.domain.usecase.SimulateRemoveAdsUseCase
import com.m7mdabaza.xogame.domain.usecase.UpdateSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Results worth telling the user about, shown once as a snackbar. */
enum class SettingsMessage {
    PURCHASED, PENDING, ALREADY_OWNED, UNAVAILABLE, PURCHASE_ERROR,
    RESTORED, NOTHING_TO_RESTORE, RESTORE_ERROR,
}

data class SettingsUiState(
    val settings: Settings = Settings(),
    val language: AppLanguage = AppLanguage.SYSTEM,
    val adsRemoved: Boolean = false,
    /** The Remove-ads price, or null while unknown / not available. */
    val removeAdsPrice: String? = null,
    /** A purchase or restore is running. */
    val busy: Boolean = false,
    /** Debug builds: the "simulate Remove ads" switch is shown. */
    val canSimulatePurchase: Boolean = false,
    val message: SettingsMessage? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeSettings: ObserveSettingsUseCase,
    private val updateSettings: UpdateSettingsUseCase,
    private val languageController: AppLanguageController,
    observeAdsRemoved: ObserveAdsRemovedUseCase,
    observeRemoveAdsOffer: ObserveRemoveAdsOfferUseCase,
    private val purchaseRemoveAds: PurchaseRemoveAdsUseCase,
    private val restorePurchases: RestorePurchasesUseCase,
    private val simulateRemoveAds: SimulateRemoveAdsUseCase,
) : ViewModel() {

    /** State that lives only in this ViewModel. */
    private data class Local(val language: AppLanguage, val busy: Boolean = false, val message: SettingsMessage? = null)

    private val local = MutableStateFlow(Local(languageController.current))

    val uiState: StateFlow<SettingsUiState> = combine(
        observeSettings(),
        observeAdsRemoved(),
        observeRemoveAdsOffer(),
        local,
    ) { settings, adsRemoved, offer, local ->
        SettingsUiState(
            settings = settings,
            language = local.language,
            adsRemoved = adsRemoved,
            removeAdsPrice = offer?.formattedPrice,
            busy = local.busy,
            canSimulatePurchase = simulateRemoveAds.isAvailable,
            message = local.message,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SettingsUiState(language = local.value.language, canSimulatePurchase = simulateRemoveAds.isAvailable),
    )

    fun setSound(enabled: Boolean) = update { it.copy(soundEnabled = enabled) }

    fun setVibration(enabled: Boolean) = update { it.copy(vibrationEnabled = enabled) }

    fun setTheme(theme: ThemeMode) = update { it.copy(theme = theme) }

    fun setMatchLength(length: MatchLength) = update { it.copy(matchLength = length) }

    fun setLanguage(language: AppLanguage) {
        if (language == local.value.language) return
        local.update { it.copy(language = language) }
        languageController.set(language)
    }

    fun onRemoveAds() = runBusy {
        when (purchaseRemoveAds()) {
            PurchaseResult.PURCHASED -> SettingsMessage.PURCHASED
            PurchaseResult.PENDING -> SettingsMessage.PENDING
            PurchaseResult.CANCELLED -> null
            PurchaseResult.ALREADY_OWNED -> SettingsMessage.ALREADY_OWNED
            PurchaseResult.UNAVAILABLE -> SettingsMessage.UNAVAILABLE
            PurchaseResult.ERROR -> SettingsMessage.PURCHASE_ERROR
        }
    }

    fun onRestorePurchases() = runBusy {
        when (restorePurchases()) {
            RestoreResult.RESTORED -> SettingsMessage.RESTORED
            RestoreResult.NOTHING_TO_RESTORE -> SettingsMessage.NOTHING_TO_RESTORE
            RestoreResult.ERROR -> SettingsMessage.RESTORE_ERROR
        }
    }

    /** Debug builds only. */
    fun onSimulateRemoveAds(removed: Boolean) {
        viewModelScope.launch { simulateRemoveAds(removed) }
    }

    fun onMessageShown() {
        local.update { it.copy(message = null) }
    }

    private fun runBusy(action: suspend () -> SettingsMessage?) {
        if (local.value.busy) return
        local.update { it.copy(busy = true) }
        viewModelScope.launch {
            val message = action()
            local.update { it.copy(busy = false, message = message) }
        }
    }

    private fun update(transform: (Settings) -> Settings) {
        viewModelScope.launch { updateSettings(transform) }
    }
}
