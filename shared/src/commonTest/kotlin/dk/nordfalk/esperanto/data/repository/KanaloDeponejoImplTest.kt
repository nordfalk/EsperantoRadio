package dk.nordfalk.esperanto.data.repository

import dk.nordfalk.esperanto.data.config.KanalAgordoLeganto
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Testas la platform-filtrilon de la kanallisto (`videblaNurSur`):
 * kanaloj kies fluoj funkcias nur sur unu platformo (ekz. CRI: HLS kiun
 * nur ExoPlayer subtenas) estas kaŝitaj sur la aliaj platformoj.
 *
 * La testoj rulas sur Desktop, do `nunaPlatformo == "desktop"`.
 */
class KanaloDeponejoImplTest {

    private fun deponejo(jsonc: String) =
        KanaloDeponejoImpl(KanalAgordoLeganto()) { jsonc }

    private val agordo = """
        {
            "kanaloj": [
                { "kodo": "muzaiko", "nomo": "Muzaiko" },
                { "kodo": "cri", "nomo": "CRI", "videblaNurSur": "android" },
                { "kodo": "nurkomputile", "nomo": "Nur Komputile", "videblaNurSur": "desktop" }
            ]
        }
    """.trimIndent()

    @Test
    fun kasxasKanalonAlianOlNunaPlatformo() = runTest {
        val kanaloj = deponejo(agordo).getKanalojn()

        // CRI (videblaNurSur=android) kaŝita sur desktop
        assertFalse(kanaloj.any { it.slug == "cri" }, "CRI devas esti kaŝita sur desktop")
        // Kanalo sen limigo restas videbla
        assertTrue(kanaloj.any { it.slug == "muzaiko" }, "Muzaiko devas resti videbla")
        // Kanalo limigita al nuna platformo restas videbla
        assertTrue(kanaloj.any { it.slug == "nurkomputile" }, "Kanalo por desktop devas resti")
        assertEquals(2, kanaloj.size)
    }

    @Test
    fun legasVideblaNurSurKAjElsendojApiSekciojn() = runTest {
        val jsonc = """
            {
                "kanaloj": [
                    {
                        "kodo": "cri",
                        "nomo": "CRI",
                        "videblaNurSur": "android",
                        "elsendojApiSekcioj": [
                            "https://esperanto.cri.cn/aktualajo/page.shtml",
                            "https://esperanto.cri.cn/LuciaStudio/page.shtml"
                        ]
                    }
                ]
            }
        """.trimIndent()

        val kanaloj = deponejo(jsonc).getKanalojn()
        // Sur desktop la kanalo estas kaŝita — kontrolu per la DTO mem
        val dto = KanalAgordoLeganto().legu(jsonc).kanaloj[0]
        assertEquals("android", dto.videblaNurSur)
        assertEquals(2, dto.elsendojApiSekcioj!!.size)
        assertEquals("https://esperanto.cri.cn/aktualajo/page.shtml", dto.elsendojApiSekcioj!![0])
    }
}
