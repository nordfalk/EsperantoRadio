package dk.nordfalk.esperanto.data.repository

/**
 * Ĉu ĉi tiu platformo subtenas sciigojn pri novaj elsendoj.
 * - Android: true (WorkManager + NotificationManager)
 * - Desktop/wasmJs/iOS: false
 */
expect val subtenasSciigojn: Boolean

/**
 * Ĉu la sciig-permeso estas donita.
 * - Android: kontrolas POST_NOTIFICATIONS (Android 13+); antaŭe ĉiam true
 * - aliaj: ĉiam true (ne bezonas permeson)
 */
expect fun sciigPermesoDonita(): Boolean

/**
 * Malfermas la sistemajn sciig-agordojn por ĉi tiu apo.
 * - Android: malfermas APP_NOTIFICATION_SETTINGS
 * - aliaj: NoOp
 */
expect fun malfermuSciigAgordojn()
