package dk.nordfalk.esperanto

import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.ElŝutStato
import dk.nordfalk.esperanto.domain.model.ElŝutitaElsendo
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Testoj de [trovuElsendonPorDetalo] — la serĉordo de la ElsendoDetalo-eniro:
 * la kanala fluo venkas; se ĝi ne plu havas la elsendon, la elŝutita kopio
 * malfermeblas (eksterrete); nur kiam ambaŭ mankas, la eniro montras klarigon.
 */
class TrovuElsendonPorDetaloTest {

    private fun elsendo(id: String) = Elsendo(
        id = id, kanaloSlug = "k1", titolo = "Elsendo $id",
        fluo = "https://x.test/$id.mp3", dato = "2026-01-01", daŭro = 100,
    )

    private fun elŝuto(e: Elsendo) =
        ElŝutitaElsendo(elsendo = e, dosieroVojo = "/iuj/$${e.id}.mp3", stato = ElŝutStato.Preta)

    @Test
    fun laKanalaFluoVenasKiamAmbaŭHavasĜin() {
        val elFluo = elsendo("e1")
        val elElŝuto = elsendo("e1") // kopio kun alia memoradreso
        val trovita = trovuElsendonPorDetalo(
            kanalajElsendoj = listOf(elFluo),
            elŝutoj = mapOf("e1" to elŝuto(elElŝuto)),
            id = "e1",
        )
        assertSame(elFluo, trovita, "la fluo-kopio devas venki (la plej freŝa datumo)")
    }

    @Test
    fun laElŝutitaKopioKiamLaFluoMankas() {
        val elElŝuto = elsendo("e1")
        val trovita = trovuElsendonPorDetalo(
            kanalajElsendoj = listOf(elsendo("alia")),
            elŝutoj = mapOf("e1" to elŝuto(elElŝuto)),
            id = "e1",
        )
        assertSame(elElŝuto, trovita, "elŝutita elsendo malfermeblas ankaŭ eksterrete")
    }

    @Test
    fun nulKiamNekFluoNekElŝutoHavasĜin() {
        assertNull(
            trovuElsendonPorDetalo(
                kanalajElsendoj = emptyList(),
                elŝutoj = mapOf("alia" to elŝuto(elsendo("alia"))),
                id = "e1",
            ),
        )
    }
}
