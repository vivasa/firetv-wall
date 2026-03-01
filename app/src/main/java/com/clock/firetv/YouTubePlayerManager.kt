package com.clock.firetv

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import java.util.regex.Pattern

class YouTubePlayerManager(
    private val webView: WebView,
    private val container: ViewGroup
) {

    companion object {
        private val PLAYLIST_URL_PATTERN = Pattern.compile("[?&]list=([A-Za-z0-9_-]+)")
        private val VIDEO_URL_PATTERN = Pattern.compile("[?&]v=([A-Za-z0-9_-]{11})")
        private val SHORT_URL_PATTERN = Pattern.compile("youtu\\.be/([A-Za-z0-9_-]{11})")
        private val BARE_PLAYLIST_PATTERN = Pattern.compile("^PL[A-Za-z0-9_-]+$")
        private val BARE_VIDEO_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{11}$")
    }

    private var currentUrl: String? = null

    @SuppressLint("SetJavaScriptEnabled")
    fun initialize() {
        webView.settings.apply {
            javaScriptEnabled = true
            mediaPlaybackRequiresUserGesture = false
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            // User agent to avoid mobile redirects
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                // Keep navigation within the WebView
                return false
            }
        }

        webView.webChromeClient = WebChromeClient()

        // Set background to black
        webView.setBackgroundColor(0xFF000000.toInt())
    }

    fun loadVideo(urlOrId: String) {
        if (urlOrId.isBlank()) {
            stop()
            return
        }

        val parsed = parseInput(urlOrId) ?: return
        currentUrl = urlOrId

        val embedUrl = when (parsed) {
            is YouTubeContent.Playlist -> buildPlaylistEmbed(parsed.playlistId)
            is YouTubeContent.Video -> buildVideoEmbed(parsed.videoId, parsed.playlistId)
        }

        val html = buildEmbedHtml(embedUrl)
        // Load with a base URL to set proper referrer, avoiding Error 153
        webView.loadDataWithBaseURL(
            "https://www.youtube.com",
            html,
            "text/html",
            "UTF-8",
            null
        )
    }

    fun updateSize(widthDp: Int, heightDp: Int) {
        val density = container.resources.displayMetrics.density
        val params = container.layoutParams
        params.width = (widthDp * density).toInt()
        params.height = (heightDp * density).toInt()
        container.layoutParams = params
    }

    fun stop() {
        webView.loadUrl("about:blank")
        currentUrl = null
    }

    fun destroy() {
        webView.stopLoading()
        webView.loadUrl("about:blank")
        webView.destroy()
    }

    fun reload() {
        currentUrl?.let { loadVideo(it) }
    }

    private fun parseInput(input: String): YouTubeContent? {
        val trimmed = input.trim()

        // Check for playlist URL
        val playlistMatcher = PLAYLIST_URL_PATTERN.matcher(trimmed)
        val videoMatcher = VIDEO_URL_PATTERN.matcher(trimmed)

        if (playlistMatcher.find()) {
            val playlistId = playlistMatcher.group(1) ?: return null
            val videoId = if (videoMatcher.find()) videoMatcher.group(1) else null
            return if (videoId != null) {
                YouTubeContent.Video(videoId, playlistId)
            } else {
                YouTubeContent.Playlist(playlistId)
            }
        }

        // Check for video URL with v= param
        if (videoMatcher.find()) {
            return YouTubeContent.Video(videoMatcher.group(1) ?: return null, null)
        }

        // Check for short URL (youtu.be)
        val shortMatcher = SHORT_URL_PATTERN.matcher(trimmed)
        if (shortMatcher.find()) {
            return YouTubeContent.Video(shortMatcher.group(1) ?: return null, null)
        }

        // Check for bare playlist ID
        if (BARE_PLAYLIST_PATTERN.matcher(trimmed).matches()) {
            return YouTubeContent.Playlist(trimmed)
        }

        // Check for bare video ID (11 characters)
        if (BARE_VIDEO_PATTERN.matcher(trimmed).matches()) {
            return YouTubeContent.Video(trimmed, null)
        }

        return null
    }

    private fun buildPlaylistEmbed(playlistId: String): String {
        return "https://www.youtube.com/embed/videoseries?list=$playlistId&autoplay=1&loop=1&mute=0&controls=1&rel=0&modestbranding=1&enablejsapi=1"
    }

    private fun buildVideoEmbed(videoId: String, playlistId: String?): String {
        val base = "https://www.youtube.com/embed/$videoId?autoplay=1&controls=1&rel=0&modestbranding=1&enablejsapi=1"
        return if (playlistId != null) {
            "$base&list=$playlistId&loop=1"
        } else {
            "$base&loop=1&playlist=$videoId"
        }
    }

    private fun buildEmbedHtml(embedUrl: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
                <meta name="referrer" content="always">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    html, body { width: 100%; height: 100%; overflow: hidden; background: #000; }
                    iframe {
                        width: 100%;
                        height: 100%;
                        border: none;
                    }
                </style>
            </head>
            <body>
                <iframe
                    src="$embedUrl"
                    allow="autoplay; encrypted-media; picture-in-picture"
                    allowfullscreen
                    referrerpolicy="origin">
                </iframe>
            </body>
            </html>
        """.trimIndent()
    }

    private sealed class YouTubeContent {
        data class Playlist(val playlistId: String) : YouTubeContent()
        data class Video(val videoId: String, val playlistId: String?) : YouTubeContent()
    }
}
