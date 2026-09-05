package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.ElshutStato
import dk.nordfalk.esperanto.domain.model.ElshutitaElsendo
import dk.nordfalk.esperanto.domain.repository.ElshutDeponejo
import dk.nordfalk.esperanto.logw
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * NoOp elŝut-deponejo por platformoj kiuj ne subtenas elŝutojn (wasmJs, iOS).
 */
class NoOpElshutDeponejo : ElshutDeponejo {
    private val _elshutoj = MutableStateFlow<Map<String, ElshutitaElsendo>>(emptyMap())
    override fun observiElshutojn(): StateFlow<Map<String, ElshutitaElsendo>> = _elshutoj.asStateFlow()
    override fun observiElshutStaton(elsendoId: String): StateFlow<ElshutStato> = MutableStateFlow(ElshutStato.NeElshutita)
    override suspend fun elshuti(elsendo: Elsendo) { logw("ElshutDeponejo", "Elŝutoj ne haveblas sur ĉi tiu platformo") }
    override suspend fun haltigi(elsendoId: String) {}
    override suspend fun forigi(elsendoId: String) {}
    override suspend fun getLokaDosieroVojo(elsendoId: String): String? = null
    override fun estasElshutita(elsendoId: String): Boolean = false
}
