package dk.nordfalk.esperanto.data.repository

import com.russhwolf.settings.Settings
import dk.nordfalk.esperanto.data.config.KanalAgordo
import dk.nordfalk.esperanto.data.config.KanalAgordoLeganto
import dk.nordfalk.esperanto.data.config.alKanalo
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.repository.KanaloDeponejo
import dk.nordfalk.esperanto.loge
import dk.nordfalk.esperanto.logd
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.logw
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.concurrent.Volatile
import kotlin.time.Clock

/**
 * KanaloDeponejo-implentaĵo. Kiel la malnova apo (`Backend.getGrunddataUrl` +
 * `App.initData`/`hentEvtNyeGrunddata`):
 *
 * 1. Ĉe relanĉo oni unue legas la konservitan (elŝutitan el la reto) konfiguron
 *    el la DISKA kaŝmemoro, kaj nur se ĝi mankas — la pakitan version el la
 *    resurco (tiel la apo funkcias tuj kaj ankaŭ sen reto);
 * 2. poste en fono oni elŝutas la nunan konfiguron el [KONFIGURO_URL_DE_RETO]
 *    (kun TTL [KONFIGURO_REFREŜA_INTERVALO_MS], kiel `intervals.settings` en la
 *    malnova apo) kaj uzas ĝin — SED nur se la parsado sukcesis kaj la kanal-listo
 *    ne estas malplena, por ke rompita respondo ne anstataŭigu la sanan konservitan
 *    version (A4). Sukcesa freŝa agordo anstataŭigas la kanalliston vive kaj
 *    vokas [alFreŝaAgordo] (ĝisdatigo de apaj novaĵoj, prizorganto-retpoŝto k.a.).
 *
 * ATENTU pri la persistigo: la konfiguro (~21 KB) NE iru en `Settings` — sur
 * Desktop la java.util.prefs-realigo rifuzas valorojn super ~8 KB per
 * `IllegalArgumentException: Value too long` (malkovrite 2026-11-22). Tial la
 * konservita teksto estas dosiero en la diska kaŝmemoro ([KANALKONFIGURO_KAŜNOMO],
 * `leguKaŝonJson`/`skribuKaŝonJson` — la sama mekanismo kiel la RSS-kaŝoj).
 */
class KanaloDeponejoImpl(
    private val leganto: KanalAgordoLeganto = KanalAgordoLeganto(),
    private val paktitaTeksto: () -> String,
    /** Elŝutas la konfiguran tekston de URL; ĵetas ĉe ret-fiasko. Null = neniu reta refreŝo (testoj,antaŭrigardoj). */
    private val elŝutuDefore: (suspend (url: String) -> String)? = null,
    /** Settings — uzata nur por la TTL-tempo (malgranda valoro; la konfiguro mem NE iras tien). */
    private val settings: Settings? = null,
    /** Legas la persistigitan konfiguran tekston (defaŭlte el la diska kaŝmemoro). */
    private val leguKonservitan: () -> String? = { leguKaŝonJson(KANALKONFIGURO_KAŜNOMO) },
    /** Skribas la persistigitan konfiguran tekston. */
    private val skribuKonservitan: (String) -> Unit = { skribuKaŝonJson(KANALKONFIGURO_KAŜNOMO, it) },
    /** Forigas putritan konservitan tekston. */
    private val foriguKonservitan: () -> Unit = { foriguKaŝon(KANALKONFIGURO_KAŜNOMO) },
    private val konfiguroUrl: String = KONFIGURO_URL_DE_RETO,
    private val refreŝaIntervaloMs: Long = KONFIGURO_REFREŜA_INTERVALO_MS,
    /** Vokata kiam freŝa, sukcese parsita defora agordo anstataŭigis la malnovan. */
    private val alFreŝaAgordo: (KanalAgordo) -> Unit = {},
) : KanaloDeponejo {

    companion object {
        /** Nuna kanalkonfiguro sur nia servilo (publika, komentoj jam striptitaj — vidu la skill-on `servilo-arkivo`). */
        const val KONFIGURO_URL_DE_RETO = "https://javabog.dk/esperanto-radio/esperantoradio_konfiguro.json"
        /** Kaŝnomo de la persistigita defora konfiguro en la diska kaŝmemoro (`defora_kanalkonfiguro.json`). */
        const val KANALKONFIGURO_KAŜNOMO = "defora_kanalkonfiguro"
        /** TTL por la reta kontrolo de la konfiguro — same kiel `intervals.settings` (1800 s) en la malnova apo. */
        const val KONFIGURO_REFREŜA_INTERVALO_MS = 30L * 60 * 1000
        /** Tempolimo por la elŝuto de la konfiguro (malgranda dosiero, sed ne rajtas bloki). */
        const val KONFIGURO_PETO_TEMPOLIMITO_MS = 15_000L

        /**
         * Kreas la elŝutanton kun la necesa higieno: `expectSuccess` (4xx/5xx ĵetu,
         * por ke fiaskpaĝo ne estu provata kiel konfiguro), tempolimo kaj
         * grandec-limo (G45 — `leguKorponLimigite` kontraŭ misdirektitaj respondoj).
         */
        fun kreuElŝutanton(httpKliento: HttpClient): suspend (String) -> String = { url ->
            httpKliento.get(url) {
                expectSuccess = true
                timeout { requestTimeoutMillis = KONFIGURO_PETO_TEMPOLIMITO_MS }
            }.leguKorponLimigite()
        }
    }

    @Volatile
    private var agordo: KanalAgordo? = null

    private val _kanaloj = MutableStateFlow<List<Kanalo>>(emptyList())
    override fun observiKanalojn(): StateFlow<List<Kanalo>> = _kanaloj.asStateFlow()

    /** Skopo por la fona reta refreŝo — Default, ne IO (IO ne ekzistas sur wasmJs). */
    private val refreŝSkopo = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * La efektive uzata agordo (konservita el la reto, aliokaze pakita).
     * Nesuspenda — por [dk.nordfalk.esperanto.AppStato.inicialigu]; la unuan fojon
     * ĝi legas kaj parsas la lokan tekston (kiel la malnova apo en `initData`).
     */
    fun getAgordo(): KanalAgordo = agordo ?: ŝarguLokanAgordon()

    // (parametro nomita `devige` — vidu la interfacon KanaloDeponejo por la nomita argumento)
    override suspend fun getKanalojn(devige: Boolean): List<Kanalo> {
        val estisMalvarma = _kanaloj.value.isEmpty()
        if (!estisMalvarma && !devige) {
            logd("KanaloDeponejo", "Kanaloj jam ŝargitaj (${_kanaloj.value.size}) — uzas kaŝon")
            return _kanaloj.value
        }
        ŝarguLokanAgordon()
        // Malvarma startigo aŭ eksplicita refreŝo (malsupren-tiro): kontrolu en
        // fono ĉu la servilo havas pli novan konfiguron (TTL ene de [refreŝiguElRete]).
        if (elŝutuDefore != null && (devige || estisMalvarma)) {
            refreŝSkopo.launch { refreŝiguElRete(devige = devige) }
        }
        return _kanaloj.value
    }

    override suspend fun getKanalo(slug: String): Kanalo? = getKanalojn(devige = false).find { it.slug == slug }

    /**
     * Legas la lokan (konservitan en la diska kaŝmemoro, aliokaze pakitan) konfiguron
     * kaj plenigas [agordo] kaj [_kanaloj]. Tolerema (A4): rompita kaŝo → forgeso +
     * rezervo al la pakita; ambaŭ rompita → malplena agordo (neniu kraŝo).
     */
    private fun ŝarguLokanAgordon(): KanalAgordo {
        val ŝargita = leguLokanKanalkonfiguron(leganto, leguKonservitan, foriguKonservitan, paktitaTeksto)
        agordo = ŝargita
        _kanaloj.value = ŝargita.kanaloj.map { it.alKanalo() }
        logi("KanaloDeponejo", "Ŝargis ${ŝargita.kanaloj.size} kanalojn el la loka konfiguro")
        return ŝargita
    }

    /**
     * Elŝutas la nunan kanalkonfiguron el [konfiguroUrl] kaj — nur se ĝi parseblas
     * kaj havas kanalojn — konservas ĝin en la diska kaŝmemoro, anstataŭigas la
     * kanalliston kaj vokas [alFreŝaAgordo] (kiel la malnova apo:
     * `hentEvtNyeGrunddata` konservis nur post sukcesa parsado).
     *
     * TTL: sen `devige` kaj kun [settings] la elŝuto okazas maks. unufoje je
     * [refreŝaIntervaloMs] (la tempo de la lasta kontrolo memoriĝas — ankaŭ ĉe
     * fiasko, kiel en la malnova apo, por ne bombi la servilon).
     */
    suspend fun refreŝiguElRete(devige: Boolean = false) {
        val elŝutanto = elŝutuDefore ?: return
        val st = settings
        if (st != null) {
            val nun = Clock.System.now().toEpochMilliseconds()
            val lasta = st.getLong(SettingsKeys.LASATA_KONFIGURO_KONTROLO, 0L)
            if (!devige && nun - lasta < refreŝaIntervaloMs) {
                logd("KanaloDeponejo", "Reta konfiguro kontrolita antaŭ ${nun - lasta} ms — ene de la TTL, saltetas")
                return
            }
            st.putLong(SettingsKeys.LASATA_KONFIGURO_KONTROLO, nun)
        }
        try {
            val teksto = elŝutanto(konfiguroUrl)
            if (teksto.isBlank()) {
                logw("KanaloDeponejo", "La reta kanalkonfiguro estas malplena — konservas la lokan")
                return
            }
            if (teksto.trim() == leguKonservitan()?.trim()) {
                logd("KanaloDeponejo", "La reta kanalkonfiguro estas sama kiel la konservita — nenio ŝanĝiĝis")
                return
            }
            val novaAgordo = try {
                leganto.legu(teksto)
            } catch (e: Throwable) {
                // Rompita respondo (ekz. fiaskpaĝo kun HTTP 200): NE konservu —
                // la konservita/pakita versio restas aktiva (kiel en la malnova apo).
                logw("KanaloDeponejo", "La reta kanalkonfiguro ne parseblas — konservas la lokan", e)
                return
            }
            if (novaAgordo.kanaloj.isEmpty()) {
                logw("KanaloDeponejo", "La reta kanalkonfiguro havas neniun kanalon — konservas la lokan")
                return
            }
            // Persistigo: fiasko (ekz. plena disko) ne rajtas bloki la vivan ĝisdatigon —
            // nur la sekva starto restas ĉe la malnova versio.
            try {
                skribuKonservitan(teksto)
            } catch (e: Exception) {
                logw("KanaloDeponejo", "Ne povis persistigi la freŝan kanalkonfiguron — la vivan liston tamen uzas", e)
            }
            val antaŭaNombro = _kanaloj.value.size
            agordo = novaAgordo
            _kanaloj.value = novaAgordo.kanaloj.map { it.alKanalo() }
            logi("KanaloDeponejo", "Nova kanalkonfiguro el la reto: ${novaAgordo.kanaloj.size} kanaloj (antaŭe $antaŭaNombro)")
            try {
                alFreŝaAgordo(novaAgordo)
            } catch (e: Exception) {
                loge("KanaloDeponejo", "Eraro en la reagado al freŝa kanalkonfiguro", e)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // Throwable, ne Exception: la retumila Ktor-motoro (Js) ĵetas kotlin.Error (ekz. ĉe CORS-blokado).
            // Atendata fiasko (sen reto, kaptiva portalo) — logw, ne loge (ne konsumu la eraro-kvoton).
            logw("KanaloDeponejo", "Elŝuto de la kanalkonfiguro malsukcesa — uzas la lokan", e)
        }
    }
}

/**
 * Legas la LOKAN kanalkonfiguron: unue la konservitan (elŝutitan el la reto) tekston
 * ([leguKonservitan], defaŭlte el la diska kaŝmemoro), kaj nur se ĝi mankas aŭ ne
 * parseblas — la pakitan version ([paktitaTeksto]). Putrita konservita teksto
 * forgesiĝas ([foriguKonservitan]), por ke la sekva starto ne reprovu ĝin (A4).
 *
 * Ankaŭ uzata ekster [KanaloDeponejoImpl] (alarmo-ricevilo, sciigo-worker), kiuj
 * ne trairas AppStato-on.
 */
fun leguLokanKanalkonfiguron(
    leganto: KanalAgordoLeganto = KanalAgordoLeganto(),
    leguKonservitan: () -> String? = { leguKaŝonJson(KanaloDeponejoImpl.KANALKONFIGURO_KAŜNOMO) },
    foriguKonservitan: () -> Unit = { foriguKaŝon(KanaloDeponejoImpl.KANALKONFIGURO_KAŜNOMO) },
    paktitaTeksto: () -> String,
): KanalAgordo {
    val konservita = try {
        leguKonservitan()
    } catch (e: Exception) {
        logw("KanaloDeponejo", "Ne povis legi la konservitan kanalkonfiguron", e)
        null
    }
    if (!konservita.isNullOrBlank()) {
        try {
            return leganto.legu(konservita)
        } catch (e: Throwable) {
            loge("KanaloDeponejo", "Konservita kanalkonfiguro ne parseblas — forgesas ĝin kaj uzas la pakitan (A4)", e)
            try {
                foriguKonservitan()
            } catch (e2: Exception) {
                logw("KanaloDeponejo", "Ne povis forigi la putritan konservitan konfiguron", e2)
            }
        }
    }
    return try {
        leganto.legu(paktitaTeksto())
    } catch (e: Throwable) {
        loge("KanaloDeponejo", "Ankaŭ la pakita kanalkonfiguro ne parseblas — malplena agordo (A4)", e)
        KanalAgordo()
    }
}
