package com.m7mdabaza.xogame.onePlayer

import com.m7mdabaza.xogame.presentation.ads.loadAdIfAllowed
import com.m7mdabaza.xogame.ui.setupEdgeToEdge
import com.m7mdabaza.xogame.presentation.ads.InterstitialAdManager
import com.google.android.gms.ads.rewarded.RewardItem
import com.m7mdabaza.xogame.presentation.ads.RewardedAdManager
import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.os.Build
import android.os.Looper
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.m7mdabaza.xogame.R
import com.google.android.gms.ads.*
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
class EasyLevelVsComputer : AppCompatActivity(), View.OnClickListener {

    private lateinit var mRewardedVideoAd: RewardedAdManager

    private lateinit var mInterstitialAd: InterstitialAdManager

    private val buttons: Array<Array<Button?>> =
        Array(3) { arrayOfNulls<Button>(3) }

    private var player1Turn = true
    private var clickable = true
    private var adWatched1 = false

    private var roundCount = 0    // to determine Draw Case
    private var playTimeCount = 0 // to determine the computer Turn pattern
    private var displayAdsCount = 0   // to displayAds after sertain nomber

    private var player1Points = 0
    private var player2Points = 0

    private var draw: String = ""
    private var draw2: String = ""
    private var phoneWin: String = ""
    private var phoneWin2: String = ""
    private var youWin: String = ""
    private var youWin2: String = ""

    private var watchAds2: String = ""
    private var loadingAd: String = ""
    private var theGameBecameEasier: String = ""
    private var canNotLoadAd: String = ""

    // this for delay computerTurn()
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val r: Runnable = Runnable {
        computerTurn()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()
        setContentView(R.layout.activity_easy_level_vs_computer)

        getButtonPosition()

        val typeface = Typeface.createFromAsset(assets, "sukar.ttf")
        txt_player_1E.typeface = typeface
        txt_player_2E.typeface = typeface
        textView2.typeface = typeface
        textView6.typeface = typeface
        textView7.typeface = typeface
        text.typeface = typeface
        txt_Help.typeface = typeface

        draw = getString(R.string.its_draw)
        draw2 = getString(R.string.its_draw2)
        phoneWin = getString(R.string.phone_win)
        phoneWin2 = getString(R.string.phone_win2)
        youWin = getString(R.string.you_win)
        youWin2 = getString(R.string.you_win2)
        watchAds2 = getString(R.string.let_s_watch_an_ad2)
        theGameBecameEasier = getString(R.string.TheGameBecameEasier)
        loadingAd = getString(R.string.LoadingAd)
        canNotLoadAd = getString(R.string.CanNotLoadAd)


        bannerAds()
        interstitialAd()
        loadRewardedVideoAd()

        txt_Help.setOnClickListener {
            showAdsDialog()
        }
    }

    @SuppressLint("CheckResult")
    override fun onClick(v: View) {
        if (clickable && (v as Button).text.toString() == "") {
            clickable = false
            if (v.text.toString() != "") {
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

                handler.postDelayed(r, 500)
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

    private fun player1Wins() {
        player1Points++
        winSound()
        updatePointsText()
        for (i in 0..2) {
            for (j in 0..2) {
                buttons[i][j]?.text = "-"
            }
        }

        showXWinDialog()
        playTimeCount++
        displayAdsCount++
        adWatched1 = false
    }

    private fun player2Wins() {
        player2Points++
        updatePointsText()
        for (i in 0..2) {
            for (j in 0..2) {
                buttons[i][j]?.text = "-"
            }
        }

        showOWinDialog()
        playTimeCount++
        displayAdsCount++
    }

    private fun draw() {
        showDrawDialog()
        playTimeCount++
        displayAdsCount++
    }

    private fun computerTurn() {
        oMainPattern()
        if (player1Turn) {
            xMainPattern()
        }
        if (player1Turn && !adWatched1) {
            xTwoInRaw()
        }
        if (player1Turn) {
            if (buttons[2][2]!!.text.toString() == "") {
                buttons[2][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[2][2]?.setText("o")!!
                player1Turn = !player1Turn
            }
            if ((player1Turn && playTimeCount == 0) || (player1Turn && playTimeCount == 3) || (player1Turn && playTimeCount == 6)) {
                if (player1Turn) {
                    ifNothingToDo1()
                }
                if (player1Turn) {
                    ifNothingToDo2()
                }
                if (player1Turn) {
                    ifNothingToDo3()
                }

            } else if ((player1Turn && playTimeCount == 1) || (player1Turn && playTimeCount == 4) || (player1Turn && playTimeCount == 7)) {
                if (player1Turn) {
                    ifNothingToDo3()
                }
                if (player1Turn) {
                    ifNothingToDo1()
                }
                if (player1Turn) {
                    ifNothingToDo2()
                }
                if (playTimeCount == 7) {
                    playTimeCount = 0
                }
            } else if ((player1Turn && playTimeCount == 2) || (player1Turn && playTimeCount == 5)) {
                if (player1Turn) {
                    ifNothingToDo2()
                }
                if (player1Turn) {
                    ifNothingToDo3()
                }
                if (player1Turn) {
                    ifNothingToDo1()
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

        clickable = true
        clickSound1()
    }

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


        if (displayAdsCount == 1 || displayAdsCount == 4 || displayAdsCount == 7 || displayAdsCount == 10 || displayAdsCount == 14 || displayAdsCount == 18) {
            if (mInterstitialAd.isLoaded) {
                mInterstitialAd.show()
            } else {
                Log.d("TAG", "The interstitial wasn't loaded yet.")
                mInterstitialAd.load()
            }
        } else if (displayAdsCount == 20) {
            displayAdsCount = 0
        }
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

    /******************** Start of Computer Turn Methods ****************************/

    private fun oMainPattern() {
        if (buttons[1][1]!!.text.toString() == "") {
            buttons[1][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[1][0]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[1][2]!!.text.toString() != "" && buttons[1][2]!!.text.toString() == "o"
                    || buttons[0][1]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[2][1]!!.text.toString() != "" && buttons[2][1]!!.text.toString() == "o"
                    || buttons[0][0]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() != "" && buttons[2][2]!!.text.toString() == "o"
                    || buttons[0][2]!!.text.toString() == buttons[2][0]!!.text.toString() && buttons[2][0]!!.text.toString() != "" && buttons[2][0]!!.text.toString() == "o")
            && buttons[1][1]!!.text.toString() == ""
            && buttons[1][1]!!.text.toString() != "x"
        ) {

            buttons[1][1]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[1][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[0][0]!!.text.toString() == buttons[0][1]!!.text.toString() && buttons[0][1]!!.text.toString() != "" && buttons[0][1]!!.text.toString() == "o"
                    || buttons[1][1]!!.text.toString() == buttons[2][0]!!.text.toString() && buttons[2][0]!!.text.toString() != "" && buttons[2][0]!!.text.toString() == "o"
                    || buttons[1][2]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() != "" && buttons[2][2]!!.text.toString() == "o")
            && buttons[0][2]!!.text.toString() == ""
            && buttons[0][2]!!.text.toString() != "x"
        ) {

            buttons[0][2]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[0][2]?.setText("o")!!
            player1Turn = !player1Turn
        } else if ((buttons[2][0]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[2][1]!!.text.toString() != "" && buttons[2][1]!!.text.toString() == "o"
                    || buttons[0][0]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[1][1]!!.text.toString() != "" && buttons[1][1]!!.text.toString() == "o"
                    || buttons[0][2]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[1][2]!!.text.toString() != "" && buttons[1][2]!!.text.toString() == "o")
            && buttons[2][2]!!.text.toString() == ""
            && buttons[2][2]!!.text.toString() != "x"
        ) {

            buttons[2][2]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[2][2]?.setText("o")!!
            player1Turn = !player1Turn
        } else if ((buttons[0][1]!!.text.toString() == buttons[0][2]!!.text.toString() && buttons[0][2]!!.text.toString() != "" && buttons[0][2]!!.text.toString() == "o"
                    || buttons[2][2]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[1][1]!!.text.toString() != "" && buttons[1][1]!!.text.toString() == "o"
                    || buttons[1][0]!!.text.toString() == buttons[2][0]!!.text.toString() && buttons[2][0]!!.text.toString() != "" && buttons[2][0]!!.text.toString() == "o")
            && buttons[0][0]!!.text.toString() == ""
            && buttons[0][0]!!.text.toString() != "x"
        ) {

            buttons[0][0]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[0][0]?.setText("o")!!
            player1Turn = !player1Turn
        } else if ((buttons[2][1]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() != "" && buttons[2][2]!!.text.toString() == "o"
                    || buttons[0][0]!!.text.toString() == buttons[1][0]!!.text.toString() && buttons[1][0]!!.text.toString() != "" && buttons[1][0]!!.text.toString() == "o"
                    || buttons[0][2]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[1][1]!!.text.toString() != "" && buttons[1][1]!!.text.toString() == "o")
            && buttons[2][0]!!.text.toString() == ""
            && buttons[2][0]!!.text.toString() != "x"
        ) {

            buttons[2][0]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[2][0]?.setText("o")!!
            player1Turn = !player1Turn
        } else if ((buttons[1][0]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[1][1]!!.text.toString() != "" && buttons[1][1]!!.text.toString() == "o"
                    || buttons[0][2]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() != "" && buttons[2][2]!!.text.toString() == "o")
            && buttons[1][2]!!.text.toString() == ""
            && buttons[1][2]!!.text.toString() != "x"
        ) {

            buttons[1][2]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[1][2]?.setText("o")!!
            player1Turn = !player1Turn
        } else if ((buttons[0][0]!!.text.toString() == buttons[0][2]!!.text.toString() && buttons[0][2]!!.text.toString() != "" && buttons[0][2]!!.text.toString() == "o"
                    || buttons[1][1]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[2][1]!!.text.toString() != "" && buttons[2][1]!!.text.toString() == "o")
            && buttons[0][1]!!.text.toString() == ""
            && buttons[0][1]!!.text.toString() != "x"
        ) {

            buttons[0][1]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[0][1]?.setText("o")!!
            player1Turn = !player1Turn
        } else if ((buttons[2][0]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() != "" && buttons[2][2]!!.text.toString() == "o"
                    || buttons[0][1]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[1][1]!!.text.toString() != "" && buttons[1][1]!!.text.toString() == "o")
            && buttons[2][1]!!.text.toString() == ""
            && buttons[2][1]!!.text.toString() != "x"
        ) {

            buttons[2][1]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[2][1]?.setText("o")!!
            player1Turn = !player1Turn
        } else if ((buttons[1][1]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[1][2]!!.text.toString() != "" && buttons[1][2]!!.text.toString() == "o"
                    || buttons[0][0]!!.text.toString() == buttons[2][0]!!.text.toString() && buttons[2][0]!!.text.toString() != "" && buttons[2][0]!!.text.toString() == "o")
            && buttons[1][0]!!.text.toString() == ""
            && buttons[1][0]!!.text.toString() != "x"
        ) {

            buttons[1][0]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[1][0]?.setText("o")!!
            player1Turn = !player1Turn
        }
    }

    private fun xMainPattern() {
        if ((buttons[1][0]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[1][2]!!.text.toString() != "" && buttons[1][2]!!.text.toString() == "x"
                    || buttons[0][1]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[2][1]!!.text.toString() != "" && buttons[2][1]!!.text.toString() == "x"
                    || buttons[0][0]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() != "" && buttons[2][2]!!.text.toString() == "x"
                    || buttons[0][2]!!.text.toString() == buttons[2][0]!!.text.toString() && buttons[2][0]!!.text.toString() != "" && buttons[2][0]!!.text.toString() == "x")
            && buttons[1][1]!!.text.toString() == ""
            && buttons[1][1]!!.text.toString() != "x"
        ) {

            buttons[1][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[0][0]!!.text.toString() == buttons[0][1]!!.text.toString() && buttons[0][1]!!.text.toString() != "" && buttons[0][1]!!.text.toString() == "x"
                    || buttons[1][1]!!.text.toString() == buttons[2][0]!!.text.toString() && buttons[2][0]!!.text.toString() != "" && buttons[2][0]!!.text.toString() == "x"
                    || buttons[1][2]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() != "" && buttons[2][2]!!.text.toString() == "x")
            && buttons[0][2]!!.text.toString() == ""
            && buttons[0][2]!!.text.toString() != "x"
        ) {

            buttons[0][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][0]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[2][1]!!.text.toString() != "" && buttons[2][1]!!.text.toString() == "x"
                    || buttons[0][0]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[1][1]!!.text.toString() != "" && buttons[1][1]!!.text.toString() == "x"
                    || buttons[0][2]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[1][2]!!.text.toString() != "" && buttons[1][2]!!.text.toString() == "x")
            && buttons[2][2]!!.text.toString() == ""
            && buttons[2][2]!!.text.toString() != "x"
        ) {

            buttons[2][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[0][1]!!.text.toString() == buttons[0][2]!!.text.toString() && buttons[0][2]!!.text.toString() != "" && buttons[0][2]!!.text.toString() == "x"
                    || buttons[2][2]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[1][1]!!.text.toString() != "" && buttons[1][1]!!.text.toString() == "x"
                    || buttons[1][0]!!.text.toString() == buttons[2][0]!!.text.toString() && buttons[2][0]!!.text.toString() != "" && buttons[2][0]!!.text.toString() == "x")
            && buttons[0][0]!!.text.toString() == ""
            && buttons[0][0]!!.text.toString() != "x"
        ) {

            buttons[0][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][0]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][1]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() != "" && buttons[2][2]!!.text.toString() == "x"
                    || buttons[0][0]!!.text.toString() == buttons[1][0]!!.text.toString() && buttons[1][0]!!.text.toString() != "" && buttons[1][0]!!.text.toString() == "x"
                    || buttons[0][2]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[1][1]!!.text.toString() != "" && buttons[1][1]!!.text.toString() == "x")
            && buttons[2][0]!!.text.toString() == ""
            && buttons[2][0]!!.text.toString() != "x"
        ) {

            buttons[2][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][0]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[1][0]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[1][1]!!.text.toString() != "" && buttons[1][1]!!.text.toString() == "x"
                    || buttons[0][2]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() != "" && buttons[2][2]!!.text.toString() == "x")
            && buttons[1][2]!!.text.toString() == ""
            && buttons[1][2]!!.text.toString() != "x"
        ) {

            buttons[1][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[0][0]!!.text.toString() == buttons[0][2]!!.text.toString() && buttons[0][2]!!.text.toString() != "" && buttons[0][2]!!.text.toString() == "x"
                    || buttons[1][1]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[2][1]!!.text.toString() != "" && buttons[2][1]!!.text.toString() == "x")
            && buttons[0][1]!!.text.toString() == ""
            && buttons[0][1]!!.text.toString() != "x"
        ) {

            buttons[0][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][0]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() != "" && buttons[2][2]!!.text.toString() == "x"
                    || buttons[0][1]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[1][1]!!.text.toString() != "" && buttons[1][1]!!.text.toString() == "x")
            && buttons[2][1]!!.text.toString() == ""
            && buttons[2][1]!!.text.toString() != "x"
        ) {

            buttons[2][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[1][1]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[1][2]!!.text.toString() != "" && buttons[1][2]!!.text.toString() == "x"
                    || buttons[0][0]!!.text.toString() == buttons[2][0]!!.text.toString() && buttons[2][0]!!.text.toString() != "" && buttons[2][0]!!.text.toString() == "x")
            && buttons[1][0]!!.text.toString() == ""
            && buttons[1][0]!!.text.toString() != "x"
        ) {

            buttons[1][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][0]?.setText("o")!!
            player1Turn = !player1Turn
        }
    }

    //prevent getting 2 X in Raw
    private fun xTwoInRaw() {
        for (i in 0..2) {
            /******************************* Horizontal Lines********************************/
            if ((buttons[i][1]!!.text.toString() == "x" && buttons[i][2]!!.text.toString() != "o")
                && buttons[i][0]!!.text.toString() == ""
                && buttons[i][0]!!.text.toString() != "x"
            ) {

                buttons[i][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[i][0]?.setText("o")!!
                player1Turn = !player1Turn
                break
            } else if (((buttons[i][0]!!.text.toString() == "x" && buttons[i][2]!!.text.toString() != "o")
                        || (buttons[i][2]!!.text.toString() == "x" && buttons[i][0]!!.text.toString() != "o"))
                && buttons[i][1]!!.text.toString() == ""
                && buttons[i][1]!!.text.toString() != "x"
            ) {

                buttons[i][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[i][1]?.setText("o")!!
                player1Turn = !player1Turn
                break
            } else if ((buttons[i][1]!!.text.toString() == "x" && buttons[i][0]!!.text.toString() != "o")
                && buttons[i][2]!!.text.toString() == ""
                && buttons[i][2]!!.text.toString() != "x"
            ) {

                buttons[i][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[i][2]?.setText("o")!!
                player1Turn = !player1Turn
                break
            }
            /******************************* Vertical Lines********************************/
            if ((buttons[1][i]!!.text.toString() == "x" && buttons[2][i]!!.text.toString() != "o")
                && buttons[0][i]!!.text.toString() == ""
                && buttons[0][i]!!.text.toString() != "x"
            ) {

                buttons[0][i]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[0][i]?.setText("o")!!
                player1Turn = !player1Turn
                break
            } else if (((buttons[0][i]!!.text.toString() == "x" && buttons[2][i]!!.text.toString() != "o")
                        || (buttons[2][i]!!.text.toString() == "x" && buttons[0][i]!!.text.toString() != "o"))
                && buttons[1][i]!!.text.toString() == ""
                && buttons[1][i]!!.text.toString() != "x"
            ) {

                buttons[1][i]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[1][i]?.setText("o")!!
                player1Turn = !player1Turn
                break
            } else if ((buttons[1][i]!!.text.toString() == "x" && buttons[0][i]!!.text.toString() != "o")
                && buttons[2][i]!!.text.toString() == ""
                && buttons[2][i]!!.text.toString() != "x"
            ) {

                buttons[2][i]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[2][i]?.setText("o")!!
                player1Turn = !player1Turn
                break
            }
        }

        /*********************** TopLeft to BottomRight Line ************************/
        if (player1Turn) {
            if ((buttons[1][1]!!.text.toString() == "x" && buttons[2][2]!!.text.toString() != "o")
                && buttons[0][0]!!.text.toString() == ""
                && buttons[0][0]!!.text.toString() != "x"
            ) {

                buttons[0][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[0][0]?.setText("o")!!
                player1Turn = !player1Turn

            } else if (((buttons[0][0]!!.text.toString() == "x" && buttons[2][2]!!.text.toString() != "o")
                        || (buttons[2][2]!!.text.toString() == "x" && buttons[0][0]!!.text.toString() != "o"))
                && buttons[1][1]!!.text.toString() == ""
                && buttons[1][1]!!.text.toString() != "x"
            ) {

                buttons[1][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[1][1]?.setText("o")!!
                player1Turn = !player1Turn

            } else if ((buttons[1][1]!!.text.toString() == "x" && buttons[0][0]!!.text.toString() != "o")
                && buttons[2][2]!!.text.toString() == ""
                && buttons[2][2]!!.text.toString() != "x"
            ) {

                buttons[2][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[2][2]?.setText("o")!!
                player1Turn = !player1Turn

            }
            /*********************** TopRight to BottomLeft Line ************************/
            else if ((buttons[1][1]!!.text.toString() == "x" && buttons[2][0]!!.text.toString() != "o")
                && buttons[0][2]!!.text.toString() == ""
                && buttons[0][2]!!.text.toString() != "x"
            ) {

                buttons[0][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[0][2]?.setText("o")!!
                player1Turn = !player1Turn

            } else if (((buttons[0][2]!!.text.toString() == "x" && buttons[2][0]!!.text.toString() != "o")
                        || (buttons[2][0]!!.text.toString() == "x" && buttons[0][2]!!.text.toString() != "o"))
                && buttons[1][1]!!.text.toString() == ""
                && buttons[1][1]!!.text.toString() != "x"
            ) {

                buttons[1][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[1][1]?.setText("o")!!
                player1Turn = !player1Turn

            } else if ((buttons[1][1]!!.text.toString() == "x" && buttons[0][2]!!.text.toString() != "o")
                && buttons[2][0]!!.text.toString() == ""
                && buttons[2][0]!!.text.toString() != "x"
            ) {

                buttons[2][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[2][0]?.setText("o")!!
                player1Turn = !player1Turn

            }
        }
    }

    private fun ifNothingToDo1() {
        when {
            buttons[1][2]!!.text.toString() == "" -> {
                buttons[1][2]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[1][2]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[0][2]!!.text.toString() == "" -> {
                buttons[0][2]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[0][2]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[2][1]!!.text.toString() == "" -> {
                buttons[2][1]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[2][1]?.setText("o")!!
                player1Turn = !player1Turn
            }
        }
    }

    private fun ifNothingToDo2() {
        when {
            buttons[0][0]!!.text.toString() == "" -> {
                buttons[0][0]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[0][0]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[1][1]!!.text.toString() == "" -> {
                buttons[1][1]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[1][1]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[2][2]!!.text.toString() == "" -> {
                buttons[2][2]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[2][2]?.setText("o")!!
                player1Turn = !player1Turn
            }
        }
    }

    private fun ifNothingToDo3() {
        when {
            buttons[2][0]!!.text.toString() == "" -> {
                buttons[2][0]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[2][0]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[0][1]!!.text.toString() == "" -> {
                buttons[0][1]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[0][1]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[1][0]!!.text.toString() == "" -> {
                buttons[1][0]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[1][0]?.setText("o")!!
                player1Turn = !player1Turn
            }
        }
    }

    /******************** End of Computer Turn Methods ****************************/

    private fun bannerAds() {
        adView.loadAdIfAllowed()
       
    }

    private fun interstitialAd() {
        mInterstitialAd = InterstitialAdManager(this, getString(R.string.interstitial_easy_vs_computer))
        mInterstitialAd.load()
    }

    /************************* Start of reward ads ************************/

    private fun loadRewardedVideoAd() {
        if (!::mRewardedVideoAd.isInitialized) {
            mRewardedVideoAd = RewardedAdManager(this, getString(R.string.rewarded_vs_computer)) { onRewarded(it) }
        }
        mRewardedVideoAd.load()
    }


    private fun onRewarded(@Suppress("UNUSED_PARAMETER") reward: RewardItem) {
        //Toast.makeText(this, "onRewarded! currency: ${reward.type} amount: ${reward.amount}", Toast.LENGTH_SHORT).show()
        // Reward the user.
        adWatched1 = true

    }

    /************************* End of reward ads ************************/
    @SuppressLint("InflateParams")
    private fun showXWinDialog() {
        val view = LayoutInflater.from(this@EasyLevelVsComputer)
            .inflate(R.layout.win_pop_up_dialog, null)
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCanceledOnTouchOutside(false)

        val typeface = Typeface.createFromAsset(assets, "sukar.ttf")
        view.textView10.typeface = typeface
        view.dialogNewRound.typeface = typeface

        view.textView10.text = youWin

        winSound()

        view.dialogNewRound.setOnClickListener {
            resetBoard()
            resetGameSound()
            updatePointsText()

            clickable = true
            dialog.dismiss()
        }
        dialog.show()
    }

    @SuppressLint("InflateParams")
    private fun showOWinDialog() {
        val view = LayoutInflater.from(this@EasyLevelVsComputer)
            .inflate(R.layout.win_pop_up_dialog, null)
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCanceledOnTouchOutside(false)

        val typeface = Typeface.createFromAsset(assets, "sukar.ttf")
        view.textView10.typeface = typeface
        view.textView11.typeface = typeface
        view.dialogNewRound.typeface = typeface

        view.textView10.text = phoneWin
        view.textView11.text = phoneWin2

        loseSound()

        view.dialogNewRound.setOnClickListener {
            resetBoard()
            resetGameSound()
            updatePointsText()

            clickable = true
            dialog.dismiss()
        }
        dialog.show()
    }

    @SuppressLint("InflateParams")
    private fun showDrawDialog() {
        val view = LayoutInflater.from(this@EasyLevelVsComputer)
            .inflate(R.layout.win_pop_up_dialog, null)
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCanceledOnTouchOutside(false)

        val typeface = Typeface.createFromAsset(assets, "sukar.ttf")
        view.textView10.typeface = typeface
        view.textView11.typeface = typeface
        view.dialogNewRound.typeface = typeface

        view.textView10.text = draw
        view.textView11.text = draw2

        drawSound()
        view.dialogNewRound.setOnClickListener {
            resetBoard()
            resetGameSound()
            updatePointsText()

            clickable = true
            dialog.dismiss()
        }
        dialog.show()
    }

    @SuppressLint("InflateParams")
    private fun showAdsDialog() {
        val view = LayoutInflater.from(this@EasyLevelVsComputer)
            .inflate(R.layout.rewaed_ads_pop_up, null)

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCanceledOnTouchOutside(false)

        val typeface = Typeface.createFromAsset(assets, "sukar.ttf")
        view.adsText.typeface = typeface
        view.watchAd.typeface = typeface
        view.cancel.typeface = typeface

        clickSound()

        view.watchAd.setOnClickListener {
            clickSound()
            if (mRewardedVideoAd.isLoaded) {
                mRewardedVideoAd.show()
                dialog.dismiss()
            } else {
                view.adsText.text = loadingAd
                view.progress_circular.visibility = View.VISIBLE
                loadRewardedVideoAd()

                if (mRewardedVideoAd.isLoaded) {
                    view.progress_circular.visibility = View.GONE
                    mRewardedVideoAd.show()
                    view.adsText.text = theGameBecameEasier
                } else {
                    view.adsText.text = canNotLoadAd
                }
            }
        }

        view.cancel.setOnClickListener {
            clickSound()
            dialog.dismiss()
        }

        dialog.show()
    }

}

// View accessors (replace the removed kotlin-android-extensions synthetics)
private val EasyLevelVsComputer.adView: com.google.android.gms.ads.AdView get() = findViewById(R.id.adView)
private val EasyLevelVsComputer.text: android.widget.TextView get() = findViewById(R.id.text)
private val EasyLevelVsComputer.textView2: android.widget.TextView get() = findViewById(R.id.textView2)
private val EasyLevelVsComputer.textView6: android.widget.TextView get() = findViewById(R.id.textView6)
private val EasyLevelVsComputer.textView7: android.widget.TextView get() = findViewById(R.id.textView7)
private val EasyLevelVsComputer.txt_Help: android.widget.TextView get() = findViewById(R.id.txt_Help)
private val EasyLevelVsComputer.txt_player_1E: android.widget.TextView get() = findViewById(R.id.txt_player_1E)
private val EasyLevelVsComputer.txt_player_2E: android.widget.TextView get() = findViewById(R.id.txt_player_2E)
private val android.view.View.adsText: android.widget.TextView get() = findViewById(R.id.adsText)
private val android.view.View.cancel: android.widget.TextView get() = findViewById(R.id.cancel)
private val android.view.View.dialogNewRound: android.widget.TextView get() = findViewById(R.id.dialogNewRound)
private val android.view.View.progress_circular: android.widget.ProgressBar get() = findViewById(R.id.progress_circular)
private val android.view.View.textView10: android.widget.TextView get() = findViewById(R.id.textView10)
private val android.view.View.textView11: android.widget.TextView get() = findViewById(R.id.textView11)
private val android.view.View.watchAd: android.widget.TextView get() = findViewById(R.id.watchAd)
