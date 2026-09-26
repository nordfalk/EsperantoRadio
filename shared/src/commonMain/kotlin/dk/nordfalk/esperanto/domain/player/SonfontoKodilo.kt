package dk.nordfalk.esperanto.domain.player

import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.logw
import kotlinx.serialization.json.Json

/**
 * Kodigas [Sonfonto] kiel JSON-teksto, por ke ĝi povu vojaĝi kun la ludata aĵo
 * (ekz. en la ekstraĵoj de Media3 `MediaMetadata`). Tiel ludilo kiu konektiĝas al jam
 * ludanta servo povas rekoni kion la servo ludas — ekz. post rekreo de la Activity,
 * aŭ kiam la sciigo "Ludi" komencis la ludadon per propra MediaController.
 */
object SonfontoKodilo {
    /** Ŝlosilo en `MediaMetadata.extras`. */
    const val SXLOSILO = "dk.nordfalk.esperanto.sonfonto"

    private val json = Json { ignoreUnknownKeys = true }

    fun kodigu(fonto: Sonfonto): String = json.encodeToString(Sonfonto.serializer(), fonto)

    /** @return null se [teksto] estas null aŭ ne malkodebla (ekz. de pli malnova apo-versio) */
    fun malkodigu(teksto: String?): Sonfonto? {
        if (teksto.isNullOrBlank()) return null
        return try {
            json.decodeFromString(Sonfonto.serializer(), teksto)
        } catch (e: Exception) {
            logw("SonfontoKodilo", "Ne povis malkodigi Sonfonton", e)
            null
        }
    }
}
