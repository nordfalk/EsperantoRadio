package dk.nordfalk.esperanto.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dk.nordfalk.esperanto.logi
import java.util.concurrent.TimeUnit

/**
 * Skedas la NovajElsendojKontroloWorker per WorkManager.
 *
 * Rulas cxiun ~12 horojn (2 fojojn tage) se retkonekto ekzistas.
 * Ankaux tuj rulas unu fojon se la apo estas nova instalita.
 */
object NovajElsendojSkedilo {

    private const val WORK_NOMO = "novaj_elsendoj_kontrolo"

    /**
     * Skedas la periodan Worker-on kaj, se unua fojo, lanĉas tuj.
     */
    fun skedu(context: Context) {
        val workManager = WorkManager.getInstance(context)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Perioda: cxiu 12 horoj (WorkManager minimumo estas 15 min)
        val periodaPeto = PeriodicWorkRequestBuilder<NovajElsendojKontroloWorker>(
            12, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NOMO,
            ExistingPeriodicWorkPolicy.KEEP,
            periodaPeto
        )

        logi("NovajElsendojSkedilo", "Worker skedita (perioda, cxiu 12h)")
    }
}
