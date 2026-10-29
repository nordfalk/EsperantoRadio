package dk.nordfalk.esperanto.data.config

import dk.nordfalk.esperanto.data.repository.SettingsKeys

/**
 * Legas la uzanto-konsenton pri erar-raportado al Sentry (opt-in, defaŭlte
 * malŝaltita; C50/PR #148). Por la platforma enirpunkto, kiu devas decidi
 * ANTAŬ la Sentry-inicialigo — en UI-kodo iru tra AgordojDeponejo.
 */
fun leguErarRaportadanKonsenton(): Boolean =
    kreuSettings().getBoolean(SettingsKeys.ERAR_RAPORTADO, false)
