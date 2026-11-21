package dk.nordfalk.esperanto.data.repository

import kotlinx.coroutines.CancellationException
import dk.nordfalk.esperanto.data.parser.RssParsilo
import dk.nordfalk.esperanto.logd
import dk.nordfalk.esperanto.loge
import dk.nordfalk.esperanto.logw
import dk.nordfalk.esperanto.raportadoAktiva
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.repository.ElsendoDeponejo
import io.ktor.client.*
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.plugins.timeout
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.protocol.Breadcrumb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.concurrent.Volatile

/** Tempolimo por petoj al nia arkiva servilo (optimumigo, ne rajtas bloki horloĝon — vidu [ElsendoDeponejoImpl]). */
private const val ARKIVA_PETO_TEMROLIMITO_MS = 10_000L

/** Tempolimo por la originala fluo KIEL ALDONA fonto (apud la arkivo) — sola originalo uzas la defaŭltan de la kliento. */
private const val ORIGINALA_PETO_TEMROLIMITO_MS = 15_000L

/** Unu elŝutota fonto: la URL kaj ĉu ĝi estas nia arkiva fluo (deArkivo). */
private data class Fonto(val url: String, val deArkivo: Boolean)

/** Rezulto de unu fonto: sukceso (parsitaj elsendoj + kruda teksto por la kaŝo) aŭ fiasko. */
private sealed interface FontoRezulto {
    data class Sukceso(val fonto: Fonto, val elsendoj: List<Elsendo>, val kruda: String) : FontoRezulto
    data class Fiasko(val fonto: Fonto, val eraro: Throwable) : FontoRezulto
}

/**
 * ElsendoDeponejo-implentaĵo. Elŝutas RSS-fluojn per Ktor, parsas per RssParsilo,
 * kaŝenas en memoro (StateFlow). Tolerema: eraro → liveri kaŝenitan datumon.
 */
open class ElsendoDeponejoImpl(
    private val httpKliento: HttpClient,
    private val parsilo: RssParsilo = RssParsilo(),
    /** Bazo de nia arkiv-servilo (riĉigitaj fluoj). Se difinita: provi unue `<bazo><slug>.xml`, poste la originalan fluon. */
    private val arkivoBazoUrl: String? = null,
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
     * In-flight dedup (FAROTA C39): se ŝargo por kanalo jam daŭras (ekz.
     * malsupren-tiro dum aŭtomata ŝargo, aŭ alarmo dum ekran-ŝargo), dua
     * samtempa voko aliĝas al la sama [Deferred] anstataŭ elŝuti la saman
     * fluon duoble. La ero foriĝas post la fino, do la sekva voko denove
     * taksas la kaŝmemoron.
     */
    private val ŝargMutex = Mutex()
    private val ŝargadoj = mutableMapOf<String, Deferred<List<Elsendo>>>()

    /**
     * Skopo por la daŭranta ŝargo — sendependa de la vokanto, ke la elŝuto
     * finiĝu kaj plenigu la kaŝon eĉ se la unua vokanto (ekz. ekran-foriro)
     * estas nuligita. Default, ne IO — IO ne ekzistas sur wasmJs.
     */
    private val ŝargSkopo = CoroutineScope(Dispatchers.Default + SupervisorJob())

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

    /**
     * (FAROTA C38) Kaŝo-nur: redonas la en-memorajn elsendojn de la kanalo,
     * neniam elŝutas — por reta ŝargo/aktualigo uzu [ŝargiElsendojnPorKanal].
     * `fortoRefresigi` estas konservita nur por interfaca kongruo; ĝi ne ŝargas.
     */
    override suspend fun getElsendojn(kanaloSlug: String, fortoRefresigi: Boolean): List<Elsendo> =
        kaŝmemoro[kanaloSlug] ?: emptyList()

    /**
     * Elŝutas kaj parsas la RSS-fluon por specifa kanalo.
     * Tolerema: eraro → liveri kaŝenitan datumon, ne ĵeti.
     *
     * Samtempaj vokoj por la sama kanalo estas dedupataj (C39): ili dividas
     * unu in-flight [Deferred] kaj do unu HTTP-peton.
     */
    open suspend fun ŝargiElsendojn(kanalo: Kanalo, fortoRefresigi: Boolean = false): List<Elsendo> {
        // Arkivo unue (riĉigitaj fluoj kun AI-rezultoj), originala fluo kiel rezervo (FAROTA: arkivo-unue).
        // Fonto portas la flagon deArkivo eksplicite — neniom da URL-ĉena komparo poste.
        val fontoj = buildList {
            if (!arkivoBazoUrl.isNullOrBlank()) {
                add(Fonto(arkivoBazoUrl.trimEnd('/') + "/" + kanalo.slug + ".xml", deArkivo = true))
            }
            kanalo.podkastaRssUrl?.let { add(Fonto(it, deArkivo = false)) }
        }.distinctBy { it.url }
        if (fontoj.isEmpty()) {
            logw("ElsendoDeponejo", "${kanalo.slug}: neniu RSS-URL — saltas")
            return kaŝmemoro[kanalo.slug] ?: emptyList()
        }
        val kaŝenitaj = kaŝmemoro[kanalo.slug]

        if (kaŝenitaj != null && !fortoRefresigi) {
            logd("ElsendoDeponejo", "${kanalo.slug}: uzas kaŝenitan datumon (${kaŝenitaj.size} elsendoj)")
            return kaŝenitaj
        }

        val ŝargo = ŝargMutex.withLock {
            ŝargadoj[kanalo.slug]?.also {
                logd("ElsendoDeponejo", "${kanalo.slug}: aliĝas al daŭranta ŝargo anstataŭ duobla elŝuto")
            } ?: ŝargSkopo.async { elŝutuKajParsu(kanalo, fontoj, kaŝenitaj) }
                .also { nova ->
                    ŝargadoj[kanalo.slug] = nova
                    // Forigu la eron kiam la ŝargo FINIĜAS (ne kiam vokanto finiĝas —
                    // la unua vokanto povas esti nuligita dum la ŝargo daŭras).
                    // Nur nian propran eron, intertempe povas aperi pli nova.
                    nova.invokeOnCompletion {
                        ŝargSkopo.launch {
                            ŝargMutex.withLock {
                                if (ŝargadoj[kanalo.slug] === nova) ŝargadoj.remove(kanalo.slug)
                            }
                        }
                    }
                }
        }
        return try {
            ŝargo.await()
        } finally {
            // Forigu la eron se la ŝargo jam finiĝis (kutima vojo — la sekva voko
            // denove taksas la kaŝmemoron). Se la vokanto estis nuligita meze de
            // la ŝargo, la ero restas kaj invokeOnCompletion forigas ĝin ĉe la fino.
            ŝargMutex.withLock {
                if (ŝargo.isCompleted && ŝargadoj[kanalo.slug] === ŝargo) ŝargadoj.remove(kanalo.slug)
            }
        }
    }

    /**
     * La reta parto de [ŝargiElsendojn]: elŝutas ĈIUJN fontojn (arkivo + originalo) parolele
     * kaj kunfandas la rezultojn — la arkivo venkas ĉe konflikto (riĉa priskribo/titolo),
     * la originalo aldonas la novajn elsendojn, kiuj ankoraŭ ne estas en la arkivo.
     * Tolerema: fiasko de unu fonto ne haltigas la aliajn (regulo A4); se ĈIUJ fiaskas,
     * redonas la kaŝon — unue la enmemoran, poste la diskan.
     */
    private suspend fun elŝutuKajParsu(kanalo: Kanalo, fontoj: List<Fonto>, kaŝenitaj: List<Elsendo>?): List<Elsendo> = coroutineScope {
        val rezultoj = fontoj.map { fonto ->
            async {
                // Tempolimoj: la arkivo neniam rajtas bloki horloĝon (10 s); la originalo
                // kiel sola fonto uzas la defaŭltan de la kliento, kiel aldona fonto 15 s.
                val temrolimoMs = when {
                    fonto.deArkivo -> ARKIVA_PETO_TEMROLIMITO_MS
                    fontoj.size > 1 -> ORIGINALA_PETO_TEMROLIMITO_MS
                    else -> null
                }
                try {
                    val (elsendoj, kruda) = elŝutuKajParsuUnu(kanalo, fonto, temrolimoMs)
                    FontoRezulto.Sukceso(fonto, elsendoj, kruda)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    // Throwable, ne Exception: la retumila Ktor-motoro (Js) ĵetas kotlin.Error("Fail to fetch")
                    // ekz. ĉe CORS-blokado. Se ĝi eskapus, ĝi nuligus la ŝargadon de ĈIUJ aliaj kanaloj (regulo A4).
                    // logw (ne loge): retaj malsukcesoj estas ATENDATAJ (sen reto, kaptiva portalo) —
                    // ili devas NE iĝi erar-nivelaj Sentry-eventoj (PR #93).
                    logw("ElsendoDeponejo", "${kanalo.slug}: ${fonto.url} malsukcesa — uzos la aliajn fontojn / la kaŝon", e)
                    FontoRezulto.Fiasko(fonto, e)
                }
            }
        }.awaitAll()

        val sukcesoj = rezultoj.filterIsInstance<FontoRezulto.Sukceso>()
        val arkiva = sukcesoj.firstOrNull { it.fonto.deArkivo }
        val originala = sukcesoj.firstOrNull { !it.fonto.deArkivo }
        val kunfanditaj = when {
            arkiva != null && originala != null -> kunfanduElsendojn(arkiva.elsendoj, originala.elsendoj)
            arkiva != null -> arkiva.elsendoj
            else -> originala?.elsendoj
        }
        if (kunfanditaj != null) {
            // Diska kaŝo: la arkiva kruda teksto se sukcesa, alie la originala (unu dosiero po kanalo)
            skribuKaŝon(kanalo.slug, (arkiva ?: originala)!!.kruda)
            konservu(kanalo.slug, kunfanditaj)
            logd("ElsendoDeponejo", "${kanalo.slug}: parsado kompleta — ${kunfanditaj.size} elsendoj (fontoj: ${sukcesoj.size}/${fontoj.size})")
            return@coroutineScope kunfanditaj
        }
        // Ĉiuj fontoj malsukcesaj: unue enmemora kaŝo, poste diska kaŝo (regulo A4)
        val lastaEraro = rezultoj.filterIsInstance<FontoRezulto.Fiasko>().lastOrNull()?.eraro
        logw(
            "ElsendoDeponejo",
            "RSS-malsukcesa por ${kanalo.slug} — uzas la kaŝon",
            lastaEraro ?: IllegalStateException("Neniu fonto difinita por ${kanalo.slug}")
        )
        kaŝenitaj?.let { return@coroutineScope it }
        leguKaŝitajnElsendojn(kanalo) ?: emptyList()
    }

    private suspend fun elŝutuKajParsuUnu(kanalo: Kanalo, fonto: Fonto, temrolimoMs: Long?): Pair<List<Elsendo>, String> {
        logd("ElsendoDeponejo", "${kanalo.slug}: elŝutas RSS-fluon: ${fonto.url}")
        if (raportadoAktiva) Sentry.addBreadcrumb(Breadcrumb.http(fonto.url, "GET"))
        // expectSuccess=true: 4xx/5xx ĵetas escepton → la catch en elŝutuKajParsu. Sen tio,
        // fiaskpaĝo (HTML de 404/500) estus parsata kiel fluo kaj anstataŭigus la kaŝon.
        // leguKorponLimigite (G45): korpo pli granda ol la limo ne estas legata en la
        // memoron — misdirektita respondo (MP3 anstataŭ fluo) kaŭzis OutOfMemoryError.
        // Niaj arkivaj fluoj povas esti pli grandaj ol 1 MB (profunda historio, ekz.
        // peranto.xml ≈ 4,5 MB) — por ili ni permesas pli, ĉar ni regas la servilon.
        val limo = if (fonto.deArkivo) 8L * 1024 * 1024 else MAKSIMUMA_FLUA_GRANDECO_BITOKOJ
        val respondo = httpKliento.get(fonto.url) {
            expectSuccess = true
            if (temrolimoMs != null) timeout { requestTimeoutMillis = temrolimoMs }
        }.leguKorponLimigite(limo)
        kontroluFluecon(respondo)
        logd("ElsendoDeponejo", "${kanalo.slug}: RSS-elŝuto kompleta — ${respondo.length} signoj")
        val elsendoj = parsilo.parsuRss(respondo, kanalo, deArkivo = fonto.deArkivo, httpKliento = arhivaPeto)
            .ordigitajPlejFreŝajUnue()
        return elsendoj to respondo
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
        // Legado (bloka dosier-I/O) kaj parsado okazas sur Dispatchers.Default — vokoj
        // ofte venas de la ĉefa fadeno (HejmoViewModel), kaj granda arkiva kaŝo
        // (ekz. peranto ≈ 1,6 MB) blokus la unuan kadron dum pluraj sekundoj
        // (logcat 2026-11-21: "Skipped 213 frames", Davey 3779 ms).
        return withContext(Dispatchers.Default) {
            val respondo = leguKaŝon(kanalo.slug) ?: return@withContext null
            logd("ElsendoDeponejo", "${kanalo.slug}: legas diskan kaŝmemoron (${respondo.length} signoj)")
            try {
                // Mem-sangigo: kaŝoj skribitaj de antaŭ-riparaj versioj povas enhavi fiaskpaĝon
                // (HTTP 200 de kaptiva portalo) — ne re-parsu ĝin kiel fluon
                kontroluFluecon(respondo)
                // Detekto: niaj arkivaj fluoj skribas la finan id-on kiel guid kun prefikso
                // "<slug>:" (la servilo envolvas la guid en CDATA — tial la regexo)
                val elArkivo = Regex("<guid>(?:<!\\[CDATA\\[)?${Regex.escape(kanalo.slug)}:").containsMatchIn(respondo)
                val elsendoj = parsilo.parsuRss(respondo, kanalo, deArkivo = elArkivo, httpKliento = arhivaPeto).ordigitajPlejFreŝajUnue()
                konservu(kanalo.slug, elsendoj)
                logd("ElsendoDeponejo", "${kanalo.slug}: diska kaŝmemoro parsita — ${elsendoj.size} elsendoj")
                elsendoj
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // Rompita parsado de la loka kaŝdosiero (Sentry montris IllegalArgumentException
                // — do interna cimo, ne kaptiva portalo): forigu la malpuran kaŝon, alikaze
                // tiu fiasko ripetiĝus je ĉiu apomfermo (KMP-1S). loge: erar-evento, ĉar la
                // kaŝmemoro estas loka kaj la eraro do indikas kod-cimon.
                loge("ElsendoDeponejo", "${kanalo.slug}: malsukcesa re-parsado de diska kaŝmemoro — forigas la kaŝon", e)
                foriguKaŝon(kanalo.slug)
                null
            }
        }
    }

    /**
     * Legas ĉiujn kaŝitajn RSS-dosierojn por la donitaj kanaloj kaj re-parsas ilin.
     * Por ĉiu kanalo kun kaŝo, plenigas la en-memoran [kaŝmemoro]-n kaj [fluoj]-n.
     * Redonas ĉiujn elsendojn kune (plata listo).
     *
     * La kanaloj estas legataj PARALELE (ĉiu sur Dispatchers.Default ene de
     * [leguKaŝitajnElsendojn]) — sinsekva legado sur la vokanta fadeno (kutime la
     * ĉefa) blokus la unuan kadron dum ~3,5 s ĉe grandaj arkivaj kaŝoj.
     */
    suspend fun leguĈiujnKaŝitajnElsendojn(kanaloj: List<Kanalo>): List<Elsendo> = coroutineScope {
        kanaloj
            .filter { it.havasPodkastojn }
            .map { kanalo -> async { leguKaŝitajnElsendojn(kanalo) } }
            .awaitAll()
            .filterNotNull()
            .flatten()
            .also { ĉiuj ->
                logd("ElsendoDeponejo", "Diska kaŝmemoro: ${ĉiuj.size} elsendoj el ${kanaloj.count { it.havasPodkastojn }} kanaloj")
            }
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

    /** C60: grupigita serĉo — titolo-trovoj kaj nur-priskribo-trovoj en unu paŝo (partition). */
    override suspend fun serĉiElsendojnGrupigite(teksto: String, limo: Int): Pair<List<Elsendo>, List<Elsendo>> {
        val (titolo, resto) = kaŝmemoro.values.flatten()
            .partition { it.titolo.contains(teksto, ignoreCase = true) }
        return titolo.take(limo) to resto
            .filter { it.priskribo?.contains(teksto, ignoreCase = true) == true }
            .take(limo)
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
 * La dato estas "yyyy-MM-dd"-ĉeno, do leksikografia ordo estas kronologica ordo.
 * Ne ĉiuj fluoj estas plej-freŝe-unue (ekz. IRo listigas plej-malnovajn unue;
 * `LudvicoLogiko` kaj la kanal-vido supozas plej-freŝe-unue) — tial la deponejo
 * devigas la ordon ĉe unu loko.
 */
internal fun List<Elsendo>.ordigitajPlejFreŝajUnue(): List<Elsendo> = sortedByDescending { it.dato }

/**
 * Kunfandas arkivajn kaj originalajn elsendojn laŭ ID: la arkivo venkas ĉe konflikto
 * (riĉa HTML-priskribo, derivita titolo, solvitanaj URL-oj), la originalo aldonas nur
 * la novajn ID-ojn (elsendoj, kiuj ankoraŭ ne estas en la arkivo). Rezulto: plej-freŝe-unue.
 */
internal fun kunfanduElsendojn(arkivaj: List<Elsendo>, originalaj: List<Elsendo>): List<Elsendo> {
    val arkivajIdoj = arkivaj.map { it.id }.toSet()
    return (arkivaj + originalaj.filter { it.id !in arkivajIdoj }).ordigitajPlejFreŝajUnue()
}
