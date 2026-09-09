package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.domain.model.LudantaElsendo
import dk.nordfalk.esperanto.domain.repository.LudantojDeponejo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Memora LudantojDeponejo por testoj — neniuj persistaj flankaj efikoj.
 */
@OptIn(ExperimentalTime::class)
class LudantojDeponejoImpl : LudantojDeponejo {
    private val _ludantoj = MutableStateFlow<Map<String, LudantaElsendo>>(emptyMap())
    override fun observiLudantojn(): StateFlow<Map<String, LudantaElsendo>> = _ludantoj.asStateFlow()

    override suspend fun registriPozicion(elsendoId: String, kanaloSlug: String, pozicioMs: Long, dauroMs: Long) {
        val nuna = _ludantoj.value.toMutableMap()
        val ekzista = nuna[elsendoId]
        nuna[elsendoId] = LudantaElsendo(
            elsendoId = elsendoId,
            kanaloSlug = kanaloSlug,
            pozicioMs = pozicioMs,
            dauroMs = if (dauroMs > 0) dauroMs else (ekzista?.dauroMs ?: 0),
            finita = ekzista?.finita ?: false,
            lasteLudita = Clock.System.now().toEpochMilliseconds(),
        )
        _ludantoj.value = nuna
    }

    override suspend fun markiFinita(elsendoId: String, kanaloSlug: String) {
        val nuna = _ludantoj.value.toMutableMap()
        val ekzista = nuna[elsendoId]
        nuna[elsendoId] = LudantaElsendo(
            elsendoId = elsendoId,
            kanaloSlug = kanaloSlug,
            pozicioMs = ekzista?.pozicioMs ?: 0,
            dauroMs = ekzista?.dauroMs ?: 0,
            finita = true,
            lasteLudita = Clock.System.now().toEpochMilliseconds(),
        )
        _ludantoj.value = nuna
    }

    override suspend fun getLudanto(elsendoId: String): LudantaElsendo? = _ludantoj.value[elsendoId]
    override suspend fun estasFinita(elsendoId: String): Boolean = _ludantoj.value[elsendoId]?.finita ?: false
    override suspend fun getPozicio(elsendoId: String): Long? = _ludantoj.value[elsendoId]?.pozicioMs?.takeIf { it > 0 }
}
