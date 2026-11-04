package com.m7mdabaza.xogame.domain.usecase

import com.m7mdabaza.xogame.domain.model.AssistAccess
import com.m7mdabaza.xogame.domain.model.AssistKind
import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.PurchaseResult
import com.m7mdabaza.xogame.domain.model.RemoveAdsOffer
import com.m7mdabaza.xogame.domain.model.RestoreResult
import com.m7mdabaza.xogame.domain.repository.AdPolicyRepository
import com.m7mdabaza.xogame.domain.repository.PurchaseRepository
import com.m7mdabaza.xogame.domain.repository.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Whether to show an interstitial now, between two rounds. Never after
 * Remove ads; otherwise only once at least [MIN_ROUNDS_THIS_SESSION] rounds
 * were played since the app started, at least [MIN_ROUNDS_BETWEEN] rounds
 * since the last interstitial, and at least [MIN_MILLIS_BETWEEN] passed.
 * (Consent is checked where the ad is loaded.)
 */
class ShouldShowInterstitialUseCase @Inject constructor(
    private val adPolicy: AdPolicyRepository,
    private val purchases: PurchaseRepository,
    private val time: TimeProvider,
) {
    suspend operator fun invoke(): Boolean {
        if (purchases.adsRemoved.first()) return false
        val state = adPolicy.state.first()
        val last = state.lastInterstitialAtMillis
        return state.roundsThisSession >= MIN_ROUNDS_THIS_SESSION &&
            state.roundsSinceInterstitial >= MIN_ROUNDS_BETWEEN &&
            (last == null || time.nowMillis() - last >= MIN_MILLIS_BETWEEN)
    }

    companion object {
        const val MIN_ROUNDS_THIS_SESSION = 2
        const val MIN_ROUNDS_BETWEEN = 2
        const val MIN_MILLIS_BETWEEN = 3 * 60 * 1000L
    }
}

class RecordInterstitialShownUseCase @Inject constructor(
    private val adPolicy: AdPolicyRepository,
    private val time: TimeProvider,
) {
    suspend operator fun invoke() = adPolicy.recordInterstitialShown(time.nowMillis())
}

/** Counts a finished round for the interstitial policy. */
class RecordRoundForAdsUseCase @Inject constructor(
    private val adPolicy: AdPolicyRepository,
) {
    suspend operator fun invoke() = adPolicy.recordRoundFinished()
}

/**
 * Whether a hint or an undo is free or needs a rewarded ad:
 * - undo in two-player mode is always free;
 * - Remove-ads buyers get one free hint and one free undo per round;
 * - everything else needs a rewarded ad.
 */
class GetAssistAccessUseCase @Inject constructor() {
    operator fun invoke(kind: AssistKind, mode: GameMode, adsRemoved: Boolean, freeUseTaken: Boolean): AssistAccess = when {
        kind == AssistKind.UNDO && mode == GameMode.TWO_PLAYERS -> AssistAccess.FREE
        adsRemoved && !freeUseTaken -> AssistAccess.FREE
        else -> AssistAccess.REWARDED_AD
    }
}

class ObserveAdsRemovedUseCase @Inject constructor(
    private val purchases: PurchaseRepository,
) {
    operator fun invoke(): Flow<Boolean> = purchases.adsRemoved
}

class ObserveRemoveAdsOfferUseCase @Inject constructor(
    private val purchases: PurchaseRepository,
) {
    operator fun invoke(): Flow<RemoveAdsOffer?> = purchases.removeAdsOffer
}

class PurchaseRemoveAdsUseCase @Inject constructor(
    private val purchases: PurchaseRepository,
) {
    suspend operator fun invoke(): PurchaseResult = purchases.purchaseRemoveAds()
}

class RestorePurchasesUseCase @Inject constructor(
    private val purchases: PurchaseRepository,
) {
    suspend operator fun invoke(): RestoreResult = purchases.restorePurchases()
}

/** Debug builds only: pretend Remove ads is bought (see [PurchaseRepository.setAdsRemovedForDebug]). */
class SimulateRemoveAdsUseCase @Inject constructor(
    private val purchases: PurchaseRepository,
) {
    val isAvailable: Boolean get() = purchases.canSimulatePurchase

    suspend operator fun invoke(removed: Boolean) = purchases.setAdsRemovedForDebug(removed)
}
