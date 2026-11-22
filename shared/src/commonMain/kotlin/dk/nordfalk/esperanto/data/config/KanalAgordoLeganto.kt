package dk.nordfalk.esperanto.data.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerialName
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.model.Alarmo
import dk.nordfalk.esperanto.domain.model.TranssaltaValoroLaŭDato

/**
 * Legas la kanalkonfiguron (JSON kun komentoj — JSONC).
 *
 * La dosiero `esperantoradio_konfiguro.json` enhavas `//`-liniajn kaj `/* */`-blokajn
 * komentojn kaj kampojn kun `XXX`-prefikso por malaktivigi. Ni striptigas komentojn
 * antaŭ parsado (vidu [striptiguKomentojn]), kaj ignoras nekonatajn kampojn.
 */
class KanalAgordoLeganto {

    private val json = Json {
        // Nekonataj kampoj (ekz. kun XXX-prefikso) estas ignorataj — tio ebligas
        // malaktivigi kanalojn en la konfiguro sen ŝanĝi la kodon (A5).
        ignoreUnknownKeys = true
        isLenient = true
        // coerceInputValues (FAROTA C8): kampo kun MALĜUSTA tipo ricevas sian
        // defaŭlton anstataŭ ĵeti — elektita por toleremo (A4): unu fuŝita kampo
        // ne rajtas mortigi la ŝargon de la TUTA konfiguro. Malavantaĝo: tajpo
        // en valoro (ekz. "truee") estas kaŝe korektita al la defaŭlto — kontrolu
        // per KanalAgordoFumTesto kaj radioTxtKomparilo.
        coerceInputValues = true
    }

    fun legu(teksto: String): KanalAgordo {
        val purigita = striptiguKomentojn(teksto)
        return json.decodeFromString(KanalAgordo.serializer(), purigita)
    }

    /**
     * Striptigas `//`-liniajn kaj `/* */`-blokajn komentojn kaj normaligas
     * plurliniajn ĉenojn. Komentoj ene de teksta ĉeno (ekz. en URL
     * `"https://…"`) ne estas tuŝataj.
     *
     * La JSONC-dosiero enhavas `//`- kaj `/* */`-komentojn kaj povas enhavi
     * plurliniajn ĉenojn kun `\` ĉe lini-fino. Ni striptigas komentojn kaj
     * traktas `\` + linisalton kiel lin-daŭrigon (forigu ambaŭ).
     * Nefinita blokkomento (`/*` sen `*/`) estas striptita ĝis la fino (milda,
     * kongrue kun `isLenient`).
     */
    fun striptiguKomentojn(teksto: String): String {
        val sb = StringBuilder(teksto.length)
        var i = 0
        var enĈeno = false
        while (i < teksto.length) {
            val c = teksto[i]
            if (enĈeno && c == '\\') {
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
                enĈeno = !enĈeno
                sb.append(c)
                i++
                continue
            }
            if (!enĈeno && c == '/' && i + 1 < teksto.length) {
                when (teksto[i + 1]) {
                    '/' -> {
                        // Linia komento — saltu ĝis linifino
                        while (i < teksto.length && teksto[i] != '\n') i++
                        continue
                    }
                    '*' -> {
                        // Blokkomento — saltu ĝis la fina `*/` (aŭ ĝis la fino)
                        i += 2
                        while (i + 1 < teksto.length && !(teksto[i] == '*' && teksto[i + 1] == '/')) i++
                        i = minOf(i + 2, teksto.length)
                        continue
                    }
                }
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
    val komenca_kanalo: String? = null,
    val elsendojUrl: String? = null,
    /** Bazo de nia arkiv-servilo (riĉigitaj fluoj); la apo provas ĝin unue, poste la originalan fluon. */
    val arkivoBazoUrl: String? = null,
    val hejmpaĝo: String? = null,
    val kanaloj: List<KanaloDto> = emptyList(),
    val sugestoj_por_alarmoj: List<Alarmo>? = null,
) {
    @Serializable
    data class AndroidSekcio(
        @SerialName("apaj_novaĵoj") val apajNovaĵoj: String? = null,
        /** Pasintaj novaĵoj — arkivo montrata faldeble sub la nuna novaĵo (datum-movita, A5). */
        @SerialName("apaj_novaĵoj_pasintaj") val apajNovaĵojPasintaj: List<String>? = null,
        /** Retpoŝto de la prizorganto — celo de „Proponi kanalon“ (datum-movita, A5). */
        @SerialName("prizorgantaRetpoŝto") val prizorgantaRetpoŝto: String? = null,
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
    /** HTML-paĝo kun la elsendo-listo (por kanaloj sen RSS) — la arkiv-servilo el ĝi generas la arkivan fluon. */
    val elsendojHtmlUrl: String? = null,
    val rektaElsendaPriskriboUrl: String? = null,
    val hejmpaĝoButono: String? = null,
    val retpoŝto: String? = null,
    val elsendojRssIgnoruTitolon: Boolean = false,
    val montruTitolojn: Boolean = true,
    val uziWebViewPorElsendo: Boolean = true,
    /** Kanal-specifaj purigaj regex-modeloj por la priskribo (regulo P6.6). */
    val puriguModeloj: List<String> = emptyList(),
    /**
     * Transsaltendaj sekundoj de la komenco de ĉiu elsendo (ripetiĝanta enkonduko —
     * FAROTA C59a). Unuopa valoro = unu ero sen „ĝis"; se la enkonduko ŝanĝiĝis,
     * pluraj eroj kun finaj datoj — la lasta ero estu sen „ĝis" (validas por
     * estontaj elsendoj). Malplena listo = ne transsalti.
     */
    val transsaltuKomenceSekundojn: List<TranssaltaValoroLaŭDatoDto> = emptyList(),
    val defaŭltŜatata: Boolean = false,
)

/** DTO-ero de transsaltuKomenceSekundojn: valoro + nedeviga fina dato (inkluziva). */
@Serializable
data class TranssaltaValoroLaŭDatoDto(
    val sekundoj: Float,
    val ĝis: String? = null,
)

fun TranssaltaValoroLaŭDatoDto.alValoro() = TranssaltaValoroLaŭDato(sekundoj = sekundoj, ĝis = ĝis)

fun KanaloDto.alKanalo(): Kanalo = Kanalo(
    slug = kodo,
    nomo = nomo,
    priskribo = priskribo,
    // Malplena emblemoUrl ("") signifas "neniu bildo" — normaligu al null, por ke
    // la UI montru la rezervan komencliteran bildon anstataŭ provi ŝargi malplenan URL-on.
    emblemoUrl = emblemoUrl?.ifBlank { null },
    rektaElsendaSonoUrl = rektaElsendaSonoUrl,
    podkastaRssUrl = elsendojRssUrl,
    htmlFontoUrl = elsendojHtmlUrl,
    rektaElsendaPriskriboUrl = rektaElsendaPriskriboUrl,
    retejoUrl = hejmpaĝoButono,
    retpoŝto = retpoŝto,
    ignoruTitolon = elsendojRssIgnoruTitolon,
    montruTitolojn = montruTitolojn,
    uzuWebViewPorElsendo = uziWebViewPorElsendo,
    puriguModeloj = puriguModeloj,
    transsaltuKomenceSekundojn = transsaltuKomenceSekundojn.map { it.alValoro() },
    defaŭltŜatata = defaŭltŜatata,
)
