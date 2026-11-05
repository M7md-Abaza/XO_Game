package com.m7mdabaza.xogame.domain.testing

import com.m7mdabaza.xogame.domain.model.Achievement
import com.m7mdabaza.xogame.domain.model.AdPolicyState
import com.m7mdabaza.xogame.domain.model.LevelStats
import com.m7mdabaza.xogame.domain.model.PurchaseResult
import com.m7mdabaza.xogame.domain.model.RemoveAdsOffer
import com.m7mdabaza.xogame.domain.model.RestoreResult
import com.m7mdabaza.xogame.domain.model.Settings
import com.m7mdabaza.xogame.domain.model.Stats
import com.m7mdabaza.xogame.domain.model.StatsKey
import com.m7mdabaza.xogame.domain.repository.AchievementsRepository
import com.m7mdabaza.xogame.domain.repository.AdPolicyRepository
import com.m7mdabaza.xogame.domain.repository.ChallengeRepository
import com.m7mdabaza.xogame.domain.repository.PromptsRepository
import com.m7mdabaza.xogame.domain.repository.PurchaseRepository
import com.m7mdabaza.xogame.domain.repository.SettingsRepository
import com.m7mdabaza.xogame.domain.repository.StatsRepository
import com.m7mdabaza.xogame.domain.repository.TimeProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeStatsRepository(initial: Stats = Stats()) : StatsRepository {
    override val stats = MutableStateFlow(initial)

    override suspend fun update(key: StatsKey, transform: (LevelStats) -> LevelStats) {
        stats.update { Stats(it.records + (key to transform(it[key]))) }
    }

    override suspend fun reset() {
        stats.value = Stats()
    }
}

class FakeSettingsRepository(initial: Settings = Settings()) : SettingsRepository {
    override val settings = MutableStateFlow(initial)

    override suspend fun update(transform: (Settings) -> Settings) {
        settings.update(transform)
    }
}

class FakeAdPolicyRepository(initial: AdPolicyState = AdPolicyState()) : AdPolicyRepository {
    override val state = MutableStateFlow(initial)

    override suspend fun recordRoundFinished() {
        state.update { it.copy(roundsSinceInterstitial = it.roundsSinceInterstitial + 1, roundsThisSession = it.roundsThisSession + 1) }
    }

    override suspend fun recordInterstitialShown(atMillis: Long) {
        state.update { it.copy(lastInterstitialAtMillis = atMillis, roundsSinceInterstitial = 0) }
    }
}

class FakePurchaseRepository(adsRemoved: Boolean = false) : PurchaseRepository {
    override val adsRemoved = MutableStateFlow(adsRemoved)
    override val removeAdsOffer = MutableStateFlow<RemoveAdsOffer?>(RemoveAdsOffer("$1.99"))
    var purchaseResult = PurchaseResult.PURCHASED
    var restoreResult = RestoreResult.NOTHING_TO_RESTORE
    override val canSimulatePurchase = true

    override suspend fun purchaseRemoveAds(): PurchaseResult {
        if (purchaseResult == PurchaseResult.PURCHASED) adsRemoved.value = true
        return purchaseResult
    }

    override suspend fun restorePurchases(): RestoreResult {
        if (restoreResult == RestoreResult.RESTORED) adsRemoved.value = true
        return restoreResult
    }

    override suspend fun setAdsRemovedForDebug(removed: Boolean) {
        adsRemoved.value = removed
    }
}

/** A clock the test moves by hand. */
class FakeTime(var now: Long = 1_000_000L) : TimeProvider {
    override fun nowMillis() = now
}

class FakeChallengeRepository(initial: Set<Long> = emptySet()) : ChallengeRepository {
    override val solvedDays = MutableStateFlow(initial)

    override suspend fun markSolved(epochDay: Long) {
        solvedDays.update { it + epochDay }
    }
}

class FakeAchievementsRepository : AchievementsRepository {
    override val unlocked = MutableStateFlow<Map<Achievement, Long>>(emptyMap())

    override suspend fun unlock(achievements: Collection<Achievement>, atMillis: Long) {
        unlocked.update { current -> current + achievements.filter { it !in current }.associateWith { atMillis } }
    }
}

class FakePromptsRepository : PromptsRepository {
    override val reviewRequestedAt = MutableStateFlow<Long?>(null)
    override val updatePromptedAt = MutableStateFlow<Long?>(null)

    override suspend fun setReviewRequestedAt(millis: Long) {
        reviewRequestedAt.value = millis
    }

    override suspend fun setUpdatePromptedAt(millis: Long) {
        updatePromptedAt.value = millis
    }
}
