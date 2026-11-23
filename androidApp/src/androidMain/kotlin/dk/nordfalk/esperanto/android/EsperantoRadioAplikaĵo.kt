package dk.nordfalk.esperanto.android

import android.app.Application
import android.os.Build
import dk.nordfalk.esperanto.AppStato
import dk.nordfalk.esperanto.data.config.appContext
import dk.nordfalk.esperanto.data.config.leguErarRaportadanKonsenton
import dk.nordfalk.esperanto.initialiguSentry
import dk.nordfalk.esperanto.logi
import dk.nordfalk.esperanto.logw
import dk.nordfalk.esperanto.raportadoAktiva
import java.security.MessageDigest

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
 * #148/C50). La DEFAŬLTO dependas de la subskriba atestilo de la APK, ne de
 * la konstruo: kun nia subskribo (Google Play, Aptoide, GitHub-releases) ĝi
 * estas ŜALTITA (neniu demando; malŝaltebla en Agordoj → Privateco); kun
 * fremda subskribo (tiel F-Droid subskribas) MALŜALTITA kun unufoja
 * konsento-demando — alie F-Droid atribuus la kontraŭecon "Tracking". Tio
 * estas RUTEMPE kontrolata ([estasSubskribitaPerNiaAtestilo]), ne bakita en
 * la konstruo (eks-`BuildConfig.SEN_NIA_SUBSKRIBO`): konstruo-dependa valoro
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
        // Konstruo-specifa defaŭlto de la erar-raportado (vidu la funkcian KDoc-on)
        AppStato.erarRaportadoDefaŭlta = estasSubskribitaPerNiaAtestilo()
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

    companion object {
        /** SHA-256-haketo (bitoko-ĉeno) de nia subskriba atestilo (2026-10-29) */
        private const val NIA_ATESTILO_SHA_256 =
            "aab92d2bb6401743128fbe641ff86a0875ca95cb2cc5e15b1d4a7cd5e534eb16"
    }

    /**
     * Ĉu la APK estas subskribita per NIA atestilo (jacobnordfalk.keystore —
     * Google Play, Aptoide, GitHub-releases). RUTEMPA kontrolo per
     * PackageManager, por ke la APK-bitokoj ne dependu de la konstruo-medio
     * (reproduktebla konstruo, F-Droid RB): la sama APK condutas ĝustamaniere
     * sendepende de kiu subskribis ĝin. Fremda subskribo (F-Droid, iu ajn
     * rekonstruo) → opt-in per konsento-demando (kontraŭeco "Tracking").
     */
    private fun estasSubskribitaPerNiaAtestilo(): Boolean = try {
        val subskriboj: Array<android.content.pm.Signature> =
            if (Build.VERSION.SDK_INT >= 28) {
                packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES)
                    .signingInfo?.apkContentsSigners ?: emptyArray()
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.GET_SIGNATURES)
                    .signatures ?: emptyArray()
            }
        val sha256 = MessageDigest.getInstance("SHA-256")
        subskriboj.any { subskribo ->
            sha256.digest(subskribo.toByteArray())
                .joinToString("") { "%02x".format(it) } == NIA_ATESTILO_SHA_256
        }
    } catch (e: Exception) {
        // A8: protokolu. Fiu kiu fiasko → supozu fremdan subskribon (opt-in).
        logw("Sentry", "Ne povis legi la subskriban atestilon — traktas kiel fremda subskribo", e)
        false
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
