package dk.nordfalk.esperanto.data.repository

import kotlinx.coroutines.CancellationException
import dk.nordfalk.esperanto.data.parser.RssParsilo
import dk.nordfalk.esperanto.logd
import dk.nordfalk.esperanto.loge
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.logw
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.repository.ElsendoDeponejo
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.SentryLevel
import io.sentry.kotlin.multiplatform.protocol.Breadcrumb
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ElsendoDeponejo-implentaĵo. Elŝutas RSS-fluojn per Ktor, parsas per RssParsilo,
 * kaŝenas en memoro (StateFlow). Tolerema: eraro → liveri kaŝenitan datumon.
 */
open class ElsendoDeponejoImpl(
    private val httpKliento: HttpClient,
    private val parsilo: RssParsilo = RssParsilo(),
) : ElsendoDeponejo {

    protected val kaŝmemoro = mutableMapOf<String, List<Elsendo>>()
    protected val fluoj = mutableMapOf<String, MutableStateFlow<List<Elsendo>>>()

    override fun observiElsendojn(kanaloSlug: String): StateFlow<List<Elsendo>> {
        return fluoj.getOrPut(kanaloSlug) { MutableStateFlow(kaŝmemoro[kanaloSlug] ?: emptyList()) }.asStateFlow()
    }

    override suspend fun getElsendojn(kanaloSlug: String, fortoRefresigi: Boolean): List<Elsendo> {
        val kaŝenitaj = kaŝmemoro[kanaloSlug]
        if (kaŝenitaj != null && !fortoRefresigi) {
            return kaŝenitaj
        }
        // Ni bezonas la kanalo-URL por elŝuti. La kanalo estas provizita ekstere.
        // Tiu metodon estos vokita kun la kanalo-URL jam konata.
        return kaŝenitaj ?: emptyList()
    }

    /**
     * Elŝutas kaj parsas la RSS-fluon por specifa kanalo.
     * Tolerema: eraro → liveri kaŝenitan datumon, ne ĵeti.
     */
    open suspend fun sxargxiElsendojn(kanalo: Kanalo, fortoRefresigi: Boolean = false): List<Elsendo> {
        val url = kanalo.podkastaRssUrl ?: run {
            logw("ElsendoDeponejo", "${kanalo.slug}: neniu RSS-URL — saltas")
            return kaŝmemoro[kanalo.slug] ?: emptyList()
        }
        val kaŝenitaj = kaŝmemoro[kanalo.slug]

        if (kaŝenitaj != null && !fortoRefresigi) {
            logd("ElsendoDeponejo", "${kanalo.slug}: uzas kaŝenitan datumon (${kaŝenitaj.size} elsendoj)")
            return kaŝenitaj
        }

        return try {
            logi("ElsendoDeponejo", "${kanalo.slug}: elŝutas RSS-fluon: $url")
            Sentry.addBreadcrumb(Breadcrumb.http(url, "GET"))
            val respondo = httpKliento.get(url).bodyAsText()
            logi("ElsendoDeponejo", "${kanalo.slug}: RSS-elŝuto kompleta — ${respondo.length} signoj")
            skribuKashon(kanalo.slug, respondo)
            val elsendoj = parsilo.parsuRss(respondo, kanalo) { urlD ->
                httpKliento.get(urlD).bodyAsText()
            }
            logi("ElsendoDeponejo", "${kanalo.slug}: parsado kompleta — ${elsendoj.size} elsendoj")
            kaŝmemoro[kanalo.slug] = elsendoj
            fluoj.getOrPut(kanalo.slug) { MutableStateFlow(emptyList()) }.value = elsendoj
            elsendoj
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // Throwable, ne Exception: la retumila Ktor-motoro (Js) ĵetas kotlin.Error("Fail to fetch")
            // ekz. ĉe CORS-blokado. Se ĝi eskapus, ĝi nuligus la ŝargadon de ĈIUJ aliaj kanaloj (regulo 4).
            loge("ElsendoDeponejo", "${kanalo.slug}: RSS-elŝuto malsukcesa", e)
            // Raportu kiel averto (ne eraro) — la apo daŭrigas kun kaŝenita datumo
            Sentry.captureMessage(
                "RSS-malsukcesa por ${kanalo.slug} — uzas ${kaŝenitaj?.size ?: 0} kaŝenitajn elsendojn"
            ) { scope ->
                scope.level = SentryLevel.WARNING
                scope.setTag("kanalo", kanalo.slug)
            }
            // Toleremeco: liveri kaŝenitan datumon se haveblan
            kaŝenitaj ?: emptyList()
        }
    }

    override suspend fun sxargxiElsendojnPorKanal(kanalo: Kanalo, fortoRefresigi: Boolean): List<Elsendo> =
        sxargxiElsendojn(kanalo, fortoRefresigi)

    /**
     * Legas la krudan RSS-tekston el diskkaŝmemoro kaj re-parsas ĝin.
     * Se neniu kaŝo ekzistas, revenigas null.
     * Ankaŭ plenigas la en-memoran [kaŝmemoro]-n kaj [fluoj]-n.
     */
    fun leguKashitajnElsendojn(kanalo: Kanalo): List<Elsendo>? {
        val respondo = leguKashon(kanalo.slug) ?: return null
        logi("ElsendoDeponejo", "${kanalo.slug}: legas diskkaŝmemoron (${respondo.length} signoj)")
        return try {
            val elsendoj = parsilo.parsuRss(respondo, kanalo)
            kaŝmemoro[kanalo.slug] = elsendoj
            fluoj.getOrPut(kanalo.slug) { MutableStateFlow(emptyList()) }.value = elsendoj
            logi("ElsendoDeponejo", "${kanalo.slug}: diskkaŝmemoro parsita — ${elsendoj.size} elsendoj")
            elsendoj
        } catch (e: Exception) {
            loge("ElsendoDeponejo", "${kanalo.slug}: malsukcesis re-parsi diskkaŝmemoron", e)
            null
        }
    }

    /**
     * Legas ĉiujn kaŝitajn RSS-dosierojn por la donitaj kanaloj kaj re-parsas ilin.
     * Por ĉiu kanalo kun kaŝo, plenigas la en-memoran [kaŝmemoro]-n kaj [fluoj]-n.
     * Redonas ĉiujn elsendojn kune (flat list).
     */
    fun leguĈiujnKashitajnElsendojn(kanaloj: List<Kanalo>): List<Elsendo> {
        val ĉiuj = mutableListOf<Elsendo>()
        for (kanalo in kanaloj) {
            if (!kanalo.havasPodkastojn) continue
            leguKashitajnElsendojn(kanalo)?.let { ĉiuj.addAll(it) }
        }
        logi("ElsendoDeponejo", "Diskkaŝmemoro: ${ĉiuj.size} elsendoj el ${kanaloj.count { it.havasPodkastojn }} kanaloj")
        return ĉiuj
    }

    override suspend fun getElsendo(id: String): Elsendo? {
        for ((_, elsendoj) in kaŝmemoro) {
            val e = elsendoj.find { it.id == id }
            if (e != null) return e
        }
        return null
    }

    override suspend fun sercxiElsendojn(teksto: String, limo: Int): List<Elsendo> {
        val ĉiuj = kaŝmemoro.values.flatten()
        val rezulto = ĉiuj.filter {
            it.titolo.contains(teksto, ignoreCase = true) ||
            (it.priskribo?.contains(teksto, ignoreCase = true) ?: false)
        }.take(limo)
        logi("ElsendoDeponejo", "Serĉas '$teksto' en ${ĉiuj.size} elsendoj — ${rezulto.size} trovoj")
        return rezulto
    }
}
