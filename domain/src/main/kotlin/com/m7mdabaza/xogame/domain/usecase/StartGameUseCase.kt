package com.m7mdabaza.xogame.domain.usecase

import com.m7mdabaza.xogame.domain.model.Board
import com.m7mdabaza.xogame.domain.model.GameConfig
import com.m7mdabaza.xogame.domain.model.GameResult
import com.m7mdabaza.xogame.domain.model.GameState
import javax.inject.Inject

/** Creates the empty starting state for [GameConfig]. */
class StartGameUseCase @Inject constructor() {

    operator fun invoke(config: GameConfig): GameState =
        GameState(
            config = config,
            board = Board.empty(config.boardSize.size),
            history = emptyList(),
            result = GameResult.InProgress,
        )
}
