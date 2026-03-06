package com.firetv.protocol

import java.util.UUID

object BleConstants {
    val SERVICE_UUID: UUID = UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb")
    val COMMAND_CHAR_UUID: UUID = UUID.fromString("0000ff02-0000-1000-8000-00805f9b34fb")
    val EVENT_CHAR_UUID: UUID = UUID.fromString("0000ff03-0000-1000-8000-00805f9b34fb")
    val CCC_DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    const val MANUFACTURER_ID = 0xFFFF // development/testing
    const val DEFAULT_MTU = 23
    const val TARGET_MTU = 512
    const val ATT_OVERHEAD = 5 // 3 ATT header + 2 header bytes
}
