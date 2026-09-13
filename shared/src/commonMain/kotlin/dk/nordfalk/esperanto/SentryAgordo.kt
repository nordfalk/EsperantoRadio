package dk.nordfalk.esperanto

import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.SentryLevel
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
        // Identigas la version en Sentry — ebligas spuradon de regreso inter versioj
        options.release = "esperantoradio@0.1"
        // Disigas evoluon de produktado en la Sentry-fasado
        options.environment = "evoluo"
    }

    // Globaj etikedoj — apearas cxe cxiuj eventoj kaj estas filtreblaj en Sentry
    Sentry.configureScope { scope ->
        scope.setTag("platformo", platformNomo)
        scope.setTag("apo", "EsperantoRadio")
    }
}

/**
 * Kaptas mesaĝon en Sentry kun specifa nivelo kaj laŭvola etikedo.
 * Por gravaj ne-eraraj eventoj (ekz. "RSS-malsukcesa, uzas kaŝenitan datumon").
 *
 * @param mesagxo la mesaĝo
 * @param nivelo rangigo (INFO, WARNING, ERROR, ktp.)
 * @param etikedo laŭvola paro (ŝlosilo, valoro) por filtri en Sentry
 */
fun kaptuSentryMesagxon(
    mesagxo: String,
    nivelo: SentryLevel = SentryLevel.INFO,
    etikedo: Pair<String, String>? = null,
) {
    Sentry.captureMessage(mesagxo) { scope ->
        scope.level = nivelo
        if (etikedo != null) scope.setTag(etikedo.first, etikedo.second)
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
