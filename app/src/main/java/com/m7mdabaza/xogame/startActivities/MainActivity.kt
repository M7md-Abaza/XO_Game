package com.m7mdabaza.xogame.startActivities

import android.graphics.Typeface
import android.media.MediaPlayer
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.m7mdabaza.xogame.R
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val typeface = Typeface.createFromAsset(assets, "sukar.ttf")
        haveFun.typeface = typeface
        singlePlayer.typeface = typeface
        multiPlayer.typeface = typeface

        bannerAds()

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
        MobileAds.initialize(this) {}
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
        val adView = AdView(this)
        adView.adSize = AdSize.SMART_BANNER
        adView.adUnitId = "ca-app-pub-3940256099942544/6300978111"
      
        
    }
    private fun clickSound() {
        val mediaPlayer: MediaPlayer = MediaPlayer.create(this, R.raw.click)
        mediaPlayer.start()
    }

}
