package com.m7mdabaza.xogame.domain.usecase

import com.m7mdabaza.xogame.domain.ai.ChallengeGenerator
import com.m7mdabaza.xogame.domain.model.Achievement
import com.m7mdabaza.xogame.domain.model.AchievementStatus
import com.m7mdabaza.xogame.domain.model.ChallengeProgress
import com.m7mdabaza.xogame.domain.model.DailyChallenge
import com.m7mdabaza.xogame.domain.model.GameConfig
import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.GameState
import com.m7mdabaza.xogame.domain.repository.AchievementsRepository
import com.m7mdabaza.xogame.domain.repository.ChallengeRepository
import com.m7mdabaza.xogame.domain.repository.StatsRepository
import com.m7mdabaza.xogame.domain.repository.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Today's puzzle (CPU work of a few milliseconds; call it off the main thread). */
class GetDailyChallengeUseCase @Inject constructor(
    private val time: TimeProvider,
) {
    operator fun invoke(): DailyChallenge = ChallengeGenerator.generate(time.todayEpochDay())
}

/**
 * The starting position of [DailyChallenge]: an 8×8 game against the
 * computer (who defends as O) with the setup moves played and X to move.
 */
class StartChallengeUseCase @Inject constructor(
    private val startGame: StartGameUseCase,
    private val playMove: PlayMoveUseCase,
) {
    operator fun invoke(challenge: DailyChallenge): GameState {
        val config = GameConfig(GameMode.VS_COMPUTER, challenge.level, humanSymbol = challenge.player, firstPlayer = challenge.player)
        return challenge.setupMoves.fold(startGame(config)) { state, cell ->
            (playMove(state, cell) as MoveResult.Played).state
        }
    }
}

class ObserveChallengeProgressUseCase @Inject constructor(
    private val repository: ChallengeRepository,
    private val time: TimeProvider,
) {
    operator fun invoke(): Flow<ChallengeProgress> =
        repository.solvedDays.map { ChallengeProgress(it, time.todayEpochDay()) }
}

class RecordChallengeSolvedUseCase @Inject constructor(
    private val repository: ChallengeRepository,
) {
    suspend operator fun invoke(epochDay: Long) = repository.markSolved(epochDay)
}

/** Every achievement with its progress and unlock time, in the enum's order. */
class ObserveAchievementsUseCase @Inject constructor(
    private val stats: StatsRepository,
    private val challenges: ChallengeRepository,
    private val achievements: AchievementsRepository,
    private val time: TimeProvider,
) {
    operator fun invoke(): Flow<List<AchievementStatus>> =
        combine(stats.stats, challenges.solvedDays, achievements.unlocked) { stats, solved, unlocked ->
            val progress = ChallengeProgress(solved, time.todayEpochDay())
            Achievement.entries.map { AchievementStatus(it, it.progress(stats, progress), unlocked[it]) }
        }
}

/**
 * Unlocks every achievement whose target is now reached and returns the
 * newly unlocked ones (to announce them). Call after a game or a challenge.
 */
class EvaluateAchievementsUseCase @Inject constructor(
    private val stats: StatsRepository,
    private val challenges: ChallengeRepository,
    private val achievements: AchievementsRepository,
    private val time: TimeProvider,
) {
    suspend operator fun invoke(): List<Achievement> {
        val progress = ChallengeProgress(challenges.solvedDays.first(), time.todayEpochDay())
        val current = stats.stats.first()
        val unlocked = achievements.unlocked.first()
        val newlyEarned = Achievement.entries.filter { it !in unlocked && it.progress(current, progress) >= it.target }
        if (newlyEarned.isNotEmpty()) achievements.unlock(newlyEarned, time.nowMillis())
        return newlyEarned
    }
}
