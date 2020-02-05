package com.example.xogame.StartActivities

import android.graphics.Typeface
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.xogame.R
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val typeface = Typeface.createFromAsset(assets, "sukar.ttf")
        haveFun.typeface = typeface
        singlePlayer.typeface = typeface
        multiPlayer.typeface = typeface

        onePlayerCard.setOnClickListener {

            val onePlayerBottomSheet = OnePlayerBottomSheetDialog()
            onePlayerBottomSheet.show(supportFragmentManager, "exampleBottomSheet")
        }

        twoPlayerCard.setOnClickListener {
            val twoPlayerBottomSheet = TwoPlayerBottomSheetDialog()
            twoPlayerBottomSheet.show(supportFragmentManager, "exampleBottomSheet")
        }
    }
}
