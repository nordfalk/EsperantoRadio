package dk.nordfalk.esperanto.data.config

/**
 * Legas la bundled kanalkonfiguron (JSONC) el la platformo-specifa resurco.
 * expect — actual implementoj estas en androidMain, jvmMain, iosMain, wasmJsMain.
 */
expect fun leguBundledKanalkonfiguron(): String
