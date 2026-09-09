package dk.nordfalk.esperanto.domain.player

import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.model.LudataElsendo
import dk.nordfalk.esperanto.domain.model.LudantoInformo
import dk.nordfalk.esperanto.domain.model.LudantoStato
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.domain.repository.ElsendoDeponejo
import dk.nordfalk.esperanto.domain.repository.KanaloDeponejo
import dk.nordfalk.esperanto.domain.repository.LudatojDeponejo
import dk.nordfalk.esperanto.domain.repository.PlejsatatajDeponejo
import dk.nordfalk.esperanto.logd
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.logw
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * LudvicoRegilo — kontrolas daŭran ludadon kun aŭtomata sekva-ludado.
 *
 * Respondecoj:
 * - **Pozicio-spurado**: dum ludado, perioda savas la pozicion (ĉiu 5s)
 * - **Resumigo**: kiam komencas elsendon, kontrolas ĉu ekzistas savita pozicio
 *   kaj komencas de tie (se ne finita)
 * - **Aŭtoludo**: kiam LudiloRegilo elsendas Finita, decidas kion ludi sekve
 *   per LudvicoLogiko kaj aŭtomate komencas ĝin
 * - **Eksplicita ludvico**: la uzanto povas aldoni elsendojn al la vico;
 *   kiam la nuna finas, la sekva el la vico estas ludita antaŭ la aŭtomata
 *   decido
 *
 * @param ludilo la suba platforma ludilo
 * @param elsendoDeponejo por ŝargi elsendojn de kanalo (por priority 1)
 * @param kanaloDeponejo por trovi kanalon laŭ slug
 * @param plejsatatajDeponejo por scii kiuj kanaloj estas ŝatataj (priority 2)
 * @param ludatojDeponejo por persisto de pozicioj kaj finstato
 * @param elshutDeponejo por kontrolado de loka dosiero (eksterreta ludado)
 */
class LudvicoRegilo(
    private val ludilo: LudiloRegilo,
    private val elsendoDeponejo: ElsendoDeponejo,
    private val kanaloDeponejo: KanaloDeponejo,
    private val plejsatatajDeponejo: PlejsatatajDeponejo,
    private val ludatojDeponejo: LudatojDeponejo,
    private val getLokaDosieroVojo: suspend (String) -> String? = { null },
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob()),
) {
    /** Delegas la staton al la suba ludilo. */
    val stato: StateFlow<LudantoInformo> get() = ludilo.stato

    /** La eksplicita ludvico — elsendoj aldonitaj de la uzanto. */
    private val _vico = MutableStateFlow<List<Elsendo>>(emptyList())
    val vico: StateFlow<List<Elsendo>> = _vico.asStateFlow()

    private var observanto: Job? = null
    private var pozicioSavanto: Job? = null
    private val ludMutex = Mutex()
    private var antauxaStato: LudantoStato = LudantoStato.Haltita

    /**
     * Komencas observi la ludilon-staton por detekti Finita kaj ĝisdatigi pozicion.
     * Vokata unufoje ĉe app-starto.
     */
    fun komenci() {
        if (observanto != null) return
        observanto = ludilo.stato
            .map { it.stato }
            .distinctUntilChanged()
            .onEach { stato ->
                traktiStatoSxangxon(stato)
            }
            .launchIn(scope)
    }

    private suspend fun traktiStatoSxangxon(stato: LudantoStato) {
        when {
            stato == LudantoStato.Finita && antauxaStato != LudantoStato.Finita -> {
                logi("Ludvico", "Ludado finiĝis (naturfino)")
                val nunaFonto = ludilo.stato.value.nunaFonto
                val nunaElsendo = when (nunaFonto) {
                    is Sonfonto.ElsendoFonto -> nunaFonto.elsendo
                    is Sonfonto.LokaElsendo -> nunaFonto.elsendo
                    else -> null
                }
                // Ne aŭtoludi post rekta kanalo — rekta elsendo ne havas sekvan
                if (nunaFonto is Sonfonto.RektaKanalo) {
                    logi("Ludvico", "Rekta kanalo finiĝis — ne aŭtoludas")
                    antauxaStato = stato
                    return
                }
                if (nunaElsendo != null) {
                    ludatojDeponejo.markiFinita(nunaElsendo.id, nunaElsendo.kanaloSlug)
                }
                // Forigu la finitan elsendon el la vico se ĝi estas tie
                if (nunaElsendo != null) {
                    _vico.value = _vico.value.filterNot { it.id == nunaElsendo.id }
                }
                // Lanĉu sekvan en aparta korutino por eviti rekurson
                val elsendoPorLudi = nunaElsendo
                scope.launch { ludiSekvan(elsendoPorLudi) }
            }
            stato == LudantoStato.Ludas -> {
                komenciPozicianSpuradon()
            }
            stato == LudantoStato.Haltita -> {
                haltiPozicianSpuradon()
                // Savu finan pozicion
                savuPozicion()
            }
            else -> {}
        }
        antauxaStato = stato
    }

    private fun komenciPozicianSpuradon() {
        pozicioSavanto?.cancel()
        pozicioSavanto = scope.launch {
            while (true) {
                delay(POZICIO_SAV_INTERVALO_MS)
                savuPozicion()
            }
        }
    }

    private fun haltiPozicianSpuradon() {
        pozicioSavanto?.cancel()
        pozicioSavanto = null
    }

    private suspend fun savuPozicion() {
        val info = ludilo.stato.value
        val elsendo = when (info.nunaFonto) {
            is Sonfonto.ElsendoFonto -> info.nunaFonto.elsendo
            is Sonfonto.LokaElsendo -> info.nunaFonto.elsendo
            else -> return
        }
        if (info.pozicioMs > 0) {
            ludatojDeponejo.registriPozicion(elsendo.id, elsendo.kanaloSlug, info.pozicioMs, info.dauroMs)
            logd("Ludvico", "Savis pozicion: ${elsendo.id} @ ${info.pozicioMs}ms")
        }
    }

    /**
     * Komencas ludi elsendon. Se ekzistas savita pozicio (kaj la elsendo ne estas
     * finita), komencas de tiu pozicio. Se ne, komencas de la komenco.
     *
     * Ankaŭ savas la pozicion de la antaŭa elsendo se necesa.
     */
    suspend fun ludiElsendon(elsendo: Elsendo) {
        ludMutex.withLock {
            // Savu pozicion de la antaŭa elsendo
            savuPozicion()

            val ludato = ludatojDeponejo.getLudato(elsendo.id)
            val komencoPozicio = if (ludato != null && !ludato.finita && ludato.pozicioMs > 0) {
                logi("Ludvico", "Resumas: ${elsendo.id} @ ${ludato.pozicioMs}ms")
                ludato.pozicioMs
            } else {
                0L
            }

            // Kontrolu ĉu ekzistas loka (elŝutita) dosiero
            val lokaVojo = getLokaDosieroVojo(elsendo.id)
            val fonto = if (lokaVojo != null) {
                Sonfonto.LokaElsendo(elsendo, lokaVojo)
            } else {
                Sonfonto.ElsendoFonto(elsendo)
            }

            ludilo.fiksiFonton(fonto, komencoPozicio)
            ludilo.ludi()
            ludatojDeponejo.registriPozicion(elsendo.id, elsendo.kanaloSlug, komencoPozicio, 0)
        }
    }

    /**
     * Aldonas elsendon al la fino de la ludvico.
     * Se nenio ludas, tuj komencas ludi ĝin.
     */
    suspend fun aldoniAlVico(elsendo: Elsendo) {
        val nunaVico = _vico.value.toMutableList()
        if (elsendo.id !in nunaVico.map { it.id }) {
            nunaVico.add(elsendo)
            _vico.value = nunaVico
        }
        logi("Ludvico", "Aldonis al vico: ${elsendo.id} — vico nun ${nunaVico.size}")

        // Se nenio ludas, tuj komencu
        val stato = ludilo.stato.value.stato
        if (stato == LudantoStato.Haltita || stato == LudantoStato.Finita) {
            ludiSekvan(null)
        }
    }

    /**
     * Forigas elsendon el la ludvico.
     */
    fun forigiElVico(elsendoId: String) {
        _vico.value = _vico.value.filterNot { it.id == elsendoId }
        logi("Ludvico", "Forigis el vico: $elsendoId")
    }

    /**
     * Forigas ĉiujn el la ludvico.
     */
    fun malplenigiVicon() {
        _vico.value = emptyList()
        logi("Ludvico", "Malplenigis vicon")
    }

    /**
     * Decidas kion ludi sekve kaj komencas ludi.
     *
     * Logiko:
     * 1. Se la eksplicita vico ne estas malplena → ludu la unuan
     * 2. Se la vico estas malplena → uzu LudvicoLogiko por aŭtomata decido
     * 3. Se nenio troviĝas → haltigu
     */
    suspend fun ludiSekvan(nunaElsendo: Elsendo?) {
        ludMutex.withLock {
            // 1. Kontrolu la eksplicitan vicon
            val vico = _vico.value
            if (vico.isNotEmpty()) {
                val sekva = vico.first()
                _vico.value = vico.drop(1)
                logi("Ludvico", "Ludas sekvan el vico: ${sekva.id}")
                val lokaVojo = getLokaDosieroVojo(sekva.id)
                val fonto = if (lokaVojo != null) {
                    Sonfonto.LokaElsendo(sekva, lokaVojo)
                } else {
                    Sonfonto.ElsendoFonto(sekva)
                }
                ludilo.fiksiFonton(fonto, 0)
                ludilo.ludi()
                return@withLock
            }

            // 2. Aŭtomata decido per LudvicoLogiko
            val samkanalaj = if (nunaElsendo != null) {
                elsendoDeponejo.getElsendojn(nunaElsendo.kanaloSlug)
            } else {
                emptyList()
            }

            val cxiujElsendoj = cxiujSargitajElsendoj()
            val plejsatataj = plejsatatajDeponejo.observiPlejsatatajn().value
            val ludatoj = ludatojDeponejo.observiLudatojn().value

            val sekva = LudvicoLogiko.deciduSekvan(
                nunaElsendo = nunaElsendo,
                samkanalajElsendoj = samkanalaj,
                cxiujElsendoj = cxiujElsendoj,
                plejsatatajKanaloj = plejsatataj,
                ludatoj = ludatoj,
            )

            if (sekva != null) {
                logi("Ludvico", "Aŭtomata sekva: ${sekva.id} — ${sekva.titolo}")
                val lokaVojo = getLokaDosieroVojo(sekva.id)
                val fonto = if (lokaVojo != null) {
                    Sonfonto.LokaElsendo(sekva, lokaVojo)
                } else {
                    Sonfonto.ElsendoFonto(sekva)
                }
                ludilo.fiksiFonton(fonto, 0)
                ludilo.ludi()
            } else {
                logi("Ludvico", "Nenio por ludi sekve — haltas")
                ludilo.halti()
            }
        }
    }

    /**
     * Kolektas ĉiujn ŝargitajn elsendojn el ĉiuj kanaloj en la kaŝmemoro.
     * Uzas la ElsendoDeponejo por akiri la kaŝenitajn elsendojn.
     */
    private suspend fun cxiujSargitajElsendoj(): List<Elsendo> {
        val kanaloj = kanaloDeponejo.observiKanalojn().value
        return kanaloj.flatMap { kanalo ->
            try {
                elsendoDeponejo.getElsendojn(kanalo.slug)
            } catch (e: Exception) {
                logw("Ludvico", "Malsukcesis akiri elsendojn por ${kanalo.slug}", e)
                emptyList()
            }
        }
    }

    fun halti() {
        haltiPozicianSpuradon()
        observanto?.cancel()
        observanto = null
    }

    companion object {
        /** Kiom ofte savas pozicion dum ludado (ms). */
        const val POZICIO_SAV_INTERVALO_MS = 5000L
    }
}
