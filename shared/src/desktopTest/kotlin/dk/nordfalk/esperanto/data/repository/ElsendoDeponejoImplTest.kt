package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.domain.model.Kanalo
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Testoj por ElsendoDeponejoImpl — (PR #89) (fiaskpaĝo ne anstataŭigu la kaŝon)
 * kaj G9 (ordigo plej-freŝe-unue, anstataŭ la fluo-ordo de ekz. IRo).
 *
 * Rulas en desktopTest ĉar ĝi bezonas veran diskan kaŝon — [dosierKaŝoBazo] estas
 * direktata al provizora dosierujo (nenia poluado de ~/.esperantoradio/cache/,
 * kontraste al DiskaKaŝoTest — (PR #89) restas malfermita por tiu).
 */
class ElsendoDeponejoImplTest {

    private val malnovaBazo = dosierKaŝoBazo
    private val provizora: File = File.createTempFile("esperantoradio-test", "").let { f ->
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

    /** Fluo kiu listigas plej-malnovajn unue (kiel IRo). */
    private val fluoPlejMalnovajUnue = """
        <?xml version="1.0"?><rss version="2.0"><channel><title>Testa</title>
        <item><title>Malnova</title><pubDate>Mon, 1 Jan 2024 00:00:00 +0000</pubDate>
        <enclosure url="https://x.example/malnova.mp3" type="audio/mpeg"/></item>
        <item><title>Nova</title><pubDate>Wed, 1 Jan 2025 00:00:00 +0000</pubDate>
        <enclosure url="https://x.example/nova.mp3" type="audio/mpeg"/></item>
        </channel></rss>
    """.trimIndent()

    private val fiaskpaĝo = "<html><body><h1>404 Not Found</h1></body></html>"

    private fun kanalo(slug: String) = Kanalo(
        slug = slug, nomo = "Testa", podkastaRssUrl = "https://x.example/feed"
    )

    @Test
    fun ordigasPlejFreŝajnUnue() = runTest {
        // G9: IRo listigas plej-malnovajn unue — la deponejo, ne la UI, devas devigi la ordon
        val motoro = MockEngine { respond(fluoPlejMalnovajUnue, HttpStatusCode.OK) }
        val deponejo = ElsendoDeponejoImpl(HttpClient(motoro))

        val elsendoj = deponejo.ŝargiElsendojn(kanalo("test_ordo"), fortoRefresigi = true)

        assertEquals(listOf("Nova", "Malnova"), elsendoj.map { it.titolo })
    }

    @Test
    fun http404NeAnstataŭigasKaŝon() = runTest {
        // K1: HTTP 404 ĵetas escepton (expectSuccess) → la kaŝitaj elsendoj estas liverataj
        var reĝimo = "bona"
        val motoro = MockEngine {
            if (reĝimo == "404") respond(fiaskpaĝo, HttpStatusCode.NotFound)
            else respond(fluoPlejMalnovajUnue, HttpStatusCode.OK)
        }
        val deponejo = ElsendoDeponejoImpl(HttpClient(motoro))
        val k = kanalo("test_404")

        val unuaj = deponejo.ŝargiElsendojn(k, fortoRefresigi = true)
        assertEquals(2, unuaj.size)

        reĝimo = "404"
        val duaj = deponejo.ŝargiElsendojn(k, fortoRefresigi = true)
        assertEquals(2, duaj.size, "Kaŝitaj elsendoj devas esti liverataj, ne malplena listo")
        assertTrue(
            leguKaŝon("test_404")!!.contains("<rss"),
            "La diska kaŝo devas enhavi la fluon, ne la fiaskpaĝon"
        )
    }

    @Test
    fun fiaskpaĝoKunHttp200NeAnstataŭigasKaŝon() = runTest {
        // K1: fiaskpaĝo kun HTTP 200 (prokura servilo, kaptiva portalo) ne aspektas kiel fluo —
        // kontroluFluecon ĵetas → la kaŝo restas
        var reĝimo = "bona"
        val motoro = MockEngine {
            if (reĝimo == "fiasko200") respond(fiaskpaĝo, HttpStatusCode.OK)
            else respond(fluoPlejMalnovajUnue, HttpStatusCode.OK)
        }
        val deponejo = ElsendoDeponejoImpl(HttpClient(motoro))
        val k = kanalo("test_200fiasko")

        val unuaj = deponejo.ŝargiElsendojn(k, fortoRefresigi = true)
        assertEquals(2, unuaj.size)

        reĝimo = "fiasko200"
        val duaj = deponejo.ŝargiElsendojn(k, fortoRefresigi = true)
        assertEquals(2, duaj.size, "Kaŝitaj elsendoj devas esti liverataj, ne malplena listo")
        assertTrue(
            leguKaŝon("test_200fiasko")!!.contains("<rss"),
            "La diska kaŝo devas enhavi la fluon, ne la fiaskpaĝon"
        )
    }

    @Test
    fun malpuraKaŝoEstasForigata() = runTest {
        // KMP-1S (2026-10-13): kaŝo verkita de antaŭ-#89 versio povas enhavi
        // fiaskpaĝon (kaptiva portalo). Legado devas revenigi null KAJ forigi
        // la malpuran kaŝon — alikaze la sama fiasko ripetiĝus je ĉiu apomfermo.
        skribuKaŝon("test_malpura", fiaskpaĝo)
        val deponejo = ElsendoDeponejoImpl(HttpClient(MockEngine { respond(fluoPlejMalnovajUnue, HttpStatusCode.OK) }))

        val k = kanalo("test_malpura")
        assertNull(deponejo.leguKaŝitajnElsendojn(k), "Malpura kaŝo ne estas legebla kiel flue")
        assertNull(leguKaŝon(k.slug), "La malpura kaŝo devas esti forigita por ke la venonta ŝargado skribu puran")
    }

    // === C39 — in-flight dedup: samtempaj vokoj por la sama kanalo → unu HTTP-peto ===

    @Test
    fun samtempajVokojPorSamaKanaloDedupatas() = runTest {
        val petoj = AtomicInteger(0)
        val unuaPetoEnAgi = CompletableDeferred<Unit>()
        val liberiguUnuanPeton = CompletableDeferred<Unit>()
        val motoro = MockEngine {
            petoj.incrementAndGet()
            unuaPetoEnAgi.complete(Unit)
            liberiguUnuanPeton.await() // tenas la unuan peton en flugo dum la dua voko alvenas
            respond(fluoPlejMalnovajUnue, HttpStatusCode.OK)
        }
        val deponejo = ElsendoDeponejoImpl(HttpClient(motoro))
        val k = kanalo("test_dedup")

        val rezultoj = coroutineScope {
            val unua = async { deponejo.ŝargiElsendojn(k, fortoRefresigi = true) }
            unuaPetoEnAgi.await() // la unua ŝargo jam ekis kaj estas registrita en la deponejo
            val dua = async { deponejo.ŝargiElsendojn(k, fortoRefresigi = true) }
            repeat(10) { yield() } // la dua voko certe aliĝis al la daŭranta ŝargo
            liberiguUnuanPeton.complete(Unit)
            listOf(unua.await(), dua.await())
        }

        assertEquals(1, petoj.get(), "Du samtempaj vokoj devas fari nur unu HTTP-peton")
        assertEquals(2, rezultoj[0].size)
        assertEquals(rezultoj[0], rezultoj[1], "Ambaŭ vokantoj ricevas la saman rezulton")

        // Post la fino la in-flight ero estas forigita: nova forta ŝargo denove elŝutas
        deponejo.ŝargiElsendojn(k, fortoRefresigi = true)
        assertEquals(2, petoj.get(), "Post la fino de la ŝargo nova voko devas denove elŝuti")
    }

    // === G45 (KMP-1H/1R) — limigita korpo-legado kontraŭ OutOfMemoryError ===

    @Test
    fun troGrandaRespondoNeAnstataŭigasKaŝon() = runTest {
        // Misdirektita respondo (ekz. 60 MB MP3 anstataŭ fluo): la ContentLength-
        // antaŭkontrolo ĵetas tuj — neniu bitoko estas legata, la kaŝo restas
        // kaj la apo ne provas enmemorigi la tutan korpon.
        var reĝimo = "bona"
        val motoro = MockEngine {
            if (reĝimo == "trogranda") respond(
                fluoPlejMalnovajUnue, HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentLength, (MAKSIMUMA_FLUA_GRANDECO_BITOKOJ + 1).toString()),
            )
            else respond(fluoPlejMalnovajUnue, HttpStatusCode.OK)
        }
        val deponejo = ElsendoDeponejoImpl(HttpClient(motoro))
        val k = kanalo("test_trogranda")

        val unuaj = deponejo.ŝargiElsendojn(k, fortoRefresigi = true)
        assertEquals(2, unuaj.size)

        reĝimo = "trogranda"
        val duaj = deponejo.ŝargiElsendojn(k, fortoRefresigi = true)
        assertEquals(2, duaj.size, "Tro granda respondo devas liveri la kaŝitajn elsendojn, ne kraŝi")
        assertTrue(
            leguKaŝon("test_trogranda")!!.contains("<rss"),
            "La diska kaŝo devas resti netuŝita — tro granda respondo ne estas kaŝata",
        )
    }

    @Test
    fun leguKorponLimigiteSekvasLaLimon() = runTest {
        val korpo = "abcdefgh"
        val kliento = HttpClient(MockEngine { respond(korpo, HttpStatusCode.OK) })

        // Sufiĉe granda limo: la tuta korpo estas legata ĝuste
        assertEquals(korpo, kliento.get("https://x.example/feed").leguKorponLimigite(limo = 1000))

        // Korpo transpasas la limon laŭ Content-Length → tuj, sen legi
        val troGranda = assertFailsWith<RespondoTroGrandaException> {
            kliento.get("https://x.example/feed").leguKorponLimigite(limo = 2)
        }
        assertEquals(2L, troGranda.limo, "La limo devas esti raportata en la escepto")
    }

    @Test
    fun leguKorponLimigiteFlueTranslimasSenContentLength() = runTest {
        // Chunked-respondo sen Content-Length: la limo estas devigata dum la
        // flu-legado — la legado ĉesas tuj post translimo, ne ĉe la korpa fino.
        val kliento = HttpClient(MockEngine {
            respond(
                ByteReadChannel("tro-longa-respondo".toByteArray()),
                HttpStatusCode.OK,
                headersOf(HttpHeaders.TransferEncoding, "chunked"),
            )
        })
        assertFailsWith<RespondoTroGrandaException> {
            kliento.get("https://x.example/feed").leguKorponLimigite(limo = 5)
        }
    }
}
