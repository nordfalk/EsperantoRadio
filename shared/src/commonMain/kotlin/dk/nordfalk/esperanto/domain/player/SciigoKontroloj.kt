package dk.nordfalk.esperanto.domain.player

import kotlin.concurrent.Volatile

/**
 * Statika tenilo por sciigo-kontroloj (Venonta).
 *
 * La servo (EsperantoLudadoServo) vokas ĉi tiun lambdon kiam la uzanto
 * klakas la "Venonta" butonon en la ludada sciigo. La Compose-tavolo (App.kt)
 * fiksas ĝin ĉe apo-starto por konekti ĝin al LudvicoRegilo.
 */
object SciigoKontroloj {
    /** Vokata kiam la uzanto klakas "Venonta" en la sciigo. */
    @Volatile
    var onVenonta: (suspend () -> Unit)? = null
}
