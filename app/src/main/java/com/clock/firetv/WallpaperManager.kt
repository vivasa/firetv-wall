package com.clock.firetv

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.widget.ImageView
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WallpaperManager(
    private val frontView: ImageView,
    private val backView: ImageView,
    private val scope: CoroutineScope
) {

    companion object {
        private const val CROSSFADE_DURATION_MS = 2000L
        private const val BASE_URL = "https://picsum.photos/1920/1080"
    }

    private var imageCounter = 0
    private var intervalMs = 5 * 60 * 1000L
    private var hueRotation = 0f
    private var rotationJob: Job? = null

    fun start(intervalMinutes: Int) {
        intervalMs = intervalMinutes * 60 * 1000L
        rotationJob?.cancel()
        loadNextWallpaper()
        rotationJob = scope.launch {
            while (isActive) {
                delay(intervalMs)
                loadNextWallpaper()
            }
        }
    }

    fun stop() {
        rotationJob?.cancel()
    }

    fun updateInterval(intervalMinutes: Int) {
        intervalMs = intervalMinutes * 60 * 1000L
        if (rotationJob?.isActive == true) {
            rotationJob?.cancel()
            rotationJob = scope.launch {
                while (isActive) {
                    delay(intervalMs)
                    loadNextWallpaper()
                }
            }
        }
    }

    private fun loadNextWallpaper() {
        imageCounter++
        val url = "$BASE_URL?random=$imageCounter&t=${System.currentTimeMillis()}"

        scope.launch {
            try {
                val loader = ImageLoader(frontView.context)
                val request = ImageRequest.Builder(frontView.context)
                    .data(url)
                    .allowHardware(false)
                    .build()

                val result = loader.execute(request)
                if (result is SuccessResult) {
                    withContext(Dispatchers.Main) {
                        crossfadeTo(result.drawable)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showFallbackGradient()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showFallbackGradient()
                }
            }
        }
    }

    private fun crossfadeTo(drawable: android.graphics.drawable.Drawable) {
        backView.setImageDrawable(frontView.drawable)
        backView.alpha = 1f
        frontView.setImageDrawable(drawable)
        frontView.alpha = 0f
        frontView.animate().alpha(1f).setDuration(CROSSFADE_DURATION_MS).start()
        backView.animate().alpha(0f).setDuration(CROSSFADE_DURATION_MS).start()
    }

    private fun showFallbackGradient() {
        hueRotation += 30f
        if (hueRotation >= 360f) hueRotation = 0f

        val bitmap = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        val hsv1 = floatArrayOf(hueRotation, 0.4f, 0.15f)
        val hsv2 = floatArrayOf((hueRotation + 40f) % 360f, 0.5f, 0.2f)

        paint.shader = LinearGradient(
            0f, 0f, 1920f, 1080f,
            Color.HSVToColor(hsv1), Color.HSVToColor(hsv2),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, 1920f, 1080f, paint)

        crossfadeTo(BitmapDrawable(frontView.resources, bitmap))
    }
}
