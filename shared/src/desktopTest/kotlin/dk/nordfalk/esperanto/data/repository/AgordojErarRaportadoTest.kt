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
 * Testoj por la opt-in-erar-raportado (C50/F-Droid-kontraŭeco "Tracking"):
 * defaŭlte MALŜALTITA; fiksi ŝanĝas la StateFlow, persistas, sinkronigas la
 * tutmondan flagon (legata de la log*-funkcioj en ProtokoloUtil) kaj
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
    }

    @AfterTest
    fun purigu() {
        nodo.removeNode()
        raportadoAktiva = false
        AppStato.estasEvoluaMedo = false
    }

    @Test
    fun defaŭlteMalŝaltitaKajFlagoSinkronigita() {
        raportadoAktiva = true
        val deponejo = AgordojDeponejoImpl(settings)
        assertFalse(deponejo.erarRaportado.value)
        assertFalse(raportadoAktiva)
        // La konsento-demando ankoraŭ ne estis respondita
        assertFalse(deponejo.erarRaportadoDemandita.value)
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
