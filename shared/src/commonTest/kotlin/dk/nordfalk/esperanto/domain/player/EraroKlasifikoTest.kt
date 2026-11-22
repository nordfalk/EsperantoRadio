package dk.nordfalk.esperanto.domain.player

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EraroKlasifikoTest {

    @Test
    fun http5xxEstasPasema() {
        assertTrue(EraroKlasifiko.estasPasemaHttpStatuson(500))
        assertTrue(EraroKlasifiko.estasPasemaHttpStatuson(503))
        assertTrue(EraroKlasifiko.estasPasemaHttpStatuson(599))
    }

    @Test
    fun http4xxKajAliajEstasDaŭraj() {
        assertFalse(EraroKlasifiko.estasPasemaHttpStatuson(404))
        assertFalse(EraroKlasifiko.estasPasemaHttpStatuson(403))
        assertFalse(EraroKlasifiko.estasPasemaHttpStatuson(200))
        assertFalse(EraroKlasifiko.estasPasemaHttpStatuson(0))
    }
}
