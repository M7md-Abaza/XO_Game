package com.m7mdabaza.xogame.domain.model

/**
 * Everything chosen before a game starts.
 *
 * @property humanSymbol the human's mark against the computer. Ignored in
 *   [GameMode.TWO_PLAYERS], where both marks are human.
 * @property firstPlayer the mark that moves first. A "random" choice is
 *   resolved to a concrete mark before the config is built.
 */
data class GameConfig(
    val mode: GameMode,
    val level: Level,
    val humanSymbol: Player = Player.X,
    val firstPlayer: Player = Player.X,
) {
    /** The computer's mark, or null in two-player mode. */
    val computerSymbol: Player?
        get() = if (mode == GameMode.VS_COMPUTER) humanSymbol.opponent else null
}
