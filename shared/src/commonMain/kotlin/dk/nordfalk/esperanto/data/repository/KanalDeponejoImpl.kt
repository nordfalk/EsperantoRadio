package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.data.config.KANALKONFIGURO_KAŜNOMO
import dk.nordfalk.esperanto.data.config.KANALKONFIGURO_REFREŜA_INTERVALO_MS
import dk.nordfalk.esperanto.data.config.KANALKONFIGURO_URL
import dk.nordfalk.esperanto.data.config.KanalAgordo
import dk.nordfalk.esperanto.data.config.KanalAgordoLeganto
import dk.nordfalk.esperanto.data.config.alKanalo
import dk.nordfalk.esperanto.data.config.konservuKanalkonfiguron
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.repository.KanaloDeponejo
import dk.nordfalk.esperanto.logd
import dk.nordfalk.esperanto.loge
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.logw
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
 * KanaloDeponejo-implentaĵo.
 *
 * Kiel en la malnova apo («grunddata») la konfiguro havas du tavolojn:
 * 1. **Komenca agordo** — jam parsita ĉe starto fare de `AppStato.inicialigu`
 *    (unue la persistigita defora versio el la diska kaŝmemoro, rezerve la
 *    paktita risurco — `leguAgordonKunRezervo`).
 * 2. **Defora refreŝigo** — [refreŝiguDefore] elŝutas la konfiguron de la servilo;
 *    nur se la teksto ŝanĝiĝis KAJ estas parsigebla kun ne-malplena kanalaro, ĝi
 *    anstataŭigas la nunan kanal-liston (StateFlow) kaj persistiĝos por la sekva
 *    starto. Fiasko aŭ ne-parsigebla respondo neniam tuŝas la nunan staton (A4).
 *
 * La refreŝigon ekigas: `AppStato.inicialigu` (unufoje po starto — «dum relanĉo»)
 * kaj `getKanalojn(fortoRefresigi = true)` (malsupren-tiro en la kanalaro — devige,
 * preterpasante la TTL). Enmemora TTL (kiel `sidstTjekket` de la malnova apo)
 * limigas ne-devigajn kontrolojn al unu po [refreŝaIntervaloMs] — sed ĉar ĝi estas
 * enmemora, ĉiu relanĉo denove elŝutas freŝan version.
 */
class KanaloDeponejoImpl(
    private val leganto: KanalAgordoLeganto,
    komencaAgordo: KanalAgordo,
    /** Elŝutas la konfiguran tekston defore; ĵetas ĉe ret-fiasko. Nul = defora refreŝigo malŝaltita. */
    private val elŝutuDefore: (suspend (url: String) -> String)? = null,
    /** URL de la defora konfiguro. */
    private val konfiguroUrl: String = KANALKONFIGURO_URL,
    /** Minimuma intervalo inter du ne-devigaj kontroloj (enmemora; injektebla por testoj). */
    private val refreŝaIntervaloMs: Long = KANALKONFIGURO_REFREŜA_INTERVALO_MS,
    /** Legas la persistigitan konfiguran tekston (diska kaŝmemoro). */
    private val leguKonservitan: () -> String? = { leguKaŝonJson(KANALKONFIGURO_KAŜNOMO) },
    /** Skribas la konfiguran tekston en la kaŝmemoron. */
    private val skribuKonservitan: (String) -> Unit = ::konservuKanalkonfiguron,
    /** Alvokata kiam freŝa, sukcese parsita defora agordo anstataŭigas la malnovan (kanal-listo, apaj novaĵoj k.a.). */
    private val alFreŝaAgordo: (KanalAgordo) -> Unit = {},
) : KanaloDeponejo {

    private val _kanaloj = MutableStateFlow(komencaAgordo.kanaloj.map { it.alKanalo() })
    override fun observiKanalojn(): StateFlow<List<Kanalo>> = _kanaloj.asStateFlow()

    /** Skopo por la lanĉita deviga refreŝigo (malsupren-tiro). Default, ne IO (IO ne ekzistas sur wasmJs). */
    private val skopo = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Tempo de la lasta kontrolo (ENmemora, kiel `sidstTjekket` en la malnova apo):
     * je ĉiu relanĉo denove elŝuteblas, sed ripetaj kontroloj ene de unu seanco
     * limiĝas al unu po [refreŝaIntervaloMs] — por ne bombi la servilon.
     * Skribata ankaŭ ĉe fiasko, same kiel la malnova apo. `devige` preterpasas.
     */
    @Volatile
    private var lastaKontroloMs = 0L

    init {
        logi("KanaloDeponejo", "Ŝargis ${komencaAgordo.kanaloj.size} kanalojn (konservita aŭ paktita konfiguro)")
    }

    override suspend fun getKanalojn(fortoRefresigi: Boolean): List<Kanalo> {
        if (_kanaloj.value.isEmpty()) {
            logw("KanaloDeponejo", "Kanallisto malplena — la komenca agordo havis neniun kanalon")
        }
        if (fortoRefresigi && elŝutuDefore != null) {
            // Malsupren-tiro: kontrolu ankaŭ la konfiguron — asinkrone, ne bloku la
            // refreŝigon; la listo ĝisdatiĝos per la StateFlow kiam la respondo alvenas.
            skopo.launch {
                try {
                    refreŝiguDefore(devige = true)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logw("KanaloDeponejo", "Deviga refreŝigo de la kanalkonfiguro fiaskis", e)
                }
            }
        }
        return _kanaloj.value
    }

    override suspend fun getKanalo(slug: String): Kanalo? {
        return _kanaloj.value.find { it.slug == slug }
    }

    /**
     * Elŝutas la kanalkonfiguron defore kaj, se vere nova, parsigebla kaj
     * ne-malplena, anstataŭigas la kanalliston kaj persistigas la tekston — la
     * fluo de la malnova apo `hentEvtNyeGrunddata`: komparo kontraŭ la konservita
     * teksto, pars-provo en freŝa objekto (neniam rompu la nunan staton), nur post
     * sukceso skribo kaj anstataŭigo. Fiasko estas atendata kaj nur protokoliĝas.
     *
     * @param devige true preterpasas la TTL (malsupren-tiro); la ekflugo ĉe relanĉo
     *   uzu la defaŭlton (TTL-kontrolita).
     */
    suspend fun refreŝiguDefore(devige: Boolean = false) {
        val elŝutilo = elŝutuDefore ?: return
        val nun = Clock.System.now().toEpochMilliseconds()
        if (!devige && lastaKontroloMs != 0L && nun - lastaKontroloMs < refreŝaIntervaloMs) {
            logd("KanaloDeponejo", "Konfiguro kontrolita antaŭ ${nun - lastaKontroloMs} ms — ene de la TTL, saltas")
            return
        }
        lastaKontroloMs = nun
        val novaTeksto = try {
            elŝutilo(konfiguroUrl)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // Throwable, ne Exception: la retumila Ktor-motoro (Js) ĵetas kotlin.Error("Fail to fetch")
            // ĉe CORS-blokado (vidu ElsendoDeponejoToleremoTest). Atendata fiasko — A4.
            logw("KanaloDeponejo", "Ne povis elŝuti la kanalkonfiguron de $konfiguroUrl", e)
            return
        }
        if (novaTeksto.isBlank()) {
            logw("KanaloDeponejo", "La konfigura respondo havis malplenan korpon — konservas la nunan")
            return
        }
        if (novaTeksto.trim() == leguKonservitan()?.trim()) {
            logd("KanaloDeponejo", "Defora kanalkonfiguro samas kiel la konservita — neniu ĝisdatigo")
            return
        }
        val novaAgordo = try {
            leganto.legu(novaTeksto)
        } catch (e: Exception) {
            // Vera problemo: la servilo redonis ne-parsigeblan konfiguron
            // (aŭ HTML-erarpaĝon). NENIAM anstataŭigi la nunan staton (A4).
            logw("KanaloDeponejo", "Defora kanalkonfiguro ne estas parsigebla — konservas la nunan", e)
            return
        }
        if (novaAgordo.kanaloj.isEmpty()) {
            // Gardilo: konfiguro sen ajna kanalo estas preskaŭ certe fuŝa
            // (erarpaĝo redonita kun HTTP 200, duon-skribita dosiero) — ne akceptu.
            logw("KanaloDeponejo", "Defora kanalkonfiguro havas neniun kanalon — verŝajne fuŝa, konservas la nunan")
            return
        }
        try {
            skribuKonservitan(novaTeksto)
        } catch (e: Exception) {
            // Persistigo fiaskis (ekz. plena disko) — la viva ĝisdatigo tamen okazas;
            // nur la SEKVA starto restas ĉe la malnova versio (kaj tiam refreŝiĝas).
            logw("KanaloDeponejo", "Ne povis persistigi la freŝan kanalkonfiguron", e)
        }
        _kanaloj.value = novaAgordo.kanaloj.map { it.alKanalo() }
        logi("KanaloDeponejo", "Ricevis freŝan kanalkonfiguron: ${_kanaloj.value.size} kanaloj")
        try {
            alFreŝaAgordo(novaAgordo)
        } catch (e: Exception) {
            loge("KanaloDeponejo", "Eraro en la reagado al freŝa kanalkonfiguro", e)
        }
    }
}
