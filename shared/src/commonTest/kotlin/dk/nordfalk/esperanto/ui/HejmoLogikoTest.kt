package dk.nordfalk.esperanto.ui

import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanalo
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Puraj testoj por la logiko de la "Kanaloj"-vico sur la frontpaĝo:
 * [kalkuliAĝon] (la flava markilo sur kanaloj) kaj [dividuKanalojn] (aktiva/arkiva).
 */
class HejmoLogikoTest {

    private val hodiaŭ = LocalDate(2026, 10, 8)

    private fun datoAntaŭ(tagoloj: Int): String =
        (hodiaŭ.minus(DatePeriod(days = tagoloj))).toString()

    @Test
    fun kalkuliAĝon_montras_ĉiam_tempon() {
        assertEquals("hodiaŭ", kalkuliAĝon(datoAntaŭ(0), hodiaŭ))
        assertEquals("hieraŭ", kalkuliAĝon(datoAntaŭ(1), hodiaŭ))
        assertEquals("5 tagoj", kalkuliAĝon(datoAntaŭ(5), hodiaŭ))
        assertEquals("2 semajnoj", kalkuliAĝon(datoAntaŭ(17), hodiaŭ))
        assertEquals("4 monatoj", kalkuliAĝon(datoAntaŭ(130), hodiaŭ))
        // Ankaŭ pli malnovaj ol 6 monatoj — neniam null
        assertEquals("6 monatoj", kalkuliAĝon(datoAntaŭ(200), hodiaŭ))
        assertEquals("1 jaro", kalkuliAĝon(datoAntaŭ(400), hodiaŭ))
        assertEquals("2 jaroj", kalkuliAĝon(datoAntaŭ(800), hodiaŭ))
        // Estonta dato → "hodiaŭ"
        assertEquals("hodiaŭ", kalkuliAĝon(datoAntaŭ(-3), hodiaŭ))
        // Neparsebla dato → null
        assertNull(kalkuliAĝon("ne-dato", hodiaŭ))
    }

    @Test
    fun estasEneDeSesMonatoj_limo() {
        // Regas "Kio novas": ene de 180 tagoj
        assertTrue(estasEneDeSesMonatoj(datoAntaŭ(179), hodiaŭ))
        assertFalse(estasEneDeSesMonatoj(datoAntaŭ(180), hodiaŭ))
        assertFalse(estasEneDeSesMonatoj("ne-dato", hodiaŭ))
        // estonta dato validas kiel "nova"
        assertTrue(estasEneDeSesMonatoj(datoAntaŭ(-3), hodiaŭ))
        // sed la markilo montras la aĝon eĉ trans la limo
        assertEquals("6 monatoj", kalkuliAĝon(datoAntaŭ(180), hodiaŭ))
        assertEquals("1 jaro", kalkuliAĝon(datoAntaŭ(364), hodiaŭ))
    }

    @Test
    fun estasEneDeUnuJaro_limo() {
        // Regas aktivan kontraŭ arkivan kanalon: ene de 365 tagoj
        assertTrue(estasEneDeUnuJaro(datoAntaŭ(364), hodiaŭ))
        assertFalse(estasEneDeUnuJaro(datoAntaŭ(365), hodiaŭ))
        assertFalse(estasEneDeUnuJaro("ne-dato", hodiaŭ))
        // estonta dato validas kiel "aktiva"
        assertTrue(estasEneDeUnuJaro(datoAntaŭ(-3), hodiaŭ))
        // la markilo montras la aĝon ankaŭ trans la limo
        assertEquals("1 jaro", kalkuliAĝon(datoAntaŭ(365), hodiaŭ))
        assertEquals("2 jaroj", kalkuliAĝon(datoAntaŭ(800), hodiaŭ))
    }

    private fun karto(slug: String, dato: String) = KanalKarto(
        kanalo = Kanalo(slug = slug, nomo = slug, podkastaRssUrl = "https://x.com/$slug.rss"),
        plejNovaElsendo = Elsendo(id = "$slug:1", kanaloSlug = slug, titolo = "e", fluo = "", dato = dato),
    )

    @Test
    fun dividuKanalojn_aktiva_estas_ene_de_unu_jaro() {
        // aktiva = ene de 365 tagoj; arkiva = pli aĝa ol unu jaro
        val kartoj = listOf(
            karto("nova", datoAntaŭ(5)),
            karto("limo", datoAntaŭ(364)),
            karto("arkivo", datoAntaŭ(365)),
            karto("malnova", datoAntaŭ(900)),
        )
        val (aktivaj, arkivaj) = dividuKanalojn(kartoj, hodiaŭ)
        assertEquals(listOf("nova", "limo"), aktivaj.map { it.kanalo.slug })
        assertEquals(listOf("arkivo", "malnova"), arkivaj.map { it.kanalo.slug })
    }

    @Test
    fun dividuKanalojn_kanalo_sen_elsendo_ne_estas_aktiva() {
        val kartoj = listOf(
            KanalKarto(
                kanalo = Kanalo(slug = "nova", nomo = "nova", podkastaRssUrl = "https://x.com/n.rss"),
                plejNovaElsendo = null,
            ),
            karto("nova2", datoAntaŭ(5)),
        )
        val (aktivaj, arkivaj) = dividuKanalojn(kartoj, hodiaŭ)
        assertEquals(listOf("nova2"), aktivaj.map { it.kanalo.slug })
        assertEquals(listOf("nova"), arkivaj.map { it.kanalo.slug })
    }
}
