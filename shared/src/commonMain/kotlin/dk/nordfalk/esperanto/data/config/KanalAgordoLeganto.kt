package dk.nordfalk.esperanto.data.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import dk.nordfalk.esperanto.domain.model.Kanal

/**
 * Legas la kanalkonfiguron (JSON kun komentoj — JSONC).
 *
 * La dosiero `esperantoradio_kanaloj_v9.json` enhavas `//`-komentojn kaj
 * kampojn kun `XXX`-prefikso por malaktivigi. Ni striptigas komentojn
 * antaŭ parsado, kaj ignoras nekonatajn kampojn.
 */
class KanalAgordoLeganto {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun legu(teksto: String): KanalAgordo {
        val purigita = striptiguKomentojn(teksto)
        return json.decodeFromString(KanalAgordo.serializer(), purigita)
    }

    /**
     * Striptigas `//`-liniajn komentojn kaj normaligas plurliniajn ĉenojn.
     *
     * La JSONC-dosiero enhavas `//`-komentojn kaj plurliniajn ĉenojn kun `\`
     * ĉe lini-fino (ekz. `sugestoj_por_alarmoj`). Ni striptigas komentojn
     * kaj traktas `\` + linisalton kiel lin-daŭrigon (forigu ambaŭ).
     */
    private fun striptiguKomentojn(teksto: String): String {
        val sb = StringBuilder(teksto.length)
        var i = 0
        var enCxeno = false
        while (i < teksto.length) {
            val c = teksto[i]
            if (enCxeno && c == '\\') {
                // `\` + linisalto = lin-daŭrigo — forigu ambaŭ
                if (i + 1 < teksto.length && (teksto[i + 1] == '\n' || teksto[i + 1] == '\r')) {
                    i++ // saltu `\`
                    // saltu linisalton(j)
                    while (i < teksto.length && (teksto[i] == '\n' || teksto[i] == '\r')) i++
                    continue
                }
                // Alie: normala JSON-eskapo (ekz. \", \\, \n) — kopiu ambaŭ signojn
                sb.append(c)
                if (i + 1 < teksto.length) {
                    sb.append(teksto[i + 1])
                    i += 2
                    continue
                }
                i++
                continue
            }
            if (c == '"') {
                enCxeno = !enCxeno
                sb.append(c)
                i++
                continue
            }
            if (!enCxeno && c == '/' && i + 1 < teksto.length && teksto[i + 1] == '/') {
                // Saltu ĝis linifino
                while (i < teksto.length && teksto[i] != '\n') i++
                continue
            }
            sb.append(c)
            i++
        }
        return sb.toString()
    }
}

@Serializable
data class KanalAgordo(
    val android: AndroidSekcio? = null,
    val intervals: Intervals? = null,
    val komenca_kanalo: String? = null,
    val elsendojUrl: String? = null,
    val hejmpagho: String? = null,
    val kanaloj: List<KanalDto> = emptyList(),
) {
    @Serializable
    data class AndroidSekcio(
        val kontakt_url: String? = null,
        val kontakt_modtagere: List<String>? = null,
        val kontakt_titel: String? = null,
        val drift_statusmeddelelse: String? = null,
    )

    @Serializable
    data class Intervals(
        val playlist: Int = 30,
        val settings: Int = 1800,
    )
}

@Serializable
data class KanalDto(
    val kodo: String,
    val nomo: String,
    val emblemoUrl: String? = null,
    val rektaElsendaSonoUrl: String? = null,
    val elsendojRssUrl: String? = null,
    val rektaElsendaPriskriboUrl: String? = null,
    val hejmpaghoButono: String? = null,
    val retposhto: String? = null,
    val elsendojRssIgnoruTitolon: Boolean = false,
    val montruTitolojn: Boolean = true,
    val uziWebViewPorElsendo: Boolean = false,
)

fun KanalDto.alKanal(): Kanal = Kanal(
    slug = kodo,
    nomo = nomo,
    emblemoUrl = emblemoUrl,
    rektaElsendaSonoUrl = rektaElsendaSonoUrl,
    podkastaRssUrl = elsendojRssUrl,
    rektaElsendaPriskriboUrl = rektaElsendaPriskriboUrl,
    retejoUrl = hejmpaghoButono,
    retposhto = retposhto,
    ignoruTitolon = elsendojRssIgnoruTitolon,
    montruTitolojn = montruTitolojn,
    uzuWebViewPorElsendo = uziWebViewPorElsendo,
)
