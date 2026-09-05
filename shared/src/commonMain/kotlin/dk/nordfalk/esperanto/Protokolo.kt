package dk.nordfalk.esperanto

/**
 * Simpla protokolilo (logilo) por la tuta apo.
 *
 * Uzas expect/actual por println — sur Desktop/JVM tio estas System.err.println,
 * sur Android android.util.Log, sur wasmJs console.log.
 *
 * Konvencio: [Komponanto] mesaĝo
 * Niveloj: d = debug, i = info, w = warning, e = error
 */
expect fun logi(tag: String, msg: String)
expect fun logd(tag: String, msg: String)
expect fun logw(tag: String, msg: String)
expect fun loge(tag: String, msg: String)
