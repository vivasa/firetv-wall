package com.clock.firetv

import android.view.KeyEvent
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TransportControlsController(
    private val transportControls: LinearLayout,
    private val buttons: List<ImageButton>,
    private val callback: Callback,
    private val scope: CoroutineScope
) {
    interface Callback {
        fun onSkipBack()
        fun onRewind()
        fun onFastForward()
        fun onSkipForward()
    }

    var isVisible = false
        private set

    private var focusIndex = 0
    private var autoHideJob: Job? = null

    fun show() {
        isVisible = true
        focusIndex = 1
        transportControls.visibility = View.VISIBLE
        transportControls.animate().alpha(1f).setDuration(200).start()
        updateFocus()
        resetAutoHide()
    }

    fun hide() {
        isVisible = false
        autoHideJob?.cancel()
        transportControls.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                transportControls.visibility = View.GONE
            }
            .start()
    }

    fun handleKeyEvent(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK -> { hide(); true }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (focusIndex > 0) { focusIndex--; updateFocus() }
                resetAutoHide(); true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (focusIndex < buttons.size - 1) { focusIndex++; updateFocus() }
                resetAutoHide(); true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                activateButton(); resetAutoHide(); true
            }
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> { resetAutoHide(); true }
            else -> { hide(); true }
        }
    }

    fun cleanup() {
        autoHideJob?.cancel()
    }

    private fun resetAutoHide() {
        autoHideJob?.cancel()
        autoHideJob = scope.launch {
            delay(5000)
            hide()
        }
    }

    private fun updateFocus() {
        buttons.forEachIndexed { index, button ->
            if (index == focusIndex) {
                button.setBackgroundResource(R.drawable.transport_button_focused_bg)
            } else {
                button.setBackgroundColor(0x00000000)
            }
        }
    }

    private fun activateButton() {
        when (focusIndex) {
            0 -> callback.onSkipBack()
            1 -> callback.onRewind()
            2 -> callback.onFastForward()
            3 -> callback.onSkipForward()
        }
    }
}
