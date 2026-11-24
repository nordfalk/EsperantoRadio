package dk.nordfalk.esperanto.domain.player

import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.loge
import kotlinx.serialization.json.Json

/**
 * Kodigas [Sonfonto] kiel JSON-teksto, por ke ĝi povu vojaĝi kun la ludata aĵo
 * (ekz. en la ekstraĵoj de Media3 `MediaMetadata`). Tiel ludilo kiu konektiĝas al jam
 * ludanta servo povas rekoni kion la servo ludas — ekz. post rekreo de la Activity,
 * aŭ kiam la sciigo "Ludi" komencis la ludadon per propra MediaController.
 *
 * (FAROTA C11) La kodita teksto vojaĝas trans Binder (regilo → servo) kaj kuŝas en
 * la Media3-sesio; la limo de binder-transakcio estas ~1 MB. Tial [kodigu] forigas
 * la pezajn kampojn de la enmetita [Elsendo] — `priskribo` kaj `priskriboHtml`
 * (riĉaj HTML-priskriboj kun enkorpigitaj `<audio>` povas esti dekoj da KB).
 * Ĉiu konsumanto de la malkodita fonto uzas nur id/kanaloSlug/titolo/kanaloNomo/
 * dato/fluo/bildoUrl; la detala ekrano (Vojo.ElsendoDetalo portas nur id+kanaloSlug)
 * reŝargas la plenan elsendon el `ElsendoDeponejo`.
 */
object SonfontoKodilo {
    /** Ŝlosilo en `MediaMetadata.extras`. */
    const val ŜLOSILO = "dk.nordfalk.esperanto.sonfonto"

    private val json = Json { ignoreUnknownKeys = true }

    fun kodigu(fonto: Sonfonto): String = json.encodeToString(Sonfonto.serializer(), maldika(fonto))

    /**
     * Forigas la pezajn kampojn (priskribo, priskriboHtml) el la enmetita elsendo —
     * ili ne estas bezonataj por rekoni/ludi la fonton kaj povas esti tre grandaj.
     */
    private fun maldika(fonto: Sonfonto): Sonfonto = when (fonto) {
        is Sonfonto.ElsendoFonto -> fonto.copy(elsendo = fonto.elsendo.maldika())
        is Sonfonto.LokaElsendo -> fonto.copy(elsendo = fonto.elsendo.maldika())
        is Sonfonto.RektaKanalo -> fonto
    }

    private fun Elsendo.maldika(): Elsendo =
        if (priskribo == null && priskriboHtml == null) this
        else copy(priskribo = null, priskriboHtml = null)

    /** @return null se [teksto] estas null aŭ ne malkodebla (ekz. de pli malnova apo-versio) */
    fun malkodigu(teksto: String?): Sonfonto? {
        if (teksto.isNullOrBlank()) return null
        return try {
            json.decodeFromString(Sonfonto.serializer(), teksto)
        } catch (e: Exception) {
            loge("SonfontoKodilo", "Ne povis malkodigi la paktitan Sonfonton", e)
            null
        }
    }
}
