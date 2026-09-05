package dk.nordfalk.esperanto.domain.player

import dk.nordfalk.esperanto.domain.model.LudantoInformo
import dk.nordfalk.esperanto.domain.model.LudantoStato
import dk.nordfalk.esperanto.domain.model.Sonfonto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.browser.document
import org.w3c.dom.HTMLAudioElement
import org.w3c.dom.events.Event

/**
 * wasmJs-implemento de LudiloRegilo per HTMLAudioElement.
 * Subtenas MP3. HLS dependas de la retumilo.
 */
class WasmJsLudiloRegilo : LudiloRegilo {
    private var audio: HTMLAudioElement? = null
    private val _stato = MutableStateFlow(LudantoInformo(stato = LudantoStato.Haltita))
    override val stato: StateFlow<LudantoInformo> = _stato.asStateFlow()
    private var nunaFonto: Sonfonto? = null

    private fun getStreamUrl(fonto: Sonfonto): String = when (fonto) {
        is Sonfonto.RektaKanalo -> fonto.kanal.rektaElsendaSonoUrl ?: ""
        is Sonfonto.ElsendoFonto -> fonto.elsendo.stream
        is Sonfonto.LokaElsendo -> fonto.dosieroVojo  // wasmJs ne havas lokan dosier-sistemon
    }

    override suspend fun fiksiFonton(fonto: Sonfonto, komencoPozicioMs: Long) {
        nunaFonto = fonto
        audio?.pause()
        val url = getStreamUrl(fonto)
        audio = document.createElement("audio") as HTMLAudioElement
        audio!!.src = url
        audio!!.currentTime = (komencoPozicioMs / 1000.0)
        audio!!.addEventListener("playing", { _ ->
            _stato.value = LudantoInformo(
                stato = LudantoStato.Ludas,
                nunaFonto = fonto,
                pozicioMs = (audio!!.currentTime * 1000).toLong(),
                dauroMs = if (audio!!.duration.isNaN()) 0 else (audio!!.duration * 1000).toLong(),
                estasRekta = fonto is Sonfonto.RektaKanalo
            )
        })
        audio!!.addEventListener("pause", { _ ->
            _stato.value = _stato.value.copy(stato = LudantoStato.Haltita)
        })
        audio!!.addEventListener("error", { _ ->
            _stato.value = _stato.value.copy(stato = LudantoStato.Eraro("Retumila audio-eraro"))
        })
        _stato.value = LudantoInformo(
            stato = LudantoStato.Konektas,
            nunaFonto = fonto,
            pozicioMs = komencoPozicioMs,
            dauroMs = 0,
            estasRekta = fonto is Sonfonto.RektaKanalo
        )
    }

    override fun ludi() { audio?.play() }
    override fun pauxzigi() { audio?.pause() }
    override fun halti() { audio?.pause(); audio = null; nunaFonto = null; _stato.value = LudantoInformo(stato = LudantoStato.Haltita) }
    override fun saltiAl(pozicioMs: Long) { audio?.currentTime = pozicioMs / 1000.0 }
    override fun fiksiLauxtecon(volumeno: Float) { audio?.volume = volumeno.toDouble() }
}
