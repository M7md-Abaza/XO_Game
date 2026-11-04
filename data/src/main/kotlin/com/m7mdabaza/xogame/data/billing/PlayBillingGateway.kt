package com.m7mdabaza.xogame.data.billing

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/** [BillingGateway] on Google Play Billing Library 9. */
@Singleton
internal class PlayBillingGateway @Inject constructor(
    @ApplicationContext context: Context,
    private val activityTracker: CurrentActivityTracker,
) : BillingGateway, PurchasesUpdatedListener {

    private val updates = MutableSharedFlow<PurchaseUpdate>(extraBufferCapacity = 8)
    override val purchaseUpdates: SharedFlow<PurchaseUpdate> = updates.asSharedFlow()

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    private val connectLock = Mutex()

    override suspend fun connect(): Boolean = connectLock.withLock {
        if (client.isReady) return true
        suspendCancellableCoroutine { continuation ->
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (continuation.isActive) continuation.resume(result.responseCode == BillingResponseCode.OK)
                }

                override fun onBillingServiceDisconnected() {
                    if (continuation.isActive) continuation.resume(false)
                }
            })
        }
    }

    override suspend fun queryRemoveAdsOffer(): ProductOffer? {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(REMOVE_ADS_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                ),
            )
            .build()
        val result = client.queryProductDetails(params)
        if (result.billingResult.responseCode != BillingResponseCode.OK) return null
        val details = result.productDetailsList?.firstOrNull { it.productId == REMOVE_ADS_PRODUCT_ID } ?: return null
        val price = details.oneTimePurchaseOfferDetails?.formattedPrice ?: return null
        return ProductOffer(price, details)
    }

    override suspend fun launchPurchase(offer: ProductOffer): LaunchResult {
        val details = offer.handle as? ProductDetails ?: return LaunchResult.ERROR
        val activity = activityTracker.current ?: return LaunchResult.UNAVAILABLE
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .apply { details.oneTimePurchaseOfferDetails?.offerToken?.let(::setOfferToken) }
            .build()
        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(productParams)).build()
        val result = withContext(Dispatchers.Main) { client.launchBillingFlow(activity, params) }
        return when (result.responseCode) {
            BillingResponseCode.OK -> LaunchResult.LAUNCHED
            BillingResponseCode.ITEM_ALREADY_OWNED -> LaunchResult.ALREADY_OWNED
            BillingResponseCode.BILLING_UNAVAILABLE,
            BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingResponseCode.ITEM_UNAVAILABLE,
            BillingResponseCode.FEATURE_NOT_SUPPORTED,
            -> LaunchResult.UNAVAILABLE
            else -> LaunchResult.ERROR
        }
    }

    override suspend fun queryOwnedPurchases(): List<OwnedPurchase>? {
        val result = client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
        )
        if (result.billingResult.responseCode != BillingResponseCode.OK) return null
        return result.purchasesList.map { it.toOwned() }
    }

    override suspend fun acknowledge(purchaseToken: String): Boolean {
        val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchaseToken).build()
        return client.acknowledgePurchase(params).responseCode == BillingResponseCode.OK
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        updates.tryEmit(
            when (result.responseCode) {
                BillingResponseCode.OK -> PurchaseUpdate.Purchases(purchases.orEmpty().map { it.toOwned() })
                BillingResponseCode.USER_CANCELED -> PurchaseUpdate.Cancelled
                BillingResponseCode.ITEM_ALREADY_OWNED -> PurchaseUpdate.AlreadyOwned
                else -> PurchaseUpdate.Failed(result.responseCode)
            },
        )
    }

    private fun Purchase.toOwned() = OwnedPurchase(
        productIds = products,
        state = when (purchaseState) {
            Purchase.PurchaseState.PURCHASED -> OwnedPurchase.State.PURCHASED
            Purchase.PurchaseState.PENDING -> OwnedPurchase.State.PENDING
            else -> OwnedPurchase.State.OTHER
        },
        token = purchaseToken,
        acknowledged = isAcknowledged,
    )
}

/**
 * Remembers the resumed activity, which the Billing library needs to show
 * its purchase sheet. Registered by the Application at startup.
 */
@Singleton
class CurrentActivityTracker @Inject constructor() : Application.ActivityLifecycleCallbacks {

    @Volatile
    private var resumed: WeakReference<Activity>? = null

    val current: Activity? get() = resumed?.get()

    override fun onActivityResumed(activity: Activity) {
        resumed = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        if (resumed?.get() === activity) resumed = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
