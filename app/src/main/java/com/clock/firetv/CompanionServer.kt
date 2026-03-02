package com.clock.firetv

import android.util.Log
import fi.iki.elonen.NanoHTTPD
import org.json.JSONArray
import org.json.JSONObject

class CompanionServer(
    private val settings: SettingsManager,
    port: Int
) : NanoHTTPD(port) {

    companion object {
        private const val TAG = "CompanionServer"
    }

    interface OnPresetChangeListener {
        fun onActivePresetChanged()
    }

    var presetChangeListener: OnPresetChangeListener? = null

    val actualPort: Int get() = listeningPort

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri ?: "/"
        val method = session.method

        return try {
            when {
                method == Method.GET && uri == "/" -> serveHtmlPage()
                method == Method.GET && uri == "/api/presets" -> serveGetPresets()
                method == Method.POST && uri.startsWith("/api/presets/") -> servePostPreset(session, uri)
                method == Method.POST && uri.startsWith("/api/active/") -> servePostActive(uri)
                else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling request: $uri", e)
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Internal error")
        }
    }

    private fun serveGetPresets(): Response {
        val json = JSONObject()
        json.put("activePreset", settings.activePreset)

        val presets = JSONArray()
        for (i in 0 until SettingsManager.PRESET_COUNT) {
            val preset = JSONObject()
            preset.put("index", i)
            preset.put("url", settings.getPresetUrl(i))
            preset.put("name", settings.getPresetName(i))
            presets.put(preset)
        }
        json.put("presets", presets)

        return newFixedLengthResponse(Response.Status.OK, "application/json", json.toString())
            .also { it.addHeader("Access-Control-Allow-Origin", "*") }
    }

    private fun servePostPreset(session: IHTTPSession, uri: String): Response {
        val indexStr = uri.removePrefix("/api/presets/")
        val index = indexStr.toIntOrNull()
            ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Invalid index")

        if (index < 0 || index >= SettingsManager.PRESET_COUNT) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Index out of range (0-3)")
        }

        val body = HashMap<String, String>()
        session.parseBody(body)
        val postData = body["postData"] ?: session.queryParameterString ?: ""
        val json = JSONObject(postData)

        val url = json.optString("url", "")
        val name = json.optString("name", "Preset ${index + 1}")

        settings.setPresetUrl(index, url)
        settings.setPresetName(index, name)

        return newFixedLengthResponse(Response.Status.OK, "application/json", """{"status":"ok"}""")
            .also { it.addHeader("Access-Control-Allow-Origin", "*") }
    }

    private fun servePostActive(uri: String): Response {
        val indexStr = uri.removePrefix("/api/active/")
        val index = indexStr.toIntOrNull()
            ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Invalid index")

        if (index < -1 || index >= SettingsManager.PRESET_COUNT) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Index out of range (-1 to 3)")
        }

        settings.activePreset = index
        presetChangeListener?.onActivePresetChanged()

        return newFixedLengthResponse(Response.Status.OK, "application/json", """{"status":"ok","activePreset":$index}""")
            .also { it.addHeader("Access-Control-Allow-Origin", "*") }
    }

    private fun serveHtmlPage(): Response {
        return newFixedLengthResponse(Response.Status.OK, "text/html", HTML_PAGE)
    }

    private val HTML_PAGE = """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>FireTV Wall Clock — Playlist Config</title>
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body {
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    background: #0A0A12;
    color: #F0EEE6;
    min-height: 100vh;
    padding: 20px;
  }
  h1 {
    text-align: center;
    font-size: 1.4em;
    margin-bottom: 24px;
    color: #E8A44A;
    font-weight: 400;
    letter-spacing: 0.05em;
  }
  .presets {
    max-width: 500px;
    margin: 0 auto;
    display: flex;
    flex-direction: column;
    gap: 16px;
  }
  .card {
    background: rgba(255,255,255,0.06);
    border: 1px solid rgba(255,255,255,0.12);
    border-radius: 12px;
    padding: 16px;
    transition: border-color 0.2s;
  }
  .card.active {
    border-color: #E8A44A;
    box-shadow: 0 0 12px rgba(232,164,74,0.15);
  }
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
  }
  .card-title {
    font-size: 0.9em;
    color: #999;
    text-transform: uppercase;
    letter-spacing: 0.08em;
  }
  .badge {
    font-size: 0.75em;
    background: #E8A44A;
    color: #0A0A12;
    padding: 2px 8px;
    border-radius: 8px;
    font-weight: 600;
  }
  input {
    width: 100%;
    background: rgba(255,255,255,0.08);
    border: 1px solid rgba(255,255,255,0.15);
    border-radius: 8px;
    padding: 10px 12px;
    color: #F0EEE6;
    font-size: 14px;
    outline: none;
    margin-bottom: 8px;
    transition: border-color 0.2s;
  }
  input:focus { border-color: #E8A44A; }
  input::placeholder { color: rgba(240,238,230,0.3); }
  .btn-row {
    display: flex;
    gap: 8px;
    margin-top: 8px;
  }
  button {
    padding: 8px 16px;
    border: none;
    border-radius: 8px;
    font-size: 13px;
    cursor: pointer;
    font-weight: 500;
    transition: opacity 0.2s;
  }
  button:active { opacity: 0.7; }
  .btn-save {
    background: rgba(255,255,255,0.12);
    color: #F0EEE6;
    flex: 1;
  }
  .btn-activate {
    background: #E8A44A;
    color: #0A0A12;
    flex: 1;
  }
  .btn-activate.active {
    background: rgba(232,164,74,0.3);
    color: #E8A44A;
  }
  .status {
    text-align: center;
    margin-top: 16px;
    font-size: 0.85em;
    color: #E8A44A;
    min-height: 1.2em;
  }
</style>
</head>
<body>
<h1>Playlist Configuration</h1>
<div class="presets" id="presets"></div>
<div class="status" id="status"></div>

<script>
const container = document.getElementById('presets');
const statusEl = document.getElementById('status');
let activePreset = -1;

function showStatus(msg) {
  statusEl.textContent = msg;
  setTimeout(() => { if (statusEl.textContent === msg) statusEl.textContent = ''; }, 2000);
}

function renderCards(data) {
  activePreset = data.activePreset;
  container.innerHTML = '';
  data.presets.forEach(p => {
    const isActive = p.index === activePreset;
    const card = document.createElement('div');
    card.className = 'card' + (isActive ? ' active' : '');
    card.innerHTML =
      '<div class="card-header">' +
        '<span class="card-title">Preset ' + (p.index + 1) + '</span>' +
        (isActive ? '<span class="badge">PLAYING</span>' : '') +
      '</div>' +
      '<input type="text" id="name-' + p.index + '" placeholder="Display name" value="' + escHtml(p.name) + '">' +
      '<input type="url" id="url-' + p.index + '" placeholder="YouTube URL or playlist" value="' + escHtml(p.url) + '">' +
      '<div class="btn-row">' +
        '<button class="btn-save" onclick="savePreset(' + p.index + ')">Save</button>' +
        '<button class="btn-activate' + (isActive ? ' active' : '') + '" onclick="activate(' + p.index + ')">' +
          (isActive ? 'Playing' : 'Activate') +
        '</button>' +
      '</div>';
    container.appendChild(card);
  });
}

function escHtml(s) {
  return s.replace(/&/g,'&amp;').replace(/"/g,'&quot;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
}

async function loadPresets() {
  try {
    const res = await fetch('/api/presets');
    const data = await res.json();
    renderCards(data);
  } catch (e) {
    showStatus('Failed to load presets');
  }
}

async function savePreset(index) {
  const name = document.getElementById('name-' + index).value;
  const url = document.getElementById('url-' + index).value;
  try {
    await fetch('/api/presets/' + index, {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({name, url})
    });
    showStatus('Preset ' + (index + 1) + ' saved');
    loadPresets();
  } catch (e) {
    showStatus('Failed to save');
  }
}

async function activate(index) {
  try {
    await fetch('/api/active/' + index, {method: 'POST'});
    showStatus('Preset ' + (index + 1) + ' activated');
    loadPresets();
  } catch (e) {
    showStatus('Failed to activate');
  }
}

loadPresets();
</script>
</body>
</html>
""".trimIndent()
}
