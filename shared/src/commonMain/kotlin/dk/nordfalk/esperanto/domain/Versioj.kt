package dk.nordfalk.esperanto.domain

/**
 * Versio-komparado por la ĝisdatigo-indikilo (FAROTA C61a: `aktualaVersio`).
 *
 * Versio-ĉenoj havas la formon `MAJOR.MINOR.PATCH` (ekz. „3.2.0“), eventuale kun
 * sufikso (ekz. „-beta.1“, „-beta29okt“). Ni komparas nur la kernon — la gvidajn
 * ciferojn kaj punktojn; ĉio post la unua alia signo estas antaŭ-eldona marko,
 * kiu devas ne igi beta-version „pli nova“ ol ĝia eldonita bazo: „3.2.0-beta.1“
 * validas kiel EGALE al „3.2.0“.
 */

/**
 * Ĉu [aktuala] estas pli nova versio ol [nuna]?
 *
 * Komparas la numerajn komponantojn de la kernoj de maldekstre dekstren;
 * mankantaj komponantoj kalkuliĝas kiel 0. Se la kernoj estas egalaj, la versioj
 * estas egalaj (la sufikso estas ignorata — sed sufikso ne igas version pli
 * nova). Neparsebla aŭ malplena valoro (de iu ajn flanko) rezultigas `false`
 * (neniu averto pro fuŝita agordo — toleremo, A4).
 */
fun ĉuNovaVersio(nuna: String, aktuala: String): Boolean {
    if (aktuala.isBlank() || nuna.isBlank()) return false
    val nunaPartoj = leguKernajnNombrojn(nuna) ?: return false
    val aktualaPartoj = leguKernajnNombrojn(aktuala) ?: return false
    val longo = maxOf(nunaPartoj.size, aktualaPartoj.size)
    for (i in 0 until longo) {
        val n = nunaPartoj.getOrElse(i) { 0 }
        val a = aktualaPartoj.getOrElse(i) { 0 }
        if (a > n) return true
        if (a < n) return false
    }
    return false
}

/**
 * Prenas la kernon de versio-ĉeno (la gvidaj ciferoj kaj punktoj; ekz.
 * „3.1.1-beta29okt“ → 3, 1, 1) kaj redonas ĝin kiel nombron-liston.
 * Redonas `null` se la kernon ne parses — ekz. malplena, komunikiĝanta aŭ
 * finanta punktoj, ĉero anstataŭ cifero, aŭ komponanto tro granda por Int.
 */
private fun leguKernajnNombrojn(versio: String): List<Int>? {
    val kerno = versio.trim().takeWhile { it.isDigit() || it == '.' }
    // Strukturo-kontrolo: „3.2.0“; rifuzas malplenan kernon, gvidan/finan punkton
    // kaj „3..2“ — ĉiuj tajperaroj en la agordo (toleremo, A4: sen averto)
    if (!Regex("""\d+(\.\d+)*""").matches(kerno)) return null
    return kerno.split('.').map { it.toIntOrNull() ?: return null }  // tro longa komponanto → null
}

/** Ligilo al la apo-paĝo en Google Play — celo de la ĝisdatig-butono en la novaĵ-dialogo (nur Android). */
const val PLAY_STORE_LIGILO = "https://play.google.com/store/apps/details?id=dk.nordfalk.esperanto.radio"
