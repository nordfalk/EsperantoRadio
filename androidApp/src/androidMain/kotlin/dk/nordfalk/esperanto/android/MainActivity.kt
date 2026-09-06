package dk.nordfalk.esperanto.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dk.nordfalk.esperanto.EsperantoRadioApp
import dk.nordfalk.esperanto.data.config.appContext
import dk.nordfalk.esperanto.data.config.KanalAgordoLeganto
import dk.nordfalk.esperanto.data.config.leguBundledKanalkonfiguron
import dk.nordfalk.esperanto.data.config.alKanal
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.logw
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var ludilo: ExoPlayerLudiloRegilo
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContext = applicationContext
        petiSciigPermeson()
        ludilo = ExoPlayerLudiloRegilo(this)
        setContent {
            EsperantoRadioApp(ludilo = ludilo)
        }
        traktuAlarmIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        traktuAlarmIntent(intent)
    }

    private fun traktuAlarmIntent(intent: Intent?) {
        if (intent?.action != "dk.nordfalk.esperanto.ALARMO_EKIGAS") return

        val kanalSlug = intent.getStringExtra("alarmo_kanal_slug")
        val etikedo = intent.getStringExtra("alarmo_etikedo")
        logi("MainActivity", "Alarmo ricevita: kanal=$kanalSlug etikedo=$etikedo")

        if (kanalSlug.isNullOrBlank()) {
            logw("MainActivity", "Neniu kanal-slug en alarm-intento")
            luduFallbackRingtonon()
            return
        }

        // Volumo-boost: certigu minimuman volumenon (2/5 de maksimumo)
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val maksVolumeno = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val minVolumeno = (maksVolumeno * 2 / 5).coerceAtLeast(1)
        if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) < minVolumeno) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, minVolumeno, 0)
            logi("MainActivity", "Volumo-boost: ${audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)}/$maksVolumeno")
        }

        scope.launch {
            try {
                // Shargu la kanalaron por trovi la gxustan kanalon
                val agordo = KanalAgordoLeganto().legu(leguBundledKanalkonfiguron())
                val kanal = agordo.kanaloj.find { it.kodo == kanalSlug }?.alKanal()

                if (kanal == null) {
                    logw("MainActivity", "Kanal ne trovita: $kanalSlug")
                    luduFallbackRingtonon()
                    return@launch
                }

                logi("MainActivity", "Komencas ludi: ${kanal.nomo}")

                // Se rekta kanalo: ludi rekte
                // Se podkasto: bezonas RSS-fluon — tro komplika cxi tie, ludi rekte se eblas
                val fonto = if (kanal.rektaElsendaSonoUrl != null) {
                    Sonfonto.RektaKanalo(kanal)
                } else {
                    // Por podkastoj: bezonas elsendon, sed ni ne sxargxis RSS fluon.
                    // Fallback al ringtono por nun.
                    logw("MainActivity", "Kanal $kanalSlug ne estas rekta — ne eblas auxtomate ludi podkaston")
                    luduFallbackRingtonon()
                    return@launch
                }

                ludilo.fiksiFonton(fonto)
                ludilo.ludi()

                // Post 10 sekundoj: se la stato estas Eraro, ludu fallback ringtonon
                delay(10_000)
                val stato = ludilo.stato.value
                if (stato.stato is dk.nordfalk.esperanto.domain.model.LudantoStato.Eraro) {
                    logw("MainActivity", "Ludado malsukcesis post 10s — ludas fallback ringtonon")
                    luduFallbackRingtonon()
                }
            } catch (e: Exception) {
                logw("MainActivity", "Eraro dum alarmo-ludado", e)
                luduFallbackRingtonon()
            }
        }
    }

    /**
     * Ludas la sisteman alarm-sonon kaj vibras kiel fallback kiam la reto malsukcesas.
     */
    private fun luduFallbackRingtonon() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE
            )
            if (uri != null) {
                val ringtone = RingtoneManager.getRingtone(applicationContext, uri)
                ringtone?.play()
                logi("MainActivity", "Ludas fallback ringtonon: $uri")
            }

            // Vibradu 4 sekundojn
            val vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(4000, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(4000)
                }
                logi("MainActivity", "Vibras 4s")
            }
        } catch (e: Exception) {
            logw("MainActivity", "Eraro dum fallback ringtono", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // NUR malkonektas la MediaController — la servo pluvivas kaj daŭre ludas en la fono
        ludilo.release()
    }

    /**
     * Petas sciig-permeson por Android 13+ (API 33+).
     * Necesa por la mediasciigo dum fona ludado.
     */
    private fun petiSciigPermeson() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }
    }
}
