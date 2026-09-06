package dk.nordfalk.esperanto.data.parser

import dk.nordfalk.esperanto.domain.model.Kanalo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RssParsiloTest {

    private val parsilo = RssParsilo()

    private fun leguFiksaĵon(nomo: String): String {
        val fluo = Thread.currentThread().contextClassLoader
            ?.getResourceAsStream("feeds/$nomo")
            ?: error("Fiksaĵo $nomo ne trovita")
        return fluo.bufferedReader().use { it.readText() }
    }

    // === Regulo 6.1 — Ĝenerala parsado (Kernpunkto) ===

    @Test
    fun parsasKernpunkton() {
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
        assertTrue(kp226.dauro != null && kp226.dauro!! > 0, "Dauro devas esti > 0: ${kp226.dauro}")
    }

    @Test
    fun kernpunktoHavasHttpsNeHttp() {
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
    fun kernpunktoIdFormato() {
        val fluo = leguFiksaĵon("kernpunkto_feed.xml")
        val kanalo = Kanalo(slug = "kernpunkto", nomo = "Kernpunkto")

        val elsendoj = parsilo.parsuRss(fluo, kanalo)

        for (e in elsendoj) {
            assertTrue(e.id.startsWith("kernpunkto:"), "ID devas komenciĝi per 'kernpunkto:': ${e.id}")
        }
    }

    // === Regulo 6.2 — Varsovia Vento (pluraj <audio> po ero) ===

    @Test
    fun parsasVarsoviaVenton() {
        val fluo = leguFiksaĵon("varsoviavento_feed.xml")
        val kanalo = Kanalo(
            slug = "varsoviavento",
            nomo = "Varsovia Vento",
            podkastaRssUrl = "https://www.podkasto.net/feed/"
        )

        val elsendoj = parsilo.parsuRss(fluo, kanalo)

        assertTrue(elsendoj.isNotEmpty(), "Varsovia Vento devas havi elsendojn")
        println("Varsovia Vento: ${elsendoj.size} elsendoj")

        // Trovu eron kun pluraj partoj (id finiĝas per :1, :2, :3)
        val plurpartaj = elsendoj.filter { it.id.matches(Regex(".*:\\d+$")) }
        if (plurpartaj.isNotEmpty()) {
            val unuaParto = plurpartaj.first()
            println("Plurparta: ${unuaParto.titolo} — ${unuaParto.id}")
            assertTrue(unuaParto.titolo.contains("parto"), "Titolo devas enhavi 'parto': ${unuaParto.titolo}")
            assertTrue(unuaParto.fluo.contains(".mp3"), "Stream devas enhavi .mp3: ${unuaParto.fluo}")
        }

        // Ĉiuj ID-oj devas havi formaton varsoviavento:yyyy-MM-dd:n
        for (e in elsendoj) {
            assertTrue(e.id.startsWith("varsoviavento:"), "ID: ${e.id}")
            assertTrue(e.kanaloSlug == "varsoviavento")
        }
    }

    // === Regulo 6.4 — Vinilkosmo (ipernity-Atom) ===

    @Test
    fun parsasVinilkosmon() {
        val fluo = leguFiksaĵon("vinilkosmo_feed.xml")
        val kanalo = Kanalo(
            slug = "vinilkosmo",
            nomo = "Vinilkosmo",
            podkastaRssUrl = "http://api.ipernity.com/feed/doc?user_id=vinilkosmo&only=audio"
        )

        val elsendoj = parsilo.parsuRss(fluo, kanalo)

        assertTrue(elsendoj.isNotEmpty(), "Vinilkosmo devas havi elsendojn")
        println("Vinilkosmo: ${elsendoj.size} elsendoj")

        // ID-formato: vk:<publikig-sen-tempzonon>
        for (e in elsendoj) {
            assertTrue(e.id.startsWith("vk:"), "ID devas komenciĝi per 'vk:': ${e.id}")
            assertTrue(e.fluo.contains("ipernity.com"), "Stream devas enhavi ipernity.com: ${e.fluo}")
            assertTrue(e.fluo.contains(".mp3"), "Stream devas enhavi .mp3: ${e.fluo}")
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

    // === Regulo 6.7 — Paĝigo ===

    @Test
    fun leguNextLinkElKernpunkto() {
        val fluo = leguFiksaĵon("kernpunkto_feed.xml")

        val nextLink = parsilo.leguNextLink(fluo)

        assertNotNull(nextLink, "Kernpunkto devas havi next-link")
        assertTrue(nextLink.contains("paged=2"), "Next-link devas enhavi paged=2: $nextLink")
    }

    // === Sen fluo → forĵetu ===

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

        val kanalo = Kanalo(slug = "test", nomo = "Test")
        val elsendoj = parsilo.parsuRss(fluo, kanalo)

        assertEquals(1, elsendoj.size, "Nur ero kun fluo devas resti")
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

        val kanalo = Kanalo(slug = "test", nomo = "Test")
        val elsendoj = parsilo.parsuRss(fluo, kanalo)

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

        val kanalo = Kanalo(slug = "test", nomo = "Test")
        val elsendoj = parsilo.parsuRss(fluo, kanalo)

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

        val kanalo = Kanalo(slug = "test", nomo = "Test")
        val elsendoj = parsilo.parsuRss(fluo, kanalo)

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

        val kanalo = Kanalo(slug = "test", nomo = "Test")
        val elsendoj = parsilo.parsuRss(fluo, kanalo)

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

        val kanalo = Kanalo(slug = "test", nomo = "Test")
        val elsendoj = parsilo.parsuRss(fluo, kanalo)

        assertEquals(1, elsendoj.size)
        assertEquals(3077L, elsendoj[0].dauro)
    }

    // === Regulo 6.3 — Peranto (Esperanta Retradio) ===

    @Test
    fun parsasPeranton() {
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
    fun perantoArchiveOrgRekonstruasMallonganUrlon() {
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
        assertEquals("peranto:2025-05-03:1", elsendoj[0].id)
    }

    @Test
    fun perantoKorektasOrkestroSkavidojj() {
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
    fun perantoRekonstruasGoogleDriveUrlon() {
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
    fun perantoSaltasNesubtenatajnGastigantojn() {
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
    fun perantoSaltasKonatajnMalplenajnDatojn() {
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
    fun perantoSenIframeEstasSaltita() {
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
}
