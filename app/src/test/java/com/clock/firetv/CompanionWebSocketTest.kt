package com.clock.firetv

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CompanionWebSocketTest {

    @Test
    fun `generatePin returns 4-digit string`() {
        val pin = CompanionWebSocket.generatePin()
        assertThat(pin).hasLength(4)
        assertThat(pin.toIntOrNull()).isNotNull()
    }

    @Test
    fun `generatePin is in range 1000-9999`() {
        repeat(100) {
            val pin = CompanionWebSocket.generatePin().toInt()
            assertThat(pin).isAtLeast(1000)
            assertThat(pin).isAtMost(9999)
        }
    }

    @Test
    fun `generateToken returns 32 character hex string`() {
        val token = CompanionWebSocket.generateToken()
        assertThat(token).hasLength(32)
        assertThat(token).matches("[0-9a-f]{32}")
    }

    @Test
    fun `generateToken produces unique values`() {
        val tokens = (1..10).map { CompanionWebSocket.generateToken() }.toSet()
        // All 10 should be unique (astronomically unlikely to collide)
        assertThat(tokens).hasSize(10)
    }

    @Test
    fun `generatePin produces varying values`() {
        val pins = (1..20).map { CompanionWebSocket.generatePin() }.toSet()
        // Should have at least 2 unique values out of 20
        assertThat(pins.size).isGreaterThan(1)
    }
}
