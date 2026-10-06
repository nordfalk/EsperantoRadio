package dk.nordfalk.esperanto.domain.player

import dk.nordfalk.esperanto.domain.model.LudantoInformo
import dk.nordfalk.esperanto.domain.model.LudantoStato
import dk.nordfalk.esperanto.domain.model.Sonfonto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Interfaco por la ludilo. Platform-specifaj implementoj:
 * - Android: Media3 ExoPlayer (kun Context, kreita en androidApp)
 * - Desktop: no-op (provizore)
 * - iOS: AVPlayer (estonte)
 * - Web: HTMLAudioElement (estonte)
 */
interface LudiloRegilo {
    val stato: StateFlow<LudantoInformo>

    /**
     * Ĉu tiu ĉi ludilo efektive kapablas salti (tio estas, ĉu [saltiAl] movas
     * la ludpozicion). Se false, la UI montras nur neinteragan pozicio-indikilon.
     * false ekz. ĉe Desktop (mp3spi-fluado ne subtenas saltadon).
     */
    val subtenasSaltadon: Boolean get() = true

    suspend fun fiksiFonton(fonto: Sonfonto, komencoPozicioMs: Long = 0)
    fun ludi()
    fun pauxzigi()
    fun halti()
    fun saltiAl(pozicioMs: Long)
    fun fiksiLauxtecon(volumeno: Float)

    /**
     * Legas la nunan laŭtecon de la ludilo (0 = mutigita … 1 = maksimume).
     * Bazo por la laŭteco-regilo en la mini-ludilbreto. Defaŭlte 1.
     */
    fun leguLauxtecon(): Float = 1f
}

/**
 * Kreas la platform-specifan LudiloRegilo-n.
 * - Android: provizita ekstere (ExoPlayerLudiloRegilo kun Context)
 * - Desktop: DesktopLudiloRegilo (JavaFX MediaPlayer)
 * - wasmJs: WasmJsLudiloRegilo (HTMLAudioElement)
 * - iOS: NoOpLudiloRegilo (estonte: AVPlayer)
 */
expect fun kreuDefauxltanLudiloRegilon(): LudiloRegilo

/**
 * No-op ludilo — UI funkcias, stato-ŝanĝoj funkcias, sed neniu sono.
 * Subtenas simuli naturfinon per [simuluFinon] por testoj.
 */
class NoOpLudiloRegilo : LudiloRegilo {
    private val _stato = MutableStateFlow(LudantoInformo(stato = LudantoStato.Haltita))
    override val stato: StateFlow<LudantoInformo> = _stato.asStateFlow()
    private var lauxteco = 1f

    override suspend fun fiksiFonton(fonto: Sonfonto, komencoPozicioMs: Long) {
        _stato.value = LudantoInformo(
            stato = LudantoStato.Haltita,
            nunaFonto = fonto,
            pozicioMs = komencoPozicioMs,
            dauroMs = 0,
            estasRekta = fonto is Sonfonto.RektaKanalo,
        )
    }

    override fun ludi() { _stato.value = _stato.value.copy(stato = LudantoStato.Ludas) }
    override fun pauxzigi() { _stato.value = _stato.value.copy(stato = LudantoStato.Haltita) }
    override fun halti() { _stato.value = LudantoInformo(stato = LudantoStato.Haltita) }
    override fun saltiAl(pozicioMs: Long) { _stato.value = _stato.value.copy(pozicioMs = pozicioMs) }
    override fun fiksiLauxtecon(volumeno: Float) { lauxteco = volumeno.coerceIn(0f, 1f) }
    override fun leguLauxtecon(): Float = lauxteco

    /**
     * Simulas naturfinon de la ludado — metas staton al Finita.
     * Uzata en testoj.
     */
    fun simuluFinon() {
        _stato.value = _stato.value.copy(stato = LudantoStato.Finita)
    }

    /**
     * Simulas pozicio-ŝanĝon — ŝajnigas ke la ludado progresis.
     * Uzata en testoj.
     */
    fun simuluPozicion(pozicioMs: Long, dauroMs: Long = 0) {
        _stato.value = _stato.value.copy(pozicioMs = pozicioMs, dauroMs = dauroMs)
    }

    /**
     * Simulas ludantan eraron — metas staton al Eraro.
     * Uzata en testoj.
     */
    fun simuluEraron(mesagho: String = "Testa eraro", reprovebla: Boolean = false) {
        _stato.value = _stato.value.copy(stato = LudantoStato.Eraro(mesagho, reprovebla))
    }
}
