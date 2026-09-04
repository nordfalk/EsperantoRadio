package dk.nordfalk.esperanto.data.parser

import dk.nordfalk.esperanto.domain.model.Kanal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RssParsiloTest {

    private val parsilo = RssParsilo()

    private fun leguFiksaĵon(nomo: String): String {
        val stream = Thread.currentThread().contextClassLoader
            ?.getResourceAsStream("feeds/$nomo")
            ?: error("Fiksaĵo $nomo ne trovita")
        return stream.bufferedReader().use { it.readText() }
    }

    // === Regulo 6.1 — Ĝenerala parsado (Kernpunkto) ===

    @Test
    fun parsasKernpunkton() {
        val fluo = leguFiksaĵon("kernpunkto_feed.xml")
        val kanal = Kanal(
            slug = "kernpunkto",
            nomo = "Kernpunkto",
            podkastaRssUrl = "https://kern.punkto.info/feed/mp3/"
        )

        val elsendoj = parsilo.parsRss(fluo, kanal)

        assertTrue(elsendoj.isNotEmpty(), "Kernpunkto devas havi elsendojn")
        println("Kernpunkto: ${elsendoj.size} elsendoj")

        // Unua elsendo
        val unua = elsendoj.first()
        assertTrue(unua.titolo.contains("KP"), "Titolo devas enhavi KP: ${unua.titolo}")
        assertTrue(unua.stream.startsWith("https://"), "Stream devas komenciĝi per https://: ${unua.stream}")
        assertTrue(unua.stream.endsWith(".mp3"), "Stream devas finii per .mp3: ${unua.stream}")
        assertNotNull(unua.dato, "Dato ne estu nul")
        assertTrue(unua.dato.matches(Regex("\\d{4}-\\d{2}-\\d{2}")), "Dato devas esti yyyy-MM-dd: ${unua.dato}")

        // La dua plej nova ero en la fiksaĵo estas KP226
        val kp226 = elsendoj.find { it.titolo.contains("KP226") }
        assertNotNull(kp226, "KP226 devas ekzisti")
        assertEquals("2025-04-06", kp226.dato)
        assertTrue(kp226.dauro != null && kp226.dauro!! > 0, "Dauro devas esti > 0: ${kp226.dauro}")
    }

    @Test
    fun kernpunktoHavasHttpsNeHttp() {
        val fluo = leguFiksaĵon("kernpunkto_feed.xml")
        val kanal = Kanal(
            slug = "kernpunkto",
            nomo = "Kernpunkto",
            podkastaRssUrl = "https://kern.punkto.info/feed/mp3/"
        )

        val elsendoj = parsilo.parsRss(fluo, kanal)

        // Ĉiuj streamoj devas esti https:// (ne http://) por Kernpunkto
        for (e in elsendoj) {
            assertTrue(e.stream.startsWith("https://"), "Stream devas esti https://: ${e.stream}")
        }
    }

    @Test
    fun kernpunktoIdFormato() {
        val fluo = leguFiksaĵon("kernpunkto_feed.xml")
        val kanal = Kanal(slug = "kernpunkto", nomo = "Kernpunkto")

        val elsendoj = parsilo.parsRss(fluo, kanal)

        for (e in elsendoj) {
            assertTrue(e.id.startsWith("kernpunkto:"), "ID devas komenciĝi per 'kernpunkto:': ${e.id}")
        }
    }

    // === Regulo 6.2 — Varsovia Vento (pluraj <audio> po ero) ===

    @Test
    fun parsasVarsoviaVenton() {
        val fluo = leguFiksaĵon("varsoviavento_feed.xml")
        val kanal = Kanal(
            slug = "varsoviavento",
            nomo = "Varsovia Vento",
            podkastaRssUrl = "https://www.podkasto.net/feed/"
        )

        val elsendoj = parsilo.parsRss(fluo, kanal)

        assertTrue(elsendoj.isNotEmpty(), "Varsovia Vento devas havi elsendojn")
        println("Varsovia Vento: ${elsendoj.size} elsendoj")

        // Trovu eron kun pluraj partoj (id finiĝas per :1, :2, :3)
        val plurpartaj = elsendoj.filter { it.id.matches(Regex(".*:\\d+$")) }
        if (plurpartaj.isNotEmpty()) {
            val unuaParto = plurpartaj.first()
            println("Plurparta: ${unuaParto.titolo} — ${unuaParto.id}")
            assertTrue(unuaParto.titolo.contains("parto"), "Titolo devas enhavi 'parto': ${unuaParto.titolo}")
            assertTrue(unuaParto.stream.contains(".mp3"), "Stream devas enhavi .mp3: ${unuaParto.stream}")
        }

        // Ĉiuj ID-oj devas havi formaton varsoviavento:yyyy-MM-dd:n
        for (e in elsendoj) {
            assertTrue(e.id.startsWith("varsoviavento:"), "ID: ${e.id}")
            assertTrue(e.kanalSlug == "varsoviavento")
        }
    }

    // === Regulo 6.4 — Vinilkosmo (ipernity-Atom) ===

    @Test
    fun parsasVinilkosmon() {
        val fluo = leguFiksaĵon("vinilkosmo_feed.xml")
        val kanal = Kanal(
            slug = "vinilkosmo",
            nomo = "Vinilkosmo",
            podkastaRssUrl = "http://api.ipernity.com/feed/doc?user_id=vinilkosmo&only=audio"
        )

        val elsendoj = parsilo.parsRss(fluo, kanal)

        assertTrue(elsendoj.isNotEmpty(), "Vinilkosmo devas havi elsendojn")
        println("Vinilkosmo: ${elsendoj.size} elsendoj")

        // ID-formato: vk:<publikig-sen-tempzonon>
        for (e in elsendoj) {
            assertTrue(e.id.startsWith("vk:"), "ID devas komenciĝi per 'vk:': ${e.id}")
            assertTrue(e.stream.contains("ipernity.com"), "Stream devas enhavi ipernity.com: ${e.stream}")
            assertTrue(e.stream.contains(".mp3"), "Stream devas enhavi .mp3: ${e.stream}")
            assertTrue(e.dato.matches(Regex("\\d{4}-\\d{2}-\\d{2}")), "Dato: ${e.dato}")
        }
    }

    // === Regulo 6.5 — Titol-derivado ===

    @Test
    fun ignoruTitolonDerivasTitolonElPriskribo() {
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

        val kanal = Kanal(
            slug = "muzaiko",
            nomo = "Muzaiko",
            ignoruTitolon = true
        )

        val elsendoj = parsilo.parsRss(fluo, kanal)

        assertEquals(1, elsendoj.size)
        val titolo = elsendoj[0].titolo
        assertTrue(titolo.contains("priskribo"), "Titolo devas veni el priskribo: $titolo")
        assertTrue(!titolo.contains("<"), "Titolo ne enhavas HTML: $titolo")
        assertTrue(titolo.length <= 200, "Titolo devas esti ≤ 200 signoj: ${titolo.length}")
    }

    // === Regulo 6.7 — Paĝigo ===

    @Test
    fun leguNextLinkElKernpunkto() {
        val fluo = leguFiksaĵon("kernpunkto_feed.xml")

        val nextLink = parsilo.leguNextLink(fluo)

        assertNotNull(nextLink, "Kernpunkto devas havi next-link")
        assertTrue(nextLink.contains("paged=2"), "Next-link devas enhavi paged=2: $nextLink")
    }

    // === Sen stream → forĵetu ===

    @Test
    fun eroSenStreamEstasForjxetita() {
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

        val kanal = Kanal(slug = "test", nomo = "Test")
        val elsendoj = parsilo.parsRss(fluo, kanal)

        assertEquals(1, elsendoj.size, "Nur ero kun stream devas resti")
        assertEquals("Kun sono", elsendoj[0].titolo)
    }

    // === Dat-normigo ===

    @Test
    fun normigasRfc822Daton() {
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

        val kanal = Kanal(slug = "test", nomo = "Test")
        val elsendoj = parsilo.parsRss(fluo, kanal)

        assertEquals(1, elsendoj.size)
        assertEquals("2022-11-09", elsendoj[0].dato)
    }

    @Test
    fun normigasDatonKunDupunktaTempzono() {
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

        val kanal = Kanal(slug = "test", nomo = "Test")
        val elsendoj = parsilo.parsRss(fluo, kanal)

        assertEquals(1, elsendoj.size)
        assertEquals("2013-08-01", elsendoj[0].dato)
    }

    @Test
    fun normigasIso8601Daton() {
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

        val kanal = Kanal(slug = "test", nomo = "Test")
        val elsendoj = parsilo.parsRss(fluo, kanal)

        assertEquals(1, elsendoj.size)
        assertEquals("2018-03-21", elsendoj[0].dato)
    }

    // === iTunes-daŭro ===

    @Test
    fun legasDauronFormatoHhMmSs() {
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

        val kanal = Kanal(slug = "test", nomo = "Test")
        val elsendoj = parsilo.parsRss(fluo, kanal)

        assertEquals(1, elsendoj.size)
        assertEquals(6916L, elsendoj[0].dauro)
    }

    @Test
    fun legasDauronFormatoSekundoj() {
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

        val kanal = Kanal(slug = "test", nomo = "Test")
        val elsendoj = parsilo.parsRss(fluo, kanal)

        assertEquals(1, elsendoj.size)
        assertEquals(3077L, elsendoj[0].dauro)
    }
}
