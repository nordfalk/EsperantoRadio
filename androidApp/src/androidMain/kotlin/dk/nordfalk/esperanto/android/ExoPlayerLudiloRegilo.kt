package dk.nordfalk.esperanto.android

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dk.nordfalk.esperanto.domain.model.LudantoInformo
import dk.nordfalk.esperanto.domain.model.LudantoStato
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.domain.player.LudiloRegilo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android-implemento de LudiloRegilo per Media3 ExoPlayer.
 *
 * Kreita en androidApp kie Context haveblas. Transdonita al EsperantoRadioApp.
 */
class ExoPlayerLudiloRegilo(context: Context) : LudiloRegilo {
    private val player = ExoPlayer.Builder(context).build()
    private val _stato = MutableStateFlow(LudantoInformo(stato = LudantoStato.Haltita))
    override val stato: StateFlow<LudantoInformo> = _stato.asStateFlow()

    private var nunaFonto: Sonfonto? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                updateState()
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateState()
            }
        })
    }

    private fun getStreamUrl(fonto: Sonfonto): String = when (fonto) {
        is Sonfonto.RektaKanalo -> fonto.kanal.rektaElsendaSonoUrl ?: ""
        is Sonfonto.ElsendoFonto -> fonto.elsendo.stream
        is Sonfonto.LokaElsendo -> "file://${fonto.dosieroVojo}"
    }

    private fun updateState() {
        val ludantoStato = when (player.playbackState) {
            Player.STATE_READY -> if (player.isPlaying) LudantoStato.Ludas else LudantoStato.Haltita
            Player.STATE_BUFFERING -> LudantoStato.Konektas
            Player.STATE_ENDED -> LudantoStato.Haltita
            Player.STATE_IDLE -> LudantoStato.Haltita
            else -> LudantoStato.Haltita
        }
        _stato.value = LudantoInformo(
            stato = ludantoStato,
            nunaFonto = nunaFonto,
            pozicioMs = player.currentPosition,
            dauroMs = if (player.duration > 0) player.duration else 0,
            estasRekta = nunaFonto is Sonfonto.RektaKanalo,
        )
    }

    override suspend fun fiksiFonton(fonto: Sonfonto, komencoPozicioMs: Long) {
        nunaFonto = fonto
        val url = getStreamUrl(fonto)
        val mediaItem = MediaItem.fromUri(url)
        player.setMediaItem(mediaItem, komencoPozicioMs)
        player.prepare()
        _stato.value = LudantoInformo(
            stato = LudantoStato.Konektas,
            nunaFonto = fonto,
            pozicioMs = komencoPozicioMs,
            dauroMs = 0,
            estasRekta = fonto is Sonfonto.RektaKanalo,
        )
    }

    override fun ludi() { player.play() }
    override fun pauxzigi() { player.pause() }
    override fun halti() { player.stop(); player.clearMediaItems(); nunaFonto = null; _stato.value = LudantoInformo(stato = LudantoStato.Haltita) }
    override fun saltiAl(pozicioMs: Long) { player.seekTo(pozicioMs) }
    override fun fiksiLauxtecon(volumeno: Float) { player.volume = volumeno }

    /**
     * Liberigas la ExoPlayer-rimedojn. Devas esti vokita en Activity.onDestroy().
     */
    fun release() {
        player.release()
    }
}
