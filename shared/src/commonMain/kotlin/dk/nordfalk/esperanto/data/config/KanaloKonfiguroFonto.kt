package dk.nordfalk.esperanto.data.config

import com.russhwolf.settings.Settings
import dk.nordfalk.esperanto.data.repository.SettingsKeys

/**
 * Fora (servila) kanalkonfiguro. La apo elŝutas ĝin ĉe ĉiu relanĉo kaj konservas
 * la tekston en la agordoj (ŝlosilo [SettingsKeys.FORA_KANALKONFIGURO]); vidu
 * [dk.nordfalk.esperanto.data.repository.KanaloDeponejoImpl].
 *
 * La servilo publikigas ĝin kiel purigitan striktan JSON (sen komentoj) —
 * la leganto tamen akceptas ankaŭ la JSONC-varianton de la pakita dosiero.
 */
const val FORA_KONFIGURO_URL = "https://javabog.dk/esperanto-radio/esperantoradio_konfiguro.json"

/**
 * Legas la EFIKTVAN konfigura tekston: la kaŝitan foran version el [settings]
 * (se ekzistas), aliokaz la paktitan enkonstruitan version.
 *
 * Uzata de la fonaj laborkantoj (alarmo, novaj-elsendoj-kontrolo), kiuj ne
 * havas [dk.nordfalk.esperanto.data.repository.KanaloDeponejoImpl]-on — tiel
 * ili vidas la saman konfigur-version kiel la UI.
 */
fun leguKonfigurTekston(settings: Settings): String =
    settings.getStringOrNull(SettingsKeys.FORA_KANALKONFIGURO)?.ifBlank { null }
        ?: leguPakititanKanalkonfiguron()
