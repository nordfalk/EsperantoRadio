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

actual fun malfermuRetposhton(retposhto: String, temo: String, teksto: String) {
    logi("Ligilo", "Malfermas retpoŝton al: $retposhto")
    try {
        val url = "mailto:$retposhto?subject=${kodiguUriKomponanton(temo)}&body=${kodiguUriKomponanton(teksto)}"
        kotlinx.browser.window.open(url, "_blank")
    } catch (e: Throwable) {
        logw("Ligilo", "Ne povas malfermi retpoŝto-programon", e)
    }
}
