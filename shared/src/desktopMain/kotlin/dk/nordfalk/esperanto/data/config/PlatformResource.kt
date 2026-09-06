package dk.nordfalk.esperanto.data.config

import java.io.InputStream

actual fun leguBundledKanalkonfiguron(): String {
    val fluo: InputStream = Thread.currentThread().contextClassLoader
        ?.getResourceAsStream("esperantoradio_kanaloj_v9.json")
        ?: error("Kanalkonfiguro ne trovita en resurcoj")
    return fluo.bufferedReader().use { it.readText() }
}
