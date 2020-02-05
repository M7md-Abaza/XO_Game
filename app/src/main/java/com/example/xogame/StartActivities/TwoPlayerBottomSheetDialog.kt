package com.example.xogame.StartActivities

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Nullable
import com.example.xogame.R
import com.example.xogame.TwoPlayers.EasyLevel
import com.example.xogame.TwoPlayers.HardLevel
import com.example.xogame.TwoPlayers.MediumLevel
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
            dismiss()
            val intent = Intent(context, EasyLevel::class.java)
            startActivity(intent)

        }
        v.medium.setOnClickListener {
            dismiss()
            val intent = Intent(context, MediumLevel::class.java)
            startActivity(intent)
        }
        v.hard.setOnClickListener {
            dismiss()
            val intent = Intent(context, HardLevel::class.java)
            startActivity(intent)
        }

        return v
    }

}