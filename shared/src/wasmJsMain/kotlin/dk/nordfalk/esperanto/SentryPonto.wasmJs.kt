package dk.nordfalk.esperanto

/**
 * wasmJs-aktualigo de SentryPonto: transdonas al la `window.ert`-ponto,
 * kiun difinas la `index.html` de la webApp (konsento + Sentry JS-SDK).
 *
 * La js()-vokoj estas esprimoj kun mallongigo (&&) — se la ponto mankas
 * (unuo-testoj, frua starto), ĉio restas sendanĝere silenta.
 */
actual fun sentryProtokoluPlatforme(nivelo: String, teksto: String) {
    sentryPontoProtokolo(nivelo, teksto)
}

actual fun sentryKaptuEsceptonPlatforme(teksto: String, staktraco: String) {
    sentryPontoEscepto(teksto, staktraco)
}

actual fun sentryEtikeduPlatforme(ŝlosilo: String, valoro: String) {
    sentryPontoEtikedo(ŝlosilo, valoro)
}

actual fun sinkroniguRaportadanKonsentonPlatforme(aktiva: Boolean) {
    sentryPontoKonsento(aktiva)
}

private fun sentryPontoProtokolo(nivelo: String, teksto: String): Unit =
    js("window.ert && window.ert.protokolo(nivelo, teksto)")

private fun sentryPontoEscepto(teksto: String, staktraco: String): Unit =
    js("window.ert && window.ert.escepto(teksto, staktraco)")

private fun sentryPontoEtikedo(ŝlosilo: String, valoro: String): Unit =
    js("window.ert && window.ert.etikedo(ŝlosilo, valoro)")

private fun sentryPontoKonsento(aktiva: Boolean): Unit =
    js("window.ert && window.ert.konsento(aktiva)")
