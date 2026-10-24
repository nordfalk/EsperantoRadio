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
 * la haketaĵo de la konfirmita mesaĝo persistiĝas en [Settings], kaj
 * nur kiam la haketaĵo ŝanĝiĝas la dialogo reaperas.
 */
class ApajNovaĵojDeponejo(
    private val settings: Settings,
    mesaĝo: String?,
) {
    private val _montrata = MutableStateFlow(deciduMontratan(mesaĝo?.trim().orEmpty(), leguKonservitanHaketaĵon()))

    /** La mesaĝo montriĝu, aŭ `null` se nenio novas. */
    fun observuMontratan(): StateFlow<String?> = _montrata.asStateFlow()

    /**
     * La uzanto konfirmis la mesaĝon — persistigu la haketaĵon kaj fermu.
     */
    fun konfirmu() {
        settings.skribuMigritanEntjeron(SettingsKeys.APAJ_NOVAĴOJ_HAKETAĴO, SettingsKeys.APAJ_NOVAĴOJ_HAKETAĴO_MALNOVA, _montrata.value.orEmpty().hashCode())
        _montrata.value = null
    }

    private fun leguKonservitanHaketaĵon(): Int =
        settings.leguMigritanEntjeroKunNulo(SettingsKeys.APAJ_NOVAĴOJ_HAKETAĴO, SettingsKeys.APAJ_NOVAĴOJ_HAKETAĴO_MALNOVA) ?: MALPLENA_HAKETAĴO

    companion object {
        /** Haketaĵo de malplena mesaĝo — ankaŭ la komenca valoro por novaj instalaĵoj. */
        const val MALPLENA_HAKETAĴO = 0

        /**
         * Pura decidlogiko: ĉu tiu ĉi mesaĝo devas montriĝi?
         * Malplena mesaĝo neniam montriĝas; ne-malplena montriĝas nur se
         * ĝia haketaĵo diferencas de la konservita.
         */
        fun deciduMontratan(mesaĝo: String, konservitaHaketaĵo: Int): String? =
            if (mesaĝo.isNotEmpty() && mesaĝo.hashCode() != konservitaHaketaĵo) mesaĝo else null
    }
}
