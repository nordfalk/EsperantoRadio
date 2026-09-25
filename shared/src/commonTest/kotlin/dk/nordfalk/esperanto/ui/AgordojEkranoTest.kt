package dk.nordfalk.esperanto.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import dk.nordfalk.esperanto.ApoVersio
import dk.nordfalk.esperanto.data.repository.AgordojDeponejoImpl
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
        onNodeWithText("Versio ${ApoVersio.VERSION}").assertIsDisplayed()
    }
}
