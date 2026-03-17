package com.clock.firetv

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class PinOverlayManager(
    private val context: Context,
    private val rootLayout: FrameLayout
) {
    fun showPin(pin: String) {
        val existing = rootLayout.findViewWithTag<View>("pinOverlay")
        existing?.let { rootLayout.removeView(it) }

        val overlay = FrameLayout(context).apply {
            tag = "pinOverlay"
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(0xCC000000.toInt())
            alpha = 0f
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        }

        val pinText = TextView(context).apply {
            text = pin
            textSize = 64f
            setTextColor(0xFFE8A850.toInt())
            typeface = android.graphics.Typeface.MONOSPACE
            gravity = Gravity.CENTER
            letterSpacing = 0.3f
        }

        val instrText = TextView(context).apply {
            text = "Enter this code on your phone"
            textSize = 16f
            setTextColor(0xAAFFFFFF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 0)
        }

        container.addView(pinText)
        container.addView(instrText)
        overlay.addView(container)
        rootLayout.addView(overlay)
        overlay.animate().alpha(1f).setDuration(300).start()
    }

    fun dismissPin() {
        val overlay = rootLayout.findViewWithTag<View>("pinOverlay") ?: return
        overlay.animate().alpha(0f).setDuration(300).withEndAction {
            rootLayout.removeView(overlay)
        }.start()
    }
}
