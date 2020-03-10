package com.m7mdabaza.xogame.startActivities

import android.content.Intent
import android.graphics.Typeface
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Nullable
import com.m7mdabaza.xogame.R
import com.m7mdabaza.xogame.twoPlayers.EasyLevel
import com.m7mdabaza.xogame.twoPlayers.HardLevel
import com.m7mdabaza.xogame.twoPlayers.MediumLevel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.two_player_bottom_sheet.view.*


class TwoPlayerBottomSheetDialog : BottomSheetDialogFragment() {


    @Nullable
    override fun onCreateView(inflater: LayoutInflater, @Nullable container: ViewGroup?, @Nullable savedInstanceState: Bundle?): View? {
        val v: View = inflater.inflate(R.layout.two_player_bottom_sheet, container, false)

        val typeface = Typeface.createFromAsset(activity?.assets, "sukar.ttf")
        v.textView4.typeface = typeface
        v.easy.typeface = typeface
        v.medium.typeface = typeface

        v.easy.setOnClickListener {
            clickSound()
            dismiss()
            val intent = Intent(context, EasyLevel::class.java)
            startActivity(intent)

        }
        v.medium.setOnClickListener {
            clickSound()
            dismiss()
            val intent = Intent(context, MediumLevel::class.java)
            startActivity(intent)
        }
        v.hard.setOnClickListener {
            clickSound()
            dismiss()
            val intent = Intent(context, HardLevel::class.java)
            startActivity(intent)
        }

        return v
    }

    private fun clickSound() {
        val mediaPlayer: MediaPlayer = MediaPlayer.create(context, R.raw.click)
        mediaPlayer.start()
    }


}