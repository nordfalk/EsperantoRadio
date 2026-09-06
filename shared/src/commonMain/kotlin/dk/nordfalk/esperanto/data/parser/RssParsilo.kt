package dk.nordfalk.esperanto.data.parser

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanal
import dk.nordfalk.esperanto.logd
import dk.nordfalk.esperanto.logi

/**
 * La ĝenerala RSS/Atom-parsilo. Traktas la sep parsregolojn.
 * Vidu docs/malnova/03_parsado_kaj_fontoj.md kaj docs/nova/04_parsado_kaj_arkivo.md.
 */
class RssParsilo {

    fun parsRss(
        fluoTeksto: String,
        kanal: Kanal,
        httpKliento: suspend (String) -> String = { "" }
    ): List<Elsendo> {
        logd("RssParsilo", "Parsas RSS por ${kanal.slug} — ${fluoTeksto.length} signoj")
        val doc = Ksoup.parseXml(fluoTeksto, "")
        val deArkivo = kanal.podkastaRssUrl?.contains("podkasta_arkivo") == true

        val rezulto = if (deArkivo) {
            logd("RssParsilo", "${kanal.slug}: uzas arkiv-parsilon")
            parsGenerel(doc, kanal)
        } else when (kanal.slug) {
            "varsoviavento" -> {
                logd("RssParsilo", "${kanal.slug}: uzas VarsoviaVento-parsilon")
                parsVarsoviaVento(doc, kanal)
            }
            "peranto" -> {
                logd("RssParsilo", "${kanal.slug}: uzas Peranto-parsilon")
                parsPeranto(doc, kanal, httpKliento)
            }
            "vinilkosmo" -> {
                logd("RssParsilo", "${kanal.slug}: uzas Vinilkosmo-parsilon")
                parsVinilkosmo(doc, kanal)
            }
            else -> {
                logd("RssParsilo", "${kanal.slug}: uzas ĝeneralan parsilon")
                parsGenerel(doc, kanal)
            }
        }

        logi("RssParsilo", "${kanal.slug}: parsado kompleta — ${rezulto.size} elsendoj")
        return rezulto
    }

    fun leguNextLink(fluoTeksto: String): String? {
        val doc = Ksoup.parseXml(fluoTeksto, "")
        return doc.selectFirst("atom|link[rel=next]")?.attr("href")
            ?: doc.selectFirst("link[rel=next]")?.attr("href")
    }

    // === Regulo 6.1 — Ĝenerala ===

    private fun parsGenerel(doc: Document, kanal: Kanal): List<Elsendo> =
        doc.select("item, entry").mapIndexedNotNull { i, ero -> parsEroGenerel(ero, kanal, i) }

    private fun parsEroGenerel(ero: Element, kanal: Kanal, indekso: Int): Elsendo? {
        val titolo = ero.selectFirst("title")?.text() ?: ""
        val priskriboHtml = ero.selectFirst("content|encoded")?.html()
            ?: ero.selectFirst("description")?.html()
            ?: ""
        val priskribo = ero.selectFirst("description")?.text()
            ?: ero.selectFirst("content|encoded")?.text()
            ?: ero.selectFirst("itunes|summary")?.text()
            ?: ""

        val pubDate = ero.selectFirst("pubDate")?.text()
            ?: ero.selectFirst("published")?.text()
            ?: ""
        val dato = normigiDaton(pubDate) ?: return null

        // Stream: enclosure (RSS) aŭ <link rel="enclosure"> (Atom) aŭ <audio><source>
        var stream = ero.selectFirst("enclosure")?.attr("url") ?: ""
        if (stream.isEmpty()) {
            stream = ero.selectFirst("link[rel=enclosure]")?.attr("href") ?: ""
        }
        if (stream.isEmpty()) {
            stream = ero.selectFirst("audio source")?.attr("src") ?: ""
        }
        if (stream.isEmpty()) return null

        if (kanal.slug == "kernpunkto" && stream.startsWith("http://")) {
            stream = "https://" + stream.removePrefix("http://")
        }

        val dauro = leguDauron(ero.selectFirst("itunes|duration")?.text())
        val bildUrl = ero.selectFirst("itunes|image")?.attr("href")
            ?: ero.selectFirst("image url")?.text()
        val retpaghoUrl = ero.selectFirst("link")?.text()
            ?: ero.selectFirst("link")?.attr("href")

        val finaTitolo = if (kanal.ignoruTitolon) derivuTitolon(priskribo) else titolo

        // Unika ID: uzu GUID se ekzistas, alie indekso (kiel Varsovia Vento)
        val guid = ero.selectFirst("guid")?.text()
        val id = if (guid != null) "${kanal.slug}:$dato:$guid" else "${kanal.slug}:$dato:${indekso + 1}"

        return Elsendo(
            id = id,
            kanalSlug = kanal.slug,
            kanalNomo = kanal.nomo,
            titolo = finaTitolo,
            priskribo = puriguHtml(priskriboHtml.ifEmpty { priskribo }),
            bildUrl = bildUrl,
            dato = dato,
            dauro = dauro,
            stream = stream,
            retpaghoUrl = retpaghoUrl,
        )
    }

    // === Regulo 6.2 — Varsovia Vento ===

    private fun parsVarsoviaVento(doc: Document, kanal: Kanal): List<Elsendo> {
        val rezulto = mutableListOf<Elsendo>()
        for (ero in doc.select("item, entry")) {
            val titolo = ero.selectFirst("title")?.text() ?: ""
            val pubDate = ero.selectFirst("pubDate")?.text()
                ?: ero.selectFirst("published")?.text() ?: ""
            val dato = normigiDaton(pubDate) ?: continue

            // La <audio>-elementoj estas ene de HTML-CDATA en <content:encoded>.
            // html() redonas la CDATA-markon (<![CDATA[...]]>) — uzu text() por
            // akiri la malkoditan HTML-enhavon.
            val htmlEnhavo = ero.getElementsByTag("content:encoded").firstOrNull()?.text()
                ?: ero.getElementsByTag("description").firstOrNull()?.text()
                ?: ""
            val htmlDoc = Ksoup.parse(htmlEnhavo)
            val audioj = htmlDoc.select("audio source")
            if (audioj.isEmpty()) continue

            for ((i, source) in audioj.withIndex()) {
                val stream = source.attr("src")
                if (stream.isEmpty()) continue
                rezulto.add(Elsendo(
                    id = "${kanal.slug}:$dato:${i + 1}",
                    kanalSlug = kanal.slug,
                    kanalNomo = kanal.nomo,
                    titolo = "$titolo ${i + 1}a parto",
                    priskribo = puriguHtml(htmlEnhavo),
                    dato = dato,
                    stream = stream,
                ))
            }
        }
        return rezulto
    }

    // === Regulo 6.3 — Peranto (Esperanta Retradio) ===

    private fun parsPeranto(doc: Document, kanal: Kanal, httpKliento: suspend (String) -> String): List<Elsendo> {
        val malplenajDatoj = setOf("2019-11-08", "2019-09-29")

        return doc.select("entry").mapIndexedNotNull { i, ero ->
            val published = ero.selectFirst("published")?.text() ?: return@mapIndexedNotNull null
            val dato = published.substringBefore("T").takeIf { it.length >= 10 } ?: return@mapIndexedNotNull null

            // Saltu konatajn malplenajn datojn
            if (dato in malplenajDatoj) return@mapIndexedNotNull null

            // La enhavo estas HTML-eskapita en <content type='html'>
            val htmlEsprimite = ero.selectFirst("content")?.text() ?: ""
            // Malkodu HTML-entitojn
            val htmlDoc = Ksoup.parse(htmlEsprimite)

            // Eltiru bildon el la unua <img>
            val bildUrl = htmlDoc.selectFirst("img")?.attr("src")

            // Forigu <img>, <iframe>, <div class="separator"> el priskribo
            htmlDoc.select("img").remove()
            htmlDoc.select("iframe").remove()
            htmlDoc.select("div.separator").remove()
            val priskribo = htmlDoc.text()

            // Trovu la unuan <iframe src>
            val iframeSrc = Ksoup.parse(htmlEsprimite).selectFirst("iframe")?.attr("src") ?: ""

            // Rekonstruu la stream-URL el la iframe-src
            val stream = when {
                // archive.org/embed/<nomo> → archive.org/download/<nomo>/<nomo>.mp3
                iframeSrc.contains("archive.org/embed/") -> {
                    val nomo = iframeSrc.substringAfter("archive.org/embed/").substringBefore("?").trimEnd('/')
                    // Korekto: orkestro_sklavidojj → orkestro_sklavidoj
                    val korektitaNomo = if (nomo == "orkestro_sklavidojj") "orkestro_sklavidoj" else nomo
                    "https://archive.org/download/$korektitaNomo/$korektitaNomo.mp3"
                }
                // drive.google.com/file/d/<ID>/... → drive.google.com/u/1/uc?id=<ID>&export=download
                iframeSrc.contains("drive.google.com/file/d/") -> {
                    val id = iframeSrc.substringAfter("/file/d/").substringBefore("/")
                    "https://drive.google.com/u/1/uc?id=$id&export=download"
                }
                // Neniu iframe aŭ nesubtenata gastiganto → saltu
                iframeSrc.isEmpty() -> return@mapIndexedNotNull null
                iframeSrc.contains("youtube.com") || iframeSrc.contains("youtu.be") -> return@mapIndexedNotNull null
                iframeSrc.contains("soundcloud.com") -> return@mapIndexedNotNull null
                iframeSrc.contains("vimeo.com") -> return@mapIndexedNotNull null
                iframeSrc.contains("audioboom.com") -> return@mapIndexedNotNull null
                iframeSrc.contains("yourlisten.com") -> return@mapIndexedNotNull null
                iframeSrc.contains("vocaroo.com") -> return@mapIndexedNotNull null
                else -> return@mapIndexedNotNull null
            }

            val titolo = ero.selectFirst("title")?.text() ?: ""
            val retpaghoUrl = ero.selectFirst("link[rel=alternate]")?.attr("href")
            val guid = ero.selectFirst("id")?.text()
            val id = if (guid != null) "peranto:$dato:$guid" else "peranto:$dato:${i + 1}"

            Elsendo(
                id = id,
                kanalSlug = kanal.slug,
                kanalNomo = kanal.nomo,
                titolo = if (kanal.ignoruTitolon) puriguHtml(priskribo).take(200) else titolo,
                priskribo = priskribo,
                bildUrl = bildUrl,
                dato = dato,
                stream = stream,
                retpaghoUrl = retpaghoUrl,
            )
        }
    }

    // === Regulo 6.4 — Vinilkosmo ===

    private fun parsVinilkosmo(doc: Document, kanal: Kanal): List<Elsendo> =
        doc.select("entry").mapNotNull { ero ->
            val published = ero.selectFirst("published")?.text() ?: return@mapNotNull null
            val dato = published.substringBefore("T").takeIf { it.length >= 10 } ?: return@mapNotNull null

            val stream = ero.selectFirst("link[rel=enclosure][type=audio/mpeg]")?.attr("href")
                ?: return@mapNotNull null
            val bildUrl = ero.selectFirst("link[type=image/jpeg]")?.attr("href")
            val retpaghoUrl = ero.selectFirst("link[type=text/html]")?.attr("href")
            val priskribo = ero.selectFirst("content")?.html() ?: ""
            val id = "vk:${published.substringBefore("+").substringBefore("Z")}"

            Elsendo(
                id = id,
                kanalSlug = kanal.slug,
                kanalNomo = kanal.nomo,
                titolo = puriguHtml(priskribo).take(200),
                priskribo = puriguHtml(priskribo),
                bildUrl = bildUrl,
                dato = dato,
                stream = stream,
                retpaghoUrl = retpaghoUrl,
            )
        }

    // === Helpiloj ===

    fun normigiDaton(datStr: String?): String? {
        if (datStr.isNullOrBlank()) return null
        if (datStr.contains("T")) {
            val dato = datStr.substringBefore("T")
            if (dato.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return dato
        }
        val korektita = datStr.replace(Regex("(\\+\\d{2}):(\\d{2})$"), "$1$2")
        val monatoj = mapOf(
            "Jan" to "01", "Feb" to "02", "Mar" to "03", "Apr" to "04",
            "May" to "05", "Jun" to "06", "Jul" to "07", "Aug" to "08",
            "Sep" to "09", "Oct" to "10", "Nov" to "11", "Dec" to "12"
        )
        val m = Regex("(\\d{1,2})\\s+(\\w{3})\\s+(\\d{4})").find(korektita) ?: return null
        val tago = m.groupValues[1].padStart(2, '0')
        val monato = monatoj[m.groupValues[2]] ?: return null
        return "${m.groupValues[3]}-$monato-$tago"
    }

    fun leguDauron(teksto: String?): Long? {
        if (teksto.isNullOrBlank()) return null
        val partoj = teksto.trim().split(":")
        return when (partoj.size) {
            1 -> partoj[0].toLongOrNull()
            2 -> (partoj[0].toLongOrNull() ?: return null) * 60 + (partoj[1].toLongOrNull() ?: return null)
            3 -> (partoj[0].toLongOrNull() ?: return null) * 3600 + (partoj[1].toLongOrNull() ?: return null) * 60 + (partoj[2].toLongOrNull() ?: return null)
            else -> null
        }
    }

    private fun derivuTitolon(priskribo: String): String {
        val pura = puriguHtml(priskribo).replace("\n", " ").trim()
        return if (pura.length > 200) pura.take(200) else pura
    }

    fun puriguHtml(html: String): String = Ksoup.parse(html).text()
}
