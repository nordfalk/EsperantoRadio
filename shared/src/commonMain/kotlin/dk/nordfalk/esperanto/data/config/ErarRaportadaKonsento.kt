package dk.nordfalk.esperanto.data.config

import dk.nordfalk.esperanto.AppStato
import dk.nordfalk.esperanto.data.repository.SettingsKeys

/**
 * Legas la uzanto-decidon pri erar-raportado al Sentry (PR #148/C50):
 * eksplicita agordo, aŭ — se la uzanto ankoraŭ neniam decidis — la
 * konstruo-specifa defaŭlto ([AppStato.erarRaportadoDefaŭlta]: kun nia
 * subskribo (Play/Aptoide) ŝaltita; sen ĝi (F-Droid) malŝaltita).
 * Por la platforma enirpunkto, kiu devas decidi ANTAŬ la Sentry-inicialigo —
 * en UI-kodo iru tra AgordojDeponejo.
 */
fun leguErarRaportadanKonsenton(): Boolean =
    kreuSettings().getBoolean(SettingsKeys.ERAR_RAPORTADO, AppStato.erarRaportadoDefaŭlta)
