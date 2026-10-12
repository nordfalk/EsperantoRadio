package dk.nordfalk.esperanto.data.repository

import com.russhwolf.settings.PreferencesSettings
import kotlinx.coroutines.test.runTest
import java.util.prefs.Preferences
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Testoj por [PersistantaPlejŝatatajDeponejo] — persisto de ŝatataj kanaloj
 * (FAROTA G40). Uzas veran persiston (java.util.prefs en provizora nodo) por
 * kontroli ke la stato estas videbla ankaŭ por nova deponejo (kiel post restarto).
 */
class PersistantaPlejŝatatajDeponejoTest {

    private val nodo = Preferences.userRoot().node("esperantoradio-test-${System.nanoTime()}")
    private val settings = PreferencesSettings(nodo)

    @AfterTest
    fun purigu() = nodo.removeNode()

    @Test
    fun baskuloKonservigxasTransRestarton() = runTest {
        val deponejo = PersistantaPlejŝatatajDeponejo(settings)
        assertFalse(deponejo.estasPlejŝatata("muzaiko"))

        deponejo.baskuliPlejŝaton("muzaiko")
        assertTrue(deponejo.estasPlejŝatata("muzaiko"))

        // Nova deponejo = kiel post restarto de la procezo
        val restartita = PersistantaPlejŝatatajDeponejo(settings)
        assertTrue(restartita.estasPlejŝatata("muzaiko"), "Ŝatateco devas pluvivi restarton")
        assertEquals(setOf("muzaiko"), restartita.observiPlejŝatatajn().value)
    }

    @Test
    fun duaBaskuloMalxatatas() = runTest {
        val deponejo = PersistantaPlejŝatatajDeponejo(settings)
        deponejo.baskuliPlejŝaton("muzaiko")
        deponejo.baskuliPlejŝaton("muzaiko")

        assertFalse(deponejo.estasPlejŝatata("muzaiko"))
        assertTrue(deponejo.observiPlejŝatatajn().value.isEmpty())
    }

    @Test
    fun plurajKanalojKonservigxas() = runTest {
        val deponejo = PersistantaPlejŝatatajDeponejo(settings)
        deponejo.baskuliPlejŝaton("muzaiko")
        deponejo.baskuliPlejŝaton("kernpunkto")

        val restartita = PersistantaPlejŝatatajDeponejo(settings)
        assertEquals(setOf("muzaiko", "kernpunkto"), restartita.observiPlejŝatatajn().value)
    }
}
