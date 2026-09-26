package dk.nordfalk.esperanto.data.repository

import com.russhwolf.settings.Settings
import dk.nordfalk.esperanto.loge
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Persista kesto de archive.org-dosiernomoj: arkivaĵo-identigilo → MP3-dosiernomo.
 *
 * La Peranto-parsilo (regulo 6.3) bezonas la veran MP3-dosiernomon ene de
 * archive.org-arkivaĵo. La dosiernomo ne ĉiam egalas al la identigilo
 * (ekz. `malapero-benda-3` enhavas `Malapero_Benda3.mp3`), do ĝi estas demandata
 * per https://archive.org/metadata/&lt;identigilo&gt; kaj konservata ĉi tie,
 * por ne ripeti la demandon ĉe ĉiu parsado.
 *
 * Uzas la saman ŝablonon kiel [PersistaLudatojDeponejo]: JSON-ĉeno en Settings.
 */
class ArchiveOrgDosiernomoKasho(
    private val settings: Settings,
) {
    private val key = "archive_org_dosiernomoj"
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = MapSerializer(String.serializer(), String.serializer())

    private val mutex = Mutex()
    private val kasho: MutableMap<String, String> = legu()

    /** Redonas la konatan MP3-dosiernomon por arkivaĵo, aŭ null se ĝi ankoraŭ ne estas konata. */
    suspend fun leguDosiernomon(identigilo: String): String? = mutex.withLock { kasho[identigilo] }

    /** Konservas la MP3-dosiernomon por arkivaĵo (daŭre, en Settings). */
    suspend fun konservuDosiernomon(identigilo: String, dosiernomo: String) = mutex.withLock {
        if (kasho[identigilo] == dosiernomo) return@withLock
        kasho[identigilo] = dosiernomo
        skribu()
    }

    private fun legu(): MutableMap<String, String> {
        val str = settings.getString(key, "")
        if (str.isEmpty()) return mutableMapOf()
        return runCatching { json.decodeFromString(serializer, str).toMutableMap() }.getOrElse { e ->
            loge("ArchiveOrgKasho", "Malsukcesis dekodi — komencas freŝe", e)
            mutableMapOf()
        }
    }

    private fun skribu() {
        settings.putString(key, json.encodeToString(serializer, kasho))
    }
}
