package com.m7mdabaza.xogame.presentation.purchase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m7mdabaza.xogame.domain.model.PurchaseResult
import com.m7mdabaza.xogame.domain.model.RestoreResult
import com.m7mdabaza.xogame.domain.usecase.ObserveAdsRemovedUseCase
import com.m7mdabaza.xogame.domain.usecase.ObserveRemoveAdsOfferUseCase
import com.m7mdabaza.xogame.domain.usecase.PurchaseRemoveAdsUseCase
import com.m7mdabaza.xogame.domain.usecase.RestorePurchasesUseCase
import com.m7mdabaza.xogame.domain.usecase.SimulateRemoveAdsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Purchase and restore results worth telling the player about, shown once as a snackbar. */
enum class RemoveAdsMessage {
    PURCHASED, PENDING, ALREADY_OWNED, UNAVAILABLE, PURCHASE_ERROR,
    RESTORED, NOTHING_TO_RESTORE, RESTORE_ERROR,
}

data class RemoveAdsUiState(
    val adsRemoved: Boolean = false,
    /** The localized price, or null while unknown or when Google Play can't sell Remove ads. */
    val price: String? = null,
    /** A purchase or restore is running. */
    val busy: Boolean = false,
    /** Debug builds: the "simulate Remove ads" switch is shown (Settings). */
    val canSimulatePurchase: Boolean = false,
    /** The player opened the Remove-ads dialog (from the "No ads" badge or an ad offer). */
    val dialogOpen: Boolean = false,
    val message: RemoveAdsMessage? = null,
) {
    /** The "No ads" badge shows while ads do and Google Play can sell Remove ads. */
    val showBadge: Boolean get() = !adsRemoved && price != null

    /** The dialog also closes by itself once ads are removed, wherever that happened. */
    val showDialog: Boolean get() = dialogOpen && !adsRemoved
}

/**
 * The Remove-ads purchase, shared by the "No ads" badge and its dialog (Home,
 * game, Daily challenge) and by the Settings section.
 */
@HiltViewModel
class RemoveAdsViewModel @Inject constructor(
    observeAdsRemoved: ObserveAdsRemovedUseCase,
    observeRemoveAdsOffer: ObserveRemoveAdsOfferUseCase,
    private val purchaseRemoveAds: PurchaseRemoveAdsUseCase,
    private val restorePurchases: RestorePurchasesUseCase,
    private val simulateRemoveAds: SimulateRemoveAdsUseCase,
) : ViewModel() {

    /** State that lives only in this ViewModel. */
    private data class Local(val busy: Boolean = false, val dialogOpen: Boolean = false, val message: RemoveAdsMessage? = null)

    private val local = MutableStateFlow(Local())

    val uiState: StateFlow<RemoveAdsUiState> = combine(observeAdsRemoved(), observeRemoveAdsOffer(), local) { adsRemoved, offer, local ->
        RemoveAdsUiState(
            adsRemoved = adsRemoved,
            price = offer?.formattedPrice,
            busy = local.busy,
            canSimulatePurchase = simulateRemoveAds.isAvailable,
            dialogOpen = local.dialogOpen,
            message = local.message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RemoveAdsUiState(canSimulatePurchase = simulateRemoveAds.isAvailable))

    fun openDialog() = local.update { it.copy(dialogOpen = true) }

    fun dismissDialog() = local.update { it.copy(dialogOpen = false) }

    fun buy() = runBusy {
        when (purchaseRemoveAds()) {
            PurchaseResult.PURCHASED -> RemoveAdsMessage.PURCHASED
            PurchaseResult.PENDING -> RemoveAdsMessage.PENDING
            PurchaseResult.CANCELLED -> null
            PurchaseResult.ALREADY_OWNED -> RemoveAdsMessage.ALREADY_OWNED
            PurchaseResult.UNAVAILABLE -> RemoveAdsMessage.UNAVAILABLE
            PurchaseResult.ERROR -> RemoveAdsMessage.PURCHASE_ERROR
        }
    }

    fun restore() = runBusy {
        when (restorePurchases()) {
            RestoreResult.RESTORED -> RemoveAdsMessage.RESTORED
            RestoreResult.NOTHING_TO_RESTORE -> RemoveAdsMessage.NOTHING_TO_RESTORE
            RestoreResult.ERROR -> RemoveAdsMessage.RESTORE_ERROR
        }
    }

    /** Debug builds only. */
    fun simulate(removed: Boolean) {
        viewModelScope.launch { simulateRemoveAds(removed) }
    }

    fun onMessageShown() = local.update { it.copy(message = null) }

    /**
     * Runs a purchase or restore, one at a time. A result with a message closes
     * the dialog (the message shows on the screen behind it); a cancelled
     * purchase leaves it open, so the player can still decide.
     */
    private fun runBusy(action: suspend () -> RemoveAdsMessage?) {
        if (local.value.busy) return
        local.update { it.copy(busy = true) }
        viewModelScope.launch {
            val message = action()
            local.update { it.copy(busy = false, message = message, dialogOpen = it.dialogOpen && message == null) }
        }
    }
}
