package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.domain.model.Alarmo
import dk.nordfalk.esperanto.domain.repository.AlarmoDeponejo
import dk.nordfalk.esperanto.logi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * En-memora AlarmoDeponejo. Platform-specifa planado (AlarmManager) venos poste.
 */
class MemorAlarmoDeponejo : AlarmoDeponejo {
    private val _alarmoj = MutableStateFlow<List<Alarmo>>(emptyList())
    override fun observiAlarmojn(): StateFlow<List<Alarmo>> = _alarmoj.asStateFlow()

    private var nextId = 1

    override suspend fun krei(alarmo: Alarmo) {
        val nova = alarmo.copy(id = nextId++)
        _alarmoj.value = _alarmoj.value + nova
        logi("AlarmoDeponejo", "Kreis: ${nova.tempoTeksto} ${nova.ripetoTeksto} → ${nova.kanalSlug}")
    }

    override suspend fun ghisdatigi(alarmo: Alarmo) {
        _alarmoj.value = _alarmoj.value.map { if (it.id == alarmo.id) alarmo else it }
        logi("AlarmoDeponejo", "Ĝisdatigis: ${alarmo.id}")
    }

    override suspend fun forigi(alarmoId: Int) {
        _alarmoj.value = _alarmoj.value.filter { it.id != alarmoId }
        logi("AlarmoDeponejo", "Forigis: $alarmoId")
    }

    override suspend fun baskuliAktivon(alarmoId: Int) {
        _alarmoj.value = _alarmoj.value.map {
            if (it.id == alarmoId) it.copy(aktiva = !it.aktiva) else it
        }
        val alarmo = _alarmoj.value.find { it.id == alarmoId }
        logi("AlarmoDeponejo", "Baskulis: $alarmoId → ${if (alarmo?.aktiva == true) "aktiva" else "malaktiva"}")
    }
}
