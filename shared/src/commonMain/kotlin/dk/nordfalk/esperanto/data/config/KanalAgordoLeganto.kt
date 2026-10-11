package dk.nordfalk.esperanto.data.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerialName
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.model.Alarmo
import dk.nordfalk.esperanto.logw

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
    val kanaloj: List<KanaloDto> = emptyList(),
    val sugestoj_por_alarmoj: String? = null,
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
data class KanaloDto(
    val kodo: String,
    val nomo: String,
    val priskribo: String? = null,
    val emblemoUrl: String? = null,
    val rektaElsendaSonoUrl: String? = null,
    val elsendojRssUrl: String? = null,
    val rektaElsendaPriskriboUrl: String? = null,
    @SerialName("hejmpaĝoButono") val hejmpaghoButono: String? = null,
    @SerialName("retpoŝto") val retposhto: String? = null,
    val elsendojRssIgnoruTitolon: Boolean = false,
    val montruTitolojn: Boolean = true,
    val uziWebViewPorElsendo: Boolean = true,
)

fun KanaloDto.alKanalo(): Kanalo = Kanalo(
    slug = kodo,
    nomo = nomo,
    priskribo = priskribo,
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

/**
 * Parsas sugestoj_por_alarmoj el la JSONC-konfiguro.
 *
 * Formato: `id/enabled/horo/minuto/ripeto/time/=kanalo/=etikedo/`
 * kun `\` cxe lini-fino por lin-daŭrigo (striptigita de KanalAgordoLeganto).
 * `+` = spaco, `%0A` = lini-rompo, `=` prefikso por kanalo kaj etikedo.
 *
 * Post JSONC-striptigo cxiuj sugestoj estas sur unu linio, apartigitaj per `/`.
 * Cxiu alarmo konsistas el 8 partoj + trailing `/` (9 elementoj per split).
 */
fun parsuSugestojnPorAlarmoj(teksto: String): List<Alarmo> {
    val partoj = teksto.split("/")
    val rezulto = mutableListOf<Alarmo>()
    var i = 0
    while (i + 7 < partoj.size) {
        try {
            val id = partoj[i].toInt()
            val aktiva = partoj[i + 1] == "1"
            val horo = partoj[i + 2].toInt()
            val minuto = partoj[i + 3].toInt()
            val ripeto = partoj[i + 4].toInt()
            // partoj[i + 5] = time (malnova, ne uzata)
            val kanaloSlug = partoj[i + 6].removePrefix("=")
            val etikedo = malkoduUrlKoditajxon(partoj[i + 7].removePrefix("="))
            rezulto.add(Alarmo(
                id = id,
                horo = horo,
                minuto = minuto,
                ripeto = ripeto,
                kanaloSlug = kanaloSlug,
                aktiva = aktiva,
                etikedo = etikedo.ifBlank { null }
            ))
        } catch (e: Exception) {
            logw("AlarmoSugestoj", "Ne eblis parsii alarmon cxe indekso $i", e)
        }
        i += 8
    }
    return rezulto
}

/**
 * Malkodas URL-koditan tekston: `+` → spaco, `%XX` → UTF-8-signo.
 *
 * Anstataŭas la antaŭan permanan supersignan tabelon, kiu estis erara:
 * `%C4%A5` estas **ĥ** (ne ĵ), `%C5%9C` estas **Ŝ** (ne ŝ), `%C5%AC` estas **Ŭ**
 * (ne Ŝ) — kaj `ĝ`, `ĵ`, `ŝ`, `Ĥ` tute mankis (FAROTA K5).
 * Jam-malkoditaj signoj (kodo > 127) estas trairataj senŝanĝe.
 */
internal fun malkoduUrlKoditajxon(teksto: String): String {
    val rezulto = StringBuilder()
    val bajtoj = mutableListOf<Int>()
    fun elfluigu() {
        if (bajtoj.isNotEmpty()) {
            rezulto.append(malkoduUtf8(bajtoj))
            bajtoj.clear()
        }
    }
    var i = 0
    while (i < teksto.length) {
        val c = teksto[i]
        when {
            c == '+' -> { elfluigu(); rezulto.append(' '); i++ }
            c == '%' && i + 2 < teksto.length -> {
                val alta = hexCifero(teksto[i + 1])
                val malalta = hexCifero(teksto[i + 2])
                if (alta != null && malalta != null) {
                    bajtoj.add((alta shl 4) or malalta)
                    i += 3
                } else {
                    elfluigu(); rezulto.append(c); i++
                }
            }
            else -> { elfluigu(); rezulto.append(c); i++ }
        }
    }
    elfluigu()
    return rezulto.toString()
}

/** Heksa cifero → valoro 0-15, aŭ null se ne heksa. */
private fun hexCifero(c: Char): Int? = when (c) {
    in '0'..'9' -> c - '0'
    in 'a'..'f' -> c - 'a' + 10
    in 'A'..'F' -> c - 'A' + 10
    else -> null
}

/** Malkodas UTF-8-bajtosekvencon al signoĉeno (subtenas 1-4-bajtajn signojn). */
private fun malkoduUtf8(bajtoj: List<Int>): String {
    val sb = StringBuilder()
    var i = 0
    while (i < bajtoj.size) {
        val b = bajtoj[i]
        when {
            b < 0x80 -> { sb.append(b.toChar()); i += 1 }
            b and 0xE0 == 0xC0 && i + 1 < bajtoj.size -> {
                sb.append((((b and 0x1F) shl 6) or (bajtoj[i + 1] and 0x3F)).toChar()); i += 2
            }
            b and 0xF0 == 0xE0 && i + 2 < bajtoj.size -> {
                sb.append((((b and 0x0F) shl 12) or ((bajtoj[i + 1] and 0x3F) shl 6) or (bajtoj[i + 2] and 0x3F)).toChar()); i += 3
            }
            b and 0xF8 == 0xF0 && i + 3 < bajtoj.size -> {
                val kodo = ((b and 0x07) shl 18) or ((bajtoj[i + 1] and 0x3F) shl 12) or
                    ((bajtoj[i + 2] and 0x3F) shl 6) or (bajtoj[i + 3] and 0x3F)
                val m = kodo - 0x10000
                sb.append((0xD800 + (m shr 10)).toChar())
                sb.append((0xDC00 + (m and 0x3FF)).toChar())
                i += 4
            }
            else -> { sb.append('\uFFFD'); i += 1 }
        }
    }
    return sb.toString()
}
