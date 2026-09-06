package dk.nordfalk.esperanto.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import dk.nordfalk.esperanto.domain.model.Elsendo
import dk.nordfalk.esperanto.domain.model.Kanal
import dk.nordfalk.esperanto.domain.repository.ElsendoDeponejo
import dk.nordfalk.esperanto.domain.repository.KanalDeponejo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.test.Test

/**
 * UI-testoj por HejmoEkrano — testas plurajn tavolojn (UI + deponejo).
 *
 * Uzas falsan ElsendoDeponejo kiu liveras realajn testelsendojn
 * por "Kio novas" kaj "Kio popularas".
 */
@OptIn(ExperimentalTestApi::class)
class HejmoEkranoTest {

    private val testKanaloj = listOf(
        Kanal(slug = "muzaiko", nomo = "Muzaiko", rektaElsendaSonoUrl = "https://x.com/m.m3u8"),
        Kanal(slug = "kernpunkto", nomo = "Kernpunkto", podkastaRssUrl = "https://x.com/k.rss"),
        Kanal(slug = "varsoviavento", nomo = "Varsovia Vento", podkastaRssUrl = "https://x.com/v.rss"),
    )

    private val testElsendoj = listOf(
        Elsendo(id = "kp:1", kanalSlug = "kernpunkto", titolo = "Kernpunkto epizodo 1", stream = "", dato = "2026-09-01"),
        Elsendo(id = "kp:2", kanalSlug = "kernpunkto", titolo = "Kernpunkto epizodo 2", stream = "", dato = "2026-08-25"),
        Elsendo(id = "vv:1", kanalSlug = "varsoviavento", titolo = "Varsovia Vento epizodo 1", stream = "", dato = "2026-09-03"),
        Elsendo(id = "vv:2", kanalSlug = "varsoviavento", titolo = "Varsovia Vento epizodo 2", stream = "", dato = "2026-08-20"),
    )

    private fun falsaKanalDeponejo() = object : KanalDeponejo {
        private val f = MutableStateFlow(testKanaloj)
        override fun observiKanalojn() = f.asStateFlow()
        override suspend fun getKanalojn(fortoRefresigi: Boolean) = f.value
        override suspend fun getKanal(slug: String) = f.value.find { it.slug == slug }
    }

    private fun falsaElsendoDeponejo() = object : ElsendoDeponejo {
        override fun observiElsendojn(kanalSlug: String): Flow<List<Elsendo>> =
            MutableStateFlow(testElsendoj.filter { it.kanalSlug == kanalSlug }).asStateFlow()

        override suspend fun getElsendojn(kanalSlug: String, fortoRefresigi: Boolean): List<Elsendo> =
            testElsendoj.filter { it.kanalSlug == kanalSlug }

        override suspend fun getElsendo(id: String): Elsendo? = testElsendoj.find { it.id == id }

        override suspend fun sercxiElsendojn(taxto: String, limo: Int): List<Elsendo> =
            testElsendoj.filter { it.titolo.contains(taxto, ignoreCase = true) }.take(limo)

        override suspend fun sxargxiElsendojnPorKanal(kanal: Kanal, fortoRefresigi: Boolean): List<Elsendo> =
            testElsendoj.filter { it.kanalSlug == kanal.slug }
    }

    @Test
    fun montrasSekciojn() = runComposeUiTest {
        setContent {
            HejmoEkrano(
                kanalDeponejo = falsaKanalDeponejo(),
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
                kanalDeponejo = falsaKanalDeponejo(),
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
    fun montrasNavigaBreton() = runComposeUiTest {
        setContent {
            HejmoEkrano(
                kanalDeponejo = falsaKanalDeponejo(),
                elsendoDeponejo = falsaElsendoDeponejo(),
            )
        }
        waitForIdle()
        onNodeWithText("Hejmo").assertIsDisplayed()
        onNodeWithText("Kanaloj").assertIsDisplayed()
        onNodeWithText("Plej ŝatataj").assertIsDisplayed()
        onNodeWithText("Serĉi").assertIsDisplayed()
    }

    @Test
    fun montrasElsendojnEnKioNovas() = runComposeUiTest {
        setContent {
            HejmoEkrano(
                kanalDeponejo = falsaKanalDeponejo(),
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
}
