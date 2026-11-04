package com.m7mdabaza.xogame.presentation.review

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import kotlinx.coroutines.CancellationException

/**
 * Returns a function that shows Google Play's in-app review sheet. Play decides
 * whether it really appears (it has its own quota) and never tells the app the
 * outcome; without Play (emulators, sideloaded builds) nothing happens. When to
 * ask is decided in the domain (`ShouldRequestReviewUseCase`).
 */
@Composable
fun rememberInAppReview(): suspend () -> Unit {
    val activity = LocalActivity.current
    return remember(activity) {
        val manager = activity?.let { ReviewManagerFactory.create(it) }
        val request: suspend () -> Unit = {
            if (activity != null && manager != null) {
                try {
                    manager.launchReview(activity, manager.requestReview())
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Play unavailable or the request failed: the game simply goes on.
                }
            }
        }
        request
    }
}
