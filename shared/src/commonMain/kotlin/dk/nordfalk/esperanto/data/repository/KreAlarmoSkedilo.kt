package dk.nordfalk.esperanto.data.repository

/**
 * Kreas la platform-specifan AlarmoSkedilo-n.
 * - Android: AlarmManager + PendingIntent + BroadcastReceiver
 * - Desktop/wasmJs/iOS: NoOp
 */
expect fun kreAlarmoSkedilo(): AlarmoSkedilo
