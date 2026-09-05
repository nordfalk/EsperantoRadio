package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.domain.model.Alarmo
import dk.nordfalk.esperanto.domain.repository.AlarmoDeponejo
import dk.nordfalk.esperanto.logi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * En-memora AlarmoDeponejo. Platform-specifa planado (AlarmManager) venos poste.
 *
 * Konduto:
 * - Se sugestoj estas provizitaj, ili estas montritaj cxe starto (cxiuj malaktivaj).
 * - La uzanto povas libere baskuli, forigi aŭ krei alarmojn.
 * - Ne persistas — post restarto la sugestoj revenas (persisto venos poste).
 */
class MemorAlarmoDeponejo(
    sugestoj: List<Alarmo> = emptyList(),
) : AlarmoDeponejo {
    private val _alarmoj = MutableStateFlow<List<Alarmo>>(emptyList())
    override fun observiAlarmojn(): StateFlow<List<Alarmo>> = _alarmoj.asStateFlow()

    private var nextId = 1000

    init {
        if (sugestoj.isNotEmpty()) {
            _alarmoj.value = sugestoj.map { it.copy(aktiva = false) }
            logi("AlarmoDeponejo", "Montras ${sugestoj.size} sugestojn (malaktivaj)")
        }
    }

    override suspend fun krei(alarmo: Alarmo) {
        val nova = alarmo.copy(id = nextId++)
        _alarmoj.value = _alarmoj.value + nova
        logi("AlarmoDeponejo", "Kreis: ${nova.tempoTeksto} ${nova.ripetoTeksto} → ${nova.kanalSlug} (nova id=${nova.id})")
    }

    override suspend fun ghisdatigi(alarmo: Alarmo) {
        val antauxa = _alarmoj.value.find { it.id == alarmo.id }
        _alarmoj.value = _alarmoj.value.map { if (it.id == alarmo.id) alarmo else it }
        logi("AlarmoDeponejo", "Ĝisdatigis: id=${alarmo.id} tempo=${alarmo.tempoTeksto} (antaŭe: ${antauxa?.tempoTeksto})")
    }

    override suspend fun forigi(alarmoId: Int) {
        val antauxa = _alarmoj.value.find { it.id == alarmoId }
        _alarmoj.value = _alarmoj.value.filter { it.id != alarmoId }
        logi("AlarmoDeponejo", "Forigis: id=$alarmoId tempo=${antauxa?.tempoTeksto} — restas ${_alarmoj.value.size} alarmoj: [${_alarmoj.value.joinToString { "${it.id}:${it.tempoTeksto}" }}]")
    }

    override suspend fun baskuliAktivon(alarmoId: Int) {
        _alarmoj.value = _alarmoj.value.map {
            if (it.id == alarmoId) it.copy(aktiva = !it.aktiva) else it
        }
        val alarmo = _alarmoj.value.find { it.id == alarmoId }
        logi("AlarmoDeponejo", "Baskulis: id=$alarmoId tempo=${alarmo?.tempoTeksto} → ${if (alarmo?.aktiva == true) "aktiva" else "malaktiva"}")
    }
}
