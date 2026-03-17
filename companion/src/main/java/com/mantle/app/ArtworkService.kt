package com.mantle.app

import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class ArtworkService(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {

    companion object {
        private val YOUTUBE_URL_PATTERN = Regex(
            """(?:https?://)?(?:www\.)?(?:youtube\.com|youtu\.be)/"""
        )
    }

    suspend fun fetchArtworkUrl(youtubeUrl: String): String? = withContext(Dispatchers.IO) {
        if (!isYouTubeUrl(youtubeUrl)) return@withContext null

        try {
            val oembedUrl = "https://www.youtube.com/oembed?url=${java.net.URLEncoder.encode(youtubeUrl, "UTF-8")}&format=json"
            val request = Request.Builder().url(oembedUrl).build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null
            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)
            json.optString("thumbnail_url", null)
        } catch (e: Exception) {
            null
        }
    }

    fun isYouTubeUrl(url: String): Boolean {
        return YOUTUBE_URL_PATTERN.containsMatchIn(url)
    }

    suspend fun fetchAndStoreArtwork(presetIndex: Int, url: String, configStore: MantleConfigStore) {
        val artworkUrl = fetchArtworkUrl(url)
        if (artworkUrl != null) {
            configStore.setPresetArtworkUrl(presetIndex, artworkUrl)
        }
    }

    fun fallbackGradientColors(name: String): Pair<Int, Int> {
        val hash = abs(name.hashCode())
        val hue1 = (hash % 360).toFloat()
        val hue2 = ((hash / 360) % 360).toFloat()
        val color1 = Color.HSVToColor(floatArrayOf(hue1, 0.5f, 0.4f))
        val color2 = Color.HSVToColor(floatArrayOf(hue2, 0.5f, 0.6f))
        return Pair(color1, color2)
    }
}
