package com.example.xogame

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import kotlinx.android.synthetic.main.easy_level.*

class HardLevel : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.hard_level)

        handelToolbar()
    }

    // Handle toolbar style, colors and Buttons
    private fun handelToolbar() {
        toolbar.title = "X-O Game"
        toolbar.setTitleTextColor(Color.WHITE)
        setSupportActionBar(toolbar)
        assert(supportActionBar != null)
        supportActionBar!!.setDisplayHomeAsUpEnabled(false)
        supportActionBar!!.setDisplayShowHomeEnabled(false)


    }

    // Handle item selection from menu
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            // menu_newGame for rest the game and change players points to zero
            R.id.menu_newGame -> {
               /* resetBoard()
                player1Points = 0
                player2Points = 0
                updatePointsText()*/
                Toast.makeText(this, "New Game Started", Toast.LENGTH_SHORT).show()
                true
            }

            R.id.menu_easy -> {
                val intent = Intent(applicationContext, EasyLevel::class.java)
                startActivity(intent)
                Toast.makeText(this, "Easy Level Started", Toast.LENGTH_SHORT).show()
                true
            }

            R.id.menu_medium -> {
                val intent = Intent(applicationContext, MediumLevel::class.java)
                startActivity(intent)
                Toast.makeText(this, "Medium Level Started", Toast.LENGTH_SHORT).show()
                true
            }

            R.id.menu_hard -> {
                val intent = Intent(applicationContext, HardLevel::class.java)
                startActivity(intent)
                Toast.makeText(this, "Hard Level Started", Toast.LENGTH_SHORT).show()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    // Handle menu to display on toolbar
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }
}
