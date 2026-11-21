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
    private var nunaLaŭteco = 1f
    private val _laŭteco = MutableStateFlow(1f)
    override val laŭteco: StateFlow<Float> = _laŭteco.asStateFlow()
    private val _stato = MutableStateFlow(LudantoInformo(stato = LudantoStato.Haltita))
    override val stato: StateFlow<LudantoInformo> = _stato.asStateFlow()
    private var nunaFonto: Sonfonto? = null

    // (FAROTA C15, 2026-11-20) Aŭskultantoj registritaj per aŭskultu(), por ke ili
    // estu FORIGEBlaj de malnova elemento — iliaj eventoj alie alvenus nesinkrone
    // kaj superskribus la staton de la nova.
    private val registritajAŭskultantoj = mutableListOf<Triple<HTMLAudioElement, String, (Event) -> Unit>>()

    private fun foriguAŭskultantojn(elemento: HTMLAudioElement?) {
        if (elemento == null) return
        for ((el, tipo, reago) in registritajAŭskultantoj) {
            if (el === elemento) elemento.removeEventListener(tipo, reago)
        }
        registritajAŭskultantoj.removeAll { it.first === elemento }
    }

    private fun aŭskultu(elemento: HTMLAudioElement, tipo: String, reago: (Event) -> Unit) {
        elemento.addEventListener(tipo, reago)
        registritajAŭskultantoj.add(Triple(elemento, tipo, reago))
    }

    private fun getStreamUrl(fonto: Sonfonto): String = when (fonto) {
        is Sonfonto.RektaKanalo -> fonto.kanalo.rektaElsendaSonoUrl ?: ""
        is Sonfonto.ElsendoFonto -> fonto.elsendo.fluo
        is Sonfonto.LokaElsendo -> fonto.dosieroVojo  // wasmJs ne havas lokan dosier-sistemon
    }

    override suspend fun fiksiFonton(fonto: Sonfonto, komencoPozicioMs: Long) {
        nunaFonto = fonto
        foriguAŭskultantojn(audio) // (FAROTA C15) neniam lasu malnovajn reagojn aktivaj
        audio?.pause()
        val elemento = document.createElement("audio") as HTMLAudioElement
        audio = elemento
        elemento.src = getStreamUrl(fonto)
        elemento.volume = nunaLaŭteco.toDouble() // reapliku la laŭtecon ankaŭ al nova fonto
        elemento.currentTime = (komencoPozicioMs / 1000.0)
        // La reagoj kaptas `elemento` (ne la kampon `audio`), do ili ĉiam validas
        // nur por la elemento al kiu ili estas registritaj.
        aŭskultu(elemento, "playing", { _ ->
            _stato.value = LudantoInformo(
                stato = LudantoStato.Ludas,
                nunaFonto = fonto,
                pozicioMs = (elemento.currentTime * 1000).toLong(),
                daŭroMs = if (elemento.duration.isNaN()) 0 else (elemento.duration * 1000).toLong(),
                estasRekta = fonto is Sonfonto.RektaKanalo
            )
        })
        aŭskultu(elemento, "pause", { _ ->
            _stato.value = _stato.value.copy(stato = LudantoStato.Haltita)
        })
        aŭskultu(elemento, "ended", { _ ->
            _stato.value = _stato.value.copy(stato = LudantoStato.Finita)
        })
        aŭskultu(elemento, "error", { _ ->
            // MEDIA_ERR_NETWORK (2) estas pasema; aliaj (ekz. 4 = nesubtenata/404) estas daŭraj
            val kodo = elemento.error?.code?.toInt()
            _stato.value = _stato.value.copy(stato = LudantoStato.Eraro("Retumila aŭda eraro ($kodo)", reprovebla = kodo == 2))
        })
        // (PR #92) sen timeupdate la pozicio frostas ĉe la valoro de "playing" —
        // la ŝovilo ne moviĝas kaj la pozicia savilo de LudvicoRegilo konservas malnovan valoron
        aŭskultu(elemento, "timeupdate", { _ ->
            _stato.value = _stato.value.copy(
                pozicioMs = (elemento.currentTime * 1000).toLong(),
                daŭroMs = if (elemento.duration.isNaN()) 0 else (elemento.duration * 1000).toLong(),
            )
        })
        _stato.value = LudantoInformo(
            stato = LudantoStato.Konektas,
            nunaFonto = fonto,
            pozicioMs = komencoPozicioMs,
            daŭroMs = 0,
            estasRekta = fonto is Sonfonto.RektaKanalo
        )
    }

    override fun ludi() {
        // (PR #92) play() redonas Promise kaj ĝia malakcepto (aŭtopl.-politiko, CORS)
        // NE ekigas la "error"-eventon — sen kapto la stato restus "Konektas" eterne
        audio?.play()?.catch { rezulto: JsAny? ->
            _stato.value = _stato.value.copy(
                stato = LudantoStato.Eraro(
                    "Ne eblis komenci la ludadon: ${rezulto?.toString() ?: "nekonata kaŭzo"}",
                    reprovebla = true,
                )
            )
            null
        }
    }
    override fun paŭzigi() { audio?.pause() }
    override fun halti() {
        foriguAŭskultantojn(audio) // (FAROTA C15)
        audio?.pause()
        audio = null
        nunaFonto = null
        _stato.value = LudantoInformo(stato = LudantoStato.Haltita)
    }
    override fun saltiAl(pozicioMs: Long) { audio?.currentTime = pozicioMs / 1000.0 }
    override fun fiksiLaŭtecon(volumeno: Float) {
        nunaLaŭteco = volumeno.coerceIn(0f, 1f)
        audio?.volume = nunaLaŭteco.toDouble()
        _laŭteco.value = nunaLaŭteco
    }
}
