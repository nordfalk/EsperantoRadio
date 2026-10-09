package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.ElshutStato
import dk.nordfalk.esperanto.domain.model.ElshutitaElsendo
import dk.nordfalk.esperanto.domain.repository.ElshutDeponejo
import dk.nordfalk.esperanto.loge
import dk.nordfalk.esperanto.logi
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream

/**
 * Ktor-bazita elŝut-deponejo. Funkcias sur JVM (Desktop + Android).
 *
 * Elŝutas MP3-dosierojn per Ktor HTTP-kliento kaj konservas ilin lokaj.
 * Progreso estas spurata per StateFlow.
 */
class KtorElshutDeponejo(
    private val httpKliento: HttpClient,
    private val elshuthejjo: () -> File,
) : ElshutDeponejo {

    private val json = Json { ignoreUnknownKeys = true }

    private val _elshutoj = MutableStateFlow<Map<String, ElshutitaElsendo>>(emptyMap())
    override fun observiElshutojn(): StateFlow<Map<String, ElshutitaElsendo>> = _elshutoj.asStateFlow()

    // ConcurrentHashMap, ne LinkedHashMap: la mapoj estas alirataj samtempe el la
    // IO-korutinoj de elŝutoj kaj el la UI-fadeno (FAROTA G11)
    private val _statoj = java.util.concurrent.ConcurrentHashMap<String, MutableStateFlow<ElshutStato>>()
    private val joboj = java.util.concurrent.ConcurrentHashMap<String, Job>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        rekargxiElshutojn()
    }

    /**
     * Skanas la elŝuthejmon kaj rekonstruas la elŝut-liston el persistitaj JSON-metadatenoj.
     * Vokata aŭtomate en init, sed ankaŭ re-vokebla mane.
     */
    fun rekargxiElshutojn() {
        val hejjo = elshuthejjo()
        if (!hejjo.exists()) return

        val trovitaj = mutableMapOf<String, ElshutitaElsendo>()

        hejjo.listFiles { f -> f.name.endsWith(".json") }?.forEach { jsonDosiero ->
            try {
                val elsendo = json.decodeFromString(Elsendo.serializer(), jsonDosiero.readText())
                val dosiernomo = sanigiDosiernomon(elsendo.id)
                val mp3 = File(hejjo, "$dosiernomo.mp3")
                if (mp3.exists() && mp3.length() > 0) {
                    val stato = ElshutStato.Preta
                    _statoj[elsendo.id] = MutableStateFlow(stato)
                    trovitaj[elsendo.id] = ElshutitaElsendo(elsendo, mp3.absolutePath, stato, dosierGrando = mp3.length())
                    logi("ElshutDeponejo", "Reŝargis: ${elsendo.titolo} (${mp3.length()} bitokoj)")
                } else {
                    logi("ElshutDeponejo", "Forigas orfan JSON: ${jsonDosiero.name} (MP3 mankas)")
                    jsonDosiero.delete()
                }
            } catch (e: Exception) {
                loge("ElshutDeponejo", "Ne eblis legi JSON: ${jsonDosiero.name}", e)
            }
        }

        _elshutoj.value = trovitaj
        logi("ElshutDeponejo", "Reŝargis ${trovitaj.size} elŝutojn")

        // G11: forigu orfajn partajn MP3-ojn — la JSON estas skribata NUR post kompleta
        // elŝuto, do MP3 sen JSON estas frue rompita parta dosiero el pasinta eraro/paŭzo
        hejjo.listFiles { f -> f.name.endsWith(".mp3") }?.forEach { mp3 ->
            val jsonNomo = mp3.name.removeSuffix(".mp3") + ".json"
            if (!File(hejjo, jsonNomo).exists()) {
                logi("ElshutDeponejo", "Forigas orphan partan dosieron: ${mp3.name} (${mp3.length()} bitokoj)")
                mp3.delete()
            }
        }
    }

    override fun observiElshutStaton(elsendoId: String): StateFlow<ElshutStato> {
        return _statoj.getOrPut(elsendoId) { MutableStateFlow(ElshutStato.NeElshutita) }
    }

    override suspend fun elshuti(elsendo: Elsendo) {
        val id = elsendo.id
        val dosiernomo = sanigiDosiernomon(id)
        logi("ElshutDeponejo", "Elŝutas: ${elsendo.titolo} — ${elsendo.fluo}")

        val hejjo = elshuthejjo()
        hejjo.mkdirs()
        val celdosiero = File(hejjo, "$dosiernomo.mp3")

        val stato = _statoj.getOrPut(id) { MutableStateFlow(ElshutStato.NeElshutita) }

        joboj[id]?.cancel()

        stato.value = ElshutStato.Elshutanta(0f, 0L, 0L)
        _elshutoj.value = _elshutoj.value + (id to ElshutitaElsendo(elsendo, celdosiero.absolutePath, stato.value))

        val job = scope.launch {
            try {
                val respondo = httpKliento.get(elsendo.fluo)
                if (!respondo.status.isSuccess()) {
                    stato.value = ElshutStato.Eraro("HTTP ${respondo.status.value}")
                    _elshutoj.value = _elshutoj.value + (id to ElshutitaElsendo(elsendo, celdosiero.absolutePath, stato.value))
                    loge("ElshutDeponejo", "Elŝuto malsukcesa: HTTP ${respondo.status.value}")
                    return@launch
                }

                val totalajBitokoj = respondo.contentLength() ?: 0L
                val channel = respondo.bodyAsChannel()

                FileOutputStream(celdosiero).use { out ->
                    val buffer = ByteArray(8192)
                    var elshutitaj = 0L
                    while (isActive) {
                        val read = channel.readAvailable(buffer)
                        if (read <= 0) break
                        out.write(buffer, 0, read)
                        elshutitaj += read
                        val progreso = if (totalajBitokoj > 0) elshutitaj.toFloat() / totalajBitokoj else 0f
                        stato.value = ElshutStato.Elshutanta(progreso, elshutitaj, totalajBitokoj)
                    }
                    // G10: frua EOF (servilo fermis la konekton) — ne marku triligitan
                    // dosieron "Preta"; ĝi estus persistita kaj neniam re-elŝutata
                    if (totalajBitokoj > 0 && elshutitaj < totalajBitokoj) {
                        throw java.io.IOException("Triligita: ricevitaj $elshutitaj el $totalajBitokoj bajtoj")
                    }
                }

                stato.value = ElshutStato.Preta
                _elshutoj.value = _elshutoj.value + (id to ElshutitaElsendo(elsendo, celdosiero.absolutePath, stato.value, dosierGrando = celdosiero.length()))
                // Persistu metadatenojn por rekargxo ce restarto
                val jsonDosiero = File(hejjo, "$dosiernomo.json")
                jsonDosiero.writeText(json.encodeToString(Elsendo.serializer(), elsendo))
                logi("ElshutDeponejo", "Elŝuto kompleta: ${elsendo.titolo} → ${celdosiero.absolutePath} (${celdosiero.length()} bitokoj)")
            } catch (e: kotlinx.coroutines.CancellationException) {
                // haltigi() — la stato (Pauxzita) estas agordata de haltigi mem;
                // forigu la partan dosieron (G11: partaj dosieroj ne akumuliĝu)
                try { celdosiero.delete() } catch (e2: Exception) {
                    loge("ElshutDeponejo", "Ne eblis forigi partan dosieron: ${celdosiero.name}", e2)
                }
                throw e
            } catch (e: Throwable) {
                // Throwable, ne Exception: la Js-motoro (wasmJs) ĵetas kotlin.Error (FAROTA G26)
                loge("ElshutDeponejo", "Elŝuto malsukcesa: ${elsendo.id}", e)
                stato.value = ElshutStato.Eraro(e.message ?: "Nekonata eraro")
                _elshutoj.value = _elshutoj.value + (id to ElshutitaElsendo(elsendo, celdosiero.absolutePath, stato.value))
                // G11: forigu la partan dosieron — ĝi estas nemankebla kaj okupus spacon por eterne
                try { celdosiero.delete() } catch (e2: Exception) {
                    loge("ElshutDeponejo", "Ne eblis forigi partan dosieron: ${celdosiero.name}", e2)
                }
            }
        }
        joboj[id] = job
    }

    override suspend fun haltigi(elsendoId: String) {
        logi("ElshutDeponejo", "Haltigas elŝuton: $elsendoId")
        joboj[elsendoId]?.cancel()
        joboj.remove(elsendoId)
        val stato = _statoj[elsendoId]
        if (stato != null) {
            stato.value = ElshutStato.Pauxzita
            val nuna = _elshutoj.value[elsendoId]
            if (nuna != null) {
                _elshutoj.value = _elshutoj.value + (elsendoId to nuna.copy(stato = ElshutStato.Pauxzita))
            }
        }
    }

    override suspend fun forigi(elsendoId: String) {
        logi("ElshutDeponejo", "Forigas elŝuton: $elsendoId")
        joboj[elsendoId]?.cancel()
        joboj.remove(elsendoId)
        val elshutita = _elshutoj.value[elsendoId]
        if (elshutita != null) {
            try {
                File(elshutita.dosieroVojo).delete()
            } catch (e: Exception) {
                loge("ElshutDeponejo", "Ne eblis forigi dosieron: ${elshutita.dosieroVojo}", e)
            }
            // Forigu ankaŭ la JSON-metadatumbazon
            val hejjo = elshuthejjo()
            val dosiernomo = sanigiDosiernomon(elsendoId)
            val jsonDosiero = File(hejjo, "$dosiernomo.json")
            if (jsonDosiero.exists()) jsonDosiero.delete()
        }
        _elshutoj.value = _elshutoj.value - elsendoId
        _statoj[elsendoId]?.value = ElshutStato.NeElshutita
    }

    override suspend fun getLokaDosieroVojo(elsendoId: String): String? {
        val elshutita = _elshutoj.value[elsendoId] ?: return null
        return if (elshutita.stato is ElshutStato.Preta) elshutita.dosieroVojo else null
    }

    override fun estasElshutita(elsendoId: String): Boolean {
        return _elshutoj.value[elsendoId]?.stato is ElshutStato.Preta
    }
}
