package com.m7mdabaza.xogame.twoPlayers

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.m7mdabaza.xogame.R
import com.google.android.gms.ads.*
import kotlinx.android.synthetic.main.hard_level.*
import kotlinx.android.synthetic.main.win_pop_up_dialog.view.*
import kotlinx.android.synthetic.main.win_pop_up_dialog.view.dialogNewRound
import kotlinx.android.synthetic.main.x_or_o_dialog.view.*

class HardLevel : AppCompatActivity(), View.OnClickListener {

    private lateinit var mInterstitialAd: InterstitialAd


    private val buttons: Array<Array<Button?>> =
        Array(8) { arrayOfNulls<Button>(8) }

    private var player1Turn = true
    private var xPlayFirst = true

    private var roundCount = 0      // to determine Draw Case
    private var playTimeCount =
        0   // to determine the computer Turn pattern and ads time to display

    private var player1Points = 0
    private var player2Points = 0

    private var draw: String = ""
    private var draw2: String = ""
    private var xWin: String = ""
    private var oWin: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.hard_level)

        getButtonPosition()

        val typeface = Typeface.createFromAsset(assets, "sukar.ttf")
        textView2.typeface = typeface
        textView9.typeface = typeface
        textView6.typeface = typeface
        textView7.typeface = typeface
        txt_player_1H.typeface = typeface
        txt_player_2H.typeface = typeface

        draw = getString(R.string.its_draw)
        draw2 = getString(R.string.its_draw2)
        xWin = getString(R.string.player_X_win)
        oWin = getString(R.string.player_O_win)

        chooseFirstPlayerDialog()

        bannerAds()
        interstitialAd()

    }

    override fun onClick(v: View) {
        if ((v as Button).text.toString() != "") {
            return
        }
        if (xPlayFirst) {
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

            /*
            if checkForWin() return true which mean that there is
            a player win then we check who player turn to decide the winner
            */
            if (checkForWin()) {
                if (player1Turn) {
                    player1Wins()
                } else {
                    player2Wins()
                }
            } else if (roundCount == 64) {
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
        else if (!xPlayFirst) {
            if (!player1Turn) {
                v.background = ContextCompat.getDrawable(this, R.drawable.o)
                v.text = "o"
            } else {
                v.background = ContextCompat.getDrawable(this, R.drawable.x)
                v.text = "x"
            }
            roundCount++

            if (checkForWin()) {
                if (!player1Turn) {
                    player2Wins()
                } else {
                    player1Wins()
                }
            } else if (roundCount == 16) {
                draw()
            } else {
                if (!player1Turn) {
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

    @SuppressLint("SetTextI18n")
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
    }

    @SuppressLint("SetTextI18n")
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
    }

    @SuppressLint("SetTextI18n")
    private fun draw() {
        showDrawDialog()
        playTimeCount++
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


        if (playTimeCount == 1 || playTimeCount == 3 || playTimeCount == 6 || playTimeCount == 9 || playTimeCount == 12 || playTimeCount == 15) {
            if (mInterstitialAd.isLoaded) {
                mInterstitialAd.show()
            } else {
                Log.d("TAG", "The interstitial wasn't loaded yet.")
            }
        } else if (playTimeCount == 16) {
            playTimeCount = 0
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
        adView.adUnitId = ""
        
    }

    private fun interstitialAd() {
        mInterstitialAd = InterstitialAd(this)
        mInterstitialAd.adUnitId = ""
        
        mInterstitialAd.loadAd(AdRequest.Builder().build())


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

    @SuppressLint("InflateParams")
    private fun showXWinDialog() {
        val view = LayoutInflater.from(this@HardLevel)
            .inflate(R.layout.win_pop_up_dialog, null)
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCanceledOnTouchOutside(false)

        val typeface = Typeface.createFromAsset(assets, "sukar.ttf")
        view.textView10.typeface = typeface
        view.dialogNewRound.typeface = typeface

        view.textView10.text = xWin

        winSound()

        view.dialogNewRound.setOnClickListener {
            resetBoard()
            resetGameSound()
            updatePointsText()

            dialog.dismiss()
            chooseFirstPlayerDialog()
        }
        dialog.show()
    }

    @SuppressLint("InflateParams")
    private fun showOWinDialog() {
        val view = LayoutInflater.from(this@HardLevel)
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

        view.textView10.text = oWin

        winSound()

        view.dialogNewRound.setOnClickListener {
            resetBoard()
            resetGameSound()
            updatePointsText()

            dialog.dismiss()
            chooseFirstPlayerDialog()
        }
        dialog.show()
    }

    @SuppressLint("InflateParams")
    private fun showDrawDialog() {
        val view = LayoutInflater.from(this@HardLevel)
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

        view.textView10.text = draw2
        view.textView11.text = draw

        drawSound()
        view.dialogNewRound.setOnClickListener {
            resetBoard()
            resetGameSound()
            updatePointsText()

            dialog.dismiss()
            chooseFirstPlayerDialog()
        }
        dialog.show()
    }

    @SuppressLint("InflateParams")
    private fun chooseFirstPlayerDialog() {
        val view = LayoutInflater.from(this@HardLevel)
            .inflate(R.layout.x_or_o_dialog, null)
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCanceledOnTouchOutside(false)

        val typeface = Typeface.createFromAsset(assets, "sukar.ttf")

        view.textView12.typeface = typeface
        view.dialogNewRound.typeface = typeface

        var playerSelected = false

        /********************Banner Ad************************/
        MobileAds.initialize(this) {}
        val adRequest = AdRequest.Builder().build()
        view.adView1.loadAd(adRequest)
        val adView = AdView(this)
        adView.adSize = AdSize.SMART_BANNER
        adView.adUnitId = ""
        
        /********************************************/

        view.imageX.setOnClickListener {
            view.imageO.setImageResource(R.drawable.o1)
            view.imageX.setImageResource(R.drawable.x_white)
            clickSound1()
            xPlayFirst = true
            player1Turn = true
            playerSelected = true

        }
        view.imageO.setOnClickListener {
            view.imageO.setImageResource(R.drawable.o_white)
            view.imageX.setImageResource(R.drawable.x)
            clickSound1()
            xPlayFirst = false
            player1Turn = false
            playerSelected = true

        }

        view.dialogNewRound.setOnClickListener {
            clickSound()
            if (playerSelected) {
                dialog.dismiss()
            } else if (!playerSelected) {
                Toast.makeText(this, "Please Choose X or O", Toast.LENGTH_LONG).show()
            }
        }
        dialog.show()
    }

}
