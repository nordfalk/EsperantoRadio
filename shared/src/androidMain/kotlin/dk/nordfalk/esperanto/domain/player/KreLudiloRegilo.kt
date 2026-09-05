package dk.nordfalk.esperanto.domain.player

import dk.nordfalk.esperanto.domain.model.LudantoInformo
import dk.nordfalk.esperanto.domain.model.LudantoStato
import dk.nordfalk.esperanto.domain.model.Sonfonto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android-implemento — no-op provizore.
 *
 * Decido: La efektiva Media3 ExoPlayer-implemento estos aldonita en la
 * androidMain-source-set kiam Media3-dependeco estos aldonita al la
 * shared-modulo. Provizore la Android-celo uzas la saman no-op logikon
 * kiel Desktop, por ke la apo konstruiĝas kaj la UI funkciu.
 *
 * Kial ne tuj aldoni Media3: La Media3-biblioteko bezonas la androidMain-
 * source-set, kaj la ExoPlayer-instance bezonas Context. Tio postulas
 * expect/actual por la factory-funkcio kun Context-parametro. Tio estas
 * pli bone farita kiel aparta paŝo kiam la tuta ludilo-tavolo estas preta.
 */
actual fun kreLudiloRegilo(): LudiloRegilo = NoOpLudiloRegilo()

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
