package com.m7mdabaza.xogame

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.m7mdabaza.xogame.domain.model.GameMode
import com.m7mdabaza.xogame.domain.model.Level
import com.m7mdabaza.xogame.onePlayer.EasyLevelVsComputer
import com.m7mdabaza.xogame.onePlayer.HardLevelVsComputer
import com.m7mdabaza.xogame.onePlayer.MediumLevelVsComputer
import com.m7mdabaza.xogame.presentation.XoApp
import com.m7mdabaza.xogame.twoPlayers.EasyLevel
import com.m7mdabaza.xogame.twoPlayers.HardLevel
import com.m7mdabaza.xogame.twoPlayers.MediumLevel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single Compose activity. AppCompat-based so per-app language switching
 * works on Android 7–12 too (milestone 5).
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            XoApp(onStartGame = ::startLegacyGame)
        }
    }

    /** Temporary bridge to the XML game screens until the Compose game screen (milestone 4). */
    private fun startLegacyGame(mode: GameMode, level: Level) {
        val screen = when (mode) {
            GameMode.VS_COMPUTER -> when (level) {
                Level.EASY -> EasyLevelVsComputer::class.java
                Level.MEDIUM -> MediumLevelVsComputer::class.java
                Level.HARD -> HardLevelVsComputer::class.java
            }
            GameMode.TWO_PLAYERS -> when (level) {
                Level.EASY -> EasyLevel::class.java
                Level.MEDIUM -> MediumLevel::class.java
                Level.HARD -> HardLevel::class.java
            }
        }
        startActivity(Intent(this, screen))
    }
}
