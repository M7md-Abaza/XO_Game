package com.m7mdabaza.xogame.domain.repository

import com.m7mdabaza.xogame.domain.model.AdPolicyState
import com.m7mdabaza.xogame.domain.model.PurchaseResult
import com.m7mdabaza.xogame.domain.model.RemoveAdsOffer
import com.m7mdabaza.xogame.domain.model.RestoreResult
import kotlinx.coroutines.flow.Flow
import java.util.TimeZone

/** The current time; an interface so time-based rules can be tested. */
fun interface TimeProvider {
    fun nowMillis(): Long

    /**
     * Today's date in the device's time zone, as days since 1970-01-01.
     * (java.util, not java.time: java.time needs Android 8 without desugaring.)
     */
    fun todayEpochDay(): Long {
        val now = nowMillis()
        return Math.floorDiv(now + TimeZone.getDefault().getOffset(now), MILLIS_PER_DAY)
    }

    companion object {
        const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L
    }
}

interface AdPolicyRepository {
    val state: Flow<AdPolicyState>

    suspend fun recordRoundFinished()

    suspend fun recordInterstitialShown(atMillis: Long)
}

/** The one-time "Remove ads" purchase. */
interface PurchaseRepository {
    /** True once Remove ads is bought. Cached, so it also works offline. */
    val adsRemoved: Flow<Boolean>

    /** The store offer, or null while unknown or when the product can't be bought. */
    val removeAdsOffer: Flow<RemoveAdsOffer?>

    /** Opens the Google Play purchase sheet and waits for the result. */
    suspend fun purchaseRemoveAds(): PurchaseResult

    /** Asks Google Play for past purchases (e.g. after reinstalling or on a new phone). */
    suspend fun restorePurchases(): RestoreResult

    /** True in debug builds, where [setAdsRemovedForDebug] works. */
    val canSimulatePurchase: Boolean

    /** Debug builds only (does nothing in release): pretend Remove ads is bought, to test the ad-free flows. */
    suspend fun setAdsRemovedForDebug(removed: Boolean)
}
