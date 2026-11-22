package dk.nordfalk.esperanto.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertTrue

/** Testo por la RSS-adresa vico de la kanal-ekrano (C54). */
@OptIn(ExperimentalTestApi::class)
class RssAdresVicoTest {

    @Test
    fun montrasAdresonKlarigonKajButonojn() = runComposeUiTest {
        var kopiiVokita = false
        var partigiVokita = false
        setContent {
            MaterialTheme {
                RssAdresVico(
                    rss = "https://ekzemplo.test/feed.xml",
                    kanaloNomo = "Testo",
                    onKopii = { kopiiVokita = true },
                    onPartigi = { partigiVokita = true },
                )
            }
        }
        waitForIdle()
        onNodeWithText("https://ekzemplo.test/feed.xml").assertIsDisplayed()
        onNodeWithText("RSS-adreso de Testo:").assertIsDisplayed()
        onNodeWithText("Kopii").performClick()
        onNodeWithText("Disdividi").performClick()
        assertTrue(kopiiVokita, "Butono Kopii devas voki onKopii")
        assertTrue(partigiVokita, "Butono Disdividi devas voki onPartigi")
    }
}
