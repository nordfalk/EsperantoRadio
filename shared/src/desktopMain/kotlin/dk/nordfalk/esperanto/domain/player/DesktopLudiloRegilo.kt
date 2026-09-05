package dk.nordfalk.esperanto.domain.player

import dk.nordfalk.esperanto.domain.model.LudantoInformo
import dk.nordfalk.esperanto.domain.model.LudantoStato
import dk.nordfalk.esperanto.domain.model.Sonfonto
import javafx.application.Platform
import javafx.scene.media.Media
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
 * KOMPLETA — surbaze de JavaFX MediaPlayer:
 * - MP3 super HTTP (podkastoj): fluas, seek funkcias
 * - Rekta radio (Icecast/Shoutcast): fluas, seek ne sencas
 *
 * Skemo:
 *   Compose-fadeno → Platform.runLater { mediaPlayer.xxx() } → JavaFX-fadeno
 *   JavaFX-fadeno → statusListener → _stato.update() → Compose observas StateFlow
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
        // Komencigu JavaFX-platformon (unusfoje po JVM)
        try {
            Platform.startup { }
        } catch (_: IllegalStateException) {
            // Jam komencigita — bone
        }
    }

    private fun getStreamUrl(fonto: Sonfonto): String = when (fonto) {
        is Sonfonto.RektaKanalo -> fonto.kanal.rektaElsendaSonoUrl ?: ""
        is Sonfonto.ElsendoFonto -> fonto.elsendo.stream
    }

    override suspend fun fiksiFonton(fonto: Sonfonto, komencoPozicioMs: Long) {
        nunaFonto = fonto
        atendataPozicioMs = komencoPozicioMs
        pozicioJob?.cancel()

        val url = getStreamUrl(fonto)
        if (url.isBlank()) {
            _stato.value = LudantoInformo(
                stato = LudantoStato.Eraro("Malplena sono-URL"),
                nunaFonto = fonto,
                estasRekta = fonto is Sonfonto.RektaKanalo
            )
            return
        }

        Platform.runLater {
            mediaPlayer?.dispose()

            try {
                val media = Media(url)
                val player = MediaPlayer(media)
                mediaPlayer = player

                player.statusProperty().addListener { _, _, status ->
                    val novaStato = when (status) {
                        MediaPlayer.Status.READY -> {
                            // Seek al komencpozicio por podkastoj (ne rekta)
                            if (atendataPozicioMs > 0 && nunaFonto !is Sonfonto.RektaKanalo) {
                                player.seek(Duration.millis(atendataPozicioMs.toDouble()))
                            }
                            LudantoStato.Konektas
                        }
                        MediaPlayer.Status.PLAYING -> LudantoStato.Ludas
                        MediaPlayer.Status.PAUSED -> LudantoStato.Haltita
                        MediaPlayer.Status.STOPPED -> LudantoStato.Haltita
                        MediaPlayer.Status.HALTED -> LudantoStato.Eraro("JavaFX-ludilo haltis")
                        MediaPlayer.Status.DISPOSED -> LudantoStato.Haltita
                        MediaPlayer.Status.STALLED -> LudantoStato.Konektas
                        MediaPlayer.Status.UNKNOWN -> LudantoStato.Konektas
                        else -> LudantoStato.Konektas
                    }
                    _stato.value = _stato.value.copy(stato = novaStato)
                }

                player.totalDurationProperty().addListener { _, _, duration ->
                    val dauroMs = duration.toMillis()
                    if (!dauroMs.isNaN() && !dauroMs.isInfinite()) {
                        _stato.value = _stato.value.copy(dauroMs = dauroMs.toLong())
                    }
                }

                player.setOnError {
                    _stato.value = _stato.value.copy(
                        stato = LudantoStato.Eraro(player.error?.message ?: "JavaFX-ludila eraro")
                    )
                }

                _stato.value = LudantoInformo(
                    stato = LudantoStato.Konektas,
                    nunaFonto = fonto,
                    pozicioMs = komencoPozicioMs,
                    dauroMs = 0,
                    estasRekta = fonto is Sonfonto.RektaKanalo
                )
            } catch (e: Exception) {
                _stato.value = _stato.value.copy(
                    stato = LudantoStato.Eraro("Ne eblis ŝargi: ${e.message}")
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
        Platform.runLater {
            mediaPlayer?.play()
            komenciPoziciSekvadon()
        }
    }

    override fun pauxzigi() {
        pozicioJob?.cancel()
        Platform.runLater {
            mediaPlayer?.pause()
        }
    }

    override fun halti() {
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
        Platform.runLater {
            mediaPlayer?.seek(Duration.millis(pozicioMs.toDouble()))
        }
        _stato.value = _stato.value.copy(pozicioMs = pozicioMs)
    }

    override fun fiksiLauxtecon(volumeno: Float) {
        Platform.runLater {
            mediaPlayer?.volume = volumeno.coerceIn(0f, 1f).toDouble()
        }
    }
}
