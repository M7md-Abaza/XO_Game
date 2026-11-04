package com.m7mdabaza.xogame.domain.usecase

import com.m7mdabaza.xogame.domain.ai.DifficultyPolicy
import com.m7mdabaza.xogame.domain.ai.SearchAi
import com.m7mdabaza.xogame.domain.model.Cell
import com.m7mdabaza.xogame.domain.model.GameState
import javax.inject.Inject

/**
 * The best move for the human whose turn it is, from the AI at full
 * strength. Null when no human may move. Whether the hint is free or costs a
 * rewarded ad is decided elsewhere. CPU-bound on 8×8, so call it on a
 * background dispatcher.
 */
class GetHintUseCase @Inject constructor(
    private val ai: SearchAi,
) {

    operator fun invoke(state: GameState): Cell? =
        if (state.isHumanTurn) ai.chooseMove(state, DifficultyPolicy.hint(state.config.level)) else null
}
