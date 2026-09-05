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

    private val _elshutoj = MutableStateFlow<Map<String, ElshutitaElsendo>>(emptyMap())
    override fun observiElshutojn(): StateFlow<Map<String, ElshutitaElsendo>> = _elshutoj.asStateFlow()

    private val _statoj = mutableMapOf<String, MutableStateFlow<ElshutStato>>()
    private val joboj = mutableMapOf<String, Job>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun observiElshutStaton(elsendoId: String): StateFlow<ElshutStato> {
        return _statoj.getOrPut(elsendoId) { MutableStateFlow(ElshutStato.NeElshutita) }
    }

    override suspend fun elshuti(elsendo: Elsendo) {
        val id = elsendo.id
        logi("ElshutDeponejo", "Elŝutas: ${elsendo.titolo} — ${elsendo.stream}")

        val hejjo = elshuthejjo()
        hejjo.mkdirs()
        val celdosiero = File(hejjo, "$id.mp3")

        val stato = _statoj.getOrPut(id) { MutableStateFlow(ElshutStato.NeElshutita) }

        joboj[id]?.cancel()

        stato.value = ElshutStato.Elshutanta(0f, 0L, 0L)
        _elshutoj.value = _elshutoj.value + (id to ElshutitaElsendo(elsendo, celdosiero.absolutePath, stato.value))

        val job = scope.launch {
            try {
                val respondo = httpKliento.get(elsendo.stream)
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
                }

                stato.value = ElshutStato.Preta
                _elshutoj.value = _elshutoj.value + (id to ElshutitaElsendo(elsendo, celdosiero.absolutePath, stato.value))
                logi("ElshutDeponejo", "Elŝuto kompleta: ${elsendo.titolo} → ${celdosiero.absolutePath} (${celdosiero.length()} bitokoj)")
            } catch (e: Exception) {
                loge("ElshutDeponejo", "Elŝuto malsukcesa: ${elsendo.id}", e)
                stato.value = ElshutStato.Eraro(e.message ?: "Nekonata eraro")
                _elshutoj.value = _elshutoj.value + (id to ElshutitaElsendo(elsendo, celdosiero.absolutePath, stato.value))
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
