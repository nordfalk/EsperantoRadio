package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanalo
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.expectSuccess
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Arkivo + originalo (FAROTA): se `arkivoBazoUrl` estas agordita, la deponejo elŝutas
 * ambaŭ fontojn parolele kaj kunfandas ilin — la arkivo (`<bazo><slug>.xml`, riĉigita
 * fluo) venkas ĉe konflikto, la originala `podkastaRssUrl` aldonas la novajn elsendojn.
 * Se ambaŭ fiaskas, redonas la kaŝon (unue enmemora, poste diska).
 */
class ElsendoDeponejoArkivoTest {

    private val arkivoUrl = "https://javabog.dk/esperanto-radio/kanaloj/testa.xml"
    private val originalaUrl = "https://fonto.example/feed.xml"

    private val kanalo = Kanalo(slug = "testa", nomo = "Testa", podkastaRssUrl = originalaUrl)

    // La arkiva fluo enhavas la eron A kun riĉigita (AI) priskribo; la guid jam estas la fina ID
    private val arkivaFluo = """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0" xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd" xml:lang="eo">
          <channel><title>Testa</title><link>https://fonto.example/</link>
            <item><title>Arkiva ero A</title><guid>testa:https://fonto.example/a</guid>
              <pubDate>Wed, 18 Nov 2026 12:00:00 +0000</pubDate>
              <description><![CDATA[Arkiva priskribo kun AI-rezumo.

Rezumo farita de la sondosiero per AI. Eraroj povas okazi]]></description>
              <enclosure url="https://fonto.example/a.mp3" type="audio/mpeg"/>
              <itunes:duration>00:07:59</itunes:duration></item>
          </channel></rss>
    """.trimIndent()

    // La originala fluo enhavas la eron A (malnova priskribo) kaj la NOVAN eron B
    private val originalaFluo = """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0" xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd" xml:lang="eo">
          <channel><title>Testa</title><link>https://fonto.example/</link>
            <item><title>Originala ero A</title><guid>https://fonto.example/a</guid>
              <pubDate>Wed, 18 Nov 2026 12:00:00 +0000</pubDate>
              <description><![CDATA[Originala priskribo de A]]></description>
              <enclosure url="https://fonto.example/a.mp3" type="audio/mpeg"/></item>
            <item><title>Nova ero B</title><guid>https://fonto.example/b</guid>
              <pubDate>Fri, 20 Nov 2026 12:00:00 +0000</pubDate>
              <description><![CDATA[Nova priskribo de B]]></description>
              <enclosure url="https://fonto.example/b.mp3" type="audio/mpeg"/></item>
          </channel></rss>
    """.trimIndent()

    // Ne skribu al la vera uzanta kaŝo dum la testoj (PR #89)
    private val malnovaBazo = dosierKaŝoBazo
    private val provizora: File = File.createTempFile("esperantoradio-arkivo-test", "").let { f ->
        f.delete()
        File(f.absolutePath + ".dir").apply { mkdirs() }
    }

    init {
        dosierKaŝoBazo = provizora.absolutePath
    }

    @AfterTest
    fun purigu() {
        dosierKaŝoBazo = malnovaBazo
        provizora.deleteRecursively()
    }

    private fun kliento(kondiĉoj: Map<String, Pair<HttpStatusCode, String>>): HttpClient {
        val motoro = MockEngine { peto ->
            val kond = kondiĉoj[peto.url.toString()]
                ?: throw IllegalStateException("Neatendita URL: ${peto.url}")
            if (kond.first.isSuccess()) respond(kond.second, kond.first)
            else respond("eraro", kond.first)
        }
        return HttpClient(motoro) { expectSuccess = true }
    }

    private fun deponejo(kliento: HttpClient, kunArkivo: Boolean = true) = ElsendoDeponejoImpl(
        kliento,
        arkivoBazoUrl = if (kunArkivo) "https://javabog.dk/esperanto-radio/kanaloj/" else null,
    )

    @Test
    fun kunfandasArkivonKajOriginalon() = runTest {
        val deponejo = deponejo(kliento(mapOf(
            arkivoUrl to (HttpStatusCode.OK to arkivaFluo),
            originalaUrl to (HttpStatusCode.OK to originalaFluo),
        )))

        val rezulto = deponejo.ŝargiElsendojnPorKanal(kanalo, fortoRefresigi = true)

        // A (el ambaŭ fontoj) + B (nur en la originalo, pli nova) = 2 elsendoj, B unue
        assertEquals(2, rezulto.size)
        assertEquals("testa:https://fonto.example/b", rezulto[0].id, "La nova ero B aperas unue")
        assertEquals("Nova priskribo de B", rezulto[0].priskribo)
        // Ĉe konflikto la arkivo venkas: sama ID, arkiva priskribo
        assertEquals("testa:https://fonto.example/a", rezulto[1].id)
        assertTrue(rezulto[1].priskribo!!.contains("Arkiva priskribo"), rezulto[1].priskribo)
        assertTrue(rezulto[1].priskribo!!.contains("Rezumo farita de la sondosiero per AI"))
    }

    @Test
    fun sukcesaRetaŜargoSkribasAnkaŭJsonKaŝon() = runTest {
        val deponejo = deponejo(kliento(mapOf(
            arkivoUrl to (HttpStatusCode.OK to arkivaFluo),
            originalaUrl to (HttpStatusCode.OK to originalaFluo),
        )))

        val rezulto = deponejo.ŝargiElsendojnPorKanal(kanalo, fortoRefresigi = true)
        assertEquals(2, rezulto.size)

        // La JSON-kaŝo de la PARSITAJ elsendoj estas skribita (rapida sekva starto, 2026-11-21)
        val jsonTeksto = leguKaŝonJson("testa")
        assertNotNull(jsonTeksto, "Post sukcesa reta ŝargo, leguKaŝonJson devas redoni la parsitajn elsendojn")
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val reen = json.decodeFromString<List<Elsendo>>(jsonTeksto)
        assertEquals(rezulto.map { it.id }.toSet(), reen.map { it.id }.toSet())
    }

    @Test
    fun arkivoFiaskasUzasNurOriginalon() = runTest {
        val deponejo = deponejo(kliento(mapOf(
            arkivoUrl to (HttpStatusCode.NotFound to ""),
            originalaUrl to (HttpStatusCode.OK to originalaFluo),
        )))

        val rezulto = deponejo.ŝargiElsendojnPorKanal(kanalo, fortoRefresigi = true)

        assertEquals(2, rezulto.size)
        assertTrue(rezulto.all { it.id.startsWith("testa:") && !it.id.startsWith("testa:testa:") })
        assertEquals("Originala priskribo de A", rezulto[1].priskribo)
    }

    @Test
    fun originaloFiaskasUzasNurArkivon() = runTest {
        val deponejo = deponejo(kliento(mapOf(
            arkivoUrl to (HttpStatusCode.OK to arkivaFluo),
            originalaUrl to (HttpStatusCode.NotFound to ""),
        )))

        val rezulto = deponejo.ŝargiElsendojnPorKanal(kanalo, fortoRefresigi = true)

        assertEquals(1, rezulto.size)
        assertEquals("testa:https://fonto.example/a", rezulto[0].id)
        assertTrue(rezulto[0].priskribo!!.contains("Arkiva priskribo"))
    }

    @Test
    fun ambaŭFiaskasUzasDiskkaŝon() = runTest {
        // Antaŭa sukcesa ŝargo lasis la arkivan fluon en la diska kaŝo
        skribuKaŝon("testa", arkivaFluo)
        val deponejo = deponejo(kliento(mapOf(
            arkivoUrl to (HttpStatusCode.NotFound to ""),
            originalaUrl to (HttpStatusCode.InternalServerError to ""),
        )))

        val rezulto = deponejo.ŝargiElsendojnPorKanal(kanalo, fortoRefresigi = true)

        assertEquals(1, rezulto.size, "Fiasko de ambaŭ fontoj → la diska kaŝo")
        assertEquals("testa:https://fonto.example/a", rezulto[0].id)
        assertTrue(rezulto[0].priskribo!!.contains("Arkiva priskribo"))
    }

    @Test
    fun senArkivoBazoUrlUzasRekteLaOriginalan() = runTest {
        val deponejo = deponejo(
            kliento(mapOf(originalaUrl to (HttpStatusCode.OK to originalaFluo))),
            kunArkivo = false,
        )

        val rezulto = deponejo.ŝargiElsendojnPorKanal(kanalo, fortoRefresigi = true)

        assertEquals(2, rezulto.size)
        assertEquals("testa:https://fonto.example/b", rezulto[0].id)
        assertEquals("Nova priskribo de B", rezulto[0].priskribo)
    }

    @Test
    fun kunfanduElsendojn_arkivoVenkasKajOriginaloAldonasNovajn() {
        fun ero(id: String, dato: String, priskribo: String) = Elsendo(
            id = id, kanaloSlug = "testa", kanaloNomo = "Testa",
            titolo = "t", dato = dato, priskribo = priskribo, fluo = "https://fonto.example/$id.mp3",
        )
        val arkivaj = listOf(ero("testa:a", "2026-11-18", "arkiva"), ero("testa:b", "2026-11-10", "arkiva b"))
        val originalaj = listOf(ero("testa:a", "2026-11-18", "originala"), ero("testa:c", "2026-11-20", "nova c"))

        val kunfanditaj = kunfanduElsendojn(arkivaj, originalaj)

        assertEquals(listOf("testa:c", "testa:a", "testa:b"), kunfanditaj.map { it.id }, "plej-freŝe-unue, sen duoblaĵoj")
        assertEquals("arkiva", kunfanditaj[1].priskribo, "ĉe konflikto la arkivo venkas")
    }
}
