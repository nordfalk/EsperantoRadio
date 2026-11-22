package dk.nordfalk.esperanto.ui

import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.logw

/** JavaScript `encodeURIComponent` — en Kotlin/Wasm `js(...)` devas esti la tuta korpo de supra-nivela funkcio. */
private fun kodiguUriKomponanton(teksto: String): String = js("encodeURIComponent(teksto)")

actual fun malfermuLigon(url: String) {
    logi("Ligilo", "Malfermas: $url")
    try {
        kotlinx.browser.window.open(url, "_blank")
    } catch (e: Throwable) {
        logw("Ligilo", "Ne povas malfermi ligilon: $url", e)
    }
}

actual fun malfermuRetpoŝton(retpoŝto: String, temo: String, teksto: String) {
    logi("Ligilo", "Malfermas retpoŝton al: $retpoŝto")
    try {
        val url = "mailto:$retpoŝto?subject=${kodiguUriKomponanton(temo)}&body=${kodiguUriKomponanton(teksto)}"
        kotlinx.browser.window.open(url, "_blank")
    } catch (e: Throwable) {
        logw("Ligilo", "Ne povas malfermi retpoŝto-programon", e)
    }
}

actual fun kopiuTekston(teksto: String) {
    try {
        kotlinx.browser.window.navigator.clipboard.writeText(teksto).catch { e: JsAny? ->
            logw("Ligilo", "Ne povas kopii en la poŝo: $e")
            null
        }
        logi("Ligilo", "Teksto kopiita en la poŝo (${teksto.length} signoj)")
    } catch (e: Throwable) {
        logw("Ligilo", "Ne povas kopii en la poŝo", e)
    }
}

actual fun kunhaviguLigilon(url: String, teksto: String) {
    // sur la web: malfermu novan fenestron kun la RSS
    try {
        kotlinx.browser.window.open(url, "_blank")
    } catch (e: Throwable) {
        logw("Ligilo", "Ne povas malfermi novan fenestron: $url", e)
    }
}
