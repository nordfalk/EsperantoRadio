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
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.loge
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.delay

/**
 * Datumo pri nova elsendo trovita de la Worker.
 */
data class NovaElsendoInfo(
    val elsendoId: String,
    val titolo: String,
    val kanaloNomo: String,
    val kanaloSlug: String,
    val fluo: String,
    val bildoUrl: String?,
)

/**
 * Fona Worker kiu periode kontrolas ŝatatajn kanalojn por novaj elsendoj.
 *
 * Skedita per WorkManager cxe cxiu ~12 horoj. Por cxiu ŝatata kanalo:
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
        const val EXTRA_KANALO_SLUG = "kanalo_slug"
        const val EXTRA_KANALO_NOMO = "kanalo_nomo"
        const val EXTRA_BILDO_URL = "bildo_url"
        const val ACTION_LUDI_ELSENDON = "dk.nordfalk.esperanto.LUDI_ELSENDON"
        const val ACTION_MALFERMI_ELSENDON = "dk.nordfalk.esperanto.MALFERMI_ELSENDON"

        private const val VIDITAJ_KEY = "viditaj_elsendoj"
        private const val MAKS_VIDITAJ = 500
    }

    override suspend fun doWork(): Result {
        logi("NovajElsendoj", "Worker komencis — kontrolas ŝatatajn kanalojn")

        return try {
            val settings = kreuSettings()

            // Legu ŝatatajn kanalojn
            val plejStr = settings.getString("plejŝatataj_kanaloj", "")
            if (plejStr.isBlank()) {
                logi("NovajElsendoj", "Neniu ŝatata kanalo — finas")
                return Result.success()
            }
            val plejŝatataj = plejStr.split(",").toSet()
            logi("NovajElsendoj", "Kontrolas ${plejŝatataj.size} ŝatatajn kanalojn: $plejŝatataj")

            // Legu kanalkonfiguron
            val agordo = KanalAgordoLeganto().legu(leguBundledKanalkonfiguron())
            val kanaloj = agordo.kanaloj
                .filter { it.kodo in plejŝatataj && it.elsendojRssUrl != null }
                .map { it.alKanalo() }

            if (kanaloj.isEmpty()) {
                logi("NovajElsendoj", "Neniu ŝatata kanalo kun RSS-fluo — finas")
                return Result.success()
            }

            // Legu viditajn elsendojn
            val viditajStr = settings.getString(VIDITAJ_KEY, "")
            val viditaj = if (viditajStr.isBlank()) mutableSetOf() else viditajStr.split(",").toMutableSet()

            val httpKliento = HttpClient(CIO) {
                install(HttpTimeout) {
                    requestTimeoutMillis = 30_000
                    connectTimeoutMillis = 10_000
                }
            }
            val parsilo = RssParsilo()

            var totalNovaj = 0

            for (kanalo in kanaloj) {
                try {
                    logi("NovajElsendoj", "Kontrolas ${kanalo.slug}: ${kanalo.podkastaRssUrl}")
                    val respondo = httpKliento.get(kanalo.podkastaRssUrl!!).bodyAsText()
                    val elsendoj = parsilo.parsuRss(respondo, kanalo) { urlD ->
                        httpKliento.get(urlD).bodyAsText()
                    }
                    logi("NovajElsendoj", "${kanalo.slug}: ${elsendoj.size} elsendoj en RSS-fluo")

                    // Prenu nur la plej novajn (maks 5 por eviti sciigo-ŝprucon)
                    val plejNovaj = elsendoj.take(5)
                    val novaj = plejNovaj.filter { it.id !in viditaj }

                    if (novaj.isNotEmpty()) {
                        logi("NovajElsendoj", "${kanalo.slug}: ${novaj.size} novaj elsendoj!")
                        for (elsendo in novaj) {
                            viditaj.add(elsendo.id)
                            val info = NovaElsendoInfo(
                                elsendoId = elsendo.id,
                                titolo = elsendo.titolo,
                                kanaloNomo = kanalo.nomo,
                                kanaloSlug = kanalo.slug,
                                fluo = elsendo.fluo,
                                bildoUrl = elsendo.bildoUrl,
                            )
                            senduSciigon(info)
                            totalNovaj++
                            // Malgranda paŭzo inter sciigoj
                            delay(200)
                        }
                    } else {
                        // Registru la viditajn eĉ se ne novaj
                        for (e in plejNovaj) viditaj.add(e.id)
                        logi("NovajElsendoj", "${kanalo.slug}: neniuj novaj elsendoj")
                    }
                } catch (e: Exception) {
                    loge("NovajElsendoj", "${kanalo.slug}: eraro dum kontrolado", e)
                    // Daŭrigu kun la sekva kanalo — toleremo
                }
            }

            httpKliento.close()

            // Persistu viditajn
            if (viditaj.size > MAKS_VIDITAJ) {
                val surplus = viditaj.size - MAKS_VIDITAJ
                viditaj.drop(surplus).toMutableSet().let { viditaj.clear(); viditaj.addAll(it) }
            }
            settings.putString(VIDITAJ_KEY, viditaj.joinToString(","))

            logi("NovajElsendoj", "Worker finis — $totalNovaj novaj elsendoj, ${viditaj.size} viditaj total")
            Result.success()
        } catch (e: Exception) {
            loge("NovajElsendoj", "Worker paneis", e)
            Result.retry()
        }
    }

    /**
     * Kreas kaj montras sciigon pri nova elsendo.
     * - Klako → malfermas la elsendon en la apo (PendingIntent al MainActivity)
     * - "Ludi" ago → komencas ludi per LudiElsendoReceivilo
     */
    private suspend fun senduSciigon(info: NovaElsendoInfo) {
        val ctx = appContext
        val sciigMastrumanto = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Kreu sciigkanalon (Android 8+ postulas)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val kanalo = NotificationChannel(
                SCIIGO_KANALO_ID,
                "Novaj elsendoj",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Sciigoj pri novaj elsendoj el ŝatataj kanaloj"
            }
            sciigMastrumanto.createNotificationChannel(kanalo)
        }

        // PendingIntent por malfermi elsendon (klako sur la sciigo)
        val malfermiIntent = Intent(ctx, Class.forName("dk.nordfalk.esperanto.android.MainActivity")).apply {
            action = ACTION_MALFERMI_ELSENDON
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ELSENDO_ID, info.elsendoId)
            putExtra(EXTRA_ELSENDO_TITOLO, info.titolo)
            putExtra(EXTRA_ELSENDO_FLUO, info.fluo)
            putExtra(EXTRA_KANALO_SLUG, info.kanaloSlug)
            putExtra(EXTRA_KANALO_NOMO, info.kanaloNomo)
            putExtra(EXTRA_BILDO_URL, info.bildoUrl)
        }
        val malfermiPendingIntent = PendingIntent.getActivity(
            ctx,
            info.elsendoId.hashCode(),
            malfermiIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // PendingIntent por ludi elsendon rekte de la sciigo
        val ludiIntent = Intent(ctx, LudiElsendoReceivilo::class.java).apply {
            action = ACTION_LUDI_ELSENDON
            putExtra(EXTRA_ELSENDO_ID, info.elsendoId)
            putExtra(EXTRA_ELSENDO_TITOLO, info.titolo)
            putExtra(EXTRA_ELSENDO_FLUO, info.fluo)
            putExtra(EXTRA_KANALO_SLUG, info.kanaloSlug)
            putExtra(EXTRA_KANALO_NOMO, info.kanaloNomo)
            putExtra(EXTRA_BILDO_URL, info.bildoUrl)
        }
        val ludiPendingIntent = PendingIntent.getBroadcast(
            ctx,
            info.elsendoId.hashCode() + 1,
            ludiIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val sciigo = NotificationCompat.Builder(ctx, SCIIGO_KANALO_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(info.kanaloNomo)
            .setContentText(info.titolo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(info.titolo))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(malfermiPendingIntent)
            .addAction(android.R.drawable.ic_media_play, "Ludi", ludiPendingIntent)
            .build()

        sciigMastrumanto.notify(info.elsendoId.hashCode(), sciigo)
        logi("NovajElsendoj", "Sciigo sendita: ${info.kanaloNomo} — ${info.titolo}")
    }
}
