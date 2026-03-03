package com.clock.firetv

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ChimeManagerTest {

    @Test
    fun `calculateMsUntilNextHalfHour at exact half-hour boundary returns 30 minutes`() {
        // Exactly at a half-hour boundary (within 1s threshold)
        val halfHourMs = 30 * 60 * 1000L
        val result = ChimeManager.calculateMsUntilNextHalfHour(halfHourMs)
        // At exact boundary: msUntil = halfHourMs - 0 = halfHourMs, which is >= 1000, so returns halfHourMs
        assertThat(result).isEqualTo(halfHourMs)
    }

    @Test
    fun `calculateMsUntilNextHalfHour at 0ms returns 30 minutes`() {
        val halfHourMs = 30 * 60 * 1000L
        val result = ChimeManager.calculateMsUntilNextHalfHour(0L)
        // nowMs % halfHourMs = 0, msUntil = halfHourMs, >= 1000 → halfHourMs
        assertThat(result).isEqualTo(halfHourMs)
    }

    @Test
    fun `calculateMsUntilNextHalfHour at mid-interval returns remaining time`() {
        val halfHourMs = 30 * 60 * 1000L
        val midpoint = halfHourMs / 2 // 15 minutes in
        val result = ChimeManager.calculateMsUntilNextHalfHour(midpoint)
        assertThat(result).isEqualTo(halfHourMs / 2) // 15 minutes remaining
    }

    @Test
    fun `calculateMsUntilNextHalfHour near boundary within 1s returns full 30 minutes`() {
        val halfHourMs = 30 * 60 * 1000L
        // 500ms before next half hour → msUntil = 500 < 1000 → returns halfHourMs
        val nearBoundary = halfHourMs - 500
        val result = ChimeManager.calculateMsUntilNextHalfHour(nearBoundary)
        assertThat(result).isEqualTo(halfHourMs)
    }

    @Test
    fun `calculateMsUntilNextHalfHour 1 second before boundary returns 1 second`() {
        val halfHourMs = 30 * 60 * 1000L
        // Exactly 1000ms before → msUntil = 1000 >= 1000 → returns 1000
        val oneSecBefore = halfHourMs - 1000
        val result = ChimeManager.calculateMsUntilNextHalfHour(oneSecBefore)
        assertThat(result).isEqualTo(1000L)
    }

    @Test
    fun `calculateMsUntilNextHalfHour with large timestamp works correctly`() {
        val halfHourMs = 30 * 60 * 1000L
        // Simulate a real timestamp: 10 minutes past some half hour
        val tenMinutesMs = 10 * 60 * 1000L
        val timestamp = halfHourMs * 100 + tenMinutesMs // Some time 10 min past a half hour
        val result = ChimeManager.calculateMsUntilNextHalfHour(timestamp)
        assertThat(result).isEqualTo(20 * 60 * 1000L) // 20 minutes remaining
    }
}
