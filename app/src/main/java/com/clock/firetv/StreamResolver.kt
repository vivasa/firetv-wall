package com.clock.firetv

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfoItem

data class StreamResult(val url: String, val title: String?)
data class PlaylistItem(val url: String, val title: String?)
data class PlaylistResult(val title: String?, val items: List<PlaylistItem>)

class StreamResolver {

    companion object {
        private const val TAG = "StreamResolver"
        private const val MAX_RESOLUTION = 720
    }

    suspend fun resolveStreamUrl(videoUrl: String): StreamResult? = withContext(Dispatchers.IO) {
        try {
            val fullUrl = if (videoUrl.startsWith("http")) videoUrl
                else "https://www.youtube.com/watch?v=$videoUrl"

            val extractor = ServiceList.YouTube.getStreamExtractor(fullUrl)
            extractor.fetchPage()

            val title = try { extractor.name?.takeIf { it.isNotBlank() } } catch (_: Exception) { null }

            val progressiveStreams = extractor.videoStreams
                .filter { !it.isVideoOnly }

            // Pick best stream at or below 720p
            val bestUnderCap = progressiveStreams
                .filter { parseResolution(it.resolution) <= MAX_RESOLUTION }
                .maxByOrNull { parseResolution(it.resolution) }

            // Fallback: best available progressive stream of any resolution
            val stream = bestUnderCap
                ?: progressiveStreams.maxByOrNull { parseResolution(it.resolution) }

            stream?.content?.let { StreamResult(it, title) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve stream for $videoUrl", e)
            null
        }
    }

    suspend fun extractPlaylistItems(playlistUrl: String): PlaylistResult = withContext(Dispatchers.IO) {
        try {
            val fullUrl = if (playlistUrl.startsWith("http")) playlistUrl
                else "https://www.youtube.com/playlist?list=$playlistUrl"

            val extractor = ServiceList.YouTube.getPlaylistExtractor(fullUrl)
            extractor.fetchPage()

            val playlistTitle = try { extractor.name?.takeIf { it.isNotBlank() } } catch (_: Exception) { null }
            val items = mutableListOf<PlaylistItem>()

            var page = extractor.initialPage
            for (item in page.items) {
                if (item is StreamInfoItem) {
                    items.add(PlaylistItem(item.url, item.name?.takeIf { it.isNotBlank() }))
                }
            }

            while (page.hasNextPage()) {
                page = extractor.getPage(page.nextPage)
                for (item in page.items) {
                    if (item is StreamInfoItem) {
                        items.add(PlaylistItem(item.url, item.name?.takeIf { it.isNotBlank() }))
                    }
                }
            }

            PlaylistResult(playlistTitle, items)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract playlist items for $playlistUrl", e)
            PlaylistResult(null, emptyList())
        }
    }

    internal fun parseResolution(resolution: String?): Int {
        return resolution?.replace("p", "")?.toIntOrNull() ?: 0
    }
}
