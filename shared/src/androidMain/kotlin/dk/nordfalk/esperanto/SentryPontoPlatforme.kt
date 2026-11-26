package dk.nordfalk.esperanto

/**
 * Android-aktualigo de SentryPonto: no-op — la KMP-Sentry-SDK mem raportas
 * (vidu SentryAgordo.kt kaj ProtokoloUtil.kt).
 */
actual fun sentryProtokoluPlatforme(nivelo: String, teksto: String) {}
actual fun sentryKaptuEsceptonPlatforme(teksto: String, staktraco: String) {}
actual fun sentryEtikeduPlatforme(ŝlosilo: String, valoro: String) {}
actual fun sinkroniguRaportadanKonsentonPlatforme(aktiva: Boolean) {}
