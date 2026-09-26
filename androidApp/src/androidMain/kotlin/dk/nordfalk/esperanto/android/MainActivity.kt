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
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dk.nordfalk.esperanto.EsperantoRadioApp
import dk.nordfalk.esperanto.initialiguSentry
import dk.nordfalk.esperanto.data.config.appContext
import dk.nordfalk.esperanto.ui.AktivecoPonto
import dk.nordfalk.esperanto.data.config.KanalAgordoLeganto
import dk.nordfalk.esperanto.data.config.leguBundledKanalkonfiguron
import dk.nordfalk.esperanto.data.config.alKanalo
import dk.nordfalk.esperanto.data.repository.NovajElsendojKontroloWorker
import dk.nordfalk.esperanto.AppStato
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.ElshutStato
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.player.elektuAlarmElsendon
import dk.nordfalk.esperanto.domain.model.Sonfonto
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.logw
import dk.nordfalk.esperanto.testuSentry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class MainActivity : ComponentActivity() {
    private lateinit var ludilo: ExoPlayerLudiloRegilo
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        initialiguSentry()
        appContext = applicationContext
        // Por la plenekrana filmo: ŝanĝi la orientiĝon bezonas Activity-referencon
        AktivecoPonto.aktiveco = this
        petiSciigPermeson()
        // Procez-nivela — la sama instanco post ĉiu rekreo de la Activity
        ludilo = ExoPlayerLudiloRegilo.akiru(this)
        setContent {
            EsperantoRadioApp(ludilo = ludilo)
        }
        // La sciig-skedo estas mastrumata de App.kt per ghisdatiguSciigSkedon()
        traktuIntenton(intent)
        // testuSentry()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        traktuIntenton(intent)
    }

    /**
     * Traktas alvenantajn intentojn de sciigoj aŭ alarmoj.
     */
    private fun traktuIntenton(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            "dk.nordfalk.esperanto.ALARMO_EKIGAS" -> traktuAlarmIntent(intent)
            NovajElsendojKontroloWorker.ACTION_MALFERMI_ELSENDON -> traktuMalfermiElsendon(intent)
        }
    }

    /**
     * Traktas la intenton kiam la uzanto klakas sur sciigo pri nova elsendo.
     * Rekonstruas la Elsendo-objekton, komencas ludi kaj navigas al la elsenda detalo.
     */
    private fun traktuMalfermiElsendon(intent: Intent) {
        val elsendoId = intent.getStringExtra(NovajElsendojKontroloWorker.EXTRA_ELSENDO_ID) ?: return
        val titolo = intent.getStringExtra(NovajElsendojKontroloWorker.EXTRA_ELSENDO_TITOLO) ?: ""
        val fluo = intent.getStringExtra(NovajElsendojKontroloWorker.EXTRA_ELSENDO_FLUO) ?: return
        val kanaloSlug = intent.getStringExtra(NovajElsendojKontroloWorker.EXTRA_KANALO_SLUG) ?: ""
        val kanaloNomo = intent.getStringExtra(NovajElsendojKontroloWorker.EXTRA_KANALO_NOMO)
        val bildoUrl = intent.getStringExtra(NovajElsendojKontroloWorker.EXTRA_BILDO_URL)
        val dato = intent.getStringExtra(NovajElsendojKontroloWorker.EXTRA_ELSENDO_DATO) ?: ""
        val priskribo = intent.getStringExtra(NovajElsendojKontroloWorker.EXTRA_ELSENDO_PRISKRIBO)

        logi("MainActivity", "Malfermi elsendon de sciigo: id=$elsendoId titolo=$titolo")

        val elsendo = Elsendo(
            id = elsendoId,
            kanaloSlug = kanaloSlug,
            kanaloNomo = kanaloNomo,
            titolo = titolo,
            priskribo = priskribo,
            fluo = fluo,
            bildoUrl = bildoUrl,
            dato = dato,
        )

        // Komencas ludi
        scope.launch {
            try {
                ludilo.fiksiFonton(Sonfonto.ElsendoFonto(elsendo))
                ludilo.ludi()
                logi("MainActivity", "Ludado komencita de sciigo: $titolo")
            } catch (e: Exception) {
                logw("MainActivity", "Eraro dum ludi elsendon de sciigo", e)
            }
        }

        // Signalu al la Compose-tavolo por navigi al la elsenda detalo
        dk.nordfalk.esperanto.data.repository.PendingElsendoNavigacio.setu(elsendo)
    }

    private fun traktuAlarmIntent(intent: Intent?) {
        if (intent?.action != "dk.nordfalk.esperanto.ALARMO_EKIGAS") return

        val kanaloSlug = intent.getStringExtra("alarmo_kanal_slug")
        val etikedo = intent.getStringExtra("alarmo_etikedo")
        logi("MainActivity", "Alarmo ricevita: kanalo=$kanaloSlug etikedo=$etikedo")

        if (kanaloSlug.isNullOrBlank()) {
            logw("MainActivity", "Neniu kanalo-slug en alarm-intento")
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
                val kanalo = agordo.kanaloj.find { it.kodo == kanaloSlug }?.alKanalo()

                if (kanalo == null) {
                    logw("MainActivity", "Kanalo ne trovita: $kanaloSlug")
                    luduFallbackRingtonon()
                    return@launch
                }

                logi("MainActivity", "Komencas ludi: ${kanalo.nomo}")

                if (kanalo.rektaElsendaSonoUrl != null) {
                    ludilo.fiksiFonton(Sonfonto.RektaKanalo(kanalo))
                    ludilo.ludi()
                } else {
                    // Podkasto: ludu la plej freŝan neaŭskultitan elsendon
                    val elsendo = trovuAlarmElsendon(kanalo)
                    val ludvicoRegilo = AppStato.ludvicoRegilo
                    if (elsendo == null || ludvicoRegilo == null) {
                        logw("MainActivity", "Neniu ludebla elsendo por $kanaloSlug — ludas fallback ringtonon")
                        luduFallbackRingtonon()
                        return@launch
                    }
                    logi("MainActivity", "Alarmo ludas podkaston: ${elsendo.id} (${elsendo.dato})")
                    // ludiElsendon preferas elŝutitan dosieron kaj resumas de savita pozicio
                    ludvicoRegilo.ludiElsendon(elsendo)
                }

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
     * Trovas la elsendon kiun alarmo ludu por podkasta kanalo. Provas laŭvice:
     * reton (freŝa RSS), diskkaŝmemoron, kaj elŝutitajn elsendojn — ĉar ĉe vekiĝo
     * la reto ofte ankoraŭ ne pretas.
     */
    private suspend fun trovuAlarmElsendon(kanalo: Kanalo): Elsendo? = withContext(Dispatchers.IO) {
        // AppStato estas inicialigita de la Compose-tavolo; atendu ĝin se la alarmo lanĉis la apon
        withTimeoutOrNull(5_000) { while (!AppStato.inicialigita()) delay(100) }
        val elsendoDeponejo = AppStato.elsendoDeponejo ?: run {
            logw("MainActivity", "AppStato ne inicialigita — ne eblas trovi elsendon por alarmo")
            return@withContext null
        }
        val elsendoj = elsendoDeponejo.sxargxiElsendojn(kanalo, fortoRefresigi = true)
            .ifEmpty { elsendoDeponejo.leguKashitajnElsendojn(kanalo) ?: emptyList() }
            .ifEmpty {
                AppStato.elshutDeponejo?.observiElshutojn()?.value?.values
                    ?.filter { it.elsendo.kanaloSlug == kanalo.slug && it.stato is ElshutStato.Preta }
                    ?.map { it.elsendo } ?: emptyList()
            }
        val ludatoj = AppStato.ludatojDeponejo?.observiLudatojn()?.value ?: emptyMap()
        elektuAlarmElsendon(elsendoj, ludatoj)
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
                vibrator.vibrate(VibrationEffect.createOneShot(4000, VibrationEffect.DEFAULT_AMPLITUDE))
                logi("MainActivity", "Vibras 4s")
            }
        } catch (e: Exception) {
            logw("MainActivity", "Eraro dum fallback ringtono", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // La ludilo (kaj ĝia MediaController) NE estas liberigita — ĝi estas procez-nivela kaj
        // uzata de AppStato.ludvicoRegilo kaj de la sekva Activity-instanco.
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
