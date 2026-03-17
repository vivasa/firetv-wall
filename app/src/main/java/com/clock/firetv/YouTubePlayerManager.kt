package com.clock.firetv

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

class YouTubePlayerManager(
    private val context: Context,
    private val playerView: PlayerView,
    private val container: ViewGroup,
    private val scope: CoroutineScope
) {

    companion object {
        private const val TAG = "YouTubePlayerMgr"
        private val PLAYLIST_URL_PATTERN = Pattern.compile("[?&]list=([A-Za-z0-9_-]+)")
        private val VIDEO_URL_PATTERN = Pattern.compile("[?&]v=([A-Za-z0-9_-]{11})")
        private val SHORT_URL_PATTERN = Pattern.compile("youtu\\.be/([A-Za-z0-9_-]{11})")
        private val BARE_PLAYLIST_PATTERN = Pattern.compile("^PL[A-Za-z0-9_-]+$")
        private val BARE_VIDEO_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{11}$")
    }

    interface OnTrackChangeListener {
        fun onTrackChanged(videoTitle: String?, playlistTitle: String?)
    }

    interface OnPlaylistLoadedListener {
        fun onPlaylistLoaded()
    }

    var trackChangeListener: OnTrackChangeListener? = null
    var playlistLoadedListener: OnPlaylistLoadedListener? = null

    private var player: ExoPlayer? = null
    private val streamResolver = StreamResolver()

    // Playlist state
    private var playlistVideoUrls: List<String> = emptyList()
    private var playlistVideoTitles: List<String?> = emptyList()
    private var currentPlaylistTitle: String? = null
    private var currentVideoTitle: String? = null
    private var currentIndex = 0
    private var currentVideoUrl: String? = null
    private var currentLoadJob: Job? = null
    private var currentInputUrl: String? = null

    // Resume state for surviving Activity.recreate()
    private var pendingResumeIndex: Int = -1
    private var pendingResumePositionMs: Long = -1L

    @OptIn(UnstableApi::class)
    fun initialize() {
        player = ExoPlayer.Builder(context).build().also { exo ->
            playerView.player = exo
            playerView.useController = false
            playerView.isFocusable = false
            playerView.isFocusableInTouchMode = false
            playerView.setBackgroundColor(0xFF000000.toInt())

            exo.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        playNext()
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    handlePlaybackError(error)
                }
            })
        }
    }

    fun loadVideo(urlOrId: String) {
        if (urlOrId.isBlank()) {
            stop()
            return
        }

        // Skip reload if already playing the same content
        if (urlOrId == currentInputUrl) {
            return
        }
        currentInputUrl = urlOrId

        val parsed = parseInput(urlOrId)
        if (parsed == null) {
            Log.w(TAG, "Unrecognized input: $urlOrId")
            return
        }

        currentLoadJob?.cancel()
        currentLoadJob = scope.launch {
            when (parsed) {
                is YouTubeContent.Playlist -> loadPlaylist(parsed.playlistId)
                is YouTubeContent.Video -> {
                    if (parsed.playlistId != null) {
                        loadPlaylistStartingAtVideo(parsed.playlistId, parsed.videoId)
                    } else {
                        loadSingleVideo(parsed.videoId)
                    }
                }
            }
        }
    }

    private suspend fun loadSingleVideo(videoId: String) {
        playlistVideoUrls = emptyList()
        playlistVideoTitles = emptyList()
        currentPlaylistTitle = null
        currentIndex = 0
        val videoUrl = "https://www.youtube.com/watch?v=$videoId"
        currentVideoUrl = videoUrl
        resolveAndPlay(videoUrl)
    }

    private suspend fun loadPlaylist(playlistId: String) {
        val playlistUrl = "https://www.youtube.com/playlist?list=$playlistId"
        val result = streamResolver.extractPlaylistItems(playlistUrl)
        if (result.items.isEmpty()) {
            Log.w(TAG, "Playlist is empty or failed to extract: $playlistId")
            return
        }
        currentPlaylistTitle = result.title
        playlistVideoUrls = result.items.map { it.url }
        playlistVideoTitles = result.items.map { it.title }
        currentIndex = if (pendingResumeIndex in playlistVideoUrls.indices) {
            pendingResumeIndex
        } else {
            0
        }
        pendingResumeIndex = -1
        currentVideoUrl = playlistVideoUrls[currentIndex]
        withContext(Dispatchers.Main) { playlistLoadedListener?.onPlaylistLoaded() }
        resolveAndPlay(playlistVideoUrls[currentIndex])
    }

    private suspend fun loadPlaylistStartingAtVideo(playlistId: String, videoId: String) {
        val playlistUrl = "https://www.youtube.com/playlist?list=$playlistId"
        val result = streamResolver.extractPlaylistItems(playlistUrl)
        if (result.items.isEmpty()) {
            // Fallback to single video
            loadSingleVideo(videoId)
            return
        }
        currentPlaylistTitle = result.title
        playlistVideoUrls = result.items.map { it.url }
        playlistVideoTitles = result.items.map { it.title }
        currentIndex = if (pendingResumeIndex in playlistVideoUrls.indices) {
            pendingResumeIndex
        } else {
            val idx = playlistVideoUrls.indexOfFirst { it.contains(videoId) }
            if (idx >= 0) idx else 0
        }
        pendingResumeIndex = -1
        currentVideoUrl = playlistVideoUrls[currentIndex]
        withContext(Dispatchers.Main) { playlistLoadedListener?.onPlaylistLoaded() }
        resolveAndPlay(playlistVideoUrls[currentIndex])
    }

    private suspend fun resolveAndPlay(videoUrl: String) {
        val result = streamResolver.resolveStreamUrl(videoUrl)
        if (result == null) {
            Log.w(TAG, "Failed to resolve stream for $videoUrl")
            // Skip to next if in playlist
            if (playlistVideoUrls.isNotEmpty()) {
                playNext()
            }
            return
        }
        // Use title from stream resolution; fall back to stored playlist title for current index
        val videoTitle = result.title
            ?: playlistVideoTitles.getOrNull(currentIndex)
        currentVideoTitle = videoTitle
        withContext(Dispatchers.Main) {
            player?.let { exo ->
                exo.setMediaItem(MediaItem.fromUri(result.url))
                exo.prepare()
                exo.play()
                if (pendingResumePositionMs > 0) {
                    exo.seekTo(pendingResumePositionMs)
                    pendingResumePositionMs = -1L
                }
            }
            trackChangeListener?.onTrackChanged(currentVideoTitle, currentPlaylistTitle)
        }
    }

    fun playNext() {
        if (playlistVideoUrls.isEmpty()) return

        currentIndex = (currentIndex + 1) % playlistVideoUrls.size
        currentVideoUrl = playlistVideoUrls[currentIndex]

        currentLoadJob?.cancel()
        currentLoadJob = scope.launch {
            resolveAndPlay(playlistVideoUrls[currentIndex])
        }
    }

    private fun handlePlaybackError(error: PlaybackException) {
        Log.e(TAG, "Playback error: ${error.errorCode}", error)

        val isHttpError = error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS
        val videoUrl = currentVideoUrl

        if (isHttpError && videoUrl != null) {
            // Likely expired stream URL (HTTP 403) — re-resolve
            currentLoadJob?.cancel()
            currentLoadJob = scope.launch {
                val freshResult = streamResolver.resolveStreamUrl(videoUrl)
                if (freshResult != null) {
                    withContext(Dispatchers.Main) {
                        player?.let { exo ->
                            exo.setMediaItem(MediaItem.fromUri(freshResult.url))
                            exo.prepare()
                            exo.play()
                        }
                    }
                } else {
                    // Re-resolution failed — skip to next or stop
                    withContext(Dispatchers.Main) { skipOrStop() }
                }
            }
        } else {
            // Non-HTTP error — skip to next or stop
            skipOrStop()
        }
    }

    private fun skipOrStop() {
        if (playlistVideoUrls.isNotEmpty()) {
            playNext()
        } else {
            stop()
        }
    }

    fun togglePlayPause() {
        player?.let { exo ->
            if (exo.isPlaying) exo.pause() else exo.play()
        }
    }

    fun pause() {
        player?.pause()
    }

    fun resume() {
        player?.play()
    }

    fun currentTrackInfo(): Pair<String, String?>? {
        return currentVideoTitle?.let { Pair(it, currentPlaylistTitle) }
    }

    fun playPrevious() {
        if (playlistVideoUrls.isEmpty()) return

        currentIndex = if (currentIndex > 0) {
            currentIndex - 1
        } else {
            playlistVideoUrls.size - 1
        }
        currentVideoUrl = playlistVideoUrls[currentIndex]

        currentLoadJob?.cancel()
        currentLoadJob = scope.launch {
            resolveAndPlay(playlistVideoUrls[currentIndex])
        }
    }

    fun seekForward(ms: Long = 10_000) {
        player?.let { exo ->
            val newPos = exo.currentPosition + ms
            if (newPos < exo.duration) {
                exo.seekTo(newPos)
            }
        }
    }

    fun seekBackward(ms: Long = 10_000) {
        player?.let { exo ->
            val newPos = (exo.currentPosition - ms).coerceAtLeast(0)
            exo.seekTo(newPos)
        }
    }

    fun playTrackAtIndex(index: Int) {
        if (index < 0 || index >= playlistVideoUrls.size) return
        currentIndex = index
        currentVideoUrl = playlistVideoUrls[index]
        currentLoadJob?.cancel()
        currentLoadJob = scope.launch {
            resolveAndPlay(playlistVideoUrls[index])
        }
    }

    fun getTrackList(): List<Pair<Int, String?>> {
        return playlistVideoTitles.mapIndexed { index, title -> Pair(index, title) }
    }

    fun getCurrentIndex(): Int = currentIndex

    fun getPlaylistTitle(): String? = currentPlaylistTitle

    fun hasPlaylist(): Boolean = playlistVideoUrls.isNotEmpty()

    fun savePlaybackState(prefs: SharedPreferences) {
        prefs.edit()
            .putInt("yt_resume_index", currentIndex)
            .putLong("yt_resume_pos", player?.currentPosition ?: 0L)
            .apply()
    }

    fun loadPlaybackState(prefs: SharedPreferences) {
        pendingResumeIndex = prefs.getInt("yt_resume_index", -1)
        pendingResumePositionMs = prefs.getLong("yt_resume_pos", -1L)
        prefs.edit().remove("yt_resume_index").remove("yt_resume_pos").apply()
    }

    fun stop() {
        currentLoadJob?.cancel()
        player?.stop()
        player?.clearMediaItems()
        playlistVideoUrls = emptyList()
        playlistVideoTitles = emptyList()
        currentPlaylistTitle = null
        currentVideoTitle = null
        currentIndex = 0
        currentVideoUrl = null
        currentInputUrl = null
        trackChangeListener?.onTrackChanged(null, null)
    }

    fun destroy() {
        currentLoadJob?.cancel()
        player?.release()
        player = null
    }

    fun updateSize(widthDp: Int, heightDp: Int) {
        val density = container.resources.displayMetrics.density
        val params = container.layoutParams
        params.width = (widthDp * density).toInt()
        params.height = (heightDp * density).toInt()
        container.layoutParams = params
    }

    internal fun parseInput(input: String): YouTubeContent? {
        val trimmed = input.trim()

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

        if (videoMatcher.find()) {
            return YouTubeContent.Video(videoMatcher.group(1) ?: return null, null)
        }

        val shortMatcher = SHORT_URL_PATTERN.matcher(trimmed)
        if (shortMatcher.find()) {
            return YouTubeContent.Video(shortMatcher.group(1) ?: return null, null)
        }

        if (BARE_PLAYLIST_PATTERN.matcher(trimmed).matches()) {
            return YouTubeContent.Playlist(trimmed)
        }

        if (BARE_VIDEO_PATTERN.matcher(trimmed).matches()) {
            return YouTubeContent.Video(trimmed, null)
        }

        return null
    }

    internal sealed class YouTubeContent {
        data class Playlist(val playlistId: String) : YouTubeContent()
        data class Video(val videoId: String, val playlistId: String?) : YouTubeContent()
    }
}
