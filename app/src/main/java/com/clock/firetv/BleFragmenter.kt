package com.clock.firetv

import java.io.ByteArrayOutputStream

object BleFragmenter {

    /**
     * Fragment data into MTU-sized chunks with 2-byte header.
     * Header: byte 0 = 0x01 (more) or 0x00 (final), byte 1 = sequence number.
     */
    fun fragment(data: ByteArray, mtu: Int): List<ByteArray> {
        val chunkSize = mtu - BleConstants.ATT_OVERHEAD
        if (chunkSize <= 0) throw IllegalArgumentException("MTU too small: $mtu")

        val fragments = mutableListOf<ByteArray>()
        var offset = 0
        var seq = 0

        while (offset < data.size) {
            val end = minOf(offset + chunkSize, data.size)
            val isLast = end == data.size
            val payload = data.copyOfRange(offset, end)

            val fragment = ByteArray(payload.size + 2)
            fragment[0] = if (isLast) 0x00 else 0x01
            fragment[1] = (seq and 0xFF).toByte()
            System.arraycopy(payload, 0, fragment, 2, payload.size)

            fragments.add(fragment)
            offset = end
            seq++
        }

        // Handle empty data
        if (fragments.isEmpty()) {
            fragments.add(byteArrayOf(0x00, 0x00))
        }

        return fragments
    }

    class Reassembler {
        private val buffer = ByteArrayOutputStream()

        /**
         * Add a fragment. Returns the complete message string when the final
         * fragment arrives, or null if more fragments are expected.
         */
        fun addFragment(data: ByteArray): String? {
            if (data.size < 2) return null

            val moreFragments = data[0] == 0x01.toByte()
            // seq = data[1], used for ordering verification (not enforced here)
            val payload = data.copyOfRange(2, data.size)
            buffer.write(payload)

            return if (!moreFragments) {
                val result = buffer.toString(Charsets.UTF_8.name())
                buffer.reset()
                result
            } else {
                null
            }
        }

        fun reset() {
            buffer.reset()
        }
    }
}
