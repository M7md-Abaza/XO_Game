package com.m7mdabaza.xogame.domain.model

/**
 * Everything chosen before a game starts.
 *
 * @property level the computer's strength; required against the computer
 *   and null in the other modes, where there is no computer.
 * @property humanSymbol the human's mark against the computer, or this
 *   phone's player's mark in [GameMode.TWO_PHONES]. Ignored in
 *   [GameMode.TWO_PLAYERS], where both marks are human.
 * @property firstPlayer the mark that moves first. A "random" choice is
 *   resolved to a concrete mark before the config is built.
 */
data class GameConfig(
    val mode: GameMode,
    val boardSize: BoardSize,
    val level: Level?,
    val humanSymbol: Player = Player.X,
    val firstPlayer: Player = Player.X,
) {
    init {
        require((mode == GameMode.VS_COMPUTER) == (level != null)) {
            "A level is needed against the computer and only there (mode $mode, level $level)"
        }
    }

    /** The computer's mark, or null in two-player mode. */
    val computerSymbol: Player?
        get() = if (mode == GameMode.VS_COMPUTER) humanSymbol.opponent else null

    /** The stats record a finished round counts towards. */
    val statsKey: StatsKey
        get() = StatsKey(mode, boardSize, level)
}
