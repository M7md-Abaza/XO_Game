package com.m7mdabaza.xogame.domain.usecase

import com.m7mdabaza.xogame.domain.model.AdPolicyState
import com.m7mdabaza.xogame.domain.model.AssistAccess
import com.m7mdabaza.xogame.domain.model.AssistKind
import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.PurchaseResult
import com.m7mdabaza.xogame.domain.model.RestoreResult
import com.m7mdabaza.xogame.domain.testing.FakeAdPolicyRepository
import com.m7mdabaza.xogame.domain.testing.FakePurchaseRepository
import com.m7mdabaza.xogame.domain.testing.FakeTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonetizationUseCasesTest {

    private val adPolicy = FakeAdPolicyRepository()
    private val purchases = FakePurchaseRepository()
    private val time = FakeTime()

    private val shouldShow = ShouldShowInterstitialUseCase(adPolicy, purchases, time)
    private val recordRound = RecordRoundForAdsUseCase(adPolicy)
    private val recordShown = RecordInterstitialShownUseCase(adPolicy, time)

    private suspend fun rounds(count: Int) = repeat(count) { recordRound() }

    // --- Interstitial policy ---------------------------------------------------------

    @Test
    fun `never before the second round of a session`() = runTest {
        assertFalse(shouldShow())
        rounds(1)
        assertFalse(shouldShow())
        rounds(1)
        assertTrue(shouldShow())
    }

    @Test
    fun `at least two rounds and three minutes between interstitials`() = runTest {
        rounds(2)
        recordShown()
        assertEquals(0, adPolicy.state.value.roundsSinceInterstitial)

        rounds(2)
        time.now += ShouldShowInterstitialUseCase.MIN_MILLIS_BETWEEN - 1
        assertFalse("too soon", shouldShow())
        time.now += 1
        assertTrue(shouldShow())
    }

    @Test
    fun `enough time but too few rounds since the last one`() = runTest {
        rounds(2)
        recordShown()
        rounds(1)
        time.now += 10 * ShouldShowInterstitialUseCase.MIN_MILLIS_BETWEEN
        assertFalse(shouldShow())
    }

    @Test
    fun `a new session waits for two rounds even if the last ad was long ago`() = runTest {
        adPolicy.state.value = AdPolicyState(lastInterstitialAtMillis = 0, roundsSinceInterstitial = 30, roundsThisSession = 1)
        assertFalse(shouldShow())
    }

    @Test
    fun `no interstitials after Remove ads`() = runTest {
        rounds(5)
        purchases.adsRemoved.value = true
        assertFalse(shouldShow())
    }

    // --- Before the two-phone lobby ----------------------------------------------------

    private val shouldShowPreGame = ShouldShowPreGameInterstitialUseCase(adPolicy, purchases, time)

    @Test
    fun `the pre-game ad needs no rounds, only three minutes since any interstitial`() = runTest {
        assertTrue("first tap of the session", shouldShowPreGame())
        recordShown()
        time.now += ShouldShowInterstitialUseCase.MIN_MILLIS_BETWEEN - 1
        assertFalse("too soon", shouldShowPreGame())
        time.now += 1
        assertTrue(shouldShowPreGame())
    }

    @Test
    fun `the pre-game ad and the between-round ad share one clock`() = runTest {
        // A between-round ad just now: no pre-game ad on the next tap.
        rounds(2)
        recordShown()
        assertFalse(shouldShowPreGame())

        // A pre-game ad: the next between-round one waits for the time and two new rounds.
        time.now += 10 * ShouldShowInterstitialUseCase.MIN_MILLIS_BETWEEN
        assertTrue(shouldShowPreGame())
        recordShown()
        rounds(2)
        assertFalse(shouldShow())
        time.now += ShouldShowInterstitialUseCase.MIN_MILLIS_BETWEEN
        assertTrue(shouldShow())
    }

    @Test
    fun `no pre-game ad after Remove ads`() = runTest {
        purchases.adsRemoved.value = true
        assertFalse(shouldShowPreGame())
    }

    // --- Assists ---------------------------------------------------------------------

    private val access = GetAssistAccessUseCase()

    @Test
    fun `undo is always free in two-player mode`() {
        for (removed in listOf(true, false)) for (taken in listOf(true, false)) {
            assertEquals(AssistAccess.FREE, access(AssistKind.UNDO, GameMode.TWO_PLAYERS, removed, taken))
        }
    }

    @Test
    fun `without Remove ads every hint and every vs-computer undo needs an ad`() {
        assertEquals(AssistAccess.REWARDED_AD, access(AssistKind.HINT, GameMode.VS_COMPUTER, adsRemoved = false, freeUseTaken = false))
        assertEquals(AssistAccess.REWARDED_AD, access(AssistKind.HINT, GameMode.TWO_PLAYERS, adsRemoved = false, freeUseTaken = false))
        assertEquals(AssistAccess.REWARDED_AD, access(AssistKind.UNDO, GameMode.VS_COMPUTER, adsRemoved = false, freeUseTaken = false))
    }

    @Test
    fun `buyers get one free use per round, then an ad`() {
        for (kind in AssistKind.entries) {
            assertEquals(AssistAccess.FREE, access(kind, GameMode.VS_COMPUTER, adsRemoved = true, freeUseTaken = false))
            assertEquals(AssistAccess.REWARDED_AD, access(kind, GameMode.VS_COMPUTER, adsRemoved = true, freeUseTaken = true))
        }
    }

    // --- Purchases -------------------------------------------------------------------

    @Test
    fun `purchase and restore go through the repository`() = runTest {
        assertEquals(PurchaseResult.PURCHASED, PurchaseRemoveAdsUseCase(purchases)())
        assertTrue(ObserveAdsRemovedUseCase(purchases)().first())

        val other = FakePurchaseRepository().apply { restoreResult = RestoreResult.RESTORED }
        assertEquals(RestoreResult.RESTORED, RestorePurchasesUseCase(other)())
        assertTrue(other.adsRemoved.value)
    }

    @Test
    fun `debug simulation toggles ads removed`() = runTest {
        val simulate = SimulateRemoveAdsUseCase(purchases)
        assertTrue(simulate.isAvailable)
        simulate(true)
        assertTrue(purchases.adsRemoved.value)
        simulate(false)
        assertFalse(purchases.adsRemoved.value)
    }
}
