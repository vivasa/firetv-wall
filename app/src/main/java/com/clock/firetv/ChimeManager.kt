package com.clock.firetv

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class ChimeManager(
    private val chimeIndicator: View,
    private val chimeDot: View,
    private val scope: CoroutineScope
) {

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val NOTE_DURATION_MS = 400
        private const val DECAY_DURATION_MS = 2000
        private const val NOTE_GAP_MS = 400

        private const val FREQ_C5 = 523.25
        private const val FREQ_E5 = 659.25
        private const val FREQ_G5 = 783.99

        private const val INDICATOR_SHOW_DURATION_MS = 3000L

        internal fun calculateMsUntilNextHalfHour(nowMs: Long): Long {
            val minuteMs = 60 * 1000L
            val halfHourMs = 30 * minuteMs
            val msSinceHalfHour = nowMs % halfHourMs
            val msUntil = halfHourMs - msSinceHalfHour
            return if (msUntil < 1000L) halfHourMs else msUntil
        }
    }

    private var scheduleJob: Job? = null

    fun scheduleNextChime(onChimePlayed: (() -> Unit)? = null) {
        scheduleJob?.cancel()

        val now = System.currentTimeMillis()
        val msUntilNextHalfHour = calculateMsUntilNextHalfHour(now)

        scheduleJob = scope.launch {
            delay(msUntilNextHalfHour)
            while (isActive) {
                playChime()
                showIndicator()
                onChimePlayed?.invoke()
                delay(30 * 60 * 1000L)
            }
        }
    }

    fun stop() {
        scheduleJob?.cancel()
    }

    private fun calculateMsUntilNextHalfHour(nowMs: Long): Long =
        Companion.calculateMsUntilNextHalfHour(nowMs)

    private fun playChime() {
        scope.launch(Dispatchers.IO) {
            try {
                val totalSamples = SAMPLE_RATE * (DECAY_DURATION_MS + NOTE_GAP_MS * 2) / 1000
                val buffer = ShortArray(totalSamples)

                addNote(buffer, FREQ_C5, 0)
                addNote(buffer, FREQ_E5, SAMPLE_RATE * NOTE_GAP_MS / 1000)
                addNote(buffer, FREQ_G5, SAMPLE_RATE * NOTE_GAP_MS * 2 / 1000)

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val audioFormat = AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()

                val bufferSize = buffer.size * 2
                val audioTrack = AudioTrack(
                    audioAttributes, audioFormat, bufferSize,
                    AudioTrack.MODE_STATIC, AudioManager.AUDIO_SESSION_ID_GENERATE
                )

                audioTrack.setVolume(0.5f)
                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()

                delay((DECAY_DURATION_MS + NOTE_GAP_MS * 2 + 500).toLong())
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun addNote(buffer: ShortArray, frequency: Double, startSample: Int) {
        val decaySamples = SAMPLE_RATE * DECAY_DURATION_MS / 1000
        for (i in 0 until decaySamples) {
            val idx = startSample + i
            if (idx >= buffer.size) break

            val t = i.toDouble() / SAMPLE_RATE
            val envelope = exp(-3.0 * t)
            val sample = sin(2.0 * PI * frequency * t) * envelope * 0.3

            val existing = buffer[idx].toDouble() / Short.MAX_VALUE
            val mixed = (existing + sample).coerceIn(-1.0, 1.0)
            buffer[idx] = (mixed * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun showIndicator() {
        scope.launch {
            chimeIndicator.visibility = View.VISIBLE
            chimeIndicator.alpha = 0f
            chimeIndicator.animate().alpha(1f).setDuration(300).start()

            val pulse = AlphaAnimation(1f, 0.3f).apply {
                duration = 600
                repeatMode = Animation.REVERSE
                repeatCount = 4
            }
            chimeDot.startAnimation(pulse)

            delay(INDICATOR_SHOW_DURATION_MS)
            chimeIndicator.animate()
                .alpha(0f)
                .setDuration(500)
                .withEndAction {
                    chimeIndicator.visibility = View.GONE
                    chimeDot.clearAnimation()
                }
                .start()
        }
    }
}
