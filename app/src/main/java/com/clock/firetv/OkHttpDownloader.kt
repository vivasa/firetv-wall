package com.clock.firetv

import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response

class OkHttpDownloader(
    private val client: OkHttpClient = OkHttpClient.Builder().build()
) : Downloader() {

    override fun execute(request: Request): Response {
        val requestBuilder = okhttp3.Request.Builder()
            .url(request.url())
            .method(request.httpMethod(), request.dataToSend()?.toRequestBody())

        request.headers().forEach { (name, values) ->
            values.forEach { value ->
                requestBuilder.addHeader(name, value)
            }
        }

        val response = client.newCall(requestBuilder.build()).execute()

        val responseHeaders = mutableMapOf<String, MutableList<String>>()
        response.headers.forEach { (name, value) ->
            responseHeaders.getOrPut(name) { mutableListOf() }.add(value)
        }

        return Response(
            response.code,
            response.message,
            responseHeaders,
            response.body?.string(),
            response.request.url.toString()
        )
    }
}
