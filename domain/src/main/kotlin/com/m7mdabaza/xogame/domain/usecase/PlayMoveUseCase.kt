package com.m7mdabaza.xogame.domain.usecase

import com.m7mdabaza.xogame.domain.engine.WinChecker
import com.m7mdabaza.xogame.domain.model.Cell
import com.m7mdabaza.xogame.domain.model.GameState
import com.m7mdabaza.xogame.domain.model.Move
import javax.inject.Inject

/**
 * Places the mark of the player whose turn it is on a cell and computes the
 * new result. Whether a *human* may move now (versus waiting for the
 * computer) is the caller's job; see [GameState.isHumanTurn].
 */
class PlayMoveUseCase @Inject constructor(
    private val winChecker: WinChecker,
) {

    operator fun invoke(state: GameState, cell: Cell): MoveResult {
        if (state.isOver) return MoveResult.GameOver
        if (cell !in state.board) return MoveResult.OutOfBounds
        if (!state.board.isEmpty(cell)) return MoveResult.CellOccupied

        val player = state.turn
        val board = state.board.with(cell, player)
        return MoveResult.Played(
            state.copy(
                board = board,
                history = state.history + Move(cell, player),
                result = winChecker.resultAfter(board, cell, state.config.boardSize.winLength),
            ),
        )
    }
}

/** What happened when a move was attempted. Rejected moves leave the state unchanged. */
sealed interface MoveResult {
    data class Played(val state: GameState) : MoveResult
    data object GameOver : MoveResult
    data object OutOfBounds : MoveResult
    data object CellOccupied : MoveResult
}
