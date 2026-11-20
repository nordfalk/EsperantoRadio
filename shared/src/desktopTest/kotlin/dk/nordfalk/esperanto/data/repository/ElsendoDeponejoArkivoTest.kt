package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.domain.model.Kanalo
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.expectSuccess
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Arkivo-unue (FAROTA): se `arkivoBazoUrl` estas agordita, la deponejo provas unue
 * `<bazo><slug>.xml` (nia riĉigita fluo) kaj nur poste la originalan `podkastaRssUrl`.
 */
class ElsendoDeponejoArkivoTest {

    private val arkivoUrl = "https://javabog.dk/esperanto-radio/kanaloj/testa.xml"
    private val originalaUrl = "https://fonto.example/feed.xml"

    private val kanalo = Kanalo(slug = "testa", nomo = "Testa", podkastaRssUrl = originalaUrl)

    private val arkivaFluo = """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0" xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd" xml:lang="eo">
          <channel><title>Testa</title><link>https://fonto.example/</link>
            <item><title>Arkiva ero</title><guid>testa:https://fonto.example/a</guid>
              <pubDate>Wed, 18 Nov 2026 12:00:00 +0000</pubDate>
              <description><![CDATA[Rezumo kun averto.

Rezumo farita de la sondosiero per AI. Eraroj povas okazi]]></description>
              <enclosure url="https://fonto.example/a.mp3" type="audio/mpeg"/>
              <itunes:duration>00:07:59</itunes:duration></item>
          </channel></rss>
    """.trimIndent()

    private val originalaFluo = """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0" xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd" xml:lang="eo">
          <channel><title>Testa</title><link>https://fonto.example/</link>
            <item><title>Originala ero</title><guid>https://fonto.example/b</guid>
              <pubDate>Wed, 18 Nov 2026 12:00:00 +0000</pubDate>
              <description><![CDATA[Originala priskribo]]></description>
              <enclosure url="https://fonto.example/b.mp3" type="audio/mpeg"/></item>
          </channel></rss>
    """.trimIndent()

    private fun kliento(kondiĉoj: Map<String, Pair<HttpStatusCode, String>>): HttpClient {
        val motoro = MockEngine { peto ->
            val kond = kondiĉoj[peto.url.toString()]
                ?: throw IllegalStateException("Neatendita URL: ${peto.url}")
            if (kond.first.isSuccess()) respond(kond.second, kond.first)
            else respond("eraro", kond.first)
        }
        return HttpClient(motoro) { expectSuccess = true }
    }

    @Test
    fun arkivoUnueKiamSukcesa() = runTest {
        val deponejo = ElsendoDeponejoImpl(
            kliento(mapOf(arkivoUrl to (HttpStatusCode.OK to arkivaFluo))),
            arkivoBazoUrl = "https://javabog.dk/esperanto-radio/kanaloj/",
        )

        val rezulto = deponejo.ŝargiElsendojnPorKanal(kanalo, fortoRefresigi = true)

        assertEquals(1, rezulto.size)
        assertEquals("testa:https://fonto.example/a", rezulto[0].id, "deArkivo konservas la id-on")
        assertTrue(rezulto[0].priskribo!!.contains("Rezumo farita de la sondosiero per AI"))
    }

    @Test
    fun faligoAlOriginalaFluoKiamArkivoFiaskas() = runTest {
        val deponejo = ElsendoDeponejoImpl(
            kliento(mapOf(
                arkivoUrl to (HttpStatusCode.NotFound to ""),
                originalaUrl to (HttpStatusCode.OK to originalaFluo),
            )),
            arkivoBazoUrl = "https://javabog.dk/esperanto-radio/kanaloj/",
        )

        val rezulto = deponejo.ŝargiElsendojnPorKanal(kanalo, fortoRefresigi = true)

        assertEquals(1, rezulto.size)
        assertEquals("testa:https://fonto.example/b", rezulto[0].id)
        assertEquals("Originala priskribo", rezulto[0].priskribo)
    }

    @Test
    fun senArkivoBazoUrlUzasRekteLaOriginalan() = runTest {
        val deponejo = ElsendoDeponejoImpl(
            kliento(mapOf(originalaUrl to (HttpStatusCode.OK to originalaFluo))),
        )

        val rezulto = deponejo.ŝargiElsendojnPorKanal(kanalo, fortoRefresigi = true)

        assertEquals(1, rezulto.size)
        assertEquals("testa:https://fonto.example/b", rezulto[0].id)
    }
}
