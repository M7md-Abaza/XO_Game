package com.m7mdabaza.xogame.domain.model

/**
 * How a round ended for player one: the human against the computer, X in
 * two-player mode, this phone's player in a two-phone game.
 */
enum class RoundOutcome { WIN, LOSS, DRAW }

/**
 * One record: against the computer on one board at one level, or in
 * two-player or two-phone mode on one board ([level] null).
 */
data class StatsKey(val mode: GameMode, val boardSize: BoardSize, val level: Level? = null) {
    init {
        require((mode == GameMode.VS_COMPUTER) == (level != null)) {
            "Records against the computer have a level, the others don't (mode $mode, level $level)"
        }
    }

    companion object {
        /** Every record: each board at each level against the computer, then each board in the other modes. */
        val all: List<StatsKey> =
            BoardSize.entries.flatMap { board -> Level.entries.map { StatsKey(GameMode.VS_COMPUTER, board, it) } } +
                BoardSize.entries.map { StatsKey(GameMode.TWO_PLAYERS, it) } +
                BoardSize.entries.map { StatsKey(GameMode.TWO_PHONES, it) }
    }
}

/**
 * The record for one [StatsKey]. Against the computer [wins] and [losses]
 * are the human's, in a two-phone game this phone's player's; in two-player
 * mode they are X's wins and O's wins (and the streaks aren't shown).
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

/** Every record. Missing entries are empty records. */
data class Stats(val records: Map<StatsKey, LevelStats> = emptyMap()) {

    operator fun get(key: StatsKey): LevelStats = records[key] ?: LevelStats()

    /** The record of [boardSize] in [mode]; [level] only (and always) against the computer. */
    operator fun get(mode: GameMode, boardSize: BoardSize, level: Level? = null): LevelStats =
        get(StatsKey(mode, boardSize, level))

    /** Every record of [mode] (empty ones included). */
    fun all(mode: GameMode): List<LevelStats> = StatsKey.all.filter { it.mode == mode }.map(::get)

    /** Rounds finished in [mode] on every board and level. */
    fun played(mode: GameMode): Int = all(mode).sumOf { it.played }

    /** Rounds finished in every mode. */
    val playedTotal: Int get() = StatsKey.all.sumOf { get(it).played }
}
