package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.ElŝutStato
import dk.nordfalk.esperanto.domain.model.ElŝutitaElsendo
import dk.nordfalk.esperanto.domain.repository.ElŝutDeponejo
import dk.nordfalk.esperanto.logw
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * NoOp elŝut-deponejo por platformoj kiuj ne subtenas elŝutojn (wasmJs, iOS).
 */
class NoOpElŝutDeponejo : ElŝutDeponejo {
    private val _elŝutoj = MutableStateFlow<Map<String, ElŝutitaElsendo>>(emptyMap())
    override fun observiElŝutojn(): StateFlow<Map<String, ElŝutitaElsendo>> = _elŝutoj.asStateFlow()
    override fun observiElŝutStaton(elsendoId: String): StateFlow<ElŝutStato> = MutableStateFlow(ElŝutStato.NeElŝutita)
    override suspend fun elŝuti(elsendo: Elsendo) { logw("ElŝutDeponejo", "Elŝutoj ne haveblas sur ĉi tiu platformo") }
    override suspend fun haltigi(elsendoId: String) {}
    override suspend fun forigi(elsendoId: String) {}
    override suspend fun getLokaDosieroVojo(elsendoId: String): String? = null
    override fun estasElŝutita(elsendoId: String): Boolean = false
}
