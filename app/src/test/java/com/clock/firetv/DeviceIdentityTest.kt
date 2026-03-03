package com.clock.firetv

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeviceIdentityTest {

    private lateinit var identity: DeviceIdentity

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        // Clear prefs to force regeneration
        context.getSharedPreferences("firetv_clock_prefs", android.content.Context.MODE_PRIVATE)
            .edit().remove("device_id").remove("device_name").commit()
        identity = DeviceIdentity(context)
    }

    @Test
    fun `deviceName is in Adjective Noun format`() {
        val name = identity.deviceName
        val parts = name.split(" ")
        assertThat(parts).hasSize(2)
    }

    @Test
    fun `deviceId is a valid UUID`() {
        val id = identity.deviceId
        // UUID format: 8-4-4-4-12
        assertThat(id).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
    }

    @Test
    fun `deviceId is stable across accesses`() {
        val id1 = identity.deviceId
        val id2 = identity.deviceId
        assertThat(id1).isEqualTo(id2)
    }
}
