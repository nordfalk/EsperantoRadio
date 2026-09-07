package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.repository.PlejŝatatajDeponejo
import dk.nordfalk.esperanto.logi
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persistanta PlejŝatatajDeponejo. Uzas multiplatform-settings por persisti
 * la plejŝatatajn kanalo-slugs inter restartoj.
 *
 * Decido: Uzas Settings (key-value store) kun komma-disigita listo.
 * Simplaj kaj sufiĉa por malgranda nombro da kanaloj.
 */
class PersistantaPlejŝatatajDeponejo(
    private val settings: Settings,
) : PlejŝatatajDeponejo {
    private val key = "plejŝatataj_kanaloj"

    private fun legu(): Set<String> {
        val str = settings.getString(key, "")
        return if (str.isEmpty()) emptySet() else str.split(",").toSet()
    }

    private fun skribu(value: Set<String>) {
        settings.putString(key, value.joinToString(","))
    }

    private val _plejŝatataj = MutableStateFlow<Set<String>>(legu())
    override fun observiPlejŝatatajn(): StateFlow<Set<String>> = _plejŝatataj.asStateFlow()

    override suspend fun baskuliPlejŝaton(kanaloSlug: String) {
        val nuna = _plejŝatataj.value.toMutableSet()
        if (kanaloSlug in nuna) nuna.remove(kanaloSlug) else nuna.add(kanaloSlug)
        skribu(nuna)
        _plejŝatataj.value = nuna
        logi("Plejŝatataj", "Baskulas: $kanaloSlug → ${if (kanaloSlug in nuna) "aldonita" else "forigita"} (total ${nuna.size})")
    }

    override suspend fun estasPlejŝatata(kanaloSlug: String): Boolean = kanaloSlug in _plejŝatataj.value
}
