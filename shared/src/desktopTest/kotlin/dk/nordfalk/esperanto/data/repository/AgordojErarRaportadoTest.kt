package dk.nordfalk.esperanto.data.repository

import com.russhwolf.settings.PreferencesSettings
import dk.nordfalk.esperanto.AppStato
import dk.nordfalk.esperanto.raportadoAktiva
import java.util.prefs.Preferences
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Testoj por la erar-raportado (C50/F-Droid-kontraŭeco "Tracking"):
 * la defaŭlto estas KONSTRUO-SPECIFA (AppStato.erarRaportadoDefaŭlta) —
 * malŝaltita en konstruoj sen nia subskribo (F-Droid, opt-in kun konsento-
 * demando), ŝaltita en konstruoj kun nia subskribo (Google Play/Aptoide,
 * opt-out en Agordoj). Fiksi ŝanĝas la StateFlow, persistas, sinkronigas la
 * tutmondan flagon (legatan de la log*-funkcioj en ProtokoloUtil) kaj
 * memorigas la respondon de la konsento-demando (dialogo unufoje).
 *
 * EstasEvoluaMedo estas ŝaltita dum la testoj — tio samtempe kontrolas, ke
 * la Sentry-inicialigo (fiksiErarRaportadon) NE okazas en evolua medio.
 */
class AgordojErarRaportadoTest {

    private val nodo = Preferences.userRoot().node("esperantoradio-test-${System.nanoTime()}")
    private val settings = PreferencesSettings(nodo)

    @BeforeTest
    fun preparu() {
        AppStato.estasEvoluaMedo = true
        AppStato.erarRaportadoDefaŭlta = false
    }

    @AfterTest
    fun purigu() {
        nodo.removeNode()
        raportadoAktiva = false
        AppStato.estasEvoluaMedo = false
        AppStato.erarRaportadoDefaŭlta = false
    }

    @Test
    fun defaŭlteMalŝaltitaKajFlagoSinkronigita() {
        // Konstruo SEN nia subskribo (F-Droid): defaŭlte malŝaltita (opt-in)
        raportadoAktiva = true
        val deponejo = AgordojDeponejoImpl(settings)
        assertFalse(deponejo.erarRaportado.value)
        assertFalse(raportadoAktiva)
        // La konsento-demando ankoraŭ ne estis respondita
        assertFalse(deponejo.erarRaportadoDemandita.value)
    }

    @Test
    fun defaŭltoLaŭKonstruoKunNiaSubskribo() {
        // Konstruo KUN nia subskribo (Google Play/Aptoide): defaŭlte ŝaltita
        // (opt-out) — la uzanto ricevas NENIAM demandon (demandita = vera)
        AppStato.erarRaportadoDefaŭlta = true
        raportadoAktiva = false
        val deponejo = AgordojDeponejoImpl(settings)
        assertTrue(deponejo.erarRaportado.value)
        assertTrue(raportadoAktiva)
        // La dialogo ne aperas: la defaŭlta ŝaltita raporto validas kiel respondita
        assertTrue(deponejo.erarRaportadoDemandita.value)

        // Malŝalto persistas trans "restarto" (nova deponejo legas ĝin)
        deponejo.fiksiErarRaportadon(false)
        val denove = AgordojDeponejoImpl(settings)
        assertFalse(denove.erarRaportado.value)
        assertFalse(raportadoAktiva)
    }

    @Test
    fun eksplicitaUzantoDecidoVenkasLaKonstruoDefaŭlton() {
        // F-Droid-konstruo, sed la uzanto jam enŝaltis (ekz. respondis "Jes"):
        // nova deponejo legas la eksplicitan valon, ne la defaŭlton
        settings.putBoolean("erar_raportado", true)
        val deponejo = AgordojDeponejoImpl(settings)
        assertTrue(deponejo.erarRaportado.value)
        assertTrue(raportadoAktiva)
    }

    @Test
    fun konsentoŜaltasPersistasKajMemorigas() {
        val deponejo = AgordojDeponejoImpl(settings)
        deponejo.fiksiErarRaportadon(true)
        assertTrue(deponejo.erarRaportado.value)
        assertTrue(raportadoAktiva)
        assertTrue(deponejo.erarRaportadoDemandita.value)

        // Persistas trans "restarto" — nova deponejo legas kaj re-sinkronigas
        raportadoAktiva = false
        val denove = AgordojDeponejoImpl(settings)
        assertTrue(denove.erarRaportado.value)
        assertTrue(raportadoAktiva)
        // Ŝaltita raportado implicas responditan demandon — dialogo ne reaperas
        assertTrue(denove.erarRaportadoDemandita.value)

        // Malŝalto same persistas
        denove.fiksiErarRaportadon(false)
        assertFalse(denove.erarRaportado.value)
        assertFalse(raportadoAktiva)
    }

    @Test
    fun rifuzoMemorigasSedNeŜaltas() {
        val deponejo = AgordojDeponejoImpl(settings)
        deponejo.konfirmuErarRaportadoDemandon()
        assertFalse(deponejo.erarRaportado.value)
        assertFalse(raportadoAktiva)
        assertTrue(deponejo.erarRaportadoDemandita.value)

        // Persistas trans "restarto" — la dialogo ne reaperas
        val denove = AgordojDeponejoImpl(settings)
        assertTrue(denove.erarRaportadoDemandita.value)
        assertFalse(denove.erarRaportado.value)
        // Ripeta konfirmo estas no-op (ne protokolu ŝanĝon sen ŝanĝo)
        denove.konfirmuErarRaportadoDemandon()
        assertTrue(denove.erarRaportadoDemandita.value)
    }
}
