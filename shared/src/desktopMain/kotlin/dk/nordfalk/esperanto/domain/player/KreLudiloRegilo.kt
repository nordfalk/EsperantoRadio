package dk.nordfalk.esperanto.domain.player

import dk.nordfalk.esperanto.domain.model.LudantoInformo
import dk.nordfalk.esperanto.domain.model.LudantoStato
import dk.nordfalk.esperanto.domain.model.Sonfonto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Desktop-implemento — provizore no-op.
 * La UI funkcias (montras staton, pozicio), sed neniu sono estas ludata.
 * TODO: Aldoni VLCJ aŭ JavaFX Media por vera sonludado sur Desktop.
 *
 * Decido: No-op estas sufiĉa por la unua versio. La uzanto povas testi la UI
 * kaj la navigadon. Vera audio estos aldonita kiam VLCJ estos integrita.
 */
actual fun kreLudiloRegilo(): LudiloRegilo = NoOpLudiloRegilo()

class NoOpLudiloRegilo : LudiloRegilo {
    private val _stato = MutableStateFlow(LudantoInformo(stato = LudantoStato.Haltita))
    override val stato: StateFlow<LudantoInformo> = _stato.asStateFlow()

    private var nunaFonto: Sonfonto? = null
    private var ludas = false
    private var pozicioMs = 0L

    override suspend fun fiksiFonton(fonto: Sonfonto, komencoPozicioMs: Long) {
        nunaFonto = fonto
        pozicioMs = komencoPozicioMs
        ludas = false
        _stato.value = LudantoInformo(
            stato = LudantoStato.Haltita,
            nunaFonto = fonto,
            pozicioMs = komencoPozicioMs,
            dauroMs = 0,
            estasRekta = fonto is Sonfonto.RektaKanalo,
        )
    }

    override fun ludi() {
        ludas = true
        _stato.value = _stato.value.copy(stato = LudantoStato.Ludas)
    }

    override fun pauxzigi() {
        ludas = false
        _stato.value = _stato.value.copy(stato = LudantoStato.Haltita)
    }

    override fun halti() {
        ludas = false
        pozicioMs = 0
        _stato.value = LudantoInformo(stato = LudantoStato.Haltita)
    }

    override fun saltiAl(pozicioMs: Long) {
        this.pozicioMs = pozicioMs
        _stato.value = _stato.value.copy(pozicioMs = pozicioMs)
    }

    override fun fiksiLauxtecon(volumeno: Float) {
        // No-op
    }
}
