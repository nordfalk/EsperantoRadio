package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.logd
import dk.nordfalk.esperanto.loge
import java.io.File

/**
 * Desktop (JVM): konservas kaŝdosierojn en ~/.esperantoradio/cache/.
 * [dosierKashoBazo] estas interne ŝanĝebla por testoj.
 */
internal var dosierKashoBazo: String =
    File(System.getProperty("user.home"), ".esperantoradio/cache").absolutePath

actual fun leguKashon(nomo: String): String? {
    return try {
        val dosiero = File(dosierKashoBazo, "$nomo.rss")
        if (!dosiero.exists()) return null
        logd("DosierKasho", "Legitas: $nomo (${dosiero.length()} bajtoj)")
        dosiero.readText()
    } catch (e: Exception) {
        loge("DosierKasho", "Malsukcesis legi kaŝon: $nomo", e)
        null
    }
}

actual fun skribuKashon(nomo: String, enhavo: String) {
    try {
        val dosiero = File(dosierKashoBazo, "$nomo.rss")
        dosiero.parentFile?.mkdirs()
        dosiero.writeText(enhavo)
        logd("DosierKasho", "Skribitas: $nomo (${enhavo.length} signoj)")
    } catch (e: Exception) {
        loge("DosierKasho", "Malsukcesis skribi kaŝon: $nomo", e)
    }
}
