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

    @Test // (PR #95) korupta persistinta valoro ne devas kraŝi la skedadon
    fun koercasEkstervalorajnHorojnKajMinutojn() {
        // horo=99 → 23, minuto=-5 → 0: la alarmo skediĝas, ne ĵetas
        assertEquals(LocalDateTime(2026, 9, 23, 23, 0), alarmo(99, -5, 0).sekvaEkigo(merkredo10))
        // ripeto ekster la 7 tago-bitoj → maskita (0x123 & 0x7f = 0x23 = Lu+Ma+Sa);
        // merkredo-11:00 jam pasis (10:00) kaj merkredo ne estas en la masko → sabato
        assertEquals(LocalDateTime(2026, 9, 26, 11, 0), alarmo(11, 0, 0x123).sekvaEkigo(merkredo10))
    }

    @Test // (PR #114) la ripet-teksto estas generata el la konfiguro, ne libera teksto
    fun ripetoTekstoGeneratasElKonfiguro() {
        assertEquals("Unufoje", alarmo(6, 0, 0x00).ripetoTeksto)
        assertEquals("Ĉiutage", alarmo(6, 0, 0x7f).ripetoTeksto)
        assertEquals("Labortage", alarmo(6, 45, 0x1f).ripetoTeksto)
        assertEquals("Semajnfine", alarmo(10, 0, 0x60).ripetoTeksto)
        assertEquals("Ĵaŭde", alarmo(21, 0, 0x08).ripetoTeksto)
        assertEquals("Dimanĉe", alarmo(8, 0, 0x40).ripetoTeksto)
        // ne-specialigitaj kombinoj restas mallongigoj
        assertEquals("Lu Ma", alarmo(6, 0, 0x03).ripetoTeksto)
    }
}
