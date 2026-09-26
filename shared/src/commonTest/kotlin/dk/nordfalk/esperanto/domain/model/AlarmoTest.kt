package dk.nordfalk.esperanto.domain.model

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

/** Testoj por [sekvaEkigo] — la kalkulo de la sekva ekigo-tempo de alarmo. */
class AlarmoTest {

    // Merkredo, 2026-09-23 10:00
    private val merkredo10 = LocalDateTime(2026, 9, 23, 10, 0)

    private fun alarmo(horo: Int, minuto: Int, ripeto: Int) =
        Alarmo(id = 1, horo = horo, minuto = minuto, ripeto = ripeto, kanaloSlug = "muzaiko")

    @Test
    fun testDatoEstasMerkredo() {
        assertEquals(DayOfWeek.WEDNESDAY, merkredo10.dayOfWeek)
    }

    @Test
    fun unufojeHodiauxSeTempoNeJamPasis() {
        assertEquals(LocalDateTime(2026, 9, 23, 11, 30), alarmo(11, 30, 0).sekvaEkigo(merkredo10))
    }

    @Test
    fun unufojeMorgauxSeTempoJamPasis() {
        assertEquals(LocalDateTime(2026, 9, 24, 7, 0), alarmo(7, 0, 0).sekvaEkigo(merkredo10))
    }

    @Test
    fun samaMinutoEstasMorgaux() {
        // Ekigo ĝuste nun ne validas — alie AlarmoReceivilo re-skedus la saman momenton
        assertEquals(LocalDateTime(2026, 9, 24, 10, 0), alarmo(10, 0, 0x7f).sekvaEkigo(merkredo10))
    }

    @Test
    fun cxiutageMorgauxSeHodiauxPasis() {
        assertEquals(LocalDateTime(2026, 9, 24, 7, 0), alarmo(7, 0, 0x7f).sekvaEkigo(merkredo10))
    }

    @Test
    fun semajnfinoSaltasAlSabato() {
        // 0x20 = sabato, 0x40 = dimanĉo
        assertEquals(LocalDateTime(2026, 9, 26, 8, 0), alarmo(8, 0, 0x60).sekvaEkigo(merkredo10))
    }

    @Test
    fun nurMerkredoJamPasisEstasPostSemajno() {
        // 0x04 = merkredo
        assertEquals(LocalDateTime(2026, 9, 30, 7, 0), alarmo(7, 0, 0x04).sekvaEkigo(merkredo10))
    }

    @Test
    fun nurLundoTransirasSemajnon() {
        // 0x01 = lundo → la sekva lundo, 2026-09-28
        assertEquals(LocalDateTime(2026, 9, 28, 6, 15), alarmo(6, 15, 0x01).sekvaEkigo(merkredo10))
    }

    @Test
    fun transirasMonatonKajJaron() {
        val silvestro = LocalDateTime(2026, 12, 31, 23, 0)
        assertEquals(LocalDateTime(2027, 1, 1, 7, 0), alarmo(7, 0, 0).sekvaEkigo(silvestro))
    }
}
