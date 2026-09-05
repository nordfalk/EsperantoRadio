package dk.nordfalk.esperanto.data.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dk.nordfalk.esperanto.data.config.appContext
import dk.nordfalk.esperanto.data.config.kreSettings
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.logw
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * BroadcastReceiver kiu re-planas cxiujn alarmojn post kiam la aparato restartis.
 *
 * Legas la persistitajn alarmojn el Settings kaj vokas AlarmoSkedilo.reskediCxiujn().
 */
class BootReceivilo : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON" &&
            intent.action != "android.intent.action.LOCKED_BOOT_COMPLETED"
        ) return

        logi("BootReceivilo", "Boot ricevita — re-planas alarmojn")

        try {
            // Agordu appContext por AlarmoSkedilo kaj kreSettings (cexe boot, MainActivity ne jam rulis)
            appContext = context.applicationContext
            val settings = kreSettings()
            val str = settings.getString("alarmoj", "")
            if (str.isBlank()) {
                logi("BootReceivilo", "Neniu persistita alarmo")
                return
            }

            val json = Json { ignoreUnknownKeys = true }
            val alarmoj = json.decodeFromString(ListSerializer(dk.nordfalk.esperanto.domain.model.Alarmo.serializer()), str)
            logi("BootReceivilo", "Legis ${alarmoj.size} alarmojn, re-planas")

            val skedilo = AlarmoSkedilo()
            skedilo.reskediCxiujn(alarmoj)
        } catch (e: Exception) {
            logw("BootReceivilo", "Eraro re-planante alarmojn", e)
        }
    }
}
