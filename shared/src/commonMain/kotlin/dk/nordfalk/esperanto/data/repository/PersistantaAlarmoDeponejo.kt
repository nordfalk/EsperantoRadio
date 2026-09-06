package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.domain.model.Alarmo
import dk.nordfalk.esperanto.domain.repository.AlarmoDeponejo
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.logw
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Persistanta AlarmoDeponejo. Uzas multiplatform-settings kun JSON-serializo.
 *
 * - Se persistitaj alarmoj ekzistas, ili estas sxargxitaj cxe starto.
 * - Se ne, la sugestoj estas montritaj (malaktivaj).
 * - Cxiu sxangxo estas tuj persistita.
 */
class PersistantaAlarmoDeponejo(
    private val settings: Settings,
    sugestoj: List<Alarmo> = emptyList(),
    private val skedilo: AlarmoSkedilo? = null,
) : AlarmoDeponejo {

    private val json = Json { ignoreUnknownKeys = true }
    private val key = "alarmoj"

    private val sugestoj = sugestoj.map { it.copy(aktiva = false) }
    private var nextId = 1000

    private val _alarmoj = MutableStateFlow<List<Alarmo>>(legu())

    override fun observiAlarmojn(): StateFlow<List<Alarmo>> = _alarmoj.asStateFlow()

    private fun legu(): List<Alarmo> {
        val str = settings.getString(key, "")
        if (str.isBlank()) {
            // Unua fojo — montru sugestojn
            if (sugestoj.isNotEmpty()) {
                // Certigu unikajn ID-ojn
                val uzitajIdj = mutableSetOf<Int>()
                val unikaj = sugestoj.map { s ->
                    var id = s.id
                    while (id in uzitajIdj) {
                        logi("AlarmoDeponejo", "ID-kolizio por $s — asignas novan")
                        id = nextId++
                    }
                    uzitajIdj.add(id)
                    s.copy(id = id)
                }
                nextId = maxOf(nextId, (uzitajIdj.maxOrNull() ?: 0) + 1)
                logi("AlarmoDeponejo", "Montras ${unikaj.size} sugestojn (malaktivaj, nenio persistita)")
                return unikaj
            }
            return emptyList()
        }
        return try {
            val alarmoj = json.decodeFromString(ListSerializer(Alarmo.serializer()), str)
            nextId = maxOf(nextId, (alarmoj.maxOfOrNull { it.id } ?: 0) + 1)
            logi("AlarmoDeponejo", "Sargxis ${alarmoj.size} persistitajn alarmojn")
            alarmoj
        } catch (e: Exception) {
            logw("AlarmoDeponejo", "Ne eblis legi persistitajn alarmojn", e)
            sugestoj
        }
    }

    private fun persistu() {
        val str = json.encodeToString(ListSerializer(Alarmo.serializer()), _alarmoj.value)
        settings.putString(key, str)
    }

    override suspend fun krei(alarmo: Alarmo) {
        val nova = alarmo.copy(id = nextId++)
        _alarmoj.value = _alarmoj.value + nova
        persistu()
        skedilo?.skedi(nova)
        logi("AlarmoDeponejo", "Kreis: ${nova.tempoTeksto} ${nova.ripetoTeksto} → ${nova.kanaloSlug} (nova id=${nova.id})")
    }

    override suspend fun ghisdatigi(alarmo: Alarmo) {
        skedilo?.malplani(alarmo.id)
        _alarmoj.value = _alarmoj.value.map { if (it.id == alarmo.id) alarmo else it }
        persistu()
        if (alarmo.aktiva) skedilo?.skedi(alarmo) else skedilo?.malplani(alarmo.id)
        logi("AlarmoDeponejo", "Ĝisdatigis: id=${alarmo.id} tempo=${alarmo.tempoTeksto}")
    }

    override suspend fun forigi(alarmoId: Int) {
        val antauxa = _alarmoj.value.find { it.id == alarmoId }
        skedilo?.malplani(alarmoId)
        _alarmoj.value = _alarmoj.value.filter { it.id != alarmoId }
        persistu()
        logi("AlarmoDeponejo", "Forigis: id=$alarmoId tempo=${antauxa?.tempoTeksto} — restas ${_alarmoj.value.size} alarmoj")
    }

    override suspend fun baskuliAktivon(alarmoId: Int) {
        _alarmoj.value = _alarmoj.value.map {
            if (it.id == alarmoId) it.copy(aktiva = !it.aktiva) else it
        }
        persistu()
        val alarmo = _alarmoj.value.find { it.id == alarmoId }
        if (alarmo != null) {
            if (alarmo.aktiva) skedilo?.skedi(alarmo) else skedilo?.malplani(alarmoId)
        }
        logi("AlarmoDeponejo", "Baskulis: id=$alarmoId tempo=${alarmo?.tempoTeksto} → ${if (alarmo?.aktiva == true) "aktiva" else "malaktiva"}")
    }
}
