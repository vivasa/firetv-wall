package com.clock.firetv

import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

class NightDimController(
    private val nightDimOverlay: View,
    private val scope: CoroutineScope
) {
    private var currentDimAlpha = 0f
    private var targetDimAlpha = 0f
    private var animJob: Job? = null

    fun check(enabled: Boolean, primaryTimezone: String) {
        if (!enabled) {
            if (currentDimAlpha > 0f) {
                animateDimTo(0f)
            }
            return
        }

        val tz = TimeZone.getTimeZone(primaryTimezone)
        val cal = Calendar.getInstance(tz)
        val hour = cal.get(Calendar.HOUR_OF_DAY)

        val isNight = hour >= 23 || hour < 6
        val newTarget = if (isNight) 0.45f else 0f

        if (newTarget != targetDimAlpha) {
            targetDimAlpha = newTarget
            animateDimTo(targetDimAlpha)
        }
    }

    fun cleanup() {
        animJob?.cancel()
    }

    private fun animateDimTo(target: Float) {
        val steps = 120
        val delta = (target - currentDimAlpha) / steps

        animJob?.cancel()
        animJob = scope.launch {
            repeat(steps) {
                currentDimAlpha += delta
                nightDimOverlay.alpha = currentDimAlpha.coerceIn(0f, 1f)
                delay(500)
            }
            currentDimAlpha = target
            nightDimOverlay.alpha = target
        }
    }
}
