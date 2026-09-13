package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.data.config.appContext
import dk.nordfalk.esperanto.logd
import dk.nordfalk.esperanto.loge
import java.io.File

/**
 * Android: konservas kaŝdosierojn en appContext.cacheDir/elsendoj_kasho/.
 */
actual fun leguKashon(nomo: String): String? {
    return try {
        val dosiero = File(appContext.cacheDir, "elsendoj_kasho/$nomo.rss")
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
        val dosiero = File(appContext.cacheDir, "elsendoj_kasho/$nomo.rss")
        dosiero.parentFile?.mkdirs()
        dosiero.writeText(enhavo)
        logd("DosierKasho", "Skribitas: $nomo (${enhavo.length} signoj)")
    } catch (e: Exception) {
        loge("DosierKasho", "Malsukcesis skribi kaŝon: $nomo", e)
    }
}
