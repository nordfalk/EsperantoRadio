package dk.nordfalk.esperanto.data.repository

/**
 * Ĉu ĉi tiu platformo subtenas sciigojn pri novaj elsendoj.
 * - Android: true (WorkManager + NotificationManager)
 * - Desktop/wasmJs/iOS: false
 */
expect val subtenasSciigojn: Boolean

/**
 * Malfermas la sistemajn sciig-agordojn por ĉi tiu apo.
 * - Android: malfermas APP_NOTIFICATION_SETTINGS
 * - aliaj: NoOp
 */
expect fun malfermuSciigAgordojn()
