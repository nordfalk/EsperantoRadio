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
        onNodeWithText("Kio popularas").assertIsDisplayed()
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
        // "Kernpunkto" aperas en "Kio novas" (2 elsendoj) kaj "Kio popularas" (2 hazardaj) = 4x
        // "Varsovia Vento" same 4x. Muzaiko ne havas podkastojn, do ĝi ne aperas en elsendoj.
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
        // Ĉiuj 4 elsendoj aperas en "Kio novas" kaj ankaŭ en "Kio popularas"
        // (nur 4 elsendoj, do take(20) prenas ĉiujn) = 2-foje po titolo
        onAllNodesWithText("Kernpunkto epizodo 1").assertCountEquals(2)
        onAllNodesWithText("Kernpunkto epizodo 2").assertCountEquals(2)
        onAllNodesWithText("Varsovia Vento epizodo 1").assertCountEquals(2)
        onAllNodesWithText("Varsovia Vento epizodo 2").assertCountEquals(2)
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
        // La flava emblemeto montras kiom nova la elsendo estas
        // (aperas nur en "Kio novas", ne en "Kio popularas")
        // 5 tagoj, 12 tagoj (<=14 restas tagoj), 3 tagoj, 17 tagoj (-> 2 semajnoj)
        onNodeWithText("5 tagoj").assertIsDisplayed()
        onNodeWithText("3 tagoj").assertIsDisplayed()
        onNodeWithText("12 tagoj").assertIsDisplayed()
        onNodeWithText("2 semajnoj").assertIsDisplayed()
    }
}
