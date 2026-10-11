package dk.nordfalk.esperanto.android

import android.app.Application
import dk.nordfalk.esperanto.data.config.appContext
import dk.nordfalk.esperanto.initialiguSentry

/**
 * Procez-nivela inicialigo (FAROTA K2 + C7).
 *
 * Antaŭe `appContext` kaj Sentry estis agordataj nur en `MainActivity` — sed en
 * procezo lanĉita de WorkManager (sciigoj pri novaj elsendoj, la normala kazo por
 * la perioda laboro je 7:00/16:00) la apo tiam ne havis kuntekston:
 * `sciigPermesoDonita()` trafis neprapezalan `lateinit`-kampon → `UninitializedPropertyAccessException`
 * → senfina `Result.retry()`-ciklo (baterio-dreno, API 33+), aŭ `kreuSettings()` reeniris
 * al `NoOpSettings` → silenta nenio-faro (API 26-32).
 *
 * `Application.onCreate` ruliĝas en ĈIU procezo de la apo antaŭ ĉiu komponanto
 * (Activity, Service, Receiver, Worker).
 */
class EsperantoRadioAplikajho : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        // Unufoje por la tuta procezo — ne en ĉiu MainActivity.onCreate
        // (Sentry avertas pri duobla inicialigo ĉe Activity-rekreo)
        initialiguSentry()
    }
}
