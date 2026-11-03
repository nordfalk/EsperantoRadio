package dk.nordfalk.esperanto.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import dk.nordfalk.esperanto.domain.model.Elsendo
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Testoj de la ludvico-butono en la supra menua linio: videbla nur se la vico
 * enhavas elementojn.
 *
 * Rulu per: ./gradlew :shared:desktopTest --tests "*LudvicoButonoTesto*"
 */
@OptIn(ExperimentalTestApi::class)
class LudvicoButonoTesto {

    private val testElsendo = Elsendo(
        id = "kernpunkto:2024-01-01",
        kanaloSlug = "kernpunkto",
        titolo = "KP204 Pigmentoj kaj koloroj en la naturo",
        fluo = "https://kern.punkto.info/podkasto.mp3",
        dato = "2024-01-01",
    )

    @Test
    fun kaŝita_se_vico_malplena() = runComposeUiTest {
        val vico = MutableStateFlow(emptyList<Elsendo>())
        setContent { MaterialTheme { LudvicoButono(vico = vico) } }
        waitForIdle()

        onNodeWithContentDescription("Ludvico").assertDoesNotExist()
    }

    @Test
    fun videbla_se_vico_ne_malplena_kaj_klako_vokas_onLudvico() = runComposeUiTest {
        val vico = MutableStateFlow(listOf(testElsendo))
        var vokita = false
        setContent { MaterialTheme { LudvicoButono(vico = vico, onLudvico = { vokita = true }) } }
        waitForIdle()

        onNodeWithContentDescription("Ludvico").assertExists()
        onNodeWithContentDescription("Ludvico").performClick()
        waitForIdle()
        assertTrue(vokita, "onLudvico devas esti vokita")
    }

    @Test
    fun aperas_kaj_malaperas_kiam_vico_ŝanĝiĝas() = runComposeUiTest {
        val vico = MutableStateFlow(emptyList<Elsendo>())
        setContent { MaterialTheme { LudvicoButono(vico = vico) } }
        waitForIdle()
        onNodeWithContentDescription("Ludvico").assertDoesNotExist()

        vico.value = listOf(testElsendo)
        waitForIdle()
        onNodeWithContentDescription("Ludvico").assertExists()

        vico.value = emptyList()
        waitForIdle()
        onNodeWithContentDescription("Ludvico").assertDoesNotExist()
    }
}
