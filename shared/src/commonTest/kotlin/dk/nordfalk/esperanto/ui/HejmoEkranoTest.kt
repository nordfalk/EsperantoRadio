package dk.nordfalk.esperanto.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.repository.ElsendoDeponejo
import dk.nordfalk.esperanto.domain.repository.KanaloDeponejo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlin.test.Test

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

    private fun falsaKanaloDeponejo() = object : KanaloDeponejo {
        private val f = MutableStateFlow(testKanaloj)
        override fun observiKanalojn() = f.asStateFlow()
        override suspend fun getKanalojn(fortoRefresigi: Boolean) = f.value
        override suspend fun getKanalo(slug: String) = f.value.find { it.slug == slug }
    }

    private fun falsaElsendoDeponejo() = object : ElsendoDeponejo {
        override fun observiElsendojn(kanaloSlug: String): Flow<List<Elsendo>> =
            MutableStateFlow(testElsendoj.filter { it.kanaloSlug == kanaloSlug }).asStateFlow()

        override suspend fun getElsendojn(kanaloSlug: String, fortoRefresigi: Boolean): List<Elsendo> =
            testElsendoj.filter { it.kanaloSlug == kanaloSlug }

        override suspend fun getElsendo(id: String): Elsendo? = testElsendoj.find { it.id == id }

        override suspend fun sercxiElsendojn(teksto: String, limo: Int): List<Elsendo> =
            testElsendoj.filter { it.titolo.contains(teksto, ignoreCase = true) }.take(limo)

        override suspend fun sxargxiElsendojnPorKanal(kanalo: Kanalo, fortoRefresigi: Boolean): List<Elsendo> =
            testElsendoj.filter { it.kanaloSlug == kanalo.slug }
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
    fun montrasNovectempajnEmblemetojn() = runComposeUiTest {
        setContent {
            HejmoEkrano(
                kanaloDeponejo = falsaKanaloDeponejo(),
                elsendoDeponejo = falsaElsendoDeponejo(),
            )
        }
        waitForIdle()
        // Flavaj emblemetoj montras kiom nova la elsendo estas — nur en "Kio novas" kaj "Ĉiuj kanaloj"
        // (malnovaj elsendoj en "Kio popularas" estas >6 monatoj, do neniu emblemeto)
        // kp:1 (5 tagoj, plej nova): Kio novas + Ĉiuj kanaloj = 2
        // vv:1 (3 tagoj, plej nova): 2
        // kp:2 (12 tagoj): Kio novas = 1
        // vv:2 (17 tagoj → "2 semajnoj"): Kio novas = 1
        onAllNodesWithText("5 tagoj").assertCountEquals(2)
        onAllNodesWithText("3 tagoj").assertCountEquals(2)
        onAllNodesWithText("12 tagoj").assertCountEquals(1)
        onAllNodesWithText("2 semajnoj").assertCountEquals(1)
    }
}
