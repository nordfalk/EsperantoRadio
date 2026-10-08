package dk.nordfalk.esperanto.domain

import dk.nordfalk.esperanto.domain.model.DiagnozoRezulto
import kotlinx.coroutines.flow.StateFlow

/**
 * Regilo por diagnozi oftajn problemojn de la aparato/agordoj.
 *
 * Platform-specifa:
 * - **Android**: kontrolas PowerManager (bateri-optimumado), NotificationManager
 *   (sciig-permeso), AlarmManager (ekzaktaj alarmoj), kaj provas DNS-solvadon.
 * - **Desktop / Web / iOS**: redonas malplenan liston (nenio specifa).
 *
 * La rezultoj estas uzataj en [dk.nordfalk.esperanto.ui.DiagnozoEkrano] por
 * montri klarigojn kaj butonojn kiuj gvidas al la ĝustaj sistem-agordoj.
 * Krome, [problemoj] estas observebla por montri avertosignon sur la ĉefekrano.
 */
expect class DiagnozoRegilo {
    /**
     * La lastaj trovitaj problemoj — observebla por montri indikilon sur la
     * ĉefekrano. Ĝisdatigata de [kontroli] kaj [kontroliRapide].
     */
    val problemoj: StateFlow<List<DiagnozoRezulto>>

    /**
     * Plenumas ĉiujn kontrolojn (inkluzive DNS-teston) kaj redonas la trovitajn
     * problemojn/avertojn. Malplena listo = ĉio en ordo.
     */
    suspend fun kontroli(): List<DiagnozoRezulto>

    /**
     * Rapida kontrolo sen la DNS-testo (por uzi ĉe app-starto en la fono,
     * ne blokante la UI). Ĝisdatigas [problemoj].
     */
    suspend fun kontroliRapide(): List<DiagnozoRezulto>
}

/** Kreas la platform-specifan DiagnozoRegilo-n. */
expect fun kreuDiagnozoRegilon(): DiagnozoRegilo

/**
 * Malfermas la sisteman agordon por la donita intenco-identigilo.
 *
 * Subtenataj intencoj (sur Android):
 * - `"battery"` — bateriaj optimumigoj (Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
 * - `"notifications"` — sciig-agordoj de la apo
 * - `"exact_alarms"` — permeso por ekzaktaj alarmoj (Android 12+)
 * - `"wireless"` — sendrataj agordoj (por privata DNS)
 *
 * Sur aliaj platformoj: faras nenion.
 */
expect fun malfermiSistemAgordon(intenco: String)
