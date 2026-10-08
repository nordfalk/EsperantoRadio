package dk.nordfalk.esperanto.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipeDown
import dk.nordfalk.esperanto.data.repository.LudatojDeponejoMaketo
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.repository.ElsendoDeponejo
import dk.nordfalk.esperanto.domain.repository.KanaloDeponejo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * UI-testoj por HejmoEkrano — testas plurajn tavolojn (UI + deponejo).
 *
 * Uzas falsan ElsendoDeponejo kiu liveras realajn testelsendojn
 * por "Kio novas" kaj "Kio popularas".
 *
 * Datoj estas kalkulitaj dinamike relative al hodiaux por ke la testoj
 * cxiiam validu (ne malnovigxu post 6 monatoj).
 */
@OptIn(ExperimentalTestApi::class, ExperimentalTime::class)
class HejmoEkranoTest {

    private val hodiaux = Clock.System.todayIn(TimeZone.UTC)

    private fun datoAntaux(tagoloj: Int): String =
        (hodiaux.minus(DatePeriod(days = tagoloj))).toString()

    private val testKanaloj = listOf(
        Kanalo(slug = "muzaiko", nomo = "Muzaiko", rektaElsendaSonoUrl = "https://x.com/m.m3u8"),
        Kanalo(slug = "kernpunkto", nomo = "Kernpunkto", podkastaRssUrl = "https://x.com/k.rss"),
        Kanalo(slug = "varsoviavento", nomo = "Varsovia Vento", podkastaRssUrl = "https://x.com/v.rss"),
    )

    private val testElsendoj = listOf(
        Elsendo(id = "kp:1", kanaloSlug = "kernpunkto", titolo = "Kernpunkto epizodo 1", fluo = "", dato = datoAntaux(5)),
        Elsendo(id = "kp:2", kanaloSlug = "kernpunkto", titolo = "Kernpunkto epizodo 2", fluo = "", dato = datoAntaux(12)),
        Elsendo(id = "vv:1", kanaloSlug = "varsoviavento", titolo = "Varsovia Vento epizodo 1", fluo = "", dato = datoAntaux(3)),
        Elsendo(id = "vv:2", kanaloSlug = "varsoviavento", titolo = "Varsovia Vento epizodo 2", fluo = "", dato = datoAntaux(17)),
        // Malnovaj elsendoj (>6 monatoj) — ne aperas en "Kio novas", nur en "Kio popularas"
        Elsendo(id = "kp:malnova", kanaloSlug = "kernpunkto", titolo = "Kernpunkto malnova", fluo = "", dato = datoAntaux(200)),
        Elsendo(id = "vv:malnova", kanaloSlug = "varsoviavento", titolo = "Varsovia Vento malnova", fluo = "", dato = datoAntaux(220)),
    )

    private fun falsaKanaloDeponejo(kanaloj: List<Kanalo> = testKanaloj) = object : KanaloDeponejo {
        private val f = MutableStateFlow(kanaloj)
        override fun observiKanalojn() = f.asStateFlow()
        override suspend fun getKanalojn(fortoRefresigi: Boolean) = f.value
        override suspend fun getKanalo(slug: String) = f.value.find { it.slug == slug }
    }

    /** Registras la `fortoRefresigi`-parametron de ĉiu voko al sxargxiElsendojnPorKanal. */
    private val refresxigoVokoj = mutableListOf<Boolean>()

    private fun falsaElsendoDeponejo(elsendoj: List<Elsendo> = testElsendoj) = object : ElsendoDeponejo {
        override fun observiElsendojn(kanaloSlug: String): Flow<List<Elsendo>> =
            MutableStateFlow(elsendoj.filter { it.kanaloSlug == kanaloSlug }).asStateFlow()

        override suspend fun getElsendojn(kanaloSlug: String, fortoRefresigi: Boolean): List<Elsendo> =
            elsendoj.filter { it.kanaloSlug == kanaloSlug }

        override suspend fun getElsendo(id: String): Elsendo? = elsendoj.find { it.id == id }

        override suspend fun sercxiElsendojn(teksto: String, limo: Int): List<Elsendo> =
            elsendoj.filter { it.titolo.contains(teksto, ignoreCase = true) }.take(limo)

        override suspend fun sxargxiElsendojnPorKanal(kanalo: Kanalo, fortoRefresigi: Boolean): List<Elsendo> {
            refresxigoVokoj += fortoRefresigi
            return elsendoj.filter { it.kanaloSlug == kanalo.slug }
        }
    }

    @Test
    fun malsuprenTiroRefresxigasKunForto() = runComposeUiTest {
        setContent {
            HejmoEkrano(
                kanaloDeponejo = falsaKanaloDeponejo(),
                elsendoDeponejo = falsaElsendoDeponejo(),
            )
        }
        waitForIdle()
        // Unua ŝargo: 2 podkastaj kanaloj, sen forto
        assertEquals(listOf(false, false), refresxigoVokoj)

        // Tiru de la supro de la listo ĝis la fundo (la sojlo estas ~80dp post rezisto)
        onRoot().performTouchInput { swipeDown(startY = top + height * 0.2f, endY = bottom) }
        waitForIdle()
        assertTrue(refresxigoVokoj.drop(2).isNotEmpty(), "Malsupren-tiro ne ekigis refreŝigon: $refresxigoVokoj")
        assertTrue(refresxigoVokoj.drop(2).all { it }, "Refreŝigo devas uzi fortoRefresigi=true: $refresxigoVokoj")
    }

    @Test
    fun montrasSekciojn() = runComposeUiTest {
        setContent {
            HejmoEkrano(
                kanaloDeponejo = falsaKanaloDeponejo(),
                elsendoDeponejo = falsaElsendoDeponejo(),
            )
        }
        waitForIdle()
        onNodeWithText("Kio novas").assertIsDisplayed()
        onNodeWithText("Aliaj elsendoj").assertIsDisplayed()
        onNodeWithText("Kanaloj").assertIsDisplayed()
    }

    @Test
    fun montrasKanalNomojnEnElsendoj() = runComposeUiTest {
        setContent {
            HejmoEkrano(
                kanaloDeponejo = falsaKanaloDeponejo(),
                elsendoDeponejo = falsaElsendoDeponejo(),
            )
        }
        waitForIdle()
        // "Kernpunkto": kp:1 (Kio novas + Ĉiuj kanaloj), kp:2 (Kio novas), kp:malnova (Kio popularas) = 4x
        // "Varsovia Vento": same 4x
        onAllNodesWithText("Kernpunkto").assertCountEquals(4)
        onAllNodesWithText("Varsovia Vento").assertCountEquals(4)
    }

    @Test
    fun montrasElsendojnEnKioNovas() = runComposeUiTest {
        setContent {
            HejmoEkrano(
                kanaloDeponejo = falsaKanaloDeponejo(),
                elsendoDeponejo = falsaElsendoDeponejo(),
            )
        }
        waitForIdle()
        // Novaj elsendoj (kp:1, kp:2, vv:1, vv:2) aperas en "Kio novas".
        // La plej novaj (kp:1, vv:1) ankaŭ en "Ĉiuj kanaloj".
        // Malnovaj (kp:malnova, vv:malnova) nur en "Kio popularas".
        // Novaj ne aperas en "Kio popularas" (ekspluditaj).
        onAllNodesWithText("Kernpunkto epizodo 1").assertCountEquals(2) // Kio novas + Ĉiuj kanaloj
        onAllNodesWithText("Kernpunkto epizodo 2").assertCountEquals(1) // Kio novas
        onAllNodesWithText("Varsovia Vento epizodo 1").assertCountEquals(2) // Kio novas + Ĉiuj kanaloj
        onAllNodesWithText("Varsovia Vento epizodo 2").assertCountEquals(1) // Kio novas
        onAllNodesWithText("Kernpunkto malnova").assertCountEquals(1) // Kio popularas
        onAllNodesWithText("Varsovia Vento malnova").assertCountEquals(1) // Kio popularas
    }

    @Test
    fun montrasAĝajnEmblemetojn() = runComposeUiTest {
        setContent {
            HejmoEkrano(
                kanaloDeponejo = falsaKanaloDeponejo(),
                elsendoDeponejo = falsaElsendoDeponejo(),
            )
        }
        waitForIdle()
        // Flavaj emblemetoj montras la aĝon de la elsendo — en "Kio novas", "Ĉiuj kanaloj" kaj "Kio popularas"
        // kp:1 (5 tagoj, plej nova): Kio novas + Ĉiuj kanaloj = 2
        // vv:1 (3 tagoj, plej nova): 2
        // kp:2 (12 tagoj): Kio novas = 1
        // vv:2 (17 tagoj → "2 semajnoj"): Kio novas = 1
        // kp:malnova (200 tagoj → "6 monatoj") kaj vv:malnova (220 tagoj → "7 monatoj"): Kio popularas = 1 po
        onAllNodesWithText("5 tagoj").assertCountEquals(2)
        onAllNodesWithText("3 tagoj").assertCountEquals(2)
        onAllNodesWithText("12 tagoj").assertCountEquals(1)
        onAllNodesWithText("2 semajnoj").assertCountEquals(1)
        onAllNodesWithText("6 monatoj").assertCountEquals(1)
        onAllNodesWithText("7 monatoj").assertCountEquals(1)
    }

    @Test
    fun montrasArkivajnKanalojnPostAktivajKunDividilo() = runComposeUiTest {
        val kanaloj = listOf(
            Kanalo(slug = "nova", nomo = "Nova Kanalo", podkastaRssUrl = "https://x.com/n.rss"),
            Kanalo(slug = "meznova", nomo = "Meznova Kanalo", podkastaRssUrl = "https://x.com/m.rss"),
            Kanalo(slug = "antikva", nomo = "Antikva Kanalo", podkastaRssUrl = "https://x.com/a.rss"),
        )
        val elsendoj = listOf(
            Elsendo(id = "nova:1", kanaloSlug = "nova", titolo = "Nova epizodo", fluo = "", dato = datoAntaux(5)),
            // Antaŭ 200 tagoj: pli aĝa ol 6 monatoj (ne en "Kio novas"), sed ene de unu jaro (aktiva)
            Elsendo(id = "meznova:1", kanaloSlug = "meznova", titolo = "Meznova epizodo", fluo = "", dato = datoAntaux(200)),
            // Pli aĝa ol unu jaro: arkiva
            Elsendo(id = "antikva:1", kanaloSlug = "antikva", titolo = "Antikva epizodo", fluo = "", dato = datoAntaux(400)),
        )
        setContent {
            HejmoEkrano(
                kanaloDeponejo = falsaKanaloDeponejo(kanaloj),
                elsendoDeponejo = falsaElsendoDeponejo(elsendoj),
            )
        }
        waitForIdle()
        // Ĉiuj tri kanaloj aperas en la "Kanaloj"-vico: "Nova Kanalo" ankaŭ en "Kio novas",
        // "Meznova Kanalo" kaj "Antikva Kanalo" nur en la kanala vico
        onAllNodesWithText("Nova Kanalo").assertCountEquals(2)
        onAllNodesWithText("Meznova Kanalo").assertCountEquals(1)
        onAllNodesWithText("Antikva Kanalo").assertCountEquals(1)
        // Dividilo kun "Arkivo" inter la aktivaj kaj arkivaj kanaloj
        onNodeWithText("Arkivo").assertIsDisplayed()
        // La flavaj markiloj montras la aĝon de la plej nova elsendo de ĉiu kanalo
        onAllNodesWithText("5 tagoj").assertCountEquals(2) // "Kio novas" + "Kanaloj"
        onAllNodesWithText("6 monatoj").assertCountEquals(1) // meznova (200 tagoj)
        onAllNodesWithText("1 jaro").assertCountEquals(1) // antikva (400 tagoj)
        // La aktivaj kanaloj staras maldekstre de la dividilo, la arkiva dekstre
        fun xDe(teksto: String) = onAllNodesWithText(teksto).fetchSemanticsNodes().first().positionInRoot.x
        assertTrue(xDe("Nova Kanalo") < xDe("Meznova Kanalo"), "aktivaj kanaloj aperas en vico")
        assertTrue(xDe("Meznova Kanalo") < xDe("Arkivo"), "aktiva kanalo devas esti maldekstre de la dividilo")
        assertTrue(xDe("Arkivo") < xDe("Antikva Kanalo"), "arkiva kanalo devas esti dekstre de la dividilo")
    }

    @Test
    fun kanalaMarkiloMontrasAĝonAnkaŭSeLudita() = runComposeUiTest {
        val kanaloj = listOf(
            Kanalo(slug = "nova", nomo = "Nova Kanalo", podkastaRssUrl = "https://x.com/n.rss"),
        )
        val elsendoj = listOf(
            Elsendo(id = "nova:1", kanaloSlug = "nova", titolo = "Nova epizodo", fluo = "", dato = datoAntaux(5)),
        )
        // La uzanto aŭskultis 42% de la plej nova elsendo
        val ludatoj = LudatojDeponejoMaketo()
        runBlocking { ludatoj.registriPozicion("nova:1", "nova", pozicioMs = 252_000, dauroMs = 600_000) }
        setContent {
            HejmoEkrano(
                kanaloDeponejo = falsaKanaloDeponejo(kanaloj),
                elsendoDeponejo = falsaElsendoDeponejo(elsendoj),
                ludatojDeponejo = ludatoj,
            )
        }
        waitForIdle()
        // La flava markilo sur la kanalo montras la aĝon de la plej nova elsendo ("5 tagoj"),
        // neniam la ludprogreson — "aŭdis 42%" aperas nur sur la elsendo-kartoj
        onAllNodesWithText("5 tagoj").assertCountEquals(1)
        onAllNodesWithText("aŭdis 42%").assertCountEquals(1) // nur "Lastatempe ludata" (luditaj malaperas de "Kio novas")
    }

    @Test
    fun luditajElsendojMalaperasDeKioNovasKajAperasEnLastatempeLudata() = runComposeUiTest {
        val kanaloj = listOf(
            Kanalo(slug = "kernpunkto", nomo = "Kernpunkto", podkastaRssUrl = "https://x.com/k.rss"),
            Kanalo(slug = "varsoviavento", nomo = "Varsovia Vento", podkastaRssUrl = "https://x.com/v.rss"),
        )
        val elsendoj = listOf(
            Elsendo(id = "kp:1", kanaloSlug = "kernpunkto", titolo = "Kernpunkto epizodo 1", fluo = "", dato = datoAntaux(5)),
            Elsendo(id = "kp:2", kanaloSlug = "kernpunkto", titolo = "Kernpunkto epizodo 2", fluo = "", dato = datoAntaux(12)),
            Elsendo(id = "vv:1", kanaloSlug = "varsoviavento", titolo = "Varsovia Vento epizodo 1", fluo = "", dato = datoAntaux(3)),
        )
        // La uzanto aŭskultis kp:1
        val ludatoj = LudatojDeponejoMaketo()
        runBlocking { ludatoj.registriPozicion("kp:1", "kernpunkto", pozicioMs = 252_000, dauroMs = 600_000) }
        setContent {
            HejmoEkrano(
                kanaloDeponejo = falsaKanaloDeponejo(kanaloj),
                elsendoDeponejo = falsaElsendoDeponejo(elsendoj),
                ludatojDeponejo = ludatoj,
            )
        }
        waitForIdle()
        // kp:1 malaperas de "Kio novas" — nur aperas en "Lastatempe ludata" + "Kanaloj" (plej nova)
        onAllNodesWithText("Kernpunkto epizodo 1").assertCountEquals(2) // Lastatempe ludata + Kanaloj
        // kp:2 ankoraŭ en "Kio novas"
        onAllNodesWithText("Kernpunkto epizodo 2").assertCountEquals(1) // Kio novas
        // vv:1 ankoraŭ en "Kio novas" + "Kanaloj"
        onAllNodesWithText("Varsovia Vento epizodo 1").assertCountEquals(2) // Kio novas + Kanaloj
        // "Lastatempe ludata" sekcio aperas
        onNodeWithText("Lastatempe ludata").assertIsDisplayed()
    }

    @Test
    fun maksimumeKvinElsendojPoKanaloEnKioNovas() = runComposeUiTest {
        val kanaloj = listOf(
            Kanalo(slug = "kernpunkto", nomo = "Kernpunkto", podkastaRssUrl = "https://x.com/k.rss"),
        )
        // 7 novaj elsendoj de sama kanalo — nur 5 aperu en "Kio novas"
        val elsendoj = (1..7).map { i ->
            Elsendo(id = "kp:$i", kanaloSlug = "kernpunkto", titolo = "Kernpunkto epizodo $i", fluo = "", dato = datoAntaux(i))
        }
        setContent {
            HejmoEkrano(
                kanaloDeponejo = falsaKanaloDeponejo(kanaloj),
                elsendoDeponejo = falsaElsendoDeponejo(elsendoj),
            )
        }
        waitForIdle()
        // Epizodoj 1-5 (plej novaj) aperas en "Kio novas" + epizodo 1 ankaŭ en "Kanaloj"
        onAllNodesWithText("Kernpunkto epizodo 1").assertCountEquals(2) // Kio novas + Kanaloj
        onAllNodesWithText("Kernpunkto epizodo 5").assertCountEquals(1) // Kio novas
        // Epizodoj 6-7 ne aperas en "Kio novas" (maks 5 po kanalo)
        onAllNodesWithText("Kernpunkto epizodo 6").assertCountEquals(1) // nur Kio popularas
        onAllNodesWithText("Kernpunkto epizodo 7").assertCountEquals(1) // nur Kio popularas
    }
}