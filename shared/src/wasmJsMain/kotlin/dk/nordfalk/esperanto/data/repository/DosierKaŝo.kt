package dk.nordfalk.esperanto.data.repository

/**
 * wasmJs: no-op — neniu diska kaŝmemoro havebla.
 */
actual fun leguKaŝon(nomo: String): String? = null
actual fun skribuKaŝon(nomo: String, enhavo: String) {}
actual fun foriguKaŝon(nomo: String) {}
