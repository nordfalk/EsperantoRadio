package dk.nordfalk.esperanto.domain.player

import kotlin.concurrent.Volatile

/**
 * Statika tenilo por sciigo-kontroloj (Venonta).
 *
 * La servo (EsperantoLudadoServo) vokas cxi tiun lambdon kiam la uzanto
 * klakas la "Venonta" butonon en la mediasciigo. La Compose-tavolo (App.kt)
 * fiksas gxin cxe app-starto por konekti gxin al LudvicoRegilo.
 */
object SciigoKontroloj {
    /** Vokata kiam la uzanto klakas "Venonta" en la sciigo. */
    @Volatile
    var onVenonta: (suspend () -> Unit)? = null
}
