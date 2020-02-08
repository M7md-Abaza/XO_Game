package com.m7mdabaza.xogame.OnePlayer

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.m7mdabaza.xogame.R
import com.google.android.gms.ads.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.activity_easy_level_vs_computer.*


class EasyLevelVsComputer : AppCompatActivity(), View.OnClickListener {

    private lateinit var mInterstitialAd: InterstitialAd

    private var mBottomSheetBehavior: BottomSheetBehavior<*>? = null

    private val buttons: Array<Array<Button?>> =
        Array(3) { arrayOfNulls<Button>(3) }

    private var player1Turn = true
    private var clickable = true

    private var roundCount = 0

    private var player1Points = 0
    private var player2Points = 0

    private var draw: String = ""
    private var draw2: String = ""
    private var phoneWin: String = ""
    private var phoneWin2: String = ""
    private var youWin: String = ""
    private var youWin2: String = ""

    // this for delay computerTurn()
    private val handler: Handler = Handler()
    private val r: Runnable = Runnable {
        computerTurn()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_easy_level_vs_computer)

        val typeface = Typeface.createFromAsset(assets, "sukar.ttf")
        txt_player_1E.typeface = typeface
        txt_player_2E.typeface = typeface
        textView2.typeface = typeface
        textView6.typeface = typeface
        textView7.typeface = typeface
        text.typeface = typeface
        congratulate.typeface = typeface
        xWin.typeface = typeface

        draw = getString(R.string.its_draw)
        draw2 = getString(R.string.its_draw2)
        phoneWin = getString(R.string.phone_win)
        phoneWin2 = getString(R.string.phone_win2)
        youWin = getString(R.string.you_win)
        youWin2 = getString(R.string.you_win2)

        getButtonPosition()
        interstitialAd()
        bannerAds()


        // btn_reset for rest Buttons without change players points
        btn_resetE.setOnClickListener {
            resetBoard()
            resetGameSound()
            updatePointsText()
            //Toast.makeText(this, "New Round Started", Toast.LENGTH_SHORT).show()
            btn_resetE.visibility = View.GONE
            clickable = true

        }
    }

    @SuppressLint("CheckResult")
    override fun onClick(v: View) {
        if (clickable) {
            clickable = false
            if ((v as Button).text.toString() != "") {
                return
            }
            v.background = ContextCompat.getDrawable(this, R.drawable.x)
            v.text = "x"
            roundCount++

            if (checkForWin()) {
                if (player1Turn) {
                    player1Wins()
                } else {
                    player2Wins()
                }

            } else if (roundCount == 5) {
                draw()
            } else {

                clickSound()
                /*
                this else is for change turn from player one to player two so
                the game check after checking that no winner and rountCount not equal 9
                that is mean there in more places"Button" to play
                */
                player1Turn = !player1Turn

                handler.postDelayed(r, 1000)
            }
        }

    }

    private fun checkForWin(): Boolean {

        val field = Array(3) { arrayOfNulls<String>(3) }
        // Next for_loop using for put buttons[][] array values to field[][] array

        for (i in 0..2) {
            for (j in 0..2) {
                field[i][j] = buttons[i][j]!!.text.toString()
            }
        }
        // Next for_loop using to check the buttons in one row "horizontal" are equal or not
        for (i in 0..2) {
            if (field[i][0] == field[i][1] && field[i][0] == field[i][2] && field[i][0] != ""
            ) {
                return true
            }
        }
        // Next for_loop using to check the buttons in one column "vertical" are equal or not
        for (i in 0..2) {
            if (field[0][i] == field[1][i] && field[0][i] == field[2][i] && field[0][i] != ""
            ) {
                return true
            }
        }
        // Next for_loop using to check the buttons from top_left to bottom_right are equal or not
        if (field[0][0] == field[1][1] && field[0][0] == field[2][2] && field[0][0] != ""
        ) {
            return true
        }
        // Next last return using to check the buttons from top_right to bottom_left are equal or not
        if (field[0][2] == field[1][1] && field[0][2] == field[2][0] && field[0][2] != ""
        ) {
            return true
        }
        return false
    }

    @SuppressLint("SetTextI18n")
    private fun player1Wins() {
        player1Points++
        winSound()
        updatePointsText()
        btn_resetE.visibility = View.VISIBLE
        for (i in 0..2) {
            for (j in 0..2) {
                buttons[i][j]?.text = "-"
            }
        }
        mBottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet)
        congratulate.text = youWin
        xWin.text = youWin2
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
        //Toast.makeText(this, "Phone wins!", Toast.LENGTH_SHORT).show()
        updatePointsText()
        btn_resetE.visibility = View.VISIBLE
        for (i in 0..2) {
            for (j in 0..2) {
                buttons[i][j]?.text = "-"
            }
        }
        mBottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet)
        loseSound()
        congratulate.text = phoneWin
        xWin.text = phoneWin2
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
        btn_resetE.visibility = View.VISIBLE

        mBottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet)
        drawSound()
        congratulate.text = draw
        xWin.text = draw2
        mBottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED

        if (mInterstitialAd.isLoaded) {
            mInterstitialAd.show()
        } else {
            Log.d("TAG", "The interstitial wasn't loaded yet.")
        }
    }

    private fun computerTurn() {

        if ((buttons[1][0]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[1][2]!!.text.toString() != ""
                    || buttons[0][1]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[2][1]!!.text.toString() != ""
                    || buttons[0][0]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() != ""
                    || buttons[0][2]!!.text.toString() == buttons[2][0]!!.text.toString() && buttons[2][0]!!.text.toString() != "")
            && buttons[1][1]!!.text.toString() == ""
            && buttons[1][1]!!.text.toString() != "x"
        ) {

            buttons[1][1]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[1][1]?.setText("o")!!

        } else if ((buttons[0][0]!!.text.toString() == buttons[0][1]!!.text.toString() && buttons[0][1]!!.text.toString() != ""
                    || buttons[1][1]!!.text.toString() == buttons[2][0]!!.text.toString() && buttons[2][0]!!.text.toString() != ""
                    || buttons[1][2]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() != "")
            && buttons[0][2]!!.text.toString() == ""
            && buttons[0][2]!!.text.toString() != "x"
        ) {

            buttons[0][2]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[0][2]?.setText("o")!!

        } else if ((buttons[2][0]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[2][1]!!.text.toString() != ""
                    || buttons[0][0]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[1][1]!!.text.toString() != ""
                    || buttons[0][2]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[1][2]!!.text.toString() != "")
            && buttons[2][2]!!.text.toString() == ""
            && buttons[2][2]!!.text.toString() != "x"
        ) {

            buttons[2][2]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[2][2]?.setText("o")!!

        } else if ((buttons[0][1]!!.text.toString() == buttons[0][2]!!.text.toString() && buttons[0][2]!!.text.toString() != ""
                    || buttons[2][2]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[1][1]!!.text.toString() != ""
                    || buttons[1][0]!!.text.toString() == buttons[2][0]!!.text.toString() && buttons[2][0]!!.text.toString() != "")
            && buttons[0][0]!!.text.toString() == ""
            && buttons[0][0]!!.text.toString() != "x"
        ) {

            buttons[0][0]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[0][0]?.setText("o")!!

        } else if ((buttons[2][1]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() != ""
                    || buttons[0][0]!!.text.toString() == buttons[1][0]!!.text.toString() && buttons[1][0]!!.text.toString() != ""
                    || buttons[0][2]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[1][1]!!.text.toString() != "")
            && buttons[2][0]!!.text.toString() == ""
            && buttons[2][0]!!.text.toString() != "x"
        ) {

            buttons[2][0]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[2][0]?.setText("o")!!

        } else if ((buttons[1][0]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[1][1]!!.text.toString() != ""
                    || buttons[0][2]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() != "")
            && buttons[1][2]!!.text.toString() == ""
            && buttons[1][2]!!.text.toString() != "x"
        ) {

            buttons[1][2]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[1][2]?.setText("o")!!

        } else if ((buttons[0][0]!!.text.toString() == buttons[0][2]!!.text.toString() && buttons[0][2]!!.text.toString() != ""
                    || buttons[1][1]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[2][1]!!.text.toString() != "")
            && buttons[0][1]!!.text.toString() == ""
            && buttons[0][1]!!.text.toString() != "x"
        ) {

            buttons[0][1]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[0][1]?.setText("o")!!

        } else if ((buttons[2][0]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() != ""
                    || buttons[0][1]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[1][1]!!.text.toString() != "")
            && buttons[2][1]!!.text.toString() == ""
            && buttons[2][1]!!.text.toString() != "x"
        ) {

            buttons[2][1]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[2][1]?.setText("o")!!

        } else if ((buttons[1][1]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[1][2]!!.text.toString() != ""
                    || buttons[0][0]!!.text.toString() == buttons[2][0]!!.text.toString() && buttons[2][0]!!.text.toString() != "")
            && buttons[1][0]!!.text.toString() == ""
            && buttons[1][0]!!.text.toString() != "x"
        ) {

            buttons[1][0]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[1][0]?.setText("o")!!
        } else {

            when {
                buttons[1][2]!!.text.toString() == "" -> {
                    buttons[1][2]?.background = ContextCompat.getDrawable(
                        this,
                        R.drawable.o
                    )
                    buttons[1][2]?.setText("o")!!
                }
                buttons[0][2]!!.text.toString() == "" -> {
                    buttons[0][2]?.background = ContextCompat.getDrawable(
                        this,
                        R.drawable.o
                    )
                    buttons[0][2]?.setText("o")!!
                }
                buttons[2][1]!!.text.toString() == "" -> {
                    buttons[2][1]?.background = ContextCompat.getDrawable(
                        this,
                        R.drawable.o
                    )
                    buttons[2][1]?.setText("o")!!
                }
                buttons[0][0]!!.text.toString() == "" -> {
                    buttons[0][0]?.background = ContextCompat.getDrawable(
                        this,
                        R.drawable.o
                    )
                    buttons[0][0]?.setText("o")!!
                }
                buttons[1][1]!!.text.toString() == "" -> {
                    buttons[1][1]?.background = ContextCompat.getDrawable(
                        this,
                        R.drawable.o
                    )
                    buttons[1][1]?.setText("o")!!
                }
                buttons[2][2]!!.text.toString() == "" -> {
                    buttons[2][2]?.background = ContextCompat.getDrawable(
                        this,
                        R.drawable.o
                    )
                    buttons[2][2]?.setText("o")!!
                }
                buttons[2][0]!!.text.toString() == "" -> {
                    buttons[2][0]?.background = ContextCompat.getDrawable(
                        this,
                        R.drawable.o
                    )
                    buttons[2][0]?.setText("o")!!
                }
                buttons[0][1]!!.text.toString() == "" -> {
                    buttons[0][1]?.background = ContextCompat.getDrawable(
                        this,
                        R.drawable.o
                    )
                    buttons[0][1]?.setText("o")!!
                }
                buttons[1][0]!!.text.toString() == "" -> {
                    buttons[1][0]?.background = ContextCompat.getDrawable(
                        this,
                        R.drawable.o
                    )
                    buttons[1][0]?.setText("o")!!
                }
            }
        }

        if (checkForWin()) {
            if (player1Turn) {
                player1Wins()
            } else {
                player2Wins()
            }
        } else if (roundCount == 5) {
            draw()
        } else {
            player1Turn = !player1Turn
        }

        clickSound1()
        clickable = true
    }

    @SuppressLint("SetTextI18n")
    private fun updatePointsText() {
        txt_player_1E.text = player1Points.toString()
        txt_player_2E.text = player2Points.toString()
    }

    // to clear Buttons screen
    @SuppressLint("NewApi")
    private fun resetBoard() {
        for (i in 0..2) {
            for (j in 0..2) {
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

        mBottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet)
        mBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    // to get the Button position
    private fun getButtonPosition() {
        for (i in 0..2) {
            for (j in 0..2) {
                /*
                Notice that the "btn_$i$j" is as same as my images id in xml File without numbers
                as numbers will be add thanks to the next three lines of code in for loop
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

    private fun loseSound() {
        val mediaPlayer: MediaPlayer = MediaPlayer.create(this, R.raw.loser)
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
        adView.adUnitId = "ca-app-pub-4454440016331822/1754402706"
        // for test Ads: ca-app-pub-3940256099942544/6300978111
        // for real Ads: ca-app-pub-4454440016331822/1754402706
    }

    private fun interstitialAd() {

        mInterstitialAd = InterstitialAd(this)
        mInterstitialAd.adUnitId = "ca-app-pub-4454440016331822/8898874481"
        // for test Ads: ca-app-pub-3940256099942544/1033173712
        // for real Ads: ca-app-pub-4454440016331822/8898874481
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
