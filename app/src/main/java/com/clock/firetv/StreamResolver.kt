package com.clock.firetv

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfoItem

class StreamResolver {

    companion object {
        private const val TAG = "StreamResolver"
        private const val MAX_RESOLUTION = 720
    }

    suspend fun resolveStreamUrl(videoUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            val fullUrl = if (videoUrl.startsWith("http")) videoUrl
                else "https://www.youtube.com/watch?v=$videoUrl"

            val extractor = ServiceList.YouTube.getStreamExtractor(fullUrl)
            extractor.fetchPage()

            val progressiveStreams = extractor.videoStreams
                .filter { !it.isVideoOnly }

            // Pick best stream at or below 720p
            val bestUnderCap = progressiveStreams
                .filter { parseResolution(it.resolution) <= MAX_RESOLUTION }
                .maxByOrNull { parseResolution(it.resolution) }

            // Fallback: best available progressive stream of any resolution
            val stream = bestUnderCap
                ?: progressiveStreams.maxByOrNull { parseResolution(it.resolution) }

            stream?.content
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve stream for $videoUrl", e)
            null
        }
    }

    suspend fun extractPlaylistItems(playlistUrl: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val fullUrl = if (playlistUrl.startsWith("http")) playlistUrl
                else "https://www.youtube.com/playlist?list=$playlistUrl"

            val extractor = ServiceList.YouTube.getPlaylistExtractor(fullUrl)
            extractor.fetchPage()

            val urls = mutableListOf<String>()

            var page = extractor.initialPage
            for (item in page.items) {
                if (item is StreamInfoItem) {
                    urls.add(item.url)
                }
            }

            while (page.hasNextPage()) {
                page = extractor.getPage(page.nextPage)
                for (item in page.items) {
                    if (item is StreamInfoItem) {
                        urls.add(item.url)
                    }
                }
            }

            urls
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract playlist items for $playlistUrl", e)
            emptyList()
        }
    }

    private fun parseResolution(resolution: String?): Int {
        return resolution?.replace("p", "")?.toIntOrNull() ?: 0
    }
}
