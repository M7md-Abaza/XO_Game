package com.m7mdabaza.xogame.StartActivities

import android.content.Intent
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.m7mdabaza.xogame.R
import kotlinx.android.synthetic.main.activity_splash.*

class splash : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val typeface = Typeface.createFromAsset(assets, "sukar.ttf")
        textView.typeface = typeface

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
