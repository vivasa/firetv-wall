package com.clock.firetv

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.HttpURLConnection
import java.net.URL

@RunWith(RobolectricTestRunner::class)
class CompanionServerTest {

    private lateinit var settings: SettingsManager
    private lateinit var server: CompanionServer

    @Before
    fun setUp() {
        settings = SettingsManager(ApplicationProvider.getApplicationContext())
        settings.prefs.edit().clear().commit()
        server = CompanionServer(settings, 0) // port 0 = random available port
        server.start()
    }

    @After
    fun tearDown() {
        server.stop()
    }

    private fun get(path: String): Pair<Int, String> {
        val url = URL("http://localhost:${server.actualPort}$path")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        val code = conn.responseCode
        val body = if (code in 200..299) conn.inputStream.bufferedReader().readText()
        else conn.errorStream?.bufferedReader()?.readText() ?: ""
        conn.disconnect()
        return code to body
    }

    private fun post(path: String, body: String = ""): Pair<Int, String> {
        val url = URL("http://localhost:${server.actualPort}$path")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        if (body.isNotEmpty()) {
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.write(body.toByteArray())
        }
        val code = conn.responseCode
        val respBody = if (code in 200..299) conn.inputStream.bufferedReader().readText()
        else conn.errorStream?.bufferedReader()?.readText() ?: ""
        conn.disconnect()
        return code to respBody
    }

    @Test
    fun `GET presets returns JSON with activePreset and 4 presets`() {
        val (code, body) = get("/api/presets")
        assertThat(code).isEqualTo(200)
        val json = JSONObject(body)
        assertThat(json.getInt("activePreset")).isEqualTo(-1) // default
        val presets = json.getJSONArray("presets")
        assertThat(presets.length()).isEqualTo(4)
    }

    @Test
    fun `POST preset with valid index 0 saves correctly`() {
        val (code, _) = post("/api/presets/0", """{"url":"https://youtube.com/test","name":"Test"}""")
        assertThat(code).isEqualTo(200)
        assertThat(settings.getPresetUrl(0)).isEqualTo("https://youtube.com/test")
        assertThat(settings.getPresetName(0)).isEqualTo("Test")
    }

    @Test
    fun `POST preset with valid index 3 saves correctly`() {
        val (code, _) = post("/api/presets/3", """{"url":"https://youtube.com/last","name":"Last"}""")
        assertThat(code).isEqualTo(200)
        assertThat(settings.getPresetUrl(3)).isEqualTo("https://youtube.com/last")
    }

    @Test
    fun `POST preset with index 4 returns bad request`() {
        val (code, _) = post("/api/presets/4", """{"url":"test","name":"test"}""")
        assertThat(code).isEqualTo(400)
    }

    @Test
    fun `POST preset with negative index returns bad request`() {
        val (code, _) = post("/api/presets/-1", """{"url":"test","name":"test"}""")
        assertThat(code).isEqualTo(400)
    }

    @Test
    fun `POST active with valid index updates activePreset`() {
        val (code, body) = post("/api/active/2")
        assertThat(code).isEqualTo(200)
        assertThat(settings.activePreset).isEqualTo(2)
        val json = JSONObject(body)
        assertThat(json.getInt("activePreset")).isEqualTo(2)
    }

    @Test
    fun `POST active with -1 is valid (stop)`() {
        val (code, _) = post("/api/active/-1")
        assertThat(code).isEqualTo(200)
        assertThat(settings.activePreset).isEqualTo(-1)
    }

    @Test
    fun `POST active with 4 returns bad request`() {
        val (code, _) = post("/api/active/4")
        assertThat(code).isEqualTo(400)
    }
}
