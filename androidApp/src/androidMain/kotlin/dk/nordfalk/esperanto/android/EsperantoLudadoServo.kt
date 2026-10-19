package dk.nordfalk.esperanto.android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent

import android.content.pm.ServiceInfo
import android.os.Bundle
import androidx.core.app.ServiceCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dk.nordfalk.esperanto.AppStato
import dk.nordfalk.esperanto.domain.model.LudantoStato
import dk.nordfalk.esperanto.domain.player.SciigoKontroloj
import dk.nordfalk.esperanto.domain.player.SonfontoKodilo
import dk.nordfalk.esperanto.logd
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.logw
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Fona ludado-servo per Media3 MediaSessionService.
 *
 * Posedas la ExoPlayer kaj MediaSession. Kiam ludado komenciĝas, la servo
 * aŭtomate iĝas malfona kun mediasciigo (sic sciig-permeso estas donita).
 * Tiel la ludado daŭras eĉ kiam la apo estas en la fono.
 *
 * Sonfokuso, kapaŭskultil-malkonekto kaj mediabutonoj estas aŭtomate
 * traktataj de ExoPlayer per [AudioAttributes] kaj [setHandleAudioBecomingNoisy].
 */
class EsperantoLudadoServo : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        private val CMD_VENONTA = SessionCommand("venonta", Bundle.EMPTY)
        private const val KANALO_DEFENDA = "ludado_defenda"
        private const val DEFENDA_SCIIG_ID = 4242
        private const val DEFENDA_TEMPO_MS = 4_000L
    }

    private var defendTasko: Job? = null

    override fun onCreate() {
        super.onCreate()
        logi("LudadoServo", "Servo kreita")

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true) // true = aŭtomata sonfokuso
            .setHandleAudioBecomingNoisy(true)          // paŭzas kiam kapaŭskultiloj malkonektiĝas
            .build()

        // Aŭtoludo en la servo mem: kiam elsendo finiĝas (STATE_ENDED), la servo
        // vokas LudvicoRegilo rekte — tiel la aŭtoludo funkcias eĉ se la aplika
        // procezo estas en kaŝmemoro aŭ la MediaController estas malrapida.
        // La protekto en LudvicoRegilo (dedup-fenestro) evitas duoblan traktadon.
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    logi("LudadoServo", "STATE_ENDED — vokas aŭtoludon en servo")
                    serviceScope.launch {
                        try {
                            val regilo = AppStato.ludvicoRegilo
                            if (regilo != null) {
                                // Transdonu la FINANTAN fonton (PR #89) — ne relegu ĝin en
                                // LudvicoRegilo, kie la aŭtoludo eble jam avancis al la sekva
                                val finantaFonto = SonfontoKodilo.malkodigu(
                                    player.currentMediaItem?.mediaMetadata?.extras
                                        ?.getString(SonfontoKodilo.SXLOSILO)
                                )
                                regilo.traktiFinonPublika(LudantoStato.Finita, erara = false, finantaFonto = finantaFonto)
                            } else {
                                logw("LudadoServo", "AppStato.ludvicoRegilo estas null — ne povas aŭtoludi")
                            }
                        } catch (e: kotlinx.coroutines.CancellationException) {
                            // serviceScope estas nuligata je detruo — la nuligo ne estas eraro
                            throw e
                        } catch (e: Exception) {
                            logw("LudadoServo", "Malsukcesis voki traktiFinonPublika", e)
                        }
                    }
                }
            }
        })

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(kreiMainActivityPendingIntent())
            .setMediaButtonPreferences(listOf(kreuVenontanButonon()))
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                ): MediaSession.ConnectionResult {
                    val sessionCommands = SessionCommands.Builder()
                        .addSessionCommands(MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.commands)
                        .add(CMD_VENONTA)
                        .build()
                    return MediaSession.ConnectionResult.accept(
                        sessionCommands,
                        MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS,
                    )
                }

                override fun onCustomCommand(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    command: SessionCommand,
                    extras: Bundle,
                ): ListenableFuture<SessionResult> {
                    when (command.customAction) {
                        "venonta" -> {
                            logi("LudadoServo", "Sciigo: venonta klakita")
                            serviceScope.launch {
                                runCatching { SciigoKontroloj.onVenonta?.invoke() }
                                    .onFailure { logw("LudadoServo", "Venonta malsukcesis", it) }
                            }
                            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                        }
                    }
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
                }
            })
            .build()

        // Evitas ke la servo haltiĝu kiam startForeground() malsukcesas pro
        // Android 12+ restriktoj pri fona lanĉo de malfona servo.
        // Tio okazas kiam aŭtoludo lanĉas novan elsendon dum la apo estas en fono.
        setListener(object : MediaSessionService.Listener {
            override fun onForegroundServiceStartNotAllowedException() {
                logw("LudadoServo", "startForeground() malsukcesis (fono) — daŭrigas sen sciigo")
            }
        })
    }

    private fun kreuVenontanButonon(): CommandButton =
        CommandButton.Builder(CommandButton.ICON_NEXT)
            .setSessionCommand(CMD_VENONTA)
            .setDisplayName("Venonta")
            .build()

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    /**
     * FAROTA K1 (Sentry KMP-1V): starto per `startForegroundService` devigas la servon
     * voki `startForeground()` ene de la sistema limtempo. En la apo tion faras Media3 —
     * sed nur kiam ludado aktiviĝas. Servo restartigita post procezo-morto (aŭ alia
     * ekstera starto, ekz. mediabutono) havas malplenan ludilon: neniam ekbufferas
     * ludado, do neniu `startForeground()` okazus kaj Android kraŝigus la apon per
     * fatala RemoteServiceException (unufoje observita sur Android 11/MIUI, dist 249).
     * Post mallonga prokrasto ni mem plenumas la kontrakton per provizora sciigo kaj
     * haltigas la servon — ludilo kun media-eroj restas sub la mastrumado de Media3
     * (la kontrakto jam estis plenumita kiam ludado ekbuferis: ludado, paŭzo kaj eĉ
     * fonto-eraro kun sciigo).
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val rezulto = super.onStartCommand(intent, flags, startId)
        defenduMalfonanKontrakton()
        return rezulto
    }

    private fun defenduMalfonanKontrakton() {
        defendTasko?.cancel()
        defendTasko = serviceScope.launch {
            delay(DEFENDA_TEMPO_MS)
            val player = mediaSession?.player
            if (player != null && player.mediaItemCount > 0) {
                logd("LudadoServo", "Defenda kontrolo: ludilo havas enhavon — Media3 mastrumas")
                return@launch
            }
            try {
                val nm = getSystemService(NotificationManager::class.java)
                nm.createNotificationChannel(
                    NotificationChannel(
                        KANALO_DEFENDA,
                        "Ludado (provizora)",
                        NotificationManager.IMPORTANCE_MIN,
                    )
                )
                val sciigo = Notification.Builder(this@EsperantoLudadoServo, KANALO_DEFENDA)
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .setContentTitle("EsperantoRadio")
                    .build()
                ServiceCompat.startForeground(
                    this@EsperantoLudadoServo,
                    DEFENDA_SCIIG_ID,
                    sciigo,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
                )
                logi("LudadoServo", "Defenda startForeground() plenumita — neniu ludado komenciĝis")
            } catch (e: Exception) {
                logw("LudadoServo", "Defenda startForeground() malsukcesis", e)
            } finally {
                // Forigu la provizoran sciigon kaj finu la startitan parton de la servo;
                // binditaj regiloj (ekz. la malfermita apo) tenas la servon vivanta, kiom necese.
                stopForeground(Service.STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    /**
     * Kreas PendingIntent kiu malfermas MainActivity kiam la uzanto klakas la sciigon.
     */
    private fun kreiMainActivityPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

        return PendingIntent.getActivity(this, 0, intent, flags)
    }

    /**
     * Kiam la uzanto forviŝas la apon el la lastatempa listo:
     - se ne ludas, haltu la servon
     - se ludas, daŭrigi (fona ludado)
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            logd("LudadoServo", "Tasko forigita kaj ne ludas — haltigas servon")
            stopSelf()
        } else {
            logi("LudadoServo", "Tasko forigita sed ludas — daŭrigas en fono")
        }
    }

    override fun onDestroy() {
        logi("LudadoServo", "Servo detruata")
        defendTasko?.cancel()
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
