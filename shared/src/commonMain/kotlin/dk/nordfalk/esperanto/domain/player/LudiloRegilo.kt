package dk.nordfalk.esperanto.domain.player

import dk.nordfalk.esperanto.domain.model.LudantoInformo
import dk.nordfalk.esperanto.domain.model.Sonfonto
import kotlinx.coroutines.flow.StateFlow

/**
 * Interfaco por la ludilo. La efektiva sonludado estas platform-specifa
 * (Media3 ExoPlayer sur Android, no-op sur Desktop/Web, AVPlayer sur iOS).
 *
 * La UI observas la StateFlow porMontri la ludanto-staton.
 */
interface LudiloRegilo {
    val stato: StateFlow<LudantoInformo>

    suspend fun fiksiFonton(fonto: Sonfonto, komencoPozicioMs: Long = 0)
    fun ludi()
    fun pauxzigi()
    fun halti()
    fun saltiAl(pozicioMs: Long)
    fun fiksiLauxtecon(volumeno: Float)
}
