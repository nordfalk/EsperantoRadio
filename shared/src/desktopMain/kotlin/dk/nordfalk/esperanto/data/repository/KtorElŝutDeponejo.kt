package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.ElŝutStato
import dk.nordfalk.esperanto.domain.model.ElŝutitaElsendo
import dk.nordfalk.esperanto.domain.repository.ElŝutDeponejo
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
class KtorElŝutDeponejo(
    private val httpKliento: HttpClient,
    private val elŝuthejjo: () -> File,
) : ElŝutDeponejo {

    private val json = Json { ignoreUnknownKeys = true }

    private val _elŝutoj = MutableStateFlow<Map<String, ElŝutitaElsendo>>(emptyMap())
    override fun observiElŝutojn(): StateFlow<Map<String, ElŝutitaElsendo>> = _elŝutoj.asStateFlow()

    // ConcurrentHashMap, ne LinkedHashMap: la mapoj estas alirataj samtempe el la
    // IO-korutinoj de elŝutoj kaj el la UI-fadeno (PR #89)
    private val _statoj = java.util.concurrent.ConcurrentHashMap<String, MutableStateFlow<ElŝutStato>>()
    private val joboj = java.util.concurrent.ConcurrentHashMap<String, Job>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        rekarĝiElŝutojn()
    }

    /**
     * Skanas la elŝutan dosierujon kaj rekonstruas la elŝutliston el persistitaj JSON-metadatumoj.
     * Vokata aŭtomate en init, sed ankaŭ re-vokebla mane.
     */
    fun rekarĝiElŝutojn() {
        val hejjo = elŝuthejjo()
        if (!hejjo.exists()) return

        val trovitaj = mutableMapOf<String, ElŝutitaElsendo>()

        hejjo.listFiles { f -> f.name.endsWith(".json") }?.forEach { jsonDosiero ->
            try {
                val elsendo = json.decodeFromString(Elsendo.serializer(), jsonDosiero.readText())
                val dosiernomo = sanigiDosiernomon(elsendo.id)
                val mp3 = File(hejjo, "$dosiernomo.mp3")
                if (mp3.exists() && mp3.length() > 0) {
                    val stato = ElŝutStato.Preta
                    _statoj[elsendo.id] = MutableStateFlow(stato)
                    trovitaj[elsendo.id] = ElŝutitaElsendo(elsendo, mp3.absolutePath, stato, dosierGrando = mp3.length())
                    logi("ElŝutDeponejo", "Reŝargis: ${elsendo.titolo} (${mp3.length()} bitokoj)")
                } else {
                    logi("ElŝutDeponejo", "Forigas orfan JSON: ${jsonDosiero.name} (MP3 mankas)")
                    jsonDosiero.delete()
                }
            } catch (e: Exception) {
                loge("ElŝutDeponejo", "Ne eblis legi JSON: ${jsonDosiero.name}", e)
            }
        }

        _elŝutoj.value = trovitaj
        logi("ElŝutDeponejo", "Reŝargis ${trovitaj.size} elŝutojn")

        // G11: forigu orfajn partajn MP3-ojn — la JSON estas skribata NUR post kompleta
        // elŝuto, do MP3 sen JSON estas frue rompita parta dosiero el pasinta eraro/paŭzo
        hejjo.listFiles { f -> f.name.endsWith(".mp3") }?.forEach { mp3 ->
            val jsonNomo = mp3.name.removeSuffix(".mp3") + ".json"
            if (!File(hejjo, jsonNomo).exists()) {
                logi("ElŝutDeponejo", "Forigas orfan partan dosieron: ${mp3.name} (${mp3.length()} bitokoj)")
                mp3.delete()
            }
        }
    }

    override fun observiElŝutStaton(elsendoId: String): StateFlow<ElŝutStato> {
        return _statoj.getOrPut(elsendoId) { MutableStateFlow(ElŝutStato.NeElŝutita) }
    }

    override suspend fun elŝuti(elsendo: Elsendo) {
        val id = elsendo.id
        val dosiernomo = sanigiDosiernomon(id)
        logi("ElŝutDeponejo", "Elŝutas: ${elsendo.titolo} — ${elsendo.fluo}")

        val hejjo = elŝuthejjo()
        hejjo.mkdirs()
        val celdosiero = File(hejjo, "$dosiernomo.mp3")

        val stato = _statoj.getOrPut(id) { MutableStateFlow(ElŝutStato.NeElŝutita) }

        joboj[id]?.cancel()

        stato.value = ElŝutStato.Elŝutanta(0f, 0L, 0L)
        _elŝutoj.value = _elŝutoj.value + (id to ElŝutitaElsendo(elsendo, celdosiero.absolutePath, stato.value))

        val job = scope.launch {
            val mi = coroutineContext[Job]
            // Se pli NOVA elŝuto transprenis la identigilon, NE tuŝu ĝian dosieron
            // (revizio de PR #89: la nuligita malnova tasko forviŝus la partan dosieron
            // de la nova elŝuto kun la sama vojo)
            fun transprenitaDePliNova() = joboj[id] != null && joboj[id] !== mi
            try {
                val respondo = httpKliento.get(elsendo.fluo)
                if (!respondo.status.isSuccess()) {
                    stato.value = ElŝutStato.Eraro("HTTP ${respondo.status.value}")
                    _elŝutoj.value = _elŝutoj.value + (id to ElŝutitaElsendo(elsendo, celdosiero.absolutePath, stato.value))
                    loge("ElŝutDeponejo", "Elŝuto malsukcesa: HTTP ${respondo.status.value}")
                    return@launch
                }

                val totalajBitokoj = respondo.contentLength() ?: 0L
                val channel = respondo.bodyAsChannel()

                FileOutputStream(celdosiero).use { out ->
                    val buffer = ByteArray(8192)
                    var elŝutitaj = 0L
                    while (isActive) {
                        val read = channel.readAvailable(buffer)
                        if (read <= 0) break
                        out.write(buffer, 0, read)
                        elŝutitaj += read
                        val progreso = if (totalajBitokoj > 0) elŝutitaj.toFloat() / totalajBitokoj else 0f
                        stato.value = ElŝutStato.Elŝutanta(progreso, elŝutitaj, totalajBitokoj)
                    }
                    // G10: frua EOF (servilo fermis la konekton) — ne marku triligitan
                    // dosieron "Preta"; ĝi estus persistita kaj neniam re-elŝutata
                    if (totalajBitokoj > 0 && elŝutitaj < totalajBitokoj) {
                        throw java.io.IOException("Triligita: ricevitaj $elŝutitaj bitokoj el $totalajBitokoj")
                    }
                }

                stato.value = ElŝutStato.Preta
                _elŝutoj.value = _elŝutoj.value + (id to ElŝutitaElsendo(elsendo, celdosiero.absolutePath, stato.value, dosierGrando = celdosiero.length()))
                // Persistu metadatumojn por reŝargo ĉe restarto
                val jsonDosiero = File(hejjo, "$dosiernomo.json")
                jsonDosiero.writeText(json.encodeToString(Elsendo.serializer(), elsendo))
                logi("ElŝutDeponejo", "Elŝuto kompleta: ${elsendo.titolo} → ${celdosiero.absolutePath} (${celdosiero.length()} bitokoj)")
            } catch (e: kotlinx.coroutines.CancellationException) {
                // haltigi() — la stato (Paŭzita) estas agordata de haltigi mem;
                // forigu la partan dosieron (PR #89: partaj dosieroj ne akumuliĝu) —
                // sed nur se neniu pli nova elŝuto transprenis la saman dosieron
                if (!transprenitaDePliNova()) {
                    try { celdosiero.delete() } catch (e2: Exception) {
                        loge("ElŝutDeponejo", "Ne eblis forigi partan dosieron: ${celdosiero.name}", e2)
                    }
                }
                throw e
            } catch (e: Throwable) {
                // Throwable, ne Exception: la Js-motoro (wasmJs) ĵetas kotlin.Error (PR #89)
                loge("ElŝutDeponejo", "Elŝuto malsukcesa: ${elsendo.id}", e)
                stato.value = ElŝutStato.Eraro(e.message ?: "Nekonata eraro")
                _elŝutoj.value = _elŝutoj.value + (id to ElŝutitaElsendo(elsendo, celdosiero.absolutePath, stato.value))
                // G11: forigu la partan dosieron — ĝi estas nemankebla kaj okupus spacon por eterne
                if (!transprenitaDePliNova()) {
                    try { celdosiero.delete() } catch (e2: Exception) {
                        loge("ElŝutDeponejo", "Ne eblis forigi partan dosieron: ${celdosiero.name}", e2)
                    }
                }
            }
        }
        joboj[id] = job
    }

    override suspend fun haltigi(elsendoId: String) {
        logi("ElŝutDeponejo", "Haltigas elŝuton: $elsendoId")
        joboj[elsendoId]?.cancel()
        joboj.remove(elsendoId)
        val stato = _statoj[elsendoId]
        if (stato != null) {
            stato.value = ElŝutStato.Paŭzita
            val nuna = _elŝutoj.value[elsendoId]
            if (nuna != null) {
                _elŝutoj.value = _elŝutoj.value + (elsendoId to nuna.copy(stato = ElŝutStato.Paŭzita))
            }
        }
    }

    override suspend fun forigi(elsendoId: String) {
        logi("ElŝutDeponejo", "Forigas elŝuton: $elsendoId")
        joboj[elsendoId]?.cancel()
        joboj.remove(elsendoId)
        val elŝutita = _elŝutoj.value[elsendoId]
        if (elŝutita != null) {
            try {
                File(elŝutita.dosieroVojo).delete()
            } catch (e: Exception) {
                loge("ElŝutDeponejo", "Ne eblis forigi dosieron: ${elŝutita.dosieroVojo}", e)
            }
            // Forigu ankaŭ la JSON-metadatumbazon
            val hejjo = elŝuthejjo()
            val dosiernomo = sanigiDosiernomon(elsendoId)
            val jsonDosiero = File(hejjo, "$dosiernomo.json")
            if (jsonDosiero.exists()) jsonDosiero.delete()
        }
        _elŝutoj.value = _elŝutoj.value - elsendoId
        _statoj[elsendoId]?.value = ElŝutStato.NeElŝutita
    }

    override suspend fun getLokaDosieroVojo(elsendoId: String): String? {
        val elŝutita = _elŝutoj.value[elsendoId] ?: return null
        return if (elŝutita.stato is ElŝutStato.Preta) elŝutita.dosieroVojo else null
    }

    override fun estasElŝutita(elsendoId: String): Boolean {
        return _elŝutoj.value[elsendoId]?.stato is ElŝutStato.Preta
    }
}
