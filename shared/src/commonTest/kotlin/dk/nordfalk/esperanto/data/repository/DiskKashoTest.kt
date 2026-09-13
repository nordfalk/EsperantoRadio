package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.AppStato
import dk.nordfalk.esperanto.data.parser.RssParsilo
import dk.nordfalk.esperanto.domain.model.Kanalo
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
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
 */
class DiskKashoTest {

    private val parsilo = RssParsilo()

    private fun leguFiksaĵon(nomo: String): String {
        val fluo = Thread.currentThread().contextClassLoader
            ?.getResourceAsStream("feeds/$nomo")
            ?: error("Fiksaĵo $nomo ne trovita")
        return fluo.bufferedReader().use { it.readText() }
    }

    @AfterTest
    fun purigu() {
        AppStato.reset()
    }

    @Test
    fun skribuKajLeguKashon() {
        val nomo = "test_diskkasho_roundtrip"
        val enhavo = "<rss><channel><title>Testo</title></channel></rss>"
        try {
            skribuKashon(nomo, enhavo)
            val legita = leguKashon(nomo)
            assertNotNull(legita, "Legu kaŝon devas redoni la skribitan enhavon")
            assertEquals(enhavo, legita)
        } finally {
            skribuKashon(nomo, "") // purigo
        }
    }

    @Test
    fun leguNeekzistantanKashonRedonasNull() {
        val rezulto = leguKashon("neniam_ekzistinta_kasho_$$")
        assertNull(rezulto, "Legu neekzistantan kaŝon devas redoni null")
    }

    @Test
    fun leguKashitajnElsendojnReParsasKorekte() {
        val slug = "test_diskkasho_kernpunkto"
        val rssTeksto = leguFiksaĵon("kernpunkto_feed.xml")
        val kanalo = Kanalo(
            slug = slug,
            nomo = "Testo Kernpunkto",
            podkastaRssUrl = "https://kern.punkto.info/feed/mp3/",
        )
        try {
            // Skribu la krudan RSS-tekston al diskkaŝmemoro
            skribuKashon(slug, rssTeksto)

            // Parsu rekte por havi la atendojn
            val atendataj = parsilo.parsuRss(rssTeksto, kanalo)
            assertTrue(atendataj.isNotEmpty(), "La fixture devas enhavi elsendojn")

            // Legu per ElsendoDeponejoImpl
            val deponejo = ElsendoDeponejoImpl(HttpClient(CIO))
            val kashitaj = deponejo.leguKashitajnElsendojn(kanalo)

            assertNotNull(kashitaj, "leguKashitajnElsendojn devas redoni elsendojn")
            assertEquals(atendataj.size, kashitaj.size, "La nombro da elsendoj devas kongrui")
            assertEquals(atendataj.first().titolo, kashitaj.first().titolo, "La unua titolo devas kongrui")
            assertEquals(atendataj.first().id, kashitaj.first().id, "La unua ID devas kongrui")
        } finally {
            skribuKashon(slug, "") // purigo
        }
    }

    @Test
    fun leguKashitajnElsendojnSenKashoRedonasNull() {
        val kanalo = Kanalo(
            slug = "neniam_kasita_kanalo_$$",
            nomo = "Neniam Kaŝita",
            podkastaRssUrl = "https://ekzemplo.net/feed.xml",
        )
        val deponejo = ElsendoDeponejoImpl(HttpClient(CIO))
        val rezulto = deponejo.leguKashitajnElsendojn(kanalo)
        assertNull(rezulto, "Sen kaŝo, leguKashitajnElsendojn devas redoni null")
    }

    @Test
    fun leguĈiujnKashitajnElsendojnKolektasPlurajnKanalojn() {
        val slug1 = "test_diskkasho_kp1"
        val slug2 = "test_diskkasho_kp2"
        val rss1 = leguFiksaĵon("kernpunkto_feed.xml")
        val rss2 = leguFiksaĵon("kernpunkto_feed.xml")

        val kanalo1 = Kanalo(slug = slug1, nomo = "Testo KP1", podkastaRssUrl = "https://kern.punkto.info/feed/mp3/")
        val kanalo2 = Kanalo(slug = slug2, nomo = "Testo KP2", podkastaRssUrl = "https://kern.punkto.info/feed/mp3/")

        try {
            skribuKashon(slug1, rss1)
            skribuKashon(slug2, rss2)

            val deponejo = ElsendoDeponejoImpl(HttpClient(CIO))
            val ĉiuj = deponejo.leguĈiujnKashitajnElsendojn(listOf(kanalo1, kanalo2))

            assertTrue(ĉiuj.isNotEmpty(), "Devas havi elsendojn de ambaŭ kanaloj")
            assertTrue(ĉiuj.any { it.kanaloSlug == slug1 }, "Devas enhavi elsendojn de kanalo 1")
            assertTrue(ĉiuj.any { it.kanaloSlug == slug2 }, "Devas enhavi elsendojn de kanalo 2")
        } finally {
            skribuKashon(slug1, "")
            skribuKashon(slug2, "")
        }
    }

    @Test
    fun leguĈiujnKashitajnElsendojnPlenigasKashmemoron() = kotlinx.coroutines.test.runTest {
        val slug = "test_diskkasho_kernpunkto"
        val rssTeksto = leguFiksaĵon("kernpunkto_feed.xml")
        val kanalo = Kanalo(slug = slug, nomo = "Testo KP", podkastaRssUrl = "https://kern.punkto.info/feed/mp3/")

        try {
            skribuKashon(slug, rssTeksto)

            val deponejo = ElsendoDeponejoImpl(HttpClient(CIO))
            // Antaŭe: kaŝmemoro estas malplena
            assertNull(deponejo.getElsendo("neniu"))

            // Legu diskkaŝmemoron — plenigas kaŝmemoron
            val kashitaj = deponejo.leguKashitajnElsendojn(kanalo)
            assertNotNull(kashitaj)
            assertTrue(kashitaj.isNotEmpty())

            // Poste: kaŝmemoro estas plena — serĉado funkcias
            val unuaElsendo = kashitaj.first()
            val trovita = deponejo.getElsendo(unuaElsendo.id)
            assertNotNull(trovita, "getElsendo devas trovi elsendon el kaŝmemoro post leguKashitajnElsendojn")
            assertEquals(unuaElsendo.titolo, trovita.titolo)
        } finally {
            skribuKashon(slug, "")
        }
    }
}
