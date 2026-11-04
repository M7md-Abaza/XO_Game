package com.m7mdabaza.xogame.presentation.testing

import com.m7mdabaza.xogame.domain.model.Achievement
import com.m7mdabaza.xogame.domain.model.AdPolicyState
import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.Level
import com.m7mdabaza.xogame.domain.model.LevelStats
import com.m7mdabaza.xogame.domain.model.Player
import com.m7mdabaza.xogame.domain.model.PurchaseResult
import com.m7mdabaza.xogame.domain.model.RemoveAdsOffer
import com.m7mdabaza.xogame.domain.model.RestoreResult
import com.m7mdabaza.xogame.domain.model.Settings
import com.m7mdabaza.xogame.domain.model.Stats
import com.m7mdabaza.xogame.domain.repository.AchievementsRepository
import com.m7mdabaza.xogame.domain.repository.AdPolicyRepository
import com.m7mdabaza.xogame.domain.repository.ChallengeRepository
import com.m7mdabaza.xogame.domain.repository.PurchaseRepository
import com.m7mdabaza.xogame.domain.repository.SettingsRepository
import com.m7mdabaza.xogame.domain.repository.StatsRepository
import com.m7mdabaza.xogame.domain.repository.TimeProvider
import com.m7mdabaza.xogame.presentation.ads.AdUnitIds
import com.m7mdabaza.xogame.presentation.game.GameFeedback
import com.m7mdabaza.xogame.presentation.game.RoundSound
import com.m7mdabaza.xogame.presentation.settings.AppLanguage
import com.m7mdabaza.xogame.presentation.settings.AppLanguageController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeSettingsRepository(initial: Settings = Settings()) : SettingsRepository {
    override val settings = MutableStateFlow(initial)

    override suspend fun update(transform: (Settings) -> Settings) {
        settings.update(transform)
    }
}

class FakeStatsRepository(initial: Stats = Stats()) : StatsRepository {
    override val stats = MutableStateFlow(initial)

    override suspend fun update(mode: GameMode, level: Level, transform: (LevelStats) -> LevelStats) {
        stats.update { Stats(it.levels + ((mode to level) to transform(it[mode, level]))) }
    }

    override suspend fun reset() {
        stats.value = Stats()
    }
}

class FakeLanguageController(override var current: AppLanguage = AppLanguage.SYSTEM) : AppLanguageController {
    val applied = mutableListOf<AppLanguage>()

    override fun set(language: AppLanguage) {
        applied += language
        current = language
    }
}

class FakeAdPolicyRepository : AdPolicyRepository {
    override val state = MutableStateFlow(AdPolicyState())
    var interstitialsShown = 0

    override suspend fun recordRoundFinished() {
        state.update { it.copy(roundsSinceInterstitial = it.roundsSinceInterstitial + 1, roundsThisSession = it.roundsThisSession + 1) }
    }

    override suspend fun recordInterstitialShown(atMillis: Long) {
        interstitialsShown++
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

class FakeTime(var now: Long = 10_000_000L) : TimeProvider {
    override fun nowMillis() = now
}

class FakeChallengeRepository : ChallengeRepository {
    override val solvedDays = MutableStateFlow<Set<Long>>(emptySet())

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

/** Records sounds and vibrations as readable strings. */
class FakeGameFeedback : GameFeedback {
    val events = mutableListOf<String>()

    override fun move(player: Player, byComputer: Boolean) {
        events += "move $player${if (byComputer) " computer" else ""}"
    }

    override fun roundEnded(sound: RoundSound) {
        events += "end $sound"
    }

    override fun newRound() {
        events += "new round"
    }
}

object FakeAdUnitIds : AdUnitIds {
    override val homeBanner = "home"
    override fun gameBanner(mode: GameMode, level: Level) = "banner-$mode-$level"
    override fun interstitial(mode: GameMode, level: Level) = "interstitial"
    override val rewarded = "rewarded"
}
