package dk.nordfalk.esperanto.data.repository

/**
 * Ero de la sciig-registrejo: sciig-ID + la kanalo al kiu ĝi apartenas
 * (bezona por la po-kanala limo). La registrejo estas ordigita de la
 * plej malnova al la plej nova ero.
 */
data class SciigRegistrejoEro(val sciigId: Int, val kanaloSlug: String)

/**
 * Rezulto de [limiguSciigojn]: la nova stato de la registrejo kaj la
 * ID-oj de sciigoj kiujn la fino devas forigi.
 */
data class SciigLimaRezulto(
    /** La sciigoj kiuj restas aktivaj (kun la nova ero), plej malnova unue. */
    val aktivaj: List<SciigRegistrejoEro>,
    /** ID-oj forigendaj ĉar ili superus la limojn. */
    val forigotaj: List<Int>,
)

/**
 * Limoj por la sciigoj pri novaj elsendoj (uzanta deziro, 2026-10-20):
 * maksimume [SCIIG_LIMO_SUMA] samtempaj sciigoj, maksimume [SCIIG_LIMO_PO_KANALO]
 * de la sama kanalo. Kiam limo atingiĝas, la plej malnovaj sciigoj malaperas —
 * tiel oni ne dronas pri sciigoj.
 */
const val SCIIG_LIMO_SUMA = 7
const val SCIIG_LIMO_PO_KANALO = 3

/**
 * La fiksa sciig-ID de la alarmo (vidu `AlarmoReceivilo.SCIIGO_ID` en androidMain) —
 * la kreskanta serio de sciigoj pri novaj elsendoj devas neniam transpreni ĝin.
 */
const val REZERVITA_SCIIG_ID = 4711

/**
 * La sekva sciig-ID el la kreskanta serio — sed neniam en la rezervita zono de la
 * alarma sciigo: transpreno anstataŭigus la alarman sciigon per sciigo pri nova
 * elsendo (kaj inverse). Ankaŭ [REZERVITA_SCIIG_ID] - 1 evitiĝas, ĉar la
 * „Ludi“-ago de sciigo uzas la pet-identigilon sciigId + 1.
 */
fun sekvaSciigId(nuna: Int): Int {
    var id = (nuna + 1).coerceAtMost(Int.MAX_VALUE - 2)
    if (id == REZERVITA_SCIIG_ID - 1 || id == REZERVITA_SCIIG_ID) {
        id = REZERVITA_SCIIG_ID + 1
    }
    return id
}

/**
 * Enmetas novan sciigon en la registrejon kaj aplikas la limojn.
 *
 * La po-kanala limo aplikiĝas unue (je la kanalo de la nova sciigo), poste la
 * suma. La forigotaj eroj estas ĉiam la plej malnovaj — do la uzanto vidas
 * ĉiam la plej freŝajn sciigojn, maksimume 3 po kanalo kaj 7 entute.
 *
 * @param aktivaj la nunaj eroj, plej malnova unue (malplena ĉe la unua sciigo)
 * @param nova la ĵus sendota sciigo
 */
fun limiguSciigojn(
    aktivaj: List<SciigRegistrejoEro>,
    nova: SciigRegistrejoEro,
): SciigLimaRezulto {
    // La nova anstataŭigas eblan sam-ID-an eron kaj venas laste (plej nova)
    val kunNova = aktivaj.filter { it.sciigId != nova.sciigId } + nova
    val restantaj = kunNova.toMutableList()
    val forigotaj = mutableListOf<SciigRegistrejoEro>()

    // 1. Po-kanala limo: de la kanalo de la nova sciigo gardu nur la plej novajn
    val deKanalo = restantaj.filter { it.kanaloSlug == nova.kanaloSlug }
    if (deKanalo.size > SCIIG_LIMO_PO_KANALO) {
        val malnovaj = deKanalo.take(deKanalo.size - SCIIG_LIMO_PO_KANALO)
        forigotaj.addAll(malnovaj)
        restantaj.removeAll(malnovaj.toSet())
    }

    // 2. Suma limo: gardu nur la plej novajn
    if (restantaj.size > SCIIG_LIMO_SUMA) {
        val malnovaj = restantaj.take(restantaj.size - SCIIG_LIMO_SUMA)
        forigotaj.addAll(malnovaj)
        restantaj.removeAll(malnovaj.toSet())
    }

    return SciigLimaRezulto(aktivaj = restantaj.toList(), forigotaj = forigotaj.map { it.sciigId })
}

/**
 * Legas la sciig-registrejon el la agordo. Formato: CSV de "id:slug",
 * ekz. "1001:kernpunkto,1002:muzaiko" (slugs ne enhavas ',' nek ':').
 */
fun leguSciigRegistrejon(settings: com.russhwolf.settings.Settings): List<SciigRegistrejoEro> {
    val teksto = settings.getString(SettingsKeys.SCIIG_REGISTREJO, "")
    if (teksto.isBlank()) return emptyList()
    return teksto.split(",").mapNotNull { ero ->
        val id = ero.substringBefore(':').toIntOrNull() ?: return@mapNotNull null
        val slug = ero.substringAfter(':', "")
        if (slug.isBlank()) return@mapNotNull null
        SciigRegistrejoEro(id, slug)
    }
}

/** Skribas la sciig-registrejon en la agordon (maks. [SCIIG_LIMO_SUMA] eroj). */
fun skribuSciigRegistrejon(settings: com.russhwolf.settings.Settings, eroj: List<SciigRegistrejoEro>) {
    settings.putString(
        SettingsKeys.SCIIG_REGISTREJO,
        eroj.joinToString(",") { "${it.sciigId}:${it.kanaloSlug}" },
    )
}
