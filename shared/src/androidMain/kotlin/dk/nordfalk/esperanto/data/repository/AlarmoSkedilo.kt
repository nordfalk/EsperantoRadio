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
import dk.nordfalk.esperanto.domain.model.sekvaEkigo
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

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

        // Ekde Android 12 (API 31) ekzaktaj alarmoj postulas permeson, kiun ni intence
        // NE deklaras (por eviti la Google Play-deklaron). Sen ĝi ni uzas setWindow()
        // kun 10-minuta fenestro — ne postulas permeson, kaj la alarmo tamen ekigas
        // ene de 10 minutoj. Sur Android 11 kaj sube setAlarmClock() funkcias sen permeso.
        if (ekzaktajAlarmojPermesataj()) {
            try {
                // setAlarmClock: escepto de Doze, kaj la sistemo montras alarm-ikonon en la statusbreto
                val montroIntent = PendingIntent.getActivity(
                    appContext, alarmo.id,
                    Intent().setClassName(appContext, Agadoj.CHEFA_AGADO),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAtMillis, montroIntent), pendingIntent)
            } catch (e: SecurityException) {
                logw("AlarmoSkedilo", "setAlarmClock malsukcesis — uzas 10-minutan fenestron", e)
                alarmManager.setWindow(AlarmManager.RTC_WAKEUP, triggerAtMillis, FENESTRO_MS, pendingIntent)
            }
        } else {
            logw("AlarmoSkedilo", "Ekzaktaj alarmoj ne subtenataj (Android 12+) — uzas 10-minutan fenestron")
            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, triggerAtMillis, FENESTRO_MS, pendingIntent)
        }

        logi("AlarmoSkedilo", "Skedis alarmon ${alarmo.id}: ${alarmo.tempoTeksto} ${alarmo.ripetoTeksto} → ${alarmo.kanaloSlug} (trigger en ${(triggerAtMillis - System.currentTimeMillis()) / 1000}s)")
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
        kreuPendingIntent(alarmo.id, alarmo.kanaloSlug, alarmo.etikedo)

    private fun kreuPendingIntent(alarmoId: Int, kanaloSlug: String? = null, etikedo: String? = null): PendingIntent {
        val intent = Intent(appContext, AlarmoReceivilo::class.java).apply {
            action = "dk.nordfalk.esperanto.ALARMO_EKIGAS"
            putExtra("alarmo_id", alarmoId)
            if (kanaloSlug != null) putExtra("kanal_slug", kanaloSlug)
            if (etikedo != null) putExtra("etikedo", etikedo)
        }
        return PendingIntent.getBroadcast(
            appContext,
            alarmoId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    @OptIn(ExperimentalTime::class)
    private fun kalkuluNexxtemTempon(alarmo: Alarmo): Long {
        val tz = TimeZone.currentSystemDefault()
        return alarmo.sekvaEkigo(Clock.System.now().toLocalDateTime(tz)).toInstant(tz).toEpochMilliseconds()
    }

    private companion object {
        /** Maksimuma malfruo kiam ekzaktaj alarmoj ne estas permesataj. */
        const val FENESTRO_MS = 10 * 60 * 1000L
    }
}
actual val subtenasVekhorlogxn: Boolean = true

actual fun ekzaktajAlarmojPermesataj(): Boolean {
    // Sen SCHEDULE_EXACT_ALARM/USE_EXACT_ALARM en la manifest, canScheduleExactAlarms()
    // ĉiam redonas false sur Android 12+. Sur Android 11 kaj sube neniu permeso bezonatas.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    return false
}
