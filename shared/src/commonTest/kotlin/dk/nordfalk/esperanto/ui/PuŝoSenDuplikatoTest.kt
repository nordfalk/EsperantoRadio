package dk.nordfalk.esperanto.ui

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipeLeft
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.NetroveblasEkrano
import dk.nordfalk.esperanto.navigation.Vojo
import dk.nordfalk.esperanto.navConfig
import dk.nordfalk.esperanto.puŝuVojon
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Navigo-elsendoj: neniu celo dufoje en la stako; nova vizito de la sama
 * elsendo komenciĝas freŝe (propra nav3-stato), dum rotacio/rekreado
 * konservas la staton de la kuranta vizito.
 *
 * Kial (2026-11-24): nav3 ŝargas la konservitan staton de eniro laŭ la
 * contentKey (`"$key:${key::class}"` — la TUTA voja valoro). Vojo.ElsendoDetalo
 * tial portas vizito-kodon unikan po malfermo: remalfermo de la sama elsendo
 * ricevas PROPRAN staton (la detala paĝilo antaŭe heredis la svipo-paĝon de
 * la antaŭa vizito — klako sur la 1-a elsendo malfermis la 3-an), dum
 * rotacio/rekreado restaŭras la staton de ĝuste tiu vizito. La cel-identecon
 * por la navigado (puŝuVojon, „jam supre“) komparas id+kanaloSlug.
 */
@OptIn(ExperimentalTestApi::class)
class PuŝoSenDuplikatoTest {

    private val e1 = Elsendo(
        id = "test:e1", kanaloSlug = "k1", titolo = "Elsendo unua",
        fluo = "https://x.test/e1.mp3", dato = "2026-01-03", daŭro = 100,
    )
    private val e2 = Elsendo(
        id = "test:e2", kanaloSlug = "k1", titolo = "Elsendo dua",
        fluo = "https://x.test/e2.mp3", dato = "2026-01-02", daŭro = 100,
    )
    private val e3 = Elsendo(
        id = "test:e3", kanaloSlug = "k1", titolo = "Elsendo tria",
        fluo = "https://x.test/e3.mp3", dato = "2026-01-01", daŭro = 100,
    )

    /** Plej-freŝe-unue, kiel liveras la deponejo. */
    private val elsendoj = listOf(e1, e2, e3)

    @Test
    fun puŝuVojonAnstataŭigasMalnovanVojonDeSamaCelo() {
        // La sama elsendo (= sama celo), sed NOVA vizito — la nova vizito-kodo
        // ne rajtas malhelpi la anstataŭigon (la vizito estas nur ŝlosilo de la
        // konservita stato en nav3, ne parto de la cel-identeco).
        val stako = mutableListOf<NavKey>(
            Vojo.Hejmo,
            Vojo.ElsendoDetalo(e1.id, e1.kanaloSlug, vizito = "v1"),
            Vojo.ElsendoDetalo(e2.id, e2.kanaloSlug, vizito = "v2"),
        )
        val anstataŭigis = puŝuVojon(stako, Vojo.ElsendoDetalo(e1.id, e1.kanaloSlug, vizito = "v3"))
        assertTrue(anstataŭigis, "la malnova vizito de la sama celo devas esti forigita")
        assertEquals(
            listOf<NavKey>(
                Vojo.Hejmo,
                Vojo.ElsendoDetalo(e2.id, e2.kanaloSlug, vizito = "v2"),
                Vojo.ElsendoDetalo(e1.id, e1.kanaloSlug, vizito = "v3"),
            ),
            stako.toList(),
        )
    }

    @Test
    fun puŝuVojonNeAnstataŭigasAliajnVojon() {
        val stako = mutableListOf<NavKey>(
            Vojo.Hejmo,
            Vojo.ElsendoDetalo(e2.id, e2.kanaloSlug, vizito = "a"),
        )
        val anstataŭigis = puŝuVojon(stako, Vojo.ElsendoDetalo(e1.id, e1.kanaloSlug, vizito = "b"))
        assertFalse(anstataŭigis)
        assertEquals(
            listOf<NavKey>(
                Vojo.Hejmo,
                Vojo.ElsendoDetalo(e2.id, e2.kanaloSlug, vizito = "a"),
                Vojo.ElsendoDetalo(e1.id, e1.kanaloSlug, vizito = "b"),
            ),
            stako.toList(),
        )
    }

    /**
     * Reprodukto de la eraro: la sama elsendo malfermiĝas, svipas al alia paĝo,
     * poste (post intertempa detalo de alia elsendo — kiel la kanal-ekrano en la
     * reala fluo) ree malfermiĝas. La rea eniro devas montri LA VIZITATAN
     * elsendon, ne la postlasitan svipo-paĝon de la unua vizito.
     */
    @Test
    fun reaMalfermoDeSamaElsendoMontrasĜinNeLaPostlasitanPaĝon() = runComposeUiTest {
        lateinit var stako: MutableList<NavKey>
        setContent {
            stako = rememberNavBackStack(navConfig, Vojo.Hejmo)
            pTemo {
                NavDisplay(
                    backStack = stako,
                    onBack = { if (stako.size > 1) stako.removeAt(stako.lastIndex) },
                    entryProvider = entryProvider {
                        entry<Vojo.Hejmo> { Text("Hejmo") }
                        entry<Vojo.ElsendoDetalo> { vojo ->
                            // Kiel la vera eniro en App.kt: la plenan elsendon serĉu laŭ la
                            // identifiers de la (slima) vojo — ĉi tie rekte el la fiksa listo.
                            val e = elsendoj.first { it.id == vojo.id }
                            ElsendoEkranoKunSvipo(
                                komencaId = e.id,
                                elsendoj = elsendoj,
                                onReen = {},
                            )
                        }
                    },
                )
            }
        }
        waitForIdle()

        // 1) Malfermu la unuan elsendon — ĝia detalo montriĝas
        runOnUiThread { puŝuVojon(stako, Vojo.ElsendoDetalo(e1.id, e1.kanaloSlug)) }
        waitForIdle()
        onAllNodesWithText(e1.titolo).fetchSemanticsNodes().let {
            assertTrue(it.isNotEmpty(), "devas montri la malfermitan elsendon")
        }

        // 2) Svipu du paĝojn dekstren — la tria elsendo montriĝas
        onRoot().performTouchInput { swipeLeft() }
        waitForIdle()
        onRoot().performTouchInput { swipeLeft() }
        waitForIdle()
        assertTrue(
            onAllNodesWithText(e3.titolo).fetchSemanticsNodes().isNotEmpty(),
            "post du svipoj la tria elsendo devas esti vidata",
        )

        // 3) Intertempa detalo de alia elsendo (la "kanalo-ligilo"-fluo)
        runOnUiThread { puŝuVojon(stako, Vojo.ElsendoDetalo(e2.id, e2.kanaloSlug)) }
        waitForIdle()

        // 4) Ree malfermu la unuan elsendon — ĜIN, ne la postlasitan trian
        runOnUiThread { puŝuVojon(stako, Vojo.ElsendoDetalo(e1.id, e1.kanaloSlug)) }
        waitForIdle()
        assertTrue(
            onAllNodesWithText(e1.titolo).fetchSemanticsNodes().isNotEmpty(),
            "la rea malfermo devas montri la unuan elsendon",
        )
        assertTrue(
            onAllNodesWithText(e3.titolo).fetchSemanticsNodes().isEmpty(),
            "la postlasita svipo-paĝo (la tria elsendo) NE devas montriĝi",
        )
    }

    /**
     * La fina stato de la detala eniro kiam la elsendo ne povas aperi
     * (kanalo forigita el la konfiguro / elsendo ne trovebla eĉ post
     * refreŝigo): klarigo + elirejo, NE eterna ŝarg-indikilo.
     */
    @Test
    fun elsendoNetroveblasEkranoMontrasKlarigonKajElirejon() = runComposeUiTest {
        setContent {
            pTemo {
                NetroveblasEkrano(
                    mesaĝo = "La kanalo de tiu ĉi elsendo ne plu ekzistas (ĝia slug: neekzistas).",
                    onReen = {},
                )
            }
        }
        waitForIdle()
        assertTrue(
            onAllNodesWithText("La kanalo de tiu ĉi elsendo ne plu ekzistas (ĝia slug: neekzistas).").fetchSemanticsNodes().isNotEmpty(),
            "la klarigo devas montriĝi",
        )
        assertTrue(
            onAllNodesWithText("Reen").fetchSemanticsNodes().isNotEmpty(),
            "la reen-butono devas montriĝi",
        )
    }
}
