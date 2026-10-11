package dk.nordfalk.esperanto.android

import android.app.Application
import dk.nordfalk.esperanto.data.config.appContext
import dk.nordfalk.esperanto.initialiguSentry

/**
 * Procez-nivela inicialigo ((PR #89) + C7).
 *
 * `appContext` kaj Sentry estas agordataj ĉi tie — ĈIU procezo de la apo
 * (Activity, Service, Receiver, Worker) rulas `Application.onCreate` antaŭ
 * iu ajn komponanto. Sen tio fonprocezoj (WorkManager, riceviloj) ne havas
 * kuntekston. La inicialigo estas unufoje por la tuta procezo — ne en ĉiu
 * `MainActivity.onCreate` (Sentry avertas pri duobla inicialigo ĉe Activity-rekreo).
 */
class EsperantoRadioAplikajho : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        // Medio laŭ la APLIKAĵO-flago (PR #93): debug-versio → "evoluo",
        // kunfandita release → "produktado" (kun 10 % specimenado kaj malpli da protokoloj).
        val estasEvolua = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        initialiguSentry(if (estasEvolua) "evoluo" else "produktado")
    }
}
