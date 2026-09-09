package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.domain.model.LudataElsendo
import dk.nordfalk.esperanto.domain.repository.LudatojDeponejo
import dk.nordfalk.esperanto.loge
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
 * Persistanta LudatojDeponejo. Uzas multiplatform-settings kun JSON-seriado.
 *
 * Konservas Map<elsendoId, LudataElsendo> kiel JSON-ĉeno en Settings.
 */
@OptIn(ExperimentalTime::class)
class PersistaLudatojDeponejo(
    private val settings: Settings,
) : LudatojDeponejo {
    private val key = "ludatoj"
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = MapSerializer(String.serializer(), LudataElsendo.serializer())

    private fun legu(): Map<String, LudataElsendo> {
        val str = settings.getString(key, "")
        if (str.isEmpty()) return emptyMap()
        return runCatching { json.decodeFromString(serializer, str) }.getOrElse { e ->
            loge("Ludatoj", "Malsukcesis dekodi — komencas freŝe", e)
            emptyMap()
        }
    }

    private fun skribu(value: Map<String, LudataElsendo>) {
        settings.putString(key, json.encodeToString(serializer, value))
    }

    private val _ludatoj = MutableStateFlow<Map<String, LudataElsendo>>(legu())
    override fun observiLudatojn(): StateFlow<Map<String, LudataElsendo>> = _ludatoj.asStateFlow()

    override suspend fun registriPozicion(elsendoId: String, kanaloSlug: String, pozicioMs: Long, dauroMs: Long) {
        val nuna = _ludatoj.value.toMutableMap()
        val ekzista = nuna[elsendoId]
        nuna[elsendoId] = LudataElsendo(
            elsendoId = elsendoId,
            kanaloSlug = kanaloSlug,
            pozicioMs = pozicioMs,
            dauroMs = if (dauroMs > 0) dauroMs else (ekzista?.dauroMs ?: 0),
            finita = ekzista?.finita ?: false,
            lasteLudita = Clock.System.now().toEpochMilliseconds(),
        )
        skribu(nuna)
        _ludatoj.value = nuna
    }

    override suspend fun markiFinita(elsendoId: String, kanaloSlug: String) {
        val nuna = _ludatoj.value.toMutableMap()
        val ekzista = nuna[elsendoId]
        nuna[elsendoId] = LudataElsendo(
            elsendoId = elsendoId,
            kanaloSlug = kanaloSlug,
            pozicioMs = ekzista?.pozicioMs ?: 0,
            dauroMs = ekzista?.dauroMs ?: 0,
            finita = true,
            lasteLudita = Clock.System.now().toEpochMilliseconds(),
        )
        skribu(nuna)
        _ludatoj.value = nuna
        logi("Ludatoj", "Markita finita: $elsendoId")
    }

    override suspend fun getLudato(elsendoId: String): LudataElsendo? = _ludatoj.value[elsendoId]

    override suspend fun estasFinita(elsendoId: String): Boolean =
        _ludatoj.value[elsendoId]?.finita ?: false

    override suspend fun getPozicio(elsendoId: String): Long? =
        _ludatoj.value[elsendoId]?.pozicioMs?.takeIf { it > 0 }
}
