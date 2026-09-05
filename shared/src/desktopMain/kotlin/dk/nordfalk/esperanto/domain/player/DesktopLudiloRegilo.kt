package dk.nordfalk.esperanto.domain.player

import dk.nordfalk.esperanto.domain.model.LudantoInformo
import dk.nordfalk.esperanto.domain.model.LudantoStato
import dk.nordfalk.esperanto.domain.model.Sonfonto
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
import java.io.BufferedInputStream
import java.net.URL
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.FloatControl
import javax.sound.sampled.SourceDataLine

/**
 * Desktop-implemento de LudiloRegilo per javax.sound.sampled + mp3spi.
 *
 * Pura Java MP3-fluado super HTTP — neniu nacia dependeco.
 * mp3spi (com.googlecode.soundlibs:mp3spi) registrigas MP3-malkodilon
 * cxe AudioSystem, kiu tiam povas legi MP3-fluojn kaj konverti ilin al PCM.
 *
 * Protokolado: cxiuj protokoloj iras al stderr (videbla en terminalo).
 */
class DesktopLudiloRegilo : LudiloRegilo {

    private val _stato = MutableStateFlow(LudantoInformo(stato = LudantoStato.Haltita))
    override val stato: StateFlow<LudantoInformo> = _stato.asStateFlow()

    private var ludaJob: Job? = null
    private var sourceDataLine: SourceDataLine? = null
    private var audioInputStream: AudioInputStream? = null
    private var nunaFonto: Sonfonto? = null
    private var volumeno: Float = 1.0f

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var ludas = false
    @Volatile private var pauxzita = false
    @Volatile private var totalBytesLuditaj: Long = 0
    private var pcmFormat: AudioFormat? = null

    private fun getStreamUrl(fonto: Sonfonto): String = when (fonto) {
        is Sonfonto.RektaKanalo -> fonto.kanal.rektaElsendaSonoUrl ?: ""
        is Sonfonto.ElsendoFonto -> fonto.elsendo.stream
        is Sonfonto.LokaElsendo -> "file://${fonto.dosieroVojo}"
    }

    private fun fontoNomo(fonto: Sonfonto): String = when (fonto) {
        is Sonfonto.RektaKanalo -> "RektaKanalo(${fonto.kanal.nomo})"
        is Sonfonto.ElsendoFonto -> "ElsendoFonto(${fonto.elsendo.titolo})"
        is Sonfonto.LokaElsendo -> "LokaElsendo(${fonto.elsendo.titolo})"
    }

    override suspend fun fiksiFonton(fonto: Sonfonto, komencoPozicioMs: Long) {
        nunaFonto = fonto
        log("fiksiFonton: ${fontoNomo(fonto)}")

        val url = getStreamUrl(fonto)
        log("fiksiFonton: URL = $url")

        if (url.isBlank()) {
            log("fiksiFonton: ERARO — malplena sono-URL")
            _stato.value = LudantoInformo(
                stato = LudantoStato.Eraro("Malplena sono-URL"),
                nunaFonto = fonto,
                estasRekta = fonto is Sonfonto.RektaKanalo
            )
            return
        }

        halti()

        try {
            log("fiksiFonton: malfermas HTTP-fluon")
            val rawStream = BufferedInputStream(URL(url).openStream())
            log("fiksiFonton: akiras AudioInputStream")
            val mp3Stream = AudioSystem.getAudioInputStream(rawStream)
            audioInputStream = mp3Stream
            log("fiksiFonton: MP3-formato = ${mp3Stream.format}")

            // Konvertu al PCM por SourceDataLine
            val baseFormat = mp3Stream.format
            val decodedFormat = AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                baseFormat.sampleRate,
                16,
                baseFormat.channels,
                baseFormat.channels * 2,
                baseFormat.sampleRate,
                false
            )
            log("fiksiFonton: PCM-formato = $decodedFormat")
            val decodedStream = AudioSystem.getAudioInputStream(decodedFormat, mp3Stream)
            audioInputStream = decodedStream
            pcmFormat = decodedFormat

            // Krei SourceDataLine
            log("fiksiFonton: kreas SourceDataLine")
            val line = AudioSystem.getSourceDataLine(decodedFormat)
            line.open(decodedFormat)
            sourceDataLine = line
            log("fiksiFonton: SourceDataLine pretas, buffer = ${line.bufferSize} bytes")

            // Volumo
            try {
                val ctrl = line.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                val min = ctrl.minimum
                val max = ctrl.maximum
                val gain = min + (max - min) * volumeno
                ctrl.value = gain
                log("fiksiFonton: volumo = $volumeno (gain=${gain}dB, range=$min..$max)")
            } catch (e: Exception) {
                log("fiksiFonton: ne eblis agordi volumon: ${e.message}")
                e.printStackTrace(System.err)
            }

            // Dauro
            val frameLength = decodedStream.frameLength
            if (frameLength > 0) {
                val dauroMs = (frameLength * 1000L / decodedFormat.sampleRate.toLong())
                log("fiksiFonton: dauro = ${dauroMs}ms ($frameLength kadroj)")
                _stato.value = LudantoInformo(
                    stato = LudantoStato.Haltita,
                    nunaFonto = fonto,
                    pozicioMs = komencoPozicioMs,
                    dauroMs = dauroMs,
                    estasRekta = fonto is Sonfonto.RektaKanalo
                )
            } else {
                log("fiksiFonton: dauro nekonata (streaming)")
                _stato.value = LudantoInformo(
                    stato = LudantoStato.Haltita,
                    nunaFonto = fonto,
                    pozicioMs = komencoPozicioMs,
                    dauroMs = if (fonto is Sonfonto.ElsendoFonto) (fonto.elsendo.dauro ?: 0L) * 1000 else 0,
                    estasRekta = fonto is Sonfonto.RektaKanalo
                )
            }

            log("fiksiFonton: bone — preta por ludi")
        } catch (e: Exception) {
            log("fiksiFonton: ESCEPTO: ${e::class.simpleName}: ${e.message}")
            e.printStackTrace(System.err)
            _stato.value = _stato.value.copy(
                stato = LudantoStato.Eraro("Ne eblis sxargxi: ${e.message}")
            )
        }
    }

    private fun komenciLudadon() {
        ludaJob?.cancel()
        ludaJob = scope.launch {
            val line = sourceDataLine ?: return@launch
            val stream = audioInputStream ?: return@launch
            val format = pcmFormat ?: return@launch

            line.start()
            log("ludi: SourceDataLine.start()")

            val buffer = ByteArray(4096)
            totalBytesLuditaj = 0L

            while (isActive && ludas) {
                if (pauxzita) {
                    delay(50)
                    continue
                }
                val read = try {
                    stream.read(buffer)
                } catch (e: Exception) {
                    log("ludi: eraro legante fluon: ${e.message}")
                    e.printStackTrace(System.err)
                    -1
                }
                if (read <= 0) {
                    log("ludi: fino de fluo (read=$read)")
                    break
                }
                line.write(buffer, 0, read)
                totalBytesLuditaj += read

                // Aktualigu pozicion
                val pozicioMs = (totalBytesLuditaj * 1000L) /
                    (format.sampleRate.toLong() * format.channels * (format.sampleSizeInBits / 8))
                _stato.value = _stato.value.copy(pozicioMs = pozicioMs)
            }

            line.drain()
            line.stop()
            log("ludi: SourceDataLine haltigita")
            if (ludas) {
                // Fino de fluo — naturfino
                ludas = false
                _stato.value = _stato.value.copy(stato = LudantoStato.Haltita)
            }
        }
    }

    override fun ludi() {
        log("ludi()")
        val line = sourceDataLine
        val stream = audioInputStream
        if (line == null || stream == null) {
            log("ludi: ERARO — sourceDataLine aü audioInputStream estas null")
            return
        }
        ludas = true
        pauxzita = false
        _stato.value = _stato.value.copy(stato = LudantoStato.Ludas)
        komenciLudadon()
    }

    override fun pauxzigi() {
        log("pauxzigi()")
        pauxzita = true
        sourceDataLine?.stop()
        _stato.value = _stato.value.copy(stato = LudantoStato.Haltita)
    }

    override fun halti() {
        log("halti()")
        ludas = false
        pauxzita = false
        ludaJob?.cancel()
        ludaJob = null
        try {
            sourceDataLine?.stop()
            sourceDataLine?.close()
        } catch (e: Exception) {
            log("halti: eraro fermante line: ${e.message}")
            e.printStackTrace(System.err)
        }
        try {
            audioInputStream?.close()
        } catch (e: Exception) {
            log("halti: eraro fermante stream: ${e.message}")
            e.printStackTrace(System.err)
        }
        sourceDataLine = null
        audioInputStream = null
        nunaFonto = null
        _stato.value = LudantoInformo(stato = LudantoStato.Haltita)
    }

    override fun saltiAl(pozicioMs: Long) {
        log("saltiAl($pozicioMs ms) — ne implementita por mp3spi (streaming)")
        _stato.value = _stato.value.copy(pozicioMs = pozicioMs)
    }

    override fun fiksiLauxtecon(volumeno: Float) {
        val v = volumeno.coerceIn(0f, 1f)
        this.volumeno = v
        log("fiksiLauxtecon($v)")
        try {
            val line = sourceDataLine ?: return
            val ctrl = line.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
            val min = ctrl.minimum
            val max = ctrl.maximum
            ctrl.value = min + (max - min) * v
            log("fiksiLauxtecon: gain=${ctrl.value}dB")
        } catch (e: Exception) {
            log("fiksiLauxtecon: ne eblis: ${e.message}")
            e.printStackTrace(System.err)
        }
    }

    private fun log(msg: String) {
        System.err.println("[DesktopLudiloRegilo] $msg")
    }
}
