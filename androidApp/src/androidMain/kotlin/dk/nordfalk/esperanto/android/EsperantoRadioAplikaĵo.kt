package dk.nordfalk.esperanto.android

import android.app.Application
import android.os.Build
import dk.nordfalk.esperanto.AppStato
import dk.nordfalk.esperanto.data.config.appContext
import dk.nordfalk.esperanto.data.config.erarRaportadaDefaŭltoLaŭInstalanto
import dk.nordfalk.esperanto.data.config.leguErarRaportadanKonsenton
import dk.nordfalk.esperanto.initialiguSentry
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.logw
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
 * #148/C50). La DEFAŬLTO dependas de la INSTAL-FONTO de la APK
 * ([erarRaportadaDefaŭltoLaŭInstalanto], 2026-11-24; decido de la posedanto:
 * nur KONATAJ ne-F-Droid-kanaloj raportas defaŭlte): instalaĵoj el la
 * konataj kanaloj (Google Play, la sistema surŝutilo) raportas defaŭlte
 * (malŝaltebla en Agordoj → Privateco); F-Droid (`org.fdroid.*`) kaj ĈIU
 * NEKONATA instala fonto estas opt-in per unufoja konsento-demando. Nedependece de la SUBSKRIBO: per reprodukteblaj
 * konstruoj F-Droid distribuas APK-on subskribitan per nia atestilo, do la
 * subskribo ne plu distingas la kanalojn (sekureca trovo de PR #200; la
 * malnova `BuildConfig.SEN_NIA_SUBSKRIBO` kaj la subskriba kontrolo de
 * PR #148 estis konstruo/subskribo-bazitaj). La instala-distingo estas
 * RUTEMPA kontrolo, ne bakita en la konstruo: konstruo-dependa valoro
 * en la APK malebligus bit-identan reprodukteblan konstruon (F-Droid RB,
 * MR !55129, 2026-11-22). Sen inicialigo ĉiuj Sentry-vokoj estas no-op.
 */
class EsperantoRadioAplikaĵo : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        val estasDebug = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        // Evolua medio (debug-konstruo aŭ emulilo): ekz. la "Apaj novaĵoj"-
        // dialogo ne montriĝu — ĝi aperus ĉe ĉiu freŝa instalaĵo
        AppStato.estasEvoluaMedo = estasDebug || estasEmulilo()
        // Debug-konstruo = alfaversio: titolo «EsperαntoRαdio alfa» en la hejmoekrano
        AppStato.estasAlfaKonstruo = estasDebug
        // Branĉnomo (malplena ĉe master) — montriĝas en Agordoj post la versio
        AppStato.branĉoNomo = BuildConfig.GIT_BRANCHO.ifBlank { null }
        // Instalaĵo-specifa defaŭlto de la erar-raportado (vidu la klasan KDoc-on):
        // konata ne-F-Droid-kanalo → defaŭlte ŝaltita; F-Droid/nekonata → opt-in demando.
        AppStato.erarRaportadoDefaŭlta = erarRaportadaDefaŭltoLaŭInstalanto(leguInstalanton())
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

    /**
     * La pako, kiu instalis tiun ĉi aplikaĵon (rutempa kontrolo; vidu la
     * klasan KDoc-on kial la instal-fonto, ne la subskribo). Play Store →
     * `com.android.vending`; F-Droid → `org.fdroid.fdroid`
     * (aŭ `org.fdroid.fdroid.privileged`); flanka ŝargo/adb →
     * `com.android.packageinstaller` aŭ `null`. La valoron legas
     * [erarRaportadaDefaŭltoLaŭInstalanto].
     */
    private fun leguInstalanton(): String? = try {
        if (Build.VERSION.SDK_INT >= 30) {
            packageManager.getInstallSourceInfo(packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstallerPackageName(packageName)
        }
    } catch (e: Exception) {
        // A8: protokolu. Fiasko → ne legebla instala fonto →
        // `erarRaportadaDefaŭltoLaŭInstalanto` traktas ĝin kiel NEKONATA
        // (laŭ decido de la posedanto: opt-in — la privatece konservema direkto).
        logw("Sentry", "Ne povis legi la instal-fonton — traktas kiel nekonata instalaĵo (opt-in)", e)
        null
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
