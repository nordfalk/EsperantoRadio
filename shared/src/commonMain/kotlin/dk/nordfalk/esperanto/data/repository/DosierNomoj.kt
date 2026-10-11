package dk.nordfalk.esperanto.data.repository

/**
 * Sanigas elsendo-ID por uzo kiel dosiernomo.
 *
 * - Anstataŭigas signojn kiuj estas nevalidaj aŭ danĝeraj en dosiernomoj
 *   (:, /, \, ?, *, ", <, >, |) per '-' — tio ankaŭ malebligas path-traversal
 *   (`../` iĝas `..-`).
 * - Forigas nul-bajtojn kaj finajn punktojn/spacojn (Windows).
 * - Evitas Windows-rezervitajn nomojn (CON, PRN, AUX, NUL, COM1-9, LPT1-9).
 * - Limigas la longon (C3): RSS-id-oj ofte estas plenaj URL-oj —
 *   >255 bajtoj malsukcesigas la dosier-kreadon. Longaj nomoj ricevas
 *   8-signan haketaĵon de la plena ID por resti unikaj kaj koliziecaj
 *   nomoj restas distingeblaj.
 */
fun sanigiDosiernomon(id: String): String {
    var nomo = id
        .replace(Regex("[:/\\\\?*\"<>|]"), "-")
        .replace("\u0000", "")
        .trimEnd('.', ' ')
    // Windows-rezervitaj nomoj (ankaŭ kun etendaĵo, ekz. "CON.mp3") — prefiksu
    if (Regex("^(?i)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(\\..*)?$").matches(nomo)) {
        nomo = "_$nomo"
    }
    if (nomo.length > MAKS_DOSIERNOMO) {
        // Haketaĵo de la PLENA ID — la mallongigita nomo restas unika;
        // String.hashCode estas platform-kongrua (difinita en la Kotlin-specifo)
        val haketajho = (id.hashCode().toLong() and 0xffffffffL).toString(16).padStart(8, '0')
        nomo = nomo.take(MAKS_DOSIERNOMO - haketajho.length) + haketajho
    }
    return nomo
}

/** Maksimuma longo de la sankigita nomo (sen etendaĵo; NTFS/limo ~255 bajtoj). */
private const val MAKS_DOSIERNOMO = 120
