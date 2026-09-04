package dk.nordfalk.esperanto.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import dk.nordfalk.esperanto.domain.model.Kanal
import dk.nordfalk.esperanto.domain.repository.KanalDeponejo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.test.Test

/**
 * UI-testo por la kanalaro-ekrano.
 *
 * Uzas falsan deponejon kun antaŭdifinitaj kanaloj por testi la UI sen reto.
 */
@OptIn(ExperimentalTestApi::class)
class KanalaroEkranoTest {

    private class FalsaKanalDeponejo(kanaloj: List<Kanal>) : KanalDeponejo {
        private val _kanaloj = MutableStateFlow(kanaloj)
        override fun observiKanalojn(): StateFlow<List<Kanal>> = _kanaloj.asStateFlow()
        override suspend fun getKanalojn(fortoRefresigi: Boolean): List<Kanal> = _kanaloj.value
        override suspend fun getKanal(slug: String): Kanal? = _kanaloj.value.find { it.slug == slug }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun kanalaroMontrasKanalojn() = runComposeUiTest {
        val deponejo = FalsaKanalDeponejo(
            listOf(
                Kanal(slug = "muzaiko", nomo = "Muzaiko", rektaElsendaSonoUrl = "x"),
                Kanal(slug = "kernpunkto", nomo = "Kernpunkto", podkastaRssUrl = "y"),
            )
        )
        val viewModel = KanalaroViewModel(deponejo)

        setContent {
            KanalaroEkrano(viewModel = viewModel)
        }

        waitForIdle()
        onNodeWithText("Muzaiko").assertIsDisplayed()
        onNodeWithText("Kernpunkto").assertIsDisplayed()
        onNodeWithText("Rekta elsendo").assertIsDisplayed()
        onNodeWithText("Podkasto").assertIsDisplayed()
    }
}
