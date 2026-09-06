package dk.nordfalk.esperanto.ui

import dk.nordfalk.esperanto.logi

actual fun malfermuLigon(url: String) {
    logi("Ligilo", "Malfermas: $url")
    try {
        kotlinx.browser.window.open(url, "_blank")
    } catch (e: dynamic) {
        dk.nordfalk.esperanto.logw("Ligilo", "Ne povas malfermi ligilon: $url", e as Throwable)
    }
}

actual fun malfermuRetposhton(retposhto: String, temo: String, teksto: String) {
    logi("Ligilo", "Malfermas retpoŝton al: $retposhto")
    try {
        val url = "mailto:$retposhto?subject=${js("encodeURIComponent").unsafeCast<String>()(temo)}&body=${js("encodeURIComponent").unsafeCast<String>()}(teksto)"
        kotlinx.browser.window.open(url, "_blank")
    } catch (e: dynamic) {
        dk.nordfalk.esperanto.logw("Ligilo", "Ne povas malfermi retpoŝto-programon", e as Throwable)
    }
}
