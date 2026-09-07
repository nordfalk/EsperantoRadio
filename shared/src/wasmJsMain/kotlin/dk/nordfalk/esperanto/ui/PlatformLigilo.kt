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
        val encode = js("encodeURIComponent").unsafeCast<String>()
        val url = "mailto:$retposhto?subject=${encode(temo)}&body=${encode(teksto)}"
        kotlinx.browser.window.open(url, "_blank")
    } catch (e: dynamic) {
        dk.nordfalk.esperanto.logw("Ligilo", "Ne povas malfermi retpoŝto-programon", e as Throwable)
    }
}
