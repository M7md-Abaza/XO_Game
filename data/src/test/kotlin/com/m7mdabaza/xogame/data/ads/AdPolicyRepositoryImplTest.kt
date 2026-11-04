package com.m7mdabaza.xogame.data.ads

import com.m7mdabaza.xogame.data.billing.FakeDataStore
import com.m7mdabaza.xogame.domain.model.AdPolicyState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AdPolicyRepositoryImplTest {

    @Test
    fun `counts rounds and resets the count when an interstitial is shown`() = runTest {
        val repo = AdPolicyRepositoryImpl(FakeDataStore())
        assertEquals(AdPolicyState(), repo.state.first())

        repeat(3) { repo.recordRoundFinished() }
        assertEquals(AdPolicyState(roundsSinceInterstitial = 3, roundsThisSession = 3), repo.state.first())

        repo.recordInterstitialShown(atMillis = 42)
        assertEquals(AdPolicyState(lastInterstitialAtMillis = 42, roundsSinceInterstitial = 0, roundsThisSession = 3), repo.state.first())
    }

    @Test
    fun `a new app session keeps the stored values but starts its round count at zero`() = runTest {
        val store = FakeDataStore()
        AdPolicyRepositoryImpl(store).apply {
            repeat(2) { recordRoundFinished() }
            recordInterstitialShown(atMillis = 7)
            recordRoundFinished()
        }
        // Same file, new process.
        assertEquals(
            AdPolicyState(lastInterstitialAtMillis = 7, roundsSinceInterstitial = 1, roundsThisSession = 0),
            AdPolicyRepositoryImpl(store).state.first(),
        )
    }
}
