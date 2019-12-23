package com.example.xogame.StartActivities

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.xogame.R
import com.example.xogame.TwoPlayers.EasyLevel

class splash : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val thread = object : Thread() {

            override fun run() {
                try {
                    sleep(2500)

                    val intent = Intent(applicationContext, MainActivity::class.java)
                    startActivity(intent)

                    finish()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

            }
        }
        thread.start()
    }

}
