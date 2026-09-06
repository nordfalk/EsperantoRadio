package dk.nordfalk.esperanto.data.parser

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.logd
import dk.nordfalk.esperanto.logi

/**
 * La ĝenerala RSS/Atom-parsilo. Traktas la sep parsregolojn.
 * Vidu docs/malnova/03_parsado_kaj_fontoj.md kaj docs/nova/04_parsado_kaj_arkivo.md.
 */
class RssParsilo {

    fun parsuRss(
        fluoTeksto: String,
        kanalo: Kanalo,
        httpKliento: suspend (String) -> String = { "" }
    ): List<Elsendo> {
        logd("RssParsilo", "Parsas RSS por ${kanalo.slug} — ${fluoTeksto.length} signoj")
        val doc = Ksoup.parseXml(fluoTeksto, "")
        val deArkivo = kanalo.podkastaRssUrl?.contains("podkasta_arkivo") == true

        val rezulto = if (deArkivo) {
            logd("RssParsilo", "${kanalo.slug}: uzas arkiv-parsilon")
            parsuGxenerala(doc, kanalo)
        } else when (kanalo.slug) {
            "varsoviavento" -> {
                logd("RssParsilo", "${kanalo.slug}: uzas VarsoviaVento-parsilon")
                parsuVarsoviaVento(doc, kanalo)
            }
            "peranto" -> {
                logd("RssParsilo", "${kanalo.slug}: uzas Peranto-parsilon")
                parsuPeranto(doc, kanalo, httpKliento)
            }
            "vinilkosmo" -> {
                logd("RssParsilo", "${kanalo.slug}: uzas Vinilkosmo-parsilon")
                parsuVinilkosmo(doc, kanalo)
            }
            else -> {
                logd("RssParsilo", "${kanalo.slug}: uzas ĝeneralan parsilon")
                parsuGxenerala(doc, kanalo)
            }
        }

        logi("RssParsilo", "${kanalo.slug}: parsado kompleta — ${rezulto.size} elsendoj")
        return rezulto
    }

    fun leguNextLink(fluoTeksto: String): String? {
        val doc = Ksoup.parseXml(fluoTeksto, "")
        return doc.selectFirst("atom|link[rel=next]")?.attr("href")
            ?: doc.selectFirst("link[rel=next]")?.attr("href")
    }

    // === Regulo 6.1 — Ĝenerala ===

    private fun parsuGxenerala(doc: Document, kanalo: Kanalo): List<Elsendo> =
        doc.select("item, entry").mapIndexedNotNull { i, ero -> parsuEroGxenerala(ero, kanalo, i) }

    private fun parsuEroGxenerala(ero: Element, kanalo: Kanalo, indekso: Int): Elsendo? {
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
        var fluo = ero.selectFirst("enclosure")?.attr("url") ?: ""
        if (fluo.isEmpty()) {
            fluo = ero.selectFirst("link[rel=enclosure]")?.attr("href") ?: ""
        }
        if (fluo.isEmpty()) {
            fluo = ero.selectFirst("audio source")?.attr("src") ?: ""
        }
        if (fluo.isEmpty()) return null

        if (kanalo.slug == "kernpunkto" && fluo.startsWith("http://")) {
            fluo = "https://" + fluo.removePrefix("http://")
        }

        val dauro = leguDauron(ero.selectFirst("itunes|duration")?.text())
        val bildoUrl = ero.selectFirst("itunes|image")?.attr("href")
            ?: ero.selectFirst("image url")?.text()
        val retpaghoUrl = ero.selectFirst("link")?.text()
            ?: ero.selectFirst("link")?.attr("href")

        val finaTitolo = if (kanalo.ignoruTitolon) derivuTitolon(priskribo) else titolo

        // Unika ID: uzu GUID se ekzistas, alie indekso (kiel Varsovia Vento)
        val guid = ero.selectFirst("guid")?.text()
        val id = if (guid != null) "${kanalo.slug}:$dato:$guid" else "${kanalo.slug}:$dato:${indekso + 1}"

        return Elsendo(
            id = id,
            kanaloSlug = kanalo.slug,
            kanaloNomo = kanalo.nomo,
            titolo = finaTitolo,
            priskribo = puriguHtml(priskriboHtml.ifEmpty { priskribo }),
            bildoUrl = bildoUrl,
            dato = dato,
            dauro = dauro,
            fluo = fluo,
            retpaghoUrl = retpaghoUrl,
        )
    }

    // === Regulo 6.2 — Varsovia Vento ===

    private fun parsuVarsoviaVento(doc: Document, kanalo: Kanalo): List<Elsendo> {
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
                val fluo = source.attr("src")
                if (fluo.isEmpty()) continue
                rezulto.add(Elsendo(
                    id = "${kanalo.slug}:$dato:${i + 1}",
                    kanaloSlug = kanalo.slug,
                    kanaloNomo = kanalo.nomo,
                    titolo = "$titolo ${i + 1}a parto",
                    priskribo = puriguHtml(htmlEnhavo),
                    dato = dato,
                    fluo = fluo,
                ))
            }
        }
        return rezulto
    }

    // === Regulo 6.3 — Peranto (Esperanta Retradio) ===

    private fun parsuPeranto(doc: Document, kanalo: Kanalo, httpKliento: suspend (String) -> String): List<Elsendo> {
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
            val bildoUrl = htmlDoc.selectFirst("img")?.attr("src")

            // Forigu <img>, <iframe>, <div class="separator"> el priskribo
            htmlDoc.select("img").remove()
            htmlDoc.select("iframe").remove()
            htmlDoc.select("div.separator").remove()
            val priskribo = htmlDoc.text()

            // Trovu la unuan <iframe src>
            val iframeSrc = Ksoup.parse(htmlEsprimite).selectFirst("iframe")?.attr("src") ?: ""

            // Rekonstruu la fluo-URL el la iframe-src
            val fluo = when {
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
                kanaloSlug = kanalo.slug,
                kanaloNomo = kanalo.nomo,
                titolo = if (kanalo.ignoruTitolon) puriguHtml(priskribo).take(200) else titolo,
                priskribo = priskribo,
                bildoUrl = bildoUrl,
                dato = dato,
                fluo = fluo,
                retpaghoUrl = retpaghoUrl,
            )
        }
    }

    // === Regulo 6.4 — Vinilkosmo ===

    private fun parsuVinilkosmo(doc: Document, kanalo: Kanalo): List<Elsendo> =
        doc.select("entry").mapNotNull { ero ->
            val published = ero.selectFirst("published")?.text() ?: return@mapNotNull null
            val dato = published.substringBefore("T").takeIf { it.length >= 10 } ?: return@mapNotNull null

            val fluo = ero.selectFirst("link[rel=enclosure][type=audio/mpeg]")?.attr("href")
                ?: return@mapNotNull null
            val bildoUrl = ero.selectFirst("link[type=image/jpeg]")?.attr("href")
            val retpaghoUrl = ero.selectFirst("link[type=text/html]")?.attr("href")
            val priskribo = ero.selectFirst("content")?.html() ?: ""
            val id = "vk:${published.substringBefore("+").substringBefore("Z")}"

            Elsendo(
                id = id,
                kanaloSlug = kanalo.slug,
                kanaloNomo = kanalo.nomo,
                titolo = puriguHtml(priskribo).take(200),
                priskribo = puriguHtml(priskribo),
                bildoUrl = bildoUrl,
                dato = dato,
                fluo = fluo,
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
