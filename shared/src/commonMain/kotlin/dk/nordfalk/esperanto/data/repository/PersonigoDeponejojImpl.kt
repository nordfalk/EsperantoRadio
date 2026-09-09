package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.LudataElsendo
import dk.nordfalk.esperanto.domain.repository.PlejsatatajDeponejo
import dk.nordfalk.esperanto.domain.repository.LastAuxskultitajDeponejo
import dk.nordfalk.esperanto.domain.repository.LudatojDeponejo
import dk.nordfalk.esperanto.domain.repository.SercxoDeponejo
import dk.nordfalk.esperanto.domain.repository.AgordojDeponejo
import dk.nordfalk.esperanto.domain.repository.ElsendoDeponejo
import dk.nordfalk.esperanto.logd
import dk.nordfalk.esperanto.logi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class PlejsatatajDeponejoImpl : PlejsatatajDeponejo {
    private val _plejsatataj = MutableStateFlow<Set<String>>(emptySet())
    override fun observiPlejsatatajn(): StateFlow<Set<String>> = _plejsatataj.asStateFlow()

    override suspend fun baskuliPlejsaton(kanaloSlug: String) {
        val nuna = _plejsatataj.value.toMutableSet()
        if (kanaloSlug in nuna) nuna.remove(kanaloSlug) else nuna.add(kanaloSlug)
        _plejsatataj.value = nuna
        logi("Plejsatataj", "Baskulas: $kanaloSlug → ${if (kanaloSlug in nuna) "aldonita" else "forigita"} (total ${nuna.size})")
    }

    override suspend fun estasPlejsatata(kanaloSlug: String): Boolean = kanaloSlug in _plejsatataj.value
}

class LastAuxskultitajDeponejoImpl : LastAuxskultitajDeponejo {
    private val _listo = MutableStateFlow<List<Elsendo>>(emptyList())
    private val pozicioj = mutableMapOf<String, Long>()

    override fun observiLastAuxskultitajn(): StateFlow<List<Elsendo>> = _listo.asStateFlow()

    override suspend fun registri(elsendo: Elsendo) {
        val nuna = _listo.value.toMutableList()
        nuna.removeAll { it.id == elsendo.id }
        nuna.add(0, elsendo)
        _listo.value = nuna.take(50) // LRU-maks 50
        logd("LastAuxskultitaj", "Registras: ${elsendo.id} — ${elsendo.titolo}")
    }

    override suspend fun getPozicio(elsendoId: String): Long? = pozicioj[elsendoId]
}

/**
 * Memora LudatojDeponejo — sen persistaj flankaj efikoj.
 * Uzu nur dum Preview kaj testoj. Por produktado uzu PersistantaLudatojDeponejo.
 */
@OptIn(ExperimentalTime::class)
class LudatojDeponejoMaketo : LudatojDeponejo {
    private val _ludatoj = MutableStateFlow<Map<String, LudataElsendo>>(emptyMap())
    override fun observiLudatojn(): StateFlow<Map<String, LudataElsendo>> = _ludatoj.asStateFlow()

    override suspend fun registriPozicion(elsendoId: String, kanaloSlug: String, pozicioMs: Long, dauroMs: Long) {
        val nuna = _ludatoj.value.toMutableMap()
        val ekzista = nuna[elsendoId]
        nuna[elsendoId] = LudataElsendo(
            elsendoId = elsendoId,
            kanaloSlug = kanaloSlug,
            pozicioMs = pozicioMs,
            dauroMs = if (dauroMs > 0) dauroMs else (ekzista?.dauroMs ?: 0),
            finita = ekzista?.finita ?: false,
            lasteLudita = Clock.System.now().toEpochMilliseconds(),
        )
        _ludatoj.value = nuna
    }

    override suspend fun markiFinita(elsendoId: String, kanaloSlug: String) {
        val nuna = _ludatoj.value.toMutableMap()
        val ekzista = nuna[elsendoId]
        nuna[elsendoId] = LudataElsendo(
            elsendoId = elsendoId,
            kanaloSlug = kanaloSlug,
            pozicioMs = ekzista?.pozicioMs ?: 0,
            dauroMs = ekzista?.dauroMs ?: 0,
            finita = true,
            lasteLudita = Clock.System.now().toEpochMilliseconds(),
        )
        _ludatoj.value = nuna
    }

    override suspend fun getLudato(elsendoId: String): LudataElsendo? = _ludatoj.value[elsendoId]
    override suspend fun estasFinita(elsendoId: String): Boolean = _ludatoj.value[elsendoId]?.finita ?: false
    override suspend fun getPozicio(elsendoId: String): Long? = _ludatoj.value[elsendoId]?.pozicioMs?.takeIf { it > 0 }
}

class SercxoDeponejoImpl(
    private val elsendoDeponejo: ElsendoDeponejo,
) : SercxoDeponejo {
    override suspend fun sercxi(teksto: String, limo: Int): List<Elsendo> {
        val rezulto = elsendoDeponejo.sercxiElsendojn(teksto, limo)
        logd("Sercxo", "Serĉas '$teksto' (limo=$limo) — ${rezulto.size} trovoj")
        return rezulto
    }
}

class AgordojDeponejoImpl : AgordojDeponejo {
    private val _lingvo = MutableStateFlow("eo")
    override val lingvo: StateFlow<String> = _lingvo.asStateFlow()

    private val _nurWifi = MutableStateFlow(false)
    override val nurWifi: StateFlow<Boolean> = _nurWifi.asStateFlow()

    private val _temo = MutableStateFlow("ANTONIA")
    override val temo: StateFlow<String> = _temo.asStateFlow()

    override fun fiksiLingvon(lingvo: String) {
        _lingvo.value = lingvo
        logi("Agordoj", "Lingvo → $lingvo")
    }
    override fun fiksiNurWifi(nurWifi: Boolean) {
        _nurWifi.value = nurWifi
        logi("Agordoj", "NurWifi → $nurWifi")
    }
    override fun fiksiTemon(temo: String) {
        _temo.value = temo
        logi("Agordoj", "Temo → $temo")
    }
}
