package com.clock.firetv

import android.animation.ValueAnimator
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.random.Random

class DriftAnimator(private val targetView: View) {

    companion object {
        private const val MAX_DRIFT = 30f
        private const val DRIFT_INTERVAL_MS = 2 * 60 * 1000L // 2 minutes
        private const val ANIMATION_DURATION_MS = 8000L

        internal fun calculateDriftPosition(
            currentX: Float, currentY: Float,
            targetX: Float, targetY: Float,
            maxDrift: Float = MAX_DRIFT
        ): Pair<Float, Float> {
            val newX = ((currentX + targetX) / 2f).coerceIn(-maxDrift, maxDrift)
            val newY = ((currentY + targetY) / 2f).coerceIn(-maxDrift, maxDrift)
            return Pair(newX, newY)
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var currentX = 0f
    private var currentY = 0f
    private var running = false

    private val driftRunnable = object : Runnable {
        override fun run() {
            if (!running) return
            driftToNewPosition()
            handler.postDelayed(this, DRIFT_INTERVAL_MS)
        }
    }

    fun start() {
        running = true
        handler.postDelayed(driftRunnable, DRIFT_INTERVAL_MS)
    }

    fun stop() {
        running = false
        handler.removeCallbacks(driftRunnable)
    }

    fun reset() {
        stop()
        currentX = 0f
        currentY = 0f
        targetView.translationX = 0f
        targetView.translationY = 0f
    }

    private fun driftToNewPosition() {
        val targetX = (Random.nextFloat() * 2 - 1) * MAX_DRIFT
        val targetY = (Random.nextFloat() * 2 - 1) * MAX_DRIFT

        val (clampedX, clampedY) = calculateDriftPosition(currentX, currentY, targetX, targetY)

        val animX = ValueAnimator.ofFloat(currentX, clampedX).apply {
            duration = ANIMATION_DURATION_MS
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { targetView.translationX = it.animatedValue as Float }
        }

        val animY = ValueAnimator.ofFloat(currentY, clampedY).apply {
            duration = ANIMATION_DURATION_MS
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { targetView.translationY = it.animatedValue as Float }
        }

        animX.start()
        animY.start()

        currentX = clampedX
        currentY = clampedY
    }
}
