package dk.nordfalk.esperanto.data.parser

import com.fleeksoft.ksoup.Ksoup
import dk.nordfalk.esperanto.logd
import dk.nordfalk.esperanto.logw

/**
 * Parsilo por HTML-mediathekoj sen RSS — ekz. radio-frei.de (2026-11-22):
 * la CMS liveras RSS (`ksubmit_show=rss`) nur por la TUTA stacio kaj ignoras la
 * serĉan filtrilon `ssuchtext` en RSS-reĝimo, do la filtrita Esperanto-listo estas
 * akirebla nur kiel HTML. Nia arkiv-servilo tiras tiun liston kaj generas el ĝi
 * la arkivan fluon `kanaloj/<slug>.xml`.
 *
 * Strukturo de la list-paĝo: ĉiu artikolo estas ligilo kun `kartikel_id=<n>`;
 * la titolo estas la ligil-teksto, la dato aperas kiel `dd.mm.yyyy` en la sama ujo.
 * En la artikol-paĝo la unua `https://audio.radio-frei.de/....mp3`-URL estas la elsendo.
 */
class HtmlFontoParsilo {

    /** Ero el la HTML-listo — la fluo (mp3) mankas ankoraŭ, venas el la artikol-paĝo. */
    data class HtmlEro(
        val id: String,           // "<slug>:<karticleId>"
        val titolo: String,
        val dato: String?,        // yyyy-MM-dd
        val priskribo: String?,   // plata teksto el la list-paĝo
        val retpaĝoUrl: String,   // absoluta URL de la artikolo
    )

    private val kartilIdReg = Regex("""kartikel_id=(\d+)""")
    private val datoReg = Regex("""(\d{1,2})\.(\d{1,2})\.(\d{4})""")
    private val komentReg = Regex("""\|\s*\d+\s*Kommen\w*""")
    private val mp3Reg = Regex("""https?://[^\s"'<>]+\.mp3""", RegexOption.IGNORE_CASE)
    private val multSpacaReg = Regex("""\s+""")
    private val strekoReg = Regex("""[|·]""")

    /**
     * Parsas unu list-paĝon. [bazoUrl] estas la origina list-URL (por absolutigi
     * relativajn ligilojn). Redonas la erojn en la ordo de la paĝo (plej novaj unue).
     */
    fun parsuListon(html: String, kanaloSlug: String, bazoUrl: String): List<HtmlEro> {
        val doc = Ksoup.parse(html)
        val radiko = Regex("""^(https?://[^/]+)""").find(bazoUrl)?.groupValues?.get(1) ?: bazoUrl
        val rezulto = mutableListOf<HtmlEro>()
        val viditaj = mutableSetOf<String>()
        for (a in doc.select("a[href*=kartikel_id=]")) {
            val trovo = kartilIdReg.find(a.attr("href")) ?: continue
            val titolo = a.text().trim().replace(multSpacaReg, " ")
            if (titolo.isEmpty()) continue
            val eroId = "$kanaloSlug:${trovo.groupValues[1]}"
            if (!viditaj.add(eroId)) continue

            // dato kaj priskribo estas en la sama <li> kiel la ligilo (h3 > a, poste p.subject / p.datecomments)
            val ujo = generateSequence(a.parent()) { it?.parent() }.firstOrNull { it != null && it.tagName() == "li" }
                ?: a.parent()?.parent() ?: a.parent() ?: continue
            val ujoTeksto = ujo.text().replace(multSpacaReg, " ").trim()
            val datoTeksto = ujo.selectFirst("p.datecomments")?.text() ?: ujoTeksto
            val dato = datoReg.find(datoTeksto)?.let {
                "${it.groupValues[3]}-${it.groupValues[2].padStart(2, '0')}-${it.groupValues[1].padStart(2, '0')}"
            }
            val subjekto = ujo.selectFirst("p.subject")?.text()?.replace(multSpacaReg, " ")?.trim()
            var priskribo = if (!subjekto.isNullOrEmpty()) subjekto else ujoTeksto
                .removePrefix(titolo)
                .let { datoReg.replace(it, "") }
                .let { komentReg.replace(it, "") }
                .replace(strekoReg, " ")
                .trim()
            if (priskribo.length > 600) priskribo = priskribo.take(597).trimEnd() + "…"

            val href = a.attr("href")
            val retpaĝo = when {
                href.startsWith("http") -> href
                href.startsWith("/") -> radiko + href
                else -> "$radiko/$href"
            }
            rezulto.add(HtmlEro(eroId, titolo, dato, priskribo.ifEmpty { null }, retpaĝo))
        }
        logd("HtmlFontoParsilo", "$kanaloSlug: list-paĝo donis ${rezulto.size} erojn")
        return rezulto
    }

    /** Eltiru la unuan mp3-URL-on el artikol-paĝo (aŭ el list-paĝo, se tie estas). */
    fun eltiruFluon(html: String): String? {
        val trovo = mp3Reg.find(html)
        if (trovo == null) logw("HtmlFontoParsilo", "neniu mp3-URL trovita en la paĝo (${html.length} signoj)")
        return trovo?.value
    }
}
