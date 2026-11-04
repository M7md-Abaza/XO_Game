package com.m7mdabaza.xogame.presentation.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.m7mdabaza.xogame.domain.model.Player
import com.m7mdabaza.xogame.presentation.R
import com.m7mdabaza.xogame.presentation.game.GameFeedback
import com.m7mdabaza.xogame.presentation.game.RoundSound
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plays the game sounds with one shared [SoundPool] (all sounds are loaded
 * once, up front) and gives short vibrations for the human's moves and for
 * round results. Lives as long as the app, so the pool is never released.
 */
@Singleton
class AndroidGameFeedback @Inject constructor(
    @ApplicationContext context: Context,
) : GameFeedback {

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val clickX = soundPool.load(context, R.raw.click, 1)
    private val clickO = soundPool.load(context, R.raw.click1, 1)
    private val win = soundPool.load(context, R.raw.win, 1)
    private val lose = soundPool.load(context, R.raw.loser, 1)
    private val draw = soundPool.load(context, R.raw.draw, 1)
    private val reset = soundPool.load(context, R.raw.rest, 1)

    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    override fun move(player: Player, byComputer: Boolean) {
        play(if (player == Player.X) clickX else clickO)
        if (!byComputer) vibrate(strong = false)
    }

    override fun roundEnded(sound: RoundSound) {
        play(
            when (sound) {
                RoundSound.WIN -> win
                RoundSound.LOSE -> lose
                RoundSound.DRAW -> draw
            },
        )
        vibrate(strong = true)
    }

    override fun newRound() = play(reset)

    /** Does nothing if the sound hasn't finished loading yet. */
    private fun play(soundId: Int) {
        soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    private fun vibrate(strong: Boolean) {
        val vibrator = vibrator?.takeIf { it.hasVibrator() } ?: return
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> vibrator.vibrate(
                VibrationEffect.createPredefined(
                    if (strong) VibrationEffect.EFFECT_HEAVY_CLICK else VibrationEffect.EFFECT_TICK,
                ),
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> vibrator.vibrate(
                VibrationEffect.createOneShot(if (strong) 60L else 15L, VibrationEffect.DEFAULT_AMPLITUDE),
            )
            else -> @Suppress("DEPRECATION") vibrator.vibrate(if (strong) 60L else 15L)
        }
    }
}
