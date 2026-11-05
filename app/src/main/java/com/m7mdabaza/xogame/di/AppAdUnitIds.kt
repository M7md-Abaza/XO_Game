package com.m7mdabaza.xogame.di

import android.content.Context
import com.m7mdabaza.xogame.R
import com.m7mdabaza.xogame.domain.model.BoardSize
import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.presentation.ads.AdUnitIds
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Reads the per-placement AdMob unit IDs that app/build.gradle.kts defines
 * as resValues (real IDs in release, Google test IDs in debug).
 *
 * The units are named after v1's screens: `easy`, `medium` and `hard` were
 * the 3×3, 4×4 and 8×8 boards, so each board keeps its v1 unit (whatever the
 * computer's level) and AdMob reports stay comparable.
 */
class AppAdUnitIds @Inject constructor(
    @ApplicationContext private val context: Context,
) : AdUnitIds {

    override val homeBanner: String
        get() = context.getString(R.string.banner_main)

    override fun gameBanner(mode: GameMode, boardSize: BoardSize): String = context.getString(
        when (mode) {
            GameMode.VS_COMPUTER -> when (boardSize) {
                BoardSize.THREE -> R.string.banner_easy_vs_computer
                BoardSize.FOUR -> R.string.banner_medium_vs_computer
                BoardSize.EIGHT -> R.string.banner_hard_vs_computer
            }
            GameMode.TWO_PLAYERS -> when (boardSize) {
                BoardSize.THREE -> R.string.banner_easy_level
                BoardSize.FOUR -> R.string.banner_medium_level
                BoardSize.EIGHT -> R.string.banner_hard_level
            }
        }
    )

    override fun interstitial(mode: GameMode, boardSize: BoardSize): String = context.getString(
        when (mode) {
            GameMode.VS_COMPUTER -> when (boardSize) {
                BoardSize.THREE -> R.string.interstitial_easy_vs_computer
                BoardSize.FOUR -> R.string.interstitial_medium_vs_computer
                BoardSize.EIGHT -> R.string.interstitial_hard_vs_computer
            }
            GameMode.TWO_PLAYERS -> when (boardSize) {
                BoardSize.THREE -> R.string.interstitial_easy_level
                BoardSize.FOUR -> R.string.interstitial_medium_level
                BoardSize.EIGHT -> R.string.interstitial_hard_level
            }
        }
    )

    override val rewarded: String
        get() = context.getString(R.string.rewarded_vs_computer)
}
