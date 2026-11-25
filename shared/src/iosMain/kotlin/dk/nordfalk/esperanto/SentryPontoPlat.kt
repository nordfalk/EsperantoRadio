package dk.nordfalk.esperanto

/**
 * iOS-aktualigo de SentryPonto: no-op — la KMP-Sentry-SDK mem raportas.
 */
actual fun sentryProtokoluPlat(nivelo: String, teksto: String) {}
actual fun sentryEsceptoPlat(teksto: String, staktraco: String) {}
actual fun sentryEtikedoPlat(ŝlosilo: String, valoro: String) {}
actual fun sinkroniguRaportadanKonsentonPlat(aktiva: Boolean) {}
