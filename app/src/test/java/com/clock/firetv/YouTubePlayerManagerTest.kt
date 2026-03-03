package com.clock.firetv

import android.content.Context
import android.view.ViewGroup
import androidx.media3.ui.PlayerView
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class YouTubePlayerManagerTest {

    private lateinit var manager: YouTubePlayerManager

    @Before
    fun setUp() {
        val context = mock(Context::class.java)
        val playerView = mock(PlayerView::class.java)
        val container = mock(ViewGroup::class.java)
        val scope: CoroutineScope = TestScope()
        manager = YouTubePlayerManager(context, playerView, container, scope)
    }

    @Test
    fun `parseInput with full YouTube URL extracts video ID`() {
        val result = manager.parseInput("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        assertThat(result).isInstanceOf(YouTubePlayerManager.YouTubeContent.Video::class.java)
        val video = result as YouTubePlayerManager.YouTubeContent.Video
        assertThat(video.videoId).isEqualTo("dQw4w9WgXcQ")
        assertThat(video.playlistId).isNull()
    }

    @Test
    fun `parseInput with short URL extracts video ID`() {
        val result = manager.parseInput("https://youtu.be/dQw4w9WgXcQ")
        assertThat(result).isInstanceOf(YouTubePlayerManager.YouTubeContent.Video::class.java)
        val video = result as YouTubePlayerManager.YouTubeContent.Video
        assertThat(video.videoId).isEqualTo("dQw4w9WgXcQ")
        assertThat(video.playlistId).isNull()
    }

    @Test
    fun `parseInput with bare 11-char video ID`() {
        val result = manager.parseInput("dQw4w9WgXcQ")
        assertThat(result).isInstanceOf(YouTubePlayerManager.YouTubeContent.Video::class.java)
        val video = result as YouTubePlayerManager.YouTubeContent.Video
        assertThat(video.videoId).isEqualTo("dQw4w9WgXcQ")
    }

    @Test
    fun `parseInput with playlist URL extracts playlist ID`() {
        val result = manager.parseInput("https://www.youtube.com/playlist?list=PLrAXtmErZgOeiKm4sgNOknGvNjby9efdf")
        assertThat(result).isInstanceOf(YouTubePlayerManager.YouTubeContent.Playlist::class.java)
        val playlist = result as YouTubePlayerManager.YouTubeContent.Playlist
        assertThat(playlist.playlistId).isEqualTo("PLrAXtmErZgOeiKm4sgNOknGvNjby9efdf")
    }

    @Test
    fun `parseInput with video+playlist URL extracts both`() {
        val result = manager.parseInput("https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=PLrAXtmErZgOeiKm4sgNOknGvNjby9efdf")
        assertThat(result).isInstanceOf(YouTubePlayerManager.YouTubeContent.Video::class.java)
        val video = result as YouTubePlayerManager.YouTubeContent.Video
        assertThat(video.videoId).isEqualTo("dQw4w9WgXcQ")
        assertThat(video.playlistId).isEqualTo("PLrAXtmErZgOeiKm4sgNOknGvNjby9efdf")
    }

    @Test
    fun `parseInput with bare playlist ID starting with PL`() {
        val result = manager.parseInput("PLrAXtmErZgOeiKm4sgNOknGvNjby9efdf")
        assertThat(result).isInstanceOf(YouTubePlayerManager.YouTubeContent.Playlist::class.java)
        val playlist = result as YouTubePlayerManager.YouTubeContent.Playlist
        assertThat(playlist.playlistId).isEqualTo("PLrAXtmErZgOeiKm4sgNOknGvNjby9efdf")
    }

    @Test
    fun `parseInput with invalid input returns null`() {
        assertThat(manager.parseInput("not-a-url")).isNull()
        assertThat(manager.parseInput("")).isNull()
        assertThat(manager.parseInput("http://example.com")).isNull()
    }

    @Test
    fun `parseInput with whitespace is trimmed`() {
        val result = manager.parseInput("  dQw4w9WgXcQ  ")
        assertThat(result).isInstanceOf(YouTubePlayerManager.YouTubeContent.Video::class.java)
        val video = result as YouTubePlayerManager.YouTubeContent.Video
        assertThat(video.videoId).isEqualTo("dQw4w9WgXcQ")
    }

    @Test
    fun `parseInput with playlist-first URL and video param`() {
        val result = manager.parseInput("https://www.youtube.com/watch?list=PLrAXtmErZgOeiKm4sgNOknGvNjby9efdf&v=dQw4w9WgXcQ")
        assertThat(result).isInstanceOf(YouTubePlayerManager.YouTubeContent.Video::class.java)
        val video = result as YouTubePlayerManager.YouTubeContent.Video
        assertThat(video.videoId).isEqualTo("dQw4w9WgXcQ")
        assertThat(video.playlistId).isEqualTo("PLrAXtmErZgOeiKm4sgNOknGvNjby9efdf")
    }

    @Test
    fun `parseInput with too-short ID returns null`() {
        assertThat(manager.parseInput("abc")).isNull()
    }
}
