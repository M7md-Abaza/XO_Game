package com.m7mdabaza.xogame.presentation.ads

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

private const val TAG = "Ads"

/**
 * One interstitial at a time for [activity]: [load] it ahead of time, then
 * [show] it between rounds. The next one is preloaded after each is closed.
 * Ads are only requested when UMP consent allows it.
 */
class InterstitialAdManager(private val activity: Activity, private val adUnitId: String) {
    private var ad: InterstitialAd? = null
    private var loading = false

    val isLoaded: Boolean get() = ad != null

    fun load() {
        if (!ConsentManager.canRequestAds(activity)) return
        if (ad != null || loading) return
        loading = true
        InterstitialAd.load(
            activity, adUnitId, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(loaded: InterstitialAd) {
                    loading = false
                    ad = loaded
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loading = false
                    Log.d(TAG, "Interstitial failed to load: ${error.message}")
                }
            },
        )
    }

    /**
     * Shows the loaded ad and calls [onFinished] when it closes (with true),
     * or right away (with false) if no ad is ready.
     */
    fun show(onFinished: (shown: Boolean) -> Unit) {
        val current = ad ?: run {
            load()
            onFinished(false)
            return
        }
        ad = null
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                load()
                onFinished(true)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                load()
                onFinished(false)
            }
        }
        current.show(activity)
    }
}

/**
 * One rewarded ad at a time for [activity], used to pay for a hint or an
 * undo. The next one is preloaded after each is closed.
 */
class RewardedAdManager(private val activity: Activity, private val adUnitId: String) {
    private var ad: RewardedAd? = null
    private var loading = false

    val isLoaded: Boolean get() = ad != null

    fun load() {
        if (!ConsentManager.canRequestAds(activity)) return
        if (ad != null || loading) return
        loading = true
        RewardedAd.load(
            activity, adUnitId, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(loaded: RewardedAd) {
                    loading = false
                    ad = loaded
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loading = false
                    Log.d(TAG, "Rewarded ad failed to load: ${error.message}")
                }
            },
        )
    }

    /**
     * Shows the loaded ad; [onFinished] runs when it closes and says whether
     * the reward was earned. Returns false (and never calls [onFinished]) if
     * no ad is ready.
     */
    fun show(onFinished: (rewarded: Boolean) -> Unit): Boolean {
        val current = ad ?: run {
            load()
            return false
        }
        ad = null
        var rewarded = false
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                load()
                onFinished(rewarded)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                load()
                onFinished(false)
            }
        }
        current.show(activity) { rewarded = true }
        return true
    }
}
