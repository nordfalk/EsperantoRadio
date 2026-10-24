package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.data.config.appContext
import dk.nordfalk.esperanto.logd
import dk.nordfalk.esperanto.loge
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.logw
import java.io.File

/**
 * Android: konservas kaŝdosierojn en appContext.cacheDir/elsendoj_kasho/.
 */
actual fun leguKaŝon(nomo: String): String? {
    return try {
        val dosiero = File(appContext.cacheDir, "elsendoj_kasho/$nomo.rss")
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
        val dosiero = File(appContext.cacheDir, "elsendoj_kasho/$nomo.rss")
        dosiero.parentFile?.mkdirs()
        dosiero.writeText(enhavo)
        logd("DosierKaŝo", "Skribitas: $nomo (${enhavo.length} signoj)")
    } catch (e: Exception) {
        loge("DosierKaŝo", "Malsukcesis skribi kaŝon: $nomo", e)
    }
}

actual fun foriguKaŝon(nomo: String) {
    try {
        val dosiero = File(appContext.cacheDir, "elsendoj_kasho/$nomo.rss")
        if (dosiero.exists() && dosiero.delete()) {
            logi("DosierKaŝo", "Forigis: $nomo")
        }
    } catch (e: Exception) {
        logw("DosierKaŝo", "Malsukcesis forigi kaŝon: $nomo", e)
    }
}
