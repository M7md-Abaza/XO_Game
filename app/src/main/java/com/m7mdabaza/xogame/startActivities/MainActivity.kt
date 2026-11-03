package com.m7mdabaza.xogame.startActivities

import com.m7mdabaza.xogame.ads.loadAdIfAllowed
import com.m7mdabaza.xogame.ui.setupEdgeToEdge
import android.graphics.Typeface
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import com.m7mdabaza.xogame.ads.ConsentManager
import androidx.appcompat.app.AppCompatActivity
import com.m7mdabaza.xogame.R
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()
        setContentView(R.layout.activity_main)

        val typeface = Typeface.createFromAsset(assets, "sukar.ttf")
        haveFun.typeface = typeface
        singlePlayer.typeface = typeface
        multiPlayer.typeface = typeface
        privacySettings.typeface = typeface

        bannerAds()

        // EU users must be able to change their ad consent at any time.
        privacySettings.visibility =
            if (ConsentManager.isPrivacyOptionsRequired(this)) View.VISIBLE else View.GONE
        privacySettings.setOnClickListener {
            clickSound()
            ConsentManager.showPrivacyOptionsForm(this) { adView.loadAdIfAllowed() }
        }

        onePlayerCard.setOnClickListener {
            clickSound()
            val onePlayerBottomSheet = OnePlayerBottomSheetDialog()
            onePlayerBottomSheet.show(supportFragmentManager, "exampleBottomSheet")
        }

        twoPlayerCard.setOnClickListener {
            clickSound()
            val twoPlayerBottomSheet = TwoPlayerBottomSheetDialog()
            twoPlayerBottomSheet.show(supportFragmentManager, "exampleBottomSheet")
        }
    }

    private fun bannerAds() {
        adView.loadAdIfAllowed()
      
        
    }
    private fun clickSound() {
        val mediaPlayer: MediaPlayer = MediaPlayer.create(this, R.raw.click)
        mediaPlayer.start()
    }

}

// View accessors (replace the removed kotlin-android-extensions synthetics)
private val MainActivity.adView: com.google.android.gms.ads.AdView get() = findViewById(R.id.adView)
private val MainActivity.haveFun: android.widget.TextView get() = findViewById(R.id.haveFun)
private val MainActivity.multiPlayer: android.widget.TextView get() = findViewById(R.id.multiPlayer)
private val MainActivity.privacySettings: android.widget.TextView get() = findViewById(R.id.privacySettings)
private val MainActivity.onePlayerCard: androidx.cardview.widget.CardView get() = findViewById(R.id.onePlayerCard)
private val MainActivity.singlePlayer: android.widget.TextView get() = findViewById(R.id.singlePlayer)
private val MainActivity.twoPlayerCard: androidx.cardview.widget.CardView get() = findViewById(R.id.twoPlayerCard)
