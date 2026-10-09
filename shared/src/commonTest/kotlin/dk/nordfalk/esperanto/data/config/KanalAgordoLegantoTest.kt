package dk.nordfalk.esperanto.data.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KanalAgordoLegantoTest {

    private val leganto = KanalAgordoLeganto()

    @Test
    fun legasKomentojnKajPlurliniajnCxenojn() {
        val jsonc = """
            {
                // Tio estas komento
                "kanaloj": [
                    {
                        "kodo": "muzaiko",
                        "nomo": "Muzaiko",
                        "elsendojRssUrl": "https://ekzemplo.com/feed/",
                        "elsendojRssIgnoruTitolon": true
                    }
                ],
                "sugestoj_por_alarmoj":
                "10000/0/6/45/31/0/=muzaiko/=Muzaiko+matene/\
                10001/0/10/0/96/0/=muzaiko/=Semajnfine/\
                "
            }
        """.trimIndent()

        val agordo = leganto.legu(jsonc)

        assertEquals(1, agordo.kanaloj.size)
        assertEquals("muzaiko", agordo.kanaloj[0].kodo)
        assertEquals("Muzaiko", agordo.kanaloj[0].nomo)
        assertEquals("https://ekzemplo.com/feed/", agordo.kanaloj[0].elsendojRssUrl)
        assertTrue(agordo.kanaloj[0].elsendojRssIgnoruTitolon)
    }

    @Test
    fun legasRektaElsendaSonoUrl() {
        val jsonc = """
            {
                "kanaloj": [
                    {
                        "kodo": "muzaiko",
                        "nomo": "Muzaiko",
                        "rektaElsendaSonoUrl": "https://fluo.muzaiko.info/hls/muzaiko/live.m3u8"
                    }
                ]
            }
        """.trimIndent()

        val agordo = leganto.legu(jsonc)

        assertEquals("https://fluo.muzaiko.info/hls/muzaiko/live.m3u8", agordo.kanaloj[0].rektaElsendaSonoUrl)
    }

    @Test
    fun ignorasNekonatajnKampojn() {
        val jsonc = """
            {
                "android": {
                    "kontakt_url": "https://ekzemplo.com",
                    "neEkzistantaKampo": "ignoru min"
                },
                "kanaloj": [
                    {
                        "kodo": "test",
                        "nomo": "Test",
                        "XXXelsendojRssUrl": "malaktivigita"
                    }
                ]
            }
        """.trimIndent()

        val agordo = leganto.legu(jsonc)

        assertEquals(1, agordo.kanaloj.size)
        assertEquals("test", agordo.kanaloj[0].kodo)
        assertNull(agordo.kanaloj[0].elsendojRssUrl) // XXX-prefikso estas nekonata kampo
        assertNotNull(agordo.android)
        assertEquals("https://ekzemplo.com", agordo.android!!.kontakt_url)
    }

    @Test
    fun legasIntervalsKajKomencaKanalo() {
        val jsonc = """
            {
                "intervals": { "playlist": 60, "settings": 3600 },
                "komenca_kanalo": "muzaiko",
                "kanaloj": []
            }
        """.trimIndent()

        val agordo = leganto.legu(jsonc)

        assertEquals(60, agordo.intervals?.playlist)
        assertEquals(3600, agordo.intervals?.settings)
        assertEquals("muzaiko", agordo.komenca_kanalo)
    }

    @Test
    fun malplenaJsonRezultigasMalplenanListon() {
        val jsonc = """{}"""

        val agordo = leganto.legu(jsonc)

        assertTrue(agordo.kanaloj.isEmpty())
    }

    // === FAROTA K5 — ĝusta percent-malkodado de supersignoj ===

    @Test
    fun malkoduUrlKoditajxonCxiujSupersignoj() {
        // %C4%A5 = ĥ (la malnova tabelo erare mapis ĝin al ĵ),
        // %C5%9C = Ŝ (ne ŝ), %C5%AC = Ŭ (ne Ŝ — kaj la linio aperis dufoje)
        assertEquals(
            "ĈĉĜĝĤĥĴĵŜŝŬŭ",
            malkoduUrlKoditajxon("%C4%88%C4%89%C4%9C%C4%9D%C4%A4%C4%A5%C4%B4%C4%B5%C5%9C%C5%9D%C5%AC%C5%AD")
        )
    }

    @Test
    fun malkoduUrlKoditajxonPlusKajLinisalto() {
        assertEquals("Muzaiko matene\nlabortago", malkoduUrlKoditajxon("Muzaiko+matene%0Alabortago"))
        // Jam-malkoditaj signoj estas trairataj senŝanĝe
        assertEquals("Ĥoro Ŭato", malkoduUrlKoditajxon("Ĥoro Ŭato"))
        // Ne-heksa %-sekvenco restas senŝanĝe
        assertEquals("100% certa", malkoduUrlKoditajxon("100%+certa"))
        // %25 = %
        assertEquals("100%", malkoduUrlKoditajxon("100%25"))
    }

    @Test
    fun malkoduUrlKoditajxonKun4BajtaSigno() {
        // U+1F600 (F0 9F 98 80) — la malkodilo subtenas 4-bajtajn UTF-8-signojn
        assertEquals("😀", malkoduUrlKoditajxon("%F0%9F%98%80"))
    }

    @Test
    fun parsuSugestojnPorAlarmojKunSupersignoj() {
        val sugestoj = "10000/0/6/45/31/0/=muzaiko/=Matena+%C4%A5oro+de+%C5%ACato/"

        val alarmoj = parsuSugestojnPorAlarmoj(sugestoj)

        assertEquals(1, alarmoj.size)
        assertEquals("Matena ĥoro de Ŭato", alarmoj[0].etikedo)
        assertEquals("muzaiko", alarmoj[0].kanaloSlug)
    }
}
