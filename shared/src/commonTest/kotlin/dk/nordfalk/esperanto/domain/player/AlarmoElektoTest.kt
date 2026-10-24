package dk.nordfalk.esperanto.domain.player

import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.LudataElsendo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AlarmoElektoTest {

    private fun elsendo(id: String, dato: String, fluo: String = "https://x/$id.mp3") =
        Elsendo(id = id, kanaloSlug = "peranto", titolo = id, fluo = fluo, dato = dato)

    private val malnova = elsendo("a", "2026-09-01")
    private val meza = elsendo("b", "2026-09-10")
    private val nova = elsendo("c", "2026-09-20")

    @Test
    fun elektasPlejNovanEcSeListoNeOrdigita() {
        assertEquals(nova, elektuAlarmElsendon(listOf(meza, nova, malnova), emptyMap()))
    }

    @Test
    fun saltasFinitajnKajErarajn() {
        val ludatoj = mapOf(
            "c" to LudataElsendo("c", "peranto", finita = true),
            "b" to LudataElsendo("b", "peranto", erara = true),
        )
        assertEquals(malnova, elektuAlarmElsendon(listOf(nova, meza, malnova), ludatoj))
    }

    @Test
    fun duonaŭskultitaEstasElektebla() {
        val ludatoj = mapOf("c" to LudataElsendo("c", "peranto", pozicioMs = 60_000))
        assertEquals(nova, elektuAlarmElsendon(listOf(nova, meza), ludatoj))
    }

    @Test
    fun ĉiujAŭskultitajDonasPlejNovan() {
        val ludatoj = listOf("a", "b", "c").associateWith { LudataElsendo(it, "peranto", finita = true) }
        assertEquals(nova, elektuAlarmElsendon(listOf(malnova, meza, nova), ludatoj))
    }

    @Test
    fun ignorasElsendojnSenSono() {
        val senSono = elsendo("d", "2026-09-22", fluo = "")
        assertEquals(nova, elektuAlarmElsendon(listOf(senSono, nova), emptyMap()))
    }

    @Test
    fun malplenaListoDonasNull() {
        assertNull(elektuAlarmElsendon(emptyList(), emptyMap()))
    }

    @Test // (PR #95) la falilo devas ignori ERARAJN elsendojn — reludado de ne-erara pli bonas
    fun faliloPreterpasasErarajn() {
        // nova estas finita KAJ erara; meza finita sed sana; malnova erara
        val ludatoj = mapOf(
            "c" to LudataElsendo("c", "peranto", finita = true, erara = true),
            "b" to LudataElsendo("b", "peranto", finita = true),
            "a" to LudataElsendo("a", "peranto", erara = true),
        )
        assertEquals(meza, elektuAlarmElsendon(listOf(nova, meza, malnova), ludatoj))
    }

    @Test // (PR #95) se ĈIUJ estas erara → null (ringtono) — erara fluo tuj paneus
    fun ĉiujErarajDonasNull() {
        val ludatoj = listOf("a", "b", "c").associateWith { LudataElsendo(it, "peranto", erara = true) }
        assertNull(elektuAlarmElsendon(listOf(malnova, meza, nova), ludatoj))
    }
}
