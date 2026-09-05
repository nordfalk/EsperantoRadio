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
 * - Se sugestoj estas provizitaj kaj la uzanto NE faris sxangxojn,
 *   la sugestoj estas montritaj (cxiuj malaktivaj).
 * - Se la uzanto faras ian ajn sxangxon (krei, forigi, baskuli),
 *   la sugestoj malaperas kaj nur la uzantaj alarmoj restas.
 * - Se la uzanto forigis cxiujn alarmojn, la listo restas malplena
 *   (la sugestoj ne revenas).
 */
class MemorAlarmoDeponejo(
    sugestoj: List<Alarmo> = emptyList(),
) : AlarmoDeponejo {
    private val _alarmoj = MutableStateFlow<List<Alarmo>>(emptyList())
    override fun observiAlarmojn(): StateFlow<List<Alarmo>> = _alarmoj.asStateFlow()

    private val sugestoj = sugestoj.map { it.copy(aktiva = false) }
    private var uzantoModifis = false
    private var nextId = 1000

    init {
        if (this.sugestoj.isNotEmpty()) {
            _alarmoj.value = this.sugestoj
            logi("AlarmoDeponejo", "Montras ${this.sugestoj.size} sugestojn (malaktivaj)")
        }
    }

    private fun foriguSugestojnSeNecese() {
        if (!uzantoModifis) {
            uzantoModifis = true
            val sugestIds = sugestoj.map { it.id }.toSet()
            _alarmoj.value = _alarmoj.value.filter { it.id !in sugestIds }
            logi("AlarmoDeponejo", "Forigis sugestojn — uzanto modifiis")
        }
    }

    override suspend fun krei(alarmo: Alarmo) {
        foriguSugestojnSeNecese()
        val nova = alarmo.copy(id = nextId++)
        _alarmoj.value = _alarmoj.value + nova
        logi("AlarmoDeponejo", "Kreis: ${nova.tempoTeksto} ${nova.ripetoTeksto} → ${nova.kanalSlug}")
    }

    override suspend fun ghisdatigi(alarmo: Alarmo) {
        foriguSugestojnSeNecese()
        _alarmoj.value = _alarmoj.value.map { if (it.id == alarmo.id) alarmo else it }
        logi("AlarmoDeponejo", "Ĝisdatigis: ${alarmo.id}")
    }

    override suspend fun forigi(alarmoId: Int) {
        foriguSugestojnSeNecese()
        _alarmoj.value = _alarmoj.value.filter { it.id != alarmoId }
        logi("AlarmoDeponejo", "Forigis: $alarmoId")
    }

    override suspend fun baskuliAktivon(alarmoId: Int) {
        foriguSugestojnSeNecese()
        _alarmoj.value = _alarmoj.value.map {
            if (it.id == alarmoId) it.copy(aktiva = !it.aktiva) else it
        }
        val alarmo = _alarmoj.value.find { it.id == alarmoId }
        logi("AlarmoDeponejo", "Baskulis: $alarmoId → ${if (alarmo?.aktiva == true) "aktiva" else "malaktiva"}")
    }
}
