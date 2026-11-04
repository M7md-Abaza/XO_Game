package com.m7mdabaza.xogame.presentation.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Large anchored adaptive banner: Google's recommended banner size in Mobile
 * Ads SDK 25 (the regular anchored adaptive size and SMART_BANNER are
 * deprecated). Its height adapts to the width and can reach about 100 dp.
 * It only requests an ad when UMP consent allows it, and follows the
 * lifecycle (pause/resume/destroy) of the screen showing it.
 */
@Composable
fun BannerAd(adUnitId: String, modifier: Modifier = Modifier) {
    if (LocalInspectionMode.current) {
        // Previews cannot load ads; keep the layout space visible.
        Box(modifier.fillMaxWidth().height(50.dp))
        return
    }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val context = LocalContext.current
        val widthDp = maxWidth.value.toInt()
        val adView = remember(adUnitId, widthDp) {
            AdView(context).apply {
                setAdSize(AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, widthDp))
                this.adUnitId = adUnitId
                loadAdIfAllowed()
            }
        }
        LifecycleResumeEffect(adView) {
            adView.resume()
            onPauseOrDispose { adView.pause() }
        }
        DisposableEffect(adView) {
            onDispose { adView.destroy() }
        }
        AndroidView(factory = { adView }, modifier = Modifier.fillMaxWidth())
    }
}
