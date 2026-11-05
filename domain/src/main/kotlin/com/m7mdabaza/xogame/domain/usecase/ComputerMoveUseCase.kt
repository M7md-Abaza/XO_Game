package com.m7mdabaza.xogame.domain.usecase

import com.m7mdabaza.xogame.domain.ai.DifficultyPolicy
import com.m7mdabaza.xogame.domain.ai.SearchAi
import com.m7mdabaza.xogame.domain.model.Cell
import com.m7mdabaza.xogame.domain.model.GameState
import javax.inject.Inject

/**
 * Picks the computer's move at the strength set for the game's board and
 * level. Only picks it; apply it with [PlayMoveUseCase]. CPU-bound on 8×8,
 * so call it on a background dispatcher.
 */
class ComputerMoveUseCase @Inject constructor(
    private val ai: SearchAi,
) {

    operator fun invoke(state: GameState): Cell {
        require(state.isComputerTurn) { "It is not the computer's turn" }
        // Only games against the computer have a computer turn, and they always have a level.
        val level = checkNotNull(state.config.level)
        return ai.chooseMove(state, DifficultyPolicy.computer(state.config.boardSize, level))
    }
}
