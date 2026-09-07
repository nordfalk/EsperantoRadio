package dk.nordfalk.esperanto.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dk.nordfalk.esperanto.logi
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Skedas la NovajElsendojKontroloWorker per WorkManager.
 *
 * Rulas 2 fojojn tage — cxe 7:00 kaj 16:00 (±1h), kiam retkonekto ekzistas
 * kaj la baterio ne estas malalta.
 */
object NovajElsendojSkedilo {

    private const val WORK_MATENA = "novaj_elsendoj_matene"
    private const val WORK_POSTTAGMEZE = "novaj_elsendoj_posttagmeze"

    private val construktoj = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build()

    /**
     * Skedas du periodajn Worker-ojn (24h ciklo):
     * - unu cxe 7:00 (matena kontrolo)
     * - unu cxe 16:00 (posttagmeza kontrolo)
     */
    fun skedu(context: Context) {
        val wm = WorkManager.getInstance(context)

        wm.enqueueUniquePeriodicWork(
            WORK_MATENA,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<NovajElsendojKontroloWorker>(24, TimeUnit.HOURS)
                .setConstraints(construktoj)
                .setInitialDelay(kalkuluInitialProkraston(7), TimeUnit.MILLISECONDS)
                .build()
        )
        wm.enqueueUniquePeriodicWork(
            WORK_POSTTAGMEZE,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<NovajElsendojKontroloWorker>(24, TimeUnit.HOURS)
                .setConstraints(construktoj)
                .setInitialDelay(kalkuluInitialProkraston(16), TimeUnit.MILLISECONDS)
                .build()
        )

        logi("NovajElsendojSkedilo", "Worker skedita (7:00 kaj 16:00, ±1h, neniu ĉe malalta baterio)")
    }

    /**
     * Kalkulas prokraston en ms ĝis la sekva horo `celaHoro`.
     */
    private fun kalkuluInitialProkraston(celaHoro: Int): Long {
        val nun = Calendar.getInstance()
        val celo = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, celaHoro)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= nun.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
        }
        return celo.timeInMillis - nun.timeInMillis
    }
}
