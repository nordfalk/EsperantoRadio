package dk.nordfalk.esperanto.data.repository

import com.russhwolf.settings.Settings
import dk.nordfalk.esperanto.data.config.FORA_KONFIGURO_URL
import dk.nordfalk.esperanto.data.config.KanalAgordo
import dk.nordfalk.esperanto.data.config.KanalAgordoLeganto
import dk.nordfalk.esperanto.data.config.alKanalo
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.repository.KanaloDeponejo
import dk.nordfalk.esperanto.logd
import dk.nordfalk.esperanto.loge
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.logw
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
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

/**
 * KanaloDeponejo-implentaĵo. Kiel la malnova apo: la kanalkonfiguron oni elŝutas
 * de la servilo ([FORA_KONFIGURO_URL]) kaj konservas en la agordoj; ĉe relanĉo
 * oni uzas unue la kaŝitan foran version, kaj nur se mankas — la paktitan
 * enkonstruitan. En fono (unufoje je procezo, aŭ tuj ĉe `fortoRefresigi`)
 * oni elŝutas la nunan version kaj, nur se ĝi sukcese parseblas, konservas ĝin
 * kaj anstataŭigas la kanalojn (A4: fuŝita fora konfiguro neniam anstataŭas la
 * funkciantan).
 */
class KanaloDeponejoImpl(
    private val leganto: KanalAgordoLeganto,
    private val paktitaTeksto: () -> String,
    private val settings: Settings? = null,
    private val httpKliento: HttpClient? = null,
    /** Fona amplekso por la refreŝigo — se null, la reta refreŝigo ne kuras (testoj, ekran-antaŭrigardoj). */
    private val fonaScope: CoroutineScope? = null,
    /** Test-hako: elŝutas tekston de URL. Se null, uzas [httpKliento]. */
    private val elŝutTeksto: (suspend (String) -> String)? = null,
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

    /** Legas la efektivan konfiguron sen reto: kaŝita fora versio el la agordoj, aliokaz la pakita. */
    private fun ŝarguLokale() {
        val fora = settings?.getStringOrNull(SettingsKeys.FORA_KANALKONFIGURO)
        val teksto = if (!fora.isNullOrBlank()) fora else paktitaTeksto()
        val fonto = if (!fora.isNullOrBlank()) "kaŝita fora" else "paktita"
        val agordo = provuParse(teksto)
            ?: provuParse(paktitaTeksto())  // (A4) fuŝita fora kaŝo → rezervo al la pakita
            ?: KanalAgordo().also {
                loge("KanaloDeponejo", "Nek la fora nek la pakita kanalkonfiguro parseblas — malplena listo")
            }
        _agordo.value = agordo
        _kanaloj.value = agordo.kanaloj.map { it.alKanalo() }
        logi("KanaloDeponejo", "Ŝargis ${_kanaloj.value.size} kanalojn el $fonto")
    }

    /** Parsas kaj redonas null (kun averto) anstataŭ ĵeti — unu fuŝita konfiguro ne rajtas mortigi la apon (A4, A8). */
    private fun provuParse(teksto: String): KanalAgordo? = try {
        leganto.legu(teksto)
    } catch (e: Exception) {
        logw("KanaloDeponejo", "Kanalkonfiguro ne parseblas (${teksto.length} signoj)", e)
        null
    }

    /** La ŝargita agordo; certigas ke zumindest la loka (kaŝo/pakita) versio estas legita. */
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
            } catch (e: Exception) {
                logw("KanaloDeponejo", "Fona refreŝigo de la kanalkonfiguro fiaskis", e)
            }
        }
    }

    /**
     * Elŝutas la nunan foran konfiguron kaj — nur se ĝi malsamas kaj sukcese
     * parseblas — konservas ĝin en la agordoj kaj anstataŭigas la kanalojn.
     * Fiasko (reto, parsado) neniam tuŝas la nunan funkciantan konfiguron (A4).
     */
    private suspend fun refreŝiguElRete() {
        val elŝutanto = elŝutTeksto ?: httpKliento?.let { kliento ->
            suspend { url: String -> kliento.get(url) { expectSuccess = true }.bodyAsText() }
        } ?: return
        refreŝMutex.withLock {
            try {
                val nova = elŝutanto(FORA_KONFIGURO_URL).trim()
                val kaŝita = settings?.getStringOrNull(SettingsKeys.FORA_KANALKONFIGURO)
                if (nova == kaŝita) {
                    logd("KanaloDeponejo", "Fora kanalkonfiguro ne ŝanĝiĝis — nenio farenda")
                    return@withLock
                }
                val agordo = provuParse(nova)
                if (agordo == null) {
                    logw("KanaloDeponejo", "Elŝutita fora kanalkonfiguro ne parseblas — ne konservita")
                    return@withLock
                }
                settings?.putString(SettingsKeys.FORA_KANALKONFIGURO, nova)
                settings?.putLong(SettingsKeys.FORA_KANALKONFIGURO_TEMPO, Clock.System.now().toEpochMilliseconds())
                _agordo.value = agordo
                _kanaloj.value = agordo.kanaloj.map { it.alKanalo() }
                logi("KanaloDeponejo", "Ĝisdatigis kanalkonfiguron el la reto: ${_kanaloj.value.size} kanaloj")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logw("KanaloDeponejo", "Elŝuto de la fora kanalkonfiguro ($FORA_KONFIGURO_URL) fiaskis", e)
            }
        }
    }
}
