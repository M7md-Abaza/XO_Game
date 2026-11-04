package com.m7mdabaza.xogame.startActivities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.m7mdabaza.xogame.MainActivity
import com.m7mdabaza.xogame.presentation.XoSplash
import com.m7mdabaza.xogame.presentation.ads.ConsentManager
import dagger.hilt.android.AndroidEntryPoint

/**
 * Launcher activity: shows the brand screen while the EU consent form (UMP)
 * is gathered, then opens [MainActivity]. Keep the class name: it is the
 * exported launcher component, and renaming it breaks home-screen shortcuts.
 */
@AndroidEntryPoint
class splash : AppCompatActivity() {

    private var minTimeElapsed = false
    private var consentDone = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        // Reopened from the launcher while the app's task still exists (for example after
        // Android killed the process in the background): this splash finished long ago, so
        // Android starts a new one on top. Close it to reveal the restored screens instead
        // of opening a second MainActivity.
        if (!isTaskRoot && intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_LAUNCHER)) {
            finish()
            return
        }
        setContent { XoSplash() }

        // Show the splash for at least 2 s, and wait for the EU consent form
        // (if one is required) before opening the main menu.
        Handler(Looper.getMainLooper()).postDelayed({
            minTimeElapsed = true
            continueToMain()
        }, 2000)
        ConsentManager.gatherConsent(this) {
            consentDone = true
            continueToMain()
        }
    }

    private fun continueToMain() {
        if (!minTimeElapsed || !consentDone || isFinishing) return
        startActivity(Intent(applicationContext, MainActivity::class.java))
        finish()
    }
}
