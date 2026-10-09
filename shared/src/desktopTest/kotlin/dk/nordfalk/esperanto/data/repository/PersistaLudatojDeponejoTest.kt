package dk.nordfalk.esperanto.data.repository

import com.russhwolf.settings.PreferencesSettings
import kotlinx.coroutines.test.runTest
import java.util.prefs.Preferences
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Testoj por [PersistaLudatojDeponejo] — persisto de ludpozicioj kaj finstatoj
 * (FAROTA G40). Uzas veran persiston (java.util.prefs en provizora nodo) por
 * kontroli ke la stato estas videbla ankaŭ por nova deponejo (kiel post restarto).
 */
class PersistaLudatojDeponejoTest {

    private val nodo = Preferences.userRoot().node("esperantoradio-test-${System.nanoTime()}")
    private val settings = PreferencesSettings(nodo)

    @AfterTest
    fun purigu() = nodo.removeNode()

    @Test
    fun pozicioKonservigxasTransRestarton() = runTest {
        val deponejo = PersistaLudatojDeponejo(settings)
        deponejo.registriPozicion("e1", "k1", 30_000, 300_000)

        // Nova deponejo = kiel post restarto de la procezo
        val restartita = PersistaLudatojDeponejo(settings)
        val ludato = restartita.getLudato("e1")
        assertNotNull(ludato, "La pozicio devas pluvivi restarton")
        assertEquals(30_000, ludato.pozicioMs)
        assertEquals(300_000, ludato.dauroMs)
        assertFalse(ludato.finita, "Ne finita — nur pozicio savita")
    }

    @Test
    fun finitaMarkoKonservigxasTransRestarton() = runTest {
        val deponejo = PersistaLudatojDeponejo(settings)
        deponejo.registriPozicion("e1", "k1", 300_000, 300_000)
        deponejo.markiFinita("e1", "k1")

        val restartita = PersistaLudatojDeponejo(settings)
        assertTrue(restartita.estasFinita("e1"), "Finstato devas pluvivi restarton")
        assertTrue(restartita.getLudato("e1")?.finita == true)
    }

    @Test
    fun malmarkiFinitaRestarigas() = runTest {
        val deponejo = PersistaLudatojDeponejo(settings)
        deponejo.registriPozicion("e1", "k1", 0, 300_000)
        deponejo.markiFinita("e1", "k1")
        assertTrue(deponejo.estasFinita("e1"))

        // La uzanto eksplicite reludas finitan elsendon — ekz. "Daŭrigi" ne aperu plu
        deponejo.malmarkiFinita("e1", "k1")

        assertFalse(deponejo.estasFinita("e1"))
        val restartita = PersistaLudatojDeponejo(settings)
        assertFalse(restartita.estasFinita("e1"), "Malmarko devas persisti")
    }

    @Test
    fun eraraMarkoKonservigxas() = runTest {
        val deponejo = PersistaLudatojDeponejo(settings)
        deponejo.registriPozicion("e1", "k1", 10_000, 300_000)
        deponejo.markiErara("e1", "k1")

        val restartita = PersistaLudatojDeponejo(settings)
        assertTrue(restartita.getLudato("e1")?.erara == true, "Erar-stato devas pluvivi restarton")
    }

    @Test
    fun koruptaJSONKomencasFreshke() = runTest {
        // Korupta persistita valoro → legu() revenas al malplena, ne kraŝas
        nodo.put("ludatoj", "{NE VALIDA JSON")

        val deponejo = PersistaLudatojDeponejo(settings)
        assertNull(deponejo.getLudato("e1"))
        assertTrue(deponejo.observiLudatojn().value.isEmpty())
    }
}
