package com.m7mdabaza.xogame.startActivities

import android.content.Intent
import android.graphics.Typeface
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Nullable
import com.m7mdabaza.xogame.onePlayer.EasyLevelVsComputer
import com.m7mdabaza.xogame.onePlayer.MediumLevelVsComputer
import com.m7mdabaza.xogame.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.m7mdabaza.xogame.onePlayer.HardLevelVsComputer
class OnePlayerBottomSheetDialog : BottomSheetDialogFragment() {

    @Nullable
    override fun onCreateView(inflater: LayoutInflater, @Nullable container: ViewGroup?, @Nullable savedInstanceState: Bundle?): View? {
        val v: View = inflater.inflate(R.layout.one_player_bottom_sheet, container, false)

        val typeface = Typeface.createFromAsset(activity?.assets, "sukar.ttf")
        v.textView4.typeface = typeface
        v.easy.typeface = typeface
        v.medium.typeface = typeface
        v.hard.typeface = typeface

        v.easy.setOnClickListener {
            clickSound()
            dismiss()
            val intent = Intent(context, EasyLevelVsComputer::class.java)
            startActivity(intent)
        }
        v.medium.setOnClickListener {
            clickSound()
            dismiss()
            val intent = Intent(context, MediumLevelVsComputer::class.java)
            startActivity(intent)
        }

        v.hard.setOnClickListener {
            clickSound()
            dismiss()
            val intent = Intent(context, HardLevelVsComputer::class.java)
            startActivity(intent)
        }

        return v
    }

    private fun clickSound() {
        val mediaPlayer: MediaPlayer = MediaPlayer.create(context, R.raw.click)
        mediaPlayer.start()
    }


}

// View accessors (replace the removed kotlin-android-extensions synthetics)
private val android.view.View.easy: android.widget.TextView get() = findViewById(R.id.easy)
private val android.view.View.hard: android.widget.TextView get() = findViewById(R.id.hard)
private val android.view.View.medium: android.widget.TextView get() = findViewById(R.id.medium)
private val android.view.View.textView4: android.widget.TextView get() = findViewById(R.id.textView4)
