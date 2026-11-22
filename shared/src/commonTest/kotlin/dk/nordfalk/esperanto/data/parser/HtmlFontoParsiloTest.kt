package dk.nordfalk.esperanto.data.parser

import dk.nordfalk.esperanto.domain.model.Kanalo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Testoj por la HTML-mediatheka fonto (radio-frei.de, 2026-11-22) kaj por la
 * arkiva fluo kiun nia servilo generas el ĝi. Fiksaĵoj estas veraj paĝoj
 * konservitaj en server/data/privata/kaŝo/ dum loka rulo de la arkiv-konstruilo.
 */
class HtmlFontoParsiloTest {

    private val parsilo = HtmlFontoParsilo()

    private fun leguFiksaĵon(nomo: String): String {
        val fluo = Thread.currentThread().contextClassLoader
            ?.getResourceAsStream("feeds/$nomo")
            ?: error("Fiksaĵo $nomo ne trovita")
        return fluo.bufferedReader().use { it.readText() }
    }

    @Test
    fun parsasListPaĝonDeRadioFrei() {
        val html = leguFiksaĵon("radiofrei-listo.html")
        val bazo = "https://www.radio-frei.de/index.php?iid=podcast&ssuchtext=esperanto"

        val eroj = parsilo.parsuListon(html, "radiofrei", bazo)

        // 2 artikoloj en la fiksaĵo (ĉiu ligilo aperas dufoje: h3 + previewimage — dedupliko)
        assertEquals(2, eroj.size)
        val unua = eroj[0]
        assertEquals("radiofrei:9062", unua.id)
        assertEquals("TURINGIO INTERNACIA 122", unua.titolo)
        assertEquals("2021-12-12", unua.dato)
        assertNotNull(unua.priskribo)
        assertTrue(unua.priskribo!!.contains("Adventsonntag"), unua.priskribo)
        // relativa ligilo absolutigita kontraŭ la bazo
        assertTrue(unua.retpaĝoUrl.startsWith("https://www.radio-frei.de/index.php"), unua.retpaĝoUrl)
        assertTrue(unua.retpaĝoUrl.contains("kartikel_id=9062"), unua.retpaĝoUrl)
        assertEquals("radiofrei:9047", eroj[1].id)
        assertEquals("2021-11-15", eroj[1].dato)
    }

    @Test
    fun eltiruFluonElArtikolPaĝo() {
        val html = leguFiksaĵon("radiofrei-artikolo.html")
        val fluo = parsilo.eltiruFluon(html)
        assertEquals("https://audio.radio-frei.de/podcast/Esperanto/20211212%20turint%20klein.mp3", fluo)
    }

    @Test
    fun arkivaFluoDeRadioFreiParsiĝasKielDeArkivo() = runTest {
        // la arkiva fluo generita de la servilo el la HTML-fonto — deArkivo konservas la finan id-on
        val fluo = leguFiksaĵon("radiofrei-arkivo.xml")
        val kanalo = Kanalo(
            slug = "radiofrei",
            nomo = "Radio Frei",
            htmlFontoUrl = "https://www.radio-frei.de/index.php?iid=podcast&ssuchtext=esperanto",
        )
        assertTrue(kanalo.havasPodkastojn, "kanalo kun htmlFontoUrl havu podkastojn en la UI")

        val elsendoj = RssParsilo().parsuRss(fluo, kanalo, deArkivo = true)

        assertEquals(2, elsendoj.size)
        val unua = elsendoj.first()
        assertEquals("radiofrei:9062", unua.id)
        assertEquals("TURINGIO INTERNACIA 122", unua.titolo)
        assertEquals("2021-12-12", unua.dato)
        assertEquals("https://audio.radio-frei.de/podcast/Esperanto/20211212%20turint%20klein.mp3", unua.fluo)
        assertTrue(unua.retpaĝoUrl!!.contains("kartikel_id=9062"))
    }
}
