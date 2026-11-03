package com.m7mdabaza.xogame.ads

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

private const val TAG = "Ads"

/**
 * Wraps the current Google Mobile Ads interstitial API behind the old
 * `isLoaded` / `show()` shape the game screens were written against.
 * A new ad is preloaded automatically after each one is dismissed.
 */
class InterstitialAdManager(private val activity: Activity, private val adUnitId: String) {
    private var ad: InterstitialAd? = null
    private var loading = false

    val isLoaded: Boolean get() = ad != null

    fun load() {
        if (!ConsentManager.canRequestAds(activity)) return
        if (ad != null || loading) return
        loading = true
        InterstitialAd.load(activity, adUnitId, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(loaded: InterstitialAd) {
                    loading = false
                    ad = loaded
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loading = false
                    Log.d(TAG, "Interstitial failed to load: ${error.message}")
                }
            })
    }

    fun show() {
        val current = ad ?: run { load(); return }
        ad = null
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = load()
            override fun onAdFailedToShowFullScreenContent(error: AdError) = load()
        }
        current.show(activity)
    }
}

/**
 * Wraps the current rewarded ad API behind the old `isLoaded` / `show()` shape.
 * [onRewarded] runs when the user earns the reward; the next ad is preloaded
 * after each one is dismissed.
 */
class RewardedAdManager(
    private val activity: Activity,
    private val adUnitId: String,
    private val onRewarded: (RewardItem) -> Unit
) {
    private var ad: RewardedAd? = null
    private var loading = false

    val isLoaded: Boolean get() = ad != null

    fun load() {
        if (!ConsentManager.canRequestAds(activity)) return
        if (ad != null || loading) return
        loading = true
        RewardedAd.load(activity, adUnitId, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(loaded: RewardedAd) {
                    loading = false
                    ad = loaded
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loading = false
                    Log.d(TAG, "Rewarded ad failed to load: ${error.message}")
                }
            })
    }

    fun show() {
        val current = ad ?: run { load(); return }
        ad = null
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = load()
            override fun onAdFailedToShowFullScreenContent(error: AdError) = load()
        }
        current.show(activity) { reward -> onRewarded(reward) }
    }
}
