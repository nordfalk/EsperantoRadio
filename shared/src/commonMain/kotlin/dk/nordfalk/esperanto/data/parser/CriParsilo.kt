package dk.nordfalk.esperanto.data.parser

import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.logd
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.logw
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Parsregulo 6.8 — CRI (Ĉina Radio Internacia, esperanto.cri.cn).
 *
 * CRI ne havas RSS-n; la elsendoj venas per POST al la nedokumentita API
 * `POST /api/getData` kun korpo `{"id": "<sekcio-URL>"}`. La respondo estas
 * JSON kun `result.modules[].cardgroups[].cards[].card{...}`; `isPlay:"1"`
 * markas artikolojn kun sono. Vidu docs/nova/07_cri_esperanto_kanalo.md.
 *
 * La sono estas HLS-video (m3u8) — ĝin povas ludi nur ExoPlayer (Android);
 * tial la kanalo havas `videblaNurSur: "android"` en la konfiguro.
 */
class CriParsilo {

    companion object {
        /** La (nedokumentita) CRI-API. La sekci-URL-oj venas el la kanalkonfiguro. */
        const val CRI_API = "https://esperanto.cri.cn/api/getData"

        /** Apartigilo inter la sekci-respondoj en la kaŝmemoro. */
        const val SEKCIO_APARTIGILON = "\u0000"

        private val json = Json { ignoreUnknownKeys = true }
    }

    /**
     * Parsas la kombinitan respondon de ĉiuj sekcioj al elsendolisto,
     * ordigita de la plej nova. Unuopa sekcio erara ne paneigu la tuton —
     * ĝi estas protokolita kaj preterlasata (regulo 4).
     */
    fun parsu(kombinita: String, kanalo: Kanalo): List<Elsendo> {
        val sekcioj = kombinita.split(SEKCIO_APARTIGILON).filter { it.isNotBlank() }
        val kartoj = LinkedHashMap<String, Elsendo>() // id → elsendo (dedupe)
        for ((i, sekcio) in sekcioj.withIndex()) {
            try {
                for (elsendo in parsuSekcion(sekcio, kanalo)) {
                    kartoj[elsendo.id] = elsendo
                }
            } catch (e: Exception) {
                logw("CriParsilo", "${kanalo.slug}: sekcio ${i + 1} neparsebla — preterlasas", e)
            }
        }
        val elsendoj = kartoj.values.sortedByDescending { it.dato }
        logi("CriParsilo", "${kanalo.slug}: ${elsendoj.size} elsendoj el ${sekcioj.size} sekcioj")
        return elsendoj
    }

    /** Parsas unu sekci-respondon (JSON) kaj redonas la ludeblajn kartojn. */
    internal fun parsuSekcion(teksto: String, kanalo: Kanalo): List<Elsendo> {
        val radiko = json.parseToJsonElement(teksto).jsonObject
        val kartoj = mutableListOf<JsonObject>()
        trairiKartojn(radiko, kartoj)

        return kartoj.mapNotNull { karto ->
            val link = karto.str("link") ?: return@mapNotNull null
            // Nur artikoloj kun sono: isPlay=1 kaj ligilo al artikolo (/2026/09/25/…)
            if (karto.str("isPlay") != "1") return@mapNotNull null
            if (!Regex("""/20\d\d/\d\d/\d\d/""").containsMatchIn(link)) return@mapNotNull null

            val video = karto["video"] as? JsonObject
            // HLS-ludlisto (novaĵoj) aŭ MP4 (malnovaj programoj) — ExoPlayer
            // ludas ambaŭ; ni bezonas nur la sontrakon
            val fluo = video?.str("url")
                ?.takeIf { it.endsWith(".m3u8") || it.endsWith(".mp4") }
            if (fluo == null) {
                logd("CriParsilo", "${kanalo.slug}: karto sen ludebla fluo — preterlasas: $link")
                return@mapNotNull null
            }

            val publishedMs = karto.str("date")?.toLongOrNull()
                ?: karto.str("modifyDate")?.toLongOrNull() ?: return@mapNotNull null
            val dato = formuDaton(publishedMs)
            val artikoloId = karto.str("id") ?: link.substringAfterLast('/')
            val bildo = (karto["photo"] as? JsonObject)?.str("large")?.takeIf { it.isNotEmpty() }

            Elsendo(
                id = "${kanalo.slug}:$dato:$artikoloId",
                kanaloSlug = kanalo.slug,
                kanaloNomo = kanalo.nomo,
                titolo = karto.str("title")?.trim().orEmpty(),
                priskribo = karto.str("brief")?.trim()?.takeIf { it.isNotEmpty() },
                priskriboHtml = null,
                bildoUrl = bildo ?: kanalo.emblemoUrl,
                dato = dato,
                dauro = video.str("duration")?.toDoubleOrNull()?.toLong()?.takeIf { it > 0 },
                fluo = fluo,
                retpaghoUrl = link,
            )
        }
    }

    /** Rekurzive kolektas ĉiujn `card`-objektojn el la paĝ-JSON-arbo. */
    private fun trairiKartojn(elemento: JsonElement, en: MutableList<JsonObject>) {
        when (elemento) {
            is JsonObject -> {
                val karto = elemento["card"]
                if (karto is JsonObject) en.add(karto)
                for (filo in elemento.values) trairiKartojn(filo, en)
            }
            is JsonArray -> for (filo in elemento) trairiKartojn(filo, en)
            else -> {}
        }
    }

    private fun JsonObject.str(ŝlosilo: String): String? =
        this[ŝlosilo]?.jsonPrimitive?.content

    /** Epoko-milisekundoj → "yyyy-MM-dd" (UTC), kongrue kun RssParsilo.normigiDaton. */
    @OptIn(ExperimentalTime::class)
    internal fun formuDaton(epokoMs: Long): String =
        Instant.fromEpochMilliseconds(epokoMs).toLocalDateTime(TimeZone.UTC).date.toString()
}
