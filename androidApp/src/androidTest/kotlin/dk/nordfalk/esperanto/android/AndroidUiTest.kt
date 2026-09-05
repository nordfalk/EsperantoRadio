package dk.nordfalk.esperanto.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dk.nordfalk.esperanto.EsperantoRadioApp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentita UI-testo por la Android-apo.
 * Bezonas emulatoron aŭ realan aparaton (./gradlew :androidApp:connectedDebugAndroidTest).
 */
@RunWith(AndroidJUnit4::class)
class AndroidUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun kanalaroMontrasKanalojn() {
        composeRule.waitForIdle()
        // Atendu ke la kanalaro ŝarĝu
        composeRule.waitUntil(timeoutMillis = 10000) {
            composeRule.onAllNodesWithText("Muzaiko").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Muzaiko").assertIsDisplayed()
    }

    @Test
    fun navigadoAlSercxo() {
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 10000) {
            composeRule.onAllNodesWithText("Muzaiko").fetchSemanticsNodes().isNotEmpty()
        }
        // Klaku sur serĉo-butono (🔍)
        composeRule.onNodeWithText("🔍").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Trovu elsendon...").assertIsDisplayed()
    }

    @Test
    fun navigadoAlAgordoj() {
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 10000) {
            composeRule.onAllNodesWithText("Muzaiko").fetchSemanticsNodes().isNotEmpty()
        }
        // Klaku sur agordoj-butono (⚙)
        composeRule.onNodeWithText("⚙").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Agordoj").assertIsDisplayed()
        composeRule.onNodeWithText("Nur per WiFi").assertIsDisplayed()
    }

    @Test
    fun navigadoAlPlejsatataj() {
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 10000) {
            composeRule.onAllNodesWithText("Muzaiko").fetchSemanticsNodes().isNotEmpty()
        }
        // Klaku sur plejŝatataj-butono (★)
        composeRule.onNodeWithText("★").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Plej ŝatataj").assertIsDisplayed()
    }
}
