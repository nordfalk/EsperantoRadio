package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.ElshutStato
import dk.nordfalk.esperanto.domain.repository.ElshutDeponejo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Testoj por NoOpElshutDeponejo — la platformoj kiuj ne subtenas elŝutojn.
 */
class NoOpElshutDeponejoTest {

    private fun kreiDeponejo(): ElshutDeponejo = NoOpElshutDeponejo()

    private val testElsendo = Elsendo(
        id = "test:2024-01-01",
        kanalSlug = "test",
        titolo = "Testa elsendo",
        stream = "https://x.com/a.mp3",
        dato = "2024-01-01"
    )

    @Test
    fun komenceMalplena() = runTest {
        val deponejo = kreiDeponejo()
        assertTrue(deponejo.observiElshutojn().value.isEmpty())
    }

    @Test
    fun estasElshutitaRestasFalse() = runTest {
        val deponejo = kreiDeponejo()
        assertFalse(deponejo.estasElshutita("iu-id"))
    }

    @Test
    fun getLokaDosieroVojoRestasNull() = runTest {
        val deponejo = kreiDeponejo()
        assertNull(deponejo.getLokaDosieroVojo("iu-id"))
    }

    @Test
    fun elshutiNeFarasNenion() = runTest {
        val deponejo = kreiDeponejo()
        deponejo.elshuti(testElsendo)
        assertTrue(deponejo.observiElshutojn().value.isEmpty())
        assertFalse(deponejo.estasElshutita(testElsendo.id))
    }

    @Test
    fun observiElshutStatonLavasPerNeElshutita() = runTest {
        val deponejo = kreiDeponejo()
        val stato = deponejo.observiElshutStaton("iu-id")
        assertTrue(stato.value is ElshutStato.NeElshutita)
    }

    @Test
    fun forigiNeEraras() = runTest {
        val deponejo = kreiDeponejo()
        deponejo.forigi("iu-id")
        assertTrue(deponejo.observiElshutojn().value.isEmpty())
    }

    @Test
    fun haltigiNeEraras() = runTest {
        val deponejo = kreiDeponejo()
        deponejo.haltigi("iu-id")
        assertTrue(deponejo.observiElshutojn().value.isEmpty())
    }
}
