package dk.nordfalk.esperanto.data.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import dk.nordfalk.esperanto.AppStato
import dk.nordfalk.esperanto.data.config.IntentŜlosilo
import dk.nordfalk.esperanto.data.config.KanalAgordoLeganto
import dk.nordfalk.esperanto.data.config.alKanalo
import dk.nordfalk.esperanto.data.config.appContext
import dk.nordfalk.esperanto.data.config.kreuSettings
import dk.nordfalk.esperanto.data.config.leguPakititanKanalkonfiguron
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.model.LudantoStato
import dk.nordfalk.esperanto.domain.player.ExoPlayerLudiloRegilo
import dk.nordfalk.esperanto.logGrava
import dk.nordfalk.esperanto.loge
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.logw
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
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
 * Videbleco por la uzanto certigas altprioritatan sciigon (CATEGORY_ALARM + PRIORITY_MAX).
 * Sen USE_FULL_SCREEN_INTENT la ekrano ne vekiĝas aŭtomate — la uzanto aŭdas la
 * alarmon kaj vidas la sciigon kiam li vekas la telefonon.
 */
class AlarmoReceivilo : BroadcastReceiver() {

    companion object {
        private const val WAKELOCK_TAG = "EsperantoRadio::Alarmo"
        private const val WAKELOCK_TIMEOUT = 10 * 60 * 1000L // 10 minutoj
        private const val SCIIGO_KANALO = "alarmoj"
        /** Fiksa ID de la alarma sciigo — tiel oni povas nuligi ĝin (la sciigo estas daŭra). */
        const val SCIIGO_ID = 4711
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

        /**
         * Nuligas la alarman sciigon. La sciigo estas daŭra dum la alarmo
         * sonas — daŭraj sciigoj ne estas forviŝeblaj per svingo, do io devas esti
         * vokata kiam la ludado haltiĝas aŭ finiĝas (vidu la gardon en onReceive).
         * Krome la sciigo mem estas nuligata per setAutoCancel kiam la uzanto tuŝas ĝin.
         */
        fun nuliguSciigon(context: Context) {
            try {
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager)
                    .cancel(SCIIGO_ID)
            } catch (e: Exception) {
                logw("AlarmoReceivilo", "Ne eblis nuligi la alarman sciigon", e)
            }
        }

        /**
         * Tenilo de la rezerva vokosono: Ringtone.play() ripetiĝas ĝis stop() —
         * sen tenilo nenio povus haltigi ĝin (la sciigo diras "tuŝu por malŝalti",
         * sed antaŭe neniu vojo haltigis la sonon).
         */
        @Volatile private var rezervaVokosono: android.media.Ringtone? = null

        /**
         * Maksimuma daŭro de la rezerva vokosono — poste ĝi mem haltas kaj la
         * daŭra alarma sciigo estas nuligata (sen tio la alarmo sonus por ĉiam).
         * Test-ŝanĝebla.
         */
        @Volatile var vokosonoMaksDaŭroMs: Long = 5 * 60 * 1000L

        /** Ĉu la rezerva vokosono nun sonas (por testoj). */
        val rezervaVokosonoAktiva: Boolean get() = rezervaVokosono?.isPlaying == true

        /**
         * Haltigas la rezervan vokosonon (idempotenta — sekura eĉ se neniu sonas).
         * Vokata per haltuAlarmVokosonon() (vd. LudiloRegilo.kt) kiam la uzanto
         * haltigas la ludadon, kaj de la mem-halto-tempujo en luduRezervanVokosonon.
         */
        fun haltuRezervanVokosonon() {
            val vokosono = rezervaVokosono
            if (vokosono != null) {
                rezervaVokosono = null
                try {
                    if (vokosono.isPlaying) {
                        vokosono.stop()
                        logi("AlarmoReceivilo", "Haltigis rezervan vokosonon")
                    }
                } catch (e: Exception) {
                    logw("AlarmoReceivilo", "Ne eblis haltigi rezervan vokosonon", e)
                }
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alarmoId = intent.getIntExtra("alarmo_id", -1)
        val kanaloSlug = intent.getStringExtra("kanal_slug")
        val etikedo = intent.getStringExtra("etikedo")

        logGrava("AlarmoReceivilo", "Vekiĝo: alarmo ekigis! id=$alarmoId kanalo=$kanaloSlug etikedo=$etikedo")

        alarmoWakeLock = (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG)
            .apply { acquire(WAKELOCK_TIMEOUT) }

        // Unufoja → malaktivigu; ripetanta → skedu la sekvan okazon. PLI ANTAŬE ol ludi:
        // se la alarmo lanĉas freŝan procezon, la skribo al Settings devas okazi antaŭ
        // ol la startanta AppStato legas la liston (aliaflanke ĝi povus persisti ŝtalan liston).
        try {
            appContext = context.applicationContext
            val deponejo = AppStato.alarmoDeponejo
                ?: PersistaAlarmoDeponejo(kreuSettings(), skedilo = AlarmoSkedilo())
            deponejo.ekigis(alarmoId)
        } catch (e: Exception) {
            logw("AlarmoReceivilo", "Eraro ĝisdatigante alarmon $alarmoId post ekigo", e)
        }

        // Alt-prioritate sciigo ĈIAM: ĝi certigas ke la uzanto vidas la vekiĝon
        // (sen USE_FULL_SCREEN_INTENT la ekrano ne vekiĝas, sed la sciigo aperas
        // kiel heads-up kiam la uzanto ŝaltas la ekranon).
        try {
            montruAltprioritatanSciigon(context, kanaloSlug, etikedo)
        } catch (e: Exception) {
            logw("AlarmoReceivilo", "Altprioritata sciigo malsukcesa", e)
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
                IntentŜlosilo.aldonuNomon(this)
            }
            context.startActivity(launchIntent)
            logi("AlarmoReceivilo", "Lanĉis MainActivity (best-effort) kun kanalo=$kanaloSlug")
        } catch (e: Exception) {
            logw("AlarmoReceivilo", "Ne lanĉis aktivon (BAL-blokita aŭ eraro) — la sciigo kovras", e)
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
                luduRezervanVokosonon(context)
            } finally {
                liberiguAlarmoWakeLock()
                pendingResult.finish()
            }
        }

        // Gardo de la daŭra alarma sciigo: nuligu ĝin kiam la alarm-ludado haltiĝas
        // aŭ finiĝas (post kiam ĝi iam komenciĝis — Konektas/Ludas/Eraro). Sen tio
        // la daŭra sciigo restus en la ombrumo por ĉiam, ĉar daŭraj sciigoj ne
        // estas forviŝeblaj per svingo.
        CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
            val ludilo = ExoPlayerLudiloRegilo.akiru(context.applicationContext)
            var iamKomencis = false
            ludilo.stato.collect { info ->
                when (info.stato) {
                    is LudantoStato.Haltita, is LudantoStato.Finita -> {
                        if (iamKomencis) {
                            nuliguSciigon(context.applicationContext)
                            cancel()
                        }
                    }
                    else -> iamKomencis = true // Konektas, Ludas, Eraro — la alarm-ludado estis iniciatita
                }
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
            luduRezervanVokosonon(context)
            return
        }

        certiguMinimumanVolumenon(context)

        // Se la alarmo lanĉis freŝan procezon, AppStato ne estas inicialigita —
        // faru nun (idempotenta). La ludilo estas procez-nivela (ExoPlayerLudiloRegilo.akiru).
        try {
            AppStato.inicialigu(
                ludilo = ExoPlayerLudiloRegilo.akiru(context.applicationContext),
                settings = kreuSettings(),
                agordojDeponejo = AgordojDeponejoImpl(kreuSettings()),
            )
        } catch (e: Exception) {
            logw("AlarmoReceivilo", "Ne povis inicialigi AppStaton — provas daŭrigi", e)
        }

        val agordo = KanalAgordoLeganto().legu(leguPakititanKanalkonfiguron())
        val kanalo: Kanalo = agordo.kanaloj.find { it.kodo == kanaloSlug }?.alKanalo() ?: run {
            logw("AlarmoReceivilo", "Kanalo ne trovita: $kanaloSlug")
            luduRezervanVokosonon(context)
            return
        }

        val regilo = AppStato.ludvicoRegilo ?: run {
            logw("AlarmoReceivilo", "LudvicoRegilo ne havebla")
            luduRezervanVokosonon(context)
            return
        }
        val ludilo = ExoPlayerLudiloRegilo.akiru(context.applicationContext)

        logi("AlarmoReceivilo", "Komencas ludi: ${kanalo.nomo}")
        if (kanalo.rektaElsendaSonoUrl != null) {
            regilo.ludiRektan(kanalo)
        } else {
            val elsendo = AppStato.trovuAlarmElsendon(kanalo)
            if (elsendo == null) {
                logw("AlarmoReceivilo", "Neniu ludebla elsendo por $kanaloSlug — ludas rezervan vokosonon")
                luduRezervanVokosonon(context)
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
            logw("AlarmoReceivilo", "Ludado malsukcesis post 10s (${stato.stato}) — ludas rezervan vokosonon")
            luduRezervanVokosonon(context)
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
     * Altprioritata sciigo (CATEGORY_ALARM + PRIORITY_MAX) — permesata el fono.
     * Sen USE_FULL_SCREEN_INTENT ĝi montriĝas kiel heads-up kiam la ekrano estas
     * ŝaltita; la alarmsono tamen ĉiam ludas (vidu luduAlarmon).
     * La sciigo estas daŭra dum la alarmo sonas — tial ĝi ne estu
     * forviŝebla per svingo; ĝi estas nuligata kiam la ludado haltiĝas aŭ finiĝas
     * (vidu la gardon en onReceive) aŭ kiam la uzanto tuŝas ĝin (setAutoCancel).
     */
    private fun montruAltprioritatanSciigon(context: Context, kanaloSlug: String?, etikedo: String?) {
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
            // De la sciigo: MainActivity haltigu la rezervan vokosonon — sed NE la
            // best-effort startActivity de la ricevilo mem (vd. MainActivity).
            putExtra("de_sciigo", true)
            IntentŜlosilo.aldonuNomon(this)
        }
        val pi = android.app.PendingIntent.getActivity(
            context, kanaloSlug.hashCode(), launchIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )
        val sciigo = androidx.core.app.NotificationCompat.Builder(context, SCIIGO_KANALO)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(etikedo ?: "Alarmo")
            .setContentText("Vekhorloĝo de EsperantoRadio — tuŝu por malŝalti")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX)
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setOngoing(true)
            .build()
        nm.notify(SCIIGO_ID, sciigo)
        logi("AlarmoReceivilo", "Montris altprioritatan sciigon por kanalo=$kanaloSlug")
    }

    /**
     * Rezerva kazo: ludas la sisteman alarm-sonon kaj vibras rekte en la ricevilo (PR #94) —
     * alarmo devas SONI eĉ kiam la fluo/reto tute ne disponeblas.
     *
     * La sono estas haltigebla per haltuRezervanVokosonon() (vokata de haltuAlarmVokosonon
     * kiam la uzanto haltigas la ludadon) kaj mem haltas post [vokosonoMaksDaŭroMs]
     * (Ringtone.play() alie ripetiĝus senfine — kaj la daŭra sciigo restus).
     */
    private fun luduRezervanVokosonon(context: Context) {
        try {
            val uri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)
                ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            if (uri != null) {
                // Unue haltigu eventualan antaŭan sonon — neniam duobla alarmo
                haltuRezervanVokosonon()
                android.media.RingtoneManager.getRingtone(context, uri)?.let {
                    it.audioAttributes = android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .build()
                    it.play()
                    // Konservu la tenilon por haltuRezervanVokosonon() kaj mem-haltu
                    // post vokosonoMaksDaŭroMs. La referenca komparo (===) certigas
                    // ke malnova tempumilo ne haltigu novan sonon.
                    rezervaVokosono = it
                    val mia = it
                    CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
                        delay(vokosonoMaksDaŭroMs)
                        if (rezervaVokosono === mia) {
                            logi("AlarmoReceivilo", "Rezerva vokosono mem haltigita post ${vokosonoMaksDaŭroMs} ms")
                            haltuRezervanVokosonon()
                            nuliguSciigon(context.applicationContext)
                        }
                    }
                }
                logi("AlarmoReceivilo", "Ludas rezervan vokosonon: $uri")
            }

            // Vibradu 4 sekundojn
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator?.hasVibrator() == true) {
                vibrator.vibrate(VibrationEffect.createOneShot(4000, VibrationEffect.DEFAULT_AMPLITUDE))
                logi("AlarmoReceivilo", "Vibras 4s")
            }
        } catch (e: Exception) {
            logw("AlarmoReceivilo", "Eraro dum rezerva vokosono", e)
        }
    }
}
