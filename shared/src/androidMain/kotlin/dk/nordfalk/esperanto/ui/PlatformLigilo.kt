package dk.nordfalk.esperanto.ui

import android.content.Intent
import android.net.Uri
import dk.nordfalk.esperanto.data.config.appContext
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.logw

actual fun malfermuLigon(url: String) {
    logi("Ligilo", "Malfermas: $url")
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
    } catch (e: Exception) {
        logw("Ligilo", "Ne povas malfermi ligilon: $url", e)
    }
}

actual fun malfermuRetposhton(retposhto: String, temo: String, teksto: String) {
    logi("Ligilo", "Malfermas retpoŝton al: $retposhto")
    try {
        // Enkodu temon kaj tekston en la mailto-URI — pli fidinde ol EXTRA_* kiujn
        // multaj retpoŝto-programoj ignoras ĉe ACTION_SENDTO.
        val uri = Uri.parse(
            "mailto:" + Uri.encode(retposhto) +
            "?subject=" + Uri.encode(temo) +
            "&body=" + Uri.encode(teksto)
        )
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra(Intent.EXTRA_SUBJECT, temo)
            putExtra(Intent.EXTRA_TEXT, teksto)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        appContext.startActivity(intent)
    } catch (e: Exception) {
        logw("Ligilo", "Ne povas malfermi retpoŝto-programon", e)
    }
}
