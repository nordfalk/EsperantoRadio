package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.logd
import dk.nordfalk.esperanto.loge
import dk.nordfalk.esperanto.logw
import java.io.File

/**
 * Desktop (JVM): konservas kaŝdosierojn en ~/.esperantoradio/cache/.
 * [dosierKaŝoBazo] estas interne ŝanĝebla por testoj.
 */
internal var dosierKaŝoBazo: String =
    File(System.getProperty("user.home"), ".esperantoradio/cache").absolutePath

actual fun leguKaŝon(nomo: String): String? {
    return try {
        val dosiero = File(dosierKaŝoBazo, "$nomo.rss")
        if (!dosiero.exists()) return null
        logd("DosierKaŝo", "Legitas: $nomo (${dosiero.length()} bajtoj)")
        dosiero.readText()
    } catch (e: Exception) {
        loge("DosierKaŝo", "Malsukcesis legi kaŝon: $nomo", e)
        null
    }
}

actual fun skribuKaŝon(nomo: String, enhavo: String) {
    try {
        val dosiero = File(dosierKaŝoBazo, "$nomo.rss")
        dosiero.parentFile?.mkdirs()
        dosiero.writeText(enhavo)
        logd("DosierKaŝo", "Skribitas: $nomo (${enhavo.length} signoj)")
    } catch (e: Exception) {
        loge("DosierKaŝo", "Malsukcesis skribi kaŝon: $nomo", e)
    }
}

actual fun foriguKaŝon(nomo: String) {
    try {
        val dosiero = File(dosierKaŝoBazo, "$nomo.rss")
        if (dosiero.exists() && dosiero.delete()) {
            logd("DosierKaŝo", "Forigis: $nomo")
        }
    } catch (e: Exception) {
        logw("DosierKaŝo", "Malsukcesis forigi kaŝon: $nomo", e)
    }
}
