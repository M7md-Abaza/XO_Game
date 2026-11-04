package com.m7mdabaza.xogame.data.billing

import kotlinx.coroutines.flow.Flow

/** The Play Console in-app product that removes ads. */
internal const val REMOVE_ADS_PRODUCT_ID = "remove_ads"

/**
 * What [PurchaseRepositoryImpl] needs from Google Play Billing, in plain
 * Kotlin types. [PlayBillingGateway] is the real one; tests use a fake, since
 * the Billing library only works on a device with the Play Store.
 */
internal interface BillingGateway {

    /** Purchases reported after the purchase sheet closes, or when a pending payment completes. */
    val purchaseUpdates: Flow<PurchaseUpdate>

    /** Connects to Google Play if needed. False when billing isn't available. */
    suspend fun connect(): Boolean

    /** The Remove-ads offer, or null if Play doesn't know the product (or on errors). */
    suspend fun queryRemoveAdsOffer(): ProductOffer?

    /** Opens the purchase sheet for [offer] over the current activity. The outcome arrives in [purchaseUpdates]. */
    suspend fun launchPurchase(offer: ProductOffer): LaunchResult

    /** The account's current one-time purchases, or null on errors. */
    suspend fun queryOwnedPurchases(): List<OwnedPurchase>?

    /** Confirms a purchase to Google Play (unacknowledged purchases are refunded after 3 days). */
    suspend fun acknowledge(purchaseToken: String): Boolean
}

/** A store offer. [handle] is the Billing library's `ProductDetails` (null in tests). */
internal class ProductOffer(val formattedPrice: String, val handle: Any? = null)

internal data class OwnedPurchase(
    val productIds: List<String>,
    val state: State,
    val token: String,
    val acknowledged: Boolean,
) {
    enum class State { PURCHASED, PENDING, OTHER }

    val isRemoveAds: Boolean get() = REMOVE_ADS_PRODUCT_ID in productIds
}

internal sealed interface PurchaseUpdate {
    data class Purchases(val purchases: List<OwnedPurchase>) : PurchaseUpdate
    data object Cancelled : PurchaseUpdate
    data object AlreadyOwned : PurchaseUpdate
    data class Failed(val responseCode: Int) : PurchaseUpdate
}

internal enum class LaunchResult { LAUNCHED, ALREADY_OWNED, UNAVAILABLE, ERROR }
