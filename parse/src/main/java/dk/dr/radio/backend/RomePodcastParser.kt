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

package dk.dr.radio.backend

import com.rometools.modules.itunes.EntryInformationImpl
import com.rometools.rome.io.SyndFeedInput
import dk.dr.radio.data.Kanal
import dk.dr.radio.data.Udsendelse
import dk.dr.radio.net.Diverse
import dk.dr.radio.net.FilCache
import org.jsoup.Jsoup
import java.io.File
import java.io.FileInputStream
import java.io.StringReader
import java.util.regex.Pattern

class RomePodcastParser {
    private val syndFeedInput = SyndFeedInput()
    init {
        FilCache.init(File("/tmp/filcache"))
    }


    /**
     * Most feeds use the following DTD to include extra information related to
     * their podcast. Info such as images, summaries, duration, categories is sometimes only available
     * via this attributes in this DTD.
     */
    private val PodcastModuleDtd = "http://www.itunes.com/dtds/podcast-1.0.dtd"


    fun parsRss(str: String, k : Kanal): ArrayList<Udsendelse> {
        val udsendelser = ArrayList<Udsendelse>()
        try {
            var deArkivo = k.eo_elsendojRssUrl.contains("podkasta_arkivo")
            if (k.slug == "varsoviavento" && !deArkivo) parsVarsoviaVento(str, k, udsendelser)
            else if (k.slug == "peranto" && !deArkivo) parsePeranto(str, k, udsendelser)
            else parseAndre(str, k, udsendelser)
        } catch (e : Exception) {
            System.out.flush()
            System.err.flush()
            Thread.sleep(10)
            e.printStackTrace()
            System.out.flush()
            System.err.flush()
            Thread.sleep(10)
            println("str="  + str)
            throw e
        }
        return udsendelser
    }

    private fun parsVarsoviaVento(
        str: String,
        kanal: Kanal,
        udsendelser: ArrayList<Udsendelse>,
    ) {
        val syndFeed = syndFeedInput.build(StringReader(str))
        println("syndFeed.entries.first().modules = ${syndFeed.entries.first().modules.map { it.uri }}")
        syndFeed.entries.forEach { entry ->
            var html = entry.contents[0].value
            for (purigu in EoRssParsado.puriguVarsoviaVento)
                html = purigu.matcher(html).replaceAll("")

            // println("\n\nhtml = ${html}")
            // println("entry = ${entry}")
            Jsoup.parse(entry.contents[0].value).select("audio")
                .forEachIndexed { index, audioElement ->
                    val lydUrl =
                        audioElement.selectFirst("source")?.attr("src") ?: return@forEachIndexed
                    //val tekstMedVarighed = audioElement.nextElementSibling()?.text()
                    // audioElement.nextElementSibling()?.remove()
                    // audioElement.remove()
                    udsendelser.add(
                        Udsendelse(
                            kanal,
                            kanal.slug + ":" + EoRssParsado.datoformato.format(entry.publishedDate) + ":" + (index + 1),
                            entry.title + " " + (index + 1) + "a parto",
                            html,
                            null,
                            entry.publishedDate,
                            lydUrl,
                            0.0,
                            entry.link,
                        )
                    )
                }
        }
    }

    private fun parsePeranto(
        str: String,
        kanal: Kanal,
        udsendelser: ArrayList<Udsendelse>,
    ) {
        val syndFeed = syndFeedInput.build(StringReader(str))
        val HENTLYD = false
        kanal.rss_nextLink = syndFeed.links.find { it.rel == "next" }?.href
        println("kanal.rss_nextLink = ${kanal.rss_nextLink}")
        // println("syndFeed.entries.first().modules = ${syndFeed.entries.first().modules.map { it.uri }}")
        syndFeed.entries.forEach { entry ->
            val slug = entry.uri // kanal.slug + ":" + EoRssParsado.datoformato.format(entry.publishedDate)
            val htmlOrg = entry.contents[0].value
            var html = htmlOrg

            var xslug = EoRssParsado.datoformato.format(entry.publishedDate)
            if (xslug == "2019-11-08" || slug=="2019-09-29") { // Ingen lyd i HTML'en
                return@forEach
            }

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
            var lydIframeUrl = Jsoup.parse(htmlOrg).selectFirst("iframe")?.attr("src")
            if (lydIframeUrl==null) {
                if (html.contains("http://yourlisten.com/")) return@forEach
                if (html.contains("http://vocaroo.com")) return@forEach
                //println("html = ${html}")
                // IllegalStateException("lydIframeUrl==null").printStackTrace()
                return@forEach
            } else if (lydIframeUrl.startsWith("https://drive.google.com/file/d/")) {
                val googleDriveId = lydIframeUrl.split("/")[5]
                lydUrl = "https://drive.google.com/u/1/uc?id="+googleDriveId+"&export=download"
            } else if (lydIframeUrl.startsWith("https://archive.org/embed/")) {
                if (lydIframeUrl == "https://archive.org/embed/orkestro_sklavidojj") lydIframeUrl = "https://archive.org/embed/orkestro_sklavidoj" // Fix fejl i feed
                val filData = Diverse.læsStreng(FileInputStream(FilCache.hentFil(lydIframeUrl, true)))
                lydUrl = "http" + (filData.split(".mp3\"")[0] + ".mp3").split("http").last()
                lydUrl = UnescapeHtml.unescapeHtml3(lydUrl)
            } else if (lydIframeUrl.contains("yourlisten.com")) {
                return@forEach // Kan ikke hente MP3 ...
            } else if (lydIframeUrl.contains("audioboom.com")) {
                return@forEach // Kan ikke hente MP3 ...
            } else if (lydIframeUrl.contains("vimeo.com")) {
                return@forEach // Kan ikke hente MP3 ...
            } else if (lydIframeUrl.contains("ipernity.com")) {
                return@forEach // Kan ikke hente MP3 ...
            } else if (lydIframeUrl.contains("youtube.com/")) {
                return@forEach // Kan ikke hente MP3 fra youtube...
            } else if (lydIframeUrl.startsWith("https://w.soundcloud.com/player/")) {
                return@forEach // Kan ikke hente MP3 fra soundcloud...
            } else {
                println("html = ${html}")
                // IllegalStateException().printStackTrace()
            }


            if (lydUrl.isNotEmpty() != null) try {
                if (HENTLYD) FilCache.hentFil(lydUrl, true)
                val u = Udsendelse( kanal, slug, entry.title, html, billedeUrl, entry.publishedDate, lydUrl,0.0, entry.link)
                udsendelser.add(u)
            } catch (e : Exception) {
                e.printStackTrace()
            }
        }
    }


    private fun parseAndre(
        str: String,
        kanal: Kanal,
        udsendelser: ArrayList<Udsendelse>
    ) {
        val syndFeed = syndFeedInput.build(StringReader(str))
        kanal.rss_nextLink = syndFeed.links.find { it.rel == "next" }?.href
        println("kanal.rss_nextLink = ${kanal.rss_nextLink}")
        syndFeed.entries.forEach { entry ->
            val slug = entry.uri ?: kanal.slug + ":" + EoRssParsado.datoformato.format(entry.publishedDate)
            val information = entry.getModule(PodcastModuleDtd) as? EntryInformationImpl
            //println("entry = ${entry}")
            //println("information = ${information}")
            var beskrivelse = entry.description?.value ?: information?.summary
            if (entry.contents.size > 0) beskrivelse = entry.contents[0].value

            var stream = if (entry.enclosures.size > 0) entry.enclosures[0].url else null
            if (stream == null) stream =
                Jsoup.parse(beskrivelse!!).selectFirst("audio")?.selectFirst("source")?.attr("src")
            if (stream == null) {
                IllegalArgumentException("Hm! stream==null!! for " + kanal + " " + entry.publishedDate)
                return@forEach
            }

            if (stream.startsWith("http://") && kanal.slug == "kernpunkto") {
                // dobbeltfejl .... <enclosure url="https://kern.punkto.info/podlove/file/2460/s/feed/c/mp3/kp204-pigmentoj.mp3"
                // bliver lavet om fra https til http af rometools - og exomedia kan ikke håndtere omdirigeringen fra http tilbage igen til https ....
                //System.out.println("XXX hov, HTTP på = " +k+" "+entry.publishedDate)
                stream = stream.replaceFirst("http", "https");
            }


            udsendelser.add(
                Udsendelse(
                    kanal,
                    slug,
                    entry.title,
                    beskrivelse?.trim(),
                    information?.imageUri ?: information?.image.toString(),
                    entry.publishedDate,
                    stream,
                    (information?.duration?.milliseconds ?: 0)/1000.0,
                    entry.link
                )
            )
        }
    }
}
