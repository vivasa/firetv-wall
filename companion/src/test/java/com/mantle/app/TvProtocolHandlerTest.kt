package com.mantle.app

import com.firetv.protocol.ProtocolCommands
import com.firetv.protocol.ProtocolConfig
import com.firetv.protocol.ProtocolKeys
import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TvProtocolHandlerTest {

    // --- parseEvent tests ---

    @Test
    fun `parseEvent AUTH_OK returns AuthOk with device info`() {
        val json = JSONObject().apply {
            put(ProtocolKeys.EVT, "auth_ok")
            put(ProtocolKeys.DEVICE_ID, "dev-123")
            put(ProtocolKeys.DEVICE_NAME, "Living Room")
        }.toString()

        val event = TvProtocolHandler.parseEvent(json)
        assertThat(event).isInstanceOf(ProtocolEvent.AuthOk::class.java)
        val authOk = event as ProtocolEvent.AuthOk
        assertThat(authOk.deviceId).isEqualTo("dev-123")
        assertThat(authOk.deviceName).isEqualTo("Living Room")
    }

    @Test
    fun `parseEvent AUTH_FAILED returns AuthFailed with reason`() {
        val json = JSONObject().apply {
            put(ProtocolKeys.EVT, "auth_failed")
            put(ProtocolKeys.REASON, "invalid_pin")
        }.toString()

        val event = TvProtocolHandler.parseEvent(json)
        assertThat(event).isInstanceOf(ProtocolEvent.AuthFailed::class.java)
        assertThat((event as ProtocolEvent.AuthFailed).reason).isEqualTo("invalid_pin")
    }

    @Test
    fun `parseEvent PAIRED returns Paired with token and device info`() {
        val json = JSONObject().apply {
            put(ProtocolKeys.EVT, "paired")
            put(ProtocolKeys.TOKEN, "abc123")
            put(ProtocolKeys.DEVICE_ID, "dev-456")
            put(ProtocolKeys.DEVICE_NAME, "Bedroom")
        }.toString()

        val event = TvProtocolHandler.parseEvent(json)
        assertThat(event).isInstanceOf(ProtocolEvent.Paired::class.java)
        val paired = event as ProtocolEvent.Paired
        assertThat(paired.token).isEqualTo("abc123")
        assertThat(paired.deviceId).isEqualTo("dev-456")
        assertThat(paired.deviceName).isEqualTo("Bedroom")
    }

    @Test
    fun `parseEvent TRACK_CHANGED returns TrackChanged`() {
        val json = JSONObject().apply {
            put(ProtocolKeys.EVT, "track_changed")
            put(ProtocolKeys.TITLE, "Song Title")
            put(ProtocolKeys.PLAYLIST, "My Playlist")
        }.toString()

        val event = TvProtocolHandler.parseEvent(json)
        assertThat(event).isInstanceOf(ProtocolEvent.TrackChanged::class.java)
        val tc = event as ProtocolEvent.TrackChanged
        assertThat(tc.title).isEqualTo("Song Title")
        assertThat(tc.playlist).isEqualTo("My Playlist")
    }

    @Test
    fun `parseEvent TRACK_CHANGED with empty playlist returns null playlist`() {
        val json = JSONObject().apply {
            put(ProtocolKeys.EVT, "track_changed")
            put(ProtocolKeys.TITLE, "Song")
            put(ProtocolKeys.PLAYLIST, "")
        }.toString()

        val event = TvProtocolHandler.parseEvent(json) as ProtocolEvent.TrackChanged
        assertThat(event.playlist).isNull()
    }

    @Test
    fun `parseEvent PLAYBACK_STATE returns PlaybackState`() {
        val json = JSONObject().apply {
            put(ProtocolKeys.EVT, "playback_state")
            put(ProtocolKeys.IS_PLAYING, true)
        }.toString()

        val event = TvProtocolHandler.parseEvent(json) as ProtocolEvent.PlaybackState
        assertThat(event.isPlaying).isTrue()
    }

    @Test
    fun `parseEvent CONFIG_APPLIED returns ConfigApplied`() {
        val json = JSONObject().apply {
            put(ProtocolKeys.EVT, "config_applied")
            put(ProtocolKeys.VERSION, 5)
        }.toString()

        val event = TvProtocolHandler.parseEvent(json) as ProtocolEvent.ConfigApplied
        assertThat(event.version).isEqualTo(5)
    }

    @Test
    fun `parseEvent STATE returns State with device info and active preset`() {
        val data = JSONObject().apply {
            put(ProtocolKeys.DEVICE_ID, "dev-789")
            put(ProtocolKeys.DEVICE_NAME, "Kitchen")
            put(ProtocolKeys.ACTIVE_PRESET, 2)
        }
        val json = JSONObject().apply {
            put(ProtocolKeys.EVT, "state")
            put(ProtocolKeys.DATA, data)
        }.toString()

        val event = TvProtocolHandler.parseEvent(json) as ProtocolEvent.State
        assertThat(event.deviceId).isEqualTo("dev-789")
        assertThat(event.deviceName).isEqualTo("Kitchen")
        assertThat(event.activePreset).isEqualTo(2)
    }

    @Test
    fun `parseEvent STATE without data returns null`() {
        val json = JSONObject().apply {
            put(ProtocolKeys.EVT, "state")
        }.toString()

        assertThat(TvProtocolHandler.parseEvent(json)).isNull()
    }

    @Test
    fun `parseEvent PONG returns Pong`() {
        val json = JSONObject().apply {
            put(ProtocolKeys.EVT, "pong")
        }.toString()

        assertThat(TvProtocolHandler.parseEvent(json)).isInstanceOf(ProtocolEvent.Pong::class.java)
    }

    @Test
    fun `parseEvent ERROR returns Error with message`() {
        val json = JSONObject().apply {
            put(ProtocolKeys.EVT, "error")
            put(ProtocolKeys.MESSAGE, "something broke")
        }.toString()

        val event = TvProtocolHandler.parseEvent(json) as ProtocolEvent.Error
        assertThat(event.message).isEqualTo("something broke")
    }

    @Test
    fun `parseEvent unknown event returns null`() {
        val json = JSONObject().apply {
            put(ProtocolKeys.EVT, "unknown_event_xyz")
        }.toString()

        assertThat(TvProtocolHandler.parseEvent(json)).isNull()
    }

    @Test
    fun `parseEvent invalid JSON returns null`() {
        assertThat(TvProtocolHandler.parseEvent("not valid json!!!")).isNull()
    }

    // --- Command builder tests ---

    @Test
    fun `buildAuth includes token and protocol version`() {
        val cmd = TvProtocolHandler.buildAuth("my-token")
        assertThat(cmd.getString(ProtocolKeys.CMD)).isEqualTo(ProtocolCommands.AUTH)
        assertThat(cmd.getString(ProtocolKeys.TOKEN)).isEqualTo("my-token")
        assertThat(cmd.getInt(ProtocolKeys.PROTOCOL_VERSION)).isEqualTo(ProtocolConfig.PROTOCOL_VERSION)
    }

    @Test
    fun `buildPairRequest has correct cmd`() {
        val cmd = TvProtocolHandler.buildPairRequest()
        assertThat(cmd.getString(ProtocolKeys.CMD)).isEqualTo(ProtocolCommands.PAIR_REQUEST)
    }

    @Test
    fun `buildPairConfirm includes pin`() {
        val cmd = TvProtocolHandler.buildPairConfirm("1234")
        assertThat(cmd.getString(ProtocolKeys.CMD)).isEqualTo(ProtocolCommands.PAIR_CONFIRM)
        assertThat(cmd.getString(ProtocolKeys.PIN)).isEqualTo("1234")
    }

    @Test
    fun `buildPlay includes preset index`() {
        val cmd = TvProtocolHandler.buildPlay(3)
        assertThat(cmd.getString(ProtocolKeys.CMD)).isEqualTo(ProtocolCommands.PLAY)
        assertThat(cmd.getInt(ProtocolKeys.PRESET_INDEX)).isEqualTo(3)
    }

    @Test
    fun `buildStop has correct cmd`() {
        assertThat(TvProtocolHandler.buildStop().getString(ProtocolKeys.CMD)).isEqualTo(ProtocolCommands.STOP)
    }

    @Test
    fun `buildPause has correct cmd`() {
        assertThat(TvProtocolHandler.buildPause().getString(ProtocolKeys.CMD)).isEqualTo(ProtocolCommands.PAUSE)
    }

    @Test
    fun `buildResume has correct cmd`() {
        assertThat(TvProtocolHandler.buildResume().getString(ProtocolKeys.CMD)).isEqualTo(ProtocolCommands.RESUME)
    }

    @Test
    fun `buildSeek includes offset`() {
        val cmd = TvProtocolHandler.buildSeek(-30)
        assertThat(cmd.getString(ProtocolKeys.CMD)).isEqualTo(ProtocolCommands.SEEK)
        assertThat(cmd.getInt(ProtocolKeys.OFFSET_SEC)).isEqualTo(-30)
    }

    @Test
    fun `buildSkip includes direction`() {
        val cmd = TvProtocolHandler.buildSkip(1)
        assertThat(cmd.getString(ProtocolKeys.CMD)).isEqualTo(ProtocolCommands.SKIP)
        assertThat(cmd.getInt(ProtocolKeys.DIRECTION)).isEqualTo(1)
    }

    @Test
    fun `buildSyncConfig includes config object`() {
        val config = JSONObject().apply { put("version", 5) }
        val cmd = TvProtocolHandler.buildSyncConfig(config)
        assertThat(cmd.getString(ProtocolKeys.CMD)).isEqualTo(ProtocolCommands.SYNC_CONFIG)
        assertThat(cmd.getJSONObject(ProtocolKeys.CONFIG).getInt("version")).isEqualTo(5)
    }

    @Test
    fun `buildGetState has correct cmd`() {
        assertThat(TvProtocolHandler.buildGetState().getString(ProtocolKeys.CMD)).isEqualTo(ProtocolCommands.GET_STATE)
    }

    @Test
    fun `buildPing has correct cmd`() {
        assertThat(TvProtocolHandler.buildPing().getString(ProtocolKeys.CMD)).isEqualTo(ProtocolCommands.PING)
    }

    // --- PLAYLIST_TRACKS parsing ---

    @Test
    fun `parseEvent PLAYLIST_TRACKS returns PlaylistTracks with tracks`() {
        val tracks = org.json.JSONArray().apply {
            put(JSONObject().apply { put(ProtocolKeys.INDEX, 0); put(ProtocolKeys.TITLE, "Track One") })
            put(JSONObject().apply { put(ProtocolKeys.INDEX, 1); put(ProtocolKeys.TITLE, "Track Two") })
            put(JSONObject().apply { put(ProtocolKeys.INDEX, 2); put(ProtocolKeys.TITLE, "Track Three") })
        }
        val json = JSONObject().apply {
            put(ProtocolKeys.EVT, "playlist_tracks")
            put(ProtocolKeys.PLAYLIST, "My Playlist")
            put(ProtocolKeys.CURRENT_INDEX, 1)
            put(ProtocolKeys.TRACKS, tracks)
        }.toString()

        val event = TvProtocolHandler.parseEvent(json)
        assertThat(event).isInstanceOf(ProtocolEvent.PlaylistTracks::class.java)
        val pt = event as ProtocolEvent.PlaylistTracks
        assertThat(pt.playlist).isEqualTo("My Playlist")
        assertThat(pt.currentIndex).isEqualTo(1)
        assertThat(pt.tracks).hasSize(3)
        assertThat(pt.tracks[0].title).isEqualTo("Track One")
        assertThat(pt.tracks[2].index).isEqualTo(2)
    }

    @Test
    fun `parseEvent PLAYLIST_TRACKS with empty tracks`() {
        val json = JSONObject().apply {
            put(ProtocolKeys.EVT, "playlist_tracks")
            put(ProtocolKeys.PLAYLIST, "")
            put(ProtocolKeys.CURRENT_INDEX, 0)
            put(ProtocolKeys.TRACKS, org.json.JSONArray())
        }.toString()

        val event = TvProtocolHandler.parseEvent(json) as ProtocolEvent.PlaylistTracks
        assertThat(event.playlist).isNull()
        assertThat(event.tracks).isEmpty()
    }

    // --- New command builders ---

    @Test
    fun `buildGetPlaylistTracks has correct cmd`() {
        val cmd = TvProtocolHandler.buildGetPlaylistTracks()
        assertThat(cmd.getString(ProtocolKeys.CMD)).isEqualTo(ProtocolCommands.GET_PLAYLIST_TRACKS)
    }

    @Test
    fun `buildPlayTrack includes track index`() {
        val cmd = TvProtocolHandler.buildPlayTrack(5)
        assertThat(cmd.getString(ProtocolKeys.CMD)).isEqualTo(ProtocolCommands.PLAY_TRACK)
        assertThat(cmd.getInt(ProtocolKeys.TRACK_INDEX)).isEqualTo(5)
    }
}
