package dk.nordfalk.esperanto.navigation

import androidx.navigation3.runtime.NavKey
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanalo
import kotlinx.serialization.Serializable

/**
 * Vojoj (routes) por Navigation 3.
 *
 * Radikaj ekranoj (elekteblaj en la naviga tirkesto): [Hejmo], [Kanalaro],
 * [Plejŝatataj], [Sercxo] plus la flankaj [Elshutoj], [Alarmoj], [Agordoj].
 * Detal-ekranoj: [KanaloDetalo], [ElsendoDetalo]
 * Plenekranoj (ekster la tirkesto): [Ludvico], [Diagnozo]
 */
@Serializable
sealed interface Vojo : NavKey {
    @Serializable data object Hejmo : Vojo
    @Serializable data object Kanalaro : Vojo
    @Serializable data object Plejŝatataj : Vojo
    @Serializable data object Sercxo : Vojo
    @Serializable data object Elshutoj : Vojo
    @Serializable data object Ludvico : Vojo
    @Serializable data object Alarmoj : Vojo
    @Serializable data object Agordoj : Vojo
    @Serializable data object Diagnozo : Vojo
    @Serializable data class KanaloDetalo(val kanalo: Kanalo) : Vojo
    @Serializable data class ElsendoDetalo(val elsendo: Elsendo) : Vojo
}
