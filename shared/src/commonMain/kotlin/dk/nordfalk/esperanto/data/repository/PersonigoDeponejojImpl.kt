package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.repository.PlejsatatajDeponejo
import dk.nordfalk.esperanto.domain.repository.LastAuxskultitajDeponejo
import dk.nordfalk.esperanto.domain.repository.SercxoDeponejo
import dk.nordfalk.esperanto.domain.repository.AgordojDeponejo
import dk.nordfalk.esperanto.domain.repository.ElsendoDeponejo
import dk.nordfalk.esperanto.logd
import dk.nordfalk.esperanto.logi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlejsatatajDeponejoImpl : PlejsatatajDeponejo {
    private val _plejsatataj = MutableStateFlow<Set<String>>(emptySet())
    override fun observiPlejsatatajn(): StateFlow<Set<String>> = _plejsatataj.asStateFlow()

    override suspend fun baskuliPlejsaton(kanalSlug: String) {
        val nuna = _plejsatataj.value.toMutableSet()
        if (kanalSlug in nuna) nuna.remove(kanalSlug) else nuna.add(kanalSlug)
        _plejsatataj.value = nuna
        logi("Plejsatataj", "Baskulas: $kanalSlug → ${if (kanalSlug in nuna) "aldonita" else "forigita"} (total ${nuna.size})")
    }

    override suspend fun estasPlejsatata(kanalSlug: String): Boolean = kanalSlug in _plejsatataj.value
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

class SercxoDeponejoImpl(
    private val elsendoDeponejo: ElsendoDeponejo,
) : SercxoDeponejo {
    override suspend fun sercxi(taxto: String, limo: Int): List<Elsendo> {
        val rezulto = elsendoDeponejo.sercxiElsendojn(taxto, limo)
        logd("Sercxo", "Serĉas '$taxto' (limo=$limo) — ${rezulto.size} trovoj")
        return rezulto
    }
}

class AgordojDeponejoImpl : AgordojDeponejo {
    private val _lingvo = MutableStateFlow("eo")
    override val lingvo: StateFlow<String> = _lingvo.asStateFlow()

    private val _nurWifi = MutableStateFlow(false)
    override val nurWifi: StateFlow<Boolean> = _nurWifi.asStateFlow()

    override fun fiksiLingvon(lingvo: String) {
        _lingvo.value = lingvo
        logi("Agordoj", "Lingvo → $lingvo")
    }
    override fun fiksiNurWifi(nurWifi: Boolean) {
        _nurWifi.value = nurWifi
        logi("Agordoj", "NurWifi → $nurWifi")
    }
}
