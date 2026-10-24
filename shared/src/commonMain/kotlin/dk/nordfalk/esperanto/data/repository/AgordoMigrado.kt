package dk.nordfalk.esperanto.data.repository

import com.russhwolf.settings.Settings

/**
 * Migrado de la historiaj x-sistemaj agord-ŝlosiloj al veraj supersignoj
 * (2026-10-24). Semantiko (laŭ deziro de la posedanto): **la malnova ŝlosilo
 * uziĝas se la nova ne ekzistas**.
 *
 * - Legado: unue la nova ŝlosilo; se ĝi mankas, la malnova valoro redoniĝas.
 * - Skribado: ĉiam al la nova ŝlosilo, kaj la malnova foriĝas — la unua
 *   skribo finas la migradon por tiu ŝlosilo.
 */

/** Legas ĉenon: nova ŝlosilo unue, retrodefaŭlto al la malnova. */
fun Settings.leguMigritanĈenon(nova: String, malnova: String, defaŭlto: String): String =
    getStringOrNull(nova) ?: getString(malnova, defaŭlto)

/** Skribas ĉenon al la nova ŝlosilo kaj forigas la malnovan. */
fun Settings.skribuMigritanĈenon(nova: String, malnova: String, valoro: String) {
    putString(nova, valoro)
    if (hasKey(malnova)) remove(malnova)
}

/** Legas buleon: nova ŝlosilo unue, retrodefaŭlto al la malnova. */
fun Settings.leguMigritanBuleon(nova: String, malnova: String, defaŭlto: Boolean): Boolean =
    getBooleanOrNull(nova) ?: getBoolean(malnova, defaŭlto)

/** Skribas buleon al la nova ŝlosilo kaj forigas la malnovan. */
fun Settings.skribuMigritanBuleon(nova: String, malnova: String, valoro: Boolean) {
    putBoolean(nova, valoro)
    if (hasKey(malnova)) remove(malnova)
}

/** Legas entjeron (aŭ null): nova ŝlosilo unue, retrodefaŭlto al la malnova. */
fun Settings.leguMigritanEntjeroKunNulo(nova: String, malnova: String): Int? =
    getIntOrNull(nova) ?: getIntOrNull(malnova)

/** Skribas entjeron al la nova ŝlosilo kaj forigas la malnovan. */
fun Settings.skribuMigritanEntjeron(nova: String, malnova: String, valoro: Int) {
    putInt(nova, valoro)
    if (hasKey(malnova)) remove(malnova)
}
