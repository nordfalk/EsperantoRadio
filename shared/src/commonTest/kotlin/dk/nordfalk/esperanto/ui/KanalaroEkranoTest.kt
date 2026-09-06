package dk.nordfalk.esperanto.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import dk.nordfalk.esperanto.domain.model.Kanalo
import dk.nordfalk.esperanto.domain.repository.KanaloDeponejo
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

    private class FalsaKanaloDeponejo(kanaloj: List<Kanalo>) : KanaloDeponejo {
        private val _kanaloj = MutableStateFlow(kanaloj)
        override fun observiKanalojn(): StateFlow<List<Kanalo>> = _kanaloj.asStateFlow()
        override suspend fun getKanalojn(fortoRefresigi: Boolean): List<Kanalo> = _kanaloj.value
        override suspend fun getKanalo(slug: String): Kanalo? = _kanaloj.value.find { it.slug == slug }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun kanalaroMontrasKanalojn() = runComposeUiTest {
        val deponejo = FalsaKanaloDeponejo(
            listOf(
                Kanalo(slug = "muzaiko", nomo = "Muzaiko", rektaElsendaSonoUrl = "x"),
                Kanalo(slug = "kernpunkto", nomo = "Kernpunkto", podkastaRssUrl = "y"),
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
