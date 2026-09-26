package dk.nordfalk.esperanto.android

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import dk.nordfalk.esperanto.AppStato
import dk.nordfalk.esperanto.data.repository.LudiElsendoReceivilo
import dk.nordfalk.esperanto.domain.player.LudiloRegilo
import dk.nordfalk.esperanto.domain.player.SonfontoKodilo
import dk.nordfalk.esperanto.loge
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.logw
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
 *
 * Estas **unu instanco por la tuta procezo** ([akiru]) — ne po Activity. [AppStato] kaj
 * [dk.nordfalk.esperanto.domain.player.LudvicoRegilo] tenas referencon dum la tuta procezo;
 * se ĉiu Activity kreus propran instancon (kaj liberigus ĝin en onDestroy), post rekreo la
 * LudvicoRegilo sendus komandojn al malkonektita MediaController kaj la UI observus instancon
 * kiu ne scias kion la servo ludas.
 *
 * La nuna [Sonfonto] vojaĝas kun la MediaItem (en `MediaMetadata.extras`, vidu [SonfontoKodilo]),
 * do ĉi tiu klaso rekonas ludadon komencitan de alia MediaController (ekz. [LudiElsendoReceivilo]).
 */
class ExoPlayerLudiloRegilo private constructor(context: Context) : LudiloRegilo {

    companion object {
        @Volatile private var instanco: ExoPlayerLudiloRegilo? = null

        /** La procez-nivela instanco — kreita ĉe la unua voko. */
        fun akiru(context: Context): ExoPlayerLudiloRegilo =
            instanco ?: synchronized(this) {
                instanco ?: ExoPlayerLudiloRegilo(context.applicationContext).also { instanco = it }
            }
    }
    private val _stato = MutableStateFlow(LudantoInformo(stato = LudantoStato.Haltita))
    override val stato: StateFlow<LudantoInformo> = _stato.asStateFlow()

    private var nunaFonto: Sonfonto? = null

    private val cxefaFadeno = Handler(Looper.getMainLooper())

    private val appContext = context.applicationContext
    private val sessionToken = SessionToken(appContext, ComponentName(appContext, EsperantoLudadoServo::class.java))
    private val controllerFuture = MediaController.Builder(appContext, sessionToken).buildAsync()
    private var controller: MediaController? = null

    /** Pleniĝas kiam la MediaController sukcese konektiĝis al la servo. */
    private val konektita = CompletableDeferred<Unit>()

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) { updateState() }
        override fun onIsPlayingChanged(isPlaying: Boolean) { updateState() }
        override fun onPlayerErrorChanged(error: PlaybackException?) {
            if (error != null) loge("Ludilo", "Ludanta eraro", error)
            updateState()
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // Alia MediaController (ekz. la sciigo "Ludi") eble ŝanĝis la ludatan aĵon
            val fonto = fontoDe(mediaItem)
            if (fonto != null && fonto != nunaFonto) {
                logi("Ludilo", "Ludata aĵo ŝanĝiĝis ekstere — nun: ${fonto::class.simpleName}")
                nunaFonto = fonto
            }
            updateState()
        }
    }

    /** Legas la Sonfonton el la ekstraĵoj de MediaItem (null se mankas — ekz. malnova aĵo). */
    private fun fontoDe(mediaItem: MediaItem?): Sonfonto? =
        SonfontoKodilo.malkodigu(mediaItem?.mediaMetadata?.extras?.getString(SonfontoKodilo.SXLOSILO))

    init {
        controllerFuture.addListener({
            try {
                val c = controllerFuture.get()
                controller = c
                c.addListener(listener)
                // La servo eble jam ludas (ekz. komencita de la sciigo) — rekonu kion
                if (nunaFonto == null) {
                    nunaFonto = fontoDe(c.currentMediaItem)
                    nunaFonto?.let { logi("Ludilo", "Servo jam ludas: ${it::class.simpleName}") }
                }
                updateState()
                konektita.complete(Unit)
                logi("Ludilo", "MediaController konektita al servo")
            } catch (e: Exception) {
                logw("Ludilo", "Malsukcesis konekti MediaController", e)
                konektita.completeExceptionally(e)
            }
        }, ContextCompat.getMainExecutor(appContext))
    }

    private fun getStreamUrl(fonto: Sonfonto): String = when (fonto) {
        is Sonfonto.RektaKanalo -> fonto.kanalo.rektaElsendaSonoUrl ?: ""
        is Sonfonto.ElsendoFonto -> fonto.elsendo.fluo
        is Sonfonto.LokaElsendo -> "file://${fonto.dosieroVojo}"
    }

    /**
     * Konstruas MediaMetadata por la sciigo (titolo + emblemo).
     */
    private fun getMediaMetadata(fonto: Sonfonto): MediaMetadata {
        val builder = MediaMetadata.Builder()
            .setExtras(Bundle().apply { putString(SonfontoKodilo.SXLOSILO, SonfontoKodilo.kodigu(fonto)) })
        when (fonto) {
            is Sonfonto.RektaKanalo -> {
                builder.setTitle(fonto.kanalo.nomo)
                fonto.kanalo.emblemoUrl?.let { builder.setArtworkUri(Uri.parse(it)) }
            }
            is Sonfonto.ElsendoFonto -> {
                builder.setTitle(fonto.elsendo.titolo)
                fonto.elsendo.kanaloNomo?.let { builder.setArtist(it) }
                fonto.elsendo.bildoUrl?.let { builder.setArtworkUri(Uri.parse(it)) }
            }
            is Sonfonto.LokaElsendo -> {
                builder.setTitle(fonto.elsendo.titolo)
                fonto.elsendo.kanaloNomo?.let { builder.setArtist(it) }
                fonto.elsendo.bildoUrl?.let { builder.setArtworkUri(Uri.parse(it)) }
            }
        }
        return builder.build()
    }

    private fun updateState() {
        val c = controller ?: return
        // Se eraro ekzistas, konservu Eraro-staton — onPlaybackStateChanged(STATE_IDLE)
        // vokas updateState() tuj post onPlayerErrorChanged kaj povus superskribi Eraro per Haltita.
        // StateFlow estas conflated, do la kolektanto povus maltrafi Eraro se ni ne gardas ĝin ĉi tie.
        val error = c.playerError
        val ludantoStato = if (error != null) {
            LudantoStato.Eraro(error.message ?: "Nekonata eraro", reprovebla = estasReprovebla(error.errorCode))
        } else when (c.playbackState) {
            Player.STATE_READY -> if (c.isPlaying) LudantoStato.Ludas else LudantoStato.Haltita
            Player.STATE_BUFFERING -> LudantoStato.Konektas
            Player.STATE_ENDED -> LudantoStato.Finita
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

    override suspend fun fiksiFonton(fonto: Sonfonto, komencoPozicioMs: Long) = withContext(Dispatchers.Main) {
        nunaFonto = fonto
        konektita.await()
        val url = getStreamUrl(fonto)
        logi("Ludilo", "Fiksas fonton al url: $url")
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

    override fun ludi() { cxefaFadeno.post { controller?.play() } }
    override fun pauxzigi() { cxefaFadeno.post { controller?.pause() } }
    override fun halti() {
        cxefaFadeno.post {
            controller?.stop()
            controller?.clearMediaItems()
        }
        nunaFonto = null
        _stato.value = LudantoInformo(stato = LudantoStato.Haltita)
    }
    override fun saltiAl(pozicioMs: Long) { cxefaFadeno.post { controller?.seekTo(pozicioMs) } }
    override fun fiksiLauxtecon(volumeno: Float) { cxefaFadeno.post { controller?.volume = volumeno } }

    /** Pasemaj eraroj (reto, tempolimo) estas reprovataj; daŭraj (HTTP-stato, formato, malkodilo) ne. */
    private fun estasReprovebla(kodo: Int): Boolean = when (kodo) {
        PlaybackException.ERROR_CODE_UNSPECIFIED,
        PlaybackException.ERROR_CODE_REMOTE_ERROR,
        PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW,
        PlaybackException.ERROR_CODE_TIMEOUT,
        PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> true
        else -> false
    }
}
