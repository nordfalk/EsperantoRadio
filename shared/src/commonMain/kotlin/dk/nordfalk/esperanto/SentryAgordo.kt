package dk.nordfalk.esperanto

import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.SentryOptions

/**
 * Sentry-agordo — komuna por cxiuj platformoj (Android, Desktop/JVM, wasmJs).
 *
 * Sur wasmJs la SDK estas no-op (kompilas sed faras nenion).
 * Sur Android gxi uzas Sentry Android SDK, sur JVM Sentry Java SDK.
 */
fun initialiguSentry() {
    Sentry.init { options: SentryOptions ->
        options.dsn = "https://764998823a401d936a5e227d1453951e@o4512078664564736.ingest.de.sentry.io/4512078868185168"
        // Kaptu 100% de traktadoj por spurado (agordu malpli en produktado)
        options.tracesSampleRate = 1.0
        // Montru kion la SDK faras dum provado
        options.debug = true
    }
}

/**
 * Testa funkcio — jetas escepton por kontroli ke Sentry funkcias.
 */
fun testuSentry() {
    try {
        throw Exception("Sentry-testo: cxi tio estas prova eraro.")
    } catch (e: Exception) {
        Sentry.captureException(e)
    }
}
