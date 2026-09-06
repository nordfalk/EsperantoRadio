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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.test.Test

/**
 * UI-testoj por HejmoEkrano — testas plurajn tavolojn (UI + deponejo).
 */
@OptIn(ExperimentalTestApi::class)
class HejmoEkranoTest {

    private val testKanaloj = listOf(
        Kanal(slug = "muzaiko", nomo = "Muzaiko", rektaElsendaSonoUrl = "https://x.com/m.m3u8"),
        Kanal(slug = "kernpunkto", nomo = "Kernpunkto", podkastaRssUrl = "https://x.com/k.rss"),
        Kanal(slug = "varsoviavento", nomo = "Varsovia Vento", podkastaRssUrl = "https://x.com/v.rss"),
    )

    private fun falsaKanalDeponejo() = object : KanalDeponejo {
        private val f = MutableStateFlow(testKanaloj)
        override fun observiKanalojn() = f.asStateFlow()
        override suspend fun getKanalojn(fortoRefresigi: Boolean) = f.value
        override suspend fun getKanal(slug: String) = f.value.find { it.slug == slug }
    }

    private fun falsaElsendoDeponejo() = object : ElsendoDeponejo {
        override fun observiElsendojn(kanalSlug: String) = MutableStateFlow(emptyList<Elsendo>()).asStateFlow()
        override suspend fun getElsendojn(kanalSlug: String, fortoRefresigi: Boolean) = emptyList<Elsendo>()
        override suspend fun getElsendo(id: String): Elsendo? = null
        override suspend fun sercxiElsendojn(taxto: String, limo: Int) = emptyList<Elsendo>()
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
    fun montrasKanalNomojn() = runComposeUiTest {
        setContent {
            HejmoEkrano(
                kanalDeponejo = falsaKanalDeponejo(),
                elsendoDeponejo = falsaElsendoDeponejo(),
            )
        }
        waitForIdle()
        // "Muzaiko" aperas en "Kio novas" (2x) + "Kio popularas" (1x) = 3x
        onAllNodesWithText("Muzaiko").assertCountEquals(3)
        onAllNodesWithText("Kernpunkto").assertCountEquals(3)
        onAllNodesWithText("Varsovia Vento").assertCountEquals(3)
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
        // "Kio novas" generas elsendojn por cxiu kanal — titolo estas "Elsendo de 2024-01-01"
        // 3 kanaloj x 1 = 3 aperoj de "Elsendo de 2024-01-01"
        onAllNodesWithText("Elsendo de 2024-01-01").assertCountEquals(3)
        // "Kio popularas" montras kanal-tipojn
        onNodeWithText("Rekta elsendo").assertIsDisplayed()
        onAllNodesWithText("Podkasto").assertCountEquals(2)
    }
}
