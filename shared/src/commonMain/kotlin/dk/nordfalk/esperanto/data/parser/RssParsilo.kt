package dk.nordfalk.esperanto.data.parser

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import dk.nordfalk.esperanto.data.repository.ArchiveOrgDosiernomoKasho
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.logd
import dk.nordfalk.esperanto.loge
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.logw
import io.ktor.http.encodeURLPathPart
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * La ĝenerala RSS/Atom-parsilo. Traktas la sep parsregolojn.
 * Vidu docs/malnova/03_parsado_kaj_fontoj.md kaj docs/nova/04_parsado_kaj_arkivo.md.
 *
 * @param archiveOrgKasho persista kesto de archive.org-dosiernomoj (regulo 6.3);
 *   null = sen persisto (testoj)
 */
class RssParsilo(
    private val archiveOrgKasho: ArchiveOrgDosiernomoKasho? = null,
) {

    suspend fun parsuRss(
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
        val priskriboKruda = ero.selectFirst("content|encoded")?.text()
            ?: ero.selectFirst("description")?.text()
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

        val finaTitolo = if (kanalo.ignoruTitolon) derivuTitolon(priskriboKruda) else titolo

        // Unika ID: uzu GUID se ekzistas, alie indekso (kiel Varsovia Vento)
        val guid = ero.selectFirst("guid")?.text()
        val id = if (guid != null) "${kanalo.slug}:$dato:$guid" else "${kanalo.slug}:$dato:${indekso + 1}"

        return Elsendo(
            id = id,
            kanaloSlug = kanalo.slug,
            kanaloNomo = kanalo.nomo,
            titolo = finaTitolo,
            priskribo = puriguHtml(priskriboKruda),
            priskriboHtml = puriguHtmlKunEtikedojn(priskriboKruda),
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
                    priskriboHtml = puriguHtmlKunEtikedojn(htmlEnhavo),
                    dato = dato,
                    fluo = fluo,
                ))
            }
        }
        return rezulto
    }

    // === Regulo 6.3 — Peranto (Esperanta Retradio) ===

    /** Interrezulto de unu Peranto-ero dum la dufaza parsado. */
    private data class PerantoEro(
        val dato: String,
        val indekso: Int,
        val guid: String?,
        val titolo: String,
        val priskribo: String,
        val priskriboHtml: String,
        val bildoUrl: String?,
        val retpaghoUrl: String?,
        /** Identigilo de archive.org-arkivaĵo, aŭ null se la fluo venas de alia gastiganto */
        val arkivaIdentigilo: String?,
        /** Fluvo-URL; por archive.org unue nur divenita, korektata en paŝo 2 */
        val fluo: String,
    )

    private suspend fun parsuPeranto(doc: Document, kanalo: Kanalo, httpKliento: suspend (String) -> String): List<Elsendo> {
        val malplenajDatoj = setOf("2019-11-08", "2019-09-29")

        // Paŝo 1: kolektu la erojn. Por archive.org la MP3-dosiernomo estas nur
        // divenita (identigilo + ".mp3") — la vera nomo estas demandata en paŝo 2.
        val eroj = doc.select("entry").mapIndexedNotNull { i, ero ->
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

            var arkivaIdentigilo: String? = null
            val fluo = when {
                // archive.org/embed/<nomo> → archive.org/download/<nomo>/<nomo>.mp3 (divenite)
                iframeSrc.contains("archive.org/embed/") -> {
                    val nomo = iframeSrc.substringAfter("archive.org/embed/").substringBefore("?").trimEnd('/')
                    // Korekto: orkestro_sklavidojj → orkestro_sklavidoj
                    val korektitaNomo = if (nomo == "orkestro_sklavidojj") "orkestro_sklavidoj" else nomo
                    arkivaIdentigilo = korektitaNomo
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

            PerantoEro(
                dato = dato,
                indekso = i,
                guid = ero.selectFirst("id")?.text(),
                titolo = ero.selectFirst("title")?.text() ?: "",
                priskribo = priskribo,
                priskriboHtml = puriguHtmlKunEtikedojn(htmlEsprimite),
                bildoUrl = bildoUrl,
                retpaghoUrl = ero.selectFirst("link[rel=alternate]")?.attr("href"),
                arkivaIdentigilo = arkivaIdentigilo,
                fluo = fluo,
            )
        }

        // Paŝo 2: demandu archive.org pri la veraj MP3-dosiernomoj (paralele, unufoje po
        // identigilo). La dosiernomo ne ĉiam egalas al la identigilo: ekz. `malapero-benda-3`
        // enhavas `Malapero_Benda3.mp3` — la divenita URL tiam redonus HTTP 404.
        val identigiloj = eroj.mapNotNull { it.arkivaIdentigilo }.distinct()
        val dosiernomoj: Map<String, String> = if (identigiloj.isEmpty()) emptyMap() else coroutineScope {
            identigiloj.map { id -> async { id to trovuArchiveOrgMp3Dosiernomon(id, httpKliento) } }.awaitAll().toMap()
        }

        // Paŝo 3: konstruu la elsendojn kun la korektitaj fluo-URL-oj
        return eroj.map { ero ->
            val fluo = if (ero.arkivaIdentigilo != null) {
                val dosiernomo = dosiernomoj[ero.arkivaIdentigilo] ?: "${ero.arkivaIdentigilo}.mp3"
                "https://archive.org/download/${ero.arkivaIdentigilo}/${dosiernomo.encodeURLPathPart()}"
            } else ero.fluo
            val id = if (ero.guid != null) "peranto:${ero.dato}:${ero.guid}" else "peranto:${ero.dato}:${ero.indekso + 1}"

            Elsendo(
                id = id,
                kanaloSlug = kanalo.slug,
                kanaloNomo = kanalo.nomo,
                titolo = if (kanalo.ignoruTitolon) puriguHtml(ero.priskribo).take(200) else ero.titolo,
                priskribo = ero.priskribo,
                priskriboHtml = ero.priskriboHtml,
                bildoUrl = ero.bildoUrl,
                dato = ero.dato,
                fluo = fluo,
                retpaghoUrl = ero.retpaghoUrl,
            )
        }
    }

    /**
     * Trovas la veran MP3-dosiernomon ene de archive.org-arkivaĵo per la metadaten-API
     * (https://archive.org/metadata/&lt;identigilo&gt;). Se la metadatenoj ne estas
     * haveblaj (ekz. sen rete), retrofalas al la divenita nomo `<identigilo>.mp3`.
     */
    private suspend fun trovuArchiveOrgMp3Dosiernomon(identigilo: String, httpKliento: suspend (String) -> String): String {
        archiveOrgKasho?.leguDosiernomon(identigilo)?.let { return it }
        val divenita = "$identigilo.mp3"
        return try {
            val teksto = httpKliento("https://archive.org/metadata/$identigilo")
            val dosiernomo = eltiruMp3DosiernomonElMetadatenoj(teksto)
            if (dosiernomo == null) {
                logw("RssParsilo", "archive.org/$identigilo: neniu MP3 en metadatenoj — uzas divenitan nomon $divenita")
                return divenita
            }
            logi("RssParsilo", "archive.org/$identigilo: vera MP3-dosiernomo estas $dosiernomo")
            archiveOrgKasho?.konservuDosiernomon(identigilo, dosiernomo)
            dosiernomo
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // Throwable, ne Exception: la Js-motoro ĵetas kotlin.Error ĉe CORS/reteraroj.
            // Unu arkivaĵo ne devas panei la tutan kanal-fluon (regulo 4).
            logw("RssParsilo", "archive.org/$identigilo: metadaten-peto malsukcesa — uzas divenitan nomon $divenita", e)
            divenita
        }
    }

    /** Elektas la unuan originalan MP3-dosieron el archive.org-metadaten-JSON. */
    private fun eltiruMp3DosiernomonElMetadatenoj(teksto: String): String? {
        if (teksto.isBlank()) return null
        return try {
            val dosieroj = Json.parseToJsonElement(teksto).jsonObject["files"]?.jsonArray ?: return null
            var unuaMp3: String? = null
            for (dosiero in dosieroj) {
                val obj = dosiero.jsonObject
                val nomo = obj["name"]?.jsonPrimitive?.contentOrNull ?: continue
                if (!nomo.endsWith(".mp3", ignoreCase = true)) continue
                // Preferu originalan dosieron; derivaj (bildoj, spectrogramoj) ne taŭgas
                if (obj["source"]?.jsonPrimitive?.contentOrNull == "original") return nomo
                if (unuaMp3 == null) unuaMp3 = nomo
            }
            unuaMp3
        } catch (e: Exception) {
            loge("RssParsilo", "archive.org: malsukcesis analizi metadaten-JSON", e)
            null
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
            val priskriboKruda = ero.selectFirst("content")?.text() ?: ""
            val id = "vk:${published.substringBefore("+").substringBefore("Z")}"

            Elsendo(
                id = id,
                kanaloSlug = kanalo.slug,
                kanaloNomo = kanalo.nomo,
                titolo = puriguHtml(priskriboKruda).take(200),
                priskribo = puriguHtml(priskriboKruda),
                priskriboHtml = puriguHtmlKunEtikedojn(priskriboKruda),
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

    fun puriguHtml(html: String): String {
        val teksto = Ksoup.parse(html).text()
        // Se la rezulto ankoraŭ enhavas HTML-etikedojn (duoble-eskapita enhavo),
        // reprovu unu fojon
        return if (teksto.contains('<') && teksto.contains('>'))
            Ksoup.parse(teksto).text()
        else teksto
    }

    fun puriguHtmlKunEtikedojn(html: String): String {
        val doc = Ksoup.parse(html)
        // Forigu danĝerajn etikedojn
        doc.select("script, style, iframe, object, embed, form, input, button, meta, link").remove()
        // Forigu danĝerajn atributojn (event-handlers, javascript:-ligiloj)
        for (el in doc.select("*")) {
            for (attr in el.attributes().asList()) {
                if (attr.key.startsWith("on") || attr.value.trim().startsWith("javascript:")) {
                    el.removeAttr(attr.key)
                }
            }
        }
        return doc.body().html()
    }
}
