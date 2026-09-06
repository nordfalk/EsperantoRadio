package dk.nordfalk.esperanto.android

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import dk.nordfalk.esperanto.domain.model.LudantoInformo
import dk.nordfalk.esperanto.domain.model.LudantoStato
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.domain.player.LudiloRegilo
import dk.nordfalk.esperanto.loge
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.logw
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android-implemento de LudiloRegilo per Media3 MediaController.
 *
 * La vera ExoPlayer vivas en [EsperantoLudadoServo] (MediaSessionService).
 * Tiu ĉi klaso estas maldika prokso: ĝi konektiĝas al la servo per
 * MediaController kaj plusendas komandojn. Tiel la ludado daŭras en la fono
 * eĉ kiam la Activity detruiĝas.
 */
class ExoPlayerLudiloRegilo(context: Context) : LudiloRegilo {
    private val _stato = MutableStateFlow(LudantoInformo(stato = LudantoStato.Haltita))
    override val stato: StateFlow<LudantoInformo> = _stato.asStateFlow()

    private var nunaFonto: Sonfonto? = null

    private val sessionToken = SessionToken(context, ComponentName(context, EsperantoLudadoServo::class.java))
    private val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
    private var controller: MediaController? = null

    /** Pleniĝas kiam la MediaController sukcese konektiĝis al la servo. */
    private val konektita = CompletableDeferred<Unit>()

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) { updateState() }
        override fun onIsPlayingChanged(isPlaying: Boolean) { updateState() }
        override fun onPlayerErrorChanged(error: PlaybackException?) {
            if (error != null) {
                loge("Ludilo", "Ludanta eraro", error)
                _stato.value = LudantoInformo(
                    stato = LudantoStato.Eraro(error.message ?: "Nekonata eraro"),
                    nunaFonto = nunaFonto,
                    estasRekta = nunaFonto is Sonfonto.RektaKanalo,
                )
            } else {
                updateState()
            }
        }
    }

    init {
        controllerFuture.addListener({
            try {
                val c = controllerFuture.get()
                controller = c
                c.addListener(listener)
                updateState()
                konektita.complete(Unit)
                logi("Ludilo", "MediaController konektita al servo")
            } catch (e: Exception) {
                logw("Ludilo", "Malsukcesis konekti MediaController", e)
                konektita.completeExceptionally(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun getStreamUrl(fonto: Sonfonto): String = when (fonto) {
        is Sonfonto.RektaKanalo -> fonto.kanal.rektaElsendaSonoUrl ?: ""
        is Sonfonto.ElsendoFonto -> fonto.elsendo.stream
        is Sonfonto.LokaElsendo -> "file://${fonto.dosieroVojo}"
    }

    /**
     * Konstruas MediaMetadata por la sciigo (titolo + emblemo).
     */
    private fun getMediaMetadata(fonto: Sonfonto): MediaMetadata {
        val builder = MediaMetadata.Builder()
        when (fonto) {
            is Sonfonto.RektaKanalo -> {
                builder.setTitle(fonto.kanal.nomo)
                fonto.kanal.emblemoUrl?.let { builder.setArtworkUri(Uri.parse(it)) }
            }
            is Sonfonto.ElsendoFonto -> {
                builder.setTitle(fonto.elsendo.titolo)
                fonto.elsendo.bildUrl?.let { builder.setArtworkUri(Uri.parse(it)) }
            }
            is Sonfonto.LokaElsendo -> {
                builder.setTitle(fonto.elsendo.titolo)
                fonto.elsendo.bildUrl?.let { builder.setArtworkUri(Uri.parse(it)) }
            }
        }
        return builder.build()
    }

    private fun updateState() {
        val c = controller ?: return
        val ludantoStato = when (c.playbackState) {
            Player.STATE_READY -> if (c.isPlaying) LudantoStato.Ludas else LudantoStato.Haltita
            Player.STATE_BUFFERING -> LudantoStato.Konektas
            Player.STATE_ENDED -> LudantoStato.Haltita
            Player.STATE_IDLE -> LudantoStato.Haltita
            else -> LudantoStato.Haltita
        }
        _stato.value = LudantoInformo(
            stato = ludantoStato,
            nunaFonto = nunaFonto,
            pozicioMs = c.currentPosition,
            dauroMs = if (c.duration > 0) c.duration else 0,
            estasRekta = nunaFonto is Sonfonto.RektaKanalo,
        )
    }

    override suspend fun fiksiFonton(fonto: Sonfonto, komencoPozicioMs: Long) {
        nunaFonto = fonto
        konektita.await()
        val url = getStreamUrl(fonto)
        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .setMediaMetadata(getMediaMetadata(fonto))
            .build()
        controller?.setMediaItem(mediaItem, komencoPozicioMs)
        controller?.prepare()
        _stato.value = LudantoInformo(
            stato = LudantoStato.Konektas,
            nunaFonto = fonto,
            pozicioMs = komencoPozicioMs,
            dauroMs = 0,
            estasRekta = fonto is Sonfonto.RektaKanalo,
        )
    }

    override fun ludi() { controller?.play() }
    override fun pauxzigi() { controller?.pause() }
    override fun halti() {
        controller?.stop()
        controller?.clearMediaItems()
        nunaFonto = null
        _stato.value = LudantoInformo(stato = LudantoStato.Haltita)
    }
    override fun saltiAl(pozicioMs: Long) { controller?.seekTo(pozicioMs) }
    override fun fiksiLauxtecon(volumeno: Float) { controller?.volume = volumeno }

    /**
     * Malkonektas la MediaController de la servo.
     * NE haltigas la servon — la servo pluvivas kaj daŭre ludas en la fono.
     * Vokata en Activity.onDestroy().
     */
    fun release() {
        controller?.removeListener(listener)
        MediaController.releaseFuture(controllerFuture)
        controller = null
    }
}
