package com.m7mdabaza.xogame.presentation.ads

/**
 * AdMob unit IDs, one per placement (user decision 2026-11-06: 6 units
 * instead of one per mode and board). Implemented in :app, which owns the
 * real (release) and test (debug) IDs, so this module never contains any ID.
 */
interface AdUnitIds {
    /** Home and the Daily challenge. */
    val homeBanner: String

    /** The game screen against the phone and for two players, on every board. */
    val gameBanner: String

    /** Between rounds against the phone and for two players, on every board. */
    val betweenRoundsInterstitial: String

    /** Pays for a hint or an undo. */
    val rewarded: String

    /** The two-phone lobby and game screen. */
    val twoPhonesBanner: String

    /** Before the two-phone lobby (there are none between two-phone rounds). */
    val twoPhonesInterstitial: String
}
