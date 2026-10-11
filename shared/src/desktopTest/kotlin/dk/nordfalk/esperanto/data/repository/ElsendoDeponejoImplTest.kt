package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.domain.model.Kanalo
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Testoj por ElsendoDeponejoImpl — FAROTA K1 (fiaskpaĝo ne anstataŭigu la kaŝon)
 * kaj G9 (ordigo plej-freŝe-unue, anstataŭ la fluo-ordo de ekz. IRo).
 *
 * Rulas en desktopTest ĉar ĝi bezonas veran diskkaŝon — [dosierKashoBazo] estas
 * direktata al provizora dosierujo (nenia poluado de ~/.esperantoradio/cache/,
 * kontraste al DiskKashoTest — FAROTA G37 restas malfermita por tiu).
 */
class ElsendoDeponejoImplTest {

    private val malnovaBazo = dosierKashoBazo
    private val provizora: File = File.createTempFile("esperantoradio-test", "").let { f ->
        f.delete()
        File(f.absolutePath + ".dir").apply { mkdirs() }
    }

    init {
        dosierKashoBazo = provizora.absolutePath
    }

    @AfterTest
    fun purigu() {
        dosierKashoBazo = malnovaBazo
        provizora.deleteRecursively()
    }

    /** Fluo kiu listigas plej-malnovajn unue (kiel IRo). */
    private val fluoPlejMalnovajUnue = """
        <?xml version="1.0"?><rss version="2.0"><channel><title>Testa</title>
        <item><title>Malnova</title><pubDate>Mon, 1 Jan 2024 00:00:00 +0000</pubDate>
        <enclosure url="https://x.example/malnova.mp3" type="audio/mpeg"/></item>
        <item><title>Nova</title><pubDate>Wed, 1 Jan 2025 00:00:00 +0000</pubDate>
        <enclosure url="https://x.example/nova.mp3" type="audio/mpeg"/></item>
        </channel></rss>
    """.trimIndent()

    private val fiaskpagho = "<html><body><h1>404 Not Found</h1></body></html>"

    private fun kanalo(slug: String) = Kanalo(
        slug = slug, nomo = "Testa", podkastaRssUrl = "https://x.example/feed"
    )

    @Test
    fun ordigasPlejFreshajnUnue() = runTest {
        // G9: IRo listigas plej-malnovajn unue — la deponejo, ne la UI, devas devigi la ordon
        val motoro = MockEngine { respond(fluoPlejMalnovajUnue, HttpStatusCode.OK) }
        val deponejo = ElsendoDeponejoImpl(HttpClient(motoro))

        val elsendoj = deponejo.sxargxiElsendojn(kanalo("test_ordo"), fortoRefresigi = true)

        assertEquals(listOf("Nova", "Malnova"), elsendoj.map { it.titolo })
    }

    @Test
    fun http404NeAnstatauxigasKashon() = runTest {
        // K1: HTTP 404 ĵetas escepton (expectSuccess) → la kaŝitaj elsendoj estas liverataj
        var reghimo = "bona"
        val motoro = MockEngine {
            if (reghimo == "404") respond(fiaskpagho, HttpStatusCode.NotFound)
            else respond(fluoPlejMalnovajUnue, HttpStatusCode.OK)
        }
        val deponejo = ElsendoDeponejoImpl(HttpClient(motoro))
        val k = kanalo("test_404")

        val unuaj = deponejo.sxargxiElsendojn(k, fortoRefresigi = true)
        assertEquals(2, unuaj.size)

        reghimo = "404"
        val duaj = deponejo.sxargxiElsendojn(k, fortoRefresigi = true)
        assertEquals(2, duaj.size, "Kaŝitaj elsendoj devas esti liverataj, ne malplena listo")
        assertTrue(
            leguKashon("test_404")!!.contains("<rss"),
            "La diskkaŝo devas enhavi la fluon, ne la fiaskpaĝon"
        )
    }

    @Test
    fun fiaskpaghoKunHttp200NeAnstatauxigasKashon() = runTest {
        // K1: fiaskpaĝo kun HTTP 200 (prokura servilo, kaptiva portalo) ne aspektas kiel fluo —
        // kontroluFluecon ĵetas → la kaŝo restas
        var reghimo = "bona"
        val motoro = MockEngine {
            if (reghimo == "fiasko200") respond(fiaskpagho, HttpStatusCode.OK)
            else respond(fluoPlejMalnovajUnue, HttpStatusCode.OK)
        }
        val deponejo = ElsendoDeponejoImpl(HttpClient(motoro))
        val k = kanalo("test_200fiasko")

        val unuaj = deponejo.sxargxiElsendojn(k, fortoRefresigi = true)
        assertEquals(2, unuaj.size)

        reghimo = "fiasko200"
        val duaj = deponejo.sxargxiElsendojn(k, fortoRefresigi = true)
        assertEquals(2, duaj.size, "Kaŝitaj elsendoj devas esti liverataj, ne malplena listo")
        assertTrue(
            leguKashon("test_200fiasko")!!.contains("<rss"),
            "La diskkaŝo devas enhavi la fluon, ne la fiaskpaĝon"
        )
    }
}
