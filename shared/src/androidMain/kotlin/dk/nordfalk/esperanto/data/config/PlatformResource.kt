package dk.nordfalk.esperanto.data.config

actual fun leguBundledKanalkonfiguron(): String {
    // TODO: Legi el Android res/raw
    // Provizore legu per la sama klaso-ŝarĝilo
    val stream = Thread.currentThread().contextClassLoader
        ?.getResourceAsStream("esperantoradio_kanaloj_v9.json")
        ?: error("Kanalkonfiguro ne trovita en resurcoj")
    return stream.bufferedReader().use { it.readText() }
}
