package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.domain.model.Alarmo

/**
 * Planas aŭ mallumas alarmon sur la platformo.
 * - Android: AlarmManager + PendingIntent + BroadcastReceiver
 * - Desktop/wasmJs/iOS: NoOp (nur UI)
 *
 * Nur aktivaj alarmoj estas skeditaj.
 */
expect class AlarmoSkedilo() {
    /** Plani alarmon — se aktiva, vekos la aparaton je la specifita tempo. */
    fun skedi(alarmo: Alarmo)
    /** Mallas la PendingIntent por cxi tiu alarmo. */
    fun malplani(alarmoId: Int)
    /** Re-planas cxiujn aktivajn alarmojn (ekz. post boot). */
    fun reskediCxiujn(alarmoj: List<Alarmo>)
}

/**
 * Cxu cxi tiu platformo vere subtenas vekhorlogxon (vekigas la aparaton).
 * - Android: true (AlarmManager)
 * - Desktop/wasmJs/iOS: false (nur UI, ne vere vekas)
 */
expect val subtenasVekhorlogxn: Boolean

/**
 * Ĉu la apo rajtas skedi ekzaktajn alarmojn.
 * - Android 11 kaj sube: ĉiam true (neniu permeso bezonata)
 * - Android 12+: false (la permeso SCHEDULE_EXACT_ALARM/USE_EXACT_ALARM estas
 *   intence ne deklarita — la alarmo tamen ekigas, sed eble ĝis 10 minutoj malfrue)
 * - aliaj platformoj: ĉiam true
 */
expect fun ekzaktajAlarmojPermesataj(): Boolean
