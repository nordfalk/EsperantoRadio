package dk.nordfalk.esperanto.android

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dk.nordfalk.esperanto.domain.player.SciigoKontroloj
import dk.nordfalk.esperanto.logd
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.logw
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
    }

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
     * Kreas PendingIntent kiu malfermas MainActivity kiam la uzanto klakas la sciigon.
     */
    private fun kreiMainActivityPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        else
            PendingIntent.FLAG_UPDATE_CURRENT

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
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
