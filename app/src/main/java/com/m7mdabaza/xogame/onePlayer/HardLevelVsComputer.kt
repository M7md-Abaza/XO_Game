package com.m7mdabaza.xogame.onePlayer

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.media.MediaPlayer
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.Window
import android.widget.Button
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.*
import com.google.android.gms.ads.reward.RewardItem
import com.m7mdabaza.xogame.R
import kotlinx.android.synthetic.main.activity_hard_level_vs_computer.*
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.reward.RewardedVideoAd
import com.google.android.gms.ads.reward.RewardedVideoAdListener
import kotlinx.android.synthetic.main.rewaed_ads_pop_up.view.*
import kotlinx.android.synthetic.main.win_pop_up_dialog.view.*

class HardLevelVsComputer : AppCompatActivity(), View.OnClickListener, RewardedVideoAdListener {

    private lateinit var mRewardedVideoAd: RewardedVideoAd
    private lateinit var mInterstitialAd: InterstitialAd


    private val buttons: Array<Array<Button?>> =
        Array(8) { arrayOfNulls<Button>(8) }

    private var player1Turn = true
    private var clickable = true
    private var adWatched1 = false
    private var adWatched2 = false

    private var roundCount = 0      // to determine Draw Case
    private var playTimeCount = 0   // to determine the computer Turn pattern
    private var displayAdsCount = 0   // to displayAds after certain number

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
        setContentView(R.layout.activity_hard_level_vs_computer)

        val typeface = Typeface.createFromAsset(assets, "sukar.ttf")
        txt_player_1H.typeface = typeface
        txt_player_2H.typeface = typeface
        textView.typeface = typeface
        textView2.typeface = typeface
        textView3.typeface = typeface
        textView6.typeface = typeface
        textView7.typeface = typeface
        txt_Help.typeface = typeface

        draw = getString(R.string.its_draw)
        draw2 = getString(R.string.its_draw2)
        phoneWin = getString(R.string.phone_win)
        phoneWin2 = getString(R.string.phone_win2)
        youWin = getString(R.string.you_win)
        watchAds2 = getString(R.string.let_s_watch_an_ad2)
        theGameBecameEasier = getString(R.string.TheGameBecameEasier)
        loadingAd = getString(R.string.LoadingAd)
        canNotLoadAd = getString(R.string.CanNotLoadAd)

        getButtonPosition()

        bannerAds()
        interstitialAd()
        loadRewardedVideoAd()

        txt_Help.setOnClickListener {
            showAdsDialog()
        }


    }

    override fun onClick(v: View) {
        if (clickable && (v as Button).text.toString() == "") {
            clickable = false
            if ((v).text.toString() != "") {
                return
            }
            v.background = ContextCompat.getDrawable(
                this,
                R.drawable.x
            )
            v.text = "x"
            roundCount++

            if (checkForWin()) {
                if (player1Turn) {
                    player1Wins()
                } else {
                    player2Wins()
                }
            } else if (roundCount == 64) {
                draw()
            } else {
                clickSound()
                /*
                this else is for change turn from player one to player two so
                the game check after checking that no winner and rountCount not equal 9
                that is mean there in more places"Button" to play
                */
                handler.postDelayed(r, 1300)

            }

        }
    }

    private fun checkForWin(): Boolean {

        val field = Array(8) { arrayOfNulls<String>(8) }
        // Next for_loop using for put buttons[][] array values to field[][] array
        for (i in 0..7) {
            for (j in 0..7) {
                field[i][j] = buttons[i][j]!!.text.toString()
            }
        }
        // Next for_loop using to check the only first five buttons in one row "horizontal" are equal or not
        for (i in 0..7) {
            if (field[i][3] == field[i][0] && field[i][3] == field[i][1] && field[i][3] == field[i][2] && field[i][3] == field[i][4] && field[i][3] != ""
                || field[i][3] == field[i][1] && field[i][3] == field[i][2] && field[i][3] == field[i][4] && field[i][3] == field[i][5] && field[i][3] != ""
                || field[i][3] == field[i][2] && field[i][3] == field[i][4] && field[i][3] == field[i][5] && field[i][3] == field[i][6] && field[i][3] != ""
                || field[i][3] == field[i][4] && field[i][3] == field[i][5] && field[i][3] == field[i][6] && field[i][3] == field[i][7] && field[i][3] != ""
            ) {
                return true
            }
        }
        // Next for_loop using to check the only first five buttons in each column "vertical" are equal or not
        for (i in 0..7) {
            if (field[3][i] == field[0][i] && field[3][i] == field[1][i] && field[3][i] == field[2][i] && field[3][i] == field[4][i] && field[3][i] != ""
                || field[3][i] == field[1][i] && field[3][i] == field[2][i] && field[3][i] == field[4][i] && field[3][i] == field[5][i] && field[3][i] != ""
                || field[3][i] == field[2][i] && field[3][i] == field[4][i] && field[3][i] == field[5][i] && field[3][i] == field[6][i] && field[3][i] != ""
                || field[3][i] == field[4][i] && field[3][i] == field[5][i] && field[3][i] == field[6][i] && field[3][i] == field[7][i] && field[3][i] != ""
            ) {
                return true
            }
        }
        // Next for_loop using to check the buttons from top_left to bottom_right are equal or not
        if (field[0][0] == field[1][1] && field[0][0] == field[2][2] && field[0][0] == field[3][3] && field[0][0] == field[4][4] && field[0][0] != ""
            || field[1][1] == field[2][2] && field[1][1] == field[3][3] && field[1][1] == field[4][4] && field[1][1] == field[5][5] && field[1][1] != ""
            || field[2][2] == field[3][3] && field[2][2] == field[4][4] && field[2][2] == field[5][5] && field[2][2] == field[6][6] && field[2][2] != ""
            || field[3][3] == field[4][4] && field[3][3] == field[5][5] && field[3][3] == field[6][6] && field[3][3] == field[7][7] && field[3][3] != ""
            || field[3][1] == field[4][2] && field[3][1] == field[5][3] && field[3][1] == field[6][4] && field[3][1] == field[7][5] && field[3][1] != ""
            || field[1][0] == field[2][1] && field[1][0] == field[3][2] && field[1][0] == field[4][3] && field[1][0] == field[5][4] && field[1][0] != ""
            || field[2][1] == field[3][2] && field[2][1] == field[4][3] && field[2][1] == field[5][4] && field[2][1] == field[6][5] && field[2][1] != ""
            || field[3][2] == field[4][3] && field[3][2] == field[5][4] && field[3][2] == field[6][5] && field[3][2] == field[7][6] && field[3][2] != ""
            || field[3][0] == field[4][1] && field[3][0] == field[5][2] && field[3][0] == field[6][3] && field[3][0] == field[7][4] && field[3][0] != ""
            || field[2][0] == field[3][1] && field[2][0] == field[4][2] && field[2][0] == field[5][3] && field[2][0] == field[6][4] && field[2][0] != ""
            || field[0][3] == field[1][4] && field[0][3] == field[2][5] && field[0][3] == field[3][6] && field[0][3] == field[4][7] && field[0][3] != ""
            || field[0][2] == field[1][3] && field[0][2] == field[2][4] && field[0][2] == field[3][5] && field[0][2] == field[4][6] && field[0][2] != ""
            || field[0][1] == field[1][2] && field[0][1] == field[2][3] && field[0][1] == field[3][4] && field[0][1] == field[4][5] && field[0][1] != ""
            || field[1][3] == field[2][4] && field[1][3] == field[3][5] && field[1][3] == field[4][6] && field[1][3] == field[5][7] && field[1][3] != ""
            || field[1][2] == field[2][3] && field[1][2] == field[3][4] && field[1][2] == field[4][5] && field[1][2] == field[5][6] && field[1][2] != ""
            || field[2][3] == field[3][4] && field[2][3] == field[4][5] && field[2][3] == field[5][6] && field[2][3] == field[6][7] && field[2][3] != ""
        ) {
            return true
        }
        // Next last return using to check the buttons from top_right to bottom_left are equal or not
        if (field[3][4] == field[0][7] && field[3][4] == field[1][6] && field[3][4] == field[2][5] && field[3][4] == field[4][3] && field[3][4] != ""
            || field[3][4] == field[1][6] && field[3][4] == field[2][5] && field[3][4] == field[4][3] && field[3][4] == field[5][2] && field[3][4] != ""
            || field[3][4] == field[2][5] && field[3][4] == field[4][3] && field[3][4] == field[5][2] && field[3][4] == field[6][1] && field[3][4] != ""
            || field[3][4] == field[4][3] && field[3][4] == field[5][2] && field[3][4] == field[6][1] && field[3][4] == field[7][0] && field[3][4] != ""
            || field[2][4] == field[0][6] && field[2][4] == field[1][5] && field[2][4] == field[3][3] && field[2][4] == field[4][2] && field[2][4] != ""
            || field[2][4] == field[1][5] && field[2][4] == field[3][3] && field[2][4] == field[4][2] && field[2][4] == field[5][1] && field[2][4] != ""
            || field[2][4] == field[3][3] && field[2][4] == field[4][2] && field[2][4] == field[5][1] && field[2][4] == field[6][0] && field[2][4] != ""
            || field[1][4] == field[0][5] && field[1][4] == field[2][3] && field[1][4] == field[3][2] && field[1][4] == field[4][1] && field[1][4] != ""
            || field[1][4] == field[2][3] && field[1][4] == field[3][2] && field[1][4] == field[4][1] && field[1][4] == field[5][0] && field[1][4] != ""
            || field[0][4] == field[1][3] && field[0][4] == field[2][2] && field[0][4] == field[3][1] && field[0][4] == field[4][0] && field[0][4] != ""
            || field[3][7] == field[4][6] && field[3][7] == field[5][5] && field[3][7] == field[6][4] && field[3][7] == field[7][3] && field[3][7] != ""
            || field[3][6] == field[2][7] && field[3][6] == field[4][5] && field[3][6] == field[5][4] && field[3][6] == field[6][3] && field[3][6] != ""
            || field[3][6] == field[4][5] && field[3][6] == field[5][4] && field[3][6] == field[6][3] && field[3][6] == field[7][2] && field[3][6] != ""
            || field[3][5] == field[1][7] && field[3][5] == field[2][6] && field[3][5] == field[4][4] && field[3][5] == field[5][3] && field[3][5] != ""
            || field[3][5] == field[2][6] && field[3][5] == field[4][4] && field[3][5] == field[5][3] && field[3][5] == field[6][2] && field[3][5] != ""
            || field[3][5] == field[4][4] && field[3][5] == field[5][3] && field[3][5] == field[6][2] && field[3][5] == field[7][1] && field[3][5] != ""
        ) {
            return true
        }
        return false
    }

    private fun player1Wins() {
        player1Points++
        updatePointsText()
        for (i in 0..7) {
            for (j in 0..7) {
                buttons[i][j]?.text = "-"
            }
        }

        showXWinDialog()
        playTimeCount++
        displayAdsCount++
        adWatched1 = false
        adWatched2 = false
    }

    private fun player2Wins() {
        player2Points++
        updatePointsText()
        for (i in 0..7) {
            for (j in 0..7) {
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

        oFourInRow()
        // when reward ads watched in Second time "adWatched2 == true"
        if (player1Turn && !adWatched2) {
            xFourInRow()
        }
        if (player1Turn) {
            xHorizontalVertical()
        }
        if (player1Turn) {
            topLiftBottomRightX()
        }
        if (player1Turn) {
            topRightBottomLiftX()
        }
        if (player1Turn) {
            xTwoByTwo()
        }
        // when reward ads watched in first time "adWatched1 == true"
        if (player1Turn && !adWatched1) {
            xTwoHorizontalVerticalMain()
        }
        if (player1Turn) {
            oHorizontalVertical()
        }
        if (player1Turn) {
            topLiftBottomRightO()
        }
        if (player1Turn) {
            topRightBottomLiftO()
        }
        if (player1Turn) {
            oTwoByTwo()
        }
        if (player1Turn) {
            xOneTrueOneFalseHorizontalVertical()
        }

        // for change computer turn patterns to play
        if (player1Turn) {
            if ((player1Turn && playTimeCount == 0) || (player1Turn && playTimeCount == 3) || (player1Turn && playTimeCount == 6)) {
                ifNothingToDoMain1()
            }
            if ((player1Turn && playTimeCount == 1) || (player1Turn && playTimeCount == 4) || (player1Turn && playTimeCount == 7)) {
                ifNothingToDoMain2()
            }
            if ((player1Turn && playTimeCount == 2) || (player1Turn && playTimeCount == 5) || (player1Turn && playTimeCount == 8)) {
                ifNothingToDoMain3()
                if (playTimeCount == 8) {
                    playTimeCount = 0
                }
            }
        }
        if (player1Turn) {
            if ((player1Turn && playTimeCount == 0) || (player1Turn && playTimeCount == 4)) {
                if (player1Turn) {
                    ifNothingToDO1()
                }
                if (player1Turn) {
                    ifNothingToDO2()
                }
                if (player1Turn) {
                    ifNothingToDO3()
                }
                if (player1Turn) {
                    ifNothingToDO4()
                }
            } else if ((player1Turn && playTimeCount == 1) || (player1Turn && playTimeCount == 5) || (player1Turn && playTimeCount == 8)) {
                if (player1Turn) {
                    ifNothingToDO3()
                }
                if (player1Turn) {
                    ifNothingToDO1()
                }
                if (player1Turn) {
                    ifNothingToDO4()
                }
                if (player1Turn) {
                    ifNothingToDO2()
                }
                if (playTimeCount == 8) {
                    playTimeCount = 0
                }
            } else if ((player1Turn && playTimeCount == 2) || (player1Turn && playTimeCount == 6)) {
                if (player1Turn) {
                    ifNothingToDO2()
                }
                if (player1Turn) {
                    ifNothingToDO3()
                }
                if (player1Turn) {
                    ifNothingToDO1()
                }
                if (player1Turn) {
                    ifNothingToDO4()
                }
            } else if ((player1Turn && playTimeCount == 3) || (player1Turn && playTimeCount == 7)) {
                if (player1Turn) {
                    ifNothingToDO4()
                }
                if (player1Turn) {
                    ifNothingToDO2()
                }
                if (player1Turn) {
                    ifNothingToDO3()
                }
                if (player1Turn) {
                    ifNothingToDO1()
                }
            }
        }

        roundCount++

        if (checkForWin()) {
            if (player1Turn) {
                player1Wins()
            } else {
                player2Wins()
            }
        } else if (roundCount == 64) {
            draw()
        } else {
            player1Turn = !player1Turn
        }

        clickable = true

        clickSound1()
    }

    @SuppressLint("SetTextI18n")
    private fun updatePointsText() {
        txt_player_1H.text = player1Points.toString()
        txt_player_2H.text = player2Points.toString()
    }

    // to clear Buttons screen
    @SuppressLint("NewApi")
    private fun resetBoard() {
        for (i in 0..7) {
            for (j in 0..7) {
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

        if (displayAdsCount == 1 || displayAdsCount == 3 || displayAdsCount == 6 || displayAdsCount == 9 || displayAdsCount == 12 || displayAdsCount == 15) {
            if (mInterstitialAd.isLoaded) {
                mInterstitialAd.show()
            } else {
                Log.d("TAG", "The interstitial wasn't loaded yet.")
            }
        } else if (displayAdsCount == 16) {
            displayAdsCount = 0
        }
    }

    // to get the Button position
    private fun getButtonPosition() {
        for (i in 0..7) {
            for (j in 0..7) {
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


    /*************** Start of all Methods for computer Turn Algorithm *******************/
    // check for x on Horizontal and Vertical to prevent get 4 X in row
    /*** Done ***/
    private fun xHorizontalVertical() {
        for (i in 0..7) {
            /******************************* Horizontal Lines********************************/
            if ((buttons[i][1]!!.text.toString() == buttons[i][2]!!.text.toString() && buttons[i][1]!!.text.toString() == buttons[i][3]!!.text.toString() && buttons[i][1]!!.text.toString() != "" && buttons[i][4]!!.text.toString() != "o")
                && buttons[i][1]!!.text.toString() == "x"
                && buttons[i][0]!!.text.toString() == ""
                && buttons[i][0]!!.text.toString() != "x"
            ) {

                buttons[i][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[i][0]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[i][3]!!.text.toString() == buttons[i][2]!!.text.toString() && buttons[i][4]!!.text.toString() == buttons[i][3]!!.text.toString() && buttons[i][3]!!.text.toString() != "" && buttons[i][4]!!.text.toString() != "o"
                        || buttons[i][3]!!.text.toString() == buttons[i][2]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][0]!!.text.toString() && buttons[i][3]!!.text.toString() != "" && buttons[i][5]!!.text.toString() != "o")
                && buttons[i][3]!!.text.toString() == "x"
                && buttons[i][1]!!.text.toString() == ""
                && buttons[i][1]!!.text.toString() != "x"
            ) {

                buttons[i][1]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[i][1]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[i][3]!!.text.toString() == buttons[i][1]!!.text.toString() && buttons[i][0]!!.text.toString() == buttons[i][3]!!.text.toString() && buttons[i][3]!!.text.toString() != "" && buttons[i][4]!!.text.toString() != "o"
                        || buttons[i][3]!!.text.toString() == buttons[i][1]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][3]!!.text.toString() != "" && buttons[i][5]!!.text.toString() != "o"
                        || buttons[i][3]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][5]!!.text.toString() && buttons[i][3]!!.text.toString() != "" && buttons[i][6]!!.text.toString() != "o"
                        )
                && buttons[i][3]!!.text.toString() == "x"
                && buttons[i][2]!!.text.toString() == ""
                && buttons[i][2]!!.text.toString() != "x"
            ) {

                buttons[i][2]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[i][2]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[i][0]!!.text.toString() == buttons[i][1]!!.text.toString() && buttons[i][1]!!.text.toString() == buttons[i][2]!!.text.toString() && buttons[i][1]!!.text.toString() != "" && buttons[i][1]!!.text.toString() == "x" && buttons[i][4]!!.text.toString() != "o"
                        || buttons[i][4]!!.text.toString() == buttons[i][1]!!.text.toString() && buttons[i][1]!!.text.toString() == buttons[i][2]!!.text.toString() && buttons[i][1]!!.text.toString() != "" && buttons[i][1]!!.text.toString() == "x" && buttons[i][5]!!.text.toString() != "o"
                        || buttons[i][5]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][4]!!.text.toString() == buttons[i][6]!!.text.toString() && buttons[i][4]!!.text.toString() != "" && buttons[i][4]!!.text.toString() == "x" && buttons[i][2]!!.text.toString() != "o"
                        || buttons[i][5]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][4]!!.text.toString() == buttons[i][2]!!.text.toString() && buttons[i][4]!!.text.toString() != "" && buttons[i][4]!!.text.toString() == "x" && buttons[i][6]!!.text.toString() != "o"
                        )
                && buttons[i][3]!!.text.toString() == ""
                && buttons[i][3]!!.text.toString() != "x"
            ) {

                buttons[i][3]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[i][3]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[i][3]!!.text.toString() == buttons[i][1]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][2]!!.text.toString() && buttons[i][3]!!.text.toString() != "" && buttons[i][3]!!.text.toString() == "x" && buttons[i][5]!!.text.toString() != "o"
                        || buttons[i][3]!!.text.toString() == buttons[i][2]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][5]!!.text.toString() && buttons[i][3]!!.text.toString() != "" && buttons[i][3]!!.text.toString() == "x" && buttons[i][6]!!.text.toString() != "o"
                        || buttons[i][3]!!.text.toString() == buttons[i][6]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][5]!!.text.toString() && buttons[i][3]!!.text.toString() != "" && buttons[i][5]!!.text.toString() == "x" && buttons[i][2]!!.text.toString() != "o"
                        || buttons[i][5]!!.text.toString() == buttons[i][6]!!.text.toString() && buttons[i][5]!!.text.toString() == buttons[i][7]!!.text.toString() && buttons[i][5]!!.text.toString() != "" && buttons[i][5]!!.text.toString() == "x" && buttons[i][3]!!.text.toString() != "o"
                        )
                && buttons[i][4]!!.text.toString() == ""
                && buttons[i][4]!!.text.toString() != "x"
            ) {

                buttons[i][4]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[i][4]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[i][3]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][4]!!.text.toString() == buttons[i][2]!!.text.toString() && buttons[i][4]!!.text.toString() != "" && buttons[i][1]!!.text.toString() != "o"
                        || buttons[i][6]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][4]!!.text.toString() != "" && buttons[i][2]!!.text.toString() != "o"
                        || buttons[i][6]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][7]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][4]!!.text.toString() != "" && buttons[i][3]!!.text.toString() != "o"
                        )
                && buttons[i][4]!!.text.toString() == "x"
                && buttons[i][5]!!.text.toString() == ""
                && buttons[i][5]!!.text.toString() != "x"
            ) {

                buttons[i][5]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[i][5]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[i][3]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][4]!!.text.toString() == buttons[i][5]!!.text.toString() && buttons[i][4]!!.text.toString() != "" && buttons[i][2]!!.text.toString() != "o"
                        || buttons[i][7]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][5]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][4]!!.text.toString() != "" && buttons[i][3]!!.text.toString() != "o"
                        )
                && buttons[i][4]!!.text.toString() == "x"
                && buttons[i][6]!!.text.toString() == ""
                && buttons[i][6]!!.text.toString() != "x"
            ) {

                buttons[i][6]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[i][6]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[i][6]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][4]!!.text.toString() == buttons[i][5]!!.text.toString() && buttons[i][4]!!.text.toString() != "" && buttons[i][3]!!.text.toString() != "o"
                        )
                && buttons[i][4]!!.text.toString() == "x"
                && buttons[i][7]!!.text.toString() == ""
                && buttons[i][7]!!.text.toString() != "x"
            ) {

                buttons[i][7]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[i][7]?.setText("o")!!
                player1Turn = !player1Turn
                break

            }

            /******************************* Vertical Lines********************************/

            else if ((buttons[1][i]!!.text.toString() == buttons[2][i]!!.text.toString() && buttons[1][i]!!.text.toString() == buttons[3][i]!!.text.toString() && buttons[1][i]!!.text.toString() != "" && buttons[4][i]!!.text.toString() != "o")
                && buttons[1][i]!!.text.toString() == "x"
                && buttons[0][i]!!.text.toString() == ""
                && buttons[0][i]!!.text.toString() != "x"
            ) {

                buttons[0][i]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[0][i]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[3][i]!!.text.toString() == buttons[2][i]!!.text.toString() && buttons[4][i]!!.text.toString() == buttons[3][i]!!.text.toString() && buttons[3][i]!!.text.toString() != "" && buttons[5][i]!!.text.toString() != "o"
                        || buttons[3][i]!!.text.toString() == buttons[2][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[0][i]!!.text.toString() && buttons[3][i]!!.text.toString() != "" && buttons[4][i]!!.text.toString() != "o")
                && buttons[3][i]!!.text.toString() == "x"
                && buttons[1][i]!!.text.toString() == ""
                && buttons[1][i]!!.text.toString() != "x"
            ) {

                buttons[1][i]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[1][i]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[3][i]!!.text.toString() == buttons[1][i]!!.text.toString() && buttons[0][i]!!.text.toString() == buttons[3][i]!!.text.toString() && buttons[3][i]!!.text.toString() != "" && buttons[4][i]!!.text.toString() != "o"
                        || buttons[3][i]!!.text.toString() == buttons[1][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[3][i]!!.text.toString() != "" && buttons[5][i]!!.text.toString() != "o"
                        || buttons[3][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[5][i]!!.text.toString() && buttons[3][i]!!.text.toString() != "" && buttons[6][i]!!.text.toString() != "o"
                        )
                && buttons[3][i]!!.text.toString() == "x"
                && buttons[2][i]!!.text.toString() == ""
                && buttons[2][i]!!.text.toString() != "x"
            ) {

                buttons[2][i]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[2][i]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[0][i]!!.text.toString() == buttons[1][i]!!.text.toString() && buttons[1][i]!!.text.toString() == buttons[2][i]!!.text.toString() && buttons[1][i]!!.text.toString() != "" && buttons[1][i]!!.text.toString() == "x" && buttons[4][i]!!.text.toString() != "o"
                        || buttons[4][i]!!.text.toString() == buttons[1][i]!!.text.toString() && buttons[1][i]!!.text.toString() == buttons[2][i]!!.text.toString() && buttons[1][i]!!.text.toString() != "" && buttons[1][i]!!.text.toString() == "x" && buttons[0][i]!!.text.toString() != "o"
                        || buttons[5][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[4][i]!!.text.toString() == buttons[6][i]!!.text.toString() && buttons[4][i]!!.text.toString() != "" && buttons[4][i]!!.text.toString() == "x" && buttons[7][i]!!.text.toString() != "o"
                        || buttons[5][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[4][i]!!.text.toString() == buttons[2][i]!!.text.toString() && buttons[4][i]!!.text.toString() != "" && buttons[4][i]!!.text.toString() == "x" && buttons[6][i]!!.text.toString() != "o"
                        )
                && buttons[3][i]!!.text.toString() == ""
                && buttons[3][i]!!.text.toString() != "x"
            ) {

                buttons[3][i]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[3][i]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[3][i]!!.text.toString() == buttons[1][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[2][i]!!.text.toString() && buttons[3][i]!!.text.toString() != "" && buttons[3][i]!!.text.toString() == "x" && buttons[0][i]!!.text.toString() != "o"
                        || buttons[3][i]!!.text.toString() == buttons[2][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[5][i]!!.text.toString() && buttons[3][i]!!.text.toString() != "" && buttons[3][i]!!.text.toString() == "x" && buttons[1][i]!!.text.toString() != "o"
                        || buttons[3][i]!!.text.toString() == buttons[6][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[5][i]!!.text.toString() && buttons[3][i]!!.text.toString() != "" && buttons[3][i]!!.text.toString() == "x" && buttons[7][i]!!.text.toString() != "o"
                        || buttons[5][i]!!.text.toString() == buttons[6][i]!!.text.toString() && buttons[5][i]!!.text.toString() == buttons[7][i]!!.text.toString() && buttons[5][i]!!.text.toString() != "" && buttons[5][i]!!.text.toString() == "x" && buttons[3][i]!!.text.toString() != "o"
                        )
                && buttons[4][i]!!.text.toString() == ""
                && buttons[4][i]!!.text.toString() != "x"
            ) {

                buttons[4][i]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[4][i]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[3][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[4][i]!!.text.toString() == buttons[2][i]!!.text.toString() && buttons[4][i]!!.text.toString() != "" && buttons[1][i]!!.text.toString() != "o"
                        || buttons[6][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[4][i]!!.text.toString() != "" && buttons[2][i]!!.text.toString() != "o"
                        || buttons[6][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[7][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[4][i]!!.text.toString() != "" && buttons[3][i]!!.text.toString() != "o"
                        )
                && buttons[4][i]!!.text.toString() == "x"
                && buttons[5][i]!!.text.toString() == ""
                && buttons[5][i]!!.text.toString() != "x"
            ) {

                buttons[5][i]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[5][i]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[3][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[4][i]!!.text.toString() == buttons[5][i]!!.text.toString() && buttons[4][i]!!.text.toString() != "" && buttons[2][i]!!.text.toString() != "o"
                        || buttons[7][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[5][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[4][i]!!.text.toString() != "" && buttons[3][i]!!.text.toString() != "o"
                        )
                && buttons[4][i]!!.text.toString() == "x"
                && buttons[6][i]!!.text.toString() == ""
                && buttons[6][i]!!.text.toString() != "x"
            ) {

                buttons[6][i]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[6][i]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[6][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[4][i]!!.text.toString() == buttons[5][i]!!.text.toString() && buttons[4][i]!!.text.toString() != "" && buttons[3][i]!!.text.toString() != "o")
                && buttons[4][i]!!.text.toString() == "x"
                && buttons[7][i]!!.text.toString() == ""
                && buttons[7][i]!!.text.toString() != "x"
            ) {

                buttons[7][i]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[7][i]?.setText("o")!!
                player1Turn = !player1Turn
                break

            }
        }
    }

    // check for x from TopLift to BottomRight to prevent get 4 X in row
    /*** Done ***/
    private fun topLiftBottomRightX() {
        /****************************-1-********************************/
        if ((buttons[1][4]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[1][4]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[1][4]!!.text.toString() != "" && buttons[4][7]!!.text.toString() != "o")
            && buttons[1][4]!!.text.toString() == "x"
            && buttons[0][3]!!.text.toString() == ""
            && buttons[0][3]!!.text.toString() != "x"
        ) {

            buttons[0][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[0][3]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[2][5]!!.text.toString() != "" //&& buttons[4][7]!!.text.toString() != "o"
                    || buttons[4][7]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[2][5]!!.text.toString() != "" //&& buttons[0][3]!!.text.toString() != "o"
                    )
            && buttons[2][5]!!.text.toString() == "x"
            && buttons[1][4]!!.text.toString() == ""
            && buttons[1][4]!!.text.toString() != "x"
        ) {

            buttons[1][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][6]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[0][3]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[3][6]!!.text.toString() != "" //&& buttons[4][7]!!.text.toString() != "o"
                    || buttons[4][7]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[1][4]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[3][6]!!.text.toString() != "" //&& buttons[0][3]!!.text.toString() != "o"
                    )
            && buttons[3][6]!!.text.toString() == "x"
            && buttons[2][5]!!.text.toString() == ""
            && buttons[2][5]!!.text.toString() != "x"
        ) {

            buttons[2][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[0][3]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[2][5]!!.text.toString() != "" //&& buttons[4][7]!!.text.toString() != "o"
                    || buttons[4][7]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[2][5]!!.text.toString() != "" //&& buttons[0][3]!!.text.toString() != "o"
                    )
            && buttons[2][5]!!.text.toString() == "x"
            && buttons[3][6]!!.text.toString() == ""
            && buttons[3][6]!!.text.toString() != "x"
        ) {

            buttons[3][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][6]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[2][5]!!.text.toString() != "" && buttons[0][3]!!.text.toString() != "o")
            && buttons[2][5]!!.text.toString() == "x"
            && buttons[4][7]!!.text.toString() == ""
            && buttons[4][7]!!.text.toString() != "x"
        ) {

            buttons[4][7]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][7]?.setText("o")!!
            player1Turn = !player1Turn

        }
        /*****************************-2-*******************************/
        else if ((buttons[1][3]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[1][3]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[1][3]!!.text.toString() != "" && buttons[4][6]!!.text.toString() != "o")
            && buttons[1][3]!!.text.toString() == "x"
            && buttons[0][2]!!.text.toString() == ""
            && buttons[0][2]!!.text.toString() != "x"
        ) {

            buttons[0][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][4]!!.text.toString() == buttons[0][2]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[2][4]!!.text.toString() != "" //&& buttons[4][6]!!.text.toString() != "o"
                    || buttons[2][4]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[2][4]!!.text.toString() != "" && buttons[5][7]!!.text.toString() != "o")
            && buttons[2][4]!!.text.toString() == "x"
            && buttons[1][3]!!.text.toString() == ""
            && buttons[1][3]!!.text.toString() != "x"
        ) {

            buttons[1][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][5]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[0][2]!!.text.toString() && buttons[3][5]!!.text.toString() != "" //&& buttons[4][6]!!.text.toString() != "o"
                    || buttons[3][5]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[3][5]!!.text.toString() != "" //&& buttons[5][7]!!.text.toString() != "o"
                    || buttons[3][5]!!.text.toString() == buttons[5][7]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[3][5]!!.text.toString() != "" //&& buttons[1][3]!!.text.toString() != "o"
                    )
            && buttons[3][5]!!.text.toString() == "x"
            && buttons[2][4]!!.text.toString() == ""
            && buttons[2][4]!!.text.toString() != "x"
        ) {

            buttons[2][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[0][2]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[2][4]!!.text.toString() != "" //&& buttons[4][6]!!.text.toString() != "o"
                    || buttons[2][4]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[2][4]!!.text.toString() != "" //&& buttons[0][2]!!.text.toString() != "o"
                    || buttons[2][4]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[5][7]!!.text.toString() && buttons[2][4]!!.text.toString() != "" //&& buttons[1][3]!!.text.toString() != "o"
                    )
            && buttons[2][4]!!.text.toString() == "x"
            && buttons[3][5]!!.text.toString() == ""
            && buttons[3][5]!!.text.toString() != "x"
        ) {

            buttons[3][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][5]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[3][5]!!.text.toString() != "" && buttons[0][2]!!.text.toString() != "o"
                    || buttons[3][5]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[5][7]!!.text.toString() && buttons[3][5]!!.text.toString() != ""
                    )
            && buttons[3][5]!!.text.toString() == "x"
            && buttons[4][6]!!.text.toString() == ""
            && buttons[4][6]!!.text.toString() != "x"
        ) {

            buttons[4][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][6]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[4][6]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[4][6]!!.text.toString() != "" && buttons[1][3]!!.text.toString() != "o")
            && buttons[4][6]!!.text.toString() == "x"
            && buttons[5][7]!!.text.toString() == ""
            && buttons[5][7]!!.text.toString() != "x"
        ) {

            buttons[5][7]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][7]?.setText("o")!!
            player1Turn = !player1Turn

        }

        /*************************-3-**************************/
        else if ((buttons[1][2]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[1][2]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[1][2]!!.text.toString() != "" && buttons[4][5]!!.text.toString() != "o")
            && buttons[1][2]!!.text.toString() == "x"
            && buttons[0][1]!!.text.toString() == ""
            && buttons[0][1]!!.text.toString() != "x"
        ) {

            buttons[0][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][3]!!.text.toString() == buttons[0][1]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[2][3]!!.text.toString() != ""
                    || buttons[2][3]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[2][3]!!.text.toString() != "" && buttons[5][6]!!.text.toString() != "o")
            && buttons[2][3]!!.text.toString() == "x"
            && buttons[1][2]!!.text.toString() == ""
            && buttons[1][2]!!.text.toString() != "x"
        ) {

            buttons[1][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][4]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[0][1]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    || buttons[3][4]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    || buttons[3][4]!!.text.toString() == buttons[5][6]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[3][4]!!.text.toString() != "" && buttons[6][7]!!.text.toString() != "o"
                    )
            && buttons[3][4]!!.text.toString() == "x"
            && buttons[2][3]!!.text.toString() == ""
            && buttons[2][3]!!.text.toString() != "x"
        ) {

            buttons[2][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[0][1]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[2][3]!!.text.toString() != "" && buttons[2][3]!!.text.toString() == "x"
                    || buttons[2][3]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[2][3]!!.text.toString() != "" && buttons[2][3]!!.text.toString() == "x"
                    || buttons[2][3]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[5][6]!!.text.toString() && buttons[2][3]!!.text.toString() != "" && buttons[2][3]!!.text.toString() == "x"
                    || buttons[6][7]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[4][5]!!.text.toString() == buttons[5][6]!!.text.toString() && buttons[4][5]!!.text.toString() != "" && buttons[4][5]!!.text.toString() == "x"
                    )
            && buttons[3][4]!!.text.toString() == ""
            && buttons[3][4]!!.text.toString() != "x"
        ) {

            buttons[3][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][4]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][4]!!.text.toString() != "" && buttons[0][1]!!.text.toString() != "o"
                    || buttons[3][4]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[5][6]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    || buttons[3][4]!!.text.toString() == buttons[6][7]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[5][6]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    )
            && buttons[3][4]!!.text.toString() == "x"
            && buttons[4][5]!!.text.toString() == ""
            && buttons[4][5]!!.text.toString() != "x"
        ) {

            buttons[4][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][4]!!.text.toString() != "" && buttons[1][2]!!.text.toString() != "o"
                    || buttons[3][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[6][7]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    )
            && buttons[3][4]!!.text.toString() == "x"
            && buttons[5][6]!!.text.toString() == ""
            && buttons[5][6]!!.text.toString() != "x"
        ) {

            buttons[5][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][5]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[5][6]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[3][4]!!.text.toString() != "" && buttons[2][3]!!.text.toString() != "o")
            && buttons[3][4]!!.text.toString() == "x"
            && buttons[6][7]!!.text.toString() == ""
            && buttons[6][7]!!.text.toString() != "x"
        ) {

            buttons[6][7]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][7]?.setText("o")!!
            player1Turn = !player1Turn

        }

        /*************************-4-***************************/

        else if ((buttons[1][1]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[1][1]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[1][1]!!.text.toString() != "" && buttons[4][4]!!.text.toString() != "o")
            && buttons[1][1]!!.text.toString() == "x"
            && buttons[0][0]!!.text.toString() == ""
            && buttons[0][0]!!.text.toString() != "x"
        ) {

            buttons[0][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][0]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][3]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[0][0]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    || buttons[3][3]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[3][3]!!.text.toString() != "" && buttons[5][5]!!.text.toString() != "o"
                    )
            && buttons[3][3]!!.text.toString() == "x"
            && buttons[1][1]!!.text.toString() == ""
            && buttons[1][1]!!.text.toString() != "x"
        ) {

            buttons[1][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][3]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[0][0]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    || buttons[3][3]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[1][1]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    || buttons[3][3]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[3][3]!!.text.toString() != "" && buttons[6][6]!!.text.toString() != "o"
                    )
            && buttons[3][3]!!.text.toString() == "x"
            && buttons[2][2]!!.text.toString() == ""
            && buttons[2][2]!!.text.toString() != "x"
        ) {

            buttons[2][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][2]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[0][0]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() != "" && buttons[2][2]!!.text.toString() == "x"
                    || buttons[2][2]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[1][1]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() != "" && buttons[2][2]!!.text.toString() == "x"
                    || buttons[2][2]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() != "" && buttons[2][2]!!.text.toString() == "x"
                    || buttons[5][5]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[6][6]!!.text.toString() && buttons[6][6]!!.text.toString() != "" && buttons[6][6]!!.text.toString() == "x" && buttons[7][7]!!.text.toString() != "o"
                    )
            && buttons[3][3]!!.text.toString() == ""
            && buttons[3][3]!!.text.toString() != "x"
        ) {

            buttons[3][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][2]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() != "" && buttons[2][2]!!.text.toString() == "x" && buttons[0][0]!!.text.toString() != "o"
                    || buttons[2][2]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() != "" && buttons[2][2]!!.text.toString() == "x"
                    || buttons[5][5]!!.text.toString() == buttons[6][6]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[5][5]!!.text.toString() != "" && buttons[5][5]!!.text.toString() == "x"
                    || buttons[5][5]!!.text.toString() == buttons[6][6]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[7][7]!!.text.toString() && buttons[5][5]!!.text.toString() != "" && buttons[5][5]!!.text.toString() == "x"
                    )
            && buttons[4][4]!!.text.toString() == ""
            && buttons[4][4]!!.text.toString() != "x"
        ) {

            buttons[4][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][2]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[4][4]!!.text.toString() != "" && buttons[1][1]!!.text.toString() != "o"
                    || buttons[6][6]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    || buttons[6][6]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[7][7]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    )
            && buttons[4][4]!!.text.toString() == "x"
            && buttons[5][5]!!.text.toString() == ""
            && buttons[5][5]!!.text.toString() != "x"
        ) {

            buttons[5][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][5]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[4][4]!!.text.toString() != "" && buttons[2][2]!!.text.toString() != "o"
                    || buttons[5][5]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[7][7]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    )
            && buttons[4][4]!!.text.toString() == "x"
            && buttons[6][6]!!.text.toString() == ""
            && buttons[6][6]!!.text.toString() != "x"
        ) {

            buttons[6][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][5]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[6][6]!!.text.toString() && buttons[4][4]!!.text.toString() != "" && buttons[3][3]!!.text.toString() != "o")
            && buttons[4][4]!!.text.toString() == "x"
            && buttons[7][7]!!.text.toString() == ""
            && buttons[7][7]!!.text.toString() != "x"
        ) {

            buttons[7][7]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[7][7]?.setText("o")!!
            player1Turn = !player1Turn

        }
        /**********************-5-*************************/
        else if ((buttons[2][1]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[2][1]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[2][1]!!.text.toString() != "" && buttons[5][4]!!.text.toString() != "o")
            && buttons[2][1]!!.text.toString() == "x"
            && buttons[1][0]!!.text.toString() == ""
            && buttons[1][0]!!.text.toString() != "x"
        ) {

            buttons[1][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][0]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][2]!!.text.toString() == buttons[1][0]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[3][2]!!.text.toString() != ""
                    || buttons[3][2]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[3][2]!!.text.toString() != "" && buttons[6][5]!!.text.toString() != "o")
            && buttons[3][2]!!.text.toString() == "x"
            && buttons[2][1]!!.text.toString() == ""
            && buttons[2][1]!!.text.toString() != "x"
        ) {

            buttons[2][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][3]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[1][0]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    || buttons[4][3]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    || buttons[4][3]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[4][3]!!.text.toString() != "" && buttons[7][6]!!.text.toString() != "o"
                    )
            && buttons[4][3]!!.text.toString() == "x"
            && buttons[3][2]!!.text.toString() == ""
            && buttons[3][2]!!.text.toString() != "x"
        ) {

            buttons[3][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[1][0]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[3][2]!!.text.toString() != "" && buttons[3][2]!!.text.toString() == "x"
                    || buttons[3][2]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[3][2]!!.text.toString() != "" && buttons[3][2]!!.text.toString() == "x"
                    || buttons[3][2]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[3][2]!!.text.toString() != "" && buttons[3][2]!!.text.toString() == "x"
                    || buttons[7][6]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[5][4]!!.text.toString() != "" && buttons[5][4]!!.text.toString() == "x"
                    )
            && buttons[4][3]!!.text.toString() == ""
            && buttons[4][3]!!.text.toString() != "x"
        ) {

            buttons[4][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][3]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[4][3]!!.text.toString() != "" && buttons[1][0]!!.text.toString() != "o"
                    || buttons[4][3]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    || buttons[4][3]!!.text.toString() == buttons[7][6]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    )
            && buttons[4][3]!!.text.toString() == "x"
            && buttons[5][4]!!.text.toString() == ""
            && buttons[5][4]!!.text.toString() != "x"
        ) {

            buttons[5][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][3]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[4][3]!!.text.toString() != "" && buttons[2][1]!!.text.toString() != "o"
                    || buttons[4][3]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[7][6]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    )
            && buttons[4][3]!!.text.toString() == "x"
            && buttons[6][5]!!.text.toString() == ""
            && buttons[6][5]!!.text.toString() != "x"
        ) {

            buttons[6][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][4]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[6][5]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[4][3]!!.text.toString() != "" && buttons[3][2]!!.text.toString() != "o")
            && buttons[4][3]!!.text.toString() == "x"
            && buttons[7][6]!!.text.toString() == ""
            && buttons[7][6]!!.text.toString() != "x"
        ) {

            buttons[7][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[7][6]?.setText("o")!!
            player1Turn = !player1Turn

        }
        /***********************-6-************************/
        else if ((buttons[3][1]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[3][1]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[3][1]!!.text.toString() != "" && buttons[6][4]!!.text.toString() != "o")
            && buttons[3][1]!!.text.toString() == "x"
            && buttons[2][0]!!.text.toString() == ""
            && buttons[2][0]!!.text.toString() != "x"
        ) {

            buttons[2][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][0]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][2]!!.text.toString() == buttons[2][0]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[4][2]!!.text.toString() != ""
                    || buttons[4][2]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[4][2]!!.text.toString() != "" && buttons[7][5]!!.text.toString() != "o")
            && buttons[4][2]!!.text.toString() == "x"
            && buttons[3][1]!!.text.toString() == ""
            && buttons[3][1]!!.text.toString() != "x"
        ) {

            buttons[3][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][3]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[2][0]!!.text.toString() && buttons[5][3]!!.text.toString() != ""
                    || buttons[5][3]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[5][3]!!.text.toString() != ""
                    || buttons[5][3]!!.text.toString() == buttons[7][5]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[5][3]!!.text.toString() != ""
                    )
            && buttons[5][3]!!.text.toString() == "x"
            && buttons[4][2]!!.text.toString() == ""
            && buttons[4][2]!!.text.toString() != "x"
        ) {

            buttons[4][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][0]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[4][2]!!.text.toString() != ""
                    || buttons[4][2]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[4][2]!!.text.toString() != ""
                    || buttons[4][2]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[7][5]!!.text.toString() && buttons[4][2]!!.text.toString() != ""
                    )
            && buttons[4][2]!!.text.toString() == "x"
            && buttons[5][3]!!.text.toString() == ""
            && buttons[5][3]!!.text.toString() != "x"
        ) {

            buttons[5][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][3]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[5][3]!!.text.toString() != "" && buttons[2][0]!!.text.toString() != "o"
                    || buttons[5][3]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[7][5]!!.text.toString() && buttons[5][3]!!.text.toString() != ""
                    )
            && buttons[5][3]!!.text.toString() == "x"
            && buttons[6][4]!!.text.toString() == ""
            && buttons[6][4]!!.text.toString() != "x"
        ) {

            buttons[6][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[6][4]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[6][4]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[6][4]!!.text.toString() != "" && buttons[3][1]!!.text.toString() != "o")
            && buttons[6][4]!!.text.toString() == "x"
            && buttons[7][5]!!.text.toString() == ""
            && buttons[7][5]!!.text.toString() != "x"
        ) {

            buttons[7][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[7][5]?.setText("o")!!
            player1Turn = !player1Turn

        }
        /***********************-7-************************/
        else if ((buttons[4][1]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[4][1]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[4][1]!!.text.toString() != "" && buttons[7][4]!!.text.toString() != "o")
            && buttons[4][1]!!.text.toString() == "x"
            && buttons[3][0]!!.text.toString() == ""
            && buttons[3][0]!!.text.toString() != "x"
        ) {

            buttons[3][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][0]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][0]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[5][2]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[5][2]!!.text.toString() != ""
                    || buttons[7][4]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[5][2]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[5][2]!!.text.toString() != ""
                    )
            && buttons[5][2]!!.text.toString() == "x"
            && buttons[4][1]!!.text.toString() == ""
            && buttons[4][1]!!.text.toString() != "x"
        ) {

            buttons[4][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[6][3]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[3][0]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[6][3]!!.text.toString() != ""
                    || buttons[7][4]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[4][1]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[6][3]!!.text.toString() != ""
                    )
            && buttons[6][3]!!.text.toString() == "x"
            && buttons[5][2]!!.text.toString() == ""
            && buttons[5][2]!!.text.toString() != "x"
        ) {

            buttons[5][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][0]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[5][2]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[5][2]!!.text.toString() != ""
                    || buttons[7][4]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[5][2]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[5][2]!!.text.toString() != ""
                    )
            && buttons[5][2]!!.text.toString() == "x"
            && buttons[6][3]!!.text.toString() == ""
            && buttons[6][3]!!.text.toString() != "x"
        ) {

            buttons[6][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[6][3]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[5][2]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[5][2]!!.text.toString() != "" && buttons[3][0]!!.text.toString() != "o")
            && buttons[5][2]!!.text.toString() == "x"
            && buttons[7][4]!!.text.toString() == ""
            && buttons[7][4]!!.text.toString() != "x"
        ) {

            buttons[7][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[7][4]?.setText("o")!!
            player1Turn = !player1Turn

        }
    }

    // check for x from TopRight to BottomLift to prevent get 4 X in row
    /*** Done ***/
    private fun topRightBottomLiftX() {
        /************************-1-*********************/
        if ((buttons[3][1]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[3][1]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[3][1]!!.text.toString() != "" && buttons[4][0]!!.text.toString() != "o")
            && buttons[3][1]!!.text.toString() == "x"
            && buttons[0][4]!!.text.toString() == ""
            && buttons[0][4]!!.text.toString() != "x"
        ) {

            buttons[0][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][1]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[3][1]!!.text.toString() == buttons[0][4]!!.text.toString() && buttons[3][1]!!.text.toString() != ""
                    || buttons[3][1]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[3][1]!!.text.toString() == buttons[4][0]!!.text.toString() && buttons[3][1]!!.text.toString() != ""
                    )
            && buttons[3][1]!!.text.toString() == "x"
            && buttons[1][3]!!.text.toString() == ""
            && buttons[1][3]!!.text.toString() != "x"
        ) {

            buttons[1][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][1]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[3][1]!!.text.toString() == buttons[0][4]!!.text.toString() && buttons[3][1]!!.text.toString() != ""
                    || buttons[3][1]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[3][1]!!.text.toString() == buttons[4][0]!!.text.toString() && buttons[3][1]!!.text.toString() != ""
                    )
            && buttons[3][1]!!.text.toString() == "x"
            && buttons[2][2]!!.text.toString() == ""
            && buttons[2][2]!!.text.toString() != "x"
        ) {

            buttons[2][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][2]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[2][2]!!.text.toString() == buttons[0][4]!!.text.toString() && buttons[2][2]!!.text.toString() != ""
                    || buttons[2][2]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[2][2]!!.text.toString() == buttons[4][0]!!.text.toString() && buttons[2][2]!!.text.toString() != ""
                    )
            && buttons[2][2]!!.text.toString() == "x"
            && buttons[3][1]!!.text.toString() == ""
            && buttons[3][1]!!.text.toString() != "x"
        ) {

            buttons[3][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][2]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[2][2]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[2][2]!!.text.toString() != "" && buttons[0][4]!!.text.toString() != "o")
            && buttons[2][2]!!.text.toString() == "x"
            && buttons[4][0]!!.text.toString() == ""
            && buttons[4][0]!!.text.toString() != "x"
        ) {

            buttons[4][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][0]?.setText("o")!!
            player1Turn = !player1Turn

        }
        /*************-2-****************/
        else if ((buttons[3][2]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[3][2]!!.text.toString() != "" && buttons[4][1]!!.text.toString() != "o")
            && buttons[3][2]!!.text.toString() == "x"
            && buttons[0][5]!!.text.toString() == ""
            && buttons[0][5]!!.text.toString() != "x"
        ) {

            buttons[0][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][2]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[0][5]!!.text.toString() && buttons[3][2]!!.text.toString() != ""
                    || buttons[3][2]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[3][2]!!.text.toString() != "" && buttons[5][0]!!.text.toString() != "o"
                    )
            && buttons[3][2]!!.text.toString() == "x"
            && buttons[1][4]!!.text.toString() == ""
            && buttons[1][4]!!.text.toString() != "x"
        ) {

            buttons[1][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][2]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[0][5]!!.text.toString() && buttons[3][2]!!.text.toString() != ""
                    || buttons[3][2]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[3][2]!!.text.toString() != ""
                    || buttons[3][2]!!.text.toString() == buttons[5][0]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[3][2]!!.text.toString() != ""
                    )
            && buttons[3][2]!!.text.toString() == "x"
            && buttons[2][3]!!.text.toString() == ""
            && buttons[2][3]!!.text.toString() != "x"
        ) {

            buttons[2][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][3]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[0][5]!!.text.toString() && buttons[2][3]!!.text.toString() != ""
                    || buttons[2][3]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[2][3]!!.text.toString() != ""
                    || buttons[2][3]!!.text.toString() == buttons[5][0]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[2][3]!!.text.toString() != ""
                    )
            && buttons[2][3]!!.text.toString() == "x"
            && buttons[3][2]!!.text.toString() == ""
            && buttons[3][2]!!.text.toString() != "x"
        ) {

            buttons[3][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][3]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[2][3]!!.text.toString() != "" && buttons[0][5]!!.text.toString() != "o"
                    || buttons[2][3]!!.text.toString() == buttons[5][0]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[2][3]!!.text.toString() != ""
                    )
            && buttons[2][3]!!.text.toString() == "x"
            && buttons[4][1]!!.text.toString() == ""
            && buttons[4][1]!!.text.toString() != "x"
        ) {

            buttons[4][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][2]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[3][2]!!.text.toString() != "" && buttons[1][4]!!.text.toString() != "o")
            && buttons[3][2]!!.text.toString() == "x"
            && buttons[5][0]!!.text.toString() == ""
            && buttons[5][0]!!.text.toString() != "x"
        ) {

            buttons[5][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][0]?.setText("o")!!
            player1Turn = !player1Turn

        }
        /*************-3-****************/
        else if ((buttons[3][3]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[1][5]!!.text.toString() && buttons[3][3]!!.text.toString() != "" && buttons[4][2]!!.text.toString() != "o")
            && buttons[3][3]!!.text.toString() == "x"
            && buttons[0][6]!!.text.toString() == ""
            && buttons[0][6]!!.text.toString() != "x"
        ) {

            buttons[0][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][3]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[0][6]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    || buttons[3][3]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[3][3]!!.text.toString() != "" && buttons[5][1]!!.text.toString() != "o"
                    )
            && buttons[3][3]!!.text.toString() == "x"
            && buttons[1][5]!!.text.toString() == ""
            && buttons[1][5]!!.text.toString() != "x"
        ) {

            buttons[1][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][3]!!.text.toString() == buttons[1][5]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[0][6]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    || buttons[3][3]!!.text.toString() == buttons[1][5]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    || buttons[3][3]!!.text.toString() == buttons[5][1]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[3][3]!!.text.toString() != "" && buttons[6][0]!!.text.toString() != "o"
                    )
            && buttons[3][3]!!.text.toString() == "x"
            && buttons[2][4]!!.text.toString() == ""
            && buttons[2][4]!!.text.toString() != "x"
        ) {

            buttons[2][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][4]!!.text.toString() == buttons[1][5]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[0][6]!!.text.toString() && buttons[2][4]!!.text.toString() != ""
                    || buttons[2][4]!!.text.toString() == buttons[1][5]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[2][4]!!.text.toString() != ""
                    || buttons[2][4]!!.text.toString() == buttons[5][1]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[2][4]!!.text.toString() != ""
                    || buttons[2][4]!!.text.toString() == buttons[5][1]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[6][0]!!.text.toString() && buttons[2][4]!!.text.toString() != ""
                    )
            && buttons[2][4]!!.text.toString() == "x"
            && buttons[3][3]!!.text.toString() == ""
            && buttons[3][3]!!.text.toString() != "x"
        ) {

            buttons[3][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][3]!!.text.toString() == buttons[1][5]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[3][3]!!.text.toString() != "" && buttons[0][6]!!.text.toString() != "o"
                    || buttons[3][3]!!.text.toString() == buttons[5][1]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    || buttons[3][3]!!.text.toString() == buttons[5][1]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[6][0]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    )
            && buttons[3][3]!!.text.toString() == "x"
            && buttons[4][2]!!.text.toString() == ""
            && buttons[4][2]!!.text.toString() != "x"
        ) {

            buttons[4][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][3]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[3][3]!!.text.toString() != "" && buttons[1][5]!!.text.toString() != "o"
                    || buttons[3][3]!!.text.toString() == buttons[6][0]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    )
            && buttons[3][3]!!.text.toString() == "x"
            && buttons[5][1]!!.text.toString() == ""
            && buttons[5][1]!!.text.toString() != "x"
        ) {

            buttons[5][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][3]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[5][1]!!.text.toString() && buttons[3][3]!!.text.toString() != "" && buttons[2][4]!!.text.toString() != "o")
            && buttons[3][3]!!.text.toString() == "x"
            && buttons[6][0]!!.text.toString() == ""
            && buttons[6][0]!!.text.toString() != "x"
        ) {

            buttons[6][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][0]?.setText("o")!!
            player1Turn = !player1Turn

        }
        /******************-4-*****************/
        else if ((buttons[3][4]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[1][6]!!.text.toString() && buttons[3][4]!!.text.toString() != "" && buttons[4][3]!!.text.toString() != "o")
            && buttons[3][4]!!.text.toString() == "x"
            && buttons[0][7]!!.text.toString() == ""
            && buttons[0][7]!!.text.toString() != "x"
        ) {

            buttons[0][7]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][7]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][4]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[0][7]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    || buttons[3][4]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[3][4]!!.text.toString() != "" && buttons[5][2]!!.text.toString() != "o"
                    )
            && buttons[3][4]!!.text.toString() == "x"
            && buttons[1][6]!!.text.toString() == ""
            && buttons[1][6]!!.text.toString() != "x"
        ) {

            buttons[1][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][4]!!.text.toString() == buttons[1][6]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[0][7]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    || buttons[3][4]!!.text.toString() == buttons[1][6]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    || buttons[3][4]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[3][4]!!.text.toString() != "" && buttons[6][1]!!.text.toString() != "o"
                    )
            && buttons[3][4]!!.text.toString() == "x"
            && buttons[2][5]!!.text.toString() == ""
            && buttons[2][5]!!.text.toString() != "x"
        ) {

            buttons[2][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][5]!!.text.toString() == buttons[1][6]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[0][7]!!.text.toString() && buttons[2][5]!!.text.toString() != "" && buttons[2][5]!!.text.toString() == "x"
                    || buttons[2][5]!!.text.toString() == buttons[1][6]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[2][5]!!.text.toString() != "" && buttons[2][5]!!.text.toString() == "x"
                    || buttons[2][5]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[2][5]!!.text.toString() != "" && buttons[2][5]!!.text.toString() == "x"
                    || buttons[6][1]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[6][1]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[6][1]!!.text.toString() != "" && buttons[6][1]!!.text.toString() == "x" && buttons[7][0]!!.text.toString() != "o"
                    )
            && buttons[3][4]!!.text.toString() == ""
            && buttons[3][4]!!.text.toString() != "x"
        ) {

            buttons[3][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][4]!!.text.toString() == buttons[1][6]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[3][4]!!.text.toString() != "" && buttons[3][4]!!.text.toString() == "x" && buttons[0][7]!!.text.toString() != "o"
                    || buttons[3][4]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[3][4]!!.text.toString() != "" && buttons[3][4]!!.text.toString() == "x"
                    || buttons[3][4]!!.text.toString() == buttons[6][1]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[3][4]!!.text.toString() != "" && buttons[3][4]!!.text.toString() == "x"
                    || buttons[7][0]!!.text.toString() == buttons[6][1]!!.text.toString() && buttons[7][0]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[7][0]!!.text.toString() != "" && buttons[7][0]!!.text.toString() == "x"
                    )
            && buttons[4][3]!!.text.toString() == ""
            && buttons[4][3]!!.text.toString() != "x"
        ) {

            buttons[4][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][3]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[4][3]!!.text.toString() != "" && buttons[1][6]!!.text.toString() != "o"
                    || buttons[4][3]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[6][1]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    || buttons[4][3]!!.text.toString() == buttons[7][0]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[6][1]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    )
            && buttons[4][3]!!.text.toString() == "x"
            && buttons[5][2]!!.text.toString() == ""
            && buttons[5][2]!!.text.toString() != "x"
        ) {

            buttons[5][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][3]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[4][3]!!.text.toString() != "" && buttons[2][5]!!.text.toString() != "o"
                    || buttons[4][3]!!.text.toString() == buttons[7][0]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    )
            && buttons[4][3]!!.text.toString() == "x"
            && buttons[6][1]!!.text.toString() == ""
            && buttons[6][1]!!.text.toString() != "x"
        ) {

            buttons[6][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][3]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[6][1]!!.text.toString() && buttons[4][3]!!.text.toString() != "" && buttons[3][4]!!.text.toString() != "o")
            && buttons[4][3]!!.text.toString() == "x"
            && buttons[7][0]!!.text.toString() == ""
            && buttons[7][0]!!.text.toString() != "x"
        ) {

            buttons[7][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[7][0]?.setText("o")!!
            player1Turn = !player1Turn

        }
        /******************-5-*****************/

        else if ((buttons[4][4]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[2][6]!!.text.toString() && buttons[4][4]!!.text.toString() != "" && buttons[5][3]!!.text.toString() != "o")
            && buttons[4][4]!!.text.toString() == "x"
            && buttons[1][7]!!.text.toString() == ""
            && buttons[1][7]!!.text.toString() != "x"
        ) {

            buttons[1][7]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][7]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][4]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[1][7]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    || buttons[4][4]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[4][4]!!.text.toString() != "" && buttons[6][2]!!.text.toString() != "o"
                    )
            && buttons[4][4]!!.text.toString() == "x"
            && buttons[2][6]!!.text.toString() == ""
            && buttons[2][6]!!.text.toString() != "x"
        ) {

            buttons[2][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][4]!!.text.toString() == buttons[2][6]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[1][7]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    || buttons[4][4]!!.text.toString() == buttons[2][6]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    || buttons[4][4]!!.text.toString() == buttons[6][2]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[4][4]!!.text.toString() != "" && buttons[7][1]!!.text.toString() != "o"
                    )
            && buttons[4][4]!!.text.toString() == "x"
            && buttons[3][5]!!.text.toString() == ""
            && buttons[3][5]!!.text.toString() != "x"
        ) {

            buttons[3][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][5]!!.text.toString() == buttons[2][6]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[1][7]!!.text.toString() && buttons[3][5]!!.text.toString() != "" && buttons[3][5]!!.text.toString() == "x"
                    || buttons[3][5]!!.text.toString() == buttons[2][6]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[3][5]!!.text.toString() != "" && buttons[3][5]!!.text.toString() == "x"
                    || buttons[3][5]!!.text.toString() == buttons[6][2]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[3][5]!!.text.toString() != "" && buttons[3][5]!!.text.toString() == "x"
                    || buttons[7][1]!!.text.toString() == buttons[6][2]!!.text.toString() && buttons[7][1]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[7][1]!!.text.toString() != "" && buttons[7][1]!!.text.toString() == "x"
                    )
            && buttons[4][4]!!.text.toString() == ""
            && buttons[4][4]!!.text.toString() != "x"
        ) {

            buttons[4][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][4]!!.text.toString() == buttons[2][6]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[4][4]!!.text.toString() != "" && buttons[1][7]!!.text.toString() != "o"
                    || buttons[4][4]!!.text.toString() == buttons[6][2]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    || buttons[4][4]!!.text.toString() == buttons[6][2]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[7][1]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    )
            && buttons[4][4]!!.text.toString() == "x"
            && buttons[5][3]!!.text.toString() == ""
            && buttons[5][3]!!.text.toString() != "x"
        ) {

            buttons[5][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][4]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[4][4]!!.text.toString() != "" && buttons[2][6]!!.text.toString() != "o"
                    || buttons[4][4]!!.text.toString() == buttons[7][1]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    )
            && buttons[4][4]!!.text.toString() == "x"
            && buttons[6][2]!!.text.toString() == ""
            && buttons[6][2]!!.text.toString() != "x"
        ) {

            buttons[6][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][4]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[6][2]!!.text.toString() && buttons[4][4]!!.text.toString() != "" && buttons[2][5]!!.text.toString() != "o")
            && buttons[4][4]!!.text.toString() == "x"
            && buttons[7][1]!!.text.toString() == ""
            && buttons[7][1]!!.text.toString() != "x"
        ) {

            buttons[7][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[7][1]?.setText("o")!!
            player1Turn = !player1Turn

        }

        /******************-6-*****************/

        else if ((buttons[5][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[5][4]!!.text.toString() != "" && buttons[6][3]!!.text.toString() != "o")
            && buttons[5][4]!!.text.toString() == "x"
            && buttons[2][7]!!.text.toString() == ""
            && buttons[2][7]!!.text.toString() != "x"
        ) {

            buttons[2][7]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][7]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[2][7]!!.text.toString() && buttons[5][4]!!.text.toString() != ""
                    || buttons[5][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[5][4]!!.text.toString() != "" && buttons[7][2]!!.text.toString() != "o"
                    )
            && buttons[5][4]!!.text.toString() == "x"
            && buttons[3][6]!!.text.toString() == ""
            && buttons[3][6]!!.text.toString() != "x"
        ) {

            buttons[3][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][4]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[2][7]!!.text.toString() && buttons[5][4]!!.text.toString() != ""
                    || buttons[5][4]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[5][4]!!.text.toString() != ""
                    || buttons[5][4]!!.text.toString() == buttons[7][2]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[5][4]!!.text.toString() != ""
                    )
            && buttons[5][4]!!.text.toString() == "x"
            && buttons[4][5]!!.text.toString() == ""
            && buttons[4][5]!!.text.toString() != "x"
        ) {

            buttons[5][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][5]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[4][5]!!.text.toString() == buttons[2][7]!!.text.toString() && buttons[4][5]!!.text.toString() != ""
                    || buttons[4][5]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[4][5]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[4][5]!!.text.toString() != ""
                    || buttons[4][5]!!.text.toString() == buttons[7][2]!!.text.toString() && buttons[4][5]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[4][5]!!.text.toString() != ""
                    )
            && buttons[4][5]!!.text.toString() == "x"
            && buttons[5][4]!!.text.toString() == ""
            && buttons[5][4]!!.text.toString() != "x"
        ) {

            buttons[5][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][5]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[4][5]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[4][5]!!.text.toString() != "" && buttons[2][7]!!.text.toString() != "o"
                    || buttons[4][5]!!.text.toString() == buttons[7][2]!!.text.toString() && buttons[4][5]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[4][5]!!.text.toString() != ""
                    )
            && buttons[4][5]!!.text.toString() == "x"
            && buttons[6][3]!!.text.toString() == ""
            && buttons[6][3]!!.text.toString() != "x"
        ) {

            buttons[6][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[5][4]!!.text.toString() != "" && buttons[3][6]!!.text.toString() != "o")
            && buttons[5][4]!!.text.toString() == "x"
            && buttons[7][2]!!.text.toString() == ""
            && buttons[7][2]!!.text.toString() != "x"
        ) {

            buttons[7][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[7][2]?.setText("o")!!
            player1Turn = !player1Turn

        }

        /******************-7-*****************/

        else if ((buttons[6][4]!!.text.toString() == buttons[5][5]!!.text.toString() && buttons[6][4]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[6][4]!!.text.toString() != "" && buttons[7][3]!!.text.toString() != "o")
            && buttons[6][4]!!.text.toString() == "x"
            && buttons[3][7]!!.text.toString() == ""
            && buttons[3][7]!!.text.toString() != "x"
        ) {

            buttons[3][7]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][7]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[6][4]!!.text.toString() == buttons[5][5]!!.text.toString() && buttons[6][4]!!.text.toString() == buttons[3][7]!!.text.toString() && buttons[6][4]!!.text.toString() != ""
                    || buttons[6][4]!!.text.toString() == buttons[5][5]!!.text.toString() && buttons[6][4]!!.text.toString() == buttons[7][3]!!.text.toString() && buttons[6][4]!!.text.toString() != ""
                    )
            && buttons[6][4]!!.text.toString() == "x"
            && buttons[4][6]!!.text.toString() == ""
            && buttons[4][6]!!.text.toString() != "x"
        ) {

            buttons[4][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[6][4]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[6][4]!!.text.toString() == buttons[3][7]!!.text.toString() && buttons[6][4]!!.text.toString() != ""
                    || buttons[6][4]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[6][4]!!.text.toString() == buttons[7][3]!!.text.toString() && buttons[6][4]!!.text.toString() != ""
                    )
            && buttons[6][4]!!.text.toString() == "x"
            && buttons[5][5]!!.text.toString() == ""
            && buttons[5][5]!!.text.toString() != "x"
        ) {

            buttons[5][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][5]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[3][7]!!.text.toString() && buttons[5][5]!!.text.toString() != ""
                    || buttons[5][5]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[7][3]!!.text.toString() && buttons[5][5]!!.text.toString() != ""
                    )
            && buttons[5][5]!!.text.toString() == "x"
            && buttons[6][4]!!.text.toString() == ""
            && buttons[6][4]!!.text.toString() != "x"
        ) {

            buttons[6][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][5]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[5][5]!!.text.toString() != "" && buttons[3][7]!!.text.toString() != "o")
            && buttons[5][5]!!.text.toString() == "x"
            && buttons[7][3]!!.text.toString() == ""
            && buttons[7][3]!!.text.toString() != "x"
        ) {

            buttons[7][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[7][3]?.setText("o")!!
            player1Turn = !player1Turn

        }

    }

    // check for o from TopLift to BottomRight to get 4 O in row
    /*** Done ***/
    private fun topLiftBottomRightO() {
        /****************************-1-********************************/
        if ((buttons[1][4]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[1][4]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[1][4]!!.text.toString() != "")
            && buttons[1][4]!!.text.toString() == "o"
            && buttons[0][3]!!.text.toString() == ""
            && buttons[0][3]!!.text.toString() != "x"
        ) {

            buttons[0][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[0][3]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[2][5]!!.text.toString() != ""
                    || buttons[4][7]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[2][5]!!.text.toString() != ""
                    )
            && buttons[2][5]!!.text.toString() == "o"
            && buttons[1][4]!!.text.toString() == ""
            && buttons[1][4]!!.text.toString() != "x"
        ) {

            buttons[1][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][6]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[0][3]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[3][6]!!.text.toString() != ""
                    || buttons[4][7]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[1][4]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[3][6]!!.text.toString() != ""
                    )
            && buttons[3][6]!!.text.toString() == "o"
            && buttons[2][5]!!.text.toString() == ""
            && buttons[2][5]!!.text.toString() != "x"
        ) {

            buttons[2][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[0][3]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[2][5]!!.text.toString() != ""
                    || buttons[4][7]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[2][5]!!.text.toString() != ""
                    )
            && buttons[2][5]!!.text.toString() == "o"
            && buttons[3][6]!!.text.toString() == ""
            && buttons[3][6]!!.text.toString() != "x"
        ) {

            buttons[3][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][6]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[2][5]!!.text.toString() != "")
            && buttons[2][5]!!.text.toString() == "o"
            && buttons[4][7]!!.text.toString() == ""
            && buttons[4][7]!!.text.toString() != "x"
        ) {

            buttons[4][7]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][7]?.setText("o")!!
            player1Turn = !player1Turn

        }
        /*****************************-2-*******************************/
        else if ((buttons[1][3]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[1][3]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[1][3]!!.text.toString() != "")
            && buttons[1][3]!!.text.toString() == "o"
            && buttons[0][2]!!.text.toString() == ""
            && buttons[0][2]!!.text.toString() != "x"
        ) {

            buttons[0][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][4]!!.text.toString() == buttons[0][2]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[2][4]!!.text.toString() != ""
                    || buttons[2][4]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[2][4]!!.text.toString() != "")
            && buttons[2][4]!!.text.toString() == "o"
            && buttons[1][3]!!.text.toString() == ""
            && buttons[1][3]!!.text.toString() != "x"
        ) {

            buttons[1][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][5]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[0][2]!!.text.toString() && buttons[3][5]!!.text.toString() != ""
                    || buttons[3][5]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[3][5]!!.text.toString() != ""
                    || buttons[3][5]!!.text.toString() == buttons[5][7]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[3][5]!!.text.toString() != ""
                    )
            && buttons[3][5]!!.text.toString() == "o"
            && buttons[2][4]!!.text.toString() == ""
            && buttons[2][4]!!.text.toString() != "x"
        ) {

            buttons[2][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[0][2]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[2][4]!!.text.toString() != ""
                    || buttons[2][4]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[2][4]!!.text.toString() != ""
                    || buttons[2][4]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[5][7]!!.text.toString() && buttons[2][4]!!.text.toString() != ""
                    )
            && buttons[2][4]!!.text.toString() == "o"
            && buttons[3][5]!!.text.toString() == ""
            && buttons[3][5]!!.text.toString() != "x"
        ) {

            buttons[3][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][5]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[3][5]!!.text.toString() != ""
                    || buttons[3][5]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[5][7]!!.text.toString() && buttons[3][5]!!.text.toString() != ""
                    )
            && buttons[3][5]!!.text.toString() == "o"
            && buttons[4][6]!!.text.toString() == ""
            && buttons[4][6]!!.text.toString() != "x"
        ) {

            buttons[4][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][6]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[4][6]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[4][6]!!.text.toString() != "")
            && buttons[4][6]!!.text.toString() == "o"
            && buttons[5][7]!!.text.toString() == ""
            && buttons[5][7]!!.text.toString() != "x"
        ) {

            buttons[5][7]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][7]?.setText("o")!!
            player1Turn = !player1Turn

        }

        /*************************-3-**************************/
        else if ((buttons[1][2]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[1][2]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[1][2]!!.text.toString() != "")
            && buttons[1][2]!!.text.toString() == "o"
            && buttons[0][1]!!.text.toString() == ""
            && buttons[0][1]!!.text.toString() != "x"
        ) {

            buttons[0][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][3]!!.text.toString() == buttons[0][1]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[2][3]!!.text.toString() != ""
                    || buttons[2][3]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[2][3]!!.text.toString() != "")
            && buttons[2][3]!!.text.toString() == "o"
            && buttons[1][2]!!.text.toString() == ""
            && buttons[1][2]!!.text.toString() != "x"
        ) {

            buttons[1][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][4]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[0][1]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    || buttons[3][4]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    || buttons[3][4]!!.text.toString() == buttons[5][6]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    )
            && buttons[3][4]!!.text.toString() == "o"
            && buttons[2][3]!!.text.toString() == ""
            && buttons[2][3]!!.text.toString() != "x"
        ) {

            buttons[2][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[0][1]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[2][3]!!.text.toString() != "" && buttons[2][3]!!.text.toString() == "o"
                    || buttons[2][3]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[2][3]!!.text.toString() != "" && buttons[2][3]!!.text.toString() == "o"
                    || buttons[2][3]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[5][6]!!.text.toString() && buttons[2][3]!!.text.toString() != "" && buttons[2][3]!!.text.toString() == "o"
                    || buttons[6][7]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[4][5]!!.text.toString() == buttons[5][6]!!.text.toString() && buttons[4][5]!!.text.toString() != "" && buttons[4][5]!!.text.toString() == "o"
                    )
            && buttons[3][4]!!.text.toString() == ""
            && buttons[3][4]!!.text.toString() != "x"
        ) {

            buttons[3][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][4]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    || buttons[3][4]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[5][6]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    || buttons[3][4]!!.text.toString() == buttons[6][7]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[5][6]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    )
            && buttons[3][4]!!.text.toString() == "o"
            && buttons[4][5]!!.text.toString() == ""
            && buttons[4][5]!!.text.toString() != "x"
        ) {

            buttons[4][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    || buttons[3][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[6][7]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    )
            && buttons[3][4]!!.text.toString() == "o"
            && buttons[5][6]!!.text.toString() == ""
            && buttons[5][6]!!.text.toString() != "x"
        ) {

            buttons[5][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][5]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[5][6]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[3][4]!!.text.toString() != "")
            && buttons[3][4]!!.text.toString() == "o"
            && buttons[6][7]!!.text.toString() == ""
            && buttons[6][7]!!.text.toString() != "x"
        ) {

            buttons[6][7]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][7]?.setText("o")!!
            player1Turn = !player1Turn

        }

        /*************************-4-***************************/

        else if ((buttons[1][1]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[1][1]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[1][1]!!.text.toString() != "")
            && buttons[1][1]!!.text.toString() == "o"
            && buttons[0][0]!!.text.toString() == ""
            && buttons[0][0]!!.text.toString() != "x"
        ) {

            buttons[0][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][0]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][3]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[0][0]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    || buttons[3][3]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    )
            && buttons[3][3]!!.text.toString() == "o"
            && buttons[1][1]!!.text.toString() == ""
            && buttons[1][1]!!.text.toString() != "x"
        ) {

            buttons[1][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][3]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[0][0]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    || buttons[3][3]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[1][1]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    || buttons[3][3]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    )
            && buttons[3][3]!!.text.toString() == "o"
            && buttons[2][2]!!.text.toString() == ""
            && buttons[2][2]!!.text.toString() != "x"
        ) {

            buttons[2][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][2]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[0][0]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() != "" && buttons[2][2]!!.text.toString() == "o"
                    || buttons[2][2]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[1][1]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() != "" && buttons[2][2]!!.text.toString() == "o"
                    || buttons[2][2]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() != "" && buttons[2][2]!!.text.toString() == "o"
                    || buttons[5][5]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[6][6]!!.text.toString() && buttons[6][6]!!.text.toString() != "" && buttons[6][6]!!.text.toString() == "o"
                    )
            && buttons[3][3]!!.text.toString() == ""
            && buttons[3][3]!!.text.toString() != "x"
        ) {

            buttons[3][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][2]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() != "" && buttons[2][2]!!.text.toString() == "o"
                    || buttons[2][2]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() != "" && buttons[2][2]!!.text.toString() == "o"
                    || buttons[5][5]!!.text.toString() == buttons[6][6]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[5][5]!!.text.toString() != "" && buttons[5][5]!!.text.toString() == "o"
                    || buttons[5][5]!!.text.toString() == buttons[6][6]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[7][7]!!.text.toString() && buttons[5][5]!!.text.toString() != "" && buttons[5][5]!!.text.toString() == "o"
                    )
            && buttons[4][4]!!.text.toString() == ""
            && buttons[4][4]!!.text.toString() != "x"
        ) {

            buttons[4][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][2]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    || buttons[6][6]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    || buttons[6][6]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[7][7]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    )
            && buttons[4][4]!!.text.toString() == "o"
            && buttons[5][5]!!.text.toString() == ""
            && buttons[5][5]!!.text.toString() != "x"
        ) {

            buttons[5][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][5]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    || buttons[5][5]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[7][7]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    )
            && buttons[4][4]!!.text.toString() == "o"
            && buttons[6][6]!!.text.toString() == ""
            && buttons[6][6]!!.text.toString() != "x"
        ) {

            buttons[6][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][5]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[6][6]!!.text.toString() && buttons[4][4]!!.text.toString() != "")
            && buttons[4][4]!!.text.toString() == "o"
            && buttons[7][7]!!.text.toString() == ""
            && buttons[7][7]!!.text.toString() != "x"
        ) {

            buttons[7][7]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[7][7]?.setText("o")!!
            player1Turn = !player1Turn

        }
        /**********************-5-*************************/
        else if ((buttons[2][1]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[2][1]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[2][1]!!.text.toString() != "")
            && buttons[2][1]!!.text.toString() == "o"
            && buttons[1][0]!!.text.toString() == ""
            && buttons[1][0]!!.text.toString() != "x"
        ) {

            buttons[1][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][0]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][2]!!.text.toString() == buttons[1][0]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[3][2]!!.text.toString() != ""
                    || buttons[3][2]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[3][2]!!.text.toString() != "")
            && buttons[3][2]!!.text.toString() == "o"
            && buttons[2][1]!!.text.toString() == ""
            && buttons[2][1]!!.text.toString() != "x"
        ) {

            buttons[2][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][3]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[1][0]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    || buttons[4][3]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    || buttons[4][3]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    )
            && buttons[4][3]!!.text.toString() == "o"
            && buttons[3][2]!!.text.toString() == ""
            && buttons[3][2]!!.text.toString() != "x"
        ) {

            buttons[3][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[1][0]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[3][2]!!.text.toString() != "" && buttons[3][2]!!.text.toString() == "o"
                    || buttons[3][2]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[3][2]!!.text.toString() != "" && buttons[3][2]!!.text.toString() == "o"
                    || buttons[3][2]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[3][2]!!.text.toString() != "" && buttons[3][2]!!.text.toString() == "o"
                    || buttons[7][6]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[5][4]!!.text.toString() != "" && buttons[5][4]!!.text.toString() == "o"
                    )
            && buttons[4][3]!!.text.toString() == ""
            && buttons[4][3]!!.text.toString() != "x"
        ) {

            buttons[4][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][3]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    || buttons[4][3]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    || buttons[4][3]!!.text.toString() == buttons[7][6]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    )
            && buttons[4][3]!!.text.toString() == "o"
            && buttons[5][4]!!.text.toString() == ""
            && buttons[5][4]!!.text.toString() != "x"
        ) {

            buttons[5][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][3]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    || buttons[4][3]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[7][6]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    )
            && buttons[4][3]!!.text.toString() == "o"
            && buttons[6][5]!!.text.toString() == ""
            && buttons[6][5]!!.text.toString() != "x"
        ) {

            buttons[6][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][4]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[6][5]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[4][3]!!.text.toString() != "")
            && buttons[4][3]!!.text.toString() == "o"
            && buttons[7][6]!!.text.toString() == ""
            && buttons[7][6]!!.text.toString() != "x"
        ) {

            buttons[7][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[7][6]?.setText("o")!!
            player1Turn = !player1Turn

        }
        /***********************-6-************************/
        else if ((buttons[3][1]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[3][1]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[3][1]!!.text.toString() != "")
            && buttons[3][1]!!.text.toString() == "o"
            && buttons[2][0]!!.text.toString() == ""
            && buttons[2][0]!!.text.toString() != "x"
        ) {

            buttons[2][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][0]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][2]!!.text.toString() == buttons[2][0]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[4][2]!!.text.toString() != ""
                    || buttons[4][2]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[4][2]!!.text.toString() != "")
            && buttons[4][2]!!.text.toString() == "o"
            && buttons[3][1]!!.text.toString() == ""
            && buttons[3][1]!!.text.toString() != "x"
        ) {

            buttons[3][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][3]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[2][0]!!.text.toString() && buttons[5][3]!!.text.toString() != ""
                    || buttons[5][3]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[5][3]!!.text.toString() != ""
                    || buttons[5][3]!!.text.toString() == buttons[7][5]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[5][3]!!.text.toString() != ""
                    )
            && buttons[5][3]!!.text.toString() == "o"
            && buttons[4][2]!!.text.toString() == ""
            && buttons[4][2]!!.text.toString() != "x"
        ) {

            buttons[4][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][0]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[4][2]!!.text.toString() != ""
                    || buttons[4][2]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[4][2]!!.text.toString() != ""
                    || buttons[4][2]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[7][5]!!.text.toString() && buttons[4][2]!!.text.toString() != ""
                    )
            && buttons[4][2]!!.text.toString() == "o"
            && buttons[5][3]!!.text.toString() == ""
            && buttons[5][3]!!.text.toString() != "x"
        ) {

            buttons[5][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][3]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[5][3]!!.text.toString() != ""
                    || buttons[5][3]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[7][5]!!.text.toString() && buttons[5][3]!!.text.toString() != ""
                    )
            && buttons[5][3]!!.text.toString() == "o"
            && buttons[6][4]!!.text.toString() == ""
            && buttons[6][4]!!.text.toString() != "x"
        ) {

            buttons[6][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[6][4]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[6][4]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[6][4]!!.text.toString() != "")
            && buttons[6][4]!!.text.toString() == "o"
            && buttons[7][5]!!.text.toString() == ""
            && buttons[7][5]!!.text.toString() != "x"
        ) {

            buttons[7][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[7][5]?.setText("o")!!
            player1Turn = !player1Turn

        }
        /***********************-7-************************/
        else if ((buttons[4][1]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[4][1]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[4][1]!!.text.toString() != "")
            && buttons[4][1]!!.text.toString() == "o"
            && buttons[3][0]!!.text.toString() == ""
            && buttons[3][0]!!.text.toString() != "x"
        ) {

            buttons[3][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][0]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][0]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[5][2]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[5][2]!!.text.toString() != ""
                    || buttons[7][4]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[5][2]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[5][2]!!.text.toString() != ""
                    )
            && buttons[5][2]!!.text.toString() == "o"
            && buttons[4][1]!!.text.toString() == ""
            && buttons[4][1]!!.text.toString() != "x"
        ) {

            buttons[4][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[6][3]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[3][0]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[6][3]!!.text.toString() != ""
                    || buttons[7][4]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[4][1]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[6][3]!!.text.toString() != ""
                    )
            && buttons[6][3]!!.text.toString() == "o"
            && buttons[5][2]!!.text.toString() == ""
            && buttons[5][2]!!.text.toString() != "x"
        ) {

            buttons[5][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][0]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[5][2]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[5][2]!!.text.toString() != ""
                    || buttons[7][4]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[5][2]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[5][2]!!.text.toString() != ""
                    )
            && buttons[5][2]!!.text.toString() == "o"
            && buttons[6][3]!!.text.toString() == ""
            && buttons[6][3]!!.text.toString() != "x"
        ) {

            buttons[6][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[6][3]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[5][2]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[5][2]!!.text.toString() != "")
            && buttons[5][2]!!.text.toString() == "o"
            && buttons[7][4]!!.text.toString() == ""
            && buttons[7][4]!!.text.toString() != "x"
        ) {

            buttons[7][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[7][4]?.setText("o")!!
            player1Turn = !player1Turn

        }
    }

    // check for o from TopRight to BottomLift to get 4 O in row
    /*** Done ***/
    private fun topRightBottomLiftO() {

        /************************-1-*********************/
        if ((buttons[3][1]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[3][1]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[3][1]!!.text.toString() != "")
            && buttons[3][1]!!.text.toString() == "o"
            && buttons[0][4]!!.text.toString() == ""
            && buttons[0][4]!!.text.toString() != "x"
        ) {

            buttons[0][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][1]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[3][1]!!.text.toString() == buttons[0][4]!!.text.toString() && buttons[3][1]!!.text.toString() != ""
                    || buttons[3][1]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[3][1]!!.text.toString() == buttons[4][0]!!.text.toString() && buttons[3][1]!!.text.toString() != ""
                    )
            && buttons[3][1]!!.text.toString() == "o"
            && buttons[1][3]!!.text.toString() == ""
            && buttons[1][3]!!.text.toString() != "x"
        ) {

            buttons[1][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][1]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[3][1]!!.text.toString() == buttons[0][4]!!.text.toString() && buttons[3][1]!!.text.toString() != ""
                    || buttons[3][1]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[3][1]!!.text.toString() == buttons[4][0]!!.text.toString() && buttons[3][1]!!.text.toString() != ""
                    )
            && buttons[3][1]!!.text.toString() == "o"
            && buttons[2][2]!!.text.toString() == ""
            && buttons[2][2]!!.text.toString() != "x"
        ) {

            buttons[2][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][2]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[2][2]!!.text.toString() == buttons[0][4]!!.text.toString() && buttons[2][2]!!.text.toString() != ""
                    || buttons[2][2]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[2][2]!!.text.toString() == buttons[4][0]!!.text.toString() && buttons[2][2]!!.text.toString() != ""
                    )
            && buttons[2][2]!!.text.toString() == "o"
            && buttons[3][1]!!.text.toString() == ""
            && buttons[3][1]!!.text.toString() != "x"
        ) {

            buttons[3][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][2]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[2][2]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[2][2]!!.text.toString() != "")
            && buttons[2][2]!!.text.toString() == "o"
            && buttons[4][0]!!.text.toString() == ""
            && buttons[4][0]!!.text.toString() != "x"
        ) {

            buttons[4][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][0]?.setText("o")!!
            player1Turn = !player1Turn

        }
        /*************-2-****************/
        else if ((buttons[3][2]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[3][2]!!.text.toString() != "")
            && buttons[3][2]!!.text.toString() == "o"
            && buttons[0][5]!!.text.toString() == ""
            && buttons[0][5]!!.text.toString() != "x"
        ) {

            buttons[0][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][2]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[0][5]!!.text.toString() && buttons[3][2]!!.text.toString() != ""
                    || buttons[3][2]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[3][2]!!.text.toString() != ""
                    )
            && buttons[3][2]!!.text.toString() == "o"
            && buttons[1][4]!!.text.toString() == ""
            && buttons[1][4]!!.text.toString() != "x"
        ) {

            buttons[1][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][2]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[0][5]!!.text.toString() && buttons[3][2]!!.text.toString() != ""
                    || buttons[3][2]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[3][2]!!.text.toString() != ""
                    || buttons[3][2]!!.text.toString() == buttons[5][0]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[3][2]!!.text.toString() != ""
                    )
            && buttons[3][2]!!.text.toString() == "o"
            && buttons[2][3]!!.text.toString() == ""
            && buttons[2][3]!!.text.toString() != "x"
        ) {

            buttons[2][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][3]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[0][5]!!.text.toString() && buttons[2][3]!!.text.toString() != ""
                    || buttons[2][3]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[2][3]!!.text.toString() != ""
                    || buttons[2][3]!!.text.toString() == buttons[5][0]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[2][3]!!.text.toString() != ""
                    )
            && buttons[2][3]!!.text.toString() == "o"
            && buttons[3][2]!!.text.toString() == ""
            && buttons[3][2]!!.text.toString() != "x"
        ) {

            buttons[3][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][3]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[2][3]!!.text.toString() != ""
                    || buttons[2][3]!!.text.toString() == buttons[5][0]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[2][3]!!.text.toString() != ""
                    )
            && buttons[2][3]!!.text.toString() == "o"
            && buttons[4][1]!!.text.toString() == ""
            && buttons[4][1]!!.text.toString() != "x"
        ) {

            buttons[4][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][2]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[3][2]!!.text.toString() != "")
            && buttons[3][2]!!.text.toString() == "o"
            && buttons[5][0]!!.text.toString() == ""
            && buttons[5][0]!!.text.toString() != "x"
        ) {

            buttons[5][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][0]?.setText("o")!!
            player1Turn = !player1Turn

        }
        /*************-3-****************/
        else if ((buttons[3][3]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[1][5]!!.text.toString() && buttons[3][3]!!.text.toString() != "")
            && buttons[3][3]!!.text.toString() == "o"
            && buttons[0][6]!!.text.toString() == ""
            && buttons[0][6]!!.text.toString() != "x"
        ) {

            buttons[0][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][3]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[0][6]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    || buttons[3][3]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    )
            && buttons[3][3]!!.text.toString() == "o"
            && buttons[1][5]!!.text.toString() == ""
            && buttons[1][5]!!.text.toString() != "x"
        ) {

            buttons[1][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][3]!!.text.toString() == buttons[1][5]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[0][6]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    || buttons[3][3]!!.text.toString() == buttons[1][5]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    || buttons[3][3]!!.text.toString() == buttons[5][1]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    )
            && buttons[3][3]!!.text.toString() == "o"
            && buttons[2][4]!!.text.toString() == ""
            && buttons[2][4]!!.text.toString() != "x"
        ) {

            buttons[2][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][4]!!.text.toString() == buttons[1][5]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[0][6]!!.text.toString() && buttons[2][4]!!.text.toString() != ""
                    || buttons[2][4]!!.text.toString() == buttons[1][5]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[2][4]!!.text.toString() != ""
                    || buttons[2][4]!!.text.toString() == buttons[5][1]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[2][4]!!.text.toString() != ""
                    || buttons[2][4]!!.text.toString() == buttons[5][1]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[6][0]!!.text.toString() && buttons[2][4]!!.text.toString() != ""
                    )
            && buttons[2][4]!!.text.toString() == "o"
            && buttons[3][3]!!.text.toString() == ""
            && buttons[3][3]!!.text.toString() != "x"
        ) {

            buttons[3][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][3]!!.text.toString() == buttons[1][5]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    || buttons[3][3]!!.text.toString() == buttons[5][1]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    || buttons[3][3]!!.text.toString() == buttons[5][1]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[6][0]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    )
            && buttons[3][3]!!.text.toString() == "o"
            && buttons[4][2]!!.text.toString() == ""
            && buttons[4][2]!!.text.toString() != "x"
        ) {

            buttons[4][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][3]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    || buttons[3][3]!!.text.toString() == buttons[6][0]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    )
            && buttons[3][3]!!.text.toString() == "o"
            && buttons[5][1]!!.text.toString() == ""
            && buttons[5][1]!!.text.toString() != "x"
        ) {

            buttons[5][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][3]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[5][1]!!.text.toString() && buttons[3][3]!!.text.toString() != "")
            && buttons[3][3]!!.text.toString() == "o"
            && buttons[6][0]!!.text.toString() == ""
            && buttons[6][0]!!.text.toString() != "x"
        ) {

            buttons[6][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][0]?.setText("o")!!
            player1Turn = !player1Turn

        }
        /******************-4-*****************/
        else if ((buttons[3][4]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[1][6]!!.text.toString() && buttons[3][4]!!.text.toString() != "")
            && buttons[3][4]!!.text.toString() == "o"
            && buttons[0][7]!!.text.toString() == ""
            && buttons[0][7]!!.text.toString() != "x"
        ) {

            buttons[0][7]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][7]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][4]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[0][7]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    || buttons[3][4]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    )
            && buttons[3][4]!!.text.toString() == "o"
            && buttons[1][6]!!.text.toString() == ""
            && buttons[1][6]!!.text.toString() != "x"
        ) {

            buttons[1][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][4]!!.text.toString() == buttons[1][6]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[0][7]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    || buttons[3][4]!!.text.toString() == buttons[1][6]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    || buttons[3][4]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    )
            && buttons[3][4]!!.text.toString() == "o"
            && buttons[2][5]!!.text.toString() == ""
            && buttons[2][5]!!.text.toString() != "x"
        ) {

            buttons[2][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][5]!!.text.toString() == buttons[1][6]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[0][7]!!.text.toString() && buttons[2][5]!!.text.toString() != "" && buttons[2][5]!!.text.toString() == "o"
                    || buttons[2][5]!!.text.toString() == buttons[1][6]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[2][5]!!.text.toString() != "" && buttons[2][5]!!.text.toString() == "o"
                    || buttons[2][5]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[2][5]!!.text.toString() != "" && buttons[2][5]!!.text.toString() == "o"
                    || buttons[6][1]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[6][1]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[6][1]!!.text.toString() != "" && buttons[6][1]!!.text.toString() == "o"
                    )
            && buttons[3][4]!!.text.toString() == ""
            && buttons[3][4]!!.text.toString() != "x"
        ) {

            buttons[3][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][4]!!.text.toString() == buttons[1][6]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[3][4]!!.text.toString() != "" && buttons[3][4]!!.text.toString() == "o"
                    || buttons[3][4]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[3][4]!!.text.toString() != "" && buttons[3][4]!!.text.toString() == "o"
                    || buttons[3][4]!!.text.toString() == buttons[6][1]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[3][4]!!.text.toString() != "" && buttons[3][4]!!.text.toString() == "o"
                    || buttons[7][0]!!.text.toString() == buttons[6][1]!!.text.toString() && buttons[7][0]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[7][0]!!.text.toString() != "" && buttons[7][0]!!.text.toString() == "o"
                    )
            && buttons[4][3]!!.text.toString() == ""
            && buttons[4][3]!!.text.toString() != "x"
        ) {

            buttons[4][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][3]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    || buttons[4][3]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[6][1]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    || buttons[4][3]!!.text.toString() == buttons[7][0]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[6][1]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    )
            && buttons[4][3]!!.text.toString() == "o"
            && buttons[5][2]!!.text.toString() == ""
            && buttons[5][2]!!.text.toString() != "x"
        ) {

            buttons[5][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][3]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    || buttons[4][3]!!.text.toString() == buttons[7][0]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    )
            && buttons[4][3]!!.text.toString() == "o"
            && buttons[6][1]!!.text.toString() == ""
            && buttons[6][1]!!.text.toString() != "x"
        ) {

            buttons[6][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][3]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[6][1]!!.text.toString() && buttons[4][3]!!.text.toString() != "")
            && buttons[4][3]!!.text.toString() == "o"
            && buttons[7][0]!!.text.toString() == ""
            && buttons[7][0]!!.text.toString() != "x"
        ) {

            buttons[7][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[7][0]?.setText("o")!!
            player1Turn = !player1Turn

        }
        /******************-5-*****************/

        else if ((buttons[4][4]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[2][6]!!.text.toString() && buttons[4][4]!!.text.toString() != "")
            && buttons[4][4]!!.text.toString() == "o"
            && buttons[1][7]!!.text.toString() == ""
            && buttons[1][7]!!.text.toString() != "x"
        ) {

            buttons[1][7]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][7]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][4]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[1][7]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    || buttons[4][4]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    )
            && buttons[4][4]!!.text.toString() == "o"
            && buttons[2][6]!!.text.toString() == ""
            && buttons[2][6]!!.text.toString() != "x"
        ) {

            buttons[2][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][4]!!.text.toString() == buttons[2][6]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[1][7]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    || buttons[4][4]!!.text.toString() == buttons[2][6]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    || buttons[4][4]!!.text.toString() == buttons[6][2]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    )
            && buttons[4][4]!!.text.toString() == "o"
            && buttons[3][5]!!.text.toString() == ""
            && buttons[3][5]!!.text.toString() != "x"
        ) {

            buttons[3][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][5]!!.text.toString() == buttons[2][6]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[1][7]!!.text.toString() && buttons[3][5]!!.text.toString() != "" && buttons[3][5]!!.text.toString() == "o"
                    || buttons[3][5]!!.text.toString() == buttons[2][6]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[3][5]!!.text.toString() != "" && buttons[3][5]!!.text.toString() == "o"
                    || buttons[3][5]!!.text.toString() == buttons[6][2]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[3][5]!!.text.toString() != "" && buttons[3][5]!!.text.toString() == "o"
                    || buttons[7][1]!!.text.toString() == buttons[6][2]!!.text.toString() && buttons[7][1]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[7][1]!!.text.toString() != "" && buttons[7][1]!!.text.toString() == "o"
                    )
            && buttons[4][4]!!.text.toString() == ""
            && buttons[4][4]!!.text.toString() != "x"
        ) {

            buttons[4][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][4]!!.text.toString() == buttons[2][6]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    || buttons[4][4]!!.text.toString() == buttons[6][2]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    || buttons[4][4]!!.text.toString() == buttons[6][2]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[7][1]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    )
            && buttons[4][4]!!.text.toString() == "o"
            && buttons[5][3]!!.text.toString() == ""
            && buttons[5][3]!!.text.toString() != "x"
        ) {

            buttons[5][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][4]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    || buttons[4][4]!!.text.toString() == buttons[7][1]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    )
            && buttons[4][4]!!.text.toString() == "o"
            && buttons[6][2]!!.text.toString() == ""
            && buttons[6][2]!!.text.toString() != "x"
        ) {

            buttons[6][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][4]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[6][2]!!.text.toString() && buttons[4][4]!!.text.toString() != "")
            && buttons[4][4]!!.text.toString() == "o"
            && buttons[7][1]!!.text.toString() == ""
            && buttons[7][1]!!.text.toString() != "x"
        ) {

            buttons[7][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[7][1]?.setText("o")!!
            player1Turn = !player1Turn

        }

        /******************-6-*****************/

        else if ((buttons[5][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[5][4]!!.text.toString() != "")
            && buttons[5][4]!!.text.toString() == "o"
            && buttons[2][7]!!.text.toString() == ""
            && buttons[2][7]!!.text.toString() != "x"
        ) {

            buttons[2][7]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][7]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[2][7]!!.text.toString() && buttons[5][4]!!.text.toString() != ""
                    || buttons[5][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[5][4]!!.text.toString() != ""
                    )
            && buttons[5][4]!!.text.toString() == "o"
            && buttons[3][6]!!.text.toString() == ""
            && buttons[3][6]!!.text.toString() != "x"
        ) {

            buttons[3][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][4]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[2][7]!!.text.toString() && buttons[5][4]!!.text.toString() != ""
                    || buttons[5][4]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[5][4]!!.text.toString() != ""
                    || buttons[5][4]!!.text.toString() == buttons[7][2]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[5][4]!!.text.toString() != ""
                    )
            && buttons[5][4]!!.text.toString() == "o"
            && buttons[4][5]!!.text.toString() == ""
            && buttons[4][5]!!.text.toString() != "x"
        ) {

            buttons[5][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][5]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[4][5]!!.text.toString() == buttons[2][7]!!.text.toString() && buttons[4][5]!!.text.toString() != ""
                    || buttons[4][5]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[4][5]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[4][5]!!.text.toString() != ""
                    || buttons[4][5]!!.text.toString() == buttons[7][2]!!.text.toString() && buttons[4][5]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[4][5]!!.text.toString() != ""
                    )
            && buttons[4][5]!!.text.toString() == "o"
            && buttons[5][4]!!.text.toString() == ""
            && buttons[5][4]!!.text.toString() != "x"
        ) {

            buttons[5][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][5]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[4][5]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[4][5]!!.text.toString() != ""
                    || buttons[4][5]!!.text.toString() == buttons[7][2]!!.text.toString() && buttons[4][5]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[4][5]!!.text.toString() != ""
                    )
            && buttons[4][5]!!.text.toString() == "o"
            && buttons[6][3]!!.text.toString() == ""
            && buttons[6][3]!!.text.toString() != "x"
        ) {

            buttons[6][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[5][4]!!.text.toString() != "")
            && buttons[5][4]!!.text.toString() == "o"
            && buttons[7][2]!!.text.toString() == ""
            && buttons[7][2]!!.text.toString() != "x"
        ) {

            buttons[7][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[7][2]?.setText("o")!!
            player1Turn = !player1Turn

        }

        /******************-7-*****************/

        else if ((buttons[6][4]!!.text.toString() == buttons[5][5]!!.text.toString() && buttons[6][4]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[6][4]!!.text.toString() != "")
            && buttons[6][4]!!.text.toString() == "o"
            && buttons[3][7]!!.text.toString() == ""
            && buttons[3][7]!!.text.toString() != "x"
        ) {

            buttons[3][7]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][7]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[6][4]!!.text.toString() == buttons[5][5]!!.text.toString() && buttons[6][4]!!.text.toString() == buttons[3][7]!!.text.toString() && buttons[6][4]!!.text.toString() != ""
                    || buttons[6][4]!!.text.toString() == buttons[5][5]!!.text.toString() && buttons[6][4]!!.text.toString() == buttons[7][3]!!.text.toString() && buttons[6][4]!!.text.toString() != ""
                    )
            && buttons[6][4]!!.text.toString() == "o"
            && buttons[4][6]!!.text.toString() == ""
            && buttons[4][6]!!.text.toString() != "x"
        ) {

            buttons[4][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[6][4]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[6][4]!!.text.toString() == buttons[3][7]!!.text.toString() && buttons[6][4]!!.text.toString() != ""
                    || buttons[6][4]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[6][4]!!.text.toString() == buttons[7][3]!!.text.toString() && buttons[6][4]!!.text.toString() != ""
                    )
            && buttons[6][4]!!.text.toString() == "o"
            && buttons[5][5]!!.text.toString() == ""
            && buttons[5][5]!!.text.toString() != "x"
        ) {

            buttons[5][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][5]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[3][7]!!.text.toString() && buttons[5][5]!!.text.toString() != ""
                    || buttons[5][5]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[7][3]!!.text.toString() && buttons[5][5]!!.text.toString() != ""
                    )
            && buttons[5][5]!!.text.toString() == "o"
            && buttons[6][4]!!.text.toString() == ""
            && buttons[6][4]!!.text.toString() != "x"
        ) {

            buttons[6][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][5]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[5][5]!!.text.toString() != "")
            && buttons[5][5]!!.text.toString() == "o"
            && buttons[7][3]!!.text.toString() == ""
            && buttons[7][3]!!.text.toString() != "x"
        ) {

            buttons[7][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[7][3]?.setText("o")!!
            player1Turn = !player1Turn

        }
    }

    // check for o from TopLift to BottomRight to get 5 O in row
    /*** Done ***/
    private fun topLiftBottomRight4InRawO() {
        /****************************-1-********************************/
        if ((buttons[1][4]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[1][4]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[1][4]!!.text.toString() == buttons[4][7]!!.text.toString() && buttons[1][4]!!.text.toString() != "")
            && buttons[1][4]!!.text.toString() == "o"
            && buttons[0][3]!!.text.toString() == ""
            && buttons[0][3]!!.text.toString() != "x"
        ) {

            buttons[0][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[0][3]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[4][7]!!.text.toString() && buttons[2][5]!!.text.toString() != ""
                    )
            && buttons[2][5]!!.text.toString() == "o"
            && buttons[1][4]!!.text.toString() == ""
            && buttons[1][4]!!.text.toString() != "x"
        ) {

            buttons[1][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][6]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[0][3]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[4][7]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[3][6]!!.text.toString() != ""
                    )
            && buttons[3][6]!!.text.toString() == "o"
            && buttons[2][5]!!.text.toString() == ""
            && buttons[2][5]!!.text.toString() != "x"
        ) {

            buttons[2][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[0][3]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[4][7]!!.text.toString() && buttons[2][5]!!.text.toString() != ""
                    )
            && buttons[2][5]!!.text.toString() == "o"
            && buttons[3][6]!!.text.toString() == ""
            && buttons[3][6]!!.text.toString() != "x"
        ) {

            buttons[3][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][6]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[0][3]!!.text.toString() && buttons[2][5]!!.text.toString() != "")
            && buttons[2][5]!!.text.toString() == "o"
            && buttons[4][7]!!.text.toString() == ""
            && buttons[4][7]!!.text.toString() != "x"
        ) {

            buttons[4][7]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][7]?.setText("o")!!
            player1Turn = !player1Turn

        }
        /*****************************-2-*******************************/
        else if ((buttons[1][3]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[1][3]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[1][3]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[1][3]!!.text.toString() != "")
            && buttons[1][3]!!.text.toString() == "o"
            && buttons[0][2]!!.text.toString() == ""
            && buttons[0][2]!!.text.toString() != "x"
        ) {

            buttons[0][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][4]!!.text.toString() == buttons[0][2]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[2][4]!!.text.toString() != ""
                    || buttons[2][4]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[5][7]!!.text.toString() && buttons[2][4]!!.text.toString() != "")
            && buttons[2][4]!!.text.toString() == "o"
            && buttons[1][3]!!.text.toString() == ""
            && buttons[1][3]!!.text.toString() != "x"
        ) {

            buttons[1][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][5]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[0][2]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[3][5]!!.text.toString() != ""
                    || buttons[3][5]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[5][7]!!.text.toString() && buttons[3][5]!!.text.toString() != ""
                    )
            && buttons[3][5]!!.text.toString() == "o"
            && buttons[2][4]!!.text.toString() == ""
            && buttons[2][4]!!.text.toString() != "x"
        ) {

            buttons[2][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[0][2]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[2][4]!!.text.toString() != ""
                    || buttons[2][4]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[5][7]!!.text.toString() && buttons[2][4]!!.text.toString() != ""
                    )
            && buttons[2][4]!!.text.toString() == "o"
            && buttons[3][5]!!.text.toString() == ""
            && buttons[3][5]!!.text.toString() != "x"
        ) {

            buttons[3][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][5]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[0][2]!!.text.toString() && buttons[3][5]!!.text.toString() != ""
                    || buttons[3][5]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[5][7]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[3][5]!!.text.toString() != ""
                    )
            && buttons[3][5]!!.text.toString() == "o"
            && buttons[4][6]!!.text.toString() == ""
            && buttons[4][6]!!.text.toString() != "x"
        ) {

            buttons[4][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][6]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[4][6]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[4][6]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[4][6]!!.text.toString() != "")
            && buttons[4][6]!!.text.toString() == "o"
            && buttons[5][7]!!.text.toString() == ""
            && buttons[5][7]!!.text.toString() != "x"
        ) {

            buttons[5][7]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][7]?.setText("o")!!
            player1Turn = !player1Turn

        }

        /*************************-3-**************************/
        else if ((buttons[1][2]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[1][2]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[1][2]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[1][2]!!.text.toString() != "")
            && buttons[1][2]!!.text.toString() == "o"
            && buttons[0][1]!!.text.toString() == ""
            && buttons[0][1]!!.text.toString() != "x"
        ) {

            buttons[0][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][3]!!.text.toString() == buttons[0][1]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[2][3]!!.text.toString() != ""
                    || buttons[2][3]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[5][6]!!.text.toString() && buttons[2][3]!!.text.toString() != "")
            && buttons[2][3]!!.text.toString() == "o"
            && buttons[1][2]!!.text.toString() == ""
            && buttons[1][2]!!.text.toString() != "x"
        ) {

            buttons[1][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][4]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[0][1]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    || buttons[3][4]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[5][6]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    || buttons[3][4]!!.text.toString() == buttons[5][6]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[6][7]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    )
            && buttons[3][4]!!.text.toString() == "o"
            && buttons[2][3]!!.text.toString() == ""
            && buttons[2][3]!!.text.toString() != "x"
        ) {

            buttons[2][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[0][1]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[2][3]!!.text.toString() != ""
                    || buttons[2][3]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[5][6]!!.text.toString() && buttons[2][3]!!.text.toString() != ""
                    || buttons[2][3]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[5][6]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[6][7]!!.text.toString() && buttons[2][3]!!.text.toString() != ""
                    )
            && buttons[2][3]!!.text.toString() == "o"
            && buttons[3][4]!!.text.toString() == ""
            && buttons[3][4]!!.text.toString() != "x"
        ) {

            buttons[3][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][4]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[0][1]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    || buttons[3][4]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[5][6]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    || buttons[3][4]!!.text.toString() == buttons[6][7]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[5][6]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    )
            && buttons[3][4]!!.text.toString() == "o"
            && buttons[4][5]!!.text.toString() == ""
            && buttons[4][5]!!.text.toString() != "x"
        ) {

            buttons[4][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    || buttons[3][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[6][7]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    )
            && buttons[3][4]!!.text.toString() == "o"
            && buttons[5][6]!!.text.toString() == ""
            && buttons[5][6]!!.text.toString() != "x"
        ) {

            buttons[5][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][5]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[5][6]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][4]!!.text.toString() != "")
            && buttons[3][4]!!.text.toString() == "o"
            && buttons[6][7]!!.text.toString() == ""
            && buttons[6][7]!!.text.toString() != "x"
        ) {

            buttons[6][7]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][7]?.setText("o")!!
            player1Turn = !player1Turn

        }

        /*************************-4-***************************/

        else if ((buttons[1][1]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[1][1]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[1][1]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[1][1]!!.text.toString() != "")
            && buttons[1][1]!!.text.toString() == "o"
            && buttons[0][0]!!.text.toString() == ""
            && buttons[0][0]!!.text.toString() != "x"
        ) {

            buttons[0][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][0]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][3]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[0][0]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    || buttons[3][3]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[5][5]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    )
            && buttons[3][3]!!.text.toString() == "o"
            && buttons[1][1]!!.text.toString() == ""
            && buttons[1][1]!!.text.toString() != "x"
        ) {

            buttons[1][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][3]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[0][0]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    || buttons[3][3]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[1][1]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[5][5]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    || buttons[3][3]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[6][6]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    )
            && buttons[3][3]!!.text.toString() == "o"
            && buttons[2][2]!!.text.toString() == ""
            && buttons[2][2]!!.text.toString() != "x"
        ) {

            buttons[2][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][2]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[0][0]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[2][2]!!.text.toString() != "" && buttons[2][2]!!.text.toString() == "o"
                    || buttons[2][2]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[1][1]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() == buttons[5][5]!!.text.toString() && buttons[2][2]!!.text.toString() != "" && buttons[2][2]!!.text.toString() == "o"
                    || buttons[2][2]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() == buttons[6][6]!!.text.toString() && buttons[2][2]!!.text.toString() != "" && buttons[2][2]!!.text.toString() == "o"
                    || buttons[5][5]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[6][6]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[7][7]!!.text.toString() && buttons[6][6]!!.text.toString() != "" && buttons[6][6]!!.text.toString() == "o"
                    )
            && buttons[3][3]!!.text.toString() == ""
            && buttons[3][3]!!.text.toString() != "x"
        ) {

            buttons[3][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][2]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() == buttons[0][0]!!.text.toString() && buttons[2][2]!!.text.toString() != "" && buttons[2][2]!!.text.toString() == "o"
                    || buttons[2][2]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[2][2]!!.text.toString() != "" && buttons[2][2]!!.text.toString() == "o"
                    || buttons[5][5]!!.text.toString() == buttons[6][6]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[5][5]!!.text.toString() != "" && buttons[5][5]!!.text.toString() == "o"
                    || buttons[5][5]!!.text.toString() == buttons[6][6]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[7][7]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[5][5]!!.text.toString() != "" && buttons[5][5]!!.text.toString() == "o"
                    )
            && buttons[4][4]!!.text.toString() == ""
            && buttons[4][4]!!.text.toString() != "x"
        ) {

            buttons[4][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][2]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    || buttons[6][6]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    || buttons[6][6]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[7][7]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    )
            && buttons[4][4]!!.text.toString() == "o"
            && buttons[5][5]!!.text.toString() == ""
            && buttons[5][5]!!.text.toString() != "x"
        ) {

            buttons[5][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][5]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    || buttons[5][5]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[7][7]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    )
            && buttons[4][4]!!.text.toString() == "o"
            && buttons[6][6]!!.text.toString() == ""
            && buttons[6][6]!!.text.toString() != "x"
        ) {

            buttons[6][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][5]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[6][6]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[4][4]!!.text.toString() != "")
            && buttons[4][4]!!.text.toString() == "o"
            && buttons[7][7]!!.text.toString() == ""
            && buttons[7][7]!!.text.toString() != "x"
        ) {

            buttons[7][7]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[7][7]?.setText("o")!!
            player1Turn = !player1Turn

        }
        /**********************-5-*************************/
        else if ((buttons[2][1]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[2][1]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[2][1]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[2][1]!!.text.toString() != "")
            && buttons[2][1]!!.text.toString() == "o"
            && buttons[1][0]!!.text.toString() == ""
            && buttons[1][0]!!.text.toString() != "x"
        ) {

            buttons[1][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][0]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][2]!!.text.toString() == buttons[1][0]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[3][2]!!.text.toString() != ""
                    || buttons[3][2]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[3][2]!!.text.toString() != "")
            && buttons[3][2]!!.text.toString() == "o"
            && buttons[2][1]!!.text.toString() == ""
            && buttons[2][1]!!.text.toString() != "x"
        ) {

            buttons[2][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][3]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[1][0]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    || buttons[4][3]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    || buttons[4][3]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[7][6]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    )
            && buttons[4][3]!!.text.toString() == "o"
            && buttons[3][2]!!.text.toString() == ""
            && buttons[3][2]!!.text.toString() != "x"
        ) {

            buttons[3][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[1][0]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[3][2]!!.text.toString() != ""
                    || buttons[3][2]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[3][2]!!.text.toString() != ""
                    || buttons[3][2]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[7][6]!!.text.toString() && buttons[3][2]!!.text.toString() != ""
                    )
            && buttons[3][2]!!.text.toString() == "o"
            && buttons[4][3]!!.text.toString() == ""
            && buttons[4][3]!!.text.toString() != "x"
        ) {

            buttons[4][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][3]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[1][0]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    || buttons[4][3]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    || buttons[4][3]!!.text.toString() == buttons[7][6]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    )
            && buttons[4][3]!!.text.toString() == "o"
            && buttons[5][4]!!.text.toString() == ""
            && buttons[5][4]!!.text.toString() != "x"
        ) {

            buttons[5][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][3]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    || buttons[4][3]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[7][6]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    )
            && buttons[4][3]!!.text.toString() == "o"
            && buttons[6][5]!!.text.toString() == ""
            && buttons[6][5]!!.text.toString() != "x"
        ) {

            buttons[6][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][4]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[6][5]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[4][3]!!.text.toString() != "")
            && buttons[4][3]!!.text.toString() == "o"
            && buttons[7][6]!!.text.toString() == ""
            && buttons[7][6]!!.text.toString() != "x"
        ) {

            buttons[7][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[7][6]?.setText("o")!!
            player1Turn = !player1Turn

        }
        /***********************-6-************************/
        else if ((buttons[3][1]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[3][1]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[3][1]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[3][1]!!.text.toString() != "")
            && buttons[3][1]!!.text.toString() == "o"
            && buttons[2][0]!!.text.toString() == ""
            && buttons[2][0]!!.text.toString() != "x"
        ) {

            buttons[2][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][0]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][2]!!.text.toString() == buttons[2][0]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[4][2]!!.text.toString() != ""
                    || buttons[4][2]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[7][5]!!.text.toString() && buttons[4][2]!!.text.toString() != "")
            && buttons[4][2]!!.text.toString() == "o"
            && buttons[3][1]!!.text.toString() == ""
            && buttons[3][1]!!.text.toString() != "x"
        ) {

            buttons[3][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][3]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[2][0]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[5][3]!!.text.toString() != ""
                    || buttons[5][3]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[7][5]!!.text.toString() && buttons[5][3]!!.text.toString() != ""
                    )
            && buttons[5][3]!!.text.toString() == "o"
            && buttons[4][2]!!.text.toString() == ""
            && buttons[4][2]!!.text.toString() != "x"
        ) {

            buttons[4][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][0]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[4][2]!!.text.toString() != ""
                    || buttons[4][2]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[7][5]!!.text.toString() && buttons[4][2]!!.text.toString() != ""
                    )
            && buttons[4][2]!!.text.toString() == "o"
            && buttons[5][3]!!.text.toString() == ""
            && buttons[5][3]!!.text.toString() != "x"
        ) {

            buttons[5][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][3]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[2][0]!!.text.toString() && buttons[5][3]!!.text.toString() != ""
                    || buttons[5][3]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[7][5]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[5][3]!!.text.toString() != ""
                    )
            && buttons[5][3]!!.text.toString() == "o"
            && buttons[6][4]!!.text.toString() == ""
            && buttons[6][4]!!.text.toString() != "x"
        ) {

            buttons[6][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[6][4]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[6][4]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[6][4]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[6][4]!!.text.toString() != "")
            && buttons[6][4]!!.text.toString() == "o"
            && buttons[7][5]!!.text.toString() == ""
            && buttons[7][5]!!.text.toString() != "x"
        ) {

            buttons[7][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[7][5]?.setText("o")!!
            player1Turn = !player1Turn

        }
        /***********************-7-************************/
        else if ((buttons[4][1]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[4][1]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[4][1]!!.text.toString() == buttons[7][4]!!.text.toString() && buttons[4][1]!!.text.toString() != "")
            && buttons[4][1]!!.text.toString() == "o"
            && buttons[3][0]!!.text.toString() == ""
            && buttons[3][0]!!.text.toString() != "x"
        ) {

            buttons[3][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][0]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][0]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[5][2]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[5][2]!!.text.toString() == buttons[7][4]!!.text.toString() && buttons[5][2]!!.text.toString() != ""
                    )
            && buttons[5][2]!!.text.toString() == "o"
            && buttons[4][1]!!.text.toString() == ""
            && buttons[4][1]!!.text.toString() != "x"
        ) {

            buttons[4][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[6][3]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[3][0]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[6][3]!!.text.toString() == buttons[7][4]!!.text.toString() && buttons[6][3]!!.text.toString() != ""
                    )
            && buttons[6][3]!!.text.toString() == "o"
            && buttons[5][2]!!.text.toString() == ""
            && buttons[5][2]!!.text.toString() != "x"
        ) {

            buttons[5][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][0]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[5][2]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[5][2]!!.text.toString() == buttons[7][4]!!.text.toString() && buttons[5][2]!!.text.toString() != ""
                    )
            && buttons[5][2]!!.text.toString() == "o"
            && buttons[6][3]!!.text.toString() == ""
            && buttons[6][3]!!.text.toString() != "x"
        ) {

            buttons[6][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[6][3]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[5][2]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[5][2]!!.text.toString() == buttons[3][0]!!.text.toString() && buttons[5][2]!!.text.toString() != "")
            && buttons[5][2]!!.text.toString() == "o"
            && buttons[7][4]!!.text.toString() == ""
            && buttons[7][4]!!.text.toString() != "x"
        ) {

            buttons[7][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[7][4]?.setText("o")!!
            player1Turn = !player1Turn

        }
    }

    // check for o from TopRight to BottomLift to get 5 O in row
    /*** Done ***/
    private fun topRightBottomLift4InRawO() {

        /************************-1-*********************/
        if ((buttons[3][1]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[3][1]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[3][1]!!.text.toString() == buttons[4][0]!!.text.toString() && buttons[3][1]!!.text.toString() != "")
            && buttons[3][1]!!.text.toString() == "o"
            && buttons[0][4]!!.text.toString() == ""
            && buttons[0][4]!!.text.toString() != "x"
        ) {

            buttons[0][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][1]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[3][1]!!.text.toString() == buttons[0][4]!!.text.toString() && buttons[3][1]!!.text.toString() == buttons[4][0]!!.text.toString() && buttons[3][1]!!.text.toString() != ""
                    )
            && buttons[3][1]!!.text.toString() == "o"
            && buttons[1][3]!!.text.toString() == ""
            && buttons[1][3]!!.text.toString() != "x"
        ) {

            buttons[1][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][1]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[3][1]!!.text.toString() == buttons[0][4]!!.text.toString() && buttons[3][1]!!.text.toString() == buttons[4][0]!!.text.toString() && buttons[3][1]!!.text.toString() != ""
                    )
            && buttons[3][1]!!.text.toString() == "o"
            && buttons[2][2]!!.text.toString() == ""
            && buttons[2][2]!!.text.toString() != "x"
        ) {

            buttons[2][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][2]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[2][2]!!.text.toString() == buttons[0][4]!!.text.toString() && buttons[2][2]!!.text.toString() == buttons[4][0]!!.text.toString() && buttons[2][2]!!.text.toString() != ""
                    )
            && buttons[2][2]!!.text.toString() == "o"
            && buttons[3][1]!!.text.toString() == ""
            && buttons[3][1]!!.text.toString() != "x"
        ) {

            buttons[3][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][2]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[2][2]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[2][2]!!.text.toString() == buttons[0][4]!!.text.toString() && buttons[2][2]!!.text.toString() != "")
            && buttons[2][2]!!.text.toString() == "o"
            && buttons[4][0]!!.text.toString() == ""
            && buttons[4][0]!!.text.toString() != "x"
        ) {

            buttons[4][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][0]?.setText("o")!!
            player1Turn = !player1Turn

        }
        /*************-2-****************/
        else if ((buttons[3][2]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[3][2]!!.text.toString() != "")
            && buttons[3][2]!!.text.toString() == "o"
            && buttons[0][5]!!.text.toString() == ""
            && buttons[0][5]!!.text.toString() != "x"
        ) {

            buttons[0][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][2]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[0][5]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[3][2]!!.text.toString() != ""
                    || buttons[3][2]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[5][0]!!.text.toString() && buttons[3][2]!!.text.toString() != ""
                    )
            && buttons[3][2]!!.text.toString() == "o"
            && buttons[1][4]!!.text.toString() == ""
            && buttons[1][4]!!.text.toString() != "x"
        ) {

            buttons[1][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][2]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[0][5]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[3][2]!!.text.toString() != ""
                    || buttons[3][2]!!.text.toString() == buttons[5][0]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[3][2]!!.text.toString() != ""
                    )
            && buttons[3][2]!!.text.toString() == "o"
            && buttons[2][3]!!.text.toString() == ""
            && buttons[2][3]!!.text.toString() != "x"
        ) {

            buttons[2][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][3]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[0][5]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[2][3]!!.text.toString() != ""
                    || buttons[2][3]!!.text.toString() == buttons[5][0]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[2][3]!!.text.toString() != ""
                    )
            && buttons[2][3]!!.text.toString() == "o"
            && buttons[3][2]!!.text.toString() == ""
            && buttons[3][2]!!.text.toString() != "x"
        ) {

            buttons[3][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][3]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[0][5]!!.text.toString() && buttons[2][3]!!.text.toString() != ""
                    || buttons[2][3]!!.text.toString() == buttons[5][0]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[2][3]!!.text.toString() != ""
                    )
            && buttons[2][3]!!.text.toString() == "o"
            && buttons[4][1]!!.text.toString() == ""
            && buttons[4][1]!!.text.toString() != "x"
        ) {

            buttons[4][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][2]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[3][2]!!.text.toString() != "")
            && buttons[3][2]!!.text.toString() == "o"
            && buttons[5][0]!!.text.toString() == ""
            && buttons[5][0]!!.text.toString() != "x"
        ) {

            buttons[5][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][0]?.setText("o")!!
            player1Turn = !player1Turn

        }
        /*************-3-****************/
        else if ((buttons[3][3]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[1][5]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[3][3]!!.text.toString() != "")
            && buttons[3][3]!!.text.toString() == "o"
            && buttons[0][6]!!.text.toString() == ""
            && buttons[0][6]!!.text.toString() != "x"
        ) {

            buttons[0][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][3]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[0][6]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    || buttons[3][3]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[5][1]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    )
            && buttons[3][3]!!.text.toString() == "o"
            && buttons[1][5]!!.text.toString() == ""
            && buttons[1][5]!!.text.toString() != "x"
        ) {

            buttons[1][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][3]!!.text.toString() == buttons[1][5]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[0][6]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    || buttons[3][3]!!.text.toString() == buttons[1][5]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[5][1]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    || buttons[3][3]!!.text.toString() == buttons[5][1]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[6][0]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    )
            && buttons[3][3]!!.text.toString() == "o"
            && buttons[2][4]!!.text.toString() == ""
            && buttons[2][4]!!.text.toString() != "x"
        ) {

            buttons[2][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][4]!!.text.toString() == buttons[1][5]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[0][6]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[2][4]!!.text.toString() != ""
                    || buttons[2][4]!!.text.toString() == buttons[1][5]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[5][1]!!.text.toString() && buttons[2][4]!!.text.toString() != ""
                    || buttons[2][4]!!.text.toString() == buttons[5][1]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[6][0]!!.text.toString() && buttons[2][4]!!.text.toString() != ""
                    )
            && buttons[2][4]!!.text.toString() == "o"
            && buttons[3][3]!!.text.toString() == ""
            && buttons[3][3]!!.text.toString() != "x"
        ) {

            buttons[3][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][3]!!.text.toString() == buttons[1][5]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[6][0]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    || buttons[3][3]!!.text.toString() == buttons[5][1]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[1][5]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    || buttons[3][3]!!.text.toString() == buttons[5][1]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[6][0]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    )
            && buttons[3][3]!!.text.toString() == "o"
            && buttons[4][2]!!.text.toString() == ""
            && buttons[4][2]!!.text.toString() != "x"
        ) {

            buttons[4][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][3]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[1][5]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    || buttons[3][3]!!.text.toString() == buttons[6][0]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    )
            && buttons[3][3]!!.text.toString() == "o"
            && buttons[5][1]!!.text.toString() == ""
            && buttons[5][1]!!.text.toString() != "x"
        ) {

            buttons[5][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][3]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[5][1]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[3][3]!!.text.toString() != "")
            && buttons[3][3]!!.text.toString() == "o"
            && buttons[6][0]!!.text.toString() == ""
            && buttons[6][0]!!.text.toString() != "x"
        ) {

            buttons[6][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][0]?.setText("o")!!
            player1Turn = !player1Turn

        }
        /******************-4-*****************/
        else if ((buttons[3][4]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[1][6]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[3][4]!!.text.toString() != "")
            && buttons[3][4]!!.text.toString() == "o"
            && buttons[0][7]!!.text.toString() == ""
            && buttons[0][7]!!.text.toString() != "x"
        ) {

            buttons[0][7]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][7]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][4]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[0][7]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    || buttons[3][4]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    )
            && buttons[3][4]!!.text.toString() == "o"
            && buttons[1][6]!!.text.toString() == ""
            && buttons[1][6]!!.text.toString() != "x"
        ) {

            buttons[1][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][4]!!.text.toString() == buttons[1][6]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[0][7]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    || buttons[3][4]!!.text.toString() == buttons[1][6]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    || buttons[3][4]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[6][1]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    )
            && buttons[3][4]!!.text.toString() == "o"
            && buttons[2][5]!!.text.toString() == ""
            && buttons[2][5]!!.text.toString() != "x"
        ) {

            buttons[2][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][5]!!.text.toString() == buttons[1][6]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[0][7]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[2][5]!!.text.toString() != "" && buttons[2][5]!!.text.toString() == "o"
                    || buttons[2][5]!!.text.toString() == buttons[1][6]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[2][5]!!.text.toString() != "" && buttons[2][5]!!.text.toString() == "o"
                    || buttons[2][5]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[6][1]!!.text.toString() && buttons[2][5]!!.text.toString() != "" && buttons[2][5]!!.text.toString() == "o"
                    || buttons[6][1]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[6][1]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[6][1]!!.text.toString() == buttons[7][0]!!.text.toString() && buttons[6][1]!!.text.toString() != "" && buttons[6][1]!!.text.toString() == "o"
                    )
            && buttons[3][4]!!.text.toString() == ""
            && buttons[3][4]!!.text.toString() != "x"
        ) {

            buttons[3][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][4]!!.text.toString() == buttons[1][6]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[0][7]!!.text.toString() && buttons[3][4]!!.text.toString() != "" && buttons[3][4]!!.text.toString() == "o"
                    || buttons[3][4]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[1][6]!!.text.toString() && buttons[3][4]!!.text.toString() != "" && buttons[3][4]!!.text.toString() == "o"
                    || buttons[3][4]!!.text.toString() == buttons[6][1]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[3][4]!!.text.toString() != "" && buttons[3][4]!!.text.toString() == "o"
                    || buttons[7][0]!!.text.toString() == buttons[6][1]!!.text.toString() && buttons[7][0]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[7][0]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[7][0]!!.text.toString() != "" && buttons[7][0]!!.text.toString() == "o"
                    )
            && buttons[4][3]!!.text.toString() == ""
            && buttons[4][3]!!.text.toString() != "x"
        ) {

            buttons[4][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][3]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[1][6]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    || buttons[4][3]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[6][1]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    || buttons[4][3]!!.text.toString() == buttons[7][0]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[6][1]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    )
            && buttons[4][3]!!.text.toString() == "o"
            && buttons[5][2]!!.text.toString() == ""
            && buttons[5][2]!!.text.toString() != "x"
        ) {

            buttons[5][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][3]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    || buttons[4][3]!!.text.toString() == buttons[7][0]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    )
            && buttons[4][3]!!.text.toString() == "o"
            && buttons[6][1]!!.text.toString() == ""
            && buttons[6][1]!!.text.toString() != "x"
        ) {

            buttons[6][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][3]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[6][1]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[4][3]!!.text.toString() != "")
            && buttons[4][3]!!.text.toString() == "o"
            && buttons[7][0]!!.text.toString() == ""
            && buttons[7][0]!!.text.toString() != "x"
        ) {

            buttons[7][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[7][0]?.setText("o")!!
            player1Turn = !player1Turn

        }
        /******************-5-*****************/

        else if ((buttons[4][4]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[2][6]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[4][4]!!.text.toString() != "")
            && buttons[4][4]!!.text.toString() == "o"
            && buttons[1][7]!!.text.toString() == ""
            && buttons[1][7]!!.text.toString() != "x"
        ) {

            buttons[1][7]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][7]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][4]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[1][7]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    || buttons[4][4]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[6][2]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    )
            && buttons[4][4]!!.text.toString() == "o"
            && buttons[2][6]!!.text.toString() == ""
            && buttons[2][6]!!.text.toString() != "x"
        ) {

            buttons[2][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][4]!!.text.toString() == buttons[2][6]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[1][7]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    || buttons[4][4]!!.text.toString() == buttons[2][6]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[6][2]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    || buttons[4][4]!!.text.toString() == buttons[6][2]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[7][1]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    )
            && buttons[4][4]!!.text.toString() == "o"
            && buttons[3][5]!!.text.toString() == ""
            && buttons[3][5]!!.text.toString() != "x"
        ) {

            buttons[3][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][5]!!.text.toString() == buttons[2][6]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[1][7]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[3][5]!!.text.toString() != ""
                    || buttons[3][5]!!.text.toString() == buttons[2][6]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[6][2]!!.text.toString() && buttons[3][5]!!.text.toString() != ""
                    || buttons[3][5]!!.text.toString() == buttons[6][2]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[7][1]!!.text.toString() && buttons[3][5]!!.text.toString() != ""
                    )
            && buttons[3][5]!!.text.toString() == "o"
            && buttons[4][4]!!.text.toString() == ""
            && buttons[4][4]!!.text.toString() != "x"
        ) {

            buttons[4][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][4]!!.text.toString() == buttons[2][6]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[1][7]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    || buttons[4][4]!!.text.toString() == buttons[6][2]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[2][6]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    || buttons[4][4]!!.text.toString() == buttons[6][2]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[7][1]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    )
            && buttons[4][4]!!.text.toString() == "o"
            && buttons[5][3]!!.text.toString() == ""
            && buttons[5][3]!!.text.toString() != "x"
        ) {

            buttons[5][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][4]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[2][6]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    || buttons[4][4]!!.text.toString() == buttons[7][1]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    )
            && buttons[4][4]!!.text.toString() == "o"
            && buttons[6][2]!!.text.toString() == ""
            && buttons[6][2]!!.text.toString() != "x"
        ) {

            buttons[6][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][4]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[6][2]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[4][4]!!.text.toString() != "")
            && buttons[4][4]!!.text.toString() == "o"
            && buttons[7][1]!!.text.toString() == ""
            && buttons[7][1]!!.text.toString() != "x"
        ) {

            buttons[7][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[7][1]?.setText("o")!!
            player1Turn = !player1Turn

        }

        /******************-6-*****************/

        else if ((buttons[5][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[5][4]!!.text.toString() != "")
            && buttons[5][4]!!.text.toString() == "o"
            && buttons[2][7]!!.text.toString() == ""
            && buttons[2][7]!!.text.toString() != "x"
        ) {

            buttons[2][7]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][7]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[2][7]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[5][4]!!.text.toString() != ""
                    || buttons[5][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[7][2]!!.text.toString() && buttons[5][4]!!.text.toString() != ""
                    )
            && buttons[5][4]!!.text.toString() == "o"
            && buttons[3][6]!!.text.toString() == ""
            && buttons[3][6]!!.text.toString() != "x"
        ) {

            buttons[3][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][4]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[2][7]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[5][4]!!.text.toString() != ""
                    || buttons[5][4]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[7][2]!!.text.toString() && buttons[5][4]!!.text.toString() != ""
                    )
            && buttons[5][4]!!.text.toString() == "o"
            && buttons[4][5]!!.text.toString() == ""
            && buttons[4][5]!!.text.toString() != "x"
        ) {

            buttons[5][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][5]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[4][5]!!.text.toString() == buttons[2][7]!!.text.toString() && buttons[4][5]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[4][5]!!.text.toString() != ""
                    || buttons[4][5]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[4][5]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[4][5]!!.text.toString() == buttons[7][2]!!.text.toString() && buttons[4][5]!!.text.toString() != ""
                    )
            && buttons[4][5]!!.text.toString() == "o"
            && buttons[5][4]!!.text.toString() == ""
            && buttons[5][4]!!.text.toString() != "x"
        ) {

            buttons[5][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][5]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[4][5]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[4][5]!!.text.toString() == buttons[2][7]!!.text.toString() && buttons[4][5]!!.text.toString() != ""
                    || buttons[4][5]!!.text.toString() == buttons[7][2]!!.text.toString() && buttons[4][5]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[4][5]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[4][5]!!.text.toString() != ""
                    )
            && buttons[4][5]!!.text.toString() == "o"
            && buttons[6][3]!!.text.toString() == ""
            && buttons[6][3]!!.text.toString() != "x"
        ) {

            buttons[6][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[5][4]!!.text.toString() != "")
            && buttons[5][4]!!.text.toString() == "o"
            && buttons[7][2]!!.text.toString() == ""
            && buttons[7][2]!!.text.toString() != "x"
        ) {

            buttons[7][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[7][2]?.setText("o")!!
            player1Turn = !player1Turn

        }

        /******************-7-*****************/

        else if ((buttons[6][4]!!.text.toString() == buttons[5][5]!!.text.toString() && buttons[6][4]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[6][4]!!.text.toString() == buttons[7][3]!!.text.toString() && buttons[6][4]!!.text.toString() != "")
            && buttons[6][4]!!.text.toString() == "o"
            && buttons[3][7]!!.text.toString() == ""
            && buttons[3][7]!!.text.toString() != "x"
        ) {

            buttons[3][7]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][7]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[6][4]!!.text.toString() == buttons[5][5]!!.text.toString() && buttons[6][4]!!.text.toString() == buttons[3][7]!!.text.toString() && buttons[6][4]!!.text.toString() == buttons[7][3]!!.text.toString() && buttons[6][4]!!.text.toString() != ""
                    )
            && buttons[6][4]!!.text.toString() == "o"
            && buttons[4][6]!!.text.toString() == ""
            && buttons[4][6]!!.text.toString() != "x"
        ) {

            buttons[4][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[6][4]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[6][4]!!.text.toString() == buttons[3][7]!!.text.toString() && buttons[6][4]!!.text.toString() == buttons[7][3]!!.text.toString() && buttons[6][4]!!.text.toString() != ""
                    )
            && buttons[6][4]!!.text.toString() == "o"
            && buttons[5][5]!!.text.toString() == ""
            && buttons[5][5]!!.text.toString() != "x"
        ) {

            buttons[5][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][5]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[3][7]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[7][3]!!.text.toString() && buttons[5][5]!!.text.toString() != ""
                    )
            && buttons[5][5]!!.text.toString() == "o"
            && buttons[6][4]!!.text.toString() == ""
            && buttons[6][4]!!.text.toString() != "x"
        ) {

            buttons[6][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][5]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[3][7]!!.text.toString() && buttons[5][5]!!.text.toString() != "")
            && buttons[5][5]!!.text.toString() == "o"
            && buttons[7][3]!!.text.toString() == ""
            && buttons[7][3]!!.text.toString() != "x"
        ) {

            buttons[7][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[7][3]?.setText("o")!!
            player1Turn = !player1Turn

        }
    }

    // check for X from TopLift to BottomRight to Prevent 5 X in row
    /*** Done ***/
    private fun topLiftBottomRight4InRawX() {
        /****************************-1-********************************/
        if ((buttons[1][4]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[1][4]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[1][4]!!.text.toString() == buttons[4][7]!!.text.toString() && buttons[1][4]!!.text.toString() != "")
            && buttons[1][4]!!.text.toString() == "x"
            && buttons[0][3]!!.text.toString() == ""
            && buttons[0][3]!!.text.toString() != "x"
        ) {

            buttons[0][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[0][3]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[4][7]!!.text.toString() && buttons[2][5]!!.text.toString() != ""
                    )
            && buttons[2][5]!!.text.toString() == "x"
            && buttons[1][4]!!.text.toString() == ""
            && buttons[1][4]!!.text.toString() != "x"
        ) {

            buttons[1][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][6]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[0][3]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[4][7]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[3][6]!!.text.toString() != ""
                    )
            && buttons[3][6]!!.text.toString() == "x"
            && buttons[2][5]!!.text.toString() == ""
            && buttons[2][5]!!.text.toString() != "x"
        ) {

            buttons[2][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[0][3]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[4][7]!!.text.toString() && buttons[2][5]!!.text.toString() != ""
                    )
            && buttons[2][5]!!.text.toString() == "x"
            && buttons[3][6]!!.text.toString() == ""
            && buttons[3][6]!!.text.toString() != "x"
        ) {

            buttons[3][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][6]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[0][3]!!.text.toString() && buttons[2][5]!!.text.toString() != "")
            && buttons[2][5]!!.text.toString() == "x"
            && buttons[4][7]!!.text.toString() == ""
            && buttons[4][7]!!.text.toString() != "x"
        ) {

            buttons[4][7]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][7]?.setText("o")!!
            player1Turn = !player1Turn

        }
        /*****************************-2-*******************************/
        else if ((buttons[1][3]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[1][3]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[1][3]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[1][3]!!.text.toString() != "")
            && buttons[1][3]!!.text.toString() == "x"
            && buttons[0][2]!!.text.toString() == ""
            && buttons[0][2]!!.text.toString() != "x"
        ) {

            buttons[0][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][4]!!.text.toString() == buttons[0][2]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[2][4]!!.text.toString() != ""
                    || buttons[2][4]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[5][7]!!.text.toString() && buttons[2][4]!!.text.toString() != "")
            && buttons[2][4]!!.text.toString() == "x"
            && buttons[1][3]!!.text.toString() == ""
            && buttons[1][3]!!.text.toString() != "x"
        ) {

            buttons[1][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][5]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[0][2]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[3][5]!!.text.toString() != ""
                    || buttons[3][5]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[5][7]!!.text.toString() && buttons[3][5]!!.text.toString() != ""
                    )
            && buttons[3][5]!!.text.toString() == "x"
            && buttons[2][4]!!.text.toString() == ""
            && buttons[2][4]!!.text.toString() != "x"
        ) {

            buttons[2][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[0][2]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[2][4]!!.text.toString() != ""
                    || buttons[2][4]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[5][7]!!.text.toString() && buttons[2][4]!!.text.toString() != ""
                    )
            && buttons[2][4]!!.text.toString() == "x"
            && buttons[3][5]!!.text.toString() == ""
            && buttons[3][5]!!.text.toString() != "x"
        ) {

            buttons[3][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][5]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[0][2]!!.text.toString() && buttons[3][5]!!.text.toString() != ""
                    || buttons[3][5]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[5][7]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[3][5]!!.text.toString() != ""
                    )
            && buttons[3][5]!!.text.toString() == "x"
            && buttons[4][6]!!.text.toString() == ""
            && buttons[4][6]!!.text.toString() != "x"
        ) {

            buttons[4][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][6]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[4][6]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[4][6]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[4][6]!!.text.toString() != "")
            && buttons[4][6]!!.text.toString() == "x"
            && buttons[5][7]!!.text.toString() == ""
            && buttons[5][7]!!.text.toString() != "x"
        ) {

            buttons[5][7]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][7]?.setText("o")!!
            player1Turn = !player1Turn

        }

        /*************************-3-**************************/
        else if ((buttons[1][2]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[1][2]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[1][2]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[1][2]!!.text.toString() != "")
            && buttons[1][2]!!.text.toString() == "x"
            && buttons[0][1]!!.text.toString() == ""
            && buttons[0][1]!!.text.toString() != "x"
        ) {

            buttons[0][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][3]!!.text.toString() == buttons[0][1]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[2][3]!!.text.toString() != ""
                    || buttons[2][3]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[5][6]!!.text.toString() && buttons[2][3]!!.text.toString() != "")
            && buttons[2][3]!!.text.toString() == "x"
            && buttons[1][2]!!.text.toString() == ""
            && buttons[1][2]!!.text.toString() != "x"
        ) {

            buttons[1][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][4]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[0][1]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    || buttons[3][4]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[5][6]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    || buttons[3][4]!!.text.toString() == buttons[5][6]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[6][7]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    )
            && buttons[3][4]!!.text.toString() == "x"
            && buttons[2][3]!!.text.toString() == ""
            && buttons[2][3]!!.text.toString() != "x"
        ) {

            buttons[2][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[0][1]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[2][3]!!.text.toString() != ""
                    || buttons[2][3]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[5][6]!!.text.toString() && buttons[2][3]!!.text.toString() != ""
                    || buttons[2][3]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[5][6]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[6][7]!!.text.toString() && buttons[2][3]!!.text.toString() != ""
                    )
            && buttons[2][3]!!.text.toString() == "x"
            && buttons[3][4]!!.text.toString() == ""
            && buttons[3][4]!!.text.toString() != "x"
        ) {

            buttons[3][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][4]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[0][1]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    || buttons[3][4]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[5][6]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    || buttons[3][4]!!.text.toString() == buttons[6][7]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[5][6]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    )
            && buttons[3][4]!!.text.toString() == "x"
            && buttons[4][5]!!.text.toString() == ""
            && buttons[4][5]!!.text.toString() != "x"
        ) {

            buttons[4][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    || buttons[3][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[6][7]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    )
            && buttons[3][4]!!.text.toString() == "x"
            && buttons[5][6]!!.text.toString() == ""
            && buttons[5][6]!!.text.toString() != "x"
        ) {

            buttons[5][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][5]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[5][6]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][4]!!.text.toString() != "")
            && buttons[3][4]!!.text.toString() == "x"
            && buttons[6][7]!!.text.toString() == ""
            && buttons[6][7]!!.text.toString() != "x"
        ) {

            buttons[6][7]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][7]?.setText("o")!!
            player1Turn = !player1Turn

        }

        /*************************-4-***************************/

        else if ((buttons[1][1]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[1][1]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[1][1]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[1][1]!!.text.toString() != "")
            && buttons[1][1]!!.text.toString() == "x"
            && buttons[0][0]!!.text.toString() == ""
            && buttons[0][0]!!.text.toString() != "x"
        ) {

            buttons[0][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][0]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][3]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[0][0]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    || buttons[3][3]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[5][5]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    )
            && buttons[3][3]!!.text.toString() == "x"
            && buttons[1][1]!!.text.toString() == ""
            && buttons[1][1]!!.text.toString() != "x"
        ) {

            buttons[1][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][3]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[0][0]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    || buttons[3][3]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[1][1]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[5][5]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    || buttons[3][3]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[6][6]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    )
            && buttons[3][3]!!.text.toString() == "x"
            && buttons[2][2]!!.text.toString() == ""
            && buttons[2][2]!!.text.toString() != "x"
        ) {

            buttons[2][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][2]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[0][0]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[2][2]!!.text.toString() != "" && buttons[2][2]!!.text.toString() == "x"
                    || buttons[2][2]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[1][1]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() == buttons[5][5]!!.text.toString() && buttons[2][2]!!.text.toString() != "" && buttons[2][2]!!.text.toString() == "x"
                    || buttons[2][2]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() == buttons[6][6]!!.text.toString() && buttons[2][2]!!.text.toString() != "" && buttons[2][2]!!.text.toString() == "x"
                    || buttons[5][5]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[6][6]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[7][7]!!.text.toString() && buttons[6][6]!!.text.toString() != "" && buttons[6][6]!!.text.toString() == "x"
                    )
            && buttons[3][3]!!.text.toString() == ""
            && buttons[3][3]!!.text.toString() != "x"
        ) {

            buttons[3][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][2]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() == buttons[0][0]!!.text.toString() && buttons[2][2]!!.text.toString() != "" && buttons[2][2]!!.text.toString() == "x"
                    || buttons[2][2]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[2][2]!!.text.toString() != "" && buttons[2][2]!!.text.toString() == "x"
                    || buttons[5][5]!!.text.toString() == buttons[6][6]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[5][5]!!.text.toString() != "" && buttons[5][5]!!.text.toString() == "x"
                    || buttons[5][5]!!.text.toString() == buttons[6][6]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[7][7]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[5][5]!!.text.toString() != "" && buttons[5][5]!!.text.toString() == "x"
                    )
            && buttons[4][4]!!.text.toString() == ""
            && buttons[4][4]!!.text.toString() != "x"
        ) {

            buttons[4][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][2]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    || buttons[6][6]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    || buttons[6][6]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[7][7]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    )
            && buttons[4][4]!!.text.toString() == "x"
            && buttons[5][5]!!.text.toString() == ""
            && buttons[5][5]!!.text.toString() != "x"
        ) {

            buttons[5][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][5]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    || buttons[5][5]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[7][7]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    )
            && buttons[4][4]!!.text.toString() == "x"
            && buttons[6][6]!!.text.toString() == ""
            && buttons[6][6]!!.text.toString() != "x"
        ) {

            buttons[6][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][5]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[6][6]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[4][4]!!.text.toString() != "")
            && buttons[4][4]!!.text.toString() == "x"
            && buttons[7][7]!!.text.toString() == ""
            && buttons[7][7]!!.text.toString() != "x"
        ) {

            buttons[7][7]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[7][7]?.setText("o")!!
            player1Turn = !player1Turn

        }
        /**********************-5-*************************/
        else if ((buttons[2][1]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[2][1]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[2][1]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[2][1]!!.text.toString() != "")
            && buttons[2][1]!!.text.toString() == "x"
            && buttons[1][0]!!.text.toString() == ""
            && buttons[1][0]!!.text.toString() != "x"
        ) {

            buttons[1][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][0]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][2]!!.text.toString() == buttons[1][0]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[3][2]!!.text.toString() != ""
                    || buttons[3][2]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[3][2]!!.text.toString() != "")
            && buttons[3][2]!!.text.toString() == "x"
            && buttons[2][1]!!.text.toString() == ""
            && buttons[2][1]!!.text.toString() != "x"
        ) {

            buttons[2][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][3]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[1][0]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    || buttons[4][3]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    || buttons[4][3]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[7][6]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    )
            && buttons[4][3]!!.text.toString() == "x"
            && buttons[3][2]!!.text.toString() == ""
            && buttons[3][2]!!.text.toString() != "x"
        ) {

            buttons[3][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[1][0]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[3][2]!!.text.toString() != ""
                    || buttons[3][2]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[3][2]!!.text.toString() != ""
                    || buttons[3][2]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[7][6]!!.text.toString() && buttons[3][2]!!.text.toString() != ""
                    )
            && buttons[3][2]!!.text.toString() == "x"
            && buttons[4][3]!!.text.toString() == ""
            && buttons[4][3]!!.text.toString() != "x"
        ) {

            buttons[4][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][3]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[1][0]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    || buttons[4][3]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    || buttons[4][3]!!.text.toString() == buttons[7][6]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    )
            && buttons[4][3]!!.text.toString() == "x"
            && buttons[5][4]!!.text.toString() == ""
            && buttons[5][4]!!.text.toString() != "x"
        ) {

            buttons[5][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][3]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    || buttons[4][3]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[7][6]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    )
            && buttons[4][3]!!.text.toString() == "x"
            && buttons[6][5]!!.text.toString() == ""
            && buttons[6][5]!!.text.toString() != "x"
        ) {

            buttons[6][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][4]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[6][5]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[4][3]!!.text.toString() != "")
            && buttons[4][3]!!.text.toString() == "x"
            && buttons[7][6]!!.text.toString() == ""
            && buttons[7][6]!!.text.toString() != "x"
        ) {

            buttons[7][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[7][6]?.setText("o")!!
            player1Turn = !player1Turn

        }
        /***********************-6-************************/
        else if ((buttons[3][1]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[3][1]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[3][1]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[3][1]!!.text.toString() != "")
            && buttons[3][1]!!.text.toString() == "x"
            && buttons[2][0]!!.text.toString() == ""
            && buttons[2][0]!!.text.toString() != "x"
        ) {

            buttons[2][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][0]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][2]!!.text.toString() == buttons[2][0]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[4][2]!!.text.toString() != ""
                    || buttons[4][2]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[7][5]!!.text.toString() && buttons[4][2]!!.text.toString() != "")
            && buttons[4][2]!!.text.toString() == "x"
            && buttons[3][1]!!.text.toString() == ""
            && buttons[3][1]!!.text.toString() != "x"
        ) {

            buttons[3][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][3]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[2][0]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[5][3]!!.text.toString() != ""
                    || buttons[5][3]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[7][5]!!.text.toString() && buttons[5][3]!!.text.toString() != ""
                    )
            && buttons[5][3]!!.text.toString() == "x"
            && buttons[4][2]!!.text.toString() == ""
            && buttons[4][2]!!.text.toString() != "x"
        ) {

            buttons[4][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][0]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[4][2]!!.text.toString() != ""
                    || buttons[4][2]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[7][5]!!.text.toString() && buttons[4][2]!!.text.toString() != ""
                    )
            && buttons[4][2]!!.text.toString() == "x"
            && buttons[5][3]!!.text.toString() == ""
            && buttons[5][3]!!.text.toString() != "x"
        ) {

            buttons[5][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][3]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[2][0]!!.text.toString() && buttons[5][3]!!.text.toString() != ""
                    || buttons[5][3]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[7][5]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[5][3]!!.text.toString() != ""
                    )
            && buttons[5][3]!!.text.toString() == "x"
            && buttons[6][4]!!.text.toString() == ""
            && buttons[6][4]!!.text.toString() != "x"
        ) {

            buttons[6][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[6][4]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[6][4]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[6][4]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[6][4]!!.text.toString() != "")
            && buttons[6][4]!!.text.toString() == "x"
            && buttons[7][5]!!.text.toString() == ""
            && buttons[7][5]!!.text.toString() != "x"
        ) {

            buttons[7][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[7][5]?.setText("o")!!
            player1Turn = !player1Turn

        }
        /***********************-7-************************/
        else if ((buttons[4][1]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[4][1]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[4][1]!!.text.toString() == buttons[7][4]!!.text.toString() && buttons[4][1]!!.text.toString() != "")
            && buttons[4][1]!!.text.toString() == "x"
            && buttons[3][0]!!.text.toString() == ""
            && buttons[3][0]!!.text.toString() != "x"
        ) {

            buttons[3][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][0]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][0]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[5][2]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[5][2]!!.text.toString() == buttons[7][4]!!.text.toString() && buttons[5][2]!!.text.toString() != ""
                    )
            && buttons[5][2]!!.text.toString() == "x"
            && buttons[4][1]!!.text.toString() == ""
            && buttons[4][1]!!.text.toString() != "x"
        ) {

            buttons[4][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[6][3]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[3][0]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[6][3]!!.text.toString() == buttons[7][4]!!.text.toString() && buttons[6][3]!!.text.toString() != ""
                    )
            && buttons[6][3]!!.text.toString() == "x"
            && buttons[5][2]!!.text.toString() == ""
            && buttons[5][2]!!.text.toString() != "x"
        ) {

            buttons[5][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][0]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[5][2]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[5][2]!!.text.toString() == buttons[7][4]!!.text.toString() && buttons[5][2]!!.text.toString() != ""
                    )
            && buttons[5][2]!!.text.toString() == "x"
            && buttons[6][3]!!.text.toString() == ""
            && buttons[6][3]!!.text.toString() != "x"
        ) {

            buttons[6][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[6][3]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[5][2]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[5][2]!!.text.toString() == buttons[3][0]!!.text.toString() && buttons[5][2]!!.text.toString() != "")
            && buttons[5][2]!!.text.toString() == "x"
            && buttons[7][4]!!.text.toString() == ""
            && buttons[7][4]!!.text.toString() != "x"
        ) {

            buttons[7][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[7][4]?.setText("o")!!
            player1Turn = !player1Turn

        }
    }

    // check for X from TopRight to BottomLift to Prevent 5 X in row
    /*** Done ***/
    private fun topRightBottomLift4InRawX() {

        /************************-1-*********************/
        if ((buttons[3][1]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[3][1]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[3][1]!!.text.toString() == buttons[4][0]!!.text.toString() && buttons[3][1]!!.text.toString() != "")
            && buttons[3][1]!!.text.toString() == "x"
            && buttons[0][4]!!.text.toString() == ""
            && buttons[0][4]!!.text.toString() != "x"
        ) {

            buttons[0][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][1]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[3][1]!!.text.toString() == buttons[0][4]!!.text.toString() && buttons[3][1]!!.text.toString() == buttons[4][0]!!.text.toString() && buttons[3][1]!!.text.toString() != ""
                    )
            && buttons[3][1]!!.text.toString() == "x"
            && buttons[1][3]!!.text.toString() == ""
            && buttons[1][3]!!.text.toString() != "x"
        ) {

            buttons[1][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][1]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[3][1]!!.text.toString() == buttons[0][4]!!.text.toString() && buttons[3][1]!!.text.toString() == buttons[4][0]!!.text.toString() && buttons[3][1]!!.text.toString() != ""
                    )
            && buttons[3][1]!!.text.toString() == "x"
            && buttons[2][2]!!.text.toString() == ""
            && buttons[2][2]!!.text.toString() != "x"
        ) {

            buttons[2][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][2]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[2][2]!!.text.toString() == buttons[0][4]!!.text.toString() && buttons[2][2]!!.text.toString() == buttons[4][0]!!.text.toString() && buttons[2][2]!!.text.toString() != ""
                    )
            && buttons[2][2]!!.text.toString() == "x"
            && buttons[3][1]!!.text.toString() == ""
            && buttons[3][1]!!.text.toString() != "x"
        ) {

            buttons[3][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][2]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[2][2]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[2][2]!!.text.toString() == buttons[0][4]!!.text.toString() && buttons[2][2]!!.text.toString() != "")
            && buttons[2][2]!!.text.toString() == "x"
            && buttons[4][0]!!.text.toString() == ""
            && buttons[4][0]!!.text.toString() != "x"
        ) {

            buttons[4][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][0]?.setText("o")!!
            player1Turn = !player1Turn

        }
        /*************-2-****************/
        else if ((buttons[3][2]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[3][2]!!.text.toString() != "")
            && buttons[3][2]!!.text.toString() == "x"
            && buttons[0][5]!!.text.toString() == ""
            && buttons[0][5]!!.text.toString() != "x"
        ) {

            buttons[0][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][2]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[0][5]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[3][2]!!.text.toString() != ""
                    || buttons[3][2]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[5][0]!!.text.toString() && buttons[3][2]!!.text.toString() != ""
                    )
            && buttons[3][2]!!.text.toString() == "x"
            && buttons[1][4]!!.text.toString() == ""
            && buttons[1][4]!!.text.toString() != "x"
        ) {

            buttons[1][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][2]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[0][5]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[3][2]!!.text.toString() != ""
                    || buttons[3][2]!!.text.toString() == buttons[5][0]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[3][2]!!.text.toString() != ""
                    )
            && buttons[3][2]!!.text.toString() == "x"
            && buttons[2][3]!!.text.toString() == ""
            && buttons[2][3]!!.text.toString() != "x"
        ) {

            buttons[2][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][3]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[0][5]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[2][3]!!.text.toString() != ""
                    || buttons[2][3]!!.text.toString() == buttons[5][0]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[2][3]!!.text.toString() != ""
                    )
            && buttons[2][3]!!.text.toString() == "x"
            && buttons[3][2]!!.text.toString() == ""
            && buttons[3][2]!!.text.toString() != "x"
        ) {

            buttons[3][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][3]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[0][5]!!.text.toString() && buttons[2][3]!!.text.toString() != ""
                    || buttons[2][3]!!.text.toString() == buttons[5][0]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[2][3]!!.text.toString() != ""
                    )
            && buttons[2][3]!!.text.toString() == "x"
            && buttons[4][1]!!.text.toString() == ""
            && buttons[4][1]!!.text.toString() != "x"
        ) {

            buttons[4][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][2]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[3][2]!!.text.toString() != "")
            && buttons[3][2]!!.text.toString() == "o"
            && buttons[5][0]!!.text.toString() == ""
            && buttons[5][0]!!.text.toString() != "x"
        ) {

            buttons[5][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][0]?.setText("o")!!
            player1Turn = !player1Turn

        }
        /*************-3-****************/
        else if ((buttons[3][3]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[1][5]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[3][3]!!.text.toString() != "")
            && buttons[3][3]!!.text.toString() == "x"
            && buttons[0][6]!!.text.toString() == ""
            && buttons[0][6]!!.text.toString() != "x"
        ) {

            buttons[0][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][3]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[0][6]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    || buttons[3][3]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[5][1]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    )
            && buttons[3][3]!!.text.toString() == "x"
            && buttons[1][5]!!.text.toString() == ""
            && buttons[1][5]!!.text.toString() != "x"
        ) {

            buttons[1][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][3]!!.text.toString() == buttons[1][5]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[0][6]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    || buttons[3][3]!!.text.toString() == buttons[1][5]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[5][1]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    || buttons[3][3]!!.text.toString() == buttons[5][1]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[6][0]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    )
            && buttons[3][3]!!.text.toString() == "x"
            && buttons[2][4]!!.text.toString() == ""
            && buttons[2][4]!!.text.toString() != "x"
        ) {

            buttons[2][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][4]!!.text.toString() == buttons[1][5]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[0][6]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[2][4]!!.text.toString() != ""
                    || buttons[2][4]!!.text.toString() == buttons[1][5]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[5][1]!!.text.toString() && buttons[2][4]!!.text.toString() != ""
                    || buttons[2][4]!!.text.toString() == buttons[5][1]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[6][0]!!.text.toString() && buttons[2][4]!!.text.toString() != ""
                    )
            && buttons[2][4]!!.text.toString() == "x"
            && buttons[3][3]!!.text.toString() == ""
            && buttons[3][3]!!.text.toString() != "x"
        ) {

            buttons[3][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][3]!!.text.toString() == buttons[1][5]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[6][0]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    || buttons[3][3]!!.text.toString() == buttons[5][1]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[1][5]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    || buttons[3][3]!!.text.toString() == buttons[5][1]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[6][0]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    )
            && buttons[3][3]!!.text.toString() == "x"
            && buttons[4][2]!!.text.toString() == ""
            && buttons[4][2]!!.text.toString() != "x"
        ) {

            buttons[4][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][3]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[1][5]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    || buttons[3][3]!!.text.toString() == buttons[6][0]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    )
            && buttons[3][3]!!.text.toString() == "x"
            && buttons[5][1]!!.text.toString() == ""
            && buttons[5][1]!!.text.toString() != "x"
        ) {

            buttons[5][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][3]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[5][1]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[3][3]!!.text.toString() != "")
            && buttons[3][3]!!.text.toString() == "x"
            && buttons[6][0]!!.text.toString() == ""
            && buttons[6][0]!!.text.toString() != "x"
        ) {

            buttons[6][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][0]?.setText("o")!!
            player1Turn = !player1Turn

        }
        /******************-4-*****************/
        else if ((buttons[3][4]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[1][6]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[3][4]!!.text.toString() != "")
            && buttons[3][4]!!.text.toString() == "x"
            && buttons[0][7]!!.text.toString() == ""
            && buttons[0][7]!!.text.toString() != "x"
        ) {

            buttons[0][7]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][7]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][4]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[0][7]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    || buttons[3][4]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    )
            && buttons[3][4]!!.text.toString() == "x"
            && buttons[1][6]!!.text.toString() == ""
            && buttons[1][6]!!.text.toString() != "x"
        ) {

            buttons[1][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][4]!!.text.toString() == buttons[1][6]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[0][7]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    || buttons[3][4]!!.text.toString() == buttons[1][6]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    || buttons[3][4]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[6][1]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    )
            && buttons[3][4]!!.text.toString() == "x"
            && buttons[2][5]!!.text.toString() == ""
            && buttons[2][5]!!.text.toString() != "x"
        ) {

            buttons[2][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][5]!!.text.toString() == buttons[1][6]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[0][7]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[2][5]!!.text.toString() != "" && buttons[2][5]!!.text.toString() == "x"
                    || buttons[2][5]!!.text.toString() == buttons[1][6]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[2][5]!!.text.toString() != "" && buttons[2][5]!!.text.toString() == "x"
                    || buttons[2][5]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[6][1]!!.text.toString() && buttons[2][5]!!.text.toString() != "" && buttons[2][5]!!.text.toString() == "x"
                    || buttons[6][1]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[6][1]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[6][1]!!.text.toString() == buttons[7][0]!!.text.toString() && buttons[6][1]!!.text.toString() != "" && buttons[6][1]!!.text.toString() == "x"
                    )
            && buttons[3][4]!!.text.toString() == ""
            && buttons[3][4]!!.text.toString() != "x"
        ) {

            buttons[3][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][4]!!.text.toString() == buttons[1][6]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[0][7]!!.text.toString() && buttons[3][4]!!.text.toString() != "" && buttons[3][4]!!.text.toString() == "x"
                    || buttons[3][4]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[1][6]!!.text.toString() && buttons[3][4]!!.text.toString() != "" && buttons[3][4]!!.text.toString() == "x"
                    || buttons[3][4]!!.text.toString() == buttons[6][1]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[3][4]!!.text.toString() != "" && buttons[3][4]!!.text.toString() == "x"
                    || buttons[7][0]!!.text.toString() == buttons[6][1]!!.text.toString() && buttons[7][0]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[7][0]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[7][0]!!.text.toString() != "" && buttons[7][0]!!.text.toString() == "x"
                    )
            && buttons[4][3]!!.text.toString() == ""
            && buttons[4][3]!!.text.toString() != "x"
        ) {

            buttons[4][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][3]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[1][6]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    || buttons[4][3]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[6][1]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    || buttons[4][3]!!.text.toString() == buttons[7][0]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[6][1]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    )
            && buttons[4][3]!!.text.toString() == "x"
            && buttons[5][2]!!.text.toString() == ""
            && buttons[5][2]!!.text.toString() != "x"
        ) {

            buttons[5][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][3]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    || buttons[4][3]!!.text.toString() == buttons[7][0]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    )
            && buttons[4][3]!!.text.toString() == "x"
            && buttons[6][1]!!.text.toString() == ""
            && buttons[6][1]!!.text.toString() != "x"
        ) {

            buttons[6][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][3]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[6][1]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[4][3]!!.text.toString() != "")
            && buttons[4][3]!!.text.toString() == "x"
            && buttons[7][0]!!.text.toString() == ""
            && buttons[7][0]!!.text.toString() != "x"
        ) {

            buttons[7][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[7][0]?.setText("o")!!
            player1Turn = !player1Turn

        }
        /******************-5-*****************/

        else if ((buttons[4][4]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[2][6]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[4][4]!!.text.toString() != "")
            && buttons[4][4]!!.text.toString() == "x"
            && buttons[1][7]!!.text.toString() == ""
            && buttons[1][7]!!.text.toString() != "x"
        ) {

            buttons[1][7]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][7]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][4]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[1][7]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    || buttons[4][4]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[6][2]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    )
            && buttons[4][4]!!.text.toString() == "x"
            && buttons[2][6]!!.text.toString() == ""
            && buttons[2][6]!!.text.toString() != "x"
        ) {

            buttons[2][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][4]!!.text.toString() == buttons[2][6]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[1][7]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    || buttons[4][4]!!.text.toString() == buttons[2][6]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[6][2]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    || buttons[4][4]!!.text.toString() == buttons[6][2]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[7][1]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    )
            && buttons[4][4]!!.text.toString() == "x"
            && buttons[3][5]!!.text.toString() == ""
            && buttons[3][5]!!.text.toString() != "x"
        ) {

            buttons[3][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][5]!!.text.toString() == buttons[2][6]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[1][7]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[3][5]!!.text.toString() != ""
                    || buttons[3][5]!!.text.toString() == buttons[2][6]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[6][2]!!.text.toString() && buttons[3][5]!!.text.toString() != ""
                    || buttons[3][5]!!.text.toString() == buttons[6][2]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[7][1]!!.text.toString() && buttons[3][5]!!.text.toString() != ""
                    )
            && buttons[3][5]!!.text.toString() == "x"
            && buttons[4][4]!!.text.toString() == ""
            && buttons[4][4]!!.text.toString() != "x"
        ) {

            buttons[4][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][4]!!.text.toString() == buttons[2][6]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[1][7]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    || buttons[4][4]!!.text.toString() == buttons[6][2]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[2][6]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    || buttons[4][4]!!.text.toString() == buttons[6][2]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[7][1]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    )
            && buttons[4][4]!!.text.toString() == "x"
            && buttons[5][3]!!.text.toString() == ""
            && buttons[5][3]!!.text.toString() != "x"
        ) {

            buttons[5][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][4]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[2][6]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    || buttons[4][4]!!.text.toString() == buttons[7][1]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    )
            && buttons[4][4]!!.text.toString() == "x"
            && buttons[6][2]!!.text.toString() == ""
            && buttons[6][2]!!.text.toString() != "x"
        ) {

            buttons[6][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][4]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[6][2]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[4][4]!!.text.toString() != "")
            && buttons[4][4]!!.text.toString() == "x"
            && buttons[7][1]!!.text.toString() == ""
            && buttons[7][1]!!.text.toString() != "x"
        ) {

            buttons[7][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[7][1]?.setText("o")!!
            player1Turn = !player1Turn

        }

        /******************-6-*****************/

        else if ((buttons[5][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[5][4]!!.text.toString() != "")
            && buttons[5][4]!!.text.toString() == "x"
            && buttons[2][7]!!.text.toString() == ""
            && buttons[2][7]!!.text.toString() != "x"
        ) {

            buttons[2][7]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][7]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[2][7]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[5][4]!!.text.toString() != ""
                    || buttons[5][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[7][2]!!.text.toString() && buttons[5][4]!!.text.toString() != ""
                    )
            && buttons[5][4]!!.text.toString() == "x"
            && buttons[3][6]!!.text.toString() == ""
            && buttons[3][6]!!.text.toString() != "x"
        ) {

            buttons[3][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][4]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[2][7]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[5][4]!!.text.toString() != ""
                    || buttons[5][4]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[7][2]!!.text.toString() && buttons[5][4]!!.text.toString() != ""
                    )
            && buttons[5][4]!!.text.toString() == "x"
            && buttons[4][5]!!.text.toString() == ""
            && buttons[4][5]!!.text.toString() != "x"
        ) {

            buttons[5][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][5]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[4][5]!!.text.toString() == buttons[2][7]!!.text.toString() && buttons[4][5]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[4][5]!!.text.toString() != ""
                    || buttons[4][5]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[4][5]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[4][5]!!.text.toString() == buttons[7][2]!!.text.toString() && buttons[4][5]!!.text.toString() != ""
                    )
            && buttons[4][5]!!.text.toString() == "x"
            && buttons[5][4]!!.text.toString() == ""
            && buttons[5][4]!!.text.toString() != "x"
        ) {

            buttons[5][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][5]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[4][5]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[4][5]!!.text.toString() == buttons[2][7]!!.text.toString() && buttons[4][5]!!.text.toString() != ""
                    || buttons[4][5]!!.text.toString() == buttons[7][2]!!.text.toString() && buttons[4][5]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[4][5]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[4][5]!!.text.toString() != ""
                    )
            && buttons[4][5]!!.text.toString() == "x"
            && buttons[6][3]!!.text.toString() == ""
            && buttons[6][3]!!.text.toString() != "x"
        ) {

            buttons[6][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[5][4]!!.text.toString() != "")
            && buttons[5][4]!!.text.toString() == "x"
            && buttons[7][2]!!.text.toString() == ""
            && buttons[7][2]!!.text.toString() != "x"
        ) {

            buttons[7][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[7][2]?.setText("o")!!
            player1Turn = !player1Turn

        }

        /******************-7-*****************/

        else if ((buttons[6][4]!!.text.toString() == buttons[5][5]!!.text.toString() && buttons[6][4]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[6][4]!!.text.toString() == buttons[7][3]!!.text.toString() && buttons[6][4]!!.text.toString() != "")
            && buttons[6][4]!!.text.toString() == "x"
            && buttons[3][7]!!.text.toString() == ""
            && buttons[3][7]!!.text.toString() != "x"
        ) {

            buttons[3][7]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][7]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[6][4]!!.text.toString() == buttons[5][5]!!.text.toString() && buttons[6][4]!!.text.toString() == buttons[3][7]!!.text.toString() && buttons[6][4]!!.text.toString() == buttons[7][3]!!.text.toString() && buttons[6][4]!!.text.toString() != ""
                    )
            && buttons[6][4]!!.text.toString() == "x"
            && buttons[4][6]!!.text.toString() == ""
            && buttons[4][6]!!.text.toString() != "x"
        ) {

            buttons[4][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[6][4]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[6][4]!!.text.toString() == buttons[3][7]!!.text.toString() && buttons[6][4]!!.text.toString() == buttons[7][3]!!.text.toString() && buttons[6][4]!!.text.toString() != ""
                    )
            && buttons[6][4]!!.text.toString() == "x"
            && buttons[5][5]!!.text.toString() == ""
            && buttons[5][5]!!.text.toString() != "x"
        ) {

            buttons[5][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][5]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[3][7]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[7][3]!!.text.toString() && buttons[5][5]!!.text.toString() != ""
                    )
            && buttons[5][5]!!.text.toString() == "x"
            && buttons[6][4]!!.text.toString() == ""
            && buttons[6][4]!!.text.toString() != "x"
        ) {

            buttons[6][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][5]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[3][7]!!.text.toString() && buttons[5][5]!!.text.toString() != "")
            && buttons[5][5]!!.text.toString() == "x"
            && buttons[7][3]!!.text.toString() == ""
            && buttons[7][3]!!.text.toString() != "x"
        ) {

            buttons[7][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[7][3]?.setText("o")!!
            player1Turn = !player1Turn

        }
    }

    // check for o on Horizontal and Vertical to get 4 O in row
    /*** Done ***/
    private fun oHorizontalVertical() {
        for (i in 0..7) {
            /******************************* Horizontal Lines********************************/
            if ((buttons[i][1]!!.text.toString() == buttons[i][2]!!.text.toString() && buttons[i][1]!!.text.toString() == buttons[i][3]!!.text.toString() && buttons[i][1]!!.text.toString() != "")
                && buttons[i][1]!!.text.toString() == "o"
                && buttons[i][0]!!.text.toString() == ""
                && buttons[i][0]!!.text.toString() != "x"
            ) {

                buttons[i][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[i][0]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[i][3]!!.text.toString() == buttons[i][2]!!.text.toString() && buttons[i][4]!!.text.toString() == buttons[i][3]!!.text.toString() && buttons[i][3]!!.text.toString() != ""
                        || buttons[i][3]!!.text.toString() == buttons[i][2]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][0]!!.text.toString() && buttons[i][3]!!.text.toString() != "")
                && buttons[i][3]!!.text.toString() == "o"
                && buttons[i][1]!!.text.toString() == ""
                && buttons[i][1]!!.text.toString() != "x"
            ) {

                buttons[i][1]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[i][1]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[i][3]!!.text.toString() == buttons[i][1]!!.text.toString() && buttons[i][0]!!.text.toString() == buttons[i][3]!!.text.toString() && buttons[i][3]!!.text.toString() != ""
                        || buttons[i][3]!!.text.toString() == buttons[i][1]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][3]!!.text.toString() != ""
                        || buttons[i][3]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][5]!!.text.toString() && buttons[i][3]!!.text.toString() != ""
                        )
                && buttons[i][3]!!.text.toString() == "o"
                && buttons[i][2]!!.text.toString() == ""
                && buttons[i][2]!!.text.toString() != "x"
            ) {

                buttons[i][2]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[i][2]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[i][0]!!.text.toString() == buttons[i][1]!!.text.toString() && buttons[i][1]!!.text.toString() == buttons[i][2]!!.text.toString() && buttons[i][1]!!.text.toString() != "" && buttons[i][1]!!.text.toString() == "o"
                        || buttons[i][4]!!.text.toString() == buttons[i][1]!!.text.toString() && buttons[i][1]!!.text.toString() == buttons[i][2]!!.text.toString() && buttons[i][1]!!.text.toString() != "" && buttons[i][1]!!.text.toString() == "o"
                        || buttons[i][5]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][4]!!.text.toString() == buttons[i][6]!!.text.toString() && buttons[i][4]!!.text.toString() != "" && buttons[i][4]!!.text.toString() == "o"
                        || buttons[i][5]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][4]!!.text.toString() == buttons[i][2]!!.text.toString() && buttons[i][4]!!.text.toString() != "" && buttons[i][4]!!.text.toString() == "o"
                        )
                && buttons[i][3]!!.text.toString() == ""
                && buttons[i][3]!!.text.toString() != "x"
            ) {

                buttons[i][3]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[i][3]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[i][3]!!.text.toString() == buttons[i][1]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][2]!!.text.toString() && buttons[i][3]!!.text.toString() != "" && buttons[i][3]!!.text.toString() == "o"
                        || buttons[i][3]!!.text.toString() == buttons[i][2]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][5]!!.text.toString() && buttons[i][3]!!.text.toString() != "" && buttons[i][3]!!.text.toString() == "o"
                        || buttons[i][3]!!.text.toString() == buttons[i][6]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][5]!!.text.toString() && buttons[i][3]!!.text.toString() != "" && buttons[i][3]!!.text.toString() == "o"
                        || buttons[i][5]!!.text.toString() == buttons[i][6]!!.text.toString() && buttons[i][5]!!.text.toString() == buttons[i][7]!!.text.toString() && buttons[i][5]!!.text.toString() != "" && buttons[i][5]!!.text.toString() == "o"
                        )
                && buttons[i][4]!!.text.toString() == ""
                && buttons[i][4]!!.text.toString() != "x"
            ) {

                buttons[i][4]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[i][4]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[i][3]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][4]!!.text.toString() == buttons[i][2]!!.text.toString() && buttons[i][4]!!.text.toString() != ""
                        || buttons[i][6]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][4]!!.text.toString() != ""
                        || buttons[i][6]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][7]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][4]!!.text.toString() != ""
                        )
                && buttons[i][4]!!.text.toString() == "o"
                && buttons[i][5]!!.text.toString() == ""
                && buttons[i][5]!!.text.toString() != "x"
            ) {

                buttons[i][5]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[i][5]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[i][3]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][4]!!.text.toString() == buttons[i][5]!!.text.toString() && buttons[i][4]!!.text.toString() != ""
                        || buttons[i][7]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][5]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][4]!!.text.toString() != ""
                        )
                && buttons[i][4]!!.text.toString() == "o"
                && buttons[i][6]!!.text.toString() == ""
                && buttons[i][6]!!.text.toString() != "x"
            ) {

                buttons[i][6]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[i][6]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[i][6]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][4]!!.text.toString() == buttons[i][5]!!.text.toString() && buttons[i][4]!!.text.toString() != "")
                && buttons[i][4]!!.text.toString() == "o"
                && buttons[i][7]!!.text.toString() == ""
                && buttons[i][7]!!.text.toString() != "x"
            ) {

                buttons[i][7]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[i][7]?.setText("o")!!
                player1Turn = !player1Turn
                break

            }

            /******************************* Vertical Lines********************************/

            else if ((buttons[1][i]!!.text.toString() == buttons[2][i]!!.text.toString() && buttons[1][i]!!.text.toString() == buttons[3][i]!!.text.toString() && buttons[1][i]!!.text.toString() != "")
                && buttons[1][i]!!.text.toString() == "o"
                && buttons[0][i]!!.text.toString() == ""
                && buttons[0][i]!!.text.toString() != "x"
            ) {

                buttons[0][i]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[0][i]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[3][i]!!.text.toString() == buttons[2][i]!!.text.toString() && buttons[4][i]!!.text.toString() == buttons[3][i]!!.text.toString() && buttons[3][i]!!.text.toString() != ""
                        || buttons[3][i]!!.text.toString() == buttons[2][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[0][i]!!.text.toString() && buttons[3][i]!!.text.toString() != "")
                && buttons[3][i]!!.text.toString() == "o"
                && buttons[1][i]!!.text.toString() == ""
                && buttons[1][i]!!.text.toString() != "x"
            ) {

                buttons[1][i]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[1][i]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[3][i]!!.text.toString() == buttons[1][i]!!.text.toString() && buttons[0][i]!!.text.toString() == buttons[3][i]!!.text.toString() && buttons[3][i]!!.text.toString() != ""
                        || buttons[3][i]!!.text.toString() == buttons[1][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[3][i]!!.text.toString() != ""
                        || buttons[3][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[5][i]!!.text.toString() && buttons[3][i]!!.text.toString() != ""
                        )
                && buttons[3][i]!!.text.toString() == "o"
                && buttons[2][i]!!.text.toString() == ""
                && buttons[2][i]!!.text.toString() != "x"
            ) {

                buttons[2][i]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[2][i]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[0][i]!!.text.toString() == buttons[1][i]!!.text.toString() && buttons[1][i]!!.text.toString() == buttons[2][i]!!.text.toString() && buttons[1][i]!!.text.toString() != "" && buttons[1][i]!!.text.toString() == "o"
                        || buttons[4][i]!!.text.toString() == buttons[1][i]!!.text.toString() && buttons[1][i]!!.text.toString() == buttons[2][i]!!.text.toString() && buttons[1][i]!!.text.toString() != "" && buttons[1][i]!!.text.toString() == "o"
                        || buttons[5][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[4][i]!!.text.toString() == buttons[6][i]!!.text.toString() && buttons[4][i]!!.text.toString() != "" && buttons[4][i]!!.text.toString() == "o"
                        || buttons[5][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[4][i]!!.text.toString() == buttons[2][i]!!.text.toString() && buttons[4][i]!!.text.toString() != "" && buttons[4][i]!!.text.toString() == "o"
                        )
                && buttons[3][i]!!.text.toString() == ""
                && buttons[3][i]!!.text.toString() != "x"
            ) {

                buttons[3][i]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[3][i]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[3][i]!!.text.toString() == buttons[1][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[2][i]!!.text.toString() && buttons[3][i]!!.text.toString() != "" && buttons[3][i]!!.text.toString() == "o"
                        || buttons[3][i]!!.text.toString() == buttons[2][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[5][i]!!.text.toString() && buttons[3][i]!!.text.toString() != "" && buttons[3][i]!!.text.toString() == "o"
                        || buttons[3][i]!!.text.toString() == buttons[6][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[5][i]!!.text.toString() && buttons[3][i]!!.text.toString() != "" && buttons[3][i]!!.text.toString() == "o"
                        || buttons[5][i]!!.text.toString() == buttons[6][i]!!.text.toString() && buttons[5][i]!!.text.toString() == buttons[7][i]!!.text.toString() && buttons[5][i]!!.text.toString() != "" && buttons[5][i]!!.text.toString() == "o"
                        )
                && buttons[4][i]!!.text.toString() == ""
                && buttons[4][i]!!.text.toString() != "x"
            ) {

                buttons[4][i]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[4][i]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[3][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[4][i]!!.text.toString() == buttons[2][i]!!.text.toString() && buttons[4][i]!!.text.toString() != ""
                        || buttons[6][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[4][i]!!.text.toString() != ""
                        || buttons[6][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[7][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[4][i]!!.text.toString() != ""
                        )
                && buttons[4][i]!!.text.toString() == "o"
                && buttons[5][i]!!.text.toString() == ""
                && buttons[5][i]!!.text.toString() != "x"
            ) {

                buttons[5][i]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[5][i]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[3][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[4][i]!!.text.toString() == buttons[5][i]!!.text.toString() && buttons[4][i]!!.text.toString() != ""
                        || buttons[7][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[5][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[4][i]!!.text.toString() != ""
                        )
                && buttons[4][i]!!.text.toString() == "o"
                && buttons[6][i]!!.text.toString() == ""
                && buttons[6][i]!!.text.toString() != "x"
            ) {

                buttons[6][i]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[6][i]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[6][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[4][i]!!.text.toString() == buttons[5][i]!!.text.toString() && buttons[4][i]!!.text.toString() != "")
                && buttons[4][i]!!.text.toString() == "o"
                && buttons[7][i]!!.text.toString() == ""
                && buttons[7][i]!!.text.toString() != "x"
            ) {

                buttons[7][i]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[7][i]?.setText("o")!!
                player1Turn = !player1Turn
                break
            }
        }
    }
    // check for o on Horizontal and Vertical to get 5 O in row
    /*** Done ***/
    private fun oFourInRow() {
        for (i in 0..7) {
            /******************************* Horizontal Lines********************************/
            if ((buttons[i][1]!!.text.toString() == buttons[i][2]!!.text.toString() && buttons[i][1]!!.text.toString() == buttons[i][3]!!.text.toString() && buttons[i][1]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][1]!!.text.toString() != "")
                && buttons[i][1]!!.text.toString() == "o"
                && buttons[i][0]!!.text.toString() == ""
                && buttons[i][0]!!.text.toString() != "x"
            ) {

                buttons[i][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[i][0]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[i][3]!!.text.toString() == buttons[i][2]!!.text.toString() && buttons[i][4]!!.text.toString() == buttons[i][3]!!.text.toString() && buttons[i][5]!!.text.toString() == buttons[i][3]!!.text.toString() && buttons[i][3]!!.text.toString() != ""
                        || buttons[i][3]!!.text.toString() == buttons[i][2]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][0]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][3]!!.text.toString() != "")
                && buttons[i][3]!!.text.toString() == "o"
                && buttons[i][1]!!.text.toString() == ""
                && buttons[i][1]!!.text.toString() != "x"
            ) {

                buttons[i][1]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[i][1]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[i][3]!!.text.toString() == buttons[i][1]!!.text.toString() && buttons[i][0]!!.text.toString() == buttons[i][3]!!.text.toString() && buttons[i][4]!!.text.toString() == buttons[i][3]!!.text.toString() && buttons[i][3]!!.text.toString() != ""
                        || buttons[i][3]!!.text.toString() == buttons[i][1]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][5]!!.text.toString() == buttons[i][3]!!.text.toString() && buttons[i][3]!!.text.toString() != ""
                        || buttons[i][3]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][5]!!.text.toString() && buttons[i][6]!!.text.toString() == buttons[i][3]!!.text.toString() && buttons[i][3]!!.text.toString() != ""
                        )
                && buttons[i][3]!!.text.toString() == "o"
                && buttons[i][2]!!.text.toString() == ""
                && buttons[i][2]!!.text.toString() != "x"
            ) {

                buttons[i][2]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[i][2]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[i][0]!!.text.toString() == buttons[i][1]!!.text.toString() && buttons[i][1]!!.text.toString() == buttons[i][2]!!.text.toString() && buttons[i][1]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][1]!!.text.toString() != "" && buttons[i][1]!!.text.toString() == "o"
                        || buttons[i][4]!!.text.toString() == buttons[i][1]!!.text.toString() && buttons[i][1]!!.text.toString() == buttons[i][2]!!.text.toString() && buttons[i][1]!!.text.toString() == buttons[i][5]!!.text.toString() && buttons[i][1]!!.text.toString() != "" && buttons[i][1]!!.text.toString() == "o"
                        || buttons[i][5]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][4]!!.text.toString() == buttons[i][6]!!.text.toString() && buttons[i][4]!!.text.toString() == buttons[i][2]!!.text.toString() && buttons[i][4]!!.text.toString() != "" && buttons[i][4]!!.text.toString() == "o"
                        || buttons[i][5]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][4]!!.text.toString() == buttons[i][6]!!.text.toString() && buttons[i][4]!!.text.toString() == buttons[i][7]!!.text.toString() && buttons[i][4]!!.text.toString() != "" && buttons[i][4]!!.text.toString() == "o"
                        )
                && buttons[i][3]!!.text.toString() == ""
                && buttons[i][3]!!.text.toString() != "x"
            ) {

                buttons[i][3]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[i][3]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[i][3]!!.text.toString() == buttons[i][1]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][2]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][0]!!.text.toString() && buttons[i][3]!!.text.toString() != "" && buttons[i][3]!!.text.toString() == "o"
                        || buttons[i][3]!!.text.toString() == buttons[i][2]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][5]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][1]!!.text.toString() && buttons[i][3]!!.text.toString() != "" && buttons[i][3]!!.text.toString() == "o"
                        || buttons[i][3]!!.text.toString() == buttons[i][6]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][5]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][2]!!.text.toString() && buttons[i][3]!!.text.toString() != "" && buttons[i][3]!!.text.toString() == "o"
                        || buttons[i][5]!!.text.toString() == buttons[i][6]!!.text.toString() && buttons[i][5]!!.text.toString() == buttons[i][7]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][5]!!.text.toString() && buttons[i][5]!!.text.toString() != "" && buttons[i][5]!!.text.toString() == "o"
                        )
                && buttons[i][4]!!.text.toString() == ""
                && buttons[i][4]!!.text.toString() != "x"
            ) {

                buttons[i][4]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[i][4]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[i][3]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][4]!!.text.toString() == buttons[i][2]!!.text.toString() && buttons[i][4]!!.text.toString() == buttons[i][1]!!.text.toString() && buttons[i][4]!!.text.toString() != ""
                        || buttons[i][6]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][2]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][4]!!.text.toString() != ""
                        || buttons[i][6]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][7]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][4]!!.text.toString() != ""
                        )
                && buttons[i][4]!!.text.toString() == "o"
                && buttons[i][5]!!.text.toString() == ""
                && buttons[i][5]!!.text.toString() != "x"
            ) {

                buttons[i][5]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[i][5]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[i][2]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][4]!!.text.toString() == buttons[i][5]!!.text.toString() && buttons[i][4]!!.text.toString() != ""
                        || buttons[i][3]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][7]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][5]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][4]!!.text.toString() != ""
                        )
                && buttons[i][4]!!.text.toString() == "o"
                && buttons[i][6]!!.text.toString() == ""
                && buttons[i][6]!!.text.toString() != "x"
            ) {

                buttons[i][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[i][6]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[i][3]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][6]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][4]!!.text.toString() == buttons[i][5]!!.text.toString() && buttons[i][4]!!.text.toString() != "")
                && buttons[i][4]!!.text.toString() == "o"
                && buttons[i][7]!!.text.toString() == ""
                && buttons[i][7]!!.text.toString() != "x"
            ) {

                buttons[i][7]?.background = ContextCompat.getDrawable(
                    this, R.drawable.o
                )
                buttons[i][7]?.setText("o")!!
                player1Turn = !player1Turn
                break

            }

            /******************************* Vertical Lines********************************/

            else if ((buttons[1][i]!!.text.toString() == buttons[2][i]!!.text.toString() && buttons[1][i]!!.text.toString() == buttons[3][i]!!.text.toString() && buttons[1][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[1][i]!!.text.toString() != "")
                && buttons[1][i]!!.text.toString() == "o"
                && buttons[0][i]!!.text.toString() == ""
                && buttons[0][i]!!.text.toString() != "x"
            ) {

                buttons[0][i]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[0][i]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[3][i]!!.text.toString() == buttons[2][i]!!.text.toString() && buttons[0][i]!!.text.toString() == buttons[3][i]!!.text.toString() && buttons[4][i]!!.text.toString() == buttons[3][i]!!.text.toString() && buttons[3][i]!!.text.toString() != ""
                        || buttons[3][i]!!.text.toString() == buttons[2][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[5][i]!!.text.toString() && buttons[3][i]!!.text.toString() != "")
                && buttons[3][i]!!.text.toString() == "o"
                && buttons[1][i]!!.text.toString() == ""
                && buttons[1][i]!!.text.toString() != "x"
            ) {

                buttons[1][i]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[1][i]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[3][i]!!.text.toString() == buttons[1][i]!!.text.toString() && buttons[0][i]!!.text.toString() == buttons[3][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[3][i]!!.text.toString() != ""
                        || buttons[3][i]!!.text.toString() == buttons[1][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[5][i]!!.text.toString() && buttons[3][i]!!.text.toString() != ""
                        || buttons[3][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[5][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[6][i]!!.text.toString() && buttons[3][i]!!.text.toString() != ""
                        )
                && buttons[3][i]!!.text.toString() == "o"
                && buttons[2][i]!!.text.toString() == ""
                && buttons[2][i]!!.text.toString() != "x"
            ) {

                buttons[2][i]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[2][i]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[0][i]!!.text.toString() == buttons[1][i]!!.text.toString() && buttons[1][i]!!.text.toString() == buttons[2][i]!!.text.toString() && buttons[1][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[1][i]!!.text.toString() != "" && buttons[1][i]!!.text.toString() == "o"
                        || buttons[4][i]!!.text.toString() == buttons[1][i]!!.text.toString() && buttons[1][i]!!.text.toString() == buttons[2][i]!!.text.toString() && buttons[1][i]!!.text.toString() == buttons[5][i]!!.text.toString() && buttons[1][i]!!.text.toString() != "" && buttons[1][i]!!.text.toString() == "o"
                        || buttons[5][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[4][i]!!.text.toString() == buttons[6][i]!!.text.toString() && buttons[4][i]!!.text.toString() == buttons[2][i]!!.text.toString() && buttons[4][i]!!.text.toString() != "" && buttons[4][i]!!.text.toString() == "o"
                        || buttons[5][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[4][i]!!.text.toString() == buttons[6][i]!!.text.toString() && buttons[4][i]!!.text.toString() == buttons[7][i]!!.text.toString() && buttons[4][i]!!.text.toString() != "" && buttons[4][i]!!.text.toString() == "o"
                        )
                && buttons[3][i]!!.text.toString() == ""
                && buttons[3][i]!!.text.toString() != "x"
            ) {

                buttons[3][i]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[3][i]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[3][i]!!.text.toString() == buttons[1][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[2][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[0][i]!!.text.toString() && buttons[3][i]!!.text.toString() != "" && buttons[3][i]!!.text.toString() == "o"
                        || buttons[3][i]!!.text.toString() == buttons[2][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[5][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[1][i]!!.text.toString() && buttons[3][i]!!.text.toString() != "" && buttons[3][i]!!.text.toString() == "o"
                        || buttons[3][i]!!.text.toString() == buttons[6][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[5][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[2][i]!!.text.toString() && buttons[3][i]!!.text.toString() != "" && buttons[3][i]!!.text.toString() == "o"
                        || buttons[5][i]!!.text.toString() == buttons[6][i]!!.text.toString() && buttons[5][i]!!.text.toString() == buttons[7][i]!!.text.toString() && buttons[5][i]!!.text.toString() == buttons[3][i]!!.text.toString() && buttons[5][i]!!.text.toString() != "" && buttons[5][i]!!.text.toString() == "o"
                        )
                && buttons[4][i]!!.text.toString() == ""
                && buttons[4][i]!!.text.toString() != "x"
            ) {

                buttons[4][i]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[4][i]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[3][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[4][i]!!.text.toString() == buttons[2][i]!!.text.toString() && buttons[4][i]!!.text.toString() == buttons[1][i]!!.text.toString() && buttons[4][i]!!.text.toString() != ""
                        || buttons[6][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[4][i]!!.text.toString() == buttons[2][i]!!.text.toString() && buttons[4][i]!!.text.toString() != ""
                        || buttons[6][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[7][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[4][i]!!.text.toString() == buttons[3][i]!!.text.toString() && buttons[4][i]!!.text.toString() != ""
                        )
                && buttons[4][i]!!.text.toString() == "o"
                && buttons[5][i]!!.text.toString() == ""
                && buttons[5][i]!!.text.toString() != "x"
            ) {

                buttons[5][i]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[5][i]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[2][i]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[4][i]!!.text.toString() == buttons[5][i]!!.text.toString() && buttons[4][i]!!.text.toString() != ""
                        || buttons[3][1]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[7][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[5][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[4][i]!!.text.toString() != ""
                        )
                && buttons[4][i]!!.text.toString() == "o"
                && buttons[6][i]!!.text.toString() == ""
                && buttons[6][i]!!.text.toString() != "x"
            ) {

                buttons[6][i]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[6][i]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[3][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[6][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[4][i]!!.text.toString() == buttons[5][i]!!.text.toString() && buttons[4][i]!!.text.toString() != "")
                && buttons[4][i]!!.text.toString() == "o"
                && buttons[7][i]!!.text.toString() == ""
                && buttons[7][i]!!.text.toString() != "x"
            ) {

                buttons[7][i]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[7][i]?.setText("o")!!
                player1Turn = !player1Turn
                break

            }
        }
        /**************** TopRight to BottomLeft ************************/
        if (player1Turn) {
            topRightBottomLift4InRawO()
        }
        /**************** TopLeft To BottomRight ************************/
        if (player1Turn) {
            topLiftBottomRight4InRawO()
        }
    }

    // check for o on Horizontal and Vertical to prevent 5 X in row
    /*** Done ***/
    private fun xFourInRow() {
        for (i in 0..7) {
            /******************************* Horizontal Lines********************************/
            if ((buttons[i][1]!!.text.toString() == buttons[i][2]!!.text.toString() && buttons[i][1]!!.text.toString() == buttons[i][3]!!.text.toString() && buttons[i][1]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][1]!!.text.toString() != "")
                && buttons[i][1]!!.text.toString() == "x"
                && buttons[i][0]!!.text.toString() == ""
                && buttons[i][0]!!.text.toString() != "x"
            ) {

                buttons[i][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[i][0]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[i][3]!!.text.toString() == buttons[i][2]!!.text.toString() && buttons[i][4]!!.text.toString() == buttons[i][3]!!.text.toString() && buttons[i][5]!!.text.toString() == buttons[i][3]!!.text.toString() && buttons[i][3]!!.text.toString() != ""
                        || buttons[i][3]!!.text.toString() == buttons[i][2]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][0]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][3]!!.text.toString() != "")
                && buttons[i][3]!!.text.toString() == "x"
                && buttons[i][1]!!.text.toString() == ""
                && buttons[i][1]!!.text.toString() != "x"
            ) {

                buttons[i][1]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[i][1]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[i][3]!!.text.toString() == buttons[i][1]!!.text.toString() && buttons[i][0]!!.text.toString() == buttons[i][3]!!.text.toString() && buttons[i][4]!!.text.toString() == buttons[i][3]!!.text.toString() && buttons[i][3]!!.text.toString() != ""
                        || buttons[i][3]!!.text.toString() == buttons[i][1]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][5]!!.text.toString() == buttons[i][3]!!.text.toString() && buttons[i][3]!!.text.toString() != ""
                        || buttons[i][3]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][5]!!.text.toString() && buttons[i][6]!!.text.toString() == buttons[i][3]!!.text.toString() && buttons[i][3]!!.text.toString() != ""
                        )
                && buttons[i][3]!!.text.toString() == "x"
                && buttons[i][2]!!.text.toString() == ""
                && buttons[i][2]!!.text.toString() != "x"
            ) {

                buttons[i][2]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[i][2]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[i][0]!!.text.toString() == buttons[i][1]!!.text.toString() && buttons[i][1]!!.text.toString() == buttons[i][2]!!.text.toString() && buttons[i][1]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][1]!!.text.toString() != "" && buttons[i][1]!!.text.toString() == "x"
                        || buttons[i][4]!!.text.toString() == buttons[i][1]!!.text.toString() && buttons[i][1]!!.text.toString() == buttons[i][2]!!.text.toString() && buttons[i][1]!!.text.toString() == buttons[i][5]!!.text.toString() && buttons[i][1]!!.text.toString() != "" && buttons[i][1]!!.text.toString() == "x"
                        || buttons[i][5]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][4]!!.text.toString() == buttons[i][6]!!.text.toString() && buttons[i][4]!!.text.toString() == buttons[i][2]!!.text.toString() && buttons[i][4]!!.text.toString() != "" && buttons[i][4]!!.text.toString() == "x"
                        || buttons[i][5]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][4]!!.text.toString() == buttons[i][6]!!.text.toString() && buttons[i][4]!!.text.toString() == buttons[i][7]!!.text.toString() && buttons[i][4]!!.text.toString() != "" && buttons[i][4]!!.text.toString() == "x"
                        )
                && buttons[i][3]!!.text.toString() == ""
                && buttons[i][3]!!.text.toString() != "x"
            ) {

                buttons[i][3]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[i][3]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[i][3]!!.text.toString() == buttons[i][1]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][2]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][0]!!.text.toString() && buttons[i][3]!!.text.toString() != "" && buttons[i][3]!!.text.toString() == "x"
                        || buttons[i][3]!!.text.toString() == buttons[i][2]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][5]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][1]!!.text.toString() && buttons[i][3]!!.text.toString() != "" && buttons[i][3]!!.text.toString() == "x"
                        || buttons[i][3]!!.text.toString() == buttons[i][6]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][5]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][2]!!.text.toString() && buttons[i][3]!!.text.toString() != "" && buttons[i][3]!!.text.toString() == "x"
                        || buttons[i][5]!!.text.toString() == buttons[i][6]!!.text.toString() && buttons[i][5]!!.text.toString() == buttons[i][7]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][5]!!.text.toString() && buttons[i][5]!!.text.toString() != "" && buttons[i][5]!!.text.toString() == "x"
                        )
                && buttons[i][4]!!.text.toString() == ""
                && buttons[i][4]!!.text.toString() != "x"
            ) {

                buttons[i][4]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[i][4]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[i][3]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][4]!!.text.toString() == buttons[i][2]!!.text.toString() && buttons[i][4]!!.text.toString() == buttons[i][1]!!.text.toString() && buttons[i][4]!!.text.toString() != ""
                        || buttons[i][6]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][2]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][4]!!.text.toString() != ""
                        || buttons[i][6]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][7]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][4]!!.text.toString() != ""
                        )
                && buttons[i][4]!!.text.toString() == "x"
                && buttons[i][5]!!.text.toString() == ""
                && buttons[i][5]!!.text.toString() != "x"
            ) {

                buttons[i][5]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[i][5]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[i][2]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][4]!!.text.toString() == buttons[i][5]!!.text.toString() && buttons[i][4]!!.text.toString() != ""
                        || buttons[i][3]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][7]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][5]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][4]!!.text.toString() != ""
                        )
                && buttons[i][4]!!.text.toString() == "x"
                && buttons[i][6]!!.text.toString() == ""
                && buttons[i][6]!!.text.toString() != "x"
            ) {

                buttons[i][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[i][6]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[i][3]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][6]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][4]!!.text.toString() == buttons[i][5]!!.text.toString() && buttons[i][4]!!.text.toString() != "")
                && buttons[i][4]!!.text.toString() == "x"
                && buttons[i][7]!!.text.toString() == ""
                && buttons[i][7]!!.text.toString() != "x"
            ) {

                buttons[i][7]?.background = ContextCompat.getDrawable(
                    this, R.drawable.o
                )
                buttons[i][7]?.setText("o")!!
                player1Turn = !player1Turn
                break

            }

            /******************************* Vertical Lines********************************/

            else if ((buttons[1][i]!!.text.toString() == buttons[2][i]!!.text.toString() && buttons[1][i]!!.text.toString() == buttons[3][i]!!.text.toString() && buttons[1][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[1][i]!!.text.toString() != "")
                && buttons[1][i]!!.text.toString() == "x"
                && buttons[0][i]!!.text.toString() == ""
                && buttons[0][i]!!.text.toString() != "x"
            ) {

                buttons[0][i]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[0][i]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[3][i]!!.text.toString() == buttons[2][i]!!.text.toString() && buttons[0][i]!!.text.toString() == buttons[3][i]!!.text.toString() && buttons[4][i]!!.text.toString() == buttons[3][i]!!.text.toString() && buttons[3][i]!!.text.toString() != ""
                        || buttons[3][i]!!.text.toString() == buttons[2][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[5][i]!!.text.toString() && buttons[3][i]!!.text.toString() != "")
                && buttons[3][i]!!.text.toString() == "x"
                && buttons[1][i]!!.text.toString() == ""
                && buttons[1][i]!!.text.toString() != "x"
            ) {

                buttons[1][i]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[1][i]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[3][i]!!.text.toString() == buttons[1][i]!!.text.toString() && buttons[0][i]!!.text.toString() == buttons[3][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[3][i]!!.text.toString() != ""
                        || buttons[3][i]!!.text.toString() == buttons[1][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[5][i]!!.text.toString() && buttons[3][i]!!.text.toString() != ""
                        || buttons[3][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[5][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[6][i]!!.text.toString() && buttons[3][i]!!.text.toString() != ""
                        )
                && buttons[3][i]!!.text.toString() == "x"
                && buttons[2][i]!!.text.toString() == ""
                && buttons[2][i]!!.text.toString() != "x"
            ) {

                buttons[2][i]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[2][i]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[0][i]!!.text.toString() == buttons[1][i]!!.text.toString() && buttons[1][i]!!.text.toString() == buttons[2][i]!!.text.toString() && buttons[1][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[1][i]!!.text.toString() != "" && buttons[1][i]!!.text.toString() == "x"
                        || buttons[4][i]!!.text.toString() == buttons[1][i]!!.text.toString() && buttons[1][i]!!.text.toString() == buttons[2][i]!!.text.toString() && buttons[1][i]!!.text.toString() == buttons[5][i]!!.text.toString() && buttons[1][i]!!.text.toString() != "" && buttons[1][i]!!.text.toString() == "x"
                        || buttons[5][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[4][i]!!.text.toString() == buttons[6][i]!!.text.toString() && buttons[4][i]!!.text.toString() == buttons[2][i]!!.text.toString() && buttons[4][i]!!.text.toString() != "" && buttons[4][i]!!.text.toString() == "x"
                        || buttons[5][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[4][i]!!.text.toString() == buttons[6][i]!!.text.toString() && buttons[4][i]!!.text.toString() == buttons[7][i]!!.text.toString() && buttons[4][i]!!.text.toString() != "" && buttons[4][i]!!.text.toString() == "x"
                        )
                && buttons[3][i]!!.text.toString() == ""
                && buttons[3][i]!!.text.toString() != "x"
            ) {

                buttons[3][i]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[3][i]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[3][i]!!.text.toString() == buttons[1][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[2][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[0][i]!!.text.toString() && buttons[3][i]!!.text.toString() != "" && buttons[3][i]!!.text.toString() == "x"
                        || buttons[3][i]!!.text.toString() == buttons[2][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[5][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[1][i]!!.text.toString() && buttons[3][i]!!.text.toString() != "" && buttons[3][i]!!.text.toString() == "x"
                        || buttons[3][i]!!.text.toString() == buttons[6][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[5][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[2][i]!!.text.toString() && buttons[3][i]!!.text.toString() != "" && buttons[3][i]!!.text.toString() == "x"
                        || buttons[5][i]!!.text.toString() == buttons[6][i]!!.text.toString() && buttons[5][i]!!.text.toString() == buttons[7][i]!!.text.toString() && buttons[5][i]!!.text.toString() == buttons[3][i]!!.text.toString() && buttons[5][i]!!.text.toString() != "" && buttons[5][i]!!.text.toString() == "x"
                        )
                && buttons[4][i]!!.text.toString() == ""
                && buttons[4][i]!!.text.toString() != "x"
            ) {

                buttons[4][i]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[4][i]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[3][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[4][i]!!.text.toString() == buttons[2][i]!!.text.toString() && buttons[4][i]!!.text.toString() == buttons[1][i]!!.text.toString() && buttons[4][i]!!.text.toString() != ""
                        || buttons[6][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[4][i]!!.text.toString() == buttons[2][i]!!.text.toString() && buttons[4][i]!!.text.toString() != ""
                        || buttons[6][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[7][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[4][i]!!.text.toString() == buttons[3][i]!!.text.toString() && buttons[4][i]!!.text.toString() != ""
                        )
                && buttons[4][i]!!.text.toString() == "x"
                && buttons[5][i]!!.text.toString() == ""
                && buttons[5][i]!!.text.toString() != "x"
            ) {

                buttons[5][i]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[5][i]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[2][i]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[4][i]!!.text.toString() == buttons[5][i]!!.text.toString() && buttons[4][i]!!.text.toString() != ""
                        || buttons[3][1]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[7][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[5][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[4][i]!!.text.toString() != ""
                        )
                && buttons[4][i]!!.text.toString() == "x"
                && buttons[6][i]!!.text.toString() == ""
                && buttons[6][i]!!.text.toString() != "x"
            ) {

                buttons[6][i]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[6][i]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[3][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[6][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[4][i]!!.text.toString() == buttons[5][i]!!.text.toString() && buttons[4][i]!!.text.toString() != "")
                && buttons[4][i]!!.text.toString() == "x"
                && buttons[7][i]!!.text.toString() == ""
                && buttons[7][i]!!.text.toString() != "x"
            ) {

                buttons[7][i]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[7][i]?.setText("o")!!
                player1Turn = !player1Turn
                break

            }
        }
        /**************** TopRight to BottomLeft ************************/
        if (player1Turn) {
            topRightBottomLift4InRawX()
        }
        /**************** TopLeft To BottomRight ************************/
        if (player1Turn) {
            topLiftBottomRight4InRawX()
        }
    }

    /*** Done ***/
    private fun xTwoByTwo() {

        /********************-1-***********************/
        if ((buttons[1][3]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[2][1]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[3][1]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[1][3]!!.text.toString() != "")
            && buttons[1][3]!!.text.toString() == "x"
            && buttons[1][1]!!.text.toString() == ""
            && buttons[1][1]!!.text.toString() != "x"
        ) {
            buttons[1][1]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[1][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[1][3]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() != ""
                    || buttons[1][1]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() == buttons[1][0]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() != ""
                    )
            && buttons[2][2]!!.text.toString() == "x"
            && buttons[1][2]!!.text.toString() == ""
            && buttons[1][2]!!.text.toString() != "x"
        ) {
            buttons[1][2]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[1][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[1][4]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[1][5]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[2][3]!!.text.toString() != ""
                    || buttons[1][1]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[2][3]!!.text.toString() != ""
                    )
            && buttons[2][3]!!.text.toString() == "x"
            && buttons[1][3]!!.text.toString() == ""
            && buttons[1][3]!!.text.toString() != "x"
        ) {
            buttons[1][3]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[1][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[1][5]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[1][6]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[2][4]!!.text.toString() != ""
                    || buttons[1][3]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[2][4]!!.text.toString() != ""
                    )
            && buttons[2][4]!!.text.toString() == "x"
            && buttons[1][4]!!.text.toString() == ""
            && buttons[1][4]!!.text.toString() != "x"
        ) {
            buttons[1][4]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[1][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[1][7]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[1][6]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[2][5]!!.text.toString() != ""
                    || buttons[1][3]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[2][5]!!.text.toString() != ""
                    )
            && buttons[2][5]!!.text.toString() == "x"
            && buttons[1][5]!!.text.toString() == ""
            && buttons[1][5]!!.text.toString() != "x"
        ) {
            buttons[1][5]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[1][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[1][5]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[2][6]!!.text.toString() == buttons[1][5]!!.text.toString() && buttons[3][6]!!.text.toString() == buttons[1][5]!!.text.toString() && buttons[1][5]!!.text.toString() != "")

            && buttons[1][5]!!.text.toString() == "x"
            && buttons[1][6]!!.text.toString() == ""
            && buttons[1][6]!!.text.toString() != "x"
        ) {
            buttons[1][6]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[1][6]?.setText("o")!!
            player1Turn = !player1Turn

        }

        /***************************for loop from 2 to 5*****************************/
        else if (player1Turn) {
            for (i in 2..5) {
                val j = i + 1
                val k = i + 2
                val s = i - 1
                val z = i - 2

                if ((buttons[i][2]!!.text.toString() == buttons[i][3]!!.text.toString() && buttons[i][2]!!.text.toString() == buttons[j][1]!!.text.toString() && buttons[i][2]!!.text.toString() == buttons[k][1]!!.text.toString() && buttons[i][2]!!.text.toString() != ""
                            || buttons[i][2]!!.text.toString() == buttons[i][3]!!.text.toString() && buttons[i][2]!!.text.toString() == buttons[s][1]!!.text.toString() && buttons[i][2]!!.text.toString() == buttons[z][1]!!.text.toString() && buttons[i][2]!!.text.toString() != ""
                            )
                    && buttons[i][2]!!.text.toString() == "x"
                    && buttons[i][1]!!.text.toString() == ""
                    && buttons[i][1]!!.text.toString() != "x"
                ) {
                    buttons[i][1]?.background = ContextCompat.getDrawable(
                        this,
                        R.drawable.o
                    )
                    buttons[i][1]?.setText("o")!!
                    player1Turn = !player1Turn
                    break

                } else if ((buttons[i][3]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[j][2]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[k][2]!!.text.toString() && buttons[i][3]!!.text.toString() != "" && buttons[i][3]!!.text.toString() == "x"
                            || buttons[i][3]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[s][2]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[z][2]!!.text.toString() && buttons[i][3]!!.text.toString() != "" && buttons[i][3]!!.text.toString() == "x"
                            || buttons[i][1]!!.text.toString() == buttons[i][0]!!.text.toString() && buttons[i][1]!!.text.toString() == buttons[j][2]!!.text.toString() && buttons[i][1]!!.text.toString() == buttons[k][2]!!.text.toString() && buttons[i][1]!!.text.toString() != "" && buttons[i][1]!!.text.toString() == "x"
                            || buttons[i][1]!!.text.toString() == buttons[i][0]!!.text.toString() && buttons[i][1]!!.text.toString() == buttons[s][2]!!.text.toString() && buttons[i][1]!!.text.toString() == buttons[z][2]!!.text.toString() && buttons[i][1]!!.text.toString() != "" && buttons[i][1]!!.text.toString() == "x"
                            )
                    && buttons[i][2]!!.text.toString() == ""
                    && buttons[i][2]!!.text.toString() != "x"
                ) {
                    buttons[i][2]?.background = ContextCompat.getDrawable(
                        this,
                        R.drawable.o
                    )
                    buttons[i][2]?.setText("o")!!
                    player1Turn = !player1Turn
                    break

                } else if ((buttons[i][5]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][5]!!.text.toString() == buttons[j][3]!!.text.toString() && buttons[i][5]!!.text.toString() == buttons[k][3]!!.text.toString() && buttons[i][5]!!.text.toString() != "" && buttons[i][5]!!.text.toString() == "x"
                            || buttons[i][5]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][5]!!.text.toString() == buttons[s][3]!!.text.toString() && buttons[i][5]!!.text.toString() == buttons[z][3]!!.text.toString() && buttons[i][5]!!.text.toString() != "" && buttons[i][5]!!.text.toString() == "x"
                            || buttons[i][1]!!.text.toString() == buttons[i][2]!!.text.toString() && buttons[i][1]!!.text.toString() == buttons[j][3]!!.text.toString() && buttons[i][1]!!.text.toString() == buttons[k][3]!!.text.toString() && buttons[i][1]!!.text.toString() != "" && buttons[i][1]!!.text.toString() == "x"
                            || buttons[i][1]!!.text.toString() == buttons[i][2]!!.text.toString() && buttons[i][1]!!.text.toString() == buttons[s][3]!!.text.toString() && buttons[i][1]!!.text.toString() == buttons[z][3]!!.text.toString() && buttons[i][1]!!.text.toString() != "" && buttons[i][1]!!.text.toString() == "x"
                            )
                    && buttons[i][3]!!.text.toString() == ""
                    && buttons[i][3]!!.text.toString() != "x"
                ) {
                    buttons[i][3]?.background = ContextCompat.getDrawable(
                        this,
                        R.drawable.o
                    )
                    buttons[i][3]?.setText("o")!!
                    player1Turn = !player1Turn
                    break

                } else if ((buttons[i][6]!!.text.toString() == buttons[i][5]!!.text.toString() && buttons[i][6]!!.text.toString() == buttons[j][4]!!.text.toString() && buttons[i][6]!!.text.toString() == buttons[k][4]!!.text.toString() && buttons[i][6]!!.text.toString() != "" && buttons[i][6]!!.text.toString() == "x"
                            || buttons[i][6]!!.text.toString() == buttons[i][5]!!.text.toString() && buttons[i][6]!!.text.toString() == buttons[s][4]!!.text.toString() && buttons[i][6]!!.text.toString() == buttons[z][4]!!.text.toString() && buttons[i][6]!!.text.toString() != "" && buttons[i][6]!!.text.toString() == "x"
                            || buttons[i][2]!!.text.toString() == buttons[i][3]!!.text.toString() && buttons[i][2]!!.text.toString() == buttons[j][4]!!.text.toString() && buttons[i][2]!!.text.toString() == buttons[k][4]!!.text.toString() && buttons[i][2]!!.text.toString() != "" && buttons[i][2]!!.text.toString() == "x"
                            || buttons[i][2]!!.text.toString() == buttons[i][3]!!.text.toString() && buttons[i][2]!!.text.toString() == buttons[s][4]!!.text.toString() && buttons[i][2]!!.text.toString() == buttons[z][4]!!.text.toString() && buttons[i][2]!!.text.toString() != "" && buttons[i][2]!!.text.toString() == "x"
                            )
                    && buttons[i][4]!!.text.toString() == ""
                    && buttons[i][4]!!.text.toString() != "x"
                ) {
                    buttons[i][4]?.background = ContextCompat.getDrawable(
                        this,
                        R.drawable.o
                    )
                    buttons[i][4]?.setText("o")!!
                    player1Turn = !player1Turn
                    break

                } else if ((buttons[i][7]!!.text.toString() == buttons[i][6]!!.text.toString() && buttons[i][7]!!.text.toString() == buttons[j][5]!!.text.toString() && buttons[i][7]!!.text.toString() == buttons[k][5]!!.text.toString() && buttons[i][7]!!.text.toString() != "" && buttons[i][7]!!.text.toString() == "x"
                            || buttons[i][7]!!.text.toString() == buttons[i][6]!!.text.toString() && buttons[i][7]!!.text.toString() == buttons[s][5]!!.text.toString() && buttons[i][7]!!.text.toString() == buttons[z][5]!!.text.toString() && buttons[i][7]!!.text.toString() != "" && buttons[i][7]!!.text.toString() == "x"
                            || buttons[i][3]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[j][5]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[k][5]!!.text.toString() && buttons[i][3]!!.text.toString() != "" && buttons[i][3]!!.text.toString() == "x"
                            || buttons[i][3]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[s][5]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[z][5]!!.text.toString() && buttons[i][3]!!.text.toString() != "" && buttons[i][3]!!.text.toString() == "x"
                            )
                    && buttons[i][5]!!.text.toString() == ""
                    && buttons[i][5]!!.text.toString() != "x"
                ) {
                    buttons[i][5]?.background = ContextCompat.getDrawable(
                        this,
                        R.drawable.o
                    )
                    buttons[i][5]?.setText("o")!!
                    player1Turn = !player1Turn
                    break

                } else if ((buttons[i][5]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][5]!!.text.toString() == buttons[j][3]!!.text.toString() && buttons[i][5]!!.text.toString() == buttons[k][6]!!.text.toString() && buttons[i][5]!!.text.toString() != ""
                            || buttons[i][5]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][5]!!.text.toString() == buttons[s][3]!!.text.toString() && buttons[i][5]!!.text.toString() == buttons[z][6]!!.text.toString() && buttons[i][5]!!.text.toString() != ""
                            )
                    && buttons[i][5]!!.text.toString() == "x"
                    && buttons[i][6]!!.text.toString() == ""
                    && buttons[i][6]!!.text.toString() != "x"
                ) {
                    buttons[i][6]?.background = ContextCompat.getDrawable(
                        this,
                        R.drawable.o
                    )
                    buttons[i][6]?.setText("o")!!
                    player1Turn = !player1Turn
                    break
                }
            }
        }

        /***************************-6-*****************************/
        else if ((buttons[5][1]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[5][1]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[6][2]!!.text.toString() == buttons[5][1]!!.text.toString() && buttons[5][1]!!.text.toString() != "")
            && buttons[5][1]!!.text.toString() == "x"
            && buttons[6][1]!!.text.toString() == ""
            && buttons[6][1]!!.text.toString() != "x"
        ) {
            buttons[6][1]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[6][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[6][3]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[5][2]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[5][2]!!.text.toString() != ""
                    || buttons[6][1]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[5][2]!!.text.toString() == buttons[6][0]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[5][2]!!.text.toString() != ""
                    )
            && buttons[5][2]!!.text.toString() == "x"
            && buttons[6][2]!!.text.toString() == ""
            && buttons[6][2]!!.text.toString() != "x"
        ) {
            buttons[6][2]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[6][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[6][4]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[5][3]!!.text.toString() != ""
                    || buttons[6][1]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[6][2]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[5][3]!!.text.toString() != ""
                    )
            && buttons[5][3]!!.text.toString() == "x"
            && buttons[6][3]!!.text.toString() == ""
            && buttons[6][3]!!.text.toString() != "x"
        ) {
            buttons[6][3]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[6][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[6][5]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[6][6]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[5][4]!!.text.toString() != ""
                    || buttons[6][3]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[6][2]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[5][4]!!.text.toString() != ""
                    )
            && buttons[5][4]!!.text.toString() == "x"
            && buttons[6][4]!!.text.toString() == ""
            && buttons[6][4]!!.text.toString() != "x"
        ) {
            buttons[6][4]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[6][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[6][7]!!.text.toString() == buttons[5][5]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[6][6]!!.text.toString() && buttons[4][5]!!.text.toString() == buttons[5][5]!!.text.toString() && buttons[5][5]!!.text.toString() != ""
                    || buttons[6][3]!!.text.toString() == buttons[5][5]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[4][5]!!.text.toString() == buttons[5][5]!!.text.toString() && buttons[5][5]!!.text.toString() != ""
                    )
            && buttons[5][5]!!.text.toString() == "x"
            && buttons[6][5]!!.text.toString() == ""
            && buttons[6][5]!!.text.toString() != "x"
        ) {
            buttons[6][5]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[6][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][6]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[6][5]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[6][4]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[6][5]!!.text.toString() != "")

            && buttons[6][5]!!.text.toString() == "x"
            && buttons[6][6]!!.text.toString() == ""
            && buttons[6][6]!!.text.toString() != "x"
        ) {
            buttons[6][6]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[6][6]?.setText("o")!!
            player1Turn = !player1Turn

        }


    }

    /*** Done ***/
    private fun oTwoByTwo() {

        /********************-1-***********************/
        if ((buttons[1][3]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[2][1]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[3][1]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[1][3]!!.text.toString() != "")
            && buttons[1][3]!!.text.toString() == "o"
            && buttons[1][1]!!.text.toString() == ""
            && buttons[1][1]!!.text.toString() != "x"
        ) {
            buttons[1][1]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[1][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[1][3]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() != ""
                    || buttons[1][1]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() == buttons[1][0]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() != ""
                    )
            && buttons[2][2]!!.text.toString() == "o"
            && buttons[1][2]!!.text.toString() == ""
            && buttons[1][2]!!.text.toString() != "x"
        ) {
            buttons[1][2]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[1][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[1][4]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[1][5]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[2][3]!!.text.toString() != ""
                    || buttons[1][1]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[2][3]!!.text.toString() != ""
                    )
            && buttons[2][3]!!.text.toString() == "o"
            && buttons[1][3]!!.text.toString() == ""
            && buttons[1][3]!!.text.toString() != "x"
        ) {
            buttons[1][3]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[1][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[1][5]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[1][6]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[2][4]!!.text.toString() != ""
                    || buttons[1][3]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[2][4]!!.text.toString() != ""
                    )
            && buttons[2][4]!!.text.toString() == "o"
            && buttons[1][4]!!.text.toString() == ""
            && buttons[1][4]!!.text.toString() != "x"
        ) {
            buttons[1][4]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[1][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[1][7]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[1][6]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[2][5]!!.text.toString() != ""
                    || buttons[1][3]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[2][5]!!.text.toString() != ""
                    )
            && buttons[2][5]!!.text.toString() == "o"
            && buttons[1][5]!!.text.toString() == ""
            && buttons[1][5]!!.text.toString() != "x"
        ) {
            buttons[1][5]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[1][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[1][5]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[2][6]!!.text.toString() == buttons[1][5]!!.text.toString() && buttons[3][6]!!.text.toString() == buttons[1][5]!!.text.toString() && buttons[1][5]!!.text.toString() != "")

            && buttons[1][5]!!.text.toString() == "o"
            && buttons[1][6]!!.text.toString() == ""
            && buttons[1][6]!!.text.toString() != "x"
        ) {
            buttons[1][6]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[1][6]?.setText("o")!!
            player1Turn = !player1Turn

        }
        /***************************for loop from 2 to 5*****************************/
        else if (player1Turn) {
            for (i in 2..5) {
                val j = i + 1
                val k = i + 2
                val s = i - 1
                val z = i - 2

                if ((buttons[i][2]!!.text.toString() == buttons[i][3]!!.text.toString() && buttons[i][2]!!.text.toString() == buttons[j][1]!!.text.toString() && buttons[i][2]!!.text.toString() == buttons[k][1]!!.text.toString() && buttons[i][2]!!.text.toString() != ""
                            || buttons[i][2]!!.text.toString() == buttons[i][3]!!.text.toString() && buttons[i][2]!!.text.toString() == buttons[s][1]!!.text.toString() && buttons[i][2]!!.text.toString() == buttons[z][1]!!.text.toString() && buttons[i][2]!!.text.toString() != ""
                            )
                    && buttons[i][2]!!.text.toString() == "o"
                    && buttons[i][1]!!.text.toString() == ""
                    && buttons[i][1]!!.text.toString() != "x"
                ) {
                    buttons[i][1]?.background = ContextCompat.getDrawable(
                        this,
                        R.drawable.o
                    )
                    buttons[i][1]?.setText("o")!!
                    player1Turn = !player1Turn
                    break

                } else if ((buttons[i][3]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[j][2]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[k][2]!!.text.toString() && buttons[i][3]!!.text.toString() != "" && buttons[i][3]!!.text.toString() == "o"
                            || buttons[i][3]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[s][2]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[z][2]!!.text.toString() && buttons[i][3]!!.text.toString() != "" && buttons[i][3]!!.text.toString() == "o"
                            || buttons[i][1]!!.text.toString() == buttons[i][0]!!.text.toString() && buttons[i][1]!!.text.toString() == buttons[j][2]!!.text.toString() && buttons[i][1]!!.text.toString() == buttons[k][2]!!.text.toString() && buttons[i][1]!!.text.toString() != "" && buttons[i][1]!!.text.toString() == "o"
                            || buttons[i][1]!!.text.toString() == buttons[i][0]!!.text.toString() && buttons[i][1]!!.text.toString() == buttons[s][2]!!.text.toString() && buttons[i][1]!!.text.toString() == buttons[z][2]!!.text.toString() && buttons[i][1]!!.text.toString() != "" && buttons[i][1]!!.text.toString() == "o"
                            )
                    && buttons[i][2]!!.text.toString() == ""
                    && buttons[i][2]!!.text.toString() != "x"
                ) {
                    buttons[i][2]?.background = ContextCompat.getDrawable(
                        this,
                        R.drawable.o
                    )
                    buttons[i][2]?.setText("o")!!
                    player1Turn = !player1Turn
                    break

                } else if ((buttons[i][5]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][5]!!.text.toString() == buttons[j][3]!!.text.toString() && buttons[i][5]!!.text.toString() == buttons[k][3]!!.text.toString() && buttons[i][5]!!.text.toString() != "" && buttons[i][5]!!.text.toString() == "o"
                            || buttons[i][5]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][5]!!.text.toString() == buttons[s][3]!!.text.toString() && buttons[i][5]!!.text.toString() == buttons[z][3]!!.text.toString() && buttons[i][5]!!.text.toString() != "" && buttons[i][5]!!.text.toString() == "o"
                            || buttons[i][1]!!.text.toString() == buttons[i][2]!!.text.toString() && buttons[i][1]!!.text.toString() == buttons[j][3]!!.text.toString() && buttons[i][1]!!.text.toString() == buttons[k][3]!!.text.toString() && buttons[i][1]!!.text.toString() != "" && buttons[i][1]!!.text.toString() == "o"
                            || buttons[i][1]!!.text.toString() == buttons[i][2]!!.text.toString() && buttons[i][1]!!.text.toString() == buttons[s][3]!!.text.toString() && buttons[i][1]!!.text.toString() == buttons[z][3]!!.text.toString() && buttons[i][1]!!.text.toString() != "" && buttons[i][1]!!.text.toString() == "o"
                            )
                    && buttons[i][3]!!.text.toString() == ""
                    && buttons[i][3]!!.text.toString() != "x"
                ) {
                    buttons[i][3]?.background = ContextCompat.getDrawable(
                        this,
                        R.drawable.o
                    )
                    buttons[i][3]?.setText("o")!!
                    player1Turn = !player1Turn
                    break

                } else if ((buttons[i][6]!!.text.toString() == buttons[i][5]!!.text.toString() && buttons[i][6]!!.text.toString() == buttons[j][4]!!.text.toString() && buttons[i][6]!!.text.toString() == buttons[k][4]!!.text.toString() && buttons[i][6]!!.text.toString() != "" && buttons[i][6]!!.text.toString() == "O"
                            || buttons[i][6]!!.text.toString() == buttons[i][5]!!.text.toString() && buttons[i][6]!!.text.toString() == buttons[s][4]!!.text.toString() && buttons[i][6]!!.text.toString() == buttons[z][4]!!.text.toString() && buttons[i][6]!!.text.toString() != "" && buttons[i][6]!!.text.toString() == "o"
                            || buttons[i][2]!!.text.toString() == buttons[i][3]!!.text.toString() && buttons[i][2]!!.text.toString() == buttons[j][4]!!.text.toString() && buttons[i][2]!!.text.toString() == buttons[k][4]!!.text.toString() && buttons[i][2]!!.text.toString() != "" && buttons[i][2]!!.text.toString() == "o"
                            || buttons[i][2]!!.text.toString() == buttons[i][3]!!.text.toString() && buttons[i][2]!!.text.toString() == buttons[s][4]!!.text.toString() && buttons[i][2]!!.text.toString() == buttons[z][4]!!.text.toString() && buttons[i][2]!!.text.toString() != "" && buttons[i][2]!!.text.toString() == "o"
                            )
                    && buttons[i][4]!!.text.toString() == ""
                    && buttons[i][4]!!.text.toString() != "x"
                ) {
                    buttons[i][4]?.background = ContextCompat.getDrawable(
                        this,
                        R.drawable.o
                    )
                    buttons[i][4]?.setText("o")!!
                    player1Turn = !player1Turn
                    break

                } else if ((buttons[i][7]!!.text.toString() == buttons[i][6]!!.text.toString() && buttons[i][7]!!.text.toString() == buttons[j][5]!!.text.toString() && buttons[i][7]!!.text.toString() == buttons[k][5]!!.text.toString() && buttons[i][7]!!.text.toString() != "" && buttons[i][7]!!.text.toString() == "o"
                            || buttons[i][7]!!.text.toString() == buttons[i][6]!!.text.toString() && buttons[i][7]!!.text.toString() == buttons[s][5]!!.text.toString() && buttons[i][7]!!.text.toString() == buttons[z][5]!!.text.toString() && buttons[i][7]!!.text.toString() != "" && buttons[i][7]!!.text.toString() == "o"
                            || buttons[i][3]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[j][5]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[k][5]!!.text.toString() && buttons[i][3]!!.text.toString() != "" && buttons[i][3]!!.text.toString() == "o"
                            || buttons[i][3]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[s][5]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[z][5]!!.text.toString() && buttons[i][3]!!.text.toString() != "" && buttons[i][3]!!.text.toString() == "o"
                            )
                    && buttons[i][5]!!.text.toString() == ""
                    && buttons[i][5]!!.text.toString() != "x"
                ) {
                    buttons[i][5]?.background = ContextCompat.getDrawable(
                        this,
                        R.drawable.o
                    )
                    buttons[i][5]?.setText("o")!!
                    player1Turn = !player1Turn
                    break

                } else if ((buttons[i][5]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][5]!!.text.toString() == buttons[j][3]!!.text.toString() && buttons[i][5]!!.text.toString() == buttons[k][6]!!.text.toString() && buttons[i][5]!!.text.toString() != ""
                            || buttons[i][5]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][5]!!.text.toString() == buttons[s][3]!!.text.toString() && buttons[i][5]!!.text.toString() == buttons[z][6]!!.text.toString() && buttons[i][5]!!.text.toString() != ""
                            )
                    && buttons[i][5]!!.text.toString() == "o"
                    && buttons[i][6]!!.text.toString() == ""
                    && buttons[i][6]!!.text.toString() != "x"
                ) {
                    buttons[i][6]?.background = ContextCompat.getDrawable(
                        this,
                        R.drawable.o
                    )
                    buttons[i][6]?.setText("o")!!
                    player1Turn = !player1Turn
                    break
                }
            }
        }
        /***************************-6-*****************************/
        else if ((buttons[5][1]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[5][1]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[6][2]!!.text.toString() == buttons[5][1]!!.text.toString() && buttons[5][1]!!.text.toString() != "")
            && buttons[5][1]!!.text.toString() == "o"
            && buttons[6][1]!!.text.toString() == ""
            && buttons[6][1]!!.text.toString() != "x"
        ) {
            buttons[6][1]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[6][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[6][3]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[5][2]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[5][2]!!.text.toString() != ""
                    || buttons[6][1]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[5][2]!!.text.toString() == buttons[6][0]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[5][2]!!.text.toString() != ""
                    )
            && buttons[5][2]!!.text.toString() == "o"
            && buttons[6][2]!!.text.toString() == ""
            && buttons[6][2]!!.text.toString() != "x"
        ) {
            buttons[6][2]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[6][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[6][4]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[5][3]!!.text.toString() != ""
                    || buttons[6][1]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[6][2]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[5][3]!!.text.toString() != ""
                    )
            && buttons[5][3]!!.text.toString() == "o"
            && buttons[6][3]!!.text.toString() == ""
            && buttons[6][3]!!.text.toString() != "x"
        ) {
            buttons[6][3]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[6][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[6][5]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[6][6]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[5][4]!!.text.toString() != ""
                    || buttons[6][3]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[6][2]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[5][4]!!.text.toString() != ""
                    )
            && buttons[5][4]!!.text.toString() == "o"
            && buttons[6][4]!!.text.toString() == ""
            && buttons[6][4]!!.text.toString() != "x"
        ) {
            buttons[6][4]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[6][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[6][7]!!.text.toString() == buttons[5][5]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[6][6]!!.text.toString() && buttons[4][5]!!.text.toString() == buttons[5][5]!!.text.toString() && buttons[5][5]!!.text.toString() != ""
                    || buttons[6][3]!!.text.toString() == buttons[5][5]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[4][5]!!.text.toString() == buttons[5][5]!!.text.toString() && buttons[5][5]!!.text.toString() != ""
                    )
            && buttons[5][5]!!.text.toString() == "o"
            && buttons[6][5]!!.text.toString() == ""
            && buttons[6][5]!!.text.toString() != "x"
        ) {
            buttons[6][5]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[6][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][6]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[6][5]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[6][4]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[6][5]!!.text.toString() != "")

            && buttons[6][5]!!.text.toString() == "o"
            && buttons[6][6]!!.text.toString() == ""
            && buttons[6][6]!!.text.toString() != "x"
        ) {
            buttons[6][6]?.background = ContextCompat.getDrawable(
                this,
                R.drawable.o
            )
            buttons[6][6]?.setText("o")!!
            player1Turn = !player1Turn

        }
    }

    /*** Done ***/
    private fun ifNothingToDO1() {
        when {
            buttons[3][3]!!.text.toString() == "" -> {
                buttons[3][3]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[3][3]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[3][4]!!.text.toString() == "" -> {
                buttons[3][4]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[3][4]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[3][5]!!.text.toString() == "" -> {
                buttons[3][5]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[3][5]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[3][6]!!.text.toString() == "" -> {
                buttons[3][6]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[3][6]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[6][1]!!.text.toString() == "" -> {
                buttons[6][1]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[6][1]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[5][1]!!.text.toString() == "" -> {
                buttons[5][1]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[5][1]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[4][1]!!.text.toString() == "" -> {
                buttons[4][1]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[4][1]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[3][1]!!.text.toString() == "" -> {
                buttons[3][1]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[3][1]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[1][2]!!.text.toString() == "" -> {
                buttons[1][2]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[1][2]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[2][3]!!.text.toString() == "" -> {
                buttons[2][3]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[2][3]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[4][5]!!.text.toString() == "" -> {
                buttons[4][5]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[4][5]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[5][6]!!.text.toString() == "" -> {
                buttons[5][6]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[5][6]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[1][7]!!.text.toString() == "" -> {
                buttons[1][7]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[1][7]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[2][7]!!.text.toString() == "" -> {
                buttons[2][7]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[2][7]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[3][7]!!.text.toString() == "" -> {
                buttons[3][7]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[3][7]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[4][7]!!.text.toString() == "" -> {
                buttons[4][7]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[4][7]?.setText("o")!!
                player1Turn = !player1Turn
            }
        }
    }

    /*** Done ***/
    private fun ifNothingToDO2() {
        when {

            buttons[2][5]!!.text.toString() == "" -> {
                buttons[2][5]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[2][5]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[1][4]!!.text.toString() == "" -> {
                buttons[1][4]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[1][4]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[6][0]!!.text.toString() == "" -> {
                buttons[6][0]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[6][0]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[5][0]!!.text.toString() == "" -> {
                buttons[5][0]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[5][0]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[4][0]!!.text.toString() == "" -> {
                buttons[4][0]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[4][0]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[3][0]!!.text.toString() == "" -> {
                buttons[3][0]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[3][0]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[6][2]!!.text.toString() == "" -> {
                buttons[6][2]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[6][2]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[6][4]!!.text.toString() == "" -> {
                buttons[6][4]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[6][4]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[6][5]!!.text.toString() == "" -> {
                buttons[6][5]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[6][5]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[6][3]!!.text.toString() == "" -> {
                buttons[6][3]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[6][3]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[5][2]!!.text.toString() == "" -> {
                buttons[5][2]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[5][2]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[4][2]!!.text.toString() == "" -> {
                buttons[4][2]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[4][2]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[3][2]!!.text.toString() == "" -> {
                buttons[3][2]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[3][2]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[7][2]!!.text.toString() == "" -> {
                buttons[7][2]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[7][2]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[7][3]!!.text.toString() == "" -> {
                buttons[7][3]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[7][3]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[7][5]!!.text.toString() == "" -> {
                buttons[7][5]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[7][5]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[7][4]!!.text.toString() == "" -> {
                buttons[7][4]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[7][4]?.setText("o")!!
                player1Turn = !player1Turn
            }
        }
    }

    /*** Done ***/
    private fun ifNothingToDO3() {
        when {
            buttons[0][1]!!.text.toString() == "" -> {
                buttons[0][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[0][1]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[0][3]!!.text.toString() == "" -> {
                buttons[0][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[0][3]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[0][4]!!.text.toString() == "" -> {
                buttons[0][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[0][4]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[0][0]!!.text.toString() == "" -> {
                buttons[0][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[0][0]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[2][2]!!.text.toString() == "" -> {
                buttons[2][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[2][2]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[1][1]!!.text.toString() == "" -> {
                buttons[1][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[1][1]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[4][4]!!.text.toString() == "" -> {
                buttons[4][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[4][4]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[5][7]!!.text.toString() == "" -> {
                buttons[5][7]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[5][7]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[4][6]!!.text.toString() == "" -> {
                buttons[4][6]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[4][6]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[6][7]!!.text.toString() == "" -> {
                buttons[6][7]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[6][7]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[7][1]!!.text.toString() == "" -> {
                buttons[7][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[7][1]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[2][1]!!.text.toString() == "" -> {
                buttons[2][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[2][1]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[2][6]!!.text.toString() == "" -> {
                buttons[2][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[2][6]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[1][0]!!.text.toString() == "" -> {
                buttons[1][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[1][0]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[0][6]!!.text.toString() == "" -> {
                buttons[0][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[0][6]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[2][4]!!.text.toString() == "" -> {
                buttons[2][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[2][4]?.setText("o")!!
                player1Turn = !player1Turn
            }

        }
    }

    /*** Done ***/
    private fun ifNothingToDO4() {
        when {
            buttons[5][5]!!.text.toString() == "" -> {
                buttons[5][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[5][5]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[7][7]!!.text.toString() == "" -> {
                buttons[7][7]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[7][7]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[6][6]!!.text.toString() == "" -> {
                buttons[6][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[6][6]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[7][0]!!.text.toString() == "" -> {
                buttons[7][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[7][0]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[2][0]!!.text.toString() == "" -> {
                buttons[2][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[2][0]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[5][4]!!.text.toString() == "" -> {
                buttons[5][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[5][4]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[4][3]!!.text.toString() == "" -> {
                buttons[4][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[4][3]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[1][5]!!.text.toString() == "" -> {
                buttons[1][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[1][5]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[1][3]!!.text.toString() == "" -> {
                buttons[1][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[1][3]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[0][5]!!.text.toString() == "" -> {
                buttons[0][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[0][5]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[0][2]!!.text.toString() == "" -> {
                buttons[0][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[0][2]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[5][3]!!.text.toString() == "" -> {
                buttons[5][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[5][3]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[1][6]!!.text.toString() == "" -> {
                buttons[1][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[1][6]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[0][7]!!.text.toString() == "" -> {
                buttons[0][7]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[0][7]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[7][6]!!.text.toString() == "" -> {
                buttons[7][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[7][6]?.setText("o")!!
                player1Turn = !player1Turn
            }
        }
    }

    /*** Done ***/
    private fun ifNothingToDoMain1() {
        when {
            buttons[4][3]!!.text.toString() == "" -> {
                buttons[4][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[4][3]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[4][4]!!.text.toString() == "" -> {
                buttons[4][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[4][4]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[3][4]!!.text.toString() == "" -> {
                buttons[3][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[3][4]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[4][2]!!.text.toString() == "" -> {
                buttons[4][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[4][2]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[3][6]!!.text.toString() == "" -> {
                buttons[3][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[3][6]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[1][3]!!.text.toString() == "" -> {
                buttons[1][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[1][3]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[5][5]!!.text.toString() == "" -> {
                buttons[5][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[5][5]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[4][1]!!.text.toString() == "" -> {
                buttons[4][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[4][1]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[3][3]!!.text.toString() == "" -> {
                buttons[3][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[3][3]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[6][1]!!.text.toString() == "" -> {
                buttons[6][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[6][1]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[4][5]!!.text.toString() == "" -> {
                buttons[4][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[4][5]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[5][3]!!.text.toString() == "" -> {
                buttons[5][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[5][3]?.setText("o")!!
                player1Turn = !player1Turn
            }
        }
    }

    /*** Done ***/
    private fun ifNothingToDoMain2() {
        when {

            buttons[3][6]!!.text.toString() == "" -> {
                buttons[3][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[3][6]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[1][3]!!.text.toString() == "" -> {
                buttons[1][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[1][3]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[5][5]!!.text.toString() == "" -> {
                buttons[5][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[5][5]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[4][1]!!.text.toString() == "" -> {
                buttons[4][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[4][1]?.setText("o")!!
                player1Turn = !player1Turn
            }

            buttons[4][3]!!.text.toString() == "" -> {
                buttons[4][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[4][3]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[4][4]!!.text.toString() == "" -> {
                buttons[4][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[4][4]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[3][4]!!.text.toString() == "" -> {
                buttons[3][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[3][4]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[4][2]!!.text.toString() == "" -> {
                buttons[4][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[4][2]?.setText("o")!!
                player1Turn = !player1Turn
            }

            buttons[3][3]!!.text.toString() == "" -> {
                buttons[3][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[3][3]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[6][1]!!.text.toString() == "" -> {
                buttons[6][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[6][1]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[4][5]!!.text.toString() == "" -> {
                buttons[4][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[4][5]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[5][3]!!.text.toString() == "" -> {
                buttons[5][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[5][3]?.setText("o")!!
                player1Turn = !player1Turn
            }

        }
    }

    /*** Done ***/
    private fun ifNothingToDoMain3() {
        when {

            buttons[3][6]!!.text.toString() == "" -> {
                buttons[3][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[3][6]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[1][3]!!.text.toString() == "" -> {
                buttons[1][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[1][3]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[3][3]!!.text.toString() == "" -> {
                buttons[3][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[3][3]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[6][1]!!.text.toString() == "" -> {
                buttons[6][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[6][1]?.setText("o")!!
                player1Turn = !player1Turn
            }

            buttons[5][5]!!.text.toString() == "" -> {
                buttons[5][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[5][5]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[4][1]!!.text.toString() == "" -> {
                buttons[4][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[4][1]?.setText("o")!!
                player1Turn = !player1Turn
            }


            buttons[4][5]!!.text.toString() == "" -> {
                buttons[4][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[4][5]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[3][4]!!.text.toString() == "" -> {
                buttons[3][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[3][4]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[4][2]!!.text.toString() == "" -> {
                buttons[4][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[4][2]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[5][3]!!.text.toString() == "" -> {
                buttons[5][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[5][3]?.setText("o")!!
                player1Turn = !player1Turn
            }

            buttons[4][3]!!.text.toString() == "" -> {
                buttons[4][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[4][3]?.setText("o")!!
                player1Turn = !player1Turn
            }
            buttons[4][4]!!.text.toString() == "" -> {
                buttons[4][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[4][4]?.setText("o")!!
                player1Turn = !player1Turn
            }
        }
    }

/*
    // check for x from TopLift to BottomRight to prevent 5 x in row
    /*** Done ***/
    private fun xTopLiftBottomRightFourInRow() {

        /****************************-1-********************************/
        if ((buttons[1][4]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[1][4]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[1][4]!!.text.toString() == buttons[4][7]!!.text.toString() && buttons[1][4]!!.text.toString() != "")
            && buttons[1][4]!!.text.toString() == "x"
            && buttons[0][3]!!.text.toString() == ""
            && buttons[0][3]!!.text.toString() != "x"
        ) {
            buttons[0][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[0][3]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[4][7]!!.text.toString() && buttons[2][5]!!.text.toString() != ""
                    )
            && buttons[2][5]!!.text.toString() == "x"
            && buttons[1][4]!!.text.toString() == ""
            && buttons[1][4]!!.text.toString() != "x"
        ) {

            buttons[1][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][6]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[0][3]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[3][6]!!.text.toString() == buttons[4][7]!!.text.toString() && buttons[3][6]!!.text.toString() != "")
            && buttons[3][6]!!.text.toString() == "x"
            && buttons[2][5]!!.text.toString() == ""
            && buttons[2][5]!!.text.toString() != "x"
        ) {

            buttons[2][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[0][3]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[4][7]!!.text.toString() && buttons[2][5]!!.text.toString() != "")
            && buttons[2][5]!!.text.toString() == "x"
            && buttons[3][6]!!.text.toString() == ""
            && buttons[3][6]!!.text.toString() != "x"
        ) {

            buttons[3][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][6]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[0][3]!!.text.toString() && buttons[2][5]!!.text.toString() != "")
            && buttons[2][5]!!.text.toString() == "x"
            && buttons[4][7]!!.text.toString() == ""
            && buttons[4][7]!!.text.toString() != "x"
        ) {

            buttons[4][7]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][7]?.setText("o")!!
            player1Turn = !player1Turn

        }
        /*****************************-2-*******************************/
        else if ((buttons[1][3]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[1][3]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[1][3]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[1][3]!!.text.toString() != "")
            && buttons[1][3]!!.text.toString() == "x"
            && buttons[0][2]!!.text.toString() == ""
            && buttons[0][2]!!.text.toString() != "x"
        ) {

            buttons[0][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][4]!!.text.toString() == buttons[0][2]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[2][4]!!.text.toString() != ""
                    || buttons[2][4]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[5][7]!!.text.toString() && buttons[2][4]!!.text.toString() != "")
            && buttons[2][4]!!.text.toString() == "x"
            && buttons[1][3]!!.text.toString() == ""
            && buttons[1][3]!!.text.toString() != "x"
        ) {

            buttons[1][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][5]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[0][2]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[3][5]!!.text.toString() != ""
                    || buttons[3][5]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[5][7]!!.text.toString() && buttons[3][5]!!.text.toString() != "")
            && buttons[3][5]!!.text.toString() == "x"
            && buttons[2][4]!!.text.toString() == ""
            && buttons[2][4]!!.text.toString() != "x"
        ) {

            buttons[2][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[0][2]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[2][4]!!.text.toString() != ""
                    || buttons[2][4]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[5][7]!!.text.toString() && buttons[2][4]!!.text.toString() != ""
                    )
            && buttons[2][4]!!.text.toString() == "x"
            && buttons[3][5]!!.text.toString() == ""
            && buttons[3][5]!!.text.toString() != "x"
        ) {

            buttons[3][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][5]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[0][2]!!.text.toString() && buttons[3][5]!!.text.toString() != ""
                    || buttons[3][5]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[5][7]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[3][5]!!.text.toString() != "")
            && buttons[3][5]!!.text.toString() == "x"
            && buttons[4][6]!!.text.toString() == ""
            && buttons[4][6]!!.text.toString() != "x"
        ) {

            buttons[4][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][6]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[4][6]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[4][6]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[4][6]!!.text.toString() != "")
            && buttons[4][6]!!.text.toString() == "x"
            && buttons[5][7]!!.text.toString() == ""
            && buttons[5][7]!!.text.toString() != "x"
        ) {

            buttons[5][7]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][7]?.setText("o")!!
            player1Turn = !player1Turn

        }

        /*************************-3-**************************/
        else if ((buttons[1][2]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[1][2]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[1][2]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[1][2]!!.text.toString() != "")
            && buttons[1][2]!!.text.toString() == "x"
            && buttons[0][1]!!.text.toString() == ""
            && buttons[0][1]!!.text.toString() != "x"
        ) {

            buttons[0][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][3]!!.text.toString() == buttons[0][1]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[2][3]!!.text.toString() != ""
                    || buttons[2][3]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[5][6]!!.text.toString() && buttons[2][3]!!.text.toString() != "")
            && buttons[2][3]!!.text.toString() == "x"
            && buttons[1][2]!!.text.toString() == ""
            && buttons[1][2]!!.text.toString() != "x"
        ) {

            buttons[1][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][4]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[0][1]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    || buttons[3][4]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[5][6]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    || buttons[3][4]!!.text.toString() == buttons[5][6]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[6][7]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    )
            && buttons[3][4]!!.text.toString() == "x"
            && buttons[2][3]!!.text.toString() == ""
            && buttons[2][3]!!.text.toString() != "x"
        ) {

            buttons[2][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[0][1]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[2][3]!!.text.toString() != "" && buttons[2][3]!!.text.toString() == "x"
                    || buttons[2][3]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[5][6]!!.text.toString() && buttons[2][3]!!.text.toString() != "" && buttons[2][3]!!.text.toString() == "x"
                    || buttons[2][3]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[5][6]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[2][3]!!.text.toString() != "" && buttons[2][3]!!.text.toString() == "x"
                    || buttons[4][5]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[4][5]!!.text.toString() == buttons[5][6]!!.text.toString() && buttons[6][7]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[4][5]!!.text.toString() != "" && buttons[4][5]!!.text.toString() == "x"
                    )
            && buttons[3][4]!!.text.toString() == ""
            && buttons[3][4]!!.text.toString() != "x"
        ) {

            buttons[3][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][4]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[0][1]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    || buttons[3][4]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[5][6]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[6][7]!!.text.toString() && buttons[3][4]!!.text.toString() != "")
            && buttons[3][4]!!.text.toString() == "x"
            && buttons[4][5]!!.text.toString() == ""
            && buttons[4][5]!!.text.toString() != "x"
        ) {

            buttons[4][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    || buttons[3][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[6][7]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    )
            && buttons[3][4]!!.text.toString() == "x"
            && buttons[5][6]!!.text.toString() == ""
            && buttons[5][6]!!.text.toString() != "x"
        ) {

            buttons[5][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][5]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[5][6]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[4][5]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][4]!!.text.toString() != "")
            && buttons[3][4]!!.text.toString() == "x"
            && buttons[6][7]!!.text.toString() == ""
            && buttons[6][7]!!.text.toString() != "x"
        ) {

            buttons[6][7]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][7]?.setText("o")!!
            player1Turn = !player1Turn

        }


        /*************************-4-***************************/

        else if ((buttons[1][1]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[1][1]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[1][1]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[1][1]!!.text.toString() != "")
            && buttons[1][1]!!.text.toString() == "x"
            && buttons[0][0]!!.text.toString() == ""
            && buttons[0][0]!!.text.toString() != "x"
        ) {

            buttons[0][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][0]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][3]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[0][0]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    || buttons[3][3]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[5][5]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    )
            && buttons[3][3]!!.text.toString() == "x"
            && buttons[1][1]!!.text.toString() == ""
            && buttons[1][1]!!.text.toString() != "x"
        ) {

            buttons[1][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][3]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[0][0]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    || buttons[3][3]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[1][1]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[0][0]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    || buttons[3][3]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[6][6]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    )
            && buttons[3][3]!!.text.toString() == "x"
            && buttons[2][2]!!.text.toString() == ""
            && buttons[2][2]!!.text.toString() != "x"
        ) {

            buttons[2][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][2]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[0][0]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[2][2]!!.text.toString() != "" && buttons[2][2]!!.text.toString() == "x"
                    || buttons[2][2]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[1][1]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() == buttons[5][5]!!.text.toString() && buttons[2][2]!!.text.toString() != "" && buttons[2][2]!!.text.toString() == "x"
                    || buttons[2][2]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() == buttons[6][6]!!.text.toString() && buttons[2][2]!!.text.toString() != "" && buttons[2][2]!!.text.toString() == "x"
                    || buttons[5][5]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[6][6]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[7][7]!!.text.toString() && buttons[5][5]!!.text.toString() != "" && buttons[5][5]!!.text.toString() == "x"
                    )
            && buttons[3][3]!!.text.toString() == ""
            && buttons[3][3]!!.text.toString() != "x"
        ) {

            buttons[3][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][2]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() == buttons[0][0]!!.text.toString() && buttons[2][2]!!.text.toString() != "" && buttons[2][2]!!.text.toString() == "x"
                    || buttons[2][2]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[2][2]!!.text.toString() != "" && buttons[2][2]!!.text.toString() == "x"
                    || buttons[5][5]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[6][6]!!.text.toString() && buttons[2][2]!!.text.toString() == buttons[5][5]!!.text.toString() && buttons[5][5]!!.text.toString() != "" && buttons[5][5]!!.text.toString() == "x"
                    || buttons[5][5]!!.text.toString() == buttons[6][6]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[7][7]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[5][5]!!.text.toString() && buttons[5][5]!!.text.toString() != "" && buttons[5][5]!!.text.toString() == "x"
                    )
            && buttons[4][4]!!.text.toString() == ""
            && buttons[4][4]!!.text.toString() != "x"
        ) {

            buttons[4][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][2]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    || buttons[6][6]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    || buttons[6][6]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[7][7]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    )
            && buttons[4][4]!!.text.toString() == "x"
            && buttons[5][5]!!.text.toString() == ""
            && buttons[5][5]!!.text.toString() != "x"
        ) {

            buttons[5][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][5]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    || buttons[5][5]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[7][7]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    )
            && buttons[4][4]!!.text.toString() == "x"
            && buttons[6][6]!!.text.toString() == ""
            && buttons[6][6]!!.text.toString() != "x"
        ) {

            buttons[6][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][5]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[6][6]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[4][4]!!.text.toString() != "")
            && buttons[4][4]!!.text.toString() == "x"
            && buttons[7][7]!!.text.toString() == ""
            && buttons[7][7]!!.text.toString() != "x"
        ) {

            buttons[7][7]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[7][7]?.setText("o")!!
            player1Turn = !player1Turn

        }
        /**********************-5-*************************/
        else if ((buttons[2][1]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[2][1]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[2][1]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[2][1]!!.text.toString() != "")
            && buttons[2][1]!!.text.toString() == "x"
            && buttons[1][0]!!.text.toString() == ""
            && buttons[1][1]!!.text.toString() != "x"
        ) {

            buttons[1][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][0]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][2]!!.text.toString() == buttons[1][0]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[3][2]!!.text.toString() != ""
                    || buttons[3][2]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[3][2]!!.text.toString() != "")
            && buttons[3][2]!!.text.toString() == "x"
            && buttons[2][1]!!.text.toString() == ""
            && buttons[2][1]!!.text.toString() != "x"
        ) {

            buttons[2][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][3]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[1][0]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    || buttons[4][3]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    || buttons[4][3]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[7][6]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    )
            && buttons[4][3]!!.text.toString() == "x"
            && buttons[3][2]!!.text.toString() == ""
            && buttons[3][2]!!.text.toString() != "x"
        ) {

            buttons[3][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[1][0]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[3][2]!!.text.toString() != "" && buttons[3][2]!!.text.toString() == "x"
                    || buttons[3][2]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[3][2]!!.text.toString() != "" && buttons[3][2]!!.text.toString() == "x"
                    || buttons[7][6]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[5][4]!!.text.toString() != "" && buttons[5][4]!!.text.toString() == "x"
                    )
            && buttons[4][3]!!.text.toString() == ""
            && buttons[4][3]!!.text.toString() != "x"
        ) {

            buttons[4][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][3]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[1][0]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    || buttons[4][3]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    || buttons[4][3]!!.text.toString() == buttons[7][6]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    )
            && buttons[4][3]!!.text.toString() == "x"
            && buttons[5][4]!!.text.toString() == ""
            && buttons[5][4]!!.text.toString() != "x"
        ) {

            buttons[5][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][3]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    || buttons[4][3]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[7][6]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    )
            && buttons[4][3]!!.text.toString() == "x"
            && buttons[6][5]!!.text.toString() == ""
            && buttons[6][5]!!.text.toString() != "x"
        ) {

            buttons[6][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][4]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[6][5]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[4][3]!!.text.toString() != "")
            && buttons[4][3]!!.text.toString() == "x"
            && buttons[7][6]!!.text.toString() == ""
            && buttons[7][6]!!.text.toString() != "x"
        ) {

            buttons[7][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[7][6]?.setText("o")!!
            player1Turn = !player1Turn

        }
        /***********************-6-************************/
        else if ((buttons[3][1]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[3][1]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[3][1]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[3][1]!!.text.toString() != "")
            && buttons[3][1]!!.text.toString() == "x"
            && buttons[2][0]!!.text.toString() == ""
            && buttons[2][0]!!.text.toString() != "x"
        ) {

            buttons[2][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][0]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][2]!!.text.toString() == buttons[2][0]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[4][2]!!.text.toString() != ""
                    || buttons[4][2]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[7][5]!!.text.toString() && buttons[4][2]!!.text.toString() != "")
            && buttons[4][2]!!.text.toString() == "x"
            && buttons[3][1]!!.text.toString() == ""
            && buttons[3][1]!!.text.toString() != "x"
        ) {

            buttons[3][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][3]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[2][0]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[5][3]!!.text.toString() != ""
                    || buttons[5][3]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[7][5]!!.text.toString() && buttons[5][3]!!.text.toString() != ""
                    )
            && buttons[5][3]!!.text.toString() == "x"
            && buttons[4][2]!!.text.toString() == ""
            && buttons[4][2]!!.text.toString() != "x"
        ) {

            buttons[4][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][0]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[4][2]!!.text.toString() != ""
                    || buttons[4][2]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[7][5]!!.text.toString() && buttons[4][2]!!.text.toString() != ""
                    )
            && buttons[4][2]!!.text.toString() == "x"
            && buttons[5][3]!!.text.toString() == ""
            && buttons[5][3]!!.text.toString() != "x"
        ) {

            buttons[5][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][3]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[2][0]!!.text.toString() && buttons[5][3]!!.text.toString() != ""
                    || buttons[5][3]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[7][5]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[5][3]!!.text.toString() != ""
                    )
            && buttons[5][3]!!.text.toString() == "x"
            && buttons[6][4]!!.text.toString() == ""
            && buttons[6][4]!!.text.toString() != "x"
        ) {

            buttons[6][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[6][4]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[6][4]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[6][4]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[6][4]!!.text.toString() != "")
            && buttons[6][4]!!.text.toString() == "x"
            && buttons[7][5]!!.text.toString() == ""
            && buttons[7][5]!!.text.toString() != "x"
        ) {

            buttons[7][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[7][5]?.setText("o")!!
            player1Turn = !player1Turn

        }
        /***********************-7-************************/
        else if ((buttons[4][1]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[4][1]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[4][1]!!.text.toString() == buttons[7][4]!!.text.toString() && buttons[4][1]!!.text.toString() != "")
            && buttons[4][1]!!.text.toString() == "x"
            && buttons[3][0]!!.text.toString() == ""
            && buttons[3][0]!!.text.toString() != "x"
        ) {

            buttons[3][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][0]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][0]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[5][2]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[5][2]!!.text.toString() == buttons[7][4]!!.text.toString() && buttons[5][2]!!.text.toString() != ""
                    )
            && buttons[5][2]!!.text.toString() == "x"
            && buttons[4][1]!!.text.toString() == ""
            && buttons[4][1]!!.text.toString() != "x"
        ) {

            buttons[4][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[6][3]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[3][0]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[6][3]!!.text.toString() == buttons[7][4]!!.text.toString() && buttons[6][3]!!.text.toString() != "")
            && buttons[6][3]!!.text.toString() == "x"
            && buttons[5][2]!!.text.toString() == ""
            && buttons[5][2]!!.text.toString() != "x"
        ) {

            buttons[5][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][0]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[5][2]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[5][2]!!.text.toString() == buttons[7][4]!!.text.toString() && buttons[5][2]!!.text.toString() != "")
            && buttons[5][2]!!.text.toString() == "x"
            && buttons[6][3]!!.text.toString() == ""
            && buttons[6][3]!!.text.toString() != "x"
        ) {

            buttons[6][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[6][3]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[5][2]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[3][0]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[5][2]!!.text.toString() != "")
            && buttons[5][2]!!.text.toString() == "x"
            && buttons[7][4]!!.text.toString() == ""
            && buttons[7][4]!!.text.toString() != "x"
        ) {

            buttons[7][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[7][4]?.setText("o")!!
            player1Turn = !player1Turn

        }
    }

    // check for o from TopLift to BottomRight to get 5 O in row
    /*** Done ***/
    private fun oTopLiftBottomRightFourInRow() {

        /****************************-1-********************************/
        if ((buttons[1][4]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[1][4]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[1][4]!!.text.toString() == buttons[4][7]!!.text.toString() && buttons[1][4]!!.text.toString() != "")
            && buttons[1][4]!!.text.toString() == "o"
            && buttons[0][3]!!.text.toString() == ""
            && buttons[0][3]!!.text.toString() != "x"
        ) {
            buttons[0][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[0][3]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[4][7]!!.text.toString() && buttons[2][5]!!.text.toString() != ""
                    )
            && buttons[2][5]!!.text.toString() == "o"
            && buttons[1][4]!!.text.toString() == ""
            && buttons[1][4]!!.text.toString() != "x"
        ) {

            buttons[1][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][6]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[0][3]!!.text.toString() == buttons[3][6]!!.text.toString() && buttons[3][6]!!.text.toString() == buttons[4][7]!!.text.toString() && buttons[3][6]!!.text.toString() != "")
            && buttons[3][6]!!.text.toString() == "o"
            && buttons[2][5]!!.text.toString() == ""
            && buttons[2][5]!!.text.toString() != "x"
        ) {

            buttons[2][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[0][3]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[4][7]!!.text.toString() && buttons[2][5]!!.text.toString() != "")
            && buttons[2][5]!!.text.toString() == "o"
            && buttons[3][6]!!.text.toString() == ""
            && buttons[3][6]!!.text.toString() != "x"
        ) {

            buttons[3][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][6]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[1][4]!!.text.toString() && buttons[2][5]!!.text.toString() == buttons[0][3]!!.text.toString() && buttons[2][5]!!.text.toString() != "")
            && buttons[2][5]!!.text.toString() == "o"
            && buttons[4][7]!!.text.toString() == ""
            && buttons[4][7]!!.text.toString() != "x"
        ) {

            buttons[4][7]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][7]?.setText("o")!!
            player1Turn = !player1Turn

        }
        /*****************************-2-*******************************/
        else if ((buttons[1][3]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[1][3]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[1][3]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[1][3]!!.text.toString() != "")
            && buttons[1][3]!!.text.toString() == "o"
            && buttons[0][2]!!.text.toString() == ""
            && buttons[0][2]!!.text.toString() != "x"
        ) {

            buttons[0][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][4]!!.text.toString() == buttons[0][2]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[2][4]!!.text.toString() != ""
                    || buttons[2][4]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[5][7]!!.text.toString() && buttons[2][4]!!.text.toString() != "")
            && buttons[2][4]!!.text.toString() == "o"
            && buttons[1][3]!!.text.toString() == ""
            && buttons[1][3]!!.text.toString() != "x"
        ) {

            buttons[1][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][5]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[0][2]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[3][5]!!.text.toString() != ""
                    || buttons[3][5]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[5][7]!!.text.toString() && buttons[3][5]!!.text.toString() != "")
            && buttons[3][5]!!.text.toString() == "o"
            && buttons[2][4]!!.text.toString() == ""
            && buttons[2][4]!!.text.toString() != "x"
        ) {

            buttons[2][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[0][2]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[2][4]!!.text.toString() != ""
                    || buttons[2][4]!!.text.toString() == buttons[4][6]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[2][4]!!.text.toString() == buttons[5][7]!!.text.toString() && buttons[2][4]!!.text.toString() != ""
                    )
            && buttons[2][4]!!.text.toString() == "o"
            && buttons[3][5]!!.text.toString() == ""
            && buttons[3][5]!!.text.toString() != "x"
        ) {

            buttons[3][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][5]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[0][2]!!.text.toString() && buttons[3][5]!!.text.toString() != ""
                    || buttons[3][5]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[5][7]!!.text.toString() && buttons[3][5]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[3][5]!!.text.toString() != "")
            && buttons[3][5]!!.text.toString() == "o"
            && buttons[4][6]!!.text.toString() == ""
            && buttons[4][6]!!.text.toString() != "x"
        ) {

            buttons[4][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][6]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[4][6]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[4][6]!!.text.toString() == buttons[1][3]!!.text.toString() && buttons[4][6]!!.text.toString() != "")
            && buttons[4][6]!!.text.toString() == "o"
            && buttons[5][7]!!.text.toString() == ""
            && buttons[5][7]!!.text.toString() != "x"
        ) {

            buttons[5][7]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][7]?.setText("o")!!
            player1Turn = !player1Turn

        }

        /*************************-3-**************************/
        else if ((buttons[1][2]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[1][2]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[1][2]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[1][2]!!.text.toString() != "")
            && buttons[1][2]!!.text.toString() == "o"
            && buttons[0][1]!!.text.toString() == ""
            && buttons[0][1]!!.text.toString() != "x"
        ) {

            buttons[0][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][3]!!.text.toString() == buttons[0][1]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[2][3]!!.text.toString() != ""
                    || buttons[2][3]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[5][6]!!.text.toString() && buttons[2][3]!!.text.toString() != "")
            && buttons[2][3]!!.text.toString() == "o"
            && buttons[1][2]!!.text.toString() == ""
            && buttons[1][2]!!.text.toString() != "x"
        ) {

            buttons[1][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][4]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[0][1]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    || buttons[3][4]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[5][6]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    || buttons[3][4]!!.text.toString() == buttons[5][6]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[6][7]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    )
            && buttons[3][4]!!.text.toString() == "o"
            && buttons[2][3]!!.text.toString() == ""
            && buttons[2][3]!!.text.toString() != "x"
        ) {

            buttons[2][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[0][1]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[2][3]!!.text.toString() != "" && buttons[2][3]!!.text.toString() == "o"
                    || buttons[2][3]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[5][6]!!.text.toString() && buttons[2][3]!!.text.toString() != "" && buttons[2][3]!!.text.toString() == "o"
                    || buttons[2][3]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[5][6]!!.text.toString() && buttons[2][3]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[2][3]!!.text.toString() != "" && buttons[2][3]!!.text.toString() == "o"
                    || buttons[4][5]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[4][5]!!.text.toString() == buttons[5][6]!!.text.toString() && buttons[6][7]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[4][5]!!.text.toString() != "" && buttons[4][5]!!.text.toString() == "o"
                    )
            && buttons[3][4]!!.text.toString() == ""
            && buttons[3][4]!!.text.toString() != "x"
        ) {

            buttons[3][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][4]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[0][1]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    || buttons[3][4]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[5][6]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[6][7]!!.text.toString() && buttons[3][4]!!.text.toString() != "")
            && buttons[3][4]!!.text.toString() == "o"
            && buttons[4][5]!!.text.toString() == ""
            && buttons[4][5]!!.text.toString() != "x"
        ) {

            buttons[4][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[1][2]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    || buttons[3][4]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[6][7]!!.text.toString() && buttons[3][4]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][4]!!.text.toString() != ""
                    )
            && buttons[3][4]!!.text.toString() == "o"
            && buttons[5][6]!!.text.toString() == ""
            && buttons[5][6]!!.text.toString() != "x"
        ) {

            buttons[5][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][5]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[5][6]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[4][5]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[3][4]!!.text.toString() != "")
            && buttons[3][4]!!.text.toString() == "o"
            && buttons[6][7]!!.text.toString() == ""
            && buttons[6][7]!!.text.toString() != "x"
        ) {

            buttons[6][7]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][7]?.setText("o")!!
            player1Turn = !player1Turn

        }


        /*************************-4-***************************/

        else if ((buttons[1][1]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[1][1]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[1][1]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[1][1]!!.text.toString() != "")
            && buttons[1][1]!!.text.toString() == "o"
            && buttons[0][0]!!.text.toString() == ""
            && buttons[0][0]!!.text.toString() != "x"
        ) {

            buttons[0][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[0][0]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][3]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[0][0]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    || buttons[3][3]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[5][5]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    )
            && buttons[3][3]!!.text.toString() == "o"
            && buttons[1][1]!!.text.toString() == ""
            && buttons[1][1]!!.text.toString() != "x"
        ) {

            buttons[1][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][3]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[0][0]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    || buttons[3][3]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[1][1]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[0][0]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    || buttons[3][3]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[6][6]!!.text.toString() && buttons[3][3]!!.text.toString() != ""
                    )
            && buttons[3][3]!!.text.toString() == "o"
            && buttons[2][2]!!.text.toString() == ""
            && buttons[2][2]!!.text.toString() != "x"
        ) {

            buttons[2][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][2]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[0][0]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[2][2]!!.text.toString() != "" && buttons[2][2]!!.text.toString() == "o"
                    || buttons[2][2]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[1][1]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() == buttons[5][5]!!.text.toString() && buttons[2][2]!!.text.toString() != "" && buttons[2][2]!!.text.toString() == "o"
                    || buttons[2][2]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() == buttons[6][6]!!.text.toString() && buttons[2][2]!!.text.toString() != "" && buttons[2][2]!!.text.toString() == "o"
                    || buttons[5][5]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[6][6]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[7][7]!!.text.toString() && buttons[5][5]!!.text.toString() != "" && buttons[5][5]!!.text.toString() == "o"
                    )
            && buttons[3][3]!!.text.toString() == ""
            && buttons[3][3]!!.text.toString() != "x"
        ) {

            buttons[3][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][2]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() == buttons[0][0]!!.text.toString() && buttons[2][2]!!.text.toString() != "" && buttons[2][2]!!.text.toString() == "o"
                    || buttons[2][2]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[2][2]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[2][2]!!.text.toString() != "" && buttons[2][2]!!.text.toString() == "o"
                    || buttons[5][5]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[6][6]!!.text.toString() && buttons[2][2]!!.text.toString() == buttons[5][5]!!.text.toString() && buttons[5][5]!!.text.toString() != "" && buttons[5][5]!!.text.toString() == "o"
                    || buttons[5][5]!!.text.toString() == buttons[6][6]!!.text.toString() && buttons[5][5]!!.text.toString() == buttons[7][7]!!.text.toString() && buttons[3][3]!!.text.toString() == buttons[5][5]!!.text.toString() && buttons[5][5]!!.text.toString() != "" && buttons[5][5]!!.text.toString() == "o"
                    )
            && buttons[4][4]!!.text.toString() == ""
            && buttons[4][4]!!.text.toString() != "x"
        ) {

            buttons[4][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][2]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[1][1]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    || buttons[6][6]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    || buttons[6][6]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[7][7]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    )
            && buttons[4][4]!!.text.toString() == "o"
            && buttons[5][5]!!.text.toString() == ""
            && buttons[5][5]!!.text.toString() != "x"
        ) {

            buttons[5][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][5]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    || buttons[5][5]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[7][7]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[4][4]!!.text.toString() != ""
                    )
            && buttons[4][4]!!.text.toString() == "o"
            && buttons[6][6]!!.text.toString() == ""
            && buttons[6][6]!!.text.toString() != "x"
        ) {

            buttons[6][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][6]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][5]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[6][6]!!.text.toString() && buttons[4][4]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[4][4]!!.text.toString() != "")
            && buttons[4][4]!!.text.toString() == "o"
            && buttons[7][7]!!.text.toString() == ""
            && buttons[7][7]!!.text.toString() != "x"
        ) {

            buttons[7][7]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[7][7]?.setText("o")!!
            player1Turn = !player1Turn

        }
        /**********************-5-*************************/
        else if ((buttons[2][1]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[2][1]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[2][1]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[2][1]!!.text.toString() != "")
            && buttons[2][1]!!.text.toString() == "o"
            && buttons[1][0]!!.text.toString() == ""
            && buttons[1][1]!!.text.toString() != "x"
        ) {

            buttons[1][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[1][0]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][2]!!.text.toString() == buttons[1][0]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[3][2]!!.text.toString() != ""
                    || buttons[3][2]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[3][2]!!.text.toString() != "")
            && buttons[3][2]!!.text.toString() == "o"
            && buttons[2][1]!!.text.toString() == ""
            && buttons[2][1]!!.text.toString() != "x"
        ) {

            buttons[2][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][3]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[1][0]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    || buttons[4][3]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    || buttons[4][3]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[7][6]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    )
            && buttons[4][3]!!.text.toString() == "o"
            && buttons[3][2]!!.text.toString() == ""
            && buttons[3][2]!!.text.toString() != "x"
        ) {

            buttons[3][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[1][0]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[3][2]!!.text.toString() != "" && buttons[3][2]!!.text.toString() == "o"
                    || buttons[3][2]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[3][2]!!.text.toString() != "" && buttons[3][2]!!.text.toString() == "o"
                    || buttons[7][6]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[5][4]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[3][2]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[5][4]!!.text.toString() != "" && buttons[5][4]!!.text.toString() == "o"
                    )
            && buttons[4][3]!!.text.toString() == ""
            && buttons[4][3]!!.text.toString() != "x"
        ) {

            buttons[4][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][3]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[1][0]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    || buttons[4][3]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    || buttons[4][3]!!.text.toString() == buttons[7][6]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[6][5]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    )
            && buttons[4][3]!!.text.toString() == "o"
            && buttons[5][4]!!.text.toString() == ""
            && buttons[5][4]!!.text.toString() != "x"
        ) {

            buttons[5][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][3]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[2][1]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    || buttons[4][3]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[7][6]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[4][3]!!.text.toString() != ""
                    )
            && buttons[4][3]!!.text.toString() == "o"
            && buttons[6][5]!!.text.toString() == ""
            && buttons[6][5]!!.text.toString() != "x"
        ) {

            buttons[6][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][5]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][4]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[6][5]!!.text.toString() == buttons[4][3]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[4][3]!!.text.toString() != "")
            && buttons[4][3]!!.text.toString() == "o"
            && buttons[7][6]!!.text.toString() == ""
            && buttons[7][6]!!.text.toString() != "x"
        ) {

            buttons[7][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[7][6]?.setText("o")!!
            player1Turn = !player1Turn

        }
        /***********************-6-************************/
        else if ((buttons[3][1]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[3][1]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[3][1]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[3][1]!!.text.toString() != "")
            && buttons[3][1]!!.text.toString() == "o"
            && buttons[2][0]!!.text.toString() == ""
            && buttons[2][0]!!.text.toString() != "x"
        ) {

            buttons[2][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[2][0]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[4][2]!!.text.toString() == buttons[2][0]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[4][2]!!.text.toString() != ""
                    || buttons[4][2]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[7][5]!!.text.toString() && buttons[4][2]!!.text.toString() != "")
            && buttons[4][2]!!.text.toString() == "o"
            && buttons[3][1]!!.text.toString() == ""
            && buttons[3][1]!!.text.toString() != "x"
        ) {

            buttons[3][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][3]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[2][0]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[5][3]!!.text.toString() != ""
                    || buttons[5][3]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[7][5]!!.text.toString() && buttons[5][3]!!.text.toString() != ""
                    )
            && buttons[5][3]!!.text.toString() == "o"
            && buttons[4][2]!!.text.toString() == ""
            && buttons[4][2]!!.text.toString() != "x"
        ) {

            buttons[4][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[2][0]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[4][2]!!.text.toString() != ""
                    || buttons[4][2]!!.text.toString() == buttons[6][4]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[7][5]!!.text.toString() && buttons[4][2]!!.text.toString() != ""
                    )
            && buttons[4][2]!!.text.toString() == "o"
            && buttons[5][3]!!.text.toString() == ""
            && buttons[5][3]!!.text.toString() != "x"
        ) {

            buttons[5][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[5][3]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[2][0]!!.text.toString() && buttons[5][3]!!.text.toString() != ""
                    || buttons[5][3]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[7][5]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[5][3]!!.text.toString() != ""
                    )
            && buttons[5][3]!!.text.toString() == "o"
            && buttons[6][4]!!.text.toString() == ""
            && buttons[6][4]!!.text.toString() != "x"
        ) {

            buttons[6][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][4]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[6][4]!!.text.toString() == buttons[5][3]!!.text.toString() && buttons[6][4]!!.text.toString() == buttons[4][2]!!.text.toString() && buttons[6][4]!!.text.toString() == buttons[3][1]!!.text.toString() && buttons[6][4]!!.text.toString() != "")
            && buttons[6][4]!!.text.toString() == "o"
            && buttons[7][5]!!.text.toString() == ""
            && buttons[7][5]!!.text.toString() != "x"
        ) {

            buttons[7][5]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[7][5]?.setText("o")!!
            player1Turn = !player1Turn

        }
        /***********************-7-************************/
        else if ((buttons[4][1]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[4][1]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[4][1]!!.text.toString() == buttons[7][4]!!.text.toString() && buttons[4][1]!!.text.toString() != "")
            && buttons[4][1]!!.text.toString() == "o"
            && buttons[3][0]!!.text.toString() == ""
            && buttons[3][0]!!.text.toString() != "x"
        ) {

            buttons[3][0]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[3][0]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][0]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[5][2]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[5][2]!!.text.toString() == buttons[7][4]!!.text.toString() && buttons[5][2]!!.text.toString() != ""
                    )
            && buttons[5][2]!!.text.toString() == "o"
            && buttons[4][1]!!.text.toString() == ""
            && buttons[4][1]!!.text.toString() != "x"
        ) {

            buttons[4][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[4][1]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[6][3]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[3][0]!!.text.toString() == buttons[6][3]!!.text.toString() && buttons[6][3]!!.text.toString() == buttons[7][4]!!.text.toString() && buttons[6][3]!!.text.toString() != "")
            && buttons[6][3]!!.text.toString() == "o"
            && buttons[5][2]!!.text.toString() == ""
            && buttons[5][2]!!.text.toString() != "x"
        ) {

            buttons[5][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[5][2]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[3][0]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[5][2]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[5][2]!!.text.toString() == buttons[7][4]!!.text.toString() && buttons[5][2]!!.text.toString() != "")
            && buttons[5][2]!!.text.toString() == "o"
            && buttons[6][3]!!.text.toString() == ""
            && buttons[6][3]!!.text.toString() != "x"
        ) {

            buttons[6][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[6][3]?.setText("o")!!
            player1Turn = !player1Turn

        } else if ((buttons[6][3]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[5][2]!!.text.toString() == buttons[4][1]!!.text.toString() && buttons[3][0]!!.text.toString() == buttons[5][2]!!.text.toString() && buttons[5][2]!!.text.toString() != "")
            && buttons[5][2]!!.text.toString() == "o"
            && buttons[7][4]!!.text.toString() == ""
            && buttons[7][4]!!.text.toString() != "x"
        ) {

            buttons[7][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
            buttons[7][4]?.setText("o")!!
            player1Turn = !player1Turn

        }
    }
*/

    // method to prevent player from getting 3 in row in two dimension
    /*** Done ***/
    private fun xTwoHorizontalVerticalMain() {
        for (i in 1..6) {
            val j = i + 1
            val s = i - 1
            /******************************* Horizontal Lines********************************/

            if (((buttons[i][3]!!.text.toString() == buttons[i][2]!!.text.toString() && (buttons[j][2]!!.text.toString() == buttons[i][2]!!.text.toString() || buttons[s][2]!!.text.toString() == buttons[i][2]!!.text.toString() || buttons[j][0]!!.text.toString() == buttons[i][2]!!.text.toString() || buttons[s][0]!!.text.toString() == buttons[i][2]!!.text.toString()) && buttons[i][2]!!.text.toString() != ""))
                && buttons[i][2]!!.text.toString() == "x"
                && buttons[i][1]!!.text.toString() == ""
                && buttons[i][1]!!.text.toString() != "x"
            ) {
                buttons[i][1]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[i][1]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if (((buttons[i][3]!!.text.toString() == buttons[i][4]!!.text.toString() &&
                        (buttons[j][3]!!.text.toString() == buttons[i][3]!!.text.toString() || buttons[s][3]!!.text.toString() == buttons[i][3]!!.text.toString() || buttons[j][1]!!.text.toString() == buttons[i][3]!!.text.toString() || buttons[s][1]!!.text.toString() == buttons[i][3]!!.text.toString())
                        && buttons[i][3]!!.text.toString() != "" && buttons[i][3]!!.text.toString() == "x")
                        || (buttons[i][0]!!.text.toString() == buttons[i][1]!!.text.toString() &&
                        (buttons[j][1]!!.text.toString() == buttons[i][1]!!.text.toString() || buttons[s][1]!!.text.toString() == buttons[i][1]!!.text.toString() || buttons[j][3]!!.text.toString() == buttons[i][1]!!.text.toString() || buttons[s][3]!!.text.toString() == buttons[i][1]!!.text.toString())
                        && buttons[i][1]!!.text.toString() != "" && buttons[i][1]!!.text.toString() == "x"))
                && buttons[i][2]!!.text.toString() == ""
                && buttons[i][2]!!.text.toString() != "x"
            ) {

                buttons[i][2]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[i][2]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if (((buttons[i][4]!!.text.toString() == buttons[i][5]!!.text.toString() &&
                        (buttons[j][4]!!.text.toString() == buttons[i][4]!!.text.toString() || buttons[s][4]!!.text.toString() == buttons[i][4]!!.text.toString() || buttons[j][2]!!.text.toString() == buttons[i][4]!!.text.toString() || buttons[s][2]!!.text.toString() == buttons[i][4]!!.text.toString())
                        && buttons[i][4]!!.text.toString() != "" && buttons[i][4]!!.text.toString() == "x")
                        || (buttons[i][1]!!.text.toString() == buttons[i][2]!!.text.toString() &&
                        (buttons[j][2]!!.text.toString() == buttons[i][1]!!.text.toString() || buttons[s][2]!!.text.toString() == buttons[i][1]!!.text.toString() || buttons[j][4]!!.text.toString() == buttons[i][1]!!.text.toString() || buttons[s][4]!!.text.toString() == buttons[i][1]!!.text.toString())
                        && buttons[i][1]!!.text.toString() != "" && buttons[i][1]!!.text.toString() == "x"))

                && buttons[i][3]!!.text.toString() == ""
                && buttons[i][3]!!.text.toString() != "x"
            ) {

                buttons[i][3]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[i][3]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if (((buttons[i][5]!!.text.toString() == buttons[i][6]!!.text.toString() &&
                        (buttons[j][5]!!.text.toString() == buttons[i][5]!!.text.toString() || buttons[s][5]!!.text.toString() == buttons[i][5]!!.text.toString() || buttons[j][3]!!.text.toString() == buttons[i][5]!!.text.toString() || buttons[s][3]!!.text.toString() == buttons[i][5]!!.text.toString())
                        && buttons[i][5]!!.text.toString() != "" && buttons[i][5]!!.text.toString() == "x")
                        || (buttons[i][2]!!.text.toString() == buttons[i][3]!!.text.toString() &&
                        (buttons[j][3]!!.text.toString() == buttons[i][2]!!.text.toString() || buttons[s][3]!!.text.toString() == buttons[i][2]!!.text.toString() || buttons[j][5]!!.text.toString() == buttons[i][2]!!.text.toString() || buttons[s][5]!!.text.toString() == buttons[i][2]!!.text.toString())
                        && buttons[i][2]!!.text.toString() != "" && buttons[i][2]!!.text.toString() == "x"))

                && buttons[i][4]!!.text.toString() == ""
                && buttons[i][4]!!.text.toString() != "x"
            ) {

                buttons[i][4]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[i][4]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if (((buttons[i][6]!!.text.toString() == buttons[i][7]!!.text.toString() &&
                        (buttons[j][6]!!.text.toString() == buttons[i][6]!!.text.toString() || buttons[s][6]!!.text.toString() == buttons[i][6]!!.text.toString() || buttons[j][4]!!.text.toString() == buttons[i][6]!!.text.toString() || buttons[s][4]!!.text.toString() == buttons[i][6]!!.text.toString())
                        && buttons[i][6]!!.text.toString() != "" && buttons[i][6]!!.text.toString() == "x")
                        || (buttons[i][3]!!.text.toString() == buttons[i][4]!!.text.toString() &&
                        (buttons[j][4]!!.text.toString() == buttons[i][3]!!.text.toString() || buttons[s][4]!!.text.toString() == buttons[i][3]!!.text.toString() || buttons[j][6]!!.text.toString() == buttons[i][3]!!.text.toString() || buttons[s][6]!!.text.toString() == buttons[i][3]!!.text.toString())
                        && buttons[i][3]!!.text.toString() != "" && buttons[i][3]!!.text.toString() == "x"))

                && buttons[i][5]!!.text.toString() == ""
                && buttons[i][5]!!.text.toString() != "x"
            ) {

                buttons[i][5]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[i][5]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if (((buttons[i][4]!!.text.toString() == buttons[i][5]!!.text.toString() &&
                        (buttons[j][5]!!.text.toString() == buttons[i][4]!!.text.toString() || buttons[s][5]!!.text.toString() == buttons[i][4]!!.text.toString() || buttons[j][7]!!.text.toString() == buttons[i][4]!!.text.toString() || buttons[s][7]!!.text.toString() == buttons[i][4]!!.text.toString())
                        && buttons[i][4]!!.text.toString() != "" && buttons[i][4]!!.text.toString() == "x"))

                && buttons[i][6]!!.text.toString() == ""
                && buttons[i][6]!!.text.toString() != "x"
            ) {

                buttons[i][6]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[i][6]?.setText("o")!!
                player1Turn = !player1Turn
                break

            }

            /******************************* Vertical Lines********************************/
            else if (((buttons[3][i]!!.text.toString() == buttons[2][i]!!.text.toString() && (buttons[2][j]!!.text.toString() == buttons[2][i]!!.text.toString() || buttons[2][s]!!.text.toString() == buttons[2][i]!!.text.toString() || buttons[0][j]!!.text.toString() == buttons[2][i]!!.text.toString() || buttons[0][s]!!.text.toString() == buttons[2][i]!!.text.toString()) && buttons[2][i]!!.text.toString() != ""))
                && buttons[2][i]!!.text.toString() == "x"
                && buttons[1][i]!!.text.toString() == ""
                && buttons[1][i]!!.text.toString() != "x"
            ) {
                buttons[1][i]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[1][i]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if (((buttons[3][i]!!.text.toString() == buttons[4][i]!!.text.toString() &&
                        (buttons[3][j]!!.text.toString() == buttons[3][i]!!.text.toString() || buttons[3][s]!!.text.toString() == buttons[3][i]!!.text.toString() || buttons[1][j]!!.text.toString() == buttons[3][i]!!.text.toString() || buttons[1][s]!!.text.toString() == buttons[3][i]!!.text.toString())
                        && buttons[3][i]!!.text.toString() != "" && buttons[3][i]!!.text.toString() == "x")
                        || (buttons[0][i]!!.text.toString() == buttons[1][i]!!.text.toString() &&
                        (buttons[1][j]!!.text.toString() == buttons[1][i]!!.text.toString() || buttons[1][s]!!.text.toString() == buttons[1][i]!!.text.toString() || buttons[3][j]!!.text.toString() == buttons[1][i]!!.text.toString() || buttons[3][s]!!.text.toString() == buttons[1][i]!!.text.toString())
                        && buttons[1][i]!!.text.toString() != "" && buttons[1][i]!!.text.toString() == "x"))
                && buttons[2][i]!!.text.toString() == ""
                && buttons[2][i]!!.text.toString() != "x"
            ) {

                buttons[2][i]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[2][i]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if (((buttons[4][i]!!.text.toString() == buttons[5][i]!!.text.toString() &&
                        (buttons[4][j]!!.text.toString() == buttons[4][i]!!.text.toString() || buttons[4][s]!!.text.toString() == buttons[4][i]!!.text.toString() || buttons[2][j]!!.text.toString() == buttons[4][i]!!.text.toString() || buttons[2][s]!!.text.toString() == buttons[4][i]!!.text.toString())
                        && buttons[4][i]!!.text.toString() != "" && buttons[4][i]!!.text.toString() == "x")
                        || (buttons[1][i]!!.text.toString() == buttons[2][i]!!.text.toString() &&
                        (buttons[2][j]!!.text.toString() == buttons[1][i]!!.text.toString() || buttons[2][s]!!.text.toString() == buttons[1][i]!!.text.toString() || buttons[4][j]!!.text.toString() == buttons[1][i]!!.text.toString() || buttons[4][s]!!.text.toString() == buttons[1][i]!!.text.toString())
                        && buttons[1][i]!!.text.toString() != "" && buttons[1][i]!!.text.toString() == "x"))

                && buttons[3][i]!!.text.toString() == ""
                && buttons[3][i]!!.text.toString() != "x"
            ) {

                buttons[3][i]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[3][i]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if (((buttons[5][i]!!.text.toString() == buttons[6][i]!!.text.toString() &&
                        (buttons[5][j]!!.text.toString() == buttons[5][i]!!.text.toString() || buttons[5][s]!!.text.toString() == buttons[5][i]!!.text.toString() || buttons[3][j]!!.text.toString() == buttons[5][i]!!.text.toString() || buttons[3][s]!!.text.toString() == buttons[5][i]!!.text.toString())
                        && buttons[5][i]!!.text.toString() != "" && buttons[5][i]!!.text.toString() == "x")
                        || (buttons[2][i]!!.text.toString() == buttons[3][i]!!.text.toString() &&
                        (buttons[3][j]!!.text.toString() == buttons[2][i]!!.text.toString() || buttons[3][s]!!.text.toString() == buttons[2][i]!!.text.toString() || buttons[5][j]!!.text.toString() == buttons[2][i]!!.text.toString() || buttons[5][s]!!.text.toString() == buttons[2][i]!!.text.toString())
                        && buttons[2][i]!!.text.toString() != "" && buttons[2][i]!!.text.toString() == "x"))

                && buttons[4][i]!!.text.toString() == ""
                && buttons[4][i]!!.text.toString() != "x"
            ) {

                buttons[4][i]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[4][i]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if (((buttons[6][i]!!.text.toString() == buttons[7][i]!!.text.toString() &&
                        (buttons[6][j]!!.text.toString() == buttons[6][i]!!.text.toString() || buttons[6][s]!!.text.toString() == buttons[6][i]!!.text.toString() || buttons[4][j]!!.text.toString() == buttons[6][i]!!.text.toString() || buttons[4][s]!!.text.toString() == buttons[6][i]!!.text.toString())
                        && buttons[6][i]!!.text.toString() != "" && buttons[6][i]!!.text.toString() == "x")
                        || (buttons[3][i]!!.text.toString() == buttons[4][i]!!.text.toString() &&
                        (buttons[4][j]!!.text.toString() == buttons[3][i]!!.text.toString() || buttons[4][s]!!.text.toString() == buttons[3][i]!!.text.toString() || buttons[6][j]!!.text.toString() == buttons[3][i]!!.text.toString() || buttons[6][s]!!.text.toString() == buttons[3][i]!!.text.toString())
                        && buttons[3][i]!!.text.toString() != "" && buttons[3][i]!!.text.toString() == "x"))

                && buttons[5][i]!!.text.toString() == ""
                && buttons[5][i]!!.text.toString() != "x"
            ) {

                buttons[5][i]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[5][i]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if (((buttons[4][i]!!.text.toString() == buttons[5][i]!!.text.toString() &&
                        (buttons[5][j]!!.text.toString() == buttons[4][i]!!.text.toString() || buttons[5][s]!!.text.toString() == buttons[4][i]!!.text.toString() || buttons[7][j]!!.text.toString() == buttons[4][i]!!.text.toString() || buttons[7][s]!!.text.toString() == buttons[4][i]!!.text.toString())
                        && buttons[4][i]!!.text.toString() != "" && buttons[4][i]!!.text.toString() == "x"))

                && buttons[6][i]!!.text.toString() == ""
                && buttons[6][i]!!.text.toString() != "x"
            ) {

                buttons[6][i]?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.o
                )
                buttons[6][i]?.setText("o")!!
                player1Turn = !player1Turn
                break

            }

        }
    }

    private fun xOneTrueOneFalseHorizontalVertical() {
        for (i in 0..7) {
            /******************************* Horizontal Lines********************************/
            if ((buttons[i][2]!!.text.toString() == buttons[i][0]!!.text.toString() && buttons[i][2]!!.text.toString() == buttons[i][4]!!.text.toString() && buttons[i][2]!!.text.toString() == buttons[i][6]!!.text.toString() && buttons[i][2]!!.text.toString() != "")
                && buttons[i][2]!!.text.toString() == "x"
                && buttons[i][3]!!.text.toString() == ""
                && buttons[i][3]!!.text.toString() != "x"
            ) {

                buttons[i][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[i][3]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[i][3]!!.text.toString() == buttons[i][1]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][5]!!.text.toString() && buttons[i][3]!!.text.toString() == buttons[i][7]!!.text.toString() && buttons[i][3]!!.text.toString() != "")
                && buttons[i][3]!!.text.toString() == "x"
                && buttons[i][4]!!.text.toString() == ""
                && buttons[i][4]!!.text.toString() != "x"
            ) {

                buttons[i][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[i][4]?.setText("o")!!
                player1Turn = !player1Turn
                break

            }
            /******************************* Vertical Lines ********************************/
            else if ((buttons[2][i]!!.text.toString() == buttons[0][i]!!.text.toString() && buttons[2][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[2][i]!!.text.toString() != ""
                        || buttons[2][i]!!.text.toString() == buttons[6][i]!!.text.toString() && buttons[2][i]!!.text.toString() == buttons[4][i]!!.text.toString() && buttons[2][i]!!.text.toString() != "")
                && buttons[2][i]!!.text.toString() == "x"
                && buttons[3][i]!!.text.toString() == ""
                && buttons[3][i]!!.text.toString() != "x"
            ) {

                buttons[3][i]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[3][i]?.setText("o")!!
                player1Turn = !player1Turn
                break

            } else if ((buttons[3][i]!!.text.toString() == buttons[1][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[5][i]!!.text.toString() && buttons[3][i]!!.text.toString() != ""
                        || buttons[3][i]!!.text.toString() == buttons[7][i]!!.text.toString() && buttons[3][i]!!.text.toString() == buttons[5][i]!!.text.toString() && buttons[3][i]!!.text.toString() != "")
                && buttons[3][i]!!.text.toString() == "x"
                && buttons[4][i]!!.text.toString() == ""
                && buttons[4][i]!!.text.toString() != "x"
            ) {

                buttons[4][i]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[4][i]?.setText("o")!!
                player1Turn = !player1Turn
                break

            }
        }
        /********************************** TopRight to bottomLeft ******************************************/
        if (player1Turn) {
            if ((buttons[4][2]!!.text.toString() == buttons[6][0]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[2][4]!!.text.toString() && buttons[4][2]!!.text.toString() == buttons[0][6]!!.text.toString() && buttons[4][2]!!.text.toString() != "")
                && buttons[4][2]!!.text.toString() == "x"
                && buttons[3][3]!!.text.toString() == ""
                && buttons[3][3]!!.text.toString() != "x"
            ) {

                buttons[3][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[3][3]?.setText("o")!!
                player1Turn = !player1Turn

            } else if ((buttons[5][2]!!.text.toString() == buttons[7][0]!!.text.toString() && buttons[5][2]!!.text.toString() == buttons[3][4]!!.text.toString() && buttons[5][2]!!.text.toString() == buttons[1][6]!!.text.toString() && buttons[5][2]!!.text.toString() != "")
                && buttons[5][2]!!.text.toString() == "x"
                && buttons[4][3]!!.text.toString() == ""
                && buttons[4][3]!!.text.toString() != "x"
            ) {

                buttons[4][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[4][3]?.setText("o")!!
                player1Turn = !player1Turn

            } else if ((buttons[4][3]!!.text.toString() == buttons[6][1]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[2][5]!!.text.toString() && buttons[4][3]!!.text.toString() == buttons[0][7]!!.text.toString() && buttons[4][3]!!.text.toString() != "")
                && buttons[4][3]!!.text.toString() == "x"
                && buttons[3][4]!!.text.toString() == ""
                && buttons[3][4]!!.text.toString() != "x"
            ) {

                buttons[3][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[3][4]?.setText("o")!!
                player1Turn = !player1Turn

            } else if ((buttons[5][3]!!.text.toString() == buttons[7][1]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[3][5]!!.text.toString() && buttons[5][3]!!.text.toString() == buttons[1][7]!!.text.toString() && buttons[5][3]!!.text.toString() != "")
                && buttons[5][3]!!.text.toString() == "x"
                && buttons[4][4]!!.text.toString() == ""
                && buttons[4][4]!!.text.toString() != "x"
            ) {

                buttons[4][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[4][4]?.setText("o")!!
                player1Turn = !player1Turn

            }
            /********************************** TopLeft to bottomRight ******************************************/
            else if ((buttons[0][1]!!.text.toString() == buttons[2][3]!!.text.toString() && buttons[0][1]!!.text.toString() == buttons[4][5]!!.text.toString() && buttons[0][1]!!.text.toString() == buttons[6][7]!!.text.toString() && buttons[0][1]!!.text.toString() != "")
                && buttons[0][1]!!.text.toString() == "x"
                && buttons[3][4]!!.text.toString() == ""
                && buttons[3][4]!!.text.toString() != "x"
            ) {

                buttons[3][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[3][4]?.setText("o")!!
                player1Turn = !player1Turn

            } else if ((buttons[0][0]!!.text.toString() == buttons[2][2]!!.text.toString() && buttons[0][0]!!.text.toString() == buttons[4][4]!!.text.toString() && buttons[0][0]!!.text.toString() == buttons[6][6]!!.text.toString() && buttons[0][0]!!.text.toString() != "")
                && buttons[0][0]!!.text.toString() == "x"
                && buttons[3][3]!!.text.toString() == ""
                && buttons[3][3]!!.text.toString() != "x"
            ) {

                buttons[3][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[3][3]?.setText("o")!!
                player1Turn = !player1Turn

            } else if ((buttons[1][1]!!.text.toString() == buttons[3][3]!!.text.toString() && buttons[1][1]!!.text.toString() == buttons[5][5]!!.text.toString() && buttons[1][1]!!.text.toString() == buttons[7][7]!!.text.toString() && buttons[1][1]!!.text.toString() != "")
                && buttons[1][1]!!.text.toString() == "x"
                && buttons[4][4]!!.text.toString() == ""
                && buttons[4][4]!!.text.toString() != "x"
            ) {

                buttons[4][4]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[4][4]?.setText("o")!!
                player1Turn = !player1Turn

            } else if ((buttons[1][0]!!.text.toString() == buttons[3][2]!!.text.toString() && buttons[1][0]!!.text.toString() == buttons[5][4]!!.text.toString() && buttons[1][0]!!.text.toString() == buttons[7][6]!!.text.toString() && buttons[1][0]!!.text.toString() != "")
                && buttons[1][0]!!.text.toString() == "x"
                && buttons[4][3]!!.text.toString() == ""
                && buttons[4][3]!!.text.toString() != "x"
            ) {

                buttons[4][3]?.background = ContextCompat.getDrawable(this, R.drawable.o)
                buttons[4][3]?.setText("o")!!
                player1Turn = !player1Turn

            }
        }
    }

    /************* End of all Methods for computer Turn Algorithim **************/

    private fun bannerAds() {
        MobileAds.initialize(this) {}
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
        val adView = AdView(this)
        adView.adSize = AdSize.SMART_BANNER
        adView.adUnitId = "ca-app-pub-4454440016331822/1368931886"
        // Real Ads : ca-app-pub-4454440016331822/1368931886

    }

    private fun interstitialAd() {
        mInterstitialAd = InterstitialAd(this)
        mInterstitialAd.adUnitId = "ca-app-pub-4454440016331822/8097991769"
        mInterstitialAd.loadAd(AdRequest.Builder().build())
        // Real Ads : ca-app-pub-4454440016331822/8097991769
        mInterstitialAd.adListener = object : AdListener() {
            override fun onAdLoaded() {
                // Code to be executed when an ad finishes loading.
            }

            override fun onAdFailedToLoad(errorCode: Int) {
                // Code to be executed when an ad request fails.
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
            "ca-app-pub-4454440016331822/7531570468",
            AdRequest.Builder().build()
        )
        // real Reward ads: ca-app-pub-4454440016331822/7531570468
        // for test : ca-app-pub-3940256099942544/5224354917
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
        val view = LayoutInflater.from(this@HardLevelVsComputer)
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
        val view = LayoutInflater.from(this@HardLevelVsComputer)
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
        val view = LayoutInflater.from(this@HardLevelVsComputer)
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
        val view = LayoutInflater.from(this@HardLevelVsComputer)
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

        if (adWatched1) {
            view.adsText.text = watchAds2
        }

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
                    view.progress_circular.visibility = View.GONE
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
