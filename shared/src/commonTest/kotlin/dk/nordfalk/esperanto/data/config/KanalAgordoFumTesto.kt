package dk.nordfalk.esperanto.data.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Fum-testo de la VERA kanalkonfiguro (FAROTA G38).
 *
 * `KanalAgordoLegantoTest` testas nur sintezitan JSONC. Sed la vera
 * `esperantoradio_kanaloj_v9.json` estas la kerno de la apo (AGENTS.md regulo 5):
 * se iu enmetas eraron en ĝin (ekz. `/* */`-blokkomenton, kiun la striptigilo ne
 * subtenas, aŭ forgesitan komon), la apo kraŝus ĉe ekfunkciigo — kaj neniu alia
 * testo kaptus tion.
 */
class KanalAgordoFumTesto {

    @Test
    fun veraKanalkonfiguroParsigxasKajEstasSana() {
        val teksto = leguBundledKanalkonfiguron()
        assertTrue(teksto.isNotBlank(), "La bundled konfiguro ne estu malplena")

        val agordo = KanalAgordoLeganto().legu(teksto)

        // Ne hardkodu la ekzaktan nombron (ĝi kreskas kun ĉiu nova kanalo) —
        // sufiĉa sano-kontrolo: multaj kanaloj, ĉiuj kun kodo kaj nomo
        assertTrue(agordo.kanaloj.size >= 20, "Atendis almenaŭ 20 kanalojn, havas ${agordo.kanaloj.size}")
        assertTrue(agordo.kanaloj.isNotEmpty())

        for (kanalo in agordo.kanaloj) {
            assertTrue(kanalo.kodo.isNotBlank(), "Kanalo sen kodo: $kanalo")
            assertTrue(kanalo.nomo.isNotBlank(), "Kanalo ${kanalo.kodo} sen nomo")
        }

        // Kodoj devas esti unikaj (ili estas ŝlosiloj: slug-identigiloj)
        val kodoj = agordo.kanaloj.map { it.kodo }
        assertEquals(kodoj.size, kodoj.distinct().size, "Kanalo-kodoj devas esti unikaj: ${kodoj}")

        // Almenaŭ kelkaj kanaloj havas RSS-fluon — alie la apo estas senenhava
        val kunRss = agordo.kanaloj.count { !it.elsendojRssUrl.isNullOrBlank() }
        assertTrue(kunRss >= 15, "Atendis almenaŭ 15 kanalojn kun RSS-fluo, havas $kunRss")
    }
}
