package dk.nordfalk.esperanto.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import dk.nordfalk.esperanto.data.config.appContext
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.logw

actual val platformo: Platformo = Platformo.ANDROIDO

actual fun malfermuLigon(url: String) {
    logi("Ligilo", "Malfermas: $url")
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
    } catch (e: Exception) {
        logw("Ligilo", "Ne povas malfermi ligilon: $url", e)
    }
}

actual fun malfermuRetpoŝton(retpoŝto: String, temo: String, teksto: String) {
    logi("Ligilo", "Malfermas retpoŝton al: $retpoŝto")
    try {
        // Enkodu temon kaj tekston en la mailto-URI — pli fidinde ol EXTRA_* kiujn
        // multaj retpoŝto-programoj ignoras ĉe ACTION_SENDTO.
        val uri = Uri.parse(
            "mailto:" + Uri.encode(retpoŝto) +
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

actual fun kopiuTekston(teksto: String) {
    try {
        val poŝo = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        poŝo.setPrimaryClip(ClipData.newPlainText("EsperantoRadio", teksto))
        logi("Ligilo", "Teksto kopiita en la poŝo (${teksto.length} signoj)")
    } catch (e: Exception) {
        logw("Ligilo", "Ne povas kopii en la poŝo", e)
    }
}

actual fun kunhaviguLigilon(url: String, teksto: String) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "$teksto\n$url")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        appContext.startActivity(Intent.createChooser(intent, teksto).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        logi("Ligilo", "Disdividas: $url")
    } catch (e: Exception) {
        logw("Ligilo", "Ne povas disdividi: $url", e)
    }
}
