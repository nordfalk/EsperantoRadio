package dk.nordfalk.esperanto.data.config

import java.io.InputStream

actual fun leguPakititanKanalkonfiguron(): String {
    val fluo: InputStream = Thread.currentThread().contextClassLoader
        ?.getResourceAsStream("esperantoradio_konfiguro.json")
        ?: error("Kanalkonfiguro ne trovita en resurcoj")
    return fluo.bufferedReader().use { it.readText() }
}
