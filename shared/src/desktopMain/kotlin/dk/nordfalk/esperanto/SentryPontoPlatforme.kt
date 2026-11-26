package dk.nordfalk.esperanto

/**
 * Desktop-aktualigo de SentryPonto: no-op — la KMP-Sentry-SDK mem raportas
 * (kaj Desktop ĝenerale ne inicialigas Sentry-n, vidu desktopApp/Main.kt).
 */
actual fun sentryProtokoluPlatforme(nivelo: String, teksto: String) {}
actual fun sentryKaptuEsceptonPlatforme(teksto: String, staktraco: String) {}
actual fun sentryEtikeduPlatforme(ŝlosilo: String, valoro: String) {}
actual fun sinkroniguRaportadanKonsentonPlatforme(aktiva: Boolean) {}
