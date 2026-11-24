package dk.nordfalk.esperanto.data.config

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Testoj por la defaŭlto de la erar-raportado laŭ la instal-fonto
 * ([erarRaportadaDefaŭltoLaŭInstalanto]): nur instalaĵoj el KONATAJ
 * ne-F-Droid-kanaloj raportas defaŭlte (opt-out); F-Droid kaj ĉiu
 * nekonata instala fonto estas opt-in per unufoja konsento-demando
 * (kontraŭeco „Tracking" alie).
 *
 * Fono (2026-11-24): per reprodukteblaj konstruoj F-Droid distribuas APK-on
 * subskribitan per nia atestilo — la subskribo do ne plu distingas la
 * kanalojn (trovo de la aŭtomata revizio en PR #200); tial la distingo
 * okazas per la instal-fonto. Laŭ decido de la posedanto: nur klaraj
 * konataj kanaloj estas opt-out, ĉio alia (inkl. nekonata) estas opt-in.
 */
class ErarRaportadaKonsentoTest {

    @Test
    fun fdroidInstalaĵojEstasOptIn() {
        assertFalse(erarRaportadaDefaŭltoLaŭInstalanto("org.fdroid.fdroid"))
        assertFalse(erarRaportadaDefaŭltoLaŭInstalanto("org.fdroid.fdroid.privileged"))
        assertFalse(erarRaportadaDefaŭltoLaŭInstalanto("org.fdroid.basic"))
        assertFalse(erarRaportadaDefaŭltoLaŭInstalanto("org.fdroid.basic.privileged"))
    }

    @Test
    fun konatajNeFdroidKanalojRaportasDefaŭlte() {
        assertTrue(erarRaportadaDefaŭltoLaŭInstalanto("com.android.vending"))
        assertTrue(erarRaportadaDefaŭltoLaŭInstalanto("com.android.packageinstaller"))
    }

    @Test
    fun nekonatajKanalojEstasOptIn() {
        assertFalse(erarRaportadaDefaŭltoLaŭInstalanto("com.io.some.thing"))
        assertFalse(erarRaportadaDefaŭltoLaŭInstalanto("com.aptoide.satics"))
    }

    @Test
    fun mankantaInstalFontoEstasOptIn() {
        assertFalse(erarRaportadaDefaŭltoLaŭInstalanto(null))
        assertFalse(erarRaportadaDefaŭltoLaŭInstalanto(""))
    }

    @Test
    fun alilandajPakojNeTrompasLaBlankliston() {
        assertFalse(erarRaportadaDefaŭltoLaŭInstalanto("com.fdroid.io"))
        assertFalse(erarRaportadaDefaŭltoLaŭInstalanto("io.fdroid.falso"))
        assertFalse(erarRaportadaDefaŭltoLaŭInstalanto("com.android.vending.extra"))
    }
}
