package com.m7mdabaza.xogame.domain.model

/** The outcome of a game so far. */
sealed interface GameResult {

    /** Nobody has won and there are empty cells left. */
    data object InProgress : GameResult

    /** The board is full and nobody has won. */
    data object Draw : GameResult

    /**
     * [player] has [Level.winLength] (or more) in a row. [line] is the whole
     * contiguous run, ordered from one end to the other, so the UI can draw a
     * line from its first to its last cell.
     */
    data class Win(val player: Player, val line: List<Cell>) : GameResult
}
