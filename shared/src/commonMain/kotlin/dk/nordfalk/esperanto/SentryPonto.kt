package dk.nordfalk.esperanto

/**
 * Platformo-preno al la malproksima erar-raportado preter la KMP-SDK.
 *
 * Sur wasmJs la KMP-Sentry-SDK estas **no-op** (ĝia artefakto ne ekzistas por
 * wasmJs), sed la reta apo ŝargas la Sentry-JavaScript-SDK-n en sia
 * `index.html`. Ĉi tiuj `expect`-funkcioj portas la protokolojn, la
 * esceptojn, la etikedojn kaj la konsento-flagon al la retumila SDK:
 *
 * - **wasmJs**: transdonas al la `window.ert`-ponto (`SentryPonto.wasmJs.kt`);
 * - **ĉiuj aliaj platformoj**: no-op — la KMP-SDK mem raportas
 *   (`SentryPontoPlatforme.kt`).
 *
 * La alirrajton ("raportadoAktiva") ĉiam kontrolas la alvokanto (ProtokoloUtil)
 * — la ponto mem nur plenumas.
 */
internal expect fun sentryProtokoluPlatforme(nivelo: String, teksto: String)

/**
 * Vera escepto — error-evento, kiu konsumas la monatan erarojn-kvoton
 * (spegulo de `Sentry.captureException` sur wasmJs).
 */
internal expect fun sentryKaptuEsceptonPlatforme(teksto: String, staktraco: String)

/** Etikedo (tag) sur ĉiuj eventoj — ekz. la pseŭdonimo kiel "uzanto". */
internal expect fun sentryEtikeduPlatforme(ŝlosilo: String, valoro: String)

/**
 * Sinkronigas la konsento-flagon al la platformo. Necesa nur sur wasmJs,
 * kie la JS-flago `window.erarRaportadoAktiva` ankaŭ regas la sendependajn
 * JS-erarojn (window.onerror) per `beforeSend`.
 */
internal expect fun sinkroniguRaportadanKonsentonPlatforme(aktiva: Boolean)
