package com.m7mdabaza.xogame.domain.usecase

import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.GameResult
import com.m7mdabaza.xogame.domain.model.GameState
import javax.inject.Inject

/**
 * Takes back the last move.
 *
 * - Two players: removes exactly the last move.
 * - Against the computer: removes moves back to and including the human's
 *   last move (normally the computer's reply plus the human's move), so it is
 *   the human's turn again.
 *
 * - Two phones: never (user decision: no undo between two phones).
 *
 * Works on finished games too, which reopens them. Returns null when there is
 * nothing to undo (no moves, or against the computer only its opening move).
 */
class UndoMoveUseCase @Inject constructor() {

    operator fun invoke(state: GameState): GameState? {
        val config = state.config
        val removeCount = when (config.mode) {
            GameMode.TWO_PLAYERS -> if (state.history.isEmpty()) return null else 1
            GameMode.TWO_PHONES -> return null
            GameMode.VS_COMPUTER -> {
                val lastHumanMove = state.history.indexOfLast { it.player == config.humanSymbol }
                if (lastHumanMove < 0) return null
                state.history.size - lastHumanMove
            }
        }

        val removed = state.history.takeLast(removeCount)
        // Every earlier position was still in progress, otherwise the game
        // would have stopped there, so the result is simply InProgress.
        return state.copy(
            board = removed.fold(state.board) { board, move -> board.without(move.cell) },
            history = state.history.dropLast(removeCount),
            result = GameResult.InProgress,
        )
    }
}
