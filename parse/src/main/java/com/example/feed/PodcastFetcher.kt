/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.feed

import com.rometools.modules.itunes.EntryInformation
import com.rometools.modules.itunes.FeedInformation
import com.rometools.rome.feed.synd.SyndEntry
import com.rometools.rome.io.SyndFeedInput
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * A class which fetches some selected podcast RSS feeds.
 *
 * @param okHttpClient [OkHttpClient] to use for network requests
 * @param syndFeedInput [SyndFeedInput] to use for parsing RSS feeds.
 * @param ioDispatcher [CoroutineDispatcher] to use for running fetch requests.
 */
class PodcastsFetcher() {
    private val syndFeedInput = SyndFeedInput()

    private val okHttpClient: OkHttpClient
    init {
        okHttpClient = OkHttpClient.Builder()
            //.cache(Cache(File(context.cacheDir, "http_cache"), (20 * 1024 * 1024).toLong()))
            .apply {
                //if (BuildConfig.DEBUG)
                    // eventListenerFactory(LoggingEventListener.Factory())
            }
            .build()

    }


    /**
     * It seems that most podcast hosts do not implement HTTP caching appropriately.
     * Instead of fetching data on every app open, we instead allow the use of 'stale'
     * network responses (up to 8 hours).
     */
    private val cacheControl by lazy {
        CacheControl.Builder().maxStale(8, TimeUnit.HOURS).build()
    }

    /**
     * Most feeds use the following DTD to include extra information related to
     * their podcast. Info such as images, summaries, duration, categories is sometimes only available
     * via this attributes in this DTD.
     */
    private val PodcastModuleDtd = "http://www.itunes.com/dtds/podcast-1.0.dtd"

    fun fetchPodcast(url: String): PodcastRssResponse {
        println("====== parsas "+url)
        val request = Request.Builder()
            .url(url)
            .cacheControl(cacheControl)
            .build()

        val response = okHttpClient.newCall(request).execute()
        val syndFeed = syndFeedInput.build(response.body!!.charStream())
        // println("syndFeed.modules = ${syndFeed.modules}")
        println("syndFeed.entries.first().modules = ${syndFeed.entries.first().modules.map { it.uri}}")

        val udsendelser = syndFeed.entries.map {
            val entryInformation = it.getModule(PodcastModuleDtd) as? EntryInformation
            Udsendelse2(
                titel = it.title,
                summary = entryInformation?.summary ?: it.description?.value,
                subtitle = entryInformation?.subtitle,
                published = it.publishedDate,
                duration = entryInformation?.duration?.milliseconds,
                feedEntry = it
            )
        }
        val feedInfo = syndFeed.getModule(PodcastModuleDtd) as? FeedInformation
        val kanal = Kanal2(
            navn = syndFeed.title,
            description = feedInfo?.summary ?: syndFeed.description,
            // copyright = syndFeed.copyright,
            kanallogo_url = feedInfo?.imageUri?.toString()
        )
        val podcastRssResponse = PodcastRssResponse(kanal, udsendelser)

        return podcastRssResponse
    }
}

data class Kanal2(
    val navn: String,
    val description: String? = null,
    val kanallogo_url: String? = null,
    // val copyright: String? = null
)

data class Udsendelse2(
    val titel: String,
    val subtitle: String? = null,
    val published: Date,
    val summary: String? = null,
    val duration: Long? = null,
    val feedEntry: SyndEntry
)

data class PodcastRssResponse(
    val kanal: Kanal2,
    val udsendelses: List<Udsendelse2>,
    ) {
}

