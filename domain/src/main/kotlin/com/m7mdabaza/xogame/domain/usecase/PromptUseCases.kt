package com.m7mdabaza.xogame.domain.usecase

import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.repository.PromptsRepository
import com.m7mdabaza.xogame.domain.repository.StatsRepository
import com.m7mdabaza.xogame.domain.repository.TimeProvider
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Whether to ask for a Play review now. Call it only at a happy moment (the
 * player just beat the phone or solved the daily challenge); it then checks
 * that they have played enough and weren't asked recently. Play applies its
 * own quota on top, so a request may show nothing.
 */
class ShouldRequestReviewUseCase @Inject constructor(
    private val stats: StatsRepository,
    private val prompts: PromptsRepository,
    private val time: TimeProvider,
) {
    suspend operator fun invoke(): Boolean {
        val current = stats.stats.first()
        val played = current.played(GameMode.VS_COMPUTER) + current.played(GameMode.TWO_PLAYERS)
        if (played < MIN_GAMES) return false
        val last = prompts.reviewRequestedAt.first() ?: return true
        return time.nowMillis() - last >= MIN_MILLIS_BETWEEN
    }

    companion object {
        const val MIN_GAMES = 5 // "after the 5th completed game" (docs/UPGRADE_PLAN.md)
        const val MIN_MILLIS_BETWEEN = 90 * TimeProvider.MILLIS_PER_DAY
    }
}

class RecordReviewRequestedUseCase @Inject constructor(
    private val prompts: PromptsRepository,
    private val time: TimeProvider,
) {
    suspend operator fun invoke() = prompts.setReviewRequestedAt(time.nowMillis())
}

/** Whether to offer a waiting app update now: at most once every few days, so a "No thanks" is respected. */
class ShouldPromptForUpdateUseCase @Inject constructor(
    private val prompts: PromptsRepository,
    private val time: TimeProvider,
) {
    suspend operator fun invoke(): Boolean {
        val last = prompts.updatePromptedAt.first() ?: return true
        return time.nowMillis() - last >= MIN_MILLIS_BETWEEN
    }

    companion object {
        const val MIN_MILLIS_BETWEEN = 3 * TimeProvider.MILLIS_PER_DAY
    }
}

class RecordUpdatePromptedUseCase @Inject constructor(
    private val prompts: PromptsRepository,
    private val time: TimeProvider,
) {
    suspend operator fun invoke() = prompts.setUpdatePromptedAt(time.nowMillis())
}
