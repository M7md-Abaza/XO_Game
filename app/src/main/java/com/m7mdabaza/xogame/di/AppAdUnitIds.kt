package com.m7mdabaza.xogame.di

import android.content.Context
import com.m7mdabaza.xogame.R
import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.Level
import com.m7mdabaza.xogame.presentation.ads.AdUnitIds
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Reads the per-placement AdMob unit IDs that app/build.gradle.kts defines
 * as resValues (real IDs in release, Google test IDs in debug).
 */
class AppAdUnitIds @Inject constructor(
    @ApplicationContext private val context: Context,
) : AdUnitIds {

    override val homeBanner: String
        get() = context.getString(R.string.banner_main)

    override fun gameBanner(mode: GameMode, level: Level): String = context.getString(
        when (mode) {
            GameMode.VS_COMPUTER -> when (level) {
                Level.EASY -> R.string.banner_easy_vs_computer
                Level.MEDIUM -> R.string.banner_medium_vs_computer
                Level.HARD -> R.string.banner_hard_vs_computer
            }
            GameMode.TWO_PLAYERS -> when (level) {
                Level.EASY -> R.string.banner_easy_level
                Level.MEDIUM -> R.string.banner_medium_level
                Level.HARD -> R.string.banner_hard_level
            }
        }
    )

    override fun interstitial(mode: GameMode, level: Level): String = context.getString(
        when (mode) {
            GameMode.VS_COMPUTER -> when (level) {
                Level.EASY -> R.string.interstitial_easy_vs_computer
                Level.MEDIUM -> R.string.interstitial_medium_vs_computer
                Level.HARD -> R.string.interstitial_hard_vs_computer
            }
            GameMode.TWO_PLAYERS -> when (level) {
                Level.EASY -> R.string.interstitial_easy_level
                Level.MEDIUM -> R.string.interstitial_medium_level
                Level.HARD -> R.string.interstitial_hard_level
            }
        }
    )

    override val rewarded: String
        get() = context.getString(R.string.rewarded_vs_computer)
}
