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
import com.rometools.rome.io.SyndFeedInput
import dk.dr.radio.backend.EoRssParsado
import dk.dr.radio.data.Kanal
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.StringReader
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

    /*

data class Kanal2(
val navn: String,
val description: String? = null,
val kanallogo_url: String? = null,
// val copyright: String? = null
)

    val feedInfo = syndFeed.getModule(PodcastModuleDtd) as? FeedInformation
    val kanal = Kanal2(
        navn = syndFeed.title,
        description = feedInfo?.summary ?: syndFeed.description,
        // copyright = syndFeed.copyright,
        kanallogo_url = feedInfo?.imageUri?.toString()
    )
    val podcastRssResponse = PodcastRssResponse(kanal, udsendelser)

     */

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

    fun fetchPodcastFromUrl(url: String, k : Kanal): List<Udsendelse2> {
        println("====== parsas "+url)
        val str = fetchUrl(url)
        return parsRss(str, k)
    }

    fun fetchUrl(url: String): String {
        val request = Request.Builder()
            .url(url)
            .cacheControl(cacheControl)
            .build()

        val str = okHttpClient.newCall(request).execute().body!!.string()
        return str
    }

    fun parsRss(str: String, k : Kanal): List<Udsendelse2> {
        val syndFeed = syndFeedInput.build(StringReader(str))
        // println("syndFeed.modules = ${syndFeed.modules}")

        try {
            println("syndFeed.entries.first().modules = ${syndFeed.entries.first().modules.map { it.uri}}")

            val uds = mutableListOf<Udsendelse2>()


            val udsendelser = syndFeed.entries.map { entry ->
                val information = entry.getModule(PodcastModuleDtd) as? EntryInformation
                //println("entry = ${entry}")
                //println("information = ${information}")
                Udsendelse2(
                    titel = entry.title,
                    beskrivelse = information?.summary ?: entry.description?.value,
                    // subtitle = entryInformation?.subtitle,
                    startTid = entry.publishedDate,
                    startTidDato = EoRssParsado.datoformato.format(entry.publishedDate),
                    slug = k.slug+":"+EoRssParsado.datoformato.format(entry.publishedDate),
                    stream = entry.enclosures[0].url,
                    duration = information?.duration?.milliseconds,
                    // feedEntry = entry
                )
            }

            return udsendelser
        } catch (e : Exception) {
            System.out.flush()
            System.err.flush()
            Thread.sleep(10)
            e.printStackTrace()
            System.out.flush()
            System.err.flush()
            Thread.sleep(10)
            println("${syndFeed.entries.first()}")
            throw e
        }
    }
}

data class Udsendelse2(
    val slug: String,
    val titel: String,
    val billedeUrl: String? = null,
    val startTid: Date,
    val startTidDato: String,
    val beskrivelse: String? = null,
    val stream: String,
    val duration: Long? = null,
    // val feedEntry: SyndEntry
) {

    override fun toString(): String {
        // return slug + "/" + startTidKl;
        return "Udsendelse{" +
                "slug='" + slug + '\'' +
                ", titel='" + titel + '\'' +
                ", beskrivelse='" + beskrivelse + '\'' +
                ", billedeUrl='" + billedeUrl + '\'' +
                ", startTidKl='" + startTidDato + '\'' +
                ", stream=" + stream +
                '}'
    }

}

