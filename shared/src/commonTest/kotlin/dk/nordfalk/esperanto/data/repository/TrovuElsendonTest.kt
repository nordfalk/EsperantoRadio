package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanalo
import io.ktor.client.HttpClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

/**
 * Testoj de [ElsendoDeponejoImpl.trovuElsendon] — la ŝarg-kaskado de la
 * ElsendoDetalo-eniro: kaŝo → kutima ŝargo → unu forta refreŝigo.
 *
 * Rulu per: ./gradlew :shared:desktopTest --tests "*TrovuElsendon*"
 */
class TrovuElsendonTest {

    private val kanalo = Kanalo(slug = "k1", nomo = "Kanalo unu")

    private fun elsendo(id: String) = Elsendo(
        id = id, kanaloSlug = kanalo.slug, titolo = "Elsendo $id",
        fluo = "https://x.test/$id.mp3", dato = "2026-01-01", daŭro = 100,
    )

    /** Deponejo, kies „ŝargo“ estas regata de la testo; kalkulas la vokojn kune kun fortflago. */
    private class RegataDeponejo(
        private val ŝargo: (fortoRefresigi: Boolean) -> List<Elsendo>,
    ) : ElsendoDeponejoImpl(HttpClient(httpMotoro)) {
        var vokoj = 0
        var fortajVokoj = 0

        /** Enmetas liston rekte en la kaŝon (eneĉela aliro al la protektita konservu). */
        suspend fun pleniguKaŝon(slug: String, list: List<Elsendo>) = konservu(slug, list)

        override suspend fun ŝargiElsendojn(kanalo: Kanalo, fortoRefresigi: Boolean): List<Elsendo> {
            vokoj++
            if (fortoRefresigi) fortajVokoj++
            return ŝargo(fortoRefresigi)
        }
    }

    @Test
    fun trovasElKaŝoSenŜargo() = runTest {
        val celo = elsendo("celo")
        val dep = RegataDeponejo { listOf(elsendo("alia"), celo) }
        dep.pleniguKaŝon(kanalo.slug, listOf(elsendo("alia"), celo))
        assertEquals(celo, dep.trovuElsendon(kanalo, celo.id))
        assertEquals(0, dep.vokoj, "la kaŝo sufiĉas — neniu ŝargo devas okazi")
    }

    @Test
    fun trovasPostKutimaŜargo() = runTest {
        val celo = elsendo("celo")
        val dep = RegataDeponejo { listOf(elsendo("alia"), celo) }
        assertEquals(celo, dep.trovuElsendon(kanalo, celo.id))
        assertEquals(1, dep.vokoj, "kutima ŝargo sufiĉas — NE devas sekvi forta refreŝigo")
        assertEquals(0, dep.fortajVokoj)
    }

    @Test
    fun trovasPostFortaRefreŝigo() = runTest {
        // La kutima ŝargo (ekz. malnova diska kaŝo) ne havas la elsendon —
        // nur la forta refreŝigo (ekz. brandnova elsendo sciigita) liveras ĝin.
        val celo = elsendo("celo")
        val dep = RegataDeponejo { forto -> if (forto) listOf(celo) else listOf(elsendo("malnova")) }
        assertEquals(celo, dep.trovuElsendon(kanalo, celo.id))
        assertEquals(2, dep.vokoj)
        assertEquals(1, dep.fortajVokoj, "ekzakte UNU forta refreŝigo rajtas okazi")
    }

    @Test
    fun liverasNullKiamNetroveblaPostFortaRefreŝigo() = runTest {
        val dep = RegataDeponejo { listOf(elsendo("alia")) }
        assertNull(dep.trovuElsendon(kanalo, "neekzistanta"))
        assertEquals(1, dep.fortajVokoj, "ekzakte unu forta refreŝigo, ne pli")
    }
}
