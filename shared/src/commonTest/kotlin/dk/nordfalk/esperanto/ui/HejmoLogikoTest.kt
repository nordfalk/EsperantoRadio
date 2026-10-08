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

    private val hodiaux = LocalDate(2026, 10, 8)

    private fun datoAntaux(tagoloj: Int): String =
        (hodiaux.minus(DatePeriod(days = tagoloj))).toString()

    @Test
    fun kalkuliAĝon_montras_ĉiam_tempon() {
        assertEquals("hodiaŭ", kalkuliAĝon(datoAntaux(0), hodiaux))
        assertEquals("hieraŭ", kalkuliAĝon(datoAntaux(1), hodiaux))
        assertEquals("5 tagoj", kalkuliAĝon(datoAntaux(5), hodiaux))
        assertEquals("2 semajnoj", kalkuliAĝon(datoAntaux(17), hodiaux))
        assertEquals("4 monatoj", kalkuliAĝon(datoAntaux(130), hodiaux))
        // Ankaŭ pli malnovaj ol 6 monatoj — neniam null
        assertEquals("6 monatoj", kalkuliAĝon(datoAntaux(200), hodiaux))
        assertEquals("1 jaro", kalkuliAĝon(datoAntaux(400), hodiaux))
        assertEquals("2 jaroj", kalkuliAĝon(datoAntaux(800), hodiaux))
        // Estonta dato → "hodiaŭ"
        assertEquals("hodiaŭ", kalkuliAĝon(datoAntaux(-3), hodiaux))
        // Neparsebla dato → null
        assertNull(kalkuliAĝon("ne-dato", hodiaux))
    }

    @Test
    fun sesMonatoj_limo_por_aktiva_kaj_kioNovas() {
        // aktiva = ene de 180 tagoj (kio ankaŭ regas "Kio novas")
        assertTrue(estasEneDeSesMonatoj(datoAntaux(179), hodiaux))
        assertFalse(estasEneDeSesMonatoj(datoAntaux(180), hodiaux))
        assertFalse(estasEneDeSesMonatoj("ne-dato", hodiaux))
        // estonta dato validas kiel "nova"
        assertTrue(estasEneDeSesMonatoj(datoAntaux(-3), hodiaux))
        // sed la markilo montras la aĝon eĉ trans la limo
        assertEquals("6 monatoj", kalkuliAĝon(datoAntaux(180), hodiaux))
        assertEquals("1 jaro", kalkuliAĝon(datoAntaux(364), hodiaux))
    }

    private fun karto(slug: String, dato: String) = KanalKarto(
        kanalo = Kanalo(slug = slug, nomo = slug, podkastaRssUrl = "https://x.com/$slug.rss"),
        plejNovaElsendo = Elsendo(id = "$slug:1", kanaloSlug = slug, titolo = "e", fluo = "", dato = dato),
    )

    @Test
    fun dividuKanalojn_aktiva_estas_ene_de_ses_monatoj() {
        val kartoj = listOf(
            karto("nova", datoAntaux(5)),
            karto("limo", datoAntaux(179)),
            karto("arkivo", datoAntaux(180)),
            karto("malnova", datoAntaux(900)),
        )
        val (aktivaj, arkivaj) = dividuKanalojn(kartoj, hodiaux)
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
            karto("nova2", datoAntaux(5)),
        )
        val (aktivaj, arkivaj) = dividuKanalojn(kartoj, hodiaux)
        assertEquals(listOf("nova2"), aktivaj.map { it.kanalo.slug })
        assertEquals(listOf("nova"), arkivaj.map { it.kanalo.slug })
    }
}
