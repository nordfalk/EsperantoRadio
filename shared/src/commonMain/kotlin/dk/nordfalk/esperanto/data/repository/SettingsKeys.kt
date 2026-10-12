package dk.nordfalk.esperanto.data.repository

/**
 * Ŝlosiloj por Settings (SharedPreferences / multiplatform-settings).
 *
 * Ĉiuj ŝlosiloj estas unu loko por eviti literumajn erarojn kaj teni sinkronigon.
 */
object SettingsKeys {
    const val PLEJŜATATAJ_KANALOJ = "plejŝatataj_kanaloj"
    const val SCIIGOJ = "sciigoj"
    const val VIDITAJ_ELSENDOJ = "viditaj_elsendoj"
    const val ALARMOJ = "alarmoj"
    /** Rezervo de la kruda alarmo-JSON kiam la ĉefa valoro ne plu estas parsebla (PR #95). */
    const val ALARMOJ_REZERVO = "alarmoj_rezervo"
    const val AUXTOMATA_DAURIGO = "auxtomata_daurigo"
    const val EVOLO = "evoluo"
    /** Ĉu la uzanto ŝaltis "detalan protokoladon" (pliaj logi-vokoj al Sentry). */
    const val DETALA_PROTOKOLADO = "detala_protokolado"
    const val KASXITAJ_AVERTOJ = "kasxitaj_avertoj"
    /** Po-instala hazarda valoro kiu protektas la intencojn al la eksportita Activity (PR #89). */
    const val INTENT_NONCE = "intent_nonce"
    /** Kreskanta kontraŭo por unikaj sciig-ID-oj ((PR #93) — hashCode-kolizioj anstataŭigis sciigojn). */
    const val SCIIG_ID = "sciig_id"
}
