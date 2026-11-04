package com.m7mdabaza.xogame.data.billing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.m7mdabaza.xogame.domain.model.PurchaseResult
import com.m7mdabaza.xogame.domain.model.RemoveAdsOffer
import com.m7mdabaza.xogame.domain.model.RestoreResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** An in-memory DataStore, so these tests run in virtual time. */
internal class FakeDataStore(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {
    private val state = MutableStateFlow(initial)
    private val lock = Mutex()
    override val data = state

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
        lock.withLock { transform(state.value).also { state.value = it } }
}

/** A scriptable Google Play. */
internal class FakeBillingGateway : BillingGateway {
    var available = true
    var offer: ProductOffer? = ProductOffer("$1.99")
    var owned: List<OwnedPurchase>? = emptyList()
    var launchResult = LaunchResult.LAUNCHED

    /** What Play reports after the purchase sheet; null = nothing yet (e.g. the user is still deciding). */
    var nextUpdate: PurchaseUpdate? = PurchaseUpdate.Cancelled
    val acknowledged = mutableListOf<String>()
    var launches = 0

    val updates = MutableSharedFlow<PurchaseUpdate>(extraBufferCapacity = 8)
    override val purchaseUpdates = updates

    override suspend fun connect() = available
    override suspend fun queryRemoveAdsOffer() = offer
    override suspend fun queryOwnedPurchases() = owned

    override suspend fun launchPurchase(offer: ProductOffer): LaunchResult {
        launches++
        if (launchResult == LaunchResult.LAUNCHED) nextUpdate?.let { updates.tryEmit(it) }
        return launchResult
    }

    override suspend fun acknowledge(purchaseToken: String): Boolean {
        acknowledged += purchaseToken
        return true
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PurchaseRepositoryImplTest {

    private val gateway = FakeBillingGateway()
    private val ownedKey = booleanPreferencesKey("remove_ads_owned")

    private fun purchase(state: OwnedPurchase.State, token: String = "t1", acknowledged: Boolean = false, product: String = REMOVE_ADS_PRODUCT_ID) =
        OwnedPurchase(listOf(product), state, token, acknowledged)

    private fun TestScope.repository(store: DataStore<Preferences> = FakeDataStore(), debuggable: Boolean = false) =
        PurchaseRepositoryImpl(gateway, store, debuggable, backgroundScope)

    private fun test(block: suspend TestScope.() -> Unit) = runTest(UnconfinedTestDispatcher(), testBody = block)

    // --- Startup sync ------------------------------------------------------------------

    @Test
    fun `startup loads the price and finds nothing owned`() = test {
        val repo = repository()
        assertEquals(RemoveAdsOffer("$1.99"), repo.removeAdsOffer.value)
        assertFalse(repo.adsRemoved.first())
    }

    @Test
    fun `startup restores and acknowledges an existing purchase`() = test {
        gateway.owned = listOf(purchase(OwnedPurchase.State.PURCHASED, acknowledged = false))
        val repo = repository()
        assertTrue(repo.adsRemoved.first())
        assertEquals(listOf("t1"), gateway.acknowledged)
    }

    @Test
    fun `offline the cached purchase keeps ads off`() = test {
        gateway.available = false
        val repo = repository(FakeDataStore(mutablePreferencesOf(ownedKey to true)))
        assertTrue(repo.adsRemoved.first())
        assertNull(repo.removeAdsOffer.value)
    }

    @Test
    fun `a refunded purchase turns ads back on`() = test {
        gateway.owned = emptyList()
        val repo = repository(FakeDataStore(mutablePreferencesOf(ownedKey to true)))
        assertFalse(repo.adsRemoved.first())
    }

    @Test
    fun `other products don't remove ads`() = test {
        gateway.owned = listOf(purchase(OwnedPurchase.State.PURCHASED, product = "coins"))
        assertFalse(repository().adsRemoved.first())
    }

    // --- Purchase flow -----------------------------------------------------------------

    @Test
    fun `a completed purchase removes ads and is acknowledged`() = test {
        val repo = repository()
        gateway.nextUpdate = PurchaseUpdate.Purchases(listOf(purchase(OwnedPurchase.State.PURCHASED, token = "new")))
        assertEquals(PurchaseResult.PURCHASED, repo.purchaseRemoveAds())
        assertTrue(repo.adsRemoved.first())
        assertEquals(listOf("new"), gateway.acknowledged)
    }

    @Test
    fun `cancelling keeps the ads`() = test {
        val repo = repository()
        gateway.nextUpdate = PurchaseUpdate.Cancelled
        assertEquals(PurchaseResult.CANCELLED, repo.purchaseRemoveAds())
        assertFalse(repo.adsRemoved.first())
    }

    @Test
    fun `a pending payment removes ads once Play reports it purchased`() = test {
        val repo = repository()
        gateway.nextUpdate = PurchaseUpdate.Purchases(listOf(purchase(OwnedPurchase.State.PENDING)))
        assertEquals(PurchaseResult.PENDING, repo.purchaseRemoveAds())
        assertFalse(repo.adsRemoved.first())

        gateway.updates.emit(PurchaseUpdate.Purchases(listOf(purchase(OwnedPurchase.State.PURCHASED))))
        runCurrent()
        assertTrue(repo.adsRemoved.first())
        assertEquals(listOf("t1"), gateway.acknowledged)
    }

    @Test
    fun `already owned restores the purchase`() = test {
        val repo = repository()
        gateway.launchResult = LaunchResult.ALREADY_OWNED
        gateway.owned = listOf(purchase(OwnedPurchase.State.PURCHASED, acknowledged = true))
        assertEquals(PurchaseResult.ALREADY_OWNED, repo.purchaseRemoveAds())
        assertTrue(repo.adsRemoved.first())
        assertTrue(gateway.acknowledged.isEmpty())
    }

    @Test
    fun `no Google Play or no product means unavailable`() = test {
        gateway.available = false
        assertEquals(PurchaseResult.UNAVAILABLE, repository().purchaseRemoveAds())

        gateway.available = true
        gateway.offer = null
        assertEquals(PurchaseResult.UNAVAILABLE, repository().purchaseRemoveAds())
        assertEquals(0, gateway.launches)
    }

    @Test
    fun `a purchase result is waited for while the sheet is open`() = test {
        val repo = repository()
        gateway.nextUpdate = null // the user hasn't decided yet
        val result = async { repo.purchaseRemoveAds() }
        runCurrent()
        assertFalse(result.isCompleted)
        gateway.updates.emit(PurchaseUpdate.Purchases(listOf(purchase(OwnedPurchase.State.PURCHASED))))
        assertEquals(PurchaseResult.PURCHASED, result.await())
    }

    // --- Restore -----------------------------------------------------------------------

    @Test
    fun `restore finds a purchase, nothing, or fails`() = test {
        val repo = repository()
        assertEquals(RestoreResult.NOTHING_TO_RESTORE, repo.restorePurchases())

        gateway.owned = listOf(purchase(OwnedPurchase.State.PURCHASED, acknowledged = true))
        assertEquals(RestoreResult.RESTORED, repo.restorePurchases())
        assertTrue(repo.adsRemoved.first())

        gateway.owned = null
        assertEquals(RestoreResult.ERROR, repo.restorePurchases())
        assertTrue("a failed query keeps the cache", repo.adsRemoved.first())
    }

    // --- Debug simulation ----------------------------------------------------------------

    @Test
    fun `simulating the purchase works only in debug builds`() = test {
        val release = repository(debuggable = false)
        assertFalse(release.canSimulatePurchase)
        release.setAdsRemovedForDebug(true)
        assertFalse(release.adsRemoved.first())

        val debug = repository(debuggable = true)
        assertTrue(debug.canSimulatePurchase)
        debug.setAdsRemovedForDebug(true)
        assertTrue(debug.adsRemoved.first())
        debug.setAdsRemovedForDebug(false)
        assertFalse(debug.adsRemoved.first())
    }
}
