package com.m7mdabaza.xogame.onePlayer

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.Button
import androidx.core.content.ContextCompat
import com.m7mdabaza.xogame.R
import com.google.android.gms.ads.*
import kotlinx.android.synthetic.main.activity_medium_level_vs_computer.*
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.reward.RewardItem
import com.google.android.gms.ads.reward.RewardedVideoAd
import com.google.android.gms.ads.reward.RewardedVideoAdListener
import kotlinx.android.synthetic.main.rewaed_ads_pop_up.view.*
import kotlinx.android.synthetic.main.win_pop_up_dialog.view.*

class MediumLevelVsComputer : AppCompatActivity(), View.OnClickListener, RewardedVideoAdListener {

    private lateinit var mRewardedVideoAd: RewardedVideoAd
    private lateinit var mInterstitialAd: InterstitialAd

    private val buttons: Array<Array<Button?>> =
        Array(4) { arrayOfNulls<Button>(4) }

    private var player1Turn = true
    private var clickable = true
    private var adWatched1 = false
    private var adWatched2 = false

    private var roundCount = 0      // to determine Draw Case
    private var playTimeCount = 0   // to determine the computer Turn pattern

    private var player1Points = 0
    private var player2Points = 0

    private var draw: String = ""
    private var draw2: String = ""
    private var phoneWin: String = ""
    private var phoneWin2: String = ""
    private var youWin: String = ""
    private var watchAds2: String = ""
    private var loadingAd: String = ""
    private var theGameBecameEasier: String = ""
    private var canNotLoadAd: String = ""

    private val handler: Handler = Handler()
    private val r: Runnable = Runnable {
        computerTurn()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medium_level_vs_computer)

        getButtonPosition()

        val typeface = Typeface.createFromAsset(assets, "sukar.ttf")
        txt_player_1M.typeface = typeface
        txt_player_2M.typeface = typeface
        textView2.typeface = typeface
        textView5.typeface = typeface
        textView6.typeface = typeface
        textView7.typeface = typeface

        draw = getString(R.string.its_draw)
        draw2 = getString(R.string.its_draw2)
        phoneWin = getString(R.string.phone_win)
        phoneWin2 = getString(R.string.phone_win2)
        youWin = getString(R.string.you_win)
        watchAds2 = getString(R.string.let_s_watch_an_ad2)
        theGameBecameEasier = getString(R.string.TheGameBecameEasier)
        loadingAd = getString(R.string.LoadingAd)
        canNotLoadAd = getString(R.string.CanNotLoadAd)


        //bannerAds()
        interstitialAd()

        loadRewardedVideoAd()

        txt_Help.setOnClickListener {
            showAdsDialog()
        }

    }

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
            } else if (roundCount == 9) {
                draw()
            } else {

                clickSound()
                /*
                this else is for change turn from player one to player two so
                the game check after checking that no winner and rountCount not equal 9
                that is mean there in more places"Button" to play
                */
                player1Turn = !player1Turn

                handler.postDelayed(r, 500)

                /*
                if (checkForWin()) {
                    if (player1Turn) {
                        player1Wins()
                    } else {
                        player2Wins()
                    }
                } else if (roundCount == 8) {
                    draw()
                } else {
                    player1Turn = !player1Turn
                }
                */
            }
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
        //Toast.makeText(this, "Player X wins!", Toast.LENGTH_SHORT).show()
        updatePointsText()

        for (i in 0..3) {
            for (j in 0..3) {
                buttons[i][j]?.text = "-"
            }
        }

        showXWinDialog()
        playTimeCount = 0

    }

    @SuppressLint("SetTextI18n")
    private fun player2Wins() {
        player2Points++
        updatePointsText()

        for (i in 0..3) {
            for (j in 0..3) {
                buttons[i][j]?.text = "-"
            }
        }

        showOWinDialog()
        playTimeCount++
    }

    @SuppressLint("SetTextI18n")
    private fun draw() {
        showDrawDialog()
        playTimeCount++
    }

    private fun computerTurn() {

        if ((buttons[0][2]!!.text.toString() == buttons[0][1]!!.text.toString() && buttons[0][2]!!.text.toString() == buttons[0][3]!!.text.toString() && buttons[0][2]!!.text.toString() != ""
                    || buttons[2][0]!!.text.toString() == buttons[1][0]!!.text.toString() && buttons[2][0]!!.text.toString() == buttons[3][0]!!.text.toString() && buttons[2][0]!!.text.toString() != ""
                    || buttons[2][2]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[2][2]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[2][2]!!.text.toString() != ""
                    )
            && buttons[0][0]!!.text.toString() == ""
            && buttons[0][0]!!.text.toString() != "x"
        ) {

            buttons[0][0]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[0][0]?.setText("o")!!

        } else if ((buttons[0][0]!!.text.toString() == buttons[0][1]!!.text.toString() && buttons[0][2]!!.text.toString() == buttons[0][1]!!.text.toString() && buttons[0][1]!!.text.toString() != ""
                    || buttons[2][3]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[2][3]!!.text.toString() != ""
                    || buttons[2][1]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[2][1]!!.text.toString() == buttons[3][0]!!.text.toString() && buttons[2][1]!!.text.toString() != ""
                    )
            && buttons[0][3]!!.text.toString() == ""
            && buttons[0][3]!!.text.toString() != "x"
        ) {

            buttons[0][3]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[0][3]?.setText("o")!!

        } else if ((buttons[3][1]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[3][1]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[3][1]!!.text.toString() != ""
                    || buttons[2][0]!!.text.toString() == buttons[0][0]!!.text.toString() && buttons[2][0]!!.text.toString() == buttons[1][0]!!.text.toString() && buttons[2][0]!!.text.toString() != ""
                    || buttons[2][1]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[2][1]!!.text.toString() == buttons[0][3]!!.text.toString() && buttons[2][1]!!.text.toString() != ""
                    )
            && buttons[3][0]!!.text.toString() == ""
            && buttons[3][0]!!.text.toString() != "x"
        ) {

            buttons[3][0]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[3][0]?.setText("o")!!

        } else if ((buttons[3][0]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[3][1]!!.text.toString() == buttons[3][0]!!.text.toString() && buttons[3][0]!!.text.toString() != ""
                    || buttons[0][3]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[0][3]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[0][3]!!.text.toString() != ""
                    || buttons[1][1]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[1][1]!!.text.toString() == buttons[0][0]!!.text.toString() && buttons[1][1]!!.text.toString() != ""
                    )
            && buttons[3][3]!!.text.toString() == ""
            && buttons[3][3]!!.text.toString() != "x"
        ) {

            buttons[3][3]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[3][3]?.setText("o")!!

        } else if ((buttons[0][1]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[3][1]!!.text.toString() == buttons[0][1]!!.text.toString() && buttons[0][1]!!.text.toString() != ""
                    || buttons[1][3]!!.text.toString() == buttons[1][0]!!.text.toString() && buttons[1][2]!!.text.toString() == buttons[1][0]!!.text.toString() && buttons[1][0]!!.text.toString() != ""
                    || buttons[2][2]!!.text.toString() == buttons[0][0]!!.text.toString() && buttons[2][2]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[2][2]!!.text.toString() != ""
                    )
            && buttons[1][1]!!.text.toString() == ""
            && buttons[1][1]!!.text.toString() != "x"
        ) {

            buttons[1][1]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[1][1]?.setText("o")!!

        } else if ((buttons[0][2]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[0][2]!!.text.toString() && buttons[0][2]!!.text.toString() != ""
                    || buttons[1][3]!!.text.toString() == buttons[1][0]!!.text.toString() && buttons[1][1]!!.text.toString() == buttons[1][0]!!.text.toString() && buttons[1][0]!!.text.toString() != ""
                    || buttons[2][1]!!.text.toString() == buttons[0][3]!!.text.toString() && buttons[2][1]!!.text.toString() == buttons[3][0]!!.text.toString() && buttons[2][1]!!.text.toString() != ""
                    )
            && buttons[1][2]!!.text.toString() == ""
            && buttons[1][2]!!.text.toString() != "x"
        ) {

            buttons[1][2]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[1][2]?.setText("o")!!

        } else if ((buttons[0][2]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[0][2]!!.text.toString() && buttons[0][2]!!.text.toString() != ""
                    || buttons[2][3]!!.text.toString() == buttons[2][0]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[2][3]!!.text.toString() != ""
                    || buttons[1][1]!!.text.toString() == buttons[0][0]!!.text.toString() && buttons[1][1]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[1][1]!!.text.toString() != ""
                    )
            && buttons[2][2]!!.text.toString() == ""
            && buttons[2][2]!!.text.toString() != "x"
        ) {

            buttons[2][2]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[2][2]?.setText("o")!!

        } else if ((buttons[0][1]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[3][1]!!.text.toString() == buttons[0][1]!!.text.toString() && buttons[0][1]!!.text.toString() != ""
                    || buttons[2][3]!!.text.toString() == buttons[2][0]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][3]!!.text.toString() != ""
                    || buttons[1][2]!!.text.toString() == buttons[3][0]!!.text.toString() && buttons[1][2]!!.text.toString() == buttons[0][3]!!.text.toString() && buttons[1][2]!!.text.toString() != ""
                    )
            && buttons[2][1]!!.text.toString() == ""
            && buttons[2][1]!!.text.toString() != "x"
        ) {

            buttons[2][1]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[2][1]?.setText("o")!!

        } else if ((buttons[0][2]!!.text.toString() == buttons[0][3]!!.text.toString() && buttons[0][2]!!.text.toString() == buttons[0][0]!!.text.toString() && buttons[0][2]!!.text.toString() != ""
                    || buttons[1][1]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[1][1]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[1][1]!!.text.toString() != ""
                    )
            && buttons[0][1]!!.text.toString() == ""
            && buttons[0][1]!!.text.toString() != "x"
        ) {

            buttons[0][1]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[0][1]?.setText("o")!!

        } else if ((buttons[0][1]!!.text.toString() == buttons[0][3]!!.text.toString() && buttons[0][1]!!.text.toString() == buttons[0][0]!!.text.toString() && buttons[0][1]!!.text.toString() != ""
                    || buttons[1][2]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[1][2]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[1][2]!!.text.toString() != ""
                    )
            && buttons[0][2]!!.text.toString() == ""
            && buttons[0][2]!!.text.toString() != "x"
        ) {

            buttons[0][2]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[0][2]?.setText("o")!!

        } else if ((buttons[1][1]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[1][1]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[1][1]!!.text.toString() != ""
                    || buttons[0][0]!!.text.toString() == buttons[2][0]!!.text.toString() && buttons[0][0]!!.text.toString() == buttons[3][0]!!.text.toString() && buttons[0][0]!!.text.toString() != ""
                    )
            && buttons[1][0]!!.text.toString() == ""
            && buttons[1][0]!!.text.toString() != "x"
        ) {

            buttons[1][0]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[1][0]?.setText("o")!!

        } else if ((buttons[1][1]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[1][1]!!.text.toString() == buttons[1][0]!!.text.toString() && buttons[1][1]!!.text.toString() != ""
                    || buttons[0][3]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[0][3]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[0][3]!!.text.toString() != ""
                    )
            && buttons[1][3]!!.text.toString() == ""
            && buttons[1][3]!!.text.toString() != "x"
        ) {

            buttons[1][3]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[1][3]?.setText("o")!!

        } else if ((buttons[2][1]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][1]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[2][1]!!.text.toString() != ""
                    || buttons[0][0]!!.text.toString() == buttons[1][0]!!.text.toString() && buttons[0][0]!!.text.toString() == buttons[3][0]!!.text.toString() && buttons[0][0]!!.text.toString() != ""
                    )
            && buttons[2][0]!!.text.toString() == ""
            && buttons[2][0]!!.text.toString() != "x"
        ) {

            buttons[2][0]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[2][0]?.setText("o")!!

        } else if ((buttons[2][1]!!.text.toString() == buttons[2][0]!!.text.toString() && buttons[2][1]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][1]!!.text.toString() != ""
                    || buttons[0][3]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[0][3]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[0][3]!!.text.toString() != ""
                    )
            && buttons[2][3]!!.text.toString() == ""
            && buttons[2][3]!!.text.toString() != "x"
        ) {

            buttons[2][3]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[2][3]?.setText("o")!!

        } else if ((buttons[3][0]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[3][0]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[3][0]!!.text.toString() != ""
                    || buttons[0][1]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[0][1]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[0][1]!!.text.toString() != ""
                    )
            && buttons[3][1]!!.text.toString() == ""
            && buttons[3][1]!!.text.toString() != "x"
        ) {

            buttons[3][1]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[3][1]?.setText("o")!!

        } else if ((buttons[3][0]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[3][0]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[3][0]!!.text.toString() != ""
                    || buttons[0][2]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[0][2]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[0][2]!!.text.toString() != ""
                    )
            && buttons[3][2]!!.text.toString() == ""
            && buttons[3][2]!!.text.toString() != "x"
        ) {

            buttons[3][2]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[3][2]?.setText("o")!!

        } else {

            when {
                buttons[2][1]!!.text.toString() == "" -> {
                    buttons[2][1]?.background = ContextCompat.getDrawable(
                        this,
                        R.drawable.o
                    )
                    buttons[2][1]?.setText("o")!!
                }
                buttons[1][2]!!.text.toString() == "" -> {
                    buttons[1][2]?.background = ContextCompat.getDrawable(
                        this,
                        R.drawable.o
                    )
                    buttons[1][2]?.setText("o")!!
                }
                buttons[2][0]!!.text.toString() == "" -> {
                    buttons[2][0]?.background = ContextCompat.getDrawable(
                        this,
                        R.drawable.o
                    )
                    buttons[2][0]?.setText("o")!!
                }
                buttons[1][0]!!.text.toString() == "" -> {
                    buttons[1][0]?.background = ContextCompat.getDrawable(
                        this,
                        R.drawable.o
                    )
                    buttons[1][0]?.setText("o")!!

                }
                buttons[0][3]!!.text.toString() == "" -> {
                    buttons[0][3]?.background = ContextCompat.getDrawable(
                        this,
                        R.drawable.o
                    )
                    buttons[0][3]?.setText("o")!!
                }
                buttons[3][3]!!.text.toString() == "" -> {
                    buttons[3][3]?.background = ContextCompat.getDrawable(
                        this,
                        R.drawable.o
                    )
                    buttons[3][3]?.setText("o")!!
                }
                buttons[0][1]!!.text.toString() == "" -> {
                    buttons[0][1]?.background = ContextCompat.getDrawable(
                        this,
                        R.drawable.o
                    )
                    buttons[0][1]?.setText("o")!!
                }
                buttons[0][0]!!.text.toString() == "" -> {
                    buttons[0][0]?.background = ContextCompat.getDrawable(
                        this,
                        R.drawable.o
                    )
                    buttons[0][0]?.setText("o")!!
                }
                buttons[1][3]!!.text.toString() == "" -> {
                    buttons[1][3]?.background = ContextCompat.getDrawable(
                        this,
                        R.drawable.o
                    )
                    buttons[1][3]?.setText("o")!!
                }
                buttons[3][1]!!.text.toString() == "" -> {
                    buttons[3][1]?.background = ContextCompat.getDrawable(
                        this,
                        R.drawable.o
                    )
                    buttons[3][1]?.setText("o")!!
                }
                buttons[2][2]!!.text.toString() == "" -> {
                    buttons[2][2]?.background = ContextCompat.getDrawable(
                        this,
                        R.drawable.o
                    )
                    buttons[2][2]?.setText("o")!!
                }
                buttons[3][2]!!.text.toString() == "" -> {
                    buttons[3][2]?.background = ContextCompat.getDrawable(
                        this,
                        R.drawable.o
                    )
                    buttons[3][2]?.setText("o")!!
                }
                buttons[3][0]!!.text.toString() == "" -> {
                    buttons[3][0]?.background = ContextCompat.getDrawable(
                        this,
                        R.drawable.o
                    )
                    buttons[3][0]?.setText("o")!!
                }
                buttons[1][1]!!.text.toString() == "" -> {
                    buttons[1][1]?.background = ContextCompat.getDrawable(
                        this,
                        R.drawable.o
                    )
                    buttons[1][1]?.setText("o")!!
                }
                buttons[0][2]!!.text.toString() == "" -> {
                    buttons[0][2]?.background = ContextCompat.getDrawable(
                        this,
                        R.drawable.o
                    )
                    buttons[0][2]?.setText("o")!!
                }
                buttons[2][3]!!.text.toString() == "" -> {
                    buttons[2][3]?.background = ContextCompat.getDrawable(
                        this,
                        R.drawable.o
                    )
                    buttons[2][3]?.setText("o")!!
                }
            }

        }

        if (checkForWin()) {
            if (player1Turn) {
                player1Wins()
            } else {
                player2Wins()
            }
        } else if (roundCount == 8) {
            draw()
        } else {
            player1Turn = !player1Turn
        }

        clickSound1()
        clickable = true
    }

    @SuppressLint("SetTextI18n")
    private fun updatePointsText() {
        txt_player_1M.text = player1Points.toString()
        txt_player_2M.text = player2Points.toString()
    }

    // to clear Buttons Data on screen
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

        /**
        if (playTimeCount == 1 || playTimeCount == 4 || playTimeCount == 7) {
        if (mInterstitialAd.isLoaded) {
        mInterstitialAd.show()
        } else {
        Log.d("TAG", "The interstitial wasn't loaded yet.")
        }
        }*/
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
        adView.adUnitId = "ca-app-pub-4454440016331822/1558961582"
        //for test: ca-app-pub-3940256099942544/6300978111
        // for real: ca-app-pub-4454440016331822/1558961582
    }

    private fun interstitialAd() {
        mInterstitialAd = InterstitialAd(this)
        mInterstitialAd.adUnitId = "ca-app-pub-3940256099942544/1033173712"
        // for test: ca-app-pub-3940256099942544/1033173712
        // for real: ca-app-pub-4454440016331822/6500297073
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


    /************************* Start of reward ads ************************/

    private fun loadRewardedVideoAd() {
        MobileAds.initialize(this, "ca-app-pub-4454440016331822~9464823022")

        mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(this)
        mRewardedVideoAd.rewardedVideoAdListener = this

        mRewardedVideoAd.loadAd(
            "ca-app-pub-3940256099942544/5224354917",
            AdRequest.Builder().build()
        )
        // real Reward ads: ca-app-pub-4454440016331822/7531570468
    }


    override fun onRewarded(reward: RewardItem) {
        //Toast.makeText(this, "onRewarded! currency: ${reward.type} amount: ${reward.amount}", Toast.LENGTH_SHORT).show()
        // Reward the user.
        if (adWatched1) {
            adWatched2 = true
        } else {
            adWatched1 = true
        }
    }

    override fun onRewardedVideoAdLeftApplication() {
        //Toast.makeText(this, "onRewardedVideoAdLeftApplication", Toast.LENGTH_SHORT).show()
    }

    override fun onRewardedVideoAdClosed() {
        //Toast.makeText(this, "onRewardedVideoAdClosed", Toast.LENGTH_SHORT).show()
        loadRewardedVideoAd()

    }

    override fun onRewardedVideoAdFailedToLoad(errorCode: Int) {
        //Toast.makeText(this, "onRewardedVideoAdFailedToLoad", Toast.LENGTH_SHORT).show()
    }

    override fun onRewardedVideoAdLoaded() {
        //Toast.makeText(this, "onRewardedVideoAdLoaded", Toast.LENGTH_SHORT).show()
    }

    override fun onRewardedVideoAdOpened() {
        //Toast.makeText(this, "onRewardedVideoAdOpened", Toast.LENGTH_SHORT).show()
    }

    override fun onRewardedVideoStarted() {
        //Toast.makeText(this, "onRewardedVideoStarted", Toast.LENGTH_SHORT).show()
    }

    override fun onRewardedVideoCompleted() {
        //Toast.makeText(this, "onRewardedVideoCompleted", Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        mRewardedVideoAd.pause(this)
    }

    override fun onResume() {
        super.onResume()
        mRewardedVideoAd.resume(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        mRewardedVideoAd.destroy(this)
    }

    /************************* End of reward ads ************************/
    @SuppressLint("InflateParams")
    private fun showXWinDialog() {
        val view = LayoutInflater.from(this@MediumLevelVsComputer)
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
        val view = LayoutInflater.from(this@MediumLevelVsComputer)
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
        val view = LayoutInflater.from(this@MediumLevelVsComputer)
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
        val view = LayoutInflater.from(this@MediumLevelVsComputer)
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

        if (adWatched1) {
            view.adsText.text = watchAds2
        }

        view.watchAd.setOnClickListener {
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
            dialog.dismiss()
        }

        dialog.show()
    }



}
