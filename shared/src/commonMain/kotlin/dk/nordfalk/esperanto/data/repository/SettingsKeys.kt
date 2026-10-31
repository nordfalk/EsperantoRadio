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
    const val AŬTOMATA_DAŬRIGO = "aŭtomata_daŭrigo"
    /** Ĉu la ludilo transsaltas la ripetiĝantan enkondukon de kanaloj (C59a). */
    const val TRANSSALTU_ENKONDUKOJN = "transsaltu_enkondukojn"
    /** Ĉu montri la snackbar-on „transsaltis la enkondukon" (forŝaltebla per „Bone"). */
    const val MONTRU_TRANSSALTAN_SCIIGON = "montru_transsaltan_sciigon"
    const val EVOLO = "evoluo"
    /** Ĉu la uzanto ŝaltis "detalan protokoladon" (pliaj logi-vokoj al Sentry). */
    const val DETALA_PROTOKOLADO = "detala_protokolado"
    /**
     * Ĉu la uzanto konsentis al aŭtomata erar-raportado al Sentry (opt-in,
     * defaŭlte malŝaltita — F-Droid-kontraŭeco "Tracking" alie; C50/PR #148).
     */
    const val ERAR_RAPORTADO = "erar_raportado"
    /** Ĉu la konsento-demando pri erar-raportado jam estis respondita (dialogo unufoje). */
    const val ERAR_RAPORTADO_DEMANDITA = "erar_raportado_demandita"
    const val KAŜITAJ_AVERTOJ = "kaŝitaj_avertoj"
    /** Po-instala hazarda valoro kiu protektas la intencojn al la eksportita Activity (PR #89). */
    const val INTENT_NONCE = "intent_nonce"
    /** Kreskanta kontraŭo por unikaj sciig-ID-oj ((PR #93) — hashCode-kolizioj anstataŭigis sciigojn). */
    const val SCIIG_ID = "sciig_id"
    /** Registrejo de la aktivaj sciigoj ("id:slug,id:slug") por la sciiglimoj (maks. 7 / maks. 3 po kanalo). */
    const val SCIIG_REGISTREJO = "sciig_registrejo"
    /** Haketaĵo de la lasta konfirmitaj apaj novaĵoj (nul = neniam montrita / malplena). */
    const val APAJ_NOVAĴOJ_HAKETAĴO = "apaj_novaĵoj_haketaĵo"

    /**
     * Historiaj x-sistemaj ŝlosiloj (antaŭ la ortografia renomo, 2026-10-24) —
     * uzataj NUR de AgordoMigrado por retrodefaŭlta legado; ĉiu skribo iras al
     * la nova ŝlosilo kaj forigas la malnovan.
     */
    const val AŬTOMATA_DAŬRIGO_MALNOVA = "auxtomata_daurigo"
    const val KAŜITAJ_AVERTOJ_MALNOVA = "kasxitaj_avertoj"
    const val APAJ_NOVAĴOJ_HAKETAĴO_MALNOVA = "apaj_novajxoj_hesh"
}
