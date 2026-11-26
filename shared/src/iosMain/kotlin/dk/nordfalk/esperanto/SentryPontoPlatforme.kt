package dk.nordfalk.esperanto

/**
 * iOS-aktualigo de SentryPonto: no-op — la KMP-Sentry-SDK mem raportas.
 */
actual fun sentryProtokoluPlatforme(nivelo: String, teksto: String) {}
actual fun sentryKaptuEsceptonPlatforme(teksto: String, staktraco: String) {}
actual fun sentryEtikeduPlatforme(ŝlosilo: String, valoro: String) {}
actual fun sinkroniguRaportadanKonsentonPlatforme(aktiva: Boolean) {}
