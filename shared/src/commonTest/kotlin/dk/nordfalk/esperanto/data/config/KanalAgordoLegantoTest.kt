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
}
