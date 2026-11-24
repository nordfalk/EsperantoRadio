package dk.nordfalk.esperanto.domain

/**
 * Versio-komparado por la ĝisdatigo-indikilo (FAROTA C61a: `aktualaVersio`).
 *
 * Versio-ĉenoj havas la formon `MAJOR.MINOR.PATCH` (ekz. „3.2.0“), eventuale kun
 * sufikso (ekz. „3.1.1-beta29okt“). Ni komparas nur la numerajn partojn —
 * la sufikso estas antaŭ-eldona marko, kiu devas ne igi beta-version „pli nova“
 * ol sia eldonita bazo.
 */

/**
 * Ĉu [aktuala] estas pli nova versio ol [nuna]?
 *
 * Komparas la numerajn komponantojn de maldekstre dekstren; mankantaj
 * komponantoj kalkuliĝas kiel 0. Se la numeraj partoj estas egalaj, la versioj
 * estas egalaj (la sufikso estas ignorata). Neparsebla aŭ malplena aktuala
 * versio rezultigas `false` (neniu averto pro fuŝita agordo — toleremo, A4).
 */
fun ĉuNovaVersio(nuna: String, aktuala: String): Boolean {
    if (aktuala.isBlank() || nuna.isBlank()) return false
    val nunaPartoj = nuna.split('.').map { leguKomencanNumeron(it) }
    val aktualaPartoj = aktuala.split('.').map { leguKomencanNumeron(it) }
    val longo = maxOf(nunaPartoj.size, aktualaPartoj.size)
    for (i in 0 until longo) {
        val n = nunaPartoj.getOrElse(i) { 0 }
        val a = aktualaPartoj.getOrElse(i) { 0 }
        if (a > n) return true
        if (a < n) return false
    }
    return false
}

/** Legas la komencajn ciferojn de komponanto — „1-beta29okt“ → 1; neniu cifero → 0. */
private fun leguKomencanNumeron(komponanto: String): Int =
    komponanto.trim().takeWhile { it.isDigit() }.take(9).toIntOrNull() ?: 0

/** Ligilo al la apo-paĝo en Google Play — celo de la ĝisdatig-butono en la novaĵ-dialogo (nur Android). */
const val PLAY_STORE_LIGILO = "https://play.google.com/store/apps/details?id=dk.nordfalk.esperanto.radio"
