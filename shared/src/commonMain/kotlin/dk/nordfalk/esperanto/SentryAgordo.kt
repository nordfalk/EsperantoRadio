package dk.nordfalk.esperanto

import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.SentryOptions

/**
 * Sentry-agordo — komuna por cxiuj platformoj (Android, Desktop/JVM, wasmJs).
 *
 * Sur wasmJs la SDK estas no-op (kompilas sed faras nenion).
 * Sur Android gxi uzas Sentry Android SDK, sur JVM Sentry Java SDK.
 *
 * @param medio "produktado" aux "evoluo" — regas la medion kaj la specimenajn
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
        // Erar-specimenado: en produktado nur 50 % — la monata eraroj-kvoto
        // estis preskaŭ elĉerpita (4 004/5 000, 2026-10-13). Sentry grupigas la
        // eventojn al problemoj, do la grupoj plu formiĝas; nur la nombroj
        // malpreciziĝas.
        options.sampleRate = if (medio == "produktado") 0.5 else 1.0
        // Montru kion la SDK faras dum provado
        // options.debug = true
        options.attachThreads = false
        // Identigas la version en Sentry — ebligas spuradon de regreso inter versioj
        options.release = "esperantoradio@${ApoVersio.VERSION}"
        // Disigas evoluon de produktado en la Sentry-fasado
        options.environment = medio
    }

    // Globaj etikedoj — apearas cxe cxiuj eventoj kaj estas filtreblaj en Sentry
    Sentry.configureScope { scope ->
        scope.setTag("apo", "EsperantoRadio")
    }
}

/**
 * Agordas Sentry-etikedon (tag) sur la aktuala amplekso.
 * Etikedoj estas filtreblaj en la Sentry-fasado.
 */
fun agorduSentryEtikedon(sxlosilo: String, valoro: String) {
    Sentry.configureScope { scope -> scope.setTag(sxlosilo, valoro) }
}

/**
 * Agordas Sentry-kuntekston (context) sur la aktuala amplekso.
 * Kunteksto provizas strukturitajn datumojn kiuj aperas cxe eventoj.
 */
fun agorduSentryKuntekston(sxlosilo: String, valoro: Any) {
    Sentry.configureScope { scope -> scope.setContext(sxlosilo, valoro) }
}

/**
 * Testa funkcio — jetas escepton por kontroli ke Sentry funkcias.
 */
fun testuSentry() {
    try {
        throw Exception("Sentry-testo: cxi tio estas prova eraro.")
    } catch (e: Exception) {
        loge("Sentry", "Testa escepto kaptita", e)
    }
}
