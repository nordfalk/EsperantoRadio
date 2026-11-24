package dk.nordfalk.esperanto.navigation

import androidx.navigation3.runtime.NavKey
import kotlin.random.Random
import kotlinx.serialization.Serializable

/**
 * Vojoj (routes) por Navigation 3.
 *
 * Radikaj ekranoj (elekteblaj en la naviga tirkesto): [Hejmo], [Kanalaro],
 * [Plejŝatataj], [Serĉo] plus la flankaj [Elŝutoj], [Alarmoj], [Agordoj].
 * Detal-ekranoj: [KanaloDetalo], [ElsendoDetalo]
 * Plenekranoj (ekster la tirkesto): [Ludvico], [Diagnozo]
 */
@Serializable
sealed interface Vojo : NavKey {
    @Serializable data object Hejmo : Vojo
    @Serializable data object Kanalaro : Vojo
    @Serializable data object Plejŝatataj : Vojo
    @Serializable data object Serĉo : Vojo
    @Serializable data object Elŝutoj : Vojo
    @Serializable data object Ludvico : Vojo
    @Serializable data object Alarmoj : Vojo
    @Serializable data object Agordoj : Vojo
    @Serializable data object Diagnozo : Vojo
    // DETALAJ ŜLOSILOJ: ŝlosiloj portas identigilojn + vizito-kodon unikan po malfermo
    @Serializable data class KanaloDetalo(
        val slug: String,
        val vizito: String = Random.nextLong().toString(36),
    ) : Vojo
    @Serializable data class ElsendoDetalo(
        val id: String,
        val kanaloSlug: String,
        val vizito: String = Random.nextLong().toString(36),
    ) : Vojo
}
