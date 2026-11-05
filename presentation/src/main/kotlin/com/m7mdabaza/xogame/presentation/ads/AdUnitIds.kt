package com.m7mdabaza.xogame.presentation.ads

import com.m7mdabaza.xogame.domain.model.BoardSize
import com.m7mdabaza.xogame.domain.model.GameMode

/**
 * AdMob unit IDs per placement. Implemented in :app, which owns the real
 * (release) and test (debug) IDs, so this module never contains any ID.
 *
 * Game placements depend on the mode and the board, not on the computer's
 * level: v1's easy / medium / hard screens were the 3×3 / 4×4 / 8×8 boards.
 */
interface AdUnitIds {
    val homeBanner: String
    fun gameBanner(mode: GameMode, boardSize: BoardSize): String
    fun interstitial(mode: GameMode, boardSize: BoardSize): String
    val rewarded: String
}
