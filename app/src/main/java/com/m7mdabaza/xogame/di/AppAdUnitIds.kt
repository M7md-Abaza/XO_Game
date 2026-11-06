package com.m7mdabaza.xogame.di

import android.content.Context
import com.m7mdabaza.xogame.R
import com.m7mdabaza.xogame.presentation.ads.AdUnitIds
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Reads the AdMob unit IDs that app/build.gradle.kts defines as resValues
 * (real IDs in release, Google test IDs in debug): one unit per placement.
 */
class AppAdUnitIds @Inject constructor(
    @ApplicationContext private val context: Context,
) : AdUnitIds {

    override val homeBanner: String
        get() = context.getString(R.string.banner_main)

    override val gameBanner: String
        get() = context.getString(R.string.banner_game)

    override val betweenRoundsInterstitial: String
        get() = context.getString(R.string.interstitial_between_rounds)

    override val rewarded: String
        get() = context.getString(R.string.rewarded)

    override val twoPhonesBanner: String
        get() = context.getString(R.string.banner_two_phones)

    override val twoPhonesInterstitial: String
        get() = context.getString(R.string.interstitial_two_phones)
}
