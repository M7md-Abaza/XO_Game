package com.m7mdabaza.xogame.twoPlayers

import com.m7mdabaza.xogame.presentation.ads.loadAdIfAllowed
import com.m7mdabaza.xogame.ui.setupEdgeToEdge
import com.m7mdabaza.xogame.presentation.ads.InterstitialAdManager

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.m7mdabaza.xogame.R
import com.google.android.gms.ads.*
class EasyLevel : AppCompatActivity(), View.OnClickListener {

    private lateinit var mInterstitialAd: InterstitialAdManager


    private val buttons: Array<Array<Button?>> =
        Array(3) { arrayOfNulls<Button>(3) }

    private var player1Turn = true
    private var xPlayFirst = true

    private var roundCount = 0      // to determine Draw Case
    private var playTimeCount =
        0   // to determine the computer Turn pattern  and ads time to display

    private var player1Points = 0
    private var player2Points = 0

    private var draw: String = ""
    private var draw2: String = ""
    private var xWin: String = ""
    private var oWin: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()
        setContentView(R.layout.easy_level)

        getButtonPosition()

        val typeface = Typeface.createFromAsset(assets, "sukar.ttf")
        textView2.typeface = typeface
        textView8.typeface = typeface
        textView6.typeface = typeface
        textView7.typeface = typeface
        txt_player_1.typeface = typeface
        txt_player_2.typeface = typeface

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

            if (checkForWin()) {
                if (player1Turn) {
                    player1Wins()
                } else {
                    player2Wins()
                }
            } else if (roundCount == 9) {
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
            } else if (roundCount == 9) {
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
        updatePointsText()

        for (i in 0..2) {
            for (j in 0..2) {
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
        for (i in 0..2) {
            for (j in 0..2) {
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
        txt_player_1.text = player1Points.toString()
        txt_player_2.text = player2Points.toString()
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


        if (playTimeCount == 1 || playTimeCount == 4 || playTimeCount == 7 || playTimeCount == 10 || playTimeCount == 14 || playTimeCount == 18) {
            if (mInterstitialAd.isLoaded) {
                mInterstitialAd.show()
            } else {
                Log.d("TAG", "The interstitial wasn't loaded yet.")
                mInterstitialAd.load()
            }
        } else if (playTimeCount == 20) {
            playTimeCount = 0
        }

    }

    // to get the Button position
    private fun getButtonPosition() {
        for (i in 0..2) {
            for (j in 0..2) {
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
        adView.loadAdIfAllowed()
        
    }

    private fun interstitialAd() {
        mInterstitialAd = InterstitialAdManager(this, getString(R.string.interstitial_easy_level))
        mInterstitialAd.load()
    }


    @SuppressLint("InflateParams")
    private fun showXWinDialog() {
        val view = LayoutInflater.from(this@EasyLevel)
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
        val view = LayoutInflater.from(this@EasyLevel)
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
        val view = LayoutInflater.from(this@EasyLevel)
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
        val view = LayoutInflater.from(this@EasyLevel)
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
        view.adView1.loadAdIfAllowed()
        
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
                Toast.makeText(this,"Please Choose X or O",Toast.LENGTH_LONG).show()
            }
        }
        dialog.show()
    }


}

// View accessors (replace the removed kotlin-android-extensions synthetics)
private val EasyLevel.adView: com.google.android.gms.ads.AdView get() = findViewById(R.id.adView)
private val EasyLevel.textView2: android.widget.TextView get() = findViewById(R.id.textView2)
private val EasyLevel.textView6: android.widget.TextView get() = findViewById(R.id.textView6)
private val EasyLevel.textView7: android.widget.TextView get() = findViewById(R.id.textView7)
private val EasyLevel.textView8: android.widget.TextView get() = findViewById(R.id.textView8)
private val EasyLevel.txt_player_1: android.widget.TextView get() = findViewById(R.id.txt_player_1)
private val EasyLevel.txt_player_2: android.widget.TextView get() = findViewById(R.id.txt_player_2)
private val android.view.View.adView1: com.google.android.gms.ads.AdView get() = findViewById(R.id.adView1)
private val android.view.View.dialogNewRound: android.widget.TextView get() = findViewById(R.id.dialogNewRound)
private val android.view.View.imageO: android.widget.ImageView get() = findViewById(R.id.imageO)
private val android.view.View.imageX: android.widget.ImageView get() = findViewById(R.id.imageX)
private val android.view.View.textView10: android.widget.TextView get() = findViewById(R.id.textView10)
private val android.view.View.textView11: android.widget.TextView get() = findViewById(R.id.textView11)
private val android.view.View.textView12: android.widget.TextView get() = findViewById(R.id.textView12)
