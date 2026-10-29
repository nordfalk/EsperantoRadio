package dk.nordfalk.esperanto.data.config

import android.content.Context

/**
 * Statika Context — devas esti agordita frue (ekz. en MainActivity.onCreate).
 * Malpura sed rapida solvo. Pli poste: transdoni kiel parametro.
 */
lateinit var appContext: Context

actual fun leguPakititanKanalkonfiguron(): String {
    return appContext.assets.open("esperantoradio_konfiguro.json")
        .bufferedReader().use { it.readText() }
}
