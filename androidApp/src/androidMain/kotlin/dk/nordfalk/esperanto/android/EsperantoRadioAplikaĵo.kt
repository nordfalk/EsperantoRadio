package dk.nordfalk.esperanto.android

import android.app.Application
import android.os.Build
import dk.nordfalk.esperanto.AppStato
import dk.nordfalk.esperanto.data.config.appContext
import dk.nordfalk.esperanto.data.config.leguErarRaportadanKonsenton
import dk.nordfalk.esperanto.initialiguSentry
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.raportadoAktiva

/**
 * Procez-nivela inicialigo ((PR #89) + C7).
 *
 * `appContext` estas agordata ĉi tie — ĈIU procezo de la apo
 * (Activity, Service, Receiver, Worker) rulas `Application.onCreate` antaŭ
 * iu ajn komponanto. Sen tio fonprocezoj (WorkManager, riceviloj) ne havas
 * kuntekston. La inicialigo estas unufoje por la tuta procezo — ne en ĉiu
 * `MainActivity.onCreate` (duobla inicialigo ĉe Activity-rekreo).
 *
 * Sentry estas inicialigata NUR por ne-debug konstruoj sur realaj aparatoj
 * KAJ nur kiam la erar-raportado estas ŝaltita (agordo "erar_raportado", PR
 * #148/C50). La DEFAŬLTO dependas de la konstruo: kun nia subskribo (Google
 * Play, Aptoide — la keystore ekzistis dum la konstruo) ĝi estas ŜALTITA
 * (neniu demando; malŝaltebla en Agordoj → Privateco); sen nia subskribo
 * (tiel F-Droid konstruas) MALŜALTITA kun unufoja konsento-demando — alie
 * F-Droid atribuus la kontraŭecon "Tracking". Sen inicialigo ĉiuj
 * Sentry-vokoj estas no-op.
 */
class EsperantoRadioAplikaĵo : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        val estasDebug = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        // Evolua medio (debug-konstruo aŭ emulilo): ekz. la "Apaj novaĵoj"-
        // dialogo ne montriĝu — ĝi aperus ĉe ĉiu freŝa instalaĵo
        AppStato.estasEvoluaMedo = estasDebug || estasEmulilo()
        // Konstruo-specifa defaŭlto de la erar-raportado (vidu la klasan KDoc-on)
        AppStato.erarRaportadoDefaŭlta = !BuildConfig.SEN_NIA_SUBSKRIBO
        if (AppStato.estasEvoluaMedo) {
            logi("Sentry", "Ne raportas al Sentry (${if (estasDebug) "debug-konstruo" else "emulilo"})")
            return
        }
        if (leguErarRaportadanKonsenton()) {
            initialiguSentry("produktado")
            raportadoAktiva = true
        } else {
            logi("Sentry", "Erar-raportado malŝaltita — ne raportas")
        }
    }

    /** La kutimaj Android-emulilaj bildoj: AVD (sdk_gphone, goldfish, ranchu), Genymotion k.a. */
    private fun estasEmulilo(): Boolean =
        Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.contains("emulator") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK built for") ||
            Build.MODEL.contains("sdk_gphone") ||
            Build.MANUFACTURER.contains("Genymotion") ||
            (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
            Build.HARDWARE.contains("goldfish") ||
            Build.HARDWARE.contains("ranchu")
}
