package com.mantle.app

import com.google.common.truth.Truth.assertThat
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ArtworkServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: ArtworkService

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `isYouTubeUrl detects standard youtube URLs`() {
        assertThat(service().isYouTubeUrl("https://www.youtube.com/watch?v=abc")).isTrue()
        assertThat(service().isYouTubeUrl("https://youtube.com/playlist?list=PLxyz")).isTrue()
        assertThat(service().isYouTubeUrl("https://youtu.be/abc")).isTrue()
        assertThat(service().isYouTubeUrl("http://www.youtube.com/watch?v=abc")).isTrue()
    }

    @Test
    fun `isYouTubeUrl rejects non-youtube URLs`() {
        assertThat(service().isYouTubeUrl("https://vimeo.com/12345")).isFalse()
        assertThat(service().isYouTubeUrl("https://example.com")).isFalse()
        assertThat(service().isYouTubeUrl("not a url")).isFalse()
    }

    @Test
    fun `fetchArtworkUrl returns null for non-youtube URLs`() = runTest {
        val result = service().fetchArtworkUrl("https://example.com/video")
        assertThat(result).isNull()
    }

    @Test
    fun `fallbackGradientColors produces deterministic colors`() {
        val service = service()
        val colors1 = service.fallbackGradientColors("Jazz Standards")
        val colors2 = service.fallbackGradientColors("Jazz Standards")
        assertThat(colors1).isEqualTo(colors2)
    }

    @Test
    fun `fallbackGradientColors produces different colors for different names`() {
        val service = service()
        val colors1 = service.fallbackGradientColors("Jazz Standards")
        val colors2 = service.fallbackGradientColors("Lo-Fi Beats")
        assertThat(colors1).isNotEqualTo(colors2)
    }

    private fun service(): ArtworkService {
        return ArtworkService(OkHttpClient())
    }
}
