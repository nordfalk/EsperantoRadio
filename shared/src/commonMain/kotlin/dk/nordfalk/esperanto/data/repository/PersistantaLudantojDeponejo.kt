package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.domain.model.LudantaElsendo
import dk.nordfalk.esperanto.domain.repository.LudantojDeponejo
import dk.nordfalk.esperanto.logi
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

/**
 * Persistanta LudantojDeponejo. Uzas multiplatform-settings kun JSON-seriado.
 *
 * Konservas Map<elsendoId, LudantaElsendo> kiel JSON-ĉeno en Settings.
 */
@OptIn(ExperimentalTime::class)
class PersistantaLudantojDeponejo(
    private val settings: Settings,
) : LudantojDeponejo {
    private val key = "ludantoj"
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = MapSerializer(String.serializer(), LudantaElsendo.serializer())

    private fun legu(): Map<String, LudantaElsendo> {
        val str = settings.getString(key, "")
        if (str.isEmpty()) return emptyMap()
        return runCatching { json.decodeFromString(serializer, str) }.getOrElse {
            logi("Ludantoj", "Malsukcesis dekodi — komencas freŝe")
            emptyMap()
        }
    }

    private fun skribu(value: Map<String, LudantaElsendo>) {
        settings.putString(key, json.encodeToString(serializer, value))
    }

    private val _ludantoj = MutableStateFlow<Map<String, LudantaElsendo>>(legu())
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
        skribu(nuna)
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
        skribu(nuna)
        _ludantoj.value = nuna
        logi("Ludantoj", "Markita finita: $elsendoId")
    }

    override suspend fun getLudanto(elsendoId: String): LudantaElsendo? = _ludantoj.value[elsendoId]

    override suspend fun estasFinita(elsendoId: String): Boolean =
        _ludantoj.value[elsendoId]?.finita ?: false

    override suspend fun getPozicio(elsendoId: String): Long? =
        _ludantoj.value[elsendoId]?.pozicioMs?.takeIf { it > 0 }
}
