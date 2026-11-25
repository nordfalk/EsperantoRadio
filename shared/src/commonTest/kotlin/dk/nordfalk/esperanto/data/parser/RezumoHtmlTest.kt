package dk.nordfalk.esperanto.data.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RezumoHtmlTest {

    private val averto = "Rezumo farita de la sondosiero per AI. Eraroj povas okazi"

    @Test
    fun platajLiniojIĝasParagrafoKunBr() {
        val html = rezumoAlHtml(listOf("Unua punkto.", "Dua punkto.", "", averto), averto)
        assertEquals("<p>Unua punkto.<br/>Dua punkto.</p>\n<p><em>${averto}</em></p>", html)
    }

    @Test
    fun bulitajLiniojIĝasUlLi() {
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
    fun unuaLinioKielTitoloGrasaKajPunktojKielListo() {
        // strukturita rezumo el la rezumo-kaŝo: unua linio = temo, poste punktoj
        val r = analizuRezumon(
            "Komenco de la apostola vojaĝo al Sud-Ameriko.\nAlveno en Montevideo.\nLa papo parolis pri la digno de ĉiu homo.\n\n$averto",
            "Esperanto Programo 08.11.2026", averto, unuaLinioTitolo = true
        )
        assertTrue(r.priskriboHtml.startsWith("<p><strong>Komenco de la apostola vojaĝo al Sud-Ameriko.</strong></p>"), r.priskriboHtml)
        assertTrue("<ul>\n<li>Alveno en Montevideo.</li>\n<li>La papo parolis pri la digno de ĉiu homo.</li>\n</ul>" in r.priskriboHtml, r.priskriboHtml)
        assertTrue("<em>$averto</em>" in r.priskriboHtml)
    }

    @Test
    fun senFlagoUnuaLinioNeGrasas() {
        // ekstrakta fallback (fluanta teksto): neniu titolo-stila unua linio
        val r = analizuRezumon(
            "Unua frazo de fluanta teksto.\nDua frazo.\n\n$averto",
            "Esperanto Programo 08.11.2026", averto
        )
        assertTrue("<strong>" !in r.priskriboHtml, r.priskriboHtml)
        assertTrue("<p>Unua frazo de fluanta teksto.<br/>Dua frazo.</p>" in r.priskriboHtml, r.priskriboHtml)
    }

    @Test
    fun longaLinioTranĉiĝasĈeLimoKajFinaPunktuacioForiĝas() {
        val titolo = derivuTitolon(
            "Post sia apostola vojaĝo al Urugvajo, Argentino kaj Peruo (6.–16.11.), papo Leono rakontis pri la tri landoj kaj iliaj eklezioj kaj multe pli da teksto sekvas ĉi tie",
            "Esperanto Programo 18.11.2026"
        )
        assertEquals("Post sia apostola vojaĝo al Urugvajo, Argentino kaj Peruo (6.–16.11.)", titolo)
    }

    // === kuniguPriskribonKunRezumo ===

    private val avertoNova = "Jen rezumo farita de la sondosiero per AI. Eraroj povas okazi:"

    @Test
    fun malplenaPriskriboNurAvertoKajRezumo() {
        val r = kuniguPriskribonKunRezumo(null, avertoNova, "<ul>\n<li>punkto</li>\n</ul>")
        assertEquals(
            "<p><em>$avertoNova</em></p>\n<ul>\n<li>punkto</li>\n</ul>",
            r
        )
    }

    @Test
    fun purTekstaPriskriboEstasEnvolvataKajRezumoSekvas() {
        val r = kuniguPriskribonKunRezumo("Novaĵoj semajnaj.", avertoNova, "<p>punkto unu</p>")
        assertEquals(
            "<p>Novaĵoj semajnaj.</p>\n<p><em>$avertoNova</em></p>\n<p>punkto unu</p>",
            r
        )
    }

    @Test
    fun htmlPriskriboRestasKiaĜiEstas() {
        val r = kuniguPriskribonKunRezumo("<p>Legado de <em>La Ovo</em></p>", avertoNova, "<p>punkto unu</p>")
        assertEquals(
            "<p>Legado de <em>La Ovo</em></p>\n<p><em>$avertoNova</em></p>\n<p>punkto unu</p>",
            r
        )
    }

    @Test
    fun puraTekstoEskapigasHtmlSignojnKajLinisaltoj() {
        assertEquals(
            "<p>A &amp; B<br/>C</p>",
            priskriboPartoAlHtml("A & B\nC")
        )
    }

    @Test
    fun malplenaPartoRedonasMalplenanĈenon() {
        assertEquals("", priskriboPartoAlHtml("   \n "))
    }
}
