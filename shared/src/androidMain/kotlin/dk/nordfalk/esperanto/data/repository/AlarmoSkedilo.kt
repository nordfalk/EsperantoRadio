package dk.nordfalk.esperanto.data.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dk.nordfalk.esperanto.domain.model.Alarmo
import dk.nordfalk.esperanto.data.config.appContext
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.logw
import java.util.Calendar

actual class AlarmoSkedilo actual constructor() {

    private val alarmManager: AlarmManager
        get() = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    actual fun skedi(alarmo: Alarmo) {
        if (!alarmo.aktiva) {
            logi("AlarmoSkedilo", "Ne skedas — alarmo ${alarmo.id} ne aktiva")
            return
        }

        val triggerAtMillis = kalkuluNexxtemTempon(alarmo)
        val pendingIntent = kreuPendingIntent(alarmo)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }

        logi("AlarmoSkedilo", "Skedis alarmon ${alarmo.id}: ${alarmo.tempoTeksto} ${alarmo.ripetoTeksto} → ${alarmo.kanalSlug} (trigger en ${(triggerAtMillis - System.currentTimeMillis()) / 1000}s)")
    }

    actual fun malplani(alarmoId: Int) {
        val pendingIntent = kreuPendingIntent(alarmoId)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        logi("AlarmoSkedilo", "Malplanis alarmon $alarmoId")
    }

    actual fun reskediCxiujn(alarmoj: List<Alarmo>) {
        logi("AlarmoSkedilo", "Re-planas ${alarmoj.size} alarmojn")
        for (alarmo in alarmoj) {
            if (alarmo.aktiva) {
                skedi(alarmo)
            } else {
                malplani(alarmo.id)
            }
        }
    }

    private fun kreuPendingIntent(alarmo: Alarmo): PendingIntent =
        kreuPendingIntent(alarmo.id, alarmo.kanalSlug, alarmo.etikedo)

    private fun kreuPendingIntent(alarmoId: Int, kanalSlug: String? = null, etikedo: String? = null): PendingIntent {
        val intent = Intent(appContext, AlarmoReceivilo::class.java).apply {
            action = "dk.nordfalk.esperanto.ALARMO_EKIGAS"
            putExtra("alarmo_id", alarmoId)
            if (kanalSlug != null) putExtra("kanal_slug", kanalSlug)
            if (etikedo != null) putExtra("etikedo", etikedo)
        }
        return PendingIntent.getBroadcast(
            appContext,
            alarmoId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Kalkulas la sekvan tempon kiam la alarmo devas ekigi.
     * Se ripeto == 0 (unufoje), ĝi ekigas hodiaŭ aŭ morgaŭ.
     * Se ripeto != 0, ĝi trovas la sekvan tagon kiu kongruas kun la bitmasko.
     */
    private fun kalkuluNexxtemTempon(alarmo: Alarmo): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarmo.horo)
            set(Calendar.MINUTE, alarmo.minuto)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (alarmo.ripeto == 0) {
            // Unufoje — se la tempo jam pasis hodiaŭ, planu por morgaŭ
            if (target.timeInMillis <= now.timeInMillis) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
        } else {
            // Ripeto — trovu la sekvan taŭgan tagon
            // Bitmasko: 0x01=Lundo(Calendar.MONDAY=2) ... 0x40=Dimanĉo(Calendar.SUNDAY=1)
            val mapo = intArrayOf(0x40, 0x01, 0x02, 0x04, 0x08, 0x10, 0x20) // Dimanĉo..Sabato
            for (i in 0..7) {
                val testCal = target.clone() as Calendar
                testCal.add(Calendar.DAY_OF_YEAR, i)
                val calTago = testCal.get(Calendar.DAY_OF_WEEK) - 1 // 1=Dimanĉo -> 0, 2=Lundo -> 1...
                val bito = if (calTago == 0) 0x40 else 1 shl (calTago - 1) // Dimanĉo=0x40, Lundo=0x01...
                if (alarmo.ripeto and bito != 0) {
                    if (testCal.timeInMillis > now.timeInMillis || i == 0 && target.timeInMillis > now.timeInMillis) {
                        return testCal.timeInMillis
                    }
                    if (i == 0 && target.timeInMillis <= now.timeInMillis) {
                        // Hodiaux la tempo pasis — provu morgauxu
                        continue
                    }
                    return testCal.timeInMillis
                }
            }
            // Se neniu tago kongruas en 7 tagoj (ne devus okazi), planu por morgaŭ
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        return target.timeInMillis
    }
}
actual val subtenasVekhorlogxn: Boolean = true
