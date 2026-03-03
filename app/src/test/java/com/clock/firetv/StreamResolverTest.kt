package com.clock.firetv

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StreamResolverTest {

    private val resolver = StreamResolver()

    @Test
    fun `parseResolution with 720p returns 720`() {
        assertThat(resolver.parseResolution("720p")).isEqualTo(720)
    }

    @Test
    fun `parseResolution with 1080p returns 1080`() {
        assertThat(resolver.parseResolution("1080p")).isEqualTo(1080)
    }

    @Test
    fun `parseResolution with 360p returns 360`() {
        assertThat(resolver.parseResolution("360p")).isEqualTo(360)
    }

    @Test
    fun `parseResolution with null returns 0`() {
        assertThat(resolver.parseResolution(null)).isEqualTo(0)
    }

    @Test
    fun `parseResolution with empty string returns 0`() {
        assertThat(resolver.parseResolution("")).isEqualTo(0)
    }

    @Test
    fun `parseResolution with just p returns 0`() {
        assertThat(resolver.parseResolution("p")).isEqualTo(0)
    }

    @Test
    fun `parseResolution with invalid string returns 0`() {
        assertThat(resolver.parseResolution("invalid")).isEqualTo(0)
    }
}
