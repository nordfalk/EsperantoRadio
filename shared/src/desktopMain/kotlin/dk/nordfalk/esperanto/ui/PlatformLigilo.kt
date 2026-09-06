package dk.nordfalk.esperanto.ui

import java.awt.Desktop
import java.net.URI
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.logw

actual fun malfermuLigon(url: String) {
    logi("Ligilo", "Malfermas: $url")
    try {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI(url))
        }
    } catch (e: Exception) {
        logw("Ligilo", "Ne povas malfermi ligilon: $url", e)
    }
}

actual fun malfermuRetposhton(retposhto: String, temo: String, teksto: String) {
    logi("Ligilo", "Malfermas retpoŝton al: $retposhto")
    try {
        val uri = URI("mailto:$retposhto?subject=${java.net.URLEncoder.encode(temo, "UTF-8")}&body=${java.net.URLEncoder.encode(teksto, "UTF-8")}")
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().mail(uri)
        }
    } catch (e: Exception) {
        logw("Ligilo", "Ne povas malfermi retpoŝto-programon", e)
    }
}
