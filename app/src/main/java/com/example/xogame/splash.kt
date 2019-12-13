package com.example.xogame

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class splash : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val thread = object : Thread() {

            override fun run() {
                try {
                    sleep(2500)

                    val intent = Intent(applicationContext, EasyLevel::class.java)
                    startActivity(intent)

                    finish()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

            }
        }
        thread.start()
    }


    // This IfInternetAvailable() to display Views if there is internet
    private fun ifInternetAvailable() {

        //progressBar.visibility = View.VISIBLE
        if (checkConnectivity()) {

        } else
        // if no internet
        {
            val intent = Intent(applicationContext, NoInternet::class.java)
            startActivity(intent)
        }

    }

    // This checkConnectivity() for Check if third is internet or not
    private fun checkConnectivity(): Boolean {
        val connectivityManager =
            this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManager.run {
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)?.run {
                    if (hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        return true
                    } else if (hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        return true
                    }
                }
            }
        }
        return false

    }

}
