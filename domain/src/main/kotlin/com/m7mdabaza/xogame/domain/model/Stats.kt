package com.m7mdabaza.xogame.domain.model

/**
 * How a round ended for player one: the human against the computer, or X in
 * two-player mode.
 */
enum class RoundOutcome { WIN, LOSS, DRAW }

/**
 * The record for one mode and level. Against the computer [wins] and
 * [losses] are the human's; in two-player mode they are X's wins and O's wins
 * (the streaks are only shown against the computer).
 *
 * A streak counts consecutive wins; a loss or a draw ends it.
 */
data class LevelStats(
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
) {
    val played: Int get() = wins + losses + draws

    /** Wins as a share of all rounds, 0..1, or null before the first round. */
    val winRate: Float? get() = if (played == 0) null else wins.toFloat() / played

    fun record(outcome: RoundOutcome): LevelStats = when (outcome) {
        RoundOutcome.WIN -> {
            val streak = currentStreak + 1
            copy(wins = wins + 1, currentStreak = streak, bestStreak = maxOf(bestStreak, streak))
        }
        RoundOutcome.LOSS -> copy(losses = losses + 1, currentStreak = 0)
        RoundOutcome.DRAW -> copy(draws = draws + 1, currentStreak = 0)
    }
}

/** Every mode/level record. Missing entries are empty records. */
data class Stats(val levels: Map<Pair<GameMode, Level>, LevelStats> = emptyMap()) {

    operator fun get(mode: GameMode, level: Level): LevelStats = levels[mode to level] ?: LevelStats()

    /** Rounds finished in [mode] across all levels. */
    fun played(mode: GameMode): Int = Level.entries.sumOf { get(mode, it).played }
}
