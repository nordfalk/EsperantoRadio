package dk.nordfalk.esperanto.data.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import dk.nordfalk.esperanto.AppStato
import dk.nordfalk.esperanto.data.config.IntentSxlosilo
import dk.nordfalk.esperanto.data.config.KanalAgordoLeganto
import dk.nordfalk.esperanto.data.config.alKanalo
import dk.nordfalk.esperanto.data.config.appContext
import dk.nordfalk.esperanto.data.config.kreuSettings
import dk.nordfalk.esperanto.data.config.leguBundledKanalkonfiguron
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.model.LudantoStato
import dk.nordfalk.esperanto.domain.player.ExoPlayerLudiloRegilo
import dk.nordfalk.esperanto.loge
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.logw
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver kiu ricevas la alarmon kiam ĝi ekigas — kaj MEM komencas la
 * ludadon, sendepende de iu ajn Activity.
 *
 * KIAL NE "lanĉu MainActivity kaj lasu ĝin ludi": startActivity el fona ricevilo
 * estas blokata de la Background-Activity-Launch-limigoj (Android 10+) kiam la apo
 * ne estas videbla — ekzakte ĉe alarmo kun malŝaltita ekrano. La Activity startas
 * en la fono kaj ĝia kodo ne plenumiĝas ĝis la uzanto vekas la telefonon.
 * La ricevilo ne havas tian limigon: ĝi povas ludi tuj (goAsync + WakeLock).
 *
 * Videbleco por la uzanto certigas la full-screen-intent-sciigo (ĉiam montrata):
 * ĝi rajtas veki la ekranon el fono (CATEGORY_ALARM + USE_FULL_SCREEN_INTENT).
 */
class AlarmoReceivilo : BroadcastReceiver() {

    companion object {
        private const val WAKELOCK_TAG = "EsperantoRadio::Alarmo"
        private const val WAKELOCK_TIMEOUT = 10 * 60 * 1000L // 10 minutoj
        private const val SCIIGO_KANALO = "alarmoj"
        const val AGO_ALARMO_EKIGAS = "dk.nordfalk.esperanto.ALARMO_EKIGAS"

        /**
         * WakeLock akirita de la ricevilo por la tuta asinkrona ludado-fazo
         * (trovi elsendon → ludi → 10s erar-kontrolo). Liberigata de la
         * korutino de la ricevilo mem en finally; la tempolimo estas la
         * sekureca reto se la korutino neniam finiĝas.
         */
        @Volatile private var alarmoWakeLock: PowerManager.WakeLock? = null

        private fun liberiguAlarmoWakeLock() {
            try {
                alarmoWakeLock?.let { if (it.isHeld) it.release() }
            } catch (e: Exception) {
                logw("AlarmoReceivilo", "Ne eblis liberigi la alarmo-WakeLock", e)
            }
            alarmoWakeLock = null
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alarmoId = intent.getIntExtra("alarmo_id", -1)
        val kanaloSlug = intent.getStringExtra("kanal_slug")
        val etikedo = intent.getStringExtra("etikedo")

        logi("AlarmoReceivilo", "Alarmo ekigis! id=$alarmoId kanalo=$kanaloSlug etikedo=$etikedo")

        alarmoWakeLock = (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG)
            .apply { acquire(WAKELOCK_TIMEOUT) }

        // Unufoja → malaktivigu; ripetanta → skedu la sekvan okazon. PLI ANTAŬE ol ludi:
        // se la alarmo lanĉas freŝan procezon, la skribo al Settings devas okazi antaŭ
        // ol la startanta AppStato legas la liston (aliaflanke ĝi povus persisti ŝtalan liston).
        try {
            appContext = context.applicationContext
            val deponejo = AppStato.alarmoDeponejo
                ?: PersistantaAlarmoDeponejo(kreuSettings(), skedilo = AlarmoSkedilo())
            deponejo.ekigis(alarmoId)
        } catch (e: Exception) {
            logw("AlarmoReceivilo", "Eraro ĝisdatigante alarmon $alarmoId post ekigo", e)
        }

        // Full-screen-intent-sciigo ĈIAM (ne nur kiel eraro-fallback): ĝi estas la
        // sola vojo vidi la apot el fono kiam la ekrano estas ŝaltita/malŝaltita.
        try {
            montruFullEkranaSciigon(context, kanaloSlug, etikedo)
        } catch (e: Exception) {
            logw("AlarmoReceivilo", "Full-screen-intent malsukcesis", e)
        }

        // Best-effort: malfermu la UI rekte — funkcias nur se la apo estas videbla
        // (alinie la sistemo silente blokas ĝin; la sciigo supre kovras tiun kazon).
        try {
            val launchIntent = Intent().apply {
                setClassName(context, Agadoj.CHEFA_AGADO)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                action = AGO_ALARMO_EKIGAS
                putExtra("alarmo_kanal_slug", kanaloSlug)
                putExtra("alarmo_etikedo", etikedo)
                // La eksportita Activity kontrolas la nonce-on (PR #89)
                IntentSxlosilo.aldonuNomon(this)
            }
            context.startActivity(launchIntent)
            logi("AlarmoReceivilo", "Lanĉis MainActivity (best-effort) kun kanalo=$kanaloSlug")
        } catch (e: Exception) {
            logw("AlarmoReceivilo", "Ne lanĉis aktivon (_BAL-blokita aŭ eraro) — la sciigo kovras", e)
        }

        // La ludado mem — en korutino de la ricevilo, NE en MainActivity.
        // goAsync() tenas la Receivilon vivanta dum la nesinkrona fazo.
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
            try {
                luduAlarmon(context, kanaloSlug)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                loge("AlarmoReceivilo", "Eraro dum alarmo-ludado", e)
                luduFallbackRingtonon(context)
            } finally {
                liberiguAlarmoWakeLock()
                pendingResult.finish()
            }
        }
    }

    /**
     * La kerno de la alarmo: trovas la kanalon/elsendon kaj komencas ludi per
     * LudvicoRegilo (kun elŝuto-prefero, pozicio-resumigo kaj reprovo-logiko).
     */
    private suspend fun luduAlarmon(context: Context, kanaloSlug: String?) {
        if (kanaloSlug.isNullOrBlank()) {
            logw("AlarmoReceivilo", "Neniu kanalo-slug en alarm-intento")
            luduFallbackRingtonon(context)
            return
        }

        certiguMinimumanVolumenon(context)

        // Se la alarmo lanĉis freŝan procezon, AppStato ne estas inicialigita —
        // faru nun (idempotent). La ludilo estas procez-nivela (ExoPlayerLudiloRegilo.akiru).
        try {
            AppStato.inicialigu(
                ludilo = ExoPlayerLudiloRegilo.akiru(context.applicationContext),
                settings = kreuSettings(),
                agordojDeponejo = AgordojDeponejoImpl(kreuSettings()),
            )
        } catch (e: Exception) {
            logw("AlarmoReceivilo", "Ne povis inicialigi AppStaton — provas daŭrigi", e)
        }

        val agordo = KanalAgordoLeganto().legu(leguBundledKanalkonfiguron())
        val kanalo: Kanalo = agordo.kanaloj.find { it.kodo == kanaloSlug }?.alKanalo() ?: run {
            logw("AlarmoReceivilo", "Kanalo ne trovita: $kanaloSlug")
            luduFallbackRingtonon(context)
            return
        }

        val regilo = AppStato.ludvicoRegilo ?: run {
            logw("AlarmoReceivilo", "LudvicoRegilo ne havebla")
            luduFallbackRingtonon(context)
            return
        }
        val ludilo = ExoPlayerLudiloRegilo.akiru(context.applicationContext)

        logi("AlarmoReceivilo", "Komencas ludi: ${kanalo.nomo}")
        if (kanalo.rektaElsendaSonoUrl != null) {
            regilo.ludiRektan(kanalo)
        } else {
            val elsendo = AppStato.trovuAlarmElsendon(kanalo)
            if (elsendo == null) {
                logw("AlarmoReceivilo", "Neniu ludebla elsendo por $kanaloSlug — ludas fallback ringtonon")
                luduFallbackRingtonon(context)
                return
            }
            logi("AlarmoReceivilo", "Alarmo ludas podkaston: ${elsendo.id} (${elsendo.dato})")
            // ludiElsendon preferas elŝutitan dosieron kaj resumas de savita pozicio
            regilo.ludiElsendon(elsendo)
        }

        // Post 10 sekundoj: se la stato estas Eraro, ludu la sisteman alarm-sonon —
        // alarmo DEVAS soni ankaŭ kiam la fluo ne disponeblas.
        delay(10_000)
        val stato = ludilo.stato.value
        if (stato.stato is LudantoStato.Eraro) {
            logw("AlarmoReceivilo", "Ludado malsukcesis post 10s (${stato.stato}) — ludas fallback ringtonon")
            luduFallbackRingtonon(context)
        }
    }

    /**
     * Volumo-boost: certigu minimuman volumenon (2/5 de maksimumo) — la uzanto
     * atendis vekiĝon, do silenta ludado ne estas akceptebla.
     */
    private fun certiguMinimumanVolumenon(context: Context) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            val maks = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
            val min = (maks * 2 / 5).coerceAtLeast(1)
            if (audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) < min) {
                audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, min, 0)
                logi("AlarmoReceivilo", "Volumo-boost: ${audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)}/$maks")
            }
        } catch (e: Exception) {
            logw("AlarmoReceivilo", "Volumo-boost malsukcesis", e)
        }
    }

    /**
     * Altprioritata sciigo kun full-screen-intento — permesata el fono (PR #94).
     * Vekas la ekranon kiam ĝi estas malŝaltita; alikaze heads-up-sciigo.
     */
    private fun montruFullEkranaSciigon(context: Context, kanaloSlug: String?, etikedo: String?) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                android.app.NotificationChannel(
                    SCIIGO_KANALO, "Vekhorloĝo", android.app.NotificationManager.IMPORTANCE_HIGH
                ).apply { description = "Vekigas la aplikaĵon kiam alarmo ekas" }
            )
        }
        val launchIntent = Intent().apply {
            setClassName(context, Agadoj.CHEFA_AGADO)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            action = AGO_ALARMO_EKIGAS
            putExtra("alarmo_kanal_slug", kanaloSlug)
            putExtra("alarmo_etikedo", etikedo)
            IntentSxlosilo.aldonuNomon(this)
        }
        val pi = android.app.PendingIntent.getActivity(
            context, kanaloSlug.hashCode(), launchIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )
        val sciigo = androidx.core.app.NotificationCompat.Builder(context, SCIIGO_KANALO)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(etikedo ?: "Alarmo")
            .setContentText("Vekhorloĝo de EsperantoRadio")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX)
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pi, true)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        nm.notify(kanaloSlug.hashCode(), sciigo)
        logi("AlarmoReceivilo", "Montris full-screen-intent-sciigon por kanalo=$kanaloSlug")
    }

    /**
     * Fallback: ludas la sisteman alarm-sonon kaj vibras rekte en la ricevilo (PR #94) —
     * alarmo devas SONI eĉ kiam la fluo/reto tute ne disponeblas.
     */
    private fun luduFallbackRingtonon(context: Context) {
        try {
            val uri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)
                ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            if (uri != null) {
                android.media.RingtoneManager.getRingtone(context, uri)?.let {
                    it.audioAttributes = android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .build()
                    it.play()
                }
                logi("AlarmoReceivilo", "Ludas fallback-ringtonon: $uri")
            }

            // Vibradu 4 sekundojn
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator?.hasVibrator() == true) {
                vibrator.vibrate(VibrationEffect.createOneShot(4000, VibrationEffect.DEFAULT_AMPLITUDE))
                logi("AlarmoReceivilo", "Vibras 4s")
            }
        } catch (e: Exception) {
            logw("AlarmoReceivilo", "Eraro dum fallback-ringtono", e)
        }
    }
}
