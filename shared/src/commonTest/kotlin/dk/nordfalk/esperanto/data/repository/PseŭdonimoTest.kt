package dk.nordfalk.esperanto.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Testoj por la pseŭdonimoj ("adjektivo substantivo") uzataj en la protokolado.
 */
class PseŭdonimoTest {

    @Test
    fun listojHavasPo200UnikajnErojn() {
        assertEquals(200, PSEŬDONIMAJ_ADJEKTIVOJ.size)
        assertEquals(200, PSEŬDONIMAJ_SUBSTANTIVOJ.size)
        assertEquals(200, PSEŬDONIMAJ_ADJEKTIVOJ.toSet().size)
        assertEquals(200, PSEŬDONIMAJ_SUBSTANTIVOJ.toSet().size)
    }

    @Test
    fun generitaNomoHavasFormonAdjektivoSubstantivo() {
        repeat(100) {
            val partoj = generuPseŭdonimon().split(" ")
            assertEquals(2, partoj.size, "Nomo devas konsisti el du vortoj")
            assertTrue(partoj[0] in PSEŬDONIMAJ_ADJEKTIVOJ, "'${partoj[0]}' ne estas en la adjektiva listo")
            assertTrue(partoj[1] in PSEŬDONIMAJ_SUBSTANTIVOJ, "'${partoj[1]}' ne estas en la substantiva listo")
        }
    }
}
