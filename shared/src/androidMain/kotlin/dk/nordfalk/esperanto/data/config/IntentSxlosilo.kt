package dk.nordfalk.esperanto.data.config

import android.content.Intent
import dk.nordfalk.esperanto.data.repository.SettingsKeys
import java.util.UUID

/**
 * Protektas la intencojn al la eksportita [android.app.Activity] kontraŭ falsado (FAROTA G3).
 *
 * `MainActivity` estas `exported="true"` (necesa por la lanĉilo) kaj traktas la agojn
 * `ALARMO_EKIGAS` kaj `MALFERMI_ELSENDON` kun ekstraĵoj — kiujn alie IA AJN apliko
 * povus forĝi per eksplicita intenco: ludi ajnan URI-on, montri atakant-kontrolitan
 * titolo/priskribo (phishing ene de la apo) kaj altigi la sisteman laŭtecon.
 *
 * La solvo: po-instala hazarda [nonce] (konservata en Settings), kiun la apo almetas
 * al ĉiu propra PendingIntent kaj kiun `MainActivity` kontrolas je ĉiu ricevo.
 * Fremdaj intencoj ne konas ĝin kaj estas rifuzataj.
 */
object IntentSxlosilo {

    /** Nomo de la intent-ekstraĵo kiu portas la nonce-on. */
    const val EXTRA_NONCE = "dk.nordfalk.esperanto.INTENT_NONCE"

    /** Almetas la nonce-on al intenco sendata al la eksportita Activity. */
    fun aldonuNomon(intent: Intent) {
        intent.putExtra(EXTRA_NONCE, leguNomon())
    }

    /**
     * Kontrolas ke la intenco venas de ni mem. Voku por ĉiu intenco kun
     * propra ago antaŭ ol legi ĝiajn ekstraĵojn.
     */
    fun estasFidinda(intent: Intent): Boolean =
        intent.getStringExtra(EXTRA_NONCE) == leguNomon()

    /** Legas (aŭ unuafoje kreas kaj persistigas) la po-instalan nonce-on. */
    private fun leguNomon(): String {
        val settings = kreuSettings()
        settings.getStringOrNull(SettingsKeys.INTENT_NONCE)?.let { return it }
        val nova = UUID.randomUUID().toString()
        settings.putString(SettingsKeys.INTENT_NONCE, nova)
        return nova
    }
}
