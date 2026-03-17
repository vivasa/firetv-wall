package com.clock.firetv

import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class DriftAnimator(
    private val targetView: View,
    private val scope: CoroutineScope
) {

    companion object {
        private const val MAX_DRIFT = 30f
        private const val DRIFT_INTERVAL_MS = 2 * 60 * 1000L
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

    private var currentX = 0f
    private var currentY = 0f
    private var driftJob: Job? = null

    fun start() {
        driftJob?.cancel()
        driftJob = scope.launch {
            delay(DRIFT_INTERVAL_MS)
            while (isActive) {
                driftToNewPosition()
                delay(DRIFT_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        driftJob?.cancel()
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
