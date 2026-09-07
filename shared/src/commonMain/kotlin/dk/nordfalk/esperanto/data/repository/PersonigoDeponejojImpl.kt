package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.repository.PlejŝatatajDeponejo
import dk.nordfalk.esperanto.domain.repository.LastAuxskultitajDeponejo
import dk.nordfalk.esperanto.domain.repository.SercxoDeponejo
import dk.nordfalk.esperanto.domain.repository.AgordojDeponejo
import dk.nordfalk.esperanto.domain.repository.ElsendoDeponejo
import dk.nordfalk.esperanto.logd
import dk.nordfalk.esperanto.logi
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlejŝatatajDeponejoImpl : PlejŝatatajDeponejo {
    private val _plejŝatataj = MutableStateFlow<Set<String>>(emptySet())
    override fun observiPlejŝatatajn(): StateFlow<Set<String>> = _plejŝatataj.asStateFlow()

    override suspend fun baskuliPlejŝaton(kanaloSlug: String) {
        val nuna = _plejŝatataj.value.toMutableSet()
        if (kanaloSlug in nuna) nuna.remove(kanaloSlug) else nuna.add(kanaloSlug)
        _plejŝatataj.value = nuna
        logi("Plejŝatataj", "Baskulas: $kanaloSlug → ${if (kanaloSlug in nuna) "aldonita" else "forigita"} (total ${nuna.size})")
    }

    override suspend fun estasPlejŝatata(kanaloSlug: String): Boolean = kanaloSlug in _plejŝatataj.value
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
    override suspend fun sercxi(teksto: String, limo: Int): List<Elsendo> {
        val rezulto = elsendoDeponejo.sercxiElsendojn(teksto, limo)
        logd("Sercxo", "Serĉas '$teksto' (limo=$limo) — ${rezulto.size} trovoj")
        return rezulto
    }
}

class AgordojDeponejoImpl(
    private val settings: Settings? = null,
) : AgordojDeponejo {
    private val _lingvo = MutableStateFlow("eo")
    override val lingvo: StateFlow<String> = _lingvo.asStateFlow()

    private val _nurWifi = MutableStateFlow(false)
    override val nurWifi: StateFlow<Boolean> = _nurWifi.asStateFlow()

    private val _temo = MutableStateFlow("ANTONIA")
    override val temo: StateFlow<String> = _temo.asStateFlow()

    private val _sciigoj = MutableStateFlow(settings?.getBoolean("sciigoj", true) ?: true)
    override val sciigoj: StateFlow<Boolean> = _sciigoj.asStateFlow()

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
    override fun fiksiSciigojn(sxaltita: Boolean) {
        _sciigoj.value = sxaltita
        settings?.putBoolean("sciigoj", sxaltita)
        logi("Agordoj", "Sciigoj → $sxaltita")
    }
}
