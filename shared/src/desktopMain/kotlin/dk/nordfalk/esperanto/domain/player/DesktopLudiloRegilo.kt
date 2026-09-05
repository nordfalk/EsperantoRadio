package dk.nordfalk.esperanto.domain.player

import dk.nordfalk.esperanto.domain.model.LudantoInformo
import dk.nordfalk.esperanto.domain.model.LudantoStato
import dk.nordfalk.esperanto.domain.model.Sonfonto
import javafx.application.Platform
import javafx.scene.media.Media
import javafx.scene.media.MediaException
import javafx.scene.media.MediaPlayer
import javafx.util.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Desktop-implemento de LudiloRegilo per JavaFX MediaPlayer.
 *
 * Subtenas MP3-fluadon super HTTP, seek, volumon kaj pozicion.
 * JavaFX Platform estas komencigita unufoje per Platform.startup().
 *
 * Protokolado: cxiuj protokoloj iras al stderr (videbla en terminalo).
 */
class DesktopLudiloRegilo : LudiloRegilo {

    private val _stato = MutableStateFlow(LudantoInformo(stato = LudantoStato.Haltita))
    override val stato: StateFlow<LudantoInformo> = _stato.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var nunaFonto: Sonfonto? = null
    private var atendataPozicioMs: Long = 0

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pozicioJob: Job? = null

    init {
        log("Komencigas JavaFX-platformon")
        try {
            Platform.startup {
                log("JavaFX-platformo pretas")
            }
        } catch (_: IllegalStateException) {
            log("JavaFX-platformo jam komencigita")
        }
    }

    private fun getStreamUrl(fonto: Sonfonto): String = when (fonto) {
        is Sonfonto.RektaKanalo -> fonto.kanal.rektaElsendaSonoUrl ?: ""
        is Sonfonto.ElsendoFonto -> fonto.elsendo.stream
    }

    private fun fontoNomo(fonto: Sonfonto): String = when (fonto) {
        is Sonfonto.RektaKanalo -> "RektaKanalo(${fonto.kanal.nomo})"
        is Sonfonto.ElsendoFonto -> "ElsendoFonto(${fonto.elsendo.titolo})"
    }

    override suspend fun fiksiFonton(fonto: Sonfonto, komencoPozicioMs: Long) {
        nunaFonto = fonto
        atendataPozicioMs = komencoPozicioMs
        pozicioJob?.cancel()

        val url = getStreamUrl(fonto)
        log("fiksiFonton: ${fontoNomo(fonto)}")
        log("fiksiFonton: URL = $url")
        log("fiksiFonton: komencoPozicioMs = $komencoPozicioMs")

        if (url.isBlank()) {
            log("fiksiFonton: ERARO — malplena sono-URL")
            _stato.value = LudantoInformo(
                stato = LudantoStato.Eraro("Malplena sono-URL"),
                nunaFonto = fonto,
                estasRekta = fonto is Sonfonto.RektaKanalo
            )
            return
        }

        Platform.runLater {
            log("fiksiFonton: disponigas antauxvan MediaPlayer")
            mediaPlayer?.dispose()

            try {
                log("fiksiFonton: kreas Media(url)")
                val media = Media(url)
                log("fiksiFonton: kreas MediaPlayer(media)")
                val player = MediaPlayer(media)
                mediaPlayer = player
                log("fiksiFonton: MediaPlayer kreita sukcese")

                player.statusProperty().addListener { _, oldStatus, status ->
                    log("statuso: $oldStatus -> $status")
                    val novaStato = when (status) {
                        MediaPlayer.Status.READY -> {
                            log("statuso READY: dauro = ${player.totalDuration}")
                            if (atendataPozicioMs > 0 && nunaFonto !is Sonfonto.RektaKanalo) {
                                log("statuso READY: seek al ${atendataPozicioMs}ms")
                                player.seek(Duration.millis(atendataPozicioMs.toDouble()))
                            }
                            LudantoStato.Konektas
                        }
                        MediaPlayer.Status.PLAYING -> LudantoStato.Ludas
                        MediaPlayer.Status.PAUSED -> LudantoStato.Haltita
                        MediaPlayer.Status.STOPPED -> LudantoStato.Haltita
                        MediaPlayer.Status.HALTED -> {
                            log("statuso HALTED — MediaPlayer eraro: ${player.error}")
                            LudantoStato.Eraro("JavaFX-ludilo haltis: ${player.error?.message}")
                        }
                        MediaPlayer.Status.DISPOSED -> LudantoStato.Haltita
                        MediaPlayer.Status.STALLED -> LudantoStato.Konektas
                        MediaPlayer.Status.UNKNOWN -> LudantoStato.Konektas
                        else -> LudantoStato.Konektas
                    }
                    _stato.value = _stato.value.copy(stato = novaStato)
                }

                player.totalDurationProperty().addListener { _, _, duration ->
                    val dauroMs = duration.toMillis()
                    log("dauro aktualigita: $duration (${dauroMs}ms)")
                    if (!dauroMs.isNaN() && !dauroMs.isInfinite()) {
                        _stato.value = _stato.value.copy(dauroMs = dauroMs.toLong())
                    }
                }

                player.setOnError {
                    val err = player.error
                    log("=== MediaPlayer.onError ===")
                    log("eraro: $err")
                    log("eraro.type: ${err?.type}")
                    log("eraro.message: ${err?.message}")
                    log("eraro.cause: ${err?.cause}")
                    if (err is MediaException) {
                        log("eraro.MediaException.type: ${err.type}")
                        log("eraro.MediaException.message: ${err.message}")
                    }
                    log("=== fino de eraro ===")
                    _stato.value = _stato.value.copy(
                        stato = LudantoStato.Eraro(err?.message ?: "JavaFX-ludila eraro")
                    )
                }

                _stato.value = LudantoInformo(
                    stato = LudantoStato.Konektas,
                    nunaFonto = fonto,
                    pozicioMs = komencoPozicioMs,
                    dauroMs = 0,
                    estasRekta = fonto is Sonfonto.RektaKanalo
                )
                log("fiksiFonton: stato = Konektas, atendas statuson READY")
            } catch (e: Exception) {
                log("fiksiFonton: ESCEPTO: ${e::class.simpleName}: ${e.message}")
                log("fiksiFonton: stack-trace:")
                e.printStackTrace(System.err)
                _stato.value = _stato.value.copy(
                    stato = LudantoStato.Eraro("Ne eblis sxargxi: ${e.message}")
                )
            }
        }
    }

    private fun komenciPoziciSekvadon() {
        pozicioJob?.cancel()
        pozicioJob = scope.launch {
            while (isActive) {
                delay(500)
                val mp = mediaPlayer ?: continue
                Platform.runLater {
                    val pos = mp.currentTime.toMillis()
                    if (!pos.isNaN() && !pos.isInfinite()) {
                        _stato.value = _stato.value.copy(pozicioMs = pos.toLong())
                    }
                }
            }
        }
    }

    override fun ludi() {
        log("ludi()")
        Platform.runLater {
            val mp = mediaPlayer
            if (mp == null) {
                log("ludi: ERARO — mediaPlayer estas null")
                return@runLater
            }
            log("ludi: vokas mediaPlayer.play()")
            mp.play()
            komenciPoziciSekvadon()
        }
    }

    override fun pauxzigi() {
        log("pauxzigi()")
        pozicioJob?.cancel()
        Platform.runLater {
            mediaPlayer?.pause()
        }
    }

    override fun halti() {
        log("halti()")
        pozicioJob?.cancel()
        Platform.runLater {
            mediaPlayer?.stop()
            mediaPlayer?.dispose()
            mediaPlayer = null
        }
        nunaFonto = null
        _stato.value = LudantoInformo(stato = LudantoStato.Haltita)
    }

    override fun saltiAl(pozicioMs: Long) {
        log("saltiAl($pozicioMs ms)")
        Platform.runLater {
            mediaPlayer?.seek(Duration.millis(pozicioMs.toDouble()))
        }
        _stato.value = _stato.value.copy(pozicioMs = pozicioMs)
    }

    override fun fiksiLauxtecon(volumeno: Float) {
        log("fiksiLauxtecon($volumeno)")
        Platform.runLater {
            mediaPlayer?.volume = volumeno.coerceIn(0f, 1f).toDouble()
        }
    }

    private fun log(msg: String) {
        System.err.println("[DesktopLudiloRegilo] $msg")
    }
}
