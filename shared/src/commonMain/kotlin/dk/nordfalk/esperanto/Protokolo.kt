package dk.nordfalk.esperanto

/**
 * Platform-specifaj protokolaj funkcioj (expect/actual).
 *
 * Cxiu platformo eligas per sia propra mekanismo:
 * - Android: android.util.Log
 * - Desktop/JVM: System.err.println
 * - wasmJs: println (console.log)
 * - iOS: NSLog
 *
 * La komunaj logi/logd/logw/loge funkcioj (kun Sentry-breadcrumb-oj)
 * estas en ProtokoloUtil.kt.
 *
 * Konvencio: [Komponanto] mesagxo
 * Niveloj: d = debug, i = info, w = warning, e = error
 */
expect fun platformLogi(tag: String, msg: String)
expect fun platformLogd(tag: String, msg: String)
expect fun platformLogw(tag: String, msg: String)
expect fun platformLoge(tag: String, msg: String)
