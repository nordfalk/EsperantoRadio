package dk.nordfalk.esperanto

import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.SentryOptions

/**
 * Sentry-agordo — komuna por ĉiuj platformoj (Android, Desktop/JVM, wasmJs).
 *
 * Sur wasmJs la SDK estas no-op (kompilas sed faras nenion).
 * Sur Android ĝi uzas Sentry Android SDK, sur JVM Sentry Java SDK.
 *
 * @param medio "produktado" aŭ "evoluo" — regas la medion kaj la specimenajn
 *   kvocientojn (traces: 1.0/0.1 de PR #93; eraroj: 1.0/0.5 de PR #109 — la
 *   eraroj-specimenado rekte protektas la monatan eraroj-kvoton).
 *   La PROTOKOLA kvanto ne dependas de la medio: logs estas ĉiam ŝaltitaj,
 *   limigitaj per la agordo "Ŝaltu detalan protokoladon" ([protokoloDetala]
 *   en ProtokoloUtil; 2026-10-13 defaŭlte ŝaltita).
 */
fun initialiguSentry(medio: String = "evoluo") {
    Sentry.init { options: SentryOptions ->
        options.dsn = "https://764998823a401d936a5e227d1453951e@o4512078664564736.ingest.de.sentry.io/4512078868185168"

        // Logs (Sentry.logger.*) ĉiam ŝaltitaj — ankaŭ en produktado. La kvanto
        // estas limigita per la kodo: defaŭlte nur eraroj kaj gravaj eventoj
        // (logGrava); la ceteraj logi-vokoj nur se "detala protokolado" estas
        // ŝaltita (protokoloDetala en ProtokoloUtil, 2026-10-12).
        options.logs.enabled = true
        // Kaptu 100 % da traktadoj nur en evoluo; 10 % en produktado (PR #93)
        options.tracesSampleRate = if (medio == "produktado") 0.1 else 1.0
        // Erar-specimenado: NENIU (100 %) ekde 2026-11-25, decido de la posedanto —
        // la kvoto estas trankvila (~200 eventoj/monato ≈ 4 % de 5 000; la krizo
        // de 2026-10-13, 4 004/5 000, ne revenis — ĝin bremsis G46 kaj la
        // F-Droid-opt-in, ne la specimenado). Historio: 50 % en produktado de PR #109.
        options.sampleRate = 1.0
        // Montru kion la SDK faras dum provado
        // options.debug = true
        options.attachThreads = false
        // Identigas la version en Sentry — ebligas spuradon de regreso inter versioj
        options.release = "esperantoradio@${ApoVersio.VERSION}"
        // Disigas evoluon de produktado en la Sentry-fasado
        options.environment = medio
    }

    // Globaj etikedoj — apearas ĉe ĉiuj eventoj kaj estas filtreblaj en Sentry
    Sentry.configureScope { scope ->
        scope.setTag("apo", "EsperantoRadio")
    }
}

/**
 * Agordas Sentry-etikedon (tag) sur la aktuala amplekso.
 * Etikedoj estas filtreblaj en la Sentry-fasado.
 */
fun agorduSentryEtikedon(ŝlosilo: String, valoro: String) {
    Sentry.configureScope { scope -> scope.setTag(ŝlosilo, valoro) }
    // wasmJs: la KMP-SDK estas no-op — la etikedo iras tra la JS-ponto
    sentryEtikeduPlatforme(ŝlosilo, valoro)
}

/**
 * Agordas Sentry-kuntekston (context) sur la aktuala amplekso.
 * Kunteksto provizas strukturitajn datumojn kiuj aperas ĉe eventoj.
 */
fun agorduSentryKuntekston(ŝlosilo: String, valoro: Any) {
    Sentry.configureScope { scope -> scope.setContext(ŝlosilo, valoro) }
}

/**
 * Testa funkcio — jetas escepton por kontroli ke Sentry funkcias.
 */
fun testuSentry() {
    try {
        throw Exception("Sentry-testo: ĉi tio estas prova eraro.")
    } catch (e: Exception) {
        loge("Sentry", "Testa escepto kaptita", e)
    }
}
