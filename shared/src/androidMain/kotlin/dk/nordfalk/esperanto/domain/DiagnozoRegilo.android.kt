package dk.nordfalk.esperanto.domain

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import dk.nordfalk.esperanto.data.config.appContext
import dk.nordfalk.esperanto.domain.model.DiagnozoAgo
import dk.nordfalk.esperanto.domain.model.DiagnozoRezulto
import dk.nordfalk.esperanto.domain.model.DiagnozoTipo
import dk.nordfalk.esperanto.domain.model.Severeco
import dk.nordfalk.esperanto.loge
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.logw
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.net.InetAddress

/**
 * Android-implemento de DiagnozoRegilo.
 *
 * Kontrolas la kvar ĉefajn kialojn kial fona ludado / aŭtoludo ĉesas:
 * 1. **Bateri-optimumado** (Samsung MARs, Android Doze) — `PowerManager.isIgnoringBatteryOptimizations`
 * 2. **Sciig-permeso** — `NotificationManagerCompat.areNotificationsEnabled`
 * 3. **Ekzaktaj alarmoj** (Android 12+) — `AlarmManager.canScheduleExactAlarms`
 * 4. **DNS-solvado** — provas solvi `archive.org` (ofte blokita de privata DNS / VPN)
 *
 * La rezulto estas konservita en [problemoj] (StateFlow) por ke la ĉefekrano
 * povu montri avertosignon.
 */
actual class DiagnozoRegilo(private val context: Context) {

    private val _problemoj = MutableStateFlow<List<DiagnozoRezulto>>(emptyList())
    actual val problemoj: StateFlow<List<DiagnozoRezulto>> = _problemoj.asStateFlow()

    actual suspend fun kontroli(): List<DiagnozoRezulto> {
        val rezultoj = kontroliInterne(inkluziveDns = true)
        _problemoj.value = rezultoj
        return rezultoj
    }

    actual suspend fun kontroliRapide(): List<DiagnozoRezulto> {
        val rezultoj = kontroliInterne(inkluziveDns = false)
        _problemoj.value = rezultoj
        return rezultoj
    }

    private suspend fun kontroliInterne(inkluziveDns: Boolean): List<DiagnozoRezulto> = withContext(Dispatchers.IO) {
        val rezultoj = mutableListOf<DiagnozoRezulto>()
        logi("Diagnozo", "Komencas kontrolojn (inkluziveDns=$inkluziveDns)…")

        // 1. Bateri-optimumado
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
                rezultoj.add(
                    DiagnozoRezulto(
                        tipo = DiagnozoTipo.BATERIO_OPTIMIZATION,
                        titolo = "Bateri-optimumado estas ŝaltita",
                        priskribo = "La sistemo (ekz. Samsung MARs aŭ Android Doze) povas limigi la retaliron kaj mortigi la aplikon en la fono. Tio kaŭzas ke aŭtomata daŭrigo kaj fona ludado ĉesas sen averto.",
                        severeco = Severeco.ERARO,
                        ago = DiagnozoAgo("Malfermi bateriajn agordojn", "battery"),
                    )
                )
                logw("Diagnozo", "Bateri-optimumado ŝaltita por ${context.packageName}")
            } else {
                logi("Diagnozo", "Bateri-optimumado: en ordo")
            }
        } catch (e: Exception) {
            loge("Diagnozo", "Malsukcesis kontroli bateri-optimumadon", e)
        }

        // 2. Sciig-permeso
        try {
            val nm = NotificationManagerCompat.from(context)
            if (!nm.areNotificationsEnabled()) {
                rezultoj.add(
                    DiagnozoRezulto(
                        tipo = DiagnozoTipo.NOTIFICATIONS,
                        titolo = "Sciigoj ne estas permesitaj",
                        priskribo = "La apliko ne povas montri sciigojn pri novaj elsendoj, kaj la media sciigo en la fono ne aperas.",
                        severeco = Severeco.AVERTO,
                        ago = DiagnozoAgo("Malfermi sciig-agordojn", "notifications"),
                    )
                )
                logw("Diagnozo", "Sciig-permeso mankas")
            } else {
                logi("Diagnozo", "Sciig-permeso: en ordo")
            }
        } catch (e: Exception) {
            loge("Diagnozo", "Malsukcesis kontroli sciig-permeson", e)
        }

        // 3. Ekzaktaj alarmoj (Android 12+)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (!am.canScheduleExactAlarms()) {
                    rezultoj.add(
                        DiagnozoRezulto(
                            tipo = DiagnozoTipo.EXACT_ALARMS,
                            titolo = "Ekzaktaj alarmoj ne estas permesitaj",
                            priskribo = "Vekhorloĝaj alarmoj povas esti prokrastitaj je ĝis 10 minutoj. Donu la permeson por ekzaktaj alarmoj.",
                            severeco = Severeco.AVERTO,
                            ago = DiagnozoAgo("Donu permeson por ekzaktaj alarmoj", "exact_alarms"),
                        )
                    )
                    logw("Diagnozo", "Ekzaktaj alarmoj ne permesitaj")
                } else {
                    logi("Diagnozo", "Ekzaktaj alarmoj: en ordo")
                }
            }
        } catch (e: Exception) {
            loge("Diagnozo", "Malsukcesis kontroli ekzaktajn alarmojn", e)
        }

        // 4. DNS-testo — nur en la kompleta kontrolo (povas esti malrapida)
        if (inkluziveDns) {
            try {
                InetAddress.getByName("archive.org")
                logi("Diagnozo", "DNS: en ordo")
            } catch (e: Exception) {
                rezultoj.add(
                    DiagnozoRezulto(
                        tipo = DiagnozoTipo.DNS,
                        titolo = "DNS-solvado malsukcesis",
                        priskribo = "La apliko ne povas solvi retej-nomojn (ekz. archive.org). Ofte kaŭzas privata DNS (DNS-over-TLS), VPN, aŭ la sistemo limigas la aplikon en la fono (vidu bateri-optimumadon). Kontrolu la sendratajn agordojn.",
                        severeco = Severeco.ERARO,
                        ago = DiagnozoAgo("Malfermi sendratajn agordojn", "wireless"),
                    )
                )
                logw("Diagnozo", "DNS-solvado malsukcesis por archive.org", e)
            }
        }

        logi("Diagnozo", "Kontroloj kompletaj — ${rezultoj.size} problemo(j) trovitaj")
        rezultoj
    }
}

actual fun kreuDiagnozoRegilon(): DiagnozoRegilo = DiagnozoRegilo(appContext)

actual fun malfermiSistemAgordon(intenco: String) {
    val context = appContext
    try {
        val intent = when (intenco) {
            "battery" -> Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

            "notifications" -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }

            "exact_alarms" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                } else null
            }

            "wireless" -> Intent(Settings.ACTION_WIRELESS_SETTINGS)

            else -> Intent(Settings.ACTION_SETTINGS)
        }
        intent?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(it)
            logi("Diagnozo", "Malfermita sistem-agordo: $intenco")
        }
    } catch (e: Exception) {
        loge("Diagnozo", "Malsukcesis malfermi sistem-agordon: $intenco", e)
    }
}
