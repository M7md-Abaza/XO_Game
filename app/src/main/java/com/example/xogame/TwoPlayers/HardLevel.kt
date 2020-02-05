package com.example.xogame.TwoPlayers

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.xogame.R
import kotlinx.android.synthetic.main.easy_level.*
import kotlinx.android.synthetic.main.hard_level.*

class HardLevel : AppCompatActivity(), View.OnClickListener {

    private val buttons: Array<Array<Button?>> =
        Array(8) { arrayOfNulls<Button>(8) }

    private var player1Turn = true

    private var roundCount = 0

    private var player1Points = 0
    private var player2Points = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.hard_level)

        getButtonPosition()

        // btn_reset for rest Buttons without change players points
        btn_resetH.setOnClickListener {
            resetBoard()
            updatePointsText()
            Toast.makeText(this, "New Round Started", Toast.LENGTH_SHORT).show()
            btn_resetH.visibility = View.GONE
        }
    }

    override fun onClick(v: View) {
        if ((v as Button).text.toString() != "") {
            return
        }
        if (player1Turn) {
            v.background = ContextCompat.getDrawable(this,
                R.drawable.x
            )
            v.text = "x"
        } else {
            v.background = ContextCompat.getDrawable(this,
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
            /*
            this else is for change turn from player one to player two so
            the game check after checking that no winner and rountCount not equal 9
            that is mean there in more places"Button" to play
            */
            player1Turn = !player1Turn
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
        Toast.makeText(this, "Player X wins!", Toast.LENGTH_SHORT).show()
        updatePointsText()
        btn_resetH.visibility = View.VISIBLE
        for (i in 0..7) {
            for (j in 0..7) {
                buttons[i][j]?.text = "-"
            }
        }
    }

    private fun player2Wins() {
        player2Points++
        Toast.makeText(this, "Player O wins!", Toast.LENGTH_SHORT).show()
        updatePointsText()
        btn_resetH.visibility = View.VISIBLE
        for (i in 0..7) {
            for (j in 0..7) {
                buttons[i][j]?.text = "-"
            }
        }
    }

    private fun draw() {
        Toast.makeText(this, "Draw!", Toast.LENGTH_SHORT).show()
        btn_resetH.visibility = View.VISIBLE
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
                    //buttons[i][j]!!.setBackgroundResource(R.drawable.empty)
                    buttons[i][j]?.setBackgroundResource(R.drawable.empty)
                } else {
                    buttons[i][j]!!.setBackgroundResource(R.drawable.empty)
                }
                buttons[i][j]?.text = ""
            }
        }
        roundCount = 0
        player1Turn = true
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
}
