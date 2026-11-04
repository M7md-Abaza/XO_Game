package com.m7mdabaza.xogame.data.billing

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.m7mdabaza.xogame.data.di.PurchaseStore
import com.m7mdabaza.xogame.domain.model.PurchaseResult
import com.m7mdabaza.xogame.domain.model.RemoveAdsOffer
import com.m7mdabaza.xogame.domain.model.RestoreResult
import com.m7mdabaza.xogame.domain.repository.PurchaseRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Remove-ads purchase. Google Play is the source of truth; the result is
 * cached in DataStore so the app stays ad-free offline.
 *
 * On creation it connects, loads the price and syncs the cache with Play's
 * list of purchases (this also turns ads back on after a refund). Pending
 * payments are finished when Play reports them as purchased.
 */
@Singleton
class PurchaseRepositoryImpl internal constructor(
    private val gateway: BillingGateway,
    private val store: DataStore<Preferences>,
    private val debuggable: Boolean,
    private val scope: CoroutineScope,
) : PurchaseRepository {

    @Inject
    internal constructor(
        gateway: BillingGateway,
        @PurchaseStore store: DataStore<Preferences>,
        @ApplicationContext context: Context,
    ) : this(
        gateway = gateway,
        store = store,
        debuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    )

    private val offer = MutableStateFlow<ProductOffer?>(null)

    /** Set while [purchaseRemoveAds] waits for its own result, so the background collector leaves it alone. */
    private val purchaseFlowLock = Mutex()

    override val adsRemoved: Flow<Boolean> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[OWNED] == true || (debuggable && it[DEBUG_OVERRIDE] == true) }
        .distinctUntilChanged()

    override val removeAdsOffer: StateFlow<RemoveAdsOffer?> = offer
        .map { it?.let { o -> RemoveAdsOffer(o.formattedPrice) } }
        .stateIn(scope, SharingStarted.Eagerly, null)

    override val canSimulatePurchase: Boolean get() = debuggable

    init {
        scope.launch {
            gateway.purchaseUpdates.collect { update ->
                // Updates while a purchase sheet is open are handled by purchaseRemoveAds itself.
                if (!purchaseFlowLock.isLocked && update is PurchaseUpdate.Purchases) process(update.purchases)
            }
        }
        scope.launch { refresh() }
    }

    override suspend fun purchaseRemoveAds(): PurchaseResult = purchaseFlowLock.withLock {
        if (!gateway.connect()) return PurchaseResult.UNAVAILABLE
        val current = offer.value ?: gateway.queryRemoveAdsOffer()?.also { offer.value = it }
            ?: return PurchaseResult.UNAVAILABLE

        coroutineScope {
            // Subscribe before launching, so the result can't be missed.
            val update = async(start = CoroutineStart.UNDISPATCHED) { gateway.purchaseUpdates.first() }
            when (gateway.launchPurchase(current)) {
                LaunchResult.LAUNCHED -> Unit
                LaunchResult.ALREADY_OWNED -> {
                    update.cancel()
                    restorePurchases()
                    return@coroutineScope PurchaseResult.ALREADY_OWNED
                }
                LaunchResult.UNAVAILABLE -> {
                    update.cancel()
                    return@coroutineScope PurchaseResult.UNAVAILABLE
                }
                LaunchResult.ERROR -> {
                    update.cancel()
                    return@coroutineScope PurchaseResult.ERROR
                }
            }
            when (val result = update.await()) {
                is PurchaseUpdate.Purchases -> {
                    val removeAds = result.purchases.filter { it.isRemoveAds }
                    process(removeAds)
                    when {
                        removeAds.any { it.state == OwnedPurchase.State.PURCHASED } -> PurchaseResult.PURCHASED
                        removeAds.any { it.state == OwnedPurchase.State.PENDING } -> PurchaseResult.PENDING
                        else -> PurchaseResult.ERROR
                    }
                }
                PurchaseUpdate.Cancelled -> PurchaseResult.CANCELLED
                PurchaseUpdate.AlreadyOwned -> {
                    restorePurchases()
                    PurchaseResult.ALREADY_OWNED
                }
                is PurchaseUpdate.Failed -> PurchaseResult.ERROR
            }
        }
    }

    override suspend fun restorePurchases(): RestoreResult {
        if (!gateway.connect()) return RestoreResult.ERROR
        val purchases = gateway.queryOwnedPurchases() ?: return RestoreResult.ERROR
        val owned = sync(purchases)
        return if (owned) RestoreResult.RESTORED else RestoreResult.NOTHING_TO_RESTORE
    }

    override suspend fun setAdsRemovedForDebug(removed: Boolean) {
        if (debuggable) store.edit { it[DEBUG_OVERRIDE] = removed }
    }

    /** Price + purchase sync, best effort (no Play Store, offline, ...). */
    private suspend fun refresh() {
        if (!gateway.connect()) return
        gateway.queryRemoveAdsOffer()?.let { offer.value = it }
        gateway.queryOwnedPurchases()?.let { sync(it) }
    }

    /**
     * Makes the cache match Play's full purchase list: owned only while a
     * purchased Remove-ads purchase exists. Returns whether it is owned.
     */
    private suspend fun sync(purchases: List<OwnedPurchase>): Boolean {
        val owned = process(purchases.filter { it.isRemoveAds })
        store.edit { it[OWNED] = owned }
        return owned
    }

    /**
     * Acknowledges purchased Remove-ads purchases and caches ownership.
     * Never turns ownership off (only [sync], with the full list, may).
     */
    private suspend fun process(purchases: List<OwnedPurchase>): Boolean {
        val purchased = purchases.filter { it.isRemoveAds && it.state == OwnedPurchase.State.PURCHASED }
        purchased.filterNot { it.acknowledged }.forEach { gateway.acknowledge(it.token) }
        if (purchased.isNotEmpty()) store.edit { it[OWNED] = true }
        return purchased.isNotEmpty()
    }

    private companion object {
        val OWNED = booleanPreferencesKey("remove_ads_owned")
        val DEBUG_OVERRIDE = booleanPreferencesKey("debug_ads_removed")
    }
}
