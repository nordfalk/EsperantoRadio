package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.data.config.appContext
import dk.nordfalk.esperanto.logd
import dk.nordfalk.esperanto.loge
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.logw
import java.io.File

/**
 * Android: konservas kaŝdosierojn en appContext.cacheDir/elsendoj_kasho/.
 * `.rss` = kruda RSS-respondo, `.json` = jam parsitaj elsendoj (rapida starto).
 */
private fun kaŝDosiero(nomo: String, sufikso: String) =
    File(appContext.cacheDir, "elsendoj_kasho/$nomo$sufikso")

private fun legu(nomo: String, sufikso: String): String? {
    return try {
        val dosiero = kaŝDosiero(nomo, sufikso)
        if (!dosiero.exists()) return null
        logd("DosierKaŝo", "Legitas: $nomo$sufikso (${dosiero.length()} bitokoj)")
        dosiero.readText()
    } catch (e: Exception) {
        loge("DosierKaŝo", "Malsukcesis legi kaŝon: $nomo$sufikso", e)
        null
    }
}

private fun skribu(nomo: String, enhavo: String, sufikso: String) {
    try {
        val dosiero = kaŝDosiero(nomo, sufikso)
        dosiero.parentFile?.mkdirs()
        dosiero.writeText(enhavo)
        logd("DosierKaŝo", "Skribitas: $nomo$sufikso (${enhavo.length} signoj)")
    } catch (e: Exception) {
        loge("DosierKaŝo", "Malsukcesis skribi kaŝon: $nomo$sufikso", e)
    }
}

actual fun leguKaŝon(nomo: String): String? = legu(nomo, ".rss")
actual fun skribuKaŝon(nomo: String, enhavo: String) = skribu(nomo, enhavo, ".rss")
actual fun leguKaŝonJson(nomo: String): String? = legu(nomo, ".json")
actual fun skribuKaŝonJson(nomo: String, enhavo: String) = skribu(nomo, enhavo, ".json")

actual fun foriguKaŝon(nomo: String) {
    for (sufikso in listOf(".rss", ".json")) {
        try {
            val dosiero = kaŝDosiero(nomo, sufikso)
            if (dosiero.exists() && dosiero.delete()) {
                logi("DosierKaŝo", "Forigis: $nomo$sufikso")
            }
        } catch (e: Exception) {
            logw("DosierKaŝo", "Malsukcesis forigi kaŝon: $nomo$sufikso", e)
        }
    }
}
