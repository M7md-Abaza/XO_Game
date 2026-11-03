package com.m7mdabaza.xogame.startActivities

import com.m7mdabaza.xogame.ui.setupEdgeToEdge
import android.content.Intent
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.m7mdabaza.xogame.ads.ConsentManager
import com.m7mdabaza.xogame.R
class splash : AppCompatActivity() {

    private var minTimeElapsed = false
    private var consentDone = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()
        setContentView(R.layout.activity_splash)

        val typeface = Typeface.createFromAsset(assets, "sukar.ttf")
        textView.typeface = typeface

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

// View accessors (replace the removed kotlin-android-extensions synthetics)
private val splash.textView: android.widget.TextView get() = findViewById(R.id.textView)
