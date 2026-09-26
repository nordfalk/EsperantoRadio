package dk.nordfalk.esperanto.data.config

import dk.nordfalk.esperanto.data.config.alKanalo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Ortesto por la VERA bundlita kanalkonfiguro (ne aparta fiksaĵo) —
 * gardas ke la agordo restas parsebla kaj ke specifaj kanaloj kun
 * platform-limigitaj kampoj (`videblaNurSur`, `elsendojApiSekcioj`)
 * ĝuste tralasas la JSONC-parsilon.
 */
class BundledAgordoTesto {

    private fun leguBundled(): String {
        val fluo = BundledAgordoTesto::class.java.classLoader
            ?.getResourceAsStream("esperantoradio_kanaloj_v9.json")
            ?: error("Bundled agordo ne trovita")
        return fluo.bufferedReader().use { it.readText() }
    }

    @Test
    fun bundledAgordoParsasKunCRI() {
        val agordo = KanalAgordoLeganto().legu(leguBundled())

        assertTrue(agordo.kanaloj.isNotEmpty(), "La agordo devas havi kanalojn")
        println("Kanaloj: ${agordo.kanaloj.map { it.kodo }}")

        val cri = agordo.kanaloj.find { it.kodo == "cri" }
        assertNotNull(cri, "cri devas esti en la kanaloj-tabelo (ne en FORPRENITAJ_KANALOJ)")
        assertEquals("android", cri.videblaNurSur, "CRI estas HLS — nur ExoPlayer (Android)")
        assertEquals(10, cri.elsendojApiSekcioj!!.size, "10 sekcioj — por atingi 100+ unikajn elsendojn")
        assertTrue(
            cri.elsendojApiSekcioj!!.all { it.startsWith("https://esperanto.cri.cn/") },
            "Sekci-URL-oj devas esti CRI-paĝoj"
        )

        val kanalo = cri.alKanalo()
        assertTrue(kanalo.havasPodkastojn, "CRI havas elsendojn per la API, kvankam sen RSS")
    }

    @Test
    fun forprenitajKanalojNeEnLaKanalaro() {
        val agordo = KanalAgordoLeganto().legu(leguBundled())
        // Kanaloj en la "FORPRENITAJ_KANALOJ"-sekcio ne devas aperi en la kanalaro
        assertTrue(agordo.kanaloj.none { it.kodo == "radioaktiva" }, "Radio Aktiva estas forprenita")
        assertTrue(agordo.kanaloj.none { it.kodo == "krokoloko" }, "Krokoloko estas forprenita")
    }
}
