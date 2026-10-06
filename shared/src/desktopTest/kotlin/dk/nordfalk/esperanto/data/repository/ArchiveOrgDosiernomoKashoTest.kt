package dk.nordfalk.esperanto.data.repository

import com.russhwolf.settings.PreferencesSettings
import dk.nordfalk.esperanto.data.parser.RssParsilo
import dk.nordfalk.esperanto.domain.model.Kanalo
import kotlinx.coroutines.test.runTest
import java.util.prefs.Preferences
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Testoj por la persista kesto de archive.org-dosiernomoj (regulo 6.3).
 *
 * Kontrolas plurajn tavolojn: ke RssParsilo demandas la metadaten-API unufoje
 * po identigilo, ke la rezulto estas konservata daŭre (videbla ankaŭ por nova
 * kesto, kiel post restarto de la procezo), kaj ke la korektita dosiernomo
 * vere aperas en la elsendo-URL.
 */
class ArchiveOrgDosiernomoKashoTest {

    private val nodo = Preferences.userRoot().node("esperantoradio-test-${System.nanoTime()}")
    private val settings = PreferencesSettings(nodo)

    @AfterTest
    fun purigu() = nodo.removeNode()

    private val perantoFluo = """
        <?xml version="1.0" encoding="UTF-8"?>
        <feed xmlns="http://www.w3.org/2005/Atom">
          <entry>
            <title>Malapero de aktoro Benda (3/3)</title>
            <published>2026-09-13T08:00:00.000+02:00</published>
            <content type='html'>&lt;iframe src=&quot;https://archive.org/embed/malapero-benda-3&quot;&gt;&lt;/iframe&gt;</content>
          </entry>
        </feed>
    """.trimIndent()

    private val kanalo = Kanalo(slug = "peranto", nomo = "Esperanta Retradio")

    @Test
    fun demandasMetadatenojnUnufojeKajKonservasIlinDaure() = runTest {
        var petoj = 0
        val parsilo = RssParsilo(ArchiveOrgDosiernomoKasho(settings))
        val falsaKliento: suspend (String) -> String = { _ ->
            petoj++
            """{"files":[{"name":"Malapero_Benda3.mp3","source":"original","format":"VBR MP3"},{"name":"Malapero_Benda3.png","source":"derivative","format":"PNG"}]}"""
        }

        // Unua parsado: demandas la metadatenojn
        val unua = parsilo.parsuRss(perantoFluo, kanalo, falsaKliento)
        assertEquals("https://archive.org/download/malapero-benda-3/Malapero_Benda3.mp3", unua[0].fluo)
        assertEquals(1, petoj, "Metadaten-peto devas okazi unufoje")

        // Dua parsado (alia fluo-kopio): la kesto jam konas la nomon
        val dua = parsilo.parsuRss(perantoFluo, kanalo, falsaKliento)
        assertEquals("https://archive.org/download/malapero-benda-3/Malapero_Benda3.mp3", dua[0].fluo)
        assertEquals(1, petoj, "Ne devas denove demandi — la kesto havas la nomon")

        // Nova parsilo kun nova kesto (kiel post restarto): legas la nomon el Settings
        val novaParsilo = RssParsilo(ArchiveOrgDosiernomoKasho(settings))
        val tria = novaParsilo.parsuRss(perantoFluo, kanalo, falsaKliento)
        assertEquals("https://archive.org/download/malapero-benda-3/Malapero_Benda3.mp3", tria[0].fluo)
        assertEquals(1, petoj, "Persisto devas teni — neniu nova peto post restarto")
    }

    @Test
    fun fiaskintaPetoNeEstasKonservata() = runTest {
        var petoj = 0
        val parsilo = RssParsilo(ArchiveOrgDosiernomoKasho(settings))
        val fiaskantaKliento: suspend (String) -> String = { _ ->
            petoj++
            throw Error("Fail to fetch")
        }

        // Senrete: retrofalas al la divenita nomo
        val unua = parsilo.parsuRss(perantoFluo, kanalo, fiaskantaKliento)
        assertEquals("https://archive.org/download/malapero-benda-3/malapero-benda-3.mp3", unua[0].fluo)
        assertEquals(1, petoj)

        // Kiam reto reaperas, la malsukceso ne devas esti konservita — reprovu
        val sukcesaKliento: suspend (String) -> String = { _ ->
            petoj++
            """{"files":[{"name":"Malapero_Benda3.mp3","source":"original","format":"VBR MP3"}]}"""
        }
        val dua = parsilo.parsuRss(perantoFluo, kanalo, sukcesaKliento)
        assertEquals("https://archive.org/download/malapero-benda-3/Malapero_Benda3.mp3", dua[0].fluo)
        assertEquals(2, petoj, "Fiasko ne devas esti kaŝita — reprovi kiam la reto revenas")
    }
}
