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
import dk.dr.radio.data.Udsendelse
import dk.dr.radio.net.Diverse
import dk.dr.radio.net.FilCache
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.File
import java.io.FileInputStream
import java.io.StringReader
import java.util.Date
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

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

        FilCache.init(File("/tmp/filcache"))
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
        val udsendelser = mutableListOf<Udsendelse2>()

        // println("syndFeed.modules = ${syndFeed.modules}")

        val syndFeed = syndFeedInput.build(StringReader(str))
        println("syndFeed.entries.first().modules = ${syndFeed.entries.first().modules.map { it.uri}}")
        try {
            if (k.slug == "varsoviavento") {
                syndFeed.entries.forEach { entry ->
                    var html = entry.contents[0].value
                    for (purigu in EoRssParsado.puriguVarsoviaVento)
                        html = purigu.matcher(html).replaceAll("")

                    // println("\n\nhtml = ${html}")
                    // println("entry = ${entry}")
                    Jsoup.parse(entry.contents[0].value).select("audio")
                        .forEachIndexed { index, audioElement ->
                            val lydUrl = audioElement.selectFirst("source")?.attr("src") ?: return@forEachIndexed
                            //val tekstMedVarighed = audioElement.nextElementSibling()?.text()
                            // audioElement.nextElementSibling()?.remove()
                            // audioElement.remove()
                            udsendelser.add(
                                Udsendelse2(
                                    kanal = k,
                                    titel = entry.title + " " + (index + 1)+"a parto",
                                    billedeUrl = null,
                                    slug = k.slug + ":" + EoRssParsado.datoformato.format(entry.publishedDate) + ":" + (index + 1),
                                    link = entry.link,
                                    startTid = entry.publishedDate,
                                    startTidDato = EoRssParsado.datoformato.format(entry.publishedDate),
                                    stream = lydUrl,
                                    beskrivelse = html,
                                )
                            )
                        }
                }

            } else if (k.slug == "peranto") {
                syndFeed.entries.forEach { entry ->
                    try {
                    var html = entry.contents[0].value

                    val billedeUrl = Jsoup.parse(html).selectFirst("img")?.attr("src")
                    if (billedeUrl != null) {
                        html = Pattern.compile("<img.+? />", Pattern.DOTALL).matcher(html).replaceFirst("")
                    }

                    for (purigu in arrayOf(
                        Pattern.compile("<iframe.+?</iframe>", Pattern.DOTALL),
                        Pattern.compile("<div class=\"separator\".+?>", Pattern.DOTALL),
                    )) html = purigu.matcher(html).replaceAll("")

                    // println("entry = ${entry}")

                    var lydUrl = ""
                    var lydIframeUrl = Jsoup.parse(entry.contents[0].value).selectFirst("iframe")?.attr("src")
                    if (lydIframeUrl != null && lydIframeUrl.startsWith("https://archive.org/embed/")) {

                        if (lydIframeUrl == "https://archive.org/embed/orkestro_sklavidojj") lydIframeUrl == "https://archive.org/embed/orkestro_sklavidoj" // tajperaro

                        val filData =
                            Diverse.læsStreng(FileInputStream(FilCache.hentFil(lydIframeUrl, true)))
                        lydUrl = "http" + (filData.split(".mp3\"")[0] + ".mp3").split("http").last()

                        //println("fil = ${fil}")
                        //val id = lydIframeUrl.split("/").last()
                        //lydUrl = "https://archive.org/download/$id/$id.mp3"
                    }

                    if (lydUrl != null) udsendelser.add(
                        Udsendelse2(
                            kanal = k,
                            titel = entry.title,
                            billedeUrl = billedeUrl,
                            slug = k.slug + ":" + EoRssParsado.datoformato.format(entry.publishedDate),
                            link = entry.link,
                            startTid = entry.publishedDate,
                            startTidDato = EoRssParsado.datoformato.format(entry.publishedDate),
                            stream = lydUrl,
                            beskrivelse = html,
                        )
                    ) else throw Exception()
                    } catch (e : Exception) {
                        System.out.flush()
                        System.err.flush()
                        Thread.sleep(10)
                        e.printStackTrace()
                        System.out.flush()
                        System.err.flush()
                        Thread.sleep(10)
                        println(entry.toString())
                    }
                }
            } else {

                syndFeed.entries.forEach { entry ->
                    val information = entry.getModule(PodcastModuleDtd) as? EntryInformation
                    //println("entry = ${entry}")
                    //println("information = ${information}")
                    var beskrivelse = entry.description?.value ?: information?.summary
                    if (entry.contents.size>0) beskrivelse = entry.contents[0].value

                    var stream = if (entry.enclosures.size>0) entry.enclosures[0].url else null
                    if (stream==null) stream = Jsoup.parse(beskrivelse).selectFirst("audio")?.selectFirst("source")?.attr("src")

                    udsendelser.add(
                        Udsendelse2(
                            kanal = k,
                            titel = entry.title,
                            billedeUrl = null,
                            slug = k.slug + ":" + EoRssParsado.datoformato.format(entry.publishedDate),
                            link = entry.link,
                            startTid = entry.publishedDate,
                            startTidDato = EoRssParsado.datoformato.format(entry.publishedDate),
                            stream = stream!!,
                            beskrivelse = beskrivelse?.trim()
                        )
                    )
                    return udsendelser
                }
            }

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
        return udsendelser
    }
}

data class Udsendelse2(
    val kanal: Kanal,
    val slug: String,
    val titel: String,
    val beskrivelse: String? = null,
    val billedeUrl: String? = null,
    val startTid: Date,
    val startTidDato: String,
    val stream: String,
    val link: String?,
) {

    override fun toString(): String {
        // return slug + "/" + startTidKl;
        return "Udsendelse2{" +
                "slug='" + slug + '\'' +
                ", titel='" + titel + '\'' +
                ", beskrivelse='" + beskrivelse + '\'' +
                ", billedeUrl='" + billedeUrl + '\'' +
                ", startTidKl='" + startTidDato + '\'' +
                ", stream=" + stream +
                '}'
    }

}

