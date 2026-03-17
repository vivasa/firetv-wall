package com.firetv.protocol

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BleFragmenterTest {

    // -- fragment(): single fragment --

    @Test
    fun `small message fits in single fragment`() {
        val data = "hello".toByteArray(Charsets.UTF_8)
        val fragments = BleFragmenter.fragment(data, BleConstants.DEFAULT_MTU)

        assertThat(fragments).hasSize(1)
        assertThat(fragments[0][0]).isEqualTo(0x00) // final
        assertThat(fragments[0][1]).isEqualTo(0x00) // seq 0
        assertThat(fragments[0].copyOfRange(2, fragments[0].size)).isEqualTo(data)
    }

    // -- fragment(): multiple fragments --

    @Test
    fun `large message splits with correct headers`() {
        // MTU 23, ATT_OVERHEAD 5 → chunk size 18. 50 bytes → ceil(50/18) = 3 fragments
        val data = ByteArray(50) { it.toByte() }
        val fragments = BleFragmenter.fragment(data, BleConstants.DEFAULT_MTU)

        assertThat(fragments.size).isGreaterThan(1)
        // All except last should have 0x01 (more)
        for (i in 0 until fragments.size - 1) {
            assertThat(fragments[i][0]).isEqualTo(0x01)
        }
        // Last should have 0x00 (final)
        assertThat(fragments.last()[0]).isEqualTo(0x00)
    }

    // -- fragment(): empty data --

    @Test
    fun `empty data produces single fragment with empty header`() {
        val fragments = BleFragmenter.fragment(ByteArray(0), BleConstants.DEFAULT_MTU)

        assertThat(fragments).hasSize(1)
        assertThat(fragments[0]).isEqualTo(byteArrayOf(0x00, 0x00))
    }

    // -- fragment(): exact boundary --

    @Test
    fun `data exactly fills one chunk produces single fragment`() {
        val chunkSize = BleConstants.DEFAULT_MTU - BleConstants.ATT_OVERHEAD // 18
        val data = ByteArray(chunkSize) { 0x42 }
        val fragments = BleFragmenter.fragment(data, BleConstants.DEFAULT_MTU)

        assertThat(fragments).hasSize(1)
        assertThat(fragments[0][0]).isEqualTo(0x00) // final
        assertThat(fragments[0].size).isEqualTo(chunkSize + 2) // payload + 2-byte header
    }

    // -- fragment(): one byte over --

    @Test
    fun `data one byte over chunk size produces two fragments`() {
        val chunkSize = BleConstants.DEFAULT_MTU - BleConstants.ATT_OVERHEAD
        val data = ByteArray(chunkSize + 1) { 0x42 }
        val fragments = BleFragmenter.fragment(data, BleConstants.DEFAULT_MTU)

        assertThat(fragments).hasSize(2)
        assertThat(fragments[0][0]).isEqualTo(0x01) // more
        assertThat(fragments[1][0]).isEqualTo(0x00) // final
        // Second fragment should have exactly 1 payload byte + 2 header
        assertThat(fragments[1].size).isEqualTo(3)
    }

    // -- fragment(): invalid MTU --

    @Test(expected = IllegalArgumentException::class)
    fun `invalid MTU throws IllegalArgumentException`() {
        BleFragmenter.fragment("test".toByteArray(), 5)
    }

    // -- fragment(): sequence numbers --

    @Test
    fun `sequence numbers increment across fragments`() {
        val data = ByteArray(100) { it.toByte() }
        val fragments = BleFragmenter.fragment(data, BleConstants.DEFAULT_MTU)

        for (i in fragments.indices) {
            assertThat(fragments[i][1]).isEqualTo((i and 0xFF).toByte())
        }
    }

    // -- fragment(): sequence wrap --

    @Test
    fun `sequence number wraps from 255 to 0`() {
        // Need > 256 fragments. MTU 8 → chunk size 3. Data = 3 * 257 = 771 bytes → 257 fragments
        val chunkSize = 8 - BleConstants.ATT_OVERHEAD // 3
        val data = ByteArray(chunkSize * 257) { 0x41 }
        val fragments = BleFragmenter.fragment(data, 8)

        assertThat(fragments.size).isEqualTo(257)
        // Fragment 255 should have seq 255
        assertThat(fragments[255][1]).isEqualTo(0xFF.toByte())
        // Fragment 256 should have seq 0 (wrapped)
        assertThat(fragments[256][1]).isEqualTo(0x00.toByte())
    }

    // -- Round-trip: default MTU --

    @Test
    fun `fragment and reassemble round-trip with default MTU`() {
        val original = """{"cmd":"sync_config","config":{"theme":"dark","chimeEnabled":true,"wallpaperInterval":30}}"""
        val fragments = BleFragmenter.fragment(original.toByteArray(Charsets.UTF_8), BleConstants.DEFAULT_MTU)
        assertThat(fragments.size).isGreaterThan(1)

        val reassembler = BleFragmenter.Reassembler()
        var result: String? = null
        for (fragment in fragments) {
            result = reassembler.addFragment(fragment)
        }

        assertThat(result).isEqualTo(original)
    }

    // -- Round-trip: large MTU --

    @Test
    fun `fragment and reassemble round-trip with large MTU`() {
        val original = """{"cmd":"sync_config","config":{"theme":"dark","chimeEnabled":true,"wallpaperInterval":30}}"""
        val fragments = BleFragmenter.fragment(original.toByteArray(Charsets.UTF_8), BleConstants.TARGET_MTU)
        assertThat(fragments).hasSize(1) // fits in one fragment at 512 MTU

        val reassembler = BleFragmenter.Reassembler()
        val result = reassembler.addFragment(fragments[0])

        assertThat(result).isEqualTo(original)
    }

    // -- Reassembler: null for intermediate, string for final --

    @Test
    fun `reassembler returns null for intermediate and string for final`() {
        val data = ByteArray(50) { it.toByte() }
        val fragments = BleFragmenter.fragment(data, BleConstants.DEFAULT_MTU)
        assertThat(fragments.size).isGreaterThan(1)

        val reassembler = BleFragmenter.Reassembler()
        for (i in 0 until fragments.size - 1) {
            assertThat(reassembler.addFragment(fragments[i])).isNull()
        }
        assertThat(reassembler.addFragment(fragments.last())).isNotNull()
    }

    // -- Reassembler: reset --

    @Test
    fun `reassembler reset clears partial message`() {
        val data = ByteArray(50) { 0x42 }
        val fragments = BleFragmenter.fragment(data, BleConstants.DEFAULT_MTU)

        val reassembler = BleFragmenter.Reassembler()
        // Add first fragment (intermediate)
        reassembler.addFragment(fragments[0])
        // Reset mid-stream
        reassembler.reset()

        // Now reassemble a fresh message
        val fresh = "fresh".toByteArray(Charsets.UTF_8)
        val freshFragments = BleFragmenter.fragment(fresh, BleConstants.DEFAULT_MTU)
        val result = reassembler.addFragment(freshFragments[0])

        assertThat(result).isEqualTo("fresh")
    }

    // -- Reassembler: sequential messages --

    @Test
    fun `sequential complete messages reassemble independently`() {
        val reassembler = BleFragmenter.Reassembler()

        val msg1 = "first message"
        val fragments1 = BleFragmenter.fragment(msg1.toByteArray(Charsets.UTF_8), BleConstants.DEFAULT_MTU)
        var result1: String? = null
        for (f in fragments1) { result1 = reassembler.addFragment(f) }
        assertThat(result1).isEqualTo(msg1)

        val msg2 = "second message"
        val fragments2 = BleFragmenter.fragment(msg2.toByteArray(Charsets.UTF_8), BleConstants.DEFAULT_MTU)
        var result2: String? = null
        for (f in fragments2) { result2 = reassembler.addFragment(f) }
        assertThat(result2).isEqualTo(msg2)
    }

    // -- Reassembler: short fragment --

    @Test
    fun `fragment shorter than 2 bytes returns null without corrupting state`() {
        val reassembler = BleFragmenter.Reassembler()

        // Short fragment should be ignored
        assertThat(reassembler.addFragment(byteArrayOf(0x00))).isNull()
        assertThat(reassembler.addFragment(ByteArray(0))).isNull()

        // Normal message should still work after
        val msg = "ok"
        val fragments = BleFragmenter.fragment(msg.toByteArray(Charsets.UTF_8), BleConstants.DEFAULT_MTU)
        val result = reassembler.addFragment(fragments[0])
        assertThat(result).isEqualTo(msg)
    }
}
