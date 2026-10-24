package dk.nordfalk.esperanto.data.repository

import com.russhwolf.settings.PreferencesSettings
import dk.nordfalk.esperanto.protokoloDetala
import java.util.prefs.Preferences
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Testoj por la agordo "detala protokolado" (Sentry-kvoto):
 * defaŭlte ŜALTITA (2026-10-13 — la protokola kvoto montriĝis ne-problema);
 * fiksi ŝanĝas la StateFlow, persistas kaj sinkronigas la tutmondan
 * protokol-flagon (legata de logi en ProtokoloUtil).
 */
class AgordojDetalaProtokoladoTest {

    private val nodo = Preferences.userRoot().node("esperantoradio-test-${System.nanoTime()}")
    private val settings = PreferencesSettings(nodo)

    @AfterTest
    fun purigu() {
        nodo.removeNode()
        protokoloDetala = false
    }

    @Test
    fun defaŭlteŜaltitaKajFlagoSinkronigita() {
        protokoloDetala = false
        val deponejo = AgordojDeponejoImpl(settings)
        assertTrue(deponejo.detalaProtokolado.value)
        assertTrue(protokoloDetala)
    }

    @Test
    fun fiksiŜaltasPersistasKajSinkronigas() {
        protokoloDetala = false
        val deponejo = AgordojDeponejoImpl(settings)
        deponejo.fiksiDetalanProtokoladon(true)
        assertTrue(deponejo.detalaProtokolado.value)
        assertTrue(protokoloDetala)
        // Persistas trans "restarto" — nova deponejo legas kaj re-sinkronigas la flagon
        protokoloDetala = false
        val denove = AgordojDeponejoImpl(settings)
        assertTrue(denove.detalaProtokolado.value)
        assertTrue(protokoloDetala)
        // Malŝalto same
        denove.fiksiDetalanProtokoladon(false)
        assertFalse(denove.detalaProtokolado.value)
        assertFalse(protokoloDetala)
    }
}
