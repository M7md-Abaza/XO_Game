package com.m7mdabaza.xogame.presentation.ads

import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.Level

/**
 * AdMob unit IDs per placement. Implemented in :app, which owns the real
 * (release) and test (debug) IDs, so this module never contains any ID.
 */
interface AdUnitIds {
    val homeBanner: String
    fun gameBanner(mode: GameMode, level: Level): String
    fun interstitial(mode: GameMode, level: Level): String
    val rewarded: String
}
