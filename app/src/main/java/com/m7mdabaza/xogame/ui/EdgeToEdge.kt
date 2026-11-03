package com.m7mdabaza.xogame.ui

import android.graphics.Color
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Android 15+ (targetSdk 35+) always draws apps edge-to-edge. Call this right
 * before setContentView(): transparent system bars with dark icons (the app
 * uses a light background), and after the layout is attached its root view is
 * padded by the system-bar/cutout insets so nothing sits under the bars while
 * the root background still fills the whole screen.
 */
fun ComponentActivity.setupEdgeToEdge() {
    enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
    )
    val content = findViewById<ViewGroup>(android.R.id.content)
    content.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
        override fun onChildViewAdded(parent: android.view.View, child: android.view.View) {
            val left = child.paddingLeft
            val top = child.paddingTop
            val right = child.paddingRight
            val bottom = child.paddingBottom
            ViewCompat.setOnApplyWindowInsetsListener(child) { v, insets ->
                val bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
                )
                v.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + bars.bottom)
                insets
            }
            ViewCompat.requestApplyInsets(child)
        }

        override fun onChildViewRemoved(parent: android.view.View, child: android.view.View) {}
    })
}
