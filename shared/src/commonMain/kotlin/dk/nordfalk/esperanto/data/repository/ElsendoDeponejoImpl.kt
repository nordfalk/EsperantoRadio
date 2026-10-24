package dk.nordfalk.esperanto.data.repository

import kotlinx.coroutines.CancellationException
import dk.nordfalk.esperanto.data.parser.RssParsilo
import dk.nordfalk.esperanto.logd
import dk.nordfalk.esperanto.logw
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.repository.ElsendoDeponejo
import io.ktor.client.*
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.protocol.Breadcrumb
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.Volatile

/**
 * ElsendoDeponejo-implentaĵo. Elŝutas RSS-fluojn per Ktor, parsas per RssParsilo,
 * kaŝenas en memoro (StateFlow). Tolerema: eraro → liveri kaŝenitan datumon.
 */
open class ElsendoDeponejoImpl(
    private val httpKliento: HttpClient,
    private val parsilo: RssParsilo = RssParsilo(),
) : ElsendoDeponejo {

    /**
     * Nemutablaj momentfotoj anstataŭ mutableMapOf (PR #91): la mapoj estas
     * legitaj (getElsendo/serĉi) dum alia korutino skribas — kun mutable mapo
     * tio estis ConcurrentModificationException-risko. Skriboj nur per [konservu]
     * (Mutex); legoj estas sekuraj ĉar la momentfoto neniam ŝanĝiĝas surloke.
     */
    @Volatile protected var kaŝmemoro: Map<String, List<Elsendo>> = emptyMap()
    @Volatile protected var fluoj: Map<String, MutableStateFlow<List<Elsendo>>> = emptyMap()
    private val mapMutex = Mutex()

    /**
     * Skribas la kanal-rezulton atome en la memoran kaŝon kaj la fluon.
     * La nura vojo por mutacii la mapojn (ankaŭ por subklasoj).
     */
    protected suspend fun konservu(kanaloSlug: String, elsendoj: List<Elsendo>) {
        mapMutex.withLock {
            kaŝmemoro = kaŝmemoro + (kanaloSlug to elsendoj)
            val fluo = fluoj[kanaloSlug] ?: MutableStateFlow<List<Elsendo>>(emptyList()).also {
                fluoj = fluoj + (kanaloSlug to it)
            }
            fluo.value = elsendoj
        }
    }

    /**
     * Peto pri la metadatumoj de archive.org (regulo P6.3) — ankaŭ uzata ĉe la re-parsado
     * de la diska kaŝo: sen ĝi la Peranto-elsendoj ricevus DIVENITAJN (ofte 404-ajn)
     * dosiernomojn ĝis la unua sukcesa reta refreŝo (PR #91).
     */
    private val arhivaPeto: suspend (String) -> String = { urlD ->
        httpKliento.get(urlD) { expectSuccess = true }.bodyAsText()
    }

    override fun observiElsendojn(kanaloSlug: String): StateFlow<List<Elsendo>> {
        fluoj[kanaloSlug]?.let { return it.asStateFlow() }
        // Unua observo antaŭ ol ion ŝargis: kreu la fluon senŝlosile (ne-suspenda
        // funkcio). La sekvanta konservu() trovas kaj uzas ĝin. Du samtempaj unuaj
        // observoj de la sama kanalo estas sensignifa raso (nenia korupto — @Volatile
        // momentfoto), sed ne povas okazi ĉi tie.
        val nova = MutableStateFlow(kaŝmemoro[kanaloSlug] ?: emptyList())
        fluoj = fluoj + (kanaloSlug to nova)
        return nova.asStateFlow()
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
    open suspend fun ŝargiElsendojn(kanalo: Kanalo, fortoRefresigi: Boolean = false): List<Elsendo> {
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
            logd("ElsendoDeponejo", "${kanalo.slug}: elŝutas RSS-fluon: $url")
            Sentry.addBreadcrumb(Breadcrumb.http(url, "GET"))
            // expectSuccess=true: 4xx/5xx ĵetas escepton → la catch sube redonas la kaŝitan
            // datumon. Sen tio, fiaskpaĝo (HTML de 404/500) estus parsata kiel fluo kaj
            // anstataŭigus kaj la enmemoran kaj la diskan kaŝon per malplena enhavo.
            // leguKorponLimigite (G45): korpo pli granda ol 1 MB ne estas legata en la
            // memoron — misdirektita respondo (MP3 anstataŭ fluo) kaŭzis OutOfMemoryError.
            val respondo = httpKliento.get(url) { expectSuccess = true }.leguKorponLimigite()
            kontroluFluecon(respondo)
            logd("ElsendoDeponejo", "${kanalo.slug}: RSS-elŝuto kompleta — ${respondo.length} signoj")
            skribuKaŝon(kanalo.slug, respondo)
            val elsendoj = parsilo.parsuRss(respondo, kanalo, arhivaPeto)
                .ordigitajPlejFreŝajUnue()
            logd("ElsendoDeponejo", "${kanalo.slug}: parsado kompleta — ${elsendoj.size} elsendoj")
            konservu(kanalo.slug, elsendoj)
            elsendoj
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // Throwable, ne Exception: la retumila Ktor-motoro (Js) ĵetas kotlin.Error("Fail to fetch")
            // ekz. ĉe CORS-blokado. Se ĝi eskapus, ĝi nuligus la ŝargadon de ĈIUJ aliaj kanaloj (regulo A4).
            // logw (ne loge): retaj malsukcesoj estas ATENDATAJ (sen reto, kaptiva portalo) —
            // ili devas NE iĝi erar-nivelaj Sentry-eventoj ((PR #93) antaŭe la sama fiasko
            // estis kaptita dufoje — loge→captureException + captureMessage).
            logw(
                "ElsendoDeponejo",
                "RSS-malsukcesa por ${kanalo.slug} — uzas ${kaŝenitaj?.size ?: 0} kaŝenitajn elsendojn",
                e
            )
            // Toleremeco: liveri kaŝenitan datumon se haveblan
            kaŝenitaj ?: emptyList()
        }
    }

    override suspend fun ŝargiElsendojnPorKanal(kanalo: Kanalo, fortoRefresigi: Boolean): List<Elsendo> =
        ŝargiElsendojn(kanalo, fortoRefresigi)

    /**
     * Legas la krudan RSS-tekston el diska kaŝmemoro kaj re-parsas ĝin.
     * Se neniu kaŝo ekzistas, revenigas null.
     * Ankaŭ plenigas la en-memoran [kaŝmemoro]-n kaj [fluoj]-n.
     */
    suspend fun leguKaŝitajnElsendojn(kanalo: Kanalo): List<Elsendo>? {
        // La en-memora kaŝo jam havas la kanalon (el aŭ disko aŭ reto) — ne
        // re-legu kaj re-parsu la diskon senbezone (KMP-esplorado 2026-10-20:
        // la ekranoj vokas tion je ĉiu eniro).
        kaŝmemoro[kanalo.slug]?.let { return it }
        val respondo = leguKaŝon(kanalo.slug) ?: return null
        logd("ElsendoDeponejo", "${kanalo.slug}: legas diskan kaŝmemoron (${respondo.length} signoj)")
        return try {
            // Mem-sangigo: kaŝoj skribitaj de antaŭ-riparaj versioj povas enhavi fiaskpaĝon
            // (HTTP 200 de kaptiva portalo) — ne re-parsu ĝin kiel fluon
            kontroluFluecon(respondo)
            val elsendoj = parsilo.parsuRss(respondo, kanalo, arhivaPeto).ordigitajPlejFreŝajUnue()
            konservu(kanalo.slug, elsendoj)
            logd("ElsendoDeponejo", "${kanalo.slug}: diska kaŝmemoro parsita — ${elsendoj.size} elsendoj")
            elsendoj
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // Fiaskpaĝo (kaptiva portalo) aŭ rompita parsado: forigu la malpuran
            // kaŝon, alikaze tiu fiasko ripetiĝus je ĉiu apomfermo (KMP-1S).
            // logw (ne loge): tolerita, atendebla situacio — ne erar-evento.
            logw("ElsendoDeponejo", "${kanalo.slug}: malsukcesis re-parsi diskan kaŝmemoron — forigas la kaŝon", e)
            foriguKaŝon(kanalo.slug)
            null
        }
    }

    /**
     * Legas ĉiujn kaŝitajn RSS-dosierojn por la donitaj kanaloj kaj re-parsas ilin.
     * Por ĉiu kanalo kun kaŝo, plenigas la en-memoran [kaŝmemoro]-n kaj [fluoj]-n.
     * Redonas ĉiujn elsendojn kune (plata listo).
     */
    suspend fun leguĈiujnKaŝitajnElsendojn(kanaloj: List<Kanalo>): List<Elsendo> {
        val ĉiuj = mutableListOf<Elsendo>()
        for (kanalo in kanaloj) {
            if (!kanalo.havasPodkastojn) continue
            leguKaŝitajnElsendojn(kanalo)?.let { ĉiuj.addAll(it) }
        }
        logd("ElsendoDeponejo", "Diska kaŝmemoro: ${ĉiuj.size} elsendoj el ${kanaloj.count { it.havasPodkastojn }} kanaloj")
        return ĉiuj
    }

    override suspend fun getElsendo(id: String): Elsendo? {
        for ((_, elsendoj) in kaŝmemoro) {
            val e = elsendoj.find { it.id == id }
            if (e != null) return e
        }
        return null
    }

    override suspend fun serĉiElsendojn(teksto: String, limo: Int): List<Elsendo> {
        val ĉiuj = kaŝmemoro.values.flatten()
        val rezulto = ĉiuj.filter {
            it.titolo.contains(teksto, ignoreCase = true) ||
            (it.priskribo?.contains(teksto, ignoreCase = true) ?: false)
        }.take(limo)
        logd("ElsendoDeponejo", "Serĉas '$teksto' en ${ĉiuj.size} elsendoj — ${rezulto.size} trovoj")
        return rezulto
    }

    /**
     * Kontrolas ke la respondo aspektas kiel RSS/Atom-fluo. Fiaskpaĝo kun HTTP 200
     * (prokura servilo, kaptiva portalo) ne devas anstataŭigi la kaŝon.
     * Ĵetas [IllegalArgumentException] se ne — la vokanto kaptas kaj konservas la kaŝon.
     */
    private fun kontroluFluecon(respondo: String) {
        val malalta = respondo.lowercase()
        require("<rss" in malalta || "<feed" in malalta || "<rdf" in malalta) {
            "La respondo ne estas RSS/Atom-fluo"
        }
    }
}

/**
 * Ordigas elsendojn plej-freŝe-unue.
 *
 * La dato estas "yyyy-MM-dd"-ĉeno, do leksikografia ordo estas kronologia ordo.
 * Ne ĉiuj fluoj estas plej-freŝe-unue (ekz. IRo listigas plej-malnovajn unue;
 * `LudvicoLogiko` kaj la kanal-vido supozas plej-freŝe-unue) — tial la deponejo
 * devigas la ordon ĉe unu loko.
 */
internal fun List<Elsendo>.ordigitajPlejFreŝajUnue(): List<Elsendo> = sortedByDescending { it.dato }
