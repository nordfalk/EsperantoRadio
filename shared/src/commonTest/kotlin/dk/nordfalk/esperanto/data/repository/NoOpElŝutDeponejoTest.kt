package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.ElŝutStato
import dk.nordfalk.esperanto.domain.repository.ElŝutDeponejo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Testoj por NoOpElŝutDeponejo — la platformoj kiuj ne subtenas elŝutojn.
 */
class NoOpElŝutDeponejoTest {

    private fun kreiDeponejo(): ElŝutDeponejo = NoOpElŝutDeponejo()

    private val testElsendo = Elsendo(
        id = "test:2024-01-01",
        kanaloSlug = "test",
        titolo = "Testa elsendo",
        fluo = "https://x.com/a.mp3",
        dato = "2024-01-01"
    )

    @Test
    fun komenceMalplena() = runTest {
        val deponejo = kreiDeponejo()
        assertTrue(deponejo.observiElŝutojn().value.isEmpty())
    }

    @Test
    fun estasElŝutitaRestasFalse() = runTest {
        val deponejo = kreiDeponejo()
        assertFalse(deponejo.estasElŝutita("iu-ID"))
    }

    @Test
    fun getLokaDosieroVojoRestasNull() = runTest {
        val deponejo = kreiDeponejo()
        assertNull(deponejo.getLokaDosieroVojo("iu-ID"))
    }

    @Test
    fun elŝutiNeFarasNenion() = runTest {
        val deponejo = kreiDeponejo()
        deponejo.elŝuti(testElsendo)
        assertTrue(deponejo.observiElŝutojn().value.isEmpty())
        assertFalse(deponejo.estasElŝutita(testElsendo.id))
    }

    @Test
    fun observiElŝutStatonLavasPerNeElŝutita() = runTest {
        val deponejo = kreiDeponejo()
        val stato = deponejo.observiElŝutStaton("iu-ID")
        assertTrue(stato.value is ElŝutStato.NeElŝutita)
    }

    @Test
    fun forigiNeEraras() = runTest {
        val deponejo = kreiDeponejo()
        deponejo.forigi("iu-ID")
        assertTrue(deponejo.observiElŝutojn().value.isEmpty())
    }

    @Test
    fun haltigiNeEraras() = runTest {
        val deponejo = kreiDeponejo()
        deponejo.haltigi("iu-ID")
        assertTrue(deponejo.observiElŝutojn().value.isEmpty())
    }
}
