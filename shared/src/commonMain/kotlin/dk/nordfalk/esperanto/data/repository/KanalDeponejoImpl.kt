package dk.nordfalk.esperanto.data.repository

import com.russhwolf.settings.Settings
import dk.nordfalk.esperanto.data.config.FORA_KONFIGURO_KAŜNOMO
import dk.nordfalk.esperanto.data.config.FORA_KONFIGURO_URL
import dk.nordfalk.esperanto.data.config.KanalAgordo
import dk.nordfalk.esperanto.data.config.KanalAgordoLeganto
import dk.nordfalk.esperanto.data.config.alKanalo
import dk.nordfalk.esperanto.data.config.leguAgordonKunRezervo
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.repository.KanaloDeponejo
import dk.nordfalk.esperanto.logd
import dk.nordfalk.esperanto.loge
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.logw
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.Volatile
import kotlin.time.Clock

/** Tempolimo por la elŝuto de la fora kanalkonfiguro (malgranda dosiero, sed ne rajtas bloki). */
private const val KONFIGURO_PETO_TEMPOLIMITO_MS = 15_000L

/** Maksimuma grandeco de la elŝutita konfiguro (bitokoj) — la vera konfiguro estas ~21 KB; 256 KB estas malproksima supra limo kontraŭ erarpaĝoj. */
private const val KONFIGURO_MAKSUMA_GRANDECO_BITOKOJ = 256L * 1024

/**
 * KanaloDeponejo-implentaĵo. Kiel la malnova apo («grunddata» en `App.initData` /
 * `hentEvtNyeGrunddata`): la kanalkonfiguro havas du tavolojn —
 *
 * 1. **Komenca agordo** — jam parsita ĉe starto: unue la persistigita defora versio
 *    el la diska kaŝmemoro, rezerve la paktita risurco ([leguAgordonKunRezervo]).
 * 2. **Defora refreŝigo** — [refreŝiguElRete] elŝutas la konfiguron de la servilo
 *    (unufoje je procezo en fono, kaj tuj ĉe `fortoRefresigi`); nur se la teksto
 *    ŝanĝiĝis, estas parsigebla kaj havas kanalojn, ĝi anstataŭigas la kanalliston
 *    (StateFlow) kaj persistiĝas por la sekva starto. Fiasko (reto, CORS sur
 *    wasmJs — la Js-motoro ĵetas `kotlin.Error`, CORS-respondo, persist-fiasko)
 *    neniam tuŝas la nunan staton (A4).
 *
 * La persistigo okazas per la diska kaŝmemoro (`DosierKaŝo`), NE per Settings — sur
 * Desktop la java.util.prefs-realigo rifuzas valorojn super 8192 signoj.
 */
class KanaloDeponejoImpl(
    private val leganto: KanalAgordoLeganto,
    private val paktitaTeksto: () -> String,
    /** Settings estas uzata nur por la tempomastriko (Long) — neniam por la teksto mem. */
    private val settings: Settings? = null,
    private val httpKliento: HttpClient? = null,
    /** Fona amplekso por la refreŝigo — se null, la reta refreŝigo ne kuras (testoj, ekran-antaŭrigardoj). */
    private val fonaScope: CoroutineScope? = null,
    /** Test-hako: elŝutas tekston de URL. Se null, uzas [httpKliento]. */
    private val elŝutTeksto: (suspend (String) -> String)? = null,
    /** Legas la persistigitan tekston (diska kaŝmemoro). Injektebla por testoj. */
    private val leguKonservitan: () -> String? = { leguKaŝonJson(FORA_KONFIGURO_KAŜNOMO) },
    /** Skribas la persistigitan tekston. Injektebla por testoj. */
    private val skribuKonservitan: (String) -> Unit = { skribuKaŝonJson(FORA_KONFIGURO_KAŜNOMO, it) },
    /** Forigas la persistigitan tekston (putrita kaŝo). Injektebla por testoj. */
    private val foriguKonservitan: () -> Unit = { foriguKaŝon(FORA_KONFIGURO_KAŜNOMO) },
    /** Vokata kiam freŝa, sukcese parsita fora agordo anstataŭigas la malnovan (kanallisto, apaj novaĵoj k.a.). */
    private val alFreŝaAgordo: (KanalAgordo) -> Unit = {},
) : KanaloDeponejo {

    private val _kanaloj = MutableStateFlow<List<Kanalo>>(emptyList())
    override fun observiKanalojn(): StateFlow<List<Kanalo>> = _kanaloj.asStateFlow()

    private val _agordo = MutableStateFlow<KanalAgordo?>(null)

    /** La ŝargita kanalkonfiguro (kaŝita fora versio, aliokaz pakita) — por arkivoBazoUrl, defaŭltaj ŝatataj, alarm-sugestoj k.a. */
    fun observiAgordon(): StateFlow<KanalAgordo?> = _agordo.asStateFlow()

    private val refreŝMutex = Mutex()

    @Volatile
    private var fonRefreŝoLanĉita = false

    init {
        ŝarguLokale()
    }

    /** Legas la efektivan konfiguron sen reto: kaŝita fora versio, aliokaz la pakita; putrita kaŝo estas forgesata (A4). */
    private fun ŝarguLokale() {
        val agordo = leguAgordonKunRezervo(leganto, paktitaTeksto, leguKonservitan, foriguKonservitan)
        val fonto = if (!leguKonservitan().isNullOrBlank()) "kaŝita fora" else "paktita"
        _agordo.value = agordo
        _kanaloj.value = agordo.kanaloj.map { it.alKanalo() }
        logi("KanaloDeponejo", "Ŝargis ${_kanaloj.value.size} kanalojn el $fonto")
    }

    /** La ŝargita agordo; certigas ke almenaŭ la loka (kaŝo/pakita) versio estas legita. */
    fun getAgordon(): KanalAgordo {
        if (_kanaloj.value.isEmpty()) ŝarguLokale()
        return _agordo.value ?: KanalAgordo()
    }

    override suspend fun getKanalojn(fortoRefresigi: Boolean): List<Kanalo> {
        if (_kanaloj.value.isEmpty()) ŝarguLokale()
        if (fortoRefresigi) {
            // Eltirado (pull-to-refresh): atendu la elŝuton nun
            refreŝiguElRete()
        } else {
            // Ĉe ĉiu relanĉo (procezo) unufoje elŝutu en fono — "dum relanĉo"
            certiguFonRefreŝigon()
        }
        return _kanaloj.value
    }

    override suspend fun getKanalo(slug: String): Kanalo? {
        if (_kanaloj.value.isEmpty()) ŝarguLokale()
        return _kanaloj.value.find { it.slug == slug }
    }

    private fun certiguFonRefreŝigon() {
        val scope = fonaScope ?: return
        if (fonRefreŝoLanĉita) return
        fonRefreŝoLanĉita = true
        scope.launch {
            try {
                refreŝiguElRete()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // Throwable, ne Exception: la Js-motoro ĵetas kotlin.Error (CORS) — atendata fiasko (A4, A8)
                logw("KanaloDeponejo", "Fona refreŝigo de la kanalkonfiguro fiaskis", e)
            }
        }
    }

    /**
     * Elŝutas la nunan foran konfiguron kaj — nur se ĝi malsamas, parseblas kaj
     * havas kanalojn — konservas ĝin kaj anstataŭigas la kanalojn (kaj vokas
     * [alFreŝaAgordo]). Fiasko (reto, parsado, grandeco) neniam tuŝas la nunan
     * funkciantan konfiguron (A4). Persist-fiasko nur protokoliĝas — la viva
     * ĝisdatigo tamen okazas.
     */
    private suspend fun refreŝiguElRete() {
        val elŝutanto = elŝutTeksto ?: httpKliento?.let { kliento ->
            suspend { url: String ->
                kliento.get(url) {
                    expectSuccess = true
                    timeout { requestTimeoutMillis = KONFIGURO_PETO_TEMPOLIMITO_MS }
                }.leguKorponLimigite(KONFIGURO_MAKSUMA_GRANDECO_BITOKOJ)
            }
        } ?: return
        refreŝMutex.withLock {
            try {
                val nova = elŝutanto(FORA_KONFIGURO_URL).trim()
                if (nova.isBlank()) {
                    logw("KanaloDeponejo", "Elŝutita fora kanalkonfiguro estas malplena — konservas la nunan")
                    return@withLock
                }
                if (nova == leguKonservitan()?.trim()) {
                    logd("KanaloDeponejo", "Fora kanalkonfiguro ne ŝanĝiĝis — nenio farenda")
                    return@withLock
                }
                val agordo = try {
                    leganto.legu(nova)
                } catch (e: Throwable) {
                    // Rompita respondo (ekz. HTML-fiaskpaĝo kun HTTP 200) — NENIAM konservu (A4)
                    logw("KanaloDeponejo", "Elŝutita fora kanalkonfiguro ne parseblas — ne konservita", e)
                    return@withLock
                }
                if (agordo.kanaloj.isEmpty()) {
                    logw("KanaloDeponejo", "Elŝutita fora kanalkonfiguro havas neniun kanalon — ne konservita")
                    return@withLock
                }
                try {
                    skribuKonservitan(nova)
                } catch (e: Throwable) {
                    // Persistigo fiaskis (ekz. plena disko) — la viva ĝisdatigo tamen okazas;
                    // la sekva starto tiam refreŝiĝas denove.
                    logw("KanaloDeponejo", "Ne povis persistigi la freŝan kanalkonfiguron", e)
                }
                settings?.putLong(SettingsKeys.FORA_KANALKONFIGURO_TEMPO, Clock.System.now().toEpochMilliseconds())
                _agordo.value = agordo
                _kanaloj.value = agordo.kanaloj.map { it.alKanalo() }
                logi("KanaloDeponejo", "Ĝisdatigis kanalkonfiguron el la reto: ${_kanaloj.value.size} kanaloj")
                try {
                    alFreŝaAgordo(agordo)
                } catch (e: Exception) {
                    loge("KanaloDeponejo", "Eraro en la reagado al freŝa kanalkonfiguro", e)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // Throwable: sur wasmJs la Js-motoro ĵetas kotlin.Error (ekz. CORS-blokado)
                logw("KanaloDeponejo", "Elŝuto de la fora kanalkonfiguro ($FORA_KONFIGURO_URL) fiaskis", e)
            }
        }
    }
}
