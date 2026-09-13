package dk.nordfalk.esperanto.data.repository

/**
 * Platform-specifaj funkcioj por legi/skribi krudan tekston al diskkaŝmemoro.
 * Uzata por konservi krudajn RSS-respondojn, nomitajn laŭ kanalo-slug.
 *
 * - Android: `appContext.cacheDir/elsendoj_kasho/`
 * - Desktop: `~/.esperantoradio/cache/`
 * - wasmJs/iOS: no-op (revenigas null / faras nenion)
 */
expect fun leguKashon(nomo: String): String?
expect fun skribuKashon(nomo: String, enhavo: String)
