package dk.nordfalk.esperanto.data.repository

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dk.nordfalk.esperanto.data.config.appContext
import dk.nordfalk.esperanto.data.config.kreuSettings
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.logw
import kotlinx.serialization.builtins.ListSerializer
import dk.nordfalk.esperanto.domain.model.Alarmo
import kotlinx.serialization.json.Json

/**
 * BroadcastReceiver kiu re-planas cxiujn alarmojn post kiam la aparato restartis.
 *
 * Legas la persistitajn alarmojn el Settings kaj vokas AlarmoSkedilo.reskediCxiujn().
 */
class BootReceivilo : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) {
            // La uzanto donis la permeson → re-skedu kun setAlarmClock anstataŭ la 10-minuta fenestro
            logi("BootReceivilo", "Permeso por ekzaktaj alarmoj ŝanĝiĝis — re-planas alarmojn")
            try {
                appContext = context.applicationContext
                AlarmoSkedilo().reskediCxiujn(leguPersistitajnAlarmojn())
            } catch (e: Exception) {
                logw("BootReceivilo", "Eraro re-planante alarmojn post permes-ŝanĝo", e)
            }
            return
        }
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON" &&
            intent.action != "android.intent.action.LOCKED_BOOT_COMPLETED"
        ) return

        logi("BootReceivilo", "Boot ricevita — re-planas alarmojn kaj sciigojn")

        try {
            // Agordu appContext por AlarmoSkedilo kaj kreuSettings (cexe boot, MainActivity ne jam rulis)
            appContext = context.applicationContext
            val settings = kreuSettings()

            // Re-skedu sciigojn nur se sciigoj ŝaltitaj kaj estas ŝatataj kanaloj
            val sciigoj = settings.getBoolean(SettingsKeys.SCIIGOJ, true)
            val plejStr = settings.getString(SettingsKeys.PLEJŜATATAJ_KANALOJ, "")
            val plejŝatataj = if (plejStr.isBlank()) emptySet() else plejStr.split(",").toSet()
            ghisdatiguSciigSkedon(plejŝatataj, sciigoj)
            val alarmoj = leguPersistitajnAlarmojn()
            logi("BootReceivilo", "Legis ${alarmoj.size} alarmojn, re-planas")

            val skedilo = AlarmoSkedilo()
            skedilo.reskediCxiujn(alarmoj)
        } catch (e: Exception) {
            logw("BootReceivilo", "Eraro re-planante alarmojn", e)
        }
    }
}

/**
 * Legas la persistitajn alarmojn rekte el Settings (sen AppStato — uzebla el Receiviloj
 * kiam la procezo ĵus startis). Postulas ke `appContext` estas agordita.
 */
internal fun leguPersistitajnAlarmojn(): List<Alarmo> {
    val str = kreuSettings().getString(SettingsKeys.ALARMOJ, "")
    if (str.isBlank()) return emptyList()
    return Json { ignoreUnknownKeys = true }.decodeFromString(ListSerializer(Alarmo.serializer()), str)
}
