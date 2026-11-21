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
 * Testoj por diska kaŝmemoro de RSS-respondoj.
 *
 * Skribas krudan RSS-tekston al diska kaŝmemoro, legas ĝin reen,
 * re-parsas per RssParsilo, kaj komparas la elsendojn.
 *
 * Rulas en desktopTest ĉar ĝi testas la Desktop-actualigon de la kaŝo
 * kaj direktas [dosierKaŝoBazo] al provizora dosierujo (PR #89):
 * antaŭe la testoj skribis en la VERAN uzantan kaŝujon `~/.esperantoradio/cache/`
 * kaj "purigis" nur per malplenigo — 5 malplenaj dosieroj restis tie.
 */
class DiskaKaŝoTest {

    private val parsilo = RssParsilo()

    private val malnovaBazo = dosierKaŝoBazo
    private val provizora: File = File.createTempFile("esperantoradio-diska-kaŝo", "").let { f ->
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
        val nomo = "test_diska_kaŝo_roundtrip"
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
        val slug = "test_diska_kaŝo_kernpunkto"
        val rssTeksto = leguFiksaĵon("kernpunkto_feed.xml")
        val kanalo = Kanalo(
            slug = slug,
            nomo = "Testo Kernpunkto",
            podkastaRssUrl = "https://kern.punkto.info/feed/mp3/",
        )
        // Skribu la krudan RSS-tekston al diska kaŝmemoro
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
    fun leguKaŝitajnElsendojnElArkivoKonservasId() = kotlinx.coroutines.test.runTest {
        // Nia arkiva fluo skribas la finan id-on kiel guid kun prefikso "<slug>:" (en CDATA).
        // La diska kaŝo devas esti rekonata kiel arkiva — alie konstruuId aldonus duan prefikson.
        val slug = "radiovatikana"
        val rssTeksto = leguFiksaĵon("radiovatikana-arkivo.xml")
        val kanalo = Kanalo(
            slug = slug,
            nomo = "Radio Vatikana",
            podkastaRssUrl = "https://www.vaticannews.va/eo/podcast/esperanto-programo.podcast.xml",
        )
        skribuKaŝon(slug, rssTeksto)

        val deponejo = ElsendoDeponejoImpl(HttpClient(CIO))
        val kaŝitaj = deponejo.leguKaŝitajnElsendojn(kanalo)

        assertNotNull(kaŝitaj)
        assertEquals(3, kaŝitaj.size)
        assertTrue(kaŝitaj.all { it.id.startsWith("radiovatikana:") && !it.id.startsWith("radiovatikana:radiovatikana:") },
            "La id-oj devas konservi la finan arkivan formon sen duobla prefikso: ${kaŝitaj.first().id}")
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
        val slug1 = "test_diska_kaŝo_kp1"
        val slug2 = "test_diska_kaŝo_kp2"
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
        val slug = "test_diska_kaŝo_kernpunkto"
        val rssTeksto = leguFiksaĵon("kernpunkto_feed.xml")
        val kanalo = Kanalo(slug = slug, nomo = "Testo KP", podkastaRssUrl = "https://kern.punkto.info/feed/mp3/")

        skribuKaŝon(slug, rssTeksto)

        val deponejo = ElsendoDeponejoImpl(HttpClient(CIO))
        // Antaŭe: kaŝmemoro estas malplena
        assertNull(deponejo.getElsendo("neniu"))

        // Legu diskan kaŝmemoron — plenigas kaŝmemoron
        val kaŝitaj = deponejo.leguKaŝitajnElsendojn(kanalo)
        assertNotNull(kaŝitaj)
        assertTrue(kaŝitaj.isNotEmpty())

        // Poste: kaŝmemoro estas plena — serĉado funkcias
        val unuaElsendo = kaŝitaj.first()
        val trovita = deponejo.getElsendo(unuaElsendo.id)
        assertNotNull(trovita, "getElsendo devas trovi elsendon el kaŝmemoro post leguKaŝitajnElsendojn")
        assertEquals(unuaElsendo.titolo, trovita.titolo)
    }

    // === JSON-kaŝo de parsitaj elsendoj (rapida starto, 2026-11-21) ===

    @Test
    fun jsonKaŝoMalkodiĝasSenKrudaRss() = kotlinx.coroutines.test.runTest {
        val slug = "test_json_kaŝo"
        val rssTeksto = leguFiksaĵon("kernpunkto_feed.xml")
        val kanalo = Kanalo(slug = slug, nomo = "Testo", podkastaRssUrl = "https://kern.punkto.info/feed/mp3/")
        val atendataj = parsilo.parsuRss(rssTeksto, kanalo)
        assertTrue(atendataj.isNotEmpty())

        // Skribu nur la JSON-kaŝon (kiel post reta freŝigo), NENIU krudan .rss
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        skribuKaŝonJson(slug, json.encodeToString(atendataj))
        assertNull(leguKaŝon(slug), "La kruda .rss ne devas ekzisti en ĉi tiu testo")

        val deponejo = ElsendoDeponejoImpl(HttpClient(CIO))
        val kaŝitaj = deponejo.leguKaŝitajnElsendojn(kanalo)

        assertNotNull(kaŝitaj, "JSON-kaŝo devas esti uzata sen kruda .rss")
        assertEquals(atendataj.size, kaŝitaj.size)
        assertEquals(atendataj.first().id, kaŝitaj.first().id)
        assertEquals(atendataj.first().titolo, kaŝitaj.first().titolo)
    }

    @Test
    fun rompitaJsonKaŝoRezervasAlKrudaRss() = kotlinx.coroutines.test.runTest {
        val slug = "test_json_kaŝo_rompita"
        val rssTeksto = leguFiksaĵon("kernpunkto_feed.xml")
        val kanalo = Kanalo(slug = slug, nomo = "Testo", podkastaRssUrl = "https://kern.punkto.info/feed/mp3/")
        val atendataj = parsilo.parsuRss(rssTeksto, kanalo)

        skribuKaŝonJson(slug, "{ tio ne estas valida JSON")
        skribuKaŝon(slug, rssTeksto)

        val deponejo = ElsendoDeponejoImpl(HttpClient(CIO))
        val kaŝitaj = deponejo.leguKaŝitajnElsendojn(kanalo)

        assertNotNull(kaŝitaj, "Ĉe rompita JSON devas uzi la krudan RSS")
        assertEquals(atendataj.size, kaŝitaj.size)
    }

    @Test
    fun foriguKaŝonForigasAnkaŭJson() = kotlinx.coroutines.test.runTest {
        val slug = "test_forigi_ambaŭ"
        skribuKaŝon(slug, "<rss/>")
        skribuKaŝonJson(slug, "[]")
        assertNotNull(leguKaŝon(slug))
        assertNotNull(leguKaŝonJson(slug))

        foriguKaŝon(slug)

        assertNull(leguKaŝon(slug), "La kruda .rss devas esti forigita")
        assertNull(leguKaŝonJson(slug), "La .json devas esti forigita ankaŭ")
    }

    @Test
    fun leguĈiujnKaŝitajnElsendojnVokasProgresanCallback() = kotlinx.coroutines.test.runTest {
        val slug1 = "test_progresa_1"
        val slug2 = "test_progresa_2"
        val rss = leguFiksaĵon("kernpunkto_feed.xml")
        val kanalo1 = Kanalo(slug = slug1, nomo = "T1", podkastaRssUrl = "https://kern.punkto.info/feed/mp3/")
        val kanalo2 = Kanalo(slug = slug2, nomo = "T2", podkastaRssUrl = "https://kern.punkto.info/feed/mp3/")
        skribuKaŝon(slug1, rss)
        skribuKaŝon(slug2, rss)
        // kanalo sen kaŝo — ne aperas en la callback
        val kanalo3 = Kanalo(slug = "test_progresa_3", nomo = "T3", podkastaRssUrl = "https://kern.punkto.info/feed/mp3/")

        val deponejo = ElsendoDeponejoImpl(HttpClient(CIO))
        val ricevitaj = mutableListOf<Int>()
        val ĉiuj = deponejo.leguĈiujnKaŝitajnElsendojn(listOf(kanalo1, kanalo2, kanalo3)) { ero ->
            ricevitaj.add(ero.size)
        }

        assertEquals(2, ricevitaj.size, "La callback devas esti vokata po preta kanalo (2×), ne por la kanalo sen kaŝo")
        assertEquals(ĉiuj.size, ricevitaj.sum(), "La sumo de la progresaj eroj devas egali la finan liston")
    }
}
