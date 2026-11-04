package com.m7mdabaza.xogame

import android.app.Application
import com.m7mdabaza.xogame.data.billing.CurrentActivityTracker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class XoApplication : Application() {

    /** Play Billing needs the resumed activity to show the purchase sheet. */
    @Inject
    lateinit var activityTracker: CurrentActivityTracker

    override fun onCreate() {
        super.onCreate() // Hilt injects here
        registerActivityLifecycleCallbacks(activityTracker)
    }
}
