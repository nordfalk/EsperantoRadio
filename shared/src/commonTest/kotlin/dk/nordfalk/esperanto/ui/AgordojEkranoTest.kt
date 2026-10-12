package dk.nordfalk.esperanto.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import dk.nordfalk.esperanto.ApoVersio
import dk.nordfalk.esperanto.data.repository.AgordojDeponejoImpl
import dk.nordfalk.esperanto.domain.model.DiagnozoRezulto
import dk.nordfalk.esperanto.domain.model.DiagnozoTipo
import dk.nordfalk.esperanto.domain.model.Severeco
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * UI-testoj por AgordojEkrano — testas plurajn tavolojn (UI + generita ApoVersio).
 */
@OptIn(ExperimentalTestApi::class)
class AgordojEkranoTest {

    @Test
    fun montrasVersionDeLaApo() = runComposeUiTest {
        setContent {
            AgordojEkrano(agordojDeponejo = AgordojDeponejoImpl(), onReen = {})
        }
        waitForIdle()
        onNodeWithText("Versio ${ApoVersio.VERSION}").assertIsDisplayed()
    }

    @Test
    fun montrasAvertosignonCxeDiagnozoKiamProblemojEkzistas() = runComposeUiTest {
        val problemoj = kotlinx.coroutines.flow.MutableStateFlow(
            listOf(
                DiagnozoRezulto(
                    tipo = DiagnozoTipo.BATERIO_OPTIMIZATION,
                    titolo = "Bateri-optimumado estas ŝaltita",
                    priskribo = "La sistemo povas limigi la aplikon en la fono.",
                    severeco = Severeco.ERARO,
                )
            )
        )
        setContent {
            AgordojEkrano(
                agordojDeponejo = AgordojDeponejoImpl(),
                onReen = {},
                diagnozoProblemoj = problemoj,
            )
        }
        waitForIdle()
        onNodeWithText("1 problemo(j) detektitaj — tuŝu por vidi la detalojn").assertIsDisplayed()
    }

    @Test
    fun hejmaAvertosxaltiloBaskuligasLaAgordon() = runComposeUiTest {
        val deponejo = AgordojDeponejoImpl()
        setContent {
            AgordojEkrano(agordojDeponejo = deponejo, onReen = {})
        }
        waitForIdle()
        assertTrue(deponejo.montruHejmajnAvertojn.value, "Defaŭlte la hejmaj avertoj devas esti ŝaltitaj")
        onNodeWithText("Montri avertojn sur la ĉefekrano").performClick()
        waitForIdle()
        assertFalse(deponejo.montruHejmajnAvertojn.value, "Klako devas malŝalti la hejmajn avertojn")
        onNodeWithText("Montri avertojn sur la ĉefekrano").performClick()
        waitForIdle()
        assertTrue(deponejo.montruHejmajnAvertojn.value, "Dua klako devas ree ŝalti ilin")
    }
}
