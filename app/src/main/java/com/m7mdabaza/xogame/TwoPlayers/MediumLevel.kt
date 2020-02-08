package com.m7mdabaza.xogame.TwoPlayers

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.media.MediaPlayer
import android.os.Build
import android.view.View
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.m7mdabaza.xogame.R
import com.google.android.gms.ads.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.medium_level.*


class MediumLevel : AppCompatActivity(), View.OnClickListener {

    private lateinit var mInterstitialAd: InterstitialAd

    private var mBottomSheetBehavior: BottomSheetBehavior<*>? = null

    private val buttons: Array<Array<Button?>> =
        Array(4) { arrayOfNulls<Button>(4) }

    private var player1Turn = true

    private var roundCount = 0

    private var player1Points = 0
    private var player2Points = 0

    private var draw: String = ""
    private var draw2: String = ""
    private var xWin: String = ""
    private var oWin: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.medium_level)

        val typeface = Typeface.createFromAsset(assets, "sukar.ttf")
        textView2.typeface = typeface
        textView.typeface = typeface
        textView6.typeface = typeface
        textView7.typeface = typeface
        txt_player_1M.typeface = typeface
        txt_player_2M.typeface = typeface
        congratulateM2.typeface = typeface
        xWinM2.typeface = typeface


        draw = getString(R.string.its_draw)
        draw2 = getString(R.string.its_draw2)
        xWin = getString(R.string.player_X_win)
        oWin = getString(R.string.player_O_win)

        getButtonPosition()
        bannerAds()
        interstitialAd()
        // btn_reset for rest Buttons without change players points
        btn_resetM.setOnClickListener {
            resetBoard()
            resetGameSound()
            updatePointsText()
            //Toast.makeText(this, "New Round Started", Toast.LENGTH_SHORT).show()
            btn_resetM.visibility = View.GONE
        }
    }

    override fun onClick(v: View) {
        if ((v as Button).text.toString() != "") {
            return
        }
        if (player1Turn) {
            v.background = ContextCompat.getDrawable(
                this,
                R.drawable.x
            )
            v.text = "x"
        } else {
            v.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            v.text = "o"
        }

        roundCount++

        if (checkForWin()) {
            if (player1Turn) {
                player1Wins()
            } else {
                player2Wins()
            }
        } else if (roundCount == 16) {
            draw()
        } else {
            if (player1Turn) {
                clickSound()
            } else {
                clickSound1()
            }
            /*
            this else is for change turn from player one to player two so
            the game check after checking that no winner and rountCount not equal 9
            that is mean there in more places"Button" to play
            */
            player1Turn = !player1Turn
        }
    }

    private fun checkForWin(): Boolean {

        val field = Array(4) { arrayOfNulls<String>(4) }
        // Next for_loop using for put buttons[][] array values to field[][] array
        for (i in 0..3) {
            for (j in 0..3) {
                field[i][j] = buttons[i][j]!!.text.toString()
            }
        }
        // Next for_loop using to check the buttons in one row "horizontal" are equal or not
        for (i in 0..3) {
            if (field[i][0] == field[i][1] && field[i][0] == field[i][2] && field[i][0] == field[i][3] && field[i][0] != ""
            ) {
                return true
            }
        }
        // Next for_loop using to check the buttons in one column "vertical" are equal or not
        for (i in 0..3) {
            if (field[0][i] == field[1][i] && field[0][i] == field[2][i] && field[0][i] == field[3][i] && field[0][i] != ""
            ) {
                return true
            }
        }
        // Next for_loop using to check the buttons from top_left to bottom_right are equal or not
        if (field[0][0] == field[1][1] && field[0][0] == field[2][2] && field[0][0] == field[3][3] && field[0][0] != ""
        ) {
            return true
        }
        // Next last return using to check the buttons from top_right to bottom_left are equal or not
        if (field[0][3] == field[1][2] && field[0][3] == field[2][1] && field[0][3] == field[3][0] && field[0][3] != ""
        ) {
            return true
        }
        return false
    }

    @SuppressLint("SetTextI18n")
    private fun player1Wins() {
        player1Points++
        //Toast.makeText(this, "Player 1 wins!", Toast.LENGTH_SHORT).show()
        updatePointsText()
        btn_resetM.visibility = View.VISIBLE
        for (i in 0..3) {
            for (j in 0..3) {
                buttons[i][j]?.text = "-"
            }
        }

        mBottomSheetBehavior = BottomSheetBehavior.from(bottom_sheetM2)
        winSound()
        xWinM2.text = xWin
        mBottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED

        if (mInterstitialAd.isLoaded) {
            mInterstitialAd.show()
        } else {
            Log.d("TAG", "The interstitial wasn't loaded yet.")
        }
    }

    @SuppressLint("SetTextI18n")
    private fun player2Wins() {
        player2Points++
        //Toast.makeText(this, "Player 2 wins!", Toast.LENGTH_SHORT).show()
        updatePointsText()
        btn_resetM.visibility = View.VISIBLE
        for (i in 0..3) {
            for (j in 0..3) {
                buttons[i][j]?.text = "-"
            }
        }

        mBottomSheetBehavior = BottomSheetBehavior.from(bottom_sheetM2)
        winSound()
        xWinM2.text = oWin
        mBottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED

        if (mInterstitialAd.isLoaded) {
            mInterstitialAd.show()
        } else {
            Log.d("TAG", "The interstitial wasn't loaded yet.")
        }
    }

    @SuppressLint("SetTextI18n")
    private fun draw() {
        //Toast.makeText(this, "Draw!", Toast.LENGTH_SHORT).show()
        btn_resetM.visibility = View.VISIBLE

        mBottomSheetBehavior = BottomSheetBehavior.from(bottom_sheetM2)
        drawSound()
        congratulateM2.text = draw
        xWinM2.text = draw2
        mBottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED

        if (mInterstitialAd.isLoaded) {
            mInterstitialAd.show()
        } else {
            Log.d("TAG", "The interstitial wasn't loaded yet.")
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updatePointsText() {
        txt_player_1M.text = player1Points.toString()
        txt_player_2M.text = player2Points.toString()
    }

    // to clear Buttons screen
    @SuppressLint("NewApi")
    private fun resetBoard() {
        for (i in 0..3) {
            for (j in 0..3) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    buttons[i][j]?.setBackgroundResource(R.drawable.empty)
                } else {
                    buttons[i][j]!!.setBackgroundResource(R.drawable.empty)
                }
                buttons[i][j]?.text = ""
            }
        }
        roundCount = 0
        player1Turn = true

        mBottomSheetBehavior = BottomSheetBehavior.from(bottom_sheetM2)
        mBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    // to get the Button position
    private fun getButtonPosition() {
        for (i in 0..3) {
            for (j in 0..3) {
                /*
                Notice that the "btn_$i$j" is as same as my images id in xml File without numbers
                as numbers will be add thanks to the next three lines of code
                */
                val imageID = "btn_$i$j"
                val resID = resources.getIdentifier(imageID, "id", packageName)
                buttons[i][j] = findViewById<View>(resID) as Button?
                buttons[i][j]?.setOnClickListener(this)
            }
        }
    }

    //onSaveInstanceState() to save data during rotate the screen till do not lose it
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("roundCount", roundCount)
        outState.putInt("player1Points", player1Points)
        outState.putInt("player2Points", player2Points)
        outState.putBoolean("player1Turn", player1Turn)
    }

    //onRestoreInstanceState() to restore data which saved by onSaveInstanceState() after rotate the screen
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        roundCount = savedInstanceState.getInt("roundCount")
        player1Points = savedInstanceState.getInt("player1Points")
        player2Points = savedInstanceState.getInt("player2Points")
        player1Turn = savedInstanceState.getBoolean("player1Turn")
    }

    private fun winSound() {
        val mediaPlayer: MediaPlayer = MediaPlayer.create(this, R.raw.win)
        mediaPlayer.start()
    }

    private fun drawSound() {
        val mediaPlayer: MediaPlayer = MediaPlayer.create(this, R.raw.draw)
        mediaPlayer.start()
    }

    private fun clickSound() {
        val mediaPlayer: MediaPlayer = MediaPlayer.create(this, R.raw.click)
        mediaPlayer.start()
    }

    private fun clickSound1() {
        val mediaPlayer: MediaPlayer = MediaPlayer.create(this, R.raw.click1)
        mediaPlayer.start()
    }

    private fun resetGameSound() {
        val mediaPlayer: MediaPlayer = MediaPlayer.create(this, R.raw.rest)
        mediaPlayer.start()
    }

    private fun bannerAds() {
        MobileAds.initialize(this) {}
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
        val adView = AdView(this)
        adView.adSize = AdSize.SMART_BANNER
        adView.adUnitId = "ca-app-pub-4454440016331822/9832682202"
        //for real: ca-app-pub-4454440016331822/9832682202
    }

    private fun interstitialAd() {
        mInterstitialAd = InterstitialAd(this)
        mInterstitialAd.adUnitId = "ca-app-pub-4454440016331822/2369480375"
        //for real: ca-app-pub-4454440016331822/2369480375
        mInterstitialAd.loadAd(AdRequest.Builder().build())


        mInterstitialAd.adListener = object : AdListener() {
            override fun onAdLoaded() {
                // Code to be executed when an ad finishes loading.
            }

            override fun onAdFailedToLoad(errorCode: Int) {
                // Code to be executed when an ad request fails.
                mInterstitialAd.loadAd(AdRequest.Builder().build())
            }

            override fun onAdOpened() {
                // Code to be executed when the ad is displayed.
            }

            override fun onAdClicked() {
                // Code to be executed when the user clicks on an ad.
            }

            override fun onAdLeftApplication() {
                // Code to be executed when the user has left the app.
            }

            override fun onAdClosed() {
                // Code to be executed when the interstitial ad is closed.
                mInterstitialAd.loadAd(AdRequest.Builder().build())
            }
        }
    }
}
