package dk.nordfalk.esperanto.navigation

import androidx.navigation3.runtime.NavKey
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanal
import kotlinx.serialization.Serializable

/**
 * Vojoj (routes) por Navigation 3.
 *
 * Radikaj ekranoj (tab-oj): [Hejmo], [Kanalaro], [Plejsatataj], [Sercxo]
 * Detal-ekranoj: [KanalDetalo], [ElsendoDetalo]
 * Plenekranoj: [Elshutoj], [Alarmoj], [Agordoj]
 */
@Serializable
sealed interface Vojo : NavKey {
    @Serializable data object Hejmo : Vojo
    @Serializable data object Kanalaro : Vojo
    @Serializable data object Plejsatataj : Vojo
    @Serializable data object Sercxo : Vojo
    @Serializable data object Elshutoj : Vojo
    @Serializable data object Alarmoj : Vojo
    @Serializable data object Agordoj : Vojo
    @Serializable data class KanalDetalo(val kanal: Kanal) : Vojo
    @Serializable data class ElsendoDetalo(val elsendo: Elsendo) : Vojo
}
