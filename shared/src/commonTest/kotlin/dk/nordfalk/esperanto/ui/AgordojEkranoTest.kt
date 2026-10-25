package dk.nordfalk.esperanto.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import dk.nordfalk.esperanto.ApoVersio
import dk.nordfalk.esperanto.data.repository.AgordojDeponejoImpl
import dk.nordfalk.esperanto.domain.model.DiagnozoRezulto
import dk.nordfalk.esperanto.domain.model.DiagnozoTipo
import dk.nordfalk.esperanto.domain.model.Severeco
import kotlin.test.Test

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
        // La versio estas malsupre — skrolu al ĝi (la ekrano nun havas pli da sekcioj)
        onNodeWithText("Versio ${ApoVersio.VERSION}").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun montrasAvertosignonĈeDiagnozoKiamProblemojEkzistas() = runComposeUiTest {
        val problemoj = kotlinx.coroutines.flow.MutableStateFlow(
            listOf(
                DiagnozoRezulto(
                    tipo = DiagnozoTipo.BATERIO_OPTIMIZATION,
                    titolo = "Bateri-optimumigo estas ŝaltita",
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
    fun montrasŜaltilonPorDetalaProtokolado() = runComposeUiTest {
        setContent {
            AgordojEkrano(agordojDeponejo = AgordojDeponejoImpl(), onReen = {})
        }
        waitForIdle()
        onNodeWithText("Ŝaltu detalan protokoladon").assertIsDisplayed()
    }
}
