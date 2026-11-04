package com.m7mdabaza.xogame.domain.usecase

import com.m7mdabaza.xogame.domain.model.GameResult
import com.m7mdabaza.xogame.domain.model.GameState
import com.m7mdabaza.xogame.domain.model.RoundOutcome
import com.m7mdabaza.xogame.domain.model.Stats
import com.m7mdabaza.xogame.domain.repository.StatsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveStatsUseCase @Inject constructor(
    private val repository: StatsRepository,
) {
    operator fun invoke(): Flow<Stats> = repository.stats
}

/**
 * Adds a finished round to the record of its mode and level, from player
 * one's point of view (`GameConfig.humanSymbol`: the human against the
 * computer, X in two-player mode), and updates the win streak.
 */
class RecordGameResultUseCase @Inject constructor(
    private val repository: StatsRepository,
) {
    suspend operator fun invoke(state: GameState) {
        val outcome = when (val result = state.result) {
            is GameResult.Win -> if (result.player == state.config.humanSymbol) RoundOutcome.WIN else RoundOutcome.LOSS
            GameResult.Draw -> RoundOutcome.DRAW
            GameResult.InProgress -> throw IllegalArgumentException("The round has not finished")
        }
        repository.update(state.config.mode, state.config.level) { it.record(outcome) }
    }
}

class ResetStatsUseCase @Inject constructor(
    private val repository: StatsRepository,
) {
    suspend operator fun invoke() = repository.reset()
}
