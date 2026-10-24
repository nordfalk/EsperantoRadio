package dk.nordfalk.esperanto.data.parser

import dk.nordfalk.esperanto.domain.model.Kanalo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RssParsiloTest {

    private val parsilo = RssParsilo()

    private fun leguFiksaĵon(nomo: String): String {
        val fluo = Thread.currentThread().contextClassLoader
            ?.getResourceAsStream("feeds/$nomo")
            ?: error("Fiksaĵo $nomo ne trovita")
        return fluo.bufferedReader().use { it.readText() }
    }

    // === Regulo P6.1 — Ĝenerala parsado (Kernpunkto) ===

    @Test
    fun parsasKernpunkton() = kotlinx.coroutines.test.runTest {
        val fluo = leguFiksaĵon("kernpunkto_feed.xml")
        val kanalo = Kanalo(
            slug = "kernpunkto",
            nomo = "Kernpunkto",
            podkastaRssUrl = "https://kern.punkto.info/feed/mp3/"
        )

        val elsendoj = parsilo.parsuRss(fluo, kanalo)

        assertTrue(elsendoj.isNotEmpty(), "Kernpunkto devas havi elsendojn")
        println("Kernpunkto: ${elsendoj.size} elsendoj")

        // Unua elsendo
        val unua = elsendoj.first()
        assertTrue(unua.titolo.contains("KP"), "Titolo devas enhavi KP: ${unua.titolo}")
        assertTrue(unua.fluo.startsWith("https://"), "Stream devas komenciĝi per https://: ${unua.fluo}")
        assertTrue(unua.fluo.endsWith(".mp3"), "Stream devas finii per .mp3: ${unua.fluo}")
        assertNotNull(unua.dato, "Dato ne estu nul")
        assertTrue(unua.dato.matches(Regex("\\d{4}-\\d{2}-\\d{2}")), "Dato devas esti yyyy-MM-dd: ${unua.dato}")

        // La dua plej nova ero en la fiksaĵo estas KP226
        val kp226 = elsendoj.find { it.titolo.contains("KP226") }
        assertNotNull(kp226, "KP226 devas ekzisti")
        assertEquals("2025-04-06", kp226.dato)
        assertTrue(kp226.daŭro != null && kp226.daŭro!! > 0, "Daŭro devas esti > 0: ${kp226.daŭro}")
    }

    @Test
    fun kernpunktoHavasHttpsNeHttp() = kotlinx.coroutines.test.runTest {
        val fluo = leguFiksaĵon("kernpunkto_feed.xml")
        val kanalo = Kanalo(
            slug = "kernpunkto",
            nomo = "Kernpunkto",
            podkastaRssUrl = "https://kern.punkto.info/feed/mp3/"
        )

        val elsendoj = parsilo.parsuRss(fluo, kanalo)

        // Ĉiuj streamoj devas esti https:// (ne http://) por Kernpunkto
        for (e in elsendoj) {
            assertTrue(e.fluo.startsWith("https://"), "Stream devas esti https://: ${e.fluo}")
        }
    }

    @Test
    fun kernpunktoIdFormato() = kotlinx.coroutines.test.runTest {
        val fluo = leguFiksaĵon("kernpunkto_feed.xml")
        val kanalo = Kanalo(slug = "kernpunkto", nomo = "Kernpunkto")

        val elsendoj = parsilo.parsuRss(fluo, kanalo)

        for (e in elsendoj) {
            // G16: kun guid la id estas <slug>:<guid> (unu prefikso) — la kernpunkto-
            // fiksaĵo havas UUID-guidojn kiel "11a27dc1-…"
            assertTrue(e.id.startsWith("kernpunkto:"), "ID devas komenciĝi per 'kernpunkto:': ${e.id}")
            assertFalse(e.id.startsWith("kernpunkto:kernpunkto:"), "Neniam duobla prefikso: ${e.id}")
        }
        // Ĉiuj id-oj devas esti unikaj
        assertEquals(elsendoj.size, elsendoj.map { it.id }.distinct().size, "Id-oj devas esti unikaj")
    }

    // === G16 — Id-konvencio (docs/nova/04, "Id-konvencioj") ===

    @Test
    fun idGvidasSenPrefiksoKiamGvidasJamKomencaPerSlugo() = kotlinx.coroutines.test.runTest {
        // guid kiu jam komencas per "<slug>:" restas senŝanĝa — neniam duobla prefikso
        val fluo = """
            <?xml version="1.0"?><rss version="2.0"><channel><title>T</title>
            <item><title>Test</title><pubDate>Wed, 1 Jan 2025 00:00:00 +0000</pubDate>
              <enclosure url="https://x.example/a.mp3" type="audio/mpeg"/>
              <guid>test:1234-abc</guid></item>
            </channel></rss>
        """.trimIndent()
        val kanalo = Kanalo(slug = "test", nomo = "Test")
        assertEquals("test:1234-abc", parsilo.parsuRss(fluo, kanalo)[0].id)
    }

    @Test
    fun idAldonasPrecizeUnuPrefiksonAlEksteraGuid() = kotlinx.coroutines.test.runTest {
        val fluo = """
            <?xml version="1.0"?><rss version="2.0"><channel><title>T</title>
            <item><title>Test</title><pubDate>Wed, 1 Jan 2025 00:00:00 +0000</pubDate>
              <enclosure url="https://x.example/a.mp3" type="audio/mpeg"/>
              <guid>https://anchor.fm/ekzemplo/epizodo</guid></item>
            </channel></rss>
        """.trimIndent()
        val kanalo = Kanalo(slug = "test", nomo = "Test")
        assertEquals("test:https://anchor.fm/ekzemplo/epizodo", parsilo.parsuRss(fluo, kanalo)[0].id)
    }

    @Test
    fun deArkivoUzasGuidonSenAjnaPrefikso() = kotlinx.coroutines.test.runTest {
        // La arkiva servilo skribas la jam FINAN id-on kiel guid; la apo devas uzi
        // ĝin senŝanĝe — eĉ se ĝi ne komencas per "<slug>:" — alikaze la id kreskus
        // ĉiun rondiron kaj la inkrementa kunfando (docs/nova/06) neniam trafus.
        val fluo = """
            <?xml version="1.0"?><rss version="2.0"><channel><title>T</title>
            <item><title>Test</title><pubDate>Wed, 1 Jan 2025 00:00:00 +0000</pubDate>
              <enclosure url="https://arkivo.example/a.mp3" type="audio/mpeg"/>
              <guid>kernpunkto:2022-11-09</guid></item>
            <item><title>Test2</title><pubDate>Thu, 2 Jan 2025 00:00:00 +0000</pubDate>
              <enclosure url="https://arkivo.example/b.mp3" type="audio/mpeg"/>
              <guid>ekstera-id</guid></item>
            </channel></rss>
        """.trimIndent()
        val kanalo = Kanalo(
            slug = "kernpunkto", nomo = "Kernpunkto",
            podkastaRssUrl = "https://servilo.example/podkasta_arkivo/kernpunkto.xml",
        )
        val elsendoj = parsilo.parsuRss(fluo, kanalo)
        assertEquals(listOf("kernpunkto:2022-11-09", "ekstera-id"), elsendoj.map { it.id })
    }

    @Test
    fun idEstasStabilaTransRondiro() = kotlinx.coroutines.test.runTest {
        // Rondiro kun la arkiva servilo: la apo konstruas id-on, la servilo skribas
        // ĝin kiel guid en sian fluon, la apo re-parsas — la id devas resti SAMA.
        val komencaFluo = """
            <?xml version="1.0"?><rss version="2.0"><channel><title>T</title>
            <item><title>Test</title><pubDate>Wed, 1 Jan 2025 00:00:00 +0000</pubDate>
              <enclosure url="https://x.example/a.mp3" type="audio/mpeg"/>
              <guid>er-abc-123</guid></item>
            </channel></rss>
        """.trimIndent()
        val kanalo = Kanalo(slug = "test", nomo = "Test")

        val unua = parsilo.parsuRss(komencaFluo, kanalo)[0]
        // La servilo skribas Elsendo.id kiel guid en sian eldonitan fluon:
        val servilaFluo = """
            <?xml version="1.0"?><rss version="2.0"><channel><title>T</title>
            <item><title>Test</title><pubDate>Wed, 1 Jan 2025 00:00:00 +0000</pubDate>
              <enclosure url="https://x.example/a.mp3" type="audio/mpeg"/>
              <guid>${unua.id}</guid></item>
            </channel></rss>
        """.trimIndent()

        assertEquals(unua.id, parsilo.parsuRss(servilaFluo, kanalo)[0].id,
            "Re-parsado devas redoni la saman id-on (stabileco de la rondiro)")
    }

    @Test
    fun konstruuIdSekvasLaKonvencion() {
        val kanalo = Kanalo(slug = "test", nomo = "Test")
        // senŝanĝe: jam prefiksita
        assertEquals("test:abc", parsilo.konstruuId(kanalo, "test:abc", "2025-01-01"))
        // unu prefikso
        assertEquals("test:abc", parsilo.konstruuId(kanalo, "abc", "2025-01-01"))
        // sen guid → <slug>:<dato>
        assertEquals("test:2025-01-01", parsilo.konstruuId(kanalo, null, "2025-01-01"))
        // deArkivo → guid ĉiam senŝanĝe
        assertEquals("aliakanalo:xyz", parsilo.konstruuId(kanalo, "aliakanalo:xyz", "2025-01-01", deArkivo = true))
        assertEquals("nuda-guid", parsilo.konstruuId(kanalo, "nuda-guid", "2025-01-01", deArkivo = true))
    }

    // === Regulo P6.1 — Ĝenerala parsado (La Malfamuloj, anchor.fm — G41-fiksaĵo) ===

    @Test
    fun parsasLamalfamulojn() = kotlinx.coroutines.test.runTest {
        val fluo = leguFiksaĵon("lamalfamuloj_feed.xml")
        val kanalo = Kanalo(
            slug = "lamalfamuloj",
            nomo = "La Malfamuloj",
            podkastaRssUrl = "https://anchor.fm/s/1e1d5f38/podcast/rss",
        )

        val elsendoj = parsilo.parsuRss(fluo, kanalo)

        assertTrue(elsendoj.size >= 114, "La Malfamuloj devas havi almenaŭ 114 elsendojn: ${elsendoj.size}")

        // La konata ero (docs/nova/04, ora test-tabelo): Thu, 26 Dec 2024,
        // daŭro 00:51:17, enclosure enhavanta 392138630…mp3
        val e2sp6gp = elsendoj.first { it.dato == "2024-12-26" }
        assertEquals(3_077L, e2sp6gp.daŭro, "Daŭro devas esti 00:51:17 = 3077 s")
        assertTrue(e2sp6gp.fluo.startsWith("https://"), "Stream devas esti https: ${e2sp6gp.fluo}")
        assertTrue(e2sp6gp.fluo.contains("392138630"), "Stream devas enhavi 392138630: ${e2sp6gp.fluo}")
        assertTrue(e2sp6gp.fluo.endsWith(".mp3"), "Stream devas finii per .mp3: ${e2sp6gp.fluo}")

        // G16: anchor.fm-eroj havas UUID-guidon → id = <slug>:<guid> (unu prefikso)
        for (e in elsendoj) {
            assertTrue(e.id.startsWith("lamalfamuloj:"), "ID devas komenciĝi per 'lamalfamuloj:': ${e.id}")
            assertTrue(e.fluo.startsWith("https://"), "Ĉiuj fluoj devas esti https: ${e.fluo}")
        }
        assertEquals(elsendoj.size, elsendoj.map { it.id }.distinct().size, "Id-oj devas esti unikaj")
    }

    // === Regulo P6.2 — Varsovia Vento (pluraj <audio> po ero) ===

    @Test
    fun parsasVarsoviaVenton() = kotlinx.coroutines.test.runTest {
        val fluo = leguFiksaĵon("varsoviavento_feed.xml")
        // La purigaj modeloj kiel en la vera kanalkonfiguro (regulo P6.6, PR #91).
        // Temperitaj modeloj (?:(?!</p>).) — ili ne povas transiri alineon.
        val kanalo = Kanalo(
            slug = "varsoviavento",
            nomo = "Varsovia Vento",
            podkastaRssUrl = "https://www.podkasto.net/feed/",
            puriguModeloj = listOf(
                "<audio .+?</audio>",
                "<p>(?:(?!</p>).)*?tempo-daŭro(?:(?!</p>).)*?</p>",
                "<p>(?:(?!</p>).)*?Subtenu nin(?:(?!</p>).)*?</p>",
            ),
        )

        val elsendoj = parsilo.parsuRss(fluo, kanalo)

        // Preciza kalkulo (PR #91): 10 eroj → 31 elsendoj (9 × 3 partoj + 182a × 4)
        assertEquals(31, elsendoj.size, "Varsovia Vento devas havi 31 elsendojn")

        // "La 185a elsendo" (2025-04-24) havas 3 partojn kun konataj fluoj
        val vv185 = elsendoj.filter { it.titolo.startsWith("La 185a elsendo") }
        assertEquals(3, vv185.size, "La 185a elsendo havas 3 partojn")
        assertEquals(
            listOf("varsoviavento:2025-04-24:1", "varsoviavento:2025-04-24:2", "varsoviavento:2025-04-24:3"),
            vv185.map { it.id },
        )
        assertEquals(
            listOf("1a parto", "2a parto", "3a parto"),
            vv185.map { it.titolo.removePrefix("La 185a elsendo ") },
        )
        assertTrue(vv185.all { it.fluo.contains("250424VVE185P1") || it.fluo.contains("250424VVE185P2") || it.fluo.contains("250424VVE185P3") },
            "La fluoj de la 185a devas esti VVE185P1/P2/P3: ${vv185.map { it.fluo }}")

        // Regres-kontrolo kontraŭ la malnova "<p>.+?FRazo.+?</p>"-formo: la enkonduka
        // alineo de la 185a-ero devas PLUVIVI la purigadon
        assertTrue(
            (vv185.first().priskribo ?: "").startsWith("Mikrofona renkontiĝo kun Matt"),
            "La enkonduko devas pluvivi: ${vv185.first().priskribo?.take(80)}",
        )

        // Regulo P6.6 (PR #91): la priskribo estas purigata — neniu parto-listilo,
        // neniu reklamo, neniu enkonstruita <audio>-ludilo
        for (e in elsendoj) {
            val priskribo = e.priskribo ?: ""
            assertTrue(priskribo.isNotBlank(), "La priskribo devas havi veran enhavon post la purigado")
            assertFalse(priskribo.contains("tempo-daŭro"), "Parto-listilo devas esti purigita: ${priskribo.take(80)}")
            assertFalse(priskribo.contains("Subtenu nin"), "Reklamo devas esti purigita")
            assertFalse(priskribo.contains("elŝutu"), "Elŝut-ligiloj devas esti purigita")
            assertFalse((e.priskriboHtml ?: "").contains("<audio"), "Enkonstruita ludilo devas esti purigita")
        }

        // Ĉiuj ID-oj devas havi la formaton varsoviavento:yyyy-MM-dd:n
        for (e in elsendoj) {
            assertTrue(e.id.startsWith("varsoviavento:"), "ID: ${e.id}")
            assertTrue(e.kanaloSlug == "varsoviavento")
        }
    }

    // === Regulo P6.4 — Vinilkosmo (ipernity-Atom) ===

    @Test
    fun parsasVinilkosmon() = kotlinx.coroutines.test.runTest {
        val fluo = leguFiksaĵon("vinilkosmo_feed.xml")
        // La puriga modelo kiel en la vera kanalkonfiguro (regulo P6.6, (PR #91))
        val kanalo = Kanalo(
            slug = "vinilkosmo",
            nomo = "Vinilkosmo",
            podkastaRssUrl = "http://api.ipernity.com/feed/doc?user_id=vinilkosmo&only=audio",
            puriguModeloj = listOf("""<p class="who">(?:(?!</p>).)*?</p>"""),
        )

        val elsendoj = parsilo.parsuRss(fluo, kanalo)

        assertTrue(elsendoj.isNotEmpty(), "Vinilkosmo devas havi elsendojn")

        // G29: la titolo venas el <title> de la ero — NE derivata el la enhavo
        // (antaŭe: "VINILKOSMO … has posted: …")
        val amsterdamo = elsendoj.first { it.titolo.contains("Amsterdam") }
        assertTrue(
            amsterdamo.titolo.contains("La Kompanoj kantas Brel"),
            "La titolo de la Amsterdam-ero venas el <title>: ${amsterdamo.titolo}",
        )
        for (e in elsendoj) {
            assertFalse(e.titolo.contains("has posted"), "Titolo ne devenu de la enhavo: ${e.titolo}")
        }

        // G15: la "<p class='who'>…has posted…</p>"-alineo estas purigata el la priskribo
        for (e in elsendoj) {
            assertFalse((e.priskribo ?: "").contains("has posted"), "Priskribo devas esti purigita")
        }

        // ID-formato: vk:<publikig-sen-tempzonon>
        for (e in elsendoj) {
            assertTrue(e.id.startsWith("vk:"), "ID devas komenciĝi per 'vk:': ${e.id}")
            assertTrue(e.fluo.contains("ipernity.com"), "Stream devas enhavi ipernity.com: ${e.fluo}")
            assertTrue(e.fluo.contains(".mp3"), "Stream devas enhavi .mp3: ${e.fluo}")
            assertTrue(e.dato.matches(Regex("\\d{4}-\\d{2}-\\d{2}")), "Dato: ${e.dato}")
        }
    }

    // === Regulo P6.5 — Titol-derivado ===

    @Test
    fun ignoruTitolonDerivasTitolonElPriskribo() = kotlinx.coroutines.test.runTest {
        val fluo = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
              <channel>
                <item>
                  <title>2024-01-15</title>
                  <pubDate>Mon, 15 Jan 2024 12:00:00 +0000</pubDate>
                  <enclosure url="https://ekzemplo.com/epizodo.mp3" type="audio/mpeg"/>
                  <description><![CDATA[<p>Tio estas priskribo kun <b>HTML</b> etikedoj kaj ĝi estas sufiĉe longa por esti uzata kiel titolo anstataŭ la senutila dato en la titolo-kampo.</p>]]></description>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val kanalo = Kanalo(
            slug = "muzaiko",
            nomo = "Muzaiko",
            ignoruTitolon = true
        )

        val elsendoj = parsilo.parsuRss(fluo, kanalo)

        assertEquals(1, elsendoj.size)
        val titolo = elsendoj[0].titolo
        assertTrue(titolo.contains("priskribo"), "Titolo devas veni el priskribo: $titolo")
        assertTrue(!titolo.contains("<"), "Titolo ne enhavas HTML: $titolo")
        assertTrue(titolo.length <= 200, "Titolo devas esti ≤ 200 signoj: ${titolo.length}")
    }

    // === Regulo P6.7 — Paĝigo ===

    @Test
    fun leguNextLinkElKernpunkto() = kotlinx.coroutines.test.runTest {
        val fluo = leguFiksaĵon("kernpunkto_feed.xml")

        val nextLink = parsilo.leguNextLink(fluo)

        assertNotNull(nextLink, "Kernpunkto devas havi next-link")
        assertTrue(nextLink.contains("paged=2"), "Next-link devas enhavi paged=2: $nextLink")
    }

    // === Sen fluo → forĵetu ===

    @Test
    fun eroSenStreamEstasForĵetita() = kotlinx.coroutines.test.runTest {
        val fluo = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
              <channel>
                <item>
                  <title>Kun sono</title>
                  <pubDate>Mon, 15 Jan 2024 12:00:00 +0000</pubDate>
                  <enclosure url="https://ekzemplo.com/kun.mp3" type="audio/mpeg"/>
                  <description>Bona elsendo</description>
                </item>
                <item>
                  <title>Sen sono</title>
                  <pubDate>Tue, 16 Jan 2024 12:00:00 +0000</pubDate>
                  <description>Manka audio</description>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val kanalo = Kanalo(slug = "test", nomo = "Test")
        val elsendoj = parsilo.parsuRss(fluo, kanalo)

        assertEquals(1, elsendoj.size, "Nur ero kun fluo devas resti")
        assertEquals("Kun sono", elsendoj[0].titolo)
    }

    // === Dat-normigo ===

    @Test
    fun normigasRfc822Daton() = kotlinx.coroutines.test.runTest {
        val fluo = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
              <channel>
                <item>
                  <title>Test</title>
                  <pubDate>Wed, 09 Nov 2022 20:59:06 +0000</pubDate>
                  <enclosure url="https://x.com/a.mp3" type="audio/mpeg"/>
                  <description>x</description>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val kanalo = Kanalo(slug = "test", nomo = "Test")
        val elsendoj = parsilo.parsuRss(fluo, kanalo)

        assertEquals(1, elsendoj.size)
        assertEquals("2022-11-09", elsendoj[0].dato)
    }

    @Test
    fun normigasDatonKunDupunktaTempzono() = kotlinx.coroutines.test.runTest {
        val fluo = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
              <channel>
                <item>
                  <title>Test</title>
                  <pubDate>Thu, 01 Aug 2013 12:01:01 +02:00</pubDate>
                  <enclosure url="https://x.com/a.mp3" type="audio/mpeg"/>
                  <description>x</description>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val kanalo = Kanalo(slug = "test", nomo = "Test")
        val elsendoj = parsilo.parsuRss(fluo, kanalo)

        assertEquals(1, elsendoj.size)
        assertEquals("2013-08-01", elsendoj[0].dato)
    }

    @Test
    fun normigasIso8601Daton() = kotlinx.coroutines.test.runTest {
        val fluo = """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
              <entry>
                <title>Test</title>
                <published>2018-03-21T17:00:56+00:00</published>
                <link rel="enclosure" type="audio/mpeg" href="https://x.com/a.mp3"/>
                <content>x</content>
              </entry>
            </feed>
        """.trimIndent()

        val kanalo = Kanalo(slug = "test", nomo = "Test")
        val elsendoj = parsilo.parsuRss(fluo, kanalo)

        assertEquals(1, elsendoj.size)
        assertEquals("2018-03-21", elsendoj[0].dato)
    }

    // === iTunes-daŭro ===

    @Test
    fun legasDaŭronFormatoHhMmSs() = kotlinx.coroutines.test.runTest {
        val fluo = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0" xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd">
              <channel>
                <item>
                  <title>Test</title>
                  <pubDate>Wed, 09 Nov 2022 20:59:06 +0000</pubDate>
                  <enclosure url="https://x.com/a.mp3" type="audio/mpeg"/>
                  <itunes:duration>01:55:16</itunes:duration>
                  <description>x</description>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val kanalo = Kanalo(slug = "test", nomo = "Test")
        val elsendoj = parsilo.parsuRss(fluo, kanalo)

        assertEquals(1, elsendoj.size)
        assertEquals(6916L, elsendoj[0].daŭro)
    }

    @Test
    fun legasDaŭronFormatoSekundoj() = kotlinx.coroutines.test.runTest {
        val fluo = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0" xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd">
              <channel>
                <item>
                  <title>Test</title>
                  <pubDate>Wed, 09 Nov 2022 20:59:06 +0000</pubDate>
                  <enclosure url="https://x.com/a.mp3" type="audio/mpeg"/>
                  <itunes:duration>3077</itunes:duration>
                  <description>x</description>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val kanalo = Kanalo(slug = "test", nomo = "Test")
        val elsendoj = parsilo.parsuRss(fluo, kanalo)

        assertEquals(1, elsendoj.size)
        assertEquals(3077L, elsendoj[0].daŭro)
    }

    // === Regulo P6.3 — Peranto (Esperanta Retradio) ===

    @Test
    fun parsasPeranton() = kotlinx.coroutines.test.runTest {
        val fluo = leguFiksaĵon("peranto_feed.xml")
        val kanalo = Kanalo(
            slug = "peranto",
            nomo = "Esperanta Retradio",
            podkastaRssUrl = "https://esperantaretradio.blogspot.com/feeds/posts/default",
            ignoruTitolon = true,
        )

        val elsendoj = parsilo.parsuRss(fluo, kanalo)

        assertTrue(elsendoj.isNotEmpty(), "Peranto devas havi elsendojn")
        println("Peranto: ${elsendoj.size} elsendoj")

        // Ĉiuj fluo-oj devas esti archive.org/download/...mp3
        for (e in elsendoj) {
            assertTrue(e.fluo.contains("archive.org/download/"), "Stream devas enhavi archive.org/download/: ${e.fluo}")
            assertTrue(e.fluo.endsWith(".mp3"), "Stream devas finii per .mp3: ${e.fluo}")
            assertTrue(e.id.startsWith("peranto:"), "ID devas komenciĝi per 'peranto:': ${e.id}")
            assertTrue(e.dato.matches(Regex("\\d{4}-\\d{2}-\\d{2}")), "Dato: ${e.dato}")
        }
    }

    @Test
    fun perantoArchiveOrgRekonstruasMallonganUrlon() = kotlinx.coroutines.test.runTest {
        val fluo = """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
              <entry>
                <title>Seksismo sabotas gruplaboron</title>
                <published>2025-05-03T08:00:00.001+02:00</published>
                <content type='html'>&lt;iframe src=&quot;https://archive.org/embed/seksismo_sabotas&quot;&gt;&lt;/iframe&gt;</content>
                <link rel="alternate" href="https://esperantaretradio.blogspot.com/2025/05/seksismo-sabotas-gruplaboron.html"/>
              </entry>
            </feed>
        """.trimIndent()

        val kanalo = Kanalo(slug = "peranto", nomo = "Peranto")
        val elsendoj = parsilo.parsuRss(fluo, kanalo)

        assertEquals(1, elsendoj.size)
        assertEquals("https://archive.org/download/seksismo_sabotas/seksismo_sabotas.mp3", elsendoj[0].fluo)
        assertEquals("peranto:2025-05-03", elsendoj[0].id, "Sen guid la id estas <slug>:<dato> (G16)")
    }

    @Test
    fun perantoKorektasOrkestroSkavidojj() = kotlinx.coroutines.test.runTest {
        val fluo = """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
              <entry>
                <title>Test</title>
                <published>2020-01-01T08:00:00.000+02:00</published>
                <content type='html'>&lt;iframe src=&quot;https://archive.org/embed/orkestro_sklavidojj&quot;&gt;&lt;/iframe&gt;</content>
              </entry>
            </feed>
        """.trimIndent()

        val kanalo = Kanalo(slug = "peranto", nomo = "Peranto")
        val elsendoj = parsilo.parsuRss(fluo, kanalo)

        assertEquals(1, elsendoj.size)
        assertEquals("https://archive.org/download/orkestro_sklavidoj/orkestro_sklavidoj.mp3", elsendoj[0].fluo)
    }

    @Test
    fun perantoRekonstruasGoogleDriveUrlon() = kotlinx.coroutines.test.runTest {
        val fluo = """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
              <entry>
                <title>Test</title>
                <published>2020-06-15T08:00:00.000+02:00</published>
                <content type='html'>&lt;iframe src=&quot;https://drive.google.com/file/d/1ABC123XYZ/view&quot;&gt;&lt;/iframe&gt;</content>
              </entry>
            </feed>
        """.trimIndent()

        val kanalo = Kanalo(slug = "peranto", nomo = "Peranto")
        val elsendoj = parsilo.parsuRss(fluo, kanalo)

        assertEquals(1, elsendoj.size)
        assertEquals("https://drive.google.com/u/1/uc?id=1ABC123XYZ&export=download", elsendoj[0].fluo)
    }

    @Test
    fun perantoSaltasNesubtenatajnGastigantojn() = kotlinx.coroutines.test.runTest {
        val fluo = """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
              <entry>
                <title>YouTube</title>
                <published>2020-01-01T08:00:00.000+02:00</published>
                <content type='html'>&lt;iframe src=&quot;https://www.youtube.com/embed/dQw4w9WgXcQ&quot;&gt;&lt;/iframe&gt;</content>
              </entry>
              <entry>
                <title>SoundCloud</title>
                <published>2020-01-02T08:00:00.000+02:00</published>
                <content type='html'>&lt;iframe src=&quot;https://w.soundcloud.com/player/?url=xyz&quot;&gt;&lt;/iframe&gt;</content>
              </entry>
              <entry>
                <title>Vimeo</title>
                <published>2020-01-03T08:00:00.000+02:00</published>
                <content type='html'>&lt;iframe src=&quot;https://vimeo.com/12345&quot;&gt;&lt;/iframe&gt;</content>
              </entry>
              <entry>
                <title>Bona</title>
                <published>2020-01-04T08:00:00.000+02:00</published>
                <content type='html'>&lt;iframe src=&quot;https://archive.org/embed/bona&quot;&gt;&lt;/iframe&gt;</content>
              </entry>
            </feed>
        """.trimIndent()

        val kanalo = Kanalo(slug = "peranto", nomo = "Peranto")
        val elsendoj = parsilo.parsuRss(fluo, kanalo)

        assertEquals(1, elsendoj.size, "Nur archive.org-ero devas resti")
        assertEquals("Bona", elsendoj[0].titolo)
    }

    @Test
    fun perantoSaltasKonatajnMalplenajnDatojn() = kotlinx.coroutines.test.runTest {
        val fluo = """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
              <entry>
                <title>Malplena1</title>
                <published>2019-11-08T08:00:00.000+02:00</published>
                <content type='html'>&lt;iframe src=&quot;https://archive.org/embed/xxx&quot;&gt;&lt;/iframe&gt;</content>
              </entry>
              <entry>
                <title>Malplena2</title>
                <published>2019-09-29T08:00:00.000+02:00</published>
                <content type='html'>&lt;iframe src=&quot;https://archive.org/embed/yyy&quot;&gt;&lt;/iframe&gt;</content>
              </entry>
              <entry>
                <title>Bona</title>
                <published>2020-01-04T08:00:00.000+02:00</published>
                <content type='html'>&lt;iframe src=&quot;https://archive.org/embed/bona&quot;&gt;&lt;/iframe&gt;</content>
              </entry>
            </feed>
        """.trimIndent()

        val kanalo = Kanalo(slug = "peranto", nomo = "Peranto")
        val elsendoj = parsilo.parsuRss(fluo, kanalo)

        assertEquals(1, elsendoj.size, "Malplenaj datoj devas esti saltitaj")
        assertEquals("Bona", elsendoj[0].titolo)
    }

    @Test
    fun perantoSenIframeEstasSaltita() = kotlinx.coroutines.test.runTest {
        val fluo = """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
              <entry>
                <title>Sen iframe</title>
                <published>2020-01-01T08:00:00.000+02:00</published>
                <content type='html'>Neniu iframe ĉi tie</content>
              </entry>
            </feed>
        """.trimIndent()

        val kanalo = Kanalo(slug = "peranto", nomo = "Peranto")
        val elsendoj = parsilo.parsuRss(fluo, kanalo)

        assertEquals(0, elsendoj.size, "Ero sen iframe devas esti saltita")
    }

    /**
     * La MP3-dosiernomo ene de archive.org-arkivaĵo ne ĉiam egalas al la identigilo:
     * `malapero-benda-3` enhavas `Malapero_Benda3.mp3`. La parsilo devas demandi
     * la metadaten-API kaj uzi la veran dosiernomon (regulo P6.3).
     */
    @Test
    fun perantoArchiveOrgUzasVeraDosiernomonElMetadatenoj() = kotlinx.coroutines.test.runTest {
        val fluo = """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
              <entry>
                <title>Malapero de aktoro Benda (3/3)</title>
                <published>2026-09-13T08:00:00.000+02:00</published>
                <content type='html'>&lt;iframe src=&quot;https://archive.org/embed/malapero-benda-3&quot;&gt;&lt;/iframe&gt;</content>
              </entry>
            </feed>
        """.trimIndent()

        val kanalo = Kanalo(slug = "peranto", nomo = "Peranto")
        val elsendoj = parsilo.parsuRss(fluo, kanalo) { url ->
            // Falsa httpKliento: nur metadaten-petoj estas atendataj
            assertEquals("https://archive.org/metadata/malapero-benda-3", url)
            """
                {"files":[
                  {"name":"Malapero_Benda3.mp3","source":"original","format":"VBR MP3"},
                  {"name":"Malapero_Benda3.png","source":"derivative","format":"PNG"},
                  {"name":"Malapero_Benda3_spectrogram.png","source":"derivative","format":"Spectrogram"}
                ]}
            """.trimIndent()
        }

        assertEquals(1, elsendoj.size)
        assertEquals(
            "https://archive.org/download/malapero-benda-3/Malapero_Benda3.mp3",
            elsendoj[0].fluo,
            "Devas uzi la veran dosiernomon el la metadatenoj, ne la divenitan"
        )
    }

    /** Se la metadaten-peto eraras (CORS, senrete), la elsendo tamen aperas kun la divenita URL (regulo A4). */
    @Test
    fun perantoArchiveOrgTolerasMetadatanEraron() = kotlinx.coroutines.test.runTest {
        val fluo = """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
              <entry>
                <title>Test</title>
                <published>2026-09-13T08:00:00.000+02:00</published>
                <content type='html'>&lt;iframe src=&quot;https://archive.org/embed/malapero-benda-3&quot;&gt;&lt;/iframe&gt;</content>
              </entry>
            </feed>
        """.trimIndent()

        val kanalo = Kanalo(slug = "peranto", nomo = "Peranto")
        // Kiel la Js-motoro: ĵetas kotlin.Error (ne Exception) ĉe reteraroj
        val elsendoj = parsilo.parsuRss(fluo, kanalo) { throw Error("Fail to fetch") }

        assertEquals(1, elsendoj.size)
        assertEquals(
            "https://archive.org/download/malapero-benda-3/malapero-benda-3.mp3",
            elsendoj[0].fluo,
            "Reteraro devas retrofali al la divenita dosiernomo"
        )
    }

    /** Malplena aŭ rompita metadaten-respondo → retrofalo al la divenita dosiernomo. */
    @Test
    fun perantoArchiveOrgMalplenajMetadatenojRetrofalas() = kotlinx.coroutines.test.runTest {
        val fluo = """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
              <entry>
                <title>Test</title>
                <published>2026-09-13T08:00:00.000+02:00</published>
                <content type='html'>&lt;iframe src=&quot;https://archive.org/embed/neniomp3-arkivo&quot;&gt;&lt;/iframe&gt;</content>
              </entry>
            </feed>
        """.trimIndent()

        val kanalo = Kanalo(slug = "peranto", nomo = "Peranto")
        val elsendoj = parsilo.parsuRss(fluo, kanalo) { "{}" }

        assertEquals(1, elsendoj.size)
        assertTrue(elsendoj[0].fluo.endsWith("/neniomp3-arkivo.mp3"), "Devas retrofali al la divenita nomo: ${elsendoj[0].fluo}")
    }

    /** Metadaten-peto kiu daŭras tro longe → tempolimo, retrofalo al la divenita dosiernomo. */
    @Test
    fun perantoArchiveOrgMetadatenPetoTranslimas() = kotlinx.coroutines.test.runTest {
        val fluo = """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
              <entry>
                <title>Test</title>
                <published>2026-09-13T08:00:00.000+02:00</published>
                <content type='html'>&lt;iframe src=&quot;https://archive.org/embed/malapero-benda-3&quot;&gt;&lt;/iframe&gt;</content>
              </entry>
            </feed>
        """.trimIndent()

        val kanalo = Kanalo(slug = "peranto", nomo = "Peranto")
        // La falsa kliento pendegas preter la limtempo (runTest uzas virtualan horloĝon)
        val elsendoj = parsilo.parsuRss(fluo, kanalo) { _ ->
            kotlinx.coroutines.delay(RssParsilo.METADATEN_LIMTEMPO_MS + 1_000)
            """{"files":[{"name":"Malapero_Benda3.mp3","source":"original"}]}"""
        }

        assertEquals(1, elsendoj.size)
        assertEquals(
            "https://archive.org/download/malapero-benda-3/malapero-benda-3.mp3",
            elsendoj[0].fluo,
            "Translima metadaten-peto devas retrofali al la divenita nomo"
        )
    }

    // === HTML-stripa bug-riparo ===

    @Test
    fun ĝeneralaParsiloTraktasEntioEskapitanHtml() = kotlinx.coroutines.test.runTest {
        val fluo = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
              <channel>
                <item>
                  <title>Test</title>
                  <pubDate>Wed, 09 Nov 2022 20:59:06 +0000</pubDate>
                  <enclosure url="https://x.com/a.mp3" type="audio/mpeg"/>
                  <description>&lt;p&gt;Hello &lt;b&gt;world&lt;/b&gt;&lt;/p&gt;</description>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val kanalo = Kanalo(slug = "test", nomo = "Test")
        val elsendoj = parsilo.parsuRss(fluo, kanalo)

        assertEquals(1, elsendoj.size)
        val priskribo = elsendoj[0].priskribo
        assertNotNull(priskribo)
        assertTrue(priskribo!!.contains("Hello"), "Priskribo devas enhavi 'Hello': $priskribo")
        assertTrue(priskribo.contains("world"), "Priskribo devas enhavi 'world': $priskribo")
        assertFalse(priskribo.contains("<"), "Priskribo ne enhavas HTML-etikedojn: $priskribo")
        // priskriboHtml devas enhavi la etikedojn
        val html = elsendoj[0].priskriboHtml
        assertNotNull(html, "priskriboHtml ne estu nul")
        assertTrue(html!!.contains("<p>"), "priskriboHtml devas enhavi <p>: $html")
        assertTrue(html.contains("<b>"), "priskriboHtml devas enhavi <b>: $html")
    }

    @Test
    fun puriguHtmlTraktasDuobleEskapitanEniron() = kotlinx.coroutines.test.runTest {
        val eniro = "&lt;p&gt;Test&lt;/p&gt;"
        val rezulto = parsilo.puriguHtml(eniro)
        assertEquals("Test", rezulto, "puriguHtml devas forigi duoble-eskapitan HTML-on: $rezulto")
    }

    @Test
    fun vinilkosmoHavasNeMalplenanPriskribon() = kotlinx.coroutines.test.runTest {
        val fluo = leguFiksaĵon("vinilkosmo_feed.xml")
        val kanalo = Kanalo(
            slug = "vinilkosmo",
            nomo = "Vinilkosmo",
            podkastaRssUrl = "http://api.ipernity.com/feed/doc?user_id=vinilkosmo&only=audio"
        )

        val elsendoj = parsilo.parsuRss(fluo, kanalo)

        assertTrue(elsendoj.isNotEmpty())
        for (e in elsendoj) {
            assertNotNull(e.priskribo, "Priskribo ne estu nul por ${e.id}")
            assertTrue(e.priskribo!!.isNotEmpty(), "Priskribo ne estu malplena por ${e.id}")
        }
    }

    @Test
    fun puriguHtmlKunEtikedojnForigasDanĝerajnEtikedojn() = kotlinx.coroutines.test.runTest {
        val eniro = "<p>Hi</p><script>alert(1)</script><iframe src='evil.com'></iframe>"
        val rezulto = parsilo.puriguHtmlKunEtikedojn(eniro)
        assertFalse(rezulto.contains("<script"), " Rezulto ne enhavas <script>: $rezulto")
        assertFalse(rezulto.contains("<iframe"), "Rezulto ne enhavas <iframe>: $rezulto")
        assertTrue(rezulto.contains("Hi"), "Rezulto devas enhavi 'Hi': $rezulto")
        assertTrue(rezulto.contains("<p>"), "Rezulto devas konservi <p>: $rezulto")
    }

    // === (PR #89) — nur http(s)-fluoj el nefidinda RSS ===

    @Test
    fun forĵasElsendojnKunNeRetajSkemoj() = kotlinx.coroutines.test.runTest {
        // file://, content:// kaj javascript:// el difektita/malica fluo ne estu ludeblaj:
        // ExoPlayer/Coil scipovas malfermi file:// kaj content://
        val rss = """
            <?xml version="1.0"?><rss version="2.0"><channel><title>T</title>
            <item><title>Bona</title><pubDate>Wed, 1 Jan 2025 00:00:00 +0000</pubDate>
              <enclosure url="https://x.example/bona.mp3" type="audio/mpeg"/></item>
            <item><title>Dosiero</title><pubDate>Thu, 2 Jan 2025 00:00:00 +0000</pubDate>
              <enclosure url="file:///data/data/sekreto.mp3" type="audio/mpeg"/></item>
            <item><title>Content</title><pubDate>Fri, 3 Jan 2025 00:00:00 +0000</pubDate>
              <enclosure url="content://media/external/audio/1" type="audio/mpeg"/></item>
            <item><title>Skripto</title><pubDate>Sat, 4 Jan 2025 00:00:00 +0000</pubDate>
              <enclosure url="javascript:alert(1)" type="audio/mpeg"/></item>
            </channel></rss>
        """.trimIndent()
        val kanalo = Kanalo(slug = "test", nomo = "Test")

        val elsendoj = parsilo.parsuRss(rss, kanalo)

        assertEquals(1, elsendoj.size, "Nur la https-fluo devas resti: ${elsendoj.map { it.titolo }}")
        assertEquals("Bona", elsendoj[0].titolo)
    }

    @Test
    fun saniguRetUrlonNuligasNepermesatajnSkemojn() {
        assertEquals("https://x.example/a.mp3", parsilo.saniguRetUrlon("https://x.example/a.mp3"))
        assertEquals("http://x.example/a.mp3", parsilo.saniguRetUrlon(" http://x.example/a.mp3 "))
        assertNull(parsilo.saniguRetUrlon("file:///sekreto"))
        assertNull(parsilo.saniguRetUrlon("content://media/1"))
        assertNull(parsilo.saniguRetUrlon("intent://malico/#Intent;end"))
        assertNull(parsilo.saniguRetUrlon("javascript:alert(1)"))
        assertNull(parsilo.saniguRetUrlon(""))
        assertNull(parsilo.saniguRetUrlon(null))
    }

    // === (PR #89) — ligilo-sanigado en priskriboj ===

    @Test
    fun puriguHtmlForigasDanxerajnLigilojn() {
        val html = "<p>Saluton " +
            "<a href=\"intent://malico/#Intent;action=android.intent.action.CALL;end\">voku</a>" +
            "<a href=\"JaVaScRiPt:alert(1)\">kliku</a>" +
            "<a href=\"data:text/html;base64,PHNjcmlwdD4=\">datumo</a>" +
            "<a href=\"https://bone.example/paĝo\">ĝusta ligilo</a></p>"
        val purigita = parsilo.puriguHtmlKunEtikedojn(html)
        assertFalse(purigita.contains("intent://"), "intent:// devas esti forigita: $purigita")
        assertFalse(purigita.lowercase().contains("javascript:"), "javascript: devas esti forigita: $purigita")
        assertFalse(purigita.contains("data:text/html"), "data:-URL devas esti forigita: $purigita")
        assertTrue(purigita.contains("https://bone.example/paĝo"), "http(s)-ligilo devas resti: $purigita")
    }

    // === (PR #91) — <audio>-fallback ene de la priskribo-HTML ===

    @Test
    fun aŭdioFallbackElPriskriboHtml() = kotlinx.coroutines.test.runTest {
        // Regulo P6.1.4: fluo kies sola sono estas en <audio><source> ene de la priskribo
        // (CDATA/eskapita) — antaŭe la serĉo okazis en la XML-dokumento kaj trovis nenion
        val rss = """
            <?xml version="1.0"?><rss version="2.0"><channel><title>T</title>
            <item><title>Kun sono en priskribo</title>
              <pubDate>Wed, 1 Jan 2025 00:00:00 +0000</pubDate>
              <description><![CDATA[<p>Aŭdu: <audio><source src="https://x.example/sono.mp3" type="audio/mpeg"></audio></p>]]></description>
            </item>
            </channel></rss>
        """.trimIndent()
        val kanalo = Kanalo(slug = "test", nomo = "Test")

        val elsendoj = parsilo.parsuRss(rss, kanalo)

        assertEquals(1, elsendoj.size, "La ero devas esti trovita per la priskribo-<audio>")
        assertEquals("https://x.example/sono.mp3", elsendoj[0].fluo)
    }

    // === (PR #91) — ĝenerala skip-listo de nesubtenataj gastigantoj ===

    @Test
    fun forĵasElsendojnKunNesubtenatajGastigantoj() = kotlinx.coroutines.test.runTest {
        val rss = """
            <?xml version="1.0"?><rss version="2.0"><channel><title>T</title>
            <item><title>Bona</title><pubDate>Wed, 1 Jan 2025 00:00:00 +0000</pubDate>
              <enclosure url="https://x.example/bona.mp3" type="audio/mpeg"/></item>
            <item><title>YouTube</title><pubDate>Thu, 2 Jan 2025 00:00:00 +0000</pubDate>
              <enclosure url="https://www.youtube.com/watch?v=abc" type="video/mpeg"/></item>
            <item><title>SoundCloud</title><pubDate>Fri, 3 Jan 2025 00:00:00 +0000</pubDate>
              <enclosure url="https://soundcloud.com/uzanto/kanto" type="audio/mpeg"/></item>
            <item><title>Vimeo</title><pubDate>Sat, 4 Jan 2025 00:00:00 +0000</pubDate>
              <enclosure url="https://vimeo.com/12345" type="video/mp4"/></item>
            </channel></rss>
        """.trimIndent()
        val kanalo = Kanalo(slug = "test", nomo = "Test")

        val elsendoj = parsilo.parsuRss(rss, kanalo)

        assertEquals(listOf("Bona"), elsendoj.map { it.titolo },
            "Nur la ludebla fluo devas resti (video-gastigantoj estas saltataj)")
    }

    // === (PR #91) — dato-, daŭro- kaj enclosure-randaĵoj ===

    @Test
    fun normigasDatojnSenTempoKajKunKvarliteraMonato() {
        // "2024-01-05" sen T — antaŭe la tuta ero estis forĵetita (return null)
        assertEquals("2024-01-05", parsilo.normigiDaton("2024-01-05"))
        // "Sept" (4 literoj) — antaŭe ne rekonita
        assertEquals("2024-09-05", parsilo.normigiDaton("5 Sept 2024"))
        // ...kaj la 3-literaj formoj daŭre funkcias
        assertEquals("2025-04-24", parsilo.normigiDaton("Thu, 24 Apr 2025 12:00:00 +0000"))
        assertNull(parsilo.normigiDaton("ne estas dato"))
    }

    @Test
    fun leguDaŭronKunFrakciajSekundoj() {
        assertEquals(3_077L, parsilo.leguDaŭron("00:51:17.47"))
        assertEquals(3_077L, parsilo.leguDaŭron("00:51:17"))
        assertEquals(90L, parsilo.leguDaŭron("1:30"))
        assertNull(parsilo.leguDaŭron("abc"))
    }

    @Test
    fun preferasAŭdianEnclosure() = kotlinx.coroutines.test.runTest {
        // Iuj fluoj metas bild-enclosure unue — la audio devas veni unue (PR #91)
        val rss = """
            <?xml version="1.0"?><rss version="2.0"><channel><title>T</title>
            <item><title>Bilda unue</title><pubDate>Wed, 1 Jan 2025 00:00:00 +0000</pubDate>
              <enclosure url="https://x.example/bildo.jpg" type="image/jpeg" length="1000"/>
              <enclosure url="https://x.example/sono.mp3" type="audio/mpeg" length="999999"/>
            </item>
            </channel></rss>
        """.trimIndent()
        val kanalo = Kanalo(slug = "test", nomo = "Test")

        val elsendoj = parsilo.parsuRss(rss, kanalo)

        assertEquals(1, elsendoj.size)
        assertEquals("https://x.example/sono.mp3", elsendoj[0].fluo,
            "La audio-enclosure devas esti preferata super la bildo")
    }
}
