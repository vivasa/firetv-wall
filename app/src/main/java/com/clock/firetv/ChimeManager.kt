package com.clock.firetv

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class ChimeManager(
    private val chimeIndicator: View,
    private val chimeDot: View
) {

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val NOTE_DURATION_MS = 400
        private const val DECAY_DURATION_MS = 2000
        private const val NOTE_GAP_MS = 400

        // Major chord: C5, E5, G5
        private const val FREQ_C5 = 523.25
        private const val FREQ_E5 = 659.25
        private const val FREQ_G5 = 783.99

        private const val INDICATOR_SHOW_DURATION_MS = 3000L
    }

    private val handler = Handler(Looper.getMainLooper())
    private var scheduledRunnable: Runnable? = null

    fun scheduleNextChime(onChimePlayed: (() -> Unit)? = null) {
        scheduledRunnable?.let { handler.removeCallbacks(it) }

        val now = System.currentTimeMillis()
        val msUntilNextHalfHour = calculateMsUntilNextHalfHour(now)

        scheduledRunnable = object : Runnable {
            override fun run() {
                playChime()
                showIndicator()
                onChimePlayed?.invoke()
                // Schedule next one in ~30 minutes
                handler.postDelayed(this, 30 * 60 * 1000L)
            }
        }
        handler.postDelayed(scheduledRunnable!!, msUntilNextHalfHour)
    }

    fun stop() {
        scheduledRunnable?.let { handler.removeCallbacks(it) }
        scheduledRunnable = null
    }

    private fun calculateMsUntilNextHalfHour(nowMs: Long): Long {
        val minuteMs = 60 * 1000L
        val halfHourMs = 30 * minuteMs
        val msSinceHalfHour = nowMs % halfHourMs
        val msUntil = halfHourMs - msSinceHalfHour
        // If we're within 1 second of a half-hour boundary, wait for the next one
        return if (msUntil < 1000L) halfHourMs else msUntil
    }

    private fun playChime() {
        Thread {
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

                val bufferSize = buffer.size * 2 // 16-bit = 2 bytes per sample
                val audioTrack = AudioTrack(
                    audioAttributes,
                    audioFormat,
                    bufferSize,
                    AudioTrack.MODE_STATIC,
                    AudioManager.AUDIO_SESSION_ID_GENERATE
                )

                // Set volume to 50%
                audioTrack.setVolume(0.5f)
                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()

                // Wait for playback to finish, then release
                Thread.sleep((DECAY_DURATION_MS + NOTE_GAP_MS * 2 + 500).toLong())
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun addNote(buffer: ShortArray, frequency: Double, startSample: Int) {
        val decaySamples = SAMPLE_RATE * DECAY_DURATION_MS / 1000
        for (i in 0 until decaySamples) {
            val idx = startSample + i
            if (idx >= buffer.size) break

            val t = i.toDouble() / SAMPLE_RATE
            val envelope = exp(-3.0 * t) // Natural exponential decay
            val sample = sin(2.0 * PI * frequency * t) * envelope * 0.3

            // Mix with existing content
            val existing = buffer[idx].toDouble() / Short.MAX_VALUE
            val mixed = (existing + sample).coerceIn(-1.0, 1.0)
            buffer[idx] = (mixed * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun showIndicator() {
        handler.post {
            chimeIndicator.visibility = View.VISIBLE
            chimeIndicator.alpha = 0f
            chimeIndicator.animate()
                .alpha(1f)
                .setDuration(300)
                .start()

            // Pulse the dot
            val pulse = AlphaAnimation(1f, 0.3f).apply {
                duration = 600
                repeatMode = Animation.REVERSE
                repeatCount = 4
            }
            chimeDot.startAnimation(pulse)

            // Hide after 3 seconds
            handler.postDelayed({
                chimeIndicator.animate()
                    .alpha(0f)
                    .setDuration(500)
                    .withEndAction {
                        chimeIndicator.visibility = View.GONE
                        chimeDot.clearAnimation()
                    }
                    .start()
            }, INDICATOR_SHOW_DURATION_MS)
        }
    }
}
