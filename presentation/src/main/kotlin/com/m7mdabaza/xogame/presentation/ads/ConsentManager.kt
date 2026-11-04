package com.m7mdabaza.xogame.presentation.ads

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

/**
 * GDPR/EEA consent via Google's User Messaging Platform (UMP).
 *
 * The consent message itself is configured in the AdMob console
 * (Privacy & messaging); this class only asks UMP whether it must be shown,
 * shows it, and gates every ad request on [canRequestAds].
 */
object ConsentManager {
    private const val TAG = "Consent"
    private val mobileAdsInitialized = AtomicBoolean(false)

    /**
     * Debug builds pretend the device is in the EEA so the consent form can be
     * tested anywhere (emulators are test devices automatically; on a real
     * phone add its hashed ID from logcat with addTestDeviceHashedId).
     */
    private fun requestParameters(activity: Activity): ConsentRequestParameters {
        val builder = ConsentRequestParameters.Builder()
        val debuggable = activity.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (debuggable) {
            builder.setConsentDebugSettings(
                ConsentDebugSettings.Builder(activity)
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                    .build()
            )
        }
        return builder.build()
    }

    private fun info(context: Context): ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)

    /**
     * Refreshes consent status and shows the consent form if the user still has
     * to answer it. [onComplete] runs exactly once, whatever happens (including
     * network errors), so the app is never blocked.
     */
    fun gatherConsent(activity: Activity, onComplete: () -> Unit) {
        val done = AtomicBoolean(false)
        fun finish() {
            if (done.compareAndSet(false, true)) {
                initializeMobileAdsIfAllowed(activity)
                onComplete()
            }
        }

        info(activity).requestConsentInfoUpdate(
            activity,
            requestParameters(activity),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    formError?.let { Log.w(TAG, "Consent form: ${it.errorCode} ${it.message}") }
                    finish()
                }
            },
            { requestError ->
                Log.w(TAG, "Consent info update: ${requestError.errorCode} ${requestError.message}")
                finish()
            }
        )
        // Consent obtained in a previous session still allows ads right away.
        initializeMobileAdsIfAllowed(activity)
    }

    fun canRequestAds(context: Context): Boolean = info(context).canRequestAds()

    /** True when the user must be offered a way to change their consent choice. */
    fun isPrivacyOptionsRequired(context: Context): Boolean =
        info(context).privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptionsForm(activity: Activity, onDismissed: () -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            formError?.let { Log.w(TAG, "Privacy options form: ${it.errorCode} ${it.message}") }
            initializeMobileAdsIfAllowed(activity)
            onDismissed()
        }
    }

    /** Initializes the Mobile Ads SDK once, and only after consent allows it. */
    fun initializeMobileAdsIfAllowed(context: Context) {
        if (!canRequestAds(context)) return
        if (mobileAdsInitialized.compareAndSet(false, true)) {
            Thread { MobileAds.initialize(context.applicationContext) {} }.start()
        }
    }
}

/** Loads this banner only if consent allows ad requests. */
fun AdView.loadAdIfAllowed() {
    if (!ConsentManager.canRequestAds(context)) return
    ConsentManager.initializeMobileAdsIfAllowed(context)
    loadAd(AdRequest.Builder().build())
}
