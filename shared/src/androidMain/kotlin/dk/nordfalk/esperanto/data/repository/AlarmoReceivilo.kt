package dk.nordfalk.esperanto.data.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import dk.nordfalk.esperanto.AppStato
import dk.nordfalk.esperanto.data.config.IntentSxlosilo
import dk.nordfalk.esperanto.data.config.appContext
import dk.nordfalk.esperanto.data.config.kreuSettings
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.logw

/**
 * BroadcastReceiver kiu ricevas la alarmon kiam ĝi ekigas.
 *
 * Ĝi akiras WakeLock, lanĉas la ĉefaktivon kaj metas la kanalo-slugon kiel
 * ekstran intencon por ke la apo povu komenci ludi la ĝustan kanalon.
 */
class AlarmoReceivilo : BroadcastReceiver() {

    companion object {
        private const val WAKELOCK_TAG = "EsperantoRadio::Alarmo"
        private const val WAKELOCK_TIMEOUT = 10 * 60 * 1000L // 10 minutoj
        private const val SCIIGO_KANALO = "alarmoj"

        /**
         * WakeLock akirita de la ricevilo (FAROTA G28): la ricevilo antaŭe liberigis ĝin
         * tuj post startActivity, do la CPU povis endormiĝi dum la asinkrona
         * "trovi elsendon kaj ludi"-fazo en MainActivity. Nun laproprieco transdoniĝas —
         * MainActivity liberigas ĝin en finally; la tempolimo estas la sekureca reto
         * se la Activity neniam lanĉiĝas.
         */
        @Volatile private var alarmoWakeLock: PowerManager.WakeLock? = null

        /** Vokata de MainActivity kiam la alarmo estas plene traktita. */
        fun liberiguAlarmoWakeLock() {
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

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        // FAROTA G28: la WakeLock transdoniĝas al MainActivity (liberiguAlarmoWakeLock) —
        // ĝi devas teni la CPU veka dum la asinkrona "trovi elsendon kaj ludi"-fazo
        alarmoWakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            WAKELOCK_TAG
        ).apply { acquire(WAKELOCK_TIMEOUT) }

        // Unufoja → malaktivigu; ripetanta → skedu la sekvan okazon. PLI ANTAŬE ol lanĉi la
        // aktivon: se la alarmo lanĉas freŝan procezon, la skribo al Settings devas okazi antaŭ
        // ol la startanta AppStato legas la liston (aliaflanke ĝi povus persisti ŝtalan liston).
        // Se la apo vivas, uzu ĝian deponejon (por ke la UI tuj vidu la ŝanĝon kaj ne poste
        // superskribu ĝin per malnova listo); alie kreu provizoran deponejon rekte sur Settings.
        try {
            appContext = context.applicationContext
            val deponejo = AppStato.alarmoDeponejo
                ?: PersistantaAlarmoDeponejo(kreuSettings(), skedilo = AlarmoSkedilo())
            deponejo.ekigis(alarmoId)
        } catch (e: Exception) {
            logw("AlarmoReceivilo", "Eraro ĝisdatigante alarmon $alarmoId post ekigo", e)
        }

        try {
            // Lanĉu la ĉefaktivon por ke la uzanto vidu la apot
            val launchIntent = Intent().apply {
                setClassName(context, "dk.nordfalk.esperanto.android.MainActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                action = "dk.nordfalk.esperanto.ALARMO_EKIGAS"
                putExtra("alarmo_kanal_slug", kanaloSlug)
                putExtra("alarmo_etikedo", etikedo)
                // FAROTA G3: la eksportita Activity kontrolas la nonce-on — sen ĝi
                // iu ajn apliko povus forĝi tiun ĉi intencon (ludi ajnan fluon, laŭteco-boost)
                IntentSxlosilo.aldonuNomon(this)
            }
            context.startActivity(launchIntent)
            logi("AlarmoReceivilo", "Lanĉis MainActivity kun kanalo=$kanaloSlug")
        } catch (e: Exception) {
            logw("AlarmoReceivilo", "Eraro lanĉante aktivon — provas full-screen-intent-sciigon", e)
            // FAROTA G28: startActivity el fona ricevilo estas blokata de la
            // BAL-limigoj (Android 10+, precipe ĉe vekiĝo kun elŝaltita ekrano) —
            // la alarmo estus tute SILENTA. Full-screen-intent-sciigo estas
            // permesata el fono kaj vekas la ekranon.
            try {
                montruFullEkranaSciigon(context, kanaloSlug, etikedo)
            } catch (e2: Exception) {
                logw("AlarmoReceivilo", "Full-screen-intent ankaŭ malsukcesis — ludas ringtonon rekte", e2)
                luduFallbackRingtonon(context)
            }
        }
        // NENIU release() ĉi tie (FAROTA G28): la WakeLock apartenas nun al MainActivity,
        // kiu liberigas ĝin kiam la alarmo estas traktita; la tempolimo estas la reto.
    }

    /**
     * Altprioritata sciigo kun full-screen-intento — permesata el fono (FAROTA G28).
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
            setClassName(context, "dk.nordfalk.esperanto.android.MainActivity")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            action = "dk.nordfalk.esperanto.ALARMO_EKIGAS"
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
     * Fallback: ludas la sisteman alarm-sonon rekte en la ricevilo (FAROTA G28) —
     * kiam eĉ la full-screen-intento ne povas lanĉi la aktivon, la alarmo
     * almenaŭ SONU (antaŭe: tute silenta alarmo).
     */
    private fun luduFallbackRingtonon(context: Context) {
        try {
            val uri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)
            if (uri != null) {
                android.media.RingtoneManager.getRingtone(context, uri)?.play()
                logi("AlarmoReceivilo", "Ludas fallback-ringtonon rekte en la ricevilo: $uri")
            }
        } catch (e: Exception) {
            logw("AlarmoReceivilo", "Eraro dum fallback-ringtono", e)
        }
    }
}
