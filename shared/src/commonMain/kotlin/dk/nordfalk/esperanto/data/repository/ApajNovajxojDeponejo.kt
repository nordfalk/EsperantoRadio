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
class ApajNovajxojDeponejo(
    private val settings: Settings,
    mesagxo: String?,
) {
    private val _montrata = MutableStateFlow(deciduMontratan(mesagxo?.trim().orEmpty(), leguKonservitanHeshon()))

    /** La mesaĝo montriĝu, aŭ `null` se nenio novas. */
    fun observuMontratan(): StateFlow<String?> = _montrata.asStateFlow()

    /**
     * La uzanto konfirmis la mesaĝon — persistigu la haŝaĵon kaj fermu.
     */
    fun konfirmu() {
        settings.putInt(SettingsKeys.APAJ_NOVAJXOJ_HESH, _montrata.value.orEmpty().hashCode())
        _montrata.value = null
    }

    private fun leguKonservitanHeshon(): Int =
        settings.getIntOrNull(SettingsKeys.APAJ_NOVAJXOJ_HESH) ?: MALPLENA_HESH

    companion object {
        /** Haŝaĵo de malplena mesaĝo — ankaŭ la komenca valoro por novaj instalaĵoj. */
        const val MALPLENA_HESH = 0

        /**
         * Pura decidlogiko: ĉu tiu ĉi mesaĝo devas montriĝi?
         * Malplena mesaĝo neniam montriĝas; ne-malplena montriĝas nur se
         * ĝia haŝaĵo diferencas de la konservita.
         */
        fun deciduMontratan(mesagxo: String, konservitaHesho: Int): String? =
            if (mesagxo.isNotEmpty() && mesagxo.hashCode() != konservitaHesho) mesagxo else null
    }
}
