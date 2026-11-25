package dk.nordfalk.esperanto

/**
 * Desktop-aktualigo de SentryPonto: no-op — la KMP-Sentry-SDK mem raportas
 * (kaj Desktop ĝenerale ne inicialigas Sentry-n, vidu desktopApp/Main.kt).
 */
actual fun sentryProtokoluPlat(nivelo: String, teksto: String) {}
actual fun sentryEsceptoPlat(teksto: String, staktraco: String) {}
actual fun sentryEtikedoPlat(ŝlosilo: String, valoro: String) {}
actual fun sinkroniguRaportadanKonsentonPlat(aktiva: Boolean) {}
