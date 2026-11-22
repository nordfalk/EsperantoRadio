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

actual fun malfermuRetpoŝton(retpoŝto: String, temo: String, teksto: String) {
    logi("Ligilo", "Malfermas retpoŝton al: $retpoŝto")
    try {
        val uri = URI("mailto:$retpoŝto?subject=${java.net.URLEncoder.encode(temo, "UTF-8")}&body=${java.net.URLEncoder.encode(teksto, "UTF-8")}")
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().mail(uri)
        }
    } catch (e: Exception) {
        logw("Ligilo", "Ne povas malfermi retpoŝto-programon", e)
    }
}

actual fun kopiuTekston(teksto: String) {
    try {
        val poŝo = java.awt.Toolkit.getDefaultToolkit().systemClipboard
        poŝo.setContents(java.awt.datatransfer.StringSelection(teksto), null)
        logi("Ligilo", "Teksto kopiita en la poŝo (${teksto.length} signoj)")
    } catch (e: Exception) {
        logw("Ligilo", "Ne povas kopii en la poŝo", e)
    }
}
