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

    suspend fun fiksiFonton(fonto: Sonfonto, komencoPozicioMs: Long = 0)
    fun ludi()
    fun pauxzigi()
    fun halti()
    fun saltiAl(pozicioMs: Long)
    fun fiksiLauxtecon(volumeno: Float)
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
 */
class NoOpLudiloRegilo : LudiloRegilo {
    private val _stato = MutableStateFlow(LudantoInformo(stato = LudantoStato.Haltita))
    override val stato: StateFlow<LudantoInformo> = _stato.asStateFlow()

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
    override fun fiksiLauxtecon(volumeno: Float) {}
}
