package com.m7mdabaza.xogame.domain.model

/**
 * A snapshot of one game. Instances are created only by the game use cases
 * (`StartGameUseCase`, `PlayMoveUseCase`, `UndoMoveUseCase`), so the board,
 * history and result always agree.
 *
 * The state is fully determined by [config] and the cells in [history], so it
 * can be saved as those two and rebuilt by replaying the moves.
 */
@ConsistentCopyVisibility
data class GameState internal constructor(
    val config: GameConfig,
    val board: Board,
    val history: List<Move>,
    val result: GameResult,
) {
    /** The mark that moves next (meaningless once [isOver]). */
    val turn: Player
        get() = history.lastOrNull()?.player?.opponent ?: config.firstPlayer

    val lastMove: Move?
        get() = history.lastOrNull()

    val isOver: Boolean
        get() = result != GameResult.InProgress

    /** True when a human may tap a cell now. */
    val isHumanTurn: Boolean
        get() = !isOver && (config.mode == GameMode.TWO_PLAYERS || turn == config.humanSymbol)

    /** True when the computer should move now. */
    val isComputerTurn: Boolean
        get() = !isOver && turn == config.computerSymbol
}
