package dk.nordfalk.esperanto.data.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RezumoHtmlTest {

    private val averto = "Rezumo farita de la sondosiero per AI. Eraroj povas okazi"

    @Test
    fun platajLiniojIgxasParagrafoKunBr() {
        val html = rezumoAlHtml(listOf("Unua punkto.", "Dua punkto.", "", averto), averto)
        assertEquals("<p>Unua punkto.<br/>Dua punkto.</p>\n<p><em>${averto}</em></p>", html)
    }

    @Test
    fun bulitajLiniojIgxasUlLi() {
        val html = rezumoAlHtml(listOf("• unua", "- dua", "• tria"), averto)
        assertEquals("<ul>\n<li>unua</li>\n<li>dua</li>\n<li>tria</li>\n</ul>", html)
    }

    @Test
    fun htmlSignojEstasEskapitaj() {
        val html = rezumoAlHtml(listOf("A & B <tag> kaj 3 > 2"), averto)
        assertEquals("<p>A &amp; B &lt;tag&gt; kaj 3 &gt; 2</p>", html)
    }

    @Test
    fun titoloLinioestasElprenitaKajNeEnHtml() {
        val r = analizuRezumon("Titolo: Apostola vojaĝo al Sud-Ameriko\n• punkto unu\n\n$averto", "Esperanto Programo 08.11.2026", averto)
        assertEquals("Apostola vojaĝo al Sud-Ameriko", r.titolo)
        assertTrue("<li>punkto unu</li>" in r.priskriboHtml)
        assertTrue("Titolo:" !in r.priskriboHtml)
        assertTrue("<em>$averto</em>" in r.priskriboHtml)
    }

    @Test
    fun dataTitoloDerivasTitolonElUnuaPunkto() {
        val r = analizuRezumon(
            "Apostola vojaĝo al Sud-Ameriko: Montevideo, Urugvajo kaj la renkontiĝoj kun la fideluloj.\n• dua punkto\n\n$averto",
            "Esperanto Programo 08.11.2026", averto
        )
        assertEquals("Apostola vojaĝo al Sud-Ameriko", r.titolo)
    }

    @Test
    fun senDataTitoloNeniuAŭtomataTitolo() {
        val r = analizuRezumon("Kelkaj punktoj pri io.\n\n$averto", "La Semajno", averto)
        assertNull(r.titolo)
    }

    @Test
    fun longaUnuaLinioTranĉiĝasĈeVorto() {
        val longa = "Tre longa unua frazo pri la tuta enhavo de la elsendo kiu devas esti iomete mallongigita por la titolo"
        val r = analizuRezumon("$longa\n\n$averto", "Esperanto Programo 01.11.2026", averto)
        assertTrue(r.titolo != null)
        assertTrue(r.titolo!!.length <= 70, "titolo: ${r.titolo}")
        assertTrue(r.titolo!!.startsWith("Tre longa unua frazo"))
        assertTrue(!r.titolo!!.endsWith(" "))
    }

    @Test
    fun derivuTitolonNullKiamTroMallonga() {
        assertNull(derivuTitolon("Io.", "Esperanto Programo 01.01.2026"))
        assertNull(derivuTitolon("", "Esperanto Programo 01.01.2026"))
    }

    @Test
    fun longaLinioTranĉiĝasĈeLimoKajFinaPunktuacioForiĝas() {
        val titolo = derivuTitolon(
            "Post sia apostola vojaĝo al Urugvaj, Argentino kaj Peruo (6.–16.11.), papo Leono rakontis pri la tri landoj kaj iliaj eklezioj kaj multe pli da teksto sekvas ĉi tie",
            "Esperanto Programo 18.11.2026"
        )
        assertEquals("Post sia apostola vojaĝo al Urugvaj, Argentino kaj Peruo (6.–16.11.)", titolo)
    }
}
