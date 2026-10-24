package dk.nordfalk.esperanto.data.repository

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Apaj novaĵoj — novaĵo-mesaĝo de la apo al la uzanto (la eksa
 * statusmesaĝo-mekanismo de la malnova apo).
 *
 * La mesaĝo venas el la `android`-sekcio de la kanalkonfiguro
 * (`apaj_novaĵoj`) kaj peras gravajn sciigojn al la uzanto (ekz.
 * versio-anoncojn aŭ ke Muzaiko ne funkcias). Kiel en la malnova apo,
 * la mesaĝo montriĝas kiel dialogo **unufoje por ĉiu nova enhavo**:
 * la haŝaĵo de la konfirmita mesaĝo persistiĝas en [Settings], kaj
 * nur kiam la haŝaĵo ŝanĝiĝas la dialogo reaperas.
 */
class ApajNovaĵojDeponejo(
    private val settings: Settings,
    mesaĝo: String?,
) {
    private val _montrata = MutableStateFlow(deciduMontratan(mesaĝo?.trim().orEmpty(), leguKonservitanHaŝon()))

    /** La mesaĝo montriĝu, aŭ `null` se nenio novas. */
    fun observuMontratan(): StateFlow<String?> = _montrata.asStateFlow()

    /**
     * La uzanto konfirmis la mesaĝon — persistigu la haŝaĵon kaj fermu.
     */
    fun konfirmu() {
        settings.skribuMigritanEntjeron(SettingsKeys.APAJ_NOVAĴOJ_HEŜ, SettingsKeys.APAJ_NOVAĴOJ_HEŜ_MALNOVA, _montrata.value.orEmpty().hashCode())
        _montrata.value = null
    }

    private fun leguKonservitanHaŝon(): Int =
        settings.leguMigritanEntjeroKunNulo(SettingsKeys.APAJ_NOVAĴOJ_HEŜ, SettingsKeys.APAJ_NOVAĴOJ_HEŜ_MALNOVA) ?: MALPLENA_HESH

    companion object {
        /** Haŝaĵo de malplena mesaĝo — ankaŭ la komenca valoro por novaj instalaĵoj. */
        const val MALPLENA_HESH = 0

        /**
         * Pura decidlogiko: ĉu tiu ĉi mesaĝo devas montriĝi?
         * Malplena mesaĝo neniam montriĝas; ne-malplena montriĝas nur se
         * ĝia haŝaĵo diferencas de la konservita.
         */
        fun deciduMontratan(mesaĝo: String, konservitaHaŝo: Int): String? =
            if (mesaĝo.isNotEmpty() && mesaĝo.hashCode() != konservitaHaŝo) mesaĝo else null
    }
}
