package dk.nordfalk.esperanto

/**
 * Android-aktualigo de SentryPonto: no-op — la KMP-Sentry-SDK mem raportas
 * (vidu SentryAgordo.kt kaj ProtokoloUtil.kt).
 */
actual fun sentryProtokoluPlat(nivelo: String, teksto: String) {}
actual fun sentryEsceptoPlat(teksto: String, staktraco: String) {}
actual fun sentryEtikedoPlat(ŝlosilo: String, valoro: String) {}
actual fun sinkroniguRaportadanKonsentonPlat(aktiva: Boolean) {}
