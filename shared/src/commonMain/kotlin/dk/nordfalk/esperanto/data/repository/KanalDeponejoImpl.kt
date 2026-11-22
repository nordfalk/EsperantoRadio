package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.data.config.KANALKONFIGURO_KAŜNOMO
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * KanaloDeponejo-implentaĵo.
 *
 * Kiel en la malnova apo («grunddata») la konfiguro havas du tavolojn:
 * 1. **Komenca agordo** — jam parsita ĉe starto fare de `AppStato.inicialigu`
 *    (unue la persistigita defora versio el la diska kaŝmemoro, rezerve la
 *    paktita risurco — `leguAgordonKunRezervo`).
 * 2. **Defora refreŝigo** — [refreŝiguDefore] elŝutas la konfiguron de la servilo;
 *    nur se la teksto ŝanĝiĝis KAJ estas parsigebla, ĝi anstataŭigas la nunan
 *    kanal-liston (StateFlow) kaj persistiĝas por la sekva starto. Fiasko aŭ
 *    ne-parsigebla respondo neniam tuŝas la nunan staton (regulo A4).
 */
class KanaloDeponejoImpl(
    private val leganto: KanalAgordoLeganto,
    komencaAgordo: KanalAgordo,
    /** Elŝutas la konfiguran tekston defore; ĵetas ĉe ret-fiasko. Nul = defora refreŝigo malŝaltita. */
    private val elŝutuDefore: (suspend (url: String) -> String)? = null,
    /** URL de la defora konfiguro. */
    private val konfiguroUrl: String = KANALKONFIGURO_URL,
    /** Legas la persistigitan konfiguran tekston (diska kaŝmemoro). */
    private val leguKonservitan: () -> String? = { leguKaŝonJson(KANALKONFIGURO_KAŜNOMO) },
    /** Skribas la konfiguran tekston en la kaŝmemoron. */
    private val skribuKonservitan: (String) -> Unit = ::konservuKanalkonfiguron,
    /** Alvokata kiam freŝa, sukcese parsita defora agordo anstataŭigas la malnovan (kanal-listo, apaj novaĵoj k.a.). */
    private val alFreŝaAgordo: (KanalAgordo) -> Unit = {},
) : KanaloDeponejo {

    init {
        logi("KanaloDeponejo", "Ŝargis ${komencaAgordo.kanaloj.size} kanalojn (konservita aŭ paktita konfiguro)")
    }

    private val _kanaloj = MutableStateFlow(komencaAgordo.kanaloj.map { it.alKanalo() })
    override fun observiKanalojn(): StateFlow<List<Kanalo>> = _kanaloj.asStateFlow()

    override suspend fun getKanalojn(fortoRefresigi: Boolean): List<Kanalo> {
        if (_kanaloj.value.isEmpty()) {
            logw("KanaloDeponejo", "Kanallisto malplena — la komenca agordo havis neniun kanalon")
        }
        return _kanaloj.value
    }

    override suspend fun getKanalo(slug: String): Kanalo? {
        return _kanaloj.value.find { it.slug == slug }
    }

    /**
     * Elŝutas la kanalkonfiguron defore kaj, se vere nova kaj parsigebla,
     * anstataŭigas la kanalliston kaj persistigas la tekston — la fluo de la
     * malnova apo `hentEvtNyeGrunddata`: komparo kontraŭ la konservita teksto,
     * pars-provo en freŝa objekto (neniam rompu la nunan staton), nur post
     * sukceso skribo kaj anstataŭigo. Rulas unufoje po starto (lanĉita de
     * `AppStato.inicialigu`); fiasko estas atendata kaj nur protokoliĝas.
     */
    suspend fun refreŝiguDefore() {
        val elŝutilo = elŝutuDefore ?: return
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
