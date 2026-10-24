package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.data.parser.RssParsilo
import dk.nordfalk.esperanto.domain.model.Kanalo
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Testoj por diskkaŝmemoro de RSS-respondoj.
 *
 * Skribas krudan RSS-tekston al diskkaŝmemoro, legas ĝin reen,
 * re-parsas per RssParsilo, kaj komparas la elsendojn.
 *
 * Rulas en desktopTest ĉar ĝi testas la Desktop-actualigon de la kaŝo
 * kaj direktas [dosierKaŝoBazo] al provizora dosierujo (PR #89):
 * antaŭe la testoj skribis en la VERAN uzantan kaŝujon `~/.esperantoradio/cache/`
 * kaj "purigis" nur per malplenigo — 5 malplenaj dosieroj restis tie.
 */
class DiskKaŝoTest {

    private val parsilo = RssParsilo()

    private val malnovaBazo = dosierKaŝoBazo
    private val provizora: File = File.createTempFile("esperantoradio-diskkaŝo", "").let { f ->
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

    private fun leguFiksaĵon(nomo: String): String {
        val fluo = Thread.currentThread().contextClassLoader
            ?.getResourceAsStream("feeds/$nomo")
            ?: error("Fiksaĵo $nomo ne trovita")
        return fluo.bufferedReader().use { it.readText() }
    }

    @Test
    fun skribuKajLeguKaŝon() = kotlinx.coroutines.test.runTest {
        val nomo = "test_diskkaŝo_roundtrip"
        val enhavo = "<rss><channel><title>Testo</title></channel></rss>"
        skribuKaŝon(nomo, enhavo)
        val legita = leguKaŝon(nomo)
        assertNotNull(legita, "Legu kaŝon devas redoni la skribitan enhavon")
        assertEquals(enhavo, legita)
    }

    @Test
    fun leguNeekzistantanKaŝonRedonasNull() = kotlinx.coroutines.test.runTest {
        val rezulto = leguKaŝon("neniam_ekzistinta_kaŝo_$$")
        assertNull(rezulto, "Legu neekzistantan kaŝon devas redoni null")
    }

    @Test
    fun leguKaŝitajnElsendojnReParsasKorekte() = kotlinx.coroutines.test.runTest {
        val slug = "test_diskkaŝo_kernpunkto"
        val rssTeksto = leguFiksaĵon("kernpunkto_feed.xml")
        val kanalo = Kanalo(
            slug = slug,
            nomo = "Testo Kernpunkto",
            podkastaRssUrl = "https://kern.punkto.info/feed/mp3/",
        )
        // Skribu la krudan RSS-tekston al diskkaŝmemoro
        skribuKaŝon(slug, rssTeksto)

        // Parsu rekte por havi la atendojn
        val atendataj = parsilo.parsuRss(rssTeksto, kanalo)
        assertTrue(atendataj.isNotEmpty(), "La fixture devas enhavi elsendojn")

        // Legu per ElsendoDeponejoImpl
        val deponejo = ElsendoDeponejoImpl(HttpClient(CIO))
        val kaŝitaj = deponejo.leguKaŝitajnElsendojn(kanalo)

        assertNotNull(kaŝitaj, "leguKaŝitajnElsendojn devas redoni elsendojn")
        assertEquals(atendataj.size, kaŝitaj.size, "La nombro da elsendoj devas kongrui")
        assertEquals(atendataj.first().titolo, kaŝitaj.first().titolo, "La unua titolo devas kongrui")
        assertEquals(atendataj.first().id, kaŝitaj.first().id, "La unua ID devas kongrui")
    }

    @Test
    fun leguKaŝitajnElsendojnSenKaŝoRedonasNull() = kotlinx.coroutines.test.runTest {
        val kanalo = Kanalo(
            slug = "neniam_kasita_kanalo_$$",
            nomo = "Neniam Kaŝita",
            podkastaRssUrl = "https://ekzemplo.net/feed.xml",
        )
        val deponejo = ElsendoDeponejoImpl(HttpClient(CIO))
        val rezulto = deponejo.leguKaŝitajnElsendojn(kanalo)
        assertNull(rezulto, "Sen kaŝo, leguKaŝitajnElsendojn devas redoni null")
    }

    @Test
    fun leguĈiujnKaŝitajnElsendojnKolektasPlurajnKanalojn() = kotlinx.coroutines.test.runTest {
        val slug1 = "test_diskkaŝo_kp1"
        val slug2 = "test_diskkaŝo_kp2"
        val rss1 = leguFiksaĵon("kernpunkto_feed.xml")
        val rss2 = leguFiksaĵon("kernpunkto_feed.xml")

        val kanalo1 = Kanalo(slug = slug1, nomo = "Testo KP1", podkastaRssUrl = "https://kern.punkto.info/feed/mp3/")
        val kanalo2 = Kanalo(slug = slug2, nomo = "Testo KP2", podkastaRssUrl = "https://kern.punkto.info/feed/mp3/")

        skribuKaŝon(slug1, rss1)
        skribuKaŝon(slug2, rss2)

        val deponejo = ElsendoDeponejoImpl(HttpClient(CIO))
        val ĉiuj = deponejo.leguĈiujnKaŝitajnElsendojn(listOf(kanalo1, kanalo2))

        assertTrue(ĉiuj.isNotEmpty(), "Devas havi elsendojn de ambaŭ kanaloj")
        assertTrue(ĉiuj.any { it.kanaloSlug == slug1 }, "Devas enhavi elsendojn de kanalo 1")
        assertTrue(ĉiuj.any { it.kanaloSlug == slug2 }, "Devas enhavi elsendojn de kanalo 2")
    }

    @Test
    fun leguĈiujnKaŝitajnElsendojnPlenigasKaŝmemoron() = kotlinx.coroutines.test.runTest {
        val slug = "test_diskkaŝo_kernpunkto"
        val rssTeksto = leguFiksaĵon("kernpunkto_feed.xml")
        val kanalo = Kanalo(slug = slug, nomo = "Testo KP", podkastaRssUrl = "https://kern.punkto.info/feed/mp3/")

        skribuKaŝon(slug, rssTeksto)

        val deponejo = ElsendoDeponejoImpl(HttpClient(CIO))
        // Antaŭe: kaŝmemoro estas malplena
        assertNull(deponejo.getElsendo("neniu"))

        // Legu diskkaŝmemoron — plenigas kaŝmemoron
        val kaŝitaj = deponejo.leguKaŝitajnElsendojn(kanalo)
        assertNotNull(kaŝitaj)
        assertTrue(kaŝitaj.isNotEmpty())

        // Poste: kaŝmemoro estas plena — serĉado funkcias
        val unuaElsendo = kaŝitaj.first()
        val trovita = deponejo.getElsendo(unuaElsendo.id)
        assertNotNull(trovita, "getElsendo devas trovi elsendon el kaŝmemoro post leguKaŝitajnElsendojn")
        assertEquals(unuaElsendo.titolo, trovita.titolo)
    }
}
