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
