package dk.nordfalk.esperanto.data.repository

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dk.nordfalk.esperanto.data.config.KanalAgordoLeganto
import dk.nordfalk.esperanto.data.config.alKanalo
import dk.nordfalk.esperanto.data.config.appContext
import dk.nordfalk.esperanto.data.config.kreuSettings
import dk.nordfalk.esperanto.data.config.leguBundledKanalkonfiguron
import dk.nordfalk.esperanto.data.parser.RssParsilo
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.loge
import dk.nordfalk.esperanto.logw
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.delay

/**
 * Fona Worker kiu periode kontrolas ŝatatajn kanalojn por novaj elsendoj.
 *
 * Skedita per WorkManager cxe 7:00 kaj 16:00 (±1h). Por cxiu ŝatata kanalo:
 * 1. Elŝutas la RSS-fluon
 * 2. Filtras novajn elsendojn (ne viditaj)
 * 3. Kreas sciigon por cxiu nova elsendo
 *
 * La sciigo havas du agojn:
 * - Klako sur la sciigo → malfermas la elsendon en la apo
 * - "Ludi" butono → komencas ludi la elsendon rekte
 */
class NovajElsendojKontroloWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    companion object {
        const val SCIIGO_KANALO_ID = "novaj_elsendoj"
        const val EXTRA_ELSENDO_ID = "elsendo_id"
        const val EXTRA_ELSENDO_TITOLO = "elsendo_titolo"
        const val EXTRA_ELSENDO_FLUO = "elsendo_fluo"
        const val EXTRA_ELSENDO_DATO = "elsendo_dato"
        const val EXTRA_ELSENDO_PRISKRIBO = "elsendo_priskribo"
        const val EXTRA_KANALO_SLUG = "kanalo_slug"
        const val EXTRA_KANALO_NOMO = "kanalo_nomo"
        const val EXTRA_BILDO_URL = "bildo_url"
        const val ACTION_LUDI_ELSENDON = "dk.nordfalk.esperanto.LUDI_ELSENDON"
        const val ACTION_MALFERMI_ELSENDON = "dk.nordfalk.esperanto.MALFERMI_ELSENDON"
        private const val VIDITAJ_KEY = "viditaj_elsendoj"
        private const val MAKS_VIDITAJ = 500
    }

    override suspend fun doWork(): Result {
        logi("NovajElsendoj", "=== Worker komencis ===")
        return try {
            val settings = kreuSettings()

            // Kontrolu ĉu sciigoj estas ŝaltitaj
            val sciigoj = settings.getBoolean("sciigoj", true)
            if (!sciigoj) {
                logi("NovajElsendoj", "Sciigoj malŝaltitaj en agordoj — finas sen kontroli")
                return Result.success()
            }

            // Kontrolu sciig-permeson (Android 13+)
            if (!sciigPermesoDonita()) {
                logw("NovajElsendoj", "Sciig-permeso (POST_NOTIFICATIONS) mankas — aŭtomate malŝaltas sciigojn")
                settings.putBoolean("sciigoj", false)
                return Result.success()
            }

            val plejStr = settings.getString("plejŝatataj_kanaloj", "")
            if (plejStr.isBlank()) {
                logi("NovajElsendoj", "Neniu ŝatata kanalo — finas")
                return Result.success()
            }
            val plejŝatataj = plejStr.split(",").toSet()
            logi("NovajElsendoj", "Kontrolas ${plejŝatataj.size} ŝatatajn kanalojn: $plejŝatataj")

            val kanaloj = KanalAgordoLeganto().legu(leguBundledKanalkonfiguron()).kanaloj
                .filter { it.kodo in plejŝatataj && it.elsendojRssUrl != null }
                .map { it.alKanalo() }
            if (kanaloj.isEmpty()) {
                logi("NovajElsendoj", "Neniu ŝatata kanalo kun RSS-fluo — finas")
                return Result.success()
            }
            logi("NovajElsendoj", "${kanaloj.size} kanaloj kun RSS-fluo: ${kanaloj.map { it.slug }}")

            val viditaj = settings.getString(VIDITAJ_KEY, "")
                .let { if (it.isBlank()) mutableSetOf() else it.split(",").toMutableSet() }
            val unuaRulo = viditaj.isEmpty()
            logi("NovajElsendoj", "${viditaj.size} jam viditaj elsendoj (unuaRulo=$unuaRulo)")

            val httpKliento = HttpClient(CIO) {
                install(HttpTimeout) { requestTimeoutMillis = 30_000; connectTimeoutMillis = 10_000 }
            }
            val parsilo = RssParsilo()
            var totalNovaj = 0

            for (kanalo in kanaloj) {
                try {
                    logi("NovajElsendoj", "Kontrolas ${kanalo.slug}: ${kanalo.podkastaRssUrl}")
                    val respondo = httpKliento.get(kanalo.podkastaRssUrl!!).bodyAsText()
                    logi("NovajElsendoj", "${kanalo.slug}: RSS-elŝuto kompleta — ${respondo.length} signoj")
                    val elsendoj = parsilo.parsuRss(respondo, kanalo) { httpKliento.get(it).bodyAsText() }
                    logi("NovajElsendoj", "${kanalo.slug}: ${elsendoj.size} elsendoj en RSS-fluo")
                    val plejNovaj = elsendoj.take(5)
                    // Unua rulo: semu viditajn SEN sciigi (evitas sciigo-ŝprucon pri ĉiuj ekzistantaj elsendoj)
                    if (unuaRulo) {
                        plejNovaj.forEach { viditaj.add(it.id) }
                        logi("NovajElsendoj", "${kanalo.slug}: unua rulo — semas ${plejNovaj.size} viditajn, ne sciigas")
                        continue
                    }
                    val novaj = plejNovaj.filter { it.id !in viditaj }
                    logi("NovajElsendoj", "${kanalo.slug}: ${novaj.size} novaj el ${plejNovaj.size} kontrolitaj")
                    for (elsendo in novaj) {
                        viditaj.add(elsendo.id)
                        senduSciigon(elsendo, kanalo)
                        totalNovaj++
                        delay(200)
                    }
                    if (novaj.isEmpty()) plejNovaj.forEach { viditaj.add(it.id) }
                } catch (e: Exception) {
                    loge("NovajElsendoj", "${kanalo.slug}: eraro dum kontrolado", e)
                }
            }

            httpKliento.close()
            if (viditaj.size > MAKS_VIDITAJ) {
                viditaj.retainAll(viditaj.toList().takeLast(MAKS_VIDITAJ).toSet())
            }
            settings.putString(VIDITAJ_KEY, viditaj.joinToString(","))
            logi("NovajElsendoj", "=== Worker finis — $totalNovaj novaj elsendoj, ${viditaj.size} viditaj total ===")
            Result.success()
        } catch (e: Exception) {
            loge("NovajElsendoj", "=== Worker paneis ===", e)
            Result.retry()
        }
    }

    private suspend fun senduSciigon(elsendo: Elsendo, kanalo: Kanalo) {
        val ctx = appContext
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(SCIIGO_KANALO_ID, "Novaj elsendoj", NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = "Sciigoj pri novaj elsendoj el ŝatataj kanaloj" }
            )
        }

        fun pleniguEkstraĵojn(intent: Intent) = intent.apply {
            putExtra(EXTRA_ELSENDO_ID, elsendo.id)
            putExtra(EXTRA_ELSENDO_TITOLO, elsendo.titolo)
            putExtra(EXTRA_ELSENDO_FLUO, elsendo.fluo)
            putExtra(EXTRA_ELSENDO_DATO, elsendo.dato)
            putExtra(EXTRA_ELSENDO_PRISKRIBO, elsendo.priskribo)
            putExtra(EXTRA_KANALO_SLUG, kanalo.slug)
            putExtra(EXTRA_KANALO_NOMO, kanalo.nomo)
            putExtra(EXTRA_BILDO_URL, elsendo.bildoUrl)
        }

        val malfermiPI = PendingIntent.getActivity(
            ctx, elsendo.id.hashCode(),
            pleniguEkstraĵojn(
                Intent(ctx, Class.forName("dk.nordfalk.esperanto.android.MainActivity")).apply {
                    action = ACTION_MALFERMI_ELSENDON
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val ludiPI = PendingIntent.getBroadcast(
            ctx, elsendo.id.hashCode() + 1,
            pleniguEkstraĵojn(
                Intent(ctx, LudiElsendoReceivilo::class.java).apply { action = ACTION_LUDI_ELSENDON }
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        nm.notify(
            elsendo.id.hashCode(),
            NotificationCompat.Builder(ctx, SCIIGO_KANALO_ID)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle(kanalo.nomo)
                .setContentText(elsendo.titolo)
                .setStyle(NotificationCompat.BigTextStyle().bigText(elsendo.titolo))
                .setAutoCancel(true)
                .setContentIntent(malfermiPI)
                .addAction(android.R.drawable.ic_media_play, "Ludi", ludiPI)
                .build()
        )
        logi("NovajElsendoj", "Sciigo sendita: ${kanalo.nomo} — ${elsendo.titolo}")
    }
}
