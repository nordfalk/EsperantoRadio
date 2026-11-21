package dk.nordfalk.esperanto.data.repository

/**
 * Platform-specifaj funkcioj por legi/skribi krudan tekston al diska kaŝmemoro.
 * Uzata por konservi krudajn RSS-respondojn, nomitajn laŭ kanalo-slug.
 *
 * - Android: `appContext.cacheDir/elsendoj_kasho/`
 * - Desktop: `~/.esperantoradio/cache/`
 * - wasmJs/iOS: no-op (revenigas null / faras nenion)
 */
expect fun leguKaŝon(nomo: String): String?
expect fun skribuKaŝon(nomo: String, enhavo: String)

/**
 * JSON-kaŝo de jam PARSITAJ elsendoj (`<nomo>.json` anstataŭ `<nomo>.rss`):
 * la sekva starto malkodas ĝin rekte — ~10× pli rapide ol re-parsi la krudan
 * RSS per Ksoup (mezurite 2026-11-21: peranto 250 eroj — parse 714 ms, JSON-decode 69 ms).
 */
expect fun leguKaŝonJson(nomo: String): String?
expect fun skribuKaŝonJson(nomo: String, enhavo: String)

/**
 * Forigas kaŝdosieron — ekz. fiaskpaĝon de kaptiva portalo kaptitan de antaŭa
 * versio (KMP-1S, 2026-10-13), alikaze la malpura kaŝo ĵetus eraron je ĉiu
 * apomfermo. Sendanĝera se la dosiero ne ekzistas.
 */
expect fun foriguKaŝon(nomo: String)
