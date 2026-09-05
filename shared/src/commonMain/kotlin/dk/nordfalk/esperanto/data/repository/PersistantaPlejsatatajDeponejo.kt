package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.repository.PlejsatatajDeponejo
import dk.nordfalk.esperanto.logi
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persistanta PlejsatatajDeponejo. Uzas multiplatform-settings por persisti
 * la plejŝatatajn kanal-slugs inter restartoj.
 *
 * Decido: Uzas Settings (key-value store) kun komma-disigita listo.
 * Simplaj kaj sufiĉa por malgranda nombro da kanaloj.
 */
class PersistantaPlejsatatajDeponejo(
    private val settings: Settings,
) : PlejsatatajDeponejo {
    private val key = "plejsatataj_kanaloj"

    private fun legu(): Set<String> {
        val str = settings.getString(key, "")
        return if (str.isEmpty()) emptySet() else str.split(",").toSet()
    }

    private fun skribu(value: Set<String>) {
        settings.putString(key, value.joinToString(","))
    }

    private val _plejsatataj = MutableStateFlow<Set<String>>(legu())
    override fun observiPlejsatatajn(): StateFlow<Set<String>> = _plejsatataj.asStateFlow()

    override suspend fun baskuliPlejsaton(kanalSlug: String) {
        val nuna = _plejsatataj.value.toMutableSet()
        if (kanalSlug in nuna) nuna.remove(kanalSlug) else nuna.add(kanalSlug)
        skribu(nuna)
        _plejsatataj.value = nuna
        logi("Plejsatataj", "Baskulas: $kanalSlug → ${if (kanalSlug in nuna) "aldonita" else "forigita"} (total ${nuna.size})")
    }

    override suspend fun estasPlejsatata(kanalSlug: String): Boolean = kanalSlug in _plejsatataj.value
}
