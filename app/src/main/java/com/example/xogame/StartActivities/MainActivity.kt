package com.example.xogame.StartActivities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.xogame.OnePlayer.EasyLevelVsComputer
import com.example.xogame.OnePlayer.MediumLevelVsComputer
import com.example.xogame.R
import com.example.xogame.TwoPlayers.EasyLevel
import com.example.xogame.TwoPlayers.MediumLevel
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        onePlayerCard.setOnClickListener {
            val intent = Intent(applicationContext, EasyLevelVsComputer::class.java)
            startActivity(intent)
        }


        twoPlayerCard.setOnClickListener {
            val intent = Intent(applicationContext, MediumLevel::class.java)
            startActivity(intent)
        }
    }
}
